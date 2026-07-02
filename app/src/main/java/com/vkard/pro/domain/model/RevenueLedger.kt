package com.vkard.pro.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RevenueLedger(
    val id: String,
    val created_at: String,
    val user_id: String? = null,
    val user_name: String? = null,
    val user_email: String? = null,
    val user_role: String? = null,
    val transaction_type: String,
    val credits_used: Int,
    val remarks: String? = null,
    val parent_franchise_id: String? = null
)
