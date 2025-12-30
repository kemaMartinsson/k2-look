package com.kema.k2look.model

/**
 * Layout template definition based on ActiveLook official templates
 *
 * Represents one of 6 official layout templates (1D through 6D) that define
 * precise positioning zones for data fields on ActiveLook glasses.
 *
 * @param id Template identifier ("1D", "2D", "3D_TRIANGLE", "3D_FULL", "4D", "5D", "6D")
 * @param name Display name for the template
 * @param zones List of positioning zones available in this template
 * @param maxFields Maximum number of data fields supported (1-6)
 * @param preview Drawable resource ID for preview image (nullable for unit tests)
 */
data class LayoutTemplate(
    val id: String,
    val name: String,
    val zones: List<LayoutZone>,
    val maxFields: Int,
    val preview: Int? = null
) {
    init {
        require(maxFields in 1..6) { "maxFields must be between 1 and 6" }
        require(zones.size == maxFields) { "Number of zones must match maxFields" }
    }
}

