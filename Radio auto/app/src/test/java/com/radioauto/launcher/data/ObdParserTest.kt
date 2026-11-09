package com.radioauto.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObdParserTest {
    @Test fun rpm() = assertEquals(1726, parseObd(ObdPid.RPM, "41 0C 1A F8"))
    @Test fun rpmWithEchoAndNoSpaces() = assertEquals(1726, parseObd(ObdPid.RPM, "010C\r410C1AF8\r\r"))
    @Test fun rpmAfterSearching() = assertEquals(1726, parseObd(ObdPid.RPM, "SEARCHING...\r41 0C 1A F8"))
    @Test fun coolant() = assertEquals(83, parseObd(ObdPid.COOLANT, "41057B"))
    @Test fun throttle() = assertEquals(50, parseObd(ObdPid.THROTTLE, "41 11 80"))
    @Test fun fuel() = assertEquals(100, parseObd(ObdPid.FUEL, "41 2F FF"))
    @Test fun speed() = assertEquals(80, parseObd(ObdPid.SPEED, "41 0D 50"))
    @Test fun voltage() = assertEquals(15_000, parseObd(ObdPid.VOLTAGE_MV, "4142 3A98"))
    @Test fun noData() = assertNull(parseObd(ObdPid.FUEL, "NO DATA"))
    @Test fun unable() = assertNull(parseObd(ObdPid.RPM, "UNABLE TO CONNECT"))
    @Test fun otherPid() = assertNull(parseObd(ObdPid.RPM, "41 05 53"))
    @Test fun truncated() = assertNull(parseObd(ObdPid.RPM, "41 0C 1A"))

    @Test fun supportedBitmap() {
        // BE 1F A8 13: PID 01 (bit 0 of BE=1011 1110) is supported, 02 is not; 0C and 0D are.
        val s = parseSupported(0x00, "41 00 BE 1F A8 13")
        assertTrue(0x01 in s && 0x03 in s && 0x0C in s && 0x0D in s && 0x20 in s)
        assertTrue(0x02 !in s)
    }

    @Test fun dtcs() = assertEquals(listOf("P0301", "P0100"), parseDtcs("43 03 01 01 00 00 00", "43"))
    @Test fun dtcsCanWithCount() = assertEquals(listOf("P0301"), parseDtcs("43 01 03 01", "43"))
    @Test fun dtcsNone() = assertEquals(emptyList<String>(), parseDtcs("43 00 00 00 00 00 00", "43"))
    @Test fun dtcLetters() {
        assertEquals("C0123", decodeDtc(0x4123))
        assertEquals("B0123", decodeDtc(0x8123))
        assertEquals("U0123", decodeDtc(0xC123))
    }

    @Test fun alertHotEngine() {
        val a = obdAlerts(ObdData(connected = true, coolantC = 110, rpm = 2000, voltageMv = 14_000))
        assertEquals(listOf(ObdTone.ERROR), a.map { it.tone })
    }

    @Test fun alertLowChargeAndBattery() {
        assertEquals("Alternador cargando poco", obdAlerts(ObdData(connected = true, rpm = 900, voltageMv = 12_800)).single().text)
        assertEquals("Batería baja", obdAlerts(ObdData(connected = true, rpm = 0, voltageMv = 11_900)).single().text)
    }

    @Test fun noAlertsWhenDisconnected() = assertTrue(obdAlerts(ObdData(coolantC = 130)).isEmpty())
}
