package com.yeuthamclub.tienganh

import android.content.Context

/** Which days Leo has finished, stored on the phone. */
class Progress(context: Context) {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    fun completedDays(): Set<Int> =
        prefs.getStringSet(KEY_DONE, emptySet())!!.mapNotNull { it.toIntOrNull() }.toSet()

    fun setDone(day: Int, done: Boolean) {
        val days = completedDays().toMutableSet()
        if (done) days.add(day) else days.remove(day)
        prefs.edit().putStringSet(KEY_DONE, days.map { it.toString() }.toSet()).apply()
    }

    companion object {
        private const val KEY_DONE = "completed_days"

        /** Today's lesson is the first day not yet finished. */
        fun currentDay(completed: Set<Int>, totalDays: Int): Int =
            (1..totalDays).firstOrNull { it !in completed } ?: totalDays
    }
}
