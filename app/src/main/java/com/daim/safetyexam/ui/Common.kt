package com.daim.safetyexam.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.daim.safetyexam.ui.theme.appColors

// ─────────────────────────── 반응형(태블릿) 컨테이너 ───────────────────────────

/**
 * 넓은 화면(태블릿 등)에서 본문이 과도하게 늘어나 가독성이 떨어지지 않도록 하는 최대 폭.
 * 폰에서는 가용 폭이 이보다 작아 아무 영향이 없다.
 *
 * ── 태블릿 대응 방향 (각주) ────────────────────────────────────────────────
 *  [적용] 옵션 2 일부 · 본격 태블릿 대응 :
 *   - 회전: 태블릿(sw600dp+)만 가로 허용, 폰은 세로 고정 (res/values(-sw600dp)/bools.xml
 *     `allow_rotation` + MainActivity 런타임 `requestedOrientation`).
 *   - 폭 판정: [rememberIsWide] (screenWidthDp ≥ 840) 로 2단/레일 게이트 (Responsive.kt).
 *   - 넓은 화면 2단: 풀이(문제↔해설)·결과·문항 상세 좌우 분할, 홈 3열·통계 2열,
 *     홈/통계/설정 트라이어드는 [TriadScaffold]로 하단탭↔[SafetyNavRail] 자동 전환.
 *   - 본문 폭: [ScrollableContentColumn] 가 [ContentMaxWidth](640dp) 로 제한·중앙 정렬(폰=꽉참).
 *   단일 APK 런타임 로직이라 같은 빌드가 폰/태블릿·세로/가로로 자동 분기.
 *
 *  [미적용] 목록/그리드 화면(회차 목록·즐겨찾기·검색·오답노트) 마스터-디테일은 후속 과제.
 * ──────────────────────────────────────────────────────────────────────────
 */
val ContentMaxWidth = 640.dp

/**
 * 세로 스크롤 본문을 [ContentMaxWidth]로 제한하고 넓은 화면에서 가운데 정렬하는 컨테이너.
 * 폰에서는 기존과 동일하게 화면을 꽉 채운다. 기존 `Column(verticalScroll)` 본문을 이 블록으로 대체한다.
 */
@Composable
fun ScrollableContentColumn(
    pad: PaddingValues,
    contentPadding: Dp = 14.dp,
    maxWidth: Dp = ContentMaxWidth,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .fillMaxSize()
                .widthIn(max = maxWidth)
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
            content = content
        )
    }
}

// ──────────────────────────────── 앱바 ────────────────────────────────

/** 네이비 앱바 (상태바 영역까지 네이비로 채움) */
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    centerTitle: Boolean = false,
    actions: @Composable () -> Unit = {}
) {
    val c = MaterialTheme.appColors
    Surface(color = c.navy) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .heightIn(min = 48.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                NavyIconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = c.onNavy)
                }
                Spacer(Modifier.width(8.dp))
            }
            Column(
                Modifier.weight(1f),
                horizontalAlignment = if (centerTitle) Alignment.CenterHorizontally else Alignment.Start
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = c.onNavy)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.labelMedium, color = c.navySub)
                }
            }
            actions()
        }
    }
}

/** 문항/모의고사 풀이 화면용 앱바 (중앙 메타 + 타이머/즐겨찾기) */
@Composable
fun QuizTopBar(
    examMeta: String,
    progressText: String,
    timer: String?,
    timerWarn: Boolean,
    isFav: Boolean,
    onBack: () -> Unit,
    onToggleFav: () -> Unit
) {
    val c = MaterialTheme.appColors
    Surface(color = c.navy) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .heightIn(min = 48.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavyIconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = c.onNavy)
            }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(examMeta, style = MaterialTheme.typography.titleMedium, color = c.onNavy)
                Text(progressText, style = MaterialTheme.typography.labelSmall, color = c.navySub)
            }
            if (timer != null) {
                Text(
                    timer,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (timerWarn) c.red else c.amber
                )
            } else {
                NavyIconButton(onClick = onToggleFav) {
                    Icon(
                        if (isFav) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "즐겨찾기",
                        tint = if (isFav) c.amber else c.onNavy
                    )
                }
            }
        }
    }
}

