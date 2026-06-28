package com.daim.safetyexam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.isSpecified
import com.daim.safetyexam.data.FontScale
import com.daim.safetyexam.data.ThemeMode

private val Green = Color(0xFF0D5C4A)
private val GreenLight = Color(0xFF4C8C7B)
private val Amber = Color(0xFFFFB300)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    secondary = GreenLight,
    tertiary = Amber,
    background = Color(0xFFF7F8F7),
    surface = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = GreenLight,
    onPrimary = Color.Black,
    secondary = Green,
    tertiary = Amber,
    background = Color(0xFF121413),
    surface = Color(0xFF1E211F),
)

private fun typography(scale: Float): Typography {
    val base = Typography()
    fun TextStyle.s() = copy(
        fontSize = if (fontSize.isSpecified) fontSize * scale else fontSize,
        lineHeight = if (lineHeight.isSpecified) lineHeight * scale else lineHeight
    )
    return base.copy(
        displayLarge = base.displayLarge.s(),
        headlineSmall = base.headlineSmall.s(),
        titleLarge = base.titleLarge.s(),
        titleMedium = base.titleMedium.s(),
        bodyLarge = base.bodyLarge.s(),
        bodyMedium = base.bodyMedium.s(),
        bodySmall = base.bodySmall.s(),
        labelLarge = base.labelLarge.s(),
        labelMedium = base.labelMedium.s(),
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
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = typography(fontScale.scale),
        content = content
    )
}
