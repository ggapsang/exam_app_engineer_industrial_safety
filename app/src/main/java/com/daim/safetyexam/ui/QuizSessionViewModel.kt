package com.daim.safetyexam.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daim.safetyexam.data.QuestionFull
import com.daim.safetyexam.data.Repository
import com.daim.safetyexam.data.ResumeSnapshot
import com.daim.safetyexam.data.SettingsStore
import com.daim.safetyexam.data.StudyMode
import com.daim.safetyexam.data.SubjectStat
import com.daim.safetyexam.data.SessionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 활동(Activity) 범위로 공유되는 풀이 세션 상태.
 * 큰 문항 리스트를 NavArg로 넘기지 않고 이 VM에 담아 화면 간 공유한다.
 */
class QuizSessionViewModel(app: Application) : AndroidViewModel(app) {

    // DB 최초 복사는 무거우므로 main 스레드 생성자에서 건드리지 않도록 lazy 처리
    private val repo by lazy { Repository.get(app) }
    private val settings = SettingsStore.get(app)

    var mode by mutableStateOf(StudyMode.EXAM); private set
    var questions by mutableStateOf<List<QuestionFull>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var currentIndex by mutableStateOf(0); private set

    /** questionId -> 선택 보기 번호 */
    val answers = mutableStateMapOf<Int, Int>()
    /** 즉시 채점에서 이미 공개된 문항 id */
    val revealed = mutableStateMapOf<Int, Boolean>()

    var instantGrading by mutableStateOf(true); private set
    var finished by mutableStateOf(false); private set
    var result by mutableStateOf<SessionResult?>(null); private set

    /** 이어풀기 스냅샷 라벨용 (예: "2021년 1회" / "기계위험방지") */
    var sessionLabel by mutableStateOf(""); private set

    // 모의고사 타이머
    var timerEnabled by mutableStateOf(false); private set
    var remainingSec by mutableStateOf(0); private set
    private var timerJob: Job? = null

    private var startedAt: String = ""
    private var sessionStartMs: Long = 0L
    private val elapsedByQ = HashMap<Int, Long>()
    private var enterQuestionMs: Long = 0L

    val current: QuestionFull? get() = questions.getOrNull(currentIndex)
    val answeredCount: Int get() = questions.count { answers.containsKey(it.id) }

    /** 이어풀기 대상 모드인지 (회차/과목만) */
    val isResumable: Boolean get() = mode == StudyMode.EXAM || mode == StudyMode.SUBJECT

    /** 지금까지 응답한 문항 중 오답 수 */
    fun answeredWrong(): Int = questions.count { val s = answers[it.id]; s != null && s != it.answerNo }

    // ---- 진입 지점 (홈/목록에서 호출) ----

    fun startExam(examId: Int) =
        launchStart(StudyMode.EXAM) { repo.examQuestionIds(examId) }

    fun startSubject(subjectId: Int, count: Int, order: String) =
        launchStart(StudyMode.SUBJECT) { repo.subjectQuestionIds(subjectId, count, order) }

    /** 모의고사. instant=true 면 즉시 채점(학습용), 기본은 일괄(실전형) */
    fun startMock(instant: Boolean) =
        launchStart(StudyMode.MOCK, withTimer = true, timerSec = 9000, instantOverride = instant) { repo.mockQuestionIds() }

    /** 이미 결정된 문항 ID 목록으로 시작 (오답노트/즐겨찾기 일괄 풀이) */
    fun startFromIds(newMode: StudyMode, ids: List<Int>) =
        launchStart(newMode) { ids }

    /** 이어풀기 — 저장된 스냅샷으로 복원 */
    fun resume(snap: ResumeSnapshot) =
        launchStart(snap.mode, restore = snap) { snap.ids }

