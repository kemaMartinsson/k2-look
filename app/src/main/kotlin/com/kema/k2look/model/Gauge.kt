package com.kema.k2look.model

/**
 * Represents a circular/arc gauge for visualizing metric values
 *
 * Uses ActiveLook native gauge API for efficient rendering
 *
 * @param id Gauge identifier (0-255)
 * @param centerX Center X coordinate on display
 * @param centerY Center Y coordinate on display
 * @param radiusOuter Outer radius in pixels
 * @param radiusInner Inner radius in pixels (thickness = outer - inner)
 * @param startPortion Start circle portion (0-15, each portion = 22.5°)
 * @param endPortion End circle portion (0-15)
 * @param clockwise Direction of gauge fill
 * @param minValue Minimum metric value (e.g., 0W for power)
 * @param maxValue Maximum metric value (e.g., 400W for power)
 * @param dataField Associated data field to display
 */
data class Gauge(
    val id: Int,
    val centerX: Int,
    val centerY: Int,
    val radiusOuter: Int,
    val radiusInner: Int,
    val startPortion: Int,
    val endPortion: Int,
    val clockwise: Boolean = true,
    val minValue: Float = 0f,
    val maxValue: Float = 100f,
    val dataField: DataField
) {
    init {
        require(id in 0..255) { "Gauge ID must be 0-255" }
        require(startPortion in 0..15) { "Start portion must be 0-15" }
        require(endPortion in 0..15) { "End portion must be 0-15" }
        require(radiusOuter > radiusInner) { "Outer radius must be > inner radius" }
        require(maxValue > minValue) { "Max value must be > min value" }
    }

    /**
     * Calculate percentage (0-100) for given metric value
     */
    fun calculatePercentage(value: Float): Int {
        val range = maxValue - minValue
        val normalized = ((value - minValue) / range).coerceIn(0f, 1f)
        return (normalized * 100).toInt()
    }
}

