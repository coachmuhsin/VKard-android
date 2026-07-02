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
        val ledger: List<RevenueLedger>
    ) : DashboardUiState
    
    data class FranchiseData(
        val name: String,
        val code: String,
        val credits: Int,
        val cardStats: CardStats,
        val cards: List<DigitalCardWithSub>,
        val agentNetwork: List<AgentUiModel>,
        val ledger: List<RevenueLedger>
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
                        
                        uiState = DashboardUiState.SuperAdminData(
                            franchiseCount = franchises.size,
                            agentCount = agents.size,
                            cardStats = cardStats,
                            cards = filteredCards,
                            ledger = ledger
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
                        val cardStats = calculateCardStats(filteredCards)
                        
                        uiState = DashboardUiState.FranchiseData(
                            name = name,
                            code = code,
                            credits = credits,
                            cardStats = cardStats,
                            cards = filteredCards,
                            agentNetwork = agentNetwork,
                            ledger = ledger
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
                
                // Fetch latest sender (franchise) credits
                val franchiseResponse = supabase.postgrest["franchises"]
                    .select { filter { eq("id", franchiseId) } }
                    .decodeList<JsonObject>()
                val latestFranchiseCredits = franchiseResponse.firstOrNull()?.get("credits_balance")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                
                // Fetch latest agent credits
                val agentResponse = supabase.postgrest["agents"]
                    .select { filter { eq("id", agentId) } }
                    .decodeList<JsonObject>()
                val latestAgentCredits = agentResponse.firstOrNull()?.get("credits_balance")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                
                if (newCredits > latestAgentCredits) {
                    // Credit Transfer
                    val transferAmount = newCredits - latestAgentCredits
                    if (latestFranchiseCredits < transferAmount) {
                        throw Exception("Insufficient Wallet Credits")
                    }
                    
                    // Deduct franchise credits
                    supabase.postgrest["franchises"].update({
                        set("credits_balance", latestFranchiseCredits - transferAmount)
                    }) {
                        filter { eq("id", franchiseId) }
                    }
                    
                    try {
                        // Add agent credits
                        supabase.postgrest["agents"].update({
                            set("credits_balance", newCredits)
                            set("name", newName)
                            set("status", newStatus)
                        }) {
                            filter { eq("id", agentId) }
                        }
                        
                        // Insert ledger entries
                        val senderLedger = buildJsonObject {
                            put("user_id", franchiseId)
                            put("transaction_type", "Credit Transfer")
                            put("credits_used", -transferAmount)
                            put("remarks", "Transferred to agent: $newName")
                        }
                        val receiverLedger = buildJsonObject {
                            put("user_id", agentId)
                            put("transaction_type", "Credit Received")
                            put("credits_used", transferAmount)
                            put("remarks", "Received from franchise")
                        }
                        supabase.postgrest["revenue_ledger"].insert(listOf(senderLedger, receiverLedger))
                        
                    } catch (e: Exception) {
                        // Rollback step 1: refund franchise
                        supabase.postgrest["franchises"].update({
                            set("credits_balance", latestFranchiseCredits)
                        }) {
                            filter { eq("id", franchiseId) }
                        }
                        throw e
                    }
                } else if (newCredits < latestAgentCredits) {
                    // Reclaim Credits
                    val transferAmount = latestAgentCredits - newCredits
                    if (latestAgentCredits < transferAmount) {
                        throw Exception("Agent has insufficient credits to reclaim")
                    }
                    
                    // Deduct agent credits
                    supabase.postgrest["agents"].update({
                        set("credits_balance", newCredits)
                        set("name", newName)
                        set("status", newStatus)
                    }) {
                        filter { eq("id", agentId) }
                    }
                    
                    try {
                        // Add franchise credits
                        supabase.postgrest["franchises"].update({
                            set("credits_balance", latestFranchiseCredits + transferAmount)
                        }) {
                            filter { eq("id", franchiseId) }
                        }
                        
                        // Insert ledger entries
                        val senderLedger = buildJsonObject {
                            put("user_id", agentId)
                            put("transaction_type", "Credit Returned")
                            put("credits_used", -transferAmount)
                            put("remarks", "Returned to franchise")
                        }
                        val receiverLedger = buildJsonObject {
                            put("user_id", franchiseId)
                            put("transaction_type", "Credit Reclaimed")
                            put("credits_used", transferAmount)
                            put("remarks", "Reclaimed from agent: $newName")
                        }
                        supabase.postgrest["revenue_ledger"].insert(listOf(senderLedger, receiverLedger))
                        
                    } catch (e: Exception) {
                        // Rollback step 1: refund agent
                        supabase.postgrest["agents"].update({
                            set("credits_balance", latestAgentCredits)
                        }) {
                            filter { eq("id", agentId) }
                        }
                        throw e
                    }
                } else {
                    // Update metadata only
                    supabase.postgrest["agents"].update({
                        set("name", newName)
                        set("status", newStatus)
                    }) {
                        filter { eq("id", agentId) }
                    }
                }
                
                loadDashboard()
                onComplete(Result.success(Unit))
            } catch (e: Exception) {
                onComplete(Result.failure(e))
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
