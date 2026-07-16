package net.atom.dpibypass.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.atom.dpibypass.data.ThemePref
import net.atom.dpibypass.ui.apps.AppsScreen
import net.atom.dpibypass.ui.home.HomeScreen
import net.atom.dpibypass.ui.mode.ModeScreen
import net.atom.dpibypass.ui.components.AuroraBackground
import net.atom.dpibypass.ui.nav.BottomPillBar
import net.atom.dpibypass.ui.nav.Dest
import net.atom.dpibypass.ui.onboarding.OnboardingOverlay
import net.atom.dpibypass.ui.settings.SettingsScreen
import net.atom.dpibypass.ui.theme.DpiBypassTheme
import net.atom.dpibypass.util.DeviceInfo
import net.atom.dpibypass.util.QuickTile
import net.atom.dpibypass.vpn.ServiceController

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    // VPN izni sonucu → onaylanırsa servisi başlat.
    private val vpnPermissionLauncher =
        registerForActivityResultInternal()

    private fun registerForActivityResultInternal() =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                ServiceController.start(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotificationPermission()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            DpiBypassTheme(themePref = settings.theme) {
                AppRoot(
                    viewModel = viewModel,
                    dark = isDark(settings.theme),
                    onConnect = ::connect,
                    onDisconnect = ::disconnect,
                    onRequestTile = ::requestQuickTile,
                    discordInstalled = isDiscordInstalled(),
                )
            }
        }
    }

    /** Bağlan: VPN izni gerekiyorsa iste, yoksa doğrudan başlat. */
    private fun connect() {
        val prepare = VpnService.prepare(this)
        if (prepare != null) {
            vpnPermissionLauncher.launch(prepare)
        } else {
            ServiceController.start(this)
        }
    }

    private fun disconnect() {
        ServiceController.stop(this)
    }

    /** Hızlı Panel kısayolunu (Android 13+ sistem onayıyla) ekle. */
    private fun requestQuickTile() {
        QuickTile.request(this)
    }

    /** Discord kurulu mu? Kurulum sihirbazının Discord adımını tetikler. */
    private fun isDiscordInstalled(): Boolean = try {
        packageManager.getPackageInfo(AppViewModel.DISCORD_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                    .launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    @Composable
    private fun isDark(pref: ThemePref): Boolean = when (pref) {
        ThemePref.Dark -> true
        ThemePref.Light -> false
        ThemePref.System -> androidx.compose.foundation.isSystemInDarkTheme()
    }
}

@Composable
private fun AppRoot(
    viewModel: AppViewModel,
    dark: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRequestTile: () -> Unit,
    discordInstalled: Boolean,
) {
    val navController = rememberNavController()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    AuroraBackground(dark = dark, modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Dest.Home.route) {
            composable(Dest.Home.route) {
                HomeScreen(viewModel, onConnect = onConnect, onDisconnect = onDisconnect)
            }
            composable(Dest.Mode.route) { ModeScreen(viewModel) }
            composable(Dest.Apps.route) { AppsScreen(viewModel) }
            composable(Dest.Settings.route) { SettingsScreen(viewModel, onRequestTile = onRequestTile) }
        }
        BottomPillBar(
            navController = navController,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
        )

        // İlk açılış kurulum sihirbazı — zeminin üstüne biner.
        if (!settings.onboardingDone) {
            OnboardingOverlay(
                discordInstalled = discordInstalled,
                brandName = DeviceInfo.brandName(),
                quickPanelName = DeviceInfo.quickPanelName(),
                onEnableDiscordMode = viewModel::enableDiscordOnlyMode,
                onRequestTile = onRequestTile,
                onFinish = { viewModel.setOnboardingDone(true) },
            )
        }
    }
}
