package com.csm.kitchenguard.presentation.screens.waste

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csm.kitchenguard.data.local.entity.IngredientEntity
import com.csm.kitchenguard.presentation.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// Design Tokens Khusus (Figma M04)
// ─────────────────────────────────────────────────────────────────────────────
private val StatusGreen = Color(0xFF16A34A)
private val DarkViewfinderBg = Color(0xFF1E293B)
private val ScaleTextGreen = Color(0xFF4ADE80)
private val BadgeBgOrangeLight = Color(0xFFFFEDD5)
private val BadgeTextOrangeDark = Color(0xFFEA580C)
private val PillBadgeYellowBg = Color(0xFFFEF3C7)
private val PillBadgeYellowText = Color(0xFF92400E)
private val PinkBleedBg = Color(0xFFFCE7F3)
private val PinkBleedText = Color(0xFFBE185D)
private val ChipDefaultBg = Color(0xFFF1F5F9)
private val ChipDefaultText = Color(0xFF475569)
private val InfoRowBg = Color(0xFFFFFFFF)

// ─────────────────────────────────────────────────────────────────────────────
// Layar Waste Logging (Figma M04)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasteLoggingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAiVerification: () -> Unit,
    viewModel: WasteLoggingViewModel
) {
    // Observasi state dari ViewModel
    val uiState by viewModel.uiState.collectAsState()
    
    val ingredientsList by viewModel.ingredientsList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Top App Bar (Oranye)
        TopBarM04(onNavigateBack = onNavigateBack)

        // Konten utama scrollable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Header (Catat Waste)
            HeaderSection()

            // Card Deteksi Timbangan (Viewfinder)
            ScaleDetectionCard(viewModel)

            // Section Pilih Bahan Baku
            SelectIngredientSection(uiState, viewModel, ingredientsList)

            // Section Alasan Pemusnahan
            WasteReasonSection(uiState, viewModel)

            // Card Estimasi Kerugian
            FinancialLossCard(uiState)

            // Tombol Aksi Bawah
            ActionFooterSection(
                uiState = uiState,
                viewModel = viewModel,
                onNavigateToAiVerification = onNavigateToAiVerification
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Top Bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TopBarM04(onNavigateBack: () -> Unit) {
    Surface(
        color = PrimaryOrange,
        shadowElevation = 4.dp
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
                        contentDescription = "Notifikasi",
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

// ─────────────────────────────────────────────────────────────────────────────
// Header (Icon QR + Title)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HeaderSection() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = InfoRowBg,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon QR Oranye
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BadgeBgOrangeLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Catat Waste",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Pencatatan Cepat Bahan Terbuang",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
            
            // Titik Hijau
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(StatusGreen)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Card Deteksi Timbangan (Viewfinder)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ScaleDetectionCard(viewModel: WasteLoggingViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkViewfinderBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Mock Viewfinder corners (Oranye)
            ViewfinderCorner(Modifier.align(Alignment.TopStart))
            ViewfinderCorner(Modifier.align(Alignment.TopEnd).padding(end = 4.dp, top = 4.dp))
            ViewfinderCorner(Modifier.align(Alignment.BottomStart).padding(start = 4.dp, bottom = 4.dp))
            ViewfinderCorner(Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 4.dp))

            // Center Content
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Detected Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ScaleTextGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DETECTED SCALE DISPLAY",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = ScaleTextGreen,
                        letterSpacing = 1.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Angka - ambil dari OCR hasil atau manual input
                val displayWeight = if (uiState.ocrWeight != null) {
                    uiState.ocrWeight.toString()
                } else {
                    uiState.quantityInput.ifEmpty { "0.00" }
                }
                
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = displayWeight,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 56.sp,
                        color = ScaleTextGreen
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = uiState.selectedUnit ?: "kg",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 24.sp,
                        color = ScaleTextGreen,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Confidence & Tare
                val ocrConfidence = uiState.ocrConfidence
                val confidenceText = if (uiState.ocrConfidenceAvailable && ocrConfidence != null) {
                    String.format("Confidence: %.1f%% • Tare 0.00kg", ocrConfidence * 100)
                } else {
                    "Confidence: N/A • Tare 0.00kg"
                }
                Text(
                    text = confidenceText,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Bottom Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Timbang Ulang
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.clickable { 
                        // Clear OCR data dan trigger ulang scan
                        viewModel.onOcrCaptured("", 0.0)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Timbang Ulang",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
                
                // Flash
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.clickable { /* action */ }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Flash",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewfinderCorner(modifier: Modifier = Modifier) {
    // Simple placeholder for corners. In a real app, you'd draw the lines with Canvas.
    // For now, we leave it as an empty box or skip drawing complex corners to save code.
    // Assuming simple padding gives enough "viewfinder" feel.
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Section Pilih Bahan Baku
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SelectIngredientSection(
    uiState: WasteFormUiState, 
    viewModel: WasteLoggingViewModel,
    ingredientsList: List<IngredientEntity>
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
                text = "Pilih Bahan Baku",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Text(
                text = "MEAT LINE #02",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = BadgeTextOrangeDark
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Selected Item Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = ChipDefaultBg, // Abu-abu muda
            border = BorderStroke(1.dp, OutlineBorder)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (uiState.selectedIngredient != null) {
                    // Display selected ingredient
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Thumbnail
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🥩", fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.selectedIngredient.name,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "ING-${uiState.selectedIngredient.id} • HPP: Rp ${uiState.selectedIngredient.minimumStock}",
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp,
                                color = BadgeTextOrangeDark
                            )
                        }
                    }
                } else {
                    // No ingredient selected placeholder
                    Text(
                        text = "Belum ada bahan dipilih",
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Horizontal Chips - Dynamic list from DB
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(ingredientsList.size) { index ->
                val ingredient = ingredientsList[index]
                ChipItem(
                    text = ingredient.name,
                    isSelected = uiState.selectedIngredient?.id == ingredient.id,
                    onClick = { viewModel.onIngredientSelected(ingredient) }
                )
            }
        }
    }
}

@Composable
private fun ChipItem(text: String, isSelected: Boolean, onClick: () -> Unit = {}) {
    val bgColor = if (isSelected) PrimaryOrange else ChipDefaultBg
    val textColor = if (isSelected) Color.White else ChipDefaultText
    
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = bgColor,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = textColor
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Section Alasan Pemusnahan
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WasteReasonSection(uiState: WasteFormUiState, viewModel: WasteLoggingViewModel) {
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
                text = "Alasan Pemusnahan",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Text(
                text = if (uiState.selectedReason.isNotBlank()) uiState.selectedReason else "Wajib dipilih",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = if (uiState.selectedReason.isNotBlank()) StatusGreen else TextSecondary
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 2x2 Grid - Dynamic based on ViewModel state
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReasonButton(
                    text = "Hangus Masak", 
                    isSelected = uiState.selectedReason == "Hangus Masak",
                    onClick = { viewModel.onReasonChanged("Hangus Masak") },
                    modifier = Modifier.weight(1f)
                )
                ReasonButton(
                    text = "Basi / Expired", 
                    isSelected = uiState.selectedReason == "Basi / Expired",
                    onClick = { viewModel.onReasonChanged("Basi / Expired") },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReasonButton(
                    text = "Jatuh / Kontaminasi", 
                    isSelected = uiState.selectedReason == "Jatuh / Kontaminasi",
                    onClick = { viewModel.onReasonChanged("Jatuh / Kontaminasi") },
                    modifier = Modifier.weight(1f)
                )
                ReasonButton(
                    text = "Potongan Berlebih", 
                    isSelected = uiState.selectedReason == "Potongan Berlebih",
                    onClick = { viewModel.onReasonChanged("Potongan Berlebih") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ReasonButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bgColor = if (isSelected) PrimaryOrange else ChipDefaultBg
    val textColor = if (isSelected) Color.White else ChipDefaultText
    
    Surface(
        modifier = modifier.height(44.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Card Estimasi Kerugian Finansial
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FinancialLossCard(uiState: WasteFormUiState) {
    val displayWeight = uiState.quantityInput.toDoubleOrNull() ?: 0.0
    val unitPrice = uiState.selectedIngredient?.minimumStock ?: 0.0
    val totalLoss = displayWeight * unitPrice
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = InfoRowBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ESTIMASI KERUGIAN FINANSIAL",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = PinkBleedBg
                ) {
                    Text(
                        text = "STOCK BLEED",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = PinkBleedText
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("Rp  %,.0f", totalLoss),
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = PillBadgeYellowText // Coklat gelap
                )
                
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BadgeBgOrangeLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = BadgeTextOrangeDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = ChipDefaultBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = PillBadgeYellowText,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (displayWeight > 0 && unitPrice > 0) {
                            String.format("%.2f kg × Rp %,d/kg", displayWeight, unitPrice)
                        } else {
                            "Menunggu input kuantitas"
                        },
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = if (displayWeight > 0 && unitPrice > 0) PillBadgeYellowText else TextSecondary.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Tombol Aksi Bawah
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ActionFooterSection(
    uiState: WasteFormUiState, 
    viewModel: WasteLoggingViewModel,
    onNavigateToAiVerification: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onNavigateToAiVerification,
            enabled = !uiState.isSubmitting && uiState.submitSuccess,
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
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Menyimpan...",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                } else if (uiState.submitSuccess) {
                    Text(
                        text = "Simpan Berhasil",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                } else {
                    Text(
                        text = "Verifikasi Mutu via AI Kamera",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Submit Waste Button - PRIMARY ACTION
        Button(
            onClick = {
                viewModel.submitWaste()
            },
            enabled = !uiState.isSubmitting && 
                     uiState.selectedIngredient != null && 
                     uiState.quantityInput.toDoubleOrNull() != null &&
                     uiState.selectedReason.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = StatusSuccess,
                disabledContainerColor = ChipDefaultBg
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mengirim Data...",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Simpan & Sync Waste",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
        
        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = StatusDanger.copy(alpha = 0.1f)
            ) {
                Text(
                    text = uiState.errorMessage,
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = StatusDanger,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = StatusGreen,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "TFLite Freshness Classifier v2.4 • Audit Trail Recorded",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp,
                color = TextSecondary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEW
// ─────────────────────────────────────────────────────────────────────────────
@Preview(
    name = "Waste Logging - Figma M04",
    showBackground = true,
    backgroundColor = 0xFFF8FAFC,
    device = "spec:width=390dp,height=844dp,dpi=420"
)
@SuppressLint("ViewModelConstructorInComposable")
@Composable
private fun WasteLoggingScreenPreview() {
    KitchenGuardTheme {
        val app = androidx.compose.ui.platform.LocalContext.current
            .applicationContext as android.app.Application
        val fakeMasterDataDao = object : com.csm.kitchenguard.data.local.dao.MasterDataDao {
            override suspend fun insertIngredients(
                items: List<com.csm.kitchenguard.data.local.entity.IngredientEntity>
            ) = Unit

            override fun getAllIngredients():
                kotlinx.coroutines.flow.Flow<List<com.csm.kitchenguard.data.local.entity.IngredientEntity>> =
                kotlinx.coroutines.flow.flowOf(emptyList())

            override suspend fun getIngredientById(
                id: Long
            ): com.csm.kitchenguard.data.local.entity.IngredientEntity? = null

            override suspend fun insertBatches(
                batches: List<com.csm.kitchenguard.data.local.entity.BatchEntity>
            ) = Unit

            override fun getBatchesByIngredient(
                ingredientId: Long
            ): kotlinx.coroutines.flow.Flow<List<com.csm.kitchenguard.data.local.entity.BatchEntity>> =
                kotlinx.coroutines.flow.flowOf(emptyList())
        }
        val fakeShiftDao = object : com.csm.kitchenguard.data.local.dao.ShiftDao {
            override suspend fun insertOrUpdateActiveShift(
                shift: com.csm.kitchenguard.data.local.entity.ActiveShiftEntity
            ) = Unit

            override fun getActiveShift() =
                kotlinx.coroutines.flow.flowOf<
                    com.csm.kitchenguard.data.local.entity.ActiveShiftEntity?>(null)

            override suspend fun getActiveShiftOnce() = null

            override suspend fun clearActiveShift() = Unit
        }
        val fakeWasteRepo = object : com.csm.kitchenguard.domain.repository.WasteRepository {
            override suspend fun saveWasteRecord(
                waste: com.csm.kitchenguard.data.local.entity.WasteRecordEntity
            ): Result<Unit> = Result.success(Unit)

            override fun getWasteHistory(
                shiftId: Long
            ): kotlinx.coroutines.flow.Flow<List<com.csm.kitchenguard.data.local.entity.WasteRecordEntity>> =
                kotlinx.coroutines.flow.flowOf(emptyList())

            override fun getTotalShiftWaste(shiftId: Long): kotlinx.coroutines.flow.Flow<Double> =
                kotlinx.coroutines.flow.flowOf(0.0)
        }
        WasteLoggingScreen(
            onNavigateBack = {},
            onNavigateToAiVerification = {},
            viewModel = WasteLoggingViewModel(
                application = app,
                masterDataDao = fakeMasterDataDao,
                shiftDao = fakeShiftDao,
                validateOcrWeightUseCase =
                    com.csm.kitchenguard.domain.usecase.ValidateOcrWeightUseCase(),
                createWasteRecordUseCase =
                    com.csm.kitchenguard.domain.usecase.CreateWasteRecordUseCase(
                        fakeWasteRepo, fakeMasterDataDao
                    )
            )
        )
    }
}
