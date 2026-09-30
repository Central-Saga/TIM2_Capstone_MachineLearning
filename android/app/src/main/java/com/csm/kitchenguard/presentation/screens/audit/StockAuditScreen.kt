package com.csm.kitchenguard.presentation.screens.audit

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Warning
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
import com.csm.kitchenguard.data.local.entity.StockAuditDraftEntity
import com.csm.kitchenguard.presentation.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// Design Tokens (Figma M06)
// ─────────────────────────────────────────────────────────────────────────────
private val StatusGreen = Color(0xFF16A34A)
private val LightGreenBg = Color(0xFFDCFCE7)
private val LightRedBg = Color(0xFFFEE2E2)
private val LightRedText = Color(0xFFB91C1C)
private val BadgeBgOrangeLight = Color(0xFFFFEDD5)
private val BadgeTextOrangeDark = Color(0xFFEA580C)
private val BadgeBgRedLight = Color(0xFFFEE2E2)
private val BadgeTextRedDark = Color(0xFFDC2626)
private val InfoRowBg = Color(0xFFFFFFFF)
private val ChipDefaultBg = Color(0xFFF1F5F9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAuditScreen(
    onNavigateBack: () -> Unit,
    viewModel: StockAuditViewModel
) {
    // Observasi state dari ViewModel
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Top App Bar
        TopBarM06(onNavigateBack = onNavigateBack)

        // Konten utama scrollable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header Stasiun & Shift
            StationHeaderCard(drafts = uiState.drafts)

            // Banner Metrik Audit
            AuditMetricsBanner(draftCount = uiState.drafts.size)

            // Card Peringatan Gramasi
            GramationWarningCard()

            // Section Daftar Item Fisik
            PhysicalItemListSection(uiState.drafts)

            // Tombol Aksi Bawah
            ActionFooterM06(
                isSubmitting = uiState.isSubmitting,
                onSubmitSuccess = uiState.submitSuccess,
                errorMessage = uiState.errorMessage,
                draftCount = uiState.drafts.size,
                onSubmit = { 
                    viewModel.submitAuditAkhirShift()
                    onNavigateBack()
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Top Bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TopBarM06(onNavigateBack: () -> Unit) {
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
// 2. Header Stasiun & Shift
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StationHeaderCard(drafts: List<StockAuditDraftEntity>) {
    // Data stasiun/shift dari draft nyata; netral bila belum ada draft (Issue #21).
    val stationLabel = drafts.firstOrNull()?.let { "Stasiun #${it.stationId}" } ?: "Stasiun belum dipilih"
    val shiftLabel = drafts.firstOrNull()?.let { "Shift #${it.shiftId}" } ?: "Belum ada shift"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = InfoRowBg,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BadgeBgOrangeLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant, // Troli/Bahan placeholder
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stationLabel,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle, // Placeholder clock icon
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = shiftLabel,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Banner Metrik Audit
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AuditMetricsBanner(draftCount: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = InfoRowBg,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kolom 1: jumlah item nyata dari draft.
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$draftCount",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Item",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Text(
                    text = "Akan Diaudit",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            
            // Kolom 2: Anomali — variance dihitung otoritatif backend,
            // belum tersedia di cache client; tampilkan netral (Issue #21).
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFEF2F2), // Very light red
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("—", color = BadgeTextRedDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Anomali",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = BadgeTextRedDark
                    )
                    Text(
                        text = "Belum tersedia",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        color = BadgeTextRedDark
                    )
                }
            }
            
            // Kolom 3
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Status",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = BadgeBgOrangeLight
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(BadgeTextOrangeDark)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "In Progress",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = BadgeTextOrangeDark
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Card Peringatan Gramasi
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GramationWarningCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = InfoRowBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF8FAFC)),
                contentAlignment = Alignment.Center
            ) {
                Text("👨‍🍳", fontSize = 24.sp) // Placeholder ilustrasi chef timbangan
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Pengecekan Gramasi",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = BadgeBgRedLight
                    ) {
                        Text(
                            text = "INFO SOP",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = BadgeTextRedDark
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pastikan timbangan digital sudah terkalibrasi dan pembacaan gramasi stabil sebelum konfirmasi tutup shift.",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Section Daftar Item Fisik Station
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PhysicalItemListSection(drafts: List<StockAuditDraftEntity>) {
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
                text = "Daftar Item Fisik",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Text(
                text = "${drafts.size} item",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = BadgeTextOrangeDark
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (drafts.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = ChipDefaultBg
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Scale,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Belum ada data draft",
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(drafts, key = { it.id }) { draft ->
                    AuditItemCard(
                        draft = draft,
                        onActualPhysicalChanged = { newPhysical ->
                            // Note: Dalam production, perlu repository method untuk
                            // menyimpan perubahan individual draft.
                            // Implementation lengkap perlu di-add di ViewModel.
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AuditItemCard(
    draft: StockAuditDraftEntity,
    onActualPhysicalChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = InfoRowBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Bahan #${draft.ingredientId}",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Batch: ${draft.batchId ?: "-"}",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fisik Terhitung",
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft.actualPhysical.toString(),
                        onValueChange = onActualPhysicalChanged,
                        singleLine = true,
                        modifier = Modifier.width(110.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = draft.unit,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// 6. Tombol Aksi Bawah
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ActionFooterM06(
    isSubmitting: Boolean,
    onSubmitSuccess: Boolean,
    errorMessage: String?,
    draftCount: Int,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kirim Hasil Audit",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color.White.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "$draftCount Item Draft",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 9.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Audit akan otomatis diarsipkan & diverifikasi ke WhatsApp\nHead Chef.",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
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
    name = "Stock Audit - Figma M06",
    showBackground = true,
    backgroundColor = 0xFFF8FAFC,
    device = "spec:width=390dp,height=844dp,dpi=420"
)
@SuppressLint("ViewModelConstructorInComposable")
@Composable
private fun StockAuditScreenPreview() {
    KitchenGuardTheme {
        StockAuditScreen(
            onNavigateBack = {},
            viewModel = StockAuditViewModel(
                shiftDao = object : com.csm.kitchenguard.data.local.dao.ShiftDao {
                    override suspend fun insertOrUpdateActiveShift(
                        shift: com.csm.kitchenguard.data.local.entity.ActiveShiftEntity
                    ) = Unit

                    override fun getActiveShift() =
                        kotlinx.coroutines.flow.flowOf<
                            com.csm.kitchenguard.data.local.entity.ActiveShiftEntity?>(null)

                    override suspend fun getActiveShiftOnce() = null

                    override suspend fun clearActiveShift() = Unit
                },
                auditRepository = object : com.csm.kitchenguard.domain.repository.StockAuditRepository {
                    override suspend fun saveDraft(
                        draft: com.csm.kitchenguard.data.local.entity.StockAuditDraftEntity
                    ): Result<Unit> = Result.success(Unit)

                    override fun getDrafts(
                        shiftId: Long
                    ): kotlinx.coroutines.flow.Flow<List<com.csm.kitchenguard.data.local.entity.StockAuditDraftEntity>> =
                        kotlinx.coroutines.flow.flowOf(emptyList())

                    override suspend fun submitAudit(
                        shiftId: Long,
                        stationId: Long
                    ): Result<Unit> = Result.success(Unit)
                }
            )
        )
    }
}
