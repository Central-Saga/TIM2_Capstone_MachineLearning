package com.csm.kitchenguard.presentation.components

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.csm.kitchenguard.presentation.navigation.Screen
import com.csm.kitchenguard.presentation.theme.PrimaryOrange

@Composable
fun KitchenBottomBar(navController: NavController) {
    val items = listOf(
        BottomNavItem("Dapur", "🏠", Screen.Hub.route),
        BottomNavItem("Timbang & Scan", "📷", Screen.WasteLogging.route),
        BottomNavItem("Rekap Shift", "📋", Screen.ShiftSummary.route)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = Color.White
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Text(item.icon) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryOrange,
                    selectedTextColor = PrimaryOrange,
                    indicatorColor = PrimaryOrange.copy(alpha = 0.1f)
                )
            )
        }
    }
}

data class BottomNavItem(
    val title: String,
    val icon: String,
    val route: String
)
