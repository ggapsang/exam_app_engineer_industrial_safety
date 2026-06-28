package com.daim.safetyexam.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.data.Repository
import com.daim.safetyexam.data.StudyMode
import com.daim.safetyexam.ui.AccentButton
import com.daim.safetyexam.ui.ChoiceItem
import com.daim.safetyexam.ui.ChoiceState
import com.daim.safetyexam.ui.ExplanationCard
import com.daim.safetyexam.ui.FavoriteStar
import com.daim.safetyexam.ui.ProgressTrack
import com.daim.safetyexam.ui.QImage
import com.daim.safetyexam.ui.QuizSessionViewModel
import com.daim.safetyexam.ui.SafetyChip
import com.daim.safetyexam.ui.SecondaryButton
import com.daim.safetyexam.ui.theme.appColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun QuizScreen(
    vm: QuizSessionViewModel,
    onExit: () -> Unit,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val c = MaterialTheme.appColors
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()

    var showExitDialog by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(vm.finished, vm.result) {
        if (vm.finished && vm.result != null) onFinished()
    }

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
                    onBack = { showExitDialog = true },
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
                    if (vm.currentIndex < total - 1) {
                        AccentButton("다음", Modifier.weight(1f)) { vm.next() }
                    } else {
                        AccentButton("제출 · 채점", Modifier.weight(1f)) { showSubmitDialog = true }
                    }
                }
            }
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
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

            q.choices.forEach { choice ->
                val state = when {
                    revealed && choice.no == q.answerNo -> ChoiceState.CORRECT
                    revealed && choice.no == selected -> ChoiceState.WRONG
                    !revealed && choice.no == selected -> ChoiceState.SELECTED
                    else -> ChoiceState.DEFAULT
                }
                ChoiceItem(
                    no = choice.no,
                    body = choice.body,
                    state = state,
                    imageAsset = choice.imageAsset,
                    onClick = if (!revealed) ({ vm.select(choice.no) }) else null
                )
                Spacer(Modifier.height(8.dp))
            }

            if (revealed) {
                Spacer(Modifier.height(6.dp))
                ExplanationCard(
                    answerNo = q.answerNo,
                    explanation = q.explanation,
                    referencesMd = q.referencesMd,
                    isFavorite = isFav,
                    onToggleFavorite = { toggleFav() },
                    memo = memo,
                    onMemoChange = { memo = it },
                    onSaveMemo = { scope.launch { withContext(Dispatchers.IO) { repo.saveMemo(q.id, memo) } } }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showExitDialog) {
        AlertDialog(
            containerColor = c.card,
            onDismissRequest = { showExitDialog = false },
            title = { Text("풀이를 중단할까요?", color = c.ink) },
            text = { Text("지금까지의 응답은 저장되지 않습니다.", color = c.muted) },
            confirmButton = { TextButton(onClick = { showExitDialog = false; onExit() }) { Text("나가기", color = c.red) } },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("계속 풀기", color = c.navy) } }
        )
    }

    if (showSubmitDialog) {
        val unanswered = total - vm.answeredCount
        AlertDialog(
            containerColor = c.card,
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("제출하고 채점할까요?", color = c.ink) },
            text = { Text(if (unanswered > 0) "미응답 $unanswered 문항은 오답 처리됩니다." else "모든 문항에 응답했습니다.", color = c.muted) },
            confirmButton = { TextButton(onClick = { showSubmitDialog = false; vm.finish() }) { Text("제출", color = c.navy) } },
            dismissButton = { TextButton(onClick = { showSubmitDialog = false }) { Text("취소", color = c.muted) } }
        )
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
