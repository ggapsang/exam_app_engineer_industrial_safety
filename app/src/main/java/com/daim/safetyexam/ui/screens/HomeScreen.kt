package com.daim.safetyexam.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.data.Repository
import com.daim.safetyexam.ui.AppTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class MenuEntry(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onExam: () -> Unit,
    onSubject: () -> Unit,
    onMock: () -> Unit,
    onWrong: () -> Unit,
    onFavorite: () -> Unit,
    onSearch: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val wrongCount by produceState(initialValue = 0) {
        value = withContext(Dispatchers.IO) {
            Repository.get(context).wrongNotes(minWrong = 1, recentDays = null).size
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(title = "산업안전기사 기출", actions = {
                androidx.compose.material3.IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "설정")
                }
            })
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // 빠른 시작
            Card(
                onClick = onMock,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "실전 모의고사 시작",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        "6과목 × 20문항 · 2시간 30분 · 합격선 60점",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(onClick = onWrong, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.height(0.dp))
                        Text("  오답노트", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("$wrongCount 문항", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("학습 메뉴", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            val menu = listOf(
                MenuEntry("회차별 풀이", Icons.Filled.CalendarMonth, onExam),
                MenuEntry("과목별 풀이", Icons.Filled.Category, onSubject),
                MenuEntry("즐겨찾기", Icons.Filled.Star, onFavorite),
                MenuEntry("검색", Icons.Filled.Search, onSearch),
                MenuEntry("통계", Icons.Filled.BarChart, onStats),
                MenuEntry("설정", Icons.Filled.Settings, onSettings),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(menu) { m ->
                    Card(onClick = m.onClick, modifier = Modifier.fillMaxWidth().height(110.dp)) {
                        Column(
                            Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(m.icon, contentDescription = m.label, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            Text(m.label, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
