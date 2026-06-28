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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.data.Repository
import com.daim.safetyexam.data.WrongNoteItem
import com.daim.safetyexam.ui.AccentButton
import com.daim.safetyexam.ui.AppTopBar
import com.daim.safetyexam.ui.SafetyChip
import com.daim.safetyexam.ui.theme.appColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WrongNoteScreen(
    onBack: () -> Unit,
    onOpen: (Int) -> Unit,
    onPlayAll: (List<Int>) -> Unit
) {
    val context = LocalContext.current
    val c = MaterialTheme.appColors
    var filter by remember { mutableIntStateOf(0) }

    val items by produceState(initialValue = emptyList<WrongNoteItem>(), filter) {
        value = withContext(Dispatchers.IO) {
            val repo = Repository.get(context)
            when (filter) {
                1 -> repo.wrongNotes(minWrong = 2, recentDays = null)
                2 -> repo.wrongNotes(minWrong = 1, recentDays = 7)
                else -> repo.wrongNotes(minWrong = 1, recentDays = null)
            }
        }
    }

    Scaffold(containerColor = c.bg, topBar = { AppTopBar("오답노트", onBack = onBack) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterPill("1회 이상", filter == 0, Modifier.weight(1f)) { filter = 0 }
                FilterPill("2회 이상", filter == 1, Modifier.weight(1f)) { filter = 1 }
                FilterPill("최근 7일", filter == 2, Modifier.weight(1f)) { filter = 2 }
            }

            if (items.isEmpty()) {
                EmptyState("아직 틀린 문항이 없습니다.\n회차별 풀이로 시작해 보세요.")
                return@Column
            }

            Box(Modifier.padding(horizontal = 14.dp)) {
                AccentButton("전체 다시 풀기 (${items.size}문항)", Modifier.fillMaxWidth()) {
                    onPlayAll(items.map { it.questionId })
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.card)
                            .border(1.dp, c.line, RoundedCornerShape(14.dp))
                            .clickable { onOpen(item.questionId) }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (item.isFavorite) {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = c.amber, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.size(4.dp))
                            }
                            SafetyChip(item.subjectShort)
                            Spacer(Modifier.size(6.dp))
                            Text("${item.examTitle} ${item.qNumber}번", style = MaterialTheme.typography.labelMedium, color = c.muted)
                            Spacer(Modifier.weight(1f))
                            Text("${item.wrongCount}회 오답", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold), color = c.red)
                        }
                        Spacer(Modifier.size(6.dp))
                        Text(item.stemPreview + "…", style = MaterialTheme.typography.bodyMedium, color = c.ink, maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(text: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = MaterialTheme.appColors
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) c.selFill else c.chip)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = if (active) c.onSelFill else c.accentFg)
    }
}

@Composable
internal fun EmptyState(message: String) {
    val c = MaterialTheme.appColors
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = c.muted, textAlign = TextAlign.Center)
    }
}
