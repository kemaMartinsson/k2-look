package com.kema.k2look.service

import android.util.Log
import com.activelook.activelooksdk.Glasses
import com.activelook.activelooksdk.types.Rotation
import com.activelook.activelooksdk.types.holdFlushAction
import com.kema.k2look.layout.LayoutPositionDefaults
import com.kema.k2look.model.Orientation
import com.kema.k2look.model.ProgressBar
import com.kema.k2look.model.ZonedProgressBar

private const val TAG_VIS = "ActiveLookLayoutService"

// ════════════════════════════════════════════════════════════════════
//  Bar & Zone-circle visualization renderers
//  (extension functions on ActiveLookLayoutService)
// ════════════════════════════════════════════════════════════════════

/**
 * Renders a horizontal or vertical progress bar at the field's actual template zone position.
 *
 * Visual style:
 * - Below the current value (achieved portion): dim fill (color 2), matching zone circles.
 * - Current value tip: 2px bright white rect.
 * - Above the current value: black (invisible).
 * - Optional thin border around the full bar area.
 *
 * @param bar Progress bar model (provides value range and orientation).
 * @param currentValue Raw metric value used to compute fill amount.
 * @param zoneId Template zone ID whose geometry drives bar placement.
 */
fun ActiveLookLayoutService.displayBarAtZone(
        bar: ProgressBar,
        currentValue: Float,
        zoneId: String,
        screenId: Int,
) {
    if (!activeLookService.isConnected) return
    val glasses = activeLookService.getConnectedGlasses() ?: return
    val geometryKey = "$screenId:$zoneId"
    val geometry =
            screenGeometry[geometryKey]
                    ?: screenGeometry[zoneId]
                            ?: run {
                        Log.w(
                                TAG_VIS,
                                "displayBarAtZone: no geometry for zone $zoneId (screen=$screenId key=$geometryKey) — skipping"
                        )
                        return
                    }

    val x0 = geometry.x0
    val y0 = geometry.y0
    val w = geometry.width
    val h = geometry.height

    // Inner bar: 3px inset on each side vertically, 0 horizontal inset
    val barInset = 3
    val barY0 = y0 + barInset
    val barH = (h - barInset * 2).coerceAtLeast(2)
    val barX0 = x0
    val barW = w

    val range = (bar.maxValue - bar.minValue).coerceAtLeast(0.001f)
    val progress = ((currentValue - bar.minValue) / range).coerceIn(0f, 1f)

    try {
        glasses.holdFlush(holdFlushAction.HOLD)

        // Clear full zone area
        glasses.color(0)
        glasses.rectf(x0.toShort(), y0.toShort(), (x0 + w).toShort(), (y0 + h).toShort())

        when (bar.orientation) {
            Orientation.HORIZONTAL -> {
                val fillPx = (barW * progress).toInt()
                // Dim fill: achieved portion
                if (fillPx > 2) {
                    glasses.color(2)
                    glasses.rectf(
                            barX0.toShort(),
                            barY0.toShort(),
                            (barX0 + fillPx - 2).toShort(),
                            (barY0 + barH).toShort()
                    )
                }
                // Bright tip: 2px wide at current position
                if (fillPx > 0) {
                    val tipX = (barX0 + fillPx - 2).coerceAtLeast(barX0)
                    glasses.color(15)
                    glasses.rectf(
                            tipX.toShort(),
                            barY0.toShort(),
                            (tipX + 2).toShort(),
                            (barY0 + barH).toShort()
                    )
                }
                // Border
                if (bar.showBorder) {
                    glasses.color(4)
                    glasses.rect(
                            barX0.toShort(),
                            barY0.toShort(),
                            (barX0 + barW).toShort(),
                            (barY0 + barH).toShort()
                    )
                }
            }
            Orientation.VERTICAL -> {
                val fillPx = (barH * progress).toInt()
                val fillY0 = barY0 + barH - fillPx
                // Dim fill: achieved portion (all but the tip)
                if (fillPx > 2) {
                    glasses.color(2)
                    glasses.rectf(
                            barX0.toShort(),
                            (fillY0 + 2).toShort(),
                            (barX0 + barW).toShort(),
                            (barY0 + barH).toShort()
                    )
                }
                // Bright tip: 2px tall at current position
                if (fillPx > 0) {
                    val tipY = fillY0.coerceAtMost(barY0 + barH - 2)
                    glasses.color(15)
                    glasses.rectf(
                            barX0.toShort(),
                            tipY.toShort(),
                            (barX0 + barW).toShort(),
                            (tipY + 2).toShort()
                    )
                }
                // Border
                if (bar.showBorder) {
                    glasses.color(4)
                    glasses.rect(
                            barX0.toShort(),
                            barY0.toShort(),
                            (barX0 + barW).toShort(),
                            (barY0 + barH).toShort()
                    )
                }
            }
        }

        glasses.holdFlush(holdFlushAction.FLUSH)
        Log.d(TAG_VIS, "Bar zone=$zoneId progress=${(progress * 100).toInt()}% value=$currentValue")
    } catch (e: Exception) {
        Log.e(TAG_VIS, "displayBarAtZone failed for zone $zoneId: ${e.message}", e)
        try {
            glasses.holdFlush(holdFlushAction.FLUSH)
        } catch (_: Exception) {}
    }
}

