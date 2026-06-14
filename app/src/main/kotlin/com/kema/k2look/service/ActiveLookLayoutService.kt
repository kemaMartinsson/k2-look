package com.kema.k2look.service

import com.kema.k2look.service.AppLog as Log
import com.activelook.activelooksdk.Glasses
import com.activelook.activelooksdk.types.ConfigurationDescription
import com.activelook.activelooksdk.types.FreeSpace
import com.activelook.activelooksdk.types.Rotation
import com.kema.k2look.layout.ActiveLookLayout
import com.kema.k2look.layout.DynamicLayoutEngine
import com.kema.k2look.layout.DynamicLayoutRenderer
import com.kema.k2look.layout.LayoutPositionDefaults
import com.kema.k2look.model.DataFieldProfile
import com.kema.k2look.model.IconSize
import com.kema.k2look.model.LayoutScreen
import com.kema.k2look.model.VisualizationType
import com.kema.k2look.util.ValueFormatter
import kotlin.coroutines.resume
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Service for managing ActiveLook layouts with persistent configuration storage.
 *
 * Each K2Look [DataFieldProfile] is stored as a named **configuration** on the glasses (cfgWrite /
 * layoutSave / gaugeSave). Switching between already-saved profiles is a single `cfgSet` command —
 * no BLE re-upload required.
 *
 * Fast-path logic:
 * - On connection: [refreshConfigCache] reads the glasses' config list once.
 * - On [saveAndActivateProfile]: if the cached version matches profile.modifiedAt, just cfgSet.
 * - Otherwise: cfgWrite + layoutSave + gaugeSave + cfgSet, then update cache.
 */
class ActiveLookLayoutService(internal val activeLookService: ActiveLookService) {

    /**
     * Geometry for one field zone as computed from the template. Cached in [screenGeometry] after
     * each [saveProfileLayouts] call so [displayAllFieldValues] uses the exact same coordinates.
     *
     * [x0] and [width] come directly from the template zone, enabling half-width (multi-column)
     * zones to render at their correct horizontal position alongside sibling zones.
     */
    internal data class ScreenFieldGeometry(
            val layoutId: Int,
            val x0: Int,
            val y0: Int,
            val width: Int,
            val height: Int,
            val font: Int,
    )

    /**
     * In-memory cache: configName → version stored on glasses. Populated by [refreshConfigCache] on
     * every connection.
     */
    private val configVersionCache = mutableMapOf<String, Long>()

    /**
     * The config name currently active on the glasses. Tracked so [displayAllFieldValues] can
     * restore it after switching to ALooK for icon rendering.
     */
    internal var activeConfigName: String? = null

    /**
     * zoneId → geometry computed by the last [saveProfileLayouts] run. Used by
     * [displayAllFieldValues] to keep render coordinates in sync with saved layouts.
     */
    internal val screenGeometry = mutableMapOf<String, ScreenFieldGeometry>()

    /**
     * zoneId → the [PendingIcon] rendered in the previous frame. Used to detect when an icon is
     * removed so we can erase the old pixels with a black rect in the ALooK pass.
     */
    private val activeIconsByZone = mutableMapOf<String, DynamicLayoutRenderer.PendingIcon>()

    /**
     * Whether to show the battery overlay. Set by [KarooActiveLookBridge.setBatteryDisplayEnabled].
     * The overlay is driven by [updateBatteryDisplay], not the per-frame render loop.
     */
    var batteryDisplayEnabled: Boolean = true

