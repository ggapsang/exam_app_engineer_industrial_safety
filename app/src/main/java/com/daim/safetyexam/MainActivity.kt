package com.daim.safetyexam

import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.daim.safetyexam.data.SettingsStore
import com.daim.safetyexam.ui.AppNav
import com.daim.safetyexam.ui.QuizSessionViewModel
import com.daim.safetyexam.ui.theme.SafetyExamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 앱바가 네이비이므로 상태바 아이콘은 항상 밝게(흰색) 유지
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT))
        // 태블릿(sw600dp+)만 자유 회전, 폰은 세로 고정 — 리소스 한정자 bool로 기기 분기
        requestedOrientation = if (resources.getBoolean(R.bool.allow_rotation))
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        else
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val settings = SettingsStore.get(this)
        setContent {
            SafetyExamTheme(themeMode = settings.theme, fontScale = settings.fontScale) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Activity 범위로 공유되는 풀이 세션 VM
                    val quizVm: QuizSessionViewModel = viewModel()
                    AppNav(quizVm = quizVm, settings = settings)
                }
            }
        }
    }
}
