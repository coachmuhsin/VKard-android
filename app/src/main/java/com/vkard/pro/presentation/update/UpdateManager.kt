package com.vkard.pro.presentation.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.vkard.pro.domain.model.DownloadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

class UpdateManager(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun downloadApk(apkUrl: String): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0))

        val destinationDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "VKARD PRO"
        )
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }

        val destinationFile = File(destinationDir, "VKARD-PRO.apk")
        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        try {
            val request = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("VKARD PRO Update")
                .setDescription("Downloading latest VKARD PRO update...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "VKARD PRO/VKARD-PRO.apk")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadId = downloadManager.enqueue(request)
            var downloading = true

            while (downloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = cursor.getInt(statusIdx)

                    val downloadedIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val downloaded = cursor.getInt(downloadedIdx)

                    val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val total = cursor.getInt(totalIdx)

                    if (total > 0) {
                        val progress = (downloaded * 100L / total).toInt()
                        emit(DownloadState.Downloading(progress))
                    }

                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            downloading = false
                            emit(DownloadState.Completed(destinationFile.absolutePath))
                        }
                        DownloadManager.STATUS_FAILED -> {
                            downloading = false
                            val reasonIdx = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                            val reason = cursor.getInt(reasonIdx)
                            emit(DownloadState.Failed("Download failed (code: $reason)"))
                        }
                    }
                }
                cursor?.close()
                if (downloading) {
                    delay(500)
                }
            }
        } catch (e: Exception) {
            emit(DownloadState.Failed(e.message ?: "Failed to initiate download"))
        }
    }.flowOn(Dispatchers.IO)

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun requestInstallPermissionIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            null
        }
    }

    fun installApk(filePath: String): Boolean {
        val file = File(filePath)
        if (!file.exists()) return false

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
