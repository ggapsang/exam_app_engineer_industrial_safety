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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.data.Repository
import com.daim.safetyexam.data.ResumeSnapshot
import com.daim.safetyexam.data.SettingsStore
import com.daim.safetyexam.ui.AppTopBar
import com.daim.safetyexam.ui.NavTab
import com.daim.safetyexam.ui.NavyIconButton
import com.daim.safetyexam.ui.TriadScaffold
import com.daim.safetyexam.ui.ScrollableContentColumn
import com.daim.safetyexam.ui.ContentMaxWidth
import com.daim.safetyexam.ui.rememberIsWide
import com.daim.safetyexam.ui.SectionLabel
import com.daim.safetyexam.ui.theme.appColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class ModeEntry(val title: String, val meta: String, val icon: ImageVector, val accentRed: Boolean, val onClick: () -> Unit)

@Composable
fun HomeScreen(
    settings: SettingsStore,
    onExam: () -> Unit,
    onSubject: () -> Unit,
    onMock: () -> Unit,
    onResume: () -> Unit,
    onWrong: () -> Unit,
    onFavorite: () -> Unit,
    onSearch: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val c = MaterialTheme.appColors

    // 홈이 다시 보일 때(ON_RESUME)마다 재조회 — 풀이 후 돌아오면 오답/즐겨찾기 수가 갱신됨
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshTick by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val counts by produceState(initialValue = 0 to 0, refreshTick) {
        value = withContext(Dispatchers.IO) {
            val repo = Repository.get(context)
            repo.wrongNotes(minWrong = 1, recentDays = null).size to repo.favoriteQuestionIds().size
        }
    }
    val (wrongCount, favCount) = counts
    val resume = ResumeSnapshot.fromJson(settings.resumeJson)

    val wide = rememberIsWide()
    TriadScaffold(
        current = NavTab.HOME,
        onHome = {}, onStats = onStats, onSettings = onSettings,
        topBar = {
            AppTopBar(
                title = "산업안전기사 기출",
                subtitle = "2017–2021 · 1,800문항",
                actions = {
                    NavyIconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "설정", tint = c.onNavy, modifier = Modifier.size(18.dp))
                    }
                }
            )
        }
    ) { pad ->
        ScrollableContentColumn(pad, maxWidth = if (wide) 920.dp else ContentMaxWidth) {
            // 빠른 시작 그라데이션 카드
            QuickStartCard(wrongCount, resume, onWrong, onResume, onMock)

            Spacer(Modifier.height(14.dp))
            SectionLabel("학습 모드")
            Spacer(Modifier.height(8.dp))

            val modes = listOf(
                ModeEntry("회차별 풀이", "15회 · 120문항", Icons.Filled.CalendarMonth, false, onExam),
                ModeEntry("과목별 풀이", "6과목 · 300문항", Icons.Filled.Category, false, onSubject),
                ModeEntry("모의고사", "2시간 30분", Icons.Filled.Schedule, false, onMock),
                ModeEntry("오답노트", "틀린 문항 $wrongCount", Icons.Filled.Close, true, onWrong),
                ModeEntry("즐겨찾기", "별표 $favCount", Icons.Filled.Star, false, onFavorite),
                ModeEntry("검색", "키워드 찾기", Icons.Filled.Search, false, onSearch),
            )
            // LazyVerticalGrid 대신 일반 Row 그리드로 배치 (verticalScroll 안에서 높이가
            // 콘텐츠에 맞게 늘어나도록 — 고정 높이 그리드는 마지막 줄이 잘림).
            // 넓은 화면(태블릿 가로)에서는 3열로 배치.
            val cols = if (wide) 3 else 2
            modes.chunked(cols).forEach { rowItems ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { m -> ModeCard(m, Modifier.weight(1f)) }
                    repeat(cols - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun QuickStartCard(
    wrongCount: Int,
    resume: ResumeSnapshot?,
    onWrong: () -> Unit,
    onResume: () -> Unit,
    onMock: () -> Unit
) {
    val c = MaterialTheme.appColors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Brush.linearGradient(listOf(c.navy, c.navy2)))
            .padding(14.dp)
    ) {
        Text("빠른 시작", style = MaterialTheme.typography.labelLarge, color = Color(0xFFC7D2E6))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickButton("$wrongCount", "오답노트", false, true, Modifier.weight(1f), onWrong)
            QuickButton(
                value = if (resume != null) "${resume.percent}%" else "—",
                label = "이어풀기",
                primary = false,
                enabled = resume != null,
                modifier = Modifier.weight(1f),
                onClick = onResume
            )
            QuickButton("120", "모의고사", true, true, Modifier.weight(1f), onMock)
        }
    }
}

@Composable
private fun QuickButton(value: String, label: String, primary: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = MaterialTheme.appColors
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (primary) c.amber else Color.White.copy(alpha = 0.10f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            color = if (primary) c.navy else if (enabled) c.amber else Color(0xFF6B7790)
        )
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (primary) c.navy else Color(0xFFC7D2E6))
    }
}

@Composable
private fun ModeCard(m: ModeEntry, modifier: Modifier = Modifier) {
    val c = MaterialTheme.appColors
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.card)
            .border(1.dp, c.line, RoundedCornerShape(14.dp))
            .clickable(onClick = m.onClick)
            .padding(12.dp)
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(c.chip),
            contentAlignment = Alignment.Center
        ) {
            Icon(m.icon, contentDescription = m.title, tint = if (m.accentRed) c.red else c.accentFg, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(m.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = c.ink)
        Spacer(Modifier.height(2.dp))
        Text(m.meta, style = MaterialTheme.typography.labelSmall, color = c.muted)
    }
}
