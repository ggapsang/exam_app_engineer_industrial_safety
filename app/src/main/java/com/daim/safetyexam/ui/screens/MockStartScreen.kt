package com.daim.safetyexam.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.data.SettingsStore
import com.daim.safetyexam.ui.AccentButton
import com.daim.safetyexam.ui.AppTopBar
import com.daim.safetyexam.ui.PrimaryButton
import com.daim.safetyexam.ui.SafetyCheckbox
import com.daim.safetyexam.ui.SafetyToggle
import com.daim.safetyexam.ui.SecondaryButton
import com.daim.safetyexam.ui.theme.appColors

@Composable
fun MockStartScreen(
    settings: SettingsStore,
    onCancel: () -> Unit,
    onStart: (instant: Boolean) -> Unit
) {
    val c = MaterialTheme.appColors
    var instant by remember { mutableStateOf(settings.mockInstant) }
    var skip by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = c.bg,
        topBar = { AppTopBar("모의고사 시작", onBack = onCancel) }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)
        ) {
            // 시험 요약 (네이비 그라데이션)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(c.navy, c.navy2)))
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem("120", "문항")
                SummaryItem("150", "분")
                SummaryItem("60", "합격점")
                SummaryItem("40", "과락")
            }

            Spacer(Modifier.height(14.dp))
            // 즉시 채점 토글
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card)
                    .border(1.dp, c.line, RoundedCornerShape(16.dp)).padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("즉시 채점", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = c.ink)
                        Text("문항마다 정답·해설을 바로 확인", style = MaterialTheme.typography.labelMedium, color = c.muted)
                    }
                    SafetyToggle(checked = instant, onCheckedChange = { instant = it })
                }
            }

            Spacer(Modifier.height(12.dp))
            // 안내 카드 (토글 상태에 따라 전환)
            GuidanceCard(instant)

            Spacer(Modifier.height(12.dp))
            SafetyCheckbox(
                checked = skip,
                onCheckedChange = { skip = it },
                label = "다음부터 이 안내 건너뛰기 (설정에서 변경)"
            )

            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("취소", Modifier.weight(1f), onClick = onCancel)
                val start: () -> Unit = {
                    settings.updateMockInstant(instant)
                    if (skip) settings.updateMockSkipStart(true)
                    onStart(instant)
                }
                if (instant) AccentButton("풀이 시작", Modifier.weight(1f), onClick = start)
                else PrimaryButton("풀이 시작", Modifier.weight(1f), onClick = start)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SummaryItem(value: String, label: String) {
    val c = MaterialTheme.appColors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = c.amber)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFFC7D2E6))
    }
}

@Composable
private fun GuidanceCard(instant: Boolean) {
    val c = MaterialTheme.appColors
    val tone = if (instant) c.amber else c.green
    val badge = if (instant) "학습용" else "권장"
    val title = if (instant) "즉시 채점 (학습용)" else "일괄 채점 (실전형)"
    val desc = if (instant)
        "문항마다 정답·해설이 즉시 표시됩니다. 실제 시험 환경과는 다릅니다."
    else
        "모든 문항을 푼 뒤 한 번에 채점합니다. 합격선·과락·시간관리까지 실전처럼 연습할 수 있습니다."
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(tone.copy(alpha = 0.12f))
            .border(1.dp, tone.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.clip(RoundedCornerShape(20.dp)).background(tone).padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(badge, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold), color = if (instant) c.navy else c.onNavy)
            }
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = c.ink)
        }
        Spacer(Modifier.height(8.dp))
        Text(desc, style = MaterialTheme.typography.bodyMedium, color = c.ink)
    }
}
