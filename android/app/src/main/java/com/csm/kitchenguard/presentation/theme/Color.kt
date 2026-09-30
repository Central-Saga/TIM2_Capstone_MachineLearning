package com.csm.kitchenguard.presentation.theme

import androidx.compose.ui.graphics.Color

// ── KitchenGuard CSM Design System ───────────────────────────────────────────
// PRD Section 7: 70% White/Light Neutral + 30% Orange Brand Accent

// Brand Orange
val PrimaryOrange    = Color(0xFFF97316)  // CTA, active state, header
val StrongOrange     = Color(0xFFEA580C)  // Pressed/hover, icon accent

// Neutral Surface
val SurfaceWhite     = Color(0xFFFFFFFF)  // Card, dialog, input background
val BackgroundLight  = Color(0xFFF8FAFC)  // App background

// Text
val TextPrimary      = Color(0xFF1F2937)  // Heading, body utama
val TextSecondary    = Color(0xFF64748B)  // Subtitle, placeholder, hint

// Border / Divider
val OutlineBorder    = Color(0xFFE2E8F0)  // Input border, divider, card stroke

// Status / Semantic
val StatusSuccess    = Color(0xFF16A34A)  // SYNCED, Fresh, OK
val StatusWarning    = Color(0xFFD97706)  // Near expiry, review needed
val StatusDanger     = Color(0xFFDC2626)  // Expired, Reject, Error
