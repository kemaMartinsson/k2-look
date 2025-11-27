package com.kema.k2look.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
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
     * @param onComplete Callback when download completes (true = success, false = failure)
     */
    fun downloadUpdate(update: AppUpdate, onComplete: (Boolean) -> Unit) {
        this.onDownloadComplete = onComplete

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
        val request = DownloadManager.Request(Uri.parse(update.downloadUrl)).apply {
            setTitle("K2Look Update")
            setDescription("Downloading version ${update.version}")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
        }

        downloadId = downloadManager.enqueue(request)
        Log.d(TAG, "Started download with ID: $downloadId")
    }

    /**
     * Handle download completion
     */
    private fun handleDownloadComplete() {
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

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                val uriString = cursor.getString(uriIndex)

                cursor.close()

                if (uriString != null) {
                    installApk(Uri.parse(uriString))
                    onDownloadComplete?.invoke(true)
                } else {
                    Log.e(TAG, "Download URI is null")
                    onDownloadComplete?.invoke(false)
                }
            } else {
                Log.e(TAG, "Download failed with status: $status")
                cursor.close()
                onDownloadComplete?.invoke(false)
            }
        } else {
            cursor.close()
            Log.e(TAG, "Download not found")
            onDownloadComplete?.invoke(false)
        }
    }

    /**
     * Install the downloaded APK
     */
    private fun installApk(uri: Uri) {
        try {
            val file = File(uri.path ?: return)

            val installUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Use FileProvider for Android 7.0+
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(installUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            context.startActivity(intent)

        } catch (e: Exception) {
            Log.e(TAG, "Error installing APK", e)
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

