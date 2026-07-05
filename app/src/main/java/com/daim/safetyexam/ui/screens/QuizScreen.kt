package com.daim.safetyexam.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.data.QuestionFull
import com.daim.safetyexam.data.Repository
import com.daim.safetyexam.data.SessionExitSave
import com.daim.safetyexam.data.SettingsStore
import com.daim.safetyexam.data.StudyMode
import com.daim.safetyexam.ui.AccentButton
import com.daim.safetyexam.ui.ChoiceItem
import com.daim.safetyexam.ui.ChoiceState
import com.daim.safetyexam.ui.ExplanationCard
import com.daim.safetyexam.ui.PrimaryButton
import com.daim.safetyexam.ui.ProgressTrack
import com.daim.safetyexam.ui.QImage
import com.daim.safetyexam.ui.QuizSessionViewModel
import com.daim.safetyexam.ui.SafetyCheckbox
import com.daim.safetyexam.ui.SafetyChip
import com.daim.safetyexam.ui.SafetyToggle
import com.daim.safetyexam.ui.ScrollableContentColumn
import com.daim.safetyexam.ui.rememberIsWide
import com.daim.safetyexam.ui.SecondaryButton
import com.daim.safetyexam.ui.SheetGrip
import com.daim.safetyexam.ui.theme.appColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    vm: QuizSessionViewModel,
    onExit: () -> Unit,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val c = MaterialTheme.appColors
    val settings = remember { SettingsStore.get(context) }
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()

    var showExitDialog by remember { mutableStateOf(false) }   // 모의고사/오답/즐겨찾기 단순 중단
    var showExitSheet by remember { mutableStateOf(false) }    // 회차/과목 중단 시 오답 저장 시트
    var showSubmitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(vm.finished, vm.result) {
        if (vm.finished && vm.result != null) onFinished()
    }

    // 중단 처리: 회차/과목은 정책(ASK/ALWAYS/NEVER)에 따라, 그 외는 단순 확인
    fun handleExit() {
        if (vm.isResumable) {
            when (settings.sessionExitSave) {
                SessionExitSave.ASK -> showExitSheet = true
                SessionExitSave.ALWAYS -> { vm.exitWithSave(true); onExit() }
                SessionExitSave.NEVER -> { vm.exitWithSave(false); onExit() }
            }
        } else {
            showExitDialog = true
        }
    }

    BackHandler(enabled = !vm.finished) { handleExit() }

    if (vm.loading) {
        Box(Modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = c.amber)
        }
        return
    }
    val q = vm.current
    if (q == null) {
        Box(Modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.Center) {
            Text("해당 조건의 문항이 없습니다.", color = c.muted)
        }
        return
    }

    val total = vm.questions.size
    val selected = vm.answers[q.id]
    val revealed = vm.revealed[q.id] == true

    var isFav by remember(q.id) { mutableStateOf(false) }
    var memo by remember(q.id) { mutableStateOf("") }
    LaunchedEffect(q.id) {
        val (f, m) = withContext(Dispatchers.IO) { repo.isFavorite(q.id) to (repo.getMemo(q.id) ?: "") }
        isFav = f; memo = m
    }

    fun toggleFav() = scope.launch { isFav = withContext(Dispatchers.IO) { repo.toggleFavorite(q.id) } }

    Scaffold(
        containerColor = c.bg,
        topBar = {
            Column {
                com.daim.safetyexam.ui.QuizTopBar(
                    examMeta = "${q.examTitle} · ${q.qNumber}번",
                    progressText = "${vm.currentIndex + 1} / $total",
                    timer = if (vm.timerEnabled) formatTime(vm.remainingSec) else null,
                    timerWarn = vm.remainingSec < 600,
                    isFav = isFav,
                    onBack = { handleExit() },
                    onToggleFav = { toggleFav() }
                )
                Box(Modifier.background(c.bg).padding(horizontal = 14.dp, vertical = 8.dp)) {
                    ProgressTrack((vm.currentIndex + 1).toFloat() / total)
                }
            }
        },
        bottomBar = {
            Surface(color = c.card) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryButton("이전", Modifier.weight(1f), enabled = vm.currentIndex > 0) { vm.prev() }
                    when {
                        // 즉시 채점: 정답 공개 전에는 '정답 확인'을 먼저 보여 준다(오선택 정정 허용)
                        vm.instantGrading && !revealed ->
                            AccentButton("정답 확인", Modifier.weight(1f), enabled = selected != null) { vm.reveal() }
                        vm.currentIndex < total - 1 ->
                            AccentButton("다음", Modifier.weight(1f)) { vm.next() }
                        else ->
                            AccentButton("제출 · 채점", Modifier.weight(1f)) { showSubmitDialog = true }
                    }
                }
            }
        }
    ) { pad ->
        val onSaveMemo = { scope.launch { withContext(Dispatchers.IO) { repo.saveMemo(q.id, memo) } }; Unit }
        if (rememberIsWide()) {
            // 넓은 화면(태블릿 가로): 좌=문제·보기 / 우=해설 (각각 독립 스크롤)
            Row(Modifier.padding(pad).fillMaxSize()) {
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(14.dp)
                ) {
                    QuestionContent(q, selected, revealed) { vm.select(it) }
                    Spacer(Modifier.height(24.dp))
                }
                Box(Modifier.fillMaxHeight().width(1.dp).background(c.line))
                if (revealed) {
                    Column(
                        Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(14.dp)
                    ) {
                        ExplanationContent(q, isFav, memo, { toggleFav() }, { memo = it }, onSaveMemo)
                        Spacer(Modifier.height(24.dp))
                    }
                } else {
                    Column(
                        Modifier.weight(1f).fillMaxHeight().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("정답 확인 후 이곳에 해설이 표시됩니다.", style = MaterialTheme.typography.bodyMedium, color = c.muted)
                    }
                }
            }
        } else {
            ScrollableContentColumn(pad) {
                QuestionContent(q, selected, revealed) { vm.select(it) }
                if (revealed) {
                    Spacer(Modifier.height(6.dp))
                    ExplanationContent(q, isFav, memo, { toggleFav() }, { memo = it }, onSaveMemo)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            containerColor = c.card,
            onDismissRequest = { showExitDialog = false },
            title = { Text("풀이를 중단할까요?", color = c.ink) },
            text = { Text("지금까지의 응답은 저장되지 않습니다.", color = c.muted) },
            confirmButton = { TextButton(onClick = { showExitDialog = false; vm.abandonSession(); onExit() }) { Text("나가기", color = c.red) } },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("계속 풀기", color = c.accentFg) } }
        )
    }

    if (showExitSheet) {
        ExitSaveSheet(
            answered = vm.answeredCount,
            wrong = vm.answeredWrong(),
            metaLabel = "${vm.sessionLabel} · ${vm.answeredCount}/$total 풀이 중",
            onContinue = { showExitSheet = false },
            onExit = { save, dontAskAgain ->
                if (dontAskAgain) settings.updateSessionExitSave(if (save) SessionExitSave.ALWAYS else SessionExitSave.NEVER)
                vm.exitWithSave(save)
                showExitSheet = false
                onExit()
            }
        )
    }

    if (showSubmitDialog) {
        val unanswered = total - vm.answeredCount
        AlertDialog(
            containerColor = c.card,
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("제출하고 채점할까요?", color = c.ink) },
            text = { Text(if (unanswered > 0) "미응답 $unanswered 문항은 오답 처리됩니다." else "모든 문항에 응답했습니다.", color = c.muted) },
            confirmButton = { TextButton(onClick = { showSubmitDialog = false; vm.finish() }) { Text("제출", color = c.accentFg) } },
            dismissButton = { TextButton(onClick = { showSubmitDialog = false }) { Text("취소", color = c.muted) } }
        )
    }
}

