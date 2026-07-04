package com.vkard.pro.presentation.update

import android.content.Context
import android.content.Intent
import android.net.Uri
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

class UpdateViewModel(
    private val updateRepository: UpdateRepository,
    private val updateManager: UpdateManager
) : ViewModel() {

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
                    
                    if (!hasUpdate) {
                        updateRepository.clearCache()
                    }
                    
                    latestVersionInfo = info
                    if (hasUpdate) {
                        if (info.forceUpdate) {
                            showUpdateBanner = false
                        } else {
                            showUpdateBanner = !updateRepository.isUpdateDismissed(info.versionCode)
                        }
                    } else {
                        showUpdateBanner = false
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

    fun openUpdateUrl(context: Context) {
        val url = "https://www.vkard.pro/download"
        try {
            val customTabsIntent = androidx.browser.customtabs.CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(context, Uri.parse(url))
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
