package com.kema.k2look.data

import com.kema.k2look.model.DataFieldProfile
import com.kema.k2look.model.FontSize
import com.kema.k2look.model.IconSize
import com.kema.k2look.model.LayoutDataField
import com.kema.k2look.model.LayoutScreen
import com.kema.k2look.model.Position

/**
 * Factory for creating default and template profiles
 */
object DefaultProfiles {

    /**
     * Default read-only profile with essential metrics:
     * - Speed
     * - Distance
     * - Elapsed Time
     */
    fun getDefaultProfile(): DataFieldProfile {
        return DataFieldProfile(
            id = "default",
            name = "Default",
            isDefault = true,
            isReadOnly = true,
            screens = listOf(
                LayoutScreen(
                    id = 1,
                    name = "Main",
                    dataFields = listOf(
                        LayoutDataField(
                            dataField = DataFieldRegistry.getById(12)!!, // Speed
                            position = Position.TOP,
                            fontSize = FontSize.LARGE,
                            showIcon = true,
                            iconSize = IconSize.LARGE
                        ),
                        LayoutDataField(
                            dataField = DataFieldRegistry.getById(2)!!, // Distance
                            position = Position.MIDDLE,
                            fontSize = FontSize.LARGE,
                            showIcon = true,
                            iconSize = IconSize.LARGE
                        ),
                        LayoutDataField(
                            dataField = DataFieldRegistry.getById(1)!!, // Elapsed Time
                            position = Position.BOTTOM,
                            fontSize = FontSize.LARGE,
                            showIcon = true,
                            iconSize = IconSize.LARGE
                        )
                    )
                )
            )
        )
    }

    /**
     * Pre-built template for road bikes with power meter
     */
    fun getRoadBikeTemplate(): DataFieldProfile {
        return DataFieldProfile(
            id = "template_road",
            name = "Road Bike",
            isDefault = false,
            isReadOnly = false,
            screens = listOf(
                LayoutScreen(
                    id = 1,
                    name = "Primary",
                    dataFields = listOf(
                        LayoutDataField(
                            dataField = DataFieldRegistry.getById(12)!!, // Speed
                            position = Position.TOP,
                            fontSize = FontSize.LARGE,
                            showIcon = true
                        ),
                        LayoutDataField(
                            dataField = DataFieldRegistry.getById(7)!!, // Power
                            position = Position.MIDDLE,
                            fontSize = FontSize.LARGE,
                            showIcon = true
                        ),
                        LayoutDataField(
                            dataField = DataFieldRegistry.getById(4)!!, // Heart Rate
                            position = Position.BOTTOM,
                            fontSize = FontSize.MEDIUM,
                            showIcon = true
                        )
                    )
                )
            )
        )
    }

    /**
     * Pre-built template for bikes without power meter
     */
    fun getGravelBikeTemplate(): DataFieldProfile {
        return DataFieldProfile(
            id = "template_gravel",
            name = "Gravel",
            isDefault = false,
            isReadOnly = false,
            screens = listOf(
                LayoutScreen(
                    id = 1,
                    name = "Primary",
                    dataFields = listOf(
                        LayoutDataField(
                            dataField = DataFieldRegistry.getById(12)!!, // Speed
                            position = Position.TOP,
                            fontSize = FontSize.LARGE,
                            showIcon = true
                        ),
                        LayoutDataField(
                            dataField = DataFieldRegistry.getById(4)!!, // Heart Rate
                            position = Position.MIDDLE,
                            fontSize = FontSize.LARGE,
                            showIcon = true
                        ),
                        LayoutDataField(
                            dataField = DataFieldRegistry.getById(2)!!, // Distance
                            position = Position.BOTTOM,
                            fontSize = FontSize.MEDIUM,
                            showIcon = true
                        )
                    )
                )
            )
        )
    }
}

