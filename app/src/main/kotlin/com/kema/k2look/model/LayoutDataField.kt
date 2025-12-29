package com.kema.k2look.model

/**
 * Represents a configured data field in a layout with display settings
 *
 * @param dataField The metric to display
 * @param zoneId Zone identifier from the layout template (e.g., "3D_HALF_L1")
 * @param showLabel Whether to show the metric label
 * @param showUnit Whether to show the unit
 * @param showIcon Whether to display icon next to label
 * @param iconSize Icon size (28×28 or 40×40)
 */
data class LayoutDataField(
    val dataField: DataField,
    val zoneId: String,
    val showLabel: Boolean = true,
    val showUnit: Boolean = true,
    val showIcon: Boolean = true,
    val iconSize: IconSize = IconSize.SMALL
) {
    /**
     * Legacy constructor for backward compatibility with Position-based layouts
     */
    @Deprecated(
        "Use zoneId instead of position",
        ReplaceWith("LayoutDataField(dataField, zoneId, showLabel, showUnit, showIcon, iconSize)")
    )
    constructor(
        dataField: DataField,
        position: Position,
        fontSize: FontSize = FontSize.MEDIUM,
        showLabel: Boolean = true,
        showUnit: Boolean = true,
        showIcon: Boolean = true,
        iconSize: IconSize = IconSize.SMALL
    ) : this(
        dataField = dataField,
        zoneId = when (position) {
            Position.TOP -> "3D_FULL_H"
            Position.MIDDLE -> "3D_FULL_M"
            Position.BOTTOM -> "3D_FULL_L"
        },
        showLabel = showLabel,
        showUnit = showUnit,
        showIcon = showIcon,
        iconSize = iconSize
    )
}

