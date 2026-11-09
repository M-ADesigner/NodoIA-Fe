package com.radioauto.launcher.link

import android.content.Context

/** Phones the driver already approved once; they come back with their token and join without a code. */
class TrustedPhones(context: Context) {
    private val prefs = context.getSharedPreferences("trusted_phones", Context.MODE_PRIVATE)

    /** Lets a phone tell this radio apart from other cars it knows. */
    val carId: String = prefs.getString(KEY_CAR_ID, null)
        ?: SessionCode.newResumeToken().also { prefs.edit().putString(KEY_CAR_ID, it).apply() }

    fun isTrusted(token: String): Boolean = token.isNotEmpty() && prefs.contains(PREFIX + token)

    /** Returns the token the phone must keep. */
    fun trust(deviceName: String): String =
        SessionCode.newResumeToken().also { prefs.edit().putString(PREFIX + it, deviceName).apply() }

    fun forget(token: String) {
        prefs.edit().remove(PREFIX + token).apply()
    }

    private companion object {
        const val KEY_CAR_ID = "car_id"
        const val PREFIX = "phone_"
    }
}
