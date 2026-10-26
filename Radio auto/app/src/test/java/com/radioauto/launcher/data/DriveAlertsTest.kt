package com.radioauto.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DriveAlertsTest {

    private fun DriveAlerts.at(sec: Int, kmh: Int, speed: Boolean = true, limit: Int = 100) =
        onSpeed(kmh, sec * 1000L, speed, limit)

    @Test fun speedAlertFiresOnceWhenCrossingTheLimit() {
        val a = DriveAlerts()
        a.at(0, 50)
        assertEquals(Alert.SLOW_DOWN, a.at(10, 105))
        assertNull(a.at(11, 108))
    }

    @Test fun speedAlertWaitsAMinuteBeforeRepeating() {
        val a = DriveAlerts()
        assertEquals(Alert.SLOW_DOWN, a.at(0, 105))
        a.at(10, 90)
        assertNull(a.at(20, 106)) // under a minute since the last warning
        a.at(30, 90)
        assertEquals(Alert.SLOW_DOWN, a.at(100, 106))
    }

    @Test fun speedAlertCanBeDisabled() {
        assertNull(DriveAlerts().at(0, 150, speed = false))
    }
}
