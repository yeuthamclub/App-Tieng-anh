package com.yeuthamclub.tienganh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeuthamclub.tienganh.Exercise
import com.yeuthamclub.tienganh.Lesson
import com.yeuthamclub.tienganh.Pronunciation
import com.yeuthamclub.tienganh.Recognizer
import com.yeuthamclub.tienganh.SpeechCheck
import com.yeuthamclub.tienganh.Speaker

private data class Feedback(val correct: Boolean, val title: String, val detail: String)

/** One lesson as a queue of exercises; a missed exercise comes back once at the end, like Duolingo. */
@Composable
fun LessonScreen(
    lesson: Lesson,
    exercises: List<Exercise>,
    speaker: Speaker,
    recognizer: Recognizer,
    hasMicPermission: Boolean,
    requestMic: () -> Unit,
    padding: PaddingValues,
    onClose: () -> Unit,
    onFinish: (xp: Int, accuracy: Int) -> Unit,
) {
    val total = exercises.size
    var queue by remember(lesson.day) { mutableStateOf(exercises.indices.map { it to exercises[it] }) }
    var index by remember(lesson.day) { mutableIntStateOf(0) }
    var done by remember(lesson.day) { mutableStateOf(setOf<Int>()) }
    var retried by remember(lesson.day) { mutableStateOf(setOf<Int>()) }
    var firstTryRight by remember(lesson.day) { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<Feedback?>(null) }
    var speech by remember { mutableStateOf<SpeechCheck?>(null) }
    var listening by remember { mutableStateOf(false) }
    var micError by remember { mutableStateOf<String?>(null) }

    if (total == 0 || index >= queue.size) {
        val accuracy = if (total == 0) 100 else firstTryRight * 100 / total
        FinishedScreen(padding, xp = 10 + firstTryRight, accuracy = accuracy) { onFinish(10 + firstTryRight, accuracy) }
        return
    }

    val (id, exercise) = queue[index]

    LaunchedEffect(index) {
        when (exercise) {
            is Exercise.ListenChoose -> speaker.say(exercise.answer, slow = true)
            is Exercise.Speak -> speaker.say(exercise.text)
            is Exercise.Meaning -> speaker.say(exercise.en)
        }
    }

    fun answer(correct: Boolean, title: String, detail: String) {
        feedback = Feedback(correct, title, detail)
        if (correct && id !in retried) firstTryRight++
    }

    fun next(skipped: Boolean = false) {
        val wasCorrect = feedback?.correct == true
        if (!wasCorrect && !skipped && id !in retried) {
            retried = retried + id
            queue = queue + (id to exercise)
        } else {
            done = done + id
        }
        selected = null
        feedback = null
        speech = null
        micError = null
        recognizer.stop()
        listening = false
        index++
    }

    fun check() {
        when (exercise) {
            is Exercise.ListenChoose -> {
                val ok = selected == exercise.answer
                answer(ok, if (ok) "Chính xác!" else "Chưa đúng", if (ok) "" else "Đáp án: ${exercise.answer}. ${exercise.sound}: ${exercise.tip}")
            }
            is Exercise.Meaning -> {
                val ok = selected == exercise.answer
                answer(ok, if (ok) "Chính xác!" else "Chưa đúng", if (ok) "" else "Đáp án: ${exercise.answer}")
            }
            is Exercise.Speak -> Unit
        }
    }

    fun listen(target: Exercise.Speak) {
        if (!hasMicPermission) { requestMic(); return }
        if (!recognizer.available) {
            micError = "Máy chưa có dịch vụ nhận dạng giọng nói. Cài hoặc cập nhật app Google rồi thử lại."
            return
        }
        speaker.stop()
        micError = null
        listening = true
        recognizer.listen(
            onResult = { alternatives ->
                listening = false
                val result = Pronunciation.check(target.text, alternatives)
                speech = result
                val confused = target.confusable != null &&
                    target.confusable.lowercase() in Pronunciation.tokens(result.heard)
                when {
                    result.passed -> answer(true, "Phát âm tốt! ${result.score}%", "")
                    confused -> answer(false, "Máy nghe thành “${target.confusable}”", "${target.sound}: ${target.tip}")
                    else -> answer(
                        false,
                        "Chưa rõ, đạt ${result.score}%",
                        "Máy nghe: “${result.heard.ifBlank { "…" }}”. Nói chậm lại và giữ rõ âm cuối.",
                    )
                }
            },
            onError = { message ->
                listening = false
                micError = message
            },
        )
    }

    Column(Modifier.fillMaxSize().padding(padding)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { recognizer.stop(); onClose() }) {
                Icon(Icons.Filled.Close, contentDescription = "Thoát", tint = Duo.TextSoft)
            }
            DuoProgress(done.size.toFloat() / total, Modifier.weight(1f).padding(end = 12.dp))
        }

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (id in retried) Text("LÀM LẠI CÂU SAI", color = Duo.Orange, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            when (exercise) {
                is Exercise.ListenChoose -> {
                    Title("Nghe và chọn từ đúng")
                    SpeakerButton { speaker.say(exercise.answer, slow = true) }
                    Text("Luyện âm ${exercise.sound}", color = Duo.TextSoft)
                    exercise.options.forEach { option ->
                        OptionCard(option, optionState(option, selected, exercise.answer, feedback != null)) {
                            if (feedback == null) { selected = option; speaker.say(option, slow = true) }
                        }
                    }
                }
                is Exercise.Meaning -> {
                    Title("Chọn nghĩa đúng")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SpeakerButton { speaker.say(exercise.en) }
                        Spacer(Modifier.width(16.dp))
                        Text(exercise.en, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Duo.Text)
                    }
                    exercise.options.forEach { option ->
                        OptionCard(option, optionState(option, selected, exercise.answer, feedback != null)) {
                            if (feedback == null) selected = option
                        }
                    }
                }
                is Exercise.Speak -> {
                    Title("Đọc to")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SpeakerButton { speaker.say(exercise.text, slow = true) }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            SpokenText(exercise.text, speech)
                            if (exercise.vi.isNotBlank()) Text(exercise.vi, color = Duo.TextSoft)
                        }
                    }
                    exercise.sound?.let { Text("Chú ý âm $it: ${exercise.tip}", color = Duo.TextSoft, fontSize = 14.sp) }
                    Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        DuoCircle(
                            size = 96.dp,
                            color = if (listening) Duo.Red else Duo.Blue,
                            shade = if (listening) Duo.RedShade else Duo.BlueShade,
                            onClick = { if (!listening) listen(exercise) },
                            enabled = feedback == null,
                        ) { Text("🎤", fontSize = 40.sp) }
                        Text(
                            if (listening) "Đang nghe… nói ngay" else "Bấm micro rồi nói",
                            color = if (listening) Duo.Red else Duo.TextSoft,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        micError?.let { Text(it, color = Duo.RedShade, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp)) }
                    }
                }
            }
        }

        BottomBar(
            feedback = feedback,
            isSpeak = exercise is Exercise.Speak,
            canCheck = selected != null,
            onCheck = ::check,
            onSkip = { next(skipped = true) },
            onContinue = { next() },
        )
    }
}

