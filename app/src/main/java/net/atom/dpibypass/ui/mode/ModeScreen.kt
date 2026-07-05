package net.atom.dpibypass.ui.mode

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.atom.dpibypass.data.OperationMode
import net.atom.dpibypass.isp.Isp
import net.atom.dpibypass.strategy.StrategyPool
import net.atom.dpibypass.strategy.StrategyTestResult
import net.atom.dpibypass.ui.AppViewModel
import net.atom.dpibypass.ui.components.GlassCard
import net.atom.dpibypass.ui.components.PillButton
import net.atom.dpibypass.ui.components.SectionTitle
import net.atom.dpibypass.ui.components.accentGradient

@Composable
fun ModeScreen(viewModel: AppViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val results by viewModel.testResults.collectAsStateWithLifecycle()
    val testing by viewModel.testing.collectAsStateWithLifecycle()
    val winner by viewModel.testWinner.collectAsStateWithLifecycle()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = topInset + 12.dp, bottom = 120.dp),
    ) {
        Text(
            text = "Mod",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(16.dp))

        // Otomatik / Manuel seçim
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(
                text = "Otomatik",
                selected = settings.operationMode == OperationMode.Auto,
                onClick = { viewModel.setOperationMode(OperationMode.Auto) },
                modifier = Modifier.weight(1f),
            )
            PillButton(
                text = "Manuel",
                selected = settings.operationMode == OperationMode.Manual,
                onClick = { viewModel.setOperationMode(OperationMode.Manual) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(16.dp))

        if (settings.operationMode == OperationMode.Manual) {
            SectionTitle("İnternet Servis Sağlayıcı (ISS)")
            IspDropdown(selected = settings.selectedIsp, onSelect = viewModel::setSelectedIsp)
            Spacer(Modifier.height(16.dp))

            SectionTitle("Strateji (preset)")
            StrategyPool.all.forEach { strategy ->
                PresetRow(
                    id = strategy.id,
                    name = strategy.name,
                    note = strategy.note,
                    selected = settings.selectedStrategyId == strategy.id && !settings.advancedEnabled,
                    onClick = { viewModel.setSelectedStrategy(strategy.id) },
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(8.dp))
            AdvancedArgsCard(
                enabled = settings.advancedEnabled,
                args = settings.advancedArgs,
                onEnabledChange = viewModel::setAdvancedEnabled,
                onArgsChange = viewModel::setAdvancedArgs,
            )
        } else {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Otomatik mod: uygulama, ISS'nizi tespit edip her stratejiyi tek tek " +
                        "test eder ve en düşük ping'li çalışanı seçer. Bağlanınca otomatik yapılır; " +
                        "aşağıdan şimdi de test edebilirsiniz.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("Canlı strateji testi")
        Spacer(Modifier.height(8.dp))
        LiveTestSection(
            testing = testing,
            results = results,
            winnerLabel = winner?.let { "Seçilen: ${it.strategy.id} — ${it.latencyMs ?: "?"} ms" },
            onStart = viewModel::runStrategyTest,
        )
    }
}

@Composable
private fun IspDropdown(selected: Isp, onSelect: (Isp) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = selected.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Rounded.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Isp.entries.forEach { isp ->
                DropdownMenuItem(
                    text = { Text(isp.displayName) },
                    onClick = {
                        onSelect(isp)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PresetRow(
    id: String,
    name: String,
    note: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .then(
                        if (selected) Modifier.background(accentGradient(), CircleShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    id,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) {
                Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun AdvancedArgsCard(
    enabled: Boolean,
    args: String,
    onEnabledChange: (Boolean) -> Unit,
    onArgsChange: (String) -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Gelişmiş (serbest ByeDPI argümanı)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("Açıkken preset yerine bu argümanlar kullanılır.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            if (enabled) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = args,
                    onValueChange = onArgsChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("--split 2 --disorder 3 --fake -1 --ttl 5") },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    keyboardOptions = KeyboardOptions.Default,
                    singleLine = false,
                )
            }
        }
    }
}

@Composable
private fun LiveTestSection(
    testing: Boolean,
    results: List<StrategyTestResult>,
    winnerLabel: String?,
    onStart: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            PillButton(
                text = if (testing) "Test ediliyor…" else "Stratejileri Test Et",
                selected = !testing,
                onClick = { if (!testing) onStart() },
                modifier = Modifier.fillMaxWidth(),
            )
            if (results.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                results.forEach { r -> TestResultRow(r) }
            }
            winnerLabel?.let {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(accentGradient(), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                ) {
                    Text(it, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TestResultRow(r: StrategyTestResult) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            r.strategy.id,
            modifier = Modifier.size(width = 40.dp, height = 20.dp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            r.strategy.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (r.state) {
            StrategyTestResult.State.Testing ->
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            StrategyTestResult.State.Success -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${r.latencyMs ?: "?"} ms",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.size(6.dp))
                Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
            }
            StrategyTestResult.State.Failed ->
                Icon(Icons.Rounded.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            StrategyTestResult.State.Pending ->
                Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
