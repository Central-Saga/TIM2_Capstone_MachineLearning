package com.csm.kitchenguard.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Font Families ─────────────────────────────────────────────────────────────
// PRD Section 7:
//   Inter          → teks umum, label, form, deskripsi
//   JetBrains Mono → angka bobot OCR, nilai Rupiah, idempotency key
//
// NOTE: FontFamily.SansSerif mewakili Inter (system fallback) sampai font
//       resource di-bundle di res/font/ pada tahap polish nanti.
val InterFontFamily     = FontFamily.SansSerif
val JetBrainsMonoFamily = FontFamily.Monospace

// ── Material3 Typography (Inter) ──────────────────────────────────────────────
val Typography = Typography(
    // Nama layar / section header besar
    titleLarge = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 22.sp,
        lineHeight    = 28.sp,
        letterSpacing = 0.sp
    ),
    // Sub-header card / section
    titleMedium = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // Teks body umum
    bodyLarge = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.4.sp
    ),
    // Label tombol / badge / chip
    labelLarge = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 11.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// ── Custom TextStyle: JetBrains Mono ─────────────────────────────────────────
// Digunakan langsung via `style = WeightDisplayStyle` di Composable,
// BUKAN via MaterialTheme.typography — karena ini di luar slot Material3.

/** Angka bobot timbangan OCR — contoh: "1.45 kg" */
val WeightDisplayStyle = TextStyle(
    fontFamily    = JetBrainsMonoFamily,
    fontWeight    = FontWeight.Bold,
    fontSize      = 28.sp,
    lineHeight    = 36.sp,
    letterSpacing = 0.sp
)

/** Nilai mata uang Rupiah — contoh: "Rp 315.000" */
val CurrencyDisplayStyle = TextStyle(
    fontFamily    = JetBrainsMonoFamily,
    fontWeight    = FontWeight.SemiBold,
    fontSize      = 16.sp,
    lineHeight    = 24.sp,
    letterSpacing = 0.sp
)

/** Token teknis / idempotency key — contoh: "waste:c3a1b8e2-9f44-..." */
val TokenStyle = TextStyle(
    fontFamily    = JetBrainsMonoFamily,
    fontWeight    = FontWeight.Normal,
    fontSize      = 12.sp,
    lineHeight    = 18.sp,
    letterSpacing = 0.sp
)
