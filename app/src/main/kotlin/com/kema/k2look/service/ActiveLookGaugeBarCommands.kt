package com.kema.k2look.service

import android.util.Log
import com.activelook.activelooksdk.types.holdFlushAction

/**
 * Gauge and progress-bar command extension functions for [ActiveLookService].
 *
 * All functions delegate directly to the connected [ActiveLookService.connectedGlasses] instance.
 * Separated from the main class to keep file size manageable; the functions are logically grouped
 * as display-primitive commands that share no mutable state with each other.
 */
private const val TAG_GCMD = "ActiveLookService"

// ── Gauge commands ─────────────────────────────────────────────────────────

/**
 * Save gauge configuration to glasses memory.
 *
 * @param gauge Gauge configuration
 * @return true if successful
 */
suspend fun ActiveLookService.saveGauge(gauge: com.kema.k2look.model.Gauge): Boolean {
    val glasses = connectedGlasses
    if (glasses == null) {
        Log.w(TAG_GCMD, "Cannot save gauge: No glasses connected")
        return false
    }
    return try {
        glasses.gaugeSave(
                gauge.id.toByte(),
                gauge.centerX.toShort(),
                gauge.centerY.toShort(),
                gauge.radiusOuter.toChar(),
                gauge.radiusInner.toChar(),
                gauge.startPortion.toByte(),
                gauge.endPortion.toByte(),
                gauge.clockwise
        )
        Log.i(TAG_GCMD, "✓ Gauge ${gauge.id} saved (${gauge.dataField.name})")
        true
    } catch (e: Exception) {
        Log.e(TAG_GCMD, "❌ Failed to save gauge ${gauge.id}", e)
        false
    }
}

/**
 * Display gauge with percentage value.
 *
 * @param gaugeId Gauge identifier
 * @param percentage Value 0-100
 * @return true if successful
 */
suspend fun ActiveLookService.displayGauge(gaugeId: Int, percentage: Int): Boolean {
    val glasses = connectedGlasses
    if (glasses == null) {
        Log.w(TAG_GCMD, "Cannot display gauge: No glasses connected")
        return false
    }
    return try {
        val clampedPercentage = percentage.coerceIn(0, 100)
        glasses.gaugeDisplay(gaugeId.toByte(), clampedPercentage.toByte())
        Log.d(TAG_GCMD, "Gauge $gaugeId: $clampedPercentage%")
        true
    } catch (e: Exception) {
        Log.e(TAG_GCMD, "❌ Failed to display gauge $gaugeId", e)
        false
    }
}

/**
 * Delete gauge from glasses memory.
 *
 * @param gaugeId Gauge identifier (or 0xFF for all)
 * @return true if successful
 */
suspend fun ActiveLookService.deleteGauge(gaugeId: Int): Boolean {
    val glasses = connectedGlasses
    if (glasses == null) {
        Log.w(TAG_GCMD, "Cannot delete gauge: No glasses connected")
        return false
    }
    return try {
        glasses.gaugeDelete(gaugeId.toByte())
        Log.i(TAG_GCMD, "✓ Gauge $gaugeId deleted")
        true
    } catch (e: Exception) {
        Log.e(TAG_GCMD, "❌ Failed to delete gauge $gaugeId", e)
        false
    }
}

// ── Progress bar commands ──────────────────────────────────────────────────

/**
 * Display progress bar using rectangles.
 *
 * @param bar Progress bar configuration
 * @param percentage Value 0-100
 * @return true if successful
 */
