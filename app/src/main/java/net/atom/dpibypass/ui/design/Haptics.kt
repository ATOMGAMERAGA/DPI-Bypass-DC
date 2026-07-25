package net.atom.dpibypass.ui.design

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

/**
 * Dokunsal geri bildirim.
 *
 * Ayarlardaki "Haptik" anahtarı eskiden hiçbir yere bağlı değildi — açık ya da
 * kapalı olması hiçbir şeyi değiştirmiyordu. Artık tek kaynak burası: anahtar
 * [LocalHapticsEnabled] üzerinden akar ve tüm etkileşimler bunu okur.
 *
 * Dokunuş şiddeti eylemin ağırlığına göre seçilir — her yerde aynı titreşim
 * kullanmak "ucuz" hissettirir:
 *   [tick]    → sekme değişimi, kaydırma eşiği (en hafif)
 *   [select]  → seçim, anahtar, çip
 *   [confirm] → bağlan/kes gibi sonucu olan eylemler (en belirgin)
 */
@Immutable
class Haptics(private val view: View?, private val enabled: Boolean) {

    fun tick() = perform(HapticFeedbackConstants.CLOCK_TICK)

    fun select() = perform(HapticFeedbackConstants.VIRTUAL_KEY)

    fun confirm() = perform(HapticFeedbackConstants.LONG_PRESS)

    private fun perform(constant: Int) {
        if (!enabled) return
        view?.performHapticFeedback(constant)
    }
}

/** Ayarlardaki haptik tercihi. */
val LocalHapticsEnabled = staticCompositionLocalOf { true }

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    val enabled = LocalHapticsEnabled.current
    return remember(view, enabled) { Haptics(view, enabled) }
}
