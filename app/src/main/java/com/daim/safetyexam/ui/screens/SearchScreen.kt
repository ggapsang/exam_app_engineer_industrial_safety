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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.data.Repository
import com.daim.safetyexam.data.SearchHit
import com.daim.safetyexam.ui.AppTopBar
import com.daim.safetyexam.ui.SafetyChip
import com.daim.safetyexam.ui.theme.appColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun SearchScreen(onBack: () -> Unit, onOpen: (Int) -> Unit) {
    val context = LocalContext.current
    val c = MaterialTheme.appColors
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchHit>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.trim().length < 2) { results = emptyList(); searched = false; return@LaunchedEffect }
        delay(300)
        results = withContext(Dispatchers.IO) { Repository.get(context).search(query.trim()) }
        searched = true
    }

    Scaffold(containerColor = c.bg, topBar = { AppTopBar("검색", onBack = onBack) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(14.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.card)
                    .border(1.dp, c.line, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = c.muted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("문제·보기·해설에서 검색 (2글자 이상)", style = MaterialTheme.typography.bodyMedium, color = c.muted)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.ink),
                        cursorBrush = SolidColor(c.amber),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.size(10.dp))

            if (searched && results.isEmpty()) {
                EmptyState("검색 결과가 없습니다.")
            } else {
                if (results.isNotEmpty()) {
                    Text("${results.size}건", style = MaterialTheme.typography.labelMedium, color = c.muted)
                    Spacer(Modifier.size(6.dp))
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(results) { hit ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(c.card)
                                .border(1.dp, c.line, RoundedCornerShape(14.dp))
                                .clickable { onOpen(hit.questionId) }
                                .padding(14.dp)
                        ) {
                            Row {
                                SafetyChip(hit.subjectShort)
                                Spacer(Modifier.size(6.dp))
                                Text("${hit.examTitle} ${hit.qNumber}번", style = MaterialTheme.typography.labelMedium, color = c.muted)
                            }
                            Spacer(Modifier.size(6.dp))
                            Text(hit.preview + "…", style = MaterialTheme.typography.bodyMedium, color = c.ink, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}
