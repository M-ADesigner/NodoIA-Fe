package com.radioauto.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MaintenanceTest {
    @Test fun remaining() = assertEquals(3609, maintenanceDue(83_987.0, 87_596))
    @Test fun remainingRoundsDownPartialKm() = assertEquals(3608, maintenanceDue(83_987.4, 87_596))
    @Test fun overdue() = assertEquals(-4, maintenanceDue(87_600.0, 87_596))

    @Test fun noAlertFarAway() = assertNull(maintenanceAlert(3609))
    @Test fun alertNearby() = assertEquals("Cambio de aceite en 480 km", maintenanceAlert(480))
    @Test fun alertAtThreshold() = assertNull(maintenanceAlert(500))
    @Test fun alertOverdue() = assertEquals("Cambio de aceite vencido", maintenanceAlert(0))

    @Test fun gpsTripStats() {
        TrackingState.resetTrip()
        TrackingState.onFix(40, 10.0, 0.0, 0.0, null, null, nowMs = 1_000)
        TrackingState.onFix(80, 10.0, 0.0, 0.0, null, null, nowMs = 61_000)
        val s = TrackingState.state.value
        assertEquals(80, s.tripMaxKmh)
        assertEquals(60, s.avgKmh)
        assertEquals(60, s.tripSeconds)
    }
}
