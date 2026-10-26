package com.radioauto.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PositionTest {

    @Test
    fun advancesWhilePlaying() {
        assertEquals(13_000, extrapolatePosition(10_000, 1_000, 4_000, 1f, true, 200_000))
    }

    @Test
    fun staysPutWhenPaused() {
        assertEquals(10_000, extrapolatePosition(10_000, 1_000, 4_000, 1f, false, 200_000))
    }

    @Test
    fun neverPassesTheDuration() {
        assertEquals(200_000, extrapolatePosition(199_000, 1_000, 9_000, 1f, true, 200_000))
    }

    @Test
    fun unknownPositionIsZero() {
        assertEquals(0, extrapolatePosition(-1, 1_000, 4_000, 1f, true, 200_000))
    }
}
