package com.vkard.pro.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class KycSubmission(
    val id: String,
    val user_id: String,
    val role: String,
    val business_name: String,
    val owner_name: String,
    val mobile_number: String,
    val whatsapp_number: String,
    val email: String,
    val address_line_1: String,
    val address_line_2: String? = null,
    val city: String,
    val district: String,
    val state: String,
    val pin_code: String,
    val country: String,
    val government_id_type: String,
    val government_id_number: String? = null,
    val government_id_url: String,
    val pan_number: String? = null,
    val pan_card_url: String? = null,
    val profile_photo_url: String,
    val status: String,
    val rejection_reason: String? = null,
    val created_at: String,
    val submitted_at: String? = null,
    val verified_at: String? = null,
    val verified_by: String? = null
)
