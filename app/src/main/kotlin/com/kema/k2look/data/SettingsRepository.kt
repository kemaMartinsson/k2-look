package com.kema.k2look.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Repository for app-level settings stored in SharedPreferences. */
class SettingsRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "k2look_settings"
        private const val KEY_KAROO_SYNC = "karoo_sync_enabled"
        private const val KEY_RADAR_WARNING = "radar_warning_enabled"
        private const val KEY_BATTERY_DISPLAY = "battery_display_enabled"
        private const val KEY_SAVE_LOGS_TO_FILE = "save_logs_to_file_enabled"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _karooSyncEnabled = MutableStateFlow(prefs.getBoolean(KEY_KAROO_SYNC, true))
    val karooSyncEnabled: StateFlow<Boolean> = _karooSyncEnabled.asStateFlow()

    fun setKarooSyncEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_KAROO_SYNC, enabled) }
        _karooSyncEnabled.value = enabled
    }

    private val _radarWarningEnabled = MutableStateFlow(prefs.getBoolean(KEY_RADAR_WARNING, true))
    val radarWarningEnabled: StateFlow<Boolean> = _radarWarningEnabled.asStateFlow()

    fun setRadarWarningEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_RADAR_WARNING, enabled) }
        _radarWarningEnabled.value = enabled
    }

    private val _batteryDisplayEnabled =
            MutableStateFlow(prefs.getBoolean(KEY_BATTERY_DISPLAY, true))
    val batteryDisplayEnabled: StateFlow<Boolean> = _batteryDisplayEnabled.asStateFlow()

    fun setBatteryDisplayEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_BATTERY_DISPLAY, enabled) }
        _batteryDisplayEnabled.value = enabled
    }

    private val _saveLogsToFileEnabled =
            MutableStateFlow(prefs.getBoolean(KEY_SAVE_LOGS_TO_FILE, false))
    val saveLogsToFileEnabled: StateFlow<Boolean> = _saveLogsToFileEnabled.asStateFlow()

    fun setSaveLogsToFileEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SAVE_LOGS_TO_FILE, enabled) }
        _saveLogsToFileEnabled.value = enabled
    }
}
