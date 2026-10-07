package com.yeuthamclub.tienganh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExercisesTest {
    private val lessons = Lessons.parse(File("src/main/assets/lessons.json").readText())

    @Test
    fun everyDayHasExercises() {
        lessons.forEach { assertTrue("day ${it.day} has no exercises", Exercises.forLesson(it).isNotEmpty()) }
    }

    @Test
    fun exercisesAreStableForADay() {
        val day1 = lessons.first { it.day == 1 }
        assertEquals(Exercises.forLesson(day1), Exercises.forLesson(day1))
    }

    @Test
    fun choiceAnswersAreAmongOptions() {
        lessons.flatMap { Exercises.forLesson(it) }.forEach { e ->
            when (e) {
                is Exercise.ListenChoose -> assertTrue(e.answer in e.options)
                is Exercise.Meaning -> {
                    assertTrue(e.answer in e.options)
                    assertEquals(e.options.size, e.options.toSet().size)
                }
                is Exercise.Speak -> assertFalse(e.text.isBlank())
            }
        }
    }
}
