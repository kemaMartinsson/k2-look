package com.kema.k2look.extension

import android.util.Log
import com.kema.k2look.K2LookApplication
import io.hammerhead.karooext.extension.KarooExtension

/**
 * K2Look Karoo Extension Service
 *
 * Started and managed by the Karoo OS. Uses the process-scoped bridge from [K2LookApplication] —
 * the bridge is already initialized before this service starts and must stay alive even when this
 * service component is destroyed and restarted by the OS.
 */
class K2LookExtension : KarooExtension("k2look", "1.0") {

    override fun onCreate() {
        super.onCreate()
        // Bridge is owned by K2LookApplication — already initialized.
        // Log confirmation so we can verify the service started.
        val bridge = (application as K2LookApplication).bridge
        Log.i(TAG, "K2Look Extension Service started (bridge state: ${bridge.bridgeState.value})")
    }

    override fun onDestroy() {
        // Do NOT call bridge.cleanup() here — the bridge is process-scoped and
        // must keep the glasses connection alive after the service restarts.
        Log.i(TAG, "K2Look Extension Service stopped (bridge kept alive in Application)")
        super.onDestroy()
    }

    companion object {
        private const val TAG = "K2LookExtension"
    }
}
