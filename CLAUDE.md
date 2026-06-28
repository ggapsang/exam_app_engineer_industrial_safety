# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Offline Android study app (Korean) for the 산업안전기사 (Industrial Safety Engineer) written exam: 1,800 past questions + explanations + 204 GIF images embedded as a SQLite DB, with quiz modes, wrong-note, mock exam, favorites, and stats. Single-module Kotlin + Jetpack Compose (Material 3), minSdk 26 / target 34.

The product spec is `android_app_plan.md` (features F1–F17, acceptance/rejection criteria). The visual spec is `design_guide.md` + `safety_app_mockups.html` (Navy + Safety Amber, token-level). Treat those three as source of truth; keep them in sync when changing behavior or visuals.

## Build / run

JDK 17 is required (AGP 8.5.2, Kotlin 1.9.24, Compose compiler 1.5.14).

```bash
./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug  → app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease      # signing not configured; use Android Studio Generate Signed APK
./gradlew lint                 # Android lint
```

There is **no test suite** (no `src/test` or `src/androidTest`). Don't claim tests pass — there are none to run.

Inspecting the embedded DB from the repo root (note: a Windows console shows Korean as mojibake, but the stored data is valid UTF-8):
```bash
python -c "import sqlite3;c=sqlite3.connect('industrial_safety_engineer.db');print(c.execute('SELECT count(*) FROM questions').fetchone())"
```

## Architecture (the non-obvious parts)

**Data layer deliberately avoids Room/DI.** A pre-built SQLite file is the content source and must stay schema-unchanged (acceptance condition 7.2.1). Using Room would force schema validation against a hand-built DB, so instead:
- `data/DbProvider.kt` copies `assets/industrial_safety_engineer.db` → internal storage on first launch and opens it read/write. It then `CREATE TABLE IF NOT EXISTS` the app-owned tables (`favorites`, `memos`, `study_sessions`) alongside the embedded content tables. Single cached `SQLiteDatabase` for the app lifetime.
- `data/Repository.kt` is a singleton wrapping that `SQLiteDatabase` with raw `rawQuery`/`execSQL` and manual `Cursor` mapping into `data/Models.kt` data classes. All DB I/O goes through here.
- **DB upgrade (F17):** bump `DbProvider.ASSET_DB_VERSION` when replacing the asset DB. On version mismatch, `migrateKeepingUserData()` dumps user tables to a separate `userdata.db` via `ATTACH`, re-copies the new content DB, and restores them — so user progress survives content updates.
- `execSQL` bind-arg arrays are written as `arrayOf<Any?>(...)` on purpose (the platform `Object[]` param + Kotlin array invariance is finicky otherwise).

**State management is intentionally lightweight — no per-screen ViewModels except one.** Most screens fetch data inline with `produceState`/`LaunchedEffect { withContext(Dispatchers.IO) { Repository.get(context)... } }`. The single shared `ui/QuizSessionViewModel` (activity-scoped, created in `MainActivity`) holds the active quiz session — the loaded `List<QuestionFull>`, per-question answers, reveal/grading state, and the mock timer — so large question lists are never passed through nav args. `start*()` entry points load question IDs then questions on IO; `finish()` records all `user_attempts` at the end (not per tap) to avoid double-counting, and instant-grading is force-disabled for `StudyMode.MOCK`.

**Navigation:** `ui/AppNav.kt` is a flat `NavHost`. Quiz playthroughs push `quiz` → on finish pop to `result`; Home/Stats/Settings form a bottom-tab triad wired with `popUpTo(HOME)` to keep the back stack shallow.

**Images:** `question_images.path` already contains the `images/` prefix; Repository turns it into `file:///android_asset/<path>` and `ui/Common.kt:QImage` loads it via Coil. **Coil's GIF decoder is configured globally in `App.kt`** (`ImageDecoderDecoder` on API ≥28 else `GifDecoder`) — `App` must stay registered as `android:name` in the manifest. `.db` is in `androidResources { noCompress }` and assets are stream-copied, so packaging compression is a non-issue.

**Design system:** tokens not expressible in Material's `ColorScheme` (choice states, explanation card, pass-line) live in `ui/theme/Color.kt` as `AppColors` (light/dark sets) exposed via `MaterialTheme.appColors` (`LocalAppColors`). The app bar / status bar stay Navy in both themes. Reusable components are in `ui/Common.kt` (app bars, `ChoiceItem` with the color+number+✓/✕ tri-state for color-blind accessibility, `PassBar` with the 60% pass-line marker, bottom nav, buttons) and `ui/LearnCards.kt` (explanation card + memo). Font size has an in-app 3-step scale (0.9/1.0/1.25) applied in `theme/Theme.kt`, separate from OS font scaling. The type scale follows design_guide_2.0.md (Stem 17 / Choice 16 / Title 20).

Two pre-quiz/exit flows exist: the mock-exam start sheet (`MockStartScreen`, grading choice, remembered via `settings.mockSkipStart`/`mockInstant`) and the session-exit sheet in `QuizScreen` (`settings.sessionExitSave` = ask/always/never; saves wrong answers + a resume snapshot). **Resume (이어풀기)** persists an active EXAM/SUBJECT session as JSON (`data/ResumeSnapshot`) in `SettingsStore.resumeJson`; `QuizSessionViewModel.exitWithSave()` writes it, `finish()` clears it, and Home's quick-start resumes it.

## Hard constraints (rejection criteria — don't break these)

- **Fully offline:** no `INTERNET` permission. Don't add network calls.
- **Portrait-locked** (`screenOrientation="portrait"`).
- All 204 mapped images must render; image load failure must show a placeholder, never crash (`QImage` handles this).
- Mock exam: same-exam questions must stay ≤20% (≤24 of 120) — enforced in `Repository.mockQuestionIds()`.
- User progress must persist across app restarts and content-DB updates.

## Authoring gotchas (this codebase was hit by all of these)

- **Kotlin extension properties cannot be reached by fully-qualified path.** `Icons.Filled.Star` etc. must be `import`ed; `androidx.compose.material.icons.Icons.Filled.Star` as an inline FQN does **not** compile.
- Don't read `MaterialTheme.*` (composable getters) inside non-composable lambdas (`buildAnnotatedString {}`, `remember {}`, `produceState`/`LaunchedEffect` blocks) — hoist the value to a `val` first.
- Keep the first DB touch off the main thread (the VM's `repo` is `by lazy` so the ~4MB first-run copy happens inside an IO coroutine).

## Data assets

- Embedded (build inputs): `app/src/main/assets/industrial_safety_engineer.db`, `app/src/main/assets/images/*.gif`.
- Repo-root `industrial_safety_engineer.db` and `images/` are the originals and are `.gitignore`d to avoid duplicating the embedded copies — edit the `assets/` copies for the app.
