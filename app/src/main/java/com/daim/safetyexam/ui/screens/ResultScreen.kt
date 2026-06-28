package com.daim.safetyexam.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.data.SubjectStat
import com.daim.safetyexam.ui.AppTopBar
import com.daim.safetyexam.ui.QuizSessionViewModel

@Composable
fun ResultScreen(vm: QuizSessionViewModel, onHome: () -> Unit) {
    val result = vm.result
    Scaffold(topBar = { AppTopBar("채점 결과") }) { pad ->
        if (result == null) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = onHome) { Text("홈으로") }
            }
            return@Scaffold
        }
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (result.passed) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${result.score100}점",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (result.passed) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        if (result.passed) "합격선 통과 (60점 이상)" else "합격선 미달 (60점)",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (result.passed) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "정답 ${result.correct} / ${result.total} · 소요 ${formatElapsed(result.elapsedSec)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (result.passed) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("과목별 점수", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            result.perSubject.forEach { s -> SubjectBar(s) }

            Spacer(Modifier.height(24.dp))
            Button(onClick = onHome, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("홈으로", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun SubjectBar(s: SubjectStat) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(s.shortName, style = MaterialTheme.typography.bodyMedium)
            Text("${s.correct}/${s.total} (${(s.accuracy * 100).toInt()}%)",
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        androidx.compose.material3.LinearProgressIndicator(
            progress = { s.accuracy },
            modifier = Modifier.fillMaxWidth().height(8.dp)
        )
    }
}

private fun formatElapsed(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return if (m > 0) "${m}분 ${s}초" else "${s}초"
}
