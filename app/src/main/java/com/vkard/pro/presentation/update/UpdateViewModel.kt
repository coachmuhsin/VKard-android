package com.vkard.pro.presentation.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.app.DownloadManager
import android.provider.Settings
import java.io.File
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vkard.pro.domain.model.VersionInfo
import com.vkard.pro.domain.repository.UpdateRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DownloadState {
    IDLE,
    DOWNLOADING,
    DOWNLOAD_COMPLETE,
    INSTALLING,
    ERROR
}

class UpdateViewModel(
    private val updateRepository: UpdateRepository,
    private val updateManager: UpdateManager
) : ViewModel() {

    var downloadState by mutableStateOf(DownloadState.IDLE)
    var downloadProgress by mutableStateOf(0)
    var downloadedBytes by mutableStateOf(0L)
    var totalBytes by mutableStateOf(0L)
    var showPermissionDialog by mutableStateOf(false)
    private var downloadId: Long? = null

    var latestVersionInfo by mutableStateOf<VersionInfo?>(null)
        private set

    var showUpdateBanner by mutableStateOf(false)

    var isCheckingUpdates by mutableStateOf(false)
        private set

    var checkError by mutableStateOf<String?>(null)
        private set

    var lastCheckedDisplay by mutableStateOf("Never")
        private set

    init {
        updateLastCheckedDisplay()
    }

    fun updateLastCheckedDisplay() {
        val lastChecked = updateRepository.getLastCheckedTime()
        lastCheckedDisplay = if (lastChecked == 0L) {
            "Never"
        } else {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(Date(lastChecked))
        }
    }

    fun getInstalledVersionCode(): Long = updateManager.getInstalledVersionCode()
    fun getInstalledVersionName(): String = updateManager.getInstalledVersionName()

    fun isUpdateAvailable(): Boolean {
        val info = latestVersionInfo ?: return false
        return getInstalledVersionCode() < info.versionCode
    }

    fun checkForUpdates(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            isCheckingUpdates = true
            checkError = null
            
            val installedCode = getInstalledVersionCode()
            val installedName = getInstalledVersionName()
            
            updateRepository.getLatestVersionInfo(forceRefresh)
                .onSuccess { info ->
                    isCheckingUpdates = false
                    updateLastCheckedDisplay()
                    
                    val latestCode = info.versionCode.toLong()
                    val latestName = info.versionName

                    val hasUpdate = installedCode < latestCode
                    val resultString = if (hasUpdate) "Update Available" else "Up To Date"
                    
                    // Detailed log output of individual fields
                    android.util.Log.d("VKARD_OTA", "Installed Version Name: $installedName")
                    android.util.Log.d("VKARD_OTA", "Installed Version Code: $installedCode")
                    android.util.Log.d("VKARD_OTA", "Latest GitHub Version Name: $latestName")
                    android.util.Log.d("VKARD_OTA", "Latest GitHub Version Code: $latestCode")
                    android.util.Log.d("VKARD_OTA", "GitHub Tag Name: ${info.tagName}")
                    android.util.Log.d("VKARD_OTA", "Release Name: ${info.releaseName}")
                    android.util.Log.d("VKARD_OTA", "Published Date: ${info.releaseDate}")
                    android.util.Log.d("VKARD_OTA", "APK Asset Name: ${info.apkAssetName}")
                    android.util.Log.d("VKARD_OTA", "APK Download URL: ${info.apk}")
                    android.util.Log.d("VKARD_OTA", "Comparison Result: $resultString")
                    
                    // Exact format requested
                    android.util.Log.d("VKARD_OTA", """
                        Installed Version:
                        $installedName
                        $installedCode

                        GitHub Version:
                        $latestName
                        $latestCode

                        Result:
                        $resultString
                    """.trimIndent())
                    
                    latestVersionInfo = info
                    if (hasUpdate) {
                        if (info.forceUpdate) {
                            showUpdateBanner = false
                        } else {
                            showUpdateBanner = !updateRepository.isUpdateDismissed(info.versionCode)
                        }
                    } else {
                        showUpdateBanner = false
                        downloadState = DownloadState.IDLE
                    }
                }
                .onFailure { error ->
                    isCheckingUpdates = false
                    val msg = when (error) {
                        is java.net.UnknownHostException,
                        is java.net.ConnectException -> "No Internet Connection"
                        else -> "Unable to check updates."
                    }
                    checkError = msg
                }
        }
    }

    fun dismissUpdate() {
        val info = latestVersionInfo ?: return
        if (!info.forceUpdate) {
            updateRepository.dismissUpdate(info.versionCode)
            showUpdateBanner = false
        }
    }

    fun startDownload(context: Context) {
        val info = latestVersionInfo ?: return
        val url = info.apk
        if (url.isBlank()) {
            checkError = "No valid APK download URL available."
            downloadState = DownloadState.ERROR
            return
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("Downloading Update")
            setDescription("VKARD PRO ${info.versionName}")
            setMimeType("application/vnd.android.package-archive")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, android.os.Environment.DIRECTORY_DOWNLOADS, "VKARD-PRO.apk")
        }

        try {
            val destinationFile = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "VKARD-PRO.apk")
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        downloadId = downloadManager.enqueue(request)
        downloadState = DownloadState.DOWNLOADING
        downloadProgress = 0
        downloadedBytes = 0L
        totalBytes = 0L

        trackDownloadProgress(context)
    }

    fun cancelDownload(context: Context) {
        val id = downloadId ?: return
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.remove(id)
        downloadId = null
        downloadState = DownloadState.IDLE
    }

    private fun trackDownloadProgress(context: Context) {
        viewModelScope.launch {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            var downloading = true
            while (downloading && downloadState == DownloadState.DOWNLOADING) {
                val id = downloadId ?: break
                val query = DownloadManager.Query().setFilterById(id)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = cursor.getInt(statusCol)
                    
                    val downloadedCol = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalCol = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    
                    val bytesSoFar = cursor.getLong(downloadedCol)
                    val bytesTotal = cursor.getLong(totalCol)
                    
                    downloadedBytes = bytesSoFar
                    totalBytes = bytesTotal
                    
                    if (bytesTotal > 0) {
                        downloadProgress = ((bytesSoFar * 100) / bytesTotal).toInt()
                    }
                    
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            downloading = false
                            downloadState = DownloadState.DOWNLOAD_COMPLETE
                        }
                        DownloadManager.STATUS_FAILED -> {
                            downloading = false
                            downloadState = DownloadState.ERROR
                            checkError = "Download failed. Please try again."
                        }
                    }
                }
                cursor?.close()
                kotlinx.coroutines.delay(500)
            }
        }
    }

    fun verifyAndInstallApk(context: Context) {
        val destinationFile = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "VKARD-PRO.apk")
        
        if (!destinationFile.exists()) {
            downloadState = DownloadState.ERROR
            checkError = "Download failed. Please try again."
            return
        }

        val minSize = 5L * 1024L * 1024L
        val isValidSize = destinationFile.length() > minSize
        val isValidExt = destinationFile.name.endsWith(".apk", ignoreCase = true)

        if (!isValidSize || !isValidExt) {
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            downloadState = DownloadState.ERROR
            checkError = "Download failed. Please try again."
            return
        }

        installApk(context, destinationFile)
    }

    private fun installApk(context: Context, apkFile: File) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                showPermissionDialog = true
                return
            }
        }
        
        try {
            downloadState = DownloadState.INSTALLING
            val authority = "${context.packageName}.fileprovider"
            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, apkFile)
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            downloadState = DownloadState.ERROR
            checkError = "Unable to launch installer: ${e.localizedMessage}"
        }
    }

    fun launchUnknownSourcesSettings(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun openUpdateUrl(context: Context) {
        startDownload(context)
    }
}
