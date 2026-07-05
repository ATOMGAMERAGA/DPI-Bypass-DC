package net.atom.dpibypass

import net.atom.dpibypass.isp.Isp
import net.atom.dpibypass.isp.IspFamily
import net.atom.dpibypass.strategy.StrategyPool
import net.atom.dpibypass.util.shellSplit
import org.junit.Assert.assertEquals
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
}
