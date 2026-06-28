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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.data.Repository
import com.daim.safetyexam.data.SearchHit
import com.daim.safetyexam.ui.AppTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun SearchScreen(onBack: () -> Unit, onOpen: (Int) -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchHit>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }

    // 디바운스 검색 (2글자 이상)
    LaunchedEffect(query) {
        if (query.trim().length < 2) {
            results = emptyList(); searched = false
            return@LaunchedEffect
        }
        delay(300)
        results = withContext(Dispatchers.IO) { Repository.get(context).search(query.trim()) }
        searched = true
    }

    Scaffold(topBar = { AppTopBar("검색", onBack = onBack) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("문제·보기·해설에서 검색 (2글자 이상)") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )
            Spacer(Modifier.padding(4.dp))

            if (searched && results.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("검색 결과가 없습니다.")
                }
            } else {
                if (results.isNotEmpty()) {
                    Text("${results.size}건", style = MaterialTheme.typography.labelMedium)
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(results) { hit ->
                        Card(onClick = { onOpen(hit.questionId) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Row {
                                    Text("[${hit.subjectShort}] ${hit.examTitle} ${hit.qNumber}번",
                                        style = MaterialTheme.typography.labelLarge)
                                }
                                Spacer(Modifier.padding(2.dp))
                                Text(hit.preview + "…", style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }
    }
}
