package com.kema.k2look.service

import io.hammerhead.karooext.models.RideState

/**
 * Holds the last-formatted display string for every metric the bridge streams. All values are
 * pre-formatted and ready to send to the glasses.
 *
 * [valueFor] provides a single lookup point from DataField ID → formatted string, replacing the
 * large `when` block that used to live in KarooActiveLookBridge.
 */
internal data class CurrentData(
        // ── Core metrics ──────────────────────────────────────────────────────
        var speed: String = "--",
        var maxSpeed: String = "--",
        var avgSpeed: String = "--",
        var heartRate: String = "--",
        var maxHeartRate: String = "--",
        var avgHeartRate: String = "--",
        var hrZone: String = "--",
        var cadence: String = "--",
        var maxCadence: String = "--",
        var avgCadence: String = "--",
        var power: String = "--",
        var maxPower: String = "--",
        var avgPower: String = "--",
        var power3s: String = "--",
        var distance: String = "--",
        var time: String = "--",
        var vam: String = "--",
        var avgVam: String = "--",

        // ── Radar ─────────────────────────────────────────────────────────────
        var radarThreatLevel: String = "--",
        var radarTargetCount: String = "--",
        var radarClosestRange: String = "--",

        // ── General additions ─────────────────────────────────────────────────
        var clockTime: String = "--",
        var temperature: String = "--",
        var batteryPercent: String = "--",
        var rideTime: String = "--",

        // ── Heart Rate additions ──────────────────────────────────────────────
        var percentMaxHr: String = "--",
        var percentHrr: String = "--",

        // ── Power additions ───────────────────────────────────────────────────
        var powerZone: String = "--",
        var power5s: String = "--",
        var power10s: String = "--",
        var power30s: String = "--",
        var normalizedPower: String = "--",
        var percentFtp: String = "--",
        var intensityFactor: String = "--",
        var tss: String = "--",
        var wPerKg: String = "--",

        // ── Energy ────────────────────────────────────────────────────────────
        var energyOutput: String = "--",
        var calories: String = "--",
        var caloriesPerHour: String = "--",

        // ── Speed / Cadence additions ──────────────────────────────────────────
        var speed3s: String = "--",
        var cadence3s: String = "--",

        // ── Elevation ─────────────────────────────────────────────────────────
        var elevationGrade: String = "--",
        var elevationGain: String = "--",
        var elevationLoss: String = "--",
        var altitude: String = "--",
        var vam30s: String = "--",

        // ── Lap ───────────────────────────────────────────────────────────────
        var lapNumber: String = "--",
        var lapTime: String = "--",
        var lapDistance: String = "--",
        var lapSpeed: String = "--",
        var lapHr: String = "--",
        var lapPower: String = "--",
        var lapNp: String = "--",
        var lapCadence: String = "--",
        var lapAscent: String = "--",

        // ── Last Lap ──────────────────────────────────────────────────────────
        var lastLapTime: String = "--",
        var lastLapDistance: String = "--",
        var lastLapSpeed: String = "--",
        var lastLapHr: String = "--",
        var lastLapPower: String = "--",
        var lastLapNp: String = "--",

        // ── Shifting ──────────────────────────────────────────────────────────
        var shiftingFrontGear: String = "--",
        var shiftingRearGear: String = "--",
        var shiftingBattery: String = "--",
        var shiftingCount: String = "--",

        // ── Navigation ────────────────────────────────────────────────────────
        var distanceToTurn: String = "--",
        var distanceToDest: String = "--",
        var timeOfArrival: String = "--",
        var timeToDest: String = "--",
        var heading: String = "--",

        // ── eBike ─────────────────────────────────────────────────────────────
        var levBattery: String = "--",
        var levRange: String = "--",
        var levAssistMode: String = "--",
        var levMotorPower: String = "--",

        // ── State ─────────────────────────────────────────────────────────────
        var rideState: RideState = RideState.Idle,
        var isDirty: Boolean = false,
        // Set to true when a radar update arrives and the profile contains a radar field.
        // Causes flushToGlasses() to bypass the normal 2-second throttle for this one frame.
        var radarFlushImmediate: Boolean = false
) {
    /**
     * Return the current formatted value for the given DataField ID. This is the single lookup
     * point used by the flush pipeline.
     */
    fun valueFor(id: Int): String =
            when (id) {
                // Core
                1 -> time
                2 -> distance
                4 -> heartRate
                5 -> maxHeartRate
                6 -> avgHeartRate
                47 -> hrZone
                7 -> power
                8 -> maxPower
                9 -> avgPower
                10 -> power3s
                12 -> speed
                13 -> maxSpeed
                14 -> avgSpeed
                18 -> cadence
                19 -> maxCadence
                20 -> avgCadence
                24 -> vam
                25 -> avgVam
                // Radar
                50 -> radarThreatLevel
                51 -> radarTargetCount
                52 -> radarClosestRange
                // General
                53 -> clockTime
                54 -> temperature
                55 -> batteryPercent
                56 -> rideTime
                // Heart Rate
                57 -> percentMaxHr
                58 -> percentHrr
                // Power
                48 -> powerZone
                59 -> power5s
                60 -> power10s
                61 -> power30s
                62 -> normalizedPower
                63 -> percentFtp
                64 -> intensityFactor
                65 -> tss
                66 -> wPerKg
                // Energy
                67 -> energyOutput
                68 -> calories
                69 -> caloriesPerHour
                // Speed / Cadence
                70 -> speed3s
                71 -> cadence3s
                // Elevation
                72 -> elevationGrade
                73 -> elevationGain
                74 -> elevationLoss
                75 -> altitude
                76 -> vam30s
                // Lap
                77 -> lapNumber
                78 -> lapTime
                79 -> lapDistance
                80 -> lapSpeed
                81 -> lapHr
                82 -> lapPower
                83 -> lapNp
                84 -> lapCadence
                85 -> lapAscent
                // Last Lap
                86 -> lastLapTime
                87 -> lastLapDistance
                88 -> lastLapSpeed
                89 -> lastLapHr
                90 -> lastLapPower
                91 -> lastLapNp
                // Shifting
                92 -> shiftingFrontGear
                93 -> shiftingRearGear
                94 -> shiftingBattery
                95 -> shiftingCount
                // Navigation
                96 -> distanceToTurn
                97 -> distanceToDest
                98 -> timeOfArrival
                99 -> timeToDest
                100 -> heading
                // eBike
                101 -> levBattery
                102 -> levRange
                103 -> levAssistMode
                104 -> levMotorPower
                else -> "n/a"
            }
}