/** 문제 본문 + 보기 목록 (세로 1단·가로 2단 좌측에서 공용). 채점 로직은 원본 choice.no 기준 유지. */
@Composable
private fun QuestionContent(
    q: QuestionFull,
    selected: Int?,
    revealed: Boolean,
    onSelect: (Int) -> Unit
) {
    val c = MaterialTheme.appColors
    Row(verticalAlignment = Alignment.CenterVertically) {
        SafetyChip("${q.subjectId}과목 · ${q.subjectShort}")
        Spacer(Modifier.weight(1f))
        if (q.isDisputed) {
            Text("이의제기 문항", color = c.red, style = MaterialTheme.typography.labelMedium)
        }
    }
    Spacer(Modifier.height(12.dp))

    if (q.stemImage != null) {
        QImage(q.stemImage)
        Spacer(Modifier.height(12.dp))
    }
    Text(q.stem, style = MaterialTheme.typography.bodyLarge, color = c.ink)
    Spacer(Modifier.height(14.dp))

    q.choices.forEachIndexed { index, choice ->
        val state = when {
            revealed && choice.no == q.answerNo -> ChoiceState.CORRECT
            revealed && choice.no == selected -> ChoiceState.WRONG
            !revealed && choice.no == selected -> ChoiceState.SELECTED
            else -> ChoiceState.DEFAULT
        }
        ChoiceItem(
            no = index + 1,
            body = choice.body,
            state = state,
            imageAsset = choice.imageAsset,
            note = if (revealed) choice.note else null,
            onClick = if (!revealed) ({ onSelect(choice.no) }) else null
        )
        Spacer(Modifier.height(8.dp))
    }
}

