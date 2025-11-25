package com.kema.k2look.extension

import android.util.Log
import com.kema.k2look.service.KarooActiveLookBridge
import io.hammerhead.karooext.extension.KarooExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

/**
 * K2Look Karoo Extension Service
 *
 * This service runs in the background and handles:
 * - Auto-starting when Karoo boots
 * - Managing connections to Karoo system and ActiveLook glasses
 * - Streaming ride data to glasses during active rides
 * - Automatic reconnection with configurable timeout
 *
 * The service runs independently of the MainActivity UI.
 */
class K2LookExtension : KarooExtension("k2look", "1.0") {

    private lateinit var bridge: KarooActiveLookBridge
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var isInitialized = false

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "K2Look Extension Service created - initializing...")

        try {
            // Initialize the bridge which handles all connection logic
            bridge = KarooActiveLookBridge(this)
            bridge.initialize()
            isInitialized = true

            Log.i(TAG, "✓ K2Look Extension Service started successfully")
            Log.i(TAG, "  - Service will auto-connect based on preferences")
            Log.i(TAG, "  - Will monitor ride state for continuous reconnection")
            Log.i(TAG, "  - Open K2Look app to view status or adjust settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize K2Look Extension: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "K2Look Extension Service stopping...")

        try {
            if (isInitialized) {
                // Disconnect services
                bridge.disconnectKaroo()
                bridge.disconnectActiveLook()
            }

            // Cancel coroutine scope
            serviceScope.cancel()

            Log.i(TAG, "✓ K2Look Extension Service stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error during service shutdown: ${e.message}", e)
        }

        super.onDestroy()
    }

    /**
     * Get the bridge instance for UI to observe
     * This allows MainActivity to bind to the service and show status
     */
    fun getBridge(): KarooActiveLookBridge? {
        return if (isInitialized) bridge else null
    }

    companion object {
        private const val TAG = "K2LookExtension"
    }
}
