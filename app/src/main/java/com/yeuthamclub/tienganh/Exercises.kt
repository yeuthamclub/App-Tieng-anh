package com.yeuthamclub.tienganh

import kotlin.random.Random

/** One step of a lesson, shown one at a time like Duolingo. */
sealed class Exercise {
    /** Hear one word of a minimal pair and pick which one it was. */
    data class ListenChoose(val answer: String, val options: List<String>, val sound: String, val tip: String) : Exercise()

    /** Pick the Vietnamese meaning of an English word. */
    data class Meaning(val en: String, val answer: String, val options: List<String>) : Exercise()

    /** Say a word or sentence aloud; the phone's speech recognizer checks it. */
    data class Speak(
        val text: String,
        val vi: String,
        val sound: String? = null,
        val tip: String? = null,
        val confusable: String? = null,
    ) : Exercise()
}

object Exercises {
    private const val MAX_MEANING = 4
    private const val MAX_SPOKEN_WORDS = 3
    private const val MAX_SENTENCES = 3

    /** Builds the same exercise list for a given day every time (seeded by day number). */
    fun forLesson(lesson: Lesson): List<Exercise> {
        val random = Random(lesson.day)
        val out = mutableListOf<Exercise>()

        lesson.sounds.forEach { sound ->
            sound.pairs.take(2).forEach { (a, b) ->
                val answer = if (random.nextBoolean()) a else b
                out += Exercise.ListenChoose(answer, listOf(a, b).shuffled(random), sound.ipa, sound.tip)
            }
            sound.pairs.firstOrNull()?.let { (a, b) ->
                out += Exercise.Speak(a, "", sound.ipa, sound.tip, confusable = b)
            }
        }

        val words = lesson.words.filter { it.vi.isNotBlank() }
        if (words.size >= 3) {
            words.shuffled(random).take(MAX_MEANING).forEach { word ->
                val wrong = words.map { it.vi }.distinct().filter { it != word.vi }.shuffled(random).take(2)
                out += Exercise.Meaning(word.en, word.vi, (wrong + word.vi).shuffled(random))
            }
        }
        lesson.words.shuffled(random).take(MAX_SPOKEN_WORDS).forEach { out += Exercise.Speak(it.en, it.vi) }
        lesson.sentences.take(MAX_SENTENCES).forEach { out += Exercise.Speak(it.en, it.vi) }
        return out
    }
}
