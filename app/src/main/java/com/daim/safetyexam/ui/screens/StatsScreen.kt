package com.daim.safetyexam.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.data.Repository
import com.daim.safetyexam.data.StatsSummary
import com.daim.safetyexam.ui.AppTopBar
import com.daim.safetyexam.ui.NavTab
import com.daim.safetyexam.ui.PassBar
import com.daim.safetyexam.ui.SafetyBottomBar
import com.daim.safetyexam.ui.SectionLabel
import com.daim.safetyexam.ui.theme.appColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StatsScreen(onHome: () -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    val c = MaterialTheme.appColors
    val stats by produceState<StatsSummary?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { Repository.get(context).stats() }
    }

    Scaffold(
        containerColor = c.bg,
        topBar = { AppTopBar("학습 통계") },
        bottomBar = { SafetyBottomBar(NavTab.STATS, onHome = onHome, onStats = {}, onSettings = onSettings) }
    ) { pad ->
        val s = stats
        if (s == null) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.amber)
            }
            return@Scaffold
        }
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("${s.totalAttempts}", "누적 풀이", Modifier.weight(1f))
                StatBox("${(s.accuracy * 100).toInt()}%", "정답률", Modifier.weight(1f))
                StatBox("${s.streakDays}일", "연속 학습", Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))
            CardBox {
                SectionLabel("과목별 정답률")
                Spacer(Modifier.height(4.dp))
                s.subjectStats.forEach { st ->
                    val pct = (st.accuracy * 100).toInt()
                    PassBar(
                        label = st.shortName,
                        value = if (st.total == 0) 0 else pct,
                        fillColor = if (st.total == 0) c.muted else if (pct < 50) c.red else c.navy2,
                        showPassMark = false
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            CardBox {
                SectionLabel("약점 과목 Top 5")
                Spacer(Modifier.height(6.dp))
                val weak = s.subjectStats.filter { it.total > 0 }.sortedBy { it.accuracy }.take(5)
                if (weak.isEmpty()) {
                    Text("아직 풀이 기록이 없습니다.", style = MaterialTheme.typography.bodyMedium, color = c.muted)
                } else {
                    weak.forEachIndexed { i, st ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${i + 1}. ${st.shortName}", style = MaterialTheme.typography.bodyMedium, color = c.ink)
                            Text("${(st.accuracy * 100).toInt()}%", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold), color = c.red)
                        }
                        if (i < weak.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            CardBox {
                SectionLabel("최근 30일 학습량")
                Spacer(Modifier.height(8.dp))
                Sparkline(s)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CardBox(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val c = MaterialTheme.appColors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.card)
            .border(1.dp, c.line, RoundedCornerShape(16.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier = Modifier) {
    val c = MaterialTheme.appColors
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.card)
            .border(1.dp, c.line, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = c.navy)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.muted)
    }
}

@Composable
private fun Sparkline(s: StatsSummary) {
    val c = MaterialTheme.appColors
    val counts = s.daily.map { it.count }
    if (counts.isEmpty()) {
        Text("최근 학습 기록이 없습니다.", style = MaterialTheme.typography.bodyMedium, color = c.muted)
        return
    }
    val maxV = (counts.maxOrNull() ?: 1).coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth().height(60.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        counts.forEach { v ->
            val frac = (v.toFloat() / maxV).coerceIn(0.04f, 1f)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(frac)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(c.amber)
            )
        }
    }
}
