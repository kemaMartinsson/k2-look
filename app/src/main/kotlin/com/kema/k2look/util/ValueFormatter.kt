package com.kema.k2look.util

import io.hammerhead.karooext.models.StreamState

/**
 * Utility class for formatting metric values for display on glasses
 *
 * Note: Karoo data streams already provide values in the user's preferred units
 * (metric/imperial) based on their Karoo profile settings. This formatter is
 * mainly for future use or custom formatting needs.
 *
 * Handles:
 * - Number formatting (decimal places based on magnitude)
 * - Time formatting (HH:MM:SS)
 * - Missing/unavailable data handling
 */
object ValueFormatter {

    /**
     * Format a numeric value with appropriate precision
     */
    fun formatValue(value: Double, decimals: Int = -1): String {
        return when {
            value >= 1000 -> "%.0f".format(value)          // 1234
            value >= 100 -> "%.0f".format(value)           // 234
            value >= 10 -> {
                if (decimals >= 0) "%.${decimals}f".format(value)
                else "%.1f".format(value)                   // 23.5
            }

            value >= 1 -> {
                if (decimals >= 0) "%.${decimals}f".format(value)
                else "%.1f".format(value)                   // 2.5
            }

            else -> {
                if (decimals >= 0) "%.${decimals}f".format(value)
                else "%.2f".format(value)                   // 0.25
            }
        }
    }

    /**
     * Format speed with unit conversion
     * @param kmh Speed in km/h
     * @param useImperial Convert to mph if true
     */
    fun formatSpeed(kmh: Double?, useImperial: Boolean = false): String {
        if (kmh == null) return "--"

        return if (useImperial) {
            val mph = kmh * 0.621371
            formatValue(mph)
        } else {
            formatValue(kmh)
        }
    }

    /**
     * Format distance with unit conversion
     * @param km Distance in kilometers
     * @param useImperial Convert to miles if true
     */
    fun formatDistance(km: Double?, useImperial: Boolean = false): String {
        if (km == null) return "--"

        return if (useImperial) {
            val miles = km * 0.621371
            formatValue(miles, 2)
        } else {
            formatValue(km, 2)
        }
    }

    /**
     * Format elevation with unit conversion
     * @param meters Elevation in meters
     * @param useImperial Convert to feet if true
     */
    fun formatElevation(meters: Double?, useImperial: Boolean = false): String {
        if (meters == null) return "--"

        return if (useImperial) {
            val feet = meters * 3.28084
            formatValue(feet, 0)
        } else {
            formatValue(meters, 0)
        }
    }

    /**
     * Format temperature with unit conversion
     * @param celsius Temperature in Celsius
     * @param useImperial Convert to Fahrenheit if true
     */
    fun formatTemperature(celsius: Double?, useImperial: Boolean = false): String {
        if (celsius == null) return "--"

        return if (useImperial) {
            val fahrenheit = (celsius * 9.0 / 5.0) + 32
            formatValue(fahrenheit, 0)
        } else {
            formatValue(celsius, 0)
        }
    }

    /**
     * Format time from milliseconds to HH:MM:SS
     */
    fun formatTime(milliseconds: Long?): String {
        if (milliseconds == null) return "--:--:--"

        val totalSeconds = (milliseconds / 1000).toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Format time from milliseconds to MM:SS (for shorter durations)
     */
    fun formatTimeShort(milliseconds: Long?): String {
        if (milliseconds == null) return "--:--"

        val totalSeconds = (milliseconds / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return String.format(java.util.Locale.ROOT, "%02d:%02d", minutes, seconds)
    }

    /**
     * Format heart rate
     */
    fun formatHeartRate(bpm: Double?): String {
        if (bpm == null) return "--"
        return "%.0f".format(bpm)
    }

    /**
     * Format power
     */
    fun formatPower(watts: Double?): String {
        if (watts == null) return "--"
        return "%.0f".format(watts)
    }

    /**
     * Format cadence
     */
    fun formatCadence(rpm: Double?): String {
        if (rpm == null) return "--"
        return "%.0f".format(rpm)
    }

    /**
     * Format gradient/slope
     */
    fun formatGradient(percent: Double?): String {
        if (percent == null) return "--"
        return formatValue(percent, 1)
    }

    /**
     * Format stream state to display value
     */
    fun formatStreamState(
        streamState: StreamState?,
        formatFunc: (Double) -> String
    ): String {
        return when (streamState) {
            is StreamState.Streaming -> {
                val value = streamState.dataPoint.singleValue
                if (value != null) formatFunc(value) else "--"
            }

            is StreamState.Searching -> "..."
            is StreamState.Idle -> "--"
            is StreamState.NotAvailable -> "N/A"
            null -> "--"
        }
    }

    /**
     * Format percentage (0-100)
     */
    fun formatPercentage(value: Double?): String {
        if (value == null) return "--"
        return "%.0f".format(value)
    }

    /**
     * Format zone (1-5 typically)
     */
    fun formatZone(zone: Double?): String {
        if (zone == null) return "--"
        return "Z%.0f".format(zone)
    }
}

