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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class UpdateManager(private val context: Context) {

    fun downloadApk(apkUrl: String): Flow<DownloadState> = flow {
        Log.d("VKARD_OTA", "VKARD OTA: Checking updates")
        emit(DownloadState.Downloading(0))

        val destinationDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "VKARD_PRO"
        )
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }

        // Clean previous downloads (Requirement 10 & 11)
        destinationDir.listFiles()?.forEach { file ->
            try {
                file.delete()
            } catch (e: Exception) {
                // ignore
            }
        }

        val destinationFile = File(destinationDir, "VKARD-PRO.apk")

        Log.d("VKARD_OTA", "VKARD OTA: Download started")

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val request = Request.Builder()
            .url(apkUrl)
            .build()

        try {
            val response = client.newCall(request).execute()
            val httpCode = response.code
            val contentType = response.body?.contentType()?.toString() ?: ""
            val contentLength = response.body?.contentLength() ?: -1L
            val finalUrl = response.request.url.toString()
            val redirectUrl = response.priorResponse?.request?.url?.toString() ?: ""

            // Log details with VKARD OTA prefix (Requirement 1 & 12)
            Log.d("VKARD_OTA", "VKARD OTA: Download URL = $apkUrl")
            Log.d("VKARD_OTA", "VKARD OTA: HTTP $httpCode")
            Log.d("VKARD_OTA", "VKARD OTA: Content-Type = $contentType")
            Log.d("VKARD_OTA", "VKARD OTA: Content-Length = $contentLength")
            if (redirectUrl.isNotEmpty()) {
                Log.d("VKARD_OTA", "VKARD OTA: Redirect URL = $redirectUrl")
            }
            Log.d("VKARD_OTA", "VKARD OTA: Final resolved URL = $finalUrl")

            // Validate status code
            if (httpCode != 200) {
                Log.e("VKARD_OTA", "VKARD OTA: Download failed - HTTP status is not 200: $httpCode")
                emit(DownloadState.Failed("HTTP status $httpCode. URL: $apkUrl"))
                return@flow
            }

            // Validate Content-Type (Requirement 4)
            val isValidContentType = contentType.contains("application/vnd.android.package-archive", ignoreCase = true) ||
                    contentType.contains("application/octet-stream", ignoreCase = true)

            if (!isValidContentType || contentType.contains("text/html", ignoreCase = true)) {
                Log.e("VKARD_OTA", "VKARD OTA: Download failed - Invalid Content-Type or HTML response: $contentType")
                emit(DownloadState.Failed("Invalid Content-Type: $contentType. URL: $apkUrl"))
                return@flow
            }

            val body = response.body
            if (body == null) {
                Log.e("VKARD_OTA", "VKARD OTA: Download failed - Empty response body")
                emit(DownloadState.Failed("Empty response body. URL: $apkUrl"))
                return@flow
            }

            // Stream download (Requirement 3)
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(destinationFile)
            val buffer = ByteArray(8192)
            var totalBytesRead = 0L
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
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
            Log.d("VKARD_OTA", "VKARD OTA: Saved file = ${destinationFile.absolutePath}")
            Log.d("VKARD_OTA", "VKARD OTA: Size = $fileSize")

            // Post-download validations (Requirement 4)
            if (!destinationFile.exists()) {
                Log.e("VKARD_OTA", "VKARD OTA: Saved file does not exist")
                emit(DownloadState.Failed("Downloaded file does not exist. URL: $apkUrl"))
                return@flow
            }

            if (!destinationFile.name.endsWith(".apk", ignoreCase = true)) {
                Log.e("VKARD_OTA", "VKARD OTA: File extension is not .apk")
                destinationFile.delete()
                emit(DownloadState.Failed("Downloaded file has invalid extension. URL: $apkUrl"))
                return@flow
            }

            if (fileSize < 10 * 1024 * 1024) { // Under 10 MB (Requirement 4)
                Log.e("VKARD_OTA", "VKARD OTA: File size is under 10 MB: $fileSize bytes")
                destinationFile.delete()
                emit(DownloadState.Failed("Downloaded APK is too small: ${fileSize / (1024 * 1024)}MB (expected > 10MB). URL: $apkUrl"))
                return@flow
            }

            if (contentLength > 0 && fileSize != contentLength) {
                Log.e("VKARD_OTA", "VKARD OTA: File size ($fileSize) differs from Content-Length ($contentLength)")
                destinationFile.delete()
                emit(DownloadState.Failed("File size mismatch. Downloaded: $fileSize, Expected: $contentLength. URL: $apkUrl"))
                return@flow
            }

            // Calculate and log SHA-256 (Requirement 5)
            val sha256 = calculateSha256(destinationFile)
            Log.d("VKARD_OTA", "VKARD OTA: SHA256 = $sha256")

            emit(DownloadState.Completed(destinationFile.absolutePath))

        } catch (e: Exception) {
            Log.e("VKARD_OTA", "VKARD OTA: Download exception", e)
            emit(DownloadState.Failed("Download error: ${e.message}. URL: $apkUrl"))
        }
    }.flowOn(Dispatchers.IO)

    private fun calculateSha256(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun verifyPackageArchive(filePath: String): Result<Unit> {
        val file = File(filePath)
        if (!file.exists()) {
            return Result.failure(Exception("APK file does not exist at $filePath"))
        }
        return try {
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(filePath, 0)
            if (info != null && info.packageName.isNotEmpty()) {
                Log.d("VKARD_OTA", "VKARD OTA: Package verified")
                Result.success(Unit)
            } else {
                file.delete()
                Result.failure(Exception("Downloaded APK is corrupted. Package information could not be read."))
            }
        } catch (e: Exception) {
            file.delete()
            Result.failure(Exception("Downloaded APK is corrupted. Verification error: ${e.message}"))
        }
    }

    fun installApk(filePath: String): Result<Unit> {
        Log.d("VKARD_OTA", "VKARD OTA: Launch installer")
        val verification = verifyPackageArchive(filePath)
        if (verification.isFailure) {
            val errorMsg = verification.exceptionOrNull()?.message ?: "Unknown corruption error"
            Log.e("VKARD_OTA", "VKARD OTA: Package verification failed: $errorMsg")
            return Result.failure(Exception(errorMsg))
        }

        val file = File(filePath)
        val apkUri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e("VKARD_OTA", "VKARD OTA: FileProvider failed: ${e.message}")
            return Result.failure(Exception("FileProvider URI sharing failed: ${e.message}"))
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        return try {
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("VKARD_OTA", "VKARD OTA: startActivity failed: ${e.message}")
            Result.failure(Exception("Failed to launch package installer activity: ${e.message}"))
        }
    }

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

    fun getInstalledVersionCode(): Long {
        return com.vkard.pro.BuildConfig.VERSION_CODE.toLong()
    }

    fun getInstalledVersionName(): String {
        return com.vkard.pro.BuildConfig.VERSION_NAME
    }
}
