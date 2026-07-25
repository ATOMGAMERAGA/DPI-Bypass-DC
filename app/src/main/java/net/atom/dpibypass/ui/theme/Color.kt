package net.atom.dpibypass.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// "Aurora Ambient" renk sistemi.
//
// İki referanstan beslenir:
//   * Material 3 Expressive — anlamlı renk rolleri, yüzey basamakları (surface
//     container ladder) ve durum renkleriyle güçlü kontrast/hiyerarşi.
//   * One UI 8.5 "Ambient Design" — saydam, bulanık, yüzen yüzeyler; nötr
//     mavi-siyah derinlik; içeriği öne çıkaran sakin zemin.
//
// Marka aksanı uygulamanın logosundan türer (cyan → mavi). Durum renkleri
// (bağlı/test/hata) tüm ekranlarda AYNI anlamı taşır: kullanıcı rengi görünce
// metni okumadan durumu anlar.
// ---------------------------------------------------------------------------

// ---- Marka rampası (logo: #22D3EE → #3B82F6) ----
val BrandCyan = Color(0xFF22D3EE)
val BrandSky = Color(0xFF38BDF8)
val BrandBlue = Color(0xFF3B82F6)
val BrandIndigo = Color(0xFF6366F1)

// Koyu temada okunur, açık temada yeterince koyu birincil tonlar.
val AccentDark = Color(0xFF5AB6FF)
val AccentLight = Color(0xFF0B69D4)

// ---- Durum renkleri (anlam sabit) ----
val StateConnectedDark = Color(0xFF34D399)   // bağlı — yeşil
val StateConnectedLight = Color(0xFF0E9F6E)
val StateBusyDark = Color(0xFFFBBF24)        // test/bağlanıyor — amber
val StateBusyLight = Color(0xFFB45309)
val StateErrorDark = Color(0xFFFB7185)       // hata — kırmızı
val StateErrorLight = Color(0xFFD92D3F)
val StateIdleDark = Color(0xFF8B96A6)        // bağlı değil — nötr
val StateIdleLight = Color(0xFF6B7482)

// ---- Zemin (aurora sahnesi) ----
val BgDarkTop = Color(0xFF070A0F)
val BgDarkBottom = Color(0xFF0C111A)
val BgLightTop = Color(0xFFF6F8FC)
val BgLightBottom = Color(0xFFE8EDF6)

// Zeminde süzülen ışık lekeleri.
val AuroraViolet = Color(0xFF6366F1)
val AuroraTeal = Color(0xFF22D3EE)

// ---- Koyu tema yüzey basamakları ----
// Her basamak bir üst katmanı temsil eder: zemin → kart → iç kart → seçili satır.
val DarkSurface = Color(0xFF0E1219)
val DarkSurfaceLowest = Color(0xFF090C12)
val DarkSurfaceLow = Color(0xFF12161E)
val DarkSurfaceContainer = Color(0xFF161B24)
val DarkSurfaceHigh = Color(0xFF1C2230)
val DarkSurfaceHighest = Color(0xFF232B3B)
val DarkOnSurface = Color(0xFFEDF2F9)
val DarkOnSurfaceVariant = Color(0xFF9EABBD)
val DarkOutline = Color(0xFF34405280)
val DarkOutlineVariant = Color(0xFF232B3A)

// ---- Açık tema yüzey basamakları ----
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceLowest = Color(0xFFFFFFFF)
val LightSurfaceLow = Color(0xFFF7F9FD)
val LightSurfaceContainer = Color(0xFFF1F4FA)
val LightSurfaceHigh = Color(0xFFE9EEF7)
val LightSurfaceHighest = Color(0xFFE1E8F3)
val LightOnSurface = Color(0xFF0E141C)
val LightOnSurfaceVariant = Color(0xFF54606F)
val LightOutline = Color(0x33334155)
val LightOutlineVariant = Color(0xFFD8E0EC)
