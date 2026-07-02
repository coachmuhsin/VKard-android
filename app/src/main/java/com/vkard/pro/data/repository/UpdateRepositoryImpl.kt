package com.vkard.pro.data.repository

import android.content.Context
import com.vkard.pro.data.remote.UpdateApiService
import com.vkard.pro.domain.model.VersionInfo
import com.vkard.pro.domain.repository.UpdateRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class UpdateRepositoryImpl(
    context: Context,
    private val apiService: UpdateApiService
) : UpdateRepository {

    private val prefs = context.getSharedPreferences("vkard_update_prefs", Context.MODE_PRIVATE)

    override suspend fun getLatestVersionInfo(forceRefresh: Boolean): Result<VersionInfo> {
        val now = System.currentTimeMillis()
        val lastChecked = getLastCheckedTime()
        val cachedJson = prefs.getString("cached_version_info", null)

        if (!forceRefresh && cachedJson != null && (now - lastChecked < 6 * 60 * 60 * 1000)) {
            return runCatching {
                Json.decodeFromString<VersionInfo>(cachedJson)
            }
        }

        return runCatching {
            val info = apiService.getLatestVersionInfo()
            prefs.edit().apply {
                putString("cached_version_info", Json.encodeToString(info))
                putLong("last_checked_time", now)
                apply()
            }
            info
        }
    }

    override fun getLastCheckedTime(): Long {
        return prefs.getLong("last_checked_time", 0L)
    }

    override fun setLastCheckedTime(timestamp: Long) {
        prefs.edit().putLong("last_checked_time", timestamp).apply()
    }

    override fun isUpdateDismissed(versionCode: Int): Boolean {
        return prefs.getInt("dismissed_version_code", -1) == versionCode
    }

    override fun dismissUpdate(versionCode: Int) {
        prefs.edit().putInt("dismissed_version_code", versionCode).apply()
    }
}
