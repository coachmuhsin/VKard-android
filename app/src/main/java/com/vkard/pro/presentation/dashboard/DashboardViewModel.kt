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
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

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

    fun updateAgent(agentId: String, newName: String, newCredits: Int, newStatus: String) {
        viewModelScope.launch {
            try {
                supabase.postgrest["agents"].update({
                    set("name", newName)
                    set("credits_balance", newCredits)
                    set("status", newStatus)
                }) {
                    filter {
                        eq("id", agentId)
                    }
                }
                loadDashboard()
            } catch (e: Exception) {
                // Fail silently or log
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
}