    companion object {
        private const val TAG = "ActiveLookLayoutService"

        const val LAYOUT_ID_BASE = 10
        const val CFG_PREFIX = "K2L" // 3 chars — leaves 9 chars for profile hash
        const val COMMAND_DELAY_MS = 100L
        private const val FREE_SPACE_TIMEOUT_MS = 5_000L
        private const val CFG_LIST_TIMEOUT_MS = 5_000L
        private const val MAX_USER_CONFIGS = 12
        private const val MIN_FREE_SPACE_PERCENT = 10

        private val zoneToLayoutId = mutableMapOf<String, Int>()
        private var nextLayoutId = LAYOUT_ID_BASE

        // ── Battery dedicated layout (ID 9 — below LAYOUT_ID_BASE=10, never conflicts) ──
        // Matches a normal font-1 ride metric zone: same x0/width as all metric zones, icon at
        // the same viewer-left position (ICON_ABS_X + 15 for 28px icons = 275), value text at
        // txtXWithIcon=178 (font-1 calibrated). Placed at y=203 — just below the top metric zone
        // (3D_FULL_H: y=153 h=50 → clears up to y=203).
        const val BATTERY_LAYOUT_ID = 9
        private const val BATTERY_ZONE_X = 30 // same left edge as all metric zones
        private const val BATTERY_ZONE_Y = 203
        private const val BATTERY_ZONE_WIDTH = 244 // same width as all metric zones
        private const val BATTERY_ZONE_HEIGHT = 30 // font-1 refHeight
        private const val BATTERY_ZONE_TXT_X: Short = 240 // font-1 txtXWithIcon (relative to zone)
        private const val BATTERY_ZONE_TXT_Y: Byte = 25 // font-1 calibrated txtY
        private const val BATTERY_ICON_X: Short = 275 // ICON_ABS_X(260) + 15 for 28px icon
        private const val BATTERY_ICON_Y: Short = 204 // y=203 + (30-28)/2 = 204 (centred in zone)
        private const val BATTERY_ICON_ERASE_X2: Short = 302 // icon right edge (275+27)
        private const val BATTERY_ICON_ERASE_Y2: Short = 231 // icon bottom edge (204+27)
        private const val BATTERY_ICON_NORMAL: Byte = 0
        private const val BATTERY_ICON_LOW: Byte = 1
        private const val BATTERY_LOW_THRESHOLD = 10

        /**
         * Returns a stable layout ID for a (screenId, zoneId) pair. Using screenId in the key
         * ensures screen 1 and screen 2 layouts never share the same ID, even when both screens use
         * the same template (and thus the same zone ID strings).
         */
        fun getLayoutIdForZone(screenId: Int, zoneId: String): Int =
                zoneToLayoutId.getOrPut("s${screenId}_${zoneId}") { nextLayoutId++ }

        /**
         * Stable 11-char config name derived from [profileId]. Format: "K2L" (3) + CRC32 hash of
         * profileId as 8 uppercase hex chars (8) = 11 chars. With NUL terminator = 12 bytes total,
         * fitting the ActiveLook 12-byte field exactly.
         *
         * Uses CRC32 hash rather than truncation so that any two distinct profile IDs always
         * produce distinct config names, regardless of shared prefixes.
         */
        fun configNameFor(profileId: String): String {
            val crc = java.util.zip.CRC32().also { it.update(profileId.toByteArray()) }.value
            val hash = String.format("%08X", crc) // always exactly 8 uppercase hex chars
            return "$CFG_PREFIX$hash" // e.g. "K2LABCD1234" — 11 chars + NUL = 12 bytes ✓
        }

        /**
         * Lower 32 bits of [DataFieldProfile.modifiedAt] used as the cfgWrite version. Changes
         * whenever the profile is edited, driving the stale-check.
         */
        fun versionFor(profile: DataFieldProfile): Long = profile.modifiedAt and 0xFFFF_FFFFL
    }

