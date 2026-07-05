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
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(connected)
            .setSilent(true)
            .setContentIntent(contentIntent)
            .addAction(0, actionLabel, actionIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
