package com.daim.safetyexam.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.data.Repository
import com.daim.safetyexam.data.WrongNoteItem
import com.daim.safetyexam.ui.AppTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WrongNoteScreen(
    onBack: () -> Unit,
    onOpen: (Int) -> Unit,
    onPlayAll: (List<Int>) -> Unit
) {
    val context = LocalContext.current
    // 0: 1회+, 1: 2회+, 2: 최근7일
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

    Scaffold(topBar = { AppTopBar("오답노트", onBack = onBack) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(filter == 0, { filter = 0 }, label = { Text("1회 이상") })
                FilterChip(filter == 1, { filter = 1 }, label = { Text("2회 이상") })
                FilterChip(filter == 2, { filter = 2 }, label = { Text("최근 7일") })
            }

            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("틀린 문항이 없습니다. 잘하고 있어요!", style = MaterialTheme.typography.bodyLarge)
                }
                return@Column
            }

            Button(
                onClick = { onPlayAll(items.map { it.questionId }) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) { Text("전체 다시 풀기 (${items.size}문항)") }

            Spacer(Modifier.padding(4.dp))
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    Card(onClick = { onOpen(item.questionId) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (item.isFavorite) {
                                    Icon(Icons.Filled.Star, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.padding(end = 4.dp))
                                }
                                Text("[${item.subjectShort}] ${item.examTitle} ${item.qNumber}번",
                                    style = MaterialTheme.typography.labelLarge)
                                Spacer(Modifier.weight(1f))
                                Text("${item.wrongCount}회 오답",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.padding(2.dp))
                            Text(item.stemPreview + "…", style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}
