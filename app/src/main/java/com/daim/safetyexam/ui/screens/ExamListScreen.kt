package com.daim.safetyexam.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.data.Exam
import com.daim.safetyexam.data.Repository
import com.daim.safetyexam.ui.AppTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ExamListScreen(onBack: () -> Unit, onStartExam: (Int) -> Unit) {
    val context = LocalContext.current
    val exams by produceState(initialValue = emptyList<Exam>()) {
        value = withContext(Dispatchers.IO) { Repository.get(context).exams() }
    }

    Scaffold(topBar = { AppTopBar("회차별 풀이", onBack = onBack) }) { pad ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.padding(pad).fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(exams) { exam ->
                Card(
                    onClick = { onStartExam(exam.id) },
                    modifier = Modifier.fillMaxWidth().height(90.dp)
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("${exam.year}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${exam.session}회", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
