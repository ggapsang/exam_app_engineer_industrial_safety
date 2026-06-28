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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.data.QuestionFull
import com.daim.safetyexam.data.Repository
import com.daim.safetyexam.ui.AccentButton
import com.daim.safetyexam.ui.AppTopBar
import com.daim.safetyexam.ui.SafetyChip
import com.daim.safetyexam.ui.theme.appColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FavoriteScreen(
    onBack: () -> Unit,
    onOpen: (Int) -> Unit,
    onPlayAll: (List<Int>) -> Unit
) {
    val context = LocalContext.current
    val c = MaterialTheme.appColors
    val items by produceState(initialValue = emptyList<QuestionFull>()) {
        value = withContext(Dispatchers.IO) {
            val repo = Repository.get(context)
            repo.loadQuestions(repo.favoriteQuestionIds())
        }
    }

    Scaffold(containerColor = c.bg, topBar = { AppTopBar("즐겨찾기", onBack = onBack) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (items.isEmpty()) {
                EmptyState("즐겨찾기한 문항이 없습니다.\n해설 카드의 ★를 눌러 추가하세요.")
                return@Column
            }
            Box(Modifier.padding(14.dp)) {
                AccentButton("랜덤으로 풀기 (${items.size}문항)", Modifier.fillMaxWidth()) {
                    onPlayAll(items.map { it.id }.shuffled())
                }
            }
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { q ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.card)
                            .border(1.dp, c.line, RoundedCornerShape(14.dp))
                            .clickable { onOpen(q.id) }
                            .padding(14.dp)
                    ) {
                        Row {
                            SafetyChip(q.subjectShort)
                            Spacer(Modifier.size(6.dp))
                            Text("${q.examTitle} ${q.qNumber}번", style = MaterialTheme.typography.labelMedium, color = c.muted)
                        }
                        Spacer(Modifier.size(6.dp))
                        Text(q.stem.take(50) + "…", style = MaterialTheme.typography.bodyMedium, color = c.ink, maxLines = 2)
                    }
                }
            }
        }
    }
}
