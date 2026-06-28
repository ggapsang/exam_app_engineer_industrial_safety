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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.data.SessionResult
import com.daim.safetyexam.ui.AppTopBar
import com.daim.safetyexam.ui.NavyIconButton
import com.daim.safetyexam.ui.PassBar
import com.daim.safetyexam.ui.PrimaryButton
import com.daim.safetyexam.ui.QuizSessionViewModel
import com.daim.safetyexam.ui.SecondaryButton
import com.daim.safetyexam.ui.SectionLabel
import com.daim.safetyexam.ui.theme.appColors

@Composable
fun ResultScreen(vm: QuizSessionViewModel, onHome: () -> Unit, onReviewWrong: () -> Unit) {
    val c = MaterialTheme.appColors
    val result = vm.result

    Scaffold(
        containerColor = c.bg,
        topBar = {
            AppTopBar("채점 결과", actions = {
                NavyIconButton(onClick = onHome) {
                    Icon(Icons.Filled.Close, contentDescription = "닫기", tint = c.onNavy, modifier = Modifier.padding(2.dp))
                }
            })
        }
    ) { pad ->
        if (result == null) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                PrimaryButton("홈으로", onClick = onHome)
            }
            return@Scaffold
        }
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            // 점수 게이지 카드
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.card)
                    .border(1.dp, c.line, RoundedCornerShape(16.dp))
                    .padding(vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (result.passed) c.green else c.red)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(if (result.passed) "합격" else "불합격", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = c.onNavy)
                }
                Spacer(Modifier.height(8.dp))
                val unitSize = MaterialTheme.typography.titleLarge.fontSize
                val mutedColor = c.muted
                Text(
                    buildAnnotatedString {
                        append("${result.score100}")
                        withStyle(SpanStyle(fontSize = unitSize, color = mutedColor)) {
                            append(" / 100")
                        }
                    },
                    style = MaterialTheme.typography.displayLarge,
                    color = c.accentFg
                )
                Spacer(Modifier.height(4.dp))
                Text("합격선 60점 · 정답 ${result.correct} / ${result.total}", style = MaterialTheme.typography.labelMedium, color = c.muted)
                Text("소요 ${formatElapsed(result.elapsedSec)}", style = MaterialTheme.typography.labelSmall, color = c.muted)
            }

            Spacer(Modifier.height(12.dp))
            // 과목별 점수
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.card)
                    .border(1.dp, c.line, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                SectionLabel("과목별 점수")
                Spacer(Modifier.height(4.dp))
                result.perSubject.forEach { s ->
                    val pct = (s.accuracy * 100).toInt()
                    PassBar(
                        label = s.shortName,
                        value = pct,
                        fillColor = if (pct >= 60) c.green else if (pct >= 40) c.amber else c.red
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("검은 선 = 과목 합격선(60) · 막대 색 = 통과 여부", style = MaterialTheme.typography.labelSmall, color = c.muted)
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("홈으로", Modifier.weight(1f), onClick = onHome)
                PrimaryButton("오답 복습", Modifier.weight(1f), onClick = onReviewWrong)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun formatElapsed(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return when {
        h > 0 -> "${h}시간 ${m}분"
        m > 0 -> "${m}분 ${s}초"
        else -> "${s}초"
    }
}
