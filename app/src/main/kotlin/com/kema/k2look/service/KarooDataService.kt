package com.kema.k2look.service

import android.content.Context
import android.util.Log
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.ActiveRideProfile
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.RideProfile
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service responsible for managing the connection to Karoo System and consuming ride data.
 *
 * This service handles:
 * - Connection lifecycle (connect/disconnect/reconnect)
 * - RideState monitoring
 * - Data stream consumption for metrics
 * - Error handling and recovery
 */
class KarooDataService(context: Context) {

    private val karooSystem = KarooSystemService(context)

    // Consumer IDs for cleanup
    private val consumerIds = mutableListOf<String>()

    // Connection state
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Ride state
    private val _rideState = MutableStateFlow<RideState>(RideState.Idle)
    val rideState: StateFlow<RideState> = _rideState.asStateFlow()

    // Active ride profile (profile selected by user on launcher)
    private val _activeRideProfile = MutableStateFlow<RideProfile?>(null)
    val activeRideProfile: StateFlow<RideProfile?> = _activeRideProfile.asStateFlow()

    // Metric streams
    private val _speedData = MutableStateFlow<StreamState?>(null)
    val speedData: StateFlow<StreamState?> = _speedData.asStateFlow()

    private val _averageSpeedData = MutableStateFlow<StreamState?>(null)
    val averageSpeedData: StateFlow<StreamState?> = _averageSpeedData.asStateFlow()

    private val _maxSpeedData = MutableStateFlow<StreamState?>(null)
    val maxSpeedData: StateFlow<StreamState?> = _maxSpeedData.asStateFlow()

    private val _heartRateData = MutableStateFlow<StreamState?>(null)
    val heartRateData: StateFlow<StreamState?> = _heartRateData.asStateFlow()

    private val _averageHeartRateData = MutableStateFlow<StreamState?>(null)
    val averageHeartRateData: StateFlow<StreamState?> = _averageHeartRateData.asStateFlow()

    private val _maxHeartRateData = MutableStateFlow<StreamState?>(null)
    val maxHeartRateData: StateFlow<StreamState?> = _maxHeartRateData.asStateFlow()

    private val _cadenceData = MutableStateFlow<StreamState?>(null)
    val cadenceData: StateFlow<StreamState?> = _cadenceData.asStateFlow()

    private val _averageCadenceData = MutableStateFlow<StreamState?>(null)
    val averageCadenceData: StateFlow<StreamState?> = _averageCadenceData.asStateFlow()

    private val _maxCadenceData = MutableStateFlow<StreamState?>(null)
    val maxCadenceData: StateFlow<StreamState?> = _maxCadenceData.asStateFlow()

    private val _powerData = MutableStateFlow<StreamState?>(null)
    val powerData: StateFlow<StreamState?> = _powerData.asStateFlow()

    private val _averagePowerData = MutableStateFlow<StreamState?>(null)
    val averagePowerData: StateFlow<StreamState?> = _averagePowerData.asStateFlow()

    private val _maxPowerData = MutableStateFlow<StreamState?>(null)
    val maxPowerData: StateFlow<StreamState?> = _maxPowerData.asStateFlow()

    private val _distanceData = MutableStateFlow<StreamState?>(null)
    val distanceData: StateFlow<StreamState?> = _distanceData.asStateFlow()

    private val _timeData = MutableStateFlow<StreamState?>(null)
    val timeData: StateFlow<StreamState?> = _timeData.asStateFlow()

    // Advanced metrics
    private val _hrZoneData = MutableStateFlow<StreamState?>(null)
    val hrZoneData: StateFlow<StreamState?> = _hrZoneData.asStateFlow()

    private val _smoothed3sPowerData = MutableStateFlow<StreamState?>(null)
    val smoothed3sPowerData: StateFlow<StreamState?> = _smoothed3sPowerData.asStateFlow()

    private val _smoothed10sPowerData = MutableStateFlow<StreamState?>(null)
    val smoothed10sPowerData: StateFlow<StreamState?> = _smoothed10sPowerData.asStateFlow()

