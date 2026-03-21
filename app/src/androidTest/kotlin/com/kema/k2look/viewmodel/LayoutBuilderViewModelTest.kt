package com.kema.k2look.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kema.k2look.data.DataFieldRegistry
import com.kema.k2look.data.SeedProfile
import com.kema.k2look.data.ProfileRepository
import com.kema.k2look.model.IconSize
import com.kema.k2look.model.LayoutDataField
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
        application.getSharedPreferences("k2look_profiles", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    @Test
    fun testInitialState() = runTest {
        val state = viewModel.uiState.first()

        // Should have the seeded Default profile
        assertTrue(state.profiles.isNotEmpty())
        assertEquals(1, state.profiles.size)

        // Active profile should be the Default
        assertNotNull(state.activeProfile)
        assertEquals(SeedProfile.SEED_PROFILE_ID, state.activeProfile?.id)

        // No error initially
        assertNull(state.error)
    }

    @Test
    fun testCreateProfile() = runTest {
        viewModel.createProfile("Test Profile")

        val state = viewModel.uiState.first()

        // Default + new profile
        assertEquals(2, state.profiles.size)

        val createdProfile = state.profiles.find { it.name == "Test Profile" }
        assertNotNull(createdProfile)
    }

    @Test
    fun testCreateDuplicateNameRejected() = runTest {
        viewModel.createProfile("My Profile")
        viewModel.createProfile("My Profile") // duplicate

        val state = viewModel.uiState.first()

        // Only one profile with that name
        assertEquals(1, state.profiles.count { it.name == "My Profile" })
        assertNotNull(state.error) // error set
    }

    @Test
    fun testCreateMultipleProfiles() = runTest {
        viewModel.createProfile("Profile 1")
        viewModel.createProfile("Profile 2")
        viewModel.createProfile("Profile 3")

        val state = viewModel.uiState.first()

        // Default + 3 profiles
        assertEquals(4, state.profiles.size)
        assertTrue(state.profiles.any { it.name == "Profile 1" })
        assertTrue(state.profiles.any { it.name == "Profile 2" })
        assertTrue(state.profiles.any { it.name == "Profile 3" })
    }

    @Test
    fun testSelectProfile() = runTest {
        viewModel.createProfile("Test Profile")

        var state = viewModel.uiState.first()
        val testProfile = state.profiles.find { it.name == "Test Profile" }!!

        viewModel.selectProfile(testProfile.id)

        state = viewModel.uiState.first()
        assertEquals(testProfile.id, state.activeProfile?.id)
        assertEquals("Test Profile", state.activeProfile?.name)
    }

    @Test
    fun testDuplicateProfile() = runTest {
        viewModel.createProfile("Original Profile")

        var state = viewModel.uiState.first()
        val originalProfile = state.profiles.find { it.name == "Original Profile" }!!

        viewModel.duplicateProfile(originalProfile.id, "Duplicated Profile")

        state = viewModel.uiState.first()

        // Default + original + duplicate
        assertEquals(3, state.profiles.size)

        val duplicate = state.profiles.find { it.name == "Duplicated Profile" }
        assertNotNull(duplicate)
        assertNotEquals(originalProfile.id, duplicate?.id)
        assertEquals(originalProfile.screens.size, duplicate?.screens?.size)
    }

    @Test
    fun testDuplicateDuplicateNameRejected() = runTest {
        viewModel.createProfile("Profile A")

        var state = viewModel.uiState.first()
        val profileA = state.profiles.find { it.name == "Profile A" }!!

        viewModel.duplicateProfile(profileA.id, "Profile A") // same name

        state = viewModel.uiState.first()
        assertEquals(1, state.profiles.count { it.name == "Profile A" })
        assertNotNull(state.error)
    }

    @Test
    fun testDeleteProfile() = runTest {
        viewModel.createProfile("Profile 1")
        viewModel.createProfile("Profile 2")

        var state = viewModel.uiState.first()
        val profile1 = state.profiles.find { it.name == "Profile 1" }!!

        viewModel.deleteProfile(profile1.id)

        state = viewModel.uiState.first()

        assertEquals(2, state.profiles.size)
        assertNull(state.profiles.find { it.name == "Profile 1" })
        assertNotNull(state.profiles.find { it.name == "Profile 2" })
    }

    @Test
    fun testCannotDeleteLastProfile() = runTest {
        val state = viewModel.uiState.first()
        assertEquals(1, state.profiles.size) // only the seed profile

        val only = state.profiles.first()
        viewModel.deleteProfile(only.id)

        val newState = viewModel.uiState.first()

        // Profile must still be there
        assertEquals(1, newState.profiles.size)
        assertNotNull(newState.error)
    }

    @Test
    fun testDeleteActiveProfileSwitchesToFirst() = runTest {
        viewModel.createProfile("Test Profile")

        var state = viewModel.uiState.first()
        val testProfile = state.profiles.find { it.name == "Test Profile" }!!

        viewModel.selectProfile(testProfile.id)
        state = viewModel.uiState.first()
        assertEquals(testProfile.id, state.activeProfile?.id)

        viewModel.deleteProfile(testProfile.id)

        state = viewModel.uiState.first()

        // Active profile should have switched away from the deleted one
        assertNotEquals(testProfile.id, state.activeProfile?.id)
        assertNotNull(state.activeProfile)
    }

    @Test
    fun testUpdateProfile() = runTest {
        viewModel.createProfile("Test Profile")

        var state = viewModel.uiState.first()
        val profile = state.profiles.find { it.name == "Test Profile" }!!

        viewModel.updateProfile(profile.copy(name = "Updated Name"))

        state = viewModel.uiState.first()

        val updated = state.profiles.find { it.id == profile.id }
        assertEquals("Updated Name", updated?.name)
    }

    @Test
    fun testAddFieldToScreen() = runTest {
        viewModel.createProfile("Test Profile")

        var state = viewModel.uiState.first()
        val profile = state.profiles.find { it.name == "Test Profile" }!!

        viewModel.selectProfile(profile.id)

        val speedField = DataFieldRegistry.getById(12)!! // Speed
        val layoutField = LayoutDataField(
            dataField = speedField,
            zoneId = "3D_FULL_H",
            showLabel = true,
            showUnit = true,
            showIcon = true,
            iconSize = IconSize.SMALL
        )

        val screen = profile.screens[0]
        val updatedProfile = profile.copy(
            screens = listOf(screen.copy(dataFields = screen.dataFields + layoutField))
        )

        viewModel.updateProfile(updatedProfile)

        state = viewModel.uiState.first()
        val updated = state.profiles.find { it.id == profile.id }

        assertTrue(
            (updated?.screens?.get(0)?.dataFields?.size ?: 0) > profile.screens[0].dataFields.size
        )
    }

    @Test
    fun testProfilePersistsAcrossViewModelRecreation() = runTest {
        viewModel.createProfile("Persistent Profile")

        var state = viewModel.uiState.first()
        val profile = state.profiles.find { it.name == "Persistent Profile" }!!

        val newViewModel = LayoutBuilderViewModel(application)
        state = newViewModel.uiState.first()

        val loadedProfile = state.profiles.find { it.name == "Persistent Profile" }
        assertNotNull(loadedProfile)
        assertEquals(profile.id, loadedProfile?.id)
    }

    @Test
    fun testProfileWithMultipleFields() = runTest {
        viewModel.createProfile("Multi-Field Profile")

        var state = viewModel.uiState.first()
        val profile = state.profiles.find { it.name == "Multi-Field Profile" }!!

        val speedField = LayoutDataField(
            dataField = DataFieldRegistry.getById(12)!!,
            zoneId = "3D_FULL_H",
            showLabel = true, showUnit = true, showIcon = true, iconSize = IconSize.SMALL
        )
        val hrField = LayoutDataField(
            dataField = DataFieldRegistry.getById(4)!!,
            zoneId = "3D_FULL_M",
            showLabel = true, showUnit = true, showIcon = true, iconSize = IconSize.LARGE
        )
        val powerField = LayoutDataField(
            dataField = DataFieldRegistry.getById(7)!!,
            zoneId = "3D_FULL_L",
            showLabel = false, showUnit = true, showIcon = true, iconSize = IconSize.SMALL
        )

        val screen = profile.screens[0]
        viewModel.updateProfile(
            profile.copy(screens = listOf(screen.copy(dataFields = listOf(speedField, hrField, powerField))))
        )

        state = viewModel.uiState.first()
        val updated = state.profiles.find { it.id == profile.id }

        assertEquals(3, updated?.screens?.get(0)?.dataFields?.size)
        assertEquals("3D_FULL_H", updated?.screens?.get(0)?.dataFields?.get(0)?.zoneId)
        assertEquals("3D_FULL_M", updated?.screens?.get(0)?.dataFields?.get(1)?.zoneId)
        assertEquals("3D_FULL_L", updated?.screens?.get(0)?.dataFields?.get(2)?.zoneId)
    }

    @Test
    fun testSeedProfileAlwaysPresent() = runTest {
        val state = viewModel.uiState.first()

        val seedProfile = state.profiles.find { it.id == SeedProfile.SEED_PROFILE_ID }
        assertNotNull(seedProfile)
        assertEquals(SeedProfile.build().name, seedProfile?.name)
    }

    @Test
    fun testErrorHandling() = runTest {
        // Delete a non-existent ID — should not crash
        viewModel.deleteProfile("non_existent_id")

        val state = viewModel.uiState.first()
        assertTrue(state.profiles.isNotEmpty())
    }
}
