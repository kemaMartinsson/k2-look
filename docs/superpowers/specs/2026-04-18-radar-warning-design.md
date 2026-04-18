# Radar Warning Overlay — Design Spec
**Date:** 2026-04-18  
**Branch:** `feature/import-k2-profiles`  
**Status:** Approved — ready for implementation planning

---

## Overview

Display a bitmap warning icon in the viewer bottom-right corner of the ActiveLook display
whenever the Karoo radar (Garmin Varia or compatible) detects an approaching vehicle. The icon
escalates from small (28×28) to large (40×40) when the car's estimated time-to-arrival (TTA)
reaches ≤ 5 seconds. It disappears when `RADAR_THREAT_LEVEL` returns to 0 (delegated to Karoo
firmware). The feature is globally enabled by default and persists across reboots.

This design intentionally avoids blinking to minimise BLE FIFO pressure — a single icon swap
(erase + render) per state transition is all that's sent.

---

## Architecture: `RadarWarningController`

**New file:** `app/src/main/kotlin/com/kema/k2look/service/RadarWarningController.kt`

Single responsibility: receive radar data, run the state machine, and invoke render/erase
callbacks. Knows nothing about `Glasses`, `Context`, or assets. No background coroutine —
entirely event-driven by incoming radar packets.

### State Machine

```
HIDDEN
  → VISIBLE_SMALL     when threat > 0 and any target detected
VISIBLE_SMALL
  → VISIBLE_LARGE     when TTA ≤ 5 seconds
  → HIDDEN            when threat == 0
VISIBLE_LARGE
  → VISIBLE_SMALL     when TTA > 5 seconds (car slowed / fell back)
  → HIDDEN            when threat == 0
```

**`HIDDEN`:** No icon on display.  
**`VISIBLE_SMALL`:** `warning_white_28.png` (28×28) rendered once. Car detected, not yet close.  
**`VISIBLE_LARGE`:** `warning_white_40.png` (40×40) rendered once. Car within ~5s TTA.

On each state transition: erase current icon region, render new icon. No transitions = no BLE
commands (icon persists on display from previous render).

### Velocity / TTA Estimation

Track the two most recent radar samples as `(timestampMs: Long, closestRangeM: Float)`.

```
velocityMps = (prevRange - closestRange) / ((nowMs - prevMs) / 1000f)
TTA         = closestRange / velocityMps      // only valid if velocityMps > 0
```

If `velocityMps ≤ 0` (car not closing or no prior sample), TTA is treated as `Float.MAX_VALUE`
(stays in VISIBLE_SMALL, no escalation). If `closestRange ≤ 0`, skip — sensor noise.

### State Transition Actions

| Transition | Action |
|-----------|--------|
| `HIDDEN → VISIBLE_SMALL` | `renderSmall()` |
| `VISIBLE_SMALL → VISIBLE_LARGE` | `eraseSmall()` then `renderLarge()` |
| `VISIBLE_LARGE → VISIBLE_SMALL` | `eraseLarge()` then `renderSmall()` |
| `VISIBLE_SMALL → HIDDEN` | `eraseSmall()` |
| `VISIBLE_LARGE → HIDDEN` | `eraseLarge()` |

### Public API

```kotlin
class RadarWarningController(
    private val renderSmall: () -> Unit,
    private val eraseSmall: () -> Unit,
    private val renderLarge: () -> Unit,
    private val eraseLarge: () -> Unit
) {
    fun onRadarUpdate(threatLevel: Int, closestRangeM: Float?)  // called on every radar packet
    fun setEnabled(enabled: Boolean)                            // live toggle — erases immediately if disabling
    fun reset()                                                 // call on glasses disconnect
}
```

No `CoroutineScope` needed — state transitions are synchronous within the caller's coroutine.

**Constant:**
```kotlin
private const val TTA_THRESHOLD_S = 5.0f
```

---

## Rendering & Position

### Images

