package com.daim.safetyexam

import android.os.Bundle
import androidx.activity.ComponentActivity
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
        enableEdgeToEdge()
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