    private val _smoothed30sPowerData = MutableStateFlow<StreamState?>(null)
    val smoothed30sPowerData: StateFlow<StreamState?> = _smoothed30sPowerData.asStateFlow()

    private val _vamData = MutableStateFlow<StreamState?>(null)
    val vamData: StateFlow<StreamState?> = _vamData.asStateFlow()

    private val _avgVamData = MutableStateFlow<StreamState?>(null)
    val avgVamData: StateFlow<StreamState?> = _avgVamData.asStateFlow()

    // Radar (threat level, target count, closest range — all come from one multi-field stream)
    private val _radarData = MutableStateFlow<StreamState?>(null)
    val radarData: StateFlow<StreamState?> = _radarData.asStateFlow()

    // General additions
    private val _clockTimeData = MutableStateFlow<StreamState?>(null)
    val clockTimeData: StateFlow<StreamState?> = _clockTimeData.asStateFlow()
    private val _temperatureData = MutableStateFlow<StreamState?>(null)
    val temperatureData: StateFlow<StreamState?> = _temperatureData.asStateFlow()
    private val _batteryPercentData = MutableStateFlow<StreamState?>(null)
    val batteryPercentData: StateFlow<StreamState?> = _batteryPercentData.asStateFlow()
    private val _rideTimeData = MutableStateFlow<StreamState?>(null)
    val rideTimeData: StateFlow<StreamState?> = _rideTimeData.asStateFlow()

    // Heart Rate additions
    private val _percentMaxHrData = MutableStateFlow<StreamState?>(null)
    val percentMaxHrData: StateFlow<StreamState?> = _percentMaxHrData.asStateFlow()
    private val _percentHrrData = MutableStateFlow<StreamState?>(null)
    val percentHrrData: StateFlow<StreamState?> = _percentHrrData.asStateFlow()

    // Power additions (note: smoothed10s and 30s already exist above)
    private val _powerZoneData = MutableStateFlow<StreamState?>(null)
    val powerZoneData: StateFlow<StreamState?> = _powerZoneData.asStateFlow()
    private val _smoothed5sPowerData = MutableStateFlow<StreamState?>(null)
    val smoothed5sPowerData: StateFlow<StreamState?> = _smoothed5sPowerData.asStateFlow()
    private val _normalizedPowerData = MutableStateFlow<StreamState?>(null)
    val normalizedPowerData: StateFlow<StreamState?> = _normalizedPowerData.asStateFlow()
    private val _percentFtpData = MutableStateFlow<StreamState?>(null)
    val percentFtpData: StateFlow<StreamState?> = _percentFtpData.asStateFlow()
    private val _intensityFactorData = MutableStateFlow<StreamState?>(null)
    val intensityFactorData: StateFlow<StreamState?> = _intensityFactorData.asStateFlow()
    private val _trainingStressScoreData = MutableStateFlow<StreamState?>(null)
    val trainingStressScoreData: StateFlow<StreamState?> = _trainingStressScoreData.asStateFlow()
    private val _powerToWeightData = MutableStateFlow<StreamState?>(null)
    val powerToWeightData: StateFlow<StreamState?> = _powerToWeightData.asStateFlow()

    // Energy
    private val _energyOutputData = MutableStateFlow<StreamState?>(null)
    val energyOutputData: StateFlow<StreamState?> = _energyOutputData.asStateFlow()
    private val _caloriesData = MutableStateFlow<StreamState?>(null)
    val caloriesData: StateFlow<StreamState?> = _caloriesData.asStateFlow()
    private val _caloriesPerHourData = MutableStateFlow<StreamState?>(null)
    val caloriesPerHourData: StateFlow<StreamState?> = _caloriesPerHourData.asStateFlow()

    // Speed additions
    private val _smoothed3sSpeedData = MutableStateFlow<StreamState?>(null)
    val smoothed3sSpeedData: StateFlow<StreamState?> = _smoothed3sSpeedData.asStateFlow()

    // Cadence additions
    private val _smoothed3sCadenceData = MutableStateFlow<StreamState?>(null)
    val smoothed3sCadenceData: StateFlow<StreamState?> = _smoothed3sCadenceData.asStateFlow()

