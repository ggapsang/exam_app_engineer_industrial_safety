package com.daim.safetyexam.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.data.Repository
import com.daim.safetyexam.data.Subject
import com.daim.safetyexam.ui.AppTopBar

@Composable
fun SubjectSetupScreen(
    onBack: () -> Unit,
    onStart: (subjectId: Int, count: Int, order: String) -> Unit
) {
    val context = LocalContext.current
    val subjects by produceState(initialValue = emptyList<Subject>()) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            Repository.get(context).subjects()
        }
    }

    var selectedSubject by remember { mutableIntStateOf(-1) }
    var count by remember { mutableIntStateOf(20) }
    var order by remember { mutableStateOf("random") }

    Scaffold(topBar = { AppTopBar("과목별 풀이", onBack = onBack) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(16.dp)
        ) {
            Text("과목 선택", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            subjects.forEach { s ->
                Card(
                    onClick = { selectedSubject = s.id },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = if (selectedSubject == s.id)
                        androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    else androidx.compose.material3.CardDefaults.cardColors()
                ) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedSubject == s.id, onClick = { selectedSubject = s.id })
                        Text(s.nameKo, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("출제 수", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(20, 50, 100, 300).forEach { n ->
                    FilterChip(selected = count == n, onClick = { count = n }, label = { Text("$n") })
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("정렬", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = order == "random", onClick = { order = "random" }, label = { Text("랜덤") })
                FilterChip(selected = order == "year", onClick = { order = "year" }, label = { Text("연도순") })
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { if (selectedSubject > 0) onStart(selectedSubject, count, order) },
                enabled = selectedSubject > 0,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("풀이 시작", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
