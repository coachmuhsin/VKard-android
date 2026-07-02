package com.vkard.pro.presentation.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import coil.compose.AsyncImage
import com.vkard.pro.domain.model.DigitalCardWithSub
import com.vkard.pro.domain.model.RevenueLedger

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                            Text(text = uiState.message, color = BrandError, fontSize = 16.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                            Button(
                                onClick = { viewModel.loadDashboard() },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                            ) {
                                Text("Retry", color = Color.White)
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
                        onRefresh = { viewModel.loadDashboard() },
                        onDeleteCard = { viewModel.deleteCard(it) }
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
                        onRefresh = { viewModel.loadDashboard() },
                        onDeleteCard = { viewModel.deleteCard(it) },
                        onUpdateAgent = { agentId, newName, newCredits, newStatus ->
                            viewModel.updateAgent(agentId, newName, newCredits, newStatus)
                        },
                        onUpdateProfile = { newName ->
                            val userId = viewModel.sessionManager.getUserId() ?: ""
                            viewModel.updateFranchiseProfile(userId, newName)
                        }
                    )
                }
                is DashboardUiState.AgentData -> {
                    AgentView(
                        data = uiState,
                        selectedTab = selectedTab,
                        onShareCard = onShareCard,
                        onCreateCard = onCreateCard,
                        onManageCustomers = onManageCustomers,
                        onLogout = onLogout,
                        onRefresh = { viewModel.loadDashboard() },
                        onDeleteCard = { viewModel.deleteCard(it) }
                    )
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
    onDeleteCard: (String) -> Unit
) {
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
                    DashboardHeader(name = "Super Admin", roleLabel = "Super Admin", onRefresh = onRefresh)
                }

                // Super Admin Metrics
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DashboardStatCard(
                            title = "Franchises",
                            value = data.franchiseCount.toString(),
                            subtitle = "Active outlets",
                            icon = Icons.Default.Business,
                            color = BrandPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        DashboardStatCard(
                            title = "Sales Agents",
                            value = data.agentCount.toString(),
                            subtitle = "Registered agents",
                            icon = Icons.Default.Groups,
                            color = BrandSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    CardStatsBlock(stats = data.cardStats)
                }

                // Quick Actions
                item {
                    QuickActionsSection(
                        actions = listOf(
                            QuickActionItem("Create Card", Icons.Default.AddCard) { onCreateCard(null) },
                            QuickActionItem("Card List", Icons.Default.CreditCard) { onTabSelected("cards") },
                            QuickActionItem("Clients", Icons.Default.People) { onTabSelected("customers") },
                            QuickActionItem("Agents Network", Icons.Default.Groups) { onTabSelected("agents") }
                        )
                    )
                }

                item {
                    Text("System Wallet Ledger", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrandText)
                }

                items(data.ledger.take(5)) { ledgerItem ->
                    LedgerCardItem(ledgerItem)
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
            AgentsListTab(agents = emptyList(), allCards = data.cards, isSuperAdmin = true, onUpdateAgent = { _, _, _, _ -> }, onRefresh = onRefresh)
        }
        "support" -> {
            SupportTab(userName = "Super Admin", userRole = "Super Admin", userCode = "N/A", userEmail = "admin@vkard.pro")
        }
        "profile" -> {
            ProfileOptionsTab(name = "Super Admin", email = "admin@vkard.pro", role = "Super Admin", onLogout = onLogout, onEditProfile = {})
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
    onUpdateAgent: (agentId: String, newName: String, newCredits: Int, newStatus: String) -> Unit,
    onUpdateProfile: (String) -> Unit
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }

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
                    DashboardHeader(name = data.name, roleLabel = "Franchise", onRefresh = onRefresh)
                }

                // Stats Card Rows
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DashboardStatCard(
                            title = "Wallet Balance",
                            value = "${data.credits}",
                            subtitle = "Kard Credits",
                            icon = Icons.Default.AccountBalanceWallet,
                            color = BrandPrimary,
                            modifier = Modifier.weight(1.1f)
                        )
                        DashboardStatCard(
                            title = "Active Agents",
                            value = "${data.agentNetwork.size}",
                            subtitle = "Sub network",
                            icon = Icons.Default.Groups,
                            color = BrandSecondary,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }

                item {
                    CardStatsBlock(stats = data.cardStats)
                }

                // Quick Actions
                item {
                    QuickActionsSection(
                        actions = listOf(
                            QuickActionItem("Create Card", Icons.Default.AddCard) { onCreateCard(null) },
                            QuickActionItem("Card List", Icons.Default.CreditCard) { onTabSelected("cards") },
                            QuickActionItem("Agents Network", Icons.Default.Groups) { onTabSelected("agents") },
                            QuickActionItem("Search Client", Icons.Default.Search) { onManageCustomers() }
                        )
                    )
                }

                // Recent Activities
                item {
                    Text("Recent Transactions Ledger", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrandText)
                }

                items(data.ledger.take(5)) { ledgerItem ->
                    LedgerCardItem(ledgerItem)
                }
            }
        }
        "cards" -> {
            VisitingCardsListTab(cards = data.cards, onShareCard = onShareCard, onCreateCard = onCreateCard, onDeleteCard = onDeleteCard, onRefresh = onRefresh)
        }
        "agents" -> {
            AgentsListTab(agents = data.agentNetwork, allCards = data.cards, isSuperAdmin = false, onUpdateAgent = onUpdateAgent, onRefresh = onRefresh)
        }
        "support" -> {
            SupportTab(userName = data.name, userRole = "Franchise Partner", userCode = data.code, userEmail = "Code: " + data.code)
        }
        "profile" -> {
            ProfileOptionsTab(
                name = data.name,
                email = "Franchise Code: " + data.code,
                role = "Franchise Partner",
                onLogout = onLogout,
                onEditProfile = { showEditProfileDialog = true }
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
    onShareCard: (String) -> Unit,
    onCreateCard: (String?) -> Unit,
    onManageCustomers: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
    onDeleteCard: (String) -> Unit
) {
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
                    DashboardHeader(name = data.name, roleLabel = "Agent", onRefresh = onRefresh)
                }

                // Stats Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DashboardStatCard(
                            title = "Wallet Credits",
                            value = "${data.credits}",
                            subtitle = "Kard balance",
                            icon = Icons.Default.AccountBalanceWallet,
                            color = BrandPrimary,
                            modifier = Modifier.weight(1.1f)
                        )
                        DashboardStatCard(
                            title = "Affiliation",
                            value = data.affiliation.take(12),
                            subtitle = "Franchise group",
                            icon = Icons.Default.Business,
                            color = BrandSecondary,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }

                item {
                    CardStatsBlock(stats = data.cardStats)
                }

                // Quick Actions
                item {
                    QuickActionsSection(
                        actions = listOf(
                            QuickActionItem("Create Card", Icons.Default.AddCard) { onCreateCard(null) },
                            QuickActionItem("Search Client", Icons.Default.Search) { onManageCustomers() }
                        )
                    )
                }

                item {
                    Text("Personal Account Ledger", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrandText)
                }

                items(data.ledger.take(5)) { ledgerItem ->
                    LedgerCardItem(ledgerItem)
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
            ProfileOptionsTab(name = data.name, email = "Agent ID: " + data.code, role = "Sales Agent", onLogout = onLogout, onEditProfile = {})
        }
    }
}

