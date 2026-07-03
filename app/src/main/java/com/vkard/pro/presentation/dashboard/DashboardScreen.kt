package com.vkard.pro.presentation.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.vkard.pro.BuildConfig
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vkard.pro.domain.model.DigitalCardWithSub
import com.vkard.pro.domain.model.RevenueLedger
import com.vkard.pro.presentation.theme.PoppinsFontFamily

// Brand Colors
private val BrandPrimary = Color(0xFF077DF7)
private val BrandSecondary = Color(0xFF2A5D93)
private val BrandBackground = Color(0xFFFFFFFF)
private val BrandLightSurface = Color(0xFFF8FAFD)
private val BrandBorder = Color(0xFFE5EAF2)
private val BrandText = Color(0xFF000102)
private val BrandSuccess = Color(0xFF34C759)
private val BrandWarning = Color(0xFFFF9500)
private val BrandError = Color(0xFFFF3B30)

// Helper Navigation Item Data
data class TabItem(val id: String, val label: String, val icon: ImageVector)
data class QuickActionItem(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    updateViewModel: com.vkard.pro.presentation.update.UpdateViewModel,
    role: String,
    onCreateCard: (String?) -> Unit,
    onManageCustomers: () -> Unit,
    onShareCard: (String) -> Unit,
    onLogout: () -> Unit
) {
    val uiState = viewModel.uiState
    var selectedTab by rememberSaveable { mutableStateOf("home") }

    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
        updateViewModel.checkForUpdates(forceRefresh = false)
    }

    Scaffold(
        containerColor = BrandBackground,
        bottomBar = {
            FloatingBottomNavigation(
                currentTab = selectedTab,
                onTabSelected = { tabId -> selectedTab = tabId },
                role = role
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BrandBackground)
        ) {
            if (updateViewModel.showUpdateBanner) {
                com.vkard.pro.presentation.update.UpdateBanner(
                    versionInfo = updateViewModel.latestVersionInfo,
                    onDismiss = { updateViewModel.dismissUpdate() },
                    onUpdateClick = { updateViewModel.startDownload() }
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BrandBackground)
            ) {
                when (uiState) {
                is DashboardUiState.Loading -> {
                    DashboardSkeletonLoader()
                }
                is DashboardUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = BrandError, modifier = Modifier.size(48.dp))
                            Text(text = uiState.message, color = BrandError, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, fontFamily = PoppinsFontFamily)
                            Button(
                                onClick = { viewModel.loadDashboard() },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                            ) {
                                Text("Retry", color = Color.White, fontFamily = PoppinsFontFamily)
                            }
                        }
                    }
                }
                is DashboardUiState.SuperAdminData -> {
                    SuperAdminView(
                        data = uiState,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        onShareCard = onShareCard,
                        onCreateCard = onCreateCard,
                        onManageCustomers = onManageCustomers,
                        onLogout = onLogout,
                        onRefresh = {
                            viewModel.loadDashboard()
                            updateViewModel.checkForUpdates(forceRefresh = true)
                        },
                        onDeleteCard = { viewModel.deleteCard(it) },
                        viewModel = viewModel,
                        updateViewModel = updateViewModel
                    )
                }
                is DashboardUiState.FranchiseData -> {
                    FranchiseView(
                        data = uiState,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        onShareCard = onShareCard,
                        onCreateCard = onCreateCard,
                        onManageCustomers = onManageCustomers,
                        onLogout = onLogout,
                        onRefresh = {
                            viewModel.loadDashboard()
                            updateViewModel.checkForUpdates(forceRefresh = true)
                        },
                        onDeleteCard = { viewModel.deleteCard(it) },
                        onUpdateAgent = { agentId, newName, newCredits, newStatus, onComplete ->
                            viewModel.updateAgent(agentId, newName, newCredits, newStatus, onComplete)
                        },
                        onUpdateProfile = { newName ->
                            val userId = viewModel.sessionManager.getUserId() ?: ""
                            viewModel.updateFranchiseProfile(userId, newName)
                        },
                        viewModel = viewModel,
                        updateViewModel = updateViewModel
                    )
                }
                is DashboardUiState.AgentData -> {
                    AgentView(
                        data = uiState,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        onShareCard = onShareCard,
                        onCreateCard = onCreateCard,
                        onManageCustomers = onManageCustomers,
                        onLogout = onLogout,
                        onRefresh = {
                            viewModel.loadDashboard()
                            updateViewModel.checkForUpdates(forceRefresh = true)
                        },
                        onDeleteCard = { viewModel.deleteCard(it) },
                        viewModel = viewModel,
                        updateViewModel = updateViewModel
                    )
                }
            }
        }
    }
}
}

// ----------------------------------------------------
// UI Views per Role
// ----------------------------------------------------

