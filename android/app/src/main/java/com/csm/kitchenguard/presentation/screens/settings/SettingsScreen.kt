package com.csm.kitchenguard.presentation.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LensBlur
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csm.kitchenguard.presentation.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// Design Tokens Khusus (Figma M13)
// ─────────────────────────────────────────────────────────────────────────────
private val StatusOrange = Color(0xFFEA580C)
private val LightOrangeBg = Color(0xFFFFEDD5)
private val OutlineColor = Color(0xFFE2E8F0)
private val FieldBgGray = Color(0xFFF8FAFC)
private val MutedText = Color(0xFF94A3B8)
private val LightGreenBg = Color(0xFFDCFCE7)
private val StatusGreen = Color(0xFF16A34A)
private val StatusRed = Color(0xFFDC2626)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNotification: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // ── 1. Top App Bar (Rekap Shift) ──────────────────────────────────────
        SettingsTopBar(
            onNavigateBack = onNavigateBack,
            onNavigateToNotification = onNavigateToNotification
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── 2. Settings Header & Search ───────────────────────────────────
            item {
                SettingsHeaderSection(onNavigateBack = onNavigateBack)
            }

            // ── 3. Grup 1: AKUN ───────────────────────────────────────────────
            item {
                SettingsGroupHeader("1. AKUN")
                SettingsCard {
                    SettingsItemRow(
                        icon = Icons.Default.Person,
                        iconBg = LightOrangeBg,
                        iconTint = StatusOrange,
                        title = "Profil Karyawan",
                        subtitle = "Marsel Lino Fulbertus • Sous Chef",
                        action = { RightArrow() }
                    )
                    SettingsDivider()
                    SettingsItemRow(
                        icon = Icons.Default.History, // as password icon
                        iconBg = FieldBgGray,
                        iconTint = TextSecondary,
                        title = "Ganti Kata Sandi",
                        subtitle = "Terakhir diubah 30 hari lalu",
                        action = { RightArrow() }
                    )
                    SettingsDivider()
                    SettingsItemRow(
                        icon = Icons.Default.Fingerprint,
                        iconBg = LightGreenBg,
                        iconTint = StatusGreen,
                        title = "Keamanan Biometrik",
                        subtitle = "Fingerprint / Face Unlock",
                        action = { CustomSwitch(checked = true) }
                    )
                }
            }

            // ── 4. Grup 2: APLIKASI ───────────────────────────────────────────
            item {
                SettingsGroupHeader("2. APLIKASI")
                SettingsCard {
                    SettingsItemRow(
                        icon = Icons.Default.Language,
                        iconBg = FieldBgGray,
                        iconTint = TextSecondary,
                        title = "Bahasa",
                        subtitle = "Bahasa Indonesia",
                        subtitleColor = StatusOrange,
                        action = { RightArrow() }
                    )
                    SettingsDivider()
                    SettingsItemRow(
                        icon = Icons.Default.Palette,
                        iconBg = FieldBgGray,
                        iconTint = TextSecondary,
                        title = "Tema Tampilan",
                        subtitle = "Light (Standar Dapur)",
                        action = { LockedTag() }
                    )
                    SettingsDivider()
                    SettingsItemRow(
                        icon = Icons.Default.TextFields,
                        iconBg = FieldBgGray,
                        iconTint = TextSecondary,
                        title = "Ukuran Teks",
                        subtitle = "Normal (100%)",
                        action = { RightArrow() }
                    )
                }
            }

            // ── 5. Grup 3: WORKFLOW DAPUR ─────────────────────────────────────
            item {
                SettingsGroupHeader("3. WORKFLOW DAPUR")
                SettingsCard {
                    SettingsItemRow(
                        icon = Icons.Default.RestaurantMenu,
                        iconBg = LightOrangeBg,
                        iconTint = StatusOrange,
                        title = "Default Station",
                        subtitle = "Hot Kitchen & Meat Station",
                        subtitleColor = StatusOrange,
                        action = { DownArrow() }
                    )
                    SettingsDivider()
                    SettingsItemRow(
                        icon = Icons.Default.Alarm,
                        iconBg = FieldBgGray,
                        iconTint = TextSecondary,
                        title = "Default Shift",
                        subtitle = "Pagi (06:00 - 15:00 WITA)",
                        action = { DownArrow() }
                    )
                    SettingsDivider()
                    SettingsItemRow(
                        icon = Icons.Default.CameraAlt,
                        iconBg = FieldBgGray,
                        iconTint = TextSecondary,
                        title = "Buka Kamera Otomatis",
                        subtitle = "Saat Catat Waste baru",
                        action = { CustomSwitch(checked = true) }
                    )
                    SettingsDivider()
                    SettingsItemRow(
                        icon = Icons.Default.Smartphone, // as ML Kit icon mock
                        iconBg = FieldBgGray,
                        iconTint = TextSecondary,
                        title = "Deteksi Otomatis ML Kit",
                        subtitle = "OCR timbangan & label barcode",
                        action = { CustomSwitch(checked = true) }
                    )
                    SettingsDivider()
                    SettingsItemRow(
                        icon = Icons.Default.Security, // as AI icon mock
                        iconBg = FieldBgGray,
                        iconTint = TextSecondary,
                        title = "Pemeriksaan Mutu AI",
                        subtitle = "TFLite evaluasi fisik bahan",
                        action = { CustomSwitch(checked = true) }
                    )
                }
            }

            // ── 6. Grup 4: OFFLINE & SINKRONISASI ─────────────────────────────
            item {
                SettingsGroupHeader("4. OFFLINE & SINKRONISASI")
                SettingsCard {
                    SettingsItemRow(
                        icon = Icons.Default.CloudSync,
                        iconBg = LightGreenBg,
                        iconTint = StatusGreen,
                        title = "Sinkronisasi Otomatis",
                        subtitle = "Upload data batch saat idle",
                        action = { CustomSwitch(checked = true) }
                    )
                    SettingsDivider()
                    SettingsItemRow(
                        icon = Icons.Default.Wifi,
                        iconBg = FieldBgGray,
                        iconTint = TextSecondary,
                        title = "Sinkronisasi Hanya Wi-Fi",
                        subtitle = "Gunakan data seluler cadangan",
                        action = { CustomSwitch(checked = false) }
                    )
                    SettingsDivider()
                    SettingsItemRow(
                        icon = Icons.Default.Save,
                        iconBg = FieldBgGray,
                        iconTint = TextSecondary,
                        title = "Simpan Data Offline",
                        subtitle = "Penyimpanan riwayat transaksi",
                        action = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "7 Hari",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = TextBrown
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                DownArrow()
                            }
                        }
                    )
                    
                    // Footer info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sinkronisasi terakhir: 14 Sep 2026 • 13:42 WITA",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 9.sp,
                            color = MutedText
                        )
                    }
                }
            }

            // ── 7. Grup 5: NOTIFIKASI ─────────────────────────────────────────
            item {
                SettingsGroupHeader("5. NOTIFIKASI")
                SettingsCard {
                    SettingsItemRow(
                        icon = Icons.Default.Warning,
                        iconBg = LightOrangeBg,
                        iconTint = StatusOrange,
                        title = "High Waste Alert",
                        subtitle = "Peringatan susut di atas toleransi",
                        action = { CustomSwitch(checked = true) }
                    )
                    SettingsDivider()
                    SettingsItemRow(
                        icon = Icons.Default.Person,
                        iconBg = FieldBgGray,
                        iconTint = TextSecondary,
                        title = "Persetujuan Head Chef",
                        subtitle = "Otorisasi pembuangan bahan mahal",
                        action = { CustomSwitch(checked = true) }
                    )
                    SettingsDivider()
                    SettingsItemRow(
                        icon = Icons.Default.Alarm,
                        iconBg = FieldBgGray,
                        iconTint = TextSecondary,
                        title = "Pengingat Tutup Shift & Audit",
                        subtitle = "Notifikasi 30 menit jelang shift berakhir",
                        action = { CustomSwitch(checked = true) }
                    )
                }
            }

            // ── 8. Grup 6: KAMERA & AI SENSOR ─────────────────────────────────
            item {
                SettingsGroupHeader("6. KAMERA & AI SENSOR")
                SettingsCard {
                    SettingsItemRow(
                        icon = Icons.Default.LensBlur,
                        iconBg = FieldBgGray,
                        iconTint = TextSecondary,
                        title = "Kualitas Kamera OCR",
                        subtitle = "Sedang (Optimal Kecepatan)",
                        action = { DownArrow() }
                    )
                    SettingsDivider()
                    SettingsItemRow(
                        icon = Icons.Default.Settings, // As TFLite icon mock
                        iconBg = LightGreenBg,
                        iconTint = StatusGreen,
                        title = "Model TFLite",
                        subtitle = "Freshness Model v2.4",
                        action = { InstalledBadge() }
                    )
                }
            }

            // ── 9. Grup 7: SISTEM & KELUAR ────────────────────────────────────
            item {
                SettingsGroupHeader("7. SISTEM & KELUAR")
                SettingsCard {
                    SettingsItemRow(
                        icon = Icons.Default.Info,
                        iconBg = FieldBgGray,
                        iconTint = TextSecondary,
                        title = "Versi Aplikasi",
                        subtitle = "Mobile Terminal Suite",
                        action = { GrayPill("v1.0.0 (Build 20260914)") }
                    )
                    SettingsDivider()
                    SettingsItemRow(
                        icon = Icons.Default.Dns,
                        iconBg = FieldBgGray,
                        iconTint = TextSecondary,
                        title = "Status Server",
                        subtitle = "Latensi 18ms",
                        action = { GreenPill("● Online • Bali-01") }
                    )
                    SettingsDivider()
                    SettingsItemRow(
                        icon = Icons.Default.CleaningServices,
                        iconBg = FieldBgGray,
                        iconTint = TextSecondary,
                        title = "Bersihkan Cache Lokal",
                        subtitle = "Data sementara & log foto",
                        action = { GrayPill("↻ 14.2 MB") }
                    )
                }
            }

            // ── 10. Tombol Keluar dari Akun ───────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = { /* Handle logout */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StatusRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed)
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
                            text = "Keluar dari Akun",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = StatusRed
                        )
                    }
                }
            }
        }
    }
}

