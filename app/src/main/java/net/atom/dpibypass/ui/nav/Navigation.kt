package net.atom.dpibypass.ui.nav

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import net.atom.dpibypass.ui.design.GlassLevel
import net.atom.dpibypass.ui.design.GlassSurface
import net.atom.dpibypass.ui.design.LocalChrome
import net.atom.dpibypass.ui.design.LocalShellBackdrop
import net.atom.dpibypass.ui.design.dockCollapseDistancePx
import net.atom.dpibypass.ui.design.rememberHaptics
import net.atom.dpibypass.ui.theme.LocalStateColors
import net.atom.dpibypass.ui.theme.Motion
import net.atom.dpibypass.ui.theme.PillShape
import kotlin.math.abs
import kotlin.math.min

enum class Dest(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector,
) {
    Home("home", "Durum", Icons.Outlined.Shield, Icons.Rounded.Shield),
    Mode("mode", "Strateji", Icons.Outlined.Tune, Icons.Rounded.Tune),
    Apps("apps", "Uygulamalar", Icons.Outlined.Apps, Icons.Rounded.Apps),
    Settings("settings", "Ayarlar", Icons.Outlined.Settings, Icons.Rounded.Settings),
}

private val ItemWidth = 62.dp
private val ItemHeight = 48.dp
private val DockPadding = 7.dp

/**
 * Yüzen dock.
 *
 * Tasarım kararları:
 *
 *  * SADECE İKON. Dört sekmelik bir gezinmede etiket, ikonun anlattığından
 *    fazlasını söylemez; ama dock'un yüksekliğini iki katına çıkarır ve ekranın
 *    dibinde kalın bir bant bırakır. Etiketler kaldırıldı, ikonlar büyütüldü ve
 *    seçili/seçili değil ayrımı DOLU-BOŞ ikon çiftiyle yapıldı (Ayarlar ile
 *    Strateji ikonları artık karışmıyor). Erişilebilirlik için her hedefin adı
 *    contentDescription olarak korunuyor.
 *
 *  * GÖSTERGE AKAR, ATLAMAZ. Seçim göstergesi hedefe yayla gider ve giderken
 *    hareket yönünde UZAYIP dikeyde basılır (squash & stretch). Bu, hızlı
 *    sekmelerde bile gözün göstergeyi kaybetmemesini sağlayan klasik animasyon
 *    hilesidir. Duracağı yere yaklaşınca kendi biçimine geri döner.
 *
 *  * İKONLAR DEVREDER. Yeni sekmenin ikonu büyüyerek ve hafifçe yükselerek
 *    öne çıkar, eskisi aynı anda söner. İki ikon çaprazlama geçtiği için
 *    "yanıp sönme" hissi olmaz.
 *
 *  * DOCK EKRANA TEPKİ VERİR. Sayfa aşağı kaydırıldıkça dock küçülür ve biraz
 *    şeffaflaşır (içeriğe yer açar); en üste dönüldüğünde tam boyuna oturur.
 *    Bu bir yeniden yerleşim (relayout) değil, saf katman dönüşümüdür — yani
 *    kaydırma sırasında hiçbir maliyeti yoktur.
 */
@Composable
fun BottomDock(navController: NavController, modifier: Modifier = Modifier) {
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: Dest.Home.route
    val haptics = rememberHaptics()
    val chrome = LocalChrome.current
    val shell = LocalShellBackdrop.current
    val density = LocalDensity.current

    val dockDistancePx = dockCollapseDistancePx()
    val itemWidthPx = with(density) { ItemWidth.toPx() }

    val selectedIndex = Dest.entries.indexOfFirst { it.route == current }.coerceAtLeast(0)
    val target = selectedIndex.toFloat()
    val indicator = animateFloatAsState(
        targetValue = target,
        animationSpec = Motion.indicator(),
        label = "dockIndicator",
    )

    // Kaydırma tepkisi: ölçek + opaklık. Yalnızca çizim aşamasında okunur.
    val collapse = { Motion.Emphasized.transform(chrome.collapse(dockDistancePx)) }

    GlassSurface(
        modifier = modifier
            .graphicsLayer {
                val c = collapse()
                val s = 1f - 0.12f * c
                scaleX = s
                scaleY = s
                translationY = c * 6.dp.toPx()
                alpha = 1f - 0.16f * c
                transformOrigin = TransformOrigin(0.5f, 1f)
            },
        shape = PillShape,
        level = GlassLevel.Shell,
        backdrop = shell,
        borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
    ) {
        Box(Modifier.padding(DockPadding)) {
            DockIndicator(
                position = { indicator.value },
                target = target,
                itemWidthPx = itemWidthPx,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Dest.entries.forEachIndexed { index, dest ->
                    DockItem(
                        dest = dest,
                        selected = index == selectedIndex,
                        onClick = {
                            if (index == selectedIndex) {
                                haptics.tick()
                            } else {
                                haptics.select()
                                navController.navigate(dest.route) {
                                    popUpTo(Dest.Home.route) { saveState = false }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * Kayan seçim göstergesi. Hedefe olan uzaklık, hareket süresince şekle
 * dönüştürülür: uzakken yatayda uzar/dikeyde basılır, yaklaşınca toparlanır.
 */
@Composable
private fun DockIndicator(
    position: () -> Float,
    target: Float,
    itemWidthPx: Float,
) {
    val gradient = LocalStateColors.current.brandGradient()
    Box(
        modifier = Modifier
            .size(width = ItemWidth, height = ItemHeight)
            .graphicsLayer {
                val p = position()
                translationX = p * itemWidthPx
                // Hızın görsel karşılığı: kalan yol ne kadar uzunsa o kadar esner.
                val stretch = min(abs(target - p), 1f)
                scaleX = 1f + 0.26f * stretch
                scaleY = 1f - 0.18f * stretch
            }
            .clip(RoundedCornerShape(percent = 42))
            .background(gradient, RoundedCornerShape(percent = 42)),
    )
}

@Composable
private fun DockItem(dest: Dest, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // Tek bir "seçilmişlik" değeri; hem ikon değişimini hem büyümeyi sürer.
    val selection = animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = Motion.spatialDefault(),
        label = "dockSelection",
    )
    val press = animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = Motion.spatialFast(),
        label = "dockPress",
    )
    val idleTint by animateColorAsState(
        targetValue = scheme.onSurfaceVariant,
        animationSpec = Motion.effectsDefault(),
        label = "dockIdleTint",
    )

    Box(
        modifier = Modifier
            .width(ItemWidth)
            .height(ItemHeight)
            .clip(RoundedCornerShape(percent = 42))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = dest.label
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.graphicsLayer {
                val s = selection.value
                val scale = (1f + 0.14f * s) * (1f - 0.10f * press.value)
                scaleX = scale
                scaleY = scale
                // Seçili ikon bir tık yükselir: göstergenin içinde "oturmuş" durur.
                translationY = -1.5f * s * density
            },
            contentAlignment = Alignment.Center,
        ) {
            // Boş (outlined) ikon: seçim arttıkça söner.
            Icon(
                imageVector = dest.icon,
                contentDescription = null,
                modifier = Modifier
                    .size(23.dp)
                    .graphicsLayer { alpha = 1f - selection.value },
                tint = idleTint,
            )
            // Dolu ikon: seçim arttıkça belirir.
            Icon(
                imageVector = dest.iconSelected,
                contentDescription = null,
                modifier = Modifier
                    .size(23.dp)
                    .graphicsLayer { alpha = selection.value },
                tint = Color.White,
            )
        }
    }
}
