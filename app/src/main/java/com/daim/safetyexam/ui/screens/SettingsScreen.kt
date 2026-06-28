package com.daim.safetyexam.ui.screens

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.data.FontScale
import com.daim.safetyexam.data.Reminder
import com.daim.safetyexam.data.MockWrongSave
import com.daim.safetyexam.data.Repository
import com.daim.safetyexam.data.SessionExitSave
import com.daim.safetyexam.data.SettingsStore
import com.daim.safetyexam.data.ThemeMode
import com.daim.safetyexam.ui.AppTopBar
import com.daim.safetyexam.ui.NavTab
import com.daim.safetyexam.ui.SafetyBottomBar
import com.daim.safetyexam.ui.SectionLabel
import com.daim.safetyexam.ui.theme.appColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(settings: SettingsStore, onHome: () -> Unit, onStats: () -> Unit) {
    val context = LocalContext.current
    val c = MaterialTheme.appColors
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            withContext(Dispatchers.IO) {
                val json = repo.exportJson()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }
            Toast.makeText(context, "백업을 저장했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (text != null) { repo.importJson(text); true } else false
                } catch (e: Exception) { false }
            }
            Toast.makeText(context, if (ok) "복원했습니다." else "복원 실패", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = c.bg,
        topBar = { AppTopBar("설정") },
        bottomBar = { SafetyBottomBar(NavTab.SETTINGS, onHome = onHome, onStats = onStats, onSettings = {}) }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)
        ) {
            SectionLabel("화면")
            Spacer(Modifier.height(6.dp))
            SettingCard {
                Text("테마", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = c.ink)
                Spacer(Modifier.height(8.dp))
                Segmented(
                    options = ThemeMode.values().map { themeLabel(it) },
                    selectedIndex = ThemeMode.values().indexOf(settings.theme),
                    onSelect = { settings.updateTheme(ThemeMode.values()[it]) }
                )
                Spacer(Modifier.height(14.dp))
                Text("글자 크기", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = c.ink)
                Spacer(Modifier.height(8.dp))
                Segmented(
                    options = FontScale.values().map { it.label },
                    selectedIndex = FontScale.values().indexOf(settings.fontScale),
                    onSelect = { settings.updateFont(FontScale.values()[it]) }
                )
            }

            Spacer(Modifier.height(14.dp))
            SectionLabel("풀이")
            Spacer(Modifier.height(6.dp))
            SettingCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("즉시 채점", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = c.ink)
                        Text("선택 즉시 정·오답·해설 표시 (회차/과목)", style = MaterialTheme.typography.labelSmall, color = c.muted)
                    }
                    Switch(
                        checked = settings.instantGrading,
                        onCheckedChange = { settings.updateInstantGrading(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = c.amber, checkedThumbColor = c.navy)
                    )
                }
                Divider()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("모의고사 시작 안내", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = c.ink)
                        Text("모의고사 진입 시 채점 방식을 매번 묻기", style = MaterialTheme.typography.labelSmall, color = c.muted)
                    }
                    Switch(
                        checked = !settings.mockSkipStart,
                        onCheckedChange = { settings.updateMockSkipStart(!it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = c.amber, checkedThumbColor = c.navy)
                    )
                }
                Divider()
                Text("중단 시 오답 저장", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = c.ink)
                Text("회차/과목 풀이를 중간에 나갈 때", style = MaterialTheme.typography.labelSmall, color = c.muted)
                Spacer(Modifier.height(8.dp))
                Segmented(
                    options = listOf("매번 묻기", "항상 저장", "저장 안 함"),
                    selectedIndex = when (settings.sessionExitSave) {
                        SessionExitSave.ASK -> 0
                        SessionExitSave.ALWAYS -> 1
                        SessionExitSave.NEVER -> 2
                    },
                    onSelect = {
                        settings.updateSessionExitSave(
                            when (it) {
                                0 -> SessionExitSave.ASK
                                1 -> SessionExitSave.ALWAYS
                                else -> SessionExitSave.NEVER
                            }
                        )
                    }
                )
                Divider()
                Text("모의고사 오답 저장", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = c.ink)
                Text("채점 후 틀린 문제를 오답노트에 저장", style = MaterialTheme.typography.labelSmall, color = c.muted)
                Spacer(Modifier.height(8.dp))
                Segmented(
                    options = listOf("매번 묻기", "항상 저장", "저장 안 함"),
                    selectedIndex = when (settings.mockWrongSave) {
                        MockWrongSave.ASK -> 0
                        MockWrongSave.ALWAYS -> 1
                        MockWrongSave.NEVER -> 2
                    },
                    onSelect = {
                        settings.updateMockWrongSave(
                            when (it) {
                                0 -> MockWrongSave.ASK
                                1 -> MockWrongSave.ALWAYS
                                else -> MockWrongSave.NEVER
                            }
                        )
                    }
                )
                if (settings.mockWrongSave == MockWrongSave.ALWAYS) {
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("미응시 문항도 함께 저장", style = MaterialTheme.typography.bodyMedium, color = c.ink, modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.mockWrongIncludeUnanswered,
                            onCheckedChange = { settings.updateMockWrongIncludeUnanswered(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = c.amber, checkedThumbColor = c.navy)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            SectionLabel("알림")
            Spacer(Modifier.height(6.dp))
            SettingCard {
                Text("학습 알림 시간", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = c.ink)
                Text(if (settings.notifyTime.isBlank()) "설정 안 함" else "매일 ${settings.notifyTime}", style = MaterialTheme.typography.bodyMedium, color = c.muted)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton("시간 설정") {
                        val now = java.util.Calendar.getInstance()
                        TimePickerDialog(context, { _, h, m ->
                            val time = "%02d:%02d".format(h, m)
                            settings.updateNotifyTime(time)
                            Reminder.schedule(context, time)
                            Toast.makeText(context, "매일 $time 알림 설정", Toast.LENGTH_SHORT).show()
                        }, now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE), true).show()
                    }
                    if (settings.notifyTime.isNotBlank()) {
                        PillButton("해제") { settings.updateNotifyTime(""); Reminder.cancel(context) }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            SectionLabel("데이터")
            Spacer(Modifier.height(6.dp))
            SettingCard {
                ActionRow("학습 기록 백업 (JSON)") { exportLauncher.launch("safetyexam_backup.json") }
                Divider()
                ActionRow("학습 기록 복원 (JSON)") { importLauncher.launch(arrayOf("application/json", "text/*")) }
                Divider()
                ActionRow("데이터 초기화", danger = true) { showResetDialog = true }
            }

            Spacer(Modifier.height(14.dp))
            SectionLabel("정보")
            Spacer(Modifier.height(6.dp))
            SettingCard {
                Text("산업안전기사 기출 v1.2.0", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = c.ink)
                Spacer(Modifier.height(4.dp))
                Text("문항 1,800 · 5개년(2017~2021) 15회차 · 완전 오프라인", style = MaterialTheme.typography.labelSmall, color = c.muted)
                Text("학습 데이터는 기기 내부에만 저장되며 외부로 전송되지 않습니다.", style = MaterialTheme.typography.labelSmall, color = c.muted)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showResetDialog) {
        ResetDialog(
            onDismiss = { showResetDialog = false },
            onReset = { which ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        when (which) {
                            0 -> repo.resetAttempts(); 1 -> repo.resetFavorites()
                            2 -> repo.resetMemos(); 3 -> repo.resetAll()
                        }
                    }
                    Toast.makeText(context, "초기화 완료", Toast.LENGTH_SHORT).show()
                }
                showResetDialog = false
            }
        )
    }
}

@Composable
private fun SettingCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val c = MaterialTheme.appColors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.card)
            .border(1.dp, c.line, RoundedCornerShape(16.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun Segmented(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val c = MaterialTheme.appColors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.chip).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEachIndexed { i, label ->
            val active = i == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) c.selFill else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = if (active) c.onSelFill else c.accentFg)
            }
        }
    }
}

@Composable
private fun PillButton(text: String, onClick: () -> Unit) {
    val c = MaterialTheme.appColors
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(c.chip)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = c.accentFg)
    }
}

@Composable
private fun ActionRow(label: String, danger: Boolean = false, onClick: () -> Unit) {
    val c = MaterialTheme.appColors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (danger) c.red else c.ink)
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.appColors.line))
}

@Composable
private fun ResetDialog(onDismiss: () -> Unit, onReset: (Int) -> Unit) {
    val c = MaterialTheme.appColors
    AlertDialog(
        containerColor = c.card,
        onDismissRequest = onDismiss,
        title = { Text("데이터 초기화", color = c.ink) },
        text = {
            Column {
                listOf("풀이 기록만", "즐겨찾기만", "메모만", "전부 초기화").forEachIndexed { i, label ->
                    TextButton(onClick = { onReset(i) }, modifier = Modifier.fillMaxWidth()) {
                        Text(label, color = if (i == 3) c.red else c.accentFg, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("닫기", color = c.muted) } }
    )
}

private fun themeLabel(t: ThemeMode) = when (t) {
    ThemeMode.SYSTEM -> "시스템"
    ThemeMode.LIGHT -> "라이트"
    ThemeMode.DARK -> "다크"
}