    // ──────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Main entry point called by [KarooActiveLookBridge] on every profile activation.
     *
     * Fast path (version match) → single `cfgSet` command. Slow path (new / modified) → cfgWrite +
     * layoutSave + gaugeSave + cfgSet.
     */
    suspend fun saveAndActivateProfile(profile: DataFieldProfile): Boolean {
        if (!activeLookService.isConnected) {
            Log.w(TAG, "Cannot activate profile: glasses not connected")
            return false
        }
        val glasses = activeLookService.getConnectedGlasses() ?: return false

        val configName = configNameFor(profile.id)
        val version = versionFor(profile)

        // Precompute layout IDs and geometry BEFORE fast/full path split.
        // This ensures IDs are always assigned in profile.screens list order, regardless of
        // which screen's displayAllFieldValues() is called first after restart.
        precomputeLayoutIdsAndGeometry(profile)

        // ── Fast path ──────────────────────────────────────────────────────
        if (configVersionCache[configName] == version) {
            Log.i(TAG, "✅ Config '$configName' up-to-date → cfgSet only")
            glasses.cfgSet(configName)
            activeConfigName = configName
            return true
        }

        Log.i(TAG, "📤 Uploading config '$configName' (v=$version) to glasses…")

        // ── Guard: free space ──────────────────────────────────────────────
        val freeSpace = withTimeoutOrNull(FREE_SPACE_TIMEOUT_MS) { cfgFreeSpaceAsync(glasses) }
        if (freeSpace != null) {
            val freePct = freeSpace.freeSpace * 100 / freeSpace.totalSize.coerceAtLeast(1)
            Log.d(
                    TAG,
                    "Flash free space: ${freeSpace.freeSpace}B / ${freeSpace.totalSize}B ($freePct%)"
            )
            if (freePct < MIN_FREE_SPACE_PERCENT) {
                Log.w(TAG, "⚠️ Low flash ($freePct%) — evicting least-used config")
                glasses.cfgDeleteLessUsed()
                delay(1000)
            }
        }

        // ── Guard: config count ────────────────────────────────────────────
        val nbConfigs = withTimeoutOrNull(3_000L) { cfgGetNbAsync(glasses) } ?: 0
        if (nbConfigs >= MAX_USER_CONFIGS) {
            Log.w(TAG, "⚠️ At config limit ($nbConfigs) — evicting least-used config")
            glasses.cfgDeleteLessUsed()
            delay(1000)
        }

        // Explicitly remove any previous config with the same name before rewriting.
        // This guarantees stale definitions in a reused config namespace are dropped.
        try {
            glasses.cfgDelete(configName)
            delay(COMMAND_DELAY_MS)
            Log.d(TAG, "Deleted previous config '$configName' before cfgWrite")
        } catch (e: Exception) {
            // If config does not exist yet, delete may fail on some firmware; continue.
            Log.d(TAG, "cfgDelete('$configName') skipped: ${e.message}")
        }

        // ── Open config for writing ────────────────────────────────────────
        try {
            glasses.cfgWrite(configName, version.toInt(), 0)
            delay(COMMAND_DELAY_MS * 2)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open config '$configName' for writing: ${e.message}", e)
            return false
        }

        // ── Save layouts (for first screen) ───────────────────────────────
        val layoutsOk = saveProfileLayouts(profile)

        // ── Save gauges ────────────────────────────────────────────────────
        saveProfileGauges(profile)

        // ── Activate the config ────────────────────────────────────────────
        try {
            glasses.cfgSet(configName)
            activeConfigName = configName
            delay(COMMAND_DELAY_MS)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to activate config '$configName': ${e.message}", e)
            return false
        }

        // ⚠️ CRITICAL: Update cache REGARDLESS of layout save success.
        // If cfgSet() completed, the config IS activated on the glasses, so the cache
        // MUST reflect that. If we skip caching a config that is actually live on the
        // glasses, the next flushToGlasses() will check isProfileSaved() → cache miss →
        // displayAllFieldValues() never called → NO METRICS DISPLAYED.
        configVersionCache[configName] = version
        if (layoutsOk) {
            Log.i(TAG, "✅ Config '$configName' saved and activated")
        } else {
            Log.w(
                    TAG,
                    "⚠️ Config '$configName' partially saved (some layouts failed), but added to cache since cfgSet completed"
            )
        }
        return layoutsOk
    }

    /**
     * Compute layout IDs and zone geometry for every field in every screen, in profile list order.
     *
     * Resetting the map and counter here (before the fast/full path split) guarantees that IDs are
     * always assigned in [DataFieldProfile.screens] order, regardless of which screen happens to
     * call [displayAllFieldValues] first. This prevents the post-restart ID scramble where
     * [displayAllFieldValues] for screen 2 ran before screen 1 and claimed IDs that the glasses had
     * saved for screen 1.
     */
    private fun precomputeLayoutIdsAndGeometry(profile: DataFieldProfile) {
        zoneToLayoutId.clear()
        nextLayoutId = LAYOUT_ID_BASE
        screenGeometry.clear()
        activeIconsByZone.clear()

        profile.screens.forEach { screen ->
            val template = screen.getTemplate()
            screen.dataFields.forEach { field ->
                val zone = template.zones.find { it.id == field.zoneId }
                val zoneX = zone?.x ?: LayoutPositionDefaults.ZONE_X0
                val zoneY = zone?.y ?: 0
                val zoneWidth = zone?.width ?: LayoutPositionDefaults.ZONE_WIDTH
                val zoneHeight = zone?.height ?: 50
                val font = if (field.largeFont) 3 else 2
                val layoutId = getLayoutIdForZone(screen.id, field.zoneId)
                screenGeometry["${screen.id}:${field.zoneId}"] =
                        ScreenFieldGeometry(layoutId, zoneX, zoneY, zoneWidth, zoneHeight, font)
            }
        }
        Log.d(
                TAG,
                "precompute: ${zoneToLayoutId.size} zone(s) across ${profile.screens.size} screen(s) — IDs ${LAYOUT_ID_BASE}–${nextLayoutId - 1}"
        )
    }

