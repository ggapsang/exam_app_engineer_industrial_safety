package com.daim.safetyexam.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class FontScale(val scale: Float, val label: String) {
    SMALL(0.9f, "작게"), NORMAL(1.0f, "보통"), LARGE(1.2f, "크게")
}

/** 설정값. Compose 상태로 노출해 즉시 반영. */
class SettingsStore private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var theme by mutableStateOf(ThemeMode.valueOf(prefs.getString("theme", ThemeMode.SYSTEM.name)!!))
        private set
    var fontScale by mutableStateOf(FontScale.valueOf(prefs.getString("font", FontScale.NORMAL.name)!!))
        private set
    var instantGrading by mutableStateOf(prefs.getBoolean("instant_grading", true))
        private set
    /** 알림 시간 HH:mm, 빈값이면 미설정 */
    var notifyTime by mutableStateOf(prefs.getString("notify_time", "") ?: "")
        private set

    fun setTheme(v: ThemeMode) { theme = v; prefs.edit().putString("theme", v.name).apply() }
    fun setFont(v: FontScale) { fontScale = v; prefs.edit().putString("font", v.name).apply() }
    fun setInstantGrading(v: Boolean) { instantGrading = v; prefs.edit().putBoolean("instant_grading", v).apply() }
    fun setNotifyTime(v: String) { notifyTime = v; prefs.edit().putString("notify_time", v).apply() }

    companion object {
        @Volatile private var instance: SettingsStore? = null
        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context.applicationContext).also { instance = it }
            }
    }
}
