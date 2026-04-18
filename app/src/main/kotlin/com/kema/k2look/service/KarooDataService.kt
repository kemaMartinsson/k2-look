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

    internal val karooSystem = KarooSystemService(context)

    // Consumer IDs for cleanup
    internal val consumerIds = mutableListOf<String>()

    // Connection state
    internal val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Ride state
    internal val _rideState = MutableStateFlow<RideState>(RideState.Idle)
    val rideState: StateFlow<RideState> = _rideState.asStateFlow()

    // Active ride profile (profile selected by user on launcher)
    internal val _activeRideProfile = MutableStateFlow<RideProfile?>(null)
    val activeRideProfile: StateFlow<RideProfile?> = _activeRideProfile.asStateFlow()

    // Metric streams
    internal val _speedData = MutableStateFlow<StreamState?>(null)
    val speedData: StateFlow<StreamState?> = _speedData.asStateFlow()

    internal val _averageSpeedData = MutableStateFlow<StreamState?>(null)
    val averageSpeedData: StateFlow<StreamState?> = _averageSpeedData.asStateFlow()

    internal val _maxSpeedData = MutableStateFlow<StreamState?>(null)
    val maxSpeedData: StateFlow<StreamState?> = _maxSpeedData.asStateFlow()

    internal val _heartRateData = MutableStateFlow<StreamState?>(null)
    val heartRateData: StateFlow<StreamState?> = _heartRateData.asStateFlow()

    internal val _averageHeartRateData = MutableStateFlow<StreamState?>(null)
    val averageHeartRateData: StateFlow<StreamState?> = _averageHeartRateData.asStateFlow()

    internal val _maxHeartRateData = MutableStateFlow<StreamState?>(null)
    val maxHeartRateData: StateFlow<StreamState?> = _maxHeartRateData.asStateFlow()

    internal val _cadenceData = MutableStateFlow<StreamState?>(null)
    val cadenceData: StateFlow<StreamState?> = _cadenceData.asStateFlow()

    internal val _averageCadenceData = MutableStateFlow<StreamState?>(null)
    val averageCadenceData: StateFlow<StreamState?> = _averageCadenceData.asStateFlow()

    internal val _maxCadenceData = MutableStateFlow<StreamState?>(null)
    val maxCadenceData: StateFlow<StreamState?> = _maxCadenceData.asStateFlow()

    internal val _powerData = MutableStateFlow<StreamState?>(null)
    val powerData: StateFlow<StreamState?> = _powerData.asStateFlow()

    internal val _averagePowerData = MutableStateFlow<StreamState?>(null)
    val averagePowerData: StateFlow<StreamState?> = _averagePowerData.asStateFlow()

    internal val _maxPowerData = MutableStateFlow<StreamState?>(null)
    val maxPowerData: StateFlow<StreamState?> = _maxPowerData.asStateFlow()

    internal val _distanceData = MutableStateFlow<StreamState?>(null)
    val distanceData: StateFlow<StreamState?> = _distanceData.asStateFlow()

    internal val _timeData = MutableStateFlow<StreamState?>(null)
    val timeData: StateFlow<StreamState?> = _timeData.asStateFlow()

    // Advanced metrics
    internal val _hrZoneData = MutableStateFlow<StreamState?>(null)
    val hrZoneData: StateFlow<StreamState?> = _hrZoneData.asStateFlow()

    internal val _smoothed3sPowerData = MutableStateFlow<StreamState?>(null)
    val smoothed3sPowerData: StateFlow<StreamState?> = _smoothed3sPowerData.asStateFlow()

    internal val _smoothed10sPowerData = MutableStateFlow<StreamState?>(null)
    val smoothed10sPowerData: StateFlow<StreamState?> = _smoothed10sPowerData.asStateFlow()

    internal val _smoothed30sPowerData = MutableStateFlow<StreamState?>(null)
    val smoothed30sPowerData: StateFlow<StreamState?> = _smoothed30sPowerData.asStateFlow()

    internal val _vamData = MutableStateFlow<StreamState?>(null)
    val vamData: StateFlow<StreamState?> = _vamData.asStateFlow()

    internal val _avgVamData = MutableStateFlow<StreamState?>(null)
    val avgVamData: StateFlow<StreamState?> = _avgVamData.asStateFlow()

    // Radar (threat level, target count, closest range — all come from one multi-field stream)
    internal val _radarData = MutableStateFlow<StreamState?>(null)
    val radarData: StateFlow<StreamState?> = _radarData.asStateFlow()

    // General additions
    internal val _clockTimeData = MutableStateFlow<StreamState?>(null)
    val clockTimeData: StateFlow<StreamState?> = _clockTimeData.asStateFlow()
    internal val _temperatureData = MutableStateFlow<StreamState?>(null)
    val temperatureData: StateFlow<StreamState?> = _temperatureData.asStateFlow()
    internal val _batteryPercentData = MutableStateFlow<StreamState?>(null)
    val batteryPercentData: StateFlow<StreamState?> = _batteryPercentData.asStateFlow()
    internal val _rideTimeData = MutableStateFlow<StreamState?>(null)
    val rideTimeData: StateFlow<StreamState?> = _rideTimeData.asStateFlow()

    // Heart Rate additions
    internal val _percentMaxHrData = MutableStateFlow<StreamState?>(null)
    val percentMaxHrData: StateFlow<StreamState?> = _percentMaxHrData.asStateFlow()
    internal val _percentHrrData = MutableStateFlow<StreamState?>(null)
    val percentHrrData: StateFlow<StreamState?> = _percentHrrData.asStateFlow()

    // Power additions (note: smoothed10s and 30s already exist above)
    internal val _powerZoneData = MutableStateFlow<StreamState?>(null)
    val powerZoneData: StateFlow<StreamState?> = _powerZoneData.asStateFlow()
    internal val _smoothed5sPowerData = MutableStateFlow<StreamState?>(null)
    val smoothed5sPowerData: StateFlow<StreamState?> = _smoothed5sPowerData.asStateFlow()
    internal val _normalizedPowerData = MutableStateFlow<StreamState?>(null)
    val normalizedPowerData: StateFlow<StreamState?> = _normalizedPowerData.asStateFlow()
    internal val _percentFtpData = MutableStateFlow<StreamState?>(null)
    val percentFtpData: StateFlow<StreamState?> = _percentFtpData.asStateFlow()
    internal val _intensityFactorData = MutableStateFlow<StreamState?>(null)
    val intensityFactorData: StateFlow<StreamState?> = _intensityFactorData.asStateFlow()
    internal val _trainingStressScoreData = MutableStateFlow<StreamState?>(null)
    val trainingStressScoreData: StateFlow<StreamState?> = _trainingStressScoreData.asStateFlow()
    internal val _powerToWeightData = MutableStateFlow<StreamState?>(null)
    val powerToWeightData: StateFlow<StreamState?> = _powerToWeightData.asStateFlow()

    // Energy
    internal val _energyOutputData = MutableStateFlow<StreamState?>(null)
    val energyOutputData: StateFlow<StreamState?> = _energyOutputData.asStateFlow()
    internal val _caloriesData = MutableStateFlow<StreamState?>(null)
    val caloriesData: StateFlow<StreamState?> = _caloriesData.asStateFlow()
    internal val _caloriesPerHourData = MutableStateFlow<StreamState?>(null)
    val caloriesPerHourData: StateFlow<StreamState?> = _caloriesPerHourData.asStateFlow()

    // Speed additions
    internal val _smoothed3sSpeedData = MutableStateFlow<StreamState?>(null)
    val smoothed3sSpeedData: StateFlow<StreamState?> = _smoothed3sSpeedData.asStateFlow()

    // Cadence additions
    internal val _smoothed3sCadenceData = MutableStateFlow<StreamState?>(null)
    val smoothed3sCadenceData: StateFlow<StreamState?> = _smoothed3sCadenceData.asStateFlow()

    // Elevation
    internal val _elevationGradeData = MutableStateFlow<StreamState?>(null)
    val elevationGradeData: StateFlow<StreamState?> = _elevationGradeData.asStateFlow()
    internal val _elevationGainData = MutableStateFlow<StreamState?>(null)
    val elevationGainData: StateFlow<StreamState?> = _elevationGainData.asStateFlow()
    internal val _elevationLossData = MutableStateFlow<StreamState?>(null)
    val elevationLossData: StateFlow<StreamState?> = _elevationLossData.asStateFlow()
    internal val _altitudeData = MutableStateFlow<StreamState?>(null)
    val altitudeData: StateFlow<StreamState?> = _altitudeData.asStateFlow()
    internal val _vam30sData = MutableStateFlow<StreamState?>(null)
    val vam30sData: StateFlow<StreamState?> = _vam30sData.asStateFlow()

    // Lap
    internal val _lapNumberData = MutableStateFlow<StreamState?>(null)
    val lapNumberData: StateFlow<StreamState?> = _lapNumberData.asStateFlow()
    internal val _lapTimeData = MutableStateFlow<StreamState?>(null)
    val lapTimeData: StateFlow<StreamState?> = _lapTimeData.asStateFlow()
    internal val _lapDistanceData = MutableStateFlow<StreamState?>(null)
    val lapDistanceData: StateFlow<StreamState?> = _lapDistanceData.asStateFlow()
    internal val _lapSpeedData = MutableStateFlow<StreamState?>(null)
    val lapSpeedData: StateFlow<StreamState?> = _lapSpeedData.asStateFlow()
    internal val _lapHrData = MutableStateFlow<StreamState?>(null)
    val lapHrData: StateFlow<StreamState?> = _lapHrData.asStateFlow()
    internal val _lapPowerData = MutableStateFlow<StreamState?>(null)
    val lapPowerData: StateFlow<StreamState?> = _lapPowerData.asStateFlow()
    internal val _lapNpData = MutableStateFlow<StreamState?>(null)
    val lapNpData: StateFlow<StreamState?> = _lapNpData.asStateFlow()
    internal val _lapCadenceData = MutableStateFlow<StreamState?>(null)
    val lapCadenceData: StateFlow<StreamState?> = _lapCadenceData.asStateFlow()
    internal val _lapAscentData = MutableStateFlow<StreamState?>(null)
    val lapAscentData: StateFlow<StreamState?> = _lapAscentData.asStateFlow()

    // Last Lap
    internal val _lastLapTimeData = MutableStateFlow<StreamState?>(null)
    val lastLapTimeData: StateFlow<StreamState?> = _lastLapTimeData.asStateFlow()
    internal val _lastLapDistanceData = MutableStateFlow<StreamState?>(null)
    val lastLapDistanceData: StateFlow<StreamState?> = _lastLapDistanceData.asStateFlow()
    internal val _lastLapSpeedData = MutableStateFlow<StreamState?>(null)
    val lastLapSpeedData: StateFlow<StreamState?> = _lastLapSpeedData.asStateFlow()
    internal val _lastLapHrData = MutableStateFlow<StreamState?>(null)
    val lastLapHrData: StateFlow<StreamState?> = _lastLapHrData.asStateFlow()
    internal val _lastLapPowerData = MutableStateFlow<StreamState?>(null)
    val lastLapPowerData: StateFlow<StreamState?> = _lastLapPowerData.asStateFlow()
    internal val _lastLapNpData = MutableStateFlow<StreamState?>(null)
    val lastLapNpData: StateFlow<StreamState?> = _lastLapNpData.asStateFlow()

    // Shifting
    internal val _shiftingFrontGearData = MutableStateFlow<StreamState?>(null)
    val shiftingFrontGearData: StateFlow<StreamState?> = _shiftingFrontGearData.asStateFlow()
    internal val _shiftingRearGearData = MutableStateFlow<StreamState?>(null)
    val shiftingRearGearData: StateFlow<StreamState?> = _shiftingRearGearData.asStateFlow()
    internal val _shiftingBatteryData = MutableStateFlow<StreamState?>(null)
    val shiftingBatteryData: StateFlow<StreamState?> = _shiftingBatteryData.asStateFlow()
    internal val _shiftingCountData = MutableStateFlow<StreamState?>(null)
    val shiftingCountData: StateFlow<StreamState?> = _shiftingCountData.asStateFlow()

    // Navigation
    internal val _distanceToTurnData = MutableStateFlow<StreamState?>(null)
    val distanceToTurnData: StateFlow<StreamState?> = _distanceToTurnData.asStateFlow()
    internal val _distanceToDestData = MutableStateFlow<StreamState?>(null)
    val distanceToDestData: StateFlow<StreamState?> = _distanceToDestData.asStateFlow()
    internal val _timeOfArrivalData = MutableStateFlow<StreamState?>(null)
    val timeOfArrivalData: StateFlow<StreamState?> = _timeOfArrivalData.asStateFlow()
    internal val _timeToDestData = MutableStateFlow<StreamState?>(null)
    val timeToDestData: StateFlow<StreamState?> = _timeToDestData.asStateFlow()
    internal val _headingData = MutableStateFlow<StreamState?>(null)
    val headingData: StateFlow<StreamState?> = _headingData.asStateFlow()

    // eBike
    internal val _levBatteryData = MutableStateFlow<StreamState?>(null)
    val levBatteryData: StateFlow<StreamState?> = _levBatteryData.asStateFlow()
    internal val _levRangeData = MutableStateFlow<StreamState?>(null)
    val levRangeData: StateFlow<StreamState?> = _levRangeData.asStateFlow()
    internal val _levAssistModeData = MutableStateFlow<StreamState?>(null)
    val levAssistModeData: StateFlow<StreamState?> = _levAssistModeData.asStateFlow()
    internal val _levMotorPowerData = MutableStateFlow<StreamState?>(null)
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

    // registerConsumers / registerStream → KarooDataServiceConsumers.kt


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

    /**
     * Get the KarooSystemService instance for direct access
     */
    fun getKarooSystem(): KarooSystemService = karooSystem

    companion object {
        private const val TAG = "KarooDataService"
    }
}

