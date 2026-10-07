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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeuthamclub.tienganh.Lesson

private val WEEKS = mapOf(
    1 to "Âm cơ bản + từ nền",
    2 to "Phonics + từ công việc",
    3 to "Giao tiếp công việc",
    4 to "Lớp training AU480/DxC 700 AU",
)
private val WEEK_COLORS = listOf(Duo.Green to Duo.GreenShade, Duo.Blue to Duo.BlueShade)

/** Zigzag offsets so the lesson nodes snake down the screen like the Duolingo path. */
private val ZIGZAG = listOf(0, 45, 70, 45, 0, -45, -70, -45)

@Composable
fun HomeScreen(
    lessons: List<Lesson>,
    completed: Set<Int>,
    today: Int,
    xp: Int,
    streak: Int,
    padding: PaddingValues,
    onOpen: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val byWeek = lessons.groupBy { it.week }
    // Row index of today's node: a spacer row, then each week's banner followed by its days.
    var todayRow = 1
    for ((_, days) in byWeek) {
        todayRow += 1
        val pos = days.indexOfFirst { it.day == today }
        if (pos >= 0) { todayRow += pos; break }
        todayRow += days.size
    }
    LaunchedEffect(today) { listState.scrollToItem((todayRow - 2).coerceAtLeast(0)) }

    Column(Modifier.fillMaxSize().padding(padding)) {
        TopStats(completed.size, lessons.size, streak, xp)
        HorizontalDivider(color = Duo.Gray, thickness = 2.dp)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Spacer(Modifier.height(0.dp)) }
            byWeek.forEach { (week, days) ->
                item(key = "week$week") { WeekBanner(week) }
                items(days, key = { it.day }) { lesson ->
                    val state = when {
                        lesson.day in completed -> NodeState.Done
                        lesson.day == today -> NodeState.Current
                        else -> NodeState.Locked
                    }
                    PathNode(lesson, state, ZIGZAG[(lesson.day - 1) % ZIGZAG.size]) { onOpen(lesson.day) }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun TopStats(done: Int, total: Int, streak: Int, xp: Int) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Stat("🇬🇧", "$done/$total", Duo.TextSoft)
        Stat("🔥", "$streak", if (streak > 0) Duo.Orange else Duo.GrayShade)
        Stat("⚡", "$xp XP", Duo.Gold)
    }
}

@Composable
private fun Stat(icon: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 20.sp)
        Text(" $value", color = color, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
    }
}

@Composable
private fun WeekBanner(week: Int) {
    val (color, shade) = WEEK_COLORS[(week - 1) % WEEK_COLORS.size]
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(shade),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(color)
                .padding(16.dp),
        ) {
            Text("TUẦN $week", color = Duo.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(WEEKS[week] ?: "", color = Duo.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }
    }
}

private enum class NodeState { Done, Current, Locked }

@Composable
private fun PathNode(lesson: Lesson, state: NodeState, xOffset: Int, onClick: () -> Unit) {
    val (color, shade) = when (state) {
        NodeState.Done -> Duo.Gold to Duo.GoldShade
        NodeState.Current -> Duo.Green to Duo.GreenShade
        NodeState.Locked -> Duo.Gray to Duo.GrayShade
    }
    Column(
        Modifier.offset(x = xOffset.dp).width(150.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state == NodeState.Current) {
            Text(
                "BẮT ĐẦU",
                color = Duo.Green,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Duo.White)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Spacer(Modifier.height(4.dp))
        }
        DuoCircle(
            size = 72.dp,
            color = color,
            shade = shade,
            onClick = onClick,
            enabled = state != NodeState.Locked,
        ) {
            val icon = when (state) {
                NodeState.Done -> Icons.Filled.Check
                NodeState.Current -> Icons.Filled.Star
                NodeState.Locked -> Icons.Filled.Lock
            }
            Icon(icon, contentDescription = null, tint = if (state == NodeState.Locked) Duo.GrayShade else Duo.White)
        }
        Text(
            "Ngày ${lesson.day} · ${lesson.title}",
            color = if (state == NodeState.Locked) Duo.TextSoft else Duo.Text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
