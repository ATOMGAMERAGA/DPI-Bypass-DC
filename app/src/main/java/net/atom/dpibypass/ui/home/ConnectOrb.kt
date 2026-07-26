package net.atom.dpibypass.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.atom.dpibypass.data.ConnectionState
import net.atom.dpibypass.ui.design.GlassLevel
import net.atom.dpibypass.ui.design.GlassSurface
import net.atom.dpibypass.ui.design.connectionColor
import net.atom.dpibypass.ui.design.connectionIcon
import net.atom.dpibypass.ui.design.connectionLabel
import net.atom.dpibypass.ui.design.pressScale
import net.atom.dpibypass.ui.design.rememberAnimationsEnabled
import net.atom.dpibypass.ui.design.rememberHaptics
import net.atom.dpibypass.ui.theme.Motion
import net.atom.dpibypass.ui.theme.NumericMedium

// ---------------------------------------------------------------------------
// Kahraman (hero) bileşen: bağlan/kes dairesi.
//
// Bu ekranın tek amacı vardır: kullanıcı uygulamayı açtığında BİR saniyeden kısa
// sürede "bağlı mıyım, değil miyim" sorusunun cevabını almalı ve tek dokunuşla
// durumu değiştirebilmeli. Bu yüzden daire ekranın en büyük, en parlak, en
// kolay vurulan hedefidir (yaklaşık 264dp — başparmak için fazlasıyla geniş).
//
// Katmanlar (alttan üste):
//   1. Nefes alan dış ışıma — durumun rengi, çevresel görüşle bile fark edilir.
//   2. Bağlanma dalgası — bağlandığı ANDA dışa doğru genişleyen tek bir halka.
//   3. Sabit iz halkası — dairenin sınırını her durumda belli eder.
//   4. Durum halkası — bağlıyken yavaş dönen tam halka, çalışırken hızlı dönen
//      yay (belirsiz ilerleme), hatada kırmızı tam halka.
//   5. İç yüzey — buzlu cam; bağlıyken üstüne dolu gradyan AKARAK biner.
//   6. İçerik — ikon + eylem sözcüğü + canlı süre.
//
// Durumlar arası geçişte hiçbir katman "kesmez"; hepsi kendi yayıyla akar.
// ---------------------------------------------------------------------------

private val ORB_SIZE = 264.dp
private val RING_INSET = 22.dp

