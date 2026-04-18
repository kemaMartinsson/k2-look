# Radar Warning Overlay Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show a warning icon on the ActiveLook display (viewer bottom-right) when the Karoo radar detects an approaching vehicle — small icon on detection, large icon when TTA ≤ 5s — controlled by a global persistent toggle.

**Architecture:** `RadarWarningController` is a pure state machine receiving radar data and invoking render/erase lambdas. No coroutines needed — fully event-driven per radar packet. `KarooActiveLookBridge` owns the lambdas and wires them to `ActiveLookService`. The global toggle lives in `SettingsRepository` and is exposed in `LayoutBuilderViewModel` and `DataFieldBuilderTab`.

**Tech Stack:** Kotlin, Android BLE (`Glasses.imgStream`/`rectf`), `SharedPreferences`, `StateFlow`, Jetpack Compose `Switch`

**Spec:** `docs/superpowers/specs/2026-04-18-radar-warning-design.md`

---

## File Map

| File | Action | Responsibility |
|------|--------|----------------|
| `service/RadarWarningController.kt` | **Create** | State machine: HIDDEN / VISIBLE_SMALL / VISIBLE_LARGE |
| `service/KarooActiveLookBridge.kt` | **Modify** | Construct controller, wire `observeRadar()`, expose `setRadarWarningEnabled`, reset on disconnect |
| `data/SettingsRepository.kt` | **Modify** | Add `radarWarningEnabled` StateFlow + setter |
| `viewmodel/LayoutBuilderViewModel.kt` | **Modify** | Add `radarWarningEnabled` to `UiState`, subscribe to setting, expose setter, wire to bridge |
| `screens/DataFieldBuilderTab.kt` | **Modify** | Add `Switch` row after `ProfileSelectorCard` |
| `test/service/RadarWarningControllerTest.kt` | **Create** | Unit tests for state machine |

---

## Task 1: `RadarWarningController` — state machine

**Files:**
- Create: `app/src/main/kotlin/com/kema/k2look/service/RadarWarningController.kt`
- Create: `app/src/test/kotlin/com/kema/k2look/service/RadarWarningControllerTest.kt`

- [ ] **Step 1.1: Write the failing tests**

Create `app/src/test/kotlin/com/kema/k2look/service/RadarWarningControllerTest.kt`:

```kotlin
package com.kema.k2look.service

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RadarWarningControllerTest {

    private val renderSmallCalls = mutableListOf<String>()
    private val eraseSmallCalls = mutableListOf<String>()
    private val renderLargeCalls = mutableListOf<String>()
    private val eraseLargeCalls = mutableListOf<String>()

    private lateinit var controller: RadarWarningController

    @Before
    fun setUp() {
        controller = RadarWarningController(
            renderSmall = { renderSmallCalls += "render_small" },
            eraseSmall  = { eraseSmallCalls  += "erase_small"  },
            renderLarge = { renderLargeCalls += "render_large" },
            eraseLarge  = { eraseLargeCalls  += "erase_large"  }
        )
    }

    // ── Detection: HIDDEN → VISIBLE_SMALL ────────────────────────────────────

    @Test
    fun `threat gt 0 with target renders small icon`() {
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        assertEquals(listOf("render_small"), renderSmallCalls)
        assertEquals(emptyList<String>(), renderLargeCalls)
    }

    @Test
    fun `threat 0 from hidden stays hidden`() {
        controller.onRadarUpdate(threatLevel = 0, closestRangeM = null)
        assertEquals(emptyList<String>(), renderSmallCalls)
        assertEquals(emptyList<String>(), renderLargeCalls)
        assertEquals(emptyList<String>(), eraseSmallCalls)
        assertEquals(emptyList<String>(), eraseLargeCalls)
    }

    // ── Escalation: VISIBLE_SMALL → VISIBLE_LARGE ────────────────────────────

    @Test
    fun `escalates to large icon when TTA within 5 seconds`() {
        // First packet: car at 200m — enters VISIBLE_SMALL
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        renderSmallCalls.clear()

        // Second packet 1s later: car at 160m → velocity = 40m/s → TTA = 160/40 = 4s ≤ 5s
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 160f, elapsedMs = 1000L)
        assertEquals(listOf("erase_small"), eraseSmallCalls)
        assertEquals(listOf("render_large"), renderLargeCalls)
    }

    @Test
    fun `stays small when TTA above 5 seconds`() {
        // 200m, then 180m in 1s → velocity = 20m/s → TTA = 180/20 = 9s > 5s
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        renderSmallCalls.clear()

        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 180f, elapsedMs = 1000L)
        assertEquals(emptyList<String>(), eraseSmallCalls)
        assertEquals(emptyList<String>(), renderLargeCalls)
    }

    // ── De-escalation: VISIBLE_LARGE → VISIBLE_SMALL ─────────────────────────

    @Test
    fun `de-escalates to small when TTA rises above 5 seconds`() {
        // Reach VISIBLE_LARGE state
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 160f, elapsedMs = 1000L)
        eraseLargeCalls.clear(); renderSmallCalls.clear()

        // Car slows: 160m → 158m in 1s → velocity = 2m/s → TTA = 79s > 5s
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 158f, elapsedMs = 1000L)
        assertEquals(listOf("erase_large"), eraseLargeCalls)
        assertEquals(listOf("render_small"), renderSmallCalls)
    }

    // ── Clearance: any VISIBLE → HIDDEN ──────────────────────────────────────

    @Test
    fun `threat 0 from VISIBLE_SMALL erases small and returns to HIDDEN`() {
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        renderSmallCalls.clear()

        controller.onRadarUpdate(threatLevel = 0, closestRangeM = null)
        assertEquals(listOf("erase_small"), eraseSmallCalls)
        assertEquals(emptyList<String>(), renderSmallCalls)
    }

    @Test
    fun `threat 0 from VISIBLE_LARGE erases large and returns to HIDDEN`() {
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 160f, elapsedMs = 1000L)
        eraseLargeCalls.clear()

        controller.onRadarUpdate(threatLevel = 0, closestRangeM = null)
        assertEquals(listOf("erase_large"), eraseLargeCalls)
    }

    // ── setEnabled ───────────────────────────────────────────────────────────

    @Test
    fun `setEnabled false from VISIBLE_SMALL erases and suppresses future updates`() {
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        eraseSmallCalls.clear(); renderSmallCalls.clear()

        controller.setEnabled(false)
        assertEquals(listOf("erase_small"), eraseSmallCalls)

        // Next update should be suppressed
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 180f, elapsedMs = 1000L)
        assertEquals(emptyList<String>(), renderSmallCalls)
    }

    @Test
    fun `setEnabled true allows subsequent updates`() {
        controller.setEnabled(false)
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        assertEquals(emptyList<String>(), renderSmallCalls)

        controller.setEnabled(true)
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        assertEquals(listOf("render_small"), renderSmallCalls)
    }

    // ── reset ─────────────────────────────────────────────────────────────────

    @Test
    fun `reset from VISIBLE_LARGE transitions to HIDDEN without erasing`() {
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 160f, elapsedMs = 1000L)
        eraseLargeCalls.clear()

        controller.reset()
        // reset does NOT erase (glasses may be disconnected)
        assertEquals(emptyList<String>(), eraseLargeCalls)

        // After reset, a fresh detection starts from HIDDEN
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f)
        assertEquals(listOf("render_small"), renderSmallCalls)
    }

    // ── No-op transitions ────────────────────────────────────────────────────

    @Test
    fun `repeated updates in same state produce no render calls`() {
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 200f) // → VISIBLE_SMALL
        renderSmallCalls.clear()

        // Same state again (TTA still > 5s)
        controller.onRadarUpdate(threatLevel = 1, closestRangeM = 195f, elapsedMs = 1000L)
        assertEquals(emptyList<String>(), renderSmallCalls)
        assertEquals(emptyList<String>(), renderLargeCalls)
    }
}
```

- [ ] **Step 1.2: Run tests to verify they all fail**

```
.\gradlew.bat :app:testDebugUnitTest --tests "com.kema.k2look.service.RadarWarningControllerTest" -x lint 2>&1 | Select-Object -Last 20
```

Expected: compilation failure — `RadarWarningController` doesn't exist yet.

- [ ] **Step 1.3: Create `RadarWarningController.kt`**

Create `app/src/main/kotlin/com/kema/k2look/service/RadarWarningController.kt`:

```kotlin
package com.kema.k2look.service

import android.util.Log

/**
 * State machine that drives the radar warning overlay on the ActiveLook display.
 *
 * Receives radar data updates from [KarooActiveLookBridge] and transitions between:
 * - [State.HIDDEN]         — no vehicle detected
 * - [State.VISIBLE_SMALL]  — vehicle detected; warning_white_28 shown
 * - [State.VISIBLE_LARGE]  — vehicle within [TTA_THRESHOLD_S] seconds; warning_white_40 shown
 *
 * Render/erase is delegated to injected lambdas — no direct BLE or asset access here.
 * All transitions are synchronous within the calling coroutine; no background job is needed.
 */
class RadarWarningController(
    private val renderSmall: () -> Unit,
    private val eraseSmall: () -> Unit,
    private val renderLarge: () -> Unit,
    private val eraseLarge: () -> Unit
) {
    private enum class State { HIDDEN, VISIBLE_SMALL, VISIBLE_LARGE }

    private var state = State.HIDDEN
    private var enabled = true

    // Velocity estimation — last sample
    private var prevRangeM: Float = 0f
    private var prevTimeMs: Long = 0L

    /**
     * Called on every [StreamState.Streaming] radar packet.
     *
     * @param threatLevel  Value of [DataType.Field.RADAR_THREAT_LEVEL]. 0 = no threat.
     * @param closestRangeM  Closest target range in metres, or null if no targets present.
     * @param elapsedMs  Override for elapsed time since last sample (used in tests). Pass 0 to
     *                   use wall clock (default).
     */
    fun onRadarUpdate(
        threatLevel: Int,
        closestRangeM: Float?,
        elapsedMs: Long = 0L
    ) {
        if (!enabled) return

        val nowMs = if (elapsedMs > 0L) prevTimeMs + elapsedMs else System.currentTimeMillis()
        val tta = computeTta(closestRangeM, nowMs)

        // Update velocity sample for next call
        if (closestRangeM != null && closestRangeM > 0f) {
            prevRangeM = closestRangeM
            prevTimeMs = nowMs
        }

        val targetState = when {
            threatLevel == 0 || closestRangeM == null -> State.HIDDEN
            tta <= TTA_THRESHOLD_S                    -> State.VISIBLE_LARGE
            else                                      -> State.VISIBLE_SMALL
        }

        transition(targetState)
    }

    /** Live-toggle the feature. Erases current icon immediately if disabling. */
    fun setEnabled(enabled: Boolean) {
        if (this.enabled == enabled) return
        this.enabled = enabled
        if (!enabled) {
            eraseCurrentIcon()
            state = State.HIDDEN
            clearVelocitySample()
        }
        Log.i(TAG, "Radar warning ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Called on glasses disconnect. Resets state without attempting to erase
     * (glasses are gone). Safe to call when already HIDDEN.
     */
    fun reset() {
        state = State.HIDDEN
        clearVelocitySample()
        Log.d(TAG, "RadarWarningController reset")
    }

    // ── Private ──────────────────────────────────────────────────────────────

    private fun transition(target: State) {
        if (target == state) return
        Log.d(TAG, "Radar warning: $state → $target")
        when (state to target) {
            State.HIDDEN         to State.VISIBLE_SMALL  -> renderSmall()
            State.HIDDEN         to State.VISIBLE_LARGE  -> renderLarge()
            State.VISIBLE_SMALL  to State.VISIBLE_LARGE  -> { eraseSmall(); renderLarge() }
            State.VISIBLE_LARGE  to State.VISIBLE_SMALL  -> { eraseLarge(); renderSmall() }
            State.VISIBLE_SMALL  to State.HIDDEN         -> eraseSmall()
            State.VISIBLE_LARGE  to State.HIDDEN         -> eraseLarge()
        }
        state = target
    }

    private fun eraseCurrentIcon() {
        when (state) {
            State.VISIBLE_SMALL -> eraseSmall()
            State.VISIBLE_LARGE -> eraseLarge()
            State.HIDDEN        -> { /* nothing to erase */ }
        }
    }

    private fun computeTta(closestRangeM: Float?, nowMs: Long): Float {
        if (closestRangeM == null || closestRangeM <= 0f) return Float.MAX_VALUE
        if (prevTimeMs == 0L) return Float.MAX_VALUE          // no prior sample
        val elapsedS = (nowMs - prevTimeMs) / 1000f
        if (elapsedS <= 0f) return Float.MAX_VALUE
        val velocityMps = (prevRangeM - closestRangeM) / elapsedS
        if (velocityMps <= 0f) return Float.MAX_VALUE         // not closing
        return closestRangeM / velocityMps
    }

    private fun clearVelocitySample() {
        prevRangeM = 0f
        prevTimeMs = 0L
    }

    private companion object {
        private const val TAG = "RadarWarningController"
        private const val TTA_THRESHOLD_S = 5.0f
    }
}
```