    private fun launchStart(
        newMode: StudyMode,
        withTimer: Boolean = false,
        timerSec: Int = 9000,
        instantOverride: Boolean? = null,
        restore: ResumeSnapshot? = null,
        idProvider: suspend () -> List<Int>
    ) {
        timerJob?.cancel()
        mode = newMode
        instantGrading = instantOverride ?: (settings.instantGrading && newMode != StudyMode.MOCK)
        timerEnabled = withTimer
        remainingSec = timerSec
        finished = false
        result = null
        currentIndex = 0
        questions = emptyList()
        answers.clear()
        revealed.clear()
        elapsedByQ.clear()
        sessionLabel = ""
        loading = true
        startedAt = nowText()
        sessionStartMs = System.currentTimeMillis()
        enterQuestionMs = sessionStartMs

        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                val ids = idProvider()
                repo.loadQuestions(ids)
            }
            questions = loaded
            sessionLabel = when (newMode) {
                StudyMode.EXAM -> loaded.firstOrNull()?.examTitle ?: ""
                StudyMode.SUBJECT -> loaded.firstOrNull()?.subjectShort ?: ""
                else -> ""
            }
            if (restore != null) {
                answers.putAll(restore.answers)
                if (instantGrading) restore.answers.keys.forEach { revealed[it] = true }
                currentIndex = restore.index.coerceIn(0, (loaded.size - 1).coerceAtLeast(0))
            }
            loading = false
            if (withTimer && loaded.isNotEmpty()) startTimer()
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (remainingSec > 0 && !finished) {
                delay(1000)
                remainingSec -= 1
            }
            if (!finished) finish()
        }
    }

    fun select(no: Int) {
        val q = current ?: return
        answers[q.id] = no
        if (instantGrading) revealed[q.id] = true
    }

    fun goTo(index: Int) {
        accrueElapsed()
        currentIndex = index.coerceIn(0, (questions.size - 1).coerceAtLeast(0))
        enterQuestionMs = System.currentTimeMillis()
    }

    fun next() { if (currentIndex < questions.size - 1) goTo(currentIndex + 1) }
    fun prev() { if (currentIndex > 0) goTo(currentIndex - 1) }

    private fun accrueElapsed() {
        val q = current ?: return
        val now = System.currentTimeMillis()
        elapsedByQ[q.id] = (elapsedByQ[q.id] ?: 0L) + (now - enterQuestionMs)
    }

    /**
     * 회차/과목 풀이 중단 시 호출. 진행 위치(이어풀기)는 항상 저장하고,
     * saveWrong=true 면 이번 세션의 (응답한) 오답을 user_attempts에 기록한다.
     */
    fun exitWithSave(saveWrong: Boolean) {
        if (!isResumable) return
        accrueElapsed()
        timerJob?.cancel()
        // 진행 위치 스냅샷 저장 (이어풀기)
        val snap = ResumeSnapshot(mode, sessionLabel, questions.map { it.id }, HashMap(answers), currentIndex)
        settings.saveResume(snap.toJson())
        if (saveWrong) {
            val snapshotAnswers = HashMap(answers)
            val elapsed = HashMap(elapsedByQ)
            val qs = questions
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    for (q in qs) {
                        val sel = snapshotAnswers[q.id] ?: continue
                        repo.recordAttempt(q.id, sel, sel == q.answerNo, elapsed[q.id] ?: 0L)
                    }
                }
            }
        }
    }

    /** 저장 없이 세션 폐기 (모의고사 등 비-이어풀기 모드 중단 시 타이머 정지) */
    fun abandonSession() {
        timerJob?.cancel()
    }

    fun finish() {
        if (finished) return
        accrueElapsed()
        timerJob?.cancel()
        finished = true
        settings.clearResume()  // 완료했으므로 이어풀기 스냅샷 제거

        viewModelScope.launch {
            val perSubjectTotal = HashMap<Int, Int>()
            val perSubjectCorrect = HashMap<Int, Int>()
            val shortNames = HashMap<Int, String>()
            var correct = 0

            withContext(Dispatchers.IO) {
                for (q in questions) {
                    val sel = answers[q.id]
                    val isCorrect = sel != null && sel == q.answerNo
                    if (isCorrect) correct++
                    perSubjectTotal[q.subjectId] = (perSubjectTotal[q.subjectId] ?: 0) + 1
                    if (isCorrect) perSubjectCorrect[q.subjectId] = (perSubjectCorrect[q.subjectId] ?: 0) + 1
                    shortNames[q.subjectId] = q.subjectShort
                    repo.recordAttempt(q.id, sel, isCorrect, elapsedByQ[q.id] ?: 0L)
                }
            }

            val elapsedSec = ((System.currentTimeMillis() - sessionStartMs) / 1000).toInt()
            val perSubject = perSubjectTotal.keys.sorted().map { sid ->
                SubjectStat(sid, shortNames[sid] ?: "", perSubjectTotal[sid] ?: 0, perSubjectCorrect[sid] ?: 0)
            }
            val res = SessionResult(mode, questions.size, correct, elapsedSec, perSubject)
            result = res
            withContext(Dispatchers.IO) { repo.saveSession(res, startedAt) }
        }
    }

    private fun nowText(): String {
        // SQLite datetime('now')와 동일한 UTC "yyyy-MM-dd HH:mm:ss" 포맷
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return fmt.format(java.util.Date())
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
