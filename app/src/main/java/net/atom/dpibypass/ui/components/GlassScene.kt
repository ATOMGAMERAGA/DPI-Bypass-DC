package net.atom.dpibypass.ui.components

import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import net.atom.dpibypass.ui.theme.AccentBlue
import net.atom.dpibypass.ui.theme.AccentCyan
import net.atom.dpibypass.ui.theme.AuroraTeal
import net.atom.dpibypass.ui.theme.AuroraViolet
import net.atom.dpibypass.ui.theme.BgDarkBottom
import net.atom.dpibypass.ui.theme.BgDarkTop
import net.atom.dpibypass.ui.theme.BgLightBottom
import net.atom.dpibypass.ui.theme.BgLightTop
import kotlin.math.min

/**
 * Yumuşak renk lekesi ("aurora"). Konum ve yarıçap, sahnenin boyutuna göre oran
 * (0..1) olarak tanımlanır; böylece hem tam ekran zeminde hem de cam yüzeylerin
 * içindeki bulanık kopyada aynı yerde çizilir ve hizalı görünür.
 */
@Immutable
internal data class AuroraBlob(val cx: Float, val cy: Float, val radius: Float, val color: Color)

/**
 * Tüm ekranı kaplayan zemin sahnesi: taban gradyanı + aurora ışıkları. Cam
 * yüzeyler kendi pencere konumlarını bilir ve bu sahnenin arkalarına denk gelen
 * bölümünü bulanıklaştırarak "buzlu cam" (backdrop blur) etkisi verir.
 */
@Immutable
class GlassScene internal constructor(
    internal val rootInWindow: Offset,
    internal val size: IntSize,
    internal val baseTop: Color,
    internal val baseBottom: Color,
    private val blobs: List<AuroraBlob>,
) {
    /**
     * Sahneyi çizer. [drift] tam ekran zeminde animasyonlu (canlı his) verilir;
     * cam yüzeyler ise sabit (drift=0) çizer — böylece bulanıklık karesi önbelleğe
     * alınır ve her karede yeniden hesaplanmaz (performans + pil).
     */
    internal fun DrawScope.paint(drift: Float) {
        val w = size.width.toFloat()
        val h = size.height.toFloat()
        if (w <= 0f || h <= 0f) return
        drawRect(Brush.verticalGradient(listOf(baseTop, baseBottom), startY = 0f, endY = h))
        val minDim = min(w, h)
        blobs.forEach { b ->
            val center = Offset(b.cx * w, (b.cy + drift) * h)
            val r = b.radius * minDim
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(b.color, Color.Transparent),
                    center = center,
                    radius = r,
                ),
                radius = r,
                center = center,
            )
        }
    }
}

internal val LocalGlassScene = compositionLocalOf<GlassScene?> { null }

private fun darkBlobs(): List<AuroraBlob> = listOf(
    AuroraBlob(0.12f, 0.06f, 0.62f, AccentCyan.copy(alpha = 0.40f)),
    AuroraBlob(0.92f, 0.14f, 0.55f, AccentBlue.copy(alpha = 0.42f)),
    AuroraBlob(0.82f, 0.80f, 0.66f, AuroraViolet.copy(alpha = 0.36f)),
    AuroraBlob(0.08f, 0.94f, 0.58f, AuroraTeal.copy(alpha = 0.30f)),
)

private fun lightBlobs(): List<AuroraBlob> = listOf(
    AuroraBlob(0.12f, 0.06f, 0.60f, AccentCyan.copy(alpha = 0.22f)),
    AuroraBlob(0.92f, 0.14f, 0.55f, AccentBlue.copy(alpha = 0.20f)),
    AuroraBlob(0.82f, 0.80f, 0.64f, AuroraViolet.copy(alpha = 0.16f)),
    AuroraBlob(0.08f, 0.94f, 0.56f, AuroraTeal.copy(alpha = 0.16f)),
)

/**
 * Uygulamanın zemini. İçindeki [content] cam yüzeyleri, [LocalGlassScene] üzerinden
 * bu sahnenin arkalarına gelen bölümünü okuyup bulanıklaştırır.
 */
@Composable
fun AuroraBackground(
    dark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var rootInWindow by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    // Aurora ışıkları çok yavaşça yukarı-aşağı süzülür — canlı ama dikkat dağıtmayan
    // "premium" his. drift State olarak tutulur ve yalnızca çizimde okunur (aşağıya bkz).
    val transition = rememberInfiniteTransition(label = "aurora")
    val driftState = transition.animateFloat(
        initialValue = -0.02f,
        targetValue = 0.02f,
        animationSpec = infiniteRepeatable(tween(9000), RepeatMode.Reverse),
        label = "auroraDrift",
    )

    val baseTop = if (dark) BgDarkTop else BgLightTop
    val baseBottom = if (dark) BgDarkBottom else BgLightBottom
    val blobs = remember(dark) { if (dark) darkBlobs() else lightBlobs() }
    val scene = GlassScene(rootInWindow, size, baseTop, baseBottom, blobs)

    Box(
        modifier
            .fillMaxSize()
            .onGloballyPositioned {
                rootInWindow = it.positionInWindow()
                size = it.size
            }
            // drift yalnızca çizim anında okunur → yeniden çizim, recomposition değil.
            .drawBehind { with(scene) { paint(driftState.value) } },
    ) {
        CompositionLocalProvider(LocalGlassScene provides scene) {
            content()
        }
    }
}

/**
 * Gerçek buzlu-cam yüzey. Arkasındaki aurora sahnesinin ilgili bölümünü
 * [blurRadius] kadar bulanıklaştırıp yarı saydam bir renk tabakası ve ince,
 * ışıklı bir kenarla kaplar. Android 12+ (API 31) donanımsal RenderEffect kullanır;
 * daha eski sürümlerde bulanıklık yerine yalnızca yarı saydam cam görünür.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    blurRadius: Dp = 34.dp,
    // Daha belirgin "buzlu cam" hissi: yüzey rengi arkadaki aurora'yı yeterince
    // örter ama yine de içinden hafif sızdırır (backdrop blur'la birlikte "dock" dokusu).
    tint: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
    content: @Composable BoxScope.() -> Unit,
) {
    val scene = LocalGlassScene.current
    var posInWindow by remember { mutableStateOf(Offset.Zero) }

    val borderBrush = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
        ),
    )
    val sheen = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.07f), Color.Transparent),
    )

    Box(
        modifier
            .clip(shape)
            .onGloballyPositioned { posInWindow = it.positionInWindow() },
    ) {
        if (scene != null && scene.size.width > 0 && scene.size.height > 0) {
            val offset = posInWindow - scene.rootInWindow
            val blurMod = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.blur(blurRadius, BlurredEdgeTreatment.Unbounded)
            } else {
                Modifier
            }
            Canvas(Modifier.matchParentSize().then(blurMod)) {
                translate(left = -offset.x, top = -offset.y) {
                    with(scene) { paint(drift = 0f) }
                }
            }
        }
        // Yarı saydam renk tabakası + üstten hafif parlaklık (buzlu cam hissi).
        Box(Modifier.matchParentSize().background(tint))
        Box(Modifier.matchParentSize().background(sheen))
        content()
        // İnce, ışıklı kenar en üstte çizilir (aksi halde arka plan kopyası örter).
        Box(Modifier.matchParentSize().border(BorderStroke(1.dp, borderBrush), shape))
    }
}
