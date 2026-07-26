package net.atom.dpibypass

import net.atom.dpibypass.isp.Transport
import net.atom.dpibypass.util.AppUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ayarlar → Bilgi bölümündeki biçimlendiriciler.
 *
 * Ondalık ayıracı cihazın diline göre değiştiği için (34.2 / 34,2) testler tam
 * dizge yerine BİRİM ve sınır davranışını doğrular; yoksa test, çalıştığı
 * makinenin diline bağımlı olurdu.
 */
class UsageFormatTest {

    @Test
    fun formatBytes_usesExpectedUnits() {
        assertEquals("0 B", AppUsage.formatBytes(0))
        assertEquals("1023 B", AppUsage.formatBytes(1023))
        assertTrue(AppUsage.formatBytes(2048).endsWith(" KB"))
        assertTrue(AppUsage.formatBytes(5L * 1024 * 1024).endsWith(" MB"))
        assertTrue(AppUsage.formatBytes(3L * 1024 * 1024 * 1024).endsWith(" GB"))
    }

    @Test
    fun formatBytes_unsupportedCounterShowsDash() {
        assertEquals("—", AppUsage.formatBytes(-1))
    }

    @Test
    fun formatDuration_switchesScale() {
        assertTrue(AppUsage.formatDuration(4_200).endsWith(" sn"))
        assertEquals("2 dk 5 sn", AppUsage.formatDuration(125_000))
        assertEquals("1 sa 2 dk", AppUsage.formatDuration(3_725_000))
        assertEquals("—", AppUsage.formatDuration(-1))
    }

    @Test
    fun traffic_totalAndSupportFlag() {
        val ok = AppUsage.Traffic(rxBytes = 100, txBytes = 40)
        assertEquals(140L, ok.totalBytes)
        assertTrue(ok.supported)

        val unsupported = AppUsage.Traffic(rxBytes = -1, txBytes = -1)
        assertTrue(!unsupported.supported)
    }

    @Test
    fun transport_hasTurkishLabels() {
        assertEquals("Wi-Fi", Transport.WiFi.displayName)
        assertEquals("Mobil veri", Transport.Cellular.displayName)
        assertEquals("Kablolu", Transport.Ethernet.displayName)
        assertEquals("Bilinmiyor", Transport.Unknown.displayName)
    }
}