| Asset | Size | State |
|-------|------|-------|
| `assets/warning_white_28.png` | 28×28 | `VISIBLE_SMALL` (car detected) |
| `assets/warning_white_40.png` | 40×40 | `VISIBLE_LARGE` (TTA ≤ 5s) |

Both assets are already present. Rendered via `glasses.imgStream(bitmap, MONO_4BPP_HEATSHRINK, x, y)`.  
No `cfgSet("ALooK")` required — raw pixel stream, not a firmware icon ID.

Both bitmaps loaded lazily and cached on first use.

### Display Position

ActiveLook display is 304×256. The viewer-right / viewer-bottom corner (due to both-axis mirror)
maps to **low display-x, low display-y**:

| Icon | display-x | display-y | Rect |
|------|-----------|-----------|------|
| 28×28 small | 30 | 25 | (30,25)→(57,52) |
| 40×40 large | 30 | 25 | (30,25)→(69,64) |

Both icons are anchored at the same corner (30,25). The large icon simply extends further.

**Erase small:**
```kotlin
glasses.holdFlush(HOLD)
glasses.color(0); glasses.rectf(30, 25, 57, 52); glasses.color(15)
glasses.holdFlush(FLUSH)
```
**Erase large:**
```kotlin
glasses.holdFlush(HOLD)
glasses.color(0); glasses.rectf(30, 25, 69, 64); glasses.color(15)
glasses.holdFlush(FLUSH)
```
Wrapping in hold/flush prevents draw-color leaking into concurrent BLE commands.

> **Note:** Overlap with existing layout zones is intentional and accepted.

### Render/Erase Lambdas (in `KarooActiveLookBridge`)

```kotlin
private var warningBitmapSmall: Bitmap? = null
private var warningBitmapLarge: Bitmap? = null

private fun getBitmapSmall(): Bitmap = warningBitmapSmall
    ?: BitmapFactory.decodeStream(context.assets.open("warning_white_28.png")).also { warningBitmapSmall = it }

private fun getBitmapLarge(): Bitmap = warningBitmapLarge
    ?: BitmapFactory.decodeStream(context.assets.open("warning_white_40.png")).also { warningBitmapLarge = it }

val renderSmallFn: () -> Unit = {
    activeLookService.getGlasses()?.imgStream(getBitmapSmall(), MONO_4BPP_HEATSHRINK, 30, 25)
}
val eraseSmallFn: () -> Unit = {
    activeLookService.getGlasses()?.let { g ->
        g.holdFlush(HOLD); g.color(0); g.rectf(30, 25, 57, 52); g.color(15); g.holdFlush(FLUSH)
    }
}
val renderLargeFn: () -> Unit = {
    activeLookService.getGlasses()?.imgStream(getBitmapLarge(), MONO_4BPP_HEATSHRINK, 30, 25)
}
val eraseLargeFn: () -> Unit = {
    activeLookService.getGlasses()?.let { g ->
        g.holdFlush(HOLD); g.color(0); g.rectf(30, 25, 69, 64); g.color(15); g.holdFlush(FLUSH)
    }
}
```

On disconnect, both bitmap caches are set to `null` (they remain valid GC-able objects — defensive only).

---

## Global Setting

### `SettingsRepository`

Add to existing `k2look_settings` SharedPreferences:

```kotlin
private const val KEY_RADAR_WARNING = "radar_warning_enabled"

private val _radarWarningEnabled = MutableStateFlow(prefs.getBoolean(KEY_RADAR_WARNING, true))
val radarWarningEnabled: StateFlow<Boolean> = _radarWarningEnabled.asStateFlow()

fun setRadarWarningEnabled(enabled: Boolean) {
    prefs.edit { putBoolean(KEY_RADAR_WARNING, enabled) }
    _radarWarningEnabled.value = enabled
}
```

Default: `true`. Persists across reboots.

### `LayoutBuilderUiState`

```kotlin
val radarWarningEnabled: Boolean = true
```

### `LayoutBuilderViewModel`