    // Elevation
    private val _elevationGradeData = MutableStateFlow<StreamState?>(null)
    val elevationGradeData: StateFlow<StreamState?> = _elevationGradeData.asStateFlow()
    private val _elevationGainData = MutableStateFlow<StreamState?>(null)
    val elevationGainData: StateFlow<StreamState?> = _elevationGainData.asStateFlow()
    private val _elevationLossData = MutableStateFlow<StreamState?>(null)
    val elevationLossData: StateFlow<StreamState?> = _elevationLossData.asStateFlow()
    private val _altitudeData = MutableStateFlow<StreamState?>(null)
    val altitudeData: StateFlow<StreamState?> = _altitudeData.asStateFlow()
    private val _vam30sData = MutableStateFlow<StreamState?>(null)
    val vam30sData: StateFlow<StreamState?> = _vam30sData.asStateFlow()

    // Lap
    private val _lapNumberData = MutableStateFlow<StreamState?>(null)
    val lapNumberData: StateFlow<StreamState?> = _lapNumberData.asStateFlow()
    private val _lapTimeData = MutableStateFlow<StreamState?>(null)
    val lapTimeData: StateFlow<StreamState?> = _lapTimeData.asStateFlow()
    private val _lapDistanceData = MutableStateFlow<StreamState?>(null)
    val lapDistanceData: StateFlow<StreamState?> = _lapDistanceData.asStateFlow()
    private val _lapSpeedData = MutableStateFlow<StreamState?>(null)
    val lapSpeedData: StateFlow<StreamState?> = _lapSpeedData.asStateFlow()
    private val _lapHrData = MutableStateFlow<StreamState?>(null)
    val lapHrData: StateFlow<StreamState?> = _lapHrData.asStateFlow()
    private val _lapPowerData = MutableStateFlow<StreamState?>(null)
    val lapPowerData: StateFlow<StreamState?> = _lapPowerData.asStateFlow()
    private val _lapNpData = MutableStateFlow<StreamState?>(null)
    val lapNpData: StateFlow<StreamState?> = _lapNpData.asStateFlow()
    private val _lapCadenceData = MutableStateFlow<StreamState?>(null)
    val lapCadenceData: StateFlow<StreamState?> = _lapCadenceData.asStateFlow()
    private val _lapAscentData = MutableStateFlow<StreamState?>(null)
    val lapAscentData: StateFlow<StreamState?> = _lapAscentData.asStateFlow()

    // Last Lap
    private val _lastLapTimeData = MutableStateFlow<StreamState?>(null)
    val lastLapTimeData: StateFlow<StreamState?> = _lastLapTimeData.asStateFlow()
    private val _lastLapDistanceData = MutableStateFlow<StreamState?>(null)
    val lastLapDistanceData: StateFlow<StreamState?> = _lastLapDistanceData.asStateFlow()
    private val _lastLapSpeedData = MutableStateFlow<StreamState?>(null)
    val lastLapSpeedData: StateFlow<StreamState?> = _lastLapSpeedData.asStateFlow()
    private val _lastLapHrData = MutableStateFlow<StreamState?>(null)
    val lastLapHrData: StateFlow<StreamState?> = _lastLapHrData.asStateFlow()
    private val _lastLapPowerData = MutableStateFlow<StreamState?>(null)
    val lastLapPowerData: StateFlow<StreamState?> = _lastLapPowerData.asStateFlow()
    private val _lastLapNpData = MutableStateFlow<StreamState?>(null)
    val lastLapNpData: StateFlow<StreamState?> = _lastLapNpData.asStateFlow()

    // Shifting
    private val _shiftingFrontGearData = MutableStateFlow<StreamState?>(null)
    val shiftingFrontGearData: StateFlow<StreamState?> = _shiftingFrontGearData.asStateFlow()
    private val _shiftingRearGearData = MutableStateFlow<StreamState?>(null)
    val shiftingRearGearData: StateFlow<StreamState?> = _shiftingRearGearData.asStateFlow()
    private val _shiftingBatteryData = MutableStateFlow<StreamState?>(null)
    val shiftingBatteryData: StateFlow<StreamState?> = _shiftingBatteryData.asStateFlow()
    private val _shiftingCountData = MutableStateFlow<StreamState?>(null)
    val shiftingCountData: StateFlow<StreamState?> = _shiftingCountData.asStateFlow()

