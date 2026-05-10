package com.kema.k2look.service

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Simulator extension functions for [KarooActiveLookBridge].
 *
 * Provides a local data simulator that periodically pushes sample values to the glasses using the
 * same flush pipeline as normal streaming. Used by the Debug tab for testing layouts without a real
 * Karoo ride.
 *
 * Entry points: [startSimulator] / [stopSimulator] (called from [MainViewModel]).
 */
private const val TAG_SIM = "KarooActiveLookBridge"

/**
 * Start a local simulator that periodically pushes sample values to the glasses. This uses the same
 * flush pipeline as normal streaming.
 */
fun KarooActiveLookBridge.startSimulator() {
    Log.i(TAG_SIM, "🎮 startSimulator() called")
    Log.i(TAG_SIM, "  simulatorJob?.isActive: ${simulatorJob?.isActive}")
    Log.i(TAG_SIM, "  activeLookService.isConnected: ${activeLookService.isConnected}")

    if (simulatorJob?.isActive == true) {
        Log.d(TAG_SIM, "⚠️ Simulator already running")
        return
    }

    if (!activeLookService.isConnected) {
        Log.w(TAG_SIM, "❌ Cannot start simulator: glasses not connected")
        return
    }

    Log.i(TAG_SIM, "✅ Starting simulator (profile-aware)")
    simulatorJob =
            scope.launch {
                var tick = 0
                Log.i(TAG_SIM, "🔁 Simulator coroutine loop started")
                while (true) {
                    tick++
                    Log.d(
                            TAG_SIM,
                            "📊 Simulator tick $tick (profile: ${activeProfile?.name ?: "none"})"
                    )
                    pushSimulatedFrame(tick)
                    delay(2000)
                }
            }
    Log.i(TAG_SIM, "✅ Simulator job launched successfully")
}

/** Stop the simulator (if running). */
fun KarooActiveLookBridge.stopSimulator() {
    if (simulatorJob?.isActive == true) {
        Log.i(TAG_SIM, "⏹ Stopping simulator")
    }
    simulatorJob?.cancel()
    simulatorJob = null
}

/**
 * Simulate one frame: populate every field that appears in the active profile, then flush to the
 * glasses. Falls back to the 6 core fields when no profile is set.
 */
private fun KarooActiveLookBridge.pushSimulatedFrame(tick: Int) {
    val fieldIds =
            activeProfile
                    ?.screens
                    ?.flatMap { it.dataFields }
                    ?.map { it.dataField.id }
                    ?.distinct()
                    ?.takeIf { it.isNotEmpty() }
                    ?.toMutableSet()
                    ?: mutableSetOf(
                            1,
                            2,
                            4,
                            7,
                            12,
                            18
                    ) // fallback: time, distance, HR, power, speed, cadence

    // Zoned overlays depend on base metrics (HR/Power) even when only zone fields are selected.
    if (47 in fieldIds) fieldIds.add(4)
    if (48 in fieldIds) fieldIds.add(7)

    for (id in fieldIds) {
        applySimulatedValue(id, tick)
    }
    currentData.isDirty = true
    flushToGlasses()
}

