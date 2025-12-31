package com.kema.k2look.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/**
 * Manages downloading and installing APK updates
 */
class UpdateDownloader(private val context: Context) {

    companion object {
        private const val TAG = "UpdateDownloader"
        private const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "update_download"
    }

    private var downloadId: Long = -1
    private var onDownloadComplete: ((Boolean) -> Unit)? = null
    private var onDownloadProgress: ((Int) -> Unit)? = null

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
            if (id == downloadId) {
                handleDownloadComplete()
            }
        }
    }

    /**
     * Download an APK update
     * @param update The update to download
     * @param onProgress Callback for download progress (0-100)
     * @param onComplete Callback when download completes (true = success, false = failure)
     */
    fun downloadUpdate(
        update: AppUpdate,
        onProgress: (Int) -> Unit = {},
        onComplete: (Boolean) -> Unit
    ) {
        this.onDownloadComplete = onComplete
        this.onDownloadProgress = onProgress

        // Register download completion receiver
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(downloadReceiver, filter)
        }

        // Start download
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val fileName = "K2Look-${update.version}.apk"

        // Use app's external cache directory - no permissions needed
        val destination = File(context.getExternalFilesDir(null), fileName)
        if (destination.exists()) {
            destination.delete()
        }

        Log.d(TAG, "Download destination: ${destination.absolutePath}")

        val request = DownloadManager.Request(Uri.parse(update.downloadUrl)).apply {
            setTitle("K2Look Update")
            setDescription("Downloading version ${update.version}")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationUri(Uri.fromFile(destination))
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
        }

        downloadId = downloadManager.enqueue(request)
        Log.d(TAG, "Started download with ID: $downloadId")

        // Start monitoring download progress
        startProgressMonitoring()
    }

    /**
     * Monitor download progress and report via callback
     */
    private fun startProgressMonitoring() {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        Thread {
            var downloading = true
            while (downloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)

                if (cursor.moveToFirst()) {
                    val bytesDownloaded = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    )
                    val bytesTotal = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    )

                    if (bytesTotal > 0) {
                        val progress = ((bytesDownloaded * 100) / bytesTotal).toInt()
                        onDownloadProgress?.invoke(progress)
                        Log.d(
                            TAG,
                            "Download progress: $progress% ($bytesDownloaded / $bytesTotal bytes)"
                        )
                    }

                    val status =
                        cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    if (status == DownloadManager.STATUS_SUCCESSFUL ||
                        status == DownloadManager.STATUS_FAILED
                    ) {
                        downloading = false
                    }
                }
                cursor.close()

                if (downloading) {
                    Thread.sleep(100) // Update every 100ms
                }
            }
        }.start()
    }

    /**
     * Handle download completion
     */
    private fun handleDownloadComplete() {
        Log.d(TAG, "Download completed, handling...")

        try {
            context.unregisterReceiver(downloadReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        if (cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val status = cursor.getInt(statusIndex)

            Log.d(TAG, "Download status: $status (${getStatusString(status)})")

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                val uriString = cursor.getString(uriIndex)

                Log.d(TAG, "Download URI: $uriString")

                cursor.close()

                if (uriString != null) {
                    installApk(Uri.parse(uriString))
                    onDownloadComplete?.invoke(true)
                } else {
                    Log.e(TAG, "Download URI is null")
                    onDownloadComplete?.invoke(false)
                }
            } else {
                // Get reason if failed
                val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                val reason = if (reasonIndex >= 0) cursor.getInt(reasonIndex) else -1
                Log.e(TAG, "Download failed with status: $status, reason: $reason")
                cursor.close()
                onDownloadComplete?.invoke(false)
            }
        } else {
            cursor.close()
            Log.e(TAG, "Download not found")
            onDownloadComplete?.invoke(false)
        }
    }

    private fun getStatusString(status: Int): String {
        return when (status) {
            DownloadManager.STATUS_PENDING -> "PENDING"
            DownloadManager.STATUS_RUNNING -> "RUNNING"
            DownloadManager.STATUS_PAUSED -> "PAUSED"
            DownloadManager.STATUS_SUCCESSFUL -> "SUCCESSFUL"
            DownloadManager.STATUS_FAILED -> "FAILED"
            else -> "UNKNOWN($status)"
        }
    }

    /**
     * Install the downloaded APK
     */
    private fun installApk(uri: Uri) {
        try {
            Log.d(TAG, "Installing APK from URI: $uri")

            // Check if we have permission to install unknown apps (Android 8.0+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canInstall = context.packageManager.canRequestPackageInstalls()
                if (!canInstall) {
                    Log.w(TAG, "Permission to install unknown apps not granted, opening settings")
                    // Open settings to allow installing from this source
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    return
                }
            }

            // Convert file:// URI to actual File
            val filePath = uri.path?.removePrefix("file://") ?: run {
                Log.e(TAG, "Invalid URI path: ${uri.path}")
                return
            }

            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "APK file does not exist at: ${file.absolutePath}")
                return
            }

            Log.d(TAG, "APK file exists at: ${file.absolutePath}")
            Log.d(TAG, "APK file size: ${file.length()} bytes")
            Log.d(TAG, "APK readable: ${file.canRead()}")

            val installUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Use FileProvider for Android 7.0+
                Log.d(TAG, "Using FileProvider for installation")
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Log.d(TAG, "Using direct file URI for installation")
                Uri.fromFile(file)
            }

            Log.d(TAG, "Install URI: $installUri")

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(installUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            context.startActivity(intent)
            Log.d(TAG, "Install intent started successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error installing APK", e)
            Log.e(TAG, "URI was: $uri")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
        }
    }

    /**
     * Cancel ongoing download
     */
    fun cancelDownload() {
        if (downloadId != -1L) {
            try {
                val downloadManager =
                    context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                downloadManager.remove(downloadId)
                context.unregisterReceiver(downloadReceiver)
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling download", e)
            }
            downloadId = -1
        }
    }
}

