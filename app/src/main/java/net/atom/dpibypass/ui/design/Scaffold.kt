package net.atom.dpibypass.ui.design

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// Ekran iskeleti.
//
// Düzen kararı: içerik, üstteki başlık çubuğunun ALTINDAN akar. Kaydırdıkça
// çubuk buzlu cama dönüşür ve küçük başlık yumuşakça belirir. Böylece ekranın
// üstünde ölü bir bant kalmaz (One UI 8.5'in "ekran alanını geri kazan" fikri)
// ve kullanıcı nerede olduğunu her an bilir.
//
// Büyük başlık kaydırma alanının İÇİNDEDİR: doğal olarak yukarı kayar, yerini
// çubuktaki küçük başlığa bırakır. Bu, hem daha akıcı hem de her kaydırma
// kabıyla (Column / LazyColumn) çalışan sade bir çözümdür.
// ---------------------------------------------------------------------------

/** Yüzen dock'un altta kapladığı güvenli alan. */
val DockSpacing: Dp = 118.dp

/** İçerik kolonunun büyük ekranlarda genişlemesini sınırlar (okunabilirlik). */
val ContentMaxWidth: Dp = 640.dp

val ScreenPadding: Dp = 18.dp

@Composable
fun rememberCollapseFraction(scroll: ScrollState, distance: Dp = 64.dp): Float {
    val px = with(LocalDensity.current) { distance.toPx() }
    val fraction by remember(scroll, px) {
        derivedStateOf { (scroll.value / px).coerceIn(0f, 1f) }
    }
    return fraction
}

@Composable
fun rememberCollapseFraction(state: LazyListState, distance: Dp = 64.dp): Float {
    val px = with(LocalDensity.current) { distance.toPx() }
    val fraction by remember(state, px) {
        derivedStateOf {
            if (state.firstVisibleItemIndex > 0) 1f
            else (state.firstVisibleItemScrollOffset / px).coerceIn(0f, 1f)
        }
    }
    return fraction
}

/**
 * Üstte yüzen buzlu cam başlık şeridi + serbest içerik.
 * [collapseFraction] 0 → şerit görünmez, 1 → şerit tamamen belirgin.
 */
@Composable
fun AppScaffold(
    title: String,
    collapseFraction: Float,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val barHeight = 52.dp

    Box(modifier.fillMaxSize()) {
        content(PaddingValues(top = statusBar + 6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusBar + barHeight),
        ) {
            // Cam şerit: yalnızca kaydırıldıkça belirir. Üstteyken ekran tamamen
            // içeriğin ve zeminin olur.
            if (collapseFraction > 0.01f) {
                GlassSurface(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = collapseFraction },
                    shape = RoundedCornerShape(0.dp),
                    blurRadius = 30.dp,
                    tint = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    borderAlpha = 0.10f,
                ) {}
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBar)
                    .height(barHeight)
                    .padding(horizontal = ScreenPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            alpha = collapseFraction
                            translationY = (1f - collapseFraction) * 12.dp.toPx()
                        },
                )
                actions()
            }
        }
    }
}

/**
 * Dikey kaydırmalı standart ekran: büyük başlık + içerik. Ekranların çoğu bunu
 * kullanır; böylece kenar boşlukları, maksimum genişlik ve dock boşluğu her
 * yerde birebir aynıdır.
 */
@Composable
fun AppScreen(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    val collapse = rememberCollapseFraction(scroll)

    AppScaffold(title = title, collapseFraction = collapse, modifier = modifier, actions = actions) { padding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = ContentMaxWidth)
                    .verticalScroll(scroll)
                    .padding(padding)
                    .padding(horizontal = ScreenPadding)
                    .padding(bottom = DockSpacing),
            ) {
                PageHeader(title = title, subtitle = subtitle, collapseFraction = collapse)
                content()
            }
        }
    }
}

/**
 * Büyük başlık. Kaydırma ile birlikte solar ki üstteki küçük başlıkla aynı anda
 * iki kez okunmasın — geçiş "devir teslim" gibi hissedilir.
 */
@Composable
fun PageHeader(
    title: String,
    subtitle: String?,
    collapseFraction: Float = 0f,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 46.dp, bottom = 12.dp)
            .graphicsLayer { alpha = (1f - collapseFraction * 1.4f).coerceIn(0f, 1f) },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Dikey boşluk kısayolu — ekranlarda tekrar eden Spacer'ları sadeleştirir. */
@Composable
fun VSpace(height: Dp) {
    Spacer(Modifier.height(height))
}
