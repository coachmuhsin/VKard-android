package com.vkard.pro.presentation.login

import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vkard.pro.domain.model.KycSubmission
import com.vkard.pro.presentation.theme.PoppinsFontFamily

// Styling Constants
private val ColorPrimary = Color(0xFF077DF7)
private val ColorSecondary = Color(0xFF2A5D93)
private val ColorBg = Color(0xFFF8FAFD)
private val ColorCardBg = Color(0xFFFFFFFF)
private val ColorBorder = Color(0xFFE5EAF2)
private val ColorTextDark = Color(0xFF0F172A)
private val ColorTextMuted = Color(0xFF64748B)
private val ColorSuccess = Color(0xFF34C759)
private val ColorError = Color(0xFFFF3B30)
private val ColorWarning = Color(0xFFFF9500)

@Composable
fun KycBlockScreen(
    viewModel: KycViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    
    val submission = viewModel.submission
    val isLoading = viewModel.isLoading
    val isSaving = viewModel.isSaving
    val errorMessage = viewModel.errorMessage

    var currentStep by remember { mutableStateOf(1) }
    var isEditingKyc by remember { mutableStateOf(false) }

    // Launcher for step 3 document (Identity Doc)
    val idPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileName = getFileName(context, uri)
            val bytes = contentResolver.openInputStream(uri)?.readBytes()
            if (bytes != null) {
                viewModel.setFileBytes(3, fileName, bytes)
            }
        }
    }

    val idCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            viewModel.setBitmap(3, it)
        }
    }

    // Launcher for step 4 document (PAN Card)
    val panPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileName = getFileName(context, uri)
            val bytes = contentResolver.openInputStream(uri)?.readBytes()
            if (bytes != null) {
                viewModel.setFileBytes(4, fileName, bytes)
            }
        }
    }

    val panCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            viewModel.setBitmap(4, it)
        }
    }

    // Launcher for step 5 document (Profile Photo)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileName = getFileName(context, uri)
            val bytes = contentResolver.openInputStream(uri)?.readBytes()
            if (bytes != null) {
                viewModel.setFileBytes(5, fileName, bytes)
            }
        }
    }

    val photoCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            viewModel.setBitmap(5, it)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBg)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ColorPrimary)
            }
        } else {
            val showWizard = submission == null || submission.status.lowercase() == "draft" || isEditingKyc
            
            if (showWizard) {
                // Multi-step Wizard Container
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    // Stepper Header Text
                    Text(
                        text = if (currentStep == 1) "Business Onboarding" else "KYC Verification",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = ColorTextDark,
                        fontFamily = PoppinsFontFamily
                    )
                    
                    Text(
                        text = if (currentStep == 1) "Step 1 of 5: Business Details" else "Step $currentStep of 5",
                        fontSize = 14.sp,
                        color = ColorTextMuted,
                        fontFamily = PoppinsFontFamily
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Progress Indicator Steps
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..5) {
                            val isCompleted = i < currentStep
                            val isActive = i == currentStep
                            
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = when {
                                            isCompleted -> ColorSuccess
                                            isActive -> ColorPrimary
                                            else -> ColorBorder
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCompleted) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                } else {
                                    Text(
                                        text = i.toString(),
                                        color = if (isActive) Color.White else ColorTextMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        fontFamily = PoppinsFontFamily
                                    )
                                }
                            }
                            
                            if (i < 5) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .background(if (i < currentStep) ColorSuccess else ColorBorder)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    // Error Notification
                    if (errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ColorError)
                                Text(
                                    text = errorMessage,
                                    color = ColorError,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = PoppinsFontFamily
                                )
                            }
                        }
                    }
                    
                    // Step Content
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ColorCardBg),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(24.dp))
                            .border(1.dp, ColorBorder, RoundedCornerShape(24.dp))
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            when (currentStep) {
                                1 -> StepBusinessDetails(viewModel)
                                2 -> StepBusinessAddress(viewModel)
                                3 -> StepIdentityVerification(viewModel, idPickerLauncher, idCameraLauncher)
                                4 -> StepPanVerification(viewModel, panPickerLauncher, panCameraLauncher)
                                5 -> StepProfilePhoto(viewModel, photoPickerLauncher, photoCameraLauncher)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Stepper Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (currentStep > 1) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, ColorPrimary),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Text("Back", color = ColorPrimary, fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily)
                            }
                        }
                        
                        Button(
                            onClick = {
                                if (currentStep < 5) {
                                    // Auto save & proceed
                                    viewModel.saveDraft { success ->
                                        if (success) {
                                            currentStep++
                                        }
                                    }
                                } else {
                                    // Final submission
                                    viewModel.submitKyc { success ->
                                        if (success) {
                                            isEditingKyc = false
                                        }
                                    }
                                }
                            },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = if (currentStep == 5) "Submit KYC" else "Continue",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = PoppinsFontFamily
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Stepper Bottom Draft Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.saveDraft()
                            },
                            enabled = !isSaving
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Draft", color = ColorSecondary, fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily)
                        }
                        
                        TextButton(
                            onClick = {
                                viewModel.saveDraft { success ->
                                    if (success) {
                                        onLogout()
                                    }
                                }
                            },
                            enabled = !isSaving
                        ) {
                            Text("Continue Later", color = ColorTextMuted, fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            } else {
                // Static status view (Pending or Rejected)
                val status = submission?.status?.lowercase() ?: "draft"
                val statusColor = if (status == "pending") ColorWarning else ColorError
                val statusBg = if (status == "pending") Color(0xFFFEF3C7) else Color(0xFFFEE2E2)
                
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (status == "pending") Icons.Default.HourglassEmpty else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(80.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = if (status == "pending") "Verification Pending" else "Verification Rejected",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = ColorTextDark,
                            textAlign = TextAlign.Center,
                            fontFamily = PoppinsFontFamily
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = statusBg),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, statusColor, RoundedCornerShape(16.dp)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Text(
                                text = if (status == "pending") {
                                    "Your identity and business documents are currently under review by our administrators. Please check back later."
                                } else {
                                    "Your KYC application was rejected.\n\nReason: ${submission?.rejection_reason ?: "Invalid details provided."}\n\nPlease update your profile details and re-submit."
                                },
                                fontSize = 14.sp,
                                color = ColorTextDark,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp,
                                modifier = Modifier.padding(20.dp),
                                fontFamily = PoppinsFontFamily
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(40.dp))
                        
                        if (status == "rejected") {
                            Button(
                                onClick = { isEditingKyc = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("EDIT & RESUBMIT", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp, fontFamily = PoppinsFontFamily)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        
                        OutlinedButton(
                            onClick = onLogout,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ColorError),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null, tint = ColorError)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("LOGOUT & GO BACK", fontWeight = FontWeight.Bold, color = ColorError, fontSize = 14.sp, fontFamily = PoppinsFontFamily)
                        }
                    }
                }
            }
        }
    }
}

