package com.kema.k2look.service

import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState

/**
 * Pure formatting functions for Karoo SDK stream states → display strings.
 *
 * All functions are package-internal and stateless. They are called from KarooActiveLookBridge
 * metric observers.
 */

/** Standard stream: format [singleValue] with a trailing [unit]. */
internal fun formatStreamData(streamState: StreamState?, unit: String): String =
        when (streamState) {
            is StreamState.Streaming ->
                    streamState.dataPoint.singleValue?.let { "${formatValue(it)} $unit" }
                            ?: "-- $unit"
            is StreamState.Searching -> "..."
            is StreamState.Idle -> "-- $unit"
            is StreamState.NotAvailable -> "N/A"
            null -> "-- $unit"
        }

/** Elapsed / ride time (milliseconds) → HH:MM:SS */
internal fun formatTimeData(streamState: StreamState?): String =
        when (streamState) {
            is StreamState.Streaming -> {
                val ms = streamState.dataPoint.singleValue?.toLong()
                if (ms != null) {
                    val s = (ms / 1000) % 60
                    val m = (ms / 60_000) % 60
                    val h = ms / 3_600_000
                    String.format(java.util.Locale.US, "%02d:%02d:%02d", h, m, s)
                } else "--:--:--"
            }
            else -> "--:--:--"
        }

/** HR zone (1-5) → Z1 … Z5 */
internal fun formatHRZoneData(streamState: StreamState?): String = formatZoneData(streamState, 5)

/** Generic zone (1..max) → Z1 … Zmax */
internal fun formatZoneData(streamState: StreamState?, max: Int): String =
        when (streamState) {
            is StreamState.Streaming ->
                    streamState.dataPoint.singleValue?.toInt()?.takeIf { it in 1..max }?.let {
                        "Z$it"
                    }
                            ?: "--"
            is StreamState.Searching -> "..."
            is StreamState.Idle, null -> "--"
            is StreamState.NotAvailable -> "N/A"
        }

/** Percentage (0-100) → "72%" */
internal fun formatPercent(streamState: StreamState?): String =
        when (streamState) {
            is StreamState.Streaming ->
                    streamState.dataPoint.singleValue?.let { "%.0f%%".format(it) } ?: "--%"
            is StreamState.Searching -> "...%"
            is StreamState.Idle, null -> "--%"
            is StreamState.NotAvailable -> "N/A"
        }

/** Elevation grade with sign → "+5.2%" / "-3.1%" */
internal fun formatGrade(streamState: StreamState?): String =
        when (streamState) {
            is StreamState.Streaming ->
                    streamState.dataPoint.singleValue?.let {
                        "${if (it > 0) "+" else ""}${"%.1f".format(it)}%"
                    }
                            ?: "--%"
            is StreamState.Searching -> "...%"
            is StreamState.Idle, null -> "--%"
            is StreamState.NotAvailable -> "N/A"
        }

/** Integer metric (lap #, shift count, assist mode level) */
internal fun formatInteger(streamState: StreamState?): String =
        when (streamState) {
            is StreamState.Streaming -> streamState.dataPoint.singleValue?.toInt()?.toString()
                            ?: "--"
            is StreamState.Searching -> "..."
            is StreamState.Idle, null -> "--"
            is StreamState.NotAvailable -> "N/A"
        }

/** Lap / last-lap time (milliseconds) → MM:SS or H:MM:SS */
internal fun formatLapTime(streamState: StreamState?): String =
        when (streamState) {
            is StreamState.Streaming -> {
                val ms = streamState.dataPoint.singleValue?.toLong()
                if (ms != null) {
                    val s = (ms / 1000) % 60
                    val m = (ms / 60_000) % 60
                    val h = ms / 3_600_000
                    if (h > 0) String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
                    else String.format(java.util.Locale.US, "%02d:%02d", m, s)
                } else "--:--"
            }
            else -> "--:--"
        }

/** Clock or ETA time (milliseconds-of-day) → HH:MM */
internal fun formatClockTime(streamState: StreamState?): String =
        when (streamState) {
            is StreamState.Streaming -> {
                val ms = streamState.dataPoint.singleValue?.toLong()
                if (ms != null) {
                    val totalMin = ms / 60_000
                    val h = (totalMin / 60) % 24
                    val m = totalMin % 60
                    String.format(java.util.Locale.US, "%02d:%02d", h, m)
                } else "--:--"
            }
            else -> "--:--"
        }

