package com.kema.k2look.service

import android.content.Context
import android.graphics.Point
import android.util.Log
import com.activelook.activelooksdk.DiscoveredGlasses
import com.activelook.activelooksdk.Glasses
import com.activelook.activelooksdk.Sdk
import com.activelook.activelooksdk.types.Rotation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service responsible for managing ActiveLook glasses connection and communication.
 *
 * This service handles:
 * - SDK initialization
 * - BLE scanning for glasses
 * - Connection management (connect/disconnect)
 * - Connection state monitoring
 * - Text display on glasses
 * - Error handling and recovery
 */
class ActiveLookService(private val context: Context) {

    private var sdk: Sdk? = null
    private var connectedGlasses: Glasses? = null

    // Connection state
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Discovered glasses
    private val _discoveredGlasses = MutableStateFlow<List<DiscoveredGlasses>>(emptyList())
    val discoveredGlasses: StateFlow<List<DiscoveredGlasses>> = _discoveredGlasses.asStateFlow()

    // Scanning state
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    /**
     * Connection state enum
     */
    sealed class ConnectionState {
        data object Disconnected : ConnectionState()
        data object Scanning : ConnectionState()
        data object Connecting : ConnectionState()
        data class Connected(val glasses: Glasses) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    /**
     * Initialize the ActiveLook SDK
     */
    fun initializeSdk() {
        if (sdk != null) {
            Log.w(TAG, "SDK already initialized")
            return
        }

        Log.i(TAG, "Initializing ActiveLook SDK...")

        try {
            sdk = Sdk.init(
                context.applicationContext,
                { update ->
                    Log.i(TAG, "Firmware update started")
                },
                { pair ->
                    Log.i(TAG, "Firmware update available")
                    // For now, don't auto-update during rides
                    // User can manually update through settings later
                },
                { update ->
                    Log.d(TAG, "Firmware update progress")
                },
                { update ->
                    Log.i(TAG, "Firmware update successful")
                },
                { update ->
                    Log.e(TAG, "Firmware update error")
                }
            )

            Log.i(TAG, "✓ ActiveLook SDK initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ActiveLook SDK: ${e.message}", e)
            _connectionState.value = ConnectionState.Error("Failed to initialize SDK: ${e.message}")
        }
    }

    /**
     * Start scanning for ActiveLook glasses
     */
    fun startScanning() {
        val sdkInstance = sdk
        if (sdkInstance == null) {
            Log.e(TAG, "SDK not initialized. Call initializeSdk() first.")
            _connectionState.value = ConnectionState.Error("SDK not initialized")
            return
        }

        if (_isScanning.value) {
            Log.w(TAG, "Already scanning")
            return
        }

        Log.i(TAG, "=== Starting BLE scan for ActiveLook glasses ===")
        Log.d(TAG, "SDK instance: $sdkInstance")
        _connectionState.value = ConnectionState.Scanning
        _isScanning.value = true
        _discoveredGlasses.value = emptyList()

        try {
            sdkInstance.startScan { discoveredGlasses ->
                Log.i(TAG, "=== DISCOVERED DEVICE ===")
                Log.i(TAG, "  Name: ${discoveredGlasses.name}")
                Log.i(TAG, "  Address: ${discoveredGlasses.address}")
                Log.i(TAG, "  Manufacturer: ${discoveredGlasses.manufacturer}")
                Log.i(TAG, "  toString(): $discoveredGlasses")
                Log.i(TAG, "========================")

                // Add to discovered list if not already present
                val currentList = _discoveredGlasses.value.toMutableList()
                if (currentList.none { it.address == discoveredGlasses.address }) {
                    currentList.add(discoveredGlasses)
                    _discoveredGlasses.value = currentList
                    Log.i(TAG, "✓ Added to discovered list: ${discoveredGlasses.name}")
                    Log.d(TAG, "Total discovered devices: ${currentList.size}")
                } else {
                    Log.d(TAG, "Device already in list, skipping: ${discoveredGlasses.name}")
                }
            }
            Log.i(TAG, "✓ Scan started successfully, waiting for devices...")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error starting scan: ${e.message}", e)
            Log.e(TAG, "Exception type: ${e.javaClass.name}")
            Log.e(TAG, "Stack trace:", e)
            _connectionState.value = ConnectionState.Error("Scan failed: ${e.message}")
            _isScanning.value = false
        }
    }

    /**
     * Stop scanning for glasses
     */
    fun stopScanning() {
        val sdkInstance = sdk
        if (sdkInstance == null) {
            Log.w(TAG, "SDK not initialized")
            return
        }

        if (!_isScanning.value) {
            return
        }

        Log.i(TAG, "Stopping BLE scan...")

        try {
            sdkInstance.stopScan()
            _isScanning.value = false

            if (_connectionState.value is ConnectionState.Scanning) {
                _connectionState.value = ConnectionState.Disconnected
            }

            Log.i(TAG, "✓ Scan stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan: ${e.message}", e)
        }
    }

