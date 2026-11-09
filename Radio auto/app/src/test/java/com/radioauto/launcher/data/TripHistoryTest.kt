package com.radioauto.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripHistoryTest {
    private fun trip(n: Int) = TripRecord(n * 1000L, 600, 12.5, 40, 90, 88, 12_400, 14_100)

    @Test fun keepsOnlyTheLast30NewestFirst() {
        var list = emptyList<TripRecord>()
        for (i in 1..35) list = pushTrip(list, trip(i))
        assertEquals(MAX_TRIPS, list.size)
        assertEquals(35_000L, list.first().startMs)
        assertEquals(6_000L, list.last().startMs)
    }

    @Test fun jsonRoundTrip() {
        val h = ObdHistory(listOf(trip(1), trip(2).copy(maxCoolantC = null, minVoltageMv = null)), listOf(DtcRecord("P0301", 5_000)))
        val back = historyFromJson(historyToJson(h))
        assertEquals(h, back)
        assertNull(back.trips[1].maxCoolantC)
    }

    @Test fun damagedFileStartsEmpty() {
        assertTrue(historyFromJson("{not json").trips.isEmpty())
        assertTrue(historyFromJson("").dtcs.isEmpty())
    }

    @Test fun dtcLogIsCapped() {
        var list = emptyList<DtcRecord>()
        for (i in 1..120) list = pushDtc(list, DtcRecord("P%04d".format(i), i.toLong()))
        assertEquals(MAX_DTCS, list.size)
        assertEquals(120L, list.first().atMs)
    }
}
