package com.vkard.pro.presentation.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vkard.pro.data.local.SecureSessionManager
import com.vkard.pro.data.remote.SupabaseClientProvider
import com.vkard.pro.domain.model.DigitalCardWithSub
import com.vkard.pro.domain.model.RevenueLedger
import com.vkard.pro.domain.model.KycSubmission
import com.vkard.pro.domain.repository.AuthRepository
import com.vkard.pro.domain.repository.CardRepository
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class CardStats(
    val total: Int = 0,
    val active: Int = 0,
    val expired: Int = 0,
    val draft: Int = 0
)

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class SuperAdminData(
        val franchiseCount: Int,
        val agentCount: Int,
        val cardStats: CardStats,
        val cards: List<DigitalCardWithSub>,
        val ledger: List<RevenueLedger>,
        val kycSubmissions: List<KycSubmission>
    ) : DashboardUiState
    
    data class FranchiseData(
        val name: String,
        val code: String,
        val credits: Int,
        val cardStats: CardStats,
        val cards: List<DigitalCardWithSub>,
        val agentNetwork: List<AgentUiModel>,
        val ledger: List<RevenueLedger>,
        val kycSubmissions: List<KycSubmission>
    ) : DashboardUiState

    
    data class AgentData(
        val name: String,
        val code: String,
        val credits: Int,
        val affiliation: String,
        val cardStats: CardStats,
        val cards: List<DigitalCardWithSub>,
        val ledger: List<RevenueLedger>
    ) : DashboardUiState
    
    data class Error(val message: String) : DashboardUiState
}

data class AgentUiModel(
    val id: String,
    val name: String,
    val code: String,
    val credits: Int,
    val status: String
)

