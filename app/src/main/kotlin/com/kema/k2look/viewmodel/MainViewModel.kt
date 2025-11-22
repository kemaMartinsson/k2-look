package com.kema.k2look.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.activelook.activelooksdk.DiscoveredGlasses
import com.kema.k2look.service.KarooActiveLookBridge
import com.kema.k2look.service.KarooDataService
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Karoo data, ActiveLook connection, and UI state
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val bridge = KarooActiveLookBridge(application)
    private val karooDataService = bridge.getKarooDataService()
    private val activeLookService = bridge.getActiveLookService()

    // UI State
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
        val connectionState: KarooDataService.ConnectionState = KarooDataService.ConnectionState.Disconnected,
        val bridgeState: KarooActiveLookBridge.BridgeState = KarooActiveLookBridge.BridgeState.Idle,
        val discoveredGlasses: List<DiscoveredGlasses> = emptyList(),
        val isScanning: Boolean = false,
        val rideState: RideState = RideState.Idle,
        val speed: String = "--",
        val avgSpeed: String = "--",
        val maxSpeed: String = "--",
        val heartRate: String = "--",
        val avgHeartRate: String = "--",
        val maxHeartRate: String = "--",
        val cadence: String = "--",
        val avgCadence: String = "--",
        val maxCadence: String = "--",
        val power: String = "--",
        val avgPower: String = "--",
        val maxPower: String = "--",
        val distance: String = "--",
        val time: String = "--",
        // Advanced metrics
        val hrZone: String = "--",
        val power3s: String = "--",
        val power10s: String = "--",
        val power30s: String = "--",
        val vam: String = "--",
        val avgVam: String = "--",
    )

    init {
        Log.i(TAG, "MainViewModel initialized")
        bridge.initialize()
        observeKarooData()
        observeActiveLookData()
        observeBridgeState()
    }

    /**
     * Connect to Karoo System
     */
    fun connectKaroo() {
        Log.i(TAG, "User requested connection to Karoo")
        bridge.connectKaroo()
    }

    /**
     * Disconnect from Karoo System
     */
    fun disconnectKaroo() {
        Log.i(TAG, "User requested disconnect from Karoo")
        bridge.disconnectKaroo()
    }

    /**
     * Start scanning for ActiveLook glasses
     */
    fun startActiveLookScan() {
        Log.i(TAG, "User requested ActiveLook scan")
        bridge.startActiveLookScan()
    }

    /**
     * Stop scanning for ActiveLook glasses
     */
    fun stopActiveLookScan() {
        Log.i(TAG, "User requested stop ActiveLook scan")
        bridge.stopActiveLookScan()
    }

    /**
     * Connect to ActiveLook glasses
     */
    fun connectActiveLook(glasses: DiscoveredGlasses) {
        Log.i(TAG, "User requested connection to ActiveLook glasses: ${glasses.name}")
        bridge.connectActiveLook(glasses)
    }

    /**
     * Disconnect from ActiveLook glasses
     */
    fun disconnectActiveLook() {
        Log.i(TAG, "User requested disconnect from ActiveLook")
        bridge.disconnectActiveLook()
    }

    /**
     * Observe bridge state
     */
    private fun observeBridgeState() {
        viewModelScope.launch {
            bridge.bridgeState.collect { state ->
                Log.d(TAG, "Bridge state changed: $state")
                _uiState.value = _uiState.value.copy(bridgeState = state)
            }
        }
    }

    /**
     * Observe ActiveLook data
     */
    private fun observeActiveLookData() {
        // Observe discovered glasses
        viewModelScope.launch {
            activeLookService.discoveredGlasses.collect { glasses ->
                Log.d(TAG, "Discovered glasses updated: ${glasses.size} devices")
                _uiState.value = _uiState.value.copy(discoveredGlasses = glasses)
            }
        }

        // Observe scanning state
        viewModelScope.launch {
            activeLookService.isScanning.collect { scanning ->
                Log.d(TAG, "Scanning state: $scanning")
                _uiState.value = _uiState.value.copy(isScanning = scanning)
            }
        }
    }

    /**
     * Observe data from KarooDataService and update UI state
     */
    private fun observeKarooData() {
        // Observe connection state
        viewModelScope.launch {
            karooDataService.connectionState.collect { state ->
                Log.d(TAG, "Connection state changed: $state")
                _uiState.value = _uiState.value.copy(connectionState = state)
            }
        }

        // Observe ride state
        viewModelScope.launch {
            karooDataService.rideState.collect { state ->
                Log.d(TAG, "Ride state changed: $state")
                _uiState.value = _uiState.value.copy(rideState = state)
            }
        }

        // Observe speed data
        viewModelScope.launch {
            karooDataService.speedData.collect { streamState ->
                val speedStr = formatStreamData(streamState, "km/h")
                _uiState.value = _uiState.value.copy(speed = speedStr)
            }
        }

        // Observe average speed data
        viewModelScope.launch {
            karooDataService.averageSpeedData.collect { streamState ->
                val avgSpeedStr = formatStreamData(streamState, "km/h")
                _uiState.value = _uiState.value.copy(avgSpeed = avgSpeedStr)
            }
        }

        // Observe max speed data
        viewModelScope.launch {
            karooDataService.maxSpeedData.collect { streamState ->
                val maxSpeedStr = formatStreamData(streamState, "km/h")
                _uiState.value = _uiState.value.copy(maxSpeed = maxSpeedStr)
            }
        }

        // Observe heart rate data
        viewModelScope.launch {
            karooDataService.heartRateData.collect { streamState ->
                val hrStr = formatStreamData(streamState, "bpm")
                _uiState.value = _uiState.value.copy(heartRate = hrStr)
            }
        }

        // Observe average heart rate data
        viewModelScope.launch {
            karooDataService.averageHeartRateData.collect { streamState ->
                val avgHrStr = formatStreamData(streamState, "bpm")
                _uiState.value = _uiState.value.copy(avgHeartRate = avgHrStr)
            }
        }

        // Observe max heart rate data
        viewModelScope.launch {
            karooDataService.maxHeartRateData.collect { streamState ->
                val maxHrStr = formatStreamData(streamState, "bpm")
                _uiState.value = _uiState.value.copy(maxHeartRate = maxHrStr)
            }
        }

        // Observe cadence data
        viewModelScope.launch {
            karooDataService.cadenceData.collect { streamState ->
                val cadenceStr = formatStreamData(streamState, "rpm")
                _uiState.value = _uiState.value.copy(cadence = cadenceStr)
            }
        }

        // Observe average cadence data
        viewModelScope.launch {
            karooDataService.averageCadenceData.collect { streamState ->
                val avgCadenceStr = formatStreamData(streamState, "rpm")
                _uiState.value = _uiState.value.copy(avgCadence = avgCadenceStr)
            }
        }

        // Observe max cadence data
        viewModelScope.launch {
            karooDataService.maxCadenceData.collect { streamState ->
                val maxCadenceStr = formatStreamData(streamState, "rpm")
                _uiState.value = _uiState.value.copy(maxCadence = maxCadenceStr)
            }
        }

        // Observe power data
        viewModelScope.launch {
            karooDataService.powerData.collect { streamState ->
                val powerStr = formatStreamData(streamState, "w")
                _uiState.value = _uiState.value.copy(power = powerStr)
            }
        }

        // Observe average power data
        viewModelScope.launch {
            karooDataService.averagePowerData.collect { streamState ->
                val avgPowerStr = formatStreamData(streamState, "w")
                _uiState.value = _uiState.value.copy(avgPower = avgPowerStr)
            }
        }

        // Observe max power data
        viewModelScope.launch {
            karooDataService.maxPowerData.collect { streamState ->
                val maxPowerStr = formatStreamData(streamState, "w")
                _uiState.value = _uiState.value.copy(maxPower = maxPowerStr)
            }
        }

        // Observe distance data
        viewModelScope.launch {
            karooDataService.distanceData.collect { streamState ->
                val distanceStr = formatStreamData(streamState, "km")
                _uiState.value = _uiState.value.copy(distance = distanceStr)
            }
        }

        // Observe time data
        viewModelScope.launch {
            karooDataService.timeData.collect { streamState ->
                val timeStr = formatTimeData(streamState)
                _uiState.value = _uiState.value.copy(time = timeStr)
            }
        }

        // Observe HR zone data
        viewModelScope.launch {
            karooDataService.hrZoneData.collect { streamState ->
                val zoneStr = when (streamState) {
                    is StreamState.Streaming -> {
                        val zoneValue = streamState.dataPoint.singleValue?.toInt()
                        if (zoneValue != null && zoneValue > 0) "Z$zoneValue" else "--"
                    }

                    else -> "--"
                }
                _uiState.value = _uiState.value.copy(hrZone = zoneStr)
            }
        }

        // Observe 3s power data
        viewModelScope.launch {
            karooDataService.smoothed3sPowerData.collect { streamState ->
                val power3sStr = formatStreamData(streamState, "w")
                _uiState.value = _uiState.value.copy(power3s = power3sStr)
            }
        }

        // Observe 10s power data
        viewModelScope.launch {
            karooDataService.smoothed10sPowerData.collect { streamState ->
                val power10sStr = formatStreamData(streamState, "w")
                _uiState.value = _uiState.value.copy(power10s = power10sStr)
            }
        }

        // Observe 30s power data
        viewModelScope.launch {
            karooDataService.smoothed30sPowerData.collect { streamState ->
                val power30sStr = formatStreamData(streamState, "w")
                _uiState.value = _uiState.value.copy(power30s = power30sStr)
            }
        }

        // Observe VAM data
        viewModelScope.launch {
            karooDataService.vamData.collect { streamState ->
                val vamStr = formatStreamData(streamState, "m/h")
                _uiState.value = _uiState.value.copy(vam = vamStr)
            }
        }

        // Observe average VAM data
        viewModelScope.launch {
            karooDataService.avgVamData.collect { streamState ->
                val avgVamStr = formatStreamData(streamState, "m/h")
                _uiState.value = _uiState.value.copy(avgVam = avgVamStr)
            }
        }
    }

    /**
     * Format stream data for display
     */
    private fun formatStreamData(streamState: StreamState?, unit: String): String {
        return when (streamState) {
            is StreamState.Streaming -> {
                val value = streamState.dataPoint.singleValue
                if (value != null) {
                    "${formatValue(value)} $unit"
                } else {
                    "-- $unit"
                }
            }

            is StreamState.Searching -> "Searching..."
            is StreamState.Idle -> "-- $unit"
            is StreamState.NotAvailable -> "N/A"
            null -> "-- $unit"
        }
    }

    /**
     * Format time data (convert ms to HH:MM:SS)
     */
    private fun formatTimeData(streamState: StreamState?): String {
        return when (streamState) {
            is StreamState.Streaming -> {
                val ms = streamState.dataPoint.singleValue?.toLong()
                if (ms != null) {
                    val seconds = (ms / 1000) % 60
                    val minutes = (ms / (1000 * 60)) % 60
                    val hours = (ms / (1000 * 60 * 60))
                    String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    "--:--:--"
                }
            }

            is StreamState.Searching -> "--:--:--"
            is StreamState.Idle -> "--:--:--"
            is StreamState.NotAvailable -> "N/A"
            null -> "--:--:--"
        }
    }

    /**
     * Format numeric value for display
     */
    private fun formatValue(value: Double): String {
        return when {
            value >= 100 -> "%.0f".format(value)
            value >= 10 -> "%.1f".format(value)
            else -> "%.2f".format(value)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.i(TAG, "MainViewModel cleared, cleaning up bridge")
        bridge.cleanup()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}

