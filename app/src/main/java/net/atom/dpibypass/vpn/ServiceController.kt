package net.atom.dpibypass.vpn

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * VPN servisini başlatmak/durdurmak için tek giriş noktası. Tile, bildirim ve UI
 * hepsi buradan geçer — böylece "servis arkada ölürse tıklayınca kendini başlatsın"
 * garantisi tek yerde toplanır.
 */
object ServiceController {
    fun start(context: Context) {
        val intent = Intent(context, DpiVpnService::class.java).apply {
            action = DpiVpnService.ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        val intent = Intent(context, DpiVpnService::class.java).apply {
            action = DpiVpnService.ACTION_STOP
        }
        ContextCompat.startForegroundService(context, intent)
    }
}
