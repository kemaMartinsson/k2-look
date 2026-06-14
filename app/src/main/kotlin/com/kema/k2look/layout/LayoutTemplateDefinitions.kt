package com.kema.k2look.layout

import com.kema.k2look.service.AppLog as Log
import com.kema.k2look.R
import com.kema.k2look.model.FontSize
import com.kema.k2look.model.LayoutTemplate
import com.kema.k2look.model.LayoutZone

/**
 * Template definitions for [LayoutTemplateRegistry].
 *
 * Contains the full data for all layout templates, in two variants:
 * - [registerAllTemplates] — Android runtime version, includes R.drawable preview references.
 * - [registerAllTemplatesWithoutPreviews] — unit-test version, no R.drawable access.
 *
 * Both are called from [LayoutTemplateRegistry.ensureInitialized]. All active templates (1D, 2D,
 * 3D_FULL) are identical between the two variants except for the `preview` field. Partial-width
 * layouts (4D–6D, 3D_TRIANGLE, 2D_GAUGE) are disabled and kept as comments.
 *
 * IMPORTANT: Keep both functions in sync when adding or modifying templates.
 */
private const val TAG = "LayoutTemplateRegistry"

// ── Helpers ────────────────────────────────────────────────────────────────

private fun LayoutTemplateRegistry.register(template: LayoutTemplate) {
    templates[template.id] = template
    Log.d(
            TAG,
            "Registered template: ${template.id} (${template.name}) with ${template.maxFields} fields"
    )
}

private fun LayoutTemplateRegistry.registerWithoutPreview(template: LayoutTemplate) {
    templates[template.id] = template
}

// ── Unit-test variant (no R.drawable) ─────────────────────────────────────

