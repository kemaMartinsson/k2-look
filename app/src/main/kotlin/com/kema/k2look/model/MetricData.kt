package com.kema.k2look.model

import io.hammerhead.karooext.models.StreamState

/**
 * Enhanced metric data with current, average, and max values
 */
data class MetricData(
    val current: Double? = null,
    val average: Double? = null,
    val max: Double? = null,
    val unit: String = "",
    val state: MetricState = MetricState.IDLE
)

/**
 * State of a metric
 */
enum class MetricState {
    IDLE,           // No data available
    SEARCHING,      // Sensor not connected yet
    STREAMING,      // Active data stream
    NOT_AVAILABLE   // Sensor/data type not available
}

/**
 * Convert StreamState to MetricState
 */
fun StreamState?.toMetricState(): MetricState {
    return when (this) {
        is StreamState.Streaming -> MetricState.STREAMING
        is StreamState.Searching -> MetricState.SEARCHING
        is StreamState.Idle -> MetricState.IDLE
        is StreamState.NotAvailable -> MetricState.NOT_AVAILABLE
        null -> MetricState.IDLE
    }
}

/**
 * Collection of all ride metrics for easy access
 */
data class RideMetrics(
    val speed: MetricData = MetricData(unit = "km/h"),
    val heartRate: MetricData = MetricData(unit = "bpm"),
    val cadence: MetricData = MetricData(unit = "rpm"),
    val power: MetricData = MetricData(unit = "w"),
    val distance: MetricData = MetricData(unit = "km"),
    val elapsedTime: Long = 0L, // milliseconds
    val rideTime: Long = 0L      // milliseconds (active riding only)
)

