package com.yeuthamclub.tienganh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yeuthamclub.tienganh.ui.App

class MainActivity : ComponentActivity() {
    private lateinit var speaker: Speaker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        speaker = Speaker(this)
        val lessons = Lessons.parse(assets.open("lessons.json").bufferedReader().use { it.readText() })
        val progress = Progress(this)
        setContent { App(lessons, progress, speaker) }
    }

    override fun onDestroy() {
        speaker.shutdown()
        super.onDestroy()
    }
}
