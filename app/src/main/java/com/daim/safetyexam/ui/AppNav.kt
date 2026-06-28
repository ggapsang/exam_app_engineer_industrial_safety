package com.daim.safetyexam.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.daim.safetyexam.data.SettingsStore
import com.daim.safetyexam.data.StudyMode
import com.daim.safetyexam.ui.screens.*

object Routes {
    const val HOME = "home"
    const val EXAM_LIST = "exam_list"
    const val SUBJECT_SETUP = "subject_setup"
    const val QUIZ = "quiz"
    const val RESULT = "result"
    const val WRONG = "wrong"
    const val FAVORITE = "favorite"
    const val SEARCH = "search"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{qid}"
    fun detail(qid: Int) = "detail/$qid"
}

@Composable
fun AppNav(quizVm: QuizSessionViewModel, settings: SettingsStore) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                onExam = { nav.navigate(Routes.EXAM_LIST) },
                onSubject = { nav.navigate(Routes.SUBJECT_SETUP) },
                onMock = {
                    quizVm.startMock()
                    nav.navigate(Routes.QUIZ)
                },
                onWrong = { nav.navigate(Routes.WRONG) },
                onFavorite = { nav.navigate(Routes.FAVORITE) },
                onSearch = { nav.navigate(Routes.SEARCH) },
                onStats = { nav.navigate(Routes.STATS) },
                onSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.EXAM_LIST) {
            ExamListScreen(
                onBack = { nav.popBackStack() },
                onStartExam = { examId ->
                    quizVm.startExam(examId)
                    nav.navigate(Routes.QUIZ)
                }
            )
        }

        composable(Routes.SUBJECT_SETUP) {
            SubjectSetupScreen(
                onBack = { nav.popBackStack() },
                onStart = { subjectId, count, order ->
                    quizVm.startSubject(subjectId, count, order)
                    nav.navigate(Routes.QUIZ)
                }
            )
        }

        composable(Routes.QUIZ) {
            QuizScreen(
                vm = quizVm,
                onExit = { nav.popBackStack(Routes.HOME, inclusive = false) },
                onFinished = {
                    nav.navigate(Routes.RESULT) {
                        popUpTo(Routes.QUIZ) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.RESULT) {
            ResultScreen(
                vm = quizVm,
                onHome = { nav.popBackStack(Routes.HOME, inclusive = false) }
            )
        }

        composable(Routes.WRONG) {
            WrongNoteScreen(
                onBack = { nav.popBackStack() },
                onOpen = { qid -> nav.navigate(Routes.detail(qid)) },
                onPlayAll = { ids ->
                    quizVm.startFromIds(StudyMode.WRONG, ids)
                    nav.navigate(Routes.QUIZ)
                }
            )
        }

        composable(Routes.FAVORITE) {
            FavoriteScreen(
                onBack = { nav.popBackStack() },
                onOpen = { qid -> nav.navigate(Routes.detail(qid)) },
                onPlayAll = { ids ->
                    quizVm.startFromIds(StudyMode.FAVORITE, ids)
                    nav.navigate(Routes.QUIZ)
                }
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = { nav.popBackStack() },
                onOpen = { qid -> nav.navigate(Routes.detail(qid)) }
            )
        }

        composable(Routes.STATS) {
            StatsScreen(onBack = { nav.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() }, settings = settings)
        }

        composable(
            Routes.DETAIL,
            arguments = listOf(navArgument("qid") { type = NavType.IntType })
        ) { entry ->
            val qid = entry.arguments?.getInt("qid") ?: 0
            QuestionDetailScreen(questionId = qid, onBack = { nav.popBackStack() })
        }
    }
}
