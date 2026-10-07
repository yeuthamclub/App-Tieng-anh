package com.yeuthamclub.tienganh

import android.content.Context
import java.time.LocalDate

/** Which days Leo has finished, XP and streak, stored on the phone. */
class Progress(context: Context) {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    fun completedDays(): Set<Int> =
        prefs.getStringSet(KEY_DONE, emptySet())!!.mapNotNull { it.toIntOrNull() }.toSet()

    fun xp(): Int = prefs.getInt(KEY_XP, 0)

    fun streak(today: Long = LocalDate.now().toEpochDay()): Int =
        visibleStreak(prefs.getInt(KEY_STREAK, 0), prefs.getLong(KEY_LAST, NEVER), today)

    /** Records a finished lesson: marks the day, adds XP and extends the streak. */
    fun finishLesson(day: Int, earnedXp: Int, today: Long = LocalDate.now().toEpochDay()) {
        val days = completedDays() + day
        val newStreak = nextStreak(prefs.getInt(KEY_STREAK, 0), prefs.getLong(KEY_LAST, NEVER), today)
        prefs.edit()
            .putStringSet(KEY_DONE, days.map { it.toString() }.toSet())
            .putInt(KEY_XP, xp() + earnedXp)
            .putInt(KEY_STREAK, newStreak)
            .putLong(KEY_LAST, today)
            .apply()
    }

    companion object {
        private const val KEY_DONE = "completed_days"
        private const val KEY_XP = "xp"
        private const val KEY_STREAK = "streak"
        private const val KEY_LAST = "last_study_day"
        private const val NEVER = Long.MIN_VALUE / 2

        /** Today's lesson is the first day not yet finished. */
        fun currentDay(completed: Set<Int>, totalDays: Int): Int =
            (1..totalDays).firstOrNull { it !in completed } ?: totalDays

        fun nextStreak(streak: Int, lastDay: Long, today: Long): Int = when (lastDay) {
            today -> maxOf(streak, 1)
            today - 1 -> streak + 1
            else -> 1
        }

        /** A streak is broken once a whole day has passed without study. */
        fun visibleStreak(streak: Int, lastDay: Long, today: Long): Int =
            if (lastDay >= today - 1) streak else 0
    }
}
