package com.vkard.pro.presentation.login

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vkard.pro.domain.model.KycSubmission
import com.vkard.pro.domain.repository.AuthRepository
import com.vkard.pro.data.remote.UuidValidator
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class KycViewModel(
    private val authRepository: AuthRepository,
    private val userId: String,
    private val role: String
) : ViewModel() {

    var submission by mutableStateOf<KycSubmission?>(null)
        private set

    var isLoading by mutableStateOf(true)
        private set

    var isSaving by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)

    // Form Fields (Step 1: Business Details)
    var businessName by mutableStateOf("")
    var ownerName by mutableStateOf("")
    var mobileNumber by mutableStateOf("")
    var whatsappNumber by mutableStateOf("")
    var email by mutableStateOf("")

    // Step 2: Address
    var addressLine1 by mutableStateOf("")
    var addressLine2 by mutableStateOf("")
    var city by mutableStateOf("")
    var district by mutableStateOf("")
    var state by mutableStateOf("")
    var country by mutableStateOf("India")
    var pinCode by mutableStateOf("")

    // Step 3: Identity
    var governmentIdType by mutableStateOf("Aadhaar")
    var governmentIdNumber by mutableStateOf("")
    var governmentIdUrl by mutableStateOf("")
    var governmentIdBytes by mutableStateOf<ByteArray?>(null)
    var governmentIdFileName by mutableStateOf("")

    // Step 4: PAN (Optional)
    var panNumber by mutableStateOf("")
    var panCardUrl by mutableStateOf("")
    var panCardBytes by mutableStateOf<ByteArray?>(null)
    var panCardFileName by mutableStateOf("")

    // Step 5: Profile Photo
    var profilePhotoUrl by mutableStateOf("")
    var profilePhotoBytes by mutableStateOf<ByteArray?>(null)
    var profilePhotoFileName by mutableStateOf("")

    init {
        loadSubmission()
    }

    fun loadSubmission() {
        if (!UuidValidator.isValidUuid(userId)) {
            isLoading = false
            errorMessage = "Please complete your profile."
            return
        }
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            authRepository.getKycSubmission(userId)
                .onSuccess { sub ->
                    submission = sub
                    if (sub != null) {
                        businessName = sub.business_name
                        ownerName = sub.owner_name
                        mobileNumber = sub.mobile_number
                        whatsappNumber = sub.whatsapp_number
                        email = sub.email
                        
                        addressLine1 = sub.address_line_1
                        addressLine2 = sub.address_line_2 ?: ""
                        city = sub.city
                        district = sub.district
                        state = sub.state
                        country = sub.country
                        pinCode = sub.pin_code
                        
                        governmentIdType = sub.government_id_type
                        governmentIdNumber = sub.government_id_number ?: ""
                        governmentIdUrl = sub.government_id_url
                        
                        panNumber = sub.pan_number ?: ""
                        panCardUrl = sub.pan_card_url ?: ""
                        
                        profilePhotoUrl = sub.profile_photo_url
                    }
                    isLoading = false
                }
                .onFailure { t ->
                    if (com.vkard.pro.BuildConfig.DEBUG) {
                        android.util.Log.e("KycViewModel", "Failed to fetch KYC", t)
                    }
                    submission = null
                    isLoading = false
                }
        }
    }

    private fun mapError(throwable: Throwable, fallback: String): String {
        val msg = throwable.message ?: ""
        if (com.vkard.pro.BuildConfig.DEBUG) {
            android.util.Log.e("KycViewModel", "KYC Error details logged only in Logcat", throwable)
        }
        return when {
            msg.contains("Permission", ignoreCase = true) || msg.contains("policy", ignoreCase = true) -> {
                "Permission denied."
            }
            msg.contains("Connect", ignoreCase = true) || msg.contains("timeout", ignoreCase = true) || msg.contains("host", ignoreCase = true) -> {
                "Internet connection unavailable."
            }
            else -> {
                fallback
            }
        }
    }

    fun saveDraft(onComplete: (Boolean) -> Unit = {}) {
        if (!UuidValidator.isValidUuid(userId)) {
            errorMessage = "Please complete your profile."
            onComplete(false)
            return
        }
        viewModelScope.launch {
            isSaving = true
            errorMessage = null
            try {
                uploadFiles()

                val newSub = KycSubmission(
                    id = submission?.id ?: "",
                    user_id = userId,
                    role = role,
                    business_name = businessName,
                    owner_name = ownerName,
                    mobile_number = mobileNumber,
                    whatsapp_number = whatsappNumber,
                    email = email,
                    address_line_1 = addressLine1,
                    address_line_2 = if (addressLine2.isBlank()) null else addressLine2,
                    city = city,
                    district = district,
                    state = state,
                    pin_code = pinCode,
                    country = country,
                    government_id_type = governmentIdType,
                    government_id_number = if (governmentIdNumber.isBlank()) null else governmentIdNumber,
                    government_id_url = governmentIdUrl,
                    pan_number = if (panNumber.isBlank()) null else panNumber,
                    pan_card_url = if (panCardUrl.isBlank()) null else panCardUrl,
                    profile_photo_url = profilePhotoUrl,
                    status = "draft",
                    rejection_reason = submission?.rejection_reason,
                    created_at = submission?.created_at ?: ""
                )

                authRepository.upsertKycSubmission(newSub)
                    .onSuccess {
                        loadSubmission()
                        isSaving = false
                        onComplete(true)
                    }
                    .onFailure { e ->
                        isSaving = false
                        errorMessage = mapError(e, "Unable to save changes.")
                        onComplete(false)
                    }
            } catch (e: Exception) {
                isSaving = false
                errorMessage = mapError(e, "Unable to save changes.")
                onComplete(false)
            }
        }
    }

    fun submitKyc(onComplete: (Boolean) -> Unit) {
        if (!UuidValidator.isValidUuid(userId)) {
            errorMessage = "Please complete your profile."
            onComplete(false)
            return
        }
        viewModelScope.launch {
            isSaving = true
            errorMessage = null
            try {
                uploadFiles()

                if (businessName.isBlank() || ownerName.isBlank() || mobileNumber.isBlank() || email.isBlank()) {
                    throw Exception("Business details are incomplete.")
                }
                if (addressLine1.isBlank() || city.isBlank() || district.isBlank() || state.isBlank() || pinCode.isBlank()) {
                    throw Exception("Business address is incomplete.")
                }
                if (governmentIdUrl.isBlank()) {
                    throw Exception("Identity document upload is required.")
                }
                if (profilePhotoUrl.isBlank()) {
                    throw Exception("Profile photo upload is required.")
                }

                val newSub = KycSubmission(
                    id = submission?.id ?: "",
                    user_id = userId,
                    role = role,
                    business_name = businessName,
                    owner_name = ownerName,
                    mobile_number = mobileNumber,
                    whatsapp_number = whatsappNumber,
                    email = email,
                    address_line_1 = addressLine1,
                    address_line_2 = if (addressLine2.isBlank()) null else addressLine2,
                    city = city,
                    district = district,
                    state = state,
                    pin_code = pinCode,
                    country = country,
                    government_id_type = governmentIdType,
                    government_id_number = if (governmentIdNumber.isBlank()) null else governmentIdNumber,
                    government_id_url = governmentIdUrl,
                    pan_number = if (panNumber.isBlank()) null else panNumber,
                    pan_card_url = if (panCardUrl.isBlank()) null else panCardUrl,
                    profile_photo_url = profilePhotoUrl,
                    status = "pending",
                    rejection_reason = null,
                    created_at = submission?.created_at ?: "",
                    submitted_at = java.time.Instant.now().toString()
                )

                authRepository.upsertKycSubmission(newSub)
                    .onSuccess {
                        loadSubmission()
                        isSaving = false
                        onComplete(true)
                    }
                    .onFailure { e ->
                        isSaving = false
                        errorMessage = mapError(e, "KYC submission failed.")
                        onComplete(false)
                    }
            } catch (e: Exception) {
                isSaving = false
                errorMessage = mapError(e, e.message ?: "KYC submission failed.")
                onComplete(false)
            }
        }
    }

    private suspend fun uploadFiles() {
        governmentIdBytes?.let { bytes ->
            val ext = if (governmentIdFileName.endsWith(".pdf", ignoreCase = true)) "pdf" else "jpg"
            val name = "${userId}_govt_id_${System.currentTimeMillis()}.$ext"
            authRepository.uploadKycFile(name, bytes, if (ext == "pdf") "application/pdf" else "image/jpeg")
                .onSuccess { url ->
                    governmentIdUrl = url
                    governmentIdBytes = null
                }
                .onFailure { throw it }
        }

        panCardBytes?.let { bytes ->
            val ext = if (panCardFileName.endsWith(".pdf", ignoreCase = true)) "pdf" else "jpg"
            val name = "${userId}_pan_${System.currentTimeMillis()}.$ext"
            authRepository.uploadKycFile(name, bytes, if (ext == "pdf") "application/pdf" else "image/jpeg")
                .onSuccess { url ->
                    panCardUrl = url
                    panCardBytes = null
                }
                .onFailure { throw it }
        }

        profilePhotoBytes?.let { bytes ->
            val name = "${userId}_photo_${System.currentTimeMillis()}.jpg"
            authRepository.uploadKycFile(name, bytes, "image/jpeg")
                .onSuccess { url ->
                    profilePhotoUrl = url
                    profilePhotoBytes = null
                }
                .onFailure { throw it }
        }
    }

    fun setFileBytes(step: Int, fileName: String, bytes: ByteArray) {
        when (step) {
            3 -> {
                governmentIdBytes = bytes
                governmentIdFileName = fileName
            }
            4 -> {
                panCardBytes = bytes
                panCardFileName = fileName
            }
            5 -> {
                profilePhotoBytes = bytes
                profilePhotoFileName = fileName
            }
        }
    }

    fun setBitmap(step: Int, bitmap: Bitmap) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val bytes = stream.toByteArray()
        setFileBytes(step, "camera_photo.jpg", bytes)
    }
}
