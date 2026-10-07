package com.yeuthamclub.tienganh

import org.json.JSONArray
import org.json.JSONObject

data class SoundDrill(val ipa: String, val tip: String, val pairs: List<Pair<String, String>>)

data class Phrase(val en: String, val vi: String)

data class Lesson(
    val day: Int,
    val week: Int,
    val title: String,
    val goal: String,
    val sounds: List<SoundDrill>,
    val words: List<Phrase>,
    val sentences: List<Phrase>,
)

object Lessons {
    /** Parses the bundled lessons.json; days are returned sorted by day number. */
    fun parse(json: String): List<Lesson> {
        val days = JSONObject(json).getJSONArray("days")
        return (0 until days.length())
            .map { parseDay(days.getJSONObject(it)) }
            .sortedBy { it.day }
    }

    private fun parseDay(o: JSONObject) = Lesson(
        day = o.getInt("day"),
        week = o.getInt("week"),
        title = o.getString("title"),
        goal = o.getString("goal"),
        sounds = o.optJSONArray("sounds").objects().map { s ->
            SoundDrill(
                ipa = s.getString("ipa"),
                tip = s.getString("tip"),
                pairs = s.optJSONArray("pairs").arrays().map { it.getString(0) to it.getString(1) },
            )
        },
        words = o.optJSONArray("words").objects().map(::parsePhrase),
        sentences = o.optJSONArray("sentences").objects().map(::parsePhrase),
    )

    private fun parsePhrase(o: JSONObject) = Phrase(o.getString("en"), o.optString("vi"))

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }

    private fun JSONArray?.arrays(): List<JSONArray> =
        if (this == null) emptyList() else (0 until length()).map { getJSONArray(it) }
}
