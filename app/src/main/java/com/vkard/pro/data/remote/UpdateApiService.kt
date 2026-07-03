package com.vkard.pro.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GitHubReleaseDto(
    val tag_name: String,
    val name: String? = null,
    val published_at: String,
    val body: String? = null,
    val assets: List<GitHubAssetDto> = emptyList()
)

@Serializable
data class GitHubAssetDto(
    val name: String,
    val browser_download_url: String,
    val size: Long
)

class UpdateApiService {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    suspend fun getLatestGitHubRelease(): GitHubReleaseDto {
        return client.get("https://api.github.com/repos/coachmuhsin/VKard-android/releases/latest").body()
    }
}
