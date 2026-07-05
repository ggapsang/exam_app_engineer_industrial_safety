package com.daim.safetyexam.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.data.QuestionFull
import com.daim.safetyexam.data.Repository
import com.daim.safetyexam.ui.AppTopBar
import com.daim.safetyexam.ui.ChoiceItem
import com.daim.safetyexam.ui.ChoiceState
import com.daim.safetyexam.ui.ExplanationCard
import com.daim.safetyexam.ui.NavyIconButton
import com.daim.safetyexam.ui.QImage
import com.daim.safetyexam.ui.SafetyChip
import com.daim.safetyexam.ui.ScrollableContentColumn
import com.daim.safetyexam.ui.rememberIsWide
import com.daim.safetyexam.ui.theme.appColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun QuestionDetailScreen(questionId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val c = MaterialTheme.appColors
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()

    var q by remember { mutableStateOf<QuestionFull?>(null) }
    var isFav by remember { mutableStateOf(false) }
    var memo by remember { mutableStateOf("") }

    LaunchedEffect(questionId) {
        val loaded = withContext(Dispatchers.IO) {
            Triple(
                repo.loadQuestions(listOf(questionId)).firstOrNull(),
                repo.isFavorite(questionId),
                repo.getMemo(questionId) ?: ""
            )
        }
        q = loaded.first; isFav = loaded.second; memo = loaded.third
    }

    Scaffold(
        containerColor = c.bg,
        topBar = {
            AppTopBar("문항 학습", onBack = onBack, actions = {
                NavyIconButton(onClick = {
                    scope.launch { isFav = withContext(Dispatchers.IO) { repo.toggleFavorite(questionId) } }
                }) {
                    Icon(
                        if (isFav) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "즐겨찾기",
                        tint = if (isFav) c.amber else c.onNavy
                    )
                }
            })
        }
    ) { pad ->
        val question = q
        if (question == null) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.amber)
            }
            return@Scaffold
        }
        val onToggleFav = { scope.launch { isFav = withContext(Dispatchers.IO) { repo.toggleFavorite(questionId) } }; Unit }
        val onSaveMemo = { scope.launch { withContext(Dispatchers.IO) { repo.saveMemo(questionId, memo) } }; Unit }
        if (rememberIsWide()) {
            // 넓은 화면(태블릿 가로): 좌=문제·보기 / 우=해설
            Row(Modifier.padding(pad).fillMaxSize()) {
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(14.dp)
                ) {
                    DetailQuestionContent(question)
                    Spacer(Modifier.height(24.dp))
                }
                Box(Modifier.fillMaxHeight().width(1.dp).background(c.line))
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(14.dp)
                ) {
                    ExplanationCard(
                        answerNo = question.answerNo,
                        explanation = question.explanation,
                        referencesMd = question.referencesMd,
                        isFavorite = isFav,
                        onToggleFavorite = onToggleFav,
                        memo = memo,
                        onMemoChange = { memo = it },
                        onSaveMemo = onSaveMemo
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        } else {
            ScrollableContentColumn(pad) {
                DetailQuestionContent(question)
                Spacer(Modifier.height(6.dp))
                ExplanationCard(
                    answerNo = question.answerNo,
                    explanation = question.explanation,
                    referencesMd = question.referencesMd,
                    isFavorite = isFav,
                    onToggleFavorite = onToggleFav,
                    memo = memo,
                    onMemoChange = { memo = it },
                    onSaveMemo = onSaveMemo
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** 문항 상세의 문제 본문 + 보기(정답 표시). 세로/가로 배치 공용. */
@Composable
private fun DetailQuestionContent(question: QuestionFull) {
    val c = MaterialTheme.appColors
    Row(verticalAlignment = Alignment.CenterVertically) {
        SafetyChip("${question.subjectId}과목 · ${question.subjectShort}")
        Spacer(Modifier.size(6.dp))
        Text("${question.examTitle} ${question.qNumber}번", style = MaterialTheme.typography.labelMedium, color = c.muted)
    }
    Spacer(Modifier.height(12.dp))
    if (question.stemImage != null) {
        QImage(question.stemImage)
        Spacer(Modifier.height(12.dp))
    }
    Text(question.stem, style = MaterialTheme.typography.bodyLarge, color = c.ink)
    Spacer(Modifier.height(14.dp))

    question.choices.forEach { ch ->
        ChoiceItem(
            no = ch.no,
            body = ch.body,
            state = if (ch.no == question.answerNo) ChoiceState.CORRECT else ChoiceState.DEFAULT,
            imageAsset = ch.imageAsset,
            note = ch.note
        )
        Spacer(Modifier.height(8.dp))
    }
}