@Composable
fun SuperAdminView(
    data: DashboardUiState.SuperAdminData,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onShareCard: (String) -> Unit,
    onCreateCard: (String?) -> Unit,
    onManageCustomers: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
    onDeleteCard: (String) -> Unit,
    viewModel: DashboardViewModel,
    updateViewModel: com.vkard.pro.presentation.update.UpdateViewModel
) {
    when (selectedTab) {
        "home" -> {
            val userId = viewModel.sessionManager.getUserId() ?: ""
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
            ) {
                item {
                    DashboardHeaderBlock(
                        name = "Super Admin",
                        roleLabel = "Super Admin",
                        credits = "Unlimited",
                        agentCount = data.agentCount,
                        onRefresh = onRefresh
                    )
                }

                item {
                    CardStatsBlock(stats = data.cardStats)
                }

                // Latest Visiting Cards
                item {
                    val latestCards = data.cards.filter { it.created_by == userId }
                        .sortedByDescending { it.created_at ?: "" }
                        .take(3)
                    LatestVisitingCardsSection(
                        latestCards = latestCards,
                        onCreateCard = onCreateCard,
                        onViewAll = { onTabSelected("cards") },
                        onShareCard = onShareCard
                    )
                }
            }
        }
        "cards" -> {
            VisitingCardsListTab(cards = data.cards, onShareCard = onShareCard, onCreateCard = onCreateCard, onDeleteCard = onDeleteCard, onRefresh = onRefresh)
        }
        "customers" -> {
            CustomersManagementTab(onManageCustomers = onManageCustomers)
        }
        "agents" -> {
            AgentsListTab(agents = emptyList(), allCards = data.cards, isSuperAdmin = true, onUpdateAgent = { _, _, _, _, _ -> }, onCreateAgent = null, onRefresh = onRefresh)
        }
        "support" -> {
            SupportTab(userName = "Super Admin", userRole = "Super Admin", userCode = "N/A", userEmail = "admin@vkard.pro")
        }
        "profile" -> {
            ProfileOptionsTab(
                name = "Super Admin",
                email = "admin@vkard.pro",
                role = "Super Admin",
                code = "N/A",
                credits = "Unlimited",
                activeCards = data.cardStats.active,
                createdDate = "Member since 2024",
                ledger = data.ledger,
                onLogout = onLogout,
                onEditProfile = {},
                viewModel = viewModel,
                updateViewModel = updateViewModel
            )
        }
    }
}

@Composable
fun FranchiseView(
    data: DashboardUiState.FranchiseData,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onShareCard: (String) -> Unit,
    onCreateCard: (String?) -> Unit,
    onManageCustomers: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
    onDeleteCard: (String) -> Unit,
    onUpdateAgent: (agentId: String, newName: String, newCredits: Int, newStatus: String, onComplete: (Result<Unit>) -> Unit) -> Unit,
    onUpdateProfile: (String) -> Unit,
    viewModel: DashboardViewModel,
    updateViewModel: com.vkard.pro.presentation.update.UpdateViewModel
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }
    val userId = viewModel.sessionManager.getUserId() ?: ""

    when (selectedTab) {
        "home" -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
            ) {
                item {
                    DashboardHeaderBlock(
                        name = data.name,
                        roleLabel = "Franchise",
                        credits = data.credits.toString(),
                        agentCount = data.agentNetwork.size,
                        onRefresh = onRefresh
                    )
                }

                item {
                    CardStatsBlock(stats = data.cardStats)
                }

                // Latest Visiting Cards
                item {
                    val latestCards = data.cards.filter { it.created_by == userId }
                        .sortedByDescending { it.created_at ?: "" }
                        .take(3)
                    LatestVisitingCardsSection(
                        latestCards = latestCards,
                        onCreateCard = onCreateCard,
                        onViewAll = { onTabSelected("cards") },
                        onShareCard = onShareCard
                    )
                }
            }
        }
        "cards" -> {
            VisitingCardsListTab(cards = data.cards, onShareCard = onShareCard, onCreateCard = onCreateCard, onDeleteCard = onDeleteCard, onRefresh = onRefresh)
        }
        "agents" -> {
            var showCreateAgentDialog by remember { mutableStateOf(false) }
            AgentsListTab(
                agents = data.agentNetwork,
                allCards = data.cards,
                isSuperAdmin = false,
                onUpdateAgent = onUpdateAgent,
                onCreateAgent = { showCreateAgentDialog = true },
                onRefresh = onRefresh
            )
            if (showCreateAgentDialog) {
                CreateAgentDialog(
                    franchiseCredits = data.credits,
                    onDismiss = { showCreateAgentDialog = false },
                    onCreate = { name, email, pass, whatsapp, credits, onComplete ->
                        viewModel.createAgent(name, email, pass, whatsapp, credits, onComplete)
                    }
                )
            }
        }
        "support" -> {
            SupportTab(userName = data.name, userRole = "Franchise Partner", userCode = data.code, userEmail = "Code: " + data.code)
        }
        "profile" -> {
            ProfileOptionsTab(
                name = data.name,
                email = "Franchise Code: " + data.code,
                role = "Franchise Partner",
                code = data.code,
                credits = data.credits.toString(),
                activeCards = data.cardStats.active,
                createdDate = "Member since 2024",
                ledger = data.ledger,
                onLogout = onLogout,
                onEditProfile = { showEditProfileDialog = true },
                viewModel = viewModel,
                updateViewModel = updateViewModel
            )
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = data.name,
            onDismiss = { showEditProfileDialog = false },
            onSave = {
                onUpdateProfile(it)
                showEditProfileDialog = false
            }
        )
    }
}

@Composable
fun AgentView(
    data: DashboardUiState.AgentData,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onShareCard: (String) -> Unit,
    onCreateCard: (String?) -> Unit,
    onManageCustomers: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
    onDeleteCard: (String) -> Unit,
    viewModel: DashboardViewModel,
    updateViewModel: com.vkard.pro.presentation.update.UpdateViewModel
) {
    val userId = viewModel.sessionManager.getUserId() ?: ""
    when (selectedTab) {
        "home" -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
            ) {
                item {
                    DashboardHeaderBlock(
                        name = data.name,
                        roleLabel = "Agent",
                        credits = data.credits.toString(),
                        agentCount = null,
                        onRefresh = onRefresh
                    )
                }

                item {
                    CardStatsBlock(stats = data.cardStats)
                }

                // Latest Visiting Cards
                item {
                    val latestCards = data.cards.filter { it.created_by == userId }
                        .sortedByDescending { it.created_at ?: "" }
                        .take(3)
                    LatestVisitingCardsSection(
                        latestCards = latestCards,
                        onCreateCard = onCreateCard,
                        onViewAll = { onTabSelected("cards") },
                        onShareCard = onShareCard
                    )
                }
            }
        }
        "cards" -> {
            VisitingCardsListTab(cards = data.cards, onShareCard = onShareCard, onCreateCard = onCreateCard, onDeleteCard = onDeleteCard, onRefresh = onRefresh)
        }
        "support" -> {
            SupportTab(userName = data.name, userRole = "Sales Agent", userCode = data.code, userEmail = "Agent ID: " + data.code)
        }
        "profile" -> {
            ProfileOptionsTab(
                name = data.name,
                email = "Agent ID: " + data.code,
                role = "Sales Agent",
                code = data.code,
                credits = data.credits.toString(),
                activeCards = data.cardStats.active,
                createdDate = "Member since 2024",
                ledger = data.ledger,
                onLogout = onLogout,
                onEditProfile = {},
                viewModel = viewModel,
                updateViewModel = updateViewModel
            )
        }
    }
}

