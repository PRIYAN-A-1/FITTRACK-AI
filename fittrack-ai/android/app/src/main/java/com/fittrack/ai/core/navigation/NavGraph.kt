package com.fittrack.ai.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.fittrack.ai.FitTrackApp
import com.fittrack.ai.core.components.FitTrackBottomNavBar
import com.fittrack.ai.core.theme.DarkBackground
import com.fittrack.ai.presentation.analytics.AnalyticsScreen
import com.fittrack.ai.presentation.assistant.AssistantScreen
import com.fittrack.ai.presentation.assistant.AssistantViewModel
import com.fittrack.ai.presentation.auth.LoginScreen
import com.fittrack.ai.presentation.auth.RegisterScreen
import com.fittrack.ai.presentation.barcode.BarcodeScannerScreen
import com.fittrack.ai.presentation.barcode.BarcodeViewModel
import com.fittrack.ai.presentation.dashboard.DashboardScreen
import com.fittrack.ai.presentation.dashboard.DashboardViewModel
import com.fittrack.ai.presentation.diary.DiaryScreen
import com.fittrack.ai.presentation.diary.DiaryViewModel
import com.fittrack.ai.presentation.onboarding.OnboardingScreen
import com.fittrack.ai.presentation.profile.ProfileScreen
import com.fittrack.ai.presentation.scanner.CameraScannerScreen
import com.fittrack.ai.presentation.scanner.ScanResultScreen
import com.fittrack.ai.presentation.scanner.ScanViewModel
import com.fittrack.ai.presentation.splash.SplashScreen
import com.fittrack.ai.presentation.voice.VoiceLoggingScreen
import com.fittrack.ai.presentation.voice.VoiceViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Composable
fun FitTrackNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val app = FitTrackApp.instance

    // Shared / Scoped ViewModels
    val scanViewModel = remember {
        ScanViewModel(app.scanRepository, app.mealRepository)
    }
    val dashboardViewModel = remember {
        DashboardViewModel(app.mealRepository, app.trackingRepository, app.sessionManager)
    }
    val diaryViewModel = remember {
        DiaryViewModel(app.mealRepository)
    }
    val voiceViewModel = remember {
        VoiceViewModel(app.scanRepository, app.mealRepository)
    }
    val barcodeViewModel = remember {
        BarcodeViewModel(app.scanRepository, app.mealRepository)
    }
    val assistantViewModel = remember {
        AssistantViewModel(context)
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Splash.route

    val bottomBarRoutes = setOf(
        Screen.Dashboard.route,
        Screen.Diary.route,
        Screen.Assistant.route,
        Screen.Analytics.route
    )

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                FitTrackBottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(Screen.Dashboard.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onCameraClick = {
                        navController.navigate(Screen.CameraScanner.route)
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                val isLoggedIn = remember {
                    runBlocking { app.sessionManager.isLoggedIn.first() }
                }
                SplashScreen(
                    isLoggedIn = isLoggedIn,
                    onNavigateNext = { loggedIn ->
                        if (loggedIn) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.Onboarding.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    authRepo = app.authRepository,
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    authRepo = app.authRepository,
                    onRegisterSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToScan = { navController.navigate(Screen.CameraScanner.route) },
                    onNavigateToVoice = { navController.navigate(Screen.VoiceLogging.route) },
                    onNavigateToBarcode = { navController.navigate(Screen.BarcodeScanner.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToDiary = { navController.navigate(Screen.Diary.route) }
                )
            }

            composable(Screen.CameraScanner.route) {
                CameraScannerScreen(
                    viewModel = scanViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onScanComplete = { navController.navigate(Screen.ScanResult.route) }
                )
            }

            composable(Screen.ScanResult.route) {
                ScanResultScreen(
                    viewModel = scanViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onSaved = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Diary.route) {
                DiaryScreen(
                    viewModel = diaryViewModel,
                    onNavigateToScan = { navController.navigate(Screen.CameraScanner.route) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.VoiceLogging.route) {
                VoiceLoggingScreen(
                    viewModel = voiceViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onMealSaved = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.BarcodeScanner.route) {
                BarcodeScannerScreen(
                    viewModel = barcodeViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onProductSaved = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Assistant.route) {
                AssistantScreen(
                    viewModel = assistantViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    sessionManager = app.sessionManager,
                    onNavigateBack = { navController.popBackStack() },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
