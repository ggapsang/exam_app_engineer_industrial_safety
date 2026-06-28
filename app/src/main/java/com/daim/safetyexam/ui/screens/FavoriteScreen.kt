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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.data.QuestionFull
import com.daim.safetyexam.data.Repository
import com.daim.safetyexam.ui.AppTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FavoriteScreen(
    onBack: () -> Unit,
    onOpen: (Int) -> Unit,
    onPlayAll: (List<Int>) -> Unit
) {
    val context = LocalContext.current
    val items by produceState(initialValue = emptyList<QuestionFull>()) {
        value = withContext(Dispatchers.IO) {
            val repo = Repository.get(context)
            repo.loadQuestions(repo.favoriteQuestionIds())
        }
    }

    Scaffold(topBar = { AppTopBar("즐겨찾기", onBack = onBack) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("즐겨찾기한 문항이 없습니다.", style = MaterialTheme.typography.bodyLarge)
                }
                return@Column
            }
            Button(
                onClick = { onPlayAll(items.map { it.id }.shuffled()) },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) { Text("랜덤으로 풀기 (${items.size}문항)") }

            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { q ->
                    Card(onClick = { onOpen(q.id) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Row {
                                Text("[${q.subjectShort}] ${q.examTitle} ${q.qNumber}번",
                                    style = MaterialTheme.typography.labelLarge)
                            }
                            Spacer(Modifier.padding(2.dp))
                            Text(q.stem.take(50) + "…", style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}
