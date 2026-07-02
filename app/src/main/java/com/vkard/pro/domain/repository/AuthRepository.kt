package com.vkard.pro.domain.repository

import com.vkard.pro.domain.model.User

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun getCurrentUser(): User?
    suspend fun getKycStatus(userId: String): Result<String>
}
