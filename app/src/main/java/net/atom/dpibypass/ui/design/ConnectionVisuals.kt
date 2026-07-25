package net.atom.dpibypass.ui.design

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import net.atom.dpibypass.data.ConnectionState
import net.atom.dpibypass.ui.theme.LocalStateColors

// ---------------------------------------------------------------------------
// Bağlantı durumunun görsel karşılığı TEK yerde tanımlanır.
//
// Renk, ikon ve metin nerede kullanılırsa kullanılsın (kahraman daire, zemin,
// durum kartı, dock) aynı kaynaktan gelir. Bu, tasarımın "aynı uygulama" gibi
// hissettirmesinin en ucuz ve en etkili yoludur.
// ---------------------------------------------------------------------------

@Composable
fun connectionColor(state: ConnectionState): Color {
    val colors = LocalStateColors.current
    return when (state) {
        ConnectionState.Connected -> colors.connected
        ConnectionState.Connecting, ConnectionState.Testing -> colors.busy
        ConnectionState.Failed -> colors.error
        ConnectionState.Disconnected -> colors.idle
    }
}

fun connectionIcon(state: ConnectionState): ImageVector = when (state) {
    ConnectionState.Connected -> Icons.Rounded.Shield
    ConnectionState.Connecting, ConnectionState.Testing -> Icons.Rounded.Sync
    ConnectionState.Failed -> Icons.Rounded.ErrorOutline
    ConnectionState.Disconnected -> Icons.Rounded.PowerSettingsNew
}

/** Daire üzerindeki kısa eylem/durum sözcüğü. */
fun connectionLabel(state: ConnectionState): String = when (state) {
    ConnectionState.Connected -> "BAĞLI"
    ConnectionState.Connecting -> "BAĞLANIYOR"
    ConnectionState.Testing -> "TEST EDİLİYOR"
    ConnectionState.Failed -> "TEKRAR DENE"
    ConnectionState.Disconnected -> "BAĞLAN"
}

/** Durumu düz Türkçe anlatan cümle — teknik terim yok. */
fun connectionHeadline(state: ConnectionState): String = when (state) {
    ConnectionState.Connected -> "Koruma açık"
    ConnectionState.Connecting -> "Tünel kuruluyor"
    ConnectionState.Testing -> "En hızlı yöntem aranıyor"
    ConnectionState.Failed -> "Bağlanılamadı"
    ConnectionState.Disconnected -> "Koruma kapalı"
}

fun connectionSupport(state: ConnectionState): String = when (state) {
    ConnectionState.Connected -> "Engellenen siteler bu cihazda açılır."
    ConnectionState.Connecting -> "Yerel tünel ve DNS hazırlanıyor…"
    ConnectionState.Testing -> "Stratejiler tek tek deneniyor, en düşük gecikmeli seçilecek."
    ConnectionState.Failed -> "Farklı bir strateji deneyin ya da tekrar bağlanın."
    ConnectionState.Disconnected -> "Bağlanmak için büyük düğmeye dokunun."
}

/** Bağlı kalma süresi — sabit genişlikli, saat gibi okunur. */
fun formatUptime(millis: Long): String {
    if (millis <= 0L) return "00:00"
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
