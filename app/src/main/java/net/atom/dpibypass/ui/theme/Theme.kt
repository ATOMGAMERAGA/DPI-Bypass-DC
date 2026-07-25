package net.atom.dpibypass.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import net.atom.dpibypass.data.ThemePref

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = Color(0xFF00243F),
    primaryContainer = Color(0xFF10304C),
    onPrimaryContainer = Color(0xFFCDE6FF),
    secondary = BrandCyan,
    onSecondary = Color(0xFF00272E),
    secondaryContainer = Color(0xFF0C3540),
    onSecondaryContainer = Color(0xFFB7F0FB),
    tertiary = BrandIndigo,
    onTertiary = Color.White,
    background = BgDarkTop,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkSurfaceLowest,
    surfaceContainerLow = DarkSurfaceLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkSurfaceHighest,
    inverseSurface = Color(0xFFE7EDF6),
    inverseOnSurface = Color(0xFF11161E),
    error = StateErrorDark,
    onError = Color(0xFF3A0710),
    errorContainer = Color(0xFF4A1220),
    onErrorContainer = Color(0xFFFFD9DE),
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    scrim = Color.Black,
)

private val LightColors = lightColorScheme(
    primary = AccentLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E9FF),
    onPrimaryContainer = Color(0xFF00284A),
    secondary = Color(0xFF0C7C93),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8F1FA),
    onSecondaryContainer = Color(0xFF00323C),
    tertiary = Color(0xFF4F46E5),
    onTertiary = Color.White,
    background = BgLightTop,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = LightSurfaceLowest,
    surfaceContainerLow = LightSurfaceLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceHigh,
    surfaceContainerHighest = LightSurfaceHighest,
    inverseSurface = Color(0xFF161C25),
    inverseOnSurface = Color(0xFFF2F5FA),
    error = StateErrorLight,
    onError = Color.White,
    errorContainer = Color(0xFFFFDADD),
    onErrorContainer = Color(0xFF40000A),
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    scrim = Color.Black,
)

/**
 * Bağlantı durumunun anlamsal renkleri. Material şemasında karşılığı olmayan ama
 * uygulamanın her yerinde AYNI anlamı taşıması gereken renkler burada tek
 * kaynaktan gelir: kullanıcı rengi görünce metni okumadan durumu anlar.
 */
@Immutable
data class StateColors(
    val connected: Color,
    val busy: Color,
    val error: Color,
    val idle: Color,
    val brandStart: Color,
    val brandEnd: Color,
) {
    /** Marka gradyanı (logo: cyan → mavi). */
    fun brandGradient(): Brush = Brush.linearGradient(listOf(brandStart, brandEnd))

    fun brandGradientVertical(): Brush = Brush.verticalGradient(listOf(brandStart, brandEnd))
}

val LocalStateColors = staticCompositionLocalOf {
    StateColors(
        connected = StateConnectedDark,
        busy = StateBusyDark,
        error = StateErrorDark,
        idle = StateIdleDark,
        brandStart = BrandCyan,
        brandEnd = BrandBlue,
    )
}

/** Koyu tema mı? Zemin/cam yoğunluğu buna göre ayarlanır. */
val LocalIsDarkTheme = staticCompositionLocalOf { true }

@Composable
fun DpiBypassTheme(
    themePref: ThemePref = ThemePref.System,
    content: @Composable () -> Unit,
) {
    val dark = when (themePref) {
        ThemePref.System -> isSystemInDarkTheme()
        ThemePref.Dark -> true
        ThemePref.Light -> false
    }
    // Marka rengi kimliğin parçası olduğu için dinamik renk (Material You) bilerek
    // kullanılmaz: durum renkleri (bağlı/hata) duvar kâğıdına göre değişemez.
    val colorScheme = if (dark) DarkColors else LightColors
    val stateColors = if (dark) {
        StateColors(
            connected = StateConnectedDark,
            busy = StateBusyDark,
            error = StateErrorDark,
            idle = StateIdleDark,
            brandStart = BrandCyan,
            brandEnd = BrandBlue,
        )
    } else {
        StateColors(
            connected = StateConnectedLight,
            busy = StateBusyLight,
            error = StateErrorLight,
            idle = StateIdleLight,
            brandStart = Color(0xFF0EA5C6),
            brandEnd = Color(0xFF2563EB),
        )
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

    CompositionLocalProvider(
        LocalStateColors provides stateColors,
        LocalIsDarkTheme provides dark,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DpiTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
