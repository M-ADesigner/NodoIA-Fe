package com.radioauto.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TripTrackerTest {

    // 0.001 degrees of latitude is about 111 m
    private fun fix(lat: Double, accuracy: Float = 5f, speed: Float? = 15f, timeMs: Long = 0) =
        Fix(lat, -70.0, accuracy, speed, timeMs)

    @Test fun firstFixOnlyAnchorsThePosition() {
        assertEquals(0.0, TripTracker().onFix(fix(-33.000)), 0.0)
    }

    @Test fun movingFixesAddTheDistanceBetweenThem() {
        val t = TripTracker()
        t.onFix(fix(-33.000))
        assertEquals(111.0, t.onFix(fix(-33.001)), 1.0)
    }

    @Test fun parkedCarJitterIsIgnored() {
        val t = TripTracker()
        t.onFix(fix(-33.000, speed = 0f))
        assertEquals(0.0, t.onFix(fix(-33.0001, speed = 0.3f)), 0.0)
    }

    @Test fun inaccurateFixesAreIgnored() {
        val t = TripTracker()
        t.onFix(fix(-33.000))
        assertEquals(0.0, t.onFix(fix(-33.001, accuracy = 80f)), 0.0)
    }

    @Test fun glitchJumpsAreIgnored() {
        val t = TripTracker()
        t.onFix(fix(-33.000))
        assertEquals(0.0, t.onFix(fix(-33.100)), 0.0)
    }

    @Test fun speedIsWorkedOutWhenTheGpsDoesNotReportIt() {
        val t = TripTracker()
        t.onFix(fix(-33.000, speed = null, timeMs = 0))
        // 33 m in 1 s is about 33 m/s (120 km/h)
        assertEquals(33.0, t.onFix(fix(-33.0003, speed = null, timeMs = 1000)), 1.0)
        assertEquals(33f, t.speedMps, 1f)
    }

    @Test fun slowDriftWithoutReportedSpeedIsIgnored() {
        val t = TripTracker()
        t.onFix(fix(-33.000, speed = null, timeMs = 0))
        // 1 m in 1 s is walking pace or noise, below the 1.5 m/s threshold
        assertEquals(0.0, t.onFix(fix(-33.000009, speed = null, timeMs = 1000)), 0.0)
    }
}
