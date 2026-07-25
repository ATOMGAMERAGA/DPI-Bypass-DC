package net.atom.dpibypass.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.atom.dpibypass.R
import net.atom.dpibypass.ui.design.AppButton
import net.atom.dpibypass.ui.design.ButtonTone
import net.atom.dpibypass.ui.design.GlassSurface
import net.atom.dpibypass.ui.design.VSpace
import net.atom.dpibypass.ui.theme.LocalStateColors
import net.atom.dpibypass.ui.theme.Motion
import net.atom.dpibypass.ui.theme.PillShape

// ---------------------------------------------------------------------------
// Kurulum sihirbazı.
//
// Eskiden iki ayrı soru kutusu vardı ve kullanıcı nerede olduğunu bilmiyordu.
// Artık gerçek bir akış var: karşılama → (Discord) → hızlı panel → hazır.
// Altta ilerleme noktaları var; aktif nokta bir çizgiye dönüşerek uzar, böylece
// "kaç adım kaldı" sorusu görsel olarak yanıtlanır.
//
// Kart, arkadaki canlı zemini gerçekten bulanıklaştırır (buzlu cam) — uygulama
// daha ilk saniyede kendi malzemesini tanıtır.
// ---------------------------------------------------------------------------

/** Adımın birincil düğmesine basılınca çalışacak eylem. */
private enum class StepAction { None, DiscordMode, RequestTile }

private data class OnboardingStep(
    val icon: ImageVector?,
    val title: String,
    val body: String,
    val primaryLabel: String,
    val secondaryLabel: String?,
    val action: StepAction = StepAction.None,
    val showLogo: Boolean = false,
)

@Composable
fun OnboardingOverlay(
    discordInstalled: Boolean,
    brandName: String,
    quickPanelName: String,
    onEnableDiscordMode: () -> Unit,
    onRequestTile: () -> Unit,
    onFinish: () -> Unit,
) {
    val steps = remember(discordInstalled, brandName, quickPanelName) {
        buildList {
            add(
                OnboardingStep(
                    icon = null,
                    showLogo = true,
                    title = "DPI Bypass'a hoş geldiniz",
                    body = "Bu uygulama, sağlayıcınızın engellediği meşru servislere erişimi " +
                        "cihazınızdan çıkan kendi trafiğinizi yerelde yeniden düzenleyerek açar.\n\n" +
                        "Trafiğiniz uzak bir sunucuya gönderilmez; bu yüzden hız düşmez.",
                    primaryLabel = "Kuruluma başla",
                    secondaryLabel = "Şimdi değil",
                ),
            )
            if (discordInstalled) {
                add(
                    OnboardingStep(
                        icon = Icons.Rounded.Forum,
                        title = "Discord algılandı",
                        body = "Yalnızca Discord'un tünelden geçmesini ister misiniz? Diğer " +
                            "uygulamalarınız hiç etkilenmez ve en iyi strateji otomatik seçilir.",
                        primaryLabel = "Evet, Discord modu",
                        secondaryLabel = "Hayır, tüm uygulamalar",
                        action = StepAction.DiscordMode,
                    ),
                )
            }
            add(
                OnboardingStep(
                    icon = Icons.Rounded.Bolt,
                    title = "$quickPanelName kısayolu",
                    body = "$brandName cihazınızın $quickPanelName'ine (WiFi ve Bluetooth " +
                        "kutucuklarının olduğu yer) tek dokunuşla aç/kapat düğmesi ekleyelim mi? " +
                        "Uygulamayı açmadan bağlanabilirsiniz.",
                    primaryLabel = "Evet, ekle",
                    secondaryLabel = "Şimdi değil",
                    action = StepAction.RequestTile,
                ),
            )
            add(
                OnboardingStep(
                    icon = Icons.Rounded.RocketLaunch,
                    title = "Hazırsınız",
                    body = "Ana ekrandaki büyük düğmeye dokunun. İlk bağlanmada stratejiler " +
                        "test edilir ve en hızlısı seçilir — bu birkaç saniye sürebilir.",
                    primaryLabel = "Başla",
                    secondaryLabel = null,
                ),
            )
        }
    }

    var index by remember { mutableStateOf(0) }
    val step = steps[index.coerceIn(0, steps.lastIndex)]

    fun advance() {
        if (index >= steps.lastIndex) onFinish() else index++
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Arkadaki ekrana dokunuşlar geçmesin.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {}
            .background(Color.Black.copy(alpha = 0.58f))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = index,
            transitionSpec = {
                (fadeIn(tween(260)) + scaleIn(initialScale = 0.94f)) togetherWith
                    (fadeOut(tween(160)) + scaleOut(targetScale = 1.03f))
            },
            label = "onboardingStep",
        ) { current ->
            val shown = steps[current.coerceIn(0, steps.lastIndex)]
            OnboardingCard(
                step = shown,
                stepIndex = current,
                stepCount = steps.size,
                onPrimary = {
                    when (shown.action) {
                        StepAction.DiscordMode -> onEnableDiscordMode()
                        StepAction.RequestTile -> onRequestTile()
                        StepAction.None -> Unit
                    }
                    advance()
                },
                onSecondary = {
                    if (current == 0) onFinish() else advance()
                },
            )
        }
    }
}

@Composable
private fun OnboardingCard(
    step: OnboardingStep,
    stepIndex: Int,
    stepCount: Int,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    val stateColors = LocalStateColors.current
    GlassSurface(
        modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        blurRadius = 40.dp,
        tint = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
        borderAlpha = 0.22f,
    ) {
        Column(
            modifier = Modifier.padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (step.showLogo) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.size(88.dp),
                )
            } else if (step.icon != null) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .background(stateColors.brandGradientVertical(), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        step.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(31.dp),
                    )
                }
            }

            VSpace(18.dp)
            Text(
                text = step.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            VSpace(10.dp)
            Text(
                text = step.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            VSpace(22.dp)

            AppButton(
                text = step.primaryLabel,
                onClick = onPrimary,
                tone = ButtonTone.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
            if (step.secondaryLabel != null) {
                VSpace(10.dp)
                AppButton(
                    text = step.secondaryLabel,
                    onClick = onSecondary,
                    tone = ButtonTone.Ghost,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            VSpace(20.dp)
            StepDots(current = stepIndex, count = stepCount)
        }
    }
}

/** İlerleme noktaları: aktif adım bir çizgiye dönüşerek uzar. */
@Composable
private fun StepDots(current: Int, count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val active = i == current
            val width by animateDpAsState(
                targetValue = if (active) 22.dp else 7.dp,
                animationSpec = Motion.spatialDefault(),
                label = "dotWidth",
            )
            val color by animateColorAsState(
                targetValue = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                },
                animationSpec = Motion.effectsDefault(),
                label = "dotColor",
            )
            Box(
                modifier = Modifier
                    .width(width)
                    .height(7.dp)
                    .background(color, PillShape),
            )
        }
    }
}
