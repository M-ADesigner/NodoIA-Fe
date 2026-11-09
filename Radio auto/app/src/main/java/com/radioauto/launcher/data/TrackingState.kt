package com.radioauto.launcher.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class Tracking(
    val kmh: Int? = null,
    val totalMeters: Double = 0.0,
    val tripMeters: Double = 0.0,
    val lat: Double? = null,
    val lon: Double? = null,
    /** Direction of travel in degrees, kept from the last time the car was moving. */
    val bearing: Float? = null,
    val satellites: Int? = null,
    // Trip numbers from the GPS, so they exist without an OBD adapter. They live for the session.
    val tripMaxKmh: Int = 0,
    val tripSpeedSum: Long = 0,
    val tripSamples: Int = 0,
    val tripStartMs: Long = 0,
    val lastFixMs: Long = 0,
) {
    val avgKmh: Int get() = if (tripSamples > 0) (tripSpeedSum / tripSamples).toInt() else 0
    val tripSeconds: Int get() = if (tripStartMs > 0) ((lastFixMs - tripStartMs) / 1000).toInt() else 0
}

/** Live speed, position and distances, written by [TrackingService] and shown by the home screen. */
object TrackingState {
    private val _state = MutableStateFlow(Tracking())
    val state: StateFlow<Tracking> = _state.asStateFlow()

    private var loaded = false

    suspend fun load(prefs: Prefs) {
        if (loaded) return
        val (total, trip) = prefs.loadTracking()
        val last = prefs.loadLastPosition()
        _state.update { it.copy(totalMeters = total, tripMeters = trip, lat = last?.first, lon = last?.second) }
        loaded = true
    }

    fun onFix(
        kmh: Int?, addedMeters: Double, lat: Double, lon: Double, bearing: Float?, satellites: Int?,
        nowMs: Long = System.currentTimeMillis(),
    ) =
        _state.update {
            it.copy(
                kmh = kmh,
                totalMeters = it.totalMeters + addedMeters,
                tripMeters = it.tripMeters + addedMeters,
                lat = lat,
                lon = lon,
                bearing = bearing ?: it.bearing,
                satellites = satellites ?: it.satellites,
                tripMaxKmh = maxOf(it.tripMaxKmh, kmh ?: 0),
                tripSpeedSum = it.tripSpeedSum + (kmh ?: 0),
                tripSamples = it.tripSamples + 1,
                tripStartMs = if (it.tripStartMs == 0L) nowMs else it.tripStartMs,
                lastFixMs = nowMs,
            )
        }

    fun setSatellites(count: Int) = _state.update { it.copy(satellites = count) }

    fun setTotalKm(km: Double) = _state.update { it.copy(totalMeters = km * 1000) }

    fun resetTrip() = _state.update {
        it.copy(tripMeters = 0.0, tripMaxKmh = 0, tripSpeedSum = 0, tripSamples = 0, tripStartMs = 0, lastFixMs = 0)
    }
}
