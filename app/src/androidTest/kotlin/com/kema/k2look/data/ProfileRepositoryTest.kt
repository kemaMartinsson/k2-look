package com.kema.k2look.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kema.k2look.model.DataFieldProfile
import com.kema.k2look.model.FontSize
import com.kema.k2look.model.IconSize
import com.kema.k2look.model.LayoutDataField
import com.kema.k2look.model.LayoutScreen
import com.kema.k2look.model.Position
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for ProfileRepository to verify persistence across app restarts
 */
@RunWith(AndroidJUnit4::class)
class ProfileRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: ProfileRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = ProfileRepository(context)

        // Clear any existing data
        context.getSharedPreferences("k2look_profiles", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    @After
    fun tearDown() {
        // Clean up after tests
        context.getSharedPreferences("k2look_profiles", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    @Test
    fun testSaveAndLoadProfile() {
        // Create a test profile
        val profile = createTestProfile("Test Profile")

        // Save it
        repository.saveProfile(profile)

        // Create a new repository instance (simulates app restart)
        val newRepository = ProfileRepository(context)

        // Load profiles
        val loadedProfiles = newRepository.loadProfiles()

        // Verify the profile was persisted
        assertEquals(1, loadedProfiles.size)
        assertEquals(profile.id, loadedProfiles[0].id)
        assertEquals(profile.name, loadedProfiles[0].name)
    }

    @Test
    fun testSaveMultipleProfiles() {
        // Create multiple profiles
        val profile1 = createTestProfile("Profile 1")
        val profile2 = createTestProfile("Profile 2")
        val profile3 = createTestProfile("Profile 3")

        // Save them
        repository.saveProfile(profile1)
        repository.saveProfile(profile2)
        repository.saveProfile(profile3)

        // Create new repository (simulates restart)
        val newRepository = ProfileRepository(context)

        // Load profiles
        val loadedProfiles = newRepository.loadProfiles()

        // Verify all profiles persisted
        assertEquals(3, loadedProfiles.size)
        assertTrue(loadedProfiles.any { it.name == "Profile 1" })
        assertTrue(loadedProfiles.any { it.name == "Profile 2" })
        assertTrue(loadedProfiles.any { it.name == "Profile 3" })
    }

    @Test
    fun testUpdateExistingProfile() {
        // Create and save a profile
        val profile = createTestProfile("Original Name")
        repository.saveProfile(profile)

        // Update the profile
        val updatedProfile = profile.copy(name = "Updated Name")
        repository.saveProfile(updatedProfile)

        // Create new repository (simulates restart)
        val newRepository = ProfileRepository(context)

        // Load profiles
        val loadedProfiles = newRepository.loadProfiles()

        // Verify only one profile exists with updated name
        assertEquals(1, loadedProfiles.size)
        assertEquals("Updated Name", loadedProfiles[0].name)
        assertEquals(profile.id, loadedProfiles[0].id)
    }

    @Test
    fun testDeleteProfile() {
        // Create and save profiles
        val profile1 = createTestProfile("Profile 1")
        val profile2 = createTestProfile("Profile 2")

        repository.saveProfile(profile1)
        repository.saveProfile(profile2)

        // Delete one profile
        repository.deleteProfile(profile1.id)

        // Create new repository (simulates restart)
        val newRepository = ProfileRepository(context)

        // Load profiles
        val loadedProfiles = newRepository.loadProfiles()

        // Verify only profile2 remains
        assertEquals(1, loadedProfiles.size)
        assertEquals(profile2.id, loadedProfiles[0].id)
        assertEquals("Profile 2", loadedProfiles[0].name)
    }

    @Test
    fun testPersistenceWithComplexProfile() {
        // Create a profile with multiple screens and fields
        val screen = LayoutScreen(
            id = 1,
            name = "Main",
            dataFields = listOf(
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
                    dataField = DataFieldRegistry.getById(4)!!, // Heart Rate
                    position = Position.MIDDLE,
                    fontSize = FontSize.MEDIUM,
                    showLabel = true,
                    showUnit = true,
                    showIcon = true,
                    iconSize = IconSize.LARGE
                ),
                LayoutDataField(
                    dataField = DataFieldRegistry.getById(7)!!, // Power
                    position = Position.BOTTOM,
                    fontSize = FontSize.MEDIUM,
                    showLabel = false,
                    showUnit = true,
                    showIcon = true,
                    iconSize = IconSize.SMALL
                )
            )
        )

        val profile = DataFieldProfile(
            id = "complex_test",
            name = "Complex Profile",
            screens = listOf(screen),
            isDefault = false,
            isReadOnly = false,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        )

        // Save the complex profile
        repository.saveProfile(profile)

        // Create new repository (simulates restart)
        val newRepository = ProfileRepository(context)

        // Load profiles
        val loadedProfiles = newRepository.loadProfiles()

        // Verify the complex profile was fully persisted
        assertEquals(1, loadedProfiles.size)
        val loadedProfile = loadedProfiles[0]
        assertEquals(profile.id, loadedProfile.id)
        assertEquals(profile.name, loadedProfile.name)
        assertEquals(1, loadedProfile.screens.size)
        assertEquals(3, loadedProfile.screens[0].dataFields.size)

        // Verify field details
        val fields = loadedProfile.screens[0].dataFields
        assertEquals(Position.TOP, fields[0].position)
        assertEquals(FontSize.LARGE, fields[0].fontSize)
        assertTrue(fields[0].showIcon)
        assertEquals(IconSize.SMALL, fields[0].iconSize)

        assertEquals(Position.MIDDLE, fields[1].position)
        assertEquals(IconSize.LARGE, fields[1].iconSize)

        assertEquals(Position.BOTTOM, fields[2].position)
        assertFalse(fields[2].showLabel)
    }

    @Test
    fun testEmptyLoadReturnsEmptyList() {
        // Don't save anything

        // Create new repository
        val newRepository = ProfileRepository(context)

        // Load profiles
        val loadedProfiles = newRepository.loadProfiles()

        // Should return empty list, not null
        assertNotNull(loadedProfiles)
        assertEquals(0, loadedProfiles.size)
    }

    /**
     * Helper to create a test profile
     */
    private fun createTestProfile(name: String): DataFieldProfile {
        val screen = LayoutScreen(
            id = 1,
            name = "Screen 1",
            dataFields = listOf(
                LayoutDataField(
                    dataField = DataFieldRegistry.getById(12)!!, // Speed
                    position = Position.TOP,
                    fontSize = FontSize.MEDIUM,
                    showLabel = true,
                    showUnit = true,
                    showIcon = true,
                    iconSize = IconSize.SMALL
                )
            )
        )

        return DataFieldProfile(
            id = "test_${System.currentTimeMillis()}",
            name = name,
            screens = listOf(screen),
            isDefault = false,
            isReadOnly = false,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        )
    }
}

