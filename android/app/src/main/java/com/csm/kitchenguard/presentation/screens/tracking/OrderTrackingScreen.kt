package com.csm.kitchenguard.presentation.screens.tracking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csm.kitchenguard.presentation.theme.*

/**
 * Alur Operasional & Manajemen Shift Dapur (Interactive Kitchen Shift Workspace).
 * Memungkinkan karyawan dapur mengontrol checklist SOP, sensor suhu chiller,
 * pemantauan waste aktif, progres audit stok, hingga handover catatan tutup shift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    shiftId: String = "#SHIFT-2026-0914",
    onNavigateBack: () -> Unit,
    onNavigateToWaste: () -> Unit = {},
    onNavigateToAudit: () -> Unit = {},
    onNavigateToSummary: () -> Unit = {},
    onNavigateToSlots: () -> Unit = {}
) {
    // State Shift
    var activeShiftType by remember { mutableStateOf("Shift Pagi (07:00 - 15:00)") }
    val shiftTypes = listOf("Shift Pagi (07:00 - 15:00)", "Shift Siang (15:00 - 23:00)")
    var isShiftRunning by remember { mutableStateOf(true) }

    // State Expansi Kartu Tahapan
    var expandedStep by remember { mutableStateOf<Int?>(3) } // Step 3 terbuka secara default (Sedang Berjalan)

    // State Tahap 1: Checklist Stok Awal
    var itemAyamChecked by remember { mutableStateOf(true) }
    var itemSapiChecked by remember { mutableStateOf(true) }
    var itemMinyakChecked by remember { mutableStateOf(true) }
    var itemTelurChecked by remember { mutableStateOf(false) }

    // State Tahap 2: Suhu Chiller Aktual
    var chillerTemp by remember { mutableStateOf("2.8") }
    var tempStatus by remember { mutableStateOf("STABIL") }

    // State Tahap 5: Catatan Handover
    var handoverNote by remember { mutableStateOf("Chiller 2 es agak tebal di rak bawah. Defrost dori dijadwalkan jam 18:00 WIB.") }
    var isNoteSaved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Alur Operasional Shift",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Hot Kitchen & Chiller Utama",
                            fontFamily = InterFontFamily,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Tombol cepat langsung ke Rekapitulasi Shift
                    IconButton(onClick = onNavigateToSummary) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                            contentDescription = "Rekap Shift",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardGoldenYellow)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // ─────────────────────────────────────────────────────────────────
            // 1. Hero Golden Header: Controller Status Shift & Timer
            // ─────────────────────────────────────────────────────────────────
            Surface(
                color = CardGoldenYellow,
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Status Badge & Selector Shift
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge Aktif
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isShiftRunning) Color(0xFF22C55E) else Color(0xFFE2E8F0))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isShiftRunning) "SHIFT AKTIF" else "SHIFT DIJEDA",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Text(
                            text = shiftId,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Timer Display
                    Text(
                        text = if (isShiftRunning) "04:30:15" else "04:30:00",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        color = Color.White
                    )
                    Text(
                        text = activeShiftType,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.95f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3 KPI Cards Real-Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ShiftKpiCard(
                            title = "Waste Shift",
                            value = "0.85 kg",
                            subtext = "3 Log Ditimbang",
                            icon = Icons.Outlined.Scale,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToWaste
                        )
                        ShiftKpiCard(
                            title = "Est. Biaya HPP",
                            value = "Rp 42.500",
                            subtext = "Toleransi Aman",
                            icon = Icons.Outlined.Payments,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToSummary
                        )
                        ShiftKpiCard(
                            title = "Suhu Chiller",
                            value = "$chillerTemp°C",
                            subtext = tempStatus,
                            icon = Icons.Outlined.Thermostat,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToSlots
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ─────────────────────────────────────────────────────────────────
            // 2. Section Title & Filter Operasional
            // ─────────────────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kelola 5 Tahapan Shift",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Ketuk untuk aksi",
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ─────────────────────────────────────────────────────────────
                // TAHAP 1: Buka Shift & Cek Stok Awal Masuk
                // ─────────────────────────────────────────────────────────────
                val isCompleted1 = itemAyamChecked && itemSapiChecked && itemMinyakChecked && itemTelurChecked
                InteractiveStepCard(
                    stepNumber = 1,
                    title = "Buka Shift & Cek Stok Awal",
                    statusLabel = if (isCompleted1) "SELESAI" else "3/4 DIPERIKSA",
                    statusColor = if (isCompleted1) Color(0xFF22C55E) else CardGoldenYellow,
                    isCurrent = false,
                    isExpanded = expandedStep == 1,
                    onToggleExpand = { expandedStep = if (expandedStep == 1) null else 1 }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Checklist verifikasi bahan kritis yang masuk ke stasiun pagi ini:",
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        ChecklistRow(
                            label = "Ayam Fillet (4.2 kg - Batch #AYM-04)",
                            checked = itemAyamChecked,
                            onCheckedChange = { itemAyamChecked = it }
                        )
                        ChecklistRow(
                            label = "Daging Sapi Sirloin (6.5 kg - Batch #BEEF-01)",
                            checked = itemSapiChecked,
                            onCheckedChange = { itemSapiChecked = it }
                        )
                        ChecklistRow(
                            label = "Minyak Goreng Sawit (15.0 L - Fresh)",
                            checked = itemMinyakChecked,
                            onCheckedChange = { itemMinyakChecked = it }
                        )
                        ChecklistRow(
                            label = "Telur Ayam Negeri (12.0 kg - Tray C)",
                            checked = itemTelurChecked,
                            onCheckedChange = { itemTelurChecked = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onNavigateToAudit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StrongOrange),
                            border = BorderStroke(1.dp, CardGoldenYellow)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Buka Audit Fisik Stok Awal",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ─────────────────────────────────────────────────────────────
                // TAHAP 2: Inspeksi Suhu & Kondisi Penyimpanan
                // ─────────────────────────────────────────────────────────────
                InteractiveStepCard(
                    stepNumber = 2,
                    title = "Inspeksi Suhu & Kondisi Chiller",
                    statusLabel = "SUHU $chillerTemp°C ($tempStatus)",
                    statusColor = Color(0xFF22C55E),
                    isCurrent = false,
                    isExpanded = expandedStep == 2,
                    onToggleExpand = { expandedStep = if (expandedStep == 2) null else 2 }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Standar keamanan suhu chiller: 2.0°C - 4.0°C. Catat suhu inspeksi berkala:",
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Log Suhu Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("2.2", "2.8", "3.5", "4.2").forEach { tempOption ->
                                val isSelected = chillerTemp == tempOption
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            chillerTemp = tempOption
                                            tempStatus = if (tempOption.toDouble() > 4.0) "PERINGATAN TINGGI" else "STABIL"
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) CardGoldenYellow else Color(0xFFF1F5F9),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) StrongOrange else Color(0xFFE2E8F0)
                                    )
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "$tempOption°C",
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) Color.White else TextPrimary
                                        )
                                        Text(
                                            text = if (tempOption == "4.2") "Warning" else "Normal",
                                            fontFamily = InterFontFamily,
                                            fontSize = 9.sp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.9f) else TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onNavigateToSlots,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StrongOrange),
                            border = BorderStroke(1.dp, CardGoldenYellow)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.GridView,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Lihat Kondisi 22 Slot Rak & Chiller",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ─────────────────────────────────────────────────────────────
                // TAHAP 3: Pencatatan Waste & Timbang Berkala (AKTIF)
                // ─────────────────────────────────────────────────────────────
                InteractiveStepCard(
                    stepNumber = 3,
                    title = "Pencatatan Waste & Timbang Berkala",
                    statusLabel = "SEDANG BERJALAN",
                    statusColor = StrongOrange,
                    isCurrent = true,
                    isExpanded = expandedStep == 3,
                    onToggleExpand = { expandedStep = if (expandedStep == 3) null else 3 }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "3 Catatan waste telah ditimbang dan terverifikasi AI pada shift ini:",
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Mini Logs
                        RecentWasteRow("Ayam Fillet (Trim Lemak)", "0.35 kg", "10:15 WIB", "Rp 17.500")
                        Spacer(modifier = Modifier.height(6.dp))
                        RecentWasteRow("Bawang Merah (Kulit)", "0.20 kg", "11:40 WIB", "Rp 8.000")
                        Spacer(modifier = Modifier.height(6.dp))
                        RecentWasteRow("Ikan Dori (Tulang & Sisik)", "0.30 kg", "12:20 WIB", "Rp 17.000")

                        Spacer(modifier = Modifier.height(14.dp))

                        // Tombol Aksi Utama: Tambah Timbang Waste Baru
                        Button(
                            onClick = onNavigateToWaste,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CardGoldenYellow)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.PhotoCamera,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "+ Timbang Sisa Bahan (OCR & AI)",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ─────────────────────────────────────────────────────────────
                // TAHAP 4: Audit Fisik Stok Tutup Shift
                // ─────────────────────────────────────────────────────────────
                InteractiveStepCard(
                    stepNumber = 4,
                    title = "Audit Fisik Stok Tutup Shift",
                    statusLabel = "MENUNGGU JAM 14:30",
                    statusColor = Color(0xFF64748B),
                    isCurrent = false,
                    isExpanded = expandedStep == 4,
                    onToggleExpand = { expandedStep = if (expandedStep == 4) null else 4 }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Lakukan audit sisa fisik bahan baku sebelum menyerahkan shift kepada tim berikutnya:",
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Target Audit Fisik",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "0 / 12 Kompartemen bahan selesai diaudit",
                                    fontFamily = InterFontFamily,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Button(
                                onClick = onNavigateToAudit,
                                modifier = Modifier.height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StrongOrange),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = "Mulai Audit",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ─────────────────────────────────────────────────────────────
                // TAHAP 5: Tutup Shift & Handover Catatan
                // ─────────────────────────────────────────────────────────────
                InteractiveStepCard(
                    stepNumber = 5,
                    title = "Tutup Shift & Handover Catatan",
                    statusLabel = if (isNoteSaved) "CATATAN TERSIMPAN" else "SIAPKAN HANDOVER",
                    statusColor = if (isNoteSaved) Color(0xFF22C55E) else Color(0xFF64748B),
                    isCurrent = false,
                    isExpanded = expandedStep == 5,
                    onToggleExpand = { expandedStep = if (expandedStep == 5) null else 5 }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Tuliskan catatan kondisi bahan dapur untuk tim shift berikutnya:",
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = handoverNote,
                            onValueChange = {
                                handoverNote = it
                                isNoteSaved = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Contoh: Daging sapi sisa 1.5 kg, defrost ayam jam 18:00...") },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CardGoldenYellow,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isNoteSaved = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isNoteSaved) Color(0xFF22C55E) else TextPrimary
                                ),
                                border = BorderStroke(1.dp, if (isNoteSaved) Color(0xFF22C55E) else Color(0xFFCBD5E1))
                            ) {
                                Icon(
                                    imageVector = if (isNoteSaved) Icons.Default.Check else Icons.Outlined.EditNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isNoteSaved) "Tersimpan" else "Simpan Catatan",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = onNavigateToSummary,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StrongOrange)
                            ) {
                                Text(
                                    text = "Rekap & Tutup Shift",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Komponen Pendukung Alur Shift Interaktif
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ShiftKpiCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = StrongOrange,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextPrimary
            )
            Text(
                text = subtext,
                fontFamily = InterFontFamily,
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun InteractiveStepCard(
    stepNumber: Int,
    title: String,
    statusLabel: String,
    statusColor: Color,
    isCurrent: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isCurrent) 6.dp else 2.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = if (isCurrent) CardGoldenYellow else Color.Black.copy(alpha = 0.05f)
            ),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(
            width = if (isCurrent) 1.5.dp else 1.dp,
            color = if (isCurrent) CardGoldenYellow else Color(0xFFE2E8F0)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Baris Tahapan
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleExpand
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lingkaran Angka Step
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCurrent) CardGoldenYellow
                            else if (statusColor == Color(0xFF22C55E)) Color(0xFF22C55E)
                            else Color(0xFFE2E8F0)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (statusColor == Color(0xFF22C55E) && !isCurrent) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = "$stepNumber",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isCurrent) Color.White else TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = statusLabel,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp,
                        color = statusColor
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Tutup" else "Buka",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Konten Detail yang Dapat Dikelola (Expandable)
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = CardGoldenYellow)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = if (checked) TextPrimary else TextSecondary
        )
    }
}

@Composable
private fun RecentWasteRow(
    itemName: String,
    weight: String,
    time: String,
    cost: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFFBEB))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = itemName,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                color = TextPrimary
            )
            Text(
                text = "$time • $cost",
                fontFamily = InterFontFamily,
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
        Text(
            text = weight,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = StrongOrange
        )
    }
}
