package com.kema.k2look.model

import java.util.UUID

/**
 * Configuration profile containing multiple screens with data fields.
 *
 * All profiles are equal — there are no system/read-only profiles.
 *
 * @param id Unique profile identifier
 * @param name Profile name (e.g., "Road Bike", "Gravel")
 * @param screens List of screens in this profile
 * @param createdAt Creation timestamp
 * @param modifiedAt Last modification timestamp
 */
data class DataFieldProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val screens: List<LayoutScreen>,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)
