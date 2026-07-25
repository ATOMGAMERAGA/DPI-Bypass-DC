package net.atom.dpibypass.ui.design

import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import net.atom.dpibypass.ui.theme.AuroraTeal
import net.atom.dpibypass.ui.theme.AuroraViolet
import net.atom.dpibypass.ui.theme.BgDarkBottom
import net.atom.dpibypass.ui.theme.BgDarkTop
import net.atom.dpibypass.ui.theme.BgLightBottom
import net.atom.dpibypass.ui.theme.BgLightTop
import net.atom.dpibypass.ui.theme.BrandBlue
import net.atom.dpibypass.ui.theme.BrandCyan
import net.atom.dpibypass.ui.theme.LocalIsDarkTheme
import net.atom.dpibypass.ui.theme.Motion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Ortam (ambient) zemin + buzlu cam.
//
// One UI 8.5'in "Ambient Design" dili tam olarak budur: zemin düz bir blok değil,
// yavaşça akan ışıktan oluşan bir sahnedir; üstteki yüzeyler o sahneyi gerçekten
// bulanıklaştırarak (backdrop blur) yüzer görünür.
//
// Buradaki kritik tasarım kararı: cam UCUZ DEĞİLDİR. Her cam yüzey, arkasındaki
// sahneyi kendi tuvaline yeniden çizip bulanıklaştırır. Bu yüzden cam yalnızca
// "kabuk" için kullanılır (üst başlık şeridi, yüzen dock, kahraman daire,
// diyaloglar). Listeler ve kartlar OPAK yüzey basamaklarını kullanır — hem 60/120
// fps'te akıcı kalır hem de metin kontrastı her zeminde garanti olur.
// ---------------------------------------------------------------------------

@Immutable
internal data class AuroraBlob(
    val cx: Float,
    val cy: Float,
    val radius: Float,
    val color: Color,
    val ax: Float = 0f,
    val ay: Float = 0f,
    val phase: Float = 0f,
    /** 0 → kendi rengi, 1 → tamamen durum rengi (bağlantı durumuna tepki verir). */
    val accentMix: Float = 0f,
)

/**
 * Zemin sahnesi. Cam yüzeyler kendi pencere konumlarını bilir ve sahnenin
 * arkalarına denk gelen bölümünü çizip bulanıklaştırır — böylece cam gerçekten
 * "arkasındakini" gösterir, sahte bir gri katman olmaz.
 */
class AmbientScene internal constructor(
    internal val rootInWindow: Offset,
    internal val size: IntSize,
    internal val baseTop: Color,
    internal val baseBottom: Color,
    private val blobs: List<AuroraBlob>,
    private val dark: Boolean,
    internal val accent: () -> Color,
    internal val progress: () -> Float,
) {
    internal fun DrawScope.paint(progress: Float, accent: Color) {
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
            val tinted = if (b.accentMix > 0f) {
                lerp(b.color, accent.copy(alpha = b.color.alpha), b.accentMix)
            } else {
                b.color
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tinted, Color.Transparent),
                    center = center,
                    radius = r,
                ),
                radius = r,
                center = center,
            )
        }

        // Kenar karartması (vignette): göz merkeze, yani içeriğe çekilir.
        if (dark) {
            drawRect(
                Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.42f)),
                    center = Offset(w / 2f, h * 0.36f),
                    radius = minDim * 1.05f,
                ),
            )
        }
    }

    private companion object {
        const val TWO_PI = (2.0 * PI).toFloat()
    }
}

internal val LocalAmbientScene = compositionLocalOf<AmbientScene?> { null }

// Büyük ve yumuşak ortam ışıkları + küçük parlak "bokeh" lekeleri. Küçük lekeler
// cam yüzeylerin arkasında bulanıklaşınca kayan ışık dokusunu verir.
private fun darkBlobs(): List<AuroraBlob> = listOf(
    AuroraBlob(0.16f, 0.06f, 0.62f, BrandCyan.copy(alpha = 0.30f), 0.05f, 0.04f, 0.0f, accentMix = 0.75f),
    AuroraBlob(0.90f, 0.14f, 0.55f, BrandBlue.copy(alpha = 0.34f), 0.06f, 0.05f, 1.7f, accentMix = 0.35f),
    AuroraBlob(0.86f, 0.84f, 0.62f, AuroraViolet.copy(alpha = 0.26f), 0.05f, 0.06f, 3.2f),
    AuroraBlob(0.08f, 0.92f, 0.56f, AuroraTeal.copy(alpha = 0.22f), 0.06f, 0.05f, 4.8f),
    AuroraBlob(0.30f, 0.30f, 0.16f, BrandCyan.copy(alpha = 0.40f), 0.16f, 0.11f, 0.8f, accentMix = 0.6f),
    AuroraBlob(0.74f, 0.52f, 0.14f, BrandBlue.copy(alpha = 0.38f), 0.13f, 0.15f, 2.5f),
    AuroraBlob(0.46f, 0.76f, 0.12f, Color.White.copy(alpha = 0.10f), 0.18f, 0.10f, 4.1f),
)

