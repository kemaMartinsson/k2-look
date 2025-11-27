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
        private const val GITHUB_API_URL = "https://api.github.com/repos/OWNER/REPO/releases/latest"

        // Replace with your GitHub username and repository name
        private const val GITHUB_OWNER = "kemaMartinsson"
        private const val GITHUB_REPO = "k2-look"
    }

    /**
     * Check if a new version is available
     * @return AppUpdate if a newer version exists, null otherwise
     */
    suspend fun checkForUpdate(): AppUpdate? = withContext(Dispatchers.IO) {
        try {
            val apiUrl = GITHUB_API_URL
                .replace("OWNER", GITHUB_OWNER)
                .replace("REPO", GITHUB_REPO)

            val url = URL(apiUrl)
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
            val versionCode = extractVersionCode(tagName)

            // Check if this version is newer
            if (versionCode > BuildConfig.VERSION_CODE) {
                return AppUpdate(
                    version = tagName,
                    versionCode = versionCode,
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseNotes,
                    publishedAt = publishedAt,
                    isPrerelease = isPrerelease
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
     * Assumes format like "1.2.3" -> 10203 or just uses the current code + 1 as fallback
     */
    private fun extractVersionCode(version: String): Int {
        return try {
            val parts = version.split(".")
            when (parts.size) {
                1 -> parts[0].toInt() * 10000
                2 -> parts[0].toInt() * 10000 + parts[1].toInt() * 100
                else -> parts[0].toInt() * 10000 + parts[1].toInt() * 100 + parts[2].toInt()
            }
        } catch (e: Exception) {
            // Fallback: if we can't parse, assume it's newer
            BuildConfig.VERSION_CODE + 1
        }
    }
}