// ----------------------------------------------------
// Custom UI Components & Subsections
// ----------------------------------------------------

@Composable
fun DashboardHeaderBlock(
    name: String,
    roleLabel: String,
    credits: String,
    agentCount: Int?,
    onRefresh: () -> Unit
) {
    val calendarHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (calendarHour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp), ambientColor = BrandPrimary.copy(alpha = 0.05f), spotColor = BrandPrimary.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(BrandPrimary.copy(alpha = 0.06f), Color.White)
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(BrandPrimary, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name.take(1).uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = PoppinsFontFamily
                        )
                    }

                    Column {
                        Text(
                            text = greeting,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium,
                            fontFamily = PoppinsFontFamily
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandText,
                                fontFamily = PoppinsFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .background(BrandPrimary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = roleLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPrimary,
                                    fontFamily = PoppinsFontFamily,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .background(BrandLightSurface, RoundedCornerShape(12.dp))
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = BrandPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = BrandBorder.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Wallet Credits Info
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(BrandLightSurface, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                    Column {
                        Text("Wallet Balance", fontSize = 10.sp, color = Color(0xFF64748B), fontFamily = PoppinsFontFamily)
                        Text("$credits Kards", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandText, fontFamily = PoppinsFontFamily)
                    }
                }

                if (agentCount != null) {
                    // Agent Network Count
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(BrandLightSurface, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = BrandSecondary, modifier = Modifier.size(16.dp))
                        Column {
                            Text("Agent Network", fontSize = 10.sp, color = Color(0xFF64748B), fontFamily = PoppinsFontFamily)
                            Text("$agentCount Agents", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandText, fontFamily = PoppinsFontFamily)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardStatsBlock(
    stats: CardStats
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0x0F000000),
                spotColor = Color(0x0F000000)
            )
            .border(1.dp, BrandBorder, RoundedCornerShape(24.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Visiting Card Stats", fontSize = 14.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily)
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatPillItem("Total", stats.total.toString(), BrandText)
                StatPillItem("Active", stats.active.toString(), BrandSuccess)
                StatPillItem("Draft", stats.draft.toString(), BrandWarning)
                StatPillItem("Expired", stats.expired.toString(), BrandError)
            }
        }
    }
}

@Composable
fun QuickActionsSection(
    actions: List<QuickActionItem>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Quick Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrandText, fontFamily = PoppinsFontFamily)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            actions.take(3).forEach { action ->
                QuickActionGridCard(action = action, modifier = Modifier.weight(1f))
            }
            repeat(3 - actions.take(3).size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        
        if (actions.size > 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                actions.drop(3).forEach { action ->
                    QuickActionGridCard(action = action, modifier = Modifier.weight(1f))
                }
                repeat(3 - actions.drop(3).size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun QuickActionGridCard(action: QuickActionItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .clickable { action.onClick() }
            .shadow(1.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = BrandPrimary.copy(alpha = 0.02f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .border(1.dp, BrandBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = action.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BrandText,
                textAlign = TextAlign.Center,
                maxLines = 2,
                fontFamily = PoppinsFontFamily,
                lineHeight = 13.sp
            )
        }
    }
}

// ----------------------------------------------------
// Tab Content Pages
// ----------------------------------------------------

@Composable
fun VisitingCardsListTab(
    cards: List<DigitalCardWithSub>,
    onShareCard: (String) -> Unit,
    onCreateCard: (String?) -> Unit,
    onDeleteCard: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val filteredCards = remember(searchQuery, cards) {
        if (searchQuery.isBlank()) cards else {
            cards.filter {
                it.full_name.contains(searchQuery, ignoreCase = true) ||
                it.company_name.contains(searchQuery, ignoreCase = true) ||
                it.designation.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // + Create VKARD Button
            Button(
                onClick = { onCreateCard(null) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = BrandPrimary.copy(alpha = 0.4f),
                        spotColor = BrandPrimary.copy(alpha = 0.4f)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(BrandPrimary, BrandSecondary)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Text(
                            text = "Create VKARD",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PoppinsFontFamily
                        )
                    }
                }
            }

            // Refresh Button (48dp circle, light background, blue icon)
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .background(BrandLightSurface, androidx.compose.foundation.shape.CircleShape)
                    .size(48.dp)
                    .border(1.dp, BrandBorder, androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = BrandPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search cards...", color = Color(0xFF9CA3AF), fontFamily = PoppinsFontFamily, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = BrandLightSurface,
                unfocusedContainerColor = BrandLightSurface,
                focusedBorderColor = BrandPrimary,
                unfocusedBorderColor = BrandBorder,
                focusedTextColor = BrandText,
                unfocusedTextColor = BrandText
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            items(filteredCards) { card ->
                val statusColor = when (card.status.lowercase()) {
                    "active" -> BrandSuccess
                    "draft", "inactive" -> BrandWarning
                    else -> BrandError
                }

                var showMenu by remember { mutableStateOf(false) }

                AnimateCardEnter {
                    Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x0A000000), spotColor = Color(0x0A000000))
                        .border(1.dp, BrandBorder, RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Profile photo (Coil loader) aligned perfectly
                        CardAvatar(logoUrl = card.logo_url, fullName = card.full_name, size = 56.dp)

                        // Middle: Name, Business Details, Expiry, Status
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = card.full_name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandText,
                                    fontFamily = PoppinsFontFamily
                                )
                                Box(
                                    modifier = Modifier
                                        .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = card.status.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor,
                                        fontFamily = PoppinsFontFamily
                                    )
                                }
                            }
                            Text(
                                text = "${card.designation} at ${card.company_name}",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                fontFamily = PoppinsFontFamily
                            )
                            if (card.subscriptions.isNotEmpty()) {
                                val sub = card.subscriptions.first()
                                Text(
                                    text = "Expires: ${sub.end_date.substringBefore("T")}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    fontFamily = PoppinsFontFamily
                                )
                            }
                        }

                        // Right: Overflow Actions
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Actions", tint = Color(0xFF64748B))
                            }
                            
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("View Online", fontFamily = PoppinsFontFamily, fontSize = 14.sp) },
                                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showMenu = false
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://vkard.pro/" + card.slug))
                                        context.startActivity(intent)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit Card", fontFamily = PoppinsFontFamily, fontSize = 14.sp) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showMenu = false
                                        onCreateCard(card.slug)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("QR Code", fontFamily = PoppinsFontFamily, fontSize = 14.sp) },
                                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showMenu = false
                                        onShareCard(card.slug)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Copy Link", fontFamily = PoppinsFontFamily, fontSize = 14.sp) },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showMenu = false
                                        clipboardManager.setText(AnnotatedString("https://vkard.pro/" + card.slug))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete", color = BrandError, fontFamily = PoppinsFontFamily, fontSize = 14.sp) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = BrandError, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showMenu = false
                                        onDeleteCard(card.id ?: "")
                                    }
                                )
                            }
                        }
                    }
                }
                }
            }

            if (filteredCards.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No digital cards match search query.", color = Color(0xFF64748B), fontSize = 14.sp, fontFamily = PoppinsFontFamily)
                    }
                }
            }
        }
    }
}

