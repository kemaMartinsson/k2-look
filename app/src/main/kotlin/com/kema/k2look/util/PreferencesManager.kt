package com.kema.k2look.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Manager for application preferences and configuration
 *
 * Version 3: Simplified to use Karoo's UserProfile for units/locales
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Auto-connect settings
     */
    fun isAutoConnectKarooEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_CONNECT_KAROO, true)
    }

    fun setAutoConnectKaroo(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_CONNECT_KAROO, enabled) }
    }

    fun isAutoConnectActiveLookEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_CONNECT_ACTIVELOOK, false)
    }

    fun setAutoConnectActiveLook(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_CONNECT_ACTIVELOOK, enabled) }
    }

    /**
     * Last connected glasses
     */
    fun getLastConnectedGlassesAddress(): String? {
        return prefs.getString(KEY_LAST_GLASSES_ADDRESS, null)
    }

    fun setLastConnectedGlasses(address: String?) {
        prefs.edit { putString(KEY_LAST_GLASSES_ADDRESS, address) }
    }

    fun clearLastConnectedGlasses() {
        prefs.edit { remove(KEY_LAST_GLASSES_ADDRESS) }
    }

    /**
     * Reconnect timeout in minutes (for auto-reconnect attempts during rides)
     */
    fun getReconnectTimeoutMinutes(): Int {
        return prefs.getInt(KEY_RECONNECT_TIMEOUT_MINUTES, 10) // Default 10 minutes
    }

    fun setReconnectTimeoutMinutes(minutes: Int) {
        prefs.edit { putInt(KEY_RECONNECT_TIMEOUT_MINUTES, minutes) }
    }

    /**
     * Startup connection timeout in minutes (for initial connection attempt on boot)
     */
    fun getStartupTimeoutMinutes(): Int {
        return prefs.getInt(KEY_STARTUP_TIMEOUT_MINUTES, 10) // Default 10 minutes
    }

    fun setStartupTimeoutMinutes(minutes: Int) {
        prefs.edit { putInt(KEY_STARTUP_TIMEOUT_MINUTES, minutes) }
    }

    /**
     * Enable continuous reconnection attempts during active rides
     */
    fun isReconnectDuringRidesEnabled(): Boolean {
        return prefs.getBoolean(KEY_RECONNECT_DURING_RIDES, true) // Default: enabled
    }

    fun setReconnectDuringRides(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_RECONNECT_DURING_RIDES, enabled) }
    }

    /**
     * Disconnect glasses when ride ends (idle state)
     */
    fun isDisconnectWhenIdleEnabled(): Boolean {
        return prefs.getBoolean(KEY_DISCONNECT_WHEN_IDLE, false) // Default: keep connected
    }

    fun setDisconnectWhenIdle(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DISCONNECT_WHEN_IDLE, enabled) }
    }

    /**
     * Auto-check for updates on app start
     */
    fun isAutoCheckUpdatesEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_CHECK_UPDATES, true) // Default: enabled
    }

    fun setAutoCheckUpdates(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_CHECK_UPDATES, enabled) }
    }

    /**
     * Last time we checked for updates (timestamp in milliseconds)
     */
    fun getLastUpdateCheckTime(): Long {
        return prefs.getLong(KEY_LAST_UPDATE_CHECK, 0L)
    }

    fun setLastUpdateCheckTime(timestamp: Long) {
        prefs.edit { putLong(KEY_LAST_UPDATE_CHECK, timestamp) }
    }

    /**
     * Dismissed update version (user clicked "Later" for this version)
     */
    fun getDismissedUpdateVersion(): String? {
        return prefs.getString(KEY_DISMISSED_UPDATE_VERSION, null)
    }

    fun setDismissedUpdateVersion(version: String?) {
        prefs.edit { putString(KEY_DISMISSED_UPDATE_VERSION, version) }
    }

    companion object {
        private const val PREFS_NAME = "k2look_preferences"
        private const val KEY_AUTO_CONNECT_KAROO = "auto_connect_karoo"
        private const val KEY_AUTO_CONNECT_ACTIVELOOK = "auto_connect_activelook"
        private const val KEY_LAST_GLASSES_ADDRESS = "last_glasses_address"
        private const val KEY_RECONNECT_TIMEOUT_MINUTES = "reconnect_timeout_minutes"
        private const val KEY_STARTUP_TIMEOUT_MINUTES = "startup_timeout_minutes"
        private const val KEY_RECONNECT_DURING_RIDES = "reconnect_during_rides"
        private const val KEY_DISCONNECT_WHEN_IDLE = "disconnect_when_idle"
        private const val KEY_AUTO_CHECK_UPDATES = "auto_check_updates"
        private const val KEY_LAST_UPDATE_CHECK = "last_update_check"
        private const val KEY_DISMISSED_UPDATE_VERSION = "dismissed_update_version"
    }
}
