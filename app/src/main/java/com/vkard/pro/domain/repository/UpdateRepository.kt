package com.vkard.pro.domain.repository

import com.vkard.pro.domain.model.VersionInfo

interface UpdateRepository {
    suspend fun getLatestVersionInfo(forceRefresh: Boolean = false): Result<VersionInfo>
    fun getLastCheckedTime(): Long
    fun setLastCheckedTime(timestamp: Long)
    fun isUpdateDismissed(versionCode: Int): Boolean
    fun dismissUpdate(versionCode: Int)
}
