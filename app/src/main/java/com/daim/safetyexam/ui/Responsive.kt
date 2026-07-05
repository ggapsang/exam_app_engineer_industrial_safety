package com.daim.safetyexam.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

// ─────────────────────────── 반응형(태블릿) 폭 판정 ───────────────────────────
//
// material3-window-size-class 의존성을 추가하지 않고, 기존 경량 철학에 맞춰
// LocalConfiguration.screenWidthDp 로 폭을 판정한다. 회전 시 configuration 이
// 갱신되어 이 값이 바뀌면 이를 읽는 컴포저블이 자동으로 재구성된다.

/** 넓은 화면(태블릿 가로 ~1280dp, 대형 태블릿 세로) — 2단 배치 · 네비게이션 레일 게이트. */
@Composable
fun rememberIsWide(): Boolean = LocalConfiguration.current.screenWidthDp >= 840
