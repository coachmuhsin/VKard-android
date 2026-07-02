package com.vkard.pro.presentation.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onCreateCard: () -> Unit,
    onManageCustomers: () -> Unit,
    onShareCard: (String) -> Unit,
    onLogout: () -> Unit
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf("home") }

    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }

    Scaffold(
        containerColor = BrandBackground,
        bottomBar = {
            FloatingBottomNavigation(
                currentTab = selectedTab,
                onTabSelected = { tabId ->
                    if (tabId == "support_whatsapp") {
                        // Launch WhatsApp directly
                        val whatsappUrl = "https://api.whatsapp.com/send?phone=+918884446666"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
                        context.startActivity(intent)
                    } else {
                        selectedTab = tabId
                    }
                },
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
                        onRefresh = { viewModel.loadDashboard() }
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
                        onRefresh = { viewModel.loadDashboard() }
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
                        onRefresh = { viewModel.loadDashboard() }
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
    onCreateCard: () -> Unit,
    onManageCustomers: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit
) {
    when (selectedTab) {
        "home" -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp)
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
                            QuickActionItem("Create Card", Icons.Default.AddCard) { onCreateCard() },
                            QuickActionItem("Card List", Icons.Default.CreditCard) { onTabSelected("cards") },
                            QuickActionItem("Clients", Icons.Default.People) { onTabSelected("customers") },
                            QuickActionItem("Agents Network", Icons.Default.Groups) { onTabSelected("agents") }
                        )
                    )
                }

                // Recent Activities title
                item {
                    Text("System Wallet Ledger", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BrandText)
                }

                items(data.ledger.take(5)) { ledgerItem ->
                    LedgerCardItem(ledgerItem)
                }
            }
        }
        "cards" -> {
            VisitingCardsListTab(cards = data.cards, onShareCard = onShareCard)
        }
        "customers" -> {
            CustomersManagementTab(onManageCustomers = onManageCustomers)
        }
        "agents" -> {
            AgentsListTab(agents = emptyList(), isSuperAdmin = true)
        }
        "profile" -> {
            ProfileOptionsTab(name = "Super Admin", email = "admin@vkard.pro", role = "Super Admin", onLogout = onLogout)
        }
    }
}

@Composable
fun FranchiseView(
    data: DashboardUiState.FranchiseData,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onShareCard: (String) -> Unit,
    onCreateCard: () -> Unit,
    onManageCustomers: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit
) {
    when (selectedTab) {
        "home" -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp)
            ) {
                item {
                    DashboardHeader(name = data.name, roleLabel = "Franchise Profile", onRefresh = onRefresh)
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
                            QuickActionItem("Create Card", Icons.Default.AddCard) { onCreateCard() },
                            QuickActionItem("Card List", Icons.Default.CreditCard) { onTabSelected("cards") },
                            QuickActionItem("Agents Network", Icons.Default.Groups) { onTabSelected("agents") },
                            QuickActionItem("Search Client", Icons.Default.Search) { onManageCustomers() }
                        )
                    )
                }

                // Recent Activities
                item {
                    Text("Recent Transactions Ledger", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BrandText)
                }

                items(data.ledger.take(5)) { ledgerItem ->
                    LedgerCardItem(ledgerItem)
                }
            }
        }
        "cards" -> {
            VisitingCardsListTab(cards = data.cards, onShareCard = onShareCard)
        }
        "agents" -> {
            AgentsListTab(agents = data.agentNetwork, isSuperAdmin = false)
        }
        "profile" -> {
            ProfileOptionsTab(name = data.name, email = "Code: " + data.code, role = "Franchise Partner", onLogout = onLogout)
        }
    }
}

