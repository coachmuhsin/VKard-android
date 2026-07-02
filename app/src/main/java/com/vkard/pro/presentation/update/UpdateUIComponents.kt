package com.vkard.pro.presentation.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vkard.pro.domain.model.DownloadState
import com.vkard.pro.domain.model.VersionInfo
import com.vkard.pro.presentation.theme.PoppinsFontFamily

private val BrandPrimary = Color(0xFF077DF7)
private val BrandText = Color(0xFF000102)
private val BrandError = Color(0xFFFF3B30)

@Composable
fun UpdateBanner(
    versionInfo: VersionInfo?,
    onDismiss: () -> Unit,
    onUpdateClick: () -> Unit
) {
    if (versionInfo == null) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = BrandError),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update Available",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "New Update Available",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PoppinsFontFamily
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Text(
                text = "Version ${versionInfo.versionName} is ready to install.",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontFamily = PoppinsFontFamily
            )

            if (versionInfo.changes.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    versionInfo.changes.take(3).forEach { change ->
                        Text(
                            text = "• $change",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontFamily = PoppinsFontFamily
                        )
                    }
                }
            }

            Button(
                onClick = onUpdateClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Update Now",
                    color = BrandError,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PoppinsFontFamily
                )
            }
        }
    }
}

@Composable
fun ForceUpdateScreen(
    versionInfo: VersionInfo,
    downloadState: DownloadState,
    onUpdateClick: () -> Unit,
    onInstallClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = "Forced Update",
                tint = BrandError,
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "Critical Update Required",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = BrandText,
                fontFamily = PoppinsFontFamily,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "To continue using VKARD PRO, please update to the latest version.",
                fontSize = 14.sp,
                color = Color.Gray,
                fontFamily = PoppinsFontFamily,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BrandError.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Version details: ${versionInfo.versionName}",
                        fontWeight = FontWeight.Bold,
                        color = BrandError,
                        fontSize = 14.sp,
                        fontFamily = PoppinsFontFamily
                    )
                    Text(
                        text = "Released on: ${versionInfo.releaseDate}",
                        color = Color.DarkGray,
                        fontSize = 12.sp,
                        fontFamily = PoppinsFontFamily
                    )

                    HorizontalDivider(color = BrandError.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "What's New:",
                        fontWeight = FontWeight.Bold,
                        color = BrandText,
                        fontSize = 13.sp,
                        fontFamily = PoppinsFontFamily
                    )

                    versionInfo.changes.forEach { change ->
                        Text(
                            text = "• $change",
                            color = Color.DarkGray,
                            fontSize = 13.sp,
                            fontFamily = PoppinsFontFamily
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            when (downloadState) {
                is DownloadState.Idle -> {
                    Button(
                        onClick = onUpdateClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandError),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Update Now",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PoppinsFontFamily
                        )
                    }
                }
                is DownloadState.Downloading -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Downloading Update... ${downloadState.progress}%",
                            fontWeight = FontWeight.Bold,
                            color = BrandText,
                            fontFamily = PoppinsFontFamily
                        )
                        LinearProgressIndicator(
                            progress = downloadState.progress / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = BrandError,
                            trackColor = BrandError.copy(alpha = 0.2f)
                        )
                        Text(
                            text = "Please keep the app open or continue using it.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontFamily = PoppinsFontFamily
                        )
                    }
                }
                is DownloadState.Completed -> {
                    Button(
                        onClick = onInstallClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Install Now",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PoppinsFontFamily
                        )
                    }
                }
                is DownloadState.Failed -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Download Failed",
                            fontWeight = FontWeight.Bold,
                            color = BrandError,
                            fontFamily = PoppinsFontFamily
                        )
                        Text(
                            text = downloadState.errorMessage,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontFamily = PoppinsFontFamily,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onUpdateClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandError),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retry Download", color = Color.White, fontFamily = PoppinsFontFamily)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadProgressDialog(
    progress: Int,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Downloading Update...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandText,
                    fontFamily = PoppinsFontFamily
                )

                LinearProgressIndicator(
                    progress = progress / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = BrandPrimary,
                    trackColor = BrandPrimary.copy(alpha = 0.2f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$progress%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary,
                        fontFamily = PoppinsFontFamily
                    )
                }

                Text(
                    text = "Please keep the app open or continue using it.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontFamily = PoppinsFontFamily,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun InstallReadyDialog(
    onInstallClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Update Ready",
                fontWeight = FontWeight.Bold,
                fontFamily = PoppinsFontFamily
            )
        },
        text = {
            Text(
                text = "VKARD PRO has been downloaded successfully.",
                fontFamily = PoppinsFontFamily
            )
        },
        confirmButton = {
            Button(
                onClick = onInstallClick,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("Install Now", color = Color.White, fontFamily = PoppinsFontFamily)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later", color = Color.Gray, fontFamily = PoppinsFontFamily)
            }
        },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    )
}
