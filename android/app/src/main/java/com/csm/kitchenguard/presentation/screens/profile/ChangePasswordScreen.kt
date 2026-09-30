package com.csm.kitchenguard.presentation.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
// Design Tokens Khusus (Figma M12)
// ─────────────────────────────────────────────────────────────────────────────
private val StatusOrange = Color(0xFFEA580C)
private val LightOrangeBg = Color(0xFFFFEDD5)
private val InfoGrayBg = Color(0xFFF8FAFC) // Very light gray
private val FieldBgGray = Color(0xFFF8FAFC)
private val StatusGreen = Color(0xFF16A34A)
private val LightGreenBg = Color(0xFF86EFAC) // KUAT 3/3 uses brighter green
private val OutlineColor = Color(0xFFE2E8F0)
private val TextBrown = Color(0xFF78350F) // For info box text

@Composable
fun ChangePasswordScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNotification: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // ── 1. Top App Bar (Rekap Shift) ──────────────────────────────────────
        ChangePasswordTopBar(
            onNavigateBack = onNavigateBack,
            onNavigateToNotification = onNavigateToNotification
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── 2. Sub Header (Pengaturan Akun) ───────────────────────────────
            item {
                SubHeaderSection(onNavigateBack = onNavigateBack)
            }

            // ── 3. Form Input Password ────────────────────────────────────────
            item {
                PasswordFormSection()
            }

            // ── 4. Card Indikator Kekuatan Sandi ──────────────────────────────
            item {
                PasswordStrengthCard()
            }

            // ── 5. Informasi Keamanan ─────────────────────────────────────────
            item {
                SecurityInfoSection()
            }

            // ── 6. Tombol Submit ──────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(32.dp))
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
                            imageVector = Icons.Default.History, // as rotate icon mock
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Perbarui Kata Sandi",
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
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Top Bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChangePasswordTopBar(
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

// ─────────────────────────────────────────────────────────────────────────────
// 2. Sub Header
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SubHeaderSection(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onNavigateBack() }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Pengaturan Akun",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary
            )
        }
        
        Surface(
            shape = RoundedCornerShape(50.dp),
            color = OutlineColor.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "SECURE PORTAL",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Form Input Password
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PasswordFormSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Kata Sandi Saat Ini
            PasswordField(
                label = "Kata Sandi Saat Ini",
                icon = Icons.Default.History, // Mock history/reset
                leadingIcon = Icons.Default.VpnKey,
                placeholder = "Masukkan kata sandi lama",
                topRightText = "Lupa sandi?"
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Kata Sandi Baru
            PasswordField(
                label = "Kata Sandi Baru",
                icon = Icons.Default.Lock,
                leadingIcon = Icons.Default.Password,
                placeholder = "Buat sandi baru yang kuat"
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Konfirmasi Kata Sandi Baru
            PasswordField(
                label = "Konfirmasi Kata Sandi Baru",
                icon = Icons.Default.DoneAll, // as double check mock
                leadingIcon = Icons.Default.CheckCircle, // mock double check
                placeholder = "Ketik ulang kata sandi baru"
            )
        }
    }
}

@Composable
private fun PasswordField(
    label: String,
    icon: ImageVector,
    leadingIcon: ImageVector,
    placeholder: String,
    topRightText: String? = null
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = StatusOrange,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
            }
            if (topRightText != null) {
                Text(
                    text = topRightText,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = StatusOrange,
                    modifier = Modifier.clickable { /* Handle forgot password */ }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = FieldBgGray,
            border = BorderStroke(1.dp, OutlineColor.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = placeholder,
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Show/Hide Password",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Card Indikator Kekuatan Sandi
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PasswordStrengthCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = InfoGrayBg
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF065F46), // Dark green
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ketentuan Keamanan\nSandi:",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = LightGreenBg
                ) {
                    Text(
                        text = "KUAT\n3/3",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color(0xFF065F46),
                        textAlign = TextAlign.Center,
                        lineHeight = 12.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ChecklistItem("Minimum 8 karakter")
            Spacer(modifier = Modifier.height(8.dp))
            ChecklistItem("Minimal 1 huruf besar (A-Z)")
            Spacer(modifier = Modifier.height(8.dp))
            ChecklistItem("Minimal 1 angka (0-9)")
        }
    }
}

@Composable
private fun ChecklistItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = StatusGreen,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = StatusGreen
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Informasi Keamanan
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SecurityInfoSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Box 1: Info Login Ulang
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFAF4ED) // Light brownish
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(LightOrangeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextBrown,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Perangkat lain yang sedang aktif mungkin perlu login ulang setelah kata sandi diubah.",
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    color = TextBrown,
                    lineHeight = 16.sp
                )
            }
        }
        
        // Box 2: Shift Terminal Safety
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, OutlineColor)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(LightOrangeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security, // Mock safe icon
                        contentDescription = null,
                        tint = StatusOrange,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "SHIFT TERMINAL SAFETY",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Gunakan sandi unik untuk otorisasi void & timbang",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEW
// ─────────────────────────────────────────────────────────────────────────────
@Preview(
    name = "Change Password Screen - Figma M12",
    showBackground = true,
    backgroundColor = 0xFFF8FAFC,
    device = "spec:width=390dp,height=844dp,dpi=420"
)
@Composable
private fun ChangePasswordScreenPreview() {
    KitchenGuardTheme {
        ChangePasswordScreen(
            onNavigateBack = {}
        )
    }
}
