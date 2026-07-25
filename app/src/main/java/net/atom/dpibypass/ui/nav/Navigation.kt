package net.atom.dpibypass.ui.nav

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import net.atom.dpibypass.ui.design.GlassSurface
import net.atom.dpibypass.ui.design.pressScale
import net.atom.dpibypass.ui.design.rememberHaptics
import net.atom.dpibypass.ui.theme.LocalStateColors
import net.atom.dpibypass.ui.theme.Motion
import net.atom.dpibypass.ui.theme.PillShape

enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Durum", Icons.Rounded.Shield),
    Mode("mode", "Strateji", Icons.Rounded.Tune),
    Apps("apps", "Uygulama", Icons.Rounded.Apps),
    Settings("settings", "Ayarlar", Icons.Rounded.Settings),
}

/**
 * Yüzen dock.
 *
 * Üç bilinçli karar:
 *   * ETİKETLER GERİ GELDİ. Eskiden yalnızca ikon vardı; "ayar" ile "mod" ikonu
 *     birbirine benziyordu ve kullanıcı tahmin etmek zorunda kalıyordu. Metin,
 *     ikonun yerini tutmaz — ikonla birlikte çalışır.
 *   * Seçim göstergesi (indicator) ikonun arkasında yay fiziğiyle büyür; sekme
 *     değişimi ani bir renk sıçraması değil, izlenebilir bir hareket olur.
 *   * Her dokunuş hafif bir haptik tik verir; zaten seçili sekmeye basmak
 *     gezinme yapmaz, yalnızca tik atar (yanlışlıkla yeniden yükleme olmaz).
 */
@Composable
fun BottomDock(navController: NavController, modifier: Modifier = Modifier) {
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: Dest.Home.route
    val haptics = rememberHaptics()

    GlassSurface(
        modifier = modifier,
        shape = PillShape,
        blurRadius = 44.dp,
        // Dock içeriğin üzerinde yüzer: arkadan geçen yazı "hayalet" gibi sızmasın
        // diye cam burada bilerek daha yoğun.
        tint = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        borderAlpha = 0.20f,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Dest.entries.forEach { dest ->
                DockItem(
                    dest = dest,
                    selected = current == dest.route,
                    onClick = {
                        if (current == dest.route) {
                            haptics.tick()
                        } else {
                            haptics.select()
                            navController.navigate(dest.route) {
                                popUpTo(Dest.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DockItem(dest: Dest, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val stateColors = LocalStateColors.current
    val interaction = remember { MutableInteractionSource() }

    val indicator by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = Motion.spatialDefault(),
        label = "dockIndicator",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else scheme.onSurfaceVariant,
        animationSpec = Motion.effectsDefault(),
        label = "dockTint",
    )

    Column(
        modifier = Modifier
            .width(70.dp)
            .pressScale(interaction, pressedScale = 0.93f)
            .clip(MaterialTheme.shapes.large)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(30.dp)
                    .graphicsLayer {
                        alpha = indicator
                        scaleX = 0.6f + 0.4f * indicator
                        scaleY = 0.6f + 0.4f * indicator
                    }
                    .clip(PillShape)
                    .background(stateColors.brandGradient(), PillShape),
            )
            Icon(
                imageVector = dest.icon,
                contentDescription = dest.label,
                tint = contentColor,
                modifier = Modifier.size(21.dp),
            )
        }
        Text(
            text = dest.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
        )
    }
}
