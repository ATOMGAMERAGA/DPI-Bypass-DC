package net.atom.dpibypass.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.atom.dpibypass.BuildConfig
import net.atom.dpibypass.R
import net.atom.dpibypass.data.ThemePref
import net.atom.dpibypass.dns.DohProvider
import net.atom.dpibypass.ui.AppViewModel
import net.atom.dpibypass.ui.design.AppButton
import net.atom.dpibypass.ui.design.AppCard
import net.atom.dpibypass.ui.design.AppScreen
import net.atom.dpibypass.ui.design.AppTextField
import net.atom.dpibypass.ui.design.ButtonTone
import net.atom.dpibypass.ui.design.ChoiceDialog
import net.atom.dpibypass.ui.design.ListRow
import net.atom.dpibypass.ui.design.RowDivider
import net.atom.dpibypass.ui.design.SectionHeader
import net.atom.dpibypass.ui.design.Segment
import net.atom.dpibypass.ui.design.SegmentedControl
import net.atom.dpibypass.ui.design.SwitchRow
import net.atom.dpibypass.ui.design.VSpace
import net.atom.dpibypass.util.DeviceInfo

// ---------------------------------------------------------------------------
// Ayarlar.
//
// Her satırın solunda tonlanmış bir ikon var: uzun bir ayar listesinde göz
// ikonlara tutunarak arar, metni baştan sona okumaz. Bölümler tek bir yuvarlak
// blokta gruplanır (One UI "focus block") ve ince ayraçlarla bölünür.
//
// Açıklamalar teknik ama SONUÇ ODAKLI yazıldı: "UDP'yi düşür" değil, "açarsanız
// Discord'da ses gitmez" — kullanıcı ne olacağını bilerek karar verir.
// ---------------------------------------------------------------------------

