package com.yeuthamclub.tienganh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PronunciationTest {
    @Test
    fun exactWordPasses() {
        assertTrue(Pronunciation.check("think", listOf("sink", "think")).passed)
    }

    @Test
    fun minimalPairMistakeFails() {
        val result = Pronunciation.check("think", listOf("sink", "zinc"))
        assertFalse(result.passed)
        assertEquals("sink", result.heard)
    }

    @Test
    fun sentenceIgnoresCaseAndPunctuation() {
        assertTrue(Pronunciation.check("Could you repeat that slowly?", listOf("could you repeat that slowly")).passed)
    }

    @Test
    fun sentenceMarksMissedWords() {
        val result = Pronunciation.check("I need to check it.", listOf("I need to chat"))
        assertEquals(listOf(true, true, true, false, false), result.words.map { it.second })
        assertFalse(result.passed)
        assertEquals(60, result.score)
    }

    @Test
    fun digitsCountAsNumberWords() {
        assertTrue(Pronunciation.check("eight", listOf("8")).passed)
    }

    @Test
    fun noResultFails() {
        assertFalse(Pronunciation.check("hello", emptyList()).passed)
    }
}
