package com.vkard.pro.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Franchise(
    val id: String,
    val franchise_code: String,
    val name: String,
    val credits_balance: Int,
    val status: String,
    val created_by: String? = null,
    val created_at: String,
    val is_active: Boolean = true
)
