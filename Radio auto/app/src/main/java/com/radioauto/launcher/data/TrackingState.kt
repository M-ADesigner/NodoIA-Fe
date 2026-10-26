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
)

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

    fun onFix(kmh: Int?, addedMeters: Double, lat: Double, lon: Double, bearing: Float?, satellites: Int?) =
        _state.update {
            it.copy(
                kmh = kmh,
                totalMeters = it.totalMeters + addedMeters,
                tripMeters = it.tripMeters + addedMeters,
                lat = lat,
                lon = lon,
                bearing = bearing ?: it.bearing,
                satellites = satellites ?: it.satellites,
            )
        }

    fun setSatellites(count: Int) = _state.update { it.copy(satellites = count) }

    fun setTotalKm(km: Double) = _state.update { it.copy(totalMeters = km * 1000) }

    fun resetTrip() = _state.update { it.copy(tripMeters = 0.0) }
}
