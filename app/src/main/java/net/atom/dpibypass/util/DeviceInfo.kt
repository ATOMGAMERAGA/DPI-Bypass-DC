package net.atom.dpibypass.util

import android.content.Context
import android.os.Build
import android.provider.Settings

/**
 * Cihaz / üretici (OEM skini) tespiti. Kurulum sihirbazı ve Samsung'a özel
 * ayarlar buradan beslenir; her skin (One UI, HyperOS/MIUI, Pixel...) Hızlı
 * Panel'i farklı adlandırdığı ve navigasyon çubuğunu farklı yönettiği için
 * kullanıcıya doğru dille sormak gerekir.
 */
object DeviceInfo {

    private fun manufacturer(): String = Build.MANUFACTURER.orEmpty().lowercase()

    fun isSamsung(): Boolean = manufacturer() == "samsung"

    fun isXiaomi(): Boolean =
        manufacturer() in setOf("xiaomi", "redmi", "poco")

    /** Kullanıcının cihazında gördüğü marka adı (metinlerde geçer). */
    fun brandName(): String = when {
        isSamsung() -> "Samsung"
        isXiaomi() -> "Xiaomi"
        else -> Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "Android"
    }

    /**
     * Hızlı ayar/kısayol panelinin üreticiye göre kullanıcıya tanıdık adı.
     * (Samsung: Hızlı Panel, Xiaomi/HyperOS: Kontrol Merkezi, saf Android: Hızlı Ayarlar.)
     */
    fun quickPanelName(): String = when {
        isSamsung() -> "Hızlı Panel"
        isXiaomi() -> "Kontrol Merkezi"
        else -> "Hızlı Ayarlar"
    }

    /**
     * Sistem navigasyon modu: 0 = 3 tuşlu (klasik navbar), 1 = 2 tuşlu,
     * 2 = tam ekran hareketler (gesture). Okunamazsa 3 tuşlu varsayılır.
     */
    fun navigationMode(context: Context): Int = try {
        Settings.Secure.getInt(context.contentResolver, "navigation_mode", 0)
    } catch (e: Exception) {
        0
    }

    /**
     * Ekranda tuşlu bir navigasyon çubuğu (navbar) kullanılıyor mu?
     * Tam ekran hareket modunda (2) çubuk yoktur.
     */
    fun hasNavigationBar(context: Context): Boolean = navigationMode(context) != 2
}