@Composable
fun AgentView(
    data: DashboardUiState.AgentData,
    selectedTab: String,
    onShareCard: (String) -> Unit,
    onCreateCard: () -> Unit,
    onManageCustomers: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit
) {
    when (selectedTab) {
        "home" -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp)
            ) {
                item {
                    DashboardHeader(name = data.name, roleLabel = "Agent Profile", onRefresh = onRefresh)
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
                            modifier = Modifier.weight(1f)
                        )
                        DashboardStatCard(
                            title = "Affiliation",
                            value = data.affiliation.take(12),
                            subtitle = "Managed by",
                            icon = Icons.Default.Business,
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
                            QuickActionItem("Create Card", Icons.Default.AddCard) { onCreateCard() },
                            QuickActionItem("Search Client", Icons.Default.Search) { onManageCustomers() }
                        )
                    )
                }

                item {
                    Text("Personal Account Ledger", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BrandText)
                }

                items(data.ledger.take(5)) { ledgerItem ->
                    LedgerCardItem(ledgerItem)
                }
            }
        }
        "cards" -> {
            VisitingCardsListTab(cards = data.cards, onShareCard = onShareCard)
        }
        "profile" -> {
            ProfileOptionsTab(name = data.name, email = "Agent ID: " + data.code, role = "Sales Agent", onLogout = onLogout)
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
            // Profile Initials Box
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
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandText
                    )
                    // Role Badge
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

        // Refresh Trigger Button
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
fun StatPillItem(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
    }
}

data class QuickActionItem(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun QuickActionsSection(
    actions: List<QuickActionItem>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Quick Actions", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BrandText)
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

// ----------------------------------------------------
// Bottom Navigation Tray
// ----------------------------------------------------

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
                TabItem("customers", "Clients", Icons.Default.People),
                TabItem("support_whatsapp", "Support", Icons.Default.Chat),
                TabItem("profile", "Profile", Icons.Default.Person)
            )
            "franchise" -> listOf(
                TabItem("home", "Home", Icons.Default.Home),
                TabItem("cards", "Cards", Icons.Default.CreditCard),
                TabItem("agents", "Agents", Icons.Default.Groups),
                TabItem("support_whatsapp", "Support", Icons.Default.Chat),
                TabItem("profile", "Profile", Icons.Default.Person)
            )
            else -> listOf( // agent
                TabItem("home", "Home", Icons.Default.Home),
                TabItem("cards", "Cards", Icons.Default.CreditCard),
                TabItem("support_whatsapp", "Support", Icons.Default.Chat),
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

// ----------------------------------------------------
// Tab Content Pages
// ----------------------------------------------------

@Composable
fun VisitingCardsListTab(
    cards: List<DigitalCardWithSub>,
    onShareCard: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
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
        Text(
            text = "Visiting Cards",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = BrandText
        )

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name or company...", color = Color(0xFF9CA3AF)) },
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

                        IconButton(
                            onClick = { onShareCard(card.slug) },
                            modifier = Modifier
                                .background(BrandPrimary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "Share QR Code",
                                tint = BrandPrimary,
                                modifier = Modifier.size(20.dp)
                            )
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
    isSuperAdmin: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Agents Network",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = BrandText
        )

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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(agents) { agent ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
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
                            Column {
                                Text(agent.name, fontWeight = FontWeight.Bold, color = BrandText, fontSize = 16.sp)
                                Text("Code: ${agent.code}", color = Color(0xFF64748B), fontSize = 12.sp)
                            }
                            Text("${agent.credits} Kards", fontWeight = FontWeight.Bold, color = BrandPrimary, fontSize = 15.sp)
                        }
                    }
                }

                if (agents.isEmpty()) {
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

@Composable
fun ProfileOptionsTab(
    name: String,
    email: String,
    role: String,
    onLogout: () -> Unit
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

        // Large Profile card
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

                // Sign Out action button
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

// ----------------------------------------------------
// Dashboard Loading Skeleton Screen
// ----------------------------------------------------

@Composable
fun DashboardSkeletonLoader() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header Skeleton
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

        // Metrics skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SkeletonBoxItem(width = 0.dp, height = 100.dp, modifier = Modifier.weight(1f))
            SkeletonBoxItem(width = 0.dp, height = 100.dp, modifier = Modifier.weight(1f))
        }

        // Stats Box skeleton
        SkeletonBoxItem(width = Modifier.fillMaxWidth(), height = 120.dp)

        // Title skeleton
        SkeletonBoxItem(width = 150.dp, height = 20.dp)

        // List item skeletons
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
