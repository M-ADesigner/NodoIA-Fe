package com.radioauto.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Weather(val tempC: Int, val kind: Kind) {
    enum class Kind { CLEAR, CLOUDY, RAIN, SNOW, STORM }
}

/** Current weather from Open-Meteo (free, no account). One small request, made rarely. */
object WeatherRepository {

    suspend fun fetch(lat: Double, lon: Double): Weather? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weather_code"
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
            }
            try {
                val current = JSONObject(conn.inputStream.bufferedReader().use { it.readText() }).getJSONObject("current")
                Weather(
                    tempC = Math.round(current.getDouble("temperature_2m")).toInt(),
                    kind = kindOf(current.getInt("weather_code")),
                )
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    /** WMO weather codes grouped into the five icons we show. */
    fun kindOf(code: Int): Weather.Kind = when (code) {
        0, 1 -> Weather.Kind.CLEAR
        71, 73, 75, 77, 85, 86 -> Weather.Kind.SNOW
        95, 96, 99 -> Weather.Kind.STORM
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> Weather.Kind.RAIN
        else -> Weather.Kind.CLOUDY
    }
}
