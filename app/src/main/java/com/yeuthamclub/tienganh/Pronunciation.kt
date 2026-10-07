package com.yeuthamclub.tienganh

/** Result of comparing what Leo said (as the recognizer heard it) with the target text. */
data class SpeechCheck(
    val words: List<Pair<String, Boolean>>,
    val heard: String,
    val passed: Boolean,
) {
    val score: Int get() = if (words.isEmpty()) 0 else words.count { it.second } * 100 / words.size
}

object Pronunciation {
    private val NUMBERS = mapOf(
        "0" to "zero", "1" to "one", "2" to "two", "3" to "three", "4" to "four", "5" to "five",
        "6" to "six", "7" to "seven", "8" to "eight", "9" to "nine", "10" to "ten",
        "13" to "thirteen", "20" to "twenty", "30" to "thirty",
    )

    fun tokens(text: String): List<String> =
        text.lowercase()
            .replace('’', '\'')
            .replace(Regex("[^a-z0-9' ]"), " ")
            .split(' ')
            .filter { it.isNotBlank() }
            .map { NUMBERS[it] ?: it }

    /**
     * Marks each target word as said or missed, using the recognizer alternative that matches best.
     * A single word must match exactly; a sentence passes at 80% of its words.
     */
    fun check(target: String, alternatives: List<String>): SpeechCheck {
        val want = tokens(target)
        val best = alternatives.ifEmpty { listOf("") }
            .map { alt -> alt to matchWords(want, tokens(alt)) }
            .maxBy { (_, marks) -> marks.count { it } }
        val marks = best.second
        val matched = marks.count { it }
        val passed = want.isNotEmpty() && if (want.size == 1) matched == 1 else matched * 100 >= want.size * 80
        return SpeechCheck(want.zip(marks), best.first, passed)
    }

    /** Longest common subsequence: which target words appear, in order, in what was heard. */
    private fun matchWords(want: List<String>, heard: List<String>): List<Boolean> {
        val n = want.size
        val m = heard.size
        val lcs = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) for (j in m - 1 downTo 0) {
            lcs[i][j] = if (want[i] == heard[j]) lcs[i + 1][j + 1] + 1 else maxOf(lcs[i + 1][j], lcs[i][j + 1])
        }
        val marks = BooleanArray(n)
        var i = 0
        var j = 0
        while (i < n && j < m) {
            when {
                want[i] == heard[j] -> { marks[i] = true; i++; j++ }
                lcs[i + 1][j] >= lcs[i][j + 1] -> i++
                else -> j++
            }
        }
        return marks.toList()
    }
}
