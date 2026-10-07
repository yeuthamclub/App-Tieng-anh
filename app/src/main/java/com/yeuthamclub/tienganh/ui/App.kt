package com.yeuthamclub.tienganh.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.yeuthamclub.tienganh.Exercises
import com.yeuthamclub.tienganh.Lesson
import com.yeuthamclub.tienganh.Progress
import com.yeuthamclub.tienganh.Recognizer
import com.yeuthamclub.tienganh.Speaker

@Composable
fun App(lessons: List<Lesson>, progress: Progress, speaker: Speaker, recognizer: Recognizer) {
    DuoTheme {
        val context = LocalContext.current
        var completed by remember { mutableStateOf(progress.completedDays()) }
        var xp by remember { mutableIntStateOf(progress.xp()) }
        var streak by remember { mutableIntStateOf(progress.streak()) }
        var openDay by rememberSaveable { mutableStateOf<Int?>(null) }
        var hasMic by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
            )
        }
        val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasMic = it }
        val today = Progress.currentDay(completed, lessons.size)
        val lesson = openDay?.let { d -> lessons.firstOrNull { it.day == d } }

        Scaffold(containerColor = Duo.White) { padding ->
            if (lesson == null) {
                HomeScreen(lessons, completed, today, xp, streak, padding) { openDay = it }
            } else {
                val close = { recognizer.stop(); openDay = null }
                BackHandler(onBack = close)
                LessonScreen(
                    lesson = lesson,
                    exercises = remember(lesson.day) { Exercises.forLesson(lesson) },
                    speaker = speaker,
                    recognizer = recognizer,
                    hasMicPermission = hasMic,
                    requestMic = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    padding = padding,
                    onClose = close,
                    onFinish = { earned, _ ->
                        progress.finishLesson(lesson.day, earned)
                        completed = progress.completedDays()
                        xp = progress.xp()
                        streak = progress.streak()
                        openDay = null
                    },
                )
            }
        }
    }
}
