package net.atom.dpibypass.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// One UI 8.5 renk paleti. Samsung One UI'ın imzası: canlı One UI mavisi accent,
// nötr (mavi-siyah) koyu zeminler, yumuşak gri açık zeminler ve yuvarlak, ferah
// yüzeyler. Değerler One UI 7/8'in sistem tonlarına yaklaştırılmıştır.
// ---------------------------------------------------------------------------

// One UI accent mavisi. Geçiş (gradient) altyapısını korumak için açık→koyu mavi
// çifti kullanılır; böylece butonlar/aktif durumlar tek renkli değil, hafif One
// UI parlaklığıyla belirir.
val AccentCyan = Color(0xFF3D9BFF) // gradient üst — açık One UI mavisi
val AccentBlue = Color(0xFF0072F5) // One UI birincil mavi (toggle/accent)
val DangerRed = Color(0xFFF5544A)  // One UI kırmızı (hata/kes)

// Zemin arkasındaki yumuşak ışık lekeleri ("aurora"). One UI'ın mavi ağırlıklı,
// dikkat dağıtmayan derinliği için mavi-mor tonlara çekildi.
val AuroraViolet = Color(0xFF5B6CF0)
val AuroraTeal = Color(0xFF2F9BE0)

// Koyu tema — One UI koyu: nötr, mavi-siyaha yakın zemin + yükseltilmiş kartlar.
val BgDarkTop = Color(0xFF0C0F14)
val BgDarkBottom = Color(0xFF12161D)
val SurfaceDark = Color(0xFF1A1D23)
val SurfaceDarkVariant = Color(0xFF23272F)
val OnDark = Color(0xFFF2F4F7)
val OnDarkMuted = Color(0xFF9BA3AF)

// Açık tema — One UI açık: yumuşak gri zemin + beyaz kartlar.
val BgLightTop = Color(0xFFF5F6F8)
val BgLightBottom = Color(0xFFECEEF2)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceLightVariant = Color(0xFFF0F2F5)
val OnLight = Color(0xFF16181C)
val OnLightMuted = Color(0xFF5B6470)
