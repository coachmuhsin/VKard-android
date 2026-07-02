package com.vkard.pro.data.repository

import com.vkard.pro.data.local.SecureSessionManager
import com.vkard.pro.data.remote.SupabaseClientProvider
import com.vkard.pro.domain.model.User
import com.vkard.pro.domain.repository.AuthRepository
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class AuthRepositoryImpl(
    private val sessionManager: SecureSessionManager
) : AuthRepository {
    
    private val supabase = SupabaseClientProvider.client
    
    override suspend fun login(email: String, password: String): Result<User> {
        return runCatching {
            // 1. Sign in with email and password
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            
            val currentSession = supabase.auth.currentSessionOrNull()
                ?: throw Exception("Failed to retrieve authentication session.")
            
            val userId = currentSession.user?.id 
                ?: throw Exception("Failed to retrieve user ID.")
            
            // 2. Fetch user profile from public.users
            val profiles = supabase.postgrest["users"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }.decodeList<User>()
            
            val profile = profiles.firstOrNull() ?: throw Exception("Profile not found.")
            
            // 3. Enforce active status based on role
            if (profile.role == "agent") {
                val agentsList = supabase.postgrest["agents"]
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }.decodeList<JsonObject>()
                
                val agent = agentsList.firstOrNull()
                val isActive = agent?.get("is_active")?.jsonPrimitive?.content?.toBoolean() ?: true
                if (!isActive) {
                    supabase.auth.signOut()
                    throw Exception("Your Agent account is deactivated. Please contact support.")
                }
            } else if (profile.role == "franchise") {
                val franchisesList = supabase.postgrest["franchises"]
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }.decodeList<JsonObject>()
                
                val franchise = franchisesList.firstOrNull()
                val isActive = franchise?.get("is_active")?.jsonPrimitive?.content?.toBoolean() ?: true
                if (!isActive) {
                    supabase.auth.signOut()
                    throw Exception("Your Franchise account is deactivated. Please contact support.")
                }
            }
            
            // 4. Save to secure storage
            sessionManager.saveSession(
                accessToken = currentSession.accessToken,
                refreshToken = currentSession.refreshToken ?: "",
                userId = userId,
                email = profile.email,
                role = profile.role
            )
            
            profile
        }
    }
    
    override suspend fun logout(): Result<Unit> {
        return runCatching {
            supabase.auth.signOut()
            sessionManager.clearSession()
        }
    }
    
    override suspend fun getCurrentUser(): User? {
        val userId = sessionManager.getUserId() ?: return null
        val email = sessionManager.getEmail() ?: return null
        val role = sessionManager.getRole() ?: return null
        return User(id = userId, email = email, role = role, created_at = "")
    }
    
    override suspend fun getKycStatus(userId: String): Result<String> {
        return runCatching {
            val kycList = supabase.postgrest["kyc_submissions"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }.decodeList<JsonObject>()
            
            val kyc = kycList.firstOrNull()
            kyc?.get("status")?.jsonPrimitive?.content ?: "draft"
        }
    }
}