internal fun LayoutTemplateRegistry.registerAllTemplatesWithoutPreviews() {
    // Register templates without preview images for unit tests
    registerWithoutPreview(
            LayoutTemplate(
                    id = "1D",
                    name = "Single Data",
                    zones =
                            listOf(
                                    LayoutZone(
                                            id = "1D",
                                            displayName = "Center",
                                            x = 59,
                                            y = 41,
                                            width = 187,
                                            height = 163,
                                            font = 5,
                                            fontSize = FontSize.LARGE,
                                            isChrono = true,
                                            chronoHourX = 239,
                                            chronoHourY = 121
                                    )
                            ),
                    maxFields = 1
            )
    )

    registerWithoutPreview(
            LayoutTemplate(
                    id = "2D",
                    name = "Two Data",
                    zones =
                            listOf(
                                    LayoutZone(
                                            id = "2D_H",
                                            displayName = "Top",
                                            x = 30,
                                            y = 129,
                                            width = 244,
                                            height = 60,
                                            font = 4,
                                            fontSize = FontSize.LARGE,
                                            isChrono = true,
                                            chronoHourX = 203,
                                            chronoHourY = 154
                                    ),
                                    LayoutZone(
                                            id = "2D_L",
                                            displayName = "Bottom",
                                            x = 30,
                                            y = 25,
                                            width = 244,
                                            height = 60,
                                            font = 4,
                                            fontSize = FontSize.LARGE,
                                            isChrono = true,
                                            chronoHourX = 203,
                                            chronoHourY = 50
                                    )
                            ),
                    maxFields = 2
            )
    )

    /* Disabled — partial-width
    registerWithoutPreview(
        LayoutTemplate(
            id = "3D_TRIANGLE",
            name = "Triangle Layout",
            zones = listOf(
                LayoutZone(
                    id = "3D_TRIANGLE_H",
                    displayName = "Top Center",
                    x = 30, y = 129, width = 244, height = 60,
                    font = 4, fontSize = FontSize.LARGE,
                    isChrono = true,
                    chronoHourX = 203, chronoHourY = 154
                ),
                LayoutZone(
                    id = "3D_HALF_L1",
                    displayName = "Bottom Right",
                    x = 157, y = 33, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true,
                    chronoHourX = 257, chronoHourY = 45
                ),
                LayoutZone(
                    id = "3D_HALF_L2",
                    displayName = "Bottom Left",
                    x = 30, y = 33, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true,
                    chronoHourX = 130, chronoHourY = 45
                )
            ),
            maxFields = 3
        )
    )
    */

    registerWithoutPreview(
            LayoutTemplate(
                    id = "3D_FULL",
                    name = "Three Data Full",
                    zones =
                            listOf(
                                    LayoutZone(
                                            id = "3D_FULL_H",
                                            displayName = "Top",
                                            x = 30,
                                            y = 153,
                                            width = 244,
                                            height = 50,
                                            font = 3,
                                            fontSize = FontSize.MEDIUM,
                                            isChrono = true,
                                            chronoHourX = 211,
                                            chronoHourY = 170
                                    ),
                                    LayoutZone(
                                            id = "3D_FULL_M",
                                            displayName = "Middle",
                                            x = 30,
                                            y = 89,
                                            width = 244,
                                            height = 50,
                                            font = 3,
                                            fontSize = FontSize.MEDIUM,
                                            isChrono = true,
                                            chronoHourX = 211,
                                            chronoHourY = 106
                                    ),
                                    LayoutZone(
                                            id = "3D_FULL_L",
                                            displayName = "Bottom",
                                            x = 30,
                                            y = 25,
                                            width = 244,
                                            height = 50,
                                            font = 3,
                                            fontSize = FontSize.MEDIUM,
                                            isChrono = true,
                                            chronoHourX = 211,
                                            chronoHourY = 42
                                    )
                            ),
                    maxFields = 3
            )
    )

    /* Disabled — partial-width layout
    registerWithoutPreview(
        LayoutTemplate(
            id = "4D",
            name = "Four Data",
            zones = listOf(
                LayoutZone(id = "4D_FULL_H", displayName = "Top",
                    x = 30, y = 149, width = 244, height = 60,
                    font = 4, fontSize = FontSize.LARGE,
                    isChrono = true, chronoHourX = 203, chronoHourY = 174),
                LayoutZone(id = "4D_FULL_L", displayName = "Second Row",
                    x = 30, y = 80, width = 244, height = 60,
                    font = 4, fontSize = FontSize.LARGE,
                    isChrono = true, chronoHourX = 203, chronoHourY = 105),
                LayoutZone(id = "3D_HALF_L1", displayName = "Bottom Right",
                    x = 157, y = 33, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 257, chronoHourY = 45),
                LayoutZone(id = "3D_HALF_L2", displayName = "Bottom Left",
                    x = 30, y = 33, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 130, chronoHourY = 45)
            ),
            maxFields = 4
        )
    )
    */

    /* Disabled — partial-width layout
    registerWithoutPreview(
        LayoutTemplate(
            id = "5D",
            name = "Five Data",
            zones = listOf(
                LayoutZone(id = "4D_FULL_H", displayName = "Top",
                    x = 30, y = 149, width = 244, height = 60,
                    font = 4, fontSize = FontSize.LARGE,
                    isChrono = true, chronoHourX = 203, chronoHourY = 174),
                LayoutZone(id = "3D_HALF_M1", displayName = "Middle Right",
                    x = 157, y = 95, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 257, chronoHourY = 105),
                LayoutZone(id = "3D_HALF_M2", displayName = "Middle Left",
                    x = 30, y = 95, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 130, chronoHourY = 105),
                LayoutZone(id = "3D_HALF_L1", displayName = "Bottom Right",
                    x = 157, y = 33, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 257, chronoHourY = 45),
                LayoutZone(id = "3D_HALF_L2", displayName = "Bottom Left",
                    x = 30, y = 33, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 130, chronoHourY = 45)
            ),
            maxFields = 5
        )
    )
    */

    /* Disabled — partial-width layout
    registerWithoutPreview(
        LayoutTemplate(
            id = "6D",
            name = "Six Data",
            zones = listOf(
                LayoutZone(id = "3D_HALF_H1", displayName = "Top Right",
                    x = 157, y = 157, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 257, chronoHourY = 166),
                LayoutZone(id = "3D_HALF_H2", displayName = "Top Left",
                    x = 30, y = 157, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 130, chronoHourY = 166),
                LayoutZone(id = "3D_HALF_M1", displayName = "Middle Right",
                    x = 157, y = 95, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 257, chronoHourY = 105),
                LayoutZone(id = "3D_HALF_M2", displayName = "Middle Left",
                    x = 30, y = 95, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 130, chronoHourY = 105),
                LayoutZone(id = "3D_HALF_L1", displayName = "Bottom Right",
                    x = 157, y = 33, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 257, chronoHourY = 45),
                LayoutZone(id = "3D_HALF_L2", displayName = "Bottom Left",
                    x = 30, y = 33, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 130, chronoHourY = 45)
            ),
            maxFields = 6
        )
    )
    */

    /* Disabled — gauge layout, re-enable post-v1.0
    registerWithoutPreview(
        LayoutTemplate(
            id = "2D_GAUGE",
            name = "2 datafields",
            zones = listOf(
                LayoutZone(id = "2D_GAUGE_TEXT", displayName = "Top (Text)",
                    x = 30, y = 196, width = 244, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM),
                LayoutZone(id = "2D_GAUGE_ARC", displayName = "Bottom (Gauge)",
                    x = 30, y = 25, width = 244, height = 160,
                    font = 3, fontSize = FontSize.LARGE)
            ),
            maxFields = 2
        )
    )
    */
}

