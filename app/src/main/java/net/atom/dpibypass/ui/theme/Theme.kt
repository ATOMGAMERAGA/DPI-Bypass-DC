package net.atom.dpibypass.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import net.atom.dpibypass.data.ThemePref

private val DarkColors = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    secondary = AccentCyan,
    onSecondary = Color(0xFF04121F),
    tertiary = AccentCyan,
    background = BgDarkTop,
    onBackground = OnDark,
    surface = SurfaceDark,
    onSurface = OnDark,
    surfaceVariant = SurfaceDarkVariant,
    onSurfaceVariant = OnDarkMuted,
    error = DangerRed,
    onError = Color.White,
    outline = Color(0x33FFFFFF),
)

private val LightColors = lightColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    secondary = AccentCyan,
    onSecondary = Color(0xFF04121F),
    tertiary = AccentCyan,
    background = BgLightTop,
    onBackground = OnLight,
    surface = SurfaceLight,
    onSurface = OnLight,
    surfaceVariant = SurfaceLightVariant,
    onSurfaceVariant = OnLightMuted,
    error = DangerRed,
    onError = Color.White,
    outline = Color(0x22000000),
)

@Composable
fun DpiBypassTheme(
    themePref: ThemePref = ThemePref.System,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themePref) {
        ThemePref.System -> isSystemInDarkTheme()
        ThemePref.Dark -> true
        ThemePref.Light -> false
    }
    val context = LocalContext.current
    val colorScheme = when {
        // Dynamic Color (Material You) — marka accent'i korumak için opsiyonel.
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DpiTypography,
        content = content,
    )
}