// ----------------------------------------------------
// Custom UI Components & Subsections
// ----------------------------------------------------

@Composable
fun DashboardHeader(
    name: String,
    roleLabel: String,
    onRefresh: () -> Unit
) {
    val calendarHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (calendarHour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
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
                    .background(BrandPrimary, RoundedCornerShape(24.dp))
                    .border(1.dp, BrandBorder, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Column {
                Text(
                    text = "$greeting,",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandText
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
                            color = BrandPrimary
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
            Text("Visiting Card Stats", fontSize = 14.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
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
        Text("Quick Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrandText)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            actions.forEach { action ->
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { action.onClick() }
                        .shadow(1.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = BrandLightSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BrandBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White, RoundedCornerShape(10.dp))
                                .border(1.dp, BrandBorder, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = action.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandText,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
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
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Visiting Cards",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = BrandText
            )
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .background(BrandLightSurface, RoundedCornerShape(12.dp))
                    .size(40.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = BrandPrimary, modifier = Modifier.size(20.dp))
            }
        }

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search cards...", color = Color(0xFF9CA3AF)) },
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
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(filteredCards) { card ->
                val statusColor = when (card.status.lowercase()) {
                    "active" -> BrandSuccess
                    "draft", "inactive" -> BrandWarning
                    else -> BrandError
                }

                var showMenu by remember { mutableStateOf(false) }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x0A000000), spotColor = Color(0x0A000000))
                        .border(1.dp, BrandBorder, RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = card.full_name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandText
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
                                        color = statusColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${card.designation} at ${card.company_name}",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                            if (card.subscriptions.isNotEmpty()) {
                                val sub = card.subscriptions.first()
                                Text(
                                    text = "Expires: ${sub.end_date.substringBefore("T")}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Overflow Actions
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
                                    text = { Text("View Online") },
                                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showMenu = false
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://vkard.pro/" + card.slug))
                                        context.startActivity(intent)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit Card") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showMenu = false
                                        onCreateCard(card.slug)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("QR Code") },
                                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showMenu = false
                                        onShareCard(card.slug)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Copy Link") },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showMenu = false
                                        clipboardManager.setText(AnnotatedString("https://vkard.pro/" + card.slug))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete", color = BrandError) },
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

            if (filteredCards.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No digital cards match search query.", color = Color(0xFF64748B), fontSize = 14.sp)
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
    onUpdateAgent: (agentId: String, name: String, credits: Int, status: String) -> Unit,
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
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Agents Network",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = BrandText
            )
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .background(BrandLightSurface, RoundedCornerShape(12.dp))
                    .size(40.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = BrandPrimary, modifier = Modifier.size(20.dp))
            }
        }

        if (isSuperAdmin) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Centralized sales agents registry is active.", color = Color(0xFF64748B), fontSize = 14.sp)
            }
        } else {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search agents...", color = Color(0xFF9CA3AF)) },
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
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredAgents) { agent ->
                    // Count cards assigned to this agent in memory
                    val totalCards = allCards.count { it.created_by == agent.id }
                    val activeCards = allCards.count { it.created_by == agent.id && it.status.lowercase() == "active" }
                    val isStateActive = agent.status == "active"

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
                                    Text(agent.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = BrandPrimary, fontSize = 16.sp)
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(agent.name, fontWeight = FontWeight.Bold, color = BrandText, fontSize = 15.sp)
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
                                                color = if (isStateActive) BrandSuccess else BrandError
                                            )
                                        }
                                    }
                                    Text("Code: ${agent.code} | Cards: $activeCards / $totalCards", color = Color(0xFF64748B), fontSize = 12.sp)
                                }
                            }
                            Text("${agent.credits} Kards", fontWeight = FontWeight.Bold, color = BrandPrimary, fontSize = 15.sp)
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
                            Text("No registered franchise agents found.", color = Color(0xFF64748B), fontSize = 14.sp)
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
            onSave = { newName, newCredits, newStatus ->
                onUpdateAgent(agent.id, newName, newCredits, newStatus)
                selectedAgentForEdit = null
            }
        )
    }
}