/** 해설 카드 (정답 표시번호 = 셔플된 목록에서의 위치). */
@Composable
private fun ExplanationContent(
    q: QuestionFull,
    isFav: Boolean,
    memo: String,
    onToggleFavorite: () -> Unit,
    onMemoChange: (String) -> Unit,
    onSaveMemo: () -> Unit
) {
    val displayAnswerNo = q.choices.indexOfFirst { it.no == q.answerNo } + 1
    ExplanationCard(
        answerNo = displayAnswerNo,
        explanation = q.explanation,
        referencesMd = q.referencesMd,
        isFavorite = isFav,
        onToggleFavorite = onToggleFavorite,
        memo = memo,
        onMemoChange = onMemoChange,
        onSaveMemo = onSaveMemo
    )
}

/** §5.14 회차/과목 풀이 중단 시트 — 오답 저장 토글 + 이어풀기 안내 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExitSaveSheet(
    answered: Int,
    wrong: Int,
    metaLabel: String,
    onContinue: () -> Unit,
    onExit: (save: Boolean, dontAskAgain: Boolean) -> Unit
) {
    val c = MaterialTheme.appColors
    val sheetState = rememberModalBottomSheetState()
    var save by remember { mutableStateOf(true) }
    var dontAsk by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onContinue,
        sheetState = sheetState,
        containerColor = c.card,
        dragHandle = { Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) { SheetGrip() } }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
            Text("풀이를 중단할까요?", style = MaterialTheme.typography.titleLarge.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize), color = c.ink)
            Spacer(Modifier.height(2.dp))
            Text(metaLabel, style = MaterialTheme.typography.labelMedium, color = c.muted)

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryChip("$answered", "푼 문항", c.accentFg, Modifier.weight(1f))
                SummaryChip("$wrong", "오답", c.red, Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, c.line, RoundedCornerShape(14.dp)).padding(13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(if (save) "오답 ${wrong}개를 오답노트에 저장" else "오답을 저장하지 않고 나가기",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = c.ink)
                    Text(if (save) "끄면 이번 풀이의 오답은 저장되지 않습니다" else "이번 풀이의 오답 ${wrong}개가 오답노트에 추가되지 않습니다",
                        style = MaterialTheme.typography.labelMedium, color = c.muted)
                }
                Spacer(Modifier.width(10.dp))
                SafetyToggle(checked = save, onCheckedChange = { save = it })
            }

            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(if (c.isDark) c.chip else Color(0xFFEEF3FB)).padding(11.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = c.accentFg, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text("진행 위치는 자동 저장되어 이어풀기로 다시 시작할 수 있습니다.", style = MaterialTheme.typography.labelMedium, color = c.accentFg)
            }

            Spacer(Modifier.height(10.dp))
            SafetyCheckbox(checked = dontAsk, onCheckedChange = { dontAsk = it }, label = "다음부터 묻지 않기 (설정에서 변경)")

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("계속 풀기", Modifier.weight(1f)) { onContinue() }
                if (save) {
                    PrimaryButton("나가기", Modifier.weight(1f)) { onExit(true, dontAsk) }
                } else {
                    WarnButton("저장 없이 나가기", Modifier.weight(1f)) { onExit(false, dontAsk) }
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(value: String, label: String, valueColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
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
private fun WarnButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = MaterialTheme.appColors
    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(c.choiceWrongBg).clickable(onClick = onClick).padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = c.red)
    }
}

private fun formatTime(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return "%d:%02d:%02d".format(h, m, s)
}

internal fun modeLabel(mode: StudyMode) = when (mode) {
    StudyMode.EXAM -> "회차별 풀이"
    StudyMode.SUBJECT -> "과목별 풀이"
    StudyMode.MOCK -> "모의고사"
    StudyMode.WRONG -> "오답노트"
    StudyMode.FAVORITE -> "즐겨찾기"
}