@Composable
fun AgentsListTab(
    agents: List<AgentUiModel>,
    allCards: List<DigitalCardWithSub>,
    isSuperAdmin: Boolean,
    onUpdateAgent: (agentId: String, name: String, credits: Int, status: String, onComplete: (Result<Unit>) -> Unit) -> Unit,
    onCreateAgent: (() -> Unit)?,
    onRefresh: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedAgentForEdit by remember { mutableStateOf<AgentUiModel?>(null) }

    val filteredAgents = remember(searchQuery, agents) {
        if (searchQuery.isBlank()) agents else {
            agents.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.code.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onCreateAgent != null) {
                // + Create Agent Button
                Button(
                    onClick = onCreateAgent,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = BrandPrimary.copy(alpha = 0.4f),
                            spotColor = BrandPrimary.copy(alpha = 0.4f)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(BrandPrimary, BrandSecondary)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Text(
                                text = "Create Agent",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PoppinsFontFamily
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Refresh Button (48dp circle, light background, blue icon)
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .background(BrandLightSurface, androidx.compose.foundation.shape.CircleShape)
                    .size(48.dp)
                    .border(1.dp, BrandBorder, androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = BrandPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (isSuperAdmin) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Centralized sales agents registry is active.", color = Color(0xFF64748B), fontSize = 14.sp, fontFamily = PoppinsFontFamily)
            }
        } else {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search agents...", color = Color(0xFF9CA3AF), fontFamily = PoppinsFontFamily, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = BrandLightSurface,
                    unfocusedContainerColor = BrandLightSurface,
                    focusedBorderColor = BrandPrimary,
                    unfocusedBorderColor = BrandBorder,
                    focusedTextColor = BrandText,
                    unfocusedTextColor = BrandText
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(filteredAgents) { agent ->
                    val totalCards = allCards.count { it.created_by == agent.id }
                    val activeCards = allCards.count { it.created_by == agent.id && it.status.lowercase() == "active" }
                    val isStateActive = agent.status == "active"

                    AnimateCardEnter {
                        Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedAgentForEdit = agent }
                            .shadow(1.dp, RoundedCornerShape(20.dp))
                            .border(1.dp, BrandBorder, RoundedCornerShape(20.dp)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(BrandLightSurface, RoundedCornerShape(22.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(agent.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = BrandPrimary, fontSize = 16.sp, fontFamily = PoppinsFontFamily)
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(agent.name, fontWeight = FontWeight.Bold, color = BrandText, fontSize = 15.sp, fontFamily = PoppinsFontFamily)
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    (if (isStateActive) BrandSuccess else BrandError).copy(alpha = 0.1f),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isStateActive) "ACTIVE" else "BLOCKED",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isStateActive) BrandSuccess else BrandError,
                                                fontFamily = PoppinsFontFamily
                                            )
                                        }
                                    }
                                    Text("Code: ${agent.code} | Cards: $activeCards / $totalCards", color = Color(0xFF64748B), fontSize = 12.sp, fontFamily = PoppinsFontFamily)
                                }
                            }
                            Text("${agent.credits} Kards", fontWeight = FontWeight.Bold, color = BrandPrimary, fontSize = 15.sp, fontFamily = PoppinsFontFamily)
                        }
                    }
                    }
                }

                if (filteredAgents.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No registered franchise agents found.", color = Color(0xFF64748B), fontSize = 14.sp, fontFamily = PoppinsFontFamily)
                        }
                    }
                }
            }
        }
    }

    if (selectedAgentForEdit != null) {
        val agent = selectedAgentForEdit!!
        val totalCards = allCards.count { it.created_by == agent.id }
        
        EditAgentDialog(
            agent = agent,
            assignedCardsCount = totalCards,
            onDismiss = { selectedAgentForEdit = null },
            onSave = { newName, newCredits, newStatus, onComplete ->
                onUpdateAgent(agent.id, newName, newCredits, newStatus, onComplete)
            }
        )
    }
}