@Composable
fun EditAgentDialog(
    agent: AgentUiModel,
    assignedCardsCount: Int,
    onDismiss: () -> Unit,
    onSave: (newName: String, newCredits: Int, newStatus: String) -> Unit
) {
    var name by remember { mutableStateOf(agent.name) }
    var credits by remember { mutableStateOf(agent.credits.toString()) }
    var status by remember { mutableStateOf(agent.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Agent Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Agent Name") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = credits,
                    onValueChange = { credits = it },
                    label = { Text("Wallet Credits Balance") },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Agent Active Status", fontWeight = FontWeight.Medium)
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
                            color = BrandPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, credits.toIntOrNull() ?: 0, status) },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color(0xFF64748B))
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
        title = { Text("Edit Profile Details", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
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
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color(0xFF64748B))
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
    var name by remember { mutableStateOf(userName) }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(userEmail) }
    var code by remember { mutableStateOf(userCode) }
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
        contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Support Ticket",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = BrandText
            )
            Text(
                text = "Submit a support inquiry. This will launch WhatsApp directly.",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BrandBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x0A000000), spotColor = Color(0x0A000000))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PremiumTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Your Name",
                        placeholder = "Name",
                        required = true,
                        leadingIcon = Icons.Default.Person
                    )

                    PremiumTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = "Mobile Number",
                        placeholder = "e.g. +919876543210",
                        required = true,
                        leadingIcon = Icons.Default.Phone,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    PremiumTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email Address",
                        placeholder = "e.g. email@domain.com",
                        required = true,
                        leadingIcon = Icons.Default.Email,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    PremiumTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = "Agent / Franchise Code",
                        placeholder = "e.g. AGENT-1234",
                        leadingIcon = Icons.Default.Code
                    )

                    // Issue dropdown
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Common Issue",
                            fontSize = 13.sp,
                            color = Color(0xFF4B5563),
                            fontWeight = FontWeight.Medium
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
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = BrandText
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
                                                    fontSize = 16.sp
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

                    // Multiline Description
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Issue Description",
                            fontSize = 13.sp,
                            color = Color(0xFF4B5563),
                            fontWeight = FontWeight.Medium
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("Describe the issue or feature request in detail...", color = Color(0xFF9CA3AF)) },
                            minLines = 4,
                            maxLines = 8,
                            shape = RoundedCornerShape(18.dp),
                            textStyle = TextStyle(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = BrandText
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

                    // Launch WhatsApp Button
                    Button(
                        onClick = {
                            if (name.isBlank() || mobile.isBlank()) {
                                return@Button
                            }
                            val prefilledMessage = """
                                *VKARD PRO Support Request*
                                • Name: $name
                                • Role: $userRole
                                • Code: $code
                                • Mobile: $mobile
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
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LAUNCH WHATSAPP SUPPORT", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
    onLogout: () -> Unit,
    onEditProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Profile Settings",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = BrandText,
            modifier = Modifier.fillMaxWidth()
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BrandBorder)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BrandText)
                Text(email, fontSize = 14.sp, color = Color(0xFF64748B))

                Box(
                    modifier = Modifier
                        .background(BrandPrimary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(role.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BrandBorder)
                Spacer(modifier = Modifier.height(16.dp))

                if (role.lowercase().contains("franchise")) {
                    Button(
                        onClick = onEditProfile,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EDIT PROFILE NAME", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = onLogout,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandError, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SIGN OUT ACCOUNT", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
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
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(12.dp, shape = RoundedCornerShape(24.dp)),
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, BrandBorder)
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
                        .clip(RoundedCornerShape(12.dp))
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
                        color = tintColor
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
                    fontSize = 14.sp
                )
                Text(
                    text = item.remarks ?: "-",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.created_at.substringBefore("T"),
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${item.credits_used} Kards",
                fontWeight = FontWeight.Bold,
                color = if (item.credits_used > 0) BrandSuccess else BrandError,
                fontSize = 15.sp
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
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DashboardStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0x0F000000),
                spotColor = Color(0x0F000000)
            )
            .border(1.dp, BrandBorder, RoundedCornerShape(24.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = BrandText
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

data class QuickActionItem(val label: String, val icon: ImageVector, val onClick: () -> Unit)

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
                fontWeight = FontWeight.Medium
            )
            if (required) {
                Text(
                    text = "*",
                    color = BrandError,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF9CA3AF)) },
            leadingIcon = leadingIcon?.let {
                { Icon(it, contentDescription = null, tint = if (isFocused) BrandPrimary else Color(0xFF9CA3AF)) }
            },
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(18.dp),
            textStyle = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = BrandText
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
            .background(Color(0xFFF1F5F9), shape)
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
            .background(Color(0xFFF1F5F9), shape)
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
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Client Customers",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = BrandText,
            modifier = Modifier.fillMaxWidth()
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .shadow(4.dp, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = BrandLightSurface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BrandBorder)
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
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "View all your registered clients, search customer details, and register new customers for digital visiting cards.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onManageCustomers,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("OPEN CUSTOMERS DIRECTORY", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
