package com.daim.safetyexam.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.data.Repository
import com.daim.safetyexam.data.StatsSummary
import com.daim.safetyexam.data.SubjectStat
import com.daim.safetyexam.ui.AppTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StatsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val stats by produceState<StatsSummary?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { Repository.get(context).stats() }
    }

    Scaffold(topBar = { AppTopBar("학습 통계", onBack = onBack) }) { pad ->
        val s = stats
        if (s == null) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("누적 풀이", "${s.totalAttempts}", Modifier.weight(1f))
                StatCard("정답률", "${(s.accuracy * 100).toInt()}%", Modifier.weight(1f))
                StatCard("연속 학습", "${s.streakDays}일", Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))
            Text("과목별 정답률", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            s.subjectStats.forEach { SubjectAccuracyBar(it) }

            Spacer(Modifier.height(20.dp))
            Text("약점 과목 Top 5", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            val weak = s.subjectStats.filter { it.total > 0 }.sortedBy { it.accuracy }.take(5)
            if (weak.isEmpty()) {
                Text("아직 풀이 기록이 없습니다.", style = MaterialTheme.typography.bodyMedium)
            } else {
                weak.forEachIndexed { i, st ->
                    Text("${i + 1}. ${st.shortName} — ${(st.accuracy * 100).toInt()}% (${st.correct}/${st.total})",
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("최근 30일 학습량", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            DailyChart(s)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SubjectAccuracyBar(s: SubjectStat) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(s.shortName, style = MaterialTheme.typography.bodyMedium)
            Text(if (s.total == 0) "미응시" else "${(s.accuracy * 100).toInt()}% (${s.correct}/${s.total})",
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(progress = { s.accuracy }, modifier = Modifier.fillMaxWidth().height(8.dp))
    }
}

@Composable
private fun DailyChart(s: StatsSummary) {
    val primary = MaterialTheme.colorScheme.primary
    val counts = s.daily.map { it.count }
    val maxV = (counts.maxOrNull() ?: 1).coerceAtLeast(1)
    if (counts.isEmpty()) {
        Text("최근 학습 기록이 없습니다.", style = MaterialTheme.typography.bodyMedium)
        return
    }
    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
        val n = counts.size
        val gap = if (n > 1) size.width / n else size.width
        val barW = gap * 0.6f
        counts.forEachIndexed { i, c ->
            val h = (c.toFloat() / maxV) * size.height
            drawRect(
                color = primary,
                topLeft = Offset(i * gap + (gap - barW) / 2, size.height - h),
                size = androidx.compose.ui.geometry.Size(barW, h)
            )
        }
    }
}
