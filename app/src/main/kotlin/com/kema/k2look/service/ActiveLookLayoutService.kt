package com.kema.k2look.service

import android.util.Log
import com.activelook.activelooksdk.Glasses
import com.activelook.activelooksdk.types.ConfigurationDescription
import com.activelook.activelooksdk.types.FreeSpace
import com.kema.k2look.layout.ActiveLookLayout
import com.kema.k2look.layout.LayoutBuilder
import com.kema.k2look.model.DataFieldProfile
import com.kema.k2look.model.VisualizationType
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Service for managing ActiveLook layouts with persistent configuration storage.
 *
 * Each K2Look [DataFieldProfile] is stored as a named **configuration** on the glasses
 * (cfgWrite / layoutSave / gaugeSave).  Switching between already-saved profiles is a
 * single `cfgSet` command — no BLE re-upload required.
 *
 * Fast-path logic:
 *   - On connection: [refreshConfigCache] reads the glasses' config list once.
 *   - On [saveAndActivateProfile]: if the cached version matches profile.modifiedAt, just cfgSet.
 *   - Otherwise: cfgWrite + layoutSave + gaugeSave + cfgSet, then update cache.
 */
class ActiveLookLayoutService(
    private val activeLookService: ActiveLookService
) {

    private val layoutBuilder = LayoutBuilder()

    /**
     * In-memory cache: configName → version stored on glasses.
     * Populated by [refreshConfigCache] on every connection.
     */
    private val configVersionCache = mutableMapOf<String, Long>()

    companion object {
        private const val TAG = "ActiveLookLayoutService"

        const val LAYOUT_ID_BASE = 10
        const val CFG_PREFIX = "K2L"          // 3 chars — leaves 9 chars for profile hash
        const val COMMAND_DELAY_MS = 100L
        private const val FREE_SPACE_TIMEOUT_MS = 5_000L
        private const val CFG_LIST_TIMEOUT_MS   = 5_000L
        private const val MAX_USER_CONFIGS       = 12
        private const val MIN_FREE_SPACE_PERCENT = 10

        private val zoneToLayoutId = mutableMapOf<String, Int>()
        private var nextLayoutId = LAYOUT_ID_BASE

        fun getLayoutIdForZone(zoneId: String): Int =
            zoneToLayoutId.getOrPut(zoneId) { nextLayoutId++ }

        /**
         * Stable 12-char config name derived from [profileId].
         * Format: "K2L" + first 9 hex chars of UUID (without dashes).
         */
        fun configNameFor(profileId: String): String {
            val hex = profileId.replace("-", "").take(9).uppercase()
            return "$CFG_PREFIX$hex"
        }

        /**
         * Lower 32 bits of [DataFieldProfile.modifiedAt] used as the cfgWrite version.
         * Changes whenever the profile is edited, driving the stale-check.
         */
        fun versionFor(profile: DataFieldProfile): Long =
            profile.modifiedAt and 0xFFFF_FFFFL
    }

    // ──────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Main entry point called by [KarooActiveLookBridge] on every profile activation.
     *
     * Fast path  (version match) → single `cfgSet` command.
     * Slow path  (new / modified) → cfgWrite + layoutSave + gaugeSave + cfgSet.
     */
    suspend fun saveAndActivateProfile(profile: DataFieldProfile): Boolean {
        if (!activeLookService.isConnected) {
            Log.w(TAG, "Cannot activate profile: glasses not connected")
            return false
        }
        val glasses = activeLookService.getConnectedGlasses() ?: return false

        val configName = configNameFor(profile.id)
        val version    = versionFor(profile)

        // ── Fast path ──────────────────────────────────────────────────────
        if (configVersionCache[configName] == version) {
            Log.i(TAG, "✅ Config '$configName' up-to-date → cfgSet only")
            glasses.cfgSet(configName)
            return true
        }

        Log.i(TAG, "📤 Uploading config '$configName' (v=$version) to glasses…")

        // ── Guard: free space ──────────────────────────────────────────────
        val freeSpace = withTimeoutOrNull(FREE_SPACE_TIMEOUT_MS) { cfgFreeSpaceAsync(glasses) }
        if (freeSpace != null) {
            val freePct = freeSpace.freeSpace * 100 / freeSpace.totalSize.coerceAtLeast(1)
            Log.d(TAG, "Flash free space: ${freeSpace.freeSpace}B / ${freeSpace.totalSize}B ($freePct%)")
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

        // ── Open config for writing ────────────────────────────────────────
        glasses.cfgWrite(configName, version.toInt(), 0)
        delay(COMMAND_DELAY_MS * 2)

        // ── Save layouts (for first screen) ───────────────────────────────
        val layoutsOk = saveProfileLayouts(profile)

        // ── Save gauges ────────────────────────────────────────────────────
        saveProfileGauges(profile)

        // ── Activate the config ────────────────────────────────────────────
        glasses.cfgSet(configName)
        delay(COMMAND_DELAY_MS)

        if (layoutsOk) {
            configVersionCache[configName] = version
            Log.i(TAG, "✅ Config '$configName' saved and activated")
        } else {
            Log.w(TAG, "⚠️ Config '$configName' partially saved (some layouts failed)")
        }
        return layoutsOk
    }

    /**
     * Populate [configVersionCache] by reading the config list from the glasses.
     * Call this once after every connection.
     */
    suspend fun refreshConfigCache() {
        val glasses = activeLookService.getConnectedGlasses() ?: run {
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
            configs
                .filter { !it.isSystem && it.name.startsWith(CFG_PREFIX) }
                .forEach { configVersionCache[it.name] = it.version }
            Log.i(TAG, "📋 Config cache refreshed: ${configVersionCache.size} K2Look config(s) on glasses: ${configVersionCache.keys}")
        } catch (e: Exception) {
            Log.w(TAG, "Could not refresh config cache: ${e.message}")
        }
    }

    /**
     * Invalidate a profile's entry in the local cache.
     * Call this when a profile is modified so the next activation triggers a re-upload.
     */
    fun invalidateConfig(profileId: String) {
        val name = configNameFor(profileId)
        configVersionCache.remove(name)
        Log.d(TAG, "Invalidated config cache for '$name'")
    }

    /**
     * Display a field value using a pre-saved layout (called at 1 Hz during rides).
     */
    fun displayFieldValue(zoneId: String, value: String) {
        if (!activeLookService.isConnected) return
        val glasses = activeLookService.getConnectedGlasses() ?: return
        val layoutId = getLayoutIdForZone(zoneId)
        try {
            glasses.layoutDisplay(layoutId.toByte(), value)
            Log.v(TAG, "Layout $layoutId (zone $zoneId): '$value'")
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying layout $layoutId: ${e.message}", e)
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────

    /** Save layout definitions for the first screen of [profile]. */
    private suspend fun saveProfileLayouts(profile: DataFieldProfile): Boolean {
        val glasses = activeLookService.getConnectedGlasses() ?: return false
        val screen  = profile.screens.firstOrNull() ?: return false

        val screenLayouts = layoutBuilder.buildScreenLayouts(LAYOUT_ID_BASE, screen)
        var saved = 0
        screenLayouts.forEach { (zoneId, layout) ->
            val id = getLayoutIdForZone(zoneId)
            val corrected = layout.copy(layoutId = id)
            if (saveLayout(glasses, corrected)) {
                saved++
                delay(COMMAND_DELAY_MS)
            }
        }
        val expected = screen.dataFields.size
        Log.i(TAG, "Layouts saved: $saved / $expected")
        return saved == expected
    }

    /** Save gauge definitions for every gauge field across all screens of [profile]. */
    private suspend fun saveProfileGauges(profile: DataFieldProfile) {
        profile.screens.forEach { screen ->
            screen.dataFields.forEach { field ->
                if ((field.visualizationType ?: VisualizationType.TEXT) == VisualizationType.GAUGE) {
                    field.gauge?.let { gauge ->
                        activeLookService.saveGauge(gauge)
                        delay(COMMAND_DELAY_MS)
                    }
                }
            }
        }
    }

    private fun saveLayout(glasses: Glasses, layout: ActiveLookLayout): Boolean {
        return try {
            val layoutParams = com.activelook.activelooksdk.types.LayoutParameters(
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
                        layoutParams.addSubCommandBitmap(cmd.id.toByte(), cmd.x.toShort(), cmd.y.toShort())
                    is com.kema.k2look.layout.GraphicCommand.Text  ->
                        layoutParams.addSubCommandText(cmd.x.toShort(), cmd.y.toShort(), cmd.text)
                    is com.kema.k2look.layout.GraphicCommand.Line  ->
                        layoutParams.addSubCommandLine(cmd.x0.toShort(), cmd.y0.toShort(), cmd.x1.toShort(), cmd.y1.toShort())
                    is com.kema.k2look.layout.GraphicCommand.Circle ->
                        layoutParams.addSubCommandCirc(cmd.x.toShort(), cmd.y.toShort(), cmd.radius.toShort())
                    is com.kema.k2look.layout.GraphicCommand.Rect  ->
                        layoutParams.addSubCommandRect(cmd.x0.toShort(), cmd.y0.toShort(), cmd.x1.toShort(), cmd.y1.toShort())
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

    private suspend fun cfgGetNbAsync(glasses: Glasses): Int =
        suspendCancellableCoroutine { cont ->
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
     * Clear local layout ID mappings and the config version cache.
     * Called on disconnect to force a full re-sync on next connection.
     */
    fun clearLayouts() {
        configVersionCache.clear()
        zoneToLayoutId.clear()
        nextLayoutId = LAYOUT_ID_BASE
        Log.i(TAG, "Layout cache cleared")
    }
}
