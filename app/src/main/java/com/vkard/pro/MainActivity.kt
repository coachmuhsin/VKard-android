package com.vkard.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = application as VKardApplication
        val container = app.container
        val sessionManager = container.sessionManager
        val authRepository = container.authRepository
        val cardRepository = container.cardRepository
        val customerRepository = container.customerRepository
        
        setContent {
            VKardProTheme {
                val navController = rememberNavController()
                val coroutineScope = rememberCoroutineScope()
                
                val startDestination = remember {
                    if (sessionManager.isLoggedIn()) {
                        val role = sessionManager.getRole()
                        if (role == "super_admin") {
                            Screen.Dashboard.route
                        } else {
                            Screen.KycBlock.route
                        }
                    } else {
                        Screen.Login.route
                    }
                }
                
                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
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
                            role = role,
                            onCreateCard = {
                                navController.navigate(Screen.CardCreate.route)
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
                        route = Screen.CardCreate.route + "?customerId={customerId}",
                        arguments = listOf(navArgument("customerId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        })
                    ) { backStackEntry ->
                        val customerId = backStackEntry.arguments?.getString("customerId")
                        val cardViewModel = remember {
                            CardViewModel(cardRepository, customerRepository)
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
            }
        }
    }
}
