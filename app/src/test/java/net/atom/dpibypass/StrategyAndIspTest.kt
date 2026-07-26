package net.atom.dpibypass

import net.atom.dpibypass.data.NetworkProfile
import net.atom.dpibypass.isp.Isp
import net.atom.dpibypass.isp.IspDetector
import net.atom.dpibypass.isp.IspFamily
import net.atom.dpibypass.isp.Transport
import net.atom.dpibypass.strategy.StrategyPool
import net.atom.dpibypass.util.shellSplit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrategyAndIspTest {

    @Test
    fun shellSplit_handlesQuotesAndSpaces() {
        assertEquals(listOf("--split", "1", "--fake", "-1"), shellSplit("--split 1 --fake -1"))
        assertEquals(listOf("a b", "c"), shellSplit("\"a b\" c"))
    }

    @Test
    fun strategy_toArgv_prependsCiadpiAndPort() {
        val argv = StrategyPool.P2.toArgv(1080).toList()
        assertEquals("ciadpi", argv[0])
        assertTrue(argv.containsAll(listOf("-p", "1080", "-i", "127.0.0.1")))
        assertTrue(argv.contains("--split"))
    }

    @Test
    fun isp_fromSimCode_mapsTurkishOperators() {
        assertEquals(Isp.TurkcellMobile, Isp.fromSimCode("28601"))
        assertEquals(Isp.VodafoneMobile, Isp.fromSimCode("28602"))
        assertEquals(IspFamily.TurkTelekom, Isp.fromSimCode("28603")?.family)
    }

    @Test
    fun isp_fromAsnOrg_prefersSpecificKeywords() {
        assertEquals(Isp.Superonline, Isp.fromAsnOrg("AS9121 Turkcell Superonline"))
        assertEquals(Isp.TurkNet, Isp.fromAsnOrg("AS12735 TurkNet Iletisim"))
        assertEquals(IspFamily.TurkTelekom, Isp.fromAsnOrg("AS9121 Turk Telekom")?.family)
    }

    @Test
    fun pool_orderedFor_containsAllSeven() {
        for (family in IspFamily.entries) {
            assertEquals(7, StrategyPool.orderedFor(family).size)
        }
    }

    /**
     * Yarış sırası, bu ağda daha önce kazanmış stratejiyi başa alır ama hiçbirini
     * DÜŞÜRMEZ: süre bütçesi dolmazsa havuzun tamamı yine ölçülür.
     */
    @Test
    fun pool_raceOrder_putsPreferredFirstWithoutLosingCandidates() {
        for (family in IspFamily.entries) {
            val order = StrategyPool.raceOrder(family, StrategyPool.P5)
            assertEquals(StrategyPool.P5, order.first())
            assertEquals(7, order.size)
            assertEquals(7, order.map { it.id }.toSet().size)
            assertEquals(StrategyPool.all.map { it.id }.toSet(), order.map { it.id }.toSet())
        }
    }

    @Test
    fun pool_raceOrder_withoutPreferred_keepsFamilyOrder() {
        for (family in IspFamily.entries) {
            assertEquals(StrategyPool.orderedFor(family), StrategyPool.raceOrder(family, null))
        }
    }

    /** Ağ profili tazeliği: gelecekteki ve pencere dışı damgalar taze sayılmaz. */
    @Test
    fun networkProfile_freshnessWindow() {
        val now = 1_000_000_000L
        val window = NetworkProfile.FRESH_WINDOW_MS
        assertTrue(NetworkProfile("P5", now - 1000).isFresh(now))
        assertTrue(NetworkProfile("P5", now - window).isFresh(now))
        assertFalse(NetworkProfile("P5", now - window - 1).isFresh(now))
        // Eski biçimden gelen "zamanı bilinmiyor" kaydı: bir kez yeniden yarıştırılır.
        assertFalse(NetworkProfile("P5", 0L).isFresh(now))
        // Saat geri alınmışsa ileri tarihli damga güvenilmez sayılır.
        assertFalse(NetworkProfile("P5", now + 5000).isFresh(now))
    }

    @Test
    fun networkKey_separatesTransportAndIsp() {
        val wifi = IspDetector.networkKey(Transport.WiFi, Isp.Superonline)
        val mobile = IspDetector.networkKey(Transport.Cellular, Isp.Superonline)
        assertTrue(wifi != mobile)
        assertEquals(wifi, IspDetector.networkKey(Transport.WiFi, Isp.Superonline))
    }
}
