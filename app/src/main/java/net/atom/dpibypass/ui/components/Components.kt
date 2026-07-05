package net.atom.dpibypass.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.atom.dpibypass.ui.theme.AccentBlue
import net.atom.dpibypass.ui.theme.AccentCyan

/** Marka accent gradyanı: cyan → mavi. */
fun accentGradient(): Brush = Brush.horizontalGradient(listOf(AccentCyan, AccentBlue))

fun accentGradientVertical(): Brush = Brush.verticalGradient(listOf(AccentCyan, AccentBlue))

/**
 * "Liquid Glass" yüzey: yarı saydam zemin, ince kenar konturu, büyük köşe yarıçapı.
 * Gerçek backdrop-blur Compose'da maliyetli olduğundan, One UI 8.5 buzlu-cam
 * hissi yarı saydamlık + yumuşak kenarlarla taklit edilir.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 28,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 2.dp,
    ) {
        Box(Modifier.padding(contentPadding)) { content() }
    }
}

/** Yüzen hap (pill) buton — içeriğin üstünde yüzer. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    leading: @Composable (RowScope.() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = shape,
        color = if (selected) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(
            modifier = if (selected) Modifier.background(accentGradient()) else Modifier,
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                leading?.invoke(this)
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
    )
}
