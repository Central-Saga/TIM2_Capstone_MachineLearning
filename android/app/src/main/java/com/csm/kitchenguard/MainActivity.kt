package com.csm.kitchenguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.csm.kitchenguard.data.local.sync.SyncScheduler
import com.csm.kitchenguard.di.AppModule
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csm.kitchenguard.presentation.theme.BackgroundLight
import com.csm.kitchenguard.presentation.theme.CurrencyDisplayStyle
import com.csm.kitchenguard.presentation.theme.KitchenGuardTheme
import com.csm.kitchenguard.presentation.theme.OutlineBorder
import com.csm.kitchenguard.presentation.theme.PrimaryOrange
import com.csm.kitchenguard.presentation.theme.StatusDanger
import com.csm.kitchenguard.presentation.theme.StatusSuccess
import com.csm.kitchenguard.presentation.theme.SurfaceWhite
import com.csm.kitchenguard.presentation.theme.TextPrimary
import com.csm.kitchenguard.presentation.theme.TextSecondary
import com.csm.kitchenguard.presentation.theme.TokenStyle
import com.csm.kitchenguard.presentation.theme.WeightDisplayStyle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Dependency Injection Container
        val appModule = AppModule.getInstance(application)
        
        // Mulai penjadwal sinkronisasi background offline-first
        SyncScheduler.schedulePeriodicSync(applicationContext)

        setContent {
            KitchenGuardTheme {
                com.csm.kitchenguard.presentation.navigation.AppNavigation(
                    diContainer = appModule
                )
            }
        }
    }
}

// ── Theme Preview Screen ──────────────────────────────────────────────────────
// Composable ini berfungsi sebagai visual smoke test untuk memvalidasi bahwa
// Design System (Color, Typography, Theme) diterapkan dengan benar sebelum
// screen fungsional dibangun di tahap-tahap selanjutnya.
@Composable
fun ThemePreviewScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Header ────────────────────────────────────────────────────
                Text(
                    text = "KitchenGuard CSM",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Design System — FASE 1 TAHAP 04",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                HorizontalDivider(color = OutlineBorder)

                // ── Monospace Weight Display ──────────────────────────────────
                Text(
                    text = "Berat OCR",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Text(
                    text = "1.45 kg",
                    style = WeightDisplayStyle,
                    color = TextPrimary
                )

                // ── Monospace Currency Display ────────────────────────────────
                Text(
                    text = "Est. Kerugian: Rp 217.500",
                    style = CurrencyDisplayStyle,
                    color = StatusDanger
                )

                // ── Monospace Token Display ───────────────────────────────────
                Text(
                    text = "waste:c3a1b8e2-9f44-4e2b-b93d",
                    style = TokenStyle,
                    color = TextSecondary
                )

                HorizontalDivider(color = OutlineBorder)

                // ── Status Badges ─────────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = StatusSuccess.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "● SYNCED",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = StatusDanger.copy(alpha = 0.10f)
                    ) {
                        Text(
                            text = "BASI / EXPIRED",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusDanger,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ── Primary CTA Button ────────────────────────────────────────
                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryOrange,
                        contentColor   = SurfaceWhite
                    )
                ) {
                    Text(
                        text = "Catat Waste Baru",
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Preview(
    name = "KitchenGuard Theme Preview",
    showBackground = true,
    backgroundColor = 0xFFF8FAFC,
    showSystemUi = false
)
@Composable
fun ThemePreviewScreenPreview() {
    KitchenGuardTheme {
        ThemePreviewScreen()
    }
}