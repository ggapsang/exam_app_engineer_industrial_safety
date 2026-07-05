package com.daim.safetyexam.data

/** 6과목 마스터 */
data class Subject(
    val id: Int,
    val nameKo: String,
    val shortName: String,
    val sortOrder: Int
)

/** 시험 회차 */
data class Exam(
    val id: Int,
    val year: Int,
    val session: Int,
    val examDate: String?,
    val deliveryType: String
) {
    val title: String get() = "${year}년 ${session}회"
}

/** 보기 + 보기 이미지(있을 때) + 보기별 해설(note) */
data class Choice(
    val no: Int,
    val body: String,
    val imageAsset: String? = null,
    val note: String? = null
)

/**
 * 화면에 필요한 문항 전체 묶음.
 * imageAsset 값들은 "file:///android_asset/..." 로 즉시 로드 가능한 형태.
 */
data class QuestionFull(
    val id: Int,
    val examId: Int,
    val examTitle: String,
    val subjectId: Int,
    val subjectShort: String,
    val qNumber: Int,
    val stem: String,
    val answerNo: Int,
    val isDisputed: Boolean,
    val choices: List<Choice>,
    val explanation: String?,
    val referencesMd: String?,
    val stemImage: String?
)

/** 오답노트 목록 한 줄 */
data class WrongNoteItem(
    val questionId: Int,
    val subjectId: Int,
    val subjectShort: String,
    val examTitle: String,
    val qNumber: Int,
    val stemPreview: String,
    val wrongCount: Int,
    val isFavorite: Boolean
)

/** 검색 결과 한 줄 */
data class SearchHit(
    val questionId: Int,
    val examTitle: String,
    val qNumber: Int,
    val subjectShort: String,
    val preview: String
)

/** 풀이 결과 1건 */
data class AttemptRecord(
    val questionId: Int,
    val selectedNo: Int?,
    val isCorrect: Boolean,
    val elapsedMs: Long
)

/** 과목별 정답률 */
data class SubjectStat(
    val subjectId: Int,
    val shortName: String,
    val total: Int,
    val correct: Int
) {
    val accuracy: Float get() = if (total == 0) 0f else correct.toFloat() / total
}

/** 일자별 학습량 */
data class DailyStat(val date: String, val count: Int)

/** 대시보드 통계 묶음 */
data class StatsSummary(
    val totalAttempts: Int,
    val totalCorrect: Int,
    val subjectStats: List<SubjectStat>,
    val daily: List<DailyStat>,
    val streakDays: Int
) {
    val accuracy: Float get() = if (totalAttempts == 0) 0f else totalCorrect.toFloat() / totalAttempts
}

/** 학습 모드 */
enum class StudyMode { EXAM, SUBJECT, MOCK, WRONG, FAVORITE }

/** 모의고사/세션 결과 */
data class SessionResult(
    val mode: StudyMode,
    val total: Int,
    val correct: Int,
    val elapsedSec: Int,
    val perSubject: List<SubjectStat>
) {
    val score100: Int get() = if (total == 0) 0 else Math.round(correct * 100f / total)
    /** 과락 과목 = 40점 미만(문항이 있는 과목만 대상). 표시 점수(정수 %)와 동일 기준으로 판정. */
    val failedSubjects: List<SubjectStat> get() =
        perSubject.filter { it.total > 0 && (it.accuracy * 100).toInt() < SUBJECT_PASS_LINE }
    val hasSubjectFail: Boolean get() = failedSubjects.isNotEmpty()
    /** 합격 = 전체 평균 60점 이상 AND 과락 과목(40점 미만) 없음. 한 과목이라도 40 미만이면 불합격. */
    val passed: Boolean get() = score100 >= OVERALL_PASS_LINE && !hasSubjectFail

    companion object {
        const val OVERALL_PASS_LINE = 60   // 전체 합격선(평균)
        const val SUBJECT_PASS_LINE = 40   // 과목 과락선
    }
}
