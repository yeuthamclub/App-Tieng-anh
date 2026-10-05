package com.yeuthamclub.tienganh.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yeuthamclub.tienganh.Lesson
import com.yeuthamclub.tienganh.Phrase
import com.yeuthamclub.tienganh.Progress
import com.yeuthamclub.tienganh.SoundDrill
import com.yeuthamclub.tienganh.Speaker

private val WEEK_NAMES = mapOf(
    1 to "Tuần 1 · Âm cơ bản + từ nền",
    2 to "Tuần 2 · Phonics + từ công việc",
    3 to "Tuần 3 · Giao tiếp công việc",
    4 to "Tuần 4 · Lớp training AU480/DxC 700 AU",
)

@Composable
fun App(lessons: List<Lesson>, progress: Progress, speaker: Speaker) {
    MaterialTheme(colorScheme = lightColorScheme()) {
        var completed by remember { mutableStateOf(progress.completedDays()) }
        var openDay by rememberSaveable { mutableStateOf<Int?>(null) }
        val today = Progress.currentDay(completed, lessons.size)
        val lesson = openDay?.let { d -> lessons.firstOrNull { it.day == d } }

        Scaffold { padding ->
            if (lesson == null) {
                HomeScreen(lessons, completed, today, padding) { openDay = it }
            } else {
                BackHandler { openDay = null }
                DayScreen(
                    lesson = lesson,
                    done = lesson.day in completed,
                    padding = padding,
                    speaker = speaker,
                    onBack = { openDay = null },
                    onToggleDone = {
                        progress.setDone(lesson.day, lesson.day !in completed)
                        completed = progress.completedDays()
                        if (lesson.day in completed) openDay = null
                    },
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    lessons: List<Lesson>,
    completed: Set<Int>,
    today: Int,
    padding: PaddingValues,
    onOpen: (Int) -> Unit,
) {
    val todayLesson = lessons.first { it.day == today }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column {
                Text("Tiếng Anh 30 ngày", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Đã xong ${completed.size}/${lessons.size} ngày", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.padding(4.dp))
                LinearProgressIndicator(
                    progress = { completed.size.toFloat() / lessons.size },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Bài học hôm nay · Ngày $today", style = MaterialTheme.typography.labelLarge)
                    Text(todayLesson.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(todayLesson.goal, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.padding(4.dp))
                    Button(onClick = { onOpen(today) }) { Text("Học ngay") }
                }
            }
        }
        lessons.groupBy { it.week }.forEach { (week, days) ->
            item {
                Text(
                    WEEK_NAMES[week] ?: "Tuần $week",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            items(days, key = { it.day }) { lesson ->
                DayRow(lesson, done = lesson.day in completed, isToday = lesson.day == today) { onOpen(lesson.day) }
            }
        }
    }
}

@Composable
private fun DayRow(lesson: Lesson, done: Boolean, isToday: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${lesson.day}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(36.dp))
            Text(lesson.title, modifier = Modifier.weight(1f))
            if (done) Icon(Icons.Filled.CheckCircle, contentDescription = "Đã xong", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun DayScreen(
    lesson: Lesson,
    done: Boolean,
    padding: PaddingValues,
    speaker: Speaker,
    onBack: () -> Unit,
    onToggleDone: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column {
                TextButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    Text(" Danh sách ngày")
                }
                Text("Ngày ${lesson.day}", style = MaterialTheme.typography.labelLarge)
                Text(lesson.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(lesson.goal, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (lesson.sounds.isNotEmpty()) {
            item { SectionTitle("1. Luyện âm (15 phút)", "Bấm từng từ để nghe. Nói theo to, rõ âm cuối.") }
            items(lesson.sounds) { SoundCard(it, speaker) }
        }
        if (lesson.words.isNotEmpty()) {
            item { SectionTitle("2. Từ vựng", "Nghe, nói theo 3 lần, che nghĩa rồi tự nhớ lại.") }
            items(lesson.words) { PhraseRow(it, speaker) }
        }
        if (lesson.sentences.isNotEmpty()) {
            item { SectionTitle("3. Câu mẫu", "Nghe chậm trước, sau đó nói theo tốc độ thường.") }
            items(lesson.sentences) { PhraseRow(it, speaker) }
        }
        item {
            Column(Modifier.padding(top = 16.dp)) {
                if (done) {
                    OutlinedButton(onClick = onToggleDone, modifier = Modifier.fillMaxWidth()) { Text("Bỏ đánh dấu hoàn thành") }
                } else {
                    Button(onClick = onToggleDone, modifier = Modifier.fillMaxWidth()) { Text("Hoàn thành ngày ${lesson.day}") }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, hint: String) {
    Column(Modifier.padding(top = 16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(hint, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SoundCard(sound: SoundDrill, speaker: Speaker) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(sound.ipa, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(sound.tip, style = MaterialTheme.typography.bodyMedium)
            sound.pairs.forEach { (a, b) ->
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = { speaker.say(a, slow = true) }, modifier = Modifier.weight(1f)) { Text(a) }
                    OutlinedButton(onClick = { speaker.say(b, slow = true) }, modifier = Modifier.weight(1f)) { Text(b) }
                }
            }
        }
    }
}

@Composable
private fun PhraseRow(phrase: Phrase, speaker: Speaker) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(phrase.en, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                if (phrase.vi.isNotBlank()) Text(phrase.vi, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { speaker.say(phrase.en, slow = true) }) {
                Text("chậm", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = { speaker.say(phrase.en) }) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Nghe")
            }
        }
    }
}