- [ ] **Step 1.4: Run tests to verify they pass**

```
.\gradlew.bat :app:testDebugUnitTest --tests "com.kema.k2look.service.RadarWarningControllerTest" -x lint 2>&1 | Select-Object -Last 20
```

Expected: `BUILD SUCCESSFUL` — all tests pass.

- [ ] **Step 1.5: Commit**

```
git add app/src/main/kotlin/com/kema/k2look/service/RadarWarningController.kt
git add app/src/test/kotlin/com/kema/k2look/service/RadarWarningControllerTest.kt
git commit -m "feat: add RadarWarningController state machine"
```

---

## Task 2: `SettingsRepository` — add `radarWarningEnabled`

**Files:**
- Modify: `app/src/main/kotlin/com/kema/k2look/data/SettingsRepository.kt`

- [ ] **Step 2.1: Add the setting**

In `SettingsRepository.kt`, add the constant, StateFlow, and setter. The full file after changes:

```kotlin
package com.kema.k2look.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for app-level settings stored in SharedPreferences.
 */
class SettingsRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "k2look_settings"
        private const val KEY_KAROO_SYNC = "karoo_sync_enabled"
        private const val KEY_RADAR_WARNING = "radar_warning_enabled"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _karooSyncEnabled = MutableStateFlow(prefs.getBoolean(KEY_KAROO_SYNC, true))
    val karooSyncEnabled: StateFlow<Boolean> = _karooSyncEnabled.asStateFlow()

    fun setKarooSyncEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_KAROO_SYNC, enabled) }
        _karooSyncEnabled.value = enabled
    }

    private val _radarWarningEnabled = MutableStateFlow(prefs.getBoolean(KEY_RADAR_WARNING, true))
    val radarWarningEnabled: StateFlow<Boolean> = _radarWarningEnabled.asStateFlow()

    fun setRadarWarningEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_RADAR_WARNING, enabled) }
        _radarWarningEnabled.value = enabled
    }
}
```

- [ ] **Step 2.2: Build to verify no compile errors**