    // Navigation
    private val _distanceToTurnData = MutableStateFlow<StreamState?>(null)
    val distanceToTurnData: StateFlow<StreamState?> = _distanceToTurnData.asStateFlow()
    private val _distanceToDestData = MutableStateFlow<StreamState?>(null)
    val distanceToDestData: StateFlow<StreamState?> = _distanceToDestData.asStateFlow()
    private val _timeOfArrivalData = MutableStateFlow<StreamState?>(null)
    val timeOfArrivalData: StateFlow<StreamState?> = _timeOfArrivalData.asStateFlow()
    private val _timeToDestData = MutableStateFlow<StreamState?>(null)
    val timeToDestData: StateFlow<StreamState?> = _timeToDestData.asStateFlow()
    private val _headingData = MutableStateFlow<StreamState?>(null)
    val headingData: StateFlow<StreamState?> = _headingData.asStateFlow()

    // eBike
    private val _levBatteryData = MutableStateFlow<StreamState?>(null)
    val levBatteryData: StateFlow<StreamState?> = _levBatteryData.asStateFlow()
    private val _levRangeData = MutableStateFlow<StreamState?>(null)
    val levRangeData: StateFlow<StreamState?> = _levRangeData.asStateFlow()
    private val _levAssistModeData = MutableStateFlow<StreamState?>(null)
    val levAssistModeData: StateFlow<StreamState?> = _levAssistModeData.asStateFlow()
    private val _levMotorPowerData = MutableStateFlow<StreamState?>(null)
    val levMotorPowerData: StateFlow<StreamState?> = _levMotorPowerData.asStateFlow()

    // Reconnection management
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5

    /**
     * Connection state enum
     */
    sealed class ConnectionState {
        data object Disconnected : ConnectionState()
        data object Connecting : ConnectionState()
        data object Connected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
        data class Reconnecting(val attempt: Int) : ConnectionState()
    }

    /**
     * Connect to KarooSystem and start consuming data
     */
    fun connect() {
        if (_connectionState.value is ConnectionState.Connected) {
            Log.w(TAG, "Already connected to KarooSystem")
            return
        }

        Log.i(TAG, "Connecting to KarooSystem...")
        _connectionState.value = ConnectionState.Connecting

        try {
            karooSystem.connect { connected ->
                if (connected) {
                    Log.i(TAG, "Successfully connected to KarooSystem")
                    _connectionState.value = ConnectionState.Connected
                    reconnectAttempts = 0
                    registerConsumers()
                } else {
                    Log.w(TAG, "Disconnected from KarooSystem")
                    _connectionState.value = ConnectionState.Disconnected
                    handleDisconnection()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to KarooSystem: ${e.message}", e)
            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
            handleConnectionError()
        }
    }

    /**
     * Disconnect from KarooSystem and clean up all consumers
     */
    fun disconnect() {
        Log.i(TAG, "Disconnecting from KarooSystem...")

        try {
            // Remove all consumers
            consumerIds.forEach { id ->
                karooSystem.removeConsumer(id)
            }
            consumerIds.clear()

            // Disconnect from service
            karooSystem.disconnect()

            // Reset state
            _connectionState.value = ConnectionState.Disconnected
            _rideState.value = RideState.Idle
            clearMetricData()

            Log.i(TAG, "Successfully disconnected from KarooSystem")
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect: ${e.message}", e)
            _connectionState.value = ConnectionState.Error(e.message ?: "Disconnect error")
        }
    }

    /**
     * Check if connected to KarooSystem
     */
    val isConnected: Boolean
        get() = karooSystem.connected && _connectionState.value is ConnectionState.Connected

    /**
     * Register all data consumers
     */
    private fun registerConsumers() {
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

    /**
     * Handle disconnection with automatic reconnection logic
     */
    private fun handleDisconnection() {
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++
            _connectionState.value = ConnectionState.Reconnecting(reconnectAttempts)

            val delayMs = minOf(
                1000L * (1 shl (reconnectAttempts - 1)),
                30000L
            ) // Exponential backoff, max 30s
            Log.i(
                TAG,
                "Attempting reconnection $reconnectAttempts/$maxReconnectAttempts in ${delayMs}ms..."
            )

            // Note: In a real implementation, we'd use a coroutine with delay here
            // For now, this is a placeholder for the reconnection logic
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                connect()
            }, delayMs)
        } else {
            Log.e(TAG, "Max reconnection attempts reached. Manual reconnection required.")
            _connectionState.value =
                ConnectionState.Error("Connection lost. Please reconnect manually.")
        }
    }

