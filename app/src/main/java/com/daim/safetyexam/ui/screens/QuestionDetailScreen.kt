package com.daim.safetyexam.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.daim.safetyexam.data.QuestionFull
import com.daim.safetyexam.data.Repository
import com.daim.safetyexam.ui.AppTopBar
import com.daim.safetyexam.ui.MarkdownText
import com.daim.safetyexam.ui.QImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun QuestionDetailScreen(questionId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()

    var q by remember { mutableStateOf<QuestionFull?>(null) }
    var isFav by remember { mutableStateOf(false) }
    var memo by remember { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(questionId) {
        val loaded = withContext(Dispatchers.IO) {
            val list = repo.loadQuestions(listOf(questionId))
            val fav = repo.isFavorite(questionId)
            val m = repo.getMemo(questionId) ?: ""
            Triple(list.firstOrNull(), fav, m)
        }
        q = loaded.first; isFav = loaded.second; memo = loaded.third
    }

    Scaffold(
        topBar = {
            AppTopBar("문항 학습", onBack = onBack, actions = {
                IconButton(onClick = {
                    scope.launch { isFav = withContext(Dispatchers.IO) { repo.toggleFavorite(questionId) } }
                }) {
                    Icon(
                        if (isFav) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "즐겨찾기",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            })
        }
    ) { pad ->
        val question = q
        if (question == null) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Text("[${question.subjectShort}] ${question.examTitle} ${question.qNumber}번",
                style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Text(question.stem, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (question.stemImage != null) {
                Spacer(Modifier.height(12.dp))
                QImage(question.stemImage)
            }
            Spacer(Modifier.height(16.dp))

            question.choices.forEach { c ->
                val isAnswer = c.no == question.answerNo
                val border = if (isAnswer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .border(1.5.dp, border, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAnswer) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row {
                            Text("${c.no}", fontWeight = FontWeight.Bold,
                                color = if (isAnswer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium)
                            Text("  ${c.body}", style = MaterialTheme.typography.bodyLarge)
                        }
                        if (c.imageAsset != null) {
                            Spacer(Modifier.height(8.dp))
                            QImage(c.imageAsset)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("정답 ${question.answerNo}번", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (!question.explanation.isNullOrBlank()) MarkdownText(question.explanation)
                    else Text("등록된 해설이 없습니다.", style = MaterialTheme.typography.bodyMedium)
                    if (!question.referencesMd.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text("참고", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        MarkdownText(question.referencesMd)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("메모", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = memo, onValueChange = { memo = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("나만의 메모") }, minLines = 2
                    )
                    TextButton(
                        onClick = { scope.launch { withContext(Dispatchers.IO) { repo.saveMemo(questionId, memo) } } },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("메모 저장") }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