```
.\gradlew.bat :app:compileDebugKotlin -x lint 2>&1 | Select-Object -Last 10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2.3: Commit**

```
git add app/src/main/kotlin/com/kema/k2look/data/SettingsRepository.kt
git commit -m "feat: add radarWarningEnabled setting to SettingsRepository"
```

---

## Task 3: `LayoutBuilderViewModel` — wire setting to UI state and bridge

**Files:**
- Modify: `app/src/main/kotlin/com/kema/k2look/viewmodel/LayoutBuilderViewModel.kt`

- [ ] **Step 3.1: Add `radarWarningEnabled` to `UiState`**

In `LayoutBuilderViewModel.kt`, find the `data class UiState(` block and add the field:

```kotlin
    data class UiState(
            val profiles: List<DataFieldProfile> = emptyList(),
            val activeProfile: DataFieldProfile? = null,
            val selectedScreen: Int = 1,
            val isLoading: Boolean = false,
            val error: String? = null,
            val successMessage: String? = null,
            val showProfileManagement: Boolean = false,
            val isGlassesConnected: Boolean = false,
            val activeRideProfile: RideProfile? = null,
            val isRiding: Boolean = false,
            val karooSyncEnabled: Boolean = true,
            val radarWarningEnabled: Boolean = true    // ← add this line
    )
```

- [ ] **Step 3.2: Subscribe to `radarWarningEnabled` in `init`**

In the `init` block, immediately after the `karooSyncEnabled` subscription (which ends with `.launchIn(viewModelScope)`), add:

```kotlin
        _uiState.value =
                _uiState.value.copy(radarWarningEnabled = settingsRepository.radarWarningEnabled.value)
        settingsRepository
                .radarWarningEnabled
                .onEach { enabled ->
                    _uiState.value = _uiState.value.copy(radarWarningEnabled = enabled)
                    bridge?.setRadarWarningEnabled(enabled)
                }
                .launchIn(viewModelScope)
```

- [ ] **Step 3.3: Propagate setting when bridge is set**

In `setBridge()`, after the existing `bridge.setProfileLookup { … }` registration block, add:

```kotlin
        // Sync radar warning setting to bridge on connect
        bridge.setRadarWarningEnabled(_uiState.value.radarWarningEnabled)
```

- [ ] **Step 3.4: Expose setter**

Add a new function at the end of the class body, immediately before the `companion object`:

```kotlin
    fun setRadarWarningEnabled(enabled: Boolean) {
        settingsRepository.setRadarWarningEnabled(enabled)
        Log.i(TAG, "Radar warning ${if (enabled) "enabled" else "disabled"}")
    }
```

- [ ] **Step 3.5: Build to verify no compile errors**

```
.\gradlew.bat :app:compileDebugKotlin -x lint 2>&1 | Select-Object -Last 10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3.6: Commit**

```
git add app/src/main/kotlin/com/kema/k2look/viewmodel/LayoutBuilderViewModel.kt
git commit -m "feat: wire radarWarningEnabled setting through LayoutBuilderViewModel"
```

---

## Task 4: `KarooActiveLookBridge` — construct controller and wire radar

**Files:**
- Modify: `app/src/main/kotlin/com/kema/k2look/service/KarooActiveLookBridge.kt`

- [ ] **Step 4.1: Add imports and bitmap cache fields**

After the existing imports block, verify these are present (add if missing):

```kotlin
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.activelook.activelooksdk.types.ImgStreamFormat
import com.activelook.activelooksdk.types.holdFlushAction
```

After `private var activeProfileHasRadar = false`, add:

```kotlin
    // ── Radar warning overlay ────────────────────────────────────────────────
    private var warningBitmapSmall: Bitmap? = null
    private var warningBitmapLarge: Bitmap? = null
    private var radarWarningEnabled: Boolean = true
```

- [ ] **Step 4.2: Add bitmap helpers**

Add two private helper functions anywhere after the field declarations (e.g. before `observeRadar()`):

```kotlin
    private fun getWarningBitmapSmall(): Bitmap =
        warningBitmapSmall ?: BitmapFactory.decodeStream(
            context.assets.open("warning_white_28.png")
        ).also { warningBitmapSmall = it }

    private fun getWarningBitmapLarge(): Bitmap =
        warningBitmapLarge ?: BitmapFactory.decodeStream(
            context.assets.open("warning_white_40.png")
        ).also { warningBitmapLarge = it }
```

- [ ] **Step 4.3: Add render/erase lambdas**

Add these four private properties after the bitmap helpers:

```kotlin
    private val renderWarningSmall: () -> Unit = {
        try {
            activeLookService.getGlasses()
                ?.imgStream(getWarningBitmapSmall(), ImgStreamFormat.MONO_4BPP_HEATSHRINK, 30, 25)
        } catch (e: Exception) {
            Log.e(TAG, "renderWarningSmall failed: ${e.message}", e)
        }
    }

    private val eraseWarningSmall: () -> Unit = {
        try {
            activeLookService.getGlasses()?.let { g ->
                g.holdFlush(holdFlushAction.HOLD)
                g.color(0)
                g.rectf(30, 25, 57, 52)
                g.color(15)
                g.holdFlush(holdFlushAction.FLUSH)
            }
        } catch (e: Exception) {
            Log.e(TAG, "eraseWarningSmall failed: ${e.message}", e)
        }
    }

    private val renderWarningLarge: () -> Unit = {
        try {
            activeLookService.getGlasses()
                ?.imgStream(getWarningBitmapLarge(), ImgStreamFormat.MONO_4BPP_HEATSHRINK, 30, 25)
        } catch (e: Exception) {
            Log.e(TAG, "renderWarningLarge failed: ${e.message}", e)
        }
    }

    private val eraseWarningLarge: () -> Unit = {
        try {
            activeLookService.getGlasses()?.let { g ->
                g.holdFlush(holdFlushAction.HOLD)
                g.color(0)
                g.rectf(30, 25, 69, 64)
                g.color(15)
                g.holdFlush(holdFlushAction.FLUSH)
            }
        } catch (e: Exception) {
            Log.e(TAG, "eraseWarningLarge failed: ${e.message}", e)
        }
    }
```

- [ ] **Step 4.4: Construct `RadarWarningController`**

Add after the lambda properties:

```kotlin
    private val radarWarningController = RadarWarningController(
        renderSmall = renderWarningSmall,
        eraseSmall  = eraseWarningSmall,
        renderLarge = renderWarningLarge,
        eraseLarge  = eraseWarningLarge
    )
```

- [ ] **Step 4.5: Call controller in `observeRadar()`**

In `observeRadar()`, inside the `is StreamState.Streaming ->` block, after `currentData.radarClosestRange = ...`, add:

```kotlin
                        if (radarWarningEnabled) {
                            radarWarningController.onRadarUpdate(threat, closest?.toFloat())
                        }
```

- [ ] **Step 4.6: Expose `setRadarWarningEnabled`**

Add a public function near `setDisplayOn`:

```kotlin
    /** Enable or disable the radar warning overlay. Propagated to the controller immediately. */
    fun setRadarWarningEnabled(enabled: Boolean) {
        radarWarningEnabled = enabled
        radarWarningController.setEnabled(enabled)
        Log.d(TAG, "Radar warning overlay: ${if (enabled) "enabled" else "disabled"}")
    }
```

- [ ] **Step 4.7: Reset controller on disconnect**

Find the function `onGlassesDisconnected` (or equivalent disconnect handler). Add at the start of its body:

```kotlin
        radarWarningController.reset()
        warningBitmapSmall = null
        warningBitmapLarge = null
```

- [ ] **Step 4.8: Build to verify no compile errors**

```
.\gradlew.bat :app:compileDebugKotlin -x lint 2>&1 | Select-Object -Last 10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4.9: Commit**

```
git add app/src/main/kotlin/com/kema/k2look/service/KarooActiveLookBridge.kt
git commit -m "feat: wire RadarWarningController into KarooActiveLookBridge"
```

---

## Task 5: `DataFieldBuilderTab` — add `Switch` toggle

**Files:**
- Modify: `app/src/main/kotlin/com/kema/k2look/screens/DataFieldBuilderTab.kt`

- [ ] **Step 5.1: Add `Switch` import**

At the top of `DataFieldBuilderTab.kt`, verify `androidx.compose.material3.Switch` is imported. If not, add:

```kotlin
import androidx.compose.material3.Switch
```

- [ ] **Step 5.2: Insert the toggle row**

In `DataFieldBuilderTab.kt`, find the block inside the main `Column` that begins:

```kotlin
        // Profile Selector
        ProfileSelectorCard(
```

Immediately **after** the `ProfileSelectorCard(...)` call (after its closing `)`) and before `// Loading indicator`, insert:

```kotlin
        // Radar warning overlay toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚠ Radar warning overlay",
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = uiState.radarWarningEnabled,
                onCheckedChange = { viewModel.setRadarWarningEnabled(it) }
            )
        }
```

- [ ] **Step 5.3: Build to verify no compile errors**

```
.\gradlew.bat :app:compileDebugKotlin -x lint 2>&1 | Select-Object -Last 10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5.4: Commit**

```
git add app/src/main/kotlin/com/kema/k2look/screens/DataFieldBuilderTab.kt
git commit -m "feat: add radar warning overlay toggle in DataFieldBuilderTab"
```

---

## Task 6: Full build and device test

- [ ] **Step 6.1: Run all unit tests**

```
.\gradlew.bat :app:testDebugUnitTest -x lint 2>&1 | Select-Object -Last 20
```

Expected: `BUILD SUCCESSFUL` — all tests pass including new `RadarWarningControllerTest`.

- [ ] **Step 6.2: Build and install on Karoo**

```
.\debugBuild.bat
```

Expected: `BUILD SUCCESSFUL`, APK installed.

- [ ] **Step 6.3: Manual verification checklist**

With Garmin Varia radar connected to Karoo:

1. Open K2Look → `DataFieldBuilderTab` → confirm "⚠ Radar warning overlay" toggle is visible below the profile selector.
2. Toggle OFF → confirm setting persists after app restart (toggle still OFF).
3. Toggle ON. Start a ride. Approach a situation with a car behind:
   - At first detection: `warning_white_28.png` (small) appears at viewer bottom-right.
   - As car approaches within ~5s TTA: icon switches to `warning_white_40.png` (large).
   - Once car passes (threat level → 0): icon disappears cleanly.
4. Disconnect glasses mid-ride. Reconnect. Confirm icon state resets to HIDDEN.

- [ ] **Step 6.4: Final commit**

```
git add -A
git commit -m "feat: radar warning overlay complete — small/large icon, TTA escalation, global toggle"
```
