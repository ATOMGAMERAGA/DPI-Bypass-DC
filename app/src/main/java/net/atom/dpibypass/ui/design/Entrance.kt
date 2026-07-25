package net.atom.dpibypass.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// Giriş (entrance) animasyonu.
//
// Ekranlar hazır hâlde "yapışmaz"; içerik blokları sırayla, birkaç kare arayla
// aşağıdan yukarı süzülerek belirir. Bu kademeli giriş (stagger) gözü doğal
// okuma sırasına — yukarıdan aşağı — yönlendirir ve açılışı hızlı hissettirir.
//
// Tüm okuma graphicsLayer içinde, yani ÇİZİM aşamasında yapılır: animasyon
// boyunca tek bir yeniden kompozisyon bile tetiklenmez.
// ---------------------------------------------------------------------------

@Composable
fun rememberEntrance(durationMs: Int = 700): State<Float> {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    return animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMs),
        label = "entrance",
    )
}

/**
 * [index] büyüdükçe blok biraz daha geç belirir (kademeli giriş).
 */
fun Modifier.entrance(progress: () -> Float, index: Int = 0): Modifier = graphicsLayer {
    val local = ((progress() - index * 0.07f) / 0.55f).coerceIn(0f, 1f)
    alpha = local
    translationY = (1f - local) * 26.dp.toPx()
}
