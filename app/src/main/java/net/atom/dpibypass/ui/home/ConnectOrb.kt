package net.atom.dpibypass.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
// kolay vurulan hedefidir (yaklaşık 260dp — başparmak için fazlasıyla geniş).
//
// Katmanlar (alttan üste):
//   1. Nefes alan dış ışıma — durumun rengi, çevresel görüşle bile fark edilir.
//   2. Sabit iz halkası — dairenin sınırını her durumda belli eder.
//   3. Durum halkası — bağlıyken yavaş dönen tam halka, çalışırken hızlı dönen
//      yay (belirsiz ilerleme), hatada kırmızı tam halka.
//   4. İç yüzey — bağlıyken dolu gradyan, değilken buzlu cam.
//   5. İçerik — ikon + eylem sözcüğü + canlı süre.
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
    val ringSweep by animateFloatAsState(
        targetValue = when {
            connected || failed -> 360f
            busy -> 110f
            else -> 0f
        },
        animationSpec = Motion.spatialSlow(),
        label = "orbSweep",
    )

    // Animasyon değerleri (breath/spin/sweep) YALNIZCA çizim ve graphicsLayer
    // içinde okunur. Böylece her karede yeniden çizim olur, yeniden kompozisyon
    // olmaz — kahraman animasyon bedavaya yakın çalışır.
    val glowStrength = {
        when {
            connected -> 0.30f + breath * 0.30f
            busy -> 0.22f + breath * 0.26f
            failed -> 0.26f
            else -> 0.10f + breath * 0.06f
        }
    }

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

            // 2) İz halkası
            val strokePx = 9.dp.toPx()
            val inset = RING_INSET.toPx()
            val diameter = size.minDimension - inset * 2f
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = scheme.onSurface.copy(alpha = 0.10f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )

            // 3) Durum halkası — dönen gradyan yay
            if (ringSweep > 0.5f) {
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
                        sweepAngle = ringSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )
                }
            }
        }

        // 4) İç yüzey
        val innerSize = ORB_SIZE - RING_INSET * 2 - 26.dp
        if (connected) {
            Box(
                modifier = Modifier
                    .size(innerSize)
                    .background(
                        Brush.linearGradient(listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.55f))),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                OrbContent(
                    state = state,
                    uptimeText = uptimeText,
                    onColor = Color.White,
                    spin = { spin },
                    busy = busy,
                )
            }
        } else {
            GlassSurface(
                modifier = Modifier.size(innerSize),
                shape = CircleShape,
                blurRadius = 26.dp,
                tint = scheme.surface.copy(alpha = 0.46f),
                borderAlpha = 0.22f,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    OrbContent(
                        state = state,
                        uptimeText = uptimeText,
                        onColor = if (failed) accent else scheme.onSurface,
                        spin = { spin },
                        busy = busy,
                    )
                }
            }
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
        Crossfade(targetState = state, animationSpec = tween(220), label = "orbIcon") { current ->
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
        Crossfade(targetState = connectionLabel(state), animationSpec = tween(220), label = "orbLabel") { label ->
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
