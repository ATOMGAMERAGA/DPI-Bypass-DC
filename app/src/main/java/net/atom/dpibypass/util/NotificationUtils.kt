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
import androidx.core.app.NotificationManagerCompat
import net.atom.dpibypass.R
import net.atom.dpibypass.ui.MainActivity
import net.atom.dpibypass.vpn.DpiVpnService

/**
 * Bildirimler ve Samsung Now Bar / Android 16 "Live Update" entegrasyonu.
 *
 * ---------------------------------------------------------------------------
 * NEDEN NOW BAR'DA GÖRÜNMÜYORDUK
 * ---------------------------------------------------------------------------
 * One UI 8, Now Bar'ı Android 16'nın "promoted ongoing" bildirim mekanizmasına
 * bağladı. Sistem bir bildirimi Now Bar'a ancak `NotificationManagerService`
 * şu üç şey aynı anda doğruysa alır:
 *
 *   1. kullanıcı uygulama için canlı bildirimleri kapatmamış (varsayılan açık),
 *   2. kanal önemi IMPORTANCE_MIN'in ÜSTÜNDE,
 *   3. `Notification.hasPromotableCharacteristics()` → true.
 *
 * Kritik nokta 3. maddede: **bu sözleşme Android 16 içinde DEĞİŞTİ.** AOSP
 * kaynağı (frameworks/base, core/java/android/app/Notification.java):
 *
 *   android16-release:
 *       ongoing && başlık var && grup özeti değil && özel görünüm yok
 *       && isColorizedRequested()          ← setColorized(true) ZORUNLU
 *       && hasPromotableStyle()
 *
 *   android16-qpr1-release (ui_rich_ongoing bayrağı açıkken — One UI 8.5 dahil):
 *       isRequestPromotedOngoing()         ← setRequestPromotedOngoing ZORUNLU
 *       && ongoing && başlık var && stil uygun && grup özeti değil
 *       && özel görünüm yok
 *       && !isColorizedRequested()         ← setColorized(true) YASAK
 *
 * Yani iki sürümün şartları birbirini DIŞLIYOR: birinde zorunlu olan, diğerinde
 * diskalifiye sebebi. Eski kod `setColorized(true)` gönderiyordu; QPR1 ve
 * sonrasındaki cihazlarda bildirim bu yüzden hiç promote edilmedi, uygulama da
 * sistemin canlı bildirim listesinde belirmedi (o liste, en az bir kez uygun
 * bildirim göndermiş uygulamalarla dolar).
 *
 * ÇÖZÜM: sürüm tahmin etmiyoruz — CİHAZA SORUYORUZ. İki aday bildirim kurulur
 * ve platformun kendi yüklemi olan `hasPromotableCharacteristics()` ile hangisinin
 * kabul edildiği ölçülür (bkz. [isPromotable]). Hangi One UI/Android sürümü
 * olursa olsun doğru olanı gider; sözleşme ileride yine değişirse de kod
 * kendiliğinden uyum sağlar.
 *
 * Not: Now Bar'ın ayrıca kullanıcı tarafında açık olması gerekir
 * (Ayarlar → Kilit ekranı ve AOD → Now bar).
 */
object NotificationUtils {

    /** Canlı tünel durumu kanalı. Eski kanal (dpi_vpn_status) IMPORTANCE_LOW idi. */
    const val CHANNEL_ID = "dpi_vpn_live"

    private const val LEGACY_CHANNEL_ID = "dpi_vpn_status"

    const val NOTIFICATION_ID = 1

    /** Android 16. compileSdk 35 olduğu için sabit adı yerine sayısı yazılır. */
    private const val SDK_ANDROID_16 = 36

    // One UI mavisi — bildirimin vurgu rengi; Now Bar / kilit ekranı göstergesinde
    // marka rengini verir.
    private const val ACCENT_COLOR = 0xFF0072F5.toInt()

    // Android 16 "Live Update" extra anahtarları. Platform sabitleri compileSdk 36
    // gerektirir; anahtarlar AOSP'de bu adlarla sabittir
    // (Notification.EXTRA_REQUEST_PROMOTED_ONGOING / EXTRA_SHORT_CRITICAL_TEXT).
    private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"
    private const val EXTRA_SHORT_CRITICAL_TEXT = "android.shortCriticalText"

