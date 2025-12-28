package com.kema.k2look.integration

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kema.k2look.data.DataFieldRegistry
import com.kema.k2look.data.ProfileRepository
import com.kema.k2look.model.FontSize
import com.kema.k2look.model.IconSize
import com.kema.k2look.model.LayoutDataField
import com.kema.k2look.model.Position
import com.kema.k2look.viewmodel.LayoutBuilderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for DataField Builder
 * Tests end-to-end workflows combining multiple components
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class DataFieldBuilderIntegrationTest {

    private lateinit var application: Application
    private lateinit var viewModel: LayoutBuilderViewModel
    private lateinit var repository: ProfileRepository

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()

        // Clear any existing profiles
        application.getSharedPreferences("k2look_profiles", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        viewModel = LayoutBuilderViewModel(application)
        repository = ProfileRepository(application)
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
    fun testCompleteProfileCreationWorkflow() = runTest {
        // 1. Create a new profile
        viewModel.createProfile("Road Bike")

        var state = viewModel.uiState.first()
        val profile = state.profiles.find { it.name == "Road Bike" }!!

        // 2. Add fields to the profile
        val speedField = LayoutDataField(
            dataField = DataFieldRegistry.getById(12)!!, // Speed
            position = Position.TOP,
            fontSize = FontSize.LARGE,
            showLabel = true,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.SMALL
        )

        val hrField = LayoutDataField(
            dataField = DataFieldRegistry.getById(4)!!, // Heart Rate
            position = Position.MIDDLE,
            fontSize = FontSize.MEDIUM,
            showLabel = true,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.LARGE
        )

        val powerField = LayoutDataField(
            dataField = DataFieldRegistry.getById(7)!!, // Power
            position = Position.BOTTOM,
            fontSize = FontSize.MEDIUM,
            showLabel = false,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.SMALL
        )

        val screen = profile.screens[0]
        val updatedScreen = screen.copy(
            dataFields = listOf(speedField, hrField, powerField)
        )
        val updatedProfile = profile.copy(
            screens = listOf(updatedScreen)
        )

        // 3. Save the updated profile
        viewModel.updateProfile(updatedProfile)

        // 4. Select the profile
        viewModel.selectProfile(updatedProfile.id)

        state = viewModel.uiState.first()

        // Verify the complete workflow
        assertEquals(updatedProfile.id, state.activeProfile?.id)
        assertEquals(3, state.activeProfile?.screens?.get(0)?.dataFields?.size)

        // 5. Verify persistence
        val newViewModel = LayoutBuilderViewModel(application)
        val newState = newViewModel.uiState.first()

        val persistedProfile = newState.profiles.find { it.name == "Road Bike" }
        assertNotNull(persistedProfile)
        assertEquals(3, persistedProfile?.screens?.get(0)?.dataFields?.size)
    }

    @Test
    fun testProfileDuplicationAndModification() = runTest {
        // 1. Create original profile
        viewModel.createProfile("Original")

        var state = viewModel.uiState.first()
        val original = state.profiles.find { it.name == "Original" }!!

        // 2. Add a field to original
        val speedField = LayoutDataField(
            dataField = DataFieldRegistry.getById(12)!!, // Speed
            position = Position.TOP,
            fontSize = FontSize.LARGE,
            showLabel = true,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.SMALL
        )

        val screen = original.screens[0]
        val updatedScreen = screen.copy(dataFields = listOf(speedField))
        val updatedOriginal = original.copy(screens = listOf(updatedScreen))

        viewModel.updateProfile(updatedOriginal)

        // 3. Duplicate the profile
        viewModel.duplicateProfile(updatedOriginal.id, "Duplicate")

        state = viewModel.uiState.first()

        val duplicate = state.profiles.find { it.name == "Duplicate" }
        assertNotNull(duplicate)

        // 4. Verify duplicate has same fields
        assertEquals(
            updatedOriginal.screens[0].dataFields.size,
            duplicate?.screens?.get(0)?.dataFields?.size
        )

        // 5. Modify duplicate (add another field)
        val hrField = LayoutDataField(
            dataField = DataFieldRegistry.getById(4)!!, // Heart Rate
            position = Position.MIDDLE,
            fontSize = FontSize.MEDIUM,
            showLabel = true,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.LARGE
        )

        val duplicateScreen = duplicate!!.screens[0]
        val modifiedScreen = duplicateScreen.copy(
            dataFields = duplicateScreen.dataFields + hrField
        )
        val modifiedDuplicate = duplicate.copy(screens = listOf(modifiedScreen))

        viewModel.updateProfile(modifiedDuplicate)

        state = viewModel.uiState.first()

        // 6. Verify original unchanged, duplicate modified
        val finalOriginal = state.profiles.find { it.name == "Original" }
        val finalDuplicate = state.profiles.find { it.name == "Duplicate" }

        assertEquals(1, finalOriginal?.screens?.get(0)?.dataFields?.size)
        assertEquals(2, finalDuplicate?.screens?.get(0)?.dataFields?.size)
    }

    @Test
    fun testMultipleProfilesWithDifferentConfigurations() = runTest {
        // Create 3 profiles with different configs

        // Profile 1: Road Bike (Speed, Power, HR)
        viewModel.createProfile("Road Bike")
        var state = viewModel.uiState.first()
        val roadBike = state.profiles.find { it.name == "Road Bike" }!!

        val roadFields = listOf(
            LayoutDataField(
                dataField = DataFieldRegistry.getById(12)!!, // Speed
                position = Position.TOP,
                fontSize = FontSize.LARGE,
                showLabel = true,
                showUnit = true,
                showIcon = true,
                iconSize = IconSize.SMALL
            ),
            LayoutDataField(
                dataField = DataFieldRegistry.getById(7)!!, // Power
                position = Position.MIDDLE,
                fontSize = FontSize.LARGE,
                showLabel = true,
                showUnit = true,
                showIcon = true,
                iconSize = IconSize.SMALL
            ),
            LayoutDataField(
                dataField = DataFieldRegistry.getById(4)!!, // Heart Rate
                position = Position.BOTTOM,
                fontSize = FontSize.MEDIUM,
                showLabel = true,
                showUnit = true,
                showIcon = true,
                iconSize = IconSize.LARGE
            )
        )

        var screen = roadBike.screens[0]
        var updatedProfile = roadBike.copy(screens = listOf(screen.copy(dataFields = roadFields)))
        viewModel.updateProfile(updatedProfile)

        // Profile 2: Climbing (VAM, HR Zone, Power)
        viewModel.createProfile("Climbing")
        state = viewModel.uiState.first()
        val climbing = state.profiles.find { it.name == "Climbing" }!!

        val climbingFields = listOf(
            LayoutDataField(
                dataField = DataFieldRegistry.getById(24)!!, // VAM
                position = Position.TOP,
                fontSize = FontSize.LARGE,
                showLabel = true,
                showUnit = true,
                showIcon = true,
                iconSize = IconSize.LARGE
            ),
            LayoutDataField(
                dataField = DataFieldRegistry.getById(47)!!, // HR Zone
                position = Position.MIDDLE,
                fontSize = FontSize.LARGE,
                showLabel = false,
                showUnit = false,
                showIcon = false,
                iconSize = IconSize.SMALL
            ),
            LayoutDataField(
                dataField = DataFieldRegistry.getById(7)!!, // Power
                position = Position.BOTTOM,
                fontSize = FontSize.MEDIUM,
                showLabel = true,
                showUnit = true,
                showIcon = true,
                iconSize = IconSize.SMALL
            )
        )

        screen = climbing.screens[0]
        updatedProfile = climbing.copy(screens = listOf(screen.copy(dataFields = climbingFields)))
        viewModel.updateProfile(updatedProfile)

        // Profile 3: Endurance (Time, Distance, Avg HR)
        viewModel.createProfile("Endurance")
        state = viewModel.uiState.first()
        val endurance = state.profiles.find { it.name == "Endurance" }!!

        val enduranceFields = listOf(
            LayoutDataField(
                dataField = DataFieldRegistry.getById(1)!!, // Time
                position = Position.TOP,
                fontSize = FontSize.MEDIUM,
                showLabel = true,
                showUnit = false,
                showIcon = true,
                iconSize = IconSize.SMALL
            ),
            LayoutDataField(
                dataField = DataFieldRegistry.getById(2)!!, // Distance
                position = Position.MIDDLE,
                fontSize = FontSize.LARGE,
                showLabel = true,
                showUnit = true,
                showIcon = true,
                iconSize = IconSize.SMALL
            ),
            LayoutDataField(
                dataField = DataFieldRegistry.getById(6)!!, // Avg HR
                position = Position.BOTTOM,
                fontSize = FontSize.MEDIUM,
                showLabel = true,
                showUnit = true,
                showIcon = true,
                iconSize = IconSize.LARGE
            )
        )

        screen = endurance.screens[0]
        updatedProfile = endurance.copy(screens = listOf(screen.copy(dataFields = enduranceFields)))
        viewModel.updateProfile(updatedProfile)

        // Verify all profiles exist with correct configurations
        state = viewModel.uiState.first()

        assertEquals(4, state.profiles.size) // Default + 3 custom

        val finalRoadBike = state.profiles.find { it.name == "Road Bike" }
        val finalClimbing = state.profiles.find { it.name == "Climbing" }
        val finalEndurance = state.profiles.find { it.name == "Endurance" }

        assertNotNull(finalRoadBike)
        assertNotNull(finalClimbing)
        assertNotNull(finalEndurance)

        assertEquals(3, finalRoadBike?.screens?.get(0)?.dataFields?.size)
        assertEquals(3, finalClimbing?.screens?.get(0)?.dataFields?.size)
        assertEquals(3, finalEndurance?.screens?.get(0)?.dataFields?.size)

        // Verify different configurations
        assertEquals(FontSize.LARGE, finalRoadBike?.screens?.get(0)?.dataFields?.get(0)?.fontSize)
        assertEquals(IconSize.LARGE, finalClimbing?.screens?.get(0)?.dataFields?.get(0)?.iconSize)
        assertFalse(finalEndurance?.screens?.get(0)?.dataFields?.get(0)?.showUnit ?: true)
    }

    @Test
    fun testProfileSwitchingWorkflow() = runTest {
        // Create two profiles
        viewModel.createProfile("Profile A")
        viewModel.createProfile("Profile B")

        var state = viewModel.uiState.first()
        val profileA = state.profiles.find { it.name == "Profile A" }!!
        val profileB = state.profiles.find { it.name == "Profile B" }!!

        // Select Profile A
        viewModel.selectProfile(profileA.id)
        state = viewModel.uiState.first()
        assertEquals(profileA.id, state.activeProfile?.id)

        // Switch to Profile B
        viewModel.selectProfile(profileB.id)
        state = viewModel.uiState.first()
        assertEquals(profileB.id, state.activeProfile?.id)

        // Switch back to Profile A
        viewModel.selectProfile(profileA.id)
        state = viewModel.uiState.first()
        assertEquals(profileA.id, state.activeProfile?.id)

        // Verify persistence of active profile
        val newViewModel = LayoutBuilderViewModel(application)
        val newState = newViewModel.uiState.first()

        // Last selected profile should still be available
        val persistedA = newState.profiles.find { it.name == "Profile A" }
        val persistedB = newState.profiles.find { it.name == "Profile B" }
        assertNotNull(persistedA)
        assertNotNull(persistedB)
    }

    @Test
    fun testDeleteActiveProfileFlow() = runTest {
        // Create and select a custom profile
        viewModel.createProfile("Custom")

        var state = viewModel.uiState.first()
        val custom = state.profiles.find { it.name == "Custom" }!!

        viewModel.selectProfile(custom.id)
        state = viewModel.uiState.first()
        assertEquals(custom.id, state.activeProfile?.id)

        // Delete the active profile
        viewModel.deleteProfile(custom.id)

        state = viewModel.uiState.first()

        // Should automatically switch to default
        assertTrue(state.activeProfile?.isDefault ?: false)

        // Custom profile should be gone
        assertNull(state.profiles.find { it.name == "Custom" })
    }

    @Test
    fun testEndToEndProfileLifecycle() = runTest {
        // 1. Start with default profile
        var state = viewModel.uiState.first()
        assertEquals(1, state.profiles.size)
        assertTrue(state.activeProfile?.isDefault ?: false)

        // 2. Create a profile
        viewModel.createProfile("Test Profile")
        state = viewModel.uiState.first()
        assertEquals(2, state.profiles.size)

        // 3. Configure it
        val profile = state.profiles.find { it.name == "Test Profile" }!!
        val field = LayoutDataField(
            dataField = DataFieldRegistry.getById(12)!!, // Speed
            position = Position.TOP,
            fontSize = FontSize.LARGE,
            showLabel = true,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.SMALL
        )

        val screen = profile.screens[0]
        val updatedProfile = profile.copy(
            screens = listOf(screen.copy(dataFields = listOf(field)))
        )
        viewModel.updateProfile(updatedProfile)

        // 4. Select it
        viewModel.selectProfile(updatedProfile.id)
        state = viewModel.uiState.first()
        assertEquals(updatedProfile.id, state.activeProfile?.id)

        // 5. Duplicate it
        viewModel.duplicateProfile(updatedProfile.id, "Test Profile Copy")
        state = viewModel.uiState.first()
        assertEquals(3, state.profiles.size)

        // 6. Delete original
        viewModel.deleteProfile(updatedProfile.id)
        state = viewModel.uiState.first()
        assertEquals(2, state.profiles.size)

        // 7. Verify copy still exists
        val copy = state.profiles.find { it.name == "Test Profile Copy" }
        assertNotNull(copy)
        assertEquals(1, copy?.screens?.get(0)?.dataFields?.size)
    }
}

