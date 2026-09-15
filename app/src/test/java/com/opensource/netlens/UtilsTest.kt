package com.opensource.netlens

import com.opensource.netlens.data.speed.PingEngine
import com.opensource.netlens.data.speed.SpeedTestEngine
import com.opensource.netlens.util.Formatters
import com.opensource.netlens.util.FreqChannel
import com.opensource.netlens.util.SecurityParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilsTest {

    @Test
    fun channel_from_24ghz_freq() {
        assertEquals(1, FreqChannel.channelOf(2412))
        assertEquals(6, FreqChannel.channelOf(2437))
        assertEquals(11, FreqChannel.channelOf(2462))
        assertEquals(14, FreqChannel.channelOf(2484))
    }

    @Test
    fun channel_from_5ghz_freq() {
        assertEquals(36, FreqChannel.channelOf(5180))
        assertEquals(149, FreqChannel.channelOf(5745))
    }

    @Test
    fun band_detection() {
        assertEquals(com.opensource.netlens.data.model.Band.BAND_24, FreqChannel.bandOf(2437))
        assertEquals(com.opensource.netlens.data.model.Band.BAND_5, FreqChannel.bandOf(5180))
        assertEquals(com.opensource.netlens.data.model.Band.BAND_6, FreqChannel.bandOf(6135))
    }

    @Test
    fun security_parse() {
        assertEquals(
            com.opensource.netlens.data.model.SecurityType.WPA2,
            SecurityParser.parse("[WPA2-PSK-CCMP][ESS]")
        )
        assertEquals(
            com.opensource.netlens.data.model.SecurityType.WPA3,
            SecurityParser.parse("[WPA3-SAE][ESS]")
        )
        assertEquals(
            com.opensource.netlens.data.model.SecurityType.OPEN,
            SecurityParser.parse("[ESS]")
        )
    }

    @Test
    fun score_bands() {
        val (s1, g1) = SpeedTestEngine.scoreOf(300.0, 100.0, 8.0, 1.0, 0.0)
        assertTrue(s1 >= 90)
        assertTrue(g1.startsWith("A"))

        val (s2, _) = SpeedTestEngine.scoreOf(2.0, 0.5, 200.0, 40.0, 12.0)
        assertTrue(s2 < 50)
    }

    @Test
    fun ping_parser() {
        val out = """
            PING 1.1.1.1 (1.1.1.1): 56 data bytes
            64 bytes from 1.1.1.1: icmp_seq=0 ttl=57 time=12.3 ms
            64 bytes from 1.1.1.1: icmp_seq=1 ttl=57 time=14.1 ms
            64 bytes from 1.1.1.1: icmp_seq=2 ttl=57 time=11.0 ms
            3 packets transmitted, 3 packets received, 0.0% packet loss
        """.trimIndent()
        val stats = PingEngine().parsePingOutput("1.1.1.1", 3, out)
        assertEquals(3, stats.received)
        assertEquals(0.0, stats.lossPercent, 0.01)
        assertEquals(12.3, stats.samplesMs[0], 0.01)
    }

    @Test
    fun mac_vendor_xiaomi() {
        assertEquals("Xiaomi", Formatters.macVendor("F0:B4:D2:11:22:33"))
    }

    @Test
    fun ip_conversion() {
        assertEquals("192.168.1.10", Formatters.intToIp(Formatters.ipToInt("192.168.1.10")))
    }
}
