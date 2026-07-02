package com.vkard.pro.presentation.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KycBlockScreen(
    status: String,
    rejectionReason: String?,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val statusData = when (status.lowercase()) {
                "pending" -> ColorStatusData(
                    icon = Icons.Default.HourglassEmpty,
                    title = "Verification Pending",
                    description = "Your identity and business documents are currently under review by our administrators. Please check back later.",
                    iconColor = Color(0xFFD97706),       // Amber 600
                    bgColor = Color(0xFFFEF3C7),         // Amber 100
                    borderColor = Color(0xFFF59E0B)     // Amber 500
                )
                "rejected" -> ColorStatusData(
                    icon = Icons.Default.ErrorOutline,
                    title = "Verification Rejected",
                    description = "Your KYC application was rejected.\n\nReason: ${rejectionReason ?: "Invalid details provided."}\n\nPlease update your profile details and re-submit on the web dashboard.",
                    iconColor = Color(0xFFDC2626),       // Red 600
                    bgColor = Color(0xFFFEE2E2),         // Red 100
                    borderColor = Color(0xFFEF4444)     // Red 500
                )
                else -> ColorStatusData(
                    icon = Icons.Default.Info,
                    title = "KYC Onboarding Required",
                    description = "You must complete your KYC onboarding profile and submit required business documents before accessing the companion portal.",
                    iconColor = Color(0xFF0284C7),       // Sky 600
                    bgColor = Color(0xFFE0F2FE),         // Sky 100
                    borderColor = Color(0xFF38BDF8)     // Sky 400
                )
            }
            
            Icon(
                imageVector = statusData.icon,
                contentDescription = null,
                tint = statusData.iconColor,
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = statusData.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A), // Dark Slate
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = statusData.bgColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, statusData.borderColor, RoundedCornerShape(16.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = statusData.description,
                    fontSize = 14.sp,
                    color = Color(0xFF1E293B), // Dark Grayish Slate
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444), // Brand Error Red
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LOGOUT & GO BACK",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

private data class ColorStatusData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String,
    val iconColor: Color,
    val bgColor: Color,
    val borderColor: Color
)
