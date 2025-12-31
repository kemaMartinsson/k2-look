package com.kema.k2look.update

import android.content.Context
import android.util.Log
import com.kema.k2look.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Data class representing a GitHub release
 */
data class AppUpdate(
    val version: String,
    val versionCode: Int,
    val downloadUrl: String,
    val htmlUrl: String,
    val releaseNotes: String,
    val publishedAt: String,
    val isPrerelease: Boolean
)

/**
 * Manages checking for app updates from GitHub releases
 */
class UpdateChecker(private val context: Context) {

    companion object {
        private const val TAG = "UpdateChecker"

        // Replace with your GitHub username and repository name
        private const val GITHUB_OWNER = "kemaMartinsson"
        private const val GITHUB_REPO = "k2-look"
        private const val GITHUB_API_URL =
            "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    }

    /**
     * Check if a new version is available
     * @return AppUpdate if a newer version exists, null otherwise
     */
    suspend fun checkForUpdate(): AppUpdate? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking for updates at: $GITHUB_API_URL")
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpsURLConnection

            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                connectTimeout = 10000
                readTimeout = 10000
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                Log.e(TAG, "GitHub API returned: $responseCode")
                return@withContext null
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            parseReleaseJson(response)

        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            null
        }
    }

    /**
     * Parse GitHub release JSON response
     */
    private fun parseReleaseJson(jsonString: String): AppUpdate? {
        try {
            val json = JSONObject(jsonString)

            val tagName = json.getString("tag_name").removePrefix("v")
            val htmlUrl = json.getString("html_url")
            val releaseNotes = json.getString("body")
            val publishedAt = json.getString("published_at")
            val isPrerelease = json.getBoolean("prerelease")

            // Find the APK asset
            val assets = json.getJSONArray("assets")
            var downloadUrl: String? = null

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.endsWith(".apk")) {
                    downloadUrl = asset.getString("browser_download_url")
                    break
                }
            }

            if (downloadUrl == null) {
                Log.e(TAG, "No APK found in release")
                return null
            }

            // Extract version code from tag or use simple comparison
            val remoteVersionCode = extractVersionCode(tagName)
            val currentVersionCode = extractVersionCode(BuildConfig.VERSION_NAME)

            Log.d(
                TAG,
                "Current version: ${BuildConfig.VERSION_NAME} (parsed code: $currentVersionCode)"
            )
            Log.d(TAG, "Remote version: $tagName (parsed code: $remoteVersionCode)")

            // Check if this version is newer - compare parsed version codes from version strings
            val isNewer = remoteVersionCode > currentVersionCode

            if (isNewer) {
                Log.d(TAG, "Update available: $tagName (htmlUrl: $htmlUrl)")
                return AppUpdate(
                    version = tagName,
                    versionCode = remoteVersionCode,
                    downloadUrl = downloadUrl,
                    htmlUrl = htmlUrl,
                    releaseNotes = releaseNotes,
                    publishedAt = publishedAt,
                    isPrerelease = isPrerelease
                )
            } else {
                Log.d(
                    TAG,
                    "No update needed, current version $currentVersionCode >= remote version $remoteVersionCode"
                )
            }

            return null

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing release JSON", e)
            return null
        }
    }

    /**
     * Extract version code from version string
     * Assumes format like "0.11" -> 1100, "0.12.1" -> 1201, "1.2.3" -> 10203
     * For 0.x versions, use minor*100 + patch to support patch versions
     */
    private fun extractVersionCode(version: String): Int {
        return try {
            val parts = version.split(".")
            when {
                parts.isEmpty() -> 0
                parts[0] == "0" && parts.size >= 2 -> {
                    // For 0.x versions, calculate: minor*100 + patch
                    // 0.11 -> 1100, 0.12 -> 1200, 0.12.1 -> 1201
                    val minor = parts[1].toInt()
                    val patch = if (parts.size >= 3) parts[2].toInt() else 0
                    minor * 100 + patch
                }

                parts.size == 1 -> parts[0].toInt() * 10000
                parts.size == 2 -> parts[0].toInt() * 10000 + parts[1].toInt() * 100
                else -> parts[0].toInt() * 10000 + parts[1].toInt() * 100 + parts[2].toInt()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse version: $version", e)
            // Return 0 if we can't parse
            0
        }
    }
}