@Composable
fun EditAgentDialog(
    agent: AgentUiModel,
    assignedCardsCount: Int,
    onDismiss: () -> Unit,
    onSave: (newName: String, newCredits: Int, newStatus: String, onComplete: (Result<Unit>) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf(agent.name) }
    var credits by remember { mutableStateOf(agent.credits.toString()) }
    var status by remember { mutableStateOf(agent.status) }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Agent Profile", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (errorMessage != null) {
                    Text(errorMessage!!, color = BrandError, fontSize = 12.sp, fontFamily = PoppinsFontFamily)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Agent Name", fontFamily = PoppinsFontFamily) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = credits,
                    onValueChange = { credits = it },
                    label = { Text("Wallet Credits Balance", fontFamily = PoppinsFontFamily) },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Agent Active Status", fontWeight = FontWeight.Medium, fontFamily = PoppinsFontFamily)
                    Switch(
                        checked = status == "active",
                        onCheckedChange = { status = if (it) "active" else "inactive" }
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BrandLightSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Total Created Cards: $assignedCardsCount",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = BrandPrimary,
                            fontFamily = PoppinsFontFamily
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isSaving) {
                CircularProgressIndicator(color = BrandPrimary, modifier = Modifier.size(24.dp))
            } else {
                Button(
                    onClick = {
                        val targetCredits = credits.toIntOrNull()
                        if (targetCredits == null) {
                            errorMessage = "Please enter a valid credit number."
                            return@Button
                        }
                        isSaving = true
                        errorMessage = null
                        onSave(name, targetCredits, status) { result ->
                            isSaving = false
                            result.onSuccess {
                                onDismiss()
                            }.onFailure {
                                errorMessage = it.message ?: "Failed to save agent details."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("SAVE", fontFamily = PoppinsFontFamily)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", fontFamily = PoppinsFontFamily, color = Color(0xFF64748B))
            }
        }
    )
}

@Composable
fun EditProfileDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (newName: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile Details", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name", fontFamily = PoppinsFontFamily) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name) },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("SAVE", fontFamily = PoppinsFontFamily)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color(0xFF64748B), fontFamily = PoppinsFontFamily)
            }
        }
    )
}

