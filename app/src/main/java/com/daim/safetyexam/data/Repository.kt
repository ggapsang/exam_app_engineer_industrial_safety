package com.daim.safetyexam.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject

class Repository private constructor(private val db: SQLiteDatabase) {

    companion object {
        @Volatile private var instance: Repository? = null
        fun get(context: Context): Repository {
            return instance ?: synchronized(this) {
                instance ?: Repository(DbProvider.get(context)).also { instance = it }
            }
        }
        private fun asset(path: String?): String? =
            if (path.isNullOrBlank()) null else "file:///android_asset/$path"
    }

    // ---------- 마스터 데이터 ----------

    fun subjects(): List<Subject> = db.rawQuery(
        "SELECT subject_id, name_ko, short_name, sort_order FROM subjects ORDER BY sort_order", null
    ).use { c ->
        buildList { while (c.moveToNext()) add(Subject(c.getInt(0), c.getString(1), c.getString(2), c.getInt(3))) }
    }

    fun exams(): List<Exam> = db.rawQuery(
        "SELECT exam_id, year, session, exam_date, delivery_type FROM exams ORDER BY year, session", null
    ).use { c ->
        buildList { while (c.moveToNext()) add(Exam(c.getInt(0), c.getInt(1), c.getInt(2), c.getStringOrNull(3), c.getString(4))) }
    }

    // ---------- 문항 ID 선택 ----------

    fun examQuestionIds(examId: Int): List<Int> = db.rawQuery(
        "SELECT question_id FROM questions WHERE exam_id=? ORDER BY q_number", arrayOf(examId.toString())
    ).use { it.toIntList() }

    /** order: "random" | "year" */
    fun subjectQuestionIds(subjectId: Int, count: Int, order: String): List<Int> {
        val orderSql = if (order == "year") "e.year, e.session, q.q_number" else "RANDOM()"
        return db.rawQuery(
            """
            SELECT q.question_id FROM questions q
            JOIN exams e ON e.exam_id = q.exam_id
            WHERE q.subject_id=?
            ORDER BY $orderSql
            LIMIT ?
            """.trimIndent(),
            arrayOf(subjectId.toString(), count.toString())
        ).use { it.toIntList() }
    }

    /**
     * F3 모의고사: 6과목 × 20문항.
     * - 최근 5세션 내 풀이 이력 문항은 가중치↓ (있으면 뒤로)
     * - 동일 회차 ≤ 20%(=24문항) 제약을 후처리로 보장
     */
    fun mockQuestionIds(): List<Int> {
        val recent = recentlyAttemptedIds(limitSessions = 5)
        val perExamCap = 24
        val examCount = HashMap<Int, Int>()
        val result = ArrayList<Int>(120)

        for (subjectId in 1..6) {
            val pool = db.rawQuery(
                """
                SELECT q.question_id, q.exam_id FROM questions q
                WHERE q.subject_id=?
                ORDER BY (CASE WHEN q.question_id IN (${recent.joinToString(",").ifEmpty { "0" }}) THEN 1 ELSE 0 END), RANDOM()
                """.trimIndent(),
                arrayOf(subjectId.toString())
            ).use { c ->
                buildList { while (c.moveToNext()) add(c.getInt(0) to c.getInt(1)) }
            }
            var picked = 0
            for ((qid, examId) in pool) {
                if (picked >= 20) break
                if ((examCount[examId] ?: 0) >= perExamCap) continue
                result.add(qid)
                examCount[examId] = (examCount[examId] ?: 0) + 1
                picked++
            }
        }
        return result
    }

    private fun recentlyAttemptedIds(limitSessions: Int): List<Int> {
        // 최근 세션 5개의 시간 이후 풀이된 문항 (간이 가중치)
        val since = db.rawQuery(
            "SELECT started_at FROM study_sessions ORDER BY session_id DESC LIMIT 1 OFFSET ?",
            arrayOf((limitSessions - 1).toString())
        ).use { if (it.moveToFirst()) it.getStringOrNull(0) else null } ?: return emptyList()
        return db.rawQuery(
            "SELECT DISTINCT question_id FROM user_attempts WHERE attempted_at >= ?", arrayOf(since)
        ).use { it.toIntList() }
    }

    fun favoriteQuestionIds(): List<Int> = db.rawQuery(
        "SELECT question_id FROM favorites ORDER BY added_at DESC", null
    ).use { it.toIntList() }

    // ---------- 문항 로딩 ----------

