package com.radioauto.launcher.data

/** One OBD-II mode 01 parameter: its code, how many data bytes it has and how to turn them into a number. */
enum class ObdPid(val code: String, val bytes: Int, val decode: (IntArray) -> Int?) {
    RPM("0C", 2, { (it[0] * 256 + it[1]) / 4 }),
    SPEED("0D", 1, { it[0] }),
    COOLANT("05", 1, { it[0] - 40 }),
    INTAKE("0F", 1, { it[0] - 40 }),
    LOAD("04", 1, { it[0] * 100 / 255 }),
    THROTTLE("11", 1, { it[0] * 100 / 255 }),
    FUEL("2F", 1, { it[0] * 100 / 255 }),
    /** Control module voltage in millivolts, so it stays an Int. */
    VOLTAGE_MV("42", 2, { it[0] * 256 + it[1] }),
    RUNTIME_S("1F", 2, { it[0] * 256 + it[1] }),
    MIL_KM("21", 2, { it[0] * 256 + it[1] });

    val number: Int get() = code.toInt(16)
}

/** Keeps only lines that are pure hex (drops echoes of words, "SEARCHING...", "NO DATA"...), joined without spaces. */
private fun hexLines(response: String): List<String> =
    response.split('\r', '\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() && it.all { c -> c in '0'..'9' || c in 'A'..'F' || c in 'a'..'f' || c.isWhitespace() } }
        .map { line -> line.filter { !it.isWhitespace() }.uppercase() }

/** Reads a mode 01 answer such as "41 0C 1A F8"; null for "NO DATA", garbage, truncated or another PID. */
fun parseObd(pid: ObdPid, response: String): Int? {
    val header = "41" + pid.code
    val hex = hexLines(response).joinToString("")
    val at = hex.indexOf(header)
    if (at < 0) return null
    val data = hex.substring(at + header.length).chunked(2).filter { it.length == 2 }.take(pid.bytes).map { it.toInt(16) }
    if (data.size < pid.bytes) return null
    return pid.decode(data.toIntArray())
}

/** PIDs the car says it supports in the block starting after [base] (0x00, 0x20, 0x40...); empty if unanswered. */
fun parseSupported(base: Int, response: String): Set<Int> {
    val header = "41" + "%02X".format(base)
    val hex = hexLines(response).joinToString("")
    val at = hex.indexOf(header)
    if (at < 0) return emptySet()
    val bytes = hex.substring(at + header.length).chunked(2).filter { it.length == 2 }.take(4).map { it.toInt(16) }
    if (bytes.size < 4) return emptySet()
    return buildSet {
        for (i in 0 until 32) if (bytes[i / 8] shr (7 - i % 8) and 1 == 1) add(base + 1 + i)
    }
}

/** Fault codes from a mode 03 ("43") or mode 07 ("47") answer, e.g. "43 03 01 01 00 00 00" -> P0301, P0100. */
fun parseDtcs(response: String, mode: String): List<String> =
    hexLines(response).filter { it.startsWith(mode) }.flatMap { line ->
        var body = line.removePrefix(mode)
        // CAN adapters put a count byte first, which leaves an odd number of bytes.
        if (body.length % 4 == 2) body = body.drop(2)
        body.chunked(4).filter { it.length == 4 && it != "0000" }.map { decodeDtc(it.toInt(16)) }
    }.distinct()

fun decodeDtc(value: Int): String {
    val type = "PCBU"[(value shr 14) and 3]
    val digit = (value shr 12) and 3
    return "%c%d%03X".format(type, digit, value and 0xFFF)
}

/** Short Spanish meaning of the most common codes; unknown ones just show the code. */
fun dtcDescription(code: String): String = when {
    code == "P0300" -> "Fallas de encendido en varios cilindros"
    Regex("P030[1-4]").matches(code) -> "Falla de encendido en el cilindro ${code.last()}"
    code == "P0171" -> "Mezcla pobre (mucho aire)"
    code == "P0172" -> "Mezcla rica (mucha bencina)"
    code == "P0420" -> "Catalizador con baja eficiencia"
    Regex("P013[0-9]|P014[01]").matches(code) -> "Sensor de oxígeno"
    else -> "Revisa en el taller"
}

enum class ObdTone { WARNING, ERROR }

data class ObdAlert(val tone: ObdTone, val text: String)

/** What the driver should be told right now; pure so it can be tested without a car. */
fun obdAlerts(d: ObdData): List<ObdAlert> = buildList {
    if (!d.connected) return@buildList
    val engineOn = (d.rpm ?: 0) > 400
    d.coolantC?.let { if (it > 105) add(ObdAlert(ObdTone.ERROR, "Motor caliente: detente y revisa")) }
    d.voltageMv?.let { mv ->
        when {
            engineOn && mv < 13_200 -> add(ObdAlert(ObdTone.WARNING, "Alternador cargando poco"))
            engineOn && mv > 14_800 -> add(ObdAlert(ObdTone.WARNING, "Voltaje alto"))
            !engineOn && d.rpm != null && mv < 12_200 -> add(ObdAlert(ObdTone.WARNING, "Batería baja"))
        }
    }
    d.dtcs.firstOrNull()?.let { add(ObdAlert(ObdTone.WARNING, "Falla $it: ${dtcDescription(it)}")) }
    if (d.idleUnstable) add(ObdAlert(ObdTone.WARNING, "Ralentí inestable"))
}
