package com.csm.kitchenguard.presentation.screens.hub

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csm.kitchenguard.R
import com.csm.kitchenguard.data.local.entity.WasteRecordEntity
import com.csm.kitchenguard.presentation.components.ConflictWarningBanner
import com.csm.kitchenguard.presentation.screens.profile.ProfileViewModel
import com.csm.kitchenguard.presentation.theme.*

/**
 * Kitchen Station Hub — Dashboard Pemantauan Kondisi Bahan Dapur & Kontrol Waste
 * Dikhususkan untuk Karyawan Dapur (Kitchen Staff, Barista, Inventory Checker).
 */
@Composable
fun KitchenHubScreen(
    viewModel: KitchenHubViewModel,
    profileViewModel: ProfileViewModel? = null,
    onNavigateToWaste: () -> Unit,
    onNavigateToAudit: () -> Unit,
    onNavigateToNotification: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToReservation: () -> Unit = {},
    onNavigateToTracking: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val conflictedWaste by viewModel.conflictedWaste.collectAsState()
    val profileUiState by (profileViewModel?.uiState?.collectAsState() ?: remember { mutableStateOf(null) })

    var selectedCategory by remember { mutableStateOf("Bahan Segar") }
    val categories = listOf("Semua", "Bahan Segar", "Sayuran", "Daging & Unggas", "Bumbu Dapur", "Sisa Masak")
    var showOperationalAlert by remember { mutableStateOf(true) }

    val chefName = profileUiState?.name?.ifBlank { "Alexander" } ?: "Alexander"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // ── 1. Top Header: Sapaan Karyawan & Status Sistem ─────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Halo $chefName,",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Pantau kondisi bahan & kendalikan waste dapur",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    // Kanan: Sync Status + Profile Avatar (Clickable)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isSynced = uiState.pendingSyncCount == 0
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = if (isSynced) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSynced) Color(0xFF86EFAC) else Color(0xFFFCD34D)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSynced) StatusSuccess else CardGoldenYellow)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSynced) "SYNCED" else "${uiState.pendingSyncCount}",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (isSynced) StatusSuccess else StrongOrange
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Avatar Profil Karyawan
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, CardGoldenYellow, CircleShape)
                                .clickable { onNavigateToProfile() },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_kitchenguard_logo),
                                contentDescription = "Avatar Profile",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            // ── Conflict Warning Banner (Jika ada konflik sync offline) ──────────────
            item {
                if (conflictedWaste.isNotEmpty()) {
                    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)) {
                        ConflictWarningBanner(
                            conflictedWaste = conflictedWaste,
                            onRetrySync = { viewModel.retryConflicts() }
                        )
                    }
                }
            }

            // ── 2. Station & Storage Pill (Menuju Monitoring Slot Rak & Chiller) ───
            item {
                Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToReservation() },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineBorder),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(CardGoldenLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationOn,
                                        contentDescription = "Station",
                                        tint = CardGoldenYellow,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Stasiun Kerja & Kompartemen Simpan",
                                        fontFamily = InterFontFamily,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "Chiller Utama & Dry Storage • Hot Kitchen",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Pilih Slot Rak",
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // ── 3. Category Filter Chips (Kategori Bahan Dapur) ────────────────────
            item {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = category == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (isSelected) CardGoldenYellow else Color.White)
                                .border(
                                    width = if (isSelected) 0.dp else 1.dp,
                                    color = OutlineBorder,
                                    shape = RoundedCornerShape(50.dp)
                                )
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 18.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category,
                                fontFamily = InterFontFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        }
                    }
                }
            }

            // ── 4. Section: Bahan Prioritas / Kritis (2 Kartu Oranye/Kuning) ──────
            item {
                Spacer(modifier = Modifier.height(22.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bahan Prioritas & Kritis",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Lihat Semua",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = CardGoldenYellow,
                        modifier = Modifier.clickable { onNavigateToReservation() }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Card 1: Daging Ayam Fillet
                    IngredientPriorityCard(
                        modifier = Modifier.weight(1f),
                        rating = "★ 95% Segar",
                        stock = "Sisa 4.2 kg",
                        title = "Daging Ayam Fillet",
                        description = "Batch #AYM-2026 • Expired 2 hari lagi • Suhu Chiller 2.8°C",
                        dishType = "ayam",
                        onClick = { onNavigateToDetail("Daging Ayam Fillet") }
                    )

                    // Card 2: Bawang Merah Kupas
                    IngredientPriorityCard(
                        modifier = Modifier.weight(1f),
                        rating = "★ 88% Segar",
                        stock = "Sisa 2.8 kg",
                        title = "Bawang Merah Kupas",
                        description = "Batch #BWG-2026 • Perlu cek pembusukan di Dry Storage",
                        dishType = "babat",
                        onClick = { onNavigateToDetail("Bawang Merah Kupas") }
                    )
                }
            }

            // ── 5. Total Waste Shift & Metrik KitchenGuard Terpadu ────────────────
            item {
                Spacer(modifier = Modifier.height(20.dp))
                TotalWasteCardIntegrated(
                    totalWeight = uiState.totalWasteWeight,
                    onNavigateToWaste = onNavigateToWaste,
                    onNavigateToTracking = onNavigateToTracking
                )
            }

            // ── 6. Section: Katalog Stok Bahan Dapur (List Vertikal di Foto) ───────
            item {
                Spacer(modifier = Modifier.height(22.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Katalog Stok Bahan Baku",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Lihat Semua",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = CardGoldenYellow,
                        modifier = Modifier.clickable { onNavigateToReservation() }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                IngredientListItem(
                    name = "Daging Sapi Sirloin",
                    description = "Chiller Daging R-02 • Kondisi optimal bebas bau",
                    stock = "Sisa 6.5 kg",
                    quality = "98% Prima",
                    dishType = "campur",
                    onClick = { onNavigateToDetail("Daging Sapi Sirloin") }
                )
            }

            item {
                IngredientListItem(
                    name = "Ikan Dori Fillet",
                    description = "Freezer R-03 • Dekati batas waktu thawing defrost",
                    stock = "Sisa 2.1 kg",
                    quality = "Near Exp",
                    dishType = "goreng",
                    onClick = { onNavigateToDetail("Ikan Dori Fillet") }
                )
            }

            item {
                IngredientListItem(
                    name = "Telur Ayam Negeri",
                    description = "Rak Suhu Ruang R-05 • Cangkang bersih tanpa retak",
                    stock = "Sisa 12.0 kg",
                    quality = "96% Prima",
                    dishType = "kuning",
                    onClick = { onNavigateToDetail("Telur Ayam Negeri") }
                )
            }

            item {
                IngredientListItem(
                    name = "Beras Pulen Super",
                    description = "Dry Storage R-14 • Wadah kedap bebas kutu & lembab",
                    stock = "Sisa 25.0 kg",
                    quality = "100% Prima",
                    dishType = "liwet",
                    onClick = { onNavigateToDetail("Beras Pulen Super") }
                )
            }

            // ── 7. Modal Banner SOP Kualitas Dapur ─────────────────────────────────
            if (showOperationalAlert) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = CardGoldenYellow,
                            shadowElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Alert",
                                            tint = CardGoldenYellow,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { showOperationalAlert = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Tutup",
                                            tint = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "SOP Kontrol Kualitas & Kesegaran Dapur",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Pastikan suhu Chiller stabil di 2°C - 4°C dan lakukan penimbangan sisa potongan/trim secara berkala untuk menjaga akurasi stok fisik shift.",
                                    fontFamily = InterFontFamily,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = Color.White.copy(alpha = 0.95f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onNavigateToReservation() },
                                    colors = ButtonDefaults.buttonColors(containerColor = TextPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Periksa Denah Slot Rak 1-22",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 8. Riwayat Aktivitas Timbang Sisa Bahan Nyata (Room DB) ───────────
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Aktivitas Timbang Sisa Bahan",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Rekap Shift >",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = CardGoldenYellow,
                        modifier = Modifier.clickable { onNavigateToAudit() }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            val recentList = uiState.recentWasteList
            if (recentList.isNotEmpty()) {
                items(recentList.take(3)) { record ->
                    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                        WasteRecordCard(record = record)
                    }
                }
            } else {
                item {
                    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Inventory,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Belum ada pencatatan sisa bahan shift ini",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Tekan tombol Timbang di bawah untuk scan dan log waste",
                                    fontFamily = InterFontFamily,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // ── 9. Tombol Aksi Utama KitchenGuard ─────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Button(
                        onClick = onNavigateToWaste,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CardGoldenYellow),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+ Catat Sisa Bahan (Timbang & OCR)",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onNavigateToAudit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StrongOrange),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardGoldenYellow)
                    ) {
                        Text(
                            text = "Audit Fisik Tutup Shift",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Komponen: Bahan Prioritas / Kritis Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IngredientPriorityCard(
    modifier: Modifier = Modifier,
    rating: String,
    stock: String,
    title: String,
    description: String,
    dishType: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = CardGoldenYellow,
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Status Capsule
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color.White.copy(alpha = 0.92f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Status",
                        tint = StrongOrange,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = rating,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Food Plate Hero Illustration
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                contentAlignment = Alignment.Center
            ) {
                FoodPlate(
                    modifier = Modifier.size(86.dp),
                    dishType = dishType
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stock & Title
            Text(
                text = stock,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.95f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontFamily = InterFontFamily,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                color = Color.White.copy(alpha = 0.88f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Komponen: Ingredient List Item Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IngredientListItem(
    name: String,
    description: String,
    stock: String,
    quality: String,
    dishType: String,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineBorder),
            shadowElevation = 1.5.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FoodPlate(
                    modifier = Modifier.size(54.dp),
                    dishType = dishType
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stock,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = StrongOrange
                        )
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Text(
                                text = quality,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color(0xFF16A34A),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = name,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                        maxLines = 1,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Komponen: Total Waste Card Terpadu
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TotalWasteCardIntegrated(
    totalWeight: Double,
    onNavigateToWaste: () -> Unit,
    onNavigateToTracking: () -> Unit
) {
    val estLoss = (totalWeight * 28000).toLong()

    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, WarmCardBorder),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL WASTE SHIFT INI",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format("%.2f", totalWeight),
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 32.sp,
                                color = StrongOrange
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "kg",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CardGoldenLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardGoldenYellow),
                        modifier = Modifier.clickable { onNavigateToTracking() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Alur Shift",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = StrongOrange
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Alur",
                                tint = StrongOrange,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CardGoldenLight
                ) {
                    Text(
                        text = if (totalWeight > 0.0) {
                            "Est. Kerugian HPP: Rp ${String.format("%,d", estLoss)}"
                        } else {
                            "Est. Kerugian: — (Terkendali)"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = StrongOrange
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Komponen: Waste Record Card Item
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WasteRecordCard(record: WasteRecordEntity) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = record.ingredientName,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${record.quantity} ${record.unit} • ${record.reason}",
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = CardGoldenLight,
                border = androidx.compose.foundation.BorderStroke(1.dp, WarmCardBorder)
            ) {
                Text(
                    text = record.syncStatus,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = StrongOrange,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Komponen Visual: Ilustrasi Bahan Baku Dapur (Food Plate)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FoodPlate(
    modifier: Modifier = Modifier,
    dishType: String = "ayam"
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White)
            .border(2.5.dp, Color.White.copy(alpha = 0.95f), CircleShape)
            .shadow(2.dp, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.92f)
                .clip(CircleShape)
                .background(Color(0xFFFCF9F2))
                .border(1.dp, Color(0xFFEFE9DB), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize(0.86f)) {
                val radius = size.minDimension / 2
                val center = Offset(size.width / 2, size.height / 2)

                val baseColor = when (dishType) {
                    "kuning" -> Color(0xFFFACC15)
                    "ayam" -> Color(0xFFFED7AA)
                    "babat" -> Color(0xFFE2E8F0)
                    else -> Color(0xFFF8FAFC)
                }

                drawCircle(
                    color = baseColor,
                    radius = radius * 0.92f,
                    center = center
                )

                // Protein / Daging / Bahan utama
                val primaryColor = when (dishType) {
                    "ayam" -> Color(0xFFB45309)
                    "babat" -> Color(0xFF78350F)
                    "kuning" -> Color(0xFFF59E0B)
                    else -> Color(0xFF92400E)
                }
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f),
                    size = Size(radius * 0.75f, radius * 0.55f),
                    cornerRadius = CornerRadius(12f, 12f)
                )

                // Kesegaran / Daun Hijau
                drawCircle(
                    color = Color(0xFF22C55E),
                    radius = radius * 0.22f,
                    center = Offset(center.x + radius * 0.25f, center.y + radius * 0.25f)
                )

                // Highlight Segar
                drawCircle(
                    color = Color(0xFFDCFCE7),
                    radius = radius * 0.12f,
                    center = Offset(center.x + radius * 0.25f, center.y + radius * 0.25f)
                )
            }
        }
    }
}