- Subscribe to `settingsRepository.radarWarningEnabled` in `init` block (same pattern as `karooSyncEnabled`).
- Update `_uiState` when it changes.
- Expose `fun setRadarWarningEnabled(enabled: Boolean)`.
- On change: call `bridge?.setRadarWarningEnabled(enabled)`.

---

## UI Toggle

**File:** `DataFieldBuilderTab.kt`

A compact `Row` inserted **immediately after `ProfileSelectorCard`**, before the loading
indicator and before the screen tabs/zone editor. Always visible regardless of profile or
connection state.

```
┌─────────────────────────────────────────┐
│  [Profile selector card]                │
├─────────────────────────────────────────┤
│  ⚠ Radar warning overlay   [  ●──  ]   │  ← new Row, Switch
├─────────────────────────────────────────┤
│  Screen 1 │ Screen 2 │ + │              │
│  [Zone editor]                          │
│  [Send to Glasses]                      │
└─────────────────────────────────────────┘
```

Implementation:

```kotlin
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

---

## Bridge Wiring

**File:** `KarooActiveLookBridge.kt`

1. Construct `RadarWarningController(renderSmallFn, eraseSmallFn, renderLargeFn, eraseLargeFn)` as a
   private field alongside the other services.
2. In `observeRadar()`, after updating `currentData`, call:
   ```kotlin
   if (radarWarningEnabled) radarWarningController.onRadarUpdate(threat, closest?.toFloat())
   ```
   (The controller's `setEnabled(false)` also handles mid-ride disable, but the `if` avoids any
   call overhead when the feature is off.)
3. Expose `fun setRadarWarningEnabled(enabled: Boolean)` → delegates to controller.
4. In `onGlassesDisconnected()` → call `radarWarningController.reset()`.
5. Reset `warningBitmapSmall = null` and `warningBitmapLarge = null` on disconnect.

### Radar data gate

`onRadarUpdate` is only called when `StreamState.Streaming`. On `Idle`/`NotAvailable`/`Searching`,
the controller is NOT updated — it stays in whatever state it was in until threat-level clears.
This is safe: `RADAR_THREAT_LEVEL = 0` arrives as a `Streaming` packet, which transitions the
controller to `HIDDEN`.

---

## Edge Cases

| Scenario | Behaviour |
|----------|-----------|
| Feature disabled mid-ride (toggle off) | `setEnabled(false)` → controller erases current icon, transitions to HIDDEN |
| Feature re-enabled mid-ride | `setEnabled(true)` → controller processes next radar packet normally |
| Glasses disconnect | `reset()` → state set to HIDDEN; no erase attempt (glasses gone); bitmap caches cleared |
| No radar hardware (rides without Varia) | `observeRadar()` receives `Idle`/`NotAvailable` → controller never called → stays HIDDEN |
| Rapid range oscillation (noisy sensor) | TTA > 5s → VISIBLE_SMALL, no escalation. Each packet either holds or transitions — no extra traffic unless state changes |
| Render/erase throws | Exception caught in bridge lambda; logged; controller state remains consistent for next packet |

---

## Files Changed

| File | Change |
|------|--------|
| `service/RadarWarningController.kt` | **New** — state machine, blink, callbacks |
| `service/KarooActiveLookBridge.kt` | Construct controller; wire `observeRadar()`; expose `setRadarWarningEnabled`; reset on disconnect |
| `data/SettingsRepository.kt` | Add `radarWarningEnabled` StateFlow + setter |
| `viewmodel/LayoutBuilderViewModel.kt` | Subscribe to setting; expose setter; wire to bridge |
| `viewmodel/LayoutBuilderViewModel.kt` (inner `UiState`) | Add `radarWarningEnabled: Boolean = true` |
| `screens/DataFieldBuilderTab.kt` | Add `Switch` row after `ProfileSelectorCard` |

---

## Out of Scope

- No per-profile override (global only)
- No blink-rate configuration
- No sound/vibration (not available via ActiveLook BLE)
- No visual distinction between 1 car vs multiple cars
