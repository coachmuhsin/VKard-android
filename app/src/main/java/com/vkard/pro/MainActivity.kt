package com.vkard.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.vkard.pro.presentation.splash.SplashScreen
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vkard.pro.presentation.card.CardCreateScreen
import com.vkard.pro.presentation.card.CardViewModel
import com.vkard.pro.presentation.customer.CustomerListScreen
import com.vkard.pro.presentation.customer.CustomerViewModel
import com.vkard.pro.presentation.dashboard.DashboardScreen
import com.vkard.pro.presentation.dashboard.DashboardViewModel
import com.vkard.pro.presentation.login.KycBlockScreen
import com.vkard.pro.presentation.login.LoginScreen
import com.vkard.pro.presentation.login.LoginViewModel
import com.vkard.pro.presentation.navigation.Screen
import com.vkard.pro.presentation.qr.QrCodeShareScreen
import com.vkard.pro.presentation.theme.VKardProTheme
import kotlinx.coroutines.launch

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.vkard.pro.presentation.update.UpdateViewModel
import com.vkard.pro.presentation.update.ForceUpdateScreen
import com.vkard.pro.presentation.update.DownloadProgressDialog
import com.vkard.pro.presentation.update.InstallReadyDialog
import com.vkard.pro.domain.model.DownloadState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        val app = application as VKardApplication
        val container = app.container
        val sessionManager = container.sessionManager
        val authRepository = container.authRepository
        val cardRepository = container.cardRepository
        val customerRepository = container.customerRepository
        val updateViewModelFactory = container.updateViewModelFactory
        
        setContent {
            VKardProTheme {
                val navController = rememberNavController()
                val coroutineScope = rememberCoroutineScope()
                val updateViewModel: UpdateViewModel = remember { updateViewModelFactory() }
                
                LaunchedEffect(Unit) {
                    updateViewModel.checkForUpdates(forceRefresh = false)
                }
                
                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Splash.route
                    ) {
                    // Splash Screen Destination
                    composable(Screen.Splash.route) {
                        SplashScreen(
                            onSplashComplete = {
                                val destination = if (sessionManager.isLoggedIn()) {
                                    val role = sessionManager.getRole()
                                    if (role == "super_admin") {
                                        Screen.Dashboard.route
                                    } else {
                                        Screen.KycBlock.route
                                    }
                                } else {
                                    Screen.Login.route
                                }
                                navController.navigate(destination) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    // Login Destination
                    composable(Screen.Login.route) {
                        val loginViewModel = remember { LoginViewModel(authRepository) }
                        LoginScreen(
                            viewModel = loginViewModel,
                            onLoginSuccess = { role ->
                                coroutineScope.launch {
                                    if (role == "super_admin") {
                                        navController.navigate(Screen.Dashboard.route) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    } else {
                                        val userId = sessionManager.getUserId() ?: ""
                                        authRepository.getKycStatus(userId)
                                            .onSuccess { kycStatus ->
                                                if (kycStatus == "verified") {
                                                    navController.navigate(Screen.Dashboard.route) {
                                                        popUpTo(Screen.Login.route) { inclusive = true }
                                                    }
                                                } else {
                                                    navController.navigate(Screen.KycBlock.route) {
                                                        popUpTo(Screen.Login.route) { inclusive = true }
                                                    }
                                                }
                                            }
                                            .onFailure {
                                                navController.navigate(Screen.KycBlock.route) {
                                                    popUpTo(Screen.Login.route) { inclusive = true }
                                                }
                                            }
                                    }
                                }
                            }
                        )
                    }
                    
                    // KYC Block Screen
                    composable(Screen.KycBlock.route) {
                        var kycStatus by remember { mutableStateOf("pending") }
                        var rejectionReason by remember { mutableStateOf<String?>(null) }
                        
                        LaunchedEffect(Unit) {
                            val userId = sessionManager.getUserId() ?: ""
                            val role = sessionManager.getRole() ?: ""
                            if (role == "super_admin") {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.KycBlock.route) { inclusive = true }
                                }
                            } else {
                                authRepository.getKycStatus(userId)
                                    .onSuccess { status ->
                                        kycStatus = status
                                        if (status == "verified") {
                                            navController.navigate(Screen.Dashboard.route) {
                                                popUpTo(Screen.KycBlock.route) { inclusive = true }
                                            }
                                        }
                                    }
                            }
                        }
                        
                        KycBlockScreen(
                            status = kycStatus,
                            rejectionReason = rejectionReason,
                            onLogout = {
                                coroutineScope.launch {
                                    authRepository.logout()
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                    
                    // Dashboard Destination
                    composable(Screen.Dashboard.route) {
                        val dashboardViewModel = remember {
                            DashboardViewModel(authRepository, cardRepository, sessionManager)
                        }
                        val role = sessionManager.getRole() ?: "agent"
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            updateViewModel = updateViewModel,
                            role = role,
                            onCreateCard = { slug ->
                                val route = if (slug != null) {
                                    Screen.CardCreate.route + "?cardSlug=$slug"
                                } else {
                                    Screen.CardCreate.route
                                }
                                navController.navigate(route)
                            },
                            onManageCustomers = {
                                navController.navigate(Screen.CustomerList.route)
                            },
                            onShareCard = { slug ->
                                navController.navigate(Screen.QrShare.createRoute(slug))
                            },
                            onLogout = {
                                coroutineScope.launch {
                                    authRepository.logout()
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                    
                    // Card Creation Destination (Supports optional customerId argument)
                    composable(
                        route = Screen.CardCreate.route + "?customerId={customerId}&cardSlug={cardSlug}",
                        arguments = listOf(
                            navArgument("customerId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("cardSlug") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val customerId = backStackEntry.arguments?.getString("customerId")
                        val cardSlug = backStackEntry.arguments?.getString("cardSlug")
                        val cardViewModel = remember {
                            CardViewModel(cardRepository, customerRepository)
                        }
                        
                        LaunchedEffect(cardSlug) {
                            if (cardSlug != null) {
                                cardViewModel.loadCardForEdit(cardSlug)
                            }
                        }
                        
                        CardCreateScreen(
                            viewModel = cardViewModel,
                            sessionManager = sessionManager,
                            customerId = customerId,
                            onBack = {
                                navController.popBackStack()
                            },
                            onSuccess = { slug ->
                                navController.navigate(Screen.QrShare.createRoute(slug)) {
                                    popUpTo(Screen.Dashboard.route) { inclusive = false }
                                }
                            }
                        )
                    }
                    
                    // Customer List & Selection Destination
                    composable(Screen.CustomerList.route) {
                        val customerViewModel = remember {
                            CustomerViewModel(customerRepository)
                        }
                        CustomerListScreen(
                            viewModel = customerViewModel,
                            sessionManager = sessionManager,
                            onBack = {
                                navController.popBackStack()
                            },
                            onSelectCustomerForCard = { customerId ->
                                navController.navigate(Screen.CardCreate.route + "?customerId=$customerId")
                            }
                        )
                    }
                    
                    // QR Share Destination
                    composable(
                        route = Screen.QrShare.route,
                        arguments = listOf(navArgument("slug") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val slug = backStackEntry.arguments?.getString("slug") ?: ""
                        QrCodeShareScreen(
                            slug = slug,
                            onBack = {
                                navController.popBackStack(Screen.Dashboard.route, false)
                            }
                        )
                    }
                }
                    
                    val forceUpdateInfo = updateViewModel.latestVersionInfo
                    if (forceUpdateInfo != null && forceUpdateInfo.forceUpdate) {
                        ForceUpdateScreen(
                            versionInfo = forceUpdateInfo,
                            downloadState = updateViewModel.downloadState,
                            onUpdateClick = { updateViewModel.startDownload() },
                            onInstallClick = { updateViewModel.checkAndInstall() }
                        )
                    }

                    when (val state = updateViewModel.downloadState) {
                        is DownloadState.Downloading -> {
                            if (forceUpdateInfo?.forceUpdate != true) {
                                DownloadProgressDialog(
                                    progress = state.progress,
                                    onDismiss = {}
                                )
                            }
                        }
                        is DownloadState.Completed -> {
                            if (forceUpdateInfo?.forceUpdate != true) {
                                InstallReadyDialog(
                                    onInstallClick = { updateViewModel.checkAndInstall() },
                                    onDismiss = {}
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