private fun optionState(option: String, selected: String?, answer: String, checked: Boolean) = when {
    checked && option == answer -> OptionState.Right
    checked && option == selected -> OptionState.Wrong
    option == selected -> OptionState.Selected
    else -> OptionState.Idle
}

@Composable
private fun Title(text: String) {
    Text(text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Duo.Text)
}

@Composable
private fun SpeakerButton(onClick: () -> Unit) {
    DuoCircle(size = 64.dp, color = Duo.Blue, shade = Duo.BlueShade, onClick = onClick) { Text("🔊", fontSize = 26.sp) }
}

/** The target text; after an attempt, words heard correctly turn green and missed ones red. */
@Composable
private fun SpokenText(text: String, speech: SpeechCheck?) {
    if (speech == null) {
        Text(text, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Duo.Text)
        return
    }
    Text(
        buildAnnotatedString {
            speech.words.forEachIndexed { i, (word, ok) ->
                if (i > 0) append(" ")
                withStyle(SpanStyle(color = if (ok) Duo.GreenShade else Duo.RedShade)) { append(word) }
            }
        },
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun BottomBar(
    feedback: Feedback?,
    isSpeak: Boolean,
    canCheck: Boolean,
    onCheck: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
) {
    if (feedback == null) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            if (isSpeak) {
                TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                    Text("KHÔNG NÓI ĐƯỢC LÚC NÀY", color = Duo.TextSoft, fontWeight = FontWeight.Bold)
                }
            } else {
                DuoButton("Kiểm tra", onCheck, enabled = canCheck)
            }
        }
        return
    }
    val bg = if (feedback.correct) Duo.GreenLight else Duo.RedLight
    val fg = if (feedback.correct) Duo.GreenShade else Duo.RedShade
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(bg)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(feedback.title, color = fg, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        if (feedback.detail.isNotBlank()) Text(feedback.detail, color = fg)
        Spacer(Modifier.height(4.dp))
        DuoButton(
            if (feedback.correct) "Tiếp tục" else "Đã hiểu",
            onContinue,
            color = if (feedback.correct) Duo.Green else Duo.Red,
            shade = if (feedback.correct) Duo.GreenShade else Duo.RedShade,
        )
    }
}

@Composable
private fun FinishedScreen(padding: PaddingValues, xp: Int, accuracy: Int, onContinue: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(padding).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🎉", fontSize = 72.sp)
        Text("Hoàn thành bài học!", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Duo.Gold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard("TỔNG XP", "⚡ $xp", Duo.Gold, Modifier.weight(1f))
            StatCard("CHÍNH XÁC", "🎯 $accuracy%", Duo.Green, Modifier.weight(1f))
        }
        Spacer(Modifier.height(32.dp))
        DuoButton("Tiếp tục", onContinue)
    }
}

@Composable
private fun StatCard(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(color)) {
        Column(Modifier.fillMaxWidth().padding(2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Duo.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, modifier = Modifier.padding(4.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Duo.White).padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Text(value, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) }
        }
    }
}
