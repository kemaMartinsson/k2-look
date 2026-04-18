package com.kema.k2look.service

import io.hammerhead.karooext.models.DataType
import kotlinx.coroutines.launch

/**
 * Karoo metric observer extension functions for [KarooActiveLookBridge].
 *
 * Each function subscribes to one category of Karoo data streams and writes formatted values into
 * [KarooActiveLookBridge.currentData] via the [KarooActiveLookBridge.observe] helper. Separated
 * from the main class to keep file size manageable; all functions are called from
 * [KarooActiveLookBridge.observeKarooData].
 */
private const val TAG_METRICS = "KarooActiveLookBridge"

// ── Core cycling metrics ───────────────────────────────────────────────

internal fun KarooActiveLookBridge.observeCoreMetrics() {
    observe(karooDataService.speedData) { speed = formatStreamData(it, "km/h") }
    observe(karooDataService.maxSpeedData) { maxSpeed = formatStreamData(it, "km/h") }
    observe(karooDataService.averageSpeedData) { avgSpeed = formatStreamData(it, "km/h") }
    observe(karooDataService.heartRateData) { heartRate = formatStreamData(it, "bpm") }
    observe(karooDataService.maxHeartRateData) { maxHeartRate = formatStreamData(it, "bpm") }
    observe(karooDataService.averageHeartRateData) { avgHeartRate = formatStreamData(it, "bpm") }
    observe(karooDataService.hrZoneData) { hrZone = formatHRZoneData(it) }
    observe(karooDataService.cadenceData) { cadence = formatStreamDataInt(it, "rpm") }
    observe(karooDataService.maxCadenceData) { maxCadence = formatStreamDataInt(it, "rpm") }
    observe(karooDataService.averageCadenceData) { avgCadence = formatStreamDataInt(it, "rpm") }
    observe(karooDataService.powerData) { power = formatStreamDataInt(it, "w") }
    observe(karooDataService.maxPowerData) { maxPower = formatStreamDataInt(it, "w") }
    observe(karooDataService.averagePowerData) { avgPower = formatStreamDataInt(it, "w") }
    observe(karooDataService.smoothed3sPowerData) { power3s = formatStreamDataInt(it, "w") }
    observe(karooDataService.distanceData) { distance = formatStreamData(it, "km") }
    observe(karooDataService.timeData) { time = formatTimeData(it) }
    observe(karooDataService.vamData) { vam = formatStreamData(it, "m/h") }
    observe(karooDataService.avgVamData) { avgVam = formatStreamData(it, "m/h") }
}

// ── General additions ──────────────────────────────────────────────────

internal fun KarooActiveLookBridge.observeGeneralMetrics() {
    observe(karooDataService.clockTimeData) { clockTime = formatClockTime(it) }
    observe(karooDataService.temperatureData) { temperature = formatStreamData(it, "°C") }
    observe(karooDataService.batteryPercentData) { batteryPercent = formatPercent(it) }
    observe(karooDataService.rideTimeData) { rideTime = formatTimeData(it) }
}

// ── Heart Rate additions ───────────────────────────────────────────────

internal fun KarooActiveLookBridge.observeHeartRateMetrics() {
    observe(karooDataService.percentMaxHrData) { percentMaxHr = formatPercent(it) }
    observe(karooDataService.percentHrrData) { percentHrr = formatPercent(it) }
}

// ── Power additions ────────────────────────────────────────────────────

internal fun KarooActiveLookBridge.observePowerMetrics() {
    observe(karooDataService.powerZoneData) { powerZone = formatZoneData(it, 7) }
    observe(karooDataService.smoothed5sPowerData) { power5s = formatStreamDataInt(it, "w") }
    observe(karooDataService.smoothed10sPowerData) { power10s = formatStreamDataInt(it, "w") }
    observe(karooDataService.smoothed30sPowerData) { power30s = formatStreamDataInt(it, "w") }
    observe(karooDataService.normalizedPowerData) { normalizedPower = formatStreamDataInt(it, "w") }
    observe(karooDataService.percentFtpData) { percentFtp = formatPercent(it) }
    observe(karooDataService.intensityFactorData) { intensityFactor = formatStreamData(it, "") }
    observe(karooDataService.trainingStressScoreData) { tss = formatStreamData(it, "") }
    observe(karooDataService.powerToWeightData) { wPerKg = formatStreamData(it, "w/kg") }
}

// ── Energy ────────────────────────────────────────────────────────────

internal fun KarooActiveLookBridge.observeEnergyMetrics() {
    observe(karooDataService.energyOutputData) { energyOutput = formatStreamDataInt(it, "kJ") }
    observe(karooDataService.caloriesData) { calories = formatStreamDataInt(it, "kcal") }
    observe(karooDataService.caloriesPerHourData) {
        caloriesPerHour = formatStreamDataInt(it, "kcal/h")
    }
}

// ── Speed and Cadence additions ────────────────────────────────────────

