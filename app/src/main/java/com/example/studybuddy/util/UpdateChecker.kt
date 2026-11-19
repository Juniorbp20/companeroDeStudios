package com.example.studybuddy.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    /** Repositorio donde el CD (`.github/workflows/cd.yml`) publica las releases. */
    private const val GITHUB_REPO = "Juniorbp20/companeroDeStudios"
    private const val GITHUB_API_LATEST =
        "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    private val VERSION_TAG_REGEX = Regex("""^v?(\d+)""")

    /**
     * Extrae el versionCode del tag de la release. Convención: el tag es `v<versionCode>`
     * (p. ej. `v8`); también se aceptan `v8.0` o `8.1.2`, de los que se toma el primer número.
     */
    fun parseVersionCode(tag: String): Int? =
        VERSION_TAG_REGEX.find(tag.trim())?.groupValues?.get(1)?.toIntOrNull()

    data class UpdateInfo(
        val versionCode: Int,
        val versionName: String,
        val apkUrl: String
    )

    suspend fun checkLatest(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(GITHUB_API_LATEST).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            connection.setRequestProperty("User-Agent", "StudyBuddy-Android")
            if (connection.responseCode != 200) {
                return@withContext null
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(body)
            val tag = root.optString("tag_name", "")
            val version = parseVersionCode(tag) ?: return@withContext null
            val assets = root.optJSONArray("assets") ?: return@withContext UpdateInfo(version, tag, "")
            var apkUrl = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name", "").endsWith(".apk")) {
                    apkUrl = asset.optString("browser_download_url", "")
                    break
                }
            }
            UpdateInfo(version, tag, apkUrl)
        } catch (e: Exception) {
            null
        }
    }

    fun isUpdateAvailable(installedVersionCode: Int, remote: UpdateInfo): Boolean =
        remote.versionCode > installedVersionCode

    suspend fun downloadApk(
        context: Context,
        url: String,
        onProgress: (downloaded: Int, total: Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val target = File(context.getExternalFilesDir(null), "studybuddy_update.apk")
            target.delete()
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            val total = connection.contentLength
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded.toInt(), total)
                    }
                }
            }
            target
        } catch (e: Exception) {
            null
        }
    }

    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    fun installApk(context: Context, file: File): Boolean = try {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }

    fun openInstallPermissionSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        )
        context.startActivity(intent)
    }
}