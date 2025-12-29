package com.kema.k2look.integration

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kema.k2look.data.DataFieldRegistry
import com.kema.k2look.data.DefaultVisualizations
import com.kema.k2look.model.VisualizationType
import com.kema.k2look.viewmodel.LayoutBuilderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for metric selector save functionality
 * Tests the complete flow from UI action to state update
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class MetricSelectorIntegrationTest {

    private lateinit var application: Application
    private lateinit var viewModel: LayoutBuilderViewModel

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()

        // Clear any existing profiles
        application.getSharedPreferences("k2look_profiles", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        viewModel = LayoutBuilderViewModel(application)
    }

    @After
    fun tearDown() {
        // Clean up
        application.getSharedPreferences("k2look_profiles", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    @Test
    fun testChangingMetricFromHrToPowerWithTextVisualization() = runTest {
        // Create a test profile with HR field
        viewModel.createProfile("Test")
        var state = viewModel.uiState.first()
        val profile = state.profiles.find { it.name == "Test" }!!

        viewModel.selectProfile(profile.id)

        // Add HR field
        val hrField = DataFieldRegistry.getById(4)!! // Heart Rate
        viewModel.addFieldToScreen(profile.screens.first().id, "3D_FULL_H", hrField)

        // Verify HR was added
        state = viewModel.uiState.first()
        val screen = state.activeProfile!!.screens.first()
        assertEquals(1, screen.dataFields.size)
        assertEquals("Heart Rate", screen.dataFields[0].dataField.name)
        assertEquals(VisualizationType.TEXT, screen.dataFields[0].visualizationType)

        // Now update to Power (simulating user selecting Power in dialog)
        val powerField = DataFieldRegistry.getById(7)!! // Power
        val updatedField = screen.dataFields[0].copy(
            dataField = powerField
        )

        viewModel.updateField(screen.id, updatedField)

        // Verify Power was saved
        state = viewModel.uiState.first()
        val updatedScreen = state.activeProfile!!.screens.first()
        assertEquals(1, updatedScreen.dataFields.size)
        assertEquals("Power", updatedScreen.dataFields[0].dataField.name)
        assertEquals(7, updatedScreen.dataFields[0].dataField.id)
        assertEquals(VisualizationType.TEXT, updatedScreen.dataFields[0].visualizationType)
    }

    @Test
    fun testChangingMetricFromHrToPowerWithGaugeVisualization() = runTest {
        // Create a test profile with HR field
        viewModel.createProfile("Test Gauge")
        var state = viewModel.uiState.first()
        val profile = state.profiles.find { it.name == "Test Gauge" }!!

        viewModel.selectProfile(profile.id)

        // Add HR field with GAUGE
        val hrField = DataFieldRegistry.getById(4)!! // Heart Rate
        viewModel.addFieldToScreen(profile.screens.first().id, "3D_FULL_H", hrField)

        // Update to GAUGE visualization
        state = viewModel.uiState.first()
        var screen = state.activeProfile!!.screens.first()
        val hrGauge = DefaultVisualizations.createHeartRateGauge()
        val fieldWithGauge = screen.dataFields[0].copy(
            visualizationType = VisualizationType.GAUGE,
            gauge = hrGauge
        )
        viewModel.updateField(screen.id, fieldWithGauge)

        // Verify HR GAUGE was saved
        state = viewModel.uiState.first()
        screen = state.activeProfile!!.screens.first()
        assertEquals("Heart Rate", screen.dataFields[0].dataField.name)
        assertEquals(VisualizationType.GAUGE, screen.dataFields[0].visualizationType)
        assertNotNull(screen.dataFields[0].gauge)
        assertEquals("Heart Rate", screen.dataFields[0].gauge!!.dataField.name)

        // Now change to Power while keeping GAUGE
        val powerField = DataFieldRegistry.getById(7)!! // Power
        val powerGauge = DefaultVisualizations.createPowerGauge()
            .copy(dataField = powerField)

        val updatedField = screen.dataFields[0].copy(
            dataField = powerField,
            gauge = powerGauge
        )

        viewModel.updateField(screen.id, updatedField)

        // Verify Power GAUGE was saved
        state = viewModel.uiState.first()
        val updatedScreen = state.activeProfile!!.screens.first()
        assertEquals(1, updatedScreen.dataFields.size)
        assertEquals("Power", updatedScreen.dataFields[0].dataField.name)
        assertEquals(7, updatedScreen.dataFields[0].dataField.id)
        assertEquals(VisualizationType.GAUGE, updatedScreen.dataFields[0].visualizationType)
        assertNotNull(updatedScreen.dataFields[0].gauge)
        assertEquals("Power", updatedScreen.dataFields[0].gauge!!.dataField.name)
        assertEquals(7, updatedScreen.dataFields[0].gauge!!.dataField.id)
    }

    @Test
    fun testChangingMetricFromSpeedToCadenceWithBarVisualization() = runTest {
        // Create a test profile with Speed field
        viewModel.createProfile("Test Bar")
        var state = viewModel.uiState.first()
        val profile = state.profiles.find { it.name == "Test Bar" }!!

        viewModel.selectProfile(profile.id)

        // Add Speed field with BAR
        val speedField = DataFieldRegistry.getById(12)!! // Speed
        viewModel.addFieldToScreen(profile.screens.first().id, "3D_FULL_H", speedField)

        // Update to BAR visualization
        state = viewModel.uiState.first()
        var screen = state.activeProfile!!.screens.first()
        val speedBar = DefaultVisualizations.createSpeedBar()
        val fieldWithBar = screen.dataFields[0].copy(
            visualizationType = VisualizationType.BAR,
            progressBar = speedBar
        )
        viewModel.updateField(screen.id, fieldWithBar)

        // Verify Speed BAR was saved
        state = viewModel.uiState.first()
        screen = state.activeProfile!!.screens.first()
        assertEquals("Speed", screen.dataFields[0].dataField.name)
        assertEquals(VisualizationType.BAR, screen.dataFields[0].visualizationType)
        assertNotNull(screen.dataFields[0].progressBar)

        // Now change to Cadence with generic BAR
        val cadenceField = DataFieldRegistry.getById(18)!! // Cadence
        val cadenceBar = com.kema.k2look.model.ProgressBar(
            id = 1,
            x = 30,
            y = 110,
            width = 244,
            height = 20,
            orientation = com.kema.k2look.model.Orientation.HORIZONTAL,
            minValue = 0f,
            maxValue = 100f,
            showBorder = true,
            dataField = cadenceField
        )

        val updatedField = screen.dataFields[0].copy(
            dataField = cadenceField,
            progressBar = cadenceBar
        )

        viewModel.updateField(screen.id, updatedField)

        // Verify Cadence BAR was saved
        state = viewModel.uiState.first()
        val updatedScreen = state.activeProfile!!.screens.first()
        assertEquals(1, updatedScreen.dataFields.size)
        assertEquals("Cadence", updatedScreen.dataFields[0].dataField.name)
        assertEquals(18, updatedScreen.dataFields[0].dataField.id)
        assertEquals(VisualizationType.BAR, updatedScreen.dataFields[0].visualizationType)
        assertNotNull(updatedScreen.dataFields[0].progressBar)
        assertEquals("Cadence", updatedScreen.dataFields[0].progressBar!!.dataField.name)
    }

    @Test
    fun testSwitchingVisualizationFromTextToGaugePreservesNewMetric() = runTest {
        // Create profile with Power as TEXT
        viewModel.createProfile("Test Switch")
        var state = viewModel.uiState.first()
        val profile = state.profiles.find { it.name == "Test Switch" }!!

        viewModel.selectProfile(profile.id)

        val powerField = DataFieldRegistry.getById(7)!! // Power
        viewModel.addFieldToScreen(profile.screens.first().id, "3D_FULL_H", powerField)

        // Verify TEXT was added
        state = viewModel.uiState.first()
        val screen = state.activeProfile!!.screens.first()
        assertEquals("Power", screen.dataFields[0].dataField.name)
        assertEquals(VisualizationType.TEXT, screen.dataFields[0].visualizationType)

        // Change to GAUGE
        val powerGauge = DefaultVisualizations.createPowerGauge()
        val updatedField = screen.dataFields[0].copy(
            visualizationType = VisualizationType.GAUGE,
            gauge = powerGauge
        )

        viewModel.updateField(screen.id, updatedField)

        // Verify GAUGE was saved and metric preserved
        state = viewModel.uiState.first()
        val updatedScreen = state.activeProfile!!.screens.first()
        assertEquals("Power", updatedScreen.dataFields[0].dataField.name)
        assertEquals(VisualizationType.GAUGE, updatedScreen.dataFields[0].visualizationType)
        assertNotNull(updatedScreen.dataFields[0].gauge)
        assertEquals("Power", updatedScreen.dataFields[0].gauge!!.dataField.name)
    }

    @Test
    fun testFieldUpdatePreservesZoneId() = runTest {
        // Create profile with field
        viewModel.createProfile("Test Zone")
        var state = viewModel.uiState.first()
        val profile = state.profiles.find { it.name == "Test Zone" }!!

        viewModel.selectProfile(profile.id)

        // Add field in specific zone
        val hrField = DataFieldRegistry.getById(4)!! // Heart Rate
        viewModel.addFieldToScreen(profile.screens.first().id, "3D_FULL_M", hrField)

        // Verify zone
        state = viewModel.uiState.first()
        var screen = state.activeProfile!!.screens.first()
        assertEquals("3D_FULL_M", screen.dataFields[0].zoneId)

        // Update metric
        val powerField = DataFieldRegistry.getById(7)!! // Power
        val updatedField = screen.dataFields[0].copy(dataField = powerField)

        viewModel.updateField(screen.id, updatedField)

        // Verify zone preserved
        state = viewModel.uiState.first()
        screen = state.activeProfile!!.screens.first()
        assertEquals("3D_FULL_M", screen.dataFields[0].zoneId)
        assertEquals("Power", screen.dataFields[0].dataField.name)
    }
}