/** 앱바용 아이콘 버튼 (rgba(255,255,255,.12), 30dp, 반경 9) */
@Composable
fun NavyIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

// ──────────────────────────────── 공통 요소 ────────────────────────────────

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.appColors.muted,
        modifier = modifier.padding(vertical = 2.dp)
    )
}

@Composable
fun SafetyChip(text: String, modifier: Modifier = Modifier) {
    val c = MaterialTheme.appColors
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(c.chip)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = c.accentFg)
    }
}

/** 진행률 트랙 (5dp, 앰버 채움) */
@Composable
fun ProgressTrack(progress: Float, modifier: Modifier = Modifier) {
    val c = MaterialTheme.appColors
    Box(
        modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(c.chip)
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(5.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(c.amber)
        )
    }
}

// ──────────────────────────────── 선택지 ────────────────────────────────

enum class ChoiceState { DEFAULT, SELECTED, CORRECT, WRONG }

@Composable
fun ChoiceItem(
    no: Int,
    body: String,
    state: ChoiceState,
    imageAsset: String? = null,
    note: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val c = MaterialTheme.appColors
    val border = when (state) {
        ChoiceState.DEFAULT -> c.line
        ChoiceState.SELECTED -> c.amber
        ChoiceState.CORRECT -> c.green
        ChoiceState.WRONG -> c.red
    }
    val bg = when (state) {
        ChoiceState.DEFAULT -> c.card
        ChoiceState.SELECTED -> c.choiceSelBg
        ChoiceState.CORRECT -> c.choiceCorrectBg
        ChoiceState.WRONG -> c.choiceWrongBg
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(11.dp))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NumberCircle(no, state)
            Spacer(Modifier.width(9.dp))
            Text(
                body,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = c.ink,
                modifier = Modifier.weight(1f)
            )
            // 색맹 친화: 색 + 번호 + ✓/✕
            when (state) {
                ChoiceState.CORRECT -> Icon(Icons.Filled.Check, "정답", tint = c.green, modifier = Modifier.size(18.dp))
                ChoiceState.WRONG -> Icon(Icons.Filled.Close, "오답", tint = c.red, modifier = Modifier.size(18.dp))
                else -> {}
            }
        }
        if (imageAsset != null) {
            Spacer(Modifier.height(8.dp))
            QImage(imageAsset)
        }
        // 보기별 해설(채점 공개 시) — 번호 좌측 정렬에 맞춰 들여쓰기
        if (!note.isNullOrBlank()) {
            Spacer(Modifier.height(7.dp))
            Text(
                note,
                style = MaterialTheme.typography.bodySmall,
                color = c.muted,
                modifier = Modifier.padding(start = 29.dp)
            )
        }
    }
}

@Composable
private fun NumberCircle(no: Int, state: ChoiceState) {
    val c = MaterialTheme.appColors
    // 채운 번호 원의 글자: 다크에서는 밝은 채움 위에 어두운 톤(§2.3), 라이트는 기존 규칙
    val darkGlyph = Color(0xFF0F141B)
    val (fill, textColor, borderColor) = when (state) {
        ChoiceState.DEFAULT -> Triple(Color.Transparent, c.muted, c.muted)
        ChoiceState.SELECTED -> Triple(c.amber, if (c.isDark) darkGlyph else c.navy, c.amber)
        ChoiceState.CORRECT -> Triple(c.green, if (c.isDark) darkGlyph else Color.White, c.green)
        ChoiceState.WRONG -> Triple(c.red, if (c.isDark) darkGlyph else Color.White, c.red)
    }
    Box(
        Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.5.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("$no", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold), color = textColor)
    }
}

