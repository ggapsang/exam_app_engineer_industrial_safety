package com.daim.safetyexam.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 디자인 가이드(design_guide.md / safety_app_mockups.html)의 토큰을 1:1 보관하는 색 묶음.
 * Material ColorScheme에 담기지 않는 의미색(선택지/해설/합격선 등)을 여기서 제공한다.
 */
data class AppColors(
    val isDark: Boolean,
    val navy: Color,
    val navy2: Color,
    val amber: Color,
    val amberDark: Color,
    val green: Color,
    val red: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val bg: Color,
    val card: Color,
    val chip: Color,
    val onNavy: Color,
    val navySub: Color,          // 앱바 보조 텍스트
    // 다크 시인성 보정용 의미 토큰 (라이트=네이비, 다크=밝은 톤)
    val accentFg: Color,         // 카드/칩 위 강조 텍스트·아이콘 (네이비 대체)
    val selFill: Color,          // 선택/활성 칩·세그먼트 배경
    val onSelFill: Color,        // selFill 위 텍스트
    val navActive: Color,        // 하단 탭 활성색
    // 선택지 파생색
    val choiceSelBg: Color,
    val choiceCorrectBg: Color,
    val choiceWrongBg: Color,
    // 해설 카드
    val expBg: Color,
    val expBorder: Color,
    val expText: Color,
    val expRefText: Color,
    val expRefBorder: Color,
)

val LightAppColors = AppColors(
    isDark = false,
    navy = Color(0xFF16233F),
    navy2 = Color(0xFF1F3258),
    amber = Color(0xFFF5A524),
    amberDark = Color(0xFFD98A0C),
    green = Color(0xFF1AA97A),
    red = Color(0xFFE25A45),
    ink = Color(0xFF1A2233),
    muted = Color(0xFF7A8499),
    line = Color(0xFFE6E9F0),
    bg = Color(0xFFF4F6FA),
    card = Color(0xFFFFFFFF),
    chip = Color(0xFFEEF1F7),
    onNavy = Color(0xFFFFFFFF),
    navySub = Color(0xFF9FB0D0),
    accentFg = Color(0xFF16233F),
    selFill = Color(0xFF16233F),
    onSelFill = Color(0xFFFFFFFF),
    navActive = Color(0xFF16233F),
    choiceSelBg = Color(0xFFFFF6E6),
    choiceCorrectBg = Color(0xFFEAFAF3),
    choiceWrongBg = Color(0xFFFDEEEB),
    expBg = Color(0xFFFFF7E9),
    expBorder = Color(0xFFF6E2B8),
    expText = Color(0xFF5A4A28),
    expRefText = Color(0xFF9A7B3E),
    expRefBorder = Color(0xFFECD9AA),
)

val DarkAppColors = AppColors(
    isDark = true,
    navy = Color(0xFF16233F),    // 앱바는 다크에서도 네이비 유지
    navy2 = Color(0xFF1F3258),
    amber = Color(0xFFF2B04A),
    amberDark = Color(0xFFF2B04A),
    green = Color(0xFF2DBE8C),
    red = Color(0xFFF0735E),
    ink = Color(0xFFE7EEF5),
    muted = Color(0xFF8A95A3),
    line = Color(0xFF2A3441),
    bg = Color(0xFF0F141B),
    card = Color(0xFF1A222D),
    chip = Color(0xFF222C38),
    onNavy = Color(0xFFFFFFFF),
    navySub = Color(0xFF9FB0D0),
    accentFg = Color(0xFFE7EEF5),        // 다크: 강조 텍스트는 밝게 (네이비 금지)
    selFill = Color(0xFFF2B04A),         // 다크: 활성 배경은 앰버
    onSelFill = Color(0xFF0F141B),       // 앰버 위 어두운 글자
    navActive = Color(0xFFF2B04A),       // 다크: 활성 탭은 앰버
    choiceSelBg = Color(0xFF2E2A1A),     // §2.3 다크 선택(채점 전) 배경
    choiceCorrectBg = Color(0xFF13312A),
    choiceWrongBg = Color(0xFF3A1F1B),
    expBg = Color(0xFF2A2516),
    expBorder = Color(0xFF4A4128),       // §2.2 해설 카드 테두리(다크)
    expText = Color(0xFFE4DAC0),         // §2.2 해설 본문 텍스트(다크)
    expRefText = Color(0xFFB7A678),      // §2.2 해설 참고줄(다크)
    expRefBorder = Color(0xFF4A4128),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
