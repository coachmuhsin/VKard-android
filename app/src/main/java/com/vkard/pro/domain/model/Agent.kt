package com.vkard.pro.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Agent(
    val id: String,
    val agent_code: String,
    val name: String,
    val franchise_id: String? = null,
    val credits_balance: Int,
    val status: String,
    val created_by: String? = null,
    val created_at: String,
    val is_active: Boolean = true
)
