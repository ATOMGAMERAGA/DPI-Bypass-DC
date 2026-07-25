package net.atom.dpibypass.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntOffset

// ---------------------------------------------------------------------------
// Hareket sistemi.
//
// Material 3 Expressive, süre + easing eğrisi yerine YAY FİZİĞİNE (spring) geçti:
// hareket, ekrandaki nesnenin kütlesi varmış gibi hızlanır ve yerine oturur. İki
// aile vardır ve karıştırılmaz:
//
//   * Uzamsal (spatial) — konum, boyut, ölçek, köşe yarıçapı. Hafif taşma
//     (overshoot) yapar; canlılık buradan gelir.
//   * Efekt (effects) — renk, opaklık. ASLA taşmaz (kritik sönümlü), çünkü
//     rengin "hedefi geçmesi" görsel olarak yanlıştır.
//
// Her ailenin üç hızı vardır: küçük öğe → fast, varsayılan → default,
// büyük yüzey → slow.
// ---------------------------------------------------------------------------

object Motion {

    // ---- Uzamsal (konum / boyut / ölçek) ----
    fun <T> spatialFast(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 800f)

    fun <T> spatialDefault(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 380f)

    fun <T> spatialSlow(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.85f, stiffness = 200f)

    /** Kahraman anları (bağlan/kes gibi) için belirgin yaylanma. */
    fun <T> bouncy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.55f, stiffness = 420f)

    // ---- Efekt (renk / opaklık) — taşma yok ----
    fun <T> effectsFast(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 3000f)

    fun <T> effectsDefault(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1400f)

    fun <T> effectsSlow(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 700f)

    /** Kaydırma/yerleşim geçişleri için piksel eşiğine duyarlı uzamsal yay. */
    fun offset(): FiniteAnimationSpec<IntOffset> =
        spring(dampingRatio = 0.8f, stiffness = 380f, visibilityThreshold = IntOffset(1, 1))

    // ---- Sahne süreleri (yay yerine döngüsel/sürekli animasyonlar için) ----
    /** Zemin auroralarının tam turu (ms). Yavaş = dikkat dağıtmaz. */
    const val AURORA_CYCLE_MS = 26_000

    /** Nefes alma (glow) döngüsü (ms). */
    const val BREATH_MS = 2_200

    /** Kahraman halkanın tam dönüşü (ms). */
    const val SWEEP_MS = 2_600

    /** İçerik giriş animasyonunda her öğe arasındaki gecikme (ms). */
    const val STAGGER_MS = 55
}
