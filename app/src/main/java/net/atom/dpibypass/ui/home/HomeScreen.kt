package net.atom.dpibypass.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.atom.dpibypass.data.ConnectionState
import net.atom.dpibypass.ui.AppViewModel
import net.atom.dpibypass.ui.components.GlassCard
import net.atom.dpibypass.ui.components.accentGradientVertical
import net.atom.dpibypass.ui.theme.DangerRed

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val state by viewModel.connectionState.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = topInset + 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Büyük başlık (One UI large title)
        Text(
            text = "DPI Bypass",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Kişisel, yasal erişim aracı",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))

        // Aktif ağ kartı
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (settings.operationMode.name == "Auto") "Otomatik mod" else "Manuel mod",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = profile?.ispName ?: settings.selectedIsp.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ModeBadge(auto = settings.operationMode.name == "Auto")
            }
        }

        Spacer(Modifier.height(40.dp))

        // Büyük yuvarlak bağlan/kes butonu
        ConnectButton(
            state = state,
            onClick = {
                if (state == ConnectionState.Connected ||
                    state == ConnectionState.Connecting ||
                    state == ConnectionState.Testing
                ) onDisconnect() else onConnect()
            },
        )

        Spacer(Modifier.height(28.dp))

        Text(
            text = statusText(state, profile?.shortLabel()),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ModeBadge(auto: Boolean) {
    Box(
        modifier = Modifier
            .background(
                if (auto) accentGradientVertical() else Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                ),
                RoundedCornerShape(50),
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = if (auto) "OTO" else "MANUEL",
            color = if (auto) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ConnectButton(state: ConnectionState, onClick: () -> Unit) {
    val connected = state == ConnectionState.Connected
    val busy = state == ConnectionState.Connecting || state == ConnectionState.Testing
    val failed = state == ConnectionState.Failed

    // Bağlıyken hafif nabız animasyonu.
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (connected || busy) 1.04f else 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "pulseScale",
    )

    val ringColor = when {
        failed -> DangerRed
        else -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .size(230.dp)
            .scale(pulse)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Dış glow / halka
        Box(
            modifier = Modifier
                .size(230.dp)
                .border(2.dp, ringColor, CircleShape),
        )
        // İç dolgu
        Box(
            modifier = Modifier
                .size(190.dp)
                .background(
                    if (connected) accentGradientVertical()
                    else Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                        )
                    ),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = when {
                    connected -> "BAĞLI"
                    busy -> "…"
                    else -> "BAĞLAN"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (connected) Color.White else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun statusText(state: ConnectionState, label: String?): String = when (state) {
    ConnectionState.Disconnected -> "Bağlı değil"
    ConnectionState.Testing -> "Stratejiler test ediliyor…"
    ConnectionState.Connecting -> "Bağlanıyor…"
    ConnectionState.Connected -> "Bağlı · ${label ?: ""}".trim().trimEnd('·', ' ')
    ConnectionState.Failed -> "Bağlantı başarısız"
}
