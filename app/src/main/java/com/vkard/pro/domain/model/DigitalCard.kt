package com.vkard.pro.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DigitalCard(
    val id: String? = null,
    val customer_id: String,
    val slug: String,
    val full_name: String,
    val designation: String,
    val company_name: String,
    val mobile_number: String,
    val whatsapp_number: String,
    val email: String,
    val address: String? = null,
    val company_description: String? = null,
    val logo_url: String? = null,
    val banner_url: String? = null,
    val website_url: String? = null,
    val facebook_url: String? = null,
    val instagram_url: String? = null,
    val linkedin_url: String? = null,
    val youtube_url: String? = null,
    val google_maps_url: String? = null,
    val google_review_url: String? = null,
    val upi_id: String? = null,
    val upi_name: String? = null,
    val upi_description: String? = null,
    val bank_holder_name: String? = null,
    val bank_name: String? = null,
    val bank_account_number: String? = null,
    val bank_ifsc: String? = null,
    val bank_branch: String? = null,
    val status: String = "active",
    val created_by: String? = null,
    val created_at: String? = null,
    val card_type: String? = "individual",
    val business_category: String? = "Others",
    val gst_number: String? = null,
    val business_hours: String? = null,
    val content_display_mode: String? = "icons_text",
    val theme_name: String? = "glass_purple",
    val credits_deducted: Boolean? = false,
    val enable_booking: Boolean? = false,
    val booking_whatsapp: String? = null,
    @Serializable(with = SafeGalleryImagesSerializer::class)
    val gallery_images: List<String> = emptyList()
)
