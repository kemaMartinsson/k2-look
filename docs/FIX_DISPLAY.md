# Display Fix — Calibration Analysis & Applied Fixes

**Date**: 2026-03-22  
**Branch**: `feature/import-k2-profiles`  
**Status**: Code changes applied, needs build & on-glasses verification

---

## Problem Summary

Calibration screenshots from ActiveLook glasses show:
- Text clipped / pushed off the visible area
- Labels ("km/h", field names) render as garbled fragments
- Data values appear at wrong positions on the display

## Root Causes Identified

### 1. Text Position Formula Was Wrong (Critical — FIXED)

**File**: `app/src/main/kotlin/com/kema/k2look/layout/LayoutBuilder.kt`

K2Look used a generic formula:
```kotlin
txtX = zone.width - 10    // e.g. 234 for a 244px zone
txtY = zone.height / 2    // e.g. 30 for a 60px zone
```

ActiveLook's `txtX` with rotation 4 (TOP_LR) is the **right edge** of the rendered text. The official values from the [Visual Assets README](../docs/Activelook-Visual-Assets/README.md) are specific to each font:

| Zone type | Size | Font | K2Look txtX | Official txtX | K2Look txtY | Official txtY |
|-----------|------|------|-------------|---------------|-------------|---------------|
| Full line | 244×50 | 3 (64px) | 234 | **194** | 25 | **64** |
| Two data | 244×60 | 4 (75px) | 234 | **172** | 30 | **75** |
| Half line | 117×35 | 2 (38px) | 107 | **87** | 17 | **38** |
| One data | 187×163 | 5 (82px) | 177 | **187** | 81 | **106** |

**Fix**: Replaced the formula with a lookup table of official positions per font ID.

### 2. Labels Rendered in Wrong Font — Garbage Characters (Critical — FIXED)

**Files**: `LayoutBuilder.kt`, `GraphicCommand.kt`, `ActiveLookLayoutService.kt`

Labels like "SPEED (km/h)" were added as text sub-commands, but ActiveLook sub-commands
inherit the **layout's main font**. Fonts 4 and 5 only contain characters `Space` to `;`
(ASCII 0x20–0x3B) — **no letters at all**. Any layout using font 4 or 5 rendered labels
as garbage.

**Fix**:
- Added `GraphicCommand.FontChange(fontId)` command type
- LayoutBuilder now emits: `FontChange(1)` → `Text(label)` → `FontChange(zone.font)`
- ActiveLookLayoutService maps `FontChange` to SDK's `addSubCommandFont()`

### 3. FontSize Enum Had Wrong Heights (Medium — FIXED)

**File**: `app/src/main/kotlin/com/kema/k2look/model/FontSize.kt`

| Font | K2Look said | Actual (Visual Assets) |
|------|-------------|-------------------------|
| 1 | 24px | 24px ✓ |
| 2 | 35px | **38px** ✗ |
| 3 | 49px | **64px** ✗ |

Fixed to match the official SourceSansPro SemiBold dimensions.

### 4. Template Registry Mismatch (Medium — FIXED)

**File**: `app/src/main/kotlin/com/kema/k2look/layout/LayoutTemplateRegistry.kt`

`registerAllTemplatesWithoutPreviews()` (used in unit tests) had different zone coordinates,
sizes, fonts, and zone IDs than `registerAllTemplates()` (used on Android). The Android
version matched the official Visual Assets; the unit-test version was stale.

Templates fixed: **3D_FULL**, **4D**, **5D**, **6D** — all now match the Android/official versions.

---

## Files Changed

| File | Change |
|------|--------|
| `app/.../layout/LayoutBuilder.kt` | Replaced generic txtX/txtY formula with official per-font lookup; label sub-commands now bracketed with font-switch commands |
| `app/.../layout/GraphicCommand.kt` | Added `FontChange(fontId)` sealed class variant |
| `app/.../model/FontSize.kt` | Corrected height values: MEDIUM 35→38, LARGE 49→64 |
| `app/.../service/ActiveLookLayoutService.kt` | Added `FontChange` → `addSubCommandFont()` mapping in `saveLayout()` |
| `app/.../layout/LayoutTemplateRegistry.kt` | Synced unit-test templates (3D_FULL, 4D, 5D, 6D) with official Android versions |

---

## Calibration Photo Findings

| Photo | Debug Test | What It Proves |
|-------|-----------|----------------|
| `20260322_112546.jpg` | testDisplayBounds + live layout | Corner labels work; garbled fragments next to "km/h" = font charset bug |
| `20260322_112616.jpg` | Live layout | Same garbled labels; "Rot 0.7 anchor" text from test 2 bleeding through |
| `20260322_112627.jpg` | testClippingSemantics | Both "ABCDEFGH" boxes identical → **width = SIZE** (not right-edge coordinate) |
| `20260322_112645.jpg` | testTextXPosition | "12.5" at 4 txtX values → **txtX is the right edge of the text** |
| `20260322_112702.jpg` | testK2LookVsOfficial | K2Look params: "20" barely visible. Official params: renders correctly |
| `20260322_112715.jpg` | Actual ride data | "14.1" and "00:00:28" positioned in lower portion, not in layout zones |

---

## Verification Steps

After building and deploying:

1. **Run debug Test 5** (`testK2LookVsOfficial`) — both layouts should now render identically
2. **Run debug Test 6** (`testThreeFieldLayout`) — 3-field layout with correct positions
3. **Create a profile with labels enabled** — labels should show readable text, not fragments
4. **Test all template types** (1D through 6D) — values should be properly positioned within zones

---

## Known Remaining Issues

- `ActiveLookLayoutEncoder.kt` is dead code with endianness bugs (LITTLE_ENDIAN, should be BIG)
  and wrong command IDs. Not used in production — can be deleted or fixed separately.
- The `LayoutBuilder` lookup table provides one txtX/txtY per font. For half-width zones (117px)
  using the same font as full-width zones (244px), the official apps use the same txtX/txtY values.
  This is correct because txtX/txtY are relative to the clipping region.

