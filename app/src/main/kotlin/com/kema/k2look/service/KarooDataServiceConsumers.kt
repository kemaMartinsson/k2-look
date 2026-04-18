package com.kema.k2look.service

import android.util.Log
import io.hammerhead.karooext.models.ActiveRideProfile
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.RideProfile
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.flow.MutableStateFlow
import com.kema.k2look.service.KarooDataService.ConnectionState

private const val TAG = "KarooDataService"

/**
 * Consumer registration extension functions for [KarooDataService].
 *
 * [registerConsumers] is called from [KarooDataService.connect] after a successful
 * KarooSystem connection. It registers all metric stream consumers and lifecycle events.
 * [registerStream] is a private helper used only within this file.
 */

private fun KarooDataService.registerStream(type: String, flow: MutableStateFlow<StreamState?>) {
    val id = karooSystem.addConsumer(
        OnStreamState.StartStreaming(type),
        onError = { error -> Log.e(TAG, "Stream error [$type]: $error") }
    ) { event: OnStreamState -> flow.value = event.state }
    consumerIds.add(id)
}

internal fun KarooDataService.registerConsumers() {
    Log.i(TAG, "Registering data consumers...")

    try {
        // Register RideState consumer
        val rideStateId = karooSystem.addConsumer(
            onError = { error ->
                Log.e(TAG, "RideState consumer error: $error")
            },
            onComplete = {
                Log.i(TAG, "RideState consumer completed")
            }
        ) { state: RideState ->
            Log.d(TAG, "RideState update: $state")
            _rideState.value = state
        }
        consumerIds.add(rideStateId)

        // Register ActiveRideProfile consumer (monitors profile selected by user)
        val profileId = karooSystem.addConsumer(
            onError = { error ->
                Log.e(TAG, "ActiveRideProfile consumer error: $error")
            },
            onComplete = {
                Log.i(TAG, "ActiveRideProfile consumer completed")
            }
        ) { event: ActiveRideProfile ->
            Log.i(
                TAG,
                "Active ride profile changed: ${event.profile.name} (${event.profile.id})"
            )
            _activeRideProfile.value = event.profile
        }
        consumerIds.add(profileId)

        // Register Speed stream
        val speedId = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.SPEED),
            onError = { error ->
                Log.e(TAG, "Speed stream error: $error")
            }
        ) { event: OnStreamState ->
            _speedData.value = event.state
        }
        consumerIds.add(speedId)

        // Register Average Speed stream
        val avgSpeedId = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.AVERAGE_SPEED),
            onError = { error ->
                Log.e(TAG, "Average Speed stream error: $error")
            }
        ) { event: OnStreamState ->
            _averageSpeedData.value = event.state
        }
        consumerIds.add(avgSpeedId)

        // Register Max Speed stream
        val maxSpeedId = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.MAX_SPEED),
            onError = { error ->
                Log.e(TAG, "Max Speed stream error: $error")
            }
        ) { event: OnStreamState ->
            _maxSpeedData.value = event.state
        }
        consumerIds.add(maxSpeedId)

        // Register Heart Rate stream
        val hrId = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.HEART_RATE),
            onError = { error ->
                Log.e(TAG, "Heart Rate stream error: $error")
            }
        ) { event: OnStreamState ->
            _heartRateData.value = event.state
        }
        consumerIds.add(hrId)

        // Register Average Heart Rate stream
        val avgHrId = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.AVERAGE_HR),
            onError = { error ->
                Log.e(TAG, "Average Heart Rate stream error: $error")
            }
        ) { event: OnStreamState ->
            _averageHeartRateData.value = event.state
        }
        consumerIds.add(avgHrId)

        // Register Max Heart Rate stream
        val maxHrId = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.MAX_HR),
            onError = { error ->
                Log.e(TAG, "Max Heart Rate stream error: $error")
            }
        ) { event: OnStreamState ->
            _maxHeartRateData.value = event.state
        }
        consumerIds.add(maxHrId)

        // Register Cadence stream
        val cadenceId = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.CADENCE),
            onError = { error ->
                Log.e(TAG, "Cadence stream error: $error")
            }
        ) { event: OnStreamState ->
            _cadenceData.value = event.state
        }
        consumerIds.add(cadenceId)

        // Register Average Cadence stream
        val avgCadenceId = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.AVERAGE_CADENCE),
            onError = { error ->
                Log.e(TAG, "Average Cadence stream error: $error")
            }
        ) { event: OnStreamState ->
            _averageCadenceData.value = event.state
        }
        consumerIds.add(avgCadenceId)

        // Register Max Cadence stream
        val maxCadenceId = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.MAX_CADENCE),
            onError = { error ->
                Log.e(TAG, "Max Cadence stream error: $error")
            }
        ) { event: OnStreamState ->
            _maxCadenceData.value = event.state
        }
        consumerIds.add(maxCadenceId)

        // Register Power stream
        val powerId = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.POWER),
            onError = { error ->
                Log.e(TAG, "Power stream error: $error")
            }
        ) { event: OnStreamState ->
            _powerData.value = event.state
        }
        consumerIds.add(powerId)

        // Register Average Power stream
        val avgPowerId = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.AVERAGE_POWER),
            onError = { error ->
                Log.e(TAG, "Average Power stream error: $error")
            }
        ) { event: OnStreamState ->
            _averagePowerData.value = event.state
        }
        consumerIds.add(avgPowerId)

        // Register Max Power stream
        val maxPowerId = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.MAX_POWER),
            onError = { error ->
                Log.e(TAG, "Max Power stream error: $error")
            }
        ) { event: OnStreamState ->
            _maxPowerData.value = event.state
        }
        consumerIds.add(maxPowerId)

        // Register Distance stream
        val distanceId = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.DISTANCE),
            onError = { error ->
                Log.e(TAG, "Distance stream error: $error")
            }
        ) { event: OnStreamState ->
            _distanceData.value = event.state
        }
        consumerIds.add(distanceId)

        // Register Time stream
        val timeId = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.ELAPSED_TIME),
            onError = { error ->
                Log.e(TAG, "Time stream error: $error")
            }
        ) { event: OnStreamState ->
            _timeData.value = event.state
        }
        consumerIds.add(timeId)

        // Register HR Zone stream
        val hrZoneId = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.HR_ZONE),
            onError = { error ->
                Log.e(TAG, "HR Zone stream error: $error")
            }
        ) { event: OnStreamState ->
            _hrZoneData.value = event.state
        }
        consumerIds.add(hrZoneId)

        // Register 3s Power stream
        val power3sId = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.SMOOTHED_3S_AVERAGE_POWER),
            onError = { error ->
                Log.e(TAG, "3s Power stream error: $error")
            }
        ) { event: OnStreamState ->
            _smoothed3sPowerData.value = event.state
        }
        consumerIds.add(power3sId)

        // Register 10s Power stream
        val power10sId = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.SMOOTHED_10S_AVERAGE_POWER),
            onError = { error ->
                Log.e(TAG, "10s Power stream error: $error")
            }
        ) { event: OnStreamState ->
            _smoothed10sPowerData.value = event.state
        }
        consumerIds.add(power10sId)

        // Register 30s Power stream
        val power30sId = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.SMOOTHED_30S_AVERAGE_POWER),
            onError = { error ->
                Log.e(TAG, "30s Power stream error: $error")
            }
        ) { event: OnStreamState ->
            _smoothed30sPowerData.value = event.state
        }
        consumerIds.add(power30sId)

        // Register VAM stream
        val vamId = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.VERTICAL_SPEED),
            onError = { error ->
                Log.e(TAG, "VAM stream error: $error")
            }
        ) { event: OnStreamState ->
            _vamData.value = event.state
        }
        consumerIds.add(vamId)

        // Register Average VAM stream
        val avgVamId = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.AVERAGE_VERTICAL_SPEED),
            onError = { error ->
                Log.e(TAG, "Average VAM stream error: $error")
            }
        ) { event: OnStreamState ->
            _avgVamData.value = event.state
        }
        consumerIds.add(avgVamId)

        // Register Radar stream (threat level + up to 8 target ranges in one DataPoint)
        val radarId = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.RADAR),
            onError = { error ->
                Log.e(TAG, "Radar stream error: $error")
            }
        ) { event: OnStreamState ->
            _radarData.value = event.state
        }
        consumerIds.add(radarId)

        // Register all remaining streams via helper
        registerStream(DataType.Type.POWER_ZONE,                   _powerZoneData)
        registerStream(DataType.Type.CLOCK_TIME,                   _clockTimeData)
        registerStream(DataType.Type.TEMPERATURE,                  _temperatureData)
        registerStream(DataType.Type.BATTERY_PERCENT,              _batteryPercentData)
        registerStream(DataType.Type.RIDE_TIME,                    _rideTimeData)
        registerStream(DataType.Type.PERCENT_MAX_HR,               _percentMaxHrData)
        registerStream(DataType.Type.PERCENT_HRR,                  _percentHrrData)
        registerStream(DataType.Type.SMOOTHED_5S_AVERAGE_POWER,    _smoothed5sPowerData)
        registerStream(DataType.Type.NORMALIZED_POWER,             _normalizedPowerData)
        registerStream(DataType.Type.PERCENT_MAX_FTP,              _percentFtpData)
        registerStream(DataType.Type.INTENSITY_FACTOR,             _intensityFactorData)
        registerStream(DataType.Type.TRAINING_STRESS_SCORE,        _trainingStressScoreData)
        registerStream(DataType.Type.POWER_TO_WEIGHT,              _powerToWeightData)
        registerStream(DataType.Type.ENERGY_OUTPUT,                _energyOutputData)
        registerStream(DataType.Type.CALORIES,                     _caloriesData)
        registerStream(DataType.Type.CALORIES_PER_HOUR,            _caloriesPerHourData)
        registerStream(DataType.Type.SMOOTHED_3S_AVERAGE_SPEED,    _smoothed3sSpeedData)
        registerStream(DataType.Type.SMOOTHED_3S_AVERAGE_CADENCE,  _smoothed3sCadenceData)
        registerStream(DataType.Type.ELEVATION_GRADE,              _elevationGradeData)
        registerStream(DataType.Type.ELEVATION_GAIN,               _elevationGainData)
        registerStream(DataType.Type.ELEVATION_LOSS,               _elevationLossData)
        registerStream(DataType.Type.PRESSURE_ELEVATION_CORRECTION,_altitudeData)
        registerStream(DataType.Type.AVERAGE_VERTICAL_SPEED_30S,   _vam30sData)
        registerStream(DataType.Type.LAP_NUMBER,                   _lapNumberData)
        registerStream(DataType.Type.ELAPSED_TIME_LAP,             _lapTimeData)
        registerStream(DataType.Type.DISTANCE_LAP,                 _lapDistanceData)
        registerStream(DataType.Type.AVERAGE_SPEED_LAP,            _lapSpeedData)
        registerStream(DataType.Type.AVERAGE_LAP_HR,               _lapHrData)
        registerStream(DataType.Type.POWER_LAP,                    _lapPowerData)
        registerStream(DataType.Type.NORMALIZED_POWER_LAP,         _lapNpData)
        registerStream(DataType.Type.CADENCE_LAP,                  _lapCadenceData)
        registerStream(DataType.Type.ELEVATION_GAIN_LAP,           _lapAscentData)
        registerStream(DataType.Type.ELAPSED_TIME_LAST_LAP,        _lastLapTimeData)
        registerStream(DataType.Type.DISTANCE_LAP_LAST_LAP,        _lastLapDistanceData)
        registerStream(DataType.Type.AVERAGE_SPEED_LAST_LAP,       _lastLapSpeedData)
        registerStream(DataType.Type.AVERAGE_HR_LAST_LAP,          _lastLapHrData)
        registerStream(DataType.Type.AVERAGE_POWER_LAST_LAP,       _lastLapPowerData)
        registerStream(DataType.Type.NORMALIZED_POWER_LAST_LAP,    _lastLapNpData)
        registerStream(DataType.Type.SHIFTING_FRONT_GEAR,          _shiftingFrontGearData)
        registerStream(DataType.Type.SHIFTING_REAR_GEAR,           _shiftingRearGearData)
        registerStream(DataType.Type.SHIFTING_BATTERY,             _shiftingBatteryData)
        registerStream(DataType.Type.SHIFTING_COUNT,               _shiftingCountData)
        registerStream(DataType.Type.DISTANCE_TO_NEXT_TURN,        _distanceToTurnData)
        registerStream(DataType.Type.DISTANCE_TO_DESTINATION,      _distanceToDestData)
        registerStream(DataType.Type.TIME_OF_ARRIVAL,              _timeOfArrivalData)
        registerStream(DataType.Type.TIME_TO_DESTINATION,          _timeToDestData)
        registerStream(DataType.Type.HEADING,                      _headingData)
        registerStream(DataType.Type.LEV_BATTERY_STATUS,           _levBatteryData)
        registerStream(DataType.Type.LEV_ESTIMATED_RANGE,          _levRangeData)
        registerStream(DataType.Type.LEV_ASSIST_MODE,              _levAssistModeData)
        registerStream(DataType.Type.LEV_MOTOR_POWER,              _levMotorPowerData)

        Log.i(TAG, "Successfully registered ${consumerIds.size} data consumers")
    } catch (e: Exception) {
        Log.e(TAG, "Error registering consumers: ${e.message}", e)
        _connectionState.value = ConnectionState.Error("Failed to register consumers")
    }
}