suspend fun ActiveLookService.displayProgressBar(
        bar: com.kema.k2look.model.ProgressBar,
        percentage: Int
): Boolean {
    val glasses = connectedGlasses
    if (glasses == null) {
        Log.w(TAG_GCMD, "Cannot display progress bar: No glasses connected")
        return false
    }
    return try {
        val clampedPercentage = percentage.coerceIn(0, 100)
        val fillAmount = bar.calculateFillAmount(clampedPercentage.toFloat())

        glasses.holdFlush(holdFlushAction.HOLD)

        // Clear previous bar area
        glasses.color(0)
        glasses.rectf(
                bar.x.toShort(),
                bar.y.toShort(),
                (bar.x + bar.width).toShort(),
                (bar.y + bar.height).toShort()
        )

        if (bar.showBorder) {
            glasses.color(8)
            glasses.rect(
                    bar.x.toShort(),
                    bar.y.toShort(),
                    (bar.x + bar.width).toShort(),
                    (bar.y + bar.height).toShort()
            )
        }

        glasses.color(15)
        when (bar.orientation) {
            com.kema.k2look.model.Orientation.HORIZONTAL -> {
                if (fillAmount > 0) {
                    glasses.rectf(
                            bar.x.toShort(),
                            bar.y.toShort(),
                            (bar.x + fillAmount).toShort(),
                            (bar.y + bar.height).toShort()
                    )
                }
            }
            com.kema.k2look.model.Orientation.VERTICAL -> {
                if (fillAmount > 0) {
                    val fillY = bar.y + bar.height - fillAmount
                    glasses.rectf(
                            bar.x.toShort(),
                            fillY.toShort(),
                            (bar.x + bar.width).toShort(),
                            (bar.y + bar.height).toShort()
                    )
                }
            }
        }

        glasses.holdFlush(holdFlushAction.FLUSH)
        Log.d(TAG_GCMD, "Progress bar ${bar.id}: $clampedPercentage% (${bar.dataField.name})")
        true
    } catch (e: Exception) {
        Log.e(TAG_GCMD, "❌ Failed to display progress bar ${bar.id}", e)
        try {
            connectedGlasses?.holdFlush(holdFlushAction.FLUSH)
        } catch (_: Exception) {}
        false
    }
}

/**
 * Display zoned progress bar with color-coded zones.
 *
 * @param zonedBar Zoned bar configuration
 * @param currentValue Current metric value
 * @return true if successful
 */
suspend fun ActiveLookService.displayZonedBar(
        zonedBar: com.kema.k2look.model.ZonedProgressBar,
        currentValue: Float
): Boolean {
    val glasses = connectedGlasses
    if (glasses == null) {
        Log.w(TAG_GCMD, "Cannot display zoned bar: No glasses connected")
        return false
    }
    return try {
        val bar = zonedBar.bar

        glasses.holdFlush(holdFlushAction.HOLD)

        // Clear previous bar area
        glasses.color(0)
        glasses.rectf(
                bar.x.toShort(),
                bar.y.toShort(),
                (bar.x + bar.width).toShort(),
                (bar.y + bar.height).toShort()
        )

        if (bar.showBorder) {
            glasses.color(8)
            glasses.rect(
                    bar.x.toShort(),
                    bar.y.toShort(),
                    (bar.x + bar.width).toShort(),
                    (bar.y + bar.height).toShort()
            )
        }

        // Draw each zone as background
        zonedBar.zones.forEach { zone ->
            val zoneStartPx = zonedBar.valueToPixel(zone.minValue)
            val zoneEndPx = zonedBar.valueToPixel(zone.maxValue)

            glasses.color(zone.color.toByte())
            when (bar.orientation) {
                com.kema.k2look.model.Orientation.HORIZONTAL -> {
                    glasses.rectf(
                            zoneStartPx.toShort(),
                            (bar.y + 2).toShort(),
                            zoneEndPx.toShort(),
                            (bar.y + bar.height - 2).toShort()
                    )
                }
                com.kema.k2look.model.Orientation.VERTICAL -> {
                    glasses.rectf(
                            (bar.x + 2).toShort(),
                            zoneEndPx.toShort(),
                            (bar.x + bar.width - 2).toShort(),
                            zoneStartPx.toShort()
                    )
                }
            }
        }

        // Draw current value indicator (bright overlay)
        val currentPx = zonedBar.valueToPixel(currentValue)
        val currentZone = zonedBar.findZone(currentValue)

        glasses.color(15)
        when (bar.orientation) {
            com.kema.k2look.model.Orientation.HORIZONTAL -> {
                glasses.rectf(
                        (currentPx - 1).toShort(),
                        bar.y.toShort(),
                        (currentPx + 1).toShort(),
                        (bar.y + bar.height).toShort()
                )
            }
            com.kema.k2look.model.Orientation.VERTICAL -> {
                glasses.rectf(
                        bar.x.toShort(),
                        (currentPx - 1).toShort(),
                        (bar.x + bar.width).toShort(),
                        (currentPx + 1).toShort()
                )
            }
        }

        glasses.holdFlush(holdFlushAction.FLUSH)
        Log.d(
                TAG_GCMD,
                "Zoned bar ${bar.id}: ${currentValue.toInt()} ${currentZone?.name ?: "?"} (${bar.dataField.name})"
        )
        true
    } catch (e: Exception) {
        Log.e(TAG_GCMD, "❌ Failed to display zoned bar ${zonedBar.bar.id}", e)
        try {
            connectedGlasses?.holdFlush(holdFlushAction.FLUSH)
        } catch (_: Exception) {}
        false
    }
}
