package net.atom.dpibypass.strategy

import net.atom.dpibypass.isp.IspFamily
import net.atom.dpibypass.util.shellSplit

/**
 * Tek bir DPI aşım stratejisi = bir ByeDPI komut satırı.
 *
 * @param id     kısa kimlik (P1..P7 ya da "CUSTOM")
 * @param name   kullanıcıya görünen ad
 * @param note   kısa açıklama
 * @param args   ByeDPI argümanları (ör. "--split 2 --disorder 3 --fake -1 --ttl 5")
 */
data class Strategy(
    val id: String,
    val name: String,
    val note: String,
    val args: String,
) {
    /**
     * ByeDPI'a verilecek argv'yi üretir. Yerel loopback'te belirtilen portta dinler.
     * @param port SOCKS5 dinleme portu (ana tünel için 1080, testte geçici port).
     */
    fun toArgv(port: Int): Array<String> =
        (listOf("ciadpi", "-i", "127.0.0.1", "-p", port.toString()) + shellSplit(args))
            .toTypedArray()
}

/**
 * Gömülü preset havuzu. Bunlar Türkiye ISS'lerinde raporlanmış çalışan
 * kombinasyonlardır; **sabit değil, otomatik test edilecek aday listesidir**
 * (bkz. StrategyTester) ve kolayca güncellenebilir.
 *
 * Gerçekçi uyarı: kazanan strateji ISS'e ve zamana göre değişir; bu yüzden tek
 * "doğru" çözüm yoktur. Liste JSON'a taşınıp uzaktan güncellenebilir.
 */
object StrategyPool {

    val P1 = Strategy(
        "P1", "Varsayılan (dengeli)",
        "ByeDPI resmi varsayılanına yakın; dengeli.",
        "--split 1 --disorder 3+s --mod-http=h,d --auto=torst --tlsrec 1+s",
    )
    val P2 = Strategy(
        "P2", "Fake + düşük TTL",
        "Topluluk testinde yüksek başarı oranı.",
        "--split 2 --disorder 3 --fake -1 --ttl 5",
    )
    val P3 = Strategy(
        "P3", "OOB + TLSRec",
        "Bazı Superonline / Türk Telekom senaryoları.",
        "--split 1 --oob 1 --disorder 3 --mod-http=h,d --fake -1 --tlsrec 3+h",
    )
    val P4 = Strategy(
        "P4", "Oto-kademeli",
        "--auto ile kademeli deneme.",
        "-s1 -o1 -Ar -o1 -At -f-1 -r1+s -As",
    )
    val P5 = Strategy(
        "P5", "TLSRec odaklı",
        "SNI'yi TLS record seviyesinde ortadan böler.",
        "--tlsrec 3+s --auto=torst --timeout 3",
    )
    val P6 = Strategy(
        "P6", "Minimal disorder",
        "Zayıf DPI'lı / küçük ISS (ör. TurkNet).",
        "--disorder 1",
    )
    val P7 = Strategy(
        "P7", "Fake + TTL (P2 varyantı)",
        "P2'nin daha yüksek TTL'li varyantı.",
        "--split 2 --disorder 3 --fake -1 --ttl 6 --auto=torst --timeout 3",
    )

    val all: List<Strategy> = listOf(P1, P2, P3, P4, P5, P6, P7)

    fun byId(id: String?): Strategy? = all.firstOrNull { it.id == id }

    /**
     * ISS ailesine göre test sırası ipucu. Tester yine de hepsini dener; bu sadece
     * en olası çalışanı erken bulup testi hızlandırmak içindir.
     */
    fun orderedFor(family: IspFamily): List<Strategy> = when (family) {
        IspFamily.TurkTelekom -> listOf(P2, P7, P3, P1, P5, P4, P6)
        IspFamily.Turkcell -> listOf(P2, P3, P7, P1, P5, P4, P6)
        IspFamily.Vodafone -> listOf(P1, P2, P5, P3, P7, P4, P6)
        IspFamily.TurkNet -> listOf(P6, P1, P2, P5, P3, P7, P4)
        IspFamily.Other -> listOf(P1, P2, P3, P5, P7, P4, P6)
    }
}