// ──────────────────────────────── 합격선 막대 ────────────────────────────────

/**
 * 막대 그래프 + (옵션) 60% 합격선 마커.
 * value: 0~100 (점수/정답률), passMark: 합격선 표시 여부
 */
@Composable
fun PassBar(
    label: String,
    value: Int,
    fillColor: Color,
    modifier: Modifier = Modifier,
    showPassMark: Boolean = true,
    passLine: Int = 60
) {
    val c = MaterialTheme.appColors
    Row(
        modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = c.ink,
            modifier = Modifier.width(78.dp)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(c.chip)
        ) {
            Box(
                Modifier
                    .fillMaxWidth((value / 100f).coerceIn(0f, 1f))
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(fillColor)
            )
            if (showPassMark) {
                Box(
                    Modifier
                        .fillMaxWidth(passLine / 100f)
                        .height(14.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(Modifier.width(2.dp).height(20.dp).background(if (c.isDark) Color.White else Color(0xFF0E1626)))
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "$value",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = c.ink,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )
    }
}

// ──────────────────────────────── 버튼 ────────────────────────────────

@Composable
fun AccentButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val c = MaterialTheme.appColors
    SafetyButton(text, c.amber, c.navy, modifier, enabled, onClick)
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val c = MaterialTheme.appColors
    SafetyButton(text, c.navy, c.onNavy, modifier, enabled, onClick)
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val c = MaterialTheme.appColors
    SafetyButton(text, c.chip, c.accentFg, modifier, enabled, onClick)
}

@Composable
private fun SafetyButton(
    text: String,
    bg: Color,
    fg: Color,
    modifier: Modifier,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) bg else bg.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = fg)
    }
}

// ──────────────────────────────── 하단 탭 ────────────────────────────────

enum class NavTab { HOME, STATS, SETTINGS }

