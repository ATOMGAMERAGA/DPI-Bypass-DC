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
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import net.atom.dpibypass.ui.apps.AppsScreen
import net.atom.dpibypass.ui.design.AmbientBackground
import net.atom.dpibypass.ui.design.LocalHapticsEnabled
import net.atom.dpibypass.ui.design.connectionColor
import net.atom.dpibypass.ui.home.HomeScreen
import net.atom.dpibypass.ui.mode.ModeScreen
import net.atom.dpibypass.ui.nav.BottomDock
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
                // Haptik tercihi tüm arayüze buradan akar; her bileşen kendi
                // ayarını okumak zorunda kalmaz.
                CompositionLocalProvider(LocalHapticsEnabled provides settings.haptics) {
                    AppRoot(
                        viewModel = viewModel,
                        onConnect = ::connect,
                        onDisconnect = ::disconnect,
                        onRequestTile = ::requestQuickTile,
                        discordInstalled = isDiscordInstalled(),
                    )
                }
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

}

@Composable
private fun AppRoot(
    viewModel: AppViewModel,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRequestTile: () -> Unit,
    discordInstalled: Boolean,
) {
    val navController = rememberNavController()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

    // Zemin, bağlantı durumunun rengini alır: uygulama bir bütün olarak durum
    // değiştirir, sadece bir düğme değil.
    AmbientBackground(
        accent = connectionColor(connectionState),
        modifier = Modifier.fillMaxSize(),
    ) {
        NavHost(
            navController = navController,
            startDestination = Dest.Home.route,
            // Sekmeler arası geçiş: hafif ölçek + soluklaşma. Yatay kaydırma
            // yerine bu seçildi, çünkü dock sekmelerinin sırası hiyerarşik değil.
            enterTransition = { fadeIn(tween(260)) + scaleIn(initialScale = 0.98f, animationSpec = tween(260)) },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(260)) + scaleIn(initialScale = 0.98f, animationSpec = tween(260)) },
            popExitTransition = { fadeOut(tween(160)) },
        ) {
            composable(Dest.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Dest.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onRequestTile = onRequestTile,
                )
            }
            composable(Dest.Mode.route) { ModeScreen(viewModel) }
            composable(Dest.Apps.route) { AppsScreen(viewModel) }
            composable(Dest.Settings.route) { SettingsScreen(viewModel, onRequestTile = onRequestTile) }
        }

        BottomDock(
            navController = navController,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // Jest çubuğu ya da tuşlu navigasyon — dock her cihazda güvenli
                // alanın üstünde yüzer.
                .navigationBarsPadding()
                .padding(bottom = 14.dp),
        )

        // İlk açılış kurulum sihirbazı — her şeyin üstüne biner.
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
