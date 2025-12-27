package com.kema.k2look.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kema.k2look.model.DataFieldProfile

/**
 * Repository for persisting and loading DataField profiles
 */
class ProfileRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("k2look_profiles", Context.MODE_PRIVATE)
    private val gson = Gson()

    /**
     * Load all user profiles from storage
     * Does not include the default profile
     */
    fun loadProfiles(): List<DataFieldProfile> {
        val json = prefs.getString("user_profiles", "[]") ?: "[]"
        val type = object : TypeToken<List<DataFieldProfile>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading profiles", e)
            emptyList()
        }
    }

    /**
     * Save a profile to storage
     * Updates existing profile if ID matches, otherwise adds new profile
     */
    fun saveProfile(profile: DataFieldProfile) {
        val profiles = loadProfiles().toMutableList()
        val index = profiles.indexOfFirst { it.id == profile.id }

        if (index >= 0) {
            // Update existing profile
            profiles[index] = profile.copy(modifiedAt = System.currentTimeMillis())
        } else {
            // Add new profile
            profiles.add(profile)
        }

        val json = gson.toJson(profiles)
        prefs.edit().putString("user_profiles", json).apply()
        Log.i(TAG, "Saved profile: ${profile.name}")
    }

    /**
     * Delete a profile from storage
     */
    fun deleteProfile(profileId: String) {
        val profiles = loadProfiles().filter { it.id != profileId }
        val json = gson.toJson(profiles)
        prefs.edit().putString("user_profiles", json).apply()
        Log.i(TAG, "Deleted profile: $profileId")
    }

    companion object {
        private const val TAG = "ProfileRepository"
    }
}

