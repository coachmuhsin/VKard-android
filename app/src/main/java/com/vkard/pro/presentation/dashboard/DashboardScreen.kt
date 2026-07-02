package com.vkard.pro.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vkard.pro.domain.model.RevenueLedger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    role: String,
    onCreateCard: () -> Unit,
    onManageCustomers: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState = viewModel.uiState
    
    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("VKARD PRO Console", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) 
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color(0xFF0F172A))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (role != "super_admin") {
                FloatingActionButton(
                    onClick = onCreateCard,
                    containerColor = Color(0xFF077DF7), // Brand Blue
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Card")
                }
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is DashboardUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF077DF7))
                    }
                }
                is DashboardUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(text = uiState.message, color = Color(0xFFEF4444), fontSize = 16.sp)
                    }
                }
                is DashboardUiState.SuperAdminData -> {
                    SuperAdminDashboard(uiState, onManageCustomers)
                }
                is DashboardUiState.FranchiseData -> {
                    FranchiseDashboard(uiState, onManageCustomers)
                }
                is DashboardUiState.AgentData -> {
                    AgentDashboard(uiState, onManageCustomers)
                }
            }
        }
    }
}

@Composable
fun SuperAdminDashboard(data: DashboardUiState.SuperAdminData, onManageCustomers: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Welcome, Super Administrator",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "Centralized system overview, franchises, and network ledgers.",
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )
        }
        
        // Metrics row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Franchises",
                    value = data.franchiseCount.toString(),
                    icon = Icons.Default.Business,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Agents",
                    value = data.agentCount.toString(),
                    icon = Icons.Default.People,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Card stats
        item {
            CardStatsSection(data.cardStats)
        }
        
        // Transaction Ledger
        item {
            Text("Kard Transaction Ledger", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        }
        
        items(data.ledger) { ledgerItem ->
            LedgerListItem(ledgerItem)
        }
    }
}

@Composable
fun FranchiseDashboard(data: DashboardUiState.FranchiseData, onManageCustomers: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Franchise Dashboard",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Code: ${data.code}", color = Color(0xFF64748B), fontSize = 12.sp)
                Text(text = "Wallet: ${data.credits} Kards", color = Color(0xFF077DF7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        // Metrics row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Sales Agents",
                    value = data.agentNetwork.size.toString(),
                    icon = Icons.Default.Groups,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Published Cards",
                    value = data.cardStats.active.toString(),
                    icon = Icons.Default.ContactMail,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Card stats details
        item {
            CardStatsSection(data.cardStats)
        }
        
        // Quick Action: Manage Customers
        item {
            Button(
                onClick = onManageCustomers,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF077DF7)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SEARCH & MANAGE CUSTOMERS", color = Color.White)
            }
        }
        
        // Agent Network list
        item {
            Text("Franchise Agents Network", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        }
        
        items(data.agentNetwork) { agent ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(agent.name, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text("Code: ${agent.code}", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                    Text("${agent.credits} Kards", fontWeight = FontWeight.Bold, color = Color(0xFF077DF7))
                }
            }
        }
        
        // Transaction Ledger
        item {
            Text("Wallet Transaction Ledger", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        }
        
        items(data.ledger) { ledgerItem ->
            LedgerListItem(ledgerItem)
        }
    }
}

@Composable
fun AgentDashboard(data: DashboardUiState.AgentData, onManageCustomers: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Agent Console",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Agent ID: ${data.code}", color = Color(0xFF64748B), fontSize = 12.sp)
                Text(text = "Wallet: ${data.credits} Kards", color = Color(0xFF077DF7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        // Affiliation Info
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF64748B))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Affiliated with: ${data.affiliation}", color = Color(0xFF64748B), fontSize = 12.sp)
                }
            }
        }
        
        // Card stats details
        item {
            CardStatsSection(data.cardStats)
        }
        
        // Customer Search Actions
        item {
            Button(
                onClick = onManageCustomers,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF077DF7)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("MANAGE CLIENT CUSTOMERS", color = Color.White)
            }
        }
        
        // Transaction Ledger
        item {
            Text("Personal Wallet Transaction Ledger", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        }
        
        items(data.ledger) { ledgerItem ->
            LedgerListItem(ledgerItem)
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                Icon(icon, contentDescription = null, tint = Color(0xFF077DF7), modifier = Modifier.size(16.dp))
            }
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
        }
    }
}

@Composable
fun CardStatsSection(stats: CardStats) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Visiting Card Stats", fontSize = 13.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Total", stats.total.toString(), Color(0xFF0F172A))
                StatItem("Active", stats.active.toString(), Color(0xFF22C55E)) // Green
                StatItem("Draft", stats.draft.toString(), Color(0xFFD97706)) // Amber
                StatItem("Expired", stats.expired.toString(), Color(0xFFEF4444)) // Red
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
        Text(label, fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LedgerListItem(item: RevenueLedger) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.transaction_type,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    fontSize = 13.sp
                )
                Text(
                    text = item.remarks ?: "-",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.created_at.substringBefore("T"),
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${item.credits_used} Kards",
                fontWeight = FontWeight.Black,
                color = if (item.credits_used > 0) Color(0xFF22C55E) else Color(0xFFEF4444),
                fontSize = 14.sp
            )
        }
    }
}