@Composable
fun SupportTab(
    userName: String,
    userRole: String,
    userCode: String,
    userEmail: String
) {
    var selectedIssue by remember { mutableStateOf("Login Problem") }
    var description by remember { mutableStateOf("") }
    var expandedIssueDropdown by remember { mutableStateOf(false) }

    val context = LocalContext.current
    
    val issuesList = listOf(
        "Login Problem",
        "Card Creation Issue",
        "QR Code Issue",
        "Image Upload Issue",
        "Subscription / Renewal",
        "Payment Issue",
        "Agent Issue",
        "Feature Request",
        "Technical Issue",
        "Other"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 120.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        description = ""
                        selectedIssue = "Login Problem"
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Text(
                            text = "New Ticket",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                // Refresh Button (48dp circle, light background, blue icon)
                IconButton(
                    onClick = {
                        description = ""
                        selectedIssue = "Login Problem"
                    },
                    modifier = Modifier
                        .background(BrandLightSurface, androidx.compose.foundation.shape.CircleShape)
                        .size(48.dp)
                        .border(1.dp, BrandBorder, androidx.compose.foundation.shape.CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = BrandPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        item {
            Text(
                text = "Submit a support inquiry. This will launch WhatsApp directly.",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                fontFamily = PoppinsFontFamily
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x0A000000), spotColor = Color(0x0A000000))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Common Issue",
                            fontSize = 13.sp,
                            color = Color(0xFF4B5563),
                            fontWeight = FontWeight.Medium,
                            fontFamily = PoppinsFontFamily
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .background(BrandLightSurface, RoundedCornerShape(18.dp))
                                    .border(1.dp, if (expandedIssueDropdown) BrandPrimary else BrandBorder, RoundedCornerShape(18.dp))
                                    .clickable { expandedIssueDropdown = true }
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedIssue,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = BrandText,
                                    fontFamily = PoppinsFontFamily
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.rotate(if (expandedIssueDropdown) 180f else 0f)
                                )
                            }

                            DropdownMenu(
                                expanded = expandedIssueDropdown,
                                onDismissRequest = { expandedIssueDropdown = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(Color.White, RoundedCornerShape(16.dp))
                                    .border(1.dp, BrandBorder, RoundedCornerShape(16.dp))
                            ) {
                                issuesList.forEach { issue ->
                                    val isSelected = selectedIssue == issue
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = issue,
                                                    color = if (isSelected) BrandPrimary else BrandText,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 14.sp,
                                                    fontFamily = PoppinsFontFamily
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = BrandPrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedIssue = issue
                                            expandedIssueDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Issue Description / Message",
                            fontSize = 13.sp,
                            color = Color(0xFF4B5563),
                            fontWeight = FontWeight.Medium,
                            fontFamily = PoppinsFontFamily
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("Describe the issue or feature request in detail...", color = Color(0xFF9CA3AF), fontFamily = PoppinsFontFamily, fontSize = 14.sp) },
                            minLines = 4,
                            maxLines = 8,
                            shape = RoundedCornerShape(18.dp),
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = BrandText,
                                fontFamily = PoppinsFontFamily
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = BrandLightSurface,
                                unfocusedContainerColor = BrandLightSurface,
                                focusedBorderColor = BrandPrimary,
                                unfocusedBorderColor = BrandBorder,
                                focusedTextColor = BrandText,
                                unfocusedTextColor = BrandText
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (description.isBlank()) {
                                return@Button
                            }
                            val prefilledMessage = """
                                *VKARD PRO Support Request*
                                • Name: $userName
                                • Role: $userRole
                                • Code: $userCode
                                • Issue: $selectedIssue
                                
                                *Description:*
                                $description
                            """.trimIndent()

                            val whatsappUrl = "https://api.whatsapp.com/send?phone=918281900772&text=${Uri.encode(prefilledMessage)}"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White)
                            Text("SEND ON WHATSAPP", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileOptionsTab(
    name: String,
    email: String,
    role: String,
    code: String,
    credits: String,
    activeCards: Int,
    createdDate: String,
    ledger: List<RevenueLedger>,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    viewModel: DashboardViewModel,
    updateViewModel: com.vkard.pro.presentation.update.UpdateViewModel
) {
    var showResetPasswordDialog by remember { mutableStateOf(false) }
    var showLedgerDialog by remember { mutableStateOf(false) }
    var isResettingPassword by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Glass Profile Details Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(BrandPrimary.copy(alpha = 0.04f), Color.White)
                        )
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(BrandPrimary, RoundedCornerShape(40.dp))
                        .border(2.dp, BrandBorder, RoundedCornerShape(40.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1).uppercase(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = PoppinsFontFamily
                    )
                }

                Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BrandText, fontFamily = PoppinsFontFamily)
                Text(email, fontSize = 12.sp, color = Color(0xFF64748B), fontFamily = PoppinsFontFamily)

                Box(
                    modifier = Modifier
                        .background(BrandPrimary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(role.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandPrimary, fontFamily = PoppinsFontFamily)
                }

                HorizontalDivider(color = BrandBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                // Metadata Rows
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetadataRowItem("Franchise / Agent Code", code)
                    MetadataRowItem("Wallet Credits Balance", "$credits Kards")
                    MetadataRowItem("Active Cards", "$activeCards Cards")
                    MetadataRowItem("Registration Date", createdDate)
                }
            }
        }

        // Actions Panel Group
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (role.lowercase().contains("franchise")) {
                    ProfileActionRow("Edit Profile", Icons.Default.Edit) { onEditProfile() }
                    HorizontalDivider(color = BrandBorder)
                }
                
                ProfileActionRow("🔒 Reset Password", Icons.Default.Lock) {
                    showResetPasswordDialog = true
                }
                
                HorizontalDivider(color = BrandBorder)
                
                ProfileActionRow("Wallet History Ledger", Icons.Default.AccountBalanceWallet) {
                    showLedgerDialog = true
                }
                
                HorizontalDivider(color = BrandBorder)
                
                ProfileActionRow("Sign Out Account", Icons.Default.ExitToApp, isDestructive = true) {
                    onLogout()
                }
            }
        }

        // App Information Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "App Information",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandText,
                    fontFamily = PoppinsFontFamily
                )
                
                MetadataRowItem("App Name", "VKARD PRO")
                val installedVer = updateViewModel.getInstalledVersionName()
                val installedBuild = updateViewModel.getInstalledVersionCode().toString()

                MetadataRowItem("Installed Version", installedVer)
                MetadataRowItem("Installed Build", installedBuild)
                MetadataRowItem("Package Name", context.packageName)
                
                val info = updateViewModel.latestVersionInfo
                MetadataRowItem("Release Date", info?.releaseDate ?: "N/A")
                MetadataRowItem("Last Update Check", updateViewModel.lastCheckedDisplay)
                
                val latestCode = info?.versionCode?.toLong() ?: 0L
                val hasUpdate = updateViewModel.getInstalledVersionCode() < latestCode
                
                MetadataRowItem("Latest GitHub Version", info?.versionName ?: "N/A")
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Update Status",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        fontFamily = PoppinsFontFamily
                    )
                    
                    if (hasUpdate) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.Red, RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🔴 Update Available",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red,
                                fontFamily = PoppinsFontFamily
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(BrandSuccess, RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🟢 Up to Date",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandSuccess,
                                fontFamily = PoppinsFontFamily
                            )
                        }
                    }
                }
                
                if (updateViewModel.checkError != null) {
                    Text(
                        text = updateViewModel.checkError ?: "",
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontFamily = PoppinsFontFamily,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                if (hasUpdate) {
                    Button(
                        onClick = { updateViewModel.startDownload() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandError),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Update Now", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily)
                    }
                } else {
                    Button(
                        onClick = { updateViewModel.checkForUpdates(forceRefresh = true) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (updateViewModel.isCheckingUpdates) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Check for Updates", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(120.dp))
    }

    if (showResetPasswordDialog) {
        ResetPasswordDialog(
            onDismiss = { showResetPasswordDialog = false },
            onReset = { _, newPassword ->
                isResettingPassword = true
                viewModel.resetPassword(newPassword) { result ->
                    isResettingPassword = false
                    showResetPasswordDialog = false
                }
            },
            isResetting = isResettingPassword
        )
    }

    if (showLedgerDialog) {
        WalletLedgerDialog(
            ledger = ledger,
            onDismiss = { showLedgerDialog = false }
        )
    }
}

@Composable
fun ProfileActionRow(
    label: String,
    icon: ImageVector,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) BrandError else BrandPrimary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDestructive) BrandError else BrandText,
                fontFamily = PoppinsFontFamily
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun MetadataRowItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = Color(0xFF64748B), fontFamily = PoppinsFontFamily)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandText, fontFamily = PoppinsFontFamily)
    }
}

@Composable
fun ResetPasswordDialog(
    onDismiss: () -> Unit,
    onReset: (current: String, new: String) -> Unit,
    isResetting: Boolean
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🔒 Reset Password", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = BrandError, fontSize = 12.sp, fontFamily = PoppinsFontFamily)
                }
                
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password", fontFamily = PoppinsFontFamily, fontSize = 12.sp) },
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password", fontFamily = PoppinsFontFamily, fontSize = 12.sp) },
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password", fontFamily = PoppinsFontFamily, fontSize = 12.sp) },
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            if (isResetting) {
                CircularProgressIndicator(color = BrandPrimary, modifier = Modifier.size(24.dp))
            } else {
                Button(
                    onClick = {
                        if (newPassword != confirmPassword) {
                            errorMsg = "New passwords do not match."
                            return@Button
                        }
                        if (newPassword.length < 6) {
                            errorMsg = "Password must be at least 6 characters."
                            return@Button
                        }
                        onReset(currentPassword, newPassword)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("RESET PASSWORD", fontFamily = PoppinsFontFamily, fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color(0xFF64748B), fontFamily = PoppinsFontFamily, fontSize = 12.sp)
            }
        }
    )
}

