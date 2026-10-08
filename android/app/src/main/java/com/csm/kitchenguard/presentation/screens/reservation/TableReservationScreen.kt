package com.csm.kitchenguard.presentation.screens.reservation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csm.kitchenguard.presentation.theme.*

/**
 * Monitoring Kondisi Bahan pada Slot Rak & Chiller Dapur (Slot 1-22).
 * Aplikasi operasional internal karyawan untuk mengecek kesegaran bahan per kompartemen penyimpanan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableReservationScreen(
    onNavigateBack: () -> Unit,
    onConfirm: (List<Int>) -> Unit = {}
) {
    var selectedZone by remember { mutableStateOf("Semua") }
    val zones = listOf("Semua", "Chiller Daging", "Chiller Sayur", "Freezer Beku", "Dry Storage")

    // Data 22 slot rak penyimpanan bahan dapur
    var selectedSlot by remember { mutableStateOf(10) }

    val slotItems = remember {
        listOf(
            SlotInfo(1, "Ayam Fillet", "4.2 kg", "SEGAR", Color(0xFF22C55E)),
            SlotInfo(2, "Daging Sapi", "6.5 kg", "SEGAR", Color(0xFF22C55E)),
            SlotInfo(3, "Ikan Dori", "2.1 kg", "KRITIS", Color(0xFFEA580C)),
            SlotInfo(4, "Slot Kosong", "—", "KOSONG", Color(0xFF94A3B8)),
            SlotInfo(5, "Telur Negeri", "12.0 kg", "SEGAR", Color(0xFF22C55E)),
            SlotInfo(6, "Bawang Merah", "5.0 kg", "SEGAR", Color(0xFF22C55E)),
            SlotInfo(7, "Susu UHT", "3.0 L", "NEAR EXP", Color(0xFFEA580C)),
            SlotInfo(8, "Bawang Putih", "4.0 kg", "SEGAR", Color(0xFF22C55E)),
            SlotInfo(9, "Wortel Segar", "3.5 kg", "SEGAR", Color(0xFF22C55E)),
            SlotInfo(10, "Ayam Potong", "5.8 kg", "PERLU CEK", CardGoldenYellow),
            SlotInfo(11, "Slot Kosong", "—", "KOSONG", Color(0xFF94A3B8)),
            SlotInfo(12, "Cabai Merah", "2.4 kg", "SEGAR", Color(0xFF22C55E)),
            SlotInfo(13, "Minyak Goreng", "15.0 L", "SEGAR", Color(0xFF22C55E)),
            SlotInfo(14, "Beras Pulen", "25.0 kg", "SEGAR", Color(0xFF22C55E)),
            SlotInfo(15, "Tomat Sayur", "1.8 kg", "PERLU CEK", CardGoldenYellow),
            SlotInfo(16, "Daun Bawang", "0.9 kg", "PERLU CEK", CardGoldenYellow),
            SlotInfo(17, "Tepung Terigu", "10.0 kg", "SEGAR", Color(0xFF22C55E)),
            SlotInfo(18, "Kentang Dieng", "4.5 kg", "SEGAR", Color(0xFF22C55E)),
            SlotInfo(19, "Keju Cheddar", "1.2 kg", "SEGAR", Color(0xFF22C55E)),
            SlotInfo(20, "Daging Cincang", "3.0 kg", "SEGAR", Color(0xFF22C55E)),
            SlotInfo(21, "Mentega Kiloan", "2.0 kg", "SEGAR", Color(0xFF22C55E)),
            SlotInfo(22, "Slot Kosong", "—", "KOSONG", Color(0xFF94A3B8))
        )
    }

    val currentSlotData = slotItems.firstOrNull { it.slotNumber == selectedSlot } ?: slotItems[0]

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Kondisi Slot Rak & Chiller",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Zone Filter Chips
            Text(
                text = "Pilih Kompartemen Dapur",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                zones.take(4).forEach { zone ->
                    val isSelected = zone == selectedZone
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) CardGoldenLight else Color.White)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) CardGoldenYellow else OutlineBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedZone = zone }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = zone,
                            fontFamily = InterFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            color = if (isSelected) StrongOrange else TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Denah Slot Rak 1 - 22
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Denah 22 Slot Penyimpanan",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Suhu Chiller: 2.8°C",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = StatusSuccess
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grid 4 kolom
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(slotItems) { item ->
                    val isSelected = item.slotNumber == selectedSlot
                    Surface(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedSlot = item.slotNumber },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) CardGoldenYellow else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 0.dp else 1.5.dp,
                            color = if (isSelected) Color.Transparent else item.statusColor.copy(alpha = 0.6f)
                        ),
                        shadowElevation = if (isSelected) 4.dp else 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "R-${item.slotNumber}",
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.name.split(" ").first(),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp,
                                maxLines = 1,
                                color = if (isSelected) Color.White.copy(alpha = 0.9f) else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendBadge(color = Color(0xFF22C55E), text = "Segar Prima")
                LegendBadge(color = CardGoldenYellow, text = "Perlu Dicek")
                LegendBadge(color = Color(0xFFEA580C), text = "Near Expired")
                LegendBadge(color = Color(0xFF94A3B8), text = "Slot Kosong")
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Detail Slot Terpilih Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, WarmCardBorder),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Slot R-${currentSlotData.slotNumber}: ${currentSlotData.name}",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Stok Tersimpan: ${currentSlotData.quantity} • Chiller Utama",
                                fontFamily = InterFontFamily,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = currentSlotData.statusColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = currentSlotData.statusLabel,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = currentSlotData.statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onConfirm(listOf(selectedSlot)); onNavigateBack() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CardGoldenYellow),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Scale,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Inspeksi & Timbang Bahan Ini",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class SlotInfo(
    val slotNumber: Int,
    val name: String,
    val quantity: String,
    val statusLabel: String,
    val statusColor: Color
)

@Composable
private fun LegendBadge(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontFamily = InterFontFamily,
            fontSize = 10.sp,
            color = TextSecondary
        )
    }
}
