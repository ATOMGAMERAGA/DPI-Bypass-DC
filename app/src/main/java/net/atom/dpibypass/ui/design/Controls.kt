package net.atom.dpibypass.ui.design

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import net.atom.dpibypass.ui.theme.LocalStateColors
import net.atom.dpibypass.ui.theme.Motion
import net.atom.dpibypass.ui.theme.PillShape
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Etkileşim öğeleri.
//
// Ortak kural: her dokunuş ÜÇ kanaldan birden yanıt verir — görsel (basılınca
// küçülme), renk (durum geçişi) ve dokunsal (haptik). Bir uygulamanın "pahalı"
// hissettirmesinin en büyük sebebi budur; tek başına renk paleti yetmez.
// ---------------------------------------------------------------------------

/** Basılıyken hafif küçülme — dokunuşun fiziksel karşılığı. */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.965f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = Motion.spatialFast(),
        label = "pressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

enum class ButtonTone { Primary, Tonal, Ghost, Danger }

/**
 * Uygulamanın tek buton bileşeni. Görsel ağırlık [tone] ile seçilir; böylece bir
 * ekranda birden fazla "birincil" buton olmaz ve göz nereye basacağını bilir.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ButtonTone = ButtonTone.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val stateColors = LocalStateColors.current
    val haptics = rememberHaptics()
    val interaction = remember { MutableInteractionSource() }

    val background: Brush = when (tone) {
        ButtonTone.Primary -> stateColors.brandGradient()
        ButtonTone.Tonal -> SolidColor(scheme.surfaceContainerHighest)
        ButtonTone.Ghost -> SolidColor(Color.Transparent)
        ButtonTone.Danger -> SolidColor(scheme.error.copy(alpha = 0.18f))
    }
    val contentColor = when (tone) {
        ButtonTone.Primary -> Color.White
        ButtonTone.Tonal -> scheme.onSurface
        ButtonTone.Ghost -> scheme.onSurfaceVariant
        ButtonTone.Danger -> scheme.error
    }

    Box(
        modifier = modifier
            .pressScale(interaction)
            .heightIn(min = 52.dp)
            .clip(PillShape)
            .background(background, PillShape)
            .then(
                if (tone == ButtonTone.Ghost) {
                    Modifier.border(1.dp, scheme.outline, PillShape)
                } else {
                    Modifier
                },
            )
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
            ) {
                haptics.select()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(19.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Segment seçeneği. */
data class Segment<T>(val value: T, val label: String, val icon: ImageVector? = null)

/**
 * Segment denetimi (Otomatik/Manuel, Tümü/Seçili/Hariç…).
 *
 * Seçim göstergesi anında yer değiştirmez; yay fiziğiyle KAYAR. Kullanıcı gözüyle
 * hareketi takip ettiği için "hangi sekmedeydim" sorusu hiç oluşmaz.
 */
@Composable
fun <T> SegmentedControl(
    options: List<Segment<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val stateColors = LocalStateColors.current
    val haptics = rememberHaptics()
    val density = LocalDensity.current

    var widthPx by remember { mutableStateOf(0) }
    val index = options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
    val itemWidthPx = if (options.isEmpty()) 0f else widthPx.toFloat() / options.size

    val indicatorX by animateFloatAsState(
        targetValue = itemWidthPx * index,
        animationSpec = Motion.spatialDefault(),
        label = "segmentIndicator",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .onSizeChanged { widthPx = it.width }
            .clip(PillShape)
            .background(scheme.surfaceContainer.copy(alpha = 0.9f))
            .border(1.dp, scheme.outline, PillShape),
    ) {
        if (widthPx > 0) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(indicatorX.roundToInt(), 0) }
                    .width(with(density) { itemWidthPx.toDp() })
                    .fillMaxHeight()
                    .padding(4.dp)
                    .clip(PillShape)
                    .background(stateColors.brandGradient(), PillShape),
            )
        }
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEach { option ->
                val isSelected = option.value == selected
                val color by animateColorAsState(
                    targetValue = if (isSelected) Color.White else scheme.onSurfaceVariant,
                    animationSpec = Motion.effectsDefault(),
                    label = "segmentLabel",
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(PillShape)
                        .clickable {
                            if (!isSelected) {
                                haptics.select()
                                onSelect(option.value)
                            }
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (option.icon != null) {
                        Icon(option.icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                    }
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Durum rozeti: küçük renkli etiket (BAĞLI, OTO, P2 …). */
@Composable
fun TagBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    filled: Boolean = false,
) {
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(if (filled) color else color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (filled) Color.White else color,
            maxLines = 1,
        )
    }
}

/** Seçim işareti — seçilince yay ile belirir, seçim kaldırılınca söner. */
@Composable
fun SelectionCheck(selected: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(if (selected) scheme.primary else Color.Transparent)
            .border(if (selected) 0.dp else 1.5.dp, scheme.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = selected,
            enter = scaleIn(animationSpec = Motion.bouncy()) + fadeIn(animationSpec = Motion.effectsFast()),
            exit = scaleOut(animationSpec = Motion.spatialFast()) + fadeOut(animationSpec = Motion.effectsFast()),
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = scheme.onPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * Metin alanı. Material'ın hazır alanı yerine elle çizilir: odaklanınca kenarlık
 * aksan rengine yay ile geçer, yükseklik ve köşe yarıçapı kart diliyle aynıdır.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    monospace: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        targetValue = if (focused) scheme.primary else scheme.outline,
        animationSpec = Motion.effectsDefault(),
        label = "fieldBorder",
    )

    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = scheme.onSurface,
        fontFamily = if (monospace) FontFamily.Monospace else MaterialTheme.typography.bodyLarge.fontFamily,
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = textStyle,
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 6,
        cursorBrush = SolidColor(scheme.primary),
        keyboardOptions = keyboardOptions,
        interactionSource = interaction,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(scheme.surfaceContainerHigh.copy(alpha = 0.8f))
                    .border(1.4.dp, borderColor, MaterialTheme.shapes.medium)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingIcon != null) {
                    Icon(
                        leadingIcon,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(Modifier.width(11.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = textStyle,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    trailing()
                }
            }
        },
    )
}

/**
 * Seçim diyaloğu (ISS, DoH sağlayıcı…). Sistem menüsü yerine uygulamanın kendi
 * kart dilinde çizilir; uzun listelerde kaydırılır ve seçili satır işaretlenir.
 */
@Composable
fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(scheme.surfaceContainerHigh)
                .border(1.dp, scheme.outline, MaterialTheme.shapes.extraLarge)
                .padding(vertical = 20.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                options.forEach { (value, label) ->
                    val isSelected = value == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(value)
                                onDismiss()
                            }
                            .padding(horizontal = 22.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) scheme.primary else scheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = scheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