@Composable
fun WalletLedgerDialog(
    ledger: List<RevenueLedger>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recent VKARD Ledger", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily, fontSize = 18.sp) },
        text = {
            Box(modifier = Modifier.size(width = 320.dp, height = 400.dp)) {
                if (ledger.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No ledger transactions found.", color = Color(0xFF64748B), fontSize = 14.sp, fontFamily = PoppinsFontFamily)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(ledger) { item ->
                            LedgerCardItem(item)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("CLOSE", fontFamily = PoppinsFontFamily, fontSize = 12.sp)
            }
        }
    )
}

@Composable
fun FloatingBottomNavigation(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    role: String
) {
    val navItems = remember(role) {
        when (role) {
            "super_admin" -> listOf(
                TabItem("home", "Home", Icons.Default.Home),
                TabItem("cards", "Cards", Icons.Default.CreditCard),
                TabItem("agents", "Agents", Icons.Default.Groups),
                TabItem("support", "Support", Icons.Default.Chat),
                TabItem("profile", "Profile", Icons.Default.Person)
            )
            "franchise" -> listOf(
                TabItem("home", "Home", Icons.Default.Home),
                TabItem("cards", "Cards", Icons.Default.CreditCard),
                TabItem("agents", "Agents", Icons.Default.Groups),
                TabItem("support", "Support", Icons.Default.Chat),
                TabItem("profile", "Profile", Icons.Default.Person)
            )
            else -> listOf( // agent
                TabItem("home", "Home", Icons.Default.Home),
                TabItem("cards", "Cards", Icons.Default.CreditCard),
                TabItem("support", "Support", Icons.Default.Chat),
                TabItem("profile", "Profile", Icons.Default.Person)
            )
        }
    }

    Surface(
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(16.dp, shape = RoundedCornerShape(28.dp)),
        color = Color.White,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val isSelected = currentTab == item.id
                val tintColor by animateColorAsState(
                    targetValue = if (isSelected) BrandPrimary else Color(0xFF64748B),
                    label = "navIconTint"
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onTabSelected(item.id) }
                        .padding(vertical = 6.dp, horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = tintColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = tintColor,
                        fontFamily = PoppinsFontFamily
                    )
                }
            }
        }
    }
}

@Composable
fun LedgerCardItem(item: RevenueLedger) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x05000000), spotColor = Color(0x05000000))
            .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.transaction_type,
                    fontWeight = FontWeight.Bold,
                    color = BrandText,
                    fontSize = 14.sp,
                    fontFamily = PoppinsFontFamily
                )
                Text(
                    text = item.remarks ?: "-",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = PoppinsFontFamily
                )
                Text(
                    text = item.created_at.substringBefore("T"),
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontFamily = PoppinsFontFamily
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${item.credits_used} Kards",
                fontWeight = FontWeight.Bold,
                color = if (item.credits_used > 0) BrandSuccess else BrandError,
                fontSize = 14.sp,
                fontFamily = PoppinsFontFamily
            )
        }
    }
}

@Composable
fun StatPillItem(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color, fontFamily = PoppinsFontFamily)
        Text(label, fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold, fontFamily = PoppinsFontFamily)
    }
}

@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    required: Boolean = false,
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = Color(0xFF4B5563),
                fontWeight = FontWeight.Medium,
                fontFamily = PoppinsFontFamily
            )
            if (required) {
                Text(
                    text = "*",
                    color = BrandError,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PoppinsFontFamily
                )
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF9CA3AF), fontFamily = PoppinsFontFamily, fontSize = 14.sp) },
            leadingIcon = leadingIcon?.let {
                { Icon(it, contentDescription = null, tint = if (isFocused) BrandPrimary else Color(0xFF9CA3AF)) }
            },
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(18.dp),
            textStyle = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = BrandText,
                fontFamily = PoppinsFontFamily
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = BrandLightSurface,
                unfocusedContainerColor = BrandLightSurface,
                focusedBorderColor = BrandPrimary,
                unfocusedBorderColor = BrandBorder,
                focusedTextColor = BrandText,
                unfocusedTextColor = BrandText
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
        )
    }
}

@Composable
fun DashboardSkeletonLoader() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBoxItem(width = 120.dp, height = 16.dp)
                SkeletonBoxItem(width = 80.dp, height = 24.dp)
            }
            SkeletonBoxItem(width = 48.dp, height = 48.dp, shape = RoundedCornerShape(24.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SkeletonBoxItem(width = 0.dp, height = 100.dp, modifier = Modifier.weight(1f))
            SkeletonBoxItem(width = 0.dp, height = 100.dp, modifier = Modifier.weight(1f))
        }

        SkeletonBoxItem(width = Modifier.fillMaxWidth(), height = 120.dp)
        SkeletonBoxItem(width = 150.dp, height = 20.dp)

        repeat(3) {
            SkeletonBoxItem(width = Modifier.fillMaxWidth(), height = 72.dp)
        }
    }
}

@Composable
fun SkeletonBoxItem(
    width: Dp,
    height: Dp,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(shape)
            .shimmerLoadingAnimation()
    )
}

@Composable
fun SkeletonBoxItem(
    width: Modifier,
    height: Dp,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp)
) {
    Box(
        modifier = width
            .height(height)
            .clip(shape)
            .shimmerLoadingAnimation()
    )
}

@Composable
fun CustomersManagementTab(
    onManageCustomers: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .shadow(4.dp, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = BrandLightSurface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(56.dp))
                Text(
                    text = "Client Registrations & Search",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandText,
                    textAlign = TextAlign.Center,
                    fontFamily = PoppinsFontFamily
                )
                Text(
                    text = "View all your registered clients, search customer details, and register new customers for digital visiting cards.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    fontFamily = PoppinsFontFamily
                )
                Button(
                    onClick = onManageCustomers,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("OPEN CUSTOMERS DIRECTORY", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily)
                }
            }
        }
    }
}