private val TextBrown = Color(0xFF78350F)

// ─────────────────────────────────────────────────────────────────────────────
// Components
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SettingsTopBar(
    onNavigateBack: () -> Unit,
    onNavigateToNotification: () -> Unit
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
                        text = "Rekap Shift", // Following the image
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }
            
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
                
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧑", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingsHeaderSection(onNavigateBack: () -> Unit) {
    Column {
        // Orange bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryOrange)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onNavigateBack() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Pengaturan",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Search bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, OutlineColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cari stasiun, sensor, atau preferensi...",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        color = TextBrown,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 24.dp, bottom = 8.dp, top = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, OutlineColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingsItemRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    subtitleColor: Color = TextSecondary,
    action: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Handle click */ }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    color = subtitleColor
                )
            }
        }
        
        Box(modifier = Modifier.padding(start = 12.dp)) {
            action()
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = OutlineColor.copy(alpha = 0.3f)
    )
}

@Composable
private fun RightArrow() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MutedText,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
private fun DownArrow() {
    Icon(
        imageVector = Icons.Default.KeyboardArrowDown,
        contentDescription = null,
        tint = MutedText,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
private fun CustomSwitch(checked: Boolean) {
    Switch(
        checked = checked,
        onCheckedChange = null,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = StatusOrange,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = OutlineColor
        ),
        modifier = Modifier.size(width = 36.dp, height = 20.dp)
    )
}

@Composable
private fun LockedTag() {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = FieldBgGray,
        border = BorderStroke(1.dp, OutlineColor)
    ) {
        Text(
            text = "DIKUNCI",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = MutedText
        )
    }
}

@Composable
private fun InstalledBadge() {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = LightGreenBg
    ) {
        Text(
            text = "● Terpasang",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = StatusGreen
        )
    }
}

@Composable
private fun GrayPill(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = FieldBgGray,
        border = BorderStroke(1.dp, OutlineColor)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            fontFamily = JetBrainsMonoFamily,
            fontSize = 10.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun GreenPill(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = LightGreenBg
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = StatusGreen
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEW
// ─────────────────────────────────────────────────────────────────────────────
@Preview(
    name = "Settings Screen - Figma M13",
    showBackground = true,
    backgroundColor = 0xFFF8FAFC,
    device = "spec:width=390dp,height=1200dp,dpi=420"
)
@Composable
private fun SettingsScreenPreview() {
    KitchenGuardTheme {
        SettingsScreen(
            onNavigateBack = {}
        )
    }
}
