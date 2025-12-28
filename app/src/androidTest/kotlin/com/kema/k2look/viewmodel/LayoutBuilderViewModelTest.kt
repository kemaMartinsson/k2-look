package com.kema.k2look.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kema.k2look.data.DataFieldRegistry
import com.kema.k2look.data.DefaultProfiles
import com.kema.k2look.data.ProfileRepository
import com.kema.k2look.model.FontSize
import com.kema.k2look.model.IconSize
import com.kema.k2look.model.LayoutDataField
import com.kema.k2look.model.Position
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for LayoutBuilderViewModel - Builder tab functionality
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class LayoutBuilderViewModelTest {

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
    fun testInitialState() = runTest {
        val state = viewModel.uiState.first()

        // Should have at least the default profile
        assertTrue(state.profiles.isNotEmpty())
        assertEquals(1, state.profiles.size)

        // Active profile should be the default
        assertNotNull(state.activeProfile)
        assertTrue(state.activeProfile?.isDefault == true)

        // No error initially
        assertNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun testCreateProfile() = runTest {
        // Create a new profile
        viewModel.createProfile("Test Profile")

        // Wait for state update
        val state = viewModel.uiState.first()

        // Should have default + new profile
        assertEquals(2, state.profiles.size)

        // Find the created profile
        val createdProfile = state.profiles.find { it.name == "Test Profile" }
        assertNotNull(createdProfile)
        assertFalse(createdProfile?.isDefault == true)
        assertFalse(createdProfile?.isReadOnly == true)
    }

    @Test
    fun testCreateMultipleProfiles() = runTest {
        // Create multiple profiles
        viewModel.createProfile("Profile 1")
        viewModel.createProfile("Profile 2")
        viewModel.createProfile("Profile 3")

        val state = viewModel.uiState.first()

        // Should have default + 3 profiles
        assertEquals(4, state.profiles.size)
        assertTrue(state.profiles.any { it.name == "Profile 1" })
        assertTrue(state.profiles.any { it.name == "Profile 2" })
        assertTrue(state.profiles.any { it.name == "Profile 3" })
    }

    @Test
    fun testSelectProfile() = runTest {
        // Create a profile
        viewModel.createProfile("Test Profile")

        var state = viewModel.uiState.first()
        val testProfile = state.profiles.find { it.name == "Test Profile" }!!

        // Select the profile
        viewModel.selectProfile(testProfile.id)

        state = viewModel.uiState.first()

        // Active profile should be the selected one
        assertEquals(testProfile.id, state.activeProfile?.id)
        assertEquals("Test Profile", state.activeProfile?.name)
    }

    @Test
    fun testDuplicateProfile() = runTest {
        // Create a profile
        viewModel.createProfile("Original Profile")

        var state = viewModel.uiState.first()
        val originalProfile = state.profiles.find { it.name == "Original Profile" }!!

        // Duplicate it
        viewModel.duplicateProfile(originalProfile.id, "Duplicated Profile")

        state = viewModel.uiState.first()

        // Should have default + original + duplicate
        assertEquals(3, state.profiles.size)

        val duplicate = state.profiles.find { it.name == "Duplicated Profile" }
        assertNotNull(duplicate)
        assertNotEquals(originalProfile.id, duplicate?.id)
        assertEquals(originalProfile.screens.size, duplicate?.screens?.size)
    }

    @Test
    fun testDeleteProfile() = runTest {
        // Create profiles
        viewModel.createProfile("Profile 1")
        viewModel.createProfile("Profile 2")

        var state = viewModel.uiState.first()
        val profile1 = state.profiles.find { it.name == "Profile 1" }!!

        // Delete Profile 1
        viewModel.deleteProfile(profile1.id)

        state = viewModel.uiState.first()

        // Should have default + Profile 2 only
        assertEquals(2, state.profiles.size)
        assertNull(state.profiles.find { it.name == "Profile 1" })
        assertNotNull(state.profiles.find { it.name == "Profile 2" })
    }

    @Test
    fun testCannotDeleteDefaultProfile() = runTest {
        val state = viewModel.uiState.first()
        val defaultProfile = state.profiles.find { it.isDefault }!!

        // Try to delete default profile
        viewModel.deleteProfile(defaultProfile.id)

        val newState = viewModel.uiState.first()

        // Should still have the default profile
        assertTrue(newState.profiles.any { it.isDefault })
        assertNotNull(newState.error) // Should have error message
    }

    @Test
    fun testDeleteActiveProfileSwitchesToDefault() = runTest {
        // Create and select a profile
        viewModel.createProfile("Test Profile")

        var state = viewModel.uiState.first()
        val testProfile = state.profiles.find { it.name == "Test Profile" }!!

        viewModel.selectProfile(testProfile.id)
        state = viewModel.uiState.first()
        assertEquals(testProfile.id, state.activeProfile?.id)

        // Delete the active profile
        viewModel.deleteProfile(testProfile.id)

        state = viewModel.uiState.first()

        // Should switch to default profile
        assertTrue(state.activeProfile?.isDefault == true)
    }

    @Test
    fun testUpdateProfile() = runTest {
        // Create a profile
        viewModel.createProfile("Test Profile")

        var state = viewModel.uiState.first()
        val profile = state.profiles.find { it.name == "Test Profile" }!!

        // Update the profile
        val updatedProfile = profile.copy(name = "Updated Name")
        viewModel.updateProfile(updatedProfile)

        state = viewModel.uiState.first()

        // Should have updated name
        val updated = state.profiles.find { it.id == profile.id }
        assertEquals("Updated Name", updated?.name)
    }

    @Test
    fun testCannotUpdateReadOnlyProfile() = runTest {
        // Create a read-only profile (simulate)
        viewModel.createProfile("Test Profile")

        var state = viewModel.uiState.first()
        val profile = state.profiles.find { it.name == "Test Profile" }!!

        // Make it read-only
        val readOnlyProfile = profile.copy(isReadOnly = true)
        repository.saveProfile(readOnlyProfile)

        // Try to update it
        val updatedProfile = readOnlyProfile.copy(name = "Should Not Update")
        viewModel.updateProfile(updatedProfile)

        state = viewModel.uiState.first()

        // Should have error
        assertNotNull(state.error)
    }

    @Test
    fun testAddFieldToScreen() = runTest {
        // Create a profile
        viewModel.createProfile("Test Profile")

        var state = viewModel.uiState.first()
        val profile = state.profiles.find { it.name == "Test Profile" }!!

        // Select it
        viewModel.selectProfile(profile.id)

        // Create a new field
        val speedField = DataFieldRegistry.getById(12)!! // Speed
        val layoutField = LayoutDataField(
            dataField = speedField,
            position = Position.TOP,
            fontSize = FontSize.MEDIUM,
            showLabel = true,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.SMALL
        )

        // Update profile with new field
        val screen = profile.screens[0]
        val updatedScreen = screen.copy(
            dataFields = screen.dataFields + layoutField
        )
        val updatedProfile = profile.copy(
            screens = listOf(updatedScreen)
        )

        viewModel.updateProfile(updatedProfile)

        state = viewModel.uiState.first()
        val updated = state.profiles.find { it.id == profile.id }

        // Should have the new field
        assertTrue(
            (updated?.screens?.get(0)?.dataFields?.size ?: 0) > profile.screens[0].dataFields.size
        )
    }

    @Test
    fun testProfilePersistsAcrossViewModelRecreation() = runTest {
        // Create a profile
        viewModel.createProfile("Persistent Profile")

        var state = viewModel.uiState.first()
        val profile = state.profiles.find { it.name == "Persistent Profile" }!!

        // Create a new ViewModel instance (simulates app restart)
        val newViewModel = LayoutBuilderViewModel(application)

        state = newViewModel.uiState.first()

        // Should still have the profile
        val loadedProfile = state.profiles.find { it.name == "Persistent Profile" }
        assertNotNull(loadedProfile)
        assertEquals(profile.id, loadedProfile?.id)
    }

    @Test
    fun testProfileWithMultipleFields() = runTest {
        // Create a profile with multiple fields
        viewModel.createProfile("Multi-Field Profile")

        var state = viewModel.uiState.first()
        val profile = state.profiles.find { it.name == "Multi-Field Profile" }!!

        // Add multiple fields
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

        viewModel.updateProfile(updatedProfile)

        state = viewModel.uiState.first()
        val updated = state.profiles.find { it.id == profile.id }

        // Should have 3 fields
        assertEquals(3, updated?.screens?.get(0)?.dataFields?.size)
        assertEquals(Position.TOP, updated?.screens?.get(0)?.dataFields?.get(0)?.position)
        assertEquals(Position.MIDDLE, updated?.screens?.get(0)?.dataFields?.get(1)?.position)
        assertEquals(Position.BOTTOM, updated?.screens?.get(0)?.dataFields?.get(2)?.position)
    }

    @Test
    fun testDefaultProfileAlwaysPresent() = runTest {
        // Even with no user profiles, default should exist
        val state = viewModel.uiState.first()

        val defaultProfile = state.profiles.find { it.isDefault }
        assertNotNull(defaultProfile)
        assertEquals(DefaultProfiles.getDefaultProfile().name, defaultProfile?.name)
    }

    @Test
    fun testErrorHandling() = runTest {
        // Try to delete non-existent profile
        viewModel.deleteProfile("non_existent_id")

        val state = viewModel.uiState.first()

        // Should not crash, profiles should be unchanged
        assertTrue(state.profiles.isNotEmpty())
    }
}

