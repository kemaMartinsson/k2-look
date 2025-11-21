package io.hammerhead.karooexttemplate.service

import android.content.Context
import android.util.Log
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.OnStreamState
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

    // Metric streams
    private val _speedData = MutableStateFlow<StreamState?>(null)
    val speedData: StateFlow<StreamState?> = _speedData.asStateFlow()

    private val _heartRateData = MutableStateFlow<StreamState?>(null)
    val heartRateData: StateFlow<StreamState?> = _heartRateData.asStateFlow()

    private val _cadenceData = MutableStateFlow<StreamState?>(null)
    val cadenceData: StateFlow<StreamState?> = _cadenceData.asStateFlow()

    private val _powerData = MutableStateFlow<StreamState?>(null)
    val powerData: StateFlow<StreamState?> = _powerData.asStateFlow()

    private val _distanceData = MutableStateFlow<StreamState?>(null)
    val distanceData: StateFlow<StreamState?> = _distanceData.asStateFlow()

    private val _timeData = MutableStateFlow<StreamState?>(null)
    val timeData: StateFlow<StreamState?> = _timeData.asStateFlow()

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

            val delayMs = minOf(1000L * (1 shl (reconnectAttempts - 1)), 30000L) // Exponential backoff, max 30s
            Log.i(TAG, "Attempting reconnection $reconnectAttempts/$maxReconnectAttempts in ${delayMs}ms...")

            // Note: In a real implementation, we'd use a coroutine with delay here
            // For now, this is a placeholder for the reconnection logic
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                connect()
            }, delayMs)
        } else {
            Log.e(TAG, "Max reconnection attempts reached. Manual reconnection required.")
            _connectionState.value = ConnectionState.Error("Connection lost. Please reconnect manually.")
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
        _speedData.value = null
        _heartRateData.value = null
        _cadenceData.value = null
        _powerData.value = null
        _distanceData.value = null
        _timeData.value = null
    }

    companion object {
        private const val TAG = "KarooDataService"
    }
}

