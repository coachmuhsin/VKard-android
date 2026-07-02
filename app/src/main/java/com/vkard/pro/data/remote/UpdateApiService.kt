package com.vkard.pro.data.remote

import com.vkard.pro.domain.model.VersionInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class UpdateApiService {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    suspend fun getLatestVersionInfo(): VersionInfo {
        return client.get("https://vkard.pro/download/version.json").body()
    }
}
