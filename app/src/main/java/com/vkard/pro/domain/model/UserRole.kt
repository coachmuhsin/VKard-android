package com.vkard.pro.domain.model

enum class UserRole {
    SUPER_ADMIN, FRANCHISE, AGENT;
    
    companion object {
        fun fromString(role: String): UserRole {
            return when (role.lowercase()) {
                "super_admin" -> SUPER_ADMIN
                "franchise" -> FRANCHISE
                else -> AGENT
            }
        }
    }
}
