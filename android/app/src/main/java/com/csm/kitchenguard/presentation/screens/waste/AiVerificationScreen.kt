package com.csm.kitchenguard.presentation.screens.waste

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.csm.kitchenguard.R
import com.csm.kitchenguard.data.local.entity.enums.FreshnessClass
import com.csm.kitchenguard.presentation.components.camera.CameraPermissionGate
import com.csm.kitchenguard.presentation.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// Design Tokens (Figma M05)
// ─────────────────────────────────────────────────────────────────────────────
private val ModelBadgeBg = Color(0xFFF1F5F9)
private val ModelBadgeText = Color(0xFF475569)
private val StatusRedBg = Color(0xFFDC2626)
private val StatusRedText = Color(0xFFFFFFFF)
private val LightRedBg = Color(0xFFFEE2E2)
private val LightRedText = Color(0xFFB91C1C)
private val ConfidenceBg = Color(0xFFFCE7F3)
private val ConfidenceText = Color(0xFFBE185D)
private val InfoRowBg = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiVerificationScreen(
    onNavigateBack: () -> Unit,
    onSubmitWriteOff: (chosenClass: String, confidence: Double) -> Unit,
    viewModel: AiVerificationViewModel,
    ingredientName: String? = null,
    ingredientCategory: String? = null,
    quantityDisplay: String? = null,
    onRequestCapture: (() -> Unit)? = null
) {
    // Observasi state AI yang berasal dari inferensi TFLite (bukan hardcoded).
    val uiState by viewModel.uiState.collectAsState()
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedClass by remember(uiState.predictedClass) {
        mutableStateOf(uiState.predictedClass)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // ── 1. Top App Bar & Model Indicator ──────────────────────────────────
        TopBarM05(onNavigateBack = onNavigateBack)
        ModelIndicator(uiState = uiState)

        // Konten utama scrollable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // ── 2. Card Frame Kamera (Live CameraX Viewfinder) ─────────────────
            CameraFrameCard(
                uiState = uiState,
                capturedBitmap = capturedBitmap,
                onCapture = { bitmap ->
                    capturedBitmap = bitmap
                    viewModel.classify(bitmap)
                },
                onRetake = {
                    capturedBitmap = null
                }
            )

            // ── 3. Card Hasil Analisis AI ─────────────────────────────────────
            AiAnalysisResultCard(
                uiState = uiState,
                selectedClass = selectedClass,
                onSelectClass = { selectedClass = it }
            )

            // ── 4. Card Identifikasi Bahan & Nilai ────────────────────────────
            IdentificationCard(
                ingredientName = ingredientName,
                ingredientCategory = ingredientCategory,
                quantityDisplay = quantityDisplay
            )

            // ── 5. Tombol Aksi Bawah ──────────────────────────────────────────
            ActionButtonsM05(
                uiState = uiState,
                selectedClass = selectedClass,
                onSubmitWriteOff = {
                    val finalCls = if (uiState.requiresHumanReview && selectedClass == FreshnessClass.UNCERTAIN.name) {
                        FreshnessClass.SPOILED.name
                    } else {
                        selectedClass
                    }
                    onSubmitWriteOff(finalCls, uiState.confidenceScore)
                },
                onRescan = {
                    capturedBitmap = null
                    if (onRequestCapture != null) {
                        onRequestCapture()
                    }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Top Bar & Model Indicator
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TopBarM05(onNavigateBack: () -> Unit) {
    Surface(
        color = PrimaryOrange,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Kiri: Logo + Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_kitchenguard_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "KITCHENGUARD CSM",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Timbang & Scan",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }

            // Kanan: Sync Badge + Notif
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6EE7B7))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SYNCED",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Box {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                            .border(1.dp, PrimaryOrange, CircleShape)
                            .align(Alignment.TopEnd)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelIndicator(uiState: AiVerificationUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = InfoRowBg,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TENSORFLOW LITE ON-DEVICE MODEL",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
            }

            // Status model diambil dari state nyata (bukan badge hardcoded).
            val modelReady = uiState.isModelAvailable
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = if (modelReady) ModelBadgeBg else LightRedBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (modelReady) Color(0xFF059669) else StatusRedBg)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (modelReady) "Offline Ready" else "Model N/A",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        color = if (modelReady) ModelBadgeText else LightRedText
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Card Frame Kamera
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CameraFrameCard(
    uiState: AiVerificationUiState,
    capturedBitmap: Bitmap?,
    onCapture: (Bitmap) -> Unit,
    onRetake: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            .height(260.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (capturedBitmap != null) {
                // Tampilkan foto hasil tangkapan kamera
                Image(
                    bitmap = capturedBitmap.asImageBitmap(),
                    contentDescription = "Hasil Foto Bahan",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Live Camera Viewfinder via CameraX
                CameraPermissionGate(
                    permissionDeniedMessage = "Akses kamera diperlukan untuk memindai mutu bahan secara real-time.",
                    onDismiss = {}
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = CameraPreview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview
                                    )
                                } catch (exc: Exception) {
                                    exc.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        }
                    )
                }
            }

            // HUD Overlay (Top)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = null,
                            tint = PrimaryOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${uiState.modelVersion} • On-Device",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (capturedBitmap != null) Color(0xFF16A34A) else Color.Red)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (capturedBitmap != null) "FRAME TERTANGKAP" else "LIVE CAMERA",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // Focus Brackets (Orange Corners)
            val bracketColor = PrimaryOrange
            val thickness = 2.dp
            val length = 24.dp

            Box(Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 40.dp).size(length).border(BorderStroke(thickness, bracketColor), RoundedCornerShape(topStart = 4.dp)).padding(start = thickness, top = thickness).background(Color.Transparent))
            Box(Modifier.align(Alignment.TopEnd).padding(end = 24.dp, top = 40.dp).size(length).border(BorderStroke(thickness, bracketColor), RoundedCornerShape(topEnd = 4.dp)).padding(end = thickness, top = thickness).background(Color.Transparent))
            Box(Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 54.dp).size(length).border(BorderStroke(thickness, bracketColor), RoundedCornerShape(bottomStart = 4.dp)).padding(start = thickness, bottom = thickness).background(Color.Transparent))
            Box(Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 54.dp).size(length).border(BorderStroke(thickness, bracketColor), RoundedCornerShape(bottomEnd = 4.dp)).padding(end = thickness, bottom = thickness).background(Color.Transparent))

            // Bottom Shutter / Retake Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                color = Color.Black.copy(alpha = 0.65f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (capturedBitmap != null) {
                        Button(
                            onClick = onRetake,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pindai Ulang Kamera",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                val bmp = previewView.bitmap ?: createPlaceholderBitmap()
                                onCapture(bmp)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                            shape = RoundedCornerShape(50.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ambil & Analisis AI",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Card Hasil Analisis AI
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AiAnalysisResultCard(
    uiState: AiVerificationUiState,
    selectedClass: String,
    onSelectClass: (String) -> Unit
) {
    // Warna aksen & status diturunkan dari state AI yang nyata.
    val accentColor: Color = when {
        !uiState.isModelAvailable -> TextSecondary
        uiState.phase == AiScanPhase.ANALYZING -> PrimaryOrange
        uiState.requiresHumanReview -> PrimaryOrange
        selectedClass == FreshnessClass.SPOILED.name ||
            selectedClass == FreshnessClass.REJECT.name -> StatusRedBg
        else -> Color(0xFF059669)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = InfoRowBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Aksen garis vertikal di kiri, warnanya mengikuti status.
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .defaultMinSize(minHeight = 230.dp)
                    .background(accentColor)
            )

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TFLITE ${uiState.modelVersion.uppercase()}",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (uiState.isModelAvailable) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ConfidenceBg
                        ) {
                            Text(
                                text = "Confidence: %.1f%%".format(uiState.confidencePercent),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = ConfidenceText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (uiState.phase) {
                    AiScanPhase.MODEL_UNAVAILABLE ->
                        ModelUnavailableBody(uiState = uiState)

                    AiScanPhase.ANALYZING ->
                        AnalyzingBody()

                    AiScanPhase.IDLE ->
                        IdleBody()

                    AiScanPhase.DONE ->
                        ResultBody(
                            uiState = uiState,
                            selectedClass = selectedClass,
                            onSelectClass = onSelectClass,
                            accentColor = accentColor
                        )
                }
            }
        }
    }
}