@Composable
fun SafetyBottomBar(current: NavTab, onHome: () -> Unit, onStats: () -> Unit, onSettings: () -> Unit) {
    val c = MaterialTheme.appColors
    Surface(color = c.card) {
        Column(Modifier.navigationBarsPadding()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
            Row(
                Modifier.fillMaxWidth().height(52.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomItem("홈", Icons.Filled.Home, current == NavTab.HOME, onHome)
                BottomItem("통계", Icons.Filled.BarChart, current == NavTab.STATS, onStats)
                BottomItem("설정", Icons.Filled.Settings, current == NavTab.SETTINGS, onSettings)
            }
        }
    }
}

@Composable
private fun BottomItem(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.appColors
    val tint = if (active) c.navActive else c.muted
    Column(
        Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

// ──────────────────────────────── 네비게이션 레일 (태블릿 가로) ────────────────────────────────

/** 넓은 화면에서 하단 탭 대신 좌측에 세우는 세로 내비게이션. [SafetyBottomBar]와 동일 3탭. */
@Composable
fun SafetyNavRail(current: NavTab, onHome: () -> Unit, onStats: () -> Unit, onSettings: () -> Unit) {
    val c = MaterialTheme.appColors
    Surface(color = c.card) {
        Row(Modifier.fillMaxHeight()) {
            Column(
                Modifier.fillMaxHeight().width(84.dp).padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RailItem("홈", Icons.Filled.Home, current == NavTab.HOME, onHome)
                RailItem("통계", Icons.Filled.BarChart, current == NavTab.STATS, onStats)
                RailItem("설정", Icons.Filled.Settings, current == NavTab.SETTINGS, onSettings)
            }
            Box(Modifier.fillMaxHeight().width(1.dp).background(c.line))
        }
    }
}

@Composable
private fun RailItem(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.appColors
    val tint = if (active) c.navActive else c.muted
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

/**
 * 홈/통계/설정 트라이어드용 어댑티브 스캐폴드.
 * 넓은 화면(태블릿 가로)이면 좌측 [SafetyNavRail], 아니면 하단 [SafetyBottomBar].
 * [topBar]/[content]는 그대로 전달하며, content 는 소비할 [PaddingValues]를 받는다.
 */
@Composable
fun TriadScaffold(
    current: NavTab,
    onHome: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val c = MaterialTheme.appColors
    if (rememberIsWide()) {
        Scaffold(containerColor = c.bg, topBar = topBar) { pad ->
            Row(Modifier.fillMaxSize().padding(pad)) {
                SafetyNavRail(current, onHome, onStats, onSettings)
                Box(Modifier.weight(1f).fillMaxHeight()) { content(PaddingValues(0.dp)) }
            }
        }
    } else {
        Scaffold(
            containerColor = c.bg,
            topBar = topBar,
            bottomBar = { SafetyBottomBar(current, onHome, onStats, onSettings) },
            content = content
        )
    }
}

// ──────────────────────────────── 토글 / 체크 / 그립 ────────────────────────────────

/** 디자인 토글 스위치 (트랙 46×26, ON=amber·우측 / OFF=#CDD4E0·좌측) */
@Composable
fun SafetyToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.appColors
    Box(
        modifier
            .size(width = 46.dp, height = 26.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) c.amber else if (c.isDark) Color(0xFF3A4350) else Color(0xFFCDD4E0))
            .clickable { onCheckedChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            Modifier
                .padding(horizontal = 3.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun SafetyCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier) {
    val c = MaterialTheme.appColors
    Row(
        modifier.clickable { onCheckedChange(!checked) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (checked) c.navy else Color.Transparent)
                .border(1.5.dp, if (checked) c.navy else c.muted, RoundedCornerShape(5.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) Icon(Icons.Filled.Check, contentDescription = null, tint = c.onNavy, modifier = Modifier.size(13.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.muted)
    }
}

@Composable
fun SheetGrip() {
    Box(
        Modifier
            .size(width = 36.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.appColors.line)
    )
}

// ──────────────────────────────── 이미지 ────────────────────────────────

/** assets GIF/이미지 로드 — 실패 시 플레이스홀더(앱 크래시 금지) */
@Composable
fun QImage(asset: String, modifier: Modifier = Modifier) {
    val c = MaterialTheme.appColors
    SubcomposeAsyncImage(
        model = asset,
        contentDescription = "문제 그림",
        contentScale = ContentScale.FillWidth,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White),
        error = {
            Box(
                Modifier.fillMaxWidth().height(80.dp).background(c.chip),
                contentAlignment = Alignment.Center
            ) {
                Text("이미지를 불러올 수 없습니다", style = MaterialTheme.typography.labelMedium, color = c.muted)
            }
        }
    )
}

// ──────────────────────────────── 마크다운 ────────────────────────────────

/** 가벼운 마크다운: **굵게**, 줄바꿈, "* " 불릿 */
@Composable
fun MarkdownText(text: String, color: Color = MaterialTheme.appColors.ink, modifier: Modifier = Modifier) {
    Column(modifier) {
        for (raw in text.split("\n")) {
            val line = raw.trimEnd()
            if (line.isBlank()) {
                Spacer(Modifier.height(6.dp))
                continue
            }
            val trimmed = line.trimStart()
            val isBullet = trimmed.startsWith("* ") || trimmed.startsWith("- ")
            if (isBullet) {
                val contentLine = trimmed.removePrefix("* ").removePrefix("- ")
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                    Text("•  ", style = MaterialTheme.typography.bodyMedium, color = color)
                    Text(parseInline(contentLine), style = MaterialTheme.typography.bodyMedium, color = color)
                }
            } else {
                Text(parseInline(line), style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.padding(vertical = 2.dp))
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
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(s.substring(start + 2, end)) }
        i = end + 2
    }
}
