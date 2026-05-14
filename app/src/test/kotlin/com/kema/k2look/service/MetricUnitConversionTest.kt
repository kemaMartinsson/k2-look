package com.kema.k2look.service

import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.StreamState
import org.junit.Assert.assertEquals
import org.junit.Test

class MetricUnitConversionTest {

    @Test
    fun `bridge speed formatters convert raw karoo meters per second to kmh`() {
        val rawKarooSpeed = StreamState.Streaming(DataPoint("speed", mapOf("value" to 11.11)))

        assertEquals("40 km/h", formatSpeedDataKmh(rawKarooSpeed))
    }

    @Test
    fun `bridge distance formatter converts raw karoo meters to km`() {
        val rawKarooDistance =
                StreamState.Streaming(DataPoint("distance", mapOf("value" to 12345.0)))

        assertEquals("12.3 km", formatDistanceDataKm(rawKarooDistance))
    }

    @Test
    fun `bridge distance formatter drops decimal at and above 100 km`() {
        val rawKarooDistance =
                StreamState.Streaming(DataPoint("distance", mapOf("value" to 100000.0)))

        assertEquals("100 km", formatDistanceDataKm(rawKarooDistance))
    }

    @Test
    fun `bridge distance formatter rounds to one decimal for lap variants`() {
        val rawLapDistance =
                StreamState.Streaming(DataPoint("lapDistance", mapOf("value" to 9876.0)))

        assertEquals("9.9 km", formatDistanceDataKm(rawLapDistance))
    }
}
