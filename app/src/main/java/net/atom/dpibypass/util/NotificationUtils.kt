package net.atom.dpibypass.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import net.atom.dpibypass.R
import net.atom.dpibypass.ui.MainActivity
import net.atom.dpibypass.vpn.DpiVpnService

/**
 * Bildirimler ve Samsung Now Bar / Android 16 "Live Update" entegrasyonu.
 *
 * ---------------------------------------------------------------------------
 * NEDEN ÖNCEDEN NOW BAR'DA GÖRÜNMÜYORDUK
 * ---------------------------------------------------------------------------
 * One UI 8, Now Bar'ı Android 16'nın (API 36) "promoted ongoing" bildirim
 * mekanizmasına bağladı. Sistem, bir bildirimi Now Bar'a ancak
 * `Notification.hasPromotableCharacteristics()` doğru dönerse alır. AOSP'deki
 * koşullar (android16-release, Notification.java) şunlardır:
 *
 *   1. `isOngoingEvent()`      → setOngoing(true)
 *   2. `hasTitle()`            → boş olmayan setContentTitle
 *   3. `!isGroupSummary()`     → grup özeti OLMAYACAK
 *   4. `!containsCustomViews()`→ RemoteViews KULLANILMAYACAK
 *   5. `isColorizedRequested()`→ setColorized(true)   (evet, ZORUNLU)
 *   6. `hasPromotableStyle()`  → stil yok / BigText / Call / Progress
 * ve ayrıca kanalın önemi IMPORTANCE_MIN'den YÜKSEK olmalıdır.
 *
 * Eski kodda 5 ve 6 sağlanıyordu ama `setSilent(true)` çağrısı bildirime
 * "silent" grup anahtarı ekliyor, kanal önemi de en düşük basamağa yakın
 * kalıyordu; üstelik promosyon hiç TALEP edilmiyordu. Bu yüzden uygulama
 * Samsung'un Now Bar uygulama listesinde bile belirmiyordu — o liste, en az bir
 * kez uygun (promote edilebilir) bildirim göndermiş uygulamalarla dolar.
 *
 * Şimdi:
 *   * Sessizlik `setSilent` ile değil, KANAL üzerinden sağlanıyor (grup anahtarı
 *     eklenmiyor).
 *   * Tünel bildirimi kendi kanalında ve IMPORTANCE_DEFAULT ile yayınlanıyor
 *     (sesi kapalı). Kanal önemi oluşturulduktan sonra değiştirilemediği için
 *     yeni bir kanal kimliği kullanıldı; eskisi siliniyor.
 *   * `android.requestPromotedOngoing` ve `android.shortCriticalText` extra'ları
 *     doğrudan yazılıyor. NotificationCompat'ın setRequestPromotedOngoing /
 *     setShortCriticalText yardımcıları da tam olarak bunu yapar (androidx core
 *     1.17+); extra'yı elle yazmak aynı sonucu verir ve kütüphane sürümüne
 *     bağımlılık getirmez.
 *
 * Not: Now Bar ayrıca kullanıcı tarafında açık olmalıdır (Ayarlar → Kilit ekranı
 * ve AOD → Now bar). Uygulama listesinde görünmek için en az bir kez bağlanmak
 * yeterlidir.
 */
object NotificationUtils {

    /** Canlı tünel durumu kanalı. Eski kanal (dpi_vpn_status) IMPORTANCE_LOW idi. */
    const val CHANNEL_ID = "dpi_vpn_live"

    private const val LEGACY_CHANNEL_ID = "dpi_vpn_status"

    const val NOTIFICATION_ID = 1

    // One UI mavisi — bildirim "colorized" olduğunda vurgu rengi olarak kullanılır ve
    // Samsung Now Bar / kilit ekranı canlı göstergesinde marka rengini verir.
    private const val ACCENT_COLOR = 0xFF0072F5.toInt()

    // Android 16 (API 36) "Live Update" extra anahtarları. Platform sabitleri
    // compileSdk 36 gerektirdiği için değerleri doğrudan yazıyoruz; anahtarlar
    // AOSP'de bu adlarla sabittir (Notification.EXTRA_REQUEST_PROMOTED_ONGOING /
    // EXTRA_SHORT_CRITICAL_TEXT).
    private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"
    private const val EXTRA_SHORT_CRITICAL_TEXT = "android.shortCriticalText"

    fun registerChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        // Eski düşük önemli kanalı temizle: aynı işi yapan iki kanal, kullanıcı
        // ayarlarında kafa karıştırır ve eskisi Now Bar için uygun değildir.
        runCatching { manager.deleteNotificationChannel(LEGACY_CHANNEL_ID) }

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            // Live Update için kanal önemi IMPORTANCE_MIN'in üzerinde olmalı.
            // DEFAULT seçildi ama sesi/titreşimi kapatıldı: yüksek öncelik,
            // sıfır rahatsızlık.
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
            setSound(null, null)
            enableLights(false)
            enableVibration(false)
            setShowBadge(false)
            // Kilit ekranında tam görünür olmalı ki Now Bar canlı göstergeyi
            // yakalayabilsin (gizli olursa Now Bar'a düşmez).
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Bağlantı durum bildirimi. Bildirim küçük ikonu tek renk beyaz siluettir
     * (ic_notification), Android bunu tepside beyaz gösterir.
     *
     * [liveIndicator] kullanıcının "Now Bar'da göster" tercihidir: açıkken
     * bildirim promosyon talep eder ve tünel süresince kapatılamaz olur.
     */
    fun buildNotification(
        context: Context,
        title: String,
        content: String,
        connected: Boolean,
        liveIndicator: Boolean = false,
        connectedSinceMs: Long = System.currentTimeMillis(),
        shortStatus: String? = null,
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val actionIntent = PendingIntent.getService(
            context, 1,
            Intent(context, DpiVpnService::class.java).setAction(
                if (connected) DpiVpnService.ACTION_STOP else DpiVpnService.ACTION_START
            ),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val actionLabel = context.getString(
            if (connected) R.string.action_disconnect else R.string.action_connect
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            // Promote edilebilirliğin ilk koşulu. Tünel açıkken her hâlükârda
            // kalıcıdır; kapalıyken yalnızca canlı gösterge açıksa.
            .setOngoing(connected || liveIndicator)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(0, actionLabel, actionIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // ZORUNLU: promote edilebilirlik `isColorizedRequested()` ister.
            // (setSilent KULLANILMIYOR — grup anahtarı ekleyip promosyonu bozar;
            // sessizlik kanal düzeyinde sağlanıyor.)
            .setColorized(true)
            .setColor(ACCENT_COLOR)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (connected) {
            // Canlı "bağlı süresi" kronometresi. Hem kullanıcıya bilgi verir hem
            // Now Bar'a bildirimin gerçekten CANLI olduğunu söyler.
            builder
                .setWhen(connectedSinceMs)
                .setShowWhen(true)
                .setUsesChronometer(true)
        } else {
            builder.setShowWhen(false)
        }

        if (liveIndicator) {
            builder.addExtras(
                Bundle().apply {
                    putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
                    // Durum çubuğundaki daralmış "chip" metni. Kısa tutulmalı;
                    // uzun metin sistem tarafından kırpılır.
                    shortStatus?.let { putString(EXTRA_SHORT_CRITICAL_TEXT, it) }
                },
            )
        }

        return builder.build()
    }
}
