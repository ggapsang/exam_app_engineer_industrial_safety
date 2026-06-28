package com.daim.safetyexam.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daim.safetyexam.ui.theme.appColors

private val circled = listOf("①", "②", "③", "④")

/** 해설 카드 (F6) — 앰버 톤, 정답 강조 + 즐겨찾기 + 메모 */
@Composable
fun ExplanationCard(
    answerNo: Int,
    explanation: String?,
    referencesMd: String?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    memo: String,
    onMemoChange: (String) -> Unit,
    onSaveMemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = MaterialTheme.appColors
    val mark = circled.getOrElse(answerNo - 1) { "$answerNo" }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.expBg)
            .border(1.dp, c.expBorder, RoundedCornerShape(13.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = c.amberDark, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text("정답 $mark 해설", style = MaterialTheme.typography.labelLarge, color = c.amberDark)
            Spacer(Modifier.weight(1f))
            FavoriteStar(isFavorite, onToggleFavorite)
        }
        Spacer(Modifier.height(8.dp))
        if (!explanation.isNullOrBlank()) {
            MarkdownText(explanation, color = c.expText)
        } else {
            Text("등록된 해설이 없습니다.", style = MaterialTheme.typography.bodyMedium, color = c.expText)
        }
        if (!referencesMd.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.expRefBorder))
            Spacer(Modifier.height(7.dp))
            MarkdownText(referencesMd, color = c.expRefText)
        }
        Spacer(Modifier.height(12.dp))
        Text("메모", style = MaterialTheme.typography.labelLarge, color = c.amberDark)
        Spacer(Modifier.height(6.dp))
        MemoField(value = memo, onValueChange = onMemoChange, onSave = onSaveMemo)
    }
}

@Composable
fun FavoriteStar(isFavorite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.appColors
    Box(
        modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
            contentDescription = "즐겨찾기",
            tint = if (isFavorite) c.amber else c.muted,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun MemoField(value: String, onValueChange: (String) -> Unit, onSave: () -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.appColors
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(11.dp))
                .background(c.card)
                .border(1.dp, c.line, RoundedCornerShape(11.dp))
                .padding(10.dp)
        ) {
            if (value.isEmpty()) {
                Text("나만의 메모를 남겨보세요", style = MaterialTheme.typography.bodyMedium, color = c.muted)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.ink),
                cursorBrush = SolidColor(c.amber),
                modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(c.chip)
                    .clickable(onClick = onSave)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text("메모 저장", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = c.navy)
            }
        }
    }
}
