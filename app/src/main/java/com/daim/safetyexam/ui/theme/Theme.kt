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

/** 디자인 가이드 §3 타입 스케일 (폰트 "보통" 기준) */
private fun baseTypography() = Typography(
    titleLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp),
    displayLarge = TextStyle(fontSize = 46.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 46.sp),
    headlineSmall = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold),
    titleMedium = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 19.4.sp),
    bodyMedium = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal, lineHeight = 17.sp),
    bodySmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, lineHeight = 14.sp),
    labelLarge = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.4.sp),
    labelMedium = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
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
