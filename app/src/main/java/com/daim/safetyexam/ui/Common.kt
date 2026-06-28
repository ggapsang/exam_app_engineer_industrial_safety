package com.daim.safetyexam.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                }
            }
        },
        actions = { actions() }
    )
}

/** assets GIF/이미지 로드 — "file:///android_asset/..." */
@Composable
fun QImage(asset: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = asset,
        contentDescription = "문제 그림",
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
    )
}

/**
 * 매우 가벼운 마크다운 렌더러.
 * 지원: **굵게**, 줄바꿈, "* " 불릿. (해설/참고 표시용)
 */
@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        for (raw in text.split("\n")) {
            val line = raw.trimEnd()
            if (line.isBlank()) {
                Spacer(Modifier.width(0.dp).padding(top = 4.dp))
                continue
            }
            val isBullet = line.trimStart().startsWith("* ") || line.trimStart().startsWith("- ")
            if (isBullet) {
                val content = line.trimStart().removePrefix("* ").removePrefix("- ")
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                    Text("•  ", style = MaterialTheme.typography.bodyMedium)
                    Text(parseInline(content), style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Text(
                    parseInline(line),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

private fun parseInline(s: String) = buildAnnotatedString {
    var i = 0
    while (i < s.length) {
        val start = s.indexOf("**", i)
        if (start < 0) { append(s.substring(i)); break }
        append(s.substring(i, start))
        val end = s.indexOf("**", start + 2)
        if (end < 0) { append(s.substring(start)); break }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(s.substring(start + 2, end))
        }
        i = end + 2
    }
}
