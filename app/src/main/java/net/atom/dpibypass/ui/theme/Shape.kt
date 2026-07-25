package net.atom.dpibypass.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// Şekil ölçeği. One UI'ın "focus block" dilinde köşeler cömerttir: kart ne kadar
// büyükse yarıçap o kadar büyük. Böylece küçük rozet ile büyük kart aynı köşe
// yarıçapını paylaşmaz ve hiyerarşi şekilden de okunur.
// ---------------------------------------------------------------------------

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),   // rozet, küçük etiket
    small = RoundedCornerShape(14.dp),        // iç kutu, ikon kabı
    medium = RoundedCornerShape(20.dp),       // satır, giriş alanı
    large = RoundedCornerShape(28.dp),        // kart / bölüm
    extraLarge = RoundedCornerShape(36.dp),   // kahraman yüzey, diyalog
)

/** Tam yuvarlak (hap) — buton, çip, dock. */
val PillShape = RoundedCornerShape(percent = 50)
