package com.daim.safetyexam.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.data.Choice
import com.daim.safetyexam.data.QuestionFull
import com.daim.safetyexam.data.Repository
import com.daim.safetyexam.data.StudyMode
import com.daim.safetyexam.ui.MarkdownText
import com.daim.safetyexam.ui.QImage
import com.daim.safetyexam.ui.QuizSessionViewModel
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
    val repo = remember { Repository.get(context) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var showExitDialog by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }

    // 풀이 완료 시 결과 화면으로 (result 는 채점 후 비동기로 채워지므로 두 값 모두 키로 관찰)
    LaunchedEffect(vm.finished, vm.result) {
        if (vm.finished && vm.result != null) onFinished()
    }

    if (vm.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val q = vm.current
    if (q == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("해당 조건의 문항이 없습니다.")
        }
        return
    }

    val total = vm.questions.size
    val selected = vm.answers[q.id]
    val revealed = vm.revealed[q.id] == true

    // 즐겨찾기/메모 상태 — 문항 전환마다 로드
    var isFav by remember(q.id) { mutableStateOf(false) }
    var memo by remember(q.id) { mutableStateOf("") }
    LaunchedEffect(q.id) {
        val (f, m) = withContext(Dispatchers.IO) { repo.isFavorite(q.id) to (repo.getMemo(q.id) ?: "") }
        isFav = f; memo = m
    }

    Scaffold(
        topBar = {
            Column {
                androidx.compose.material3.TopAppBar(
                    title = {
                        Column {
                            Text(modeLabel(vm.mode), style = MaterialTheme.typography.titleMedium)
                            Text("${vm.currentIndex + 1} / $total · 응답 ${vm.answeredCount}",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    navigationIcon = {
                        TextButton(onClick = { showExitDialog = true }) { Text("나가기") }
                    },
                    actions = {
                        if (vm.timerEnabled) {
                            Text(
                                formatTime(vm.remainingSec),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (vm.remainingSec < 300) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        }
                    }
                )
                LinearProgressIndicator(
                    progress = { (vm.currentIndex + 1).toFloat() / total },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { vm.prev() },
                    enabled = vm.currentIndex > 0,
                    modifier = Modifier.weight(1f)
                ) { Text("이전") }

                if (vm.currentIndex < total - 1) {
                    Button(onClick = { vm.next() }, modifier = Modifier.weight(1f)) { Text("다음") }
                } else {
                    Button(
                        onClick = { showSubmitDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) { Text("제출/채점") }
                }
            }
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 메타
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubjectChip(q.subjectShort)
                Spacer(Modifier.height(0.dp))
                Text("  ${q.examTitle} ${q.qNumber}번", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                if (q.isDisputed) {
                    Text("이의제기 문항", color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(12.dp))

            // 본문
            Text(q.stem, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (q.stemImage != null) {
                Spacer(Modifier.height(12.dp))
                QImage(q.stemImage)
            }
            Spacer(Modifier.height(16.dp))

            // 보기
            q.choices.forEach { choice ->
                ChoiceRow(
                    choice = choice,
                    selected = selected == choice.no,
                    revealed = revealed,
                    isAnswer = q.answerNo == choice.no,
                    onClick = { if (!revealed) vm.select(choice.no) }
                )
                Spacer(Modifier.height(8.dp))
            }

            // 해설 (즉시채점 공개 시)
            if (revealed) {
                Spacer(Modifier.height(8.dp))
                ExplanationCard(
                    q = q,
                    isFav = isFav,
                    onToggleFav = {
                        scope.launch {
                            isFav = withContext(Dispatchers.IO) { repo.toggleFavorite(q.id) }
                        }
                    },
                    memo = memo,
                    onMemoChange = { memo = it },
                    onSaveMemo = {
                        scope.launch { withContext(Dispatchers.IO) { repo.saveMemo(q.id, memo) } }
                    }
                )
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("풀이를 중단할까요?") },
            text = { Text("지금까지의 응답은 저장되지 않습니다.") },
            confirmButton = { TextButton(onClick = { showExitDialog = false; onExit() }) { Text("나가기") } },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("계속 풀기") } }
        )
    }

    if (showSubmitDialog) {
        val unanswered = total - vm.answeredCount
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("제출하고 채점할까요?") },
            text = { Text(if (unanswered > 0) "미응답 $unanswered 문항은 오답 처리됩니다." else "모든 문항에 응답했습니다.") },
            confirmButton = { TextButton(onClick = { showSubmitDialog = false; vm.finish() }) { Text("제출") } },
            dismissButton = { TextButton(onClick = { showSubmitDialog = false }) { Text("취소") } }
        )
    }
}

@Composable
private fun SubjectChip(text: String) {
    Box(
        Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun ChoiceRow(
    choice: Choice,
    selected: Boolean,
    revealed: Boolean,
    isAnswer: Boolean,
    onClick: () -> Unit
) {
    val correct = MaterialTheme.colorScheme.primary
    val wrong = MaterialTheme.colorScheme.error
    val (bg, border) = when {
        revealed && isAnswer -> correct.copy(alpha = 0.15f) to correct
        revealed && selected && !isAnswer -> wrong.copy(alpha = 0.12f) to wrong
        selected -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.outlineVariant
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    "${choice.no}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        revealed && isAnswer -> correct
                        revealed && selected && !isAnswer -> wrong
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(Modifier.height(0.dp))
                Text("  ${choice.body}", style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f))
            }
            if (choice.imageAsset != null) {
                Spacer(Modifier.height(8.dp))
                QImage(choice.imageAsset)
            }
        }
    }
}

@Composable
private fun ExplanationCard(
    q: QuestionFull,
    isFav: Boolean,
    onToggleFav: () -> Unit,
    memo: String,
    onMemoChange: (String) -> Unit,
    onSaveMemo: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("정답 ${q.answerNo}번", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggleFav) {
                    Icon(
                        if (isFav) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "즐겨찾기",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (!q.explanation.isNullOrBlank()) {
                MarkdownText(q.explanation)
            } else {
                Text("등록된 해설이 없습니다.", style = MaterialTheme.typography.bodyMedium)
            }
            if (!q.referencesMd.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text("참고", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                MarkdownText(q.referencesMd)
            }
            Spacer(Modifier.height(16.dp))
            Text("메모", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = memo,
                onValueChange = onMemoChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("나만의 메모를 남겨보세요") },
                minLines = 2
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSaveMemo, modifier = Modifier.align(Alignment.End)) {
                Text("메모 저장")
            }
        }
    }
}

private fun modeLabel(mode: StudyMode) = when (mode) {
    StudyMode.EXAM -> "회차별 풀이"
    StudyMode.SUBJECT -> "과목별 풀이"
    StudyMode.MOCK -> "모의고사"
    StudyMode.WRONG -> "오답노트"
    StudyMode.FAVORITE -> "즐겨찾기"
}

private fun formatTime(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return "%d:%02d:%02d".format(h, m, s)
}
