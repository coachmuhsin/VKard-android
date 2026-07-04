package com.vkard.pro.data.repository

import android.content.Context
import com.vkard.pro.data.remote.UpdateApiService
import com.vkard.pro.domain.model.VersionInfo
import com.vkard.pro.domain.repository.UpdateRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class UpdateRepositoryImpl(
    context: Context,
    private val apiService: UpdateApiService
) : UpdateRepository {

    private val prefs = context.getSharedPreferences("vkard_update_prefs", Context.MODE_PRIVATE)

    override suspend fun getLatestVersionInfo(forceRefresh: Boolean): Result<VersionInfo> {
        val now = System.currentTimeMillis()
        val lastChecked = prefs.getLong("last_checked_time", 0L)
        val cachedJson = prefs.getString("cached_version_info", null)

        if (!forceRefresh && (now - lastChecked) < 600000L && cachedJson != null) {
            val cachedInfoResult = runCatching {
                Json.decodeFromString<VersionInfo>(cachedJson)
            }
            if (cachedInfoResult.isSuccess) {
                return cachedInfoResult
            }
        }

        val networkResult = runCatching {
            val releaseDto = apiService.getLatestGitHubRelease()
            val tagClean = releaseDto.tag_name.removePrefix("v").trim()
            
            val parsedVersionCode = try {
                val singleInt = tagClean.toIntOrNull()
                if (singleInt != null) {
                    singleInt
                } else {
                    val parts = tagClean.split(".")
                    var code = 0
                    if (parts.size >= 1) code += (parts[0].toIntOrNull() ?: 0) * 1000000
                    if (parts.size >= 2) code += (parts[1].toIntOrNull() ?: 0) * 1000
                    if (parts.size >= 3) code += (parts[2].toIntOrNull() ?: 0)
                    code
                }
            } catch (e: Exception) {
                0
            }

            val apkAsset = releaseDto.assets.find { it.name.endsWith(".apk", ignoreCase = true) }
            val apkUrl = apkAsset?.browser_download_url ?: ""
            val apkName = apkAsset?.name ?: "N/A"

            val bodyText = releaseDto.body ?: ""
            val changesList = bodyText.split("\n", "\r")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { line ->
                    if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*")) {
                        line.substring(1).trim()
                    } else {
                        line
                    }
                }

            val isForce = bodyText.contains("force", ignoreCase = true) || 
                          bodyText.contains("critical", ignoreCase = true) ||
                          (releaseDto.name ?: "").contains("force", ignoreCase = true)

            val prettyDate = parsePublishedDate(releaseDto.published_at)

            val info = VersionInfo(
                versionCode = parsedVersionCode,
                versionName = "Release $parsedVersionCode",
                releaseDate = prettyDate,
                apk = apkUrl,
                forceUpdate = isForce,
                changes = changesList,
                tagName = releaseDto.tag_name,
                releaseName = releaseDto.name ?: "N/A",
                publishedAtRaw = releaseDto.published_at,
                apkAssetName = apkName
            )

            prefs.edit().apply {
                putString("cached_version_info", Json.encodeToString(info))
                putLong("last_checked_time", now)
                apply()
            }
            info
        }

        if (networkResult.isSuccess) {
            return networkResult
        } else {
            if (cachedJson != null) {
                val cachedInfoResult = runCatching {
                    Json.decodeFromString<VersionInfo>(cachedJson)
                }
                if (cachedInfoResult.isSuccess) {
                    return cachedInfoResult
                }
            }
            return networkResult
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

    override fun clearCache() {
        prefs.edit().apply {
            remove("cached_version_info")
            remove("last_checked_time")
            remove("dismissed_version_code")
            apply()
        }
    }

    private fun parsePublishedDate(isoString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(isoString) ?: return isoString
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            outputFormat.format(date)
        } catch (e: Exception) {
            isoString
        }
    }
}
