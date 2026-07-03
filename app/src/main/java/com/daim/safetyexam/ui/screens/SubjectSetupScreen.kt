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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.data.Repository
import com.daim.safetyexam.data.Subject
import com.daim.safetyexam.ui.AccentButton
import com.daim.safetyexam.ui.AppTopBar
import com.daim.safetyexam.ui.SectionLabel
import com.daim.safetyexam.ui.ScrollableContentColumn
import com.daim.safetyexam.ui.theme.appColors

@Composable
fun SubjectSetupScreen(
    onBack: () -> Unit,
    onStart: (subjectId: Int, count: Int, order: String) -> Unit
) {
    val context = LocalContext.current
    val c = MaterialTheme.appColors
    val subjects by produceState(initialValue = emptyList<Subject>()) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            Repository.get(context).subjects()
        }
    }

    var selectedSubject by remember { mutableIntStateOf(-1) }
    var count by remember { mutableIntStateOf(20) }
    var order by remember { mutableStateOf("random") }

    Scaffold(containerColor = c.bg, topBar = { AppTopBar("과목별 풀이", onBack = onBack) }) { pad ->
        ScrollableContentColumn(pad) {
            SectionLabel("과목 선택")
            Spacer(Modifier.height(8.dp))
            subjects.forEach { s ->
                val active = selectedSubject == s.id
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) c.navy else c.card)
                        .border(1.dp, if (active) c.navy else c.line, RoundedCornerShape(12.dp))
                        .clickable { selectedSubject = s.id }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(20.dp).clip(CircleShape)
                            .background(if (active) c.amber else c.chip),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${s.id}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold), color = if (active) c.navy else c.muted)
                    }
                    Spacer(Modifier.size(10.dp))
                    Text(s.nameKo, style = MaterialTheme.typography.bodyLarge, color = if (active) c.onNavy else c.ink)
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("출제 수")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(20, 50, 100, 300).forEach { n ->
                    SelectChip("$n", count == n, Modifier.weight(1f)) { count = n }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("정렬")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectChip("랜덤", order == "random", Modifier.weight(1f)) { order = "random" }
                SelectChip("연도순", order == "year", Modifier.weight(1f)) { order = "year" }
            }

            Spacer(Modifier.height(24.dp))
            AccentButton(
                "풀이 시작",
                Modifier.fillMaxWidth(),
                enabled = selectedSubject > 0
            ) { if (selectedSubject > 0) onStart(selectedSubject, count, order) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SelectChip(text: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = MaterialTheme.appColors
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) c.selFill else c.chip)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = if (active) c.onSelFill else c.accentFg)
    }
}
