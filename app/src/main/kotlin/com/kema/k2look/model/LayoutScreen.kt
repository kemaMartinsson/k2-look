package com.kema.k2look.model

/**
 * Represents a screen with data fields in a profile
 *
 * @param id Screen identifier
 * @param name Screen name
 * @param dataFields List of configured data fields (max 3)
 */
data class LayoutScreen(
    val id: Int,
    val name: String,
    val dataFields: List<LayoutDataField> = emptyList()
) {
    init {
        require(dataFields.size <= 3) { "Maximum 3 data fields per screen" }
    }
}

