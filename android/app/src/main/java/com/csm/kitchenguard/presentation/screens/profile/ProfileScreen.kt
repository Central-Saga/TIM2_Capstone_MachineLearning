package com.csm.kitchenguard.presentation.screens.profile

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csm.kitchenguard.presentation.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// Design Tokens Khusus (Figma M09)
// ─────────────────────────────────────────────────────────────────────────────
private val StatusGreen = Color(0xFF16A34A)
private val LightGreenBg = Color(0xFFDCFCE7)
private val StatusRed = Color(0xFFDC2626)
private val LightRedBg = Color(0xFFFEE2E2)
private val StatusOrange = Color(0xFFEA580C)
private val LightOrangeBg = Color(0xFFFFEDD5)
private val StatusBrown = Color(0xFF92400E)
private val LightBrownBg = Color(0xFFFDE68A)
private val IconBgGray = Color(0xFFF8FAFC)
private val BadgeBgGreyLight = Color(0xFFF1F5F9)
private val OutlineColor = Color(0xFFE2E8F0)
private val InfoRowBg = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel,
    onNavigateToNotification: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToChangePassword: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    // Data profil nyata dari SessionManager (Issue #21) — bukan persona hardcoded.
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // ── 1. Top App Bar ────────────────────────────────────────────────────
        ProfileTopBar(
            onNavigateBack = onNavigateBack,
            onNavigateToNotification = onNavigateToNotification,
            onNavigateToSettings = onNavigateToSettings
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── 2. Header Profil ──────────────────────────────────────────────
            item {
                ProfileHeaderSection(uiState = uiState)
            }

            // ── 3. Grid Statistik Karyawan ────────────────────────────────────
            item {
                EmployeeStatsGrid(uiState = uiState)
            }

            // ── 4. Progress Bar Reduksi Limbah ────────────────────────────────
            item {
                WasteReductionProgressCard()
            }

            // ── 5. Menu Navigasi Akun ─────────────────────────────────────────
            item {
                AccountPreferencesSection(
                    onNavigateToEditProfile = onNavigateToEditProfile,
                    onNavigateToChangePassword = onNavigateToChangePassword,
                    onNavigateToSettings = onNavigateToSettings
                )
            }

            // ── 6. Tombol Keluar & Footer ─────────────────────────────────────
            item {
                LogoutSection()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Top Bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ProfileTopBar(
    onNavigateBack: () -> Unit,
    onNavigateToNotification: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
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
            // Logo & Title
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
                        text = "Rekap Shift",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }

            // Sync Badge & Icons
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
                            text = "SYNC 100%",
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
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onNavigateToNotification() }
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Small Profile Avatar (M09 design top right)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧑", fontSize = 18.sp)
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
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Header Profil
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ProfileHeaderSection(uiState: ProfileUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar dengan centang hijau
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                // Inisial nama nyata (bukan emoji/foto placeholder persona).
                Text(
                    text = uiState.initials,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(StatusGreen)
                    .border(2.dp, BackgroundLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Nama (dari sesi nyata)
        Text(
            text = uiState.name.ifBlank { "—" },
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = TextPrimary
        )
        
        // Jabatan (role nyata dari sesi)
        Text(
            text = uiState.roleLabel,
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Badges (ID dan Aktif)
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = BadgeBgGreyLight
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Badge,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = uiState.employeeId.ifBlank { "—" },
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = LightGreenBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(StatusGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AKTIF",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = StatusGreen
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Card Lokasi
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = StatusOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "LOKASI OPERASIONAL",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = uiState.stationLabel,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                    }
                }
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LightOrangeBg
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = StatusOrange,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(16.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Grid Statistik Karyawan (2x2)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EmployeeStatsGrid(uiState: ProfileUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Box 1: Shift Aktif.
            // Hanya shift ID yang benar-benar tersedia dari sesi yang ditampilkan;
            // tidak menampilkan jam/nama shift yang dikarang (Issue #21).
            StatBox(
                modifier = Modifier.weight(1f),
                title = "Shift Aktif",
                value = uiState.activeShiftLabel,
                subtitle = uiState.activeShiftSubtitle,
                subtitleColor = TextSecondary,
                icon = Icons.Default.WbSunny,
                iconBg = LightOrangeBg,
                iconTint = StatusOrange
            )
            
            // Box 2: Waste Logged.
            // Metrik count/rupiah belum tersedia dari sumber nyata pada layar ini,
            // tampilkan netral daripada angka palsu.
            StatBox(
                modifier = Modifier.weight(1f),
                title = "Waste Logged",
                value = "—",
                subtitle = "Belum tersedia",
                subtitleColor = TextSecondary,
                icon = Icons.Default.DeleteOutline,
                iconBg = LightRedBg,
                iconTint = StatusRed
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Box 3: Audit Submisi — data belum tersedia, tampilkan netral.
            StatBox(
                modifier = Modifier.weight(1f),
                title = "Audit Submisi",
                value = "—",
                subtitle = "Belum tersedia",
                subtitleColor = TextSecondary,
                icon = Icons.Default.AssignmentTurnedIn,
                iconBg = LightGreenBg,
                iconTint = StatusGreen
            )
            
            // Box 4: Pending Sync — data belum tersedia, tampilkan netral.
            StatBox(
                modifier = Modifier.weight(1f),
                title = "Pending Sync",
                value = "—",
                subtitle = "Belum tersedia",
                subtitleColor = TextSecondary,
                icon = Icons.Default.Sync,
                iconBg = LightBrownBg,
                iconTint = StatusBrown
            )
        }
    }
}

@Composable
private fun StatBox(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    subtitleColor: Color,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color
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
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = subtitle,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                color = subtitleColor
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Progress Bar Reduksi Limbah
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WasteReductionProgressCard() {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = StatusOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Target Reduksi Limbah Shift",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
                
                Text(
                    text = "92% TERJAGA",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = StatusGreen
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Custom Progress Bar (Hijau - Oranye - Abu)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(0.7f).fillMaxHeight().background(StatusGreen))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.weight(0.2f).fillMaxHeight().background(StatusOrange))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.weight(0.1f).fillMaxHeight().background(OutlineColor))
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Maks. Toleransi: 3.50 kg",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
                Text(
                    text = "Aktual: 2.15 kg",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Menu Navigasi Akun
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AccountPreferencesSection(
    onNavigateToEditProfile: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = "PREFERENSI & AKUN",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = TextSecondary,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = InfoRowBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                MenuItem(
                    title = "Edit Profil", 
                    icon = Icons.Default.PersonOutline, 
                    onClick = onNavigateToEditProfile
                )
                HorizontalDivider(color = OutlineColor.copy(alpha = 0.5f))
                MenuItem(
                    title = "Ganti Kata Sandi", 
                    icon = Icons.Default.Lock,
                    onClick = onNavigateToChangePassword
                )
                HorizontalDivider(color = OutlineColor.copy(alpha = 0.5f))
                MenuItem(
                    title = "Pengaturan Aplikasi", 
                    icon = Icons.Default.Tune,
                    onClick = onNavigateToSettings
                )
                HorizontalDivider(color = OutlineColor.copy(alpha = 0.5f))
                MenuItem(title = "Offline & Sync Data", icon = Icons.Default.CloudSync, hasSyncBadge = true)
                HorizontalDivider(color = OutlineColor.copy(alpha = 0.5f))
                MenuItem(title = "Riwayat Aktivitas Shift", icon = Icons.Default.History)
            }
        }
    }
}

@Composable
private fun MenuItem(
    title: String,
    icon: ImageVector,
    hasSyncBadge: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(IconBgGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = TextPrimary
            )
        }
        
        if (hasSyncBadge) {
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = LightGreenBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = StatusGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tersinkron",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = StatusGreen
                    )
                }
            }
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Tombol Keluar & Footer
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LogoutSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedButton(
            onClick = { /* Handle logout */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed),
            border = BorderStroke(1.dp, LightRedBg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = StatusRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Keluar dari Sesi Dapur",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = StatusRed
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "KitchenGuard OS v2.4.8 (CSM Sentinel Build)\nKitchen Terminal: POS-HOT-01 • Bali, Indonesia",
            fontFamily = JetBrainsMonoFamily,
            fontSize = 9.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEW
// ─────────────────────────────────────────────────────────────────────────────
@Preview(
    name = "Profile Screen - Figma M09",
    showBackground = true,
    backgroundColor = 0xFFF8FAFC,
    device = "spec:width=390dp,height=844dp,dpi=420"
)
@SuppressLint("ViewModelConstructorInComposable")
@Composable
private fun ProfileScreenPreview() {
    KitchenGuardTheme {
        val app = androidx.compose.ui.platform.LocalContext.current
            .applicationContext as android.app.Application
        ProfileScreen(
            onNavigateBack = {},
            viewModel = ProfileViewModel(
                application = app,
                sessionManager = com.csm.kitchenguard.data.local.preferences.SessionManager(app)
            ),
            onNavigateToNotification = {}
        )
    }
}
