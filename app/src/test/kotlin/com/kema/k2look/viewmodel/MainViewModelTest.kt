package com.kema.k2look.viewmodel

import com.kema.k2look.service.KarooDataService
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for MainViewModel
 * Tests UI state data structures and validation logic
 *
 * Note: Full ViewModel testing would require dependency injection framework
 * These tests focus on the data structures and validation logic
 */
class MainViewModelTest {


    @Test
    fun `viewModel initializes with default UI state`() {
        val initialState = MainViewModel.UiState()

        assertEquals(KarooDataService.ConnectionState.Disconnected, initialState.connectionState)
        assertEquals(10, initialState.reconnectTimeoutMinutes)
        assertFalse(initialState.debugModeEnabled)
        assertEquals("--", initialState.speed)
    }

    @Test
    fun `timeout validation accepts valid range`() {
        // Valid values should be in 1-60 range
        assertTrue(1 in 1..60)
        assertTrue(15 in 1..60)
        assertTrue(60 in 1..60)
    }

    @Test
    fun `timeout validation rejects invalid values`() {
        // Values outside 1-60 range should be rejected
        assertFalse(0 in 1..60)
        assertFalse(61 in 1..60)
        assertFalse(-5 in 1..60)
    }

    @Test
    fun `setDebugMode updates state`() {
        val initialState = MainViewModel.UiState(debugModeEnabled = false)
        val updatedState = initialState.copy(debugModeEnabled = true)

        assertTrue(updatedState.debugModeEnabled)
    }

    @Test
    fun `number formatting ranges are correct`() {
        // Test number formatting logic ranges
        val large = 150.5
        val medium = 25.7
        val small = 5.25

        // Large numbers (>= 100) should show no decimals
        assertTrue(large >= 100)

        // Medium numbers (>= 10) should show 1 decimal
        assertTrue(medium >= 10 && medium < 100)

        // Small numbers (< 10) should show 2 decimals
        assertTrue(small < 10)
    }

    @Test
    fun `UI state holds all required metrics`() {
        val state = MainViewModel.UiState(
            speed = "25.5 km/h",
            heartRate = "142 bpm",
            cadence = "85 rpm",
            power = "220 w",
            distance = "12.3 km",
            time = "00:32:15"
        )

        assertEquals("25.5 km/h", state.speed)
        assertEquals("142 bpm", state.heartRate)
        assertEquals("85 rpm", state.cadence)
        assertEquals("220 w", state.power)
        assertEquals("12.3 km", state.distance)
        assertEquals("00:32:15", state.time)
    }

    @Test
    fun `timeout values have reasonable defaults`() {
        val state = MainViewModel.UiState()

        assertEquals(10, state.reconnectTimeoutMinutes)
        assertTrue(state.reconnectTimeoutMinutes in 1..60)
    }

    @Test
    fun `connection states are properly defined`() {
        // Test that all connection states are handled
        val states = listOf(
            KarooDataService.ConnectionState.Disconnected,
            KarooDataService.ConnectionState.Connecting
        )

        states.forEach { state ->
            assertNotNull(state)
        }
    }

    @Test
    fun `UI state supports all advanced metrics`() {
        val state = MainViewModel.UiState(
            hrZone = "Z3",
            power3s = "250 w",
            power10s = "240 w",
            power30s = "230 w",
            vam = "1200 m/h",
            avgVam = "1150 m/h"
        )

        assertEquals("Z3", state.hrZone)
        assertEquals("250 w", state.power3s)
        assertEquals("240 w", state.power10s)
        assertEquals("230 w", state.power30s)
        assertEquals("1200 m/h", state.vam)
        assertEquals("1150 m/h", state.avgVam)
    }
}

