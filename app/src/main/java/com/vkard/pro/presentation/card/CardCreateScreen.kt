package com.vkard.pro.presentation.card

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vkard.pro.data.local.SecureSessionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardCreateScreen(
    viewModel: CardViewModel,
    sessionManager: SecureSessionManager,
    customerId: String?,
    onBack: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val userId = sessionManager.getUserId() ?: ""
    val role = sessionManager.getRole() ?: "agent"
    val uiState = viewModel.uiState
    
    // Set customer selection if passed from clients list
    LaunchedEffect(customerId) {
        if (customerId != null) {
            viewModel.selectedCustomerId = customerId
        }
        viewModel.loadCustomers(userId, role)
    }
    
    // Image Pickers
    val logoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                viewModel.uploadLogo("data:image/png;base64,$base64")
            }
        }
    }
    
    val bannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                viewModel.uploadBanner("data:image/png;base64,$base64")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Design Visiting Card", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A))
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dropdown: Customer Selection
            Text("Assign Client Customer *", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
            var expandedCust by remember { mutableStateOf(false) }
            val selectedCustomer = viewModel.customers.find { it.id == viewModel.selectedCustomerId }
            
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expandedCust = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedCustomer?.name ?: "Select Customer *",
                            color = Color(0xFF0F172A)
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF0F172A))
                    }
                }
                
                DropdownMenu(
                    expanded = expandedCust,
                    onDismissRequest = { expandedCust = false },
                    modifier = Modifier.fillMaxWidth().background(Color.White)
                ) {
                    viewModel.customers.forEach { customer ->
                        DropdownMenuItem(
                            text = { Text(customer.name, color = Color(0xFF0F172A)) },
                            onClick = {
                                viewModel.selectedCustomerId = customer.id
                                expandedCust = false
                            }
                        )
                    }
                }
            }
            
            HorizontalDivider(color = Color(0xFFE2E8F0))
            
            // Section 1: Core Fields
            Text("1. Core Business Profile", fontSize = 14.sp, color = Color(0xFF077DF7), fontWeight = FontWeight.Bold)
            
            OutlinedTextField(
                value = viewModel.fullName,
                onValueChange = { viewModel.fullName = it },
                label = { Text("Card Name / Title *") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )
            
            OutlinedTextField(
                value = viewModel.designation,
                onValueChange = { viewModel.designation = it },
                label = { Text("Designation / Tagline *") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )
            
            OutlinedTextField(
                value = viewModel.companyName,
                onValueChange = { viewModel.companyName = it },
                label = { Text("Company / Org Name *") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )
            
            OutlinedTextField(
                value = viewModel.slug,
                onValueChange = { viewModel.slug = it },
                label = { Text("Slug (Auto-generated if empty)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )

            // Business Category Dropdown
            Text("Business Category *", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
            var expandedCat by remember { mutableStateOf(false) }
            val categories = listOf("Others", "Retail", "Tech", "Healthcare", "Finance", "Food & Beverage", "Education", "Services")
            
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expandedCat = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = viewModel.businessCategory, color = Color(0xFF0F172A))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF0F172A))
                    }
                }
                
                DropdownMenu(
                    expanded = expandedCat,
                    onDismissRequest = { expandedCat = false },
                    modifier = Modifier.fillMaxWidth().background(Color.White)
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category, color = Color(0xFF0F172A)) },
                            onClick = {
                                viewModel.businessCategory = category
                                expandedCat = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = viewModel.gstNumber,
                onValueChange = { viewModel.gstNumber = it },
                label = { Text("GSTIN Registration (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )
            
            HorizontalDivider(color = Color(0xFFE2E8F0))
            
            // Section 2: Contact
            Text("2. Contact Channels", fontSize = 14.sp, color = Color(0xFF077DF7), fontWeight = FontWeight.Bold)
            
            OutlinedTextField(
                value = viewModel.mobileNumber,
                onValueChange = { viewModel.mobileNumber = it },
                label = { Text("Mobile Number") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )
            
            OutlinedTextField(
                value = viewModel.whatsappNumber,
                onValueChange = { viewModel.whatsappNumber = it },
                label = { Text("WhatsApp Number") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )
            
            OutlinedTextField(
                value = viewModel.email,
                onValueChange = { viewModel.email = it },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )
            
            OutlinedTextField(
                value = viewModel.address,
                onValueChange = { viewModel.address = it },
                label = { Text("Office Address") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )
            
            OutlinedTextField(
                value = viewModel.companyDescription,
                onValueChange = { viewModel.companyDescription = it },
                label = { Text("About / Biography") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )

            OutlinedTextField(
                value = viewModel.businessHours,
                onValueChange = { viewModel.businessHours = it },
                label = { Text("Business Timing Hours (e.g. Mon-Sat 9AM-6PM)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )

            // Enable Booking Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable Appointment Booking", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                Switch(
                    checked = viewModel.enableBooking,
                    onCheckedChange = { viewModel.enableBooking = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF077DF7))
                )
            }

            if (viewModel.enableBooking) {
                OutlinedTextField(
                    value = viewModel.bookingWhatsapp,
                    onValueChange = { viewModel.bookingWhatsapp = it },
                    label = { Text("Booking WhatsApp Number") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF077DF7),
                        focusedLabelColor = Color(0xFF077DF7),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A)
                    )
                )
            }
            
            HorizontalDivider(color = Color(0xFFE2E8F0))

            // Section 3: Social Links
            Text("3. Social Media Channels", fontSize = 14.sp, color = Color(0xFF077DF7), fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = viewModel.websiteUrl,
                onValueChange = { viewModel.websiteUrl = it },
                label = { Text("Website URL") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )

            OutlinedTextField(
                value = viewModel.facebookUrl,
                onValueChange = { viewModel.facebookUrl = it },
                label = { Text("Facebook URL") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )

            OutlinedTextField(
                value = viewModel.instagramUrl,
                onValueChange = { viewModel.instagramUrl = it },
                label = { Text("Instagram URL") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )

            OutlinedTextField(
                value = viewModel.linkedinUrl,
                onValueChange = { viewModel.linkedinUrl = it },
                label = { Text("LinkedIn URL") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )

            OutlinedTextField(
                value = viewModel.youtubeUrl,
                onValueChange = { viewModel.youtubeUrl = it },
                label = { Text("YouTube Channel URL") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )

            OutlinedTextField(
                value = viewModel.googleMapsUrl,
                onValueChange = { viewModel.googleMapsUrl = it },
                label = { Text("Google Maps Location URL") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )

            OutlinedTextField(
                value = viewModel.googleReviewUrl,
                onValueChange = { viewModel.googleReviewUrl = it },
                label = { Text("Google Review Page URL") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )

            HorizontalDivider(color = Color(0xFFE2E8F0))
            
            // Section 4: Custom Branding & Style
            Text("4. Visual Theme & Style", fontSize = 14.sp, color = Color(0xFF077DF7), fontWeight = FontWeight.Bold)

            // Profile Card Type Dropdown
            Text("Profile Card Type", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
            var expandedType by remember { mutableStateOf(false) }
            
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expandedType = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (viewModel.cardType == "individual") "Individual" else "Business",
                            color = Color(0xFF0F172A)
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF0F172A))
                    }
                }
                
                DropdownMenu(
                    expanded = expandedType,
                    onDismissRequest = { expandedType = false },
                    modifier = Modifier.fillMaxWidth().background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Individual", color = Color(0xFF0F172A)) },
                        onClick = {
                            viewModel.cardType = "individual"
                            expandedType = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Business", color = Color(0xFF0F172A)) },
                        onClick = {
                            viewModel.cardType = "business"
                            expandedType = false
                        }
                    )
                }
            }

            // Template Theme Dropdown
            Text("Template Theme", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
            var expandedTheme by remember { mutableStateOf(false) }
            val themes = listOf(
                "glass_purple" to "Glass Purple",
                "glass_blue" to "Glass Blue",
                "glass_green" to "Glass Green"
            )
            
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expandedTheme = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = themes.find { it.first == viewModel.themeName }?.second ?: "Glass Purple",
                            color = Color(0xFF0F172A)
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF0F172A))
                    }
                }
                
                DropdownMenu(
                    expanded = expandedTheme,
                    onDismissRequest = { expandedTheme = false },
                    modifier = Modifier.fillMaxWidth().background(Color.White)
                ) {
                    themes.forEach { theme ->
                        DropdownMenuItem(
                            text = { Text(theme.second, color = Color(0xFF0F172A)) },
                            onClick = {
                                viewModel.themeName = theme.first
                                expandedTheme = false
                            }
                        )
                    }
                }
            }
            
            HorizontalDivider(color = Color(0xFFE2E8F0))
            
            // Section 5: Banking Info
            Text("5. UPI Payment & Banking", fontSize = 14.sp, color = Color(0xFF077DF7), fontWeight = FontWeight.Bold)
            
            OutlinedTextField(
                value = viewModel.upiId,
                onValueChange = { viewModel.upiId = it },
                label = { Text("UPI ID (VPA)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )
            
            OutlinedTextField(
                value = viewModel.upiName,
                onValueChange = { viewModel.upiName = it },
                label = { Text("UPI Payee Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )

            OutlinedTextField(
                value = viewModel.upiDescription,
                onValueChange = { viewModel.upiDescription = it },
                label = { Text("UPI Note Description") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )

            OutlinedTextField(
                value = viewModel.bankHolderName,
                onValueChange = { viewModel.bankHolderName = it },
                label = { Text("Holder Account Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )
            
            OutlinedTextField(
                value = viewModel.bankName,
                onValueChange = { viewModel.bankName = it },
                label = { Text("Bank Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )
            
            OutlinedTextField(
                value = viewModel.bankAccountNumber,
                onValueChange = { viewModel.bankAccountNumber = it },
                label = { Text("Account Number") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )
            
            OutlinedTextField(
                value = viewModel.bankIfsc,
                onValueChange = { viewModel.bankIfsc = it },
                label = { Text("IFSC Bank Code") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )

            OutlinedTextField(
                value = viewModel.bankBranch,
                onValueChange = { viewModel.bankBranch = it },
                label = { Text("Branch Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF077DF7),
                    focusedLabelColor = Color(0xFF077DF7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                )
            )
            
            HorizontalDivider(color = Color(0xFFE2E8F0))
            
            // Section 6: Media Picker
            Text("6. Brand Media Upload", fontSize = 14.sp, color = Color(0xFF077DF7), fontWeight = FontWeight.Bold)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Logo Picker
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Business Logo", fontSize = 12.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (viewModel.logoUrl != null) {
                            AsyncImage(
                                model = viewModel.logoUrl,
                                contentDescription = "Logo Preview",
                                modifier = Modifier.size(60.dp)
                            )
                        } else {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(40.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { logoLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF077DF7))
                        ) {
                            Text("PICK", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
                
                // Banner Picker
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Banner Image", fontSize = 12.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (viewModel.bannerUrl != null) {
                            AsyncImage(
                                model = viewModel.bannerUrl,
                                contentDescription = "Banner Preview",
                                modifier = Modifier.size(60.dp)
                            )
                        } else {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(40.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { bannerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF077DF7))
                        ) {
                            Text("PICK", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            }
            
            if (viewModel.isUploadingMedia) {
                LinearProgressIndicator(color = Color(0xFF077DF7), modifier = Modifier.fillMaxWidth())
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Error Display
            if (uiState is CardFormUiState.Error) {
                Text(text = uiState.message, color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            
            // Action button
            if (uiState is CardFormUiState.Loading) {
                CircularProgressIndicator(color = Color(0xFF077DF7), modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Button(
                    onClick = { viewModel.submitCard(userId, role) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF077DF7), contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("GENERATE & PUBLISH CARD", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    
    // Redirect on Success
    LaunchedEffect(uiState) {
        if (uiState is CardFormUiState.Success) {
            onSuccess(uiState.slug)
        }
    }
}
