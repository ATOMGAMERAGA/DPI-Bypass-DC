package net.atom.dpibypass.ui.components

import android.os.Build
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.runtime.State
import net.atom.dpibypass.ui.theme.AccentBlue
import net.atom.dpibypass.ui.theme.AccentCyan
import net.atom.dpibypass.ui.theme.AuroraTeal
import net.atom.dpibypass.ui.theme.AuroraViolet
import net.atom.dpibypass.ui.theme.BgDarkBottom
import net.atom.dpibypass.ui.theme.BgDarkTop
import net.atom.dpibypass.ui.theme.BgLightBottom
import net.atom.dpibypass.ui.theme.BgLightTop
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Yumuşak renk lekesi ("aurora"). Konum ve yarıçap, sahnenin boyutuna göre oran
 * (0..1) olarak tanımlanır; böylece hem tam ekran zeminde hem de cam yüzeylerin
 * içindeki bulanık kopyada aynı yerde çizilir ve hizalı görünür.
 *
 * [ax]/[ay] her lekenin kendi elips yörüngesinde süzülme genliğidir (oran olarak).
 * [phase] başlangıç açısıdır — lekeler farklı fazlarla hareket eder, böylece zemin
 * tekdüze değil, canlı ve organik akar. Bu hareket cam yüzeylerin arkasında da
 * geçtiği için "buzlu cam" bulanıklığı artık gözle görülür bir doku kazanır.
 */
@Immutable
internal data class AuroraBlob(
    val cx: Float,
    val cy: Float,
    val radius: Float,
    val color: Color,
    val ax: Float = 0f,
    val ay: Float = 0f,
    val phase: Float = 0f,
)

/**
 * Tüm ekranı kaplayan zemin sahnesi: taban gradyanı + aurora ışıkları. Cam
 * yüzeyler kendi pencere konumlarını bilir ve bu sahnenin arkalarına denk gelen
 * bölümünü bulanıklaştırarak "buzlu cam" (backdrop blur) etkisi verir.
 */
