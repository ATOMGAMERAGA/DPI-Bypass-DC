package net.atom.dpibypass.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.atom.dpibypass.data.SettingsRepository
import net.atom.dpibypass.vpn.ServiceController

/**
 * Cihaz açılınca otomatik bağlan (ayardan açıksa). VPN izni yoksa hiçbir şey yapmaz
 * (kullanıcı uygulamayı açıp izin vermeli).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(appContext).settings.first()
                if (settings.autoConnectOnBoot && VpnService.prepare(appContext) == null) {
                    ServiceController.start(appContext)
                    Log.i(TAG, "Açılışta otomatik bağlanılıyor")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Boot auto-connect hatası: ${e.message}")
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
