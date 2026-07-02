package com.vkard.pro.presentation.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object KycBlock : Screen("kyc_block")
    object Dashboard : Screen("dashboard")
    object CardCreate : Screen("card_create")
    object CustomerList : Screen("customer_list")
    object QrShare : Screen("qr_share/{slug}") {
        fun createRoute(slug: String) = "qr_share/$slug"
    }
}
