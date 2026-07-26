package net.atom.dpibypass.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import net.atom.dpibypass.ui.theme.Motion

// ---------------------------------------------------------------------------
// Giriş (entrance) animasyonu.
//
// Ekranlar hazır hâlde "yapışmaz"; içerik blokları sırayla, birkaç kare arayla
// aşağıdan yukarı süzülerek ve hafifçe büyüyerek belirir. Bu kademeli giriş
// (stagger) gözü doğal okuma sırasına — yukarıdan aşağı — yönlendirir ve
// açılışı hızlı hissettirir.
//
// Üç şey aynı anda değişir ama farklı hızlarda: opaklık en hızlı biter, konum
// ortada, ölçek en son oturur. Hepsi aynı anda bitseydi hareket "mekanik"
// görünürdü; kademe farkı ona ağırlık kazandırır.
//
// Tüm okuma graphicsLayer içinde, yani ÇİZİM aşamasında yapılır: animasyon
// boyunca tek bir yeniden kompozisyon bile tetiklenmez.
// ---------------------------------------------------------------------------

@Composable
fun rememberEntrance(durationMs: Int = 760): State<Float> {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    return animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMs, easing = Motion.EmphasizedDecelerate),
        label = "entrance",
    )
}

/** Blok başına gecikme oranı — [index] büyüdükçe blok biraz daha geç belirir. */
private const val STAGGER_STEP = 0.075f

/** Her bloğun kendi animasyonunun kapladığı oran (üst üste biner). */
private const val BLOCK_SPAN = 0.5f

fun Modifier.entrance(progress: () -> Float, index: Int = 0): Modifier = graphicsLayer {
    val local = ((progress() - index * STAGGER_STEP) / BLOCK_SPAN).coerceIn(0f, 1f)
    // Opaklık önce dolar (kare 0.75'te tamam), hareket ve ölçek arkadan gelir.
    alpha = (local / 0.75f).coerceAtMost(1f)
    translationY = (1f - local) * 28.dp.toPx()
    val s = 0.96f + 0.04f * local
    scaleX = s
    scaleY = s
    transformOrigin = TransformOrigin(0.5f, 0f)
}
