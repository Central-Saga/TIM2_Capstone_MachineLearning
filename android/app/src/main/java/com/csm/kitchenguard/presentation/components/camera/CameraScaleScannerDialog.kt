package com.csm.kitchenguard.presentation.components.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.csm.kitchenguard.presentation.theme.PrimaryOrange
import com.csm.kitchenguard.presentation.theme.StatusDanger
import com.csm.kitchenguard.presentation.theme.StatusSuccess
import com.csm.kitchenguard.presentation.theme.StatusWarning
import com.csm.kitchenguard.utils.ocr.OcrScaleReading
import java.util.concurrent.Executors

@Composable
fun CameraScaleScannerDialog(
    onDismiss: () -> Unit,
    onWeightConfirmed: (reading: OcrScaleReading) -> Unit
) {
    // Wajibkan izin CAMERA sebelum kamera dibuka (Issue #18).
    // Bila izin belum ada, gate menampilkan pesan/rationale dan konten kamera
    // TIDAK dirender — sehingga startCamera tidak pernah dipanggil tanpa izin.
    CameraPermissionGate(onDismiss = onDismiss) {
        CameraScannerContent(
            onDismiss = onDismiss,
            onWeightConfirmed = onWeightConfirmed
        )
    }
}

@Composable
private fun CameraScannerContent(
    onDismiss: () -> Unit,
    onWeightConfirmed: (reading: OcrScaleReading) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State hasil OCR terbaru
    var currentReading by remember { mutableStateOf<OcrScaleReading?>(null) }

    val currentScannedText = currentReading?.rawText ?: "Memindai..."
    val confidenceAvailable = currentReading?.confidenceAvailable == true
    val currentConfidence: Float = currentReading?.confidence ?: 0f

    // Warna threshold dinamis berdasarkan confidence (hanya bila tersedia)
    val indicatorColor = when {
        !confidenceAvailable -> StatusWarning
        currentConfidence >= 0.90f -> StatusSuccess
        currentConfidence >= 0.70f -> StatusWarning
        else -> StatusDanger
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Camera View
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                startCamera(ctx, lifecycleOwner, previewView) { reading ->
                    currentReading = reading
                }
                previewView
            }
        )

        // 2. Overlay Kotak Pemindaian (Targeting Box)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = 300.dp, height = 150.dp)
                .border(2.dp, indicatorColor, RoundedCornerShape(8.dp))
                .clipToBounds()
        ) {
            // Corners/Guides visual bisa ditambahkan di sini (Opsional)
        }

        // 3. Status Bar (Bawah)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentScannedText,
                style = MaterialTheme.typography.headlineLarge,
                color = indicatorColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when {
                    !confidenceAvailable -> "Confidence tidak tersedia — Konfirmasi Manual"
                    currentConfidence >= 0.90f -> "Terbaca Jelas"
                    else -> "Konfirmasi Manual Diperlukan"
                },
                color = indicatorColor,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Batal")
                }

                Button(
                    onClick = {
                        currentReading?.let { onWeightConfirmed(it) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    // Cegah konfirmasi bila belum ada bacaan, atau confidence tersedia
                    // tapi terlalu rendah (noise). Bila confidence TIDAK tersedia,
                    // staff tetap boleh mengonfirmasi angka secara manual.
                    enabled = currentReading != null &&
                        (!confidenceAvailable || currentConfidence > 0.5f)
                ) {
                    Text("Gunakan Angka Ini")
                }
            }
        }
    }
}

private fun startCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    onResult: (OcrScaleReading) -> Unit
) {
    // Pertahanan berlapis (Issue #18): jangan pernah menyalakan kamera tanpa izin.
    // Meski gate sudah menjaga pemanggilan, cek ini mencegah crash bila startCamera
    // dipanggil dari jalur lain di masa depan.
    val granted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
    if (!granted) {
        android.util.Log.w("CameraScaleScanner", "startCamera dipanggil tanpa izin CAMERA — dibatalkan.")
        return
    }

    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val executor = Executors.newSingleThreadExecutor()

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(executor, ScaleOcrAnalyzer { reading ->
                    onResult(reading)
                })
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
        } catch (exc: Exception) {
            exc.printStackTrace()
        }
    }, ContextCompat.getMainExecutor(context))
}
