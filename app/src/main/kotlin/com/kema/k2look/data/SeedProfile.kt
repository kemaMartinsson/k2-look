package com.kema.k2look.data

import com.kema.k2look.model.DataFieldProfile
import com.kema.k2look.model.IconSize
import com.kema.k2look.model.LayoutDataField
import com.kema.k2look.model.LayoutScreen

/**
 * Factory that builds the first-run seed profile.
 *
 * The seed profile is written to storage once on a fresh install (or when migrating
 * from an old install that never persisted the starter profile). After that it is
 * just a regular profile — fully editable and deletable like any other.
 */
object SeedProfile {

    /** Storage ID used to detect whether the seed has already been saved. */
    const val SEED_PROFILE_ID = "default"

    /**
     * Returns a new [DataFieldProfile] to use as the starter profile.
     * Speed · Distance · Elapsed Time on a 3-row (3D_FULL) layout.
     */
    fun build(): DataFieldProfile = DataFieldProfile(
        id = SEED_PROFILE_ID,
        name = "Default",
        screens = listOf(
            LayoutScreen(
                id = 1,
                name = "Main",
                templateId = "3D_FULL",
                dataFields = listOf(
                    LayoutDataField(
                        dataField = DataFieldRegistry.getById(12)!!, // Speed
                        zoneId = "3D_FULL_H",
                        showIcon = true,
                        iconSize = IconSize.LARGE
                    ),
                    LayoutDataField(
                        dataField = DataFieldRegistry.getById(2)!!, // Distance
                        zoneId = "3D_FULL_M",
                        showIcon = true,
                        iconSize = IconSize.LARGE
                    ),
                    LayoutDataField(
                        dataField = DataFieldRegistry.getById(1)!!, // Elapsed Time
                        zoneId = "3D_FULL_L",
                        showIcon = true,
                        iconSize = IconSize.LARGE
                    )
                )
            )
        )
    )
}

