package com.csm.kitchenguard.presentation.components.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.csm.kitchenguard.data.local.entity.enums.FreshnessClass
import com.csm.kitchenguard.presentation.theme.PrimaryOrange
import com.csm.kitchenguard.presentation.theme.StatusDanger
import com.csm.kitchenguard.presentation.theme.StatusSuccess
import com.csm.kitchenguard.presentation.theme.StatusWarning

/**
 * Dialog konfirmasi (Human-in-the-loop) hasil deteksi AI Freshness.
 * PRD Section 16 & Figma M05.
 * Mengizinkan kasir memodifikasi hasil klasifikasi TFLite sebelum disimpan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiVerificationDialog(
    initialClass: String,
    confidenceScore: Double,
    onDismiss: () -> Unit,
    onConfirm: (finalClass: String) -> Unit
) {
    // State lokal untuk memantau perubahan manual oleh pengguna
    var selectedClass by remember { mutableStateOf(initialClass) }
    
    // Tentukan warna badge berdasarkan kelas awal (Prediksi Asli AI)
    val badgeColor = when (initialClass) {
        FreshnessClass.FRESH.name -> StatusSuccess
        FreshnessClass.ACCEPTABLE.name -> StatusWarning
        FreshnessClass.SPOILED.name, FreshnessClass.REJECT.name -> StatusDanger
        else -> Color.Gray // UNCERTAIN
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Verifikasi Hasil AI", 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Placeholder Gambar
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(Color.LightGray, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Foto Bahan", color = Color.DarkGray)
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Status Badge Prediksi Awal
                Surface(
                    color = badgeColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Prediksi: $initialClass",
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                // Tampilkan Confidence
                Text(
                    text = "Keyakinan AI: ${(confidenceScore * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )

                if (initialClass == FreshnessClass.UNCERTAIN.name) {
                    Text(
                        text = "Skor terlalu rendah (< 85%). Harap tentukan kondisi secara manual.",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusDanger,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                // Radio Button Options untuk Override Manual
                val options = listOf(
                    FreshnessClass.FRESH.name,
                    FreshnessClass.ACCEPTABLE.name,
                    FreshnessClass.SPOILED.name,
                    FreshnessClass.REJECT.name
                )
                
                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = (selectedClass == option),
                            onClick = { selectedClass = option },
                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryOrange)
                        )
                        Text(text = option, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = Color.Gray)
                    }
                    Button(
                        onClick = { onConfirm(selectedClass) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        // Nonaktifkan tombol jika pengguna masih belum memilih apa-apa dari UNCERTAIN
                        enabled = selectedClass != FreshnessClass.UNCERTAIN.name 
                    ) {
                        Text("Konfirmasi")
                    }
                }
            }
        }
    }
}
