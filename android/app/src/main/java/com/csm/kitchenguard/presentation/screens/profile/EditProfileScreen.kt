package com.csm.kitchenguard.presentation.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
// Design Tokens Khusus (Figma M10)
// ─────────────────────────────────────────────────────────────────────────────
private val StatusGreen = Color(0xFF16A34A)
private val LightGreenBg = Color(0xFFDCFCE7)
private val InfoGrayBg = Color(0xFFF1F5F9)
private val InfoTextGray = Color(0xFF475569)
private val DisabledFieldBg = Color(0xFFF1F5F9)
private val LockedTagBg = Color(0xFFE2E8F0)
private val LockedTagText = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit
) {
    var showPhotoBottomSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // ── 1. Header Oranye ──────────────────────────────────────────────────
        EditProfileTopHeader(
            onNavigateBack = onNavigateBack,
            onClickChangePhoto = { showPhotoBottomSheet = true }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── 2. Data Pribadi ───────────────────────────────────────────────
            item {
                PersonalInfoSection()
            }

            // ── 3. Data Otoritas Dapur ────────────────────────────────────────
            item {
                AuthorityDataSection()
            }

            // ── 4. Card Keamanan PIN ──────────────────────────────────────────
            item {
                PinSecurityCard()
            }

            // ── 5. Tombol Submit ──────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onNavigateBack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Simpan Perubahan",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showPhotoBottomSheet) {
        ChangePhotoBottomSheet(
            onDismissRequest = { showPhotoBottomSheet = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Header Oranye (Top Bar + Avatar)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EditProfileTopHeader(
    onNavigateBack: () -> Unit,
    onClickChangePhoto: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = PrimaryOrange,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            // TopBar standard (back & simpan)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onNavigateBack() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Edit Profil",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.clickable { onNavigateBack() }
                ) {
                    Text(
                        text = "Simpan",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
            
            // Avatar Profil
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clickable { onClickChangePhoto() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        // Lingkaran foto profil
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(4.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🧑", fontSize = 56.sp)
                        }
                        
                        // Badge Kamera Ubah Foto
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.offset(x = (-4).dp, y = (-4).dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Ubah Foto",
                                tint = PrimaryOrange,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ubah Foto",
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
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Section Data Pribadi
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PersonalInfoSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(16.dp)
                            .background(PrimaryOrange)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Data Pribadi",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                }
                
                Text(
                    text = "DAPAT DIEDIT",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Nama Lengkap
            EditableField(
                label = "Nama Lengkap",
                value = "Marsel Lino Fulbertus",
                trailingIcon = Icons.Default.Edit,
                iconLabel = "👤"
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Email Kerja
            EditableField(
                label = "Email Kerja",
                value = "marsel@csm.id",
                trailingIcon = Icons.Default.Email,
                iconLabel = "📧"
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Nomor Telepon
            EditableField(
                label = "Nomor Telepon",
                value = "0812 3456 7890",
                trailingIcon = Icons.Default.PhoneIphone,
                iconLabel = "📞"
            )
        }
    }
}

@Composable
private fun EditableField(
    label: String,
    value: String,
    trailingIcon: ImageVector,
    iconLabel: String
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = iconLabel,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = InfoGrayBg
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Section Data Otoritas Dapur
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AuthorityDataSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(16.dp)
                            .background(TextSecondary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Data Otoritas Dapur",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = LockedTagBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = LockedTagText,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "TERKUNCI",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = LockedTagText,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Info Box Abu-abu
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = InfoGrayBg
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFB45309), // Brownish orange
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bidang ini diverifikasi oleh sistem. Perubahan penugasan memerlukan otorisasi HR / General Manager.",
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                        color = InfoTextGray,
                        lineHeight = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Employee ID
            DisabledField(
                label = "Employee ID",
                value = "CSM-KT-021",
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                leadingIcon = Icons.Outlined.Badge,
                extraHeader = "Verified Hash"
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Role / Jabatan
            DisabledField(
                label = "Role / Jabatan",
                value = "Kitchen Staff",
                trailingContent = {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = LockedTagBg
                    ) {
                        Text(
                            text = "OPERASIONAL",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = LockedTagText
                        )
                    }
                },
                leadingIcon = Icons.Default.Restaurant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Kitchen Station
            DisabledField(
                label = "Kitchen Station",
                value = "Hot Kitchen & Meat Station",
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                leadingIcon = Icons.Default.Restaurant // Use something like chair/station if available
            )
        }
    }
}

@Composable
private fun DisabledField(
    label: String,
    value: String,
    trailingContent: @Composable () -> Unit,
    leadingIcon: ImageVector,
    extraHeader: String? = null
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = TextSecondary
            )
            if (extraHeader != null) {
                Text(
                    text = extraHeader,
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 9.sp,
                    color = TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = DisabledFieldBg,
            border = BorderStroke(1.dp, OutlineBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = value,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }
                trailingContent()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Card Keamanan PIN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PinSecurityCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(LightGreenBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "PIN Terminal Prep Aktif",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Terakhir diganti 14 hari lalu",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
            
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = InfoGrayBg,
                modifier = Modifier.clickable { /* Ganti PIN */ }
            ) {
                Text(
                    text = "Ganti PIN",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEW
// ─────────────────────────────────────────────────────────────────────────────
@Preview(
    name = "Edit Profile Screen - Figma M10",
    showBackground = true,
    backgroundColor = 0xFFF8FAFC,
    device = "spec:width=390dp,height=844dp,dpi=420"
)
@Composable
private fun EditProfileScreenPreview() {
    KitchenGuardTheme {
        EditProfileScreen(
            onNavigateBack = {}
        )
    }
}
