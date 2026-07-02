package com.vkard.pro.presentation.customer

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vkard.pro.data.local.SecureSessionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    viewModel: CustomerViewModel,
    sessionManager: SecureSessionManager,
    onBack: () -> Unit,
    onSelectCustomerForCard: (String) -> Unit
) {
    val userId = sessionManager.getUserId() ?: ""
    val role = sessionManager.getRole() ?: "agent"
    
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadCustomers(userId, role)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clients & Customers", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A))
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Customer", tint = Color(0xFF0F172A))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchCustomers(it, userId, role)
                },
                label = { Text("Search by customer name") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A),
                    focusedBorderColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedLabelColor = Color(0xFF64748B)
                )
            )
            
            // Customer List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(viewModel.customerList) { customer ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = customer.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = customer.company_name ?: "No Company Name",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                                customer.phone?.let {
                                    Text(text = "Phone: $it", fontSize = 11.sp, color = Color(0xFF64748B))
                                }
                                customer.email?.let {
                                    Text(text = "Email: $it", fontSize = 11.sp, color = Color(0xFF64748B))
                                }
                            }
                            
                            // Action: Create visiting card for this customer
                            IconButton(
                                onClick = { onSelectCustomerForCard(customer.id ?: "") },
                                modifier = Modifier
                                    .background(Color(0xFF077DF7), RoundedCornerShape(12.dp))
                                    .size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = "Create Card",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
                
                if (viewModel.customerList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No customers found. Click + to add a client.",
                                color = Color(0xFF64748B),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Create Customer Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { 
                showCreateDialog = false 
                viewModel.clearState()
            },
            title = { Text("New Client Registration", color = Color(0xFF0F172A)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = viewModel.name,
                        onValueChange = { viewModel.name = it },
                        label = { Text("Full Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = Color(0xFF077DF7),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = Color(0xFF077DF7),
                            unfocusedLabelColor = Color(0xFF64748B)
                        )
                    )
                    OutlinedTextField(
                        value = viewModel.companyName,
                        onValueChange = { viewModel.companyName = it },
                        label = { Text("Company Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = Color(0xFF077DF7),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = Color(0xFF077DF7),
                            unfocusedLabelColor = Color(0xFF64748B)
                        )
                    )
                    OutlinedTextField(
                        value = viewModel.phone,
                        onValueChange = { viewModel.phone = it },
                        label = { Text("Mobile Number") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = Color(0xFF077DF7),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = Color(0xFF077DF7),
                            unfocusedLabelColor = Color(0xFF64748B)
                        )
                    )
                    OutlinedTextField(
                        value = viewModel.email,
                        onValueChange = { viewModel.email = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = Color(0xFF077DF7),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = Color(0xFF077DF7),
                            unfocusedLabelColor = Color(0xFF64748B)
                        )
                    )
                    
                    if (viewModel.uiState is CustomerUiState.Error) {
                        Text(
                            text = (viewModel.uiState as CustomerUiState.Error).message,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                if (viewModel.uiState is CustomerUiState.Loading) {
                    CircularProgressIndicator(color = Color(0xFF077DF7))
                } else {
                    Button(
                        onClick = { 
                            viewModel.createCustomer(userId)
                            if (viewModel.uiState !is CustomerUiState.Error) {
                                showCreateDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF077DF7),
                            contentColor = Color.White
                        )
                    ) {
                        Text("REGISTER")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showCreateDialog = false
                    viewModel.clearState()
                }) {
                    Text("CANCEL", color = Color(0xFF64748B))
                }
            }
        )
    }
}