// Stepper Step 1: Business Details
@Composable
fun StepBusinessDetails(viewModel: KycViewModel) {
    Text("Business Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ColorTextDark, modifier = Modifier.padding(bottom = 16.dp), fontFamily = PoppinsFontFamily)
    
    OutlinedTextField(
        value = viewModel.businessName,
        onValueChange = { viewModel.businessName = it },
        label = { Text("Business / Agency Name", fontFamily = PoppinsFontFamily) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    )
    OutlinedTextField(
        value = viewModel.ownerName,
        onValueChange = { viewModel.ownerName = it },
        label = { Text("Owner Full Name", fontFamily = PoppinsFontFamily) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    )
    OutlinedTextField(
        value = viewModel.mobileNumber,
        onValueChange = { viewModel.mobileNumber = it },
        label = { Text("Mobile Number", fontFamily = PoppinsFontFamily) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    )
    OutlinedTextField(
        value = viewModel.whatsappNumber,
        onValueChange = { viewModel.whatsappNumber = it },
        label = { Text("WhatsApp Number", fontFamily = PoppinsFontFamily) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    )
    OutlinedTextField(
        value = viewModel.email,
        onValueChange = { viewModel.email = it },
        label = { Text("Business Email Address", fontFamily = PoppinsFontFamily) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth()
    )
}

// Stepper Step 2: Address
@Composable
fun StepBusinessAddress(viewModel: KycViewModel) {
    Text("Business Address", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ColorTextDark, modifier = Modifier.padding(bottom = 16.dp), fontFamily = PoppinsFontFamily)
    
    OutlinedTextField(
        value = viewModel.addressLine1,
        onValueChange = { viewModel.addressLine1 = it },
        label = { Text("Address Line 1", fontFamily = PoppinsFontFamily) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    )
    OutlinedTextField(
        value = viewModel.addressLine2,
        onValueChange = { viewModel.addressLine2 = it },
        label = { Text("Address Line 2 (Optional)", fontFamily = PoppinsFontFamily) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    )
    OutlinedTextField(
        value = viewModel.city,
        onValueChange = { viewModel.city = it },
        label = { Text("City", fontFamily = PoppinsFontFamily) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    )
    OutlinedTextField(
        value = viewModel.district,
        onValueChange = { viewModel.district = it },
        label = { Text("District", fontFamily = PoppinsFontFamily) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    )
    OutlinedTextField(
        value = viewModel.state,
        onValueChange = { viewModel.state = it },
        label = { Text("State", fontFamily = PoppinsFontFamily) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    )
    OutlinedTextField(
        value = viewModel.country,
        onValueChange = { viewModel.country = it },
        label = { Text("Country", fontFamily = PoppinsFontFamily) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    )
    OutlinedTextField(
        value = viewModel.pinCode,
        onValueChange = { viewModel.pinCode = it },
        label = { Text("PIN Code", fontFamily = PoppinsFontFamily) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

// Stepper Step 3: Identity Verification
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepIdentityVerification(
    viewModel: KycViewModel,
    pickerLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    cameraLauncher: androidx.activity.result.ActivityResultLauncher<Void?>
) {
    Text("Identity Verification", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ColorTextDark, modifier = Modifier.padding(bottom = 16.dp), fontFamily = PoppinsFontFamily)
    
    var expanded by remember { mutableStateOf(false) }
    val idTypes = listOf("Aadhaar", "Passport", "Driving Licence", "Voter ID")
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        OutlinedTextField(
            readOnly = true,
            value = viewModel.governmentIdType,
            onValueChange = {},
            label = { Text("Identity Document Type", fontFamily = PoppinsFontFamily) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            idTypes.forEach { selection ->
                DropdownMenuItem(
                    text = { Text(selection, fontFamily = PoppinsFontFamily) },
                    onClick = {
                        viewModel.governmentIdType = selection
                        expanded = false
                    }
                )
            }
        }
    }
    
    OutlinedTextField(
        value = viewModel.governmentIdNumber,
        onValueChange = { viewModel.governmentIdNumber = it },
        label = { Text("Document ID Number", fontFamily = PoppinsFontFamily) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    )
    
    Text("Upload Identity Document", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorTextDark, modifier = Modifier.padding(bottom = 8.dp), fontFamily = PoppinsFontFamily)
    
    UploadButtonWithPreview(
        fileName = viewModel.governmentIdFileName,
        fileUrl = viewModel.governmentIdUrl,
        hasLocalBytes = viewModel.governmentIdBytes != null,
        onPickFile = { pickerLauncher.launch("*/*") },
        onCaptureCamera = { cameraLauncher.launch(null) }
    )
}

// Stepper Step 4: PAN Verification (Optional)
@Composable
fun StepPanVerification(
    viewModel: KycViewModel,
    pickerLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    cameraLauncher: androidx.activity.result.ActivityResultLauncher<Void?>
) {
    Text("PAN Verification (Optional)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ColorTextDark, modifier = Modifier.padding(bottom = 16.dp), fontFamily = PoppinsFontFamily)
    
    OutlinedTextField(
        value = viewModel.panNumber,
        onValueChange = { viewModel.panNumber = it },
        label = { Text("PAN Number", fontFamily = PoppinsFontFamily) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    )
    
    Text("Upload PAN Document", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorTextDark, modifier = Modifier.padding(bottom = 8.dp), fontFamily = PoppinsFontFamily)
    
    UploadButtonWithPreview(
        fileName = viewModel.panCardFileName,
        fileUrl = viewModel.panCardUrl,
        hasLocalBytes = viewModel.panCardBytes != null,
        onPickFile = { pickerLauncher.launch("*/*") },
        onCaptureCamera = { cameraLauncher.launch(null) }
    )
}

// Stepper Step 5: Profile Photo
@Composable
fun StepProfilePhoto(
    viewModel: KycViewModel,
    pickerLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    cameraLauncher: androidx.activity.result.ActivityResultLauncher<Void?>
) {
    Text("Profile Photo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ColorTextDark, modifier = Modifier.padding(bottom = 16.dp), fontFamily = PoppinsFontFamily)
    Text("Upload a clear photo of the owner / reseller.", fontSize = 12.sp, color = ColorTextMuted, modifier = Modifier.padding(bottom = 16.dp), fontFamily = PoppinsFontFamily)
    
    UploadButtonWithPreview(
        fileName = viewModel.profilePhotoFileName,
        fileUrl = viewModel.profilePhotoUrl,
        hasLocalBytes = viewModel.profilePhotoBytes != null,
        onPickFile = { pickerLauncher.launch("image/*") },
        onCaptureCamera = { cameraLauncher.launch(null) }
    )
}

// Reusable Document Upload & Preview Section
@Composable
fun UploadButtonWithPreview(
    fileName: String,
    fileUrl: String,
    hasLocalBytes: Boolean,
    onPickFile: () -> Unit,
    onCaptureCamera: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorBg, RoundedCornerShape(16.dp))
            .border(1.dp, ColorBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (hasLocalBytes || fileUrl.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, ColorBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (fileName.endsWith(".pdf", ignoreCase = true) || fileUrl.endsWith(".pdf", ignoreCase = true)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = ColorError, modifier = Modifier.size(48.dp))
                        Text(
                            text = if (hasLocalBytes) fileName else "PDF Document",
                            fontSize = 11.sp,
                            maxLines = 1,
                            color = ColorTextDark,
                            fontFamily = PoppinsFontFamily,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                } else {
                    AsyncImage(
                        model = if (hasLocalBytes) fileName else fileUrl,
                        contentDescription = "Document Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPickFile,
                colors = ButtonDefaults.buttonColors(containerColor = ColorSecondary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pick File", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily)
            }
            
            Button(
                onClick = onCaptureCamera,
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Camera", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PoppinsFontFamily)
            }
        }
    }
}

// Utility to read file name from Uri
private fun getFileName(context: android.content.Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "document"
}