    /**
     * Handle connection errors
     */
    private fun handleConnectionError() {
        // Attempt reconnection for errors as well
        handleDisconnection()
    }

    /**
     * Clear all metric data
     */
    private fun clearMetricData() {
        _speedData.value = null; _averageSpeedData.value = null; _maxSpeedData.value = null
        _heartRateData.value = null; _averageHeartRateData.value = null; _maxHeartRateData.value = null
        _cadenceData.value = null; _averageCadenceData.value = null; _maxCadenceData.value = null
        _powerData.value = null; _averagePowerData.value = null; _maxPowerData.value = null
        _distanceData.value = null; _timeData.value = null; _hrZoneData.value = null
        _smoothed3sPowerData.value = null; _smoothed10sPowerData.value = null; _smoothed30sPowerData.value = null
        _vamData.value = null; _avgVamData.value = null; _radarData.value = null
        // General
        _clockTimeData.value = null; _temperatureData.value = null
        _batteryPercentData.value = null; _rideTimeData.value = null
        // HR
        _percentMaxHrData.value = null; _percentHrrData.value = null
        // Power
        _powerZoneData.value = null; _smoothed5sPowerData.value = null
        _normalizedPowerData.value = null; _percentFtpData.value = null
        _intensityFactorData.value = null; _trainingStressScoreData.value = null
        _powerToWeightData.value = null
        // Energy
        _energyOutputData.value = null; _caloriesData.value = null; _caloriesPerHourData.value = null
        // Speed / Cadence
        _smoothed3sSpeedData.value = null; _smoothed3sCadenceData.value = null
        // Elevation
        _elevationGradeData.value = null; _elevationGainData.value = null
        _elevationLossData.value = null; _altitudeData.value = null; _vam30sData.value = null
        // Lap
        _lapNumberData.value = null; _lapTimeData.value = null; _lapDistanceData.value = null
        _lapSpeedData.value = null; _lapHrData.value = null; _lapPowerData.value = null
        _lapNpData.value = null; _lapCadenceData.value = null; _lapAscentData.value = null
        // Last Lap
        _lastLapTimeData.value = null; _lastLapDistanceData.value = null
        _lastLapSpeedData.value = null; _lastLapHrData.value = null
        _lastLapPowerData.value = null; _lastLapNpData.value = null
        // Shifting
        _shiftingFrontGearData.value = null; _shiftingRearGearData.value = null
        _shiftingBatteryData.value = null; _shiftingCountData.value = null
        // Navigation
        _distanceToTurnData.value = null; _distanceToDestData.value = null
        _timeOfArrivalData.value = null; _timeToDestData.value = null; _headingData.value = null
        // eBike
        _levBatteryData.value = null; _levRangeData.value = null
        _levAssistModeData.value = null; _levMotorPowerData.value = null
    }

    /**
     * Registers a standard single-value stream consumer.
     */
    private fun registerStream(type: String, flow: MutableStateFlow<StreamState?>) {
        val id = karooSystem.addConsumer(
            OnStreamState.StartStreaming(type),
            onError = { error -> Log.e(TAG, "Stream error [$type]: $error") }
        ) { event: OnStreamState -> flow.value = event.state }
        consumerIds.add(id)
    }

    /**
     * Get the KarooSystemService instance for direct access
     */
    fun getKarooSystem(): KarooSystemService = karooSystem

    companion object {
        private const val TAG = "KarooDataService"
    }
}

