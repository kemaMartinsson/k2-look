package com.kema.k2look.util

import android.content.Context
import android.content.SharedPreferences

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
        prefs.edit().putBoolean(KEY_AUTO_CONNECT_KAROO, enabled).apply()
    }

    fun isAutoConnectActiveLookEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_CONNECT_ACTIVELOOK, false)
    }

    fun setAutoConnectActiveLook(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CONNECT_ACTIVELOOK, enabled).apply()
    }

    /**
     * Last connected glasses
     */
    fun getLastConnectedGlassesAddress(): String? {
        return prefs.getString(KEY_LAST_GLASSES_ADDRESS, null)
    }

    fun setLastConnectedGlasses(address: String?) {
        prefs.edit().putString(KEY_LAST_GLASSES_ADDRESS, address).apply()
    }

    /**
     * Reconnect timeout in minutes (for auto-reconnect attempts during rides)
     */
    fun getReconnectTimeoutMinutes(): Int {
        return prefs.getInt(KEY_RECONNECT_TIMEOUT_MINUTES, 10) // Default 10 minutes
    }

    fun setReconnectTimeoutMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_RECONNECT_TIMEOUT_MINUTES, minutes).apply()
    }

    companion object {
        private const val PREFS_NAME = "k2look_preferences"
        private const val KEY_AUTO_CONNECT_KAROO = "auto_connect_karoo"
        private const val KEY_AUTO_CONNECT_ACTIVELOOK = "auto_connect_activelook"
        private const val KEY_LAST_GLASSES_ADDRESS = "last_glasses_address"
        private const val KEY_RECONNECT_TIMEOUT_MINUTES = "reconnect_timeout_minutes"
    }
}