    /**
     * Connect to discovered glasses
     */
    fun connect(glasses: DiscoveredGlasses) {
        Log.i(TAG, "=== INITIATING CONNECTION ===")
        Log.i(TAG, "  Target Name: ${glasses.name}")
        Log.i(TAG, "  Target Address: ${glasses.address}")
        Log.i(TAG, "  Target Manufacturer: ${glasses.manufacturer}")
        Log.i(TAG, "  Current State: ${_connectionState.value}")
        Log.i(TAG, "  Is Scanning: ${_isScanning.value}")

        // Stop scanning if active
        if (_isScanning.value) {
            Log.d(TAG, "Stopping scan before connection...")
            stopScanning()
        }

        Log.d(TAG, "Setting state to Connecting...")
        _connectionState.value = ConnectionState.Connecting

        try {
            Log.d(TAG, "Calling glasses.connect()...")
            glasses.connect(
                { connectedGlasses ->
                    Log.i(TAG, "=== CONNECTION SUCCESS ===")
                    Log.i(TAG, "  Connected Name: ${connectedGlasses.name}")
                    Log.i(TAG, "  Connected Address: ${connectedGlasses.address}")
                    Log.i(TAG, "  Manufacturer: ${connectedGlasses.manufacturer}")
                    Log.i(TAG, "=========================")

                    this.connectedGlasses = connectedGlasses
                    _connectionState.value = ConnectionState.Connected(connectedGlasses)

                    Log.i(TAG, "✓ Connection established successfully")
                },
                { failedGlasses ->
                    Log.e(TAG, "=== CONNECTION FAILED ===")
                    Log.e(TAG, "  Failed Name: ${failedGlasses.name}")
                    Log.e(TAG, "  Failed Address: ${failedGlasses.address}")
                    Log.e(TAG, "  Manufacturer: ${failedGlasses.manufacturer}")
                    Log.e(TAG, "  Connection State: Failed")
                    Log.e(TAG, "=========================")

                    _connectionState.value = ConnectionState.Error("Connection failed")
                    this.connectedGlasses = null

                    Log.e(TAG, "✗ Connection failed - check glasses power, BLE, and proximity")
                },
                { disconnectedGlasses ->
                    Log.w(TAG, "=== DISCONNECTION EVENT ===")
                    Log.w(TAG, "  Disconnected Name: ${disconnectedGlasses.name}")
                    Log.w(TAG, "  Disconnected Address: ${disconnectedGlasses.address}")
                    Log.w(TAG, "  Previous State: ${_connectionState.value}")
                    Log.w(TAG, "===========================")

                    _connectionState.value = ConnectionState.Disconnected
                    this.connectedGlasses = null

                    Log.w(TAG, "Connection lost - glasses disconnected")
                }
            )
            Log.d(TAG, "glasses.connect() call completed, waiting for callbacks...")
        } catch (e: Exception) {
            Log.e(TAG, "=== CONNECTION EXCEPTION ===")
            Log.e(TAG, "  Exception Type: ${e.javaClass.name}")
            Log.e(TAG, "  Message: ${e.message}")
            Log.e(TAG, "  Stack trace:", e)
            Log.e(TAG, "============================")

            _connectionState.value = ConnectionState.Error("Connection error: ${e.message}")
            this.connectedGlasses = null

            Log.e(TAG, "✗ Exception during connection attempt")
        }
    }

    /**
     * Disconnect from glasses
     */
    fun disconnect() {
        val glasses = connectedGlasses
        if (glasses == null) {
            Log.w(TAG, "No glasses connected")
            return
        }

        Log.i(TAG, "Disconnecting from glasses: ${glasses.name}...")

        try {
            glasses.disconnect()
            connectedGlasses = null
            _connectionState.value = ConnectionState.Disconnected
            Log.i(TAG, "✓ Disconnected successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect: ${e.message}", e)
            _connectionState.value = ConnectionState.Error("Disconnect error: ${e.message}")
        }
    }

    /**
     * Display text on glasses
     */
    fun displayText(text: String, x: Int = 0, y: Int = 0) {
        val glasses = connectedGlasses
        if (glasses == null) {
            Log.w(TAG, "Cannot display text: No glasses connected")
            return
        }

        try {
            // Clear display first
            glasses.clear()

            // Display text at position with default rotation, font size, and color
            val position = Point(x, y)
            val rotation = Rotation.TOP_LR
            val fontSize: Byte = 3
            val color: Byte = 15  // White/Full brightness (0-15)

            glasses.txt(position, rotation, fontSize, color, text)

            Log.d(TAG, "Displayed text: '$text' at ($x, $y)")
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying text: ${e.message}", e)
        }
    }

    /**
     * Clear the glasses display
     */
    fun clearDisplay() {
        val glasses = connectedGlasses
        if (glasses == null) {
            Log.w(TAG, "Cannot clear display: No glasses connected")
            return
        }

        try {
            glasses.clear()
            Log.d(TAG, "Display cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing display: ${e.message}", e)
        }
    }

    /**
     * Check if connected to glasses
     */
    val isConnected: Boolean
        get() = connectedGlasses != null && _connectionState.value is ConnectionState.Connected

    /**
     * Get the currently connected glasses
     */
    fun getConnectedGlasses(): Glasses? = connectedGlasses

    /**
     * Clean up resources
     */
    fun cleanup() {
        Log.i(TAG, "Cleaning up ActiveLook service...")

        // Stop scanning if active
        if (_isScanning.value) {
            stopScanning()
        }

        // Disconnect if connected
        if (connectedGlasses != null) {
            disconnect()
        }

        // Clear state
        _discoveredGlasses.value = emptyList()

        Log.i(TAG, "✓ Cleanup complete")
    }

    companion object {
        private const val TAG = "ActiveLookService"
    }
}