/** Duration in seconds → "H:MM" or "N min" */
internal fun formatDuration(streamState: StreamState?): String =
        when (streamState) {
            is StreamState.Streaming -> {
                val sec = streamState.dataPoint.singleValue?.toLong()
                if (sec != null) {
                    val h = sec / 3600
                    val m = (sec % 3600) / 60
                    if (h > 0) String.format(java.util.Locale.US, "%d:%02d", h, m)
                    else String.format(java.util.Locale.US, "%d min", m)
                } else "--"
            }
            is StreamState.Searching -> "..."
            is StreamState.Idle, null -> "--"
            is StreamState.NotAvailable -> "N/A"
        }

/** Distance to next turn → "150 m" (<1 km) or "1.2 km" (≥1 km) */
internal fun formatDistanceToTurn(streamState: StreamState?): String =
        when (streamState) {
            is StreamState.Streaming ->
                    streamState.dataPoint.singleValue?.let {
                        if (it < 1000) "%.0f m".format(it) else "%.1f km".format(it / 1000.0)
                    }
                            ?: "-- m"
            is StreamState.Searching -> "..."
            is StreamState.Idle, null -> "-- m"
            is StreamState.NotAvailable -> "N/A"
        }

/** Gear position as "current/max" (e.g. "3/11") from multi-field DataPoint. */
internal fun formatGear(streamState: StreamState?, gearField: String, maxField: String): String =
        when (streamState) {
            is StreamState.Streaming -> {
                val v = streamState.dataPoint.values
                val gear = v[gearField]?.toInt()
                val max = v[maxField]?.toInt()
                when {
                    gear == null -> "--"
                    max != null -> "$gear/$max"
                    else -> "$gear"
                }
            }
            is StreamState.Searching -> "..."
            is StreamState.Idle, null -> "--"
            is StreamState.NotAvailable -> "N/A"
        }

/**
 * Drivetrain battery from SHIFTING_BATTERY stream. BatteryStatus ordinal: 0=New, 1=Good, 2=OK,
 * 3=Low, 4=Critical, 5=Invalid Prefers rear derailleur status; falls back to overall status.
 */
internal fun formatShiftingBattery(streamState: StreamState?): String {
    val labels = arrayOf("New", "Good", "OK", "Low", "Critical", "?")
    return when (streamState) {
        is StreamState.Streaming -> {
            val v = streamState.dataPoint.values
            val raw =
                    (v[DataType.Field.SHIFTING_BATTERY_STATUS_REAR_DERAILLEUR]
                                    ?: v[DataType.Field.SHIFTING_BATTERY_STATUS])?.toInt()
            labels.getOrElse(raw ?: 5) { "?" }
        }
        is StreamState.Searching -> "..."
        is StreamState.Idle, null -> "--"
        is StreamState.NotAvailable -> "N/A"
    }
}

/** Compass heading index (0=N … 7=NW) → cardinal abbreviation. */
internal fun formatHeading(streamState: StreamState?): String {
    val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    return when (streamState) {
        is StreamState.Streaming ->
                dirs.getOrElse(streamState.dataPoint.singleValue?.toInt() ?: 8) { "--" }
        is StreamState.Searching -> "..."
        is StreamState.Idle, null -> "--"
        is StreamState.NotAvailable -> "N/A"
    }
}

/** Round a raw Double to a clean display string (no trailing zeros for ≥100). */
internal fun formatValue(value: Double): String =
        when {
            value >= 100 -> "%.0f".format(value)
            else -> "%.1f".format(value)
        }

/** Integer stream: format [singleValue] as a whole number with a trailing [unit]. */
internal fun formatStreamDataInt(streamState: StreamState?, unit: String): String =
        when (streamState) {
            is StreamState.Streaming ->
                    streamState.dataPoint.singleValue?.let { "%.0f $unit".format(it) } ?: "-- $unit"
            is StreamState.Searching -> "..."
            is StreamState.Idle -> "-- $unit"
            is StreamState.NotAvailable -> "N/A"
            null -> "-- $unit"
        }
