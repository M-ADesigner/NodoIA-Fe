package com.radioauto.launcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** One finished drive, kept on the device so the numbers survive a restart. */
data class TripRecord(
    val startMs: Long,
    val seconds: Int,
    val km: Double,
    val avgKmh: Int,
    val maxKmh: Int,
    val maxCoolantC: Int?,
    val minVoltageMv: Int?,
    val maxVoltageMv: Int?,
)

/** A fault code the first time it showed up. */
data class DtcRecord(val code: String, val atMs: Long)

data class ObdHistory(val trips: List<TripRecord> = emptyList(), val dtcs: List<DtcRecord> = emptyList())

const val MAX_TRIPS = 30
const val MAX_DTCS = 100

/** Newest first, capped: old trips fall off the end. */
fun pushTrip(trips: List<TripRecord>, trip: TripRecord, max: Int = MAX_TRIPS): List<TripRecord> = (listOf(trip) + trips).take(max)

fun pushDtc(dtcs: List<DtcRecord>, dtc: DtcRecord, max: Int = MAX_DTCS): List<DtcRecord> = (listOf(dtc) + dtcs).take(max)

fun historyToJson(h: ObdHistory): String = JSONObject().apply {
    put("trips", JSONArray().apply {
        h.trips.forEach {
            put(JSONObject().apply {
                put("start", it.startMs)
                put("seconds", it.seconds)
                put("km", it.km)
                put("avg", it.avgKmh)
                put("max", it.maxKmh)
                it.maxCoolantC?.let { v -> put("coolant", v) }
                it.minVoltageMv?.let { v -> put("vmin", v) }
                it.maxVoltageMv?.let { v -> put("vmax", v) }
            })
        }
    })
    put("dtcs", JSONArray().apply { h.dtcs.forEach { put(JSONObject().apply { put("code", it.code); put("at", it.atMs) }) } })
}.toString()

/** Tolerates a missing or damaged file: the history simply starts empty. */
fun historyFromJson(text: String): ObdHistory = runCatching {
    val root = JSONObject(text)
    val trips = root.optJSONArray("trips")?.let { a ->
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            TripRecord(
                o.getLong("start"), o.getInt("seconds"), o.getDouble("km"), o.getInt("avg"), o.getInt("max"),
                if (o.has("coolant")) o.getInt("coolant") else null,
                if (o.has("vmin")) o.getInt("vmin") else null,
                if (o.has("vmax")) o.getInt("vmax") else null,
            )
        }
    }.orEmpty()
    val dtcs = root.optJSONArray("dtcs")?.let { a ->
        (0 until a.length()).map { i -> a.getJSONObject(i).let { DtcRecord(it.getString("code"), it.getLong("at")) } }
    }.orEmpty()
    ObdHistory(trips, dtcs)
}.getOrDefault(ObdHistory())

/** The history file in the app's private folder: no storage permission needed. */
class TripStore(context: Context) {
    private val file = File(context.filesDir, "obd_history.json")

    fun load(): ObdHistory = if (file.exists()) historyFromJson(file.readText()) else ObdHistory()

    fun save(h: ObdHistory) {
        runCatching { file.writeText(historyToJson(h)) }
    }
}
