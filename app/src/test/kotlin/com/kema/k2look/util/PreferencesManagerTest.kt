package com.kema.k2look.util

import android.content.Context
import android.content.SharedPreferences
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PreferencesManager
 * Tests all preference storage and retrieval functionality
 */
class PreferencesManagerTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var preferencesManager: PreferencesManager

    @Before
    fun setup() {
        // Mock Android dependencies
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        // Setup mock behavior
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.apply() } just Runs

        preferencesManager = PreferencesManager(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `isAutoConnectKarooEnabled returns default true`() {
        every { sharedPreferences.getBoolean("auto_connect_karoo", true) } returns true

        val result = preferencesManager.isAutoConnectKarooEnabled()

        assertTrue(result)
    }

    @Test
    fun `setAutoConnectKaroo stores value correctly`() {
        val slot = slot<Boolean>()

        preferencesManager.setAutoConnectKaroo(false)

        verify { editor.putBoolean("auto_connect_karoo", capture(slot)) }
        assertEquals(false, slot.captured)
    }

    @Test
    fun `isAutoConnectActiveLookEnabled returns default false`() {
        every { sharedPreferences.getBoolean("auto_connect_activelook", false) } returns false

        val result = preferencesManager.isAutoConnectActiveLookEnabled()

        assertFalse(result)
    }

    @Test
    fun `setAutoConnectActiveLook stores value correctly`() {
        val slot = slot<Boolean>()

        preferencesManager.setAutoConnectActiveLook(true)

        verify { editor.putBoolean("auto_connect_activelook", capture(slot)) }
        assertEquals(true, slot.captured)
    }

    @Test
    fun `getLastConnectedGlassesAddress returns null when not set`() {
        every { sharedPreferences.getString("last_glasses_address", null) } returns null

        val result = preferencesManager.getLastConnectedGlassesAddress()

        assertNull(result)
    }

    @Test
    fun `getLastConnectedGlassesAddress returns stored address`() {
        val testAddress = "00:11:22:33:44:55"
        every { sharedPreferences.getString("last_glasses_address", null) } returns testAddress

        val result = preferencesManager.getLastConnectedGlassesAddress()

        assertEquals(testAddress, result)
    }

    @Test
    fun `setLastConnectedGlasses stores address correctly`() {
        val testAddress = "00:11:22:33:44:55"
        val slot = slot<String>()

        preferencesManager.setLastConnectedGlasses(testAddress)

        verify { editor.putString("last_glasses_address", capture(slot)) }
        assertEquals(testAddress, slot.captured)
    }

    @Test
    fun `getReconnectTimeoutMinutes returns default 10`() {
        every { sharedPreferences.getInt("reconnect_timeout_minutes", 10) } returns 10

        val result = preferencesManager.getReconnectTimeoutMinutes()

        assertEquals(10, result)
    }

    @Test
    fun `setReconnectTimeoutMinutes stores value correctly`() {
        val slot = slot<Int>()

        preferencesManager.setReconnectTimeoutMinutes(15)

        verify { editor.putInt("reconnect_timeout_minutes", capture(slot)) }
        assertEquals(15, slot.captured)
    }

    @Test
    fun `getStartupTimeoutMinutes returns default 10`() {
        every { sharedPreferences.getInt("startup_timeout_minutes", 10) } returns 10

        val result = preferencesManager.getStartupTimeoutMinutes()

        assertEquals(10, result)
    }

    @Test
    fun `setStartupTimeoutMinutes stores value correctly`() {
        val slot = slot<Int>()

        preferencesManager.setStartupTimeoutMinutes(20)

        verify { editor.putInt("startup_timeout_minutes", capture(slot)) }
        assertEquals(20, slot.captured)
    }

    @Test
    fun `isReconnectDuringRidesEnabled returns default true`() {
        every { sharedPreferences.getBoolean("reconnect_during_rides", true) } returns true

        val result = preferencesManager.isReconnectDuringRidesEnabled()

        assertTrue(result)
    }

    @Test
    fun `setReconnectDuringRides stores value correctly`() {
        val slot = slot<Boolean>()

        preferencesManager.setReconnectDuringRides(false)

        verify { editor.putBoolean("reconnect_during_rides", capture(slot)) }
        assertEquals(false, slot.captured)
    }

    @Test
    fun `isDisconnectWhenIdleEnabled returns default false`() {
        every { sharedPreferences.getBoolean("disconnect_when_idle", false) } returns false

        val result = preferencesManager.isDisconnectWhenIdleEnabled()

        assertFalse(result)
    }

    @Test
    fun `setDisconnectWhenIdle stores value correctly`() {
        val slot = slot<Boolean>()

        preferencesManager.setDisconnectWhenIdle(true)

        verify { editor.putBoolean("disconnect_when_idle", capture(slot)) }
        assertEquals(true, slot.captured)
    }

    @Test
    fun `timeout values respect valid range`() {
        // Test that reasonable timeout values work
        preferencesManager.setReconnectTimeoutMinutes(1)
        preferencesManager.setReconnectTimeoutMinutes(60)
        preferencesManager.setStartupTimeoutMinutes(1)
        preferencesManager.setStartupTimeoutMinutes(60)

        // Verify all calls were made
        verify(exactly = 4) { editor.putInt(any(), any()) }
    }
}

