package com.kema.k2look

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.kema.k2look.screens.MainScreen
import com.kema.k2look.theme.AppTheme
import com.kema.k2look.update.UpdateChecker
import com.kema.k2look.update.UpdateNotificationManager
import com.kema.k2look.util.PreferencesManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for updates if auto-check is enabled
        checkForUpdatesIfEnabled()

        setContent {
            AppTheme {
                BackHandler {
                    finish()
                }
                MainScreen(
                    onBack = { finish() }
                )
            }
        }
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
            } catch (e: Exception) {
                // Silent failure for background check
            }
        }
    }
}

