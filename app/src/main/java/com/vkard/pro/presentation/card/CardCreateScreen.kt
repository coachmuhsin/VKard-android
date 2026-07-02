package com.vkard.pro.presentation.card

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vkard.pro.data.local.SecureSessionManager
import com.vkard.pro.presentation.theme.PoppinsFontFamily
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

data class DayHours(
    val isOpen: Boolean,
    val openTime: String,
    val closeTime: String
)

private val daysOfWeekList = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")

fun parseBusinessHours(jsonStr: String): Map<String, DayHours> {
    return try {
        val jsonElement = Json.parseToJsonElement(jsonStr).jsonObject
        daysOfWeekList.associateWith { day ->
            val dayObj = jsonElement[day]?.jsonObject
            DayHours(
                isOpen = dayObj?.get("isOpen")?.jsonPrimitive?.boolean ?: false,
                openTime = dayObj?.get("openTime")?.jsonPrimitive?.contentOrNull ?: "09:00",
                closeTime = dayObj?.get("closeTime")?.jsonPrimitive?.contentOrNull ?: "18:00"
            )
        }
    } catch (e: Exception) {
        daysOfWeekList.associateWith { DayHours(isOpen = true, openTime = "09:00", closeTime = "18:00") }
    }
}

fun serializeBusinessHours(map: Map<String, DayHours>): String {
    return buildString {
        append("{")
        val entries = map.entries.map { (day, hours) ->
            "\"$day\":{\"isOpen\":${hours.isOpen},\"openTime\":\"${hours.openTime}\",\"closeTime\":\"${hours.closeTime}\"}"
        }
        append(entries.joinToString(","))
        append("}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardCreateScreen(
    viewModel: CardViewModel,
    sessionManager: SecureSessionManager,
    customerId: String? = null,
    onBack: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val userId = sessionManager.getUserId() ?: ""
    val role = sessionManager.getRole() ?: "agent"
    val uiState = viewModel.uiState

    // Launcher Activators for Image pickers
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.readBytes()
            if (bytes != null) {
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                viewModel.uploadLogo("data:image/png;base64,$base64")
            }
        }
    }

    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.readBytes()
            if (bytes != null) {
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                viewModel.uploadBanner("data:image/png;base64,$base64")
            }
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.readBytes()
            if (bytes != null) {
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                viewModel.uploadGalleryImage("data:image/png;base64,$base64")
            }
        }
    }

    // Auto-generate slug from name if blank and not in edit mode
    LaunchedEffect(viewModel.fullName) {
        if (!viewModel.isEditMode && viewModel.slug.isBlank() && viewModel.fullName.isNotBlank()) {
            viewModel.slug = viewModel.fullName.lowercase().trim()
                .replace("\\s+".toRegex(), "-")
                .replace("[^\\w-]".toRegex(), "")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadCustomers(userId, role)
        if (customerId != null) {
            viewModel.selectedCustomerId = customerId
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is CardFormUiState.Success) {
            onSuccess(uiState.slug)
            viewModel.clearError()
        }
    }

    // Expanded states
    var expandedBasic by rememberSaveable { mutableStateOf(true) }
    var expandedContact by rememberSaveable { mutableStateOf(false) }
    var expandedSocial by rememberSaveable { mutableStateOf(false) }
    var expandedMedia by rememberSaveable { mutableStateOf(false) }
    var expandedHoursBooking by rememberSaveable { mutableStateOf(false) }
    var expandedBranding by rememberSaveable { mutableStateOf(false) }
    var expandedPayment by rememberSaveable { mutableStateOf(false) }

    val categories = listOf("Automobile", "Real Estate", "Healthcare", "Finance", "Food & Beverage", "Education", "Services", "Others")
    val themes = listOf(
        "glass_blue" to "Glass Blue",
        "glass_green" to "Glass Green",
        "glass_purple" to "Glass Purple",
        "modern" to "Modern",
        "minimal" to "Minimal",
        "corporate" to "Corporate",
        "classic" to "Classic",
        "dark" to "Dark",
        "rainbow" to "Rainbow"
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
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel: Outlined Primary Blue, 50%
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPrimary),
                        border = BorderStroke(1.5.dp, BrandPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PoppinsFontFamily,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Create/Update Card: Filled Primary Blue, 50%
                    if (uiState is CardFormUiState.Loading) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BrandPrimary, modifier = Modifier.size(24.dp))
                        }
                    } else {
                        Button(
                            onClick = { viewModel.submitCard(userId, role, status = "active") },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text(
                                text = if (viewModel.isEditMode) "Save Card" else "Create Card",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PoppinsFontFamily,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        },
        containerColor = BrandBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = if (viewModel.isEditMode) "Edit Digital Card" else "Create Digital Card",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandText,
                    fontFamily = PoppinsFontFamily
                )
                Text(
                    text = "Configure details for your premium digital visiting card",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    fontFamily = PoppinsFontFamily
                )
            }

            // Error display
            if (uiState is CardFormUiState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BrandError.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = BrandError)
                        Text(
                            text = uiState.message,
                            color = BrandError,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = PoppinsFontFamily
                        )
                    }
                }
            }

            // Form Content: Collapsible sections
            
            // 2. Profile Details
            CollapsibleCardSection(
                title = "Primary Profile Info",
                description = "Name, designation, and primary business categorization details",
                icon = Icons.Default.Person,
                expanded = expandedBasic,
                onToggle = { expandedBasic = !expandedBasic }
            ) {
                PremiumTextField(value = viewModel.fullName, onValueChange = { viewModel.fullName = it }, label = "Full Name", placeholder = "Name", required = true, leadingIcon = Icons.Default.Person)
                PremiumTextField(value = viewModel.designation, onValueChange = { viewModel.designation = it }, label = "Job Title / Designation", placeholder = "e.g. Sales Manager", required = true, leadingIcon = Icons.Default.Badge)
                PremiumTextField(value = viewModel.companyName, onValueChange = { viewModel.companyName = it }, label = "Company / Business Name", placeholder = "e.g. Acme Corp", required = true, leadingIcon = Icons.Default.Business)
                
                val catOptions = categories.map { it to it }
                PremiumDropdown(
                    selectedOption = viewModel.businessCategory,
                    options = catOptions,
                    onOptionSelected = { viewModel.businessCategory = it },
                    label = "Primary Business Category"
                )
            }

            // 3. Contact Coordinates
            CollapsibleCardSection(
                title = "Contact Coordinates",
                description = "Primary phone number, WhatsApp contact info, and email addresses",
                icon = Icons.Default.Phone,
                expanded = expandedContact,
                onToggle = { expandedContact = !expandedContact }
            ) {
                PremiumTextField(value = viewModel.mobileNumber, onValueChange = { viewModel.mobileNumber = it }, label = "Mobile Number", placeholder = "e.g. +918884446666", leadingIcon = Icons.Default.Phone, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                PremiumTextField(value = viewModel.whatsappNumber, onValueChange = { viewModel.whatsappNumber = it }, label = "WhatsApp Number", placeholder = "WhatsApp number", leadingIcon = Icons.Default.Chat, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                PremiumTextField(value = viewModel.email, onValueChange = { viewModel.email = it }, label = "Email Address", placeholder = "e.g. email@domain.com", leadingIcon = Icons.Default.Email, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                PremiumTextField(value = viewModel.address, onValueChange = { viewModel.address = it }, label = "Office Address (Physical Location)", placeholder = "Physical address", leadingIcon = Icons.Default.LocationOn)
                PremiumTextField(value = viewModel.companyDescription, onValueChange = { viewModel.companyDescription = it }, label = "Company Tagline / Bio", placeholder = "Tagline or short bio", leadingIcon = Icons.Default.Info)
            }

            // 4. Social Links
            CollapsibleCardSection(
                title = "Social Channels",
                description = "Configure optional profile URLs for search indexing and card mapping",
                icon = Icons.Default.Language,
                expanded = expandedSocial,
                onToggle = { expandedSocial = !expandedSocial }
            ) {
                PremiumTextField(value = viewModel.websiteUrl, onValueChange = { viewModel.websiteUrl = it }, label = "Company Website URL", placeholder = "https://...", leadingIcon = Icons.Default.Language)
                PremiumTextField(value = viewModel.facebookUrl, onValueChange = { viewModel.facebookUrl = it }, label = "Facebook Profile URL", placeholder = "Facebook link", leadingIcon = Icons.Default.Link)
                PremiumTextField(value = viewModel.instagramUrl, onValueChange = { viewModel.instagramUrl = it }, label = "Instagram Username/Link", placeholder = "Instagram link", leadingIcon = Icons.Default.Link)
                PremiumTextField(value = viewModel.linkedinUrl, onValueChange = { viewModel.linkedinUrl = it }, label = "LinkedIn Profile Link", placeholder = "LinkedIn link", leadingIcon = Icons.Default.Link)
                PremiumTextField(value = viewModel.youtubeUrl, onValueChange = { viewModel.youtubeUrl = it }, label = "YouTube Channel URL", placeholder = "YouTube link", leadingIcon = Icons.Default.PlayArrow)
                PremiumTextField(value = viewModel.googleMapsUrl, onValueChange = { viewModel.googleMapsUrl = it }, label = "Google Maps Location Link", placeholder = "Google Maps link", leadingIcon = Icons.Default.LocationOn)
                PremiumTextField(value = viewModel.googleReviewUrl, onValueChange = { viewModel.googleReviewUrl = it }, label = "Google Review Link", placeholder = "Review link", leadingIcon = Icons.Default.Star)
            }

            // 5. Media Assets Upload
            CollapsibleCardSection(
                title = "Media & Assets Upload",
                description = "Upload logo badge, main cover banner, and gallery images",
                icon = Icons.Default.Image,
                expanded = expandedMedia,
                onToggle = { expandedMedia = !expandedMedia }
            ) {
                PremiumUploadCard(
                    title = "Business Logo",
                    subtitle = "Recommended size: 500x500px square format",
                    imageUrl = viewModel.logoUrl,
                    onPickImage = { logoPickerLauncher.launch("image/*") },
                    onRemoveImage = { viewModel.logoUrl = null },
                    isUploading = viewModel.isUploadingMedia
                )

                PremiumUploadCard(
                    title = "Cover Banner",
                    subtitle = "Recommended ratio: 16:9 widescreen layout banner",
                    imageUrl = viewModel.bannerUrl,
                    onPickImage = { bannerPickerLauncher.launch("image/*") },
                    onRemoveImage = { viewModel.bannerUrl = null },
                    isUploading = viewModel.isUploadingMedia
                )

                BusinessGallerySection(
                    galleryImages = viewModel.galleryImages,
                    onPickImages = { galleryPickerLauncher.launch("image/*") },
                    onRemoveImage = { index -> viewModel.galleryImages.removeAt(index) },
                    onMoveLeft = { index ->
                        if (index > 0) {
                            val item = viewModel.galleryImages.removeAt(index)
                            viewModel.galleryImages.add(index - 1, item)
                        }
                    },
                    onMoveRight = { index ->
                        if (index < viewModel.galleryImages.size - 1) {
                            val item = viewModel.galleryImages.removeAt(index)
                            viewModel.galleryImages.add(index + 1, item)
                        }
                    },
                    isUploading = viewModel.isUploadingGallery
                )
            }

            // 6. Business Timing
            CollapsibleCardSection(
                title = "Business Hours & Booking",
                description = "Working schedules and appointment triggers",
                icon = Icons.Default.AccessTime,
                expanded = expandedHoursBooking,
                onToggle = { expandedHoursBooking = !expandedHoursBooking }
            ) {
                BusinessHoursEditor(
                    businessHoursJson = viewModel.businessHours,
                    onHoursChanged = { viewModel.businessHours = it }
                )

                PremiumSegmentedControl(
                    selectedOption = viewModel.enableBooking.toString(),
                    onOptionSelected = { viewModel.enableBooking = it.toBoolean() },
                    options = listOf("false" to "Disabled", "true" to "Enabled"),
                    label = "Appointment Booking State"
                )

                if (viewModel.enableBooking) {
                    PremiumTextField(value = viewModel.bookingWhatsapp, onValueChange = { viewModel.bookingWhatsapp = it }, label = "Booking Contact WhatsApp", placeholder = "WhatsApp number for booking", leadingIcon = Icons.Default.Chat, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                }
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
                    selectedOption = viewModel.cardType ?: "individual",
                    onOptionSelected = { viewModel.cardType = it },
                    options = listOf("individual" to "Individual Profile", "business" to "Business Profile"),
                    label = "Profile Mode Type"
                )

                PremiumDropdown(
                    selectedOption = viewModel.themeName ?: "glass_purple",
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
                PremiumTextField(value = viewModel.gstNumber, onValueChange = { viewModel.gstNumber = it }, label = "GSTIN Business Registration (Optional)", placeholder = "15-character GST registration", leadingIcon = Icons.Default.Business)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ----------------------------------------------------
// Custom UI Components & Subsections
// ----------------------------------------------------

@Composable
fun CollapsibleCardSection(
    title: String,
    description: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val rotationAngle by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "rotationAngle")

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BrandBorder),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x0A000000), spotColor = Color(0x0A000000))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(BrandPrimary.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandText,
                        fontFamily = PoppinsFontFamily
                    )
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontFamily = PoppinsFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.rotate(rotationAngle)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    content = content
                )
            }
        }
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
                fontWeight = FontWeight.Medium,
                fontFamily = PoppinsFontFamily
            )
            if (required) {
                Text(
                    text = " *",
                    fontSize = 13.sp,
                    color = BrandError,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PoppinsFontFamily
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = BrandText,
                    fontFamily = PoppinsFontFamily
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
                    .heightIn(max = 280.dp)
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
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    fontFamily = PoppinsFontFamily
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = BrandPrimary, modifier = Modifier.size(20.dp))
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
            fontWeight = FontWeight.Medium,
            fontFamily = PoppinsFontFamily
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(BrandLightSurface, RoundedCornerShape(16.dp))
                .border(1.dp, BrandPrimary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { (id, optionLabel) ->
                val isSelected = selectedOption == id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { onOptionSelected(id) }
                        .shadow(if (isSelected) 1.dp else 0.dp, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = optionLabel,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) BrandPrimary else Color(0xFF64748B),
                        fontFamily = PoppinsFontFamily
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
            .height(130.dp)
            .shadow(1.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = BrandLightSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandText,
                    fontFamily = PoppinsFontFamily
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontFamily = PoppinsFontFamily
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onPickImage,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = BrandPrimary.copy(alpha = 0.1f),
                            contentColor = BrandPrimary
                        ),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text(if (imageUrl != null) "Replace" else "Upload", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily)
                    }

                    if (imageUrl != null && onRemoveImage != null) {
                        TextButton(
                            onClick = onRemoveImage,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = BrandError.copy(alpha = 0.1f),
                                contentColor = BrandError
                            ),
                            modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Remove", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BusinessGallerySection(
    galleryImages: List<String>,
    onPickImages: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onMoveLeft: (Int) -> Unit,
    onMoveRight: (Int) -> Unit,
    isUploading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Business Gallery Images",
            fontSize = 13.sp,
            color = Color(0xFF4B5563),
            fontWeight = FontWeight.Medium,
            fontFamily = PoppinsFontFamily
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier
                    .size(90.dp)
                    .clickable { onPickImages() }
                    .shadow(1.dp, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = BrandLightSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BrandBorder)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            color = BrandPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Add Photo", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandPrimary, fontFamily = PoppinsFontFamily)
                        }
                    }
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(galleryImages.size) { index ->
                    val url = galleryImages[index]
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, BrandBorder, RoundedCornerShape(14.dp))
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x33000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (index > 0) {
                                    IconButton(
                                        onClick = { onMoveLeft(index) },
                                        modifier = Modifier.size(24.dp).background(Color.White, RoundedCornerShape(12.dp))
                                    ) {
                                        Icon(Icons.Default.ChevronLeft, contentDescription = "Move Left", tint = BrandPrimary, modifier = Modifier.size(14.dp))
                                    }
                                }
                                
                                IconButton(
                                    onClick = { onRemoveImage(index) },
                                    modifier = Modifier.size(24.dp).background(BrandError, RoundedCornerShape(12.dp))
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                                }

                                if (index < galleryImages.size - 1) {
                                    IconButton(
                                        onClick = { onMoveRight(index) },
                                        modifier = Modifier.size(24.dp).background(Color.White, RoundedCornerShape(12.dp))
                                    ) {
                                        Icon(Icons.Default.ChevronRight, contentDescription = "Move Right", tint = BrandPrimary, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BusinessHoursEditor(
    businessHoursJson: String,
    onHoursChanged: (String) -> Unit
) {
    val daysOfWeek = remember { listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday") }
    val parsedMap = remember(businessHoursJson) {
        parseBusinessHours(businessHoursJson)
    }

    val timeSlots = remember {
        (0..23).flatMap { hour ->
            listOf(
                String.format("%02d:00", hour) to String.format("%02d:00 %s", if (hour == 0 || hour == 12) 12 else hour % 12, if (hour < 12) "AM" else "PM"),
                String.format("%02d:30", hour) to String.format("%02d:30 %s", if (hour == 0 || hour == 12) 12 else hour % 12, if (hour < 12) "AM" else "PM")
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandLightSurface, RoundedCornerShape(20.dp))
            .border(1.dp, BrandPrimary.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Weekly Business Hours", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandText, fontFamily = PoppinsFontFamily)
        
        daysOfWeek.forEach { day ->
            val hours = parsedMap[day] ?: DayHours(isOpen = false, openTime = "09:00", closeTime = "18:00")
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = day.replaceFirstChar { it.uppercase() },
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    fontFamily = PoppinsFontFamily,
                    modifier = Modifier.width(90.dp)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(if (hours.isOpen) "Open" else "Closed", fontSize = 12.sp, color = Color(0xFF64748B), fontFamily = PoppinsFontFamily)
                    Switch(
                        checked = hours.isOpen,
                        onCheckedChange = { isOpen ->
                            val updatedMap = parsedMap.toMutableMap()
                            updatedMap[day] = hours.copy(isOpen = isOpen)
                            onHoursChanged(serializeBusinessHours(updatedMap))
                        },
                        modifier = Modifier.scale(0.8f)
                    )
                }

                if (hours.isOpen) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TimeDropdownSelector(
                            selectedTime = hours.openTime,
                            timeSlots = timeSlots,
                            onTimeSelected = { time ->
                                val updatedMap = parsedMap.toMutableMap()
                                updatedMap[day] = hours.copy(openTime = time)
                                onHoursChanged(serializeBusinessHours(updatedMap))
                            }
                        )
                        Text("to", color = Color(0xFF64748B), fontFamily = PoppinsFontFamily, fontSize = 12.sp)
                        TimeDropdownSelector(
                            selectedTime = hours.closeTime,
                            timeSlots = timeSlots,
                            onTimeSelected = { time ->
                                val updatedMap = parsedMap.toMutableMap()
                                updatedMap[day] = hours.copy(closeTime = time)
                                onHoursChanged(serializeBusinessHours(updatedMap))
                            }
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(180.dp))
                }
            }
        }
    }
}

@Composable
fun TimeDropdownSelector(
    selectedTime: String,
    timeSlots: List<Pair<String, String>>,
    onTimeSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayLabel = timeSlots.firstOrNull { it.first == selectedTime }?.second ?: selectedTime

    Box {
        Row(
            modifier = Modifier
                .width(95.dp)
                .height(36.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, BrandPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = BrandText,
                fontFamily = PoppinsFontFamily,
                maxLines = 1
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .height(250.dp)
                .background(Color.White)
        ) {
            timeSlots.forEach { (value, label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontFamily = PoppinsFontFamily,
                            color = BrandText
                        )
                    },
                    onClick = {
                        onTimeSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}
