# 산업안전기사 기출 (Android)

산업안전기사 필기 5개년 기출(2017\~2021) **1,800문항·해설·이미지를 전부 오프라인 임베드**한
학습 앱입니다. 회차별·과목별·랜덤 출제 + 모의고사 + 오답노트 + 즐겨찾기 + 학습 통계를 제공합니다.

> 기획안 원문: [`android_app_plan.md`](android_app_plan.md)

## 기술 스택

- **언어/UI**: Kotlin + Jetpack Compose (Material 3)
- **아키텍처**: 단방향 데이터 흐름 + Repository (Compose state / AndroidViewModel)
- **DB**: 임베드 SQLite를 내부 저장소로 복사해 읽기/쓰기 (`android.database.sqlite`)
- **이미지**: Coil(GIF 디코더) — `assets/images`에서 오프라인 로드
- **최소 SDK**: 26 (Android 8.0) / target 34

## 디자인 시스템

`design_guide.md` · `safety_app_mockups.html` 시안(시안 A · 네이비 + 세이프티 앰버)을 토큰 단위로 구현했습니다.

- **컬러 토큰**: `ui/theme/Color.kt` 의 `AppColors`(라이트/다크 2세트) — `MaterialTheme.appColors` 로 접근. 앱바·상태바는 다크에서도 네이비 유지.
- **타이포**: 가이드 §3 타입 스케일(Title 16·Stem 12.5·Body 11 …) + 폰트 3단계(0.9/1.0/1.15) 배율.
- **선택지 4상태**(기본/선택/정답/오답): 색 + 번호 + ✓/✕ 3중 표시(색맹 친화).
- **합격선 막대**: 60% 지점 마커가 있는 `PassBar` — 모의고사 결과·통계 공용.
- **하단 탭**(홈/통계/설정), 네이비 앱바, 앰버 진행률, 해설 카드(앰버 톤), 이미지 로드 실패 플레이스홀더.
- 공용 컴포넌트: `ui/Common.kt`, `ui/LearnCards.kt`.

## 데이터 자산

| 자산 | 위치 | 비고 |
| :-- | :-- | :-- |
| SQLite 본체 | `app/src/main/assets/industrial_safety_engineer.db` | 3.88 MB, 스키마 무변경 |
| 그림 이미지 | `app/src/main/assets/images/*.gif` | 204장 |

앱은 컨텐츠 DB를 그대로 사용하고, 사용자 데이터용 테이블(`favorites`, `memos`,
`study_sessions`)과 풀이 기록(`user_attempts`)을 같은 DB에 **추가**합니다 (스키마 변경 아님).

## 빌드 방법

### Android Studio
1. Android Studio (Koala 이상)로 프로젝트 루트를 엽니다.
2. Gradle Sync 후 **Run ▶**.

### 명령줄
```bash
# 디버그 APK
./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug
# 산출물: app/build/outputs/apk/debug/app-debug.apk

# 릴리스(서명) APK — 키스토어 준비 후
./gradlew assembleRelease
```

JDK 17 이상이 필요합니다. (Android Gradle Plugin 8.5.x)

### 릴리스 서명 (Play Console 내부 테스트 업로드용)
`app/build.gradle.kts`의 `buildTypes.release`에 `signingConfig`를 추가하거나,
Android Studio의 **Build > Generate Signed Bundle / APK**를 사용하세요. 키스토어 파일은
`.gitignore`에 의해 커밋되지 않습니다.

## 구현된 기능

| ID | 기능 | 상태 |
| :-- | :-- | :-- |
| F1 | 회차별 풀이 (15회차) | ✅ |
| F2 | 과목별 풀이 (출제 수/정렬 옵션) | ✅ |
| F3 | 모의고사 (6×20=120, 2:30 타이머, 동일회차 ≤20%, 최근 풀이 가중치↓) | ✅ |
| F4 | 오답노트 (1회+/2회+/최근7일 필터, 즐겨찾기 우선) | ✅ |
| F5 | 즐겨찾기 | ✅ |
| F6 | 해설 카드 (마크다운 일부 + stem/choice 이미지) | ✅ |
| F7 | 키워드 검색 (문제/보기/해설) | ✅ |
| F8 | 통계 대시보드 (정답률, 과목별, 약점 Top5, 30일 차트, streak) | ✅ |
| F9 | 다크 모드 (시스템/라이트/다크) | ✅ |
| F10 | 폰트 크기 3단계 | ✅ |
| F11 | 학습 알림 (시간 지정 로컬 노티) | ✅ |
| F12 | 오프라인 동작 (인터넷 권한 없음) | ✅ |
| F13 | 세로 고정 | ✅ |
| F14/F15 | 백업/복원 (JSON, SAF) | ✅ |
| F16 | 선택 초기화 (기록/즐겨찾기/메모/전부) | ✅ |
| F17 | DB 마이그레이션 (사용자 기록 보존) | ✅ (아래 절차) |

## DB 갱신 절차 (회차 추가 / 오답 정정)

1. 새 SQLite 본체를 `app/src/main/assets/industrial_safety_engineer.db` 에 덮어씁니다.
   (스키마는 동일하게 유지 — `exams/questions/choices/explanations/question_images/subjects`)
2. `DbProvider.ASSET_DB_VERSION` 값을 +1 합니다.
3. 새 이미지가 있으면 `app/src/main/assets/images/` 에 추가합니다.
4. 빌드/배포하면, 앱은 다음 실행 시 자동으로:
   - 기존 사용자 데이터(`favorites/memos/study_sessions/user_attempts`)를 별도 백업 DB로 옮기고,
   - 새 컨텐츠 DB로 교체한 뒤,
   - 사용자 데이터를 복원합니다. (`DbProvider.migrateKeepingUserData`)

## 프로젝트 구조

```
app/src/main/java/com/daim/safetyexam/
├─ App.kt                     # Coil(GIF) 전역 설정
├─ MainActivity.kt            # 테마 + NavHost 진입점
├─ data/
│  ├─ DbProvider.kt           # assets DB 복사/오픈 + 추가 테이블 + 마이그레이션
│  ├─ Repository.kt           # 모든 쿼리(문항/오답/검색/통계/백업)
│  ├─ Models.kt               # 데이터 클래스
│  ├─ SettingsStore.kt        # 테마/폰트/알림 설정
│  └─ Reminder.kt             # 학습 알림(AlarmManager)
└─ ui/
   ├─ AppNav.kt               # 내비게이션 그래프
   ├─ QuizSessionViewModel.kt # 풀이 세션 상태(공유)
   ├─ Common.kt               # 마크다운/이미지/탑바
   ├─ theme/Theme.kt
   └─ screens/                # Home/Exam/Subject/Quiz/Result/Wrong/Favorite/Search/Stats/Settings/Detail
```

## 라이선스 / 면책

- 학습 데이터는 기기 내부에만 저장되며 외부로 전송되지 않습니다.
- 이미지 출처: kinz.kr
