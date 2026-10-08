package com.csm.kitchenguard.presentation.theme

import androidx.compose.ui.graphics.Color

// ── KitchenGuard CSM Design System ───────────────────────────────────────────
// PRD Section 7: 70% White/Light Neutral + 30% Orange Brand Accent

// Brand Colors (PRD Section 7)
val PrimaryOrange       = Color(0xFFF97316)  // #F97316 (CTA, active tab, brand accent)
val StrongOrange        = Color(0xFFEA580C)  // #EA580C (Pressed/hover, dark accent)
val SurfaceWhite        = Color(0xFFFFFFFF)  // #FFFFFF (Card, dialog, surface)
val BackgroundLight     = Color(0xFFF8FAFC)  // #F8FAFC (App background)
val TextPrimary         = Color(0xFF1F2937)  // #1F2937 (Heading, body utama)

// Status / Semantic (PRD Section 7)
val StatusSuccess       = Color(0xFF16A34A)  // #16A34A (SYNCED, Fresh, OK)
val StatusWarning       = Color(0xFFD97706)  // #D97706 (Near expiry, review needed)
val StatusDanger        = Color(0xFFDC2626)  // #DC2626 (Expired, Reject, Error)

// Neutral & Secondary Tokens
val TextSecondary       = Color(0xFF64748B)  // Subtitle, placeholder, hint
val TextMuted           = Color(0xFF94A3B8)  // Caption text
val OutlineBorder       = Color(0xFFE2E8F0)  // Input border, divider, card stroke
val BackgroundMuted     = Color(0xFFF1F5F9)  // Pill inactive background

// Light Tint Helpers
val CardGoldenLight     = Color(0xFFFFF7ED)  // Light orange tint (#F97316 10%)
val CardGoldenYellow    = Color(0xFFFDBA74)  // Soft warm amber
val AccentGold          = Color(0xFFFBBF24)  // Golden accent
val WarmCardBorder      = Color(0xFFFED7AA)  // Border warm tint
val BrandOrangeVibrant  = Color(0xFFF97316)  // Brand primary