class DashboardViewModel(
    private val authRepository: AuthRepository,
    private val cardRepository: CardRepository,
    val sessionManager: SecureSessionManager
) : ViewModel() {
    
    var uiState by mutableStateOf<DashboardUiState>(DashboardUiState.Loading)
        private set
        
    private val supabase = SupabaseClientProvider.client
    
    fun loadDashboard() {
        uiState = DashboardUiState.Loading
        val role = sessionManager.getRole() ?: "agent"
        
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: run {
                uiState = DashboardUiState.Error("Session expired.")
                return@launch
            }
            try {
                // Fetch all cards with nested subscriptions
                val allCards = supabase.postgrest["digital_cards"]
                    .select(columns = Columns.raw("*, subscriptions(*)"))
                    .decodeList<DigitalCardWithSub>()
                
                // Fetch ledger entries
                val ledger = cardRepository.getLedger(userId, role).getOrDefault(emptyList())
                
                when (role) {
                    "super_admin" -> {
                        val franchises = supabase.postgrest["franchises"].select().decodeList<JsonObject>()
                        val agents = supabase.postgrest["agents"].select().decodeList<JsonObject>()
                        
                        val filteredCards = allCards // Super admin sees all cards
                        val cardStats = calculateCardStats(filteredCards)
                        
                        val kycSubmissions = authRepository.getAllKycSubmissions().getOrDefault(emptyList())
                        
                        uiState = DashboardUiState.SuperAdminData(
                            franchiseCount = franchises.size,
                            agentCount = agents.size,
                            cardStats = cardStats,
                            cards = filteredCards,
                            ledger = ledger,
                            kycSubmissions = kycSubmissions
                        )
                    }
                    "franchise" -> {
                        val franchiseProfileList = supabase.postgrest["franchises"]
                            .select {
                                filter {
                                    eq("id", userId)
                                }
                            }
                            .decodeList<JsonObject>()
                        val franchiseProfile = franchiseProfileList.firstOrNull()
                        
                        val code = franchiseProfile?.get("franchise_code")?.jsonPrimitive?.content ?: "N/A"
                        val credits = franchiseProfile?.get("credits_balance")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                        val name = franchiseProfile?.get("name")?.jsonPrimitive?.content 
                            ?: sessionManager.getEmail()?.substringBefore("@")?.replaceFirstChar { it.uppercase() } 
                            ?: "Franchise"
                        
                        // Fetch child agents
                        val agentsList = supabase.postgrest["agents"]
                            .select {
                                filter {
                                    eq("franchise_id", userId)
                                }
                            }
                            .decodeList<JsonObject>()
                        
                        val agentNetwork = agentsList.map {
                            AgentUiModel(
                                id = it["id"]?.jsonPrimitive?.content ?: "",
                                name = it["name"]?.jsonPrimitive?.content ?: "Agent",
                                code = it["agent_code"]?.jsonPrimitive?.content ?: "",
                                credits = it["credits_balance"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                                status = it["status"]?.jsonPrimitive?.content ?: "active"
                            )
                        }
                        
                        // Filter: Franchise sees their own cards + cards created by their child agents
                        val agentIds = agentNetwork.map { it.id }.toSet()
                        val filteredCards = allCards.filter { it.created_by == userId || agentIds.contains(it.created_by) }
                        
                        // Stats calculated ONLY for self-created cards
                        val selfCreatedCards = allCards.filter { it.created_by == userId }
                        val cardStats = calculateCardStats(selfCreatedCards)
                        
                        val kycSubmissions = authRepository.getFranchiseAgentsKycSubmissions(userId).getOrDefault(emptyList())
                        
                        uiState = DashboardUiState.FranchiseData(
                            name = name,
                            code = code,
                            credits = credits,
                            cardStats = cardStats,
                            cards = filteredCards,
                            agentNetwork = agentNetwork,
                            ledger = ledger,
                            kycSubmissions = kycSubmissions
                        )
                    }
                    else -> { // agent
                        val agentProfileList = supabase.postgrest["agents"]
                            .select {
                                filter {
                                    eq("id", userId)
                                }
                            }
                            .decodeList<JsonObject>()
                        val agentProfile = agentProfileList.firstOrNull()
                        
                        val code = agentProfile?.get("agent_code")?.jsonPrimitive?.content ?: "N/A"
                        val credits = agentProfile?.get("credits_balance")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                        val name = agentProfile?.get("name")?.jsonPrimitive?.content 
                            ?: sessionManager.getEmail()?.substringBefore("@")?.replaceFirstChar { it.uppercase() } 
                            ?: "Agent"
                        
                        val franchiseId = agentProfile?.get("franchise_id")?.jsonPrimitive?.content
                        var affiliation = "Company Direct"
                        if (franchiseId != null) {
                            val franchiseList = supabase.postgrest["franchises"]
                                .select {
                                    filter {
                                        eq("id", franchiseId)
                                    }
                                }
                                .decodeList<JsonObject>()
                            affiliation = franchiseList.firstOrNull()?.get("name")?.jsonPrimitive?.content ?: "Franchise"
                        }
                        
                        // Filter: Agent sees only their own created cards
                        val filteredCards = allCards.filter { it.created_by == userId }
                        val cardStats = calculateCardStats(filteredCards)
                        
                        uiState = DashboardUiState.AgentData(
                            name = name,
                            code = code,
                            credits = credits,
                            affiliation = affiliation,
                            cardStats = cardStats,
                            cards = filteredCards,
                            ledger = ledger
                        )
                    }
                }
            } catch (e: Exception) {
                uiState = DashboardUiState.Error(e.message ?: "Failed to load dashboard.")
            }
        }
    }

    fun deleteCard(cardId: String) {
        viewModelScope.launch {
            cardRepository.deleteCard(cardId)
                .onSuccess {
                    loadDashboard()
                }
        }
    }

    fun updateAgent(
        agentId: String,
        newName: String,
        newCredits: Int,
        newStatus: String,
        onComplete: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val franchiseId = sessionManager.getUserId() ?: throw Exception("Unauthorized")
                
                // Perform the updates inside a single database transaction using the custom postgres RPC function
                val rpcResponse = supabase.postgrest.rpc(
                    function = "update_agent_card_balance",
                    parameters = buildJsonObject {
                        put("p_franchise_id", franchiseId)
                        put("p_agent_id", agentId)
                        put("p_new_credits", newCredits)
                        put("p_new_name", newName)
                        put("p_new_status", newStatus)
                    }
                )
                
                val responseJson = rpcResponse.decodeAs<JsonObject>()
                val errorMsg = responseJson["error"]?.jsonPrimitive?.content
                if (!errorMsg.isNullOrEmpty()) {
                    throw Exception(errorMsg)
                }
                
                loadDashboard()
                onComplete(Result.success(Unit))
            } catch (e: Exception) {
                // Log the technical error only to Logcat (Debug mode)
                if (com.vkard.pro.BuildConfig.DEBUG) {
                    android.util.Log.e("DashboardViewModel", "Database transaction failed", e)
                }
                
                val msg = e.message ?: ""
                val friendlyMessage = when {
                    msg.contains("http", ignoreCase = true) ||
                    msg.contains("jwt", ignoreCase = true) ||
                    msg.contains("bearer", ignoreCase = true) ||
                    msg.contains("apikey", ignoreCase = true) ||
                    msg.contains("token", ignoreCase = true) ||
                    msg.contains("postgrest", ignoreCase = true) ||
                    msg.contains("postgres", ignoreCase = true) ||
                    msg.contains("supabase", ignoreCase = true) ||
                    msg.contains("sql", ignoreCase = true) ||
                    msg.contains("headers", ignoreCase = true) -> {
                        "Something went wrong. Please try again later."
                    }
                    msg.contains("row-level security", ignoreCase = true) ||
                    msg.contains("violates row-level security", ignoreCase = true) ||
                    msg.contains("policy", ignoreCase = true) ||
                    msg.contains("permission denied", ignoreCase = true) ||
                    msg.contains("unauthorized", ignoreCase = true) ||
                    msg.contains("not authorized", ignoreCase = true) -> {
                        "Permission denied. You are not authorized to update this agent."
                    }
                    msg.contains("connect", ignoreCase = true) ||
                    msg.contains("network", ignoreCase = true) ||
                    msg.contains("timeout", ignoreCase = true) ||
                    msg.contains("host", ignoreCase = true) ||
                    e is java.net.ConnectException ||
                    e is java.net.UnknownHostException -> {
                        "Unable to connect. Please try again."
                    }
                    msg.contains("Insufficient", ignoreCase = true) || 
                    msg.contains("wallet", ignoreCase = true) ||
                    msg.contains("balance", ignoreCase = true) ||
                    msg.contains("credits", ignoreCase = true) -> {
                        msg
                    }
                    else -> {
                        "Something went wrong. Please try again later."
                    }
                }
                
                onComplete(Result.failure(Exception(friendlyMessage)))
            }
        }
    }

    fun updateKycStatus(
        submissionId: String,
        status: String,
        rejectionReason: String?,
        onComplete: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val adminId = sessionManager.getUserId() ?: throw Exception("Unauthorized")
                authRepository.updateKycStatus(submissionId, status, rejectionReason, adminId)
                    .onSuccess {
                        loadDashboard()
                        onComplete(Result.success(Unit))
                    }
                    .onFailure { throw it }
            } catch (e: Exception) {
                if (com.vkard.pro.BuildConfig.DEBUG) {
                    android.util.Log.e("DashboardViewModel", "Failed to update KYC status", e)
                }
                val msg = e.message ?: ""
                val friendlyMessage = when {
                    msg.contains("Permission", ignoreCase = true) || msg.contains("policy", ignoreCase = true) -> {
                        "Permission denied."
                    }
                    msg.contains("Connect", ignoreCase = true) || msg.contains("timeout", ignoreCase = true) || msg.contains("host", ignoreCase = true) -> {
                        "Internet connection unavailable."
                    }
                    else -> {
                        "Unable to save changes."
                    }
                }
                onComplete(Result.failure(Exception(friendlyMessage)))
            }
        }
    }

    fun createAgent(
        agentName: String,
        agentEmail: String,
        agentPassword: String,
        agentWhatsapp: String,
        initialCredits: Int,
        onComplete: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val franchiseId = sessionManager.getUserId() ?: throw Exception("Unauthorized")
                
                // Fetch latest franchise credits
                val franchiseResponse = supabase.postgrest["franchises"]
                    .select { filter { eq("id", franchiseId) } }
                    .decodeList<JsonObject>()
                val latestFranchiseCredits = franchiseResponse.firstOrNull()?.get("credits_balance")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                
                if (initialCredits > latestFranchiseCredits) {
                    throw Exception("Insufficient Wallet Credits")
                }
                
                // 1. Sign up the agent user auth
                val authResult = supabase.auth.signUpWith(Email) {
                    email = agentEmail
                    password = agentPassword
                    data = buildJsonObject {
                        put("role", "agent")
                        put("name", agentName)
                        put("credits_balance", 0)
                        put("franchise_id", franchiseId)
                        put("created_by", franchiseId)
                        put("whatsapp_number", agentWhatsapp)
                    }
                }
                
                val newAgentId = authResult?.id ?: throw Exception("Failed to create agent user")
                
                // 2. Fetch new agent from agents table to verify insertion trigger completed
                var retries = 5
                var newAgentVerified = false
                while (retries > 0 && !newAgentVerified) {
                    val checkAgent = supabase.postgrest["agents"]
                        .select { filter { eq("id", newAgentId) } }
                        .decodeList<JsonObject>()
                    if (checkAgent.isNotEmpty()) {
                        newAgentVerified = true
                    } else {
                        retries--
                        kotlinx.coroutines.delay(1000)
                    }
                }
                
                if (!newAgentVerified) {
                    throw Exception("Database trigger timed out. Agent profile not verified.")
                }
                
                // 3. If initial credits > 0, perform credit transfer using transfer_credits RPC
                if (initialCredits > 0) {
                    supabase.postgrest.rpc(
                        function = "transfer_credits",
                        parameters = buildJsonObject {
                            put("p_from_type", "franchise")
                            put("p_from_id", franchiseId)
                            put("p_to_type", "agent")
                            put("p_to_id", newAgentId)
                            put("p_credits", initialCredits)
                            put("p_remarks", "Initial credit allocation upon agent creation: $initialCredits credits.")
                            put("p_idempotency_key", "create-agent-credits-$newAgentId")
                        }
                    )
                }
                
                loadDashboard()
                onComplete(Result.success(Unit))
            } catch (e: Exception) {
                onComplete(Result.failure(e))
            }
        }
    }

    fun updateFranchiseProfile(franchiseId: String, newName: String) {
        viewModelScope.launch {
            try {
                supabase.postgrest["franchises"].update({
                    set("name", newName)
                }) {
                    filter {
                        eq("id", franchiseId)
                    }
                }
                loadDashboard()
            } catch (e: Exception) {
                // Fail silently or log
            }
        }
    }
    
    private fun calculateCardStats(cards: List<DigitalCardWithSub>): CardStats {
        var total = cards.size
        var active = 0
        var expired = 0
        var draft = 0
        
        cards.forEach { c ->
            if (c.status.lowercase() == "inactive" || c.status.lowercase() == "draft") {
                draft++
            } else if (c.status.lowercase() == "active") {
                val activeSub = c.subscriptions.firstOrNull { it.status.lowercase() == "active" }
                if (activeSub != null) {
                    active++
                } else {
                    expired++
                }
            } else {
                expired++
            }
        }
        return CardStats(total, active, expired, draft)
    }

    fun resetPassword(newPassword: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                supabase.auth.modifyUser {
                    password = newPassword
                }
                onResult(Result.success(Unit))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }
}
