package net.atom.dpibypass.ui.design

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.atom.dpibypass.ui.theme.Motion
import net.atom.dpibypass.ui.theme.NumericMedium
import java.util.Locale

// ---------------------------------------------------------------------------
// Yüzeyler — One UI'ın "focus block" dili, buzlu cam malzemesiyle.
//
// İçerik, zeminden net biçimde ayrılan büyük yuvarlak bloklar hâlinde gruplanır.
// Blok içindeki satırlar ince, içeriden boşluklu ayraçlarla ayrılır.
//
// Malzeme kararı: kartlar artık düz yarı saydam renk DEĞİL, gerçek buzlu camdır
// (bkz. Glass.kt). Arkalarındaki aurora bulanıklaşarak geçer; metin kontrastı
// tint yoğunluğuyla garanti altına alınır.
// ---------------------------------------------------------------------------

// Başlıklar Türkçe yazıldığı için büyük harfe çevirme Türkçe kurallarıyla
// yapılır: varsayılan (ROOT) çeviri "erişim" → "ERISIM" gibi noktasız I üretir.
private val TurkishLocale: Locale = Locale.forLanguageTag("tr-TR")

internal fun String.upperTr(): String = uppercase(TurkishLocale)

/** Kart yoğunluğu — hiyerarşide hangi basamakta durduğunu belirler. */
enum class CardTone { Plain, Raised, Accent, Danger }

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    tone: CardTone = CardTone.Plain,
    shape: Shape = MaterialTheme.shapes.large,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    // Cam rengi: nötr yüzeyden aksana doğru karıştırılır. Ton değişimi (ör. bir
    // preset seçilince) renk yayıyla akar — kart "birden" renk değiştirmez.
    val targetTint = when (tone) {
        CardTone.Plain -> scheme.surface
        CardTone.Raised -> scheme.surfaceContainerHigh
        CardTone.Accent -> lerp(scheme.surface, scheme.primary, 0.30f)
        CardTone.Danger -> lerp(scheme.surface, scheme.error, 0.26f)
    }
    val targetBorder = when (tone) {
        CardTone.Accent -> scheme.primary.copy(alpha = 0.52f)
        CardTone.Danger -> scheme.error.copy(alpha = 0.46f)
        else -> scheme.onSurface.copy(alpha = 0.14f)
    }
    val tint by animateColorAsState(targetTint, Motion.effectsDefault(), label = "cardTint")
    val border by animateColorAsState(targetBorder, Motion.effectsDefault(), label = "cardBorder")

    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressScale(interaction) else Modifier)
            .glass(
                shape = shape,
                level = GlassLevel.Card,
                tintColor = tint,
                borderColor = border,
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
        content = content,
    )
}

/**
 * Bölüm başlığı: küçük, kalın, aksan renkli — ekranı hızlı taranır kılar.
 * Başlığın solundaki kısa çizgi, bölümün başladığı yeri gözle yakalatır.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 6.dp, top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 3.dp, height = 12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title.upperTr(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/**
 * Yuvarlak, tonlanmış ikon kabı. İkon değiştiğinde eskisi yukarı süzülüp çıkar,
 * yenisi aşağıdan gelir — durum değişimi fark edilir olur.
 */
@Composable
fun IconBubble(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 40.dp,
) {
    val bubbleTint by animateColorAsState(tint, Motion.effectsDefault(), label = "bubbleTint")
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 34))
            .background(bubbleTint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = {
                (slideInVertically { it / 2 } + fadeIn(Motion.effectsDefault())) togetherWith
                    (slideOutVertically { -it / 2 } + fadeOut(Motion.effectsFast()))
            },
            contentAlignment = Alignment.Center,
            label = "bubbleIcon",
        ) { current ->
            Icon(
                current,
                contentDescription = null,
                tint = bubbleTint,
                modifier = Modifier.size(size * 0.5f),
            )
        }
    }
}

/**
 * Liste satırı. Başlık + (opsiyonel) alt başlık, solda ikon, sağda serbest içerik.
 *
 * Dokunulabilir olduğunda iki kanaldan yanıt verir: satır hafifçe küçülür ve
 * altına yumuşak bir vurgu yayılır. Tek kanal (yalnızca ölçek) ucuz hissettirir.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    maxSubtitleLines: Int = 4,
    trailing: @Composable (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val highlight by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 1f else 0f,
        animationSpec = Motion.effectsDefault(),
        label = "rowPress",
    )
    val highlightColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .pressScale(interaction, pressedScale = 0.985f)
                        .background(highlightColor.copy(alpha = 0.06f * highlight))
                        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            IconBubble(icon, tint = iconTint)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = maxSubtitleLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
) {
    val haptics = rememberHaptics()
    // Açıkken ikon aksan rengine geçer: anahtarı görmeden de durum okunur.
    val tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    ListRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        iconTint = tint,
        modifier = modifier,
        onClick = {
            haptics.select()
            onCheckedChange(!checked)
        },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = {
                    haptics.select()
                    onCheckedChange(it)
                },
                colors = appSwitchColors(),
            )
        },
    )
}

@Composable
fun appSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = Color.White,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
)

/** İçeriden boşluklu ince ayraç — satırları ayırır, bloğu bölmez. */
@Composable
fun RowDivider(modifier: Modifier = Modifier, startInset: Dp = 18.dp) {
    HorizontalDivider(
        modifier = modifier.padding(start = startInset, end = 18.dp),
        thickness = 0.7.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
    )
}

/**
 * Ölçüm kutucuğu: küçük etiket + büyük değer. Değerler tabular rakamla yazılır,
 * böylece canlı güncellenirken yazı zıplamaz. Değer DEĞİŞTİĞİNDE eskisi yukarı
 * kayıp yenisi aşağıdan gelir — canlı bir sayaç gibi okunur.
 */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    animateValue: Boolean = true,
) {
    val tileColor by animateColorAsState(valueColor, Motion.effectsDefault(), label = "tileValueColor")
    Column(
        modifier = modifier
            .glass(
                shape = MaterialTheme.shapes.medium,
                level = GlassLevel.Inset,
                tintColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                borderWidth = 0.dp,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label.upperTr(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (animateValue) {
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn(Motion.effectsDefault())) togetherWith
                        (slideOutVertically { -it } + fadeOut(Motion.effectsFast()))
                },
                label = "tileValue",
            ) { current ->
                Text(
                    text = current,
                    style = NumericMedium,
                    color = tileColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Text(
                text = value,
                style = NumericMedium,
                color = tileColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
