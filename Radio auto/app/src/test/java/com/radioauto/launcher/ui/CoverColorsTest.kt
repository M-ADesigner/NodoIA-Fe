package com.radioauto.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverColorsTest {

    @Test
    fun sameSongGivesSameHues() {
        assertEquals(coverHues("Song|Artist"), coverHues("Song|Artist"))
    }

    @Test
    fun alwaysTwoOrThreeValidHues() {
        listOf("", "a", "Bohemian Rhapsody|Queen", "ñandú|Ñ", "x".repeat(500)).forEach {
            val hues = coverHues(it)
            assertTrue(hues.size in 2..3)
            hues.forEach { h -> assertTrue(h in 0f..360f) }
        }
    }

    @Test
    fun differentSongsUsuallyDiffer() {
        val distinct = (1..50).map { coverHues("song $it") }.toSet()
        assertTrue(distinct.size > 25)
    }
}
