package com.vkard.pro.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionDto(
    val id: String,
    val digital_card_id: String,
    val start_date: String,
    val end_date: String,
    val status: String
)

@Serializable
data class DigitalCardWithSub(
    val id: String? = null,
    val customer_id: String,
    val slug: String,
    val full_name: String,
    val designation: String,
    val company_name: String,
    val mobile_number: String,
    val whatsapp_number: String,
    val email: String,
    val status: String,
    val created_by: String? = null,
    val created_at: String? = null,
    val theme_name: String? = "glass_purple",
    val logo_url: String? = null,
    val subscriptions: List<SubscriptionDto> = emptyList(),
    @Serializable(with = SafeGalleryImagesSerializer::class)
    val gallery_images: List<String> = emptyList()
)
