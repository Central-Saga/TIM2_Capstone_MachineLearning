package com.csm.kitchenguard.presentation.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
// Design Tokens Khusus (Figma M11)
// ─────────────────────────────────────────────────────────────────────────────
private val StatusOrange = Color(0xFFEA580C)
private val LightOrangeBg = Color(0xFFFFEDD5)
private val StatusRed = Color(0xFFDC2626)
private val LightRedBg = Color(0xFFFEE2E2)
private val LightYellowBg = Color(0xFFFEF9C3)
private val YellowIconColor = Color(0xFFB45309) // Darker yellow/orange for icon
private val InfoGrayBg = Color(0xFFF1F5F9)
private val InfoTextGray = Color(0xFF475569)
private val OutlineColor = Color(0xFFE2E8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePhotoBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── 1. Header Modal ───────────────────────────────────────────────
            BottomSheetHeader(onClose = onDismissRequest)

            Spacer(modifier = Modifier.height(24.dp))

            // ── 2. Preview Foto & Slider Zoom ─────────────────────────────────
            PhotoPreviewSection()

            Spacer(modifier = Modifier.height(24.dp))

            // ── 3. Pilihan Aksi (List Card) ───────────────────────────────────
            ActionOptionsSection()

            Spacer(modifier = Modifier.height(16.dp))

            // ── 4. Box Panduan ────────────────────────────────────────────────
            GuidanceBox()

            Spacer(modifier = Modifier.height(24.dp))

            // ── 5. Tombol Aksi ────────────────────────────────────────────────
            ActionButtons(onCancel = onDismissRequest)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Header Modal
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BottomSheetHeader(onClose: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(StatusOrange)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "IDENTITAS STAF CSM",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = StatusOrange,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ubah Foto Profil",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextPrimary
                )
            }
            
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(InfoGrayBg)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Pilih foto profil kerja yang jelas dengan seragam dapur standar.",
            fontFamily = InterFontFamily,
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 18.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Preview Foto
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PhotoPreviewSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar Bulat
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .border(2.dp, StatusOrange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🧑", fontSize = 64.sp)
            }
            
            // Badge Icon Kamera
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(StatusOrange)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Slider Control
        Surface(
            shape = RoundedCornerShape(50.dp),
            color = InfoGrayBg,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                var sliderPosition by remember { mutableFloatStateOf(0.5f) }
                
                Icon(
                    imageVector = Icons.Default.ZoomOut,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = StatusOrange,
                        activeTrackColor = StatusOrange,
                        inactiveTrackColor = OutlineColor
                    )
                )
                
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(OutlineColor)
                )
                Spacer(modifier = Modifier.width(12.dp))
                
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Rotate",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Pilihan Aksi (List Card)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ActionOptionsSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Ambil Foto dari Kamera
        ActionOptionCard(
            title = "Ambil Foto dari Kamera",
            subtitle = "Gunakan kamera tablet/smartphone dapur",
            icon = Icons.Default.CameraAlt,
            iconBg = LightOrangeBg,
            iconTint = StatusOrange,
            onClick = { /* Handle camera */ }
        )
        
        // Pilih dari Galeri Foto
        ActionOptionCard(
            title = "Pilih dari Galeri Foto",
            subtitle = "Unggah file JPG atau PNG (Maks. 5 MB)",
            icon = Icons.Default.Image,
            iconBg = LightOrangeBg,
            iconTint = StatusOrange,
            onClick = { /* Handle gallery */ }
        )
        
        // Hapus Foto Saat Ini
        ActionOptionCard(
            title = "Hapus Foto Saat Ini",
            subtitle = "Kembalikan ke avatar inisial nama",
            icon = Icons.Default.DeleteOutline,
            iconBg = LightRedBg,
            iconTint = StatusRed,
            titleColor = StatusRed,
            trailingIcon = Icons.Default.RemoveCircleOutline,
            trailingIconTint = StatusRed,
            borderColor = LightRedBg,
            onClick = { /* Handle delete */ }
        )
    }
}

@Composable
private fun ActionOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    titleColor: Color = TextPrimary,
    trailingIcon: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
    trailingIconTint: Color = TextSecondary,
    borderColor: Color = OutlineColor,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = titleColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
            
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = trailingIconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Box Panduan
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GuidanceBox() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = LightYellowBg,
        border = BorderStroke(1.dp, Color(0xFFFEF08A)) // Slightly darker yellow border
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = YellowIconColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Pedoman Foto CSM: Foto harus memperlihatkan wajah staf tanpa masker dapur dan mengenakan apron resmi CSM.",
                fontFamily = InterFontFamily,
                fontSize = 11.sp,
                color = YellowIconColor,
                lineHeight = 16.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Tombol Aksi
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ActionButtons(onCancel: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { /* Handle confirm */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gunakan Foto Ini",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = InfoGrayBg)
        ) {
            Text(
                text = "Batal",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEW
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Change Photo BottomSheet - Figma M11",
    showBackground = true,
    backgroundColor = 0xFFF8FAFC
)
@Composable
private fun ChangePhotoBottomSheetPreview() {
    KitchenGuardTheme {
        ChangePhotoBottomSheet(
            onDismissRequest = {}
        )
    }
}
