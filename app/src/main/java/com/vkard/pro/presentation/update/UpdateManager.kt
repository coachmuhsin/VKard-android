package com.vkard.pro.presentation.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.vkard.pro.domain.model.DownloadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class UpdateManager(private val context: Context) {

    fun downloadApk(apkUrl: String): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0))

        val destinationDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "VKARD_PRO"
        )
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }

        val destinationFile = File(destinationDir, "VKARD-PRO.apk")
        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(apkUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.instanceFollowRedirects = true

            val status = connection.responseCode
            val contentType = connection.contentType ?: ""
            val contentLength = connection.contentLengthLong

            // Log required fields (Requirement 7)
            Log.d("VKARD_OTA_DOWNLOAD", "Download URL: $apkUrl")
            Log.d("VKARD_OTA_DOWNLOAD", "HTTP status: $status")
            Log.d("VKARD_OTA_DOWNLOAD", "Content-Type: $contentType")
            Log.d("VKARD_OTA_DOWNLOAD", "Saved file path: ${destinationFile.absolutePath}")

            // Validate response code (Requirement 4)
            if (status != HttpURLConnection.HTTP_OK) {
                Log.e("VKARD_OTA_DOWNLOAD", "HTTP Status is not 200: $status. URL: $apkUrl")
                emit(DownloadState.Failed("HTTP Status $status. URL: $apkUrl"))
                return@flow
            }

            // Validate Content-Type (Requirement 4)
            val isValidContentType = contentType.contains("application/vnd.android.package-archive", ignoreCase = true) ||
                    contentType.contains("application/octet-stream", ignoreCase = true)
            
            if (!isValidContentType || contentType.contains("text/html", ignoreCase = true)) {
                Log.e("VKARD_OTA_DOWNLOAD", "Invalid Content-Type or HTML response: $contentType. URL: $apkUrl")
                emit(DownloadState.Failed("Invalid Content-Type: $contentType. URL: $apkUrl"))
                return@flow
            }

            // Stream down the file
            val inputStream = BufferedInputStream(connection.inputStream)
            val outputStream = FileOutputStream(destinationFile)
            val data = ByteArray(8192)
            var totalBytesRead = 0L
            var bytesRead: Int

            while (inputStream.read(data).also { bytesRead = it } != -1) {
                outputStream.write(data, 0, bytesRead)
                totalBytesRead += bytesRead
                if (contentLength > 0) {
                    val progress = ((totalBytesRead * 100L) / contentLength).toInt()
                    emit(DownloadState.Downloading(progress))
                }
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()

            val fileSize = destinationFile.length()
            Log.d("VKARD_OTA_DOWNLOAD", "Downloaded file size: $fileSize bytes")

            // Post-download validations (Requirement 6)
            if (!destinationFile.exists()) {
                Log.e("VKARD_OTA_DOWNLOAD", "Saved file does not exist. URL: $apkUrl")
                emit(DownloadState.Failed("Downloaded file does not exist. URL: $apkUrl"))
                return@flow
            }

            if (fileSize < 5 * 1024 * 1024) { // Under 5 MB
                Log.e("VKARD_OTA_DOWNLOAD", "File size is under 5 MB: $fileSize bytes. URL: $apkUrl")
                emit(DownloadState.Failed("Downloaded file is invalid: size is ${fileSize / (1024 * 1024)}MB (expected > 5MB). URL: $apkUrl"))
                return@flow
            }

            if (!destinationFile.name.endsWith(".apk", ignoreCase = true)) {
                Log.e("VKARD_OTA_DOWNLOAD", "File extension is not .apk. URL: $apkUrl")
                emit(DownloadState.Failed("Downloaded file has invalid extension. URL: $apkUrl"))
                return@flow
            }

            emit(DownloadState.Completed(destinationFile.absolutePath))

        } catch (e: Exception) {
            Log.e("VKARD_OTA_DOWNLOAD", "Download exception for URL $apkUrl", e)
            emit(DownloadState.Failed("Download error: ${e.message}. URL: $apkUrl"))
        } finally {
            connection?.disconnect()
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

    fun getInstalledVersionCode(): Long {
        return com.vkard.pro.BuildConfig.VERSION_CODE.toLong()
    }

    fun getInstalledVersionName(): String {
        return com.vkard.pro.BuildConfig.VERSION_NAME
    }
}
