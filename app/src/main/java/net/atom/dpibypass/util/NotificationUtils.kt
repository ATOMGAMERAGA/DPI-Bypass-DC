package net.atom.dpibypass.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import net.atom.dpibypass.R
import net.atom.dpibypass.ui.MainActivity
import net.atom.dpibypass.vpn.DpiVpnService

object NotificationUtils {
    const val CHANNEL_ID = "dpi_vpn_status"
    const val NOTIFICATION_ID = 1

    // One UI mavisi — bildirim "colorized" olduğunda vurgu rengi olarak kullanılır ve
    // Samsung Now Bar / kilit ekranı canlı göstergesinde marka rengini verir.
    private const val ACCENT_COLOR = 0xFF0072F5.toInt()

    fun registerChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
                // Kilit ekranında tam görünür olmalı ki Samsung Now Bar canlı göstergeyi
                // yakalayabilsin (gizli olursa Now Bar'a düşmez).
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Bağlantı durum bildirimi. Bildirim küçük ikonu tek renk beyaz siluettir
     * (ic_notification), Android bunu tepside beyaz gösterir.
     */
    fun buildNotification(
        context: Context,
        title: String,
        content: String,
        connected: Boolean,
        persistent: Boolean = false,
        connectedSinceMs: Long = System.currentTimeMillis(),
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
            // Samsung durum göstergesi açıkken tünel süresince kalıcı (kapatılamaz) tut.
            .setOngoing(connected || persistent)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(0, actionLabel, actionIntent)
            // Samsung Now Bar / kilit ekranı canlı göstergesi, "status" kategorili,
            // kilit ekranında görünür (PUBLIC) ve renklendirilmiş (colorized) sürekli
            // bildirimleri yakalar. Bu üçlü olmadan tünel göstergesi Now Bar'a düşmez.
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColorized(true)
            .setColor(ACCENT_COLOR)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (connected) {
            // Canlı "bağlı süresi" kronometresi — sürekli akan (ticking) bir sayaç,
            // Samsung Now Bar'ının canlı etkinlik olarak yakalaması için güçlü bir
            // "live" sinyalidir. Bağlanma anından itibaren sayar.
            builder
                .setWhen(connectedSinceMs)
                .setShowWhen(true)
                .setUsesChronometer(true)
        } else {
            builder.setShowWhen(false)
        }

        return builder.build()
    }
}
