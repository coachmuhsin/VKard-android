package com.vkard.pro.presentation.card

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vkard.pro.data.local.SecureSessionManager

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

    // Remember expanded state of sections
    var expandedCore by rememberSaveable { mutableStateOf(true) }
    var expandedBusinessInfo by rememberSaveable { mutableStateOf(false) }
    var expandedContact by rememberSaveable { mutableStateOf(false) }
    var expandedAddress by rememberSaveable { mutableStateOf(false) }
    var expandedHoursBooking by rememberSaveable { mutableStateOf(false) }
    var expandedSocial by rememberSaveable { mutableStateOf(false) }
    var expandedBranding by rememberSaveable { mutableStateOf(false) }
    var expandedPayment by rememberSaveable { mutableStateOf(false) }
    var expandedUploads by rememberSaveable { mutableStateOf(false) }

    // Derive progress value dynamically
    val profileProgress = remember(viewModel.fullName, viewModel.designation, viewModel.companyName,
        viewModel.slug, viewModel.gstNumber, viewModel.mobileNumber, viewModel.whatsappNumber,
        viewModel.email, viewModel.address, viewModel.companyDescription, viewModel.businessHours,
        viewModel.websiteUrl, viewModel.facebookUrl, viewModel.instagramUrl, viewModel.linkedinUrl,
        viewModel.youtubeUrl, viewModel.googleMapsUrl, viewModel.googleReviewUrl, viewModel.logoUrl,
        viewModel.bannerUrl, viewModel.upiId, viewModel.upiName, viewModel.bankHolderName,
        viewModel.bankName, viewModel.bankAccountNumber, viewModel.bankIfsc
    ) {
        calculateProfileProgress(viewModel)
    }

    // Deriving real-time validation warnings
    val slugError = if (viewModel.slug.isNotEmpty() && !viewModel.slug.matches("^[a-z0-9-]+$".toRegex())) {
        "Slug can only contain lowercase letters, numbers, and hyphens (-)"
    } else null

    val emailError = if (viewModel.email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(viewModel.email).matches()) {
        "Please enter a valid email address"
    } else null

    val mobileError = if (viewModel.mobileNumber.isNotEmpty() && !viewModel.mobileNumber.matches("^[0-9+() -]{10,15}$".toRegex())) {
        "Please enter a valid phone number"
    } else null

    val whatsappError = if (viewModel.whatsappNumber.isNotEmpty() && !viewModel.whatsappNumber.matches("^[0-9+() -]{10,15}$".toRegex())) {
        "Please enter a valid WhatsApp number"
    } else null

    val categories = listOf(
        "Others" to "Others",
        "Retail" to "Retail",
        "Tech" to "Tech",
        "Healthcare" to "Healthcare",
        "Finance" to "Finance",
        "Food & Beverage" to "Food & Beverage",
        "Education" to "Education",
        "Services" to "Services"
    )

    val themes = listOf(
        "glass_purple" to "Glass Purple",
        "glass_blue" to "Glass Blue",
        "glass_green" to "Glass Green"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = BrandText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBackground)
            )
        },
        containerColor = BrandBackground,
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = BrandBackground,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, BrandBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel Action
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4B5563)),
                        border = BorderStroke(1.dp, BrandBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Save Draft Action
                    OutlinedButton(
                        onClick = { viewModel.submitCard(userId, role, status = "draft") },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPrimary),
                        border = BorderStroke(1.dp, BrandPrimary),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(56.dp)
                    ) {
                        Text("Save Draft", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Create Active Card Action
                    if (uiState is CardFormUiState.Loading) {
                        Box(
                            modifier = Modifier
                                .weight(1.7f)
                                .height(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BrandPrimary, modifier = Modifier.size(28.dp))
                        }
                    } else {
                        Button(
                            onClick = { viewModel.submitCard(userId, role, status = "active") },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = Color.White),
                            modifier = Modifier
                                .weight(1.7f)
                                .height(56.dp)
                                .shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    ambientColor = BrandPrimary.copy(alpha = 0.4f),
                                    spotColor = BrandPrimary.copy(alpha = 0.4f)
                                )
                        ) {
                            Text("Create Card", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BrandBackground)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Giant Title Header
            Text(
                text = "Design Visiting Card",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = BrandText,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Dynamic Completion Bar
            ProfileCompletionBar(progress = profileProgress)

            // API Error Feedback Block
            if (uiState is CardFormUiState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BrandError.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BrandError.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = BrandError,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = uiState.message,
                            color = BrandError,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 1. Core Profile Collapsible Card
            CollapsibleCardSection(
                title = "Core Business Profile",
                description = "Name, title, designation, and URL slug info",
                icon = Icons.Default.Person,
                expanded = expandedCore,
                onToggle = { expandedCore = !expandedCore }
            ) {
                PremiumTextField(
                    value = viewModel.fullName,
                    onValueChange = { viewModel.fullName = it },
                    label = "Card Name / Title",
                    placeholder = "e.g. John Doe",
                    required = true,
                    leadingIcon = Icons.Default.Person
                )

                PremiumTextField(
                    value = viewModel.designation,
                    onValueChange = { viewModel.designation = it },
                    label = "Designation / Tagline",
                    placeholder = "e.g. Director of Operations",
                    required = true,
                    leadingIcon = Icons.Default.Badge
                )

                PremiumTextField(
                    value = viewModel.companyName,
                    onValueChange = { viewModel.companyName = it },
                    label = "Company / Org Name",
                    placeholder = "e.g. VKard Pro Solutions",
                    required = true,
                    leadingIcon = Icons.Default.Business
                )

                PremiumTextField(
                    value = viewModel.slug,
                    onValueChange = { viewModel.slug = it },
                    label = "URL Slug (Custom)",
                    placeholder = "Auto-generated if empty",
                    isError = slugError != null,
                    errorMessage = slugError,
                    leadingIcon = Icons.Default.Link
                )
            }

            // 2. Business Info Collapsible Card
            CollapsibleCardSection(
                title = "Business Information",
                description = "Domain segment category and official GSTIN details",
                icon = Icons.Default.Category,
                expanded = expandedBusinessInfo,
                onToggle = { expandedBusinessInfo = !expandedBusinessInfo }
            ) {
                PremiumDropdown(
                    selectedOption = viewModel.businessCategory,
                    options = categories,
                    onOptionSelected = { viewModel.businessCategory = it },
                    label = "Business Category",
                    required = true
                )

                PremiumTextField(
                    value = viewModel.gstNumber,
                    onValueChange = { viewModel.gstNumber = it },
                    label = "GSTIN Registration",
                    placeholder = "Optional registration number",
                    leadingIcon = Icons.Default.Assignment
                )
            }

            // 3. Contact Info Collapsible Card
            CollapsibleCardSection(
                title = "Contact Information",
                description = "Mobile, WhatsApp, email channels and biographical info",
                icon = Icons.Default.Phone,
                expanded = expandedContact,
                onToggle = { expandedContact = !expandedContact }
            ) {
                PremiumTextField(
                    value = viewModel.mobileNumber,
                    onValueChange = { viewModel.mobileNumber = it },
                    label = "Mobile Number",
                    placeholder = "e.g. +919876543210",
                    required = true,
                    isError = mobileError != null,
                    errorMessage = mobileError,
                    leadingIcon = Icons.Default.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                PremiumTextField(
                    value = viewModel.whatsappNumber,
                    onValueChange = { viewModel.whatsappNumber = it },
                    label = "WhatsApp Number",
                    placeholder = "e.g. +919876543210",
                    required = true,
                    isError = whatsappError != null,
                    errorMessage = whatsappError,
                    leadingIcon = Icons.Default.Chat,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                PremiumTextField(
                    value = viewModel.email,
                    onValueChange = { viewModel.email = it },
                    label = "Email Address",
                    placeholder = "e.g. hello@business.com",
                    required = true,
                    isError = emailError != null,
                    errorMessage = emailError,
                    leadingIcon = Icons.Default.Email,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                PremiumTextField(
                    value = viewModel.companyDescription,
                    onValueChange = { viewModel.companyDescription = it },
                    label = "About / Biography",
                    placeholder = "Say a few words about your professional summary",
                    leadingIcon = Icons.Default.Info
                )
            }

            // 4. Address Collapsible Card
            CollapsibleCardSection(
                title = "Address",
                description = "Physical office or warehouse address locations",
                icon = Icons.Default.LocationOn,
                expanded = expandedAddress,
                onToggle = { expandedAddress = !expandedAddress }
            ) {
                PremiumTextField(
                    value = viewModel.address,
                    onValueChange = { viewModel.address = it },
                    label = "Office Address",
                    placeholder = "e.g. Building 45, Street C, City",
                    leadingIcon = Icons.Default.LocationOn
                )
            }

            // 5. Business Timings & Booking Collapsible Card
            CollapsibleCardSection(
                title = "Business Hours & Booking",
                description = "Working schedules and appointment triggers",
                icon = Icons.Default.AccessTime,
                expanded = expandedHoursBooking,
                onToggle = { expandedHoursBooking = !expandedHoursBooking }
            ) {
                PremiumTextField(
                    value = viewModel.businessHours,
                    onValueChange = { viewModel.businessHours = it },
                    label = "Business Timing Hours",
                    placeholder = "e.g. Mon-Sat 9AM-6PM",
                    leadingIcon = Icons.Default.AccessTime
                )

                PremiumSegmentedControl(
                    selectedOption = viewModel.enableBooking.toString(),
                    onOptionSelected = { viewModel.enableBooking = it.toBoolean() },
                    options = listOf("false" to "Disabled", "true" to "Enabled"),
                    label = "Appointment Booking State"
                )

                if (viewModel.enableBooking) {
                    PremiumTextField(
                        value = viewModel.bookingWhatsapp,
                        onValueChange = { viewModel.bookingWhatsapp = it },
                        label = "Booking WhatsApp Number",
                        placeholder = "e.g. +919876543210",
                        leadingIcon = Icons.Default.Chat,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }
            }

            // 6. Social Media Collapsible Card
            CollapsibleCardSection(
                title = "Social Media Profiles",
                description = "Connect external link handles to your profile layout",
                icon = Icons.Default.Share,
                expanded = expandedSocial,
                onToggle = { expandedSocial = !expandedSocial }
            ) {
                PremiumTextField(value = viewModel.websiteUrl, onValueChange = { viewModel.websiteUrl = it }, label = "Website URL", placeholder = "https://example.com", leadingIcon = Icons.Default.Language)
                PremiumTextField(value = viewModel.facebookUrl, onValueChange = { viewModel.facebookUrl = it }, label = "Facebook Profile URL", placeholder = "https://facebook.com/...", leadingIcon = Icons.Default.Language)
                PremiumTextField(value = viewModel.instagramUrl, onValueChange = { viewModel.instagramUrl = it }, label = "Instagram Handle URL", placeholder = "https://instagram.com/...", leadingIcon = Icons.Default.Image)
                PremiumTextField(value = viewModel.linkedinUrl, onValueChange = { viewModel.linkedinUrl = it }, label = "LinkedIn Profile URL", placeholder = "https://linkedin.com/in/...", leadingIcon = Icons.Default.Work)
                PremiumTextField(value = viewModel.youtubeUrl, onValueChange = { viewModel.youtubeUrl = it }, label = "YouTube Channel URL", placeholder = "https://youtube.com/c/...", leadingIcon = Icons.Default.PlayArrow)
                PremiumTextField(value = viewModel.googleMapsUrl, onValueChange = { viewModel.googleMapsUrl = it }, label = "Google Maps Business URL", placeholder = "Google maps drop-pin link", leadingIcon = Icons.Default.LocationOn)
                PremiumTextField(value = viewModel.googleReviewUrl, onValueChange = { viewModel.googleReviewUrl = it }, label = "Google Review Page URL", placeholder = "Direct review redirect link", leadingIcon = Icons.Default.Star)
            }

            // 7. Branding & Theme Layout Card
            CollapsibleCardSection(
                title = "Branding & Visual Style",
                description = "Select layout structure types and visual template color palettes",
                icon = Icons.Default.Palette,
                expanded = expandedBranding,
                onToggle = { expandedBranding = !expandedBranding }
            ) {
                PremiumSegmentedControl(
                    selectedOption = viewModel.cardType,
                    onOptionSelected = { viewModel.cardType = it },
                    options = listOf("individual" to "Individual Profile", "business" to "Business Profile"),
                    label = "Profile Mode Type"
                )

                PremiumDropdown(
                    selectedOption = viewModel.themeName,
                    options = themes,
                    onOptionSelected = { viewModel.themeName = it },
                    label = "Template Theme Style"
                )
            }

            // 8. Payment & Banking Information
            CollapsibleCardSection(
                title = "Payment & Banking Info",
                description = "Configure online payments UPI VPA addresses and settlement info",
                icon = Icons.Default.AccountBalance,
                expanded = expandedPayment,
                onToggle = { expandedPayment = !expandedPayment }
            ) {
                PremiumTextField(value = viewModel.upiId, onValueChange = { viewModel.upiId = it }, label = "UPI Virtual Address (VPA)", placeholder = "e.g. name@bank", leadingIcon = Icons.Default.QrCode)
                PremiumTextField(value = viewModel.upiName, onValueChange = { viewModel.upiName = it }, label = "UPI Registered Payee Name", placeholder = "Payee merchant name", leadingIcon = Icons.Default.Person)
                PremiumTextField(value = viewModel.upiDescription, onValueChange = { viewModel.upiDescription = it }, label = "UPI Note / Memo", placeholder = "e.g. Thanks for purchasing", leadingIcon = Icons.Default.Edit)
                PremiumTextField(value = viewModel.bankHolderName, onValueChange = { viewModel.bankHolderName = it }, label = "Settlement Holder Name", placeholder = "Full bank account holder name", leadingIcon = Icons.Default.Person)
                PremiumTextField(value = viewModel.bankName, onValueChange = { viewModel.bankName = it }, label = "Settlement Bank Name", placeholder = "e.g. HDFC Bank", leadingIcon = Icons.Default.AccountBalance)
                PremiumTextField(value = viewModel.bankAccountNumber, onValueChange = { viewModel.bankAccountNumber = it }, label = "Settlement Account Number", placeholder = "Account number", leadingIcon = Icons.Default.Lock, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                PremiumTextField(value = viewModel.bankIfsc, onValueChange = { viewModel.bankIfsc = it }, label = "Settlement IFSC Code", placeholder = "11-character bank code", leadingIcon = Icons.Default.Code)
                PremiumTextField(value = viewModel.bankBranch, onValueChange = { viewModel.bankBranch = it }, label = "Settlement Branch Name", placeholder = "Local branch location", leadingIcon = Icons.Default.Home)
            }

            // 9. Media Assets Upload
            CollapsibleCardSection(
                title = "Media Assets Upload",
                description = "Upload high-quality business logos and banner headers",
                icon = Icons.Default.Image,
                expanded = expandedUploads,
                onToggle = { expandedUploads = !expandedUploads }
            ) {
                PremiumUploadCard(
                    title = "Business Logo",
                    subtitle = "Square graphic image logo (e.g. PNG/JPG)",
                    imageUrl = viewModel.logoUrl,
                    onPickImage = { logoLauncher.launch("image/*") },
                    onRemoveImage = { viewModel.logoUrl = null },
                    isUploading = viewModel.isUploadingMedia
                )

                Spacer(modifier = Modifier.height(4.dp))

                PremiumUploadCard(
                    title = "Banner Cover Image",
                    subtitle = "Wide header aspect ratio cover picture",
                    imageUrl = viewModel.bannerUrl,
                    onPickImage = { bannerLauncher.launch("image/*") },
                    onRemoveImage = { viewModel.bannerUrl = null },
                    isUploading = viewModel.isUploadingMedia
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Redirect on Successful Generation
    LaunchedEffect(uiState) {
        if (uiState is CardFormUiState.Success) {
            onSuccess(uiState.slug)
        }
    }
}

// ----------------------------------------------------
// UI Component Helpers for Premium UI/UX Styling
// ----------------------------------------------------

@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    errorMessage: String? = null,
    required: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = if (isError) BrandError else if (isFocused) BrandPrimary else Color(0xFF4B5563),
                fontWeight = FontWeight.Medium
            )
            if (required) {
                Text(
                    text = " *",
                    fontSize = 13.sp,
                    color = BrandError,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = 15.sp,
                    color = Color(0xFF9CA3AF)
                )
            },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = if (isError) BrandError else if (isFocused) BrandPrimary else Color(0xFF9CA3AF)
                    )
                }
            },
            singleLine = true,
            isError = isError,
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
                errorContainerColor = BrandLightSurface,
                focusedBorderColor = BrandPrimary,
                unfocusedBorderColor = BrandBorder,
                errorBorderColor = BrandError,
                focusedTextColor = BrandText,
                unfocusedTextColor = BrandText,
                errorTextColor = BrandText
            ),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isFocused) 4.dp else 1.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = Color(0xFF000000),
                    spotColor = Color(0xFF000000)
                )
                .onFocusChanged {
                    isFocused = it.isFocused
                }
        )

        if (isError && !errorMessage.isNullOrEmpty()) {
            Text(
                text = errorMessage,
                fontSize = 12.sp,
                color = BrandError,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
fun PremiumSegmentedControl(
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    options: List<Pair<String, String>>,
    label: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF4B5563),
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(BrandLightSurface, RoundedCornerShape(24.dp))
                .border(1.dp, BrandBorder, RoundedCornerShape(24.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { (id, optionLabel) ->
                val isSelected = selectedOption == id
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) BrandPrimary else Color.Transparent,
                    animationSpec = tween(300),
                    label = "segmentedBg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color(0xFF4B5563),
                    animationSpec = tween(300),
                    label = "segmentedText"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(backgroundColor)
                        .clickable { onOptionSelected(id) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = optionLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumDropdown(
    selectedOption: String,
    options: List<Pair<String, String>>,
    onOptionSelected: (String) -> Unit,
    label: String,
    required: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = Color(0xFF4B5563),
                fontWeight = FontWeight.Medium
            )
            if (required) {
                Text(
                    text = " *",
                    fontSize = 13.sp,
                    color = BrandError,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            val selectedLabel = options.find { it.first == selectedOption }?.second ?: selectedOption

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(BrandLightSurface, RoundedCornerShape(18.dp))
                    .border(1.dp, if (expanded) BrandPrimary else BrandBorder, RoundedCornerShape(18.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedLabel,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = BrandText
                )
                val rotationAngle by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "arrowRotation")
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.rotate(rotationAngle)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(1.dp, BrandBorder, RoundedCornerShape(16.dp))
            ) {
                options.forEach { (id, optionLabel) ->
                    val isSelected = selectedOption == id
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = optionLabel,
                                    color = if (isSelected) BrandPrimary else BrandText,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
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
                            onOptionSelected(id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumUploadCard(
    title: String,
    subtitle: String,
    imageUrl: String?,
    onPickImage: () -> Unit,
    onRemoveImage: (() -> Unit)?,
    isUploading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        colors = CardDefaults.cardColors(containerColor = BrandLightSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .border(1.dp, BrandBorder, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(36.dp)
                    )
                }

                if (isUploading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x80FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = BrandPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPickImage,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPrimary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (imageUrl != null) "REPLACE" else "UPLOAD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (imageUrl != null && onRemoveImage != null) {
                        OutlinedButton(
                            onClick = onRemoveImage,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = BrandError
                            ),
                            border = BorderStroke(1.dp, BrandError.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "REMOVE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CollapsibleCardSection(
    title: String,
    description: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0x1A000000),
                spotColor = Color(0x1A000000)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(BrandLightSurface, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandText
                        )
                        if (description.isNotEmpty()) {
                            Text(
                                text = description,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                val rotationAngle by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "arrowRotate")
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationAngle)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = BrandBorder, modifier = Modifier.padding(horizontal = 20.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileCompletionBar(
    progress: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0x1A000000),
                spotColor = Color(0x1A000000)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BrandSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Profile Completion",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandText
                    )
                }

                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary
                )
            }

            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(500),
                label = "progressAnimation"
            )

            LinearProgressIndicator(
                progress = { animatedProgress },
                color = BrandPrimary,
                trackColor = BrandLightSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

private fun calculateProfileProgress(viewModel: CardViewModel): Float {
    var completedFields = 0
    var totalFields = 0

    fun checkField(value: String?) {
        totalFields++
        if (!value.isNullOrBlank()) {
            completedFields++
        }
    }

    // Required Core fields
    checkField(viewModel.fullName)
    checkField(viewModel.designation)
    checkField(viewModel.companyName)

    // Optional Core fields
    checkField(viewModel.slug)
    checkField(viewModel.gstNumber)

    // Contact fields
    checkField(viewModel.mobileNumber)
    checkField(viewModel.whatsappNumber)
    checkField(viewModel.email)
    checkField(viewModel.address)
    checkField(viewModel.companyDescription)
    checkField(viewModel.businessHours)

    // Social fields
    checkField(viewModel.websiteUrl)
    checkField(viewModel.facebookUrl)
    checkField(viewModel.instagramUrl)
    checkField(viewModel.linkedinUrl)
    checkField(viewModel.youtubeUrl)
    checkField(viewModel.googleMapsUrl)
    checkField(viewModel.googleReviewUrl)

    // Branding / Media fields
    checkField(viewModel.logoUrl)
    checkField(viewModel.bannerUrl)

    // Payment fields
    checkField(viewModel.upiId)
    checkField(viewModel.upiName)
    checkField(viewModel.bankHolderName)
    checkField(viewModel.bankName)
    checkField(viewModel.bankAccountNumber)
    checkField(viewModel.bankIfsc)

    return if (totalFields > 0) completedFields.toFloat() / totalFields.toFloat() else 0f
}
