package com.vkard.pro.data.repository

import com.vkard.pro.Config
import com.vkard.pro.data.local.SecureSessionManager
import com.vkard.pro.data.remote.SupabaseClientProvider
import com.vkard.pro.data.remote.UuidValidator
import com.vkard.pro.domain.model.DigitalCard
import com.vkard.pro.domain.model.RevenueLedger
import com.vkard.pro.domain.repository.CardRepository
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.add

class CardRepositoryImpl(
    private val sessionManager: SecureSessionManager
) : CardRepository {
    
    private val supabase = SupabaseClientProvider.client
    
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }
    
    override suspend fun getCards(userId: String, role: String): Result<List<DigitalCard>> {
        return runCatching {
            // RLS naturally restricts views based on authenticated role
            supabase.postgrest["digital_cards"]
                .select()
                .decodeList<DigitalCard>()
        }
    }
    
    override suspend fun getCardBySlug(slug: String): Result<DigitalCard?> {
        return runCatching {
            supabase.postgrest["digital_cards"]
                .select {
                    filter {
                        eq("slug", slug)
                    }
                }.decodeList<DigitalCard>()
                .firstOrNull()
        }
    }
    
    override suspend fun createCard(card: DigitalCard, creatorRole: String): Result<String> {
        return runCatching {
            val userId = sessionManager.getUserId() ?: throw Exception("Unauthorized")
            
            // Build parameters map matching the Postgres RPC function arguments
            val params = buildJsonObject {
                put("p_user_id", userId)
                put("p_user_role", creatorRole)
                put("p_slug", card.slug)
                put("p_full_name", card.full_name)
                put("p_designation", card.designation)
                put("p_company_name", card.company_name)
                put("p_mobile_number", card.mobile_number)
                put("p_whatsapp_number", card.whatsapp_number)
                put("p_email", card.email)
                put("p_address", card.address)
                put("p_company_description", card.company_description)
                put("p_logo_url", card.logo_url)
                put("p_banner_url", card.banner_url)
                put("p_website_url", card.website_url)
                put("p_facebook_url", card.facebook_url)
                put("p_instagram_url", card.instagram_url)
                put("p_linkedin_url", card.linkedin_url)
                put("p_youtube_url", card.youtube_url)
                put("p_google_maps_url", card.google_maps_url)
                put("p_google_review_url", card.google_review_url)
                put("p_upi_id", card.upi_id)
                put("p_upi_name", card.upi_name)
                put("p_upi_description", card.upi_description)
                put("p_bank_holder_name", card.bank_holder_name)
                put("p_bank_name", card.bank_name)
                put("p_bank_account_number", card.bank_account_number)
                put("p_bank_ifsc", card.bank_ifsc)
                put("p_bank_branch", card.bank_branch)
                put("p_gallery_images", buildJsonArray {
                    card.gallery_images.forEach { add(it) }
                })
                put("p_brochure_url", card.logo_url)  // Backwards compatibility placeholder
                put("p_theme_name", card.theme_name ?: "glass_purple")
                put("p_card_type", card.card_type ?: "individual")
                put("p_gst_number", card.gst_number)
                put("p_business_hours", card.business_hours)
                put("p_services", jsonArrayOf())
                put("p_products", jsonArrayOf())
                put("p_phone_numbers", jsonArrayOf())
                put("p_whatsapp_numbers", jsonArrayOf())
                put("p_emails", jsonArrayOf())
                put("p_websites", jsonArrayOf())
                put("p_locations", jsonArrayOf())
                put("p_social_links", jsonArrayOf())
                put("p_brochures", jsonArrayOf())
                put("p_content_display_mode", card.content_display_mode ?: "icons_text")
            }
            
            val response = supabase.postgrest.rpc(
                function = "create_digital_card_transaction",
                parameters = params
            )
            
            // Returns the slug as text
            response.data.trim('"')
        }
    }
    
    override suspend fun updateCard(card: DigitalCard): Result<Unit> {
        val cardId = card.id
        if (!UuidValidator.isValidUuid(cardId)) {
            return Result.failure(Exception("Missing or invalid card ID for update operation."))
        }
        return runCatching {
            supabase.postgrest["digital_cards"].update(card) {
                filter {
                    eq("id", cardId!!)
                }
            }
        }
    }
    
    override suspend fun deleteCard(cardId: String): Result<Unit> {
        if (!UuidValidator.isValidUuid(cardId)) {
            return Result.failure(Exception("Invalid card ID UUID."))
        }
        return runCatching {
            supabase.postgrest["digital_cards"].delete {
                filter {
                    eq("id", cardId)
                }
            }
        }
    }
    
    override suspend fun getLedger(userId: String, role: String): Result<List<RevenueLedger>> {
        return runCatching {
            // RLS automatically limits returned rows
            supabase.postgrest["revenue_ledger"]
                .select()
                .decodeList<RevenueLedger>()
                .sortedByDescending { it.created_at }
        }
    }
    
    override suspend fun uploadMedia(base64Image: String): Result<String> {
        return runCatching {
            val token = sessionManager.getAccessToken() ?: throw Exception("Unauthorized")
            val response = httpClient.post("${Config.BASE_URL}/api/gallery/upload") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("image", base64Image)
                })
            }
            
            val responseText = response.bodyAsText()
            if (response.status.value !in 200..299) {
                throw Exception("Upload failed: $responseText")
            }
            
            val jsonResponse = Json.decodeFromString<JsonObject>(responseText)
            jsonResponse["url"]?.jsonPrimitive?.content 
                ?: throw Exception("Invalid upload API response structure.")
        }
    }
    
    private fun jsonArrayOf() = buildJsonArray { }
}