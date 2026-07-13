package com.daim.safetyexam.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 이어풀기 스냅샷 — 회차/과목 풀이를 중단했을 때 진행 위치/응답을 보관.
 * (모의고사/오답/즐겨찾기 모드는 이어풀기 대상이 아니다)
 */
data class ResumeSnapshot(
    val mode: StudyMode,
    val label: String,          // 예: "2021년 1회" / "기계위험방지"
    val ids: List<Int>,
    val answers: Map<Int, Int>, // questionId -> 선택 보기 번호
    val index: Int,
    val committedIds: Set<Int> = emptySet() // user_attempts에 이미 저장된 문항 ID
) {
    val total: Int get() = ids.size
    /** 진행률(%) — 응답 기준 */
    val percent: Int get() = if (ids.isEmpty()) 0 else (answers.size * 100 / ids.size)

    fun toJson(): String {
        val o = JSONObject()
        o.put("mode", mode.name)
        o.put("label", label)
        o.put("ids", JSONArray(ids))
        val ans = JSONObject()
        answers.forEach { (k, v) -> ans.put(k.toString(), v) }
        o.put("answers", ans)
        o.put("index", index)
        o.put("committed", JSONArray(committedIds.toList()))
        return o.toString()
    }

    companion object {
        fun fromJson(json: String): ResumeSnapshot? {
            if (json.isBlank()) return null
            return try {
                val o = JSONObject(json)
                val idsArr = o.getJSONArray("ids")
                val ids = ArrayList<Int>(idsArr.length())
                for (i in 0 until idsArr.length()) ids.add(idsArr.getInt(i))
                val ansObj = o.getJSONObject("answers")
                val answers = HashMap<Int, Int>()
                val keys = ansObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    answers[k.toInt()] = ansObj.getInt(k)
                }
                val committed = o.optJSONArray("committed")?.let { arr ->
                    (0 until arr.length()).mapTo(HashSet()) { arr.getInt(it) }
                } ?: emptySet<Int>()
                ResumeSnapshot(
                    mode = StudyMode.valueOf(o.getString("mode")),
                    label = o.getString("label"),
                    ids = ids,
                    answers = answers,
                    index = o.getInt("index"),
                    committedIds = committed
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