    /**
     * Populate [configVersionCache] by reading the config list from the glasses. Call this once
     * after every connection.
     */
    suspend fun refreshConfigCache() {
        val glasses =
                activeLookService.getConnectedGlasses()
                        ?: run {
                            Log.w(TAG, "refreshConfigCache: glasses not connected")
                            return
                        }
        try {
            val configs = withTimeoutOrNull(CFG_LIST_TIMEOUT_MS) { cfgListAsync(glasses) }
            if (configs == null) {
                Log.w(TAG, "cfgList timed out — config cache not refreshed")
                return
            }
            configVersionCache.clear()
            configs.filter { !it.isSystem && it.name.startsWith(CFG_PREFIX) }.forEach {
                configVersionCache[it.name] = it.version
            }
            Log.i(
                    TAG,
                    "📋 Config cache refreshed: ${configVersionCache.size} K2Look config(s) on glasses: ${configVersionCache.keys}"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not refresh config cache: ${e.message}")
        }
    }

    /**
     * Invalidate a profile's entry in the local cache. Call this when a profile is modified so the
     * next activation triggers a re-upload.
     */
    fun invalidateConfig(profileId: String) {
        val name = configNameFor(profileId)
        configVersionCache.remove(name)
        Log.d(TAG, "Invalidated config cache for '$name'")
    }

    /**
     * Display all field values atomically using holdFlush to prevent flickering. [fields] maps
     * zoneId → formatted value string.
     *
     * Each field is rendered via [layoutClearAndDisplayExtended] with a [LayoutExtraCmd] overlay
     * containing unit labels and/or elapsed-time seconds (built by [DynamicLayoutRenderer]). Icons
     * are batched into a second pass under the ALooK config where [imgDisplay] is available.
     *
     * @param fields Map of zoneId → formatted display value.
     * @param screen Active layout screen providing zone geometry and field configuration.
     */
    fun displayAllFieldValues(fields: Map<String, String>, screen: LayoutScreen) {
        if (!activeLookService.isConnected) return
        val glasses = activeLookService.getConnectedGlasses() ?: return
        val pendingIcons = mutableListOf<DynamicLayoutRenderer.PendingIcon>()
        val currentIconsByZone = mutableMapOf<String, DynamicLayoutRenderer.PendingIcon>()
        try {
            glasses.holdFlush(com.activelook.activelooksdk.types.holdFlushAction.HOLD)

            fields.forEach { (zoneId, value) ->
                val layoutId = getLayoutIdForZone(screen.id, zoneId)
                val field = screen.dataFields.find { it.zoneId == zoneId }
                val geometry = screenGeometry["${screen.id}:${zoneId}"]

                if (field == null || geometry == null) {
                    // Fallback: geometry not yet saved — basic display without ExtraCmd
                    glasses.layoutClearAndDisplay(layoutId.toByte(), value)
                    Log.v(TAG, "Layout $layoutId (zone $zoneId) [fallback — no geometry]: '$value'")
                    return@forEach
                }

                val hasIcon =
                        field.showIcon &&
                                (field.dataField.icon28 != null || field.dataField.icon40 != null)

                val iconPxForUnit = if (hasIcon) field.iconSize.pixels else 0
                val isText =
                        field.visualizationType == VisualizationType.TEXT ||
                                field.visualizationType == null
                val normalizedValue =
                        if (isText) stripTrailingUnit(value, field.dataField.unit) else value
                val paddedValue =
                        if (isText) {
                            val maxDigits = ValueFormatter.unitMaxDigits(field.dataField.unit)
                            ValueFormatter.padSpace(normalizedValue, maxDigits)
                        } else normalizedValue
                val (extraCmd, renderValue) =
                        DynamicLayoutRenderer.buildExtraCmd(
                                paddedValue,
                                field.dataField.unit,
                                geometry.font,
                                geometry.height,
                                field.showUnit,
                                iconPxForUnit,
                        )

                glasses.layoutClearAndDisplayExtended(
                        layoutId.toByte(),
                        geometry.x0.toShort(),
                        geometry.y0.toByte(),
                        renderValue,
                        extraCmd,
                )

                // Queue icon for the ALooK pass (imgDisplay requires ALooK config context)
                if (hasIcon) {
                    val iconPx = field.iconSize.pixels
                    val iconId =
                            when (field.iconSize) {
                                IconSize.LARGE -> field.dataField.icon40
                                IconSize.SMALL -> field.dataField.icon28
                            }
                    if (iconId != null) {
                        DynamicLayoutRenderer.queueIcon(
                                pendingIcons,
                                iconId,
                                iconPx,
                                geometry.y0,
                                geometry.height,
                        )
                        currentIconsByZone[zoneId] = pendingIcons.last()
                    }
                }

                // Log.v(TAG, "Layout $layoutId (zone $zoneId font=${geometry.font}): '$renderValue'
                // unit=${field.dataField.unit} icon=$hasIcon")
            }

            // Icon pass: imgDisplay requires ALooK system config context.
            // Also erases icon areas for zones that had an icon last frame but no longer do.
            // Battery has its own layout (ID 9) and is updated independently via
            // updateBatteryDisplay().
            val zonesToErase = activeIconsByZone.keys - currentIconsByZone.keys
            if (pendingIcons.isNotEmpty() || zonesToErase.isNotEmpty()) {
                glasses.cfgSet("ALooK")
                // Erase stale icons by painting a black filled rect over the old icon area
                if (zonesToErase.isNotEmpty()) {
                    glasses.color(0)
                    zonesToErase.forEach { zoneId ->
                        val old = activeIconsByZone[zoneId]!!
                        val x2 = (old.absX + old.iconPx - 1).toShort()
                        val y2 = (old.absY + old.iconPx - 1).toShort()
                        glasses.rectf(old.absX, old.absY, x2, y2)
                        Log.d(
                                TAG,
                                "Erased stale icon at zone=$zoneId x=${old.absX} y=${old.absY} px=${old.iconPx}"
                        )
                    }
                }
                DynamicLayoutRenderer.renderPendingIcons(glasses, pendingIcons)
                // Restore the user config so the next frame's layoutClearAndDisplayExtended
                // calls find our saved layouts — not the ALooK system defaults.
                val restoreName = activeConfigName
                if (restoreName != null) {
                    glasses.cfgSet(restoreName)
                }
            }
            // Update cross-frame icon tracking
            activeIconsByZone.clear()
            activeIconsByZone.putAll(currentIconsByZone)

            glasses.holdFlush(com.activelook.activelooksdk.types.holdFlushAction.FLUSH)
        } catch (e: Exception) {
            Log.e(TAG, "Error in batch display: ${e.message}", e)
            try {
                glasses.holdFlush(com.activelook.activelooksdk.types.holdFlushAction.FLUSH)
            } catch (_: Exception) {}
        }
    }

    private fun stripTrailingUnit(value: String, unit: String): String {
        if (unit.isEmpty() || value == "--" || value == "..." || value == "n/a") {
            return value
        }
        val suffix = " $unit"
        return if (value.endsWith(suffix)) value.removeSuffix(suffix).trimEnd() else value
    }

    // ──────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Save layout definitions for ALL screens of [profile] using [DynamicLayoutEngine] to compute
     * row positions and [DynamicLayoutRenderer] to build each [LayoutParameters].
     *
     * Layout IDs are namespaced by screen ID so that two screens using the same template (same zone
     * IDs) get distinct layout IDs and do not overwrite each other.
     *
     * Row sizes are derived from the template zone heights:
     * - height ≥ 50 px → "large" → font 3
     * - height ≥ 35 px → "medium" → font 2
     * - height < 35 px → "small" → font 1
     *
     * The resulting geometry is cached in [screenGeometry] (keyed by "screenId:zoneId") so
     * [displayAllFieldValues] uses the exact same y0 / height / font values that were saved.
     */
    private suspend fun saveProfileLayouts(profile: DataFieldProfile): Boolean {
        val glasses = activeLookService.getConnectedGlasses() ?: return false
        if (profile.screens.isEmpty()) return false

        // screenGeometry and activeIconsByZone are already cleared and pre-populated by
        // precomputeLayoutIdsAndGeometry() which ran before this call. Do not clear them here
        // — that would lose the geometry data that the fast path relies on.

        // Delete all layouts in this config before saving new ones.
        glasses.layoutDeleteAll()
        delay(COMMAND_DELAY_MS)

        // Clear the display so stale pixels from a previous template do not linger.
        glasses.clear()
        delay(COMMAND_DELAY_MS)

        // Delete all saved gauge definitions so arc pixels from a gauge layout cannot
        // reappear after switching to a non-gauge template.
        glasses.gaugeDelete(0xFF.toByte())
        delay(COMMAND_DELAY_MS)

        var totalSaved = 0
        var totalExpected = 0

        profile.screens.forEach { screen ->
            val template = screen.getTemplate()
            val fields = screen.dataFields
            totalExpected += fields.size

            fields.forEach { field ->
                // Use template zone coordinates directly so multi-column (half-width) zones
                // are placed at their designed x/y position, not recalculated as rows.
                val zone = template.zones.find { it.id == field.zoneId }
                val zoneX = zone?.x ?: LayoutPositionDefaults.ZONE_X0
                val zoneY = zone?.y ?: 0
                val zoneWidth = zone?.width ?: LayoutPositionDefaults.ZONE_WIDTH
                val zoneHeight = zone?.height ?: 50
                val font = if (field.largeFont) 3 else 2
                val hasIcon =
                        field.showIcon &&
                                (field.dataField.icon28 != null || field.dataField.icon40 != null)
                // Screen-namespaced layout ID: screen 1 and screen 2 with the same zone IDs
                // get different layout IDs and co-exist on the glasses.
                val layoutId = getLayoutIdForZone(screen.id, field.zoneId)

                val params =
                        DynamicLayoutRenderer.buildLayoutParams(
                                layoutId = layoutId,
                                x0 = zoneX,
                                y0 = zoneY,
                                width = zoneWidth,
                                zoneHeight = zoneHeight,
                                font = font,
                                hasIcon = hasIcon,
                        )
                if (saveLayoutDirect(glasses, layoutId, params)) {
                    totalSaved++
                    delay(COMMAND_DELAY_MS)
                }
                // Cache geometry with screen-namespaced key so displayAllFieldValues picks up
                // the correct coordinates for whichever screen is currently active.
                screenGeometry["${screen.id}:${field.zoneId}"] =
                        ScreenFieldGeometry(layoutId, zoneX, zoneY, zoneWidth, zoneHeight, font)
            }

            Log.i(TAG, "Screen ${screen.id}: ${fields.size} layout(s) saved")
        }

        Log.i(
                TAG,
                "Layouts saved: $totalSaved / $totalExpected across ${profile.screens.size} screen(s)"
        )

        // Always save the battery layout so it is available regardless of profile content.
        saveBatteryLayout(glasses)
        delay(COMMAND_DELAY_MS)

        return totalSaved == totalExpected
    }

    /**
     * Save the battery text layout (ID [BATTERY_LAYOUT_ID]=9) inside the currently open cfgWrite
     * context. Must be called from within [saveProfileLayouts] before [cfgSet] is issued.
     *
     * The layout covers only the percentage text zone (x=100, y=203, 100×30 px, font 1). The
     * battery icon is drawn separately via [updateBatteryDisplay] using imgDisplay in ALooK
     * context. The two elements never overlap so neither can clobber the other.
     */
    private fun saveBatteryLayout(glasses: Glasses) {
        try {
            val params =
                    com.activelook.activelooksdk.types.LayoutParameters(
                            BATTERY_LAYOUT_ID.toByte(),
                            BATTERY_ZONE_X.toShort(), // x0
                            BATTERY_ZONE_Y
                                    .toByte(), // y0 (203 → signed byte -53; firmware treats as
                            // unsigned 203)
                            BATTERY_ZONE_WIDTH.toShort(), // width
                            BATTERY_ZONE_HEIGHT.toByte(), // height
                            15.toByte(), // fg WHITE
                            0.toByte(), // bg BLACK
                            1.toByte(), // font 1
                            true, // textValid
                            BATTERY_ZONE_TXT_X, // textX (right-aligned in 100px zone)
                            BATTERY_ZONE_TXT_Y, // textY
                            com.activelook.activelooksdk.types.Rotation.TOP_LR,
                            true, // opacity — zone clear on each update
                    )
            glasses.layoutSave(params)
            Log.d(TAG, "✓ Battery layout $BATTERY_LAYOUT_ID saved")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving battery layout: ${e.message}", e)
        }
    }

    /**
     * Render or erase the battery overlay (icon + percentage text).
     *
     * - Icon at ([BATTERY_ICON_X], [BATTERY_ICON_Y]) via imgDisplay in ALooK context.
     * - Text via [layoutClearAndDisplay] on layout [BATTERY_LAYOUT_ID] in the K2L config.
     *
     * Call this whenever the battery level changes or [batteryDisplayEnabled] is toggled. It is NOT
     * called per-frame — only on change — so it never interferes with the metric render loop.
     *
     * @param level BLE battery level 0–100, or -1 to erase the overlay.
     */
    fun updateBatteryDisplay(level: Int) {
        if (!activeLookService.isConnected) return
        val glasses = activeLookService.getConnectedGlasses() ?: return
        val configName = activeConfigName ?: return

        if (!batteryDisplayEnabled || level < 0) {
            // Erase icon pixels in ALooK context, then clear text zone via layout
            glasses.cfgSet("ALooK")
            glasses.color(0)
            glasses.rectf(
                    BATTERY_ICON_X,
                    BATTERY_ICON_Y,
                    BATTERY_ICON_ERASE_X2,
                    BATTERY_ICON_ERASE_Y2
            )
            glasses.cfgSet(configName)
            glasses.layoutClearAndDisplay(BATTERY_LAYOUT_ID.toByte(), "")
            Log.d(TAG, "Battery overlay erased")
            return
        }

        val iconId = if (level < BATTERY_LOW_THRESHOLD) BATTERY_ICON_LOW else BATTERY_ICON_NORMAL
        glasses.cfgSet("ALooK")
        glasses.imgDisplay(iconId, BATTERY_ICON_X, BATTERY_ICON_Y)
        glasses.cfgSet(configName)
        glasses.layoutClearAndDisplay(BATTERY_LAYOUT_ID.toByte(), "${level}%")
        Log.d(TAG, "Battery overlay updated: $level%")
    }

    // ──────────────────────────────────────────────────────────────────────
    // Layout size / font helpers
    // ──────────────────────────────────────────────────────────────────────

    /** Maps a zone pixel height to a [DynamicLayoutEngine] size label. */
    private fun heightToSize(height: Int): String =
            when {
                height >= 50 -> "large"
                height >= 35 -> "medium"
                else -> "small"
            }

    /**
     * Maps a [DynamicLayoutEngine] size label to the corresponding ActiveLook font number. Mirrors
     * the size→height mapping in [DynamicLayoutEngine] and the calibrated font configs in
     * [LayoutPositionDefaults].
     */
    private fun sizeToFont(size: String): Int =
            when (size) {
                "large" -> 3
                "medium" -> 2
                else -> 1
            }

    /**
     * Saves a [LayoutParameters] object directly to the glasses without going through
     * [ActiveLookLayout]. Used by [saveProfileLayouts] when geometry is already computed by
     * [DynamicLayoutEngine] + [DynamicLayoutRenderer].
     */
    private fun saveLayoutDirect(
            glasses: Glasses,
            layoutId: Int,
            params: com.activelook.activelooksdk.types.LayoutParameters,
    ): Boolean =
            try {
                glasses.layoutSave(params)
                Log.d(TAG, "✓ Layout $layoutId saved (DynamicEngine)")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error saving layout $layoutId: ${e.message}", e)
                false
            }

    /** Save gauge definitions for every gauge field across all screens of [profile]. */
    private suspend fun saveProfileGauges(profile: DataFieldProfile) {
        profile.screens.forEach { screen ->
            screen.dataFields.forEach { field ->
                if ((field.visualizationType ?: VisualizationType.TEXT) == VisualizationType.GAUGE
                ) {
                    field.gauge?.let { gauge ->
                        // Re-centre the gauge arc in the zone it is assigned to.
                        // screenGeometry is populated by saveProfileLayouts which runs first,
                        // so the geometry is available here.
                        val geom = screenGeometry["${screen.id}:${field.zoneId}"]
                        val centeredGauge =
                                if (geom != null) {
                                    gauge.copy(
                                            centerX = geom.x0 + geom.width / 2,
                                            centerY = geom.y0 + geom.height / 2
                                    )
                                } else gauge
                        activeLookService.saveGauge(centeredGauge)
                        delay(COMMAND_DELAY_MS)
                    }
                }
            }
        }
    }

    private fun saveLayout(glasses: Glasses, layout: ActiveLookLayout): Boolean {
        return try {
            val layoutParams =
                    com.activelook.activelooksdk.types.LayoutParameters(
                            layout.layoutId.toByte(),
                            layout.clippingRegion.x.toShort(),
                            layout.clippingRegion.y.toByte(),
                            layout.clippingRegion.width.toShort(),
                            layout.clippingRegion.height.toByte(),
                            layout.foreColor.toByte(),
                            layout.backColor.toByte(),
                            layout.font.toByte(),
                            true,
                            layout.textConfig.x.toShort(),
                            layout.textConfig.y.toByte(),
                            com.activelook.activelooksdk.types.Rotation.TOP_LR,
                            layout.textConfig.opacity
                    )
            layout.additionalCommands.forEach { cmd ->
                when (cmd) {
                    is com.kema.k2look.layout.GraphicCommand.Image ->
                            layoutParams.addSubCommandBitmap(
                                    cmd.id.toByte(),
                                    cmd.x.toShort(),
                                    cmd.y.toShort()
                            )
                    is com.kema.k2look.layout.GraphicCommand.Text ->
                            layoutParams.addSubCommandText(
                                    cmd.x.toShort(),
                                    cmd.y.toShort(),
                                    cmd.text
                            )
                    is com.kema.k2look.layout.GraphicCommand.Line ->
                            layoutParams.addSubCommandLine(
                                    cmd.x0.toShort(),
                                    cmd.y0.toShort(),
                                    cmd.x1.toShort(),
                                    cmd.y1.toShort()
                            )
                    is com.kema.k2look.layout.GraphicCommand.Circle ->
                            layoutParams.addSubCommandCirc(
                                    cmd.x.toShort(),
                                    cmd.y.toShort(),
                                    cmd.radius.toShort()
                            )
                    is com.kema.k2look.layout.GraphicCommand.Rect ->
                            layoutParams.addSubCommandRect(
                                    cmd.x0.toShort(),
                                    cmd.y0.toShort(),
                                    cmd.x1.toShort(),
                                    cmd.y1.toShort()
                            )
                    is com.kema.k2look.layout.GraphicCommand.FontChange ->
                            layoutParams.addSubCommandFont(cmd.fontId.toByte())
                }
            }
            glasses.layoutSave(layoutParams)
            Log.d(TAG, "✓ Layout ${layout.layoutId} saved")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving layout ${layout.layoutId}: ${e.message}", e)
            false
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Coroutine wrappers for callback-based SDK calls
    // ──────────────────────────────────────────────────────────────────────

    private suspend fun cfgFreeSpaceAsync(glasses: Glasses): FreeSpace =
            suspendCancellableCoroutine { cont ->
                glasses.cfgFreeSpace { fs -> if (cont.isActive) cont.resume(fs) }
            }

    private suspend fun cfgGetNbAsync(glasses: Glasses): Int = suspendCancellableCoroutine { cont ->
        glasses.cfgGetNb { nb -> if (cont.isActive) cont.resume(nb) }
    }

    private suspend fun cfgListAsync(glasses: Glasses): List<ConfigurationDescription> =
            suspendCancellableCoroutine { cont ->
                glasses.cfgList { list -> if (cont.isActive) cont.resume(list) }
            }

    // ──────────────────────────────────────────────────────────────────────
    // Legacy / compatibility
    // ──────────────────────────────────────────────────────────────────────

    fun isProfileSaved(profileId: String): Boolean =
            configVersionCache.containsKey(configNameFor(profileId))

    /**
     * Clear local layout ID mappings and the config version cache. Called on disconnect to force a
     * full re-sync on next connection.
     */
    fun clearLayouts() {
        configVersionCache.clear()
        zoneToLayoutId.clear()
        nextLayoutId = LAYOUT_ID_BASE
        screenGeometry.clear()
        activeConfigName = null
        Log.i(TAG, "Layout cache cleared")
    }

    // ── Bar visualization ──────────────────────────────────────────────────
    // Moved to ActiveLookVisualizationRenderers.kt as extension functions:
    //   fun ActiveLookLayoutService.displayBarAtZone(...)
    //   fun ActiveLookLayoutService.displayZoneCircles(...)
}

