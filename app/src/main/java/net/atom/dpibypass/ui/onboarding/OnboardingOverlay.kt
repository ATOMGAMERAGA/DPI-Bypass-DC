package net.atom.dpibypass.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Shield
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.atom.dpibypass.ui.components.GlassCard
import net.atom.dpibypass.ui.components.PillButton
import net.atom.dpibypass.ui.components.accentGradientVertical

/**
 * İlk açılış kurulum sihirbazı. Zemindeki aurora sahnesinin üstüne yarı saydam
 * bir örtü + buzlu cam kart olarak biner (GlassCard → gerçek backdrop blur).
 *
 * Adımlar:
 *  1. Discord kuruluysa → "Sadece Discord engeli aşma modu" önerisi.
 *  2. Hızlı Panel'e (WiFi/Bluetooth kutucukları) tek dokunuş aç/kapat kısayolu
 *     ekleme önerisi — kullanıcının OEM skinine (Samsung/Xiaomi/…) göre adlandırılır.
 */
private enum class Step { Discord, Tile }

@Composable
fun OnboardingOverlay(
    discordInstalled: Boolean,
    brandName: String,
    quickPanelName: String,
    onEnableDiscordMode: () -> Unit,
    onRequestTile: () -> Unit,
    onFinish: () -> Unit,
) {
    var step by remember {
        mutableStateOf(if (discordInstalled) Step.Discord else Step.Tile)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Arkadaki içeriğe dokunuşları geçirme (örtü etkin kalsın).
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { }
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (fadeIn(tween(260)) togetherWith fadeOut(tween(160)))
            },
            label = "onboardingStep",
        ) { current ->
            when (current) {
                Step.Discord -> OnboardingCard(
                    icon = Icons.Rounded.Shield,
                    title = "Discord algılandı",
                    body = "Cihazınızda Discord kurulu. Uygulamayı yalnızca Discord'un " +
                        "engelini aşacak şekilde ayarlayalım mı?\n\nBu modda sadece Discord'un " +
                        "trafiği tünelden geçer; diğer uygulamalarınız etkilenmez.",
                    primaryLabel = "Evet, Discord modu",
                    secondaryLabel = "Hayır, tüm uygulamalar",
                    onPrimary = {
                        onEnableDiscordMode()
                        step = Step.Tile
                    },
                    onSecondary = { step = Step.Tile },
                )

                Step.Tile -> OnboardingCard(
                    icon = Icons.Rounded.Bolt,
                    title = "$quickPanelName kısayolu",
                    body = "$brandName cihazınızın $quickPanelName'ine (WiFi ve Bluetooth " +
                        "kutucuklarının bulunduğu yer) tek dokunuşla aç/kapat kısayolu " +
                        "ekleyelim mi? Böylece uygulamayı açmadan bağlanabilirsiniz.",
                    primaryLabel = "Evet, ekle",
                    secondaryLabel = "Şimdi değil",
                    onPrimary = {
                        onRequestTile()
                        onFinish()
                    },
                    onSecondary = onFinish,
                )
            }
        }
    }
}

@Composable
private fun OnboardingCard(
    icon: ImageVector,
    title: String,
    body: String,
    primaryLabel: String,
    secondaryLabel: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
        cornerRadius = 30,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(accentGradientVertical(), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PillButton(
                    text = primaryLabel,
                    onClick = onPrimary,
                    selected = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                PillButton(
                    text = secondaryLabel,
                    onClick = onSecondary,
                    selected = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
