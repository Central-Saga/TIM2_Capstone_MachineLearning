package com.csm.kitchenguard.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.csm.kitchenguard.presentation.navigation.Screen
import com.csm.kitchenguard.presentation.theme.CardGoldenYellow
import com.csm.kitchenguard.presentation.theme.InterFontFamily
import com.csm.kitchenguard.presentation.theme.PrimaryOrange
import com.csm.kitchenguard.presentation.theme.StrongOrange

/**
 * 5-Tab Floating Bottom Navigation untuk Karyawan Dapur (Sesuai Desain Referensi):
 * 1. Dapur (Kitchen Hub)
 * 2. Slot Rak (Kondisi 22 Slot Rak & Chiller Dapur)
 * 3. Timbang (Center Elevated Floating Button: Scan OCR & Klasifikasi AI)
 * 4. Alur Shift (Alur Operasional Shift & Audit Kualitas)
 * 5. Profil (Akun & Shift Karyawan)
 */
@Composable
fun KitchenBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val barContentHeight = 62.dp
    val centerFabSize = 54.dp
    val fabProtrusion = 16.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. Container Dock Putih dengan Sudut Melengkung & Shadow Halus
        Surface(
            color = Color.White,
            shadowElevation = 14.dp,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = fabProtrusion)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // Aman untuk navigation bar Android (3-button atau gesture pill)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barContentHeight)
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tab 1: Dapur
                    val isHome = currentRoute == Screen.Hub.route
                    BottomBarItem(
                        title = "Dapur",
                        icon = Icons.Outlined.Home,
                        isSelected = isHome,
                        onClick = {
                            navController.navigate(Screen.Hub.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )

                    // Tab 2: Audit Stok (PRD P0 #13 & Section 24)
                    val isAudit = currentRoute == Screen.StockAudit.route
                    BottomBarItem(
                        title = "Audit Stok",
                        icon = Icons.Outlined.Inventory,
                        isSelected = isAudit,
                        onClick = {
                            navController.navigate(Screen.StockAudit.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )

                    // Tab 3: Center Slot Placeholder untuk Tombol Melayang
                    val isWaste = currentRoute == Screen.WasteLogging.route || currentRoute == Screen.AiVerification.route
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .width(62.dp)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                navController.navigate(Screen.WasteLogging.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            .padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "Timbang",
                            fontFamily = InterFontFamily,
                            fontWeight = if (isWaste) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = if (isWaste) StrongOrange else Color(0xFF94A3B8)
                        )
                    }

                    // Tab 4: Rekap Shift (PRD Section 8 & Section 4 P0 #15)
                    val isShift = currentRoute == Screen.ShiftSummary.route
                    BottomBarItem(
                        title = "Rekap Shift",
                        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                        isSelected = isShift,
                        onClick = {
                            navController.navigate(Screen.ShiftSummary.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )

                    // Tab 5: Profil
                    val isProfil = currentRoute == Screen.Profile.route
                    BottomBarItem(
                        title = "Profil",
                        icon = Icons.Outlined.Person,
                        isSelected = isProfil,
                        onClick = {
                            navController.navigate(Screen.Profile.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }

        // 2. Center Floating Action Button (Elevated Golden Circle)
        val isWaste = currentRoute == Screen.WasteLogging.route || currentRoute == Screen.AiVerification.route
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 0.dp) // Mengapung di atas kurva putih Surface
                .size(centerFabSize)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    spotColor = CardGoldenYellow,
                    ambientColor = PrimaryOrange
                )
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFCF53),
                            CardGoldenYellow
                        )
                    )
                )
                .border(3.dp, Color.White, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    navController.navigate(Screen.WasteLogging.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = "Timbang & Scan OCR",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun BottomBarItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(width = 62.dp, height = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) StrongOrange else Color(0xFF94A3B8),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = title,
            fontFamily = InterFontFamily,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp,
            color = if (isSelected) StrongOrange else Color(0xFF94A3B8)
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 2.dp)
                    .clip(CircleShape)
                    .background(StrongOrange)
            )
        }
    }
}
