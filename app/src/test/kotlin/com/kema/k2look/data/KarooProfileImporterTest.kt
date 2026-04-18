package com.kema.k2look.data

import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.RideProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KarooProfileImporterTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun element(dataTypeId: String) =
            RideProfile.Page.Element(dataTypeId = dataTypeId, gridSize = Pair(60, 15))

    private fun page(vararg dataTypeIds: String, mapPage: Boolean = false) =
            RideProfile.Page(mapPage = mapPage, elements = dataTypeIds.map { element(it) })

    private fun profile(vararg pages: RideProfile.Page, name: String = "Test") =
            RideProfile(
                    id = "test-id",
                    name = name,
                    pages = pages.toList(),
                    indoor = false,
                    defaultActivityType = "RIDE",
                    routingPreference = "ROAD"
            )

    // ── Template selection ────────────────────────────────────────────────────

    @Test
    fun `single field page uses 1D template`() {
        val result = KarooProfileImporter.import(profile(page(DataType.Type.SPEED)))
        assertEquals("1D", result.screens.first().templateId)
        assertEquals(1, result.screens.first().dataFields.size)
    }

    @Test
    fun `two field page uses 2D template`() {
        val result =
                KarooProfileImporter.import(
                        profile(page(DataType.Type.SPEED, DataType.Type.HEART_RATE))
                )
        assertEquals("2D", result.screens.first().templateId)
    }

    @Test
    fun `three field page uses 3D_FULL template`() {
        val result =
                KarooProfileImporter.import(
                        profile(
                                page(
                                        DataType.Type.SPEED,
                                        DataType.Type.HEART_RATE,
                                        DataType.Type.POWER
                                )
                        )
                )
        assertEquals("3D_FULL", result.screens.first().templateId)
    }

    // TODO: 4D/5D/6D layouts are not yet selectable — re-enable when these templates are added to
    // the UI
    // @Test fun `four field page uses 4D template`() { ... }
    // @Test fun `five field page uses 5D template`() { ... }
    // @Test fun `six field page uses 6D template`() { ... }

    // ── Truncation ────────────────────────────────────────────────────────────

    // TODO: re-enable when 6D template is selectable
    // @Test fun `page with more than 6 fields is truncated to 6`() { ... }

    // ── Map page skipping ─────────────────────────────────────────────────────

    @Test
    fun `map pages are skipped`() {
        val result =
                KarooProfileImporter.import(
                        profile(
                                page(DataType.Type.SPEED, mapPage = true),
                                page(DataType.Type.HEART_RATE)
                        )
                )
        assertEquals(1, result.screens.size)
        assertEquals("Heart Rate", result.screens.first().dataFields.first().dataField.name)
    }

    // ── Unknown fields ────────────────────────────────────────────────────────

    @Test
    fun `unknown third-party fields are silently skipped`() {
        val result =
                KarooProfileImporter.import(
                        profile(page(DataType.Type.SPEED, "com.thirdparty.extension.field"))
                )
        // Only Speed should be mapped; unknown field skipped → 1 field → 1D template
        assertEquals(1, result.screens.first().dataFields.size)
        assertEquals("Speed", result.screens.first().dataFields.first().dataField.name)
        assertEquals("1D", result.screens.first().templateId)
    }

    @Test
    fun `page with only unknown fields is skipped`() {
        val result =
                KarooProfileImporter.import(
                        profile(page("com.unknown.a", "com.unknown.b"), page(DataType.Type.POWER))
                )
        assertEquals(1, result.screens.size)
        assertEquals("Power", result.screens.first().dataFields.first().dataField.name)
    }

    // ── Profile properties ────────────────────────────────────────────────────

    @Test
    fun `imported profile name matches Karoo profile name`() {
        val result =
                KarooProfileImporter.import(profile(page(DataType.Type.SPEED), name = "Race Day"))
        assertEquals("Race Day", result.name)
    }

    @Test
    fun `each non-map page becomes one screen`() {
        val result =
                KarooProfileImporter.import(
                        profile(
                                page(DataType.Type.SPEED, mapPage = true),
                                page(DataType.Type.SPEED),
                                page(DataType.Type.HEART_RATE, DataType.Type.POWER)
                        )
                )
        assertEquals(2, result.screens.size)
        assertEquals(1, result.screens[0].id)
        assertEquals(2, result.screens[1].id)
    }

    // ── Fallback for empty profiles ───────────────────────────────────────────

    @Test
    fun `profile with no recognisable pages produces single fallback screen`() {
        val result = KarooProfileImporter.import(profile(page("com.unknown.a", mapPage = false)))
        assertEquals(1, result.screens.size)
        assertTrue(result.screens.first().dataFields.isEmpty())
    }

    // ── Zone assignment ───────────────────────────────────────────────────────

    @Test
    fun `fields are assigned to template zones in order`() {
        val result =
                KarooProfileImporter.import(
                        profile(page(DataType.Type.SPEED, DataType.Type.HEART_RATE))
                )
        val fields = result.screens.first().dataFields
        assertEquals("2D_H", fields[0].zoneId) // first zone of 2D template
        assertEquals("2D_L", fields[1].zoneId) // second zone of 2D template
    }
}
