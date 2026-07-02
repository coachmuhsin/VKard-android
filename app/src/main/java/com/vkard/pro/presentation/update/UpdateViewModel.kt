package com.vkard.pro.presentation.update

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vkard.pro.BuildConfig
import com.vkard.pro.domain.model.DownloadState
import com.vkard.pro.domain.model.VersionInfo
import com.vkard.pro.domain.repository.UpdateRepository
import kotlinx.coroutines.flow.catch
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

    var downloadState by mutableStateOf<DownloadState>(DownloadState.Idle)
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

    fun checkForUpdates(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            isCheckingUpdates = true
            checkError = null
            updateRepository.getLatestVersionInfo(forceRefresh)
                .onSuccess { info ->
                    isCheckingUpdates = false
                    updateLastCheckedDisplay()
                    val currentVersionCode = BuildConfig.VERSION_CODE
                    if (info.versionCode > currentVersionCode) {
                        latestVersionInfo = info
                        if (info.forceUpdate) {
                            showUpdateBanner = false
                        } else {
                            showUpdateBanner = !updateRepository.isUpdateDismissed(info.versionCode)
                        }
                    } else {
                        latestVersionInfo = null
                        showUpdateBanner = false
                    }
                }
                .onFailure { error ->
                    isCheckingUpdates = false
                    checkError = error.message ?: "Failed to check updates"
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

    fun startDownload() {
        val info = latestVersionInfo ?: return
        viewModelScope.launch {
            updateManager.downloadApk(info.apk)
                .catch { e ->
                    downloadState = DownloadState.Failed(e.message ?: "Download failed")
                }
                .collect { state ->
                    downloadState = state
                    if (state is DownloadState.Completed) {
                        installApk(state.localUri)
                    }
                }
        }
    }

    fun installApk(filePath: String) {
        if (updateManager.canRequestPackageInstalls()) {
            updateManager.installApk(filePath)
        }
    }

    fun checkAndInstall() {
        val state = downloadState
        if (state is DownloadState.Completed) {
            updateManager.installApk(state.localUri)
        }
    }

    fun getRequestInstallIntent() = updateManager.requestInstallPermissionIntent()
    fun canRequestPackageInstalls() = updateManager.canRequestPackageInstalls()
}
