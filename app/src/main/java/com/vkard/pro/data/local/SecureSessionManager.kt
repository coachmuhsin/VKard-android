package com.vkard.pro.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SecureSessionManager(context: Context) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    private val sharedPreferences = EncryptedSharedPreferences.create(
        "vkard_secure_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        email: String,
        role: String
    ) {
        sharedPreferences.edit().apply {
            putString("access_token", accessToken)
            putString("refresh_token", refreshToken)
            putString("user_id", userId)
            putString("email", email)
            putString("role", role)
            apply()
        }
    }
    
    fun getAccessToken(): String? = sharedPreferences.getString("access_token", null)
    fun getRefreshToken(): String? = sharedPreferences.getString("refresh_token", null)
    fun getUserId(): String? = sharedPreferences.getString("user_id", null)
    fun getEmail(): String? = sharedPreferences.getString("email", null)
    fun getRole(): String? = sharedPreferences.getString("role", null)
    
    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }
    
    fun isLoggedIn(): Boolean {
        return getAccessToken() != null
    }
}
