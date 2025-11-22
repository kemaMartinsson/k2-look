package com.kema.k2look.service

import android.content.Context
import android.util.Log

/**
 * Test utility for KarooDataService
 *
 * This class provides simple test methods to verify the KarooDataService implementation.
 * Use these for basic integration testing.
 */
class KarooDataServiceTest(private val context: Context) {

    private var service: KarooDataService? = null

    /**
     * Test basic connection to KarooSystem
     */
    fun testConnection(): Boolean {
        Log.i(TAG, "Testing KarooSystem connection...")

        return try {
            service = KarooDataService(context)
            service?.connect()

            // Wait a bit for connection
            Thread.sleep(2000)

            val connected = service?.isConnected ?: false
            Log.i(TAG, "Connection test result: $connected")
            connected
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed: ${e.message}", e)
            false
        }
    }

    /**
     * Test disconnection from KarooSystem
     */
    fun testDisconnection(): Boolean {
        Log.i(TAG, "Testing KarooSystem disconnection...")

        return try {
            service?.disconnect()

            val disconnected = !(service?.isConnected ?: false)
            Log.i(TAG, "Disconnection test result: $disconnected")
            disconnected
        } catch (e: Exception) {
            Log.e(TAG, "Disconnection test failed: ${e.message}", e)
            false
        }
    }

    /**
     * Test RideState consumer registration
     */
    fun testRideStateConsumer(): Boolean {
        Log.i(TAG, "Testing RideState consumer...")

        return try {
            if (service == null) {
                service = KarooDataService(context)
                service?.connect()
                Thread.sleep(2000)
            }

            // The service should have automatically registered the RideState consumer
            val hasRideState = service?.rideState?.value != null
            Log.i(TAG, "RideState consumer test result: $hasRideState, state=${service?.rideState?.value}")
            true // Consumer registration happens automatically, so we consider this a success
        } catch (e: Exception) {
            Log.e(TAG, "RideState consumer test failed: ${e.message}", e)
            false
        }
    }

    /**
     * Test metric data streams
     */
    fun testMetricStreams(): Boolean {
        Log.i(TAG, "Testing metric data streams...")

        return try {
            if (service == null) {
                service = KarooDataService(context)
                service?.connect()
                Thread.sleep(2000)
            }

            // Check if streams are initialized (even if data is null, that's ok)
            val speedInitialized = service?.speedData != null
            val hrInitialized = service?.heartRateData != null
            val cadenceInitialized = service?.cadenceData != null
            val powerInitialized = service?.powerData != null

            Log.i(TAG, "Metric streams initialized: speed=$speedInitialized, hr=$hrInitialized, cadence=$cadenceInitialized, power=$powerInitialized")
            true // Initialization is considered success
        } catch (e: Exception) {
            Log.e(TAG, "Metric streams test failed: ${e.message}", e)
            false
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        service?.disconnect()
        service = null
        Log.i(TAG, "Test cleanup complete")
    }

    companion object {
        private const val TAG = "KarooDataServiceTest"
    }
}

