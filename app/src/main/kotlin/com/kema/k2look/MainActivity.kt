package com.kema.k2look

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.kema.k2look.screens.MainScreen
import com.kema.k2look.theme.AppTheme
import com.kema.k2look.update.UpdateChecker
import com.kema.k2look.update.UpdateNotificationManager
import com.kema.k2look.util.PermissionUtils
import com.kema.k2look.util.PreferencesManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var permissionsGranted by mutableStateOf(false)
    private var showRationaleDialog by mutableStateOf(false)
    private var showSettingsDialog by mutableStateOf(false)
    private var showInitialRationaleDialog by mutableStateOf(false)

    private val permsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val denied = results.filterValues { granted -> !granted }.keys
        if (denied.isEmpty()) {
            // All permissions granted
            onPermissionsGranted()
            return@registerForActivityResult
        }

        // Check if any permission was permanently denied (user selected "Don't ask again")
        val permanentlyDenied = denied.any { perm ->
            !shouldShowRequestPermissionRationale(perm)
        }

        if (permanentlyDenied) {
            showSettingsDialog = true
        } else {
            showRationaleDialog = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set content first to show dialogs
        setContent {
            AppTheme {
                if (permissionsGranted) {
                    BackHandler {
                        finish()
                    }
                    MainScreen(
                        onBack = { finish() }
                    )
                }

                // Permission dialogs
                if (showInitialRationaleDialog) {
                    InitialRationaleDialog(
                        onContinue = {
                            showInitialRationaleDialog = false
                            permsLauncher.launch(PermissionUtils.requiredPermissions())
                        },
                        onExit = {
                            showInitialRationaleDialog = false
                            finish()
                        }
                    )
                }

                if (showRationaleDialog) {
                    RationaleDialog(
                        onGrantPermissions = {
                            showRationaleDialog = false
                            permsLauncher.launch(PermissionUtils.requiredPermissions())
                        },
                        onExit = {
                            showRationaleDialog = false
                            finish()
                        }
                    )
                }

                if (showSettingsDialog) {
                    SettingsDialog(
                        onOpenSettings = {
                            showSettingsDialog = false
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", packageName, null)
                                }
                            startActivity(intent)
                        },
                        onCancel = {
                            showSettingsDialog = false
                            finish()
                        }
                    )
                }
            }
        }

        // Check and request permissions if needed
        if (!PermissionUtils.allGranted(this)) {
            requestPermissions()
        } else {
            onPermissionsGranted()
        }
    }

    override fun onResume() {
        super.onResume()

        // Check permissions again when returning from settings
        if (!permissionsGranted && PermissionUtils.allGranted(this)) {
            onPermissionsGranted()
        }
    }

    private fun requestPermissions() {
        val missingPermissions = PermissionUtils.getMissingPermissions(this)

        if (missingPermissions.isEmpty()) {
            onPermissionsGranted()
            return
        }

        // Show rationale if needed
        if (PermissionUtils.anyShouldShowRationale(this)) {
            showInitialRationaleDialog = true
        } else {
            // Request permissions directly
            permsLauncher.launch(PermissionUtils.requiredPermissions())
        }
    }

    private fun onPermissionsGranted() {
        if (permissionsGranted) {
            return // Already initialized
        }

        permissionsGranted = true

        // Check for updates if auto-check is enabled
        checkForUpdatesIfEnabled()
    }

    private fun checkForUpdatesIfEnabled() {
        val prefsManager = PreferencesManager(this)

        if (!prefsManager.isAutoCheckUpdatesEnabled()) {
            return
        }

        // Only check once per day
        val lastCheck = prefsManager.getLastUpdateCheckTime()
        val dayInMillis = 24 * 60 * 60 * 1000L
        if (System.currentTimeMillis() - lastCheck < dayInMillis) {
            return
        }

        lifecycleScope.launch {
            try {
                val updateChecker = UpdateChecker(this@MainActivity)
                val update = updateChecker.checkForUpdate()

                prefsManager.setLastUpdateCheckTime(System.currentTimeMillis())

                if (update != null) {
                    // Don't show notification if user already dismissed this version
                    val dismissedVersion = prefsManager.getDismissedUpdateVersion()
                    if (dismissedVersion != update.version) {
                        val notificationManager = UpdateNotificationManager(this@MainActivity)
                        notificationManager.showUpdateAvailableNotification(update)
                    }
                }
            } catch (_: Exception) {
                // Silent failure for background check
            }
        }
    }
}

@Composable
private fun InitialRationaleDialog(
    onContinue: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Prevent dismissal */ },
        title = { Text("Permissions Needed") },
        text = {
            Text(
                "K2Look needs the following permissions to work:\n\n" +
                        "• Bluetooth - to connect to your glasses\n" +
                        "• Location - required for Bluetooth scanning on Android\n" +
                        "• Notifications - to notify you of updates\n\n" +
                        "Your location data is not collected or shared."
            )
        },
        confirmButton = {
            Button(onClick = onContinue) {
                Text("Continue")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onExit) {
                Text("Exit")
            }
        }
    )
}

@Composable
private fun RationaleDialog(
    onGrantPermissions: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Prevent dismissal */ },
        title = { Text("Permissions Needed") },
        text = {
            Text(
                "K2Look needs Bluetooth and location permissions to discover and connect to your ActiveLook glasses.\n\n" +
                        "Without these permissions, the app cannot function."
            )
        },
        confirmButton = {
            Button(onClick = onGrantPermissions) {
                Text("Grant Permissions")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onExit) {
                Text("Exit")
            }
        }
    )
}

@Composable
private fun SettingsDialog(
    onOpenSettings: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Prevent dismissal */ },
        title = { Text("Permissions Required") },
        text = {
            Text(
                "K2Look needs Bluetooth and location permissions to scan and connect to your glasses.\n\n" +
                        "Please enable these permissions in app settings."
            )
        },
        confirmButton = {
            Button(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

