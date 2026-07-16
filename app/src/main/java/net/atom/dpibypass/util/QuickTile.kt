package net.atom.dpibypass.util

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import net.atom.dpibypass.R
import net.atom.dpibypass.tile.DpiTileService
import java.util.function.Consumer

/**
 * Hızlı Panel (Quick Settings) kısayolunu tek dokunuşla eklemenin tek giriş
 * noktası. Android 13+ (API 33) `requestAddTileService` ile sistem, kullanıcıya
 * "DPI Bypass'ı Hızlı Panel'e ekle?" onayı gösterir; daha eski sürümlerde panel
 * elle düzenlenmelidir, o yüzden kısa bir yönerge gösterilir.
 */
object QuickTile {

    fun request(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val sbm = context.getSystemService(StatusBarManager::class.java)
            if (sbm == null) {
                showManualHint(context)
                return
            }
            sbm.requestAddTileService(
                ComponentName(context, DpiTileService::class.java),
                context.getString(R.string.tile_label),
                Icon.createWithResource(context, R.drawable.ic_qs_tile),
                { command -> command.run() },
                Consumer<Int> { /* sonucu sessizce yut: eklendi/vazgeçildi fark etmez */ },
            )
        } else {
            showManualHint(context)
        }
    }

    private fun showManualHint(context: Context) {
        Toast.makeText(
            context,
            "${DeviceInfo.quickPanelName()}'i düzenleyip “DPI Bypass” kutucuğunu ekleyin.",
            Toast.LENGTH_LONG,
        ).show()
    }
}
