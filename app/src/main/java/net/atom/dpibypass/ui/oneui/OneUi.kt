package net.atom.dpibypass.ui.oneui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.atom.dpibypass.ui.components.GlassCard

// ---------------------------------------------------------------------------
// One UI 8.5 tasarım bileşenleri.
//
// Samsung One UI'ın imza öğeleri buradadır ve tüm ekranlarda paylaşılır:
//   * Büyük, kaydırınca daralan başlık çubuğu (large collapsing title)
//   * Yuvarlak köşeli, gruplanmış liste bölümleri (rounded grouped list)
//   * İnce, içeriden boşluklu ayraçlar (inset dividers)
//   * One UI mavisi hap (pill) anahtarlar
//
// Uygulamanın mevcut "buzlu cam" altyapısı (GlassCard/GlassSurface) One UI 7/8'in
// yarı saydam yüzey diliyle uyumludur; One UI bileşenleri onların üzerine kurulur.
// ---------------------------------------------------------------------------

/**
 * One UI kaydırma davranışlı iskele (scaffold): büyük başlık kaydırınca daralır.
 * İçeriğin kendi kaydırmasını yönettiği ekranlar (ör. LazyColumn'lu Uygulamalar)
 * ve kahraman (hero) düzeni için [content], iç kenar boşluklarıyla verilir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneUiScaffold(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        content = content,
    )
}

/**
 * Dikey kaydırılabilir One UI ekranı: [OneUiScaffold] + otomatik kaydırma sarmalı.
 * Basit form/liste ekranları (Ayarlar, Mod) için idealdir.
 */
@Composable
fun OneUiScreen(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    OneUiScaffold(title = title, modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 18.dp)
                .padding(bottom = 120.dp),
            content = content,
        )
    }
}

/**
 * One UI gruplanmış liste bölümü: renkli başlık + tek yuvarlak kartta toplanan
 * satırlar. Satırlar arasına [OneUiDivider] koyarak One UI ayraç hissi verilir.
 */
@Composable
fun OneUiSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 24.dp, top = 6.dp, bottom = 10.dp),
            )
        }
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 26,
            contentPadding = PaddingValues(0.dp),
        ) {
            Column(Modifier.fillMaxWidth(), content = content)
        }
    }
}

/**
 * One UI liste satırı: başlık (+ alt başlık) solda, sonda opsiyonel bileşen
 * (anahtar, ok, onay vb.). Tıklanabilir yapmak için [onClick] verin.
 */
@Composable
fun OneUiListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val rowModifier = modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = 20.dp, vertical = 16.dp)
    Row(rowModifier, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(14.dp))
            trailing()
        }
    }
}

/** One UI içeriden boşluklu ince ayraç. */
@Composable
fun OneUiDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 20.dp),
        thickness = 0.6.dp,
        color = MaterialTheme.colorScheme.outline,
    )
}

/** One UI mavisi hap (pill) anahtar renkleri. */
@Composable
fun oneUiSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = Color.White,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
)
