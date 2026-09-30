package com.csm.kitchenguard.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.csm.kitchenguard.di.AppModule
import com.csm.kitchenguard.domain.model.UserRole
import com.csm.kitchenguard.domain.model.UserRole.Companion.canAccessKitchenFeatures
import com.csm.kitchenguard.presentation.components.KitchenBottomBar
import com.csm.kitchenguard.presentation.screens.audit.ShiftSummaryScreen
import com.csm.kitchenguard.presentation.screens.audit.StockAuditScreen
import com.csm.kitchenguard.presentation.screens.auth.AuthViewModel
import com.csm.kitchenguard.presentation.screens.auth.LoginScreen
import com.csm.kitchenguard.presentation.screens.auth.ResetPasswordScreen
import com.csm.kitchenguard.presentation.screens.hub.KitchenHubScreen
import com.csm.kitchenguard.presentation.screens.hub.KitchenHubViewModel
import com.csm.kitchenguard.presentation.screens.waste.AiVerificationScreen
import com.csm.kitchenguard.presentation.screens.waste.WasteLoggingScreen
import com.csm.kitchenguard.presentation.screens.waste.WasteLoggingViewModel
import com.csm.kitchenguard.presentation.screens.notification.NotificationCenterScreen
import com.csm.kitchenguard.presentation.screens.profile.ProfileScreen
import com.csm.kitchenguard.presentation.screens.profile.EditProfileScreen
import com.csm.kitchenguard.presentation.screens.profile.ChangePasswordScreen
import com.csm.kitchenguard.presentation.screens.settings.SettingsScreen
import com.csm.kitchenguard.presentation.screens.audit.StockAuditViewModel


@Composable
fun AppNavigation(diContainer: AppModule) {
    val navController = rememberNavController()

    // Mengamati state navigasi saat ini
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Issue #26 — RBAC: Amati role pengguna dari sesi.
    // Bila role UNKNOWN (belum login / sesi tidak valid), redirect ke Login
    // saat mencoba mengakses rute yang dilindungi.
    val profileUiState by diContainer.profileViewModel.uiState.collectAsState()
    val currentRole = UserRole.fromValue(profileUiState.role)

    // Rute yang memerlukan role valid (minimal STAFF)
    val protectedRoutes = setOf(
        Screen.Hub.route,
        Screen.WasteLogging.route,
        Screen.StockAudit.route,
        Screen.ShiftSummary.route,
        Screen.AiVerification.route,
        Screen.NotificationCenter.route,
        Screen.Profile.route,
        Screen.EditProfile.route,
        Screen.ChangePassword.route,
        Screen.Settings.route
    )

    LaunchedEffect(currentRoute, currentRole) {
        val isProtected = currentRoute != null && currentRoute in protectedRoutes
        val isLoading = profileUiState.isLoading
        if (isProtected && !isLoading && !currentRole.canAccessKitchenFeatures()) {
            // Sesi tidak valid atau role tidak dikenal → redirect ke Login
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Tentukan apakah BottomBar harus disembunyikan
    // BottomBar tidak dimunculkan di layar Login, Waste Logging, dan Stock Audit
    val showBottomBar = currentRoute in listOf(
        Screen.Hub.route,
        Screen.ShiftSummary.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                KitchenBottomBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                val viewModel = diContainer.authViewModel
                LoginScreen(
                    onLoginSuccess = { 
                        navController.navigate(Screen.Hub.route) { 
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onForgotPassword = { 
                        navController.navigate(Screen.ResetPassword.route)
                    },
                    viewModel = viewModel
                )
            }

            composable(Screen.ResetPassword.route) {
                ResetPasswordScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.Hub.route) {
                val hubViewModel = diContainer.kitchenHubViewModel
                KitchenHubScreen(
                    viewModel = hubViewModel,
                    onNavigateToWaste = { navController.navigate(Screen.WasteLogging.route) },
                    onNavigateToAudit = { navController.navigate(Screen.StockAudit.route) },
                    onNavigateToNotification = { navController.navigate(Screen.NotificationCenter.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            
            composable(Screen.WasteLogging.route) {
                val wasteViewModel = diContainer.wasteLoggingViewModel
                WasteLoggingScreen(
                    viewModel = wasteViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAiVerification = { navController.navigate(Screen.AiVerification.route) }
                )
            }
            
            composable(Screen.StockAudit.route) {
                val auditViewModel = diContainer.stockAuditViewModel
                StockAuditScreen(
                    viewModel = auditViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.ShiftSummary.route) {
                ShiftSummaryScreen()
            }
            
            composable(Screen.AiVerification.route) {
                AiVerificationScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSubmitWriteOff = { 
                        navController.navigate(Screen.Hub.route) {
                            popUpTo(Screen.Hub.route) { inclusive = false }
                        }
                    },
                    viewModel = diContainer.aiVerificationViewModel
                )
            }
            
            composable(Screen.NotificationCenter.route) {
                NotificationCenterScreen(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = diContainer.notificationViewModel
                )
            }
            
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = diContainer.profileViewModel,
                    onNavigateToNotification = { navController.navigate(Screen.NotificationCenter.route) },
                    onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
                    onNavigateToChangePassword = { navController.navigate(Screen.ChangePassword.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            
            composable(Screen.EditProfile.route) {
                EditProfileScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.ChangePassword.route) {
                ChangePasswordScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNotification = { navController.navigate(Screen.NotificationCenter.route) }
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNotification = { navController.navigate(Screen.NotificationCenter.route) }
                )
            }
        }
    }
}
