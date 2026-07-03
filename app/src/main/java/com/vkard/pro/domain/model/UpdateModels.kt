package com.vkard.pro.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val releaseDate: String,
    val apk: String,
    val forceUpdate: Boolean,
    val changes: List<String>
)
