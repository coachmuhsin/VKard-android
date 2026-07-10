package com.vkard.pro.data.remote

object UuidValidator {
    fun isValidUuid(uuid: String?): Boolean {
        if (uuid.isNullOrBlank() || uuid == "null") return false
        return try {
            java.util.UUID.fromString(uuid)
            true
        } catch (e: Exception) {
            false
        }
    }
}
