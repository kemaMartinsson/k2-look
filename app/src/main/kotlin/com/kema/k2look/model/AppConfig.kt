package com.kema.k2look.model

/**
 * Application configuration settings
 */
data class AppConfig(
    val locale: AppLocale = AppLocale.SWEDISH,
    val speedUnit: SpeedUnit = SpeedUnit.KPH,
    val distanceUnit: DistanceUnit = DistanceUnit.KILOMETERS,
    val elevationUnit: ElevationUnit = ElevationUnit.METERS,
    val autoConnectKaroo: Boolean = true,
    val autoConnectActiveLook: Boolean = false, // Requires user to select glasses
    val lastConnectedGlassesAddress: String? = null
)

/**
 * Supported locales
 */
enum class AppLocale(val code: String, val displayName: String) {
    SWEDISH("sv-SE", "Svenska"),
    ENGLISH("en-US", "English"),
    GERMAN("de-DE", "Deutsch"),
    FRENCH("fr-FR", "Français"),
    ITALIAN("it-IT", "Italiano"),
    SPANISH("es-ES", "Español");

    fun toJavaLocale(): java.util.Locale {
        val parts = code.split("-")
        return if (parts.size == 2) {
            java.util.Locale(parts[0], parts[1])
        } else {
            java.util.Locale(parts[0])
        }
    }
}

/**
 * Speed unit preference
 */
enum class SpeedUnit(val displayName: String, val abbreviation: String) {
    KPH("Kilometers per hour", "km/h"),
    MPH("Miles per hour", "mph");

    val useMiles: Boolean
        get() = this == MPH
}

/**
 * Distance unit preference
 */
enum class DistanceUnit(val displayName: String, val abbreviation: String) {
    KILOMETERS("Kilometers", "km"),
    MILES("Miles", "mi");

    val useMiles: Boolean
        get() = this == MILES
}

/**
 * Elevation unit preference
 */
enum class ElevationUnit(val displayName: String, val abbreviation: String) {
    METERS("Meters", "m"),
    FEET("Feet", "ft");

    val useFeet: Boolean
        get() = this == FEET
}

