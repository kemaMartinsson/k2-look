package io.hammerhead.karooexttemplate.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooexttemplate.service.KarooDataService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Karoo data and UI state
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val karooDataService = KarooDataService(application)

    // UI State
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
        val connectionState: KarooDataService.ConnectionState = KarooDataService.ConnectionState.Disconnected,
        val rideState: RideState = RideState.Idle,
        val speed: String = "--",
        val heartRate: String = "--",
        val cadence: String = "--",
        val power: String = "--",
        val distance: String = "--",
        val time: String = "--",
    )

    init {
        Log.i(TAG, "MainViewModel initialized")
        observeKarooData()
    }

    /**
     * Connect to Karoo System
     */
    fun connect() {
        Log.i(TAG, "User requested connection to Karoo")
        karooDataService.connect()
    }

    /**
     * Disconnect from Karoo System
     */
    fun disconnect() {
        Log.i(TAG, "User requested disconnect from Karoo")
        karooDataService.disconnect()
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

        // Observe heart rate data
        viewModelScope.launch {
            karooDataService.heartRateData.collect { streamState ->
                val hrStr = formatStreamData(streamState, "bpm")
                _uiState.value = _uiState.value.copy(heartRate = hrStr)
            }
        }

        // Observe cadence data
        viewModelScope.launch {
            karooDataService.cadenceData.collect { streamState ->
                val cadenceStr = formatStreamData(streamState, "rpm")
                _uiState.value = _uiState.value.copy(cadence = cadenceStr)
            }
        }

        // Observe power data
        viewModelScope.launch {
            karooDataService.powerData.collect { streamState ->
                val powerStr = formatStreamData(streamState, "w")
                _uiState.value = _uiState.value.copy(power = powerStr)
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
        Log.i(TAG, "MainViewModel cleared, disconnecting from Karoo")
        karooDataService.disconnect()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}