@Composable
fun SettingsScreen(viewModel: AppViewModel, onRequestTile: () -> Unit = {}) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var dohPickerOpen by remember { mutableStateOf(false) }

    AppScreen(
        title = "Ayarlar",
        subtitle = "Bağlantı davranışı, görünüm ve cihaz kısayolları.",
    ) {
        // ---- Hızlı erişim ----
        SectionHeader("Hızlı erişim")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            ListRow(
                title = "${DeviceInfo.quickPanelName()}'e kısayol ekle",
                subtitle = "WiFi/Bluetooth kutucuklarının olduğu yere tek dokunuşla aç/kapat " +
                    "düğmesi ekler; uygulamayı açmadan bağlanırsınız.",
                icon = Icons.Rounded.Bolt,
                onClick = onRequestTile,
                trailing = {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            if (DeviceInfo.isSamsung()) {
                RowDivider()
                SwitchRow(
                    title = "Now Bar'da göster",
                    subtitle = "Tünel açıkken kilit ekranındaki Now Bar'da ve durum çubuğunda canlı " +
                        "durum tutar; oradan tek dokunuşla kesebilirsiniz. (One UI ayarlarından " +
                        "Now Bar açık olmalıdır.)",
                    icon = Icons.Rounded.Notifications,
                    checked = settings.samsungVpnIndicator,
                    onCheckedChange = viewModel::setSamsungVpnIndicator,
                )
            }
        }

        // ---- Bağlantı ----
        SectionHeader("Bağlantı")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            SwitchRow(
                title = "Cihaz açılınca otomatik bağlan",
                subtitle = "Yeniden başlatmadan sonra tünel kendiliğinden kurulur.",
                icon = Icons.Rounded.PowerSettingsNew,
                checked = settings.autoConnectOnBoot,
                onCheckedChange = viewModel::setAutoConnectOnBoot,
            )
            RowDivider()
            SwitchRow(
                title = "UDP/QUIC'i tünelde düşür",
                subtitle = "Kapalı tutun. Açarsanız Discord'da sesli sohbet çalışmaz; yalnızca " +
                    "QUIC yüzünden takılan nadir durumlar için vardır.",
                icon = Icons.Rounded.Block,
                checked = settings.disableQuic,
                onCheckedChange = viewModel::setDisableQuic,
            )
        }

        // ---- DNS ----
        SectionHeader("DNS (DoH)")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            ListRow(
                title = "DoH sağlayıcı",
                subtitle = settings.dohProvider.displayName,
                icon = Icons.Rounded.Dns,
                onClick = { dohPickerOpen = true },
                trailing = {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            RowDivider()
            Column(Modifier.padding(18.dp)) {
                Text(
                    text = "Sağlayıcılar düz DNS'i ele geçirir; DoH sunucusuna IP ile bağlanılarak " +
                        "bu yönlendirme aşılır. İsterseniz kendi DoH adresinizi yazın.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VSpace(12.dp)
                AppTextField(
                    value = settings.customDohUrl,
                    onValueChange = viewModel::setCustomDohUrl,
                    placeholder = "https://1.1.1.1/dns-query",
                )
            }
        }

        // ---- Ek alan adları ----
        SectionHeader("Ek engellenen alan adları")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    text = "Otomatik testte bu alan adları da denenir. Virgülle ayırın.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VSpace(12.dp)
                AppTextField(
                    value = settings.extraBlockedHosts,
                    onValueChange = viewModel::setExtraBlockedHosts,
                    placeholder = "discord.com, media.discordapp.net",
                    singleLine = false,
                )
            }
        }

        // ---- Görünüm ----
        SectionHeader("Görünüm ve his")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                SegmentedControl(
                    options = listOf(
                        Segment(ThemePref.System, "Sistem", Icons.Rounded.BrightnessAuto),
                        Segment(ThemePref.Light, "Açık", Icons.Rounded.LightMode),
                        Segment(ThemePref.Dark, "Koyu", Icons.Rounded.DarkMode),
                    ),
                    selected = settings.theme,
                    onSelect = viewModel::setTheme,
                )
            }
            RowDivider()
            SwitchRow(
                title = "Dokunsal geri bildirim",
                subtitle = "Bağlanma, seçim ve sekme değişimlerinde hafif titreşim.",
                icon = Icons.Rounded.Vibration,
                checked = settings.haptics,
                onCheckedChange = viewModel::setHaptics,
            )
        }

        // ---- Pil ----
        SectionHeader("Pil")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.BatteryChargingFull,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Arka planda kesintisiz çalışsın",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                VSpace(8.dp)
                Text(
                    text = "Pil optimizasyonunu kapatın. Samsung cihazlarda ayrıca Ayarlar → Pil → " +
                        "Arka plan kullanım limitleri'nden uygulamayı \"Sınırsız\" yapın.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VSpace(14.dp)
                AppButton(
                    text = "Pil optimizasyonunu kapat",
                    onClick = {
                        val intent = Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            .setData(Uri.parse("package:${context.packageName}"))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            context.startActivity(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                    },
                    tone = ButtonTone.Tonal,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // ---- Hakkında ----
        SectionHeader("Hakkında")
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                )
                Text(
                    text = "DPI Bypass",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Sürüm ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RowDivider()
            Column(Modifier.padding(18.dp)) {
                Text(
                    text = "Bu araç kişisel ve yasal erişim içindir. Trafiğinizi ŞİFRELEMEZ, IP'nizi " +
                        "gizlemez; yalnızca DPI'ın SNI/Host okumasını bozar ve DNS'i DoH ile çözer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VSpace(8.dp)
                Text(
                    text = "Lisans GPL-3.0 · Bileşenler: ByeDPI (hufrea), hev-socks5-tunnel (heiher) · " +
                        "Referans mimari: ByeDPIAndroid (dovecoteescapee).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RowDivider()
            ListRow(
                title = "Kurulum sihirbazını tekrar aç",
                subtitle = "Karşılama adımlarını baştan gösterir.",
                icon = Icons.Rounded.RestartAlt,
                onClick = { viewModel.setOnboardingDone(false) },
                trailing = {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }

        VSpace(8.dp)
    }

    if (dohPickerOpen) {
        ChoiceDialog(
            title = "DoH sağlayıcı",
            options = DohProvider.entries.map { it to it.displayName },
            selected = settings.dohProvider,
            onSelect = viewModel::setDohProvider,
            onDismiss = { dohPickerOpen = false },
        )
    }
}
