package com.csm.kitchenguard.presentation.screens.waste

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
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
import /* androidx.compose.material.icons.filled.FlashOn */ androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csm.kitchenguard.data.local.entity.enums.FreshnessClass
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
    onSubmitWriteOff: () -> Unit,
    viewModel: AiVerificationViewModel,
    /**
     * Dipanggil saat staff menekan "Scan Ulang / Analisis".
     * Pemanggil bertanggung jawab menyediakan Bitmap dari kamera/evidence,
     * lalu memanggil [AiVerificationViewModel.classify].
     *
     * Bila null, tombol analisis memakai gambar placeholder abu-abu agar
     * pipeline inferensi tetap dapat diuji pada perangkat tanpa kamera.
     */
    onRequestCapture: (() -> Unit)? = null
) {
    // Observasi state AI yang berasal dari inferensi TFLite (bukan hardcoded).
    val uiState by viewModel.uiState.collectAsState()

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
            // ── 2. Card Frame Kamera (Live Viewfinder) ────────────────────────
            CameraFrameCard(uiState = uiState)

            // ── 3. Card Hasil Analisis AI ─────────────────────────────────────
            AiAnalysisResultCard(uiState = uiState)

            // ── 4. Card Identifikasi Bahan & Nilai ────────────────────────────
            IdentificationCard()

            // ── 5. Tombol Aksi Bawah ──────────────────────────────────────────
            ActionButtonsM05(
                uiState = uiState,
                onSubmitWriteOff = onSubmitWriteOff,
                onRescan = {
                    // Prioritaskan capture kamera nyata bila disediakan.
                    if (onRequestCapture != null) {
                        onRequestCapture()
                    } else {
                        // Fallback uji-coba: gambar placeholder agar pipeline
                        // inferensi tetap dapat dijalankan/di-demo tanpa kamera.
                        viewModel.classify(createPlaceholderBitmap())
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
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("👨‍🍳", fontSize = 20.sp)
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
private fun CameraFrameCard(uiState: AiVerificationUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            .height(240.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray), // Placeholder foto
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Mock background image (gray for now to represent the photo)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF334155)) // Slate gray
            )
            
            // HUD Overlay (Top)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // TFLite Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CropFree,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${uiState.modelVersion} • Offline",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }
                
                // REC Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "REC 60 FPS",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }
            }
            
            // Focus Brackets (Orange Corners)
            val bracketColor = PrimaryOrange
            val thickness = 2.dp
            val length = 24.dp
            
            // Top Left
            Box(Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 36.dp).size(length).border(BorderStroke(thickness, bracketColor), RoundedCornerShape(topStart = 4.dp)).padding(start = thickness, top = thickness).background(Color.Transparent))
            // Top Right
            Box(Modifier.align(Alignment.TopEnd).padding(end = 24.dp, top = 36.dp).size(length).border(BorderStroke(thickness, bracketColor), RoundedCornerShape(topEnd = 4.dp)).padding(end = thickness, top = thickness).background(Color.Transparent))
            // Bottom Left
            Box(Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 48.dp).size(length).border(BorderStroke(thickness, bracketColor), RoundedCornerShape(bottomStart = 4.dp)).padding(start = thickness, bottom = thickness).background(Color.Transparent))
            // Bottom Right
            Box(Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 48.dp).size(length).border(BorderStroke(thickness, bracketColor), RoundedCornerShape(bottomEnd = 4.dp)).padding(end = thickness, bottom = thickness).background(Color.Transparent))

            // Bottom Controls Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = null,
                            tint = PrimaryOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Auto-Focus Lensa Makro",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FlashlightOn,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "TORCH ON",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
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
private fun AiAnalysisResultCard(uiState: AiVerificationUiState) {
    // Warna aksen & status diturunkan dari state AI yang nyata.
    val accentColor: Color = when {
        !uiState.isModelAvailable -> TextSecondary
        uiState.phase == AiScanPhase.ANALYZING -> PrimaryOrange
        uiState.requiresHumanReview -> PrimaryOrange
        uiState.predictedClass == FreshnessClass.SPOILED.name ||
            uiState.predictedClass == FreshnessClass.REJECT.name -> StatusRedBg
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
                        ResultBody(uiState = uiState, accentColor = accentColor)
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
        text = "Siap Memindai",
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        color = TextPrimary
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Arahkan kamera ke bahan lalu tekan Analisis untuk menjalankan " +
            "klasifikasi kesegaran on-device.",
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
private fun ResultBody(uiState: AiVerificationUiState, accentColor: Color) {
    // Status badge
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(4.dp), color = accentColor) {
            Text(
                text = "STATUS ${uiState.predictedClass}",
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
            text = titleFor(uiState),
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
        text = descriptionFor(uiState),
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = TextSecondary,
        lineHeight = 18.sp
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Waktu inferensi: ${uiState.inferenceTimeMs} ms",
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        color = TextSecondary
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Kotak SOP / call-to-action manusia
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF8FAFC),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
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
                        "Diperlukan Verifikasi Manual"
                    } else {
                        "Decision Support (AI)"
                    },
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (uiState.requiresHumanReview) {
                        "Confidence di bawah 85%. AI tidak berhak menolak otomatis — " +
                            "staff wajib mengonfirmasi secara manual."
                    } else {
                        "Hasil AI bersifat pendukung keputusan. Keputusan akhir tetap " +
                            "pada staff sesuai SOP."
                    },
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

private fun titleFor(uiState: AiVerificationUiState): String = when (uiState.predictedClass) {
    FreshnessClass.FRESH.name -> "SEGAR / FRESH"
    FreshnessClass.ACCEPTABLE.name -> "MASIH LAYAK"
    FreshnessClass.SPOILED.name -> "BASI / SPOILED"
    FreshnessClass.REJECT.name -> "TOLAK / REJECT"
    else -> "PERLU OBSERVASI"
}

private fun descriptionFor(uiState: AiVerificationUiState): String = when (uiState.predictedClass) {
    FreshnessClass.FRESH.name ->
        "Bahan dalam kondisi segar optimal dan layak digunakan."
    FreshnessClass.ACCEPTABLE.name ->
        "Bahan masih layak pakai namun perlu segera diprioritaskan penggunaannya."
    FreshnessClass.SPOILED.name ->
        "Bahan terindikasi mulai membusuk. Verifikasi visual lanjutan disarankan."
    FreshnessClass.REJECT.name ->
        "Bahan tidak layak konsumsi berdasarkan klasifikasi model kesegaran."
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
private fun IdentificationCard() {
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
                text = "IDENTIFIKASI BAHAN & NILAI",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Memory, // Placeholder IoT icon
                    contentDescription = null,
                    tint = Color(0xFF059669), // Green
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "IoT Tereduksi",
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
            color = Color(0xFFF1F5F9), // Gray background
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kiri
                Column {
                    Text(
                        text = "Wagyu Ribeye MB7",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "SKU: ING-MEAT-004 • Chiller A2",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
                
                // Kanan
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "1.45",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF92400E) // Coklat
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "kg",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Nilai: Rp 217.500",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        color = StatusRedBg
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
        // Dinonaktifkan selama analisis berjalan.
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
                    imageVector = Icons.Outlined.Save, // Placeholder for upload/save
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gunakan Hasil & Ajukan Write-Off",
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
 *
 * Dipakai HANYA ketika tidak ada sumber kamera ([AiVerificationScreen.onRequestCapture]
 * null), agar pipeline inferensi tetap dapat dijalankan. Input ini tetap
 * melewati model TFLite yang sama — tidak ada nilai hasil yang dipalsukan.
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
            onSubmitWriteOff = {},
            viewModel = AiVerificationViewModel(
                application = app,
                freshnessClassifier = com.csm.kitchenguard.utils.ai.FreshnessClassifier(app)
            )
        )
    }
}
