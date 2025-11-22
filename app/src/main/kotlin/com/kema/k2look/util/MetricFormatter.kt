package com.kema.k2look.util

import com.kema.k2look.model.AppConfig
import com.kema.k2look.model.MetricData
import com.kema.k2look.model.MetricState
import java.util.Locale

/**
 * Utility class for formatting metric values for display
 */
object MetricFormatter {

    // Default locale (Swedish)
    private val defaultLocale = Locale("sv", "SE")

    /**
     * Format speed value using app configuration
     */
    fun formatSpeed(metricData: MetricData, config: AppConfig): FormattedMetric {
        return formatSpeed(metricData, config.speedUnit.useMiles, config.locale.toJavaLocale())
    }

    /**
     * Format speed value (km/h or mph) with locale
     */
    fun formatSpeed(
        metricData: MetricData,
        useMiles: Boolean = false,
        locale: Locale = defaultLocale
    ): FormattedMetric {
        val multiplier = if (useMiles) 0.621371 else 1.0
        val unit = if (useMiles) "mph" else "km/h"

        return FormattedMetric(
            current = formatValue(metricData.current?.times(multiplier), 1, locale),
            average = formatValue(metricData.average?.times(multiplier), 1, locale),
            max = formatValue(metricData.max?.times(multiplier), 1, locale),
            unit = unit,
            state = metricData.state
        )
    }

    /**
     * Format distance value using app configuration
     */
    fun formatDistance(metricData: MetricData, config: AppConfig): FormattedMetric {
        return formatDistance(
            metricData,
            config.distanceUnit.useMiles,
            config.locale.toJavaLocale()
        )
    }

    /**
     * Format distance value (km or miles) with locale
     */
    fun formatDistance(
        metricData: MetricData,
        useMiles: Boolean = false,
        locale: Locale = defaultLocale
    ): FormattedMetric {
        val multiplier = if (useMiles) 0.621371 else 1.0
        val unit = if (useMiles) "mi" else "km"

        return FormattedMetric(
            current = formatValue(metricData.current?.times(multiplier), 2, locale),
            average = null, // Distance doesn't have average
            max = null,     // Distance doesn't have max
            unit = unit,
            state = metricData.state
        )
    }

    /**
     * Format elevation value using app configuration
     */
    fun formatElevation(metricData: MetricData, config: AppConfig): FormattedMetric {
        return formatElevation(
            metricData,
            config.elevationUnit.useFeet,
            config.locale.toJavaLocale()
        )
    }

    /**
     * Format elevation value (meters or feet) with locale
     */
    fun formatElevation(
        metricData: MetricData,
        useFeet: Boolean = false,
        locale: Locale = defaultLocale
    ): FormattedMetric {
        val multiplier = if (useFeet) 3.28084 else 1.0
        val unit = if (useFeet) "ft" else "m"

        return FormattedMetric(
            current = formatValue(metricData.current?.times(multiplier), 0, locale),
            average = null,
            max = null,
            unit = unit,
            state = metricData.state
        )
    }

    /**
     * Format heart rate value (bpm)
     */
    fun formatHeartRate(metricData: MetricData): FormattedMetric {
        return FormattedMetric(
            current = formatValue(metricData.current, 0),
            average = formatValue(metricData.average, 0),
            max = formatValue(metricData.max, 0),
            unit = "bpm",
            state = metricData.state
        )
    }

    /**
     * Format cadence value (rpm)
     */
    fun formatCadence(metricData: MetricData): FormattedMetric {
        return FormattedMetric(
            current = formatValue(metricData.current, 0),
            average = formatValue(metricData.average, 0),
            max = formatValue(metricData.max, 0),
            unit = "rpm",
            state = metricData.state
        )
    }

    /**
     * Format power value (watts)
     */
    fun formatPower(metricData: MetricData): FormattedMetric {
        return FormattedMetric(
            current = formatValue(metricData.current, 0),
            average = formatValue(metricData.average, 0),
            max = formatValue(metricData.max, 0),
            unit = "w",
            state = metricData.state
        )
    }

    /**
     * Format time in milliseconds to HH:MM:SS format
     */
    fun formatTime(milliseconds: Long): String {
        if (milliseconds < 0) return "--:--:--"

        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        val hours = (milliseconds / (1000 * 60 * 60))

        return String.format(Locale("sv", "SE"), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Format time in a compact format (MM:SS for < 1 hour, HH:MM otherwise)
     */
    fun formatTimeCompact(milliseconds: Long): String {
        if (milliseconds < 0) return "--:--"

        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        val hours = (milliseconds / (1000 * 60 * 60))

        return if (hours > 0) {
            String.format(Locale("sv", "SE"), "%d:%02d", hours, minutes)
        } else {
            String.format(Locale("sv", "SE"), "%d:%02d", minutes, seconds)
        }
    }

    /**
     * Format a numeric value with specified decimal places and locale
     */
    private fun formatValue(
        value: Double?,
        decimalPlaces: Int,
        locale: Locale = defaultLocale
    ): String? {
        return value?.let {
            when (decimalPlaces) {
                0 -> "%.0f".format(locale, it)
                1 -> "%.1f".format(locale, it)
                2 -> "%.2f".format(locale, it)
                else -> it.toString()
            }
        }
    }

    /**
     * Get display string with state handling
     */
    fun getDisplayString(formattedMetric: FormattedMetric, showUnit: Boolean = true): String {
        val unitStr = if (showUnit) " ${formattedMetric.unit}" else ""

        return when (formattedMetric.state) {
            MetricState.STREAMING -> {
                formattedMetric.current?.let { "$it$unitStr" } ?: "--$unitStr"
            }

            MetricState.SEARCHING -> "Searching..."
            MetricState.NOT_AVAILABLE -> "N/A"
            MetricState.IDLE -> "--$unitStr"
        }
    }

    /**
     * Get display string with current/average/max
     */
    fun getDetailedDisplayString(formattedMetric: FormattedMetric): String {
        return when (formattedMetric.state) {
            MetricState.STREAMING -> {
                val parts = mutableListOf<String>()
                formattedMetric.current?.let { parts.add("$it ${formattedMetric.unit}") }
                formattedMetric.average?.let { parts.add("avg: $it") }
                formattedMetric.max?.let { parts.add("max: $it") }
                parts.joinToString(" | ")
            }

            MetricState.SEARCHING -> "Searching..."
            MetricState.NOT_AVAILABLE -> "N/A"
            MetricState.IDLE -> "--"
        }
    }
}

/**
 * Formatted metric with optional average and max values
 */
data class FormattedMetric(
    val current: String?,
    val average: String?,
    val max: String?,
    val unit: String,
    val state: MetricState
)

