package com.daim.safetyexam.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class FontScale(val scale: Float, val label: String) {
    SMALL(0.9f, "작게"), NORMAL(1.0f, "보통"), LARGE(1.25f, "크게")
}

/** 회차/과목 풀이 중단 시 오답 저장 정책 */
enum class SessionExitSave { ASK, ALWAYS, NEVER }

/** 설정값. Compose 상태로 노출해 즉시 반영. */
class SettingsStore private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var theme by mutableStateOf(ThemeMode.valueOf(prefs.getString("theme", ThemeMode.SYSTEM.name)!!))
        private set
    var fontScale by mutableStateOf(FontScale.valueOf(prefs.getString("font", FontScale.NORMAL.name)!!))
        private set
    /** 일반(회차/과목) 풀이의 즉시 채점 */
    var instantGrading by mutableStateOf(prefs.getBoolean("instant_grading", true))
        private set
    /** 알림 시간 HH:mm, 빈값이면 미설정 */
    var notifyTime by mutableStateOf(prefs.getString("notify_time", "") ?: "")
        private set

    // 모의고사 시작 시트(§5.13)
    var mockSkipStart by mutableStateOf(prefs.getBoolean("mock_skip_start", false))
        private set
    var mockInstant by mutableStateOf(prefs.getBoolean("mock_instant", false))
        private set

    // 풀이 중단 시 오답 저장(§5.14)
    var sessionExitSave by mutableStateOf(
        SessionExitSave.valueOf(prefs.getString("session_exit_save", SessionExitSave.ASK.name)!!)
    )
        private set

    /** 이어풀기 스냅샷(JSON). 빈값이면 없음 */
    var resumeJson by mutableStateOf(prefs.getString("resume", "") ?: "")
        private set

    fun updateTheme(v: ThemeMode) { theme = v; prefs.edit().putString("theme", v.name).apply() }
    fun updateFont(v: FontScale) { fontScale = v; prefs.edit().putString("font", v.name).apply() }
    fun updateInstantGrading(v: Boolean) { instantGrading = v; prefs.edit().putBoolean("instant_grading", v).apply() }
    fun updateNotifyTime(v: String) { notifyTime = v; prefs.edit().putString("notify_time", v).apply() }

    fun updateMockSkipStart(v: Boolean) { mockSkipStart = v; prefs.edit().putBoolean("mock_skip_start", v).apply() }
    fun updateMockInstant(v: Boolean) { mockInstant = v; prefs.edit().putBoolean("mock_instant", v).apply() }
    fun updateSessionExitSave(v: SessionExitSave) { sessionExitSave = v; prefs.edit().putString("session_exit_save", v.name).apply() }

    fun saveResume(json: String) { resumeJson = json; prefs.edit().putString("resume", json).apply() }
    fun clearResume() { resumeJson = ""; prefs.edit().remove("resume").apply() }

    companion object {
        @Volatile private var instance: SettingsStore? = null
        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context.applicationContext).also { instance = it }
            }
    }
}