/**
 * Renders zone progress as a row of filled circles, one per zone in [zonedBar].
 *
 * The circles are horizontally distributed across the template zone identified by [zoneId]. Fill
 * progression: zones below the active one are dim; the active zone is bright white; zones above the
 * active one are not drawn (black = invisible).
 *
 * Must be called from a coroutine (uses hold/flush + optional cfgSet("ALooK") for the icon).
 *
 * @param zonedBar Zoned bar data model (zones + value range).
 * @param currentValue Current metric value used to determine the active zone.
 * @param zoneId Template zone ID whose geometry drives circle placement.
 * @param iconId Optional 28×28 icon ID to render at the viewer-left end of the zone.
 */
fun ActiveLookLayoutService.displayZoneCircles(
        zonedBar: ZonedProgressBar,
        currentValue: Float,
        zoneId: String,
        screenId: Int,
        iconId: Int?,
        sourceMetricId: Int? = null,
        activeZoneIndexOverride: Int? = null,
        overlayText: String? = null,
) {
    if (!activeLookService.isConnected) return
    val glasses = activeLookService.getConnectedGlasses() ?: return
    val geometryKey = "$screenId:$zoneId"
    val geometry =
            screenGeometry[geometryKey]
                    ?: screenGeometry[zoneId]
                            ?: run {
                        Log.w(
                                TAG_VIS,
                                "displayZoneCircles: no geometry for zone $zoneId (screen=$screenId key=$geometryKey) — skipping"
                        )
                        return
                    }

    val numZones = zonedBar.zones.size
    // Icon at the viewer-left edge (high display-x), outside the circle area.
    val iconPx = if (iconId != null) 28 else 0
    val safeRight = LayoutPositionDefaults.ZONE_X0 + LayoutPositionDefaults.ZONE_WIDTH // =274
    val iconX = (safeRight - iconPx + 12).toShort() // 246 with icon, 274 without
    // Shift circles +20px in display-x (toward viewer-left) to align with icon row above.
    val circleXOffset = 0
    val circleAreaWidth = iconX - 6 - (LayoutPositionDefaults.ZONE_X0 + circleXOffset)

    val slot = circleAreaWidth / numZones
    // Base radius: fit within slot width and within zone height.
    val rBySlot = slot / 2 - 1
    val rByHeight = geometry.height / 2 - 2
    val r = minOf(rBySlot, rByHeight).coerceAtLeast(2)
    val cy = geometry.y0 + geometry.height / 2

    val isHeartRateMode = sourceMetricId == 47

    // Determine active zone index (1-based). If value exceeds all zones, use last.
    val activeZoneIdx: Int =
            activeZoneIndexOverride
                    ?: run {
                        val idx =
                                zonedBar.zones.indexOfFirst {
                                    currentValue >= it.minValue && currentValue < it.maxValue
                                }
                        if (idx < 0) zonedBar.zones.size else idx + 1
                    }

    // Zone 1 (lowest effort) → highest display-x → viewer-left.
    // Zone N (max effort) → lowest display-x → viewer-right. Matches Test 13 geometry.
    fun zoneCx(z: Int) =
            LayoutPositionDefaults.ZONE_X0 + circleXOffset + slot / 2 + (numZones - z) * slot

    try {
        glasses.holdFlush(holdFlushAction.HOLD)

        // Clear the zone area — extend 24px above y0 so any label drawn above the zone
        // boundary (possible when txtY = cy-24 < y0 for short zones) is also erased.
        val eraseY0 = (geometry.y0 - 24).coerceAtLeast(0).toShort()
        glasses.color(0)
        glasses.rectf(
                geometry.x0.toShort(),
                eraseY0,
                (geometry.x0 + geometry.width).toShort(),
                (geometry.y0 + geometry.height).toShort(),
        )

        for (z in 1..numZones) {
            val cx = zoneCx(z)
            val rInactive = (r * 0.7f).toInt().coerceAtLeast(2)
            val rActive = minOf((r * 1.3f).toInt(), geometry.height / 2).coerceAtLeast(r)
            if (z == activeZoneIdx) {
                // Active zone: bright white and larger.
                glasses.color(15)
                outlineCircle(glasses, cx, cy, rActive)
                if (isHeartRateMode && overlayText != null) {
                    val hrText = overlayText
                    val txtX = (cx + 17).toShort()
                    val txtYCenter = (cy + 10).toShort()
                    glasses.txt(txtX, txtYCenter, Rotation.TOP_LR, 1.toByte(), 15.toByte(), hrText)
                }
            } else {
                // Non-active zones are always visible.
                glasses.color(2)
                outlineCircle(glasses, cx, cy, if (isHeartRateMode) rInactive else r)
            }
        }

        // Icon pass: imgDisplay requires ALooK system config
        if (iconId != null && !isHeartRateMode) {
            glasses.cfgSet("ALooK")
            glasses.imgDisplay(iconId.toByte(), iconX, (cy - iconPx / 2).toShort())
            activeConfigName?.let { glasses.cfgSet(it) }
        }

        glasses.holdFlush(holdFlushAction.FLUSH)

        Log.d(
                TAG_VIS,
                "Zone circles zone=$zoneId numZones=$numZones active=$activeZoneIdx value=$currentValue"
        )
    } catch (e: Exception) {
        Log.e(TAG_VIS, "displayZoneCircles failed for zone $zoneId: ${e.message}", e)
        try {
            glasses.holdFlush(holdFlushAction.FLUSH)
        } catch (_: Exception) {}
    }
}

/** Draws a filled circle at ([cx], [cy]) with radius [r] using horizontal scan-lines. */
private fun filledCircle(glasses: Glasses, cx: Int, cy: Int, r: Int) {
    for (dy in -r..r) {
        val dx = Math.sqrt((r * r - dy * dy).toDouble()).toInt()
        if (dx == 0) continue
        glasses.line(
                (cx - dx).toShort(),
                (cy + dy).toShort(),
                (cx + dx).toShort(),
                (cy + dy).toShort(),
        )
    }
}

/** Draws a circle outline at ([cx], [cy]) with radius [r] using a polyline. */
private fun outlineCircle(glasses: Glasses, cx: Int, cy: Int, r: Int, steps: Int = 24) {
    val pts = ShortArray((steps + 1) * 2)
    for (i in 0..steps) {
        val angle = 2 * Math.PI * i / steps
        pts[i * 2] = (cx + (r * Math.cos(angle)).toInt()).toShort()
        pts[i * 2 + 1] = (cy + (r * Math.sin(angle)).toInt()).toShort()
    }
    glasses.polyline(pts)
}
