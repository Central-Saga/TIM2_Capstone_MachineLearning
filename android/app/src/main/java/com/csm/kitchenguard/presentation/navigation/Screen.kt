package com.csm.kitchenguard.presentation.navigation

/**
 * Daftar rute navigasi untuk aplikasi KitchenGuard CSM.
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object ResetPassword : Screen("reset_password")
    object Hub : Screen("hub")
    object WasteLogging : Screen("waste_logging")
    object StockAudit : Screen("stock_audit")
    object ShiftSummary : Screen("shift_summary")
    object AiVerification : Screen("ai_verification")
    object NotificationCenter : Screen("notification_center")
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object ChangePassword : Screen("change_password")
    object Settings : Screen("settings")
}
