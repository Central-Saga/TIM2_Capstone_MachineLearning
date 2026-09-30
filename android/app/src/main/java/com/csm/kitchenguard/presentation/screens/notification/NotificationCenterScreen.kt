package com.csm.kitchenguard.presentation.screens.notification

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csm.kitchenguard.presentation.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// Design Tokens Khusus (Figma M08)
// ─────────────────────────────────────────────────────────────────────────────
private val StatusGreen = Color(0xFF16A34A)
private val LightGreenBg = Color(0xFFDCFCE7)
private val StatusRedBg = Color(0xFFDC2626)
private val LightRedBg = Color(0xFFFEE2E2)
private val LightRedText = Color(0xFFB91C1C)
private val BadgeBgOrangeLight = Color(0xFFFFEDD5)
private val BadgeTextOrangeDark = Color(0xFFEA580C)
private val BadgeBgBrownLight = Color(0xFFFDE68A) // Placeholder brown light
private val BadgeTextBrownDark = Color(0xFF92400E)
private val BadgeBgGreyLight = Color(0xFFF1F5F9)
private val BadgeTextGreyDark = Color(0xFF475569)
private val InfoRowBg = Color(0xFFFFFFFF)
private val OutlineColor = Color(0xFFE2E8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationViewModel
) {
    // Notifikasi nyata dari cache lokal (Issue #14 & #21).
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // ── 1. Top App Bar ────────────────────────────────────────────────────
        TopBarM08(onNavigateBack = onNavigateBack)

        // Konten utama scrollable
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // ── 2. Header (Pusat Peringatan & Tabs) ───────────────────────────
            item {
                NotificationHeader()
                FilterTabs()
                DateHeader()
            }

            // ── 3. Daftar Kartu Notifikasi ────────────────────────────────────
            // ── 3. Daftar Kartu Notifikasi (dari cache nyata, Issue #21) ──────
            if (uiState.isEmpty) {
                item {
                    Text(
                        text = "Belum ada notifikasi.",
                        modifier = Modifier.padding(16.dp),
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            } else {
                items(uiState.notifications) { notification ->
                    NotificationCardItem(
                        icon = Icons.Default.Notifications,
                        iconBgColor = BadgeBgGreyLight,
                        iconTintColor = BadgeTextGreyDark,
                        title = notification.title,
                        hasRedDot = !notification.isRead,
                        badgeText = notification.type,
                        badgeBgColor = BadgeBgGreyLight,
                        badgeTextColor = BadgeTextGreyDark,
                        content = {
                            Text(
                                text = notification.message,
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        },
                        footerText = notification.createdAt,
                        isUnread = !notification.isRead
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // ── 4. Bottom Info Card ───────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SentinelInfoCard()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Top Bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TopBarM08(onNavigateBack: () -> Unit) {
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
            // Kiri: Logo + Title (Sama dengan M07)
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
                        text = "Rekap Shift", // Mengikuti gambar M08
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
                            text = "SYNC 100%",
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
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Header (Pusat Peringatan & Tabs)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NotificationHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Pusat\nPeringatan",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextPrimary,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryOrange),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "2",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Baru",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 9.sp,
                        color = Color.White
                    )
                }
            }
        }
        
        Surface(
            shape = RoundedCornerShape(50.dp),
            color = BadgeBgGreyLight,
            modifier = Modifier.clickable { /* mark all read */ }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = StatusGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tandai Semua\nDibaca",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FilterTabs() {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(text = "Semua (4)", isSelected = true)
        }
        item {
            FilterChip(text = "Waste (2)", isSelected = false)
        }
        item {
            FilterChip(text = "Audit (1)", isSelected = false)
        }
        item {
            FilterChip(text = "Approval", isSelected = false)
        }
    }
}

@Composable
private fun FilterChip(text: String, isSelected: Boolean) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = if (isSelected) PrimaryOrange else Color.White,
        border = if (!isSelected) BorderStroke(1.dp, OutlineColor) else null,
        modifier = Modifier.clickable { /* select tab */ }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = if (isSelected) Color.White else TextSecondary
        )
    }
}

@Composable
private fun DateHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.LocalFireDepartment,
            contentDescription = null,
            tint = PrimaryOrange,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "Hari ini",
            fontFamily = JetBrainsMonoFamily,
            fontSize = 11.sp,
            color = TextSecondary
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Daftar Kartu Notifikasi
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NotificationCardItem(
    icon: ImageVector,
    iconBgColor: Color,
    iconTintColor: Color,
    title: String,
    hasRedDot: Boolean = false,
    badgeText: String,
    badgeBgColor: Color,
    badgeTextColor: Color,
    content: @Composable () -> Unit,
    footerText: String,
    isUnread: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = InfoRowBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnread) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon Background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                // Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        if (hasRedDot) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryOrange)
                            )
                        }
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeBgColor
                    ) {
                        Text(
                            text = badgeText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = badgeTextColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Content
                content()
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = footerText,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Bottom Info Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SentinelInfoCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = BadgeBgGreyLight
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "KitchenGuard Live Sentinel Aktif",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Sensitivitas toleransi timbangan ±2.5g",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEW
// ─────────────────────────────────────────────────────────────────────────────
@Preview(
    name = "Notification Center - Figma M08",
    showBackground = true,
    backgroundColor = 0xFFF8FAFC,
    device = "spec:width=390dp,height=844dp,dpi=420"
)
@SuppressLint("ViewModelConstructorInComposable")
@Composable
private fun NotificationCenterScreenPreview() {
    KitchenGuardTheme {
        val app = androidx.compose.ui.platform.LocalContext.current
            .applicationContext as android.app.Application
        NotificationCenterScreen(
            onNavigateBack = {},
            viewModel = NotificationViewModel(
                com.csm.kitchenguard.di.AppModule.getInstance(app).syncRepository
            )
        )
    }
}