class GlassScene internal constructor(
    internal val rootInWindow: Offset,
    internal val size: IntSize,
    internal val baseTop: Color,
    internal val baseBottom: Color,
    private val blobs: List<AuroraBlob>,
    // Canlı animasyon ilerlemesi (0..1 döngü). Bir lambda olarak tutulur ki hem tam
    // ekran zemin hem de cam yüzeyler ÇİZİM anında okuyup her karede yeniden çizsin
    // (recomposition değil). Böylece camın arkasındaki hareket bulanıklaşarak geçer.
    internal val progress: () -> Float,
) {
    /**
     * Sahneyi [progress] (0..1 döngü) anına göre çizer. Her leke kendi elips
     * yörüngesinde ([ax]/[ay], [phase]) süzülür; hem tam ekran zeminde hem de cam
     * yüzeylerin arkasındaki bulanık kopyada AYNI hesapla çizildiği için hareket
     * camın içinden hizalı ve akıcı görünür.
     */
    internal fun DrawScope.paint(progress: Float) {
        val w = size.width.toFloat()
        val h = size.height.toFloat()
        if (w <= 0f || h <= 0f) return
        drawRect(Brush.verticalGradient(listOf(baseTop, baseBottom), startY = 0f, endY = h))
        val minDim = min(w, h)
        val ang = progress * TWO_PI
        blobs.forEach { b ->
            val cx = (b.cx + b.ax * cos(ang + b.phase)) * w
            val cy = (b.cy + b.ay * sin(ang + b.phase)) * h
            val center = Offset(cx, cy)
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

    private companion object {
        const val TWO_PI = (2.0 * PI).toFloat()
    }
}

internal val LocalGlassScene = compositionLocalOf<GlassScene?> { null }

// Büyük, yumuşak ortam ışıkları (yavaş süzülür) + küçük, parlak "bokeh" lekeleri
// (daha geniş yörüngede, daha hızlı akar). Küçük parlak lekeler cam yüzeylerin
// arkasında bulanıklaşınca klasik buzlu-cam "kayan ışık" dokusunu verir — kullanıcının
// istediği "arkasında bir şey hareket eden gerçek blur" hissi buradan gelir.
private fun darkBlobs(): List<AuroraBlob> = listOf(
    AuroraBlob(0.15f, 0.10f, 0.58f, AccentCyan.copy(alpha = 0.38f), ax = 0.05f, ay = 0.04f, phase = 0.0f),
    AuroraBlob(0.88f, 0.16f, 0.52f, AccentBlue.copy(alpha = 0.42f), ax = 0.06f, ay = 0.05f, phase = 1.7f),
    AuroraBlob(0.84f, 0.82f, 0.60f, AuroraViolet.copy(alpha = 0.34f), ax = 0.05f, ay = 0.06f, phase = 3.2f),
    AuroraBlob(0.10f, 0.90f, 0.54f, AuroraTeal.copy(alpha = 0.28f), ax = 0.06f, ay = 0.05f, phase = 4.8f),
    // bokeh (küçük + parlak + geniş yörünge)
    AuroraBlob(0.30f, 0.34f, 0.17f, AccentCyan.copy(alpha = 0.52f), ax = 0.15f, ay = 0.11f, phase = 0.8f),
    AuroraBlob(0.72f, 0.54f, 0.15f, AccentBlue.copy(alpha = 0.50f), ax = 0.13f, ay = 0.14f, phase = 2.5f),
    AuroraBlob(0.48f, 0.74f, 0.13f, Color.White.copy(alpha = 0.16f), ax = 0.17f, ay = 0.10f, phase = 4.1f),
)

private fun lightBlobs(): List<AuroraBlob> = listOf(
    AuroraBlob(0.15f, 0.10f, 0.56f, AccentCyan.copy(alpha = 0.22f), ax = 0.05f, ay = 0.04f, phase = 0.0f),
    AuroraBlob(0.88f, 0.16f, 0.52f, AccentBlue.copy(alpha = 0.22f), ax = 0.06f, ay = 0.05f, phase = 1.7f),
    AuroraBlob(0.84f, 0.82f, 0.60f, AuroraViolet.copy(alpha = 0.16f), ax = 0.05f, ay = 0.06f, phase = 3.2f),
    AuroraBlob(0.10f, 0.90f, 0.54f, AuroraTeal.copy(alpha = 0.16f), ax = 0.06f, ay = 0.05f, phase = 4.8f),
    // bokeh
    AuroraBlob(0.30f, 0.34f, 0.17f, AccentCyan.copy(alpha = 0.26f), ax = 0.15f, ay = 0.11f, phase = 0.8f),
    AuroraBlob(0.72f, 0.54f, 0.15f, AccentBlue.copy(alpha = 0.26f), ax = 0.13f, ay = 0.14f, phase = 2.5f),
    AuroraBlob(0.48f, 0.74f, 0.13f, Color.White.copy(alpha = 0.30f), ax = 0.17f, ay = 0.10f, phase = 4.1f),
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

    // Lekeler tek bir sürekli döngü (0→1) boyunca kendi yörüngelerinde süzülür — canlı
    // ama dikkat dağıtmayan "premium" his. progress State olarak tutulur ve YALNIZCA
    // çizimde okunur; böylece animasyon yeniden çizim yapar, recomposition değil.
    val transition = rememberInfiniteTransition(label = "aurora")
    val progressState: State<Float> = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing)),
        label = "auroraProgress",
    )

    val baseTop = if (dark) BgDarkTop else BgLightTop
    val baseBottom = if (dark) BgDarkBottom else BgLightBottom
    val blobs = remember(dark) { if (dark) darkBlobs() else lightBlobs() }
    val scene = GlassScene(rootInWindow, size, baseTop, baseBottom, blobs) { progressState.value }

    Box(
        modifier
            .fillMaxSize()
            .onGloballyPositioned {
                rootInWindow = it.positionInWindow()
                size = it.size
            }
            // progress yalnızca çizim anında okunur → yeniden çizim, recomposition değil.
            .drawBehind { with(scene) { paint(scene.progress()) } },
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
    tint: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
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
                // Sahnenin CANLI anını (progress) çizim sırasında oku → cam yüzeyin
                // arkasındaki hareket her karede bulanıklaşarak geçer. Böylece blur
                // "düz saydam" değil, gerçek buzlu-cam gibi kayan ışıkla dolu görünür.
                translate(left = -offset.x, top = -offset.y) {
                    with(scene) { paint(scene.progress()) }
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
