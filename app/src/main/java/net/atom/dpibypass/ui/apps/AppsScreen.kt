package net.atom.dpibypass.ui.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.atom.dpibypass.data.AppFilterMode
import net.atom.dpibypass.ui.AppEntry
import net.atom.dpibypass.ui.AppViewModel
import net.atom.dpibypass.ui.components.GlassCard
import net.atom.dpibypass.ui.components.PillButton
import net.atom.dpibypass.ui.components.SectionTitle
import net.atom.dpibypass.ui.oneui.OneUiScaffold

private val FAVORITES = setOf(
    "com.discord", "com.android.chrome", "org.mozilla.firefox",
    "com.roblox.client", "com.brave.browser",
)

@Composable
fun AppsScreen(viewModel: AppViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadInstalledApps() }

    val listEnabled = settings.appFilterMode != AppFilterMode.All
    val filtered = remember(apps, query) {
        val q = query.trim().lowercase()
        val base = if (q.isBlank()) apps else apps.filter {
            it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
        base.sortedByDescending { it.packageName in FAVORITES }
    }

    OneUiScaffold(title = "Uygulamalar") { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .padding(innerPadding)
                .padding(horizontal = 18.dp),
        ) {
            Text(
                "Hangi uygulamaların tünelden geçeceğini seçin (split tunneling).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Tümü", { viewModel.setAppFilterMode(AppFilterMode.All) },
                    selected = settings.appFilterMode == AppFilterMode.All, modifier = Modifier.weight(1f))
                PillButton("Seçili", { viewModel.setAppFilterMode(AppFilterMode.Include) },
                    selected = settings.appFilterMode == AppFilterMode.Include, modifier = Modifier.weight(1f))
                PillButton("Hariç", { viewModel.setAppFilterMode(AppFilterMode.Exclude) },
                    selected = settings.appFilterMode == AppFilterMode.Exclude, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))
            if (listEnabled) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Uygulama ara") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                SectionTitle(
                    when (settings.appFilterMode) {
                        AppFilterMode.Include -> "Yalnızca işaretli uygulamalar tünelde"
                        AppFilterMode.Exclude -> "İşaretli uygulamalar tünel DIŞINDA"
                        AppFilterMode.All -> ""
                    }
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            checked = app.packageName in settings.selectedApps,
                            onCheckedChange = { viewModel.toggleApp(app.packageName, it) },
                        )
                    }
                }
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Tüm uygulamalar tünelden geçiyor. Belirli uygulamaları seçmek için " +
                            "\"Seçili\" veya \"Hariç\" moduna geçin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: AppEntry, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(app.packageName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
