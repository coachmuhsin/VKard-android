package com.vkard.pro.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Customer(
    val id: String? = null,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val company_name: String? = null,
    val created_by: String? = null,
    val created_at: String? = null
)
