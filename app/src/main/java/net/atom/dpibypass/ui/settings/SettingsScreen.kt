package net.atom.dpibypass.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.atom.dpibypass.BuildConfig
import net.atom.dpibypass.data.ThemePref
import net.atom.dpibypass.dns.DohProvider
import net.atom.dpibypass.ui.AppViewModel
import net.atom.dpibypass.ui.components.GlassCard
import net.atom.dpibypass.ui.components.PillButton
import net.atom.dpibypass.ui.components.SectionTitle

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = topInset + 12.dp, bottom = 120.dp),
    ) {
        Text("Ayarlar", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(16.dp))

        // ---- DNS / DoH ----
        SectionTitle("DNS (DoH)")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    "DoH zorunludur: TT/Superonline/Vodafone düz DNS'i ele geçirir. DoH sunucusuna " +
                        "IP ile bağlanılır, hijack aşılır.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                DohDropdown(selected = settings.dohProvider, onSelect = viewModel::setDohProvider)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = settings.customDohUrl,
                    onValueChange = viewModel::setCustomDohUrl,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Özel DoH URL (opsiyonel)") },
                    placeholder = { Text("https://1.1.1.1/dns-query") },
                    singleLine = true,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("Bağlantı")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SwitchRow(
                    title = "UDP/QUIC'i tünelde bırakma",
                    subtitle = "Bazı DPI'lar QUIC'i ayrı işler. Açıkça UDP düşürülür (DNS/sesli görüşmeyi etkileyebilir).",
                    checked = settings.disableQuic,
                    onCheckedChange = viewModel::setDisableQuic,
                )
                Divider()
                SwitchRow(
                    title = "Cihaz açılınca otomatik bağlan",
                    subtitle = "Yeniden başlatmadan sonra tünel otomatik kurulur.",
                    checked = settings.autoConnectOnBoot,
                    onCheckedChange = viewModel::setAutoConnectOnBoot,
                )
                Divider()
                SwitchRow(
                    title = "Haptik / dokunsal geri bildirim",
                    subtitle = "Bağlanınca hafif titreşim.",
                    checked = settings.haptics,
                    onCheckedChange = viewModel::setHaptics,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("Engellenen ek domainler")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = settings.extraBlockedHosts,
                onValueChange = viewModel::setExtraBlockedHosts,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Virgülle ayırın") },
                placeholder = { Text("discord.com, media.discordapp.net") },
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("Görünüm")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Sistem", { viewModel.setTheme(ThemePref.System) },
                    selected = settings.theme == ThemePref.System, modifier = Modifier.weight(1f))
                PillButton("Açık", { viewModel.setTheme(ThemePref.Light) },
                    selected = settings.theme == ThemePref.Light, modifier = Modifier.weight(1f))
                PillButton("Koyu", { viewModel.setTheme(ThemePref.Dark) },
                    selected = settings.theme == ThemePref.Dark, modifier = Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("Pil")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    "Arka planda kesintisiz çalışmak için pil optimizasyonunu kapatın. Samsung " +
                        "cihazlarda ayrıca Ayarlar → Piller → Arka plan kullanım limitleri'nden " +
                        "uygulamayı \"Sınırsız\" yapın.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                PillButton(
                    "Pil optimizasyonunu kapat",
                    onClick = {
                        val intent = Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            .setData(Uri.parse("package:${context.packageName}"))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            context.startActivity(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                    },
                    selected = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("Hakkında")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("DPI Bypass", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text("Sürüm ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Bu araç kişisel, yasal erişim amaçlıdır. Trafiğinizi ŞİFRELEMEZ; yalnızca " +
                        "DPI'ın SNI/Host okumasını bozar ve DNS'i DoH ile çözer. Gizlilik/anonimlik " +
                        "gerekiyorsa VPN ayrı bir konudur.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Lisans: GPL-3.0. Bileşenler: ByeDPI (hufrea/byedpi), hev-socks5-tunnel " +
                        "(heiher). Referans mimari: ByeDPIAndroid (dovecoteescapee).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DohDropdown(selected: DohProvider, onSelect: (DohProvider) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        GlassCard(
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(selected.displayName, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                Icon(Icons.Rounded.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DohProvider.entries.forEach { p ->
                DropdownMenuItem(text = { Text(p.displayName) }, onClick = { onSelect(p); expanded = false })
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun Divider() {
    androidx.compose.material3.HorizontalDivider(
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(vertical = 6.dp),
    )
}
