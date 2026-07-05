package com.daim.safetyexam.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * 임베드된 SQLite 본체를 내부 저장소로 복사해 읽기/쓰기로 연다.
 *
 * 설계 의도:
 *  - 컨텐츠(문항/해설/이미지매핑)는 assets 의 DB 그대로 사용 (스키마 무변경).
 *  - 사용자 데이터(favorites/memos/study_sessions/user_attempts)는 같은 DB에
 *    "추가 테이블"로 보관하되, 자산 DB가 새 버전으로 교체되면(F17) 사용자 데이터를
 *    별도 백업 DB로 옮겼다가 다시 복원한다.
 */
object DbProvider {

    private const val ASSET_DB = "industrial_safety_engineer.db"
    private const val INSTALLED_DB = "content.db"      // 내부 저장소에 설치되는 이름
    private const val USERDATA_DB = "userdata.db"      // 사용자 데이터 백업본(영속)
    private const val PREF = "db_pref"
    private const val KEY_ASSET_VER = "asset_db_version"

    /** assets DB를 교체(회차 추가/오답 정정)할 때마다 이 숫자를 올린다. */
    // v3: choices.note 컬럼 추가(보기별 해설 — 보기 셔플 대비). 기존 콘텐츠/사용자 데이터는 보존.
    // v4: 콘텐츠(오답/해설) 갱신 반영.
    // v5: 콘텐츠 갱신 반영.
    const val ASSET_DB_VERSION = 5

    /** 단일 로컬 사용자 식별자 */
    const val LOCAL_USER = "local"

    @Volatile private var instance: SQLiteDatabase? = null

    fun get(context: Context): SQLiteDatabase {
        instance?.let { return it }
        synchronized(this) {
            instance?.let { return it }
            val db = open(context.applicationContext)
            instance = db
            return db
        }
    }

    private fun open(context: Context): SQLiteDatabase {
        val dbFile = context.getDatabasePath(INSTALLED_DB)
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val installedVer = prefs.getInt(KEY_ASSET_VER, -1)

        if (!dbFile.exists()) {
            // 최초 설치
            copyAsset(context, dbFile)
            prefs.edit().putInt(KEY_ASSET_VER, ASSET_DB_VERSION).apply()
        } else if (installedVer != ASSET_DB_VERSION) {
            // 자산 DB 교체 → 사용자 데이터 보존 마이그레이션
            migrateKeepingUserData(context, dbFile)
            prefs.edit().putInt(KEY_ASSET_VER, ASSET_DB_VERSION).apply()
        }

        val db = SQLiteDatabase.openDatabase(
            dbFile.path, null, SQLiteDatabase.OPEN_READWRITE
        )
        createAppTables(db)
        return db
    }

    private fun copyAsset(context: Context, dbFile: File) {
        dbFile.parentFile?.mkdirs()
        context.assets.open(ASSET_DB).use { input ->
            dbFile.outputStream().use { output -> input.copyTo(output) }
        }
    }

    private fun createAppTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS favorites (
                question_id INTEGER PRIMARY KEY,
                added_at    TEXT DEFAULT (datetime('now'))
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS memos (
                question_id INTEGER PRIMARY KEY,
                content     TEXT NOT NULL,
                updated_at  TEXT DEFAULT (datetime('now'))
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS study_sessions (
                session_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                mode         TEXT NOT NULL,
                started_at   TEXT NOT NULL,
                finished_at  TEXT,
                total        INTEGER,
                correct      INTEGER,
                elapsed_sec  INTEGER
            )
            """.trimIndent()
        )
        // user_attempts 는 자산 DB에 이미 존재하나, 자산 변경 대비 IF NOT EXISTS 보강
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_attempts (
                attempt_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id        TEXT NOT NULL,
                question_id    INTEGER NOT NULL,
                selected_no    INTEGER,
                is_correct     INTEGER NOT NULL,
                attempted_at   TEXT NOT NULL DEFAULT (datetime('now')),
                elapsed_ms     INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_attempt_q ON user_attempts(question_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_attempt_at ON user_attempts(attempted_at)")
    }

    /**
     * F17: 자산 DB 교체 시 사용자 데이터를 보존한다.
     * 1) 기존 DB의 사용자 테이블을 userdata.db 로 덤프
     * 2) 새 자산 DB로 교체
     * 3) userdata.db 에서 사용자 데이터 복원
     */
    private fun migrateKeepingUserData(context: Context, dbFile: File) {
        val backup = context.getDatabasePath(USERDATA_DB)
        backup.parentFile?.mkdirs()

        // 1) 사용자 데이터를 백업 DB로 복사
        val old = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        createAppTables(old)
        SQLiteDatabase.openOrCreateDatabase(backup, null).use { bk ->
            createAppTables(bk)
            bk.execSQL("DELETE FROM favorites")
            bk.execSQL("DELETE FROM memos")
            bk.execSQL("DELETE FROM study_sessions")
            bk.execSQL("DELETE FROM user_attempts")
            bk.execSQL("ATTACH DATABASE '${dbFile.path}' AS src")
            bk.execSQL("INSERT INTO favorites SELECT * FROM src.favorites")
            bk.execSQL("INSERT INTO memos SELECT * FROM src.memos")
            bk.execSQL("INSERT INTO study_sessions SELECT * FROM src.study_sessions")
            bk.execSQL("INSERT INTO user_attempts SELECT * FROM src.user_attempts")
            bk.execSQL("DETACH DATABASE src")
        }
        old.close()

        // 2) 새 자산 DB로 교체
        dbFile.delete()
        copyAsset(context, dbFile)

        // 3) 복원
        val fresh = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        createAppTables(fresh)
        fresh.execSQL("ATTACH DATABASE '${backup.path}' AS bk")
        fresh.execSQL("INSERT OR REPLACE INTO favorites SELECT * FROM bk.favorites")
        fresh.execSQL("INSERT OR REPLACE INTO memos SELECT * FROM bk.memos")
        fresh.execSQL("INSERT INTO study_sessions SELECT * FROM bk.study_sessions")
        fresh.execSQL("INSERT INTO user_attempts SELECT * FROM bk.user_attempts")
        fresh.execSQL("DETACH DATABASE bk")
        fresh.close()
    }
}