    fun loadQuestions(ids: List<Int>): List<QuestionFull> {
        if (ids.isEmpty()) return emptyList()
        val inClause = ids.joinToString(",")
        val map = HashMap<Int, QuestionFull>()

        db.rawQuery(
            """
            SELECT q.question_id, q.exam_id, e.year, e.session, q.subject_id, s.short_name,
                   q.q_number, q.stem, q.answer_no, q.is_disputed,
                   ex.body, ex.references_md
            FROM questions q
            JOIN exams e ON e.exam_id = q.exam_id
            JOIN subjects s ON s.subject_id = q.subject_id
            LEFT JOIN explanations ex ON ex.question_id = q.question_id
            WHERE q.question_id IN ($inClause)
            """.trimIndent(), null
        ).use { c ->
            while (c.moveToNext()) {
                val qid = c.getInt(0)
                map[qid] = QuestionFull(
                    id = qid,
                    examId = c.getInt(1),
                    examTitle = "${c.getInt(2)}년 ${c.getInt(3)}회",
                    subjectId = c.getInt(4),
                    subjectShort = c.getString(5),
                    qNumber = c.getInt(6),
                    stem = c.getString(7),
                    answerNo = c.getInt(8),
                    isDisputed = c.getInt(9) == 1,
                    choices = emptyList(),
                    explanation = c.getStringOrNull(10),
                    referencesMd = c.getStringOrNull(11),
                    stemImage = null
                )
            }
        }

        // 보기
        val choiceImg = HashMap<Int, HashMap<Int, String>>() // qid -> (choiceNo -> asset)
        val stemImg = HashMap<Int, String>()
        db.rawQuery(
            "SELECT question_id, role, path FROM question_images WHERE question_id IN ($inClause)", null
        ).use { c ->
            while (c.moveToNext()) {
                val qid = c.getInt(0)
                val role = c.getString(1)
                val a = asset(c.getString(2)) ?: continue
                if (role == "stem") stemImg[qid] = a
                else if (role.startsWith("choice")) {
                    val n = role.removePrefix("choice").toIntOrNull() ?: continue
                    choiceImg.getOrPut(qid) { HashMap() }[n] = a
                }
            }
        }

        val choicesByQ = HashMap<Int, MutableList<Choice>>()
        db.rawQuery(
            "SELECT question_id, choice_no, body FROM choices WHERE question_id IN ($inClause) ORDER BY choice_no", null
        ).use { c ->
            while (c.moveToNext()) {
                val qid = c.getInt(0)
                val no = c.getInt(1)
                choicesByQ.getOrPut(qid) { mutableListOf() }
                    .add(Choice(no, c.getString(2), choiceImg[qid]?.get(no)))
            }
        }

        // 조립 + ids 순서 유지
        return ids.mapNotNull { qid ->
            map[qid]?.copy(
                choices = choicesByQ[qid] ?: emptyList(),
                stemImage = stemImg[qid]
            )
        }
    }

    // ---------- 풀이 기록 / 세션 ----------

    fun recordAttempt(questionId: Int, selectedNo: Int?, isCorrect: Boolean, elapsedMs: Long) {
        db.execSQL(
            "INSERT INTO user_attempts(user_id, question_id, selected_no, is_correct, elapsed_ms) VALUES(?,?,?,?,?)",
            arrayOf<Any?>(DbProvider.LOCAL_USER, questionId, selectedNo, if (isCorrect) 1 else 0, elapsedMs)
        )
    }

    fun saveSession(result: SessionResult, startedAt: String) {
        db.execSQL(
            "INSERT INTO study_sessions(mode, started_at, finished_at, total, correct, elapsed_sec) VALUES(?,?,datetime('now'),?,?,?)",
            arrayOf<Any?>(result.mode.name.lowercase(), startedAt, result.total, result.correct, result.elapsedSec)
        )
    }

    // ---------- 즐겨찾기 / 메모 ----------

    fun isFavorite(questionId: Int): Boolean = db.rawQuery(
        "SELECT 1 FROM favorites WHERE question_id=?", arrayOf(questionId.toString())
    ).use { it.moveToFirst() }

    fun toggleFavorite(questionId: Int): Boolean {
        return if (isFavorite(questionId)) {
            db.execSQL("DELETE FROM favorites WHERE question_id=?", arrayOf<Any?>(questionId))
            false
        } else {
            db.execSQL("INSERT OR REPLACE INTO favorites(question_id) VALUES(?)", arrayOf<Any?>(questionId))
            true
        }
    }

    fun getMemo(questionId: Int): String? = db.rawQuery(
        "SELECT content FROM memos WHERE question_id=?", arrayOf(questionId.toString())
    ).use { if (it.moveToFirst()) it.getString(0) else null }

