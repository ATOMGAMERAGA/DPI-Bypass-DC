package net.atom.dpibypass.isp

/**
 * DPI davranış ailesi. Strateji havuzu sıralaması bu aileye göre önceliklendirilir
 * (kesin karar canlı testtedir — bkz. StrategyTester).
 */
enum class IspFamily {
    TurkTelekom,
    Turkcell,
    Vodafone,
    TurkNet,
    Other,
}

/**
 * Kullanıcıya gösterilen ISS listesi + otomatik eşleme ipuçları.
 *
 * - [simCodes]: MCC+MNC (mobil veri tespiti).
 * - [asnKeywords]: dış IP'nin ASN/ISP org adında aranan alt dizeler (Wi-Fi/ev tespiti).
 *   Küçük harfe indirgenmiş karşılaştırma yapılır.
 * - [family]: DPI davranış ailesi (strateji önceliklendirmesi için).
 *
 * Not: Redbox = Türk Telekom ön ödemeli mobil → TT ailesi.
 *      Superbox = Turkcell sabit-kablosuz (FWA) → Turkcell ailesi.
 */
enum class Isp(
    val displayName: String,
    val family: IspFamily,
    val simCodes: List<String> = emptyList(),
    val asnKeywords: List<String> = emptyList(),
) {
    TurkTelekomMobile(
        "Türk Telekom (Mobil)", IspFamily.TurkTelekom,
        simCodes = listOf("28603", "28604"),
        asnKeywords = listOf("turk telekom", "ttnet", "avea"),
    ),
    TurkTelekomHome(
        "Türk Telekom Evde İnternet", IspFamily.TurkTelekom,
        asnKeywords = listOf("turk telekom", "ttnet"),
    ),
    TurkTelekomHotspot(
        "Türk Telekom Hotspot", IspFamily.TurkTelekom,
        asnKeywords = listOf("turk telekom", "ttnet"),
    ),
    Redbox(
        "Redbox (Türk Telekom)", IspFamily.TurkTelekom,
        simCodes = listOf("28603", "28604"),
        asnKeywords = listOf("turk telekom"),
    ),
    TurkcellMobile(
        "Turkcell (Mobil)", IspFamily.Turkcell,
        simCodes = listOf("28601"),
        asnKeywords = listOf("turkcell"),
    ),
    Superonline(
        "Turkcell Superonline", IspFamily.Turkcell,
        asnKeywords = listOf("superonline", "turkcell"),
    ),
    Superbox(
        "Superbox (Turkcell FWA)", IspFamily.Turkcell,
        simCodes = listOf("28601"),
        asnKeywords = listOf("turkcell", "superonline"),
    ),
    TurkcellHotspot(
        "Turkcell Hotspot", IspFamily.Turkcell,
        asnKeywords = listOf("turkcell", "superonline"),
    ),
    VodafoneMobile(
        "Vodafone (Mobil)", IspFamily.Vodafone,
        simCodes = listOf("28602"),
        asnKeywords = listOf("vodafone"),
    ),
    VodafoneHome(
        "Vodafone Evde İnternet", IspFamily.Vodafone,
        asnKeywords = listOf("vodafone"),
    ),
    VodafoneHotspot(
        "Vodafone Hotspot", IspFamily.Vodafone,
        asnKeywords = listOf("vodafone"),
    ),
    TurkNet(
        "TurkNet", IspFamily.TurkNet,
        asnKeywords = listOf("turknet"),
    ),
    Other(
        "Diğer / Bilinmiyor", IspFamily.Other,
    );

    companion object {
        fun fromId(id: String?): Isp =
            entries.firstOrNull { it.name == id } ?: Other

        /** MCC+MNC koduyla ilk eşleşen ISS. */
        fun fromSimCode(code: String?): Isp? {
            if (code.isNullOrBlank()) return null
            return entries.firstOrNull { code in it.simCodes }
        }

        /** ASN/org adı içinde ilk eşleşen ISS (küçük harf duyarsız, en spesifik önce). */
        fun fromAsnOrg(org: String?): Isp? {
            if (org.isNullOrBlank()) return null
            val lower = org.lowercase()
            // Superonline/TurkNet gibi spesifik anahtarları genel "turkcell/turk telekom"
            // öncesinde denemek için sıralı bir öncelik listesi.
            val priority = listOf(
                Superonline, TurkNet, Superbox,
                TurkcellMobile, TurkTelekomHome, VodafoneHome,
            )
            for (isp in priority) {
                if (isp.asnKeywords.any { lower.contains(it) }) return isp
            }
            return entries.firstOrNull { isp ->
                isp.asnKeywords.any { lower.contains(it) }
            }
        }
    }
}
