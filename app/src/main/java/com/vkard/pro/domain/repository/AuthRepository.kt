package com.vkard.pro.domain.repository

import com.vkard.pro.domain.model.User
import com.vkard.pro.domain.model.KycSubmission

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun getCurrentUser(): User?
    suspend fun getKycStatus(userId: String): Result<String>
    
    suspend fun getKycSubmission(userId: String): Result<KycSubmission?>
    suspend fun upsertKycSubmission(submission: KycSubmission): Result<Unit>
    suspend fun uploadKycFile(fileName: String, fileBytes: ByteArray, contentType: String): Result<String>
    suspend fun getAllKycSubmissions(): Result<List<KycSubmission>>
    suspend fun getFranchiseAgentsKycSubmissions(franchiseId: String): Result<List<KycSubmission>>
    suspend fun updateKycStatus(submissionId: String, status: String, rejectionReason: String?, verifiedBy: String): Result<Unit>
}

