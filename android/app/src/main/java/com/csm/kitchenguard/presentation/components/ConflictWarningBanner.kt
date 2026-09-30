package com.csm.kitchenguard.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csm.kitchenguard.data.local.entity.WasteRecordEntity
import com.csm.kitchenguard.presentation.theme.StatusDanger
import com.csm.kitchenguard.presentation.theme.StatusWarning

/**
 * Banner peringatan yang muncul di Hub Screen ketika ada transaksi dengan status CONFLICT.
 * Konflik terjadi saat shift di backend sudah dikunci (SHIFT_LOCKED) saat transaksi
 * offline mencoba disinkronisasi.
 *
 * PRD Section 23-25 — Conflict Handling & Shift Lock Resolution.
 */
@Composable
fun ConflictWarningBanner(
    conflictedWaste: List<WasteRecordEntity>,
    onRetrySync: () -> Unit
) {
    if (conflictedWaste.isEmpty()) return

    var isExpanded by remember { mutableStateOf(false) }
    val count = conflictedWaste.size
    // Tentukan warna: 1 konflik = kuning, lebih dari 1 = merah
    val bannerColor = if (count > 1) StatusDanger else StatusWarning

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        // ── Banner Utama ────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = bannerColor.copy(alpha = 0.12f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Ikon peringatan
                    Text(
                        text = "⚠",
                        fontSize = 18.sp,
                        color = bannerColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "$count Transaksi Mengalami Konflik",
                            fontWeight = FontWeight.Bold,
                            color = bannerColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Shift aktif telah dikunci di server",
                            color = bannerColor.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Text(
                    text = if (isExpanded) "Tutup ▲" else "Lihat Detail ▼",
                    color = bannerColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // ── Expanded: Detail Item Konflik ───────────────────────────────────────
        if (isExpanded) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Transaksi yang Menunggu Resolusi:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    conflictedWaste.forEach { record ->
                        ConflictItemRow(record)
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = Color.LightGray.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Pesan Instruksi
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StatusWarning.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "Minta Head Chef untuk membuka kembali kunci shift di Web Admin, lalu tekan Kirim Ulang.",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusWarning,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tombol Retry
                    Button(
                        onClick = onRetrySync,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = bannerColor)
                    ) {
                        Text(
                            text = "🔄  Kirim Ulang Semua Konflik",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConflictItemRow(record: WasteRecordEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Ingredient ID #${record.ingredientId}",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = record.clientEventAt,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${record.quantity} ${record.unit}",
                fontWeight = FontWeight.Bold,
                color = StatusDanger,
                style = MaterialTheme.typography.bodySmall
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = StatusDanger.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "CONFLICT",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusDanger,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
