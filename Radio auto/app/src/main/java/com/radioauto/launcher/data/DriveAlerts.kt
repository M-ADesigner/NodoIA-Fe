package com.radioauto.launcher.data

enum class Alert(val phrase: String) {
    SLOW_DOWN("Reduce la velocidad"),
}

/**
 * Decides when to speak: one speed warning when crossing the limit, at most once a minute.
 */
class DriveAlerts {
    private var overLimit = false
    private var lastSlowDownMs = -MIN_GAP_MS

    fun onSpeed(kmh: Int, nowMs: Long, speedAlert: Boolean, limit: Int): Alert? {
        if (!speedAlert) {
            overLimit = false
            return null
        }
        if (kmh > limit && !overLimit) {
            overLimit = true
            // At most one warning a minute, even if the speed hovers around the limit.
            if (nowMs - lastSlowDownMs >= MIN_GAP_MS) {
                lastSlowDownMs = nowMs
                return Alert.SLOW_DOWN
            }
        } else if (kmh <= limit - HYSTERESIS_KMH) {
            overLimit = false
        }
        return null
    }

    private companion object {
        const val MIN_GAP_MS = 60_000L
        const val HYSTERESIS_KMH = 5
    }
}
