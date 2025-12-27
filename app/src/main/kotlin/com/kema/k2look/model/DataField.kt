package com.kema.k2look.model

/**
 * Represents a metric/data field that can be displayed on glasses
 *
 * @param id Garmin metric ID for reference
 * @param name Display name (e.g., "Speed", "Heart Rate")
 * @param unit Unit of measurement (e.g., "km/h", "bpm")
 * @param category Category for UI organization
 * @param karooStreamType Karoo data stream type (String constant from DataType.Type)
 * @param icon28 28×28 icon ID from ActiveLook Visual Assets (optional)
 * @param icon40 40×40 icon ID from ActiveLook Visual Assets (optional)
 */
data class DataField(
    val id: Int,
    val name: String,
    val unit: String,
    val category: DataFieldCategory,
    val karooStreamType: String,
    val icon28: Int? = null,
    val icon40: Int? = null
)

