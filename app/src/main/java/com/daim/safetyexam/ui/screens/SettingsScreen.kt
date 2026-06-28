package com.daim.safetyexam.ui.screens

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.daim.safetyexam.data.FontScale
import com.daim.safetyexam.data.Reminder
import com.daim.safetyexam.data.Repository
import com.daim.safetyexam.data.SettingsStore
import com.daim.safetyexam.data.ThemeMode
import com.daim.safetyexam.ui.AppTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(onBack: () -> Unit, settings: SettingsStore) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) }

    // 백업: JSON 저장
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
    // 복원: JSON 읽기
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

    Scaffold(topBar = { AppTopBar("설정", onBack = onBack) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            SectionTitle("화면")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("테마", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.values().forEach { t ->
                            FilterChip(
                                selected = settings.theme == t,
                                onClick = { settings.setTheme(t) },
                                label = { Text(themeLabel(t)) }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("글자 크기", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FontScale.values().forEach { f ->
                            FilterChip(
                                selected = settings.fontScale == f,
                                onClick = { settings.setFont(f) },
                                label = { Text(f.label) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("풀이")
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("즉시 채점", fontWeight = FontWeight.Bold)
                        Text("선택 즉시 정·오답과 해설 표시 (모의고사 제외)",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = settings.instantGrading, onCheckedChange = { settings.setInstantGrading(it) })
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("알림")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("학습 알림 시간", fontWeight = FontWeight.Bold)
                    Text(if (settings.notifyTime.isBlank()) "설정 안 함" else "매일 ${settings.notifyTime}",
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val now = java.util.Calendar.getInstance()
                            TimePickerDialog(context, { _, h, m ->
                                val time = "%02d:%02d".format(h, m)
                                settings.setNotifyTime(time)
                                Reminder.schedule(context, time)
                                Toast.makeText(context, "매일 $time 알림 설정", Toast.LENGTH_SHORT).show()
                            }, now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE), true).show()
                        }) { Text("시간 설정") }
                        if (settings.notifyTime.isNotBlank()) {
                            OutlinedButton(onClick = {
                                settings.setNotifyTime("")
                                Reminder.cancel(context)
                            }) { Text("해제") }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("데이터")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SettingRow("학습 기록 백업 (JSON)") {
                        exportLauncher.launch("safetyexam_backup.json")
                    }
                    Divider()
                    SettingRow("학습 기록 복원 (JSON)") {
                        importLauncher.launch(arrayOf("application/json", "text/*"))
                    }
                    Divider()
                    SettingRow("데이터 초기화") { showResetDialog = true }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("정보")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("산업안전기사 기출 v1.0.0", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("문항 1,800 · 5개년(2017~2021) 15회차 · 완전 오프라인",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text("이미지 출처: kinz.kr", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text("본 앱의 학습 데이터는 기기 내부에만 저장되며 외부로 전송되지 않습니다.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showResetDialog) {
        ResetDialog(
            onDismiss = { showResetDialog = false },
            onReset = { which ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        when (which) {
                            0 -> repo.resetAttempts()
                            1 -> repo.resetFavorites()
                            2 -> repo.resetMemos()
                            3 -> repo.resetAll()
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
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun SettingRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ResetDialog(onDismiss: () -> Unit, onReset: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("데이터 초기화") },
        text = {
            Column {
                listOf("풀이 기록만", "즐겨찾기만", "메모만", "전부 초기화").forEachIndexed { i, label ->
                    TextButton(onClick = { onReset(i) }, modifier = Modifier.fillMaxWidth()) {
                        Text(label, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("닫기") } }
    )
}

private fun themeLabel(t: ThemeMode) = when (t) {
    ThemeMode.SYSTEM -> "시스템"
    ThemeMode.LIGHT -> "라이트"
    ThemeMode.DARK -> "다크"
}
