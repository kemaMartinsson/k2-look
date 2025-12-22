package com.kema.k2look.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtils {
    /**
     * Returns the list of required runtime permissions based on Android version
     */
    fun requiredPermissions(): Array<String> {
        val perms = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires new Bluetooth permissions
            perms += Manifest.permission.BLUETOOTH_SCAN
            perms += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            // Android < 12 requires location permissions for BLE scanning
            perms += Manifest.permission.ACCESS_FINE_LOCATION
            perms += Manifest.permission.ACCESS_COARSE_LOCATION
        }

        // Android 13+ requires notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.POST_NOTIFICATIONS
        }

        return perms.toTypedArray()
    }

    /**
     * Check if all required permissions are granted
     */
    fun allGranted(context: Context): Boolean =
        requiredPermissions().all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }

    /**
     * Check if we should show rationale for any required permission
     */
    fun anyShouldShowRationale(activity: Activity): Boolean =
        requiredPermissions().any { perm ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
        }

    /**
     * Get the list of permissions that are not yet granted
     */
    fun getMissingPermissions(context: Context): List<String> =
        requiredPermissions().filter { perm ->
            ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
        }
}