/** Body saat model TFLite tidak tersedia — pesan jujur, tanpa tebakan. */
@Composable
private fun ModelUnavailableBody(uiState: AiVerificationUiState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(4.dp), color = LightRedBg) {
            Text(
                text = "AI TIDAK TERSEDIA",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = LightRedText
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Menunggu Observasi Manual",
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        color = TextPrimary
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Model AI kesegaran belum tersedia di perangkat ini, sehingga sistem " +
            "tidak memberikan penilaian otomatis. Staff wajib memverifikasi " +
            "kesegaran bahan secara visual sesuai SOP.",
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = TextSecondary,
        lineHeight = 18.sp
    )

    uiState.errorMessage?.let { message ->
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF8FAFC),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(12.dp),
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}

/** Body saat belum ada gambar untuk dianalisis. */
@Composable
private fun IdleBody() {
    Text(
        text = "Siap Memindai Bahan",
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = TextPrimary
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Arahkan kamera ke bahan lalu tekan tombol Ambil & Analisis AI di atas untuk menjalankan " +
            "klasifikasi kesegaran on-device secara instan.",
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = TextSecondary,
        lineHeight = 18.sp
    )
}

/** Body saat inferensi sedang berjalan. */
@Composable
private fun AnalyzingBody() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = PrimaryOrange
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Menganalisis kesegaran bahan…",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = TextPrimary
        )
    }
}

