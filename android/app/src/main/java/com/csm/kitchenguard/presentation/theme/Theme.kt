package com.csm.kitchenguard.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ── KitchenGuard CSM Theme ────────────────────────────────────────────────────
// PRD Section 7: 70% White/Light Neutral + 30% Orange Brand Accent
// dynamicColor = false  →  warna brand tetap konsisten, tidak ikut wallpaper HP

private val KitchenGuardLightColorScheme = lightColorScheme(
    primary               = PrimaryOrange,
    onPrimary             = SurfaceWhite,
    primaryContainer      = Color(0xFFFFEDD5),   // orange tint lembut
    onPrimaryContainer    = StrongOrange,

    secondary             = StrongOrange,
    onSecondary           = SurfaceWhite,
    secondaryContainer    = Color(0xFFFED7AA),
    onSecondaryContainer  = StrongOrange,

    tertiary              = StatusSuccess,
    onTertiary            = SurfaceWhite,
    tertiaryContainer     = Color(0xFFDCFCE7),
    onTertiaryContainer   = StatusSuccess,

    error                 = StatusDanger,
    onError               = SurfaceWhite,
    errorContainer        = Color(0xFFFEE2E2),
    onErrorContainer      = StatusDanger,

    background            = BackgroundLight,
    onBackground          = TextPrimary,

    surface               = SurfaceWhite,
    onSurface             = TextPrimary,
    surfaceVariant        = Color(0xFFF1F5F9),
    onSurfaceVariant      = TextSecondary,

    outline               = OutlineBorder,
    outlineVariant        = Color(0xFFCBD5E1)
)

private val KitchenGuardDarkColorScheme = darkColorScheme(
    primary               = PrimaryOrange,
    onPrimary             = TextPrimary,
    primaryContainer      = StrongOrange,
    onPrimaryContainer    = SurfaceWhite,
    background            = Color(0xFF0F172A),
    onBackground          = SurfaceWhite,
    surface               = Color(0xFF1E293B),
    onSurface             = SurfaceWhite,
    surfaceVariant        = Color(0xFF334155),
    onSurfaceVariant      = TextSecondary,
    outline               = Color(0xFF475569),
    outlineVariant        = Color(0xFF334155)
)

@Composable
fun KitchenGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // dynamicColor dinonaktifkan — brand palette harus konsisten di semua device
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> KitchenGuardDarkColorScheme
        else      -> KitchenGuardLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