internal fun KarooActiveLookBridge.observeSpeedCadenceMetrics() {
    observe(karooDataService.smoothed3sSpeedData) { speed3s = formatStreamData(it, "km/h") }
    observe(karooDataService.smoothed3sCadenceData) { cadence3s = formatStreamData(it, "rpm") }
}

// ── Elevation ─────────────────────────────────────────────────────────

internal fun KarooActiveLookBridge.observeElevationMetrics() {
    observe(karooDataService.elevationGradeData) { elevationGrade = formatGrade(it) }
    observe(karooDataService.elevationGainData) { elevationGain = formatStreamData(it, "m") }
    observe(karooDataService.elevationLossData) { elevationLoss = formatStreamData(it, "m") }
    observe(karooDataService.altitudeData) { altitude = formatStreamData(it, "m") }
    observe(karooDataService.vam30sData) { vam30s = formatStreamData(it, "m/h") }
}

// ── Lap ───────────────────────────────────────────────────────────────

internal fun KarooActiveLookBridge.observeLapMetrics() {
    observe(karooDataService.lapNumberData) { lapNumber = formatInteger(it) }
    observe(karooDataService.lapTimeData) { lapTime = formatLapTime(it) }
    observe(karooDataService.lapDistanceData) { lapDistance = formatStreamData(it, "km") }
    observe(karooDataService.lapSpeedData) { lapSpeed = formatStreamData(it, "km/h") }
    observe(karooDataService.lapHrData) { lapHr = formatStreamData(it, "bpm") }
    observe(karooDataService.lapPowerData) { lapPower = formatStreamData(it, "w") }
    observe(karooDataService.lapNpData) { lapNp = formatStreamData(it, "w") }
    observe(karooDataService.lapCadenceData) { lapCadence = formatStreamData(it, "rpm") }
    observe(karooDataService.lapAscentData) { lapAscent = formatStreamData(it, "m") }
}

// ── Last Lap ──────────────────────────────────────────────────────────

internal fun KarooActiveLookBridge.observeLastLapMetrics() {
    observe(karooDataService.lastLapTimeData) { lastLapTime = formatLapTime(it) }
    observe(karooDataService.lastLapDistanceData) { lastLapDistance = formatStreamData(it, "km") }
    observe(karooDataService.lastLapSpeedData) { lastLapSpeed = formatStreamData(it, "km/h") }
    observe(karooDataService.lastLapHrData) { lastLapHr = formatStreamData(it, "bpm") }
    observe(karooDataService.lastLapPowerData) { lastLapPower = formatStreamData(it, "w") }
    observe(karooDataService.lastLapNpData) { lastLapNp = formatStreamData(it, "w") }
}

// ── Shifting (multi-field for gears, simple for count) ─────────────────

internal fun KarooActiveLookBridge.observeShiftingMetrics() {
    scope.launch {
        karooDataService.shiftingFrontGearData.collect {
            currentData.shiftingFrontGear =
                    formatGear(
                            it,
                            DataType.Field.SHIFTING_FRONT_GEAR,
                            DataType.Field.SHIFTING_FRONT_GEAR_MAX
                    )
            currentData.isDirty = true
        }
    }
    scope.launch {
        karooDataService.shiftingRearGearData.collect {
            currentData.shiftingRearGear =
                    formatGear(
                            it,
                            DataType.Field.SHIFTING_REAR_GEAR,
                            DataType.Field.SHIFTING_REAR_GEAR_MAX
                    )
            currentData.isDirty = true
        }
    }
    scope.launch {
        karooDataService.shiftingBatteryData.collect {
            currentData.shiftingBattery = formatShiftingBattery(it)
            currentData.isDirty = true
        }
    }
    observe(karooDataService.shiftingCountData) { shiftingCount = formatInteger(it) }
}

// ── Navigation ────────────────────────────────────────────────────────

internal fun KarooActiveLookBridge.observeNavigationMetrics() {
    observe(karooDataService.distanceToTurnData) { distanceToTurn = formatDistanceToTurn(it) }
    observe(karooDataService.distanceToDestData) { distanceToDest = formatStreamData(it, "km") }
    observe(karooDataService.timeOfArrivalData) { timeOfArrival = formatClockTime(it) }
    observe(karooDataService.timeToDestData) { timeToDest = formatDuration(it) }
    observe(karooDataService.headingData) { heading = formatHeading(it) }
}

// ── eBike ─────────────────────────────────────────────────────────────

internal fun KarooActiveLookBridge.observeEBikeMetrics() {
    observe(karooDataService.levBatteryData) { levBattery = formatPercent(it) }
    observe(karooDataService.levRangeData) { levRange = formatStreamData(it, "km") }
    observe(karooDataService.levAssistModeData) { levAssistMode = formatInteger(it) }
    observe(karooDataService.levMotorPowerData) { levMotorPower = formatStreamData(it, "w") }
}
