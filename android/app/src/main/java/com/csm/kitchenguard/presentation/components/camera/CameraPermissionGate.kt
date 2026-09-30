package com.csm.kitchenguard.presentation.components.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/**
 * Membungkus konten kamera dan memastikan izin CAMERA sudah diberikan (Issue #18).
 *
 * Perilaku:
 * - Saat pertama tampil, bila izin belum ada → otomatis meminta izin.
 * - [GRANTED]            → menampilkan [content].
 * - [DENIED]             → menampilkan pesan + tombol "Izinkan Kamera".
 * - [PERMANENTLY_DENIED] → menampilkan pesan + tombol "Buka Pengaturan".
 *
 * Selama izin belum diberikan, [content] TIDAK dipanggil sehingga `startCamera`
 * tidak pernah dieksekusi tanpa izin.
 */
@Composable
fun CameraPermissionGate(
    permissionDeniedMessage: String = "Akses kamera diperlukan untuk memindai timbangan. " +
        "Aktifkan izin kamera untuk melanjutkan.",
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    var state by remember {
        mutableStateOf(
            if (hasCameraPermission(context)) {
                CameraPermissionState.GRANTED
            } else {
                CameraPermissionState.DENIED
            }
        )
    }
    var hasRequestedBefore by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRequestedBefore = true
        state = resolveCameraPermissionState(
            granted = granted,
            hasRequestedBefore = true,
            showRationale = false
        )
    }

    // Re-evaluasi saat kembali dari Settings (resume).
    LaunchedEffect(Unit) {
        if (hasCameraPermission(context)) {
            state = CameraPermissionState.GRANTED
        } else if (!hasRequestedBefore) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    when (state) {
        CameraPermissionState.GRANTED -> content()

        CameraPermissionState.DENIED -> PermissionRationale(
            message = permissionDeniedMessage,
            primaryLabel = "Izinkan Kamera",
            onPrimary = {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onDismiss = onDismiss
        )

        CameraPermissionState.PERMANENTLY_DENIED -> PermissionRationale(
            message = "Izin kamera diblokir. Buka Pengaturan aplikasi lalu aktifkan " +
                "izin Kamera untuk memindai timbangan.",
            primaryLabel = "Buka Pengaturan",
            onPrimary = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(intent)
            },
            onDismiss = onDismiss
        )
    }
}

/** True bila izin CAMERA sudah diberikan. */
private fun hasCameraPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

@Composable
private fun PermissionRationale(
    message: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFBBF24),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Izin Kamera Diperlukan",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onPrimary,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(primaryLabel, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Batal")
            }
        }
    }
}
