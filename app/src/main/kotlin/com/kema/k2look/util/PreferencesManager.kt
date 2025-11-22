package com.kema.k2look.util

import android.content.Context
import android.content.SharedPreferences
import com.kema.k2look.model.AppConfig
import com.kema.k2look.model.AppLocale
import com.kema.k2look.model.DistanceUnit
import com.kema.k2look.model.ElevationUnit
import com.kema.k2look.model.SpeedUnit

/**
 * Manager for application preferences and configuration
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Get current app configuration
     */
    fun getConfig(): AppConfig {
        return AppConfig(
            locale = AppLocale.valueOf(
                prefs.getString(KEY_LOCALE, AppLocale.SWEDISH.name) ?: AppLocale.SWEDISH.name
            ),
            speedUnit = SpeedUnit.valueOf(
                prefs.getString(KEY_SPEED_UNIT, SpeedUnit.KPH.name) ?: SpeedUnit.KPH.name
            ),
            distanceUnit = DistanceUnit.valueOf(
                prefs.getString(KEY_DISTANCE_UNIT, DistanceUnit.KILOMETERS.name)
                    ?: DistanceUnit.KILOMETERS.name
            ),
            elevationUnit = ElevationUnit.valueOf(
                prefs.getString(KEY_ELEVATION_UNIT, ElevationUnit.METERS.name)
                    ?: ElevationUnit.METERS.name
            ),
            autoConnectKaroo = prefs.getBoolean(KEY_AUTO_CONNECT_KAROO, true),
            autoConnectActiveLook = prefs.getBoolean(KEY_AUTO_CONNECT_ACTIVELOOK, false),
            lastConnectedGlassesAddress = prefs.getString(KEY_LAST_GLASSES_ADDRESS, null)
        )
    }

    /**
     * Save app configuration
     */
    fun saveConfig(config: AppConfig) {
        prefs.edit()
            .putString(KEY_LOCALE, config.locale.name)
            .putString(KEY_SPEED_UNIT, config.speedUnit.name)
            .putString(KEY_DISTANCE_UNIT, config.distanceUnit.name)
            .putString(KEY_ELEVATION_UNIT, config.elevationUnit.name)
            .putBoolean(KEY_AUTO_CONNECT_KAROO, config.autoConnectKaroo)
            .putBoolean(KEY_AUTO_CONNECT_ACTIVELOOK, config.autoConnectActiveLook)
            .apply {
                config.lastConnectedGlassesAddress?.let {
                    putString(KEY_LAST_GLASSES_ADDRESS, it)
                }
            }
            .apply()
    }

    /**
     * Update individual settings
     */
    fun setLocale(locale: AppLocale) {
        prefs.edit().putString(KEY_LOCALE, locale.name).apply()
    }

    fun setSpeedUnit(unit: SpeedUnit) {
        prefs.edit().putString(KEY_SPEED_UNIT, unit.name).apply()
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        prefs.edit().putString(KEY_DISTANCE_UNIT, unit.name).apply()
    }

    fun setElevationUnit(unit: ElevationUnit) {
        prefs.edit().putString(KEY_ELEVATION_UNIT, unit.name).apply()
    }

    fun setAutoConnectKaroo(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CONNECT_KAROO, enabled).apply()
    }

    fun setAutoConnectActiveLook(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CONNECT_ACTIVELOOK, enabled).apply()
    }

    fun setLastConnectedGlasses(address: String?) {
        prefs.edit().putString(KEY_LAST_GLASSES_ADDRESS, address).apply()
    }

    /**
     * Get individual settings
     */
    fun getLocale(): AppLocale {
        return AppLocale.valueOf(
            prefs.getString(KEY_LOCALE, AppLocale.SWEDISH.name) ?: AppLocale.SWEDISH.name
        )
    }

    fun getSpeedUnit(): SpeedUnit {
        return SpeedUnit.valueOf(
            prefs.getString(KEY_SPEED_UNIT, SpeedUnit.KPH.name) ?: SpeedUnit.KPH.name
        )
    }

    fun getDistanceUnit(): DistanceUnit {
        return DistanceUnit.valueOf(
            prefs.getString(KEY_DISTANCE_UNIT, DistanceUnit.KILOMETERS.name)
                ?: DistanceUnit.KILOMETERS.name
        )
    }

    fun getElevationUnit(): ElevationUnit {
        return ElevationUnit.valueOf(
            prefs.getString(KEY_ELEVATION_UNIT, ElevationUnit.METERS.name)
                ?: ElevationUnit.METERS.name
        )
    }

    fun isAutoConnectKarooEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_CONNECT_KAROO, true)
    }

    fun isAutoConnectActiveLookEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_CONNECT_ACTIVELOOK, false)
    }

    fun getLastConnectedGlassesAddress(): String? {
        return prefs.getString(KEY_LAST_GLASSES_ADDRESS, null)
    }

    companion object {
        private const val PREFS_NAME = "k2look_preferences"

        private const val KEY_LOCALE = "locale"
        private const val KEY_SPEED_UNIT = "speed_unit"
        private const val KEY_DISTANCE_UNIT = "distance_unit"
        private const val KEY_ELEVATION_UNIT = "elevation_unit"
        private const val KEY_AUTO_CONNECT_KAROO = "auto_connect_karoo"
        private const val KEY_AUTO_CONNECT_ACTIVELOOK = "auto_connect_activelook"
        private const val KEY_LAST_GLASSES_ADDRESS = "last_glasses_address"
    }
}

