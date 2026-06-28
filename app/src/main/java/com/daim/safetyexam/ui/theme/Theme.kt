package com.daim.safetyexam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.daim.safetyexam.data.FontScale
import com.daim.safetyexam.data.ThemeMode

private fun colorScheme(c: AppColors) = if (c.isDark) {
    darkColorScheme(
        primary = c.navy,
        onPrimary = c.onNavy,
        secondary = c.amber,
        onSecondary = c.navy,
        tertiary = c.amber,
        onTertiary = c.navy,
        background = c.bg,
        onBackground = c.ink,
        surface = c.card,
        onSurface = c.ink,
        surfaceVariant = c.chip,
        onSurfaceVariant = c.ink,
        outline = c.muted,
        outlineVariant = c.line,
        error = c.red,
        onError = c.onNavy,
        primaryContainer = c.navy2,
        onPrimaryContainer = c.onNavy,
        secondaryContainer = c.chip,
        onSecondaryContainer = c.ink,
    )
} else {
    lightColorScheme(
        primary = c.navy,
        onPrimary = c.onNavy,
        secondary = c.amber,
        onSecondary = c.navy,
        tertiary = c.amber,
        onTertiary = c.navy,
        background = c.bg,
        onBackground = c.ink,
        surface = c.card,
        onSurface = c.ink,
        surfaceVariant = c.chip,
        onSurfaceVariant = c.ink,
        outline = c.muted,
        outlineVariant = c.line,
        error = c.red,
        onError = c.onNavy,
        primaryContainer = c.navy2,
        onPrimaryContainer = c.onNavy,
        secondaryContainer = c.chip,
        onSecondaryContainer = c.navy,
    )
}

/** 디자인 가이드 v2.0 §3 타입 스케일 (폰트 "보통" 기준, 본문 교정값) */
private fun baseTypography() = Typography(
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp),  // 앱바 제목
    displayLarge = TextStyle(fontSize = 54.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 54.sp),        // 점수
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold),                           // 통계 수치
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),                                  // 보기/카드 제목
    bodyLarge = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),            // 문항 본문(stem)
    bodyMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 23.sp),             // 해설 본문
    bodySmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.4.sp),      // 섹션 라벨
    labelMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),                              // 메타/캡션/번호
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),                               // 하단 탭
)

private fun Typography.scaled(scale: Float): Typography {
    fun TextStyle.s() = copy(
        fontSize = if (fontSize.isSpecified) fontSize * scale else fontSize,
        lineHeight = if (lineHeight.isSpecified) lineHeight * scale else lineHeight
    )
    return copy(
        displayLarge = displayLarge.s(),
        headlineSmall = headlineSmall.s(),
        titleLarge = titleLarge.s(),
        titleMedium = titleMedium.s(),
        bodyLarge = bodyLarge.s(),
        bodyMedium = bodyMedium.s(),
        bodySmall = bodySmall.s(),
        labelLarge = labelLarge.s(),
        labelMedium = labelMedium.s(),
        labelSmall = labelSmall.s(),
    )
}

@Composable
fun SafetyExamTheme(
    themeMode: ThemeMode,
    fontScale: FontScale,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val appColors = if (dark) DarkAppColors else LightAppColors
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme(appColors),
            typography = baseTypography().scaled(fontScale.scale),
            content = content
        )
    }
}

/** MaterialTheme.appColors 로 의미색 토큰 접근 */
val MaterialTheme.appColors: AppColors
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current
