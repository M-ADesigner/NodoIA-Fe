package com.radioauto.launcher.data

/** Default next oil change of the owner's i20 Active (km on the odometer). */
const val DEFAULT_NEXT_OIL_KM = 87_596

/** Typical gap between two oil changes; only used to draw the progress bar. */
const val OIL_INTERVAL_KM = 10_000

/** Km left until [nextKm]; negative once it is overdue. */
fun maintenanceDue(totalKm: Double, nextKm: Int): Int = Math.floor(nextKm - totalKm).toInt()

/** The warning to show when the oil change is close or overdue, or null while there is plenty of road left. */
fun maintenanceAlert(remainingKm: Int): String? = when {
    remainingKm <= 0 -> "Cambio de aceite vencido"
    remainingKm < 500 -> "Cambio de aceite en $remainingKm km"
    else -> null
}