@Composable
fun ConnectOrb(
    state: ConnectionState,
    uptimeText: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    val animated = rememberAnimationsEnabled()
    val interaction = remember { MutableInteractionSource() }

    val busy = state == ConnectionState.Connecting || state == ConnectionState.Testing
    val connected = state == ConnectionState.Connected
    val failed = state == ConnectionState.Failed

    val target = connectionColor(state)
    val accent by animateColorAsState(
        targetValue = target,
        animationSpec = Motion.effectsSlow(),
        label = "orbAccent",
    )

    val transition = rememberInfiniteTransition(label = "orb")
    // Nefes: ışıma yoğunluğu yavaşça artıp azalır. Bağlıyken daha canlı, boştayken
    // neredeyse durgun — uygulama "yaşıyor" ama dikkat çalmıyor.
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animated) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(Motion.BREATH_MS), RepeatMode.Reverse),
        label = "orbBreath",
    )
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animated) 360f else 0f,
        animationSpec = infiniteRepeatable(
            tween(if (busy) Motion.SWEEP_MS else 14_000, easing = LinearEasing),
        ),
        label = "orbSpin",
    )

    // Halka doluluğu duruma göre yay ile değişir: yay 0 → 360 arasında akar,
    // "birden belirdi" hissi olmaz.
    val ringSweep = animateFloatAsState(
        targetValue = when {
            connected || failed -> 360f
            busy -> 110f
            else -> 0f
        },
        animationSpec = Motion.spatialSlow(),
        label = "orbSweep",
    )

    // İç yüzeyin dolması: cam → dolu gradyan geçişi tek bir yayla sürülür.
    val fill = animateFloatAsState(
        targetValue = if (connected) 1f else 0f,
        animationSpec = Motion.spatialSlow(),
        label = "orbFill",
    )

    // Bağlanma dalgası: yalnızca BAĞLANDIĞI anda bir kez dışa açılır. Sürekli
    // tekrarlayan bir "pulse" dikkat çalardı; tek seferlik dalga olayı işaretler.
    val ripple = remember { Animatable(0f) }
    LaunchedEffect(connected) {
        if (connected && animated) {
            ripple.snapTo(0f)
            ripple.animateTo(1f, tween(900, easing = Motion.EmphasizedDecelerate))
        } else {
            ripple.snapTo(0f)
        }
    }

    val contentColor by animateColorAsState(
        targetValue = when {
            connected -> Color.White
            failed -> target
            else -> scheme.onSurface
        },
        animationSpec = Motion.effectsSlow(),
        label = "orbContentColor",
    )

    // Animasyon değerleri YALNIZCA çizim ve graphicsLayer içinde okunur. Böylece
    // her karede yeniden çizim olur, yeniden kompozisyon olmaz — kahraman
    // animasyon bedavaya yakın çalışır.
    val glowStrength = {
        when {
            connected -> 0.30f + breath * 0.30f
            busy -> 0.22f + breath * 0.26f
            failed -> 0.26f
            else -> 0.10f + breath * 0.06f
        }
    }

    val trackColor = scheme.onSurface.copy(alpha = 0.10f)

    Box(
        modifier = modifier
            .size(ORB_SIZE)
            .pressScale(interaction, pressedScale = 0.94f)
            .clickable(interactionSource = interaction, indication = null) {
                haptics.confirm()
                onClick()
            }
            .semantics {
                contentDescription = if (connected) "Bağlantıyı kes" else "Bağlan"
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // 1) Dış ışıma
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = glowStrength()), Color.Transparent),
                    center = center,
                    radius = size.minDimension / 2f,
                ),
                radius = size.minDimension / 2f,
                center = center,
            )

            val strokePx = 9.dp.toPx()
            val inset = RING_INSET.toPx()
            val diameter = size.minDimension - inset * 2f
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            // 2) Bağlanma dalgası
            val r = ripple.value
            if (r > 0f && r < 1f) {
                drawCircle(
                    color = accent.copy(alpha = (1f - r) * 0.45f),
                    radius = diameter / 2f + r * inset * 2.4f,
                    center = center,
                    style = Stroke(width = strokePx * (1f - r) * 0.8f),
                )
            }

            // 3) İz halkası
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )

            // 4) Durum halkası — dönen gradyan yay
            val sweep = ringSweep.value
            if (sweep > 0.5f) {
                rotate(degrees = spin, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.15f),
                                accent,
                                accent.copy(alpha = 0.95f),
                                accent.copy(alpha = 0.15f),
                            ),
                            center = center,
                        ),
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )
                }
            }
        }

        // 5) İç yüzey: cam taban + üstüne akan dolu gradyan.
        val innerSize = ORB_SIZE - RING_INSET * 2 - 26.dp
        Box(Modifier.size(innerSize), contentAlignment = Alignment.Center) {
            GlassSurface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                level = GlassLevel.Hero,
                borderColor = scheme.onSurface.copy(alpha = 0.22f),
            ) {}
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val f = fill.value
                        alpha = f
                        // Dolarken hafifçe "şişer": sıvı dolduruyormuş hissi.
                        val s = 0.92f + 0.08f * f
                        scaleX = s
                        scaleY = s
                    }
                    .background(
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.55f)),
                        ),
                        CircleShape,
                    ),
            )
            OrbContent(
                state = state,
                uptimeText = uptimeText,
                onColor = contentColor,
                spin = { spin },
                busy = busy,
            )
        }
    }
}

@Composable
private fun OrbContent(
    state: ConnectionState,
    uptimeText: String?,
    onColor: Color,
    spin: () -> Float,
    busy: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (scaleIn(initialScale = 0.6f, animationSpec = Motion.bouncy()) + fadeIn(Motion.effectsDefault())) togetherWith
                    (scaleOut(targetScale = 1.35f, animationSpec = Motion.spatialFast()) + fadeOut(Motion.effectsFast()))
            },
            contentAlignment = Alignment.Center,
            label = "orbIcon",
        ) { current ->
            Icon(
                imageVector = connectionIcon(current),
                contentDescription = null,
                tint = onColor,
                modifier = Modifier
                    .size(34.dp)
                    // Çalışırken ikon halkayla aynı yönde döner: iki hareket birbirini
                    // destekler, ekran "meşgul" olduğunu tek bakışta anlatır.
                    .graphicsLayer { rotationZ = if (busy) spin() else 0f },
            )
        }
        Spacer(Modifier.height(10.dp))
        AnimatedContent(
            targetState = connectionLabel(state),
            transitionSpec = {
                (fadeIn(Motion.effectsDefault()) + scaleIn(initialScale = 0.9f)) togetherWith
                    (fadeOut(Motion.effectsFast()) + scaleOut(targetScale = 1.08f))
            },
            contentAlignment = Alignment.Center,
            label = "orbLabel",
        ) { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = onColor,
            )
        }
        if (uptimeText != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = uptimeText,
                style = NumericMedium,
                color = onColor.copy(alpha = 0.85f),
            )
        }
    }
}
