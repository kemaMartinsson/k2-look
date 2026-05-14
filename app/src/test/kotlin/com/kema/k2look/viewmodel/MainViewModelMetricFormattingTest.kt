package com.kema.k2look.viewmodel

import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.StreamState
import org.junit.Assert.assertEquals
import org.junit.Test

class MainViewModelMetricFormattingTest {

    @Test
    fun `ui speed formatter converts raw karoo meters per second to kmh`() {
        val rawKarooSpeed = StreamState.Streaming(DataPoint("speed", mapOf("value" to 11.11)))

        assertEquals("40 km/h", formatUiSpeedDataKmh(rawKarooSpeed))
    }

    @Test
    fun `ui distance formatter converts raw karoo meters to km`() {
        val rawKarooDistance =
                StreamState.Streaming(DataPoint("distance", mapOf("value" to 12345.0)))

        assertEquals("12.3 km", formatUiDistanceDataKm(rawKarooDistance))
    }
}