/** Write one simulated value into [currentData] for [id]. */
@Suppress("ComplexMethod", "LongMethod")
private fun KarooActiveLookBridge.applySimulatedValue(id: Int, t: Int) {
    when (id) {
        // ── General ──────────────────────────────────────────────────
        1 -> currentData.time = formatSimulatedTime(t * 2)
        2 -> currentData.distance = "${t / 10}.${t % 10} km"
        53 -> currentData.clockTime = String.format(java.util.Locale.ROOT, "14:%02d", t % 60)
        54 -> currentData.temperature = "${18 + t % 10} °C"
        55 -> currentData.batteryPercent = "${80 - t % 30}%"
        56 -> currentData.rideTime = formatSimulatedTime(t * 2)
        // ── Heart Rate ───────────────────────────────────────────────
        4 -> currentData.heartRate = "${140 + t % 30}"
        5 -> currentData.maxHeartRate = "175"
        6 -> currentData.avgHeartRate = "145"
        47 -> currentData.hrZone = "Z${2 + (t / 10) % 3}"
        57 -> currentData.percentMaxHr = "${75 + t % 15}%"
        58 -> currentData.percentHrr = "${65 + t % 20}%"
        // ── Power ────────────────────────────────────────────────────
        7 -> currentData.power = "${200 + t % 100}"
        8 -> currentData.maxPower = "450"
        9 -> currentData.avgPower = "210"
        10 -> currentData.power3s = "${195 + t % 80}"
        48 -> currentData.powerZone = "Z${2 + (t / 15) % 4}"
        59 -> currentData.power5s = "${198 + t % 90}"
        60 -> currentData.power10s = "${205 + t % 70}"
        61 -> currentData.power30s = "${210 + t % 50}"
        62 -> currentData.normalizedPower = "${215 + t % 40}"
        63 -> currentData.percentFtp = "${85 + t % 30}%"
        64 ->
                currentData.intensityFactor =
                        String.format(java.util.Locale.ROOT, "%.2f", 0.85 + (t % 15) * 0.01)
        65 -> currentData.tss = "${50 + t * 2}"
        66 ->
                currentData.wPerKg =
                        String.format(java.util.Locale.ROOT, "%.1f", 3.2 + (t % 10) * 0.1)
        // ── Speed ────────────────────────────────────────────────────
        12 -> currentData.speed = "${25 + t % 15}"
        13 -> currentData.maxSpeed = "42"
        14 -> currentData.avgSpeed = "28"
        70 -> currentData.speed3s = "${24 + t % 12}"
        // ── Cadence ──────────────────────────────────────────────────
        18 -> currentData.cadence = "${85 + t % 20}"
        19 -> currentData.maxCadence = "102"
        20 -> currentData.avgCadence = "88"
        71 -> currentData.cadence3s = "${83 + t % 18}"
        // ── Energy ───────────────────────────────────────────────────
        67 -> currentData.energyOutput = "${200 + t * 5} kJ"
        68 -> currentData.calories = "${150 + t * 3}"
        69 -> currentData.caloriesPerHour = "${600 + t % 200}"
        // ── Climbing ─────────────────────────────────────────────────
        24 -> currentData.vam = "${800 + t % 400}"
        25 -> currentData.avgVam = "650"
        // ── Elevation ────────────────────────────────────────────────
        72 -> currentData.elevationGrade = "${-2 + t % 8}%"
        73 -> currentData.elevationGain = "${100 + t * 2} m"
        74 -> currentData.elevationLoss = "${30 + t} m"
        75 -> currentData.altitude = "${250 + t * 3} m"
        76 -> currentData.vam30s = "${700 + t % 300}"
        // ── Lap ──────────────────────────────────────────────────────
        77 -> currentData.lapNumber = "${1 + t / 30}"
        78 -> currentData.lapTime = formatSimulatedTime(t * 2 % 3600)
        79 ->
                currentData.lapDistance =
                        String.format(java.util.Locale.ROOT, "%.1f km", (t % 50) * 0.1 + 0.1)
        80 -> currentData.lapSpeed = "${26 + t % 10}"
        81 -> currentData.lapHr = "${138 + t % 25}"
        82 -> currentData.lapPower = "${205 + t % 80}"
        83 -> currentData.lapNp = "${210 + t % 70}"
        84 -> currentData.lapCadence = "${87 + t % 15}"
        85 -> currentData.lapAscent = "${20 + t % 80} m"
        // ── Last Lap ─────────────────────────────────────────────────
        86 -> currentData.lastLapTime = "00:45:12"
        87 -> currentData.lastLapDistance = "22.5 km"
        88 -> currentData.lastLapSpeed = "29"
        89 -> currentData.lastLapHr = "142"
        90 -> currentData.lastLapPower = "215"
        91 -> currentData.lastLapNp = "220"
        // ── Radar ────────────────────────────────────────────────────
        50 -> currentData.radarThreatLevel = "${t % 3}"
        51 -> currentData.radarTargetCount = "${1 + t % 4}"
        52 -> currentData.radarClosestRange = "${15 + t % 50} m"
        // ── Shifting ─────────────────────────────────────────────────
        92 -> currentData.shiftingFrontGear = "3/3"
        93 -> currentData.shiftingRearGear = "${1 + t % 11}/11"
        94 -> currentData.shiftingBattery = "${85 - t % 20}%"
        95 -> currentData.shiftingCount = "${t * 3}"
        // ── Navigation ───────────────────────────────────────────────
        96 -> currentData.distanceToTurn = "${(2000 - t * 10).coerceAtLeast(0)} m"
        97 ->
                currentData.distanceToDest =
                        String.format(
                                java.util.Locale.ROOT,
                                "%.1f km",
                                (50.0 - t * 0.1).coerceAtLeast(0.0)
                        )
        98 -> currentData.timeOfArrival = "15:30"
        99 -> currentData.timeToDest = formatSimulatedTime((5400 - t * 2).coerceAtLeast(0))
        100 -> currentData.heading = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")[t % 8]
        // ── eBike ────────────────────────────────────────────────────
        101 -> currentData.levBattery = "${80 - t % 30}%"
        102 -> currentData.levRange = "${(60 - t).coerceAtLeast(0)} km"
        103 -> currentData.levAssistMode = listOf("OFF", "ECO", "TRAIL", "BOOST")[t % 4]
        104 -> currentData.levMotorPower = "${80 + t % 120} w"
    }
}

private fun formatSimulatedTime(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", h, m, s)
}