/** Body saat inferensi selesai (model tersedia). */
@Composable
private fun ResultBody(
    uiState: AiVerificationUiState,
    selectedClass: String,
    onSelectClass: (String) -> Unit,
    accentColor: Color
) {
    // Status badge
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(4.dp), color = accentColor) {
            Text(
                text = "STATUS $selectedClass",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Risiko: ${riskLabel(uiState)}",
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            color = accentColor
        )
    }

    Spacer(modifier = Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = titleForClass(selectedClass),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = accentColor
        )
        Icon(
            imageVector = if (uiState.requiresHumanReview) {
                Icons.Default.VerifiedUser
            } else {
                Icons.Default.CheckCircle
            },
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(24.dp)
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = descriptionForClass(selectedClass),
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = TextSecondary,
        lineHeight = 18.sp
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Waktu inferensi: ${uiState.inferenceTimeMs} ms • Prediksi Awal: ${uiState.predictedClass}",
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        color = TextSecondary
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Kotak SOP / decision support & manual selection
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF8FAFC),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.VerifiedUser,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (uiState.requiresHumanReview) {
                            "Perlu Observasi Manual (Confidence < 85%)"
                        } else {
                            "Decision Support (AI Terverifikasi)"
                        },
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (uiState.requiresHumanReview) {
                            "TFLite menghasilkan status UNCERTAIN (ambang 85%). AI tidak berhak auto-reject. Pilih observasi visual fisik di bawah:"
                        } else {
                            "Hasil AI bersifat pendukung keputusan. Anda dapat mengubah kelas mutu jika evaluasi fisik berbeda."
                        },
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selector Mutu Fisik (Decision Support & Override)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    FreshnessClass.FRESH.name to Color(0xFF16A34A),
                    FreshnessClass.ACCEPTABLE.name to Color(0xFFD97706),
                    FreshnessClass.SPOILED.name to Color(0xFFEA580C),
                    FreshnessClass.REJECT.name to Color(0xFFDC2626)
                ).forEach { (cls, color) ->
                    val isSelected = selectedClass == cls
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) color else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) color else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectClass(cls) }
                    ) {
                        Text(
                            text = cls,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = if (isSelected) Color.White else TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun titleForClass(className: String): String = when (className) {
    FreshnessClass.FRESH.name -> "SEGAR / FRESH"
    FreshnessClass.ACCEPTABLE.name -> "MASIH LAYAK"
    FreshnessClass.SPOILED.name -> "BASI / SPOILED"
    FreshnessClass.REJECT.name -> "TOLAK / REJECT"
    else -> "PERLU OBSERVASI"
}

private fun descriptionForClass(className: String): String = when (className) {
    FreshnessClass.FRESH.name ->
        "Bahan dalam kondisi segar optimal dan layak digunakan dalam operasional."
    FreshnessClass.ACCEPTABLE.name ->
        "Bahan masih layak pakai namun perlu segera diprioritaskan penggunaannya."
    FreshnessClass.SPOILED.name ->
        "Bahan terindikasi mulai membusuk / basi. Disarankan untuk write-off."
    FreshnessClass.REJECT.name ->
        "Bahan tidak layak konsumsi dan harus segera dimusnahkan."
    else ->
        "Model tidak cukup yakin pada gambar ini. Lakukan observasi visual manual."
}

private fun riskLabel(uiState: AiVerificationUiState): String = when {
    uiState.requiresHumanReview -> "Perlu Review"
    uiState.predictedClass == FreshnessClass.FRESH.name -> "Rendah"
    uiState.predictedClass == FreshnessClass.ACCEPTABLE.name -> "Sedang"
    uiState.predictedClass == FreshnessClass.REJECT.name -> "Ekstrem"
    else -> "Tinggi"
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Card Identifikasi Bahan & Nilai
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IdentificationCard(
    ingredientName: String?,
    ingredientCategory: String?,
    quantityDisplay: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "IDENTIFIKASI BAHAN DARI FORM",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Konteks Terhubung",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color(0xFF059669)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF1F5F9),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kiri
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ingredientName ?: "Bahan Terpilih (Kitchen Station)",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ingredientCategory ?: "Kategori: Bahan Baku Dapur",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
                
                // Kanan
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = quantityDisplay ?: "—",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF92400E)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Kuantitas Waste",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Tombol Aksi Bawah
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ActionButtonsM05(
    uiState: AiVerificationUiState,
    selectedClass: String,
    onSubmitWriteOff: () -> Unit,
    onRescan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tombol Utama Oranye — menuliskan hasil ke record waste.
        Button(
            onClick = onSubmitWriteOff,
            enabled = uiState.phase != AiScanPhase.ANALYZING,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gunakan Mutu $selectedClass & Lanjutkan",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tombol Outlined — memicu capture & analisis ulang.
        OutlinedButton(
            onClick = onRescan,
            enabled = uiState.phase != AiScanPhase.ANALYZING,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = TextPrimary
            ),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = PrimaryOrange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scan Ulang Kamera",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Membuat Bitmap abu-abu berukuran 224x224 sebagai placeholder uji-coba.
 */
private fun createPlaceholderBitmap(): android.graphics.Bitmap {
    val size = 224
    val bitmap = android.graphics.Bitmap.createBitmap(
        size, size, android.graphics.Bitmap.Config.ARGB_8888
    )
    bitmap.eraseColor(android.graphics.Color.rgb(51, 65, 85)) // slate gray
    return bitmap
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEW
// ─────────────────────────────────────────────────────────────────────────────
@Preview(
    name = "AI Verification - Figma M05",
    showBackground = true,
    backgroundColor = 0xFFF8FAFC,
    device = "spec:width=390dp,height=844dp,dpi=420"
)
@SuppressLint("ViewModelConstructorInComposable")
@Composable
private fun AiVerificationScreenPreview() {
    KitchenGuardTheme {
        val app = androidx.compose.ui.platform.LocalContext.current
            .applicationContext as android.app.Application
        AiVerificationScreen(
            onNavigateBack = {},
            onSubmitWriteOff = { _, _ -> },
            viewModel = AiVerificationViewModel(
                application = app,
                freshnessClassifier = com.csm.kitchenguard.utils.ai.FreshnessClassifier(app)
            ),
            ingredientName = "Wagyu Ribeye MB7",
            ingredientCategory = "Meat & Butcher",
            quantityDisplay = "1.45 kg"
        )
    }
}
