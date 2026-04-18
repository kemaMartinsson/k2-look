package com.kema.k2look

import android.app.Application
import android.util.Log
import com.kema.k2look.service.KarooActiveLookBridge

/**
 * Application class that owns the single process-scoped bridge instance.
 *
 * This ensures the BLE connection to ActiveLook glasses and the Karoo data connection are kept
 * alive regardless of whether the UI is open or closed. Both K2LookExtension (background service)
 * and MainViewModel (UI) share this same bridge instance — there is exactly one connection at all
 * times.
 */
class K2LookApplication : Application() {

    lateinit var bridge: KarooActiveLookBridge
        private set

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "K2LookApplication starting — initializing bridge...")
        bridge = KarooActiveLookBridge(this)
        bridge.initialize()
        Log.i(TAG, "✓ Bridge initialized at application scope")
    }

    companion object {
        private const val TAG = "K2LookApplication"
    }
}