@Composable
fun LatestVisitingCardsSection(
    latestCards: List<DigitalCardWithSub>,
    onCreateCard: (String?) -> Unit,
    onViewAll: () -> Unit,
    onShareCard: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Latest Visiting Cards",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BrandText,
                fontFamily = PoppinsFontFamily
            )
            if (latestCards.isNotEmpty()) {
                TextButton(onClick = onViewAll) {
                    Text(
                        text = "View All Cards →",
                        color = BrandPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PoppinsFontFamily
                    )
                }
            }
        }

        if (latestCards.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BrandBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = BrandPrimary.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No visiting cards created yet.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B),
                        fontFamily = PoppinsFontFamily
                    )
                    Button(
                        onClick = { onCreateCard(null) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) {
                        Text(
                            text = "Create Your First VKARD",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            latestCards.forEach { card ->
                val statusColor = when (card.status.lowercase()) {
                    "active" -> BrandSuccess
                    "draft", "inactive" -> BrandWarning
                    else -> BrandError
                }
                val expiryDate = card.subscriptions.firstOrNull()?.end_date?.substringBefore("T") ?: "N/A"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x05000000), spotColor = Color(0x05000000))
                        .border(1.dp, BrandBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            CardAvatar(logoUrl = card.logo_url, fullName = card.full_name, size = 56.dp)

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = card.full_name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandText,
                                    fontFamily = PoppinsFontFamily,
                                    maxLines = 1
                                )
                                Text(
                                    text = card.company_name,
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    fontFamily = PoppinsFontFamily,
                                    maxLines = 1
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = card.status.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor,
                                            fontFamily = PoppinsFontFamily
                                        )
                                    }
                                    
                                    Text(
                                        text = "Expires: $expiryDate",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8),
                                        fontFamily = PoppinsFontFamily
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onShareCard(card.slug) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(BrandLightSurface, RoundedCornerShape(8.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = "QR Code",
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = { onCreateCard(card.slug) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(BrandLightSurface, RoundedCornerShape(8.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Card",
                                    tint = BrandSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateAgentDialog(
    franchiseCredits: Int,
    onDismiss: () -> Unit,
    onCreate: (name: String, email: String, pass: String, whatsapp: String, credits: Int, onComplete: (Result<Unit>) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var credits by remember { mutableStateOf("10") }
    
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Agent", fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (errorMessage != null) {
                    Text(errorMessage!!, color = BrandError, fontSize = 12.sp, fontFamily = PoppinsFontFamily)
                }
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Agent Name", fontFamily = PoppinsFontFamily) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address", fontFamily = PoppinsFontFamily) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Initial Password", fontFamily = PoppinsFontFamily) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = whatsapp,
                    onValueChange = { whatsapp = it },
                    label = { Text("WhatsApp Number", fontFamily = PoppinsFontFamily) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = credits,
                    onValueChange = { credits = it },
                    label = { Text("Initial Credits Allocation", fontFamily = PoppinsFontFamily) },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "Your Wallet Balance: $franchiseCredits Credits",
                    fontSize = 12.sp,
                    color = BrandPrimary,
                    fontWeight = FontWeight.Medium,
                    fontFamily = PoppinsFontFamily
                )
            }
        },
        confirmButton = {
            if (isSaving) {
                CircularProgressIndicator(color = BrandPrimary, modifier = Modifier.size(24.dp))
            } else {
                Button(
                    onClick = {
                        if (name.isBlank() || email.isBlank() || password.isBlank() || whatsapp.isBlank()) {
                            errorMessage = "All fields are required."
                            return@Button
                        }
                        val creditsNum = credits.toIntOrNull()
                        if (creditsNum == null || creditsNum < 0) {
                            errorMessage = "Credits must be a positive number."
                            return@Button
                        }
                        if (creditsNum > franchiseCredits) {
                            errorMessage = "Insufficient wallet credits."
                            return@Button
                        }
                        isSaving = true
                        errorMessage = null
                        onCreate(name, email, password, whatsapp, creditsNum) { result ->
                            isSaving = false
                            result.onSuccess {
                                onDismiss()
                            }.onFailure {
                                errorMessage = it.message ?: "Failed to create agent."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("CREATE", fontFamily = PoppinsFontFamily)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", fontFamily = PoppinsFontFamily, color = Color(0xFF64748B))
            }
        }
    )
}

fun Modifier.shimmerLoadingAnimation(): Modifier = composed {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 1200, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    this.background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(10f, 10f),
            end = Offset(translateAnim, translateAnim)
        )
    )
}

@Composable
fun CardAvatar(
    logoUrl: String?,
    fullName: String,
    size: Dp = 56.dp
) {
    if (!logoUrl.isNullOrBlank()) {
        coil.compose.SubcomposeAsyncImage(
            model = logoUrl,
            contentDescription = "Profile Photo",
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BrandLightSurface, androidx.compose.foundation.shape.CircleShape)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp).align(Alignment.Center),
                        color = BrandPrimary,
                        strokeWidth = 2.dp
                    )
                }
            },
            error = {
                LetterAvatar(fullName = fullName, size = size)
            },
            modifier = Modifier
                .size(size)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .border(1.dp, BrandBorder, androidx.compose.foundation.shape.CircleShape)
        )
    } else {
        LetterAvatar(fullName = fullName, size = size)
    }
}

@Composable
fun LetterAvatar(
    fullName: String,
    size: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(BrandLightSurface, androidx.compose.foundation.shape.CircleShape)
            .border(1.dp, BrandBorder, androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = fullName.firstOrNull()?.toString()?.uppercase() ?: "V",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = BrandPrimary,
            fontFamily = PoppinsFontFamily
        )
    }
}

@Composable
fun AnimateCardEnter(
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 350),
        label = "alpha"
    )
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0.95f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 350),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .graphicsLayer(
                alpha = alpha,
                scaleX = scale,
                scaleY = scale
            )
    ) {
        content()
    }
}
