package com.radioauto.launcher.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** [speedMps] is null when the GPS chip does not report speed (common in cheap head units). */
data class Fix(
    val lat: Double,
    val lon: Double,
    val accuracyM: Float,
    val speedMps: Float? = null,
    val timeMs: Long = 0,
)

/** Turns a stream of GPS fixes into metres driven, ignoring noise from a parked or badly located car. */
class TripTracker {
    private var last: Fix? = null

    /** Speed of the latest usable fix: reported by the GPS, or worked out from distance and time. */
    var speedMps = 0f
        private set

    /** Metres to add for this fix; 0 when the fix is ignored. */
    fun onFix(fix: Fix): Double {
        if (fix.accuracyM > MAX_ACCURACY_M) return 0.0
        val prev = last
        last = fix
        if (prev == null) return 0.0

        val d = distanceM(prev, fix)
        val dtS = (fix.timeMs - prev.timeMs) / 1000f
        speedMps = fix.speedMps ?: if (dtS > 0) (d / dtS).toFloat() else 0f

        // While stopped the position wanders a few metres, which must not count as distance.
        if (speedMps < MIN_SPEED_MPS) return 0.0
        // A jump this big is a GPS glitch, not driving (1 s fixes are ~30 m at 110 km/h).
        return if (d > MAX_JUMP_M) 0.0 else d
    }

    private fun distanceM(a: Fix, b: Fix): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_M * atan2(sqrt(h), sqrt(1 - h))
    }

    private companion object {
        const val MAX_ACCURACY_M = 25f
        const val MIN_SPEED_MPS = 1.5f
        const val MAX_JUMP_M = 1000.0
        const val EARTH_RADIUS_M = 6_371_000.0
    }
}
