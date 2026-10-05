package com.yeuthamclub.tienganh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LessonsTest {
    private val lessons = Lessons.parse(File("src/main/assets/lessons.json").readText())

    @Test
    fun bundledLessonsCoverThirtyDays() {
        assertEquals((1..30).toList(), lessons.map { it.day })
        assertEquals(setOf(1, 2, 3, 4), lessons.map { it.week }.toSet())
        assertTrue(lessons.all { it.title.isNotBlank() && it.goal.isNotBlank() })
    }

    @Test
    fun weekOneHasSoundDrillsAndWords() {
        lessons.filter { it.week == 1 }.forEach { day ->
            assertTrue("day ${day.day} has no sound drills", day.sounds.isNotEmpty())
            assertTrue("day ${day.day} has no words", day.words.isNotEmpty())
        }
    }

    @Test
    fun currentDayIsFirstUnfinishedDay() {
        assertEquals(1, Progress.currentDay(emptySet(), 30))
        assertEquals(3, Progress.currentDay(setOf(1, 2, 4), 30))
        assertEquals(30, Progress.currentDay((1..30).toSet(), 30))
    }
}
