package com.example.data.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class AppReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseTitle: String,
    val changelog: String,
    val apkDownloadUrl: String,
    val apkFileName: String,
    val apkSizeBytes: Long
)

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val release: AppReleaseInfo, val currentVersion: String) : UpdateState()
    data class UpToDate(val currentVersion: String) : UpdateState()
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : UpdateState()
    data class ReadyToInstall(val apkFile: File, val release: AppReleaseInfo) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class AppUpdateManager(private val context: Context) {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    val currentVersionName: String = BuildConfig.VERSION_NAME

    suspend fun checkForUpdates(githubRepo: String, isUserInitiated: Boolean = false): UpdateState {
        if (githubRepo.isBlank()) {
            val state = UpdateState.Error("No se ha configurado un repositorio de GitHub.")
            if (isUserInitiated) _updateState.value = state
            return state
        }

        _updateState.value = UpdateState.Checking

        return withContext(Dispatchers.IO) {
            try {
                val cleanRepo = githubRepo.trim().removePrefix("https://github.com/").removeSuffix(".git")
                val apiUrl = "https://api.github.com/repos/$cleanRepo/releases/latest"

                val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "MinimalLauncher-Android")
                }

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    val errorMsg = if (responseCode == 404) {
                        "Repositorio o release no encontrado en GitHub ($cleanRepo)"
                    } else {
                        "Error al consultar GitHub (Código $responseCode)"
                    }
                    val state = UpdateState.Error(errorMsg)
                    _updateState.value = state
                    return@withContext state
                }

                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseBody)

                val tagName = json.optString("tag_name", "")
                val releaseTitle = json.optString("name", tagName)
                val changelog = json.optString("body", "Sin notas de versión disponibles.")

                val assets = json.optJSONArray("assets")
                var downloadUrl = ""
                var apkFileName = "MinimalLauncher.apk"
                var apkSize = 0L

                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            apkFileName = name
                            apkSize = asset.optLong("size", 0L)
                            break
                        }
                    }
                }

                if (downloadUrl.isEmpty()) {
                    val state = UpdateState.Error("El último release no contiene ningún archivo .apk adjunto.")
                    _updateState.value = state
                    return@withContext state
                }

                val remoteVersion = tagName.removePrefix("v").trim()
                val isNewer = isVersionNewer(remoteVersion, currentVersionName)

                val state = if (isNewer) {
                    UpdateState.UpdateAvailable(
                        release = AppReleaseInfo(
                            tagName = tagName,
                            versionName = remoteVersion,
                            releaseTitle = releaseTitle,
                            changelog = changelog,
                            apkDownloadUrl = downloadUrl,
                            apkFileName = apkFileName,
                            apkSizeBytes = apkSize
                        ),
                        currentVersion = currentVersionName
                    )
                } else {
                    UpdateState.UpToDate(currentVersion = currentVersionName)
                }

                _updateState.value = state
                state
            } catch (e: Exception) {
                val state = UpdateState.Error(e.localizedMessage ?: "Error al comprobar actualizaciones")
                _updateState.value = state
                state
            }
        }
    }

    suspend fun downloadUpdate(release: AppReleaseInfo) {
        withContext(Dispatchers.IO) {
            try {
                val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
                // Clean older apk downloads in cache
                updatesDir.listFiles()?.forEach { it.delete() }

                val destinationFile = File(updatesDir, release.apkFileName)

                var targetUrl = release.apkDownloadUrl
                var redirects = 0
                var conn: HttpURLConnection

                do {
                    conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 15000
                        readTimeout = 30000
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "MinimalLauncher-Android")
                    }

                    val code = conn.responseCode
                    if (code in 300..399) {
                        val location = conn.getHeaderField("Location")
                        if (!location.isNullOrEmpty()) {
                            targetUrl = location
                            conn.disconnect()
                            redirects++
                        } else {
                            break
                        }
                    } else {
                        break
                    }
                } while (redirects < 5)

                val contentLength = conn.contentLengthLong.let { if (it > 0) it else release.apkSizeBytes }

                conn.inputStream.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var totalRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead

                            val progress = if (contentLength > 0) {
                                (totalRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0.5f
                            }
                            _updateState.value = UpdateState.Downloading(
                                progress = progress,
                                downloadedBytes = totalRead,
                                totalBytes = contentLength
                            )
                        }
                        output.flush()
                    }
                }

                _updateState.value = UpdateState.ReadyToInstall(
                    apkFile = destinationFile,
                    release = release
                )
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error("Error al descargar APK: ${e.localizedMessage}")
            }
        }
    }

    fun installApk(apkFile: File) {
        try {
            // Check Unknown Sources permission on Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Error al iniciar instalación: ${e.localizedMessage}")
        }
    }

    fun dismissState() {
        _updateState.value = UpdateState.Idle
    }

    companion object {
        fun isVersionNewer(remoteVersion: String, currentVersion: String): Boolean {
            val cleanRemote = remoteVersion.removePrefix("v").trim()
            val cleanCurrent = currentVersion.removePrefix("v").trim()

            val remoteParts = cleanRemote.split(".").map { part ->
                part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
            }
            val currentParts = cleanCurrent.split(".").map { part ->
                part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
            }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        }
    }
}
