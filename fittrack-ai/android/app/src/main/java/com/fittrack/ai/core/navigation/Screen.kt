package com.fittrack.ai.core.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")
    object ProfileSetup : Screen("profile_setup")
    object GoalSetup : Screen("goal_setup")

    // Main App Destinations
    object Dashboard : Screen("dashboard")
    object CameraScanner : Screen("camera_scanner")
    object ScanResult : Screen("scan_result")
    object Diary : Screen("diary")
    object BarcodeScanner : Screen("barcode_scanner")
    object LabelScanner : Screen("label_scanner")
    object VoiceLogging : Screen("voice_logging")
    object Assistant : Screen("assistant")
    object Analytics : Screen("analytics")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
}
