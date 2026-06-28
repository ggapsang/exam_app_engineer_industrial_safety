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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.data.MockWrongSave
import com.daim.safetyexam.data.SettingsStore
import com.daim.safetyexam.data.StudyMode
import com.daim.safetyexam.data.SubjectStat
import com.daim.safetyexam.ui.AppTopBar
import com.daim.safetyexam.ui.NavyIconButton
import com.daim.safetyexam.ui.PassBar
import com.daim.safetyexam.ui.PrimaryButton
import com.daim.safetyexam.ui.QuizSessionViewModel
import com.daim.safetyexam.ui.SafetyCheckbox
import com.daim.safetyexam.ui.SafetyToggle
import com.daim.safetyexam.ui.SecondaryButton
import com.daim.safetyexam.ui.SectionLabel
import com.daim.safetyexam.ui.SheetGrip
import com.daim.safetyexam.ui.theme.appColors

private data class SavedInfo(val wrong: Int, val unanswered: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    vm: QuizSessionViewModel,
    settings: SettingsStore,
    onHome: () -> Unit,
    onReviewWrongNote: () -> Unit,
    onSessionReview: (List<Int>) -> Unit,
    onRetryMock: () -> Unit
) {
    val c = MaterialTheme.appColors
    val result = vm.result

    val isMock = vm.mode == StudyMode.MOCK
    val total = result?.total ?: 0
    val correct = result?.correct ?: 0
    val unanswered = vm.unansweredCount
    val wrongAnswered = (total - correct - unanswered).coerceAtLeast(0)

    var savedInfo by remember { mutableStateOf<SavedInfo?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    // 모의고사: 정책에 따라 시트 표시 또는 자동 처리 (1회). 저장할 게 없으면 생략.
    LaunchedEffect(isMock, result != null) {
        if (isMock && result != null && (wrongAnswered + unanswered) > 0) {
            when (settings.mockWrongSave) {
                MockWrongSave.ASK -> showSheet = true
                MockWrongSave.ALWAYS -> {
                    val incl = settings.mockWrongIncludeUnanswered
                    vm.saveMockWrongToNote(incl)
                    savedInfo = SavedInfo(wrongAnswered, if (incl) unanswered else 0)
                }
                MockWrongSave.NEVER -> { /* 저장 안 함 */ }
            }
        }
    }

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
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)
        ) {
            // 점수 게이지 카드
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card)
                    .border(1.dp, c.line, RoundedCornerShape(16.dp)).padding(vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(if (result.passed) c.green else c.red)
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
                        withStyle(SpanStyle(fontSize = unitSize, color = mutedColor)) { append(" / 100") }
                    },
                    style = MaterialTheme.typography.displayLarge, color = c.accentFg
                )
                Spacer(Modifier.height(4.dp))
                Text("합격선 60점 · 정답 ${result.correct} / ${result.total}", style = MaterialTheme.typography.labelMedium, color = c.muted)
                Text("소요 ${formatElapsed(result.elapsedSec)}", style = MaterialTheme.typography.labelSmall, color = c.muted)
            }

            // 저장 확인 배너 (저장한 경우)
            savedInfo?.let { s ->
                Spacer(Modifier.height(12.dp))
                SavedBanner(s.wrong + s.unanswered, s.wrong, s.unanswered)
            }

            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card)
                    .border(1.dp, c.line, RoundedCornerShape(16.dp)).padding(14.dp)
            ) {
                SectionLabel("과목별 점수")
                Spacer(Modifier.height(4.dp))
                result.perSubject.forEach { st ->
                    val pct = (st.accuracy * 100).toInt()
                    PassBar(label = st.shortName, value = pct, fillColor = if (pct >= 60) c.green else if (pct >= 40) c.amber else c.red)
                }
                Spacer(Modifier.height(4.dp))
                Text("검은 선 = 과목 합격선(60) · 막대 색 = 통과 여부", style = MaterialTheme.typography.labelSmall, color = c.muted)
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isMock) {
                    SecondaryButton("다시 풀기", Modifier.weight(1f), onClick = onRetryMock)
                    if (savedInfo != null) {
                        PrimaryButton("오답노트에서 복습", Modifier.weight(1f), onClick = onReviewWrongNote)
                    } else {
                        PrimaryButton("이번 회차 오답 보기", Modifier.weight(1f)) {
                            onSessionReview(vm.sessionWrongIds(includeUnanswered = true))
                        }
                    }
                } else {
                    SecondaryButton("홈으로", Modifier.weight(1f), onClick = onHome)
                    PrimaryButton("오답 복습", Modifier.weight(1f), onClick = onReviewWrongNote)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showSheet && result != null) {
        MockWrongSaveSheet(
            wrong = wrongAnswered,
            unanswered = unanswered,
            correct = correct,
            defaultIncludeUnanswered = settings.mockWrongIncludeUnanswered,
            onDismiss = { showSheet = false },
            onSave = { include, remember ->
                if (remember) {
                    settings.updateMockWrongSave(MockWrongSave.ALWAYS)
                    settings.updateMockWrongIncludeUnanswered(include)
                }
                vm.saveMockWrongToNote(include)
                savedInfo = SavedInfo(wrongAnswered, if (include) unanswered else 0)
                showSheet = false
            },
            onSkip = { remember ->
                if (remember) settings.updateMockWrongSave(MockWrongSave.NEVER)
                showSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MockWrongSaveSheet(
    wrong: Int,
    unanswered: Int,
    correct: Int,
    defaultIncludeUnanswered: Boolean,
    onDismiss: () -> Unit,
    onSave: (include: Boolean, remember: Boolean) -> Unit,
    onSkip: (remember: Boolean) -> Unit
) {
    val c = MaterialTheme.appColors
    val sheetState = rememberModalBottomSheetState()
    var include by remember { mutableStateOf(defaultIncludeUnanswered) }
    var autoSave by remember { mutableStateOf(false) }
    val saveCount = wrong + if (include) unanswered else 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.card,
        dragHandle = { Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) { SheetGrip() } }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
            Text("틀린 문제를 오답노트에 저장할까요?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = c.ink)
            Spacer(Modifier.height(2.dp))
            Text("모의고사 채점 완료", style = MaterialTheme.typography.labelMedium, color = c.muted)

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatChip("$wrong", "오답", c.red, Modifier.weight(1f))
                StatChip("$unanswered", "미응시", c.muted, Modifier.weight(1f))
                StatChip("$correct", "정답", c.accentFg, Modifier.weight(1f))
            }

            if (unanswered > 0) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, c.line, RoundedCornerShape(14.dp)).padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("미응시 ${unanswered}문항도 함께 저장", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = c.ink)
                        Text("풀지 않은 문항도 복습 대상에 포함", style = MaterialTheme.typography.labelMedium, color = c.muted)
                    }
                    Spacer(Modifier.width(10.dp))
                    SafetyToggle(checked = include, onCheckedChange = { include = it })
                }
            }

            Spacer(Modifier.height(10.dp))
            SafetyCheckbox(checked = autoSave, onCheckedChange = { autoSave = it }, label = "다음 모의고사부터 자동 저장 (설정에서 변경)")

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("저장 안 함", Modifier.weight(1f)) { onSkip(autoSave) }
                PrimaryButton("${saveCount}문항 저장", Modifier.weight(1f)) { onSave(include, autoSave) }
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String, valueColor: Color, modifier: Modifier = Modifier) {
    val c = MaterialTheme.appColors
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(c.chip).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = valueColor)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.muted)
    }
}

@Composable
private fun SavedBanner(saved: Int, wrong: Int, unanswered: Int) {
    val c = MaterialTheme.appColors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(c.choiceCorrectBg)
            .border(1.dp, c.green.copy(alpha = 0.5f), RoundedCornerShape(13.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(c.green), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(9.dp))
        Column {
            Text("오답노트에 ${saved}문항 저장됨", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = c.green)
            Text("오답 $wrong · 미응시 $unanswered", style = MaterialTheme.typography.labelMedium, color = c.green)
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
