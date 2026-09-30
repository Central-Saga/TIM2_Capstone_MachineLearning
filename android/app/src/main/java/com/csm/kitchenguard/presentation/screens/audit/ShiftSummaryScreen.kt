package com.csm.kitchenguard.presentation.screens.audit

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csm.kitchenguard.presentation.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// Design Tokens (Figma M07)
// ─────────────────────────────────────────────────────────────────────────────
private val StatusGreen = Color(0xFF16A34A)
private val LightGreenBg = Color(0xFFDCFCE7)
private val StatusRedBg = Color(0xFFDC2626)
private val LightRedBg = Color(0xFFFEE2E2)
private val LightRedText = Color(0xFFB91C1C)
private val BadgeBgOrangeLight = Color(0xFFFFEDD5)
private val BadgeTextOrangeDark = Color(0xFFEA580C)
private val InfoRowBg = Color(0xFFFFFFFF)
private val ChipDefaultBg = Color(0xFFF1F5F9)
private val OutlineColor = Color(0xFFE2E8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftSummaryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // ── 1. Top App Bar ────────────────────────────────────────────────────
        TopBarM07()

        // Konten utama scrollable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // ── 2. Header Rekapitulasi ────────────────────────────────────────
            SummaryHeaderCard()

            // ── 3. Grid Ringkasan 2x2 ─────────────────────────────────────────
            SummaryGridSection()

            // ── 4. Rincian Posisi Bahan Kritis ────────────────────────────────
            CriticalIngredientsSection()

            // ── 5. Card QR Code Approval ──────────────────────────────────────
            QrApprovalCard()

            // ── 6. Checklist Prosedur Sanitasi ────────────────────────────────
            SanitationChecklistSection()

            // ── 7. Tombol Aksi Bawah ──────────────────────────────────────────
            ActionFooterM07()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Top Bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TopBarM07() {
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
                        .background(Color.White),
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
                        text = "Rekap Shift",
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
                
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Header Rekapitulasi
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SummaryHeaderCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = InfoRowBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Hot Kitchen Label & Close Btn
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HOT KITCHEN & MEAT STATION",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BadgeBgOrangeLight)
                        .clickable { /* close */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Judul
            Text(
                text = "Rekapitulasi Shift\nPagi — 11 Sep 2026",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextPrimary,
                lineHeight = 28.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Supervisor Card
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ChipDefaultBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Image (Placeholder)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🧑", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Wayan Sudarma (CDP Station Lead)",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Shift: 06:00 - 15:00 WITA • Kitchen Bali 01",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Status Badges
            Row {
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = BadgeBgOrangeLight
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(BadgeTextOrangeDark)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Menunggu Persetujuan Head Chef",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = BadgeTextOrangeDark
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = ChipDefaultBg
                ) {
                    Text(
                        text = "Audit ID #8842",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontFamily = InterFontFamily,
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
// 3. Grid Ringkasan 2x2
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SummaryGridSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Item Diaudit
            SummaryCardItem(
                modifier = Modifier.weight(1f),
                title = "Item Diaudit",
                icon = { /* None specific icon in design, maybe list/check */ },
                content = {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("8", fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = TextPrimary)
                        Text(" / 8 SKU", fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = StatusGreen, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Progress Bar Hijau
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(StatusGreen))
                }
            )
            
            // Anomali
            SummaryCardItem(
                modifier = Modifier.weight(1f),
                title = "Anomali",
                icon = {
                    Icon(imageVector = Icons.Default.WarningAmber, contentDescription = null, tint = StatusRedBg, modifier = Modifier.size(16.dp))
                },
                content = {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("1", fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = StatusRedBg)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tenderloin", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = StatusRedBg, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(shape = RoundedCornerShape(4.dp), color = LightRedBg) {
                        Text("Selisih 420g", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = LightRedText)
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total Waste
            SummaryCardItem(
                modifier = Modifier.weight(1f),
                title = "Total Waste",
                icon = {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                },
                content = {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("2.1", fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("kg", fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("-18% vs shift kemarin", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 9.sp, color = StatusGreen)
                    }
                }
            )
            
            // Nilai Waste
            SummaryCardItem(
                modifier = Modifier.weight(1f),
                title = "Nilai Waste (HPP)",
                icon = {
                    Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = BadgeTextOrangeDark, modifier = Modifier.size(16.dp))
                },
                content = {
                    Text("Rp 270.000", fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF92400E))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Batas toleransi: Rp 450k", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 9.sp, color = TextSecondary)
                }
            )
        }
    }
}

@Composable
private fun SummaryCardItem(
    modifier: Modifier = Modifier,
    title: String,
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
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
                    text = title,
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                icon()
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Rincian Posisi Bahan Kritis
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CriticalIngredientsSection() {
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
                text = "Rincian Posisi Bahan Kritis",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary
            )
            Text(
                text = "Periksa Semua",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = Color(0xFF92400E),
                modifier = Modifier.clickable { /* action */ }
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Item 1
        CriticalItemRow(
            title = "Wagyu Beef Ribeye MB7+",
            desc = "Prep: 6.4 kg • Sisa fisik: 4.1 kg",
            statusText = "Presisi (0%)",
            statusColor = StatusGreen,
            icon = { Icon(Icons.Default.CheckCircle, null, tint = StatusGreen, modifier = Modifier.size(16.dp)) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Item 2
        CriticalItemRow(
            title = "Australian Beef Tenderloin",
            desc = "Deviasi: 420g tidak tercatat kasir",
            statusText = "-420g",
            statusColor = StatusRedBg,
            isRedAlert = true,
            icon = { Icon(Icons.Default.ErrorOutline, null, tint = StatusRedBg, modifier = Modifier.size(16.dp)) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Item 3
        CriticalItemRow(
            title = "Organic Duck Breast (Kintamani)",
            desc = "Trimming skin: 650g teralokasi Jus",
            statusText = "Ok",
            statusColor = TextSecondary,
            icon = { Icon(Icons.Default.CheckCircle, null, tint = StatusGreen, modifier = Modifier.size(16.dp)) }
        )
    }
}

@Composable
private fun CriticalItemRow(
    title: String,
    desc: String,
    statusText: String,
    statusColor: Color,
    isRedAlert: Boolean = false,
    icon: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isRedAlert) LightRedBg.copy(alpha = 0.5f) else ChipDefaultBg,
        border = if (isRedAlert) BorderStroke(1.dp, LightRedBg) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isRedAlert) StatusRedBg else TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    fontFamily = InterFontFamily,
                    fontSize = 10.sp,
                    color = if (isRedAlert) StatusRedBg else TextSecondary
                )
            }
            Text(
                text = statusText,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = statusColor
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Card QR Code Approval
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun QrApprovalCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = InfoRowBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BadgeBgOrangeLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "QR Persetujuan Audit Fisik",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tunjukkan QR code ini kepada Head Chef untuk validasi fisik dan otorisasi resmi tutup shift kerja.",
                fontFamily = InterFontFamily,
                fontSize = 11.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Mock QR Code Image
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ChipDefaultBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.size(140.dp)
                )
                // Middle check
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(PrimaryOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = StatusGreen,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Rotasi refresh otomatis 45s",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = OutlineColor.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KG-CSM-2026-0911-A01",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = Color(0xFF92400E),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Checklist Prosedur Sanitasi
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SanitationChecklistSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = "Prosedur Sanitasi & Serah Terima",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        ChecklistItem(text = "Pembersihan sanitasi grease trap & cutting board tuntas", isChecked = true)
        Spacer(modifier = Modifier.height(8.dp))
        ChecklistItem(text = "Chiller Walk-in terkunci pada suhu target 2.4°C", isChecked = true)
        Spacer(modifier = Modifier.height(8.dp))
        ChecklistItem(text = "Log book manual disinkronkan dengan Head Chef malam", isChecked = false)
    }
}

@Composable
private fun ChecklistItem(text: String, isChecked: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = InfoRowBg,
        border = BorderStroke(1.dp, OutlineColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isChecked) PrimaryOrange else Color.White)
                    .border(if (isChecked) 0.dp else 1.dp, OutlineColor, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isChecked) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                fontFamily = InterFontFamily,
                fontSize = 11.sp,
                color = TextPrimary,
                lineHeight = 16.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. Tombol Aksi Bawah
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ActionFooterM07() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tombol Oranye: Download PDF
        Button(
            onClick = { /* Download PDF */ },
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
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Download Ringkasan PDF",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Tombol Putih Outlined: Kembali
        OutlinedButton(
            onClick = { /* Navigasi kembali */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = TextPrimary
            ),
            border = BorderStroke(1.dp, OutlineColor)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = PrimaryOrange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kembali ke Dapur",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEW
// ─────────────────────────────────────────────────────────────────────────────
@Preview(
    name = "Shift Summary - Figma M07",
    showBackground = true,
    backgroundColor = 0xFFF8FAFC,
    device = "spec:width=390dp,height=844dp,dpi=420"
)
@Composable
private fun ShiftSummaryScreenPreview() {
    KitchenGuardTheme {
        ShiftSummaryScreen()
    }
}
