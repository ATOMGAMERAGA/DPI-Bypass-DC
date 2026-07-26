package net.atom.dpibypass.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import java.util.Locale

/**
 * Uygulamanın kendi kaynak kullanımı — Ayarlar'daki "Bilgi" bölümünü besler.
 *
 * DÜRÜSTLÜK NOTU: Android, bir uygulamanın "yüzde kaç pil yediğini" herhangi bir
 * genel API ile vermez; o rakamı yalnızca sistem hesaplar ve yalnızca kendi pil
 * ekranında gösterir. Uydurma bir yüzde üretmek yerine burada ÖLÇÜLEBİLEN
 * gerçekler gösterilir:
 *
 *   * Veri: [TrafficStats] ile bu UID'nin cihaz açılışından beri gönderdiği ve
 *     aldığı bayt. Tünel açıkken tüm tünel trafiği bu UID'ye yazılır, dolayısıyla
 *     "uygulamanın yediği internet" tam olarak budur. İzin gerektirmez.
 *   * İşlemci: [Process.getElapsedCpuTime] ile bu sürecin harcadığı CPU süresi.
 *     Pil tüketiminin en doğrudan ölçülebilir bileşeni budur.
 *   * Pil optimizasyonu muafiyeti ve cihaz pil durumu.
 *
 * Yüzdelik pil payı için kullanıcı sistemin kendi ekranına yönlendirilir.
 */
object AppUsage {

    data class Traffic(val rxBytes: Long, val txBytes: Long) {
        val totalBytes: Long get() = rxBytes + txBytes
        /** TrafficStats desteklenmiyorsa (-1) veri gösterilmez. */
        val supported: Boolean get() = rxBytes >= 0 && txBytes >= 0
    }

    data class Snapshot(
        val traffic: Traffic,
        val cpuTimeMs: Long,
        val batteryPercent: Int,
        val charging: Boolean,
        val unrestricted: Boolean,
    )

    fun snapshot(context: Context): Snapshot = Snapshot(
        traffic = traffic(),
        cpuTimeMs = Process.getElapsedCpuTime(),
        batteryPercent = batteryPercent(context),
        charging = isCharging(context),
        unrestricted = isIgnoringBatteryOptimizations(context),
    )

    /** Cihaz açılışından beri bu uygulamanın ağ trafiği. */
    fun traffic(): Traffic {
        val uid = Process.myUid()
        val rx = runCatching { TrafficStats.getUidRxBytes(uid) }.getOrDefault(-1L)
        val tx = runCatching { TrafficStats.getUidTxBytes(uid) }.getOrDefault(-1L)
        return Traffic(rx, tx)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean = runCatching {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }.getOrDefault(false)

    fun batteryPercent(context: Context): Int = runCatching {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
    }.getOrDefault(-1)

    fun isCharging(context: Context): Boolean = runCatching {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }.getOrDefault(false)

    /** Cihaz tanımı: "Samsung SM-S928B · Android 15 (API 35)". */
    fun deviceSummary(): String =
        "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} · " +
            "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    fun abiSummary(): String = Build.SUPPORTED_ABIS.firstOrNull() ?: "bilinmiyor"

    /** İnsan okur biçimde bayt: 0 B / 812 KB / 34,2 MB / 1,21 GB. */
    fun formatBytes(bytes: Long): String {
        if (bytes < 0) return "—"
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.getDefault(), "%.0f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.getDefault(), "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.getDefault(), "%.2f GB", gb)
    }

    /** CPU süresi: 4,2 sn / 3 dk 12 sn / 1 sa 4 dk. */
    fun formatDuration(millis: Long): String {
        if (millis < 0) return "—"
        val totalSeconds = millis / 1000
        return when {
            totalSeconds < 60 -> String.format(Locale.getDefault(), "%.1f sn", millis / 1000.0)
            totalSeconds < 3600 -> "${totalSeconds / 60} dk ${totalSeconds % 60} sn"
            else -> "${totalSeconds / 3600} sa ${(totalSeconds % 3600) / 60} dk"
        }
    }
}