    fun saveMemo(questionId: Int, content: String) {
        if (content.isBlank()) {
            db.execSQL("DELETE FROM memos WHERE question_id=?", arrayOf<Any?>(questionId))
        } else {
            db.execSQL(
                "INSERT OR REPLACE INTO memos(question_id, content, updated_at) VALUES(?,?,datetime('now'))",
                arrayOf<Any?>(questionId, content)
            )
        }
    }

    // ---------- 오답노트 (F4) ----------

    /** minWrong: 1 = 한 번 이상 틀림, 2 = 두 번 이상. recentDays: null 이면 전체 */
    fun wrongNotes(minWrong: Int, recentDays: Int?): List<WrongNoteItem> {
        val dateFilter = if (recentDays != null) "AND a.attempted_at >= datetime('now','-$recentDays days')" else ""
        return db.rawQuery(
            """
            SELECT q.question_id, q.subject_id, s.short_name, e.year, e.session, q.q_number, q.stem,
                   SUM(CASE WHEN a.is_correct=0 THEN 1 ELSE 0 END) AS wrong_cnt,
                   (SELECT 1 FROM favorites f WHERE f.question_id=q.question_id) AS fav
            FROM user_attempts a
            JOIN questions q ON q.question_id = a.question_id
            JOIN exams e ON e.exam_id = q.exam_id
            JOIN subjects s ON s.subject_id = q.subject_id
            WHERE 1=1 $dateFilter
            GROUP BY q.question_id
            HAVING wrong_cnt >= ?
            ORDER BY fav DESC, q.subject_id, wrong_cnt DESC
            """.trimIndent(),
            arrayOf(minWrong.toString())
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val stem = c.getString(6)
                    add(
                        WrongNoteItem(
                            questionId = c.getInt(0),
                            subjectId = c.getInt(1),
                            subjectShort = c.getString(2),
                            examTitle = "${c.getInt(3)}년 ${c.getInt(4)}회",
                            qNumber = c.getInt(5),
                            stemPreview = stem.take(40),
                            wrongCount = c.getInt(7),
                            isFavorite = !c.isNull(8)
                        )
                    )
                }
            }
        }
    }

    // ---------- 검색 (F7) ----------

    fun search(keyword: String, limit: Int = 100): List<SearchHit> {
        val kw = "%${keyword.trim()}%"
        return db.rawQuery(
            """
            SELECT DISTINCT q.question_id, e.year, e.session, q.q_number, s.short_name, q.stem
            FROM questions q
            JOIN exams e ON e.exam_id = q.exam_id
            JOIN subjects s ON s.subject_id = q.subject_id
            LEFT JOIN choices ch ON ch.question_id = q.question_id
            LEFT JOIN explanations ex ON ex.question_id = q.question_id
            WHERE q.stem LIKE ? OR ch.body LIKE ? OR ex.body LIKE ?
            ORDER BY e.year, e.session, q.q_number
            LIMIT ?
            """.trimIndent(),
            arrayOf(kw, kw, kw, limit.toString())
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        SearchHit(
                            questionId = c.getInt(0),
                            examTitle = "${c.getInt(1)}년 ${c.getInt(2)}회",
                            qNumber = c.getInt(3),
                            subjectShort = c.getString(4),
                            preview = c.getString(5).take(40)
                        )
                    )
                }
            }
        }
    }

    // ---------- 통계 (F8) ----------

    fun stats(): StatsSummary {
        val total = scalarInt("SELECT COUNT(*) FROM user_attempts")
        val correct = scalarInt("SELECT COUNT(*) FROM user_attempts WHERE is_correct=1")

        val subjectStats = db.rawQuery(
            """
            SELECT s.subject_id, s.short_name,
                   COUNT(a.attempt_id),
                   SUM(CASE WHEN a.is_correct=1 THEN 1 ELSE 0 END)
            FROM subjects s
            LEFT JOIN questions q ON q.subject_id = s.subject_id
            LEFT JOIN user_attempts a ON a.question_id = q.question_id
            GROUP BY s.subject_id
            ORDER BY s.sort_order
            """.trimIndent(), null
        ).use { c ->
            buildList { while (c.moveToNext()) add(SubjectStat(c.getInt(0), c.getString(1), c.getInt(2), c.getInt(3))) }
        }

        val daily = db.rawQuery(
            """
            SELECT date(attempted_at) d, COUNT(*) FROM user_attempts
            WHERE attempted_at >= datetime('now','-30 days')
            GROUP BY d ORDER BY d
            """.trimIndent(), null
        ).use { c ->
            buildList { while (c.moveToNext()) add(DailyStat(c.getString(0), c.getInt(1))) }
        }

        return StatsSummary(total, correct, subjectStats, daily, computeStreak(daily.map { it.date }))
    }

    private fun computeStreak(datesDesc: List<String>): Int {
        if (datesDesc.isEmpty()) return 0
        val set = datesDesc.toHashSet()
        // '오늘' 기준 연속일 — 날짜 문자열 비교용으로 오늘/어제를 DB에서 받음
        var streak = 0
        var offset = 0
        while (true) {
            val day = scalarString("SELECT date('now','-$offset days')") ?: break
            if (set.contains(day)) { streak++; offset++ } else break
        }
        return streak
    }

    // ---------- 데이터 관리 (F14~F16) ----------

    fun exportJson(): String {
        val root = JSONObject()
        root.put("version", DbProvider.ASSET_DB_VERSION)
        root.put("attempts", dumpArray("SELECT question_id, selected_no, is_correct, attempted_at, elapsed_ms FROM user_attempts",
            arrayOf("question_id", "selected_no", "is_correct", "attempted_at", "elapsed_ms")))
        root.put("favorites", dumpArray("SELECT question_id, added_at FROM favorites",
            arrayOf("question_id", "added_at")))
        root.put("memos", dumpArray("SELECT question_id, content, updated_at FROM memos",
            arrayOf("question_id", "content", "updated_at")))
        root.put("sessions", dumpArray("SELECT mode, started_at, finished_at, total, correct, elapsed_sec FROM study_sessions",
            arrayOf("mode", "started_at", "finished_at", "total", "correct", "elapsed_sec")))
        return root.toString(2)
    }

    fun importJson(json: String) {
        val root = JSONObject(json)
        db.beginTransaction()
        try {
            root.optJSONArray("attempts")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.execSQL(
                        "INSERT INTO user_attempts(user_id, question_id, selected_no, is_correct, attempted_at, elapsed_ms) VALUES(?,?,?,?,?,?)",
                        arrayOf<Any?>(DbProvider.LOCAL_USER, o.getInt("question_id"),
                            if (o.isNull("selected_no")) null else o.getInt("selected_no"),
                            o.getInt("is_correct"), o.getString("attempted_at"),
                            if (o.isNull("elapsed_ms")) null else o.getLong("elapsed_ms"))
                    )
                }
            }
            root.optJSONArray("favorites")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.execSQL("INSERT OR REPLACE INTO favorites(question_id, added_at) VALUES(?,?)",
                        arrayOf<Any?>(o.getInt("question_id"), o.getString("added_at")))
                }
            }
            root.optJSONArray("memos")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.execSQL("INSERT OR REPLACE INTO memos(question_id, content, updated_at) VALUES(?,?,?)",
                        arrayOf<Any?>(o.getInt("question_id"), o.getString("content"), o.getString("updated_at")))
                }
            }
            root.optJSONArray("sessions")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.execSQL("INSERT INTO study_sessions(mode, started_at, finished_at, total, correct, elapsed_sec) VALUES(?,?,?,?,?,?)",
                        arrayOf<Any?>(o.getString("mode"), o.getString("started_at"),
                            if (o.isNull("finished_at")) null else o.getString("finished_at"),
                            o.optInt("total"), o.optInt("correct"), o.optInt("elapsed_sec")))
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** F16 선택 초기화 */
    fun resetAttempts() = db.execSQL("DELETE FROM user_attempts")
    fun resetFavorites() = db.execSQL("DELETE FROM favorites")
    fun resetMemos() = db.execSQL("DELETE FROM memos")
    fun resetAll() {
        resetAttempts(); resetFavorites(); resetMemos()
        db.execSQL("DELETE FROM study_sessions")
    }

    // ---------- 내부 헬퍼 ----------

    private fun dumpArray(sql: String, cols: Array<String>): JSONArray {
        val arr = JSONArray()
        db.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) {
                val o = JSONObject()
                for (i in cols.indices) {
                    if (c.isNull(i)) o.put(cols[i], JSONObject.NULL)
                    else o.put(cols[i], c.getString(i))
                }
                arr.put(o)
            }
        }
        return arr
    }

    private fun scalarInt(sql: String): Int =
        db.rawQuery(sql, null).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    private fun scalarString(sql: String): String? =
        db.rawQuery(sql, null).use { if (it.moveToFirst()) it.getStringOrNull(0) else null }
}

private fun Cursor.toIntList(): List<Int> = buildList { while (moveToNext()) add(getInt(0)) }
private fun Cursor.getStringOrNull(i: Int): String? = if (isNull(i)) null else getString(i)
