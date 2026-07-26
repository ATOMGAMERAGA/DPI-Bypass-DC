package net.atom.dpibypass.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

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
//
// YAYLARIN GÖRÜNÜR EŞİĞİ (visibilityThreshold) bilerek belirtilmez; Compose her
// tip için doğru eşiği kendi seçer (Dp için 0.1dp, renk için algısal fark gibi).
// Elle verilen eşikler animasyonun sonunda "takılma" hissi yaratır.
// ---------------------------------------------------------------------------

object Motion {

    // ---- Uzamsal (konum / boyut / ölçek) ----
    /** Küçük öğeler: basma tepkisi, işaret kutusu, rozet. Neredeyse anında oturur. */
    fun <T> spatialFast(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.82f, stiffness = 900f)

    /** Varsayılan: sekme göstergesi, seçim, satır hareketi. */
    fun <T> spatialDefault(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.78f, stiffness = 420f)

    /** Büyük yüzeyler: kart açılması, dock boyu, ilerleme çubuğu. */
    fun <T> spatialSlow(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.86f, stiffness = 210f)

    /** Kahraman anları (bağlan/kes gibi) için belirgin yaylanma. */
    fun <T> bouncy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.52f, stiffness = 440f)

    /**
     * Yer değiştiren seçim göstergesi. Taşma var ama sadece bir tık — dock'ta
     * gösterge hedefi geçip geri gelirken göz onu kolayca takip eder.
     */
    fun <T> indicator(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.68f, stiffness = 520f)

    // ---- Efekt (renk / opaklık) — taşma yok ----
    fun <T> effectsFast(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 3000f)

    fun <T> effectsDefault(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1400f)

    fun <T> effectsSlow(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 700f)

    // ---- Easing eğrileri (yay uygun olmayan yerler: giriş, ekran geçişi) ----
    /** Hızlı çık, yumuşak yerleş — One UI'ın karakteristik giriş eğrisi. */
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Ekrandan çıkan öğe: baştan hızlı, sonu düz. */
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Ekrana giren öğe. */
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    // ---- Süreler ----
    const val SCREEN_ENTER_MS = 320
    const val SCREEN_EXIT_MS = 190

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