private fun lightBlobs(): List<AuroraBlob> = listOf(
    AuroraBlob(0.16f, 0.06f, 0.60f, BrandCyan.copy(alpha = 0.20f), 0.05f, 0.04f, 0.0f, accentMix = 0.7f),
    AuroraBlob(0.90f, 0.14f, 0.55f, BrandBlue.copy(alpha = 0.18f), 0.06f, 0.05f, 1.7f, accentMix = 0.3f),
    AuroraBlob(0.86f, 0.84f, 0.62f, AuroraViolet.copy(alpha = 0.14f), 0.05f, 0.06f, 3.2f),
    AuroraBlob(0.08f, 0.92f, 0.56f, AuroraTeal.copy(alpha = 0.14f), 0.06f, 0.05f, 4.8f),
    AuroraBlob(0.30f, 0.30f, 0.16f, BrandCyan.copy(alpha = 0.22f), 0.16f, 0.11f, 0.8f, accentMix = 0.5f),
    AuroraBlob(0.74f, 0.52f, 0.14f, BrandBlue.copy(alpha = 0.20f), 0.13f, 0.15f, 2.5f),
    AuroraBlob(0.46f, 0.76f, 0.12f, Color.White.copy(alpha = 0.55f), 0.18f, 0.10f, 4.1f),
)

/**
 * Sistemde animasyonlar kapatılmış mı (Geliştirici seçenekleri / erişilebilirlik)?
 * Kapatılmışsa sonsuz döngülü animasyonlar durur — hem erişilebilirlik hem pil için.
 */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            AndroidSettings.Global.getFloat(
                context.contentResolver,
                AndroidSettings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) != 0f
        }.getOrDefault(true)
    }
}

/**
 * Uygulamanın zemini. [accent] bağlantı durumunun rengidir: zemin, bağlandığında
 * yeşile, hata durumunda kırmızıya doğru YUMUŞAKÇA kayar. Kullanıcı ekranın
 * herhangi bir yerine baksa bile durumu çevresel görüşüyle fark eder.
 */
@Composable
fun AmbientBackground(
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val dark = LocalIsDarkTheme.current
    var rootInWindow by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    val animated = rememberAnimationsEnabled()
    val transition = rememberInfiniteTransition(label = "ambient")
    val progressState: State<Float> = transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animated) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(Motion.AURORA_CYCLE_MS, easing = LinearEasing)),
        label = "ambientProgress",
    )
    // Durum rengi geçişi: renk animasyonu ASLA taşımaz (effects yayı).
    val accentState = animateColorAsState(
        targetValue = accent,
        animationSpec = Motion.effectsSlow(),
        label = "ambientAccent",
    )

    val baseTop = if (dark) BgDarkTop else BgLightTop
    val baseBottom = if (dark) BgDarkBottom else BgLightBottom
    val blobs = remember(dark) { if (dark) darkBlobs() else lightBlobs() }
    val scene = AmbientScene(
        rootInWindow = rootInWindow,
        size = size,
        baseTop = baseTop,
        baseBottom = baseBottom,
        blobs = blobs,
        dark = dark,
        accent = { accentState.value },
        progress = { progressState.value },
    )

    Box(
        modifier
            .fillMaxSize()
            .onGloballyPositioned {
                rootInWindow = it.positionInWindow()
                size = it.size
            }
            // Animasyon değerleri YALNIZCA çizim anında okunur: her karede yeniden
            // çizim olur, yeniden kompozisyon (recomposition) olmaz.
            .drawBehind { with(scene) { paint(scene.progress(), scene.accent()) } },
    ) {
        CompositionLocalProvider(LocalAmbientScene provides scene) {
            content()
        }
    }
}

/**
 * Buzlu cam yüzey (backdrop blur). Android 12+ donanımsal RenderEffect kullanır;
 * daha eskilerde bulanıklık yerine yoğunluğu artırılmış yarı saydam cam görünür —
 * her iki durumda da metin kontrastı korunur.
 *
 * Pahalıdır: yalnızca yüzen kabuk öğelerinde kullanın (dock, başlık şeridi,
 * kahraman daire, diyalog). Listelerde [AppCard] kullanın.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    blurRadius: Dp = 36.dp,
    tint: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.60f),
    borderAlpha: Float = 0.16f,
    content: @Composable BoxScope.() -> Unit,
) {
    val scene = LocalAmbientScene.current
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var posInWindow by remember { mutableStateOf(Offset.Zero) }

    val onSurface = MaterialTheme.colorScheme.onSurface
    val borderBrush = Brush.verticalGradient(
        listOf(onSurface.copy(alpha = borderAlpha), onSurface.copy(alpha = borderAlpha * 0.25f)),
    )
    // Üst kenarda ince ışık (specular): camın kalınlığını hissettirir.
    val sheen = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = if (LocalIsDarkTheme.current) 0.08f else 0.35f), Color.Transparent),
    )
    // Blur yoksa cam daha opak olmalı, yoksa arkadaki keskin içerik sızar.
    val effectiveTint = if (supportsBlur) tint else tint.copy(alpha = (tint.alpha + 0.28f).coerceAtMost(0.97f))

    Box(
        modifier
            .clip(shape)
            .onGloballyPositioned { posInWindow = it.positionInWindow() },
    ) {
        if (scene != null && scene.size.width > 0 && scene.size.height > 0) {
            val offset = posInWindow - scene.rootInWindow
            val blurMod = if (supportsBlur) {
                Modifier.blur(blurRadius, BlurredEdgeTreatment.Unbounded)
            } else {
                Modifier
            }
            Canvas(Modifier.matchParentSize().then(blurMod)) {
                translate(left = -offset.x, top = -offset.y) {
                    with(scene) { paint(scene.progress(), scene.accent()) }
                }
            }
        }
        Box(Modifier.matchParentSize().background(effectiveTint))
        Box(Modifier.matchParentSize().background(sheen))
        content()
        Box(Modifier.matchParentSize().border(BorderStroke(1.dp, borderBrush), shape))
    }
}