// ── Android runtime variant (includes R.drawable previews) ─────────────────

internal fun LayoutTemplateRegistry.registerAllTemplates() {
    // 1D - Single Large Data
    register(
            LayoutTemplate(
                    id = "1D",
                    name = "Single Data",
                    zones =
                            listOf(
                                    LayoutZone(
                                            id = "1D",
                                            displayName = "Center",
                                            x = 59,
                                            y = 41,
                                            width = 187,
                                            height = 163,
                                            font = 5,
                                            fontSize = FontSize.LARGE,
                                            isChrono = true,
                                            chronoHourX = 239,
                                            chronoHourY = 121
                                    )
                            ),
                    maxFields = 1,
                    preview = R.drawable.layout_preview_1d
            )
    )

    // 2D - Two Data Fields
    register(
            LayoutTemplate(
                    id = "2D",
                    name = "Two Data",
                    zones =
                            listOf(
                                    LayoutZone(
                                            id = "2D_H",
                                            displayName = "Top",
                                            x = 30,
                                            y = 129,
                                            width = 244,
                                            height = 60,
                                            font = 4,
                                            fontSize = FontSize.LARGE,
                                            isChrono = true,
                                            chronoHourX = 203,
                                            chronoHourY = 154
                                    ),
                                    LayoutZone(
                                            id = "2D_L",
                                            displayName = "Bottom",
                                            x = 30,
                                            y = 25,
                                            width = 244,
                                            height = 60,
                                            font = 4,
                                            fontSize = FontSize.LARGE,
                                            isChrono = true,
                                            chronoHourX = 203,
                                            chronoHourY = 50
                                    )
                            ),
                    maxFields = 2,
                    preview = R.drawable.layout_preview_2d
            )
    )

    /* Disabled — partial-width layout
    // 3D_TRIANGLE - Triangle Layout
    register(
        LayoutTemplate(
            id = "3D_TRIANGLE",
            name = "Triangle Layout",
            zones = listOf(
                LayoutZone(
                    id = "3D_TRIANGLE_H",
                    displayName = "Top Center",
                    x = 30, y = 129, width = 244, height = 60,
                    font = 4, fontSize = FontSize.LARGE,
                    isChrono = true,
                    chronoHourX = 203, chronoHourY = 154
                ),
                LayoutZone(
                    id = "3D_HALF_L1",
                    displayName = "Bottom Right",
                    x = 157, y = 33, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true,
                    chronoHourX = 257, chronoHourY = 45
                ),
                LayoutZone(
                    id = "3D_HALF_L2",
                    displayName = "Bottom Left",
                    x = 30, y = 33, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true,
                    chronoHourX = 130, chronoHourY = 45
                )
            ),
            maxFields = 3,
            preview = R.drawable.layout_preview_3d_triangle
        )
    )
    */

    // 3D_FULL - Three Full Width (Default)
    register(
            LayoutTemplate(
                    id = "3D_FULL",
                    name = "Three Rows",
                    zones =
                            listOf(
                                    LayoutZone(
                                            id = "3D_FULL_H",
                                            displayName = "Top",
                                            x = 30,
                                            y = 153,
                                            width = 244,
                                            height = 50,
                                            font = 3,
                                            fontSize = FontSize.MEDIUM,
                                            isChrono = true,
                                            chronoHourX = 211,
                                            chronoHourY = 170
                                    ),
                                    LayoutZone(
                                            id = "3D_FULL_M",
                                            displayName = "Middle",
                                            x = 30,
                                            y = 89,
                                            width = 244,
                                            height = 50,
                                            font = 3,
                                            fontSize = FontSize.MEDIUM,
                                            isChrono = true,
                                            chronoHourX = 211,
                                            chronoHourY = 106
                                    ),
                                    LayoutZone(
                                            id = "3D_FULL_L",
                                            displayName = "Bottom",
                                            x = 30,
                                            y = 25,
                                            width = 244,
                                            height = 50,
                                            font = 3,
                                            fontSize = FontSize.MEDIUM,
                                            isChrono = true,
                                            chronoHourX = 211,
                                            chronoHourY = 42
                                    )
                            ),
                    maxFields = 3,
                    preview = R.drawable.layout_preview_3d_full
            )
    )

    /* Disabled — partial-width layout
    // 4D - Four Data Fields (2 full + 2 half)
    register(
        LayoutTemplate(
            id = "4D",
            name = "Four Data",
            zones = listOf(
                LayoutZone(id = "4D_FULL_H", displayName = "Top",
                    x = 30, y = 149, width = 244, height = 60,
                    font = 4, fontSize = FontSize.LARGE,
                    isChrono = true, chronoHourX = 203, chronoHourY = 174),
                LayoutZone(id = "4D_FULL_L", displayName = "Second Row",
                    x = 30, y = 80, width = 244, height = 60,
                    font = 4, fontSize = FontSize.LARGE,
                    isChrono = true, chronoHourX = 203, chronoHourY = 105),
                LayoutZone(id = "3D_HALF_L1", displayName = "Bottom Right",
                    x = 157, y = 33, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 257, chronoHourY = 45),
                LayoutZone(id = "3D_HALF_L2", displayName = "Bottom Left",
                    x = 30, y = 33, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 130, chronoHourY = 45)
            ),
            maxFields = 4,
            preview = R.drawable.layout_preview_4d
        )
    )
    */

    /* Disabled — partial-width layout
    // 5D - Five Data Fields
    register(
        LayoutTemplate(
            id = "5D",
            name = "Five Data",
            zones = listOf(
                LayoutZone(id = "4D_FULL_H", displayName = "Top",
                    x = 30, y = 149, width = 244, height = 60,
                    font = 4, fontSize = FontSize.LARGE,
                    isChrono = true, chronoHourX = 203, chronoHourY = 174),
                LayoutZone(id = "3D_HALF_M1", displayName = "Middle Right",
                    x = 157, y = 95, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 257, chronoHourY = 105),
                LayoutZone(id = "3D_HALF_M2", displayName = "Middle Left",
                    x = 30, y = 95, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 130, chronoHourY = 105),
                LayoutZone(id = "3D_HALF_L1", displayName = "Bottom Right",
                    x = 157, y = 33, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 257, chronoHourY = 45),
                LayoutZone(id = "3D_HALF_L2", displayName = "Bottom Left",
                    x = 30, y = 33, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 130, chronoHourY = 45)
            ),
            maxFields = 5,
            preview = R.drawable.layout_preview_5d
        )
    )
    */

    /* Disabled — partial-width layout
    // 6D - Six Half-Width Fields
    register(
        LayoutTemplate(
            id = "6D",
            name = "Six Data",
            zones = listOf(
                LayoutZone(id = "3D_HALF_H1", displayName = "Top Right",
                    x = 157, y = 157, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 257, chronoHourY = 166),
                LayoutZone(id = "3D_HALF_H2", displayName = "Top Left",
                    x = 30, y = 157, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 130, chronoHourY = 166),
                LayoutZone(id = "3D_HALF_M1", displayName = "Middle Right",
                    x = 157, y = 95, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 257, chronoHourY = 105),
                LayoutZone(id = "3D_HALF_M2", displayName = "Middle Left",
                    x = 30, y = 95, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 130, chronoHourY = 105),
                LayoutZone(id = "3D_HALF_L1", displayName = "Bottom Right",
                    x = 157, y = 33, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 257, chronoHourY = 45),
                LayoutZone(id = "3D_HALF_L2", displayName = "Bottom Left",
                    x = 30, y = 33, width = 117, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM,
                    isChrono = true, chronoHourX = 130, chronoHourY = 45)
            ),
            maxFields = 6,
            preview = R.drawable.layout_preview_6d
        )
    )
    */

    /* Disabled — gauge layout, re-enable post-v1.0
    register(
        LayoutTemplate(
            id = "2D_GAUGE",
            name = "2 datafields",
            zones = listOf(
                LayoutZone(id = "2D_GAUGE_TEXT", displayName = "Top (Text)",
                    x = 30, y = 196, width = 244, height = 35,
                    font = 2, fontSize = FontSize.MEDIUM),
                LayoutZone(id = "2D_GAUGE_ARC", displayName = "Bottom (Gauge)",
                    x = 30, y = 25, width = 244, height = 160,
                    font = 3, fontSize = FontSize.LARGE)
            ),
            maxFields = 2
        )
    )
    */
}

