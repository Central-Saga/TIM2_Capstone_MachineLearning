package com.csm.kitchenguard.presentation.screens.hub

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Inventory
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
import com.csm.kitchenguard.data.local.entity.WasteRecordEntity
import com.csm.kitchenguard.presentation.components.ConflictWarningBanner
import com.csm.kitchenguard.presentation.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// Design Tokens Khusus (Figma M03)
// ─────────────────────────────────────────────────────────────────────────────
private val StatusGreen = Color(0xFF16A34A)
private val BadgeBgOrangeLight = Color(0xFFFFEDD5)
private val BadgeTextOrangeDark = Color(0xFFEA580C)
private val InfoRowBg = Color(0xFFFFFFFF)
private val PillBadgeYellowBg = Color(0xFFFEF3C7)
private val PillBadgeYellowText = Color(0xFF92400E)

private val TagRedBg = Color(0xFFFEE2E2)
private val TagRedText = Color(0xFFB91C1C)
private val TagGrayBg = Color(0xFFF1F5F9)
private val TagGrayText = Color(0xFF475569)
private val TagGreenBg = Color(0xFFDCFCE7)
private val TagGreenText = Color(0xFF15803D)

// ─────────────────────────────────────────────────────────────────────────────
// Layar Utama (Figma M03)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun KitchenHubScreen(
    onNavigateToWaste: () -> Unit,
    onNavigateToAudit: () -> Unit,
    onNavigateToNotification: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: KitchenHubViewModel
) {
    // Observasi state dari ViewModel
    val uiState by viewModel.uiState.collectAsState()
    
    val conflictedWaste by viewModel.conflictedWaste.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // ── 1. Top App Bar & Status (Oranye) ───────────────────────────────
        HubTopBar(
            pendingSyncCount = uiState.pendingSyncCount,
            onNavigateToNotification = onNavigateToNotification,
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToSettings = onNavigateToSettings
        )

        // ── 2. Konten Utama (Scrollable) ──────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Conflict Warning Banner
            item {
                if (conflictedWaste.isNotEmpty()) {
                    ConflictWarningBanner(
                        conflictedWaste = conflictedWaste,
                        onRetrySync = { viewModel.retryConflicts() }
                    )
                }
            }

            // Info Bar: status stasiun & shift dari data nyata (Issue #21).
            // Tidak ada nama persona/shift palsu sebagai fallback.
            item {
                StatusInfoBar(
                    stationName = uiState.stationName.takeIf { it != "-" } ?: "Stasiun belum dipilih",
                    shiftName = uiState.shiftName.takeIf { it != "No Active Shift" } ?: "Tidak ada shift aktif"
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Hero Card: TOTAL WASTE SHIFT INI ─────────────────────────
            item {
                TotalWasteCard(totalWeight = uiState.totalWasteWeight)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Grid: Distribusi Kategori ─────────────────────────────────
            item {
                CategoryDistributionGrid()
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── List: Aktivitas Terbaru ───────────────────────────────────
            item {
                RecentActivityHeader()
                Spacer(modifier = Modifier.height(12.dp))
            }

            val recentList = uiState.recentWasteList
            if (recentList.isNotEmpty()) {
                items(recentList.take(3)) { record ->
                    WasteRecordItem(record = record)
                }
            } else {
                // Tidak ada data → tampilkan empty state jujur, bukan mock (Issue #21).
                item { EmptyRecentActivity() }
            }

            // ── Tombol Aksi Bawah ─────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ActionButtons(
                    onNavigateToWaste = onNavigateToWaste,
                    onNavigateToAudit = onNavigateToAudit
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Komponen: Top Bar (Background Oranye)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HubTopBar(
    pendingSyncCount: Int, 
    onNavigateToNotification: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
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
            // Logo & Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Mock Logo
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
                        text = "Dapur",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }

            // Kanan: Sync Badge + Notif
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pill Sync
                val isSynced = pendingSyncCount == 0
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
                                .background(if (isSynced) Color(0xFF6EE7B7) else Color(0xFFFCD34D)) // Hijau terang/Kuning
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSynced) "SYNCED" else "SYNCING",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Icon Lonceng + Titik Merah
                Box(modifier = Modifier.clickable { onNavigateToNotification() }) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifikasi",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    // Titik Merah
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                            .border(1.dp, PrimaryOrange, CircleShape)
                            .align(Alignment.TopEnd)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Settings Gear Icon
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Pengaturan",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onNavigateToSettings() }
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Avatar Profile
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { onNavigateToProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧑", fontSize = 18.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Komponen: Status Info Bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StatusInfoBar(stationName: String, shiftName: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(12.dp),
        color = InfoRowBg,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Titik Hijau ONLINE
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StatusGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ONLINE",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = StatusGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$shiftName • 11 Sep 2026",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            
            // Badge Station
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PillBadgeYellowBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🍳", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stationName,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = PillBadgeYellowText
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Komponen: Hero Card Total Waste
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TotalWasteCard(totalWeight: Double) {
    // Tampilkan nilai nyata dari DB. Tidak ada mock default (Issue #21).
    val displayWeight = totalWeight

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Aksen garis vertikal oranye di kiri
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(180.dp) // Estimasi tinggi
                    .background(PrimaryOrange)
            )
            
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL WASTE SHIFT INI",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = TagGrayBg
                    ) {
                        Text(
                            text = "Target < 1.5kg",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = displayWeight.toString(),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "kg",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 20.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Nominal kerugian finansial dihitung otoritatif oleh backend
                // (PRD Section 13) dan belum tersedia di cache client.
                // Tampilkan netral, bukan angka karangan (Issue #21).
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BadgeBgOrangeLight
                ) {
                    Text(
                        text = "Est. Kerugian: —",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BadgeTextOrangeDark
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Peringatan
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = BadgeBgOrangeLight
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = BadgeTextOrangeDark,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Perlu Review Head Chef",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = BadgeTextOrangeDark
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+0.6kg vs rerata shift ↗",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = BadgeTextOrangeDark
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Komponen: Grid Distribusi Kategori
// ─────────────────────────────────────────────────────────────────────────────
/**
 * Distribusi waste per kategori beserta nilai finansial dihitung otoritatif
 * oleh backend (PRD Section 13) dan belum tersedia di cache offline client.
 *
 * Sebelumnya layar ini menampilkan angka kategori palsu (Meat 1.4 / Rp 210.000,
 * dst). Kini ditampilkan status jujur "belum tersedia" (Issue #21).
 */
@Composable
private fun CategoryDistributionGrid() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Distribusi Kategori",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Inventory,
                    contentDescription = null,
                    tint = TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Distribusi kategori belum tersedia",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Rekap per kategori dihitung di server saat sinkronisasi.",
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    color = TextSecondary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Komponen: Header Aktivitas Terbaru
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RecentActivityHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Aktivitas Terbaru",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimary
        )
        Text(
            text = "Lihat Semua (8) >",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = BadgeTextOrangeDark
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Komponen: Empty state Aktivitas Terbaru
// ─────────────────────────────────────────────────────────────────────────────
/**
 * Ditampilkan bila belum ada aktivitas waste nyata untuk shift ini.
 * Menggantikan mock persona hardcoded (Issue #21).
 */
@Composable
private fun EmptyRecentActivity() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Inventory,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Belum ada aktivitas waste",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Catatan baru akan tampil di sini setelah disimpan.",
                fontFamily = InterFontFamily,
                fontSize = 11.sp,
                color = TextSecondary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WasteRecordCard(
    itemName: String,
    tagText: String,
    tagBg: Color,
    tagColor: Color,
    locationTime: String,
    weight: String,
    loss: String,
    icon: String,
    iconBg: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row {
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = icon, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = itemName,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = locationTime,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                
                // Tag
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = tagBg
                ) {
                    Text(
                        text = tagText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = tagColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = OutlineBorder)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory, // Placeholder untuk icon scale
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = weight,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }
                
                Row {
                    Text(
                        text = "Loss: ",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = loss,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = BadgeTextOrangeDark
                    )
                }
            }
        }
    }
}

// Jika ada data asli dari viewmodel
@Composable
private fun WasteRecordItem(record: WasteRecordEntity) {
    WasteRecordCard(
        itemName = record.ingredientName,
        tagText = record.reason,
        tagBg = TagGrayBg,
        tagColor = TagGrayText,
        locationTime = "ID: ${record.clientUuid.take(8)} • ${record.clientEventAt}",
        weight = "${record.quantity} ${record.unit}",
        loss = "Rp -",
        icon = "📝",
        iconBg = BackgroundLight
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Komponen: Action Buttons
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ActionButtons(
    onNavigateToWaste: () -> Unit,
    onNavigateToAudit: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        // Tombol Primary (Catat Waste Baru)
        Button(
            onClick = onNavigateToWaste,
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
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Catat Waste Baru",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                // Pill < 5 DETIK
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color.White.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = "< 5 DETIK",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Tombol Secondary (Audit Fisik)
        OutlinedButton(
            onClick = onNavigateToAudit,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = BadgeTextOrangeDark
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = BadgeTextOrangeDark.copy(alpha = 0.3f)
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle, // Placeholder clipboard icon
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Audit Fisik Tutup Shift",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEW
// ─────────────────────────────────────────────────────────────────────────────
@Preview(
    name = "Hub Screen - Figma M03",
    showBackground = true,
    backgroundColor = 0xFFF8FAFC,
    device = "spec:width=390dp,height=844dp,dpi=420"
)
@SuppressLint("ViewModelConstructorInComposable")
@Composable
private fun KitchenStationHubScreenPreview() {
    KitchenGuardTheme {
        val app = androidx.compose.ui.platform.LocalContext.current
            .applicationContext as android.app.Application
        val fakeWasteDao = object : com.csm.kitchenguard.data.local.dao.WasteDao {
            override suspend fun insertWaste(
                waste: com.csm.kitchenguard.data.local.entity.WasteRecordEntity
            ) = Unit

            override fun getWasteByShift(
                shiftId: Long
            ): kotlinx.coroutines.flow.Flow<List<com.csm.kitchenguard.data.local.entity.WasteRecordEntity>> =
                kotlinx.coroutines.flow.flowOf(emptyList())

            override fun getPendingWaste():
                kotlinx.coroutines.flow.Flow<List<com.csm.kitchenguard.data.local.entity.WasteRecordEntity>> =
                kotlinx.coroutines.flow.flowOf(emptyList())

            override suspend fun updateSyncStatus(clientUuid: String, status: String) = Unit

            override fun getConflictedWaste():
                kotlinx.coroutines.flow.Flow<List<com.csm.kitchenguard.data.local.entity.WasteRecordEntity>> =
                kotlinx.coroutines.flow.flowOf(emptyList())

            override suspend fun resetConflictToPending(): Int = 0

            override fun getTotalWasteQuantity(shiftId: Long): kotlinx.coroutines.flow.Flow<Double?> =
                kotlinx.coroutines.flow.flowOf(0.0)
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
        val fakeSyncQueueDao = object : com.csm.kitchenguard.data.local.dao.SyncQueueDao {
            override suspend fun enqueue(
                item: com.csm.kitchenguard.data.local.entity.SyncQueueEntity
            ): Long = 0L

            override fun getPendingQueue() =
                kotlinx.coroutines.flow.flowOf(
                    emptyList<com.csm.kitchenguard.data.local.entity.SyncQueueEntity>()
                )

            override suspend fun getNextPendingBatch(limit: Int) =
                emptyList<com.csm.kitchenguard.data.local.entity.SyncQueueEntity>()

            override suspend fun updateStatus(
                id: Long,
                status: String,
                error: String?,
                retryCount: Int
            ) = Unit

            override suspend fun deleteSynced(id: Long) = Unit

            override suspend fun getByClientUuid(
                uuid: String
            ): com.csm.kitchenguard.data.local.entity.SyncQueueEntity? = null

            override suspend fun resetConflictToPending(): Int = 0
        }
        val fakeSyncRepository = object : com.csm.kitchenguard.domain.repository.SyncRepository {
            override suspend fun enqueueSyncItem(
                clientUuid: String,
                idempotencyKey: String,
                entityType: String,
                operation: String,
                payloadJson: String,
                clientEventAt: String?
            ): Result<Long> = Result.success(0L)

            override suspend fun pushBatchSync():
                Result<com.csm.kitchenguard.domain.repository.SyncSummary> =
                Result.success(
                    com.csm.kitchenguard.domain.repository.SyncSummary(0, 0, 0, 0)
                )

            override suspend fun pullMasterData(): Result<Unit> = Result.success(Unit)

            override suspend fun retryConflictedItems(): Result<Int> = Result.success(0)

            override fun getNotifications():
                kotlinx.coroutines.flow.Flow<
                    List<com.csm.kitchenguard.data.local.entity.NotificationEntity>> =
                kotlinx.coroutines.flow.flowOf(emptyList())

            override fun getUnreadNotificationCount(): kotlinx.coroutines.flow.Flow<Int> =
                kotlinx.coroutines.flow.flowOf(0)

            override suspend fun markNotificationAsRead(id: Long) = Unit
        }
        KitchenHubScreen(
            onNavigateToWaste = {},
            onNavigateToAudit = {},
            viewModel = KitchenHubViewModel(
                application = app,
                getStationSummaryUseCase = com.csm.kitchenguard.domain.usecase.GetStationSummaryUseCase(
                    fakeShiftDao, fakeWasteRepo, fakeSyncQueueDao
                ),
                wasteRepository = fakeWasteRepo,
                wasteDao = fakeWasteDao,
                syncRepository = fakeSyncRepository
            )
        )
    }
}
