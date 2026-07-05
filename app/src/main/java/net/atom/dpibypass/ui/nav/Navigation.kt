package net.atom.dpibypass.ui.nav

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import net.atom.dpibypass.ui.components.accentGradient

enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Ana", Icons.Rounded.Home),
    Mode("mode", "Mod", Icons.Rounded.Tune),
    Apps("apps", "Uygulamalar", Icons.Rounded.Apps),
    Settings("settings", "Ayarlar", Icons.Rounded.Settings),
}

/**
 * Alt yüzen hap (pill) navigasyon çubuğu — One UI 8.5 hissi: içeriğin üstünde
 * yüzer, alt bara sabit oturmaz, tek elle erişilebilir.
 */
@Composable
fun BottomPillBar(navController: NavController, modifier: Modifier = Modifier) {
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: Dest.Home.route

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Dest.entries.forEach { dest ->
                val selected = current == dest.route
                Surface(
                    onClick = {
                        if (!selected) {
                            navController.navigate(dest.route) {
                                popUpTo(Dest.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                ) {
                    val iconModifier = if (selected) {
                        Modifier
                            .background(accentGradient(), RoundedCornerShape(50))
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    } else {
                        Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                    }
                    Icon(
                        imageVector = dest.icon,
                        contentDescription = dest.label,
                        tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = iconModifier,
                    )
                }
            }
        }
    }
}
