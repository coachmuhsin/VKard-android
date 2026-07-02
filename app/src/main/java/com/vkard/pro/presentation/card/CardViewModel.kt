package com.vkard.pro.presentation.card

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vkard.pro.domain.model.Customer
import com.vkard.pro.domain.model.DigitalCard
import com.vkard.pro.domain.repository.CardRepository
import com.vkard.pro.domain.repository.CustomerRepository
import kotlinx.coroutines.launch

sealed interface CardFormUiState {
    object Idle : CardFormUiState
    object Loading : CardFormUiState
    data class Success(val slug: String) : CardFormUiState
    data class Error(val message: String) : CardFormUiState
}

class CardViewModel(
    private val cardRepository: CardRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {
    
    // Form fields
    var fullName by mutableStateOf("")
    var designation by mutableStateOf("")
    var companyName by mutableStateOf("")
    var mobileNumber by mutableStateOf("")
    var whatsappNumber by mutableStateOf("")
    var email by mutableStateOf("")
    var address by mutableStateOf("")
    var companyDescription by mutableStateOf("")
    var slug by mutableStateOf("")
    
    // Media URLs
    var logoUrl by mutableStateOf<String?>(null)
    var bannerUrl by mutableStateOf<String?>(null)
    
    // Social links & details
    var websiteUrl by mutableStateOf("")
    var facebookUrl by mutableStateOf("")
    var instagramUrl by mutableStateOf("")
    var linkedinUrl by mutableStateOf("")
    var youtubeUrl by mutableStateOf("")
    var googleMapsUrl by mutableStateOf("")
    var googleReviewUrl by mutableStateOf("")
    
    // UPI & Bank
    var upiId by mutableStateOf("")
    var upiName by mutableStateOf("")
    var upiDescription by mutableStateOf("")
    var bankHolderName by mutableStateOf("")
    var bankName by mutableStateOf("")
    var bankAccountNumber by mutableStateOf("")
    var bankIfsc by mutableStateOf("")
    var bankBranch by mutableStateOf("")
    
    // Theme and settings
    var themeName by mutableStateOf("glass_purple")
    var cardType by mutableStateOf("individual")
    var gstNumber by mutableStateOf("")
    var businessHours by mutableStateOf("")
    var contentDisplayMode by mutableStateOf("icons_text")
    var enableBooking by mutableStateOf(false)
    var bookingWhatsapp by mutableStateOf("")
    var businessCategory by mutableStateOf("Others")
    
    // Selection state
    var selectedCustomerId by mutableStateOf<String?>(null)
    var customers by mutableStateOf<List<Customer>>(emptyList())
        private set
        
    var uiState by mutableStateOf<CardFormUiState>(CardFormUiState.Idle)
        private set
        
    var isUploadingMedia by mutableStateOf(false)
        private set
        
    fun loadCustomers(userId: String, role: String) {
        viewModelScope.launch {
            customerRepository.getCustomers(userId, role)
                .onSuccess {
                    customers = it
                    if (selectedCustomerId == null && it.isNotEmpty()) {
                        selectedCustomerId = it.first().id
                    }
                }
        }
    }
    
    fun uploadLogo(base64Image: String) {
        isUploadingMedia = true
        viewModelScope.launch {
            cardRepository.uploadMedia(base64Image)
                .onSuccess {
                    logoUrl = it
                    isUploadingMedia = false
                }
                .onFailure {
                    isUploadingMedia = false
                }
        }
    }
    
    fun uploadBanner(base64Image: String) {
        isUploadingMedia = true
        viewModelScope.launch {
            cardRepository.uploadMedia(base64Image)
                .onSuccess {
                    bannerUrl = it
                    isUploadingMedia = false
                }
                .onFailure {
                    isUploadingMedia = false
                }
        }
    }
    
    var isEditMode by mutableStateOf(false)
    var editingCardId by mutableStateOf<String?>(null)

    fun loadCardForEdit(slug: String) {
        uiState = CardFormUiState.Loading
        viewModelScope.launch {
            cardRepository.getCardBySlug(slug)
                .onSuccess { card ->
                    if (card != null) {
                        isEditMode = true
                        editingCardId = card.id
                        setEditData(card)
                        uiState = CardFormUiState.Idle
                    } else {
                        uiState = CardFormUiState.Error("Card not found.")
                    }
                }
                .onFailure {
                    uiState = CardFormUiState.Error(it.message ?: "Failed to load card.")
                }
        }
    }

    fun submitCard(userId: String, role: String, status: String = "active") {
        val customerId = selectedCustomerId
        if (customerId == null) {
            uiState = CardFormUiState.Error("Please select or create a customer first.")
            return
        }
        if (fullName.isBlank() || designation.isBlank() || companyName.isBlank()) {
            uiState = CardFormUiState.Error("Name, Designation and Company Name are required.")
            return
        }
        
        // Auto-generate slug if blank
        val computedSlug = if (slug.isBlank()) {
            fullName.lowercase().trim().replace("\\s+".toRegex(), "-").replace("[^\\w-]".toRegex(), "")
        } else {
            slug.lowercase().trim()
        }
        
        val card = DigitalCard(
            id = editingCardId,
            customer_id = customerId,
            slug = computedSlug,
            full_name = fullName.trim(),
            designation = designation.trim(),
            company_name = companyName.trim(),
            mobile_number = mobileNumber.trim(),
            whatsapp_number = whatsappNumber.trim(),
            email = email.trim(),
            address = address.trim().ifBlank { null },
            company_description = companyDescription.trim().ifBlank { null },
            logo_url = logoUrl,
            banner_url = bannerUrl,
            website_url = websiteUrl.trim().ifBlank { null },
            facebook_url = facebookUrl.trim().ifBlank { null },
            instagram_url = instagramUrl.trim().ifBlank { null },
            linkedin_url = linkedinUrl.trim().ifBlank { null },
            youtube_url = youtubeUrl.trim().ifBlank { null },
            google_maps_url = googleMapsUrl.trim().ifBlank { null },
            google_review_url = googleReviewUrl.trim().ifBlank { null },
            upi_id = upiId.trim().ifBlank { null },
            upi_name = upiName.trim().ifBlank { null },
            upi_description = upiDescription.trim().ifBlank { null },
            bank_holder_name = bankHolderName.trim().ifBlank { null },
            bank_name = bankName.trim().ifBlank { null },
            bank_account_number = bankAccountNumber.trim().ifBlank { null },
            bank_ifsc = bankIfsc.trim().ifBlank { null },
            bank_branch = bankBranch.trim().ifBlank { null },
            theme_name = themeName,
            card_type = cardType,
            business_category = businessCategory,
            gst_number = gstNumber.trim().ifBlank { null },
            business_hours = businessHours.trim().ifBlank { null },
            content_display_mode = contentDisplayMode,
            enable_booking = enableBooking,
            booking_whatsapp = bookingWhatsapp.trim().ifBlank { null },
            status = status
        )
        
        uiState = CardFormUiState.Loading
        viewModelScope.launch {
            if (isEditMode) {
                cardRepository.updateCard(card)
                    .onSuccess {
                        uiState = CardFormUiState.Success(card.slug)
                    }
                    .onFailure {
                        uiState = CardFormUiState.Error(it.message ?: "Failed to update visiting card.")
                    }
            } else {
                cardRepository.createCard(card, role)
                    .onSuccess {
                        uiState = CardFormUiState.Success(it)
                    }
                    .onFailure {
                        uiState = CardFormUiState.Error(it.message ?: "Failed to create visiting card.")
                    }
            }
        }
    }
    
    fun setEditData(card: DigitalCard) {
        fullName = card.full_name
        designation = card.designation
        companyName = card.company_name
        mobileNumber = card.mobile_number
        whatsappNumber = card.whatsapp_number
        email = card.email
        address = card.address ?: ""
        companyDescription = card.company_description ?: ""
        slug = card.slug
        logoUrl = card.logo_url
        bannerUrl = card.banner_url
        websiteUrl = card.website_url ?: ""
        facebookUrl = card.facebook_url ?: ""
        instagramUrl = card.instagram_url ?: ""
        linkedinUrl = card.linkedin_url ?: ""
        youtubeUrl = card.youtube_url ?: ""
        googleMapsUrl = card.google_maps_url ?: ""
        googleReviewUrl = card.google_review_url ?: ""
        upiId = card.upi_id ?: ""
        upiName = card.upi_name ?: ""
        upiDescription = card.upi_description ?: ""
        bankHolderName = card.bank_holder_name ?: ""
        bankName = card.bank_name ?: ""
        bankAccountNumber = card.bank_account_number ?: ""
        bankIfsc = card.bank_ifsc ?: ""
        bankBranch = card.bank_branch ?: ""
        themeName = card.theme_name ?: "glass_purple"
        cardType = card.card_type ?: "individual"
        businessCategory = card.business_category ?: "Others"
        gstNumber = card.gst_number ?: ""
        businessHours = card.business_hours ?: ""
        contentDisplayMode = card.content_display_mode ?: "icons_text"
        enableBooking = card.enable_booking ?: false
        bookingWhatsapp = card.booking_whatsapp ?: ""
        selectedCustomerId = card.customer_id
    }
    
    fun clearError() {
        uiState = CardFormUiState.Idle
    }
}