    fun registerChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        // Eski düşük önemli kanalı temizle: aynı işi yapan iki kanal, kullanıcı
        // ayarlarında kafa karıştırır ve eskisi Live Update için uygun değildir.
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
            // Kilit ekranında tam görünür olmalı ki canlı gösterge oraya düşebilsin.
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
        fun variant(colorized: Boolean, requestPromotion: Boolean): Notification =
            build(
                context = context,
                title = title,
                content = content,
                connected = connected,
                ongoing = connected || liveIndicator,
                connectedSinceMs = connectedSinceMs,
                shortStatus = shortStatus,
                colorized = colorized,
                requestPromotion = requestPromotion,
            )

        if (!liveIndicator || Build.VERSION.SDK_INT < SDK_ANDROID_16) {
            return variant(colorized = false, requestPromotion = false)
        }

        // Android 16 QPR1+ sözleşmesi: promosyon TALEP edilir, renklendirme YASAK.
        val requested = variant(colorized = false, requestPromotion = true)
        if (isPromotable(requested)) return requested

        // Android 16 ilk sürüm sözleşmesi: renklendirme ZORUNLU, talep gereksiz
        // (ama zararsız — bilinmeyen extra'lar yok sayılır).
        val colorizedVariant = variant(colorized = true, requestPromotion = true)
        if (isPromotable(colorizedVariant)) return colorizedVariant

        // Yüklem hiç çalışmadıysa (yansıma başarısız / bayrak kapalı) ölçemedik.
        // O zaman API VARLIĞINA bakarız: setRequestPromotedOngoing yalnızca yeni
        // sözleşmeyle birlikte geldi; varsa yeni, yoksa eski sözleşme cihazıyız.
        // Bu ayrım olmasaydı ölçüm başarısızlığında Android 16 ilk sürümündeki
        // çalışan davranışı bozardık.
        return if (supportsPromotionRequest) requested else colorizedVariant
    }

    /** `Notification.Builder#setRequestPromotedOngoing` bu cihazda var mı? */
    private val supportsPromotionRequest: Boolean by lazy {
        runCatching {
            Notification.Builder::class.java.getMethod(
                "setRequestPromotedOngoing",
                Boolean::class.javaPrimitiveType,
            )
            true
        }.getOrDefault(false)
    }

    private fun build(
        context: Context,
        title: String,
        content: String,
        connected: Boolean,
        ongoing: Boolean,
        connectedSinceMs: Long,
        shortStatus: String?,
        colorized: Boolean,
        requestPromotion: Boolean,
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
            // Promote edilebilirliğin değişmeyen üç şartı: başlık, ongoing,
            // özel görünüm/grup özeti YOK. (setSilent de KULLANILMIYOR — grup
            // anahtarı ekler; sessizlik kanal düzeyinde sağlanıyor.)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(0, actionLabel, actionIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(ACCENT_COLOR)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (colorized) builder.setColorized(true)

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

        if (requestPromotion) {
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

    /**
     * Bildirimi platformun kendi yüklemine sorar: "bunu Now Bar'a alır mıydın?"
     *
     * `hasPromotableCharacteristics()` API 36 ile gelen genel API'dir; compileSdk
     * 35 olduğu için yansımayla çağrılır. Yoksa/başarısızsa `false` döner —
     * çağıran taraf o zaman bilinen sözleşmelerden birine düşer.
     */
    private fun isPromotable(notification: Notification): Boolean = runCatching {
        Notification::class.java
            .getMethod("hasPromotableCharacteristics")
            .invoke(notification) as? Boolean ?: false
    }.getOrDefault(false)

    /** Canlı gösterge (Now Bar) için sistemin o anki durumu — ayarlarda gösterilir. */
    enum class LiveUpdateState {
        /** Android 16 öncesi: promoted ongoing mekanizması yok. */
        Unsupported,

        /** Uygulamanın bildirimleri kapalı — hiçbir gösterge çıkamaz. */
        NotificationsOff,

        /** Kullanıcı bu uygulama için canlı bildirimleri kapatmış. */
        Blocked,

        /** Her şey hazır. */
        Ready,
    }

    fun liveUpdateState(context: Context): LiveUpdateState {
        if (Build.VERSION.SDK_INT < SDK_ANDROID_16) return LiveUpdateState.Unsupported
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return LiveUpdateState.NotificationsOff
        }
        val manager = context.getSystemService(NotificationManager::class.java)
            ?: return LiveUpdateState.Unsupported
        // API 36: NotificationManager.canPostPromotedNotifications()
        val allowed = runCatching {
            NotificationManager::class.java
                .getMethod("canPostPromotedNotifications")
                .invoke(manager) as? Boolean
        }.getOrNull()
        return if (allowed == false) LiveUpdateState.Blocked else LiveUpdateState.Ready
    }
}
