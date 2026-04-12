# K2Look — Copilot Instructions & Session Handoff

## Project Overview

**K2Look** is a Kotlin/Android app for the **Karoo 2** cycling computer that mirrors ride metrics
onto **ActiveLook AR glasses**. It is a Karoo Extension (uses `karoo-ext` SDK) and communicates
directly with ActiveLook glasses over BLE.

- **Package**: `com.kema.k2look`
- **Min SDK**: 23 (Karoo 2 runs API 27)
- **Stack**: Kotlin, Jetpack Compose, Hilt is NOT used (manual DI), Gson, karoo-ext SDK
- **Git remote**: `github-private:kemaMartinsson/k2-look.git` (SSH alias → github.com)
- **Active branch**: `feature/import-k2-profiles`

---

## Architecture

```
MainActivity
├── MainViewModel          ← Karoo data streams, ride state, glasses connection
├── LayoutBuilderViewModel ← Profile management, active profile, screen selection
└── KarooActiveLookBridge  ← Core bridge: Karoo SDK ↔ ActiveLook BLE service
    ├── KarooDataService   ← KarooSystemService wrapper, all SDK consumers
    └── ActiveLookService  ← BLE connection + ActiveLook command protocol
```

**Key data flow**: `KarooDataService` consumes SDK events → `KarooActiveLookBridge` processes
them → `ActiveLookService` sends display commands to glasses.

**Profile storage**: `ProfileRepository` (Gson + SharedPreferences key `user_profiles`).
`DataFieldProfile` → `LayoutScreen[]` → `LayoutDataField[]`.

---

## ActiveLook Display & Coordinate System

### Display Hardware
- **Resolution**: 304 × 256 pixels (green monochrome OLED projected onto lens)
- **Safe area**: 30px horizontal margins, 25px vertical margins → (30,25) to (274,231)
- **Projection**: Image appears **mirrored** on the lens — left/right flipped when photographed

### Coordinate System (CRITICAL — source of many bugs)
- **Origin (0,0)**: Top-left of display
- **X**: increases left → right (0–303)
- **Y**: increases top → bottom (0–255)
- **Clipping region**: defined by **(x, y)** = upper-left corner + **(width, height)** = pixel SIZE
- **Text position (txtX, txtY)**: relative to clipping region's upper-left, NOT absolute display coords
- **Width**: 2-byte big-endian (u16). Height: 1-byte (u8). Both are pixel COUNTS, not coordinates.

### Viewer Axis Mapping (EMPIRICALLY VERIFIED)

The lens projects the image **mirrored on both axes**. This affects ALL element placement:

| Viewer direction | Display axis |
|-----------------|--------------|
| viewer-LEFT | **high** display-x / **high** rel-x |
| viewer-RIGHT | **low** display-x / **low** rel-x |
| viewer-TOP | **high** zone y0 (y=153 = top row) |
| viewer-BOTTOM | **low** zone y0 (y=25 = bottom row) |

**Rule**: increase rel-x → moves element viewer-LEFT. Decrease → viewer-RIGHT.
**Rule**: increase zone y0 → moves zone viewer-UP (toward top of display).

### Layout Sub-command Coordinates
All sub-command positions (icon x/y, label x/y, line endpoints) are **relative to the clipping
region**, NOT absolute display coordinates. This is stated in the API: "element positions are all
referenced from the layout clipping region (X0, Y0)."

### Reference Documentation
- **ActiveLook API**: `docs/Activelook-API-Documentation/ActiveLook_API.md` (section 5.5.6 coordinates, 5.10 layouts)
- **Visual Assets README**: `docs/Activelook-Visual-Assets/README.md` (official layout table, zone positions, font specs, **icon ID table** = ground truth)
- **Coordinate images**: `docs/Activelook-API-Documentation/resources/ActiveLook_coordinates.png` and `ActiveLook_clipping.png`
- **Demo app**: `reference/demo-app/android/` (LayoutsCommands.java, PageCommands.java show SDK usage patterns)
- **SDK source**: `reference/android-sdk/ActiveLookSDK/` (LayoutParameters.java, CommandData.java)
- **⚠️ WRONG/OUTDATED**: `docs/ActiveLook-Icon-Reference.md` — icon IDs do NOT match firmware. Use Visual Assets README instead.

---

## ActiveLook Font System

| Font | Size (px) | ASCII Range | Notes |
|------|-----------|-------------|-------|
| 1 | 24 | Space to `~` | Full ASCII. Used for labels, battery, time, unit overlays. |
| 2 | 38 | Space to `~` | Full ASCII. Medium rows. |
| 3 | 64 | Space to `~` | Full ASCII. Large rows. |
| 4 | 75 | Space to `;` | **Digits + punctuation ONLY**. No letters. |
| 5 | 82 | Space to `;` | **Digits + punctuation ONLY**. No letters. |

**Ghost characters** for alignment (from Visual Assets README):
- `$` = invisible char same width as digit `0`
- `&` = invisible char same width as `:` or `.`
- **Only invisible in fonts 4 and 5.** Fonts 1–3 render standard ASCII `$` visibly.
- Right-align example (fonts 4/5 only): `&$123`, Left-align: `1.23$`
- For fonts 1–3: use **space padding** (`padSpace()`) — spaces are ~half a digit wide but
  sufficient to stabilize the unit label position across digit counts.

**Official text positions** (from Visual Assets layout table):

| Font | txtX | txtY | Rotation |
|------|------|------|----------|
| 1 | 62 | 22 | 4 (TOP_LR) |
| 2 | 87 | 38 | 4 (TOP_LR) |
| 3 | 194 | 64 | 4 (TOP_LR) |
| 4 | 172 | 75 | 4 (TOP_LR) |
| 5 | 187 | 106 | 4 (TOP_LR) |

---

## ActiveLook Icon System (EMPIRICALLY VERIFIED)

### Icon ID Ground Truth
`docs/Activelook-Visual-Assets/README.md` Image table = **only correct source**.
`docs/ActiveLook-Icon-Reference.md` = **WRONG/outdated** — IDs do not match firmware. Ignore it.

### Key Cycling Icon IDs (28×28 small)

| ID | Icon | ID | Icon |
|----|------|----|------|
| 2  | altitude | 13 | heart-beat-avg |
| 4  | cadence | 16 | pace |
| 5  | cadence-avg | 19 | power |
| 9  | distance | 20 | power-3s |
| 12 | heart-beat | 26 | speed |
| | | 27 | speed-avg |

**Large icons (40×40)**: small_id + 32 (e.g. speed-small=26, speed-large=58). Confirmed empirically.

### cfgSet Requirement
**`cfgSet("ALooK")` MUST be called before any `imgDisplay` or `layoutSave` with bitmap sub-commands**.
Without it, the bitmap command is silently ignored with no error.

---

## Production Rendering Pipeline (CURRENT)

### Key Files

| File | Role |
|------|------|
| `ActiveLookLayoutService.kt` | Orchestrator: config management, profile save, per-frame display loop |
| `DynamicLayoutEngine.kt` | Stateless geometry: computes y0/height for N rows in 246px |
| `DynamicLayoutRenderer.kt` | Builds `LayoutParameters` + `LayoutExtraCmd` overlays, icon queuing |
| `LayoutPositionDefaults.kt` | All calibrated constants: font configs, unit X positions, icon X |
| `LayoutTemplateRegistry.kt` | Template zone definitions — used for height→size mapping and UI |
| `DisplayDebugService.kt` | Debug test patterns (10 tests for hardware calibration) |

### Dead Code (NOT used at runtime)

| File | Status |
|------|--------|
| `LayoutBuilder.kt` | Dead — superseded by DynamicLayoutRenderer. Only used in legacy unit test. |
| `ActiveLookLayoutEncoder.kt` | Dead — never instantiated. SDK handles encoding. |
| `GraphicCommand.kt` | Dead — only referenced by LayoutBuilder. |
| `ActiveLookLayout.kt` | Dead — only referenced by LayoutBuilder / Encoder. |

### Call Chain: Profile Save → Glasses Render

```
saveAndActivateProfile(profile)
  ├── cfgWrite(configName, version, 0)         // open user config for writing
  ├── saveProfileLayouts(profile)
  │     ├── screen.getTemplate()                // LayoutTemplateRegistry → zone heights
  │     ├── glasses.layoutDeleteAll()           // clear stale layout definitions in this config
  │     ├── glasses.clear()                     // erase stale pixels from old template
  │     ├── for each field:
  │     │     ├── zone = template.zones.find { it.id == field.zoneId }  // use template coords directly
  │     │     ├── DynamicLayoutRenderer.buildLayoutParams(layoutId, x0, y0, width, zoneHeight, font, hasIcon)
  │     │     └── glasses.layoutSave(params)
  │     └── cache geometry in screenGeometry[zoneId] (x0, y0, width, height, font)
  ├── saveProfileGauges(profile)
  └── glasses.cfgSet(configName)                // activate the config

displayAllFieldValues(fields, screen)           // called every frame
  ├── glasses.holdFlush(HOLD)
  ├── for each field:
  │     ├── DynamicLayoutRenderer.buildExtraCmd(value, unit, font, height, showUnit, iconPx)
  │     │     └── returns (LayoutExtraCmd, renderValue)
  │     ├── glasses.layoutClearAndDisplayExtended(layoutId, x0, y0, value, extraCmd)
  │     └── DynamicLayoutRenderer.queueIcon(...) if hasIcon → currentIconsByZone[zoneId] = icon
  ├── zonesToErase = activeIconsByZone.keys - currentIconsByZone.keys
  ├── if pendingIcons OR zonesToErase:
  │     ├── glasses.cfgSet("ALooK")
  │     ├── if zonesToErase: glasses.color(0) + glasses.rectf(x,y,x2,y2) per stale zone
  │     ├── DynamicLayoutRenderer.renderPendingIcons(glasses, icons)
  │     └── glasses.cfgSet(activeConfigName)    // restore user config
  ├── activeIconsByZone = currentIconsByZone     // update cross-frame tracking
  └── glasses.holdFlush(FLUSH)
```

### Stale Layout Prevention

Three mechanisms prevent ghost layouts and pixels from appearing:

1. **`layoutDeleteAll()`** in `saveProfileLayouts()`: Clears all layout *definitions* in the
   current K2L config before saving new ones. Scoped to config namespace — does not affect
   Suunto or ALooK.

2. **`glasses.clear()`** immediately after `layoutDeleteAll()`: Erases all *pixels* on the
   display. Without this, pixels from the old template (e.g. zones that no longer exist after
   switching from a pyramid to a 3-row layout) remain visible as garbage until overwritten.

3. **`invalidateProfileConfig()`** on tab entry: `LayoutBuilderViewModel.setBridge()` invalidates
   the config cache before auto-applying, forcing a full re-upload (which includes the delete
   and clear). Without this, the fast path (`cfgSet` only) would activate stale layouts.

---

## Calibrated Layout Constants (LayoutPositionDefaults.kt)

All values are empirically verified on physical hardware.

### Display Geometry

| Constant | Value | Purpose |
|----------|-------|---------|
| `AVAILABLE_HEIGHT` | 246 | Vertical space for dynamic row layout |
| `MIN_GAP` | 2 | Minimum pixel gap between rows |
| `ZONE_X0` | 30 | Left edge of safe render area |
| `ZONE_WIDTH` | 244 | Safe area width |
| `ICON_ABS_X` | 260 | Absolute display-x for `imgDisplay` icons |

### Font Configuration (CalibratedFontConfig)

| Font | txtY | unitY | refHeight | txtXWithIcon | txtXNoIcon | Rotation |
|------|------|-------|-----------|--------------|------------|----------|
| 1 | 25 | 25 | 30 | 208 | 244 | TOP_LR |
| 2 | 35 | 38 | 35 | 208 | 244 | TOP_LR |
| 3 | 48 | 47 | 50 | 220 | 244 | TOP_LR |

When zone height differs from `refHeight`, a vertical adjustment is applied:
`yAdjust = (actualHeight - refHeight) / 2`

### Row Sizes (DynamicLayoutEngine)

| Size | Height (px) | Font |
|------|-------------|------|
| large | 50 | 3 |
| medium | 35 | 2 |
| small | 30 | 1 |

### Unit Label X Positions (unitXLookup + offsets)

Unit labels are rendered as font-1 overlays via `LayoutExtraCmd`. With TOP_LR rotation,
text flows viewer-left from the anchor — lower x = further viewer-right.

The final X is a **3-factor formula** in `LayoutPositionDefaults.unitXFor(unit, font, iconPx)`:
```
finalX = unitXLookup[unit] + unitXFontOffset[font] + unitXIconOffset[iconPx]
```

**`unitXLookup`** — base X calibrated for font 2, large icon (40px):

| Unit | X | Unit | X |
|------|---|------|---|
| km/h | 165 | w | 179 |
| bpm | 165 | m | 179 |
| rpm | 165 | % | 178 |
| km | 173 | ft | 179 |
| w/kg | 165 | kcal | 170 |
| kcal/h | 158 | mph | 168 |

**`unitXFontOffset`** — adjustment per font (larger font = wider value text = unit must move viewer-right):

| Font | Offset |
|------|--------|
| 1 | 0 |
| 2 | +10 |
| 3 | −20 |

**`unitXIconOffset`** — adjustment per icon size (smaller icon = more room for value = unit moves viewer-right):

| iconPx | Offset |
|--------|--------|
| 0 (no icon) | +28 |
| 28 (small) | +7 |
| 40 (large) | +2 |

### Elapsed Time Split Rendering

For "HH:MM:SS" unit with font ≥ 2: main font renders "H:MM", a font-1 overlay renders ":SS"
at the viewer-right. Seconds X positions: font 2 → 153, font 3 → 145.

---

## Config System

### Config Naming
K2Look configs: `"K2L" + CRC32(profileId)` = 11 chars. Fits the 12-byte config name field.

### Config Rules (CRITICAL)
- `cfgSet("ALooK")` = read-only system config. All `layoutSave` calls are **silently ignored**.
- `cfgWrite("name", version, 0)` = open a user config for writing. All layout saves go here.
- Layout IDs are **global across configs** — save in K2L config, render under ALooK context.
- Pattern: `cfgWrite` → `layoutDeleteAll` → `layoutSave(...)×N` → `cfgSet(configName)`.
- For icon rendering: `cfgSet("ALooK")` → `imgDisplay(...)×N` → `cfgSet(activeConfigName)`.

### Config Version Cache
`configVersionCache[configName] = version` (based on `profile.modifiedAt`).
- **Fast path**: cache hit → `cfgSet(configName)` only (no re-upload).
- **Full path**: cache miss → `cfgWrite` → delete all → save layouts → `cfgSet`.
- Cache is invalidated by `invalidateProfileConfig()` when profiles are edited or on tab entry.

---

## ActiveLook Icon Rendering

### Icon Placement
Icons are rendered via `imgDisplay` in the ALooK config pass (after all layout renders).
- **Absolute X**: `ICON_ABS_X = 260` base. Small icons (28px) get +15 applied in `queueIcon`.
- **Absolute Y**: `y0 + (zoneHeight - iconPx) / 2` (vertically centered in zone)
- Small icons: 28×28 px. Large icons: 40×40 px.
- A +6px calibrated Y offset is applied for 28px icons in 35px zones (medium rows).

### Icon Pass Flow
Icons cannot be rendered with `layoutClearAndDisplayExtended` because `imgDisplay` requires
the ALooK system config. The display loop:
1. Queues icons per frame into `pendingIcons`; also records them in `currentIconsByZone[zoneId]`.
2. Computes `zonesToErase = activeIconsByZone.keys - currentIconsByZone.keys` (zones that had
   an icon last frame but not this frame).
3. If either list is non-empty: `cfgSet("ALooK")`, then `color(0)` + `rectf` over each stale
   icon area to erase it, then `renderPendingIcons`.
4. Restores user config. Updates `activeIconsByZone = currentIconsByZone` for next frame.

**`activeIconsByZone`** is cleared on `saveProfileLayouts` so stale erase data from a
previous profile cannot accidentally erase pixels on the new layout.

---

## Official ActiveLook Layout Positions (Reference)

These come from the **Visual Assets README**. Our dynamic layout engine computes positions
at runtime, but these are the reference values for the standard zone configurations.

### Zone Position Table (from Visual Assets)

| Zone | Data Type | x0 | y0 | width | height | font |
|------|-----------|----|----|-------|--------|------|
| 1D | One data | 59 | 41 | 187 | 163 | 5 |
| 2D H | Two data | 30 | 129 | 244 | 60 | 4 |
| 2D L | Two data | 30 | 25 | 244 | 60 | 4 |
| 3D Full H | Three data | 30 | 153 | 244 | 50 | 3 |
| 3D Full M | Three data | 30 | 89 | 244 | 50 | 3 |
| 3D Full L | Three data | 30 | 25 | 244 | 50 | 3 |
| 3D Half H1 | Half line | 157 | 157 | 117 | 35 | 2 |
| 3D Half H2 | Half line | 30 | 157 | 117 | 35 | 2 |

**Critical**: Official heights are SMALLER than txtY (font 3: txtY=64, height=50). ActiveLook
intentionally clips the bottom of text. Do NOT "fix" this by enlarging heights.

---

## Active Feature: Import Karoo Profile

**Branch**: `feature/import-k2-profiles`
**Spec**: `docs/Feature-Profile-Import.md`
**Status**: Core implemented. Display pipeline working. Unit tweaks in progress.

### What It Does
Reads the user's Karoo ride profile (pages + fields) from the SDK and auto-generates a matching
K2Look/ActiveLook layout. Eliminates manual setup.

### Key Design Decisions
1. **Never prompt during a ride.** `RideState.Idle` check required before showing any import UI.
2. **Event-driven, no polling.** `ActiveRideProfile` is a `KarooEvent` — push-based.
3. **Auto-switch already works.** `KarooActiveLookBridge.tryAutoSwitchProfile()` handles name match.

---

## DisplayDebugService.kt — Test Reference

| Test | What it draws |
|------|---------------|
| 1 | Display boundary + safe area + corner labels |
| 2 | Same text at same point with all 8 rotations |
| 3 | Two identical zones with different width values |
| 4 | Two fonts side by side |
| 5 | K2Look vs Official text positions on same zone |
| 6 | Full 3D_FULL layout (3 zones, values displayed) |
| 7 | txtY=38 center calibration |
| 8 | Icon grid (IDs 0–5, 12, 19, 26 across 3 rows) |
| 9 | Realistic layout — [icon][value][unit] with cycling data |
| 10 | Visual styles: gauge, bar, zone view |

---

## Key Gotchas & Hard-Won Lessons

1. **Font 4 and 5 cannot render letters.** Only Space through `;` (0x20–0x3B). Unit overlays
   MUST use font 1 (via `addSubCommandFont` in ExtraCmd).

2. **Official heights intentionally clip text.** txtY > zone height is by design. Don't "fix" it.

3. **Sub-command coordinates are relative** to the clipping region, not absolute display coords.

4. **`layoutClearAndDisplay` replays saved sub-commands BEFORE main text.** Main text with
   `opacity=true` overwrites sub-commands. Use `LayoutExtraCmd` + `layoutClearAndDisplayExtended`
   for overlays drawn AFTER main text.

5. **`cfgSet("ALooK")` makes `layoutSave` a silent no-op.** Save layouts in a user config only.

6. **`cfgSet("ALooK")` is required before `imgDisplay`.** Without it, icons are silently skipped.

7. **`layoutClearAndDisplayExtended` x/y override saved x0/y0.** The saved values are never
   used at render time. ExtraCmd positions are relative to the call-time x/y.

8. **Icon IDs in `docs/ActiveLook-Icon-Reference.md` are WRONG.** Use Visual Assets README only.

9. **Config naming**: `"K2L" + CRC32(profileId)` = 11 chars. Fits 12-byte field exactly.

10. **`layoutDeleteAll()` only affects the current config namespace.** Safe to call — won't
    touch Suunto or other apps' layouts.

11. **Ghost characters `$` and `&` are only invisible in fonts 4/5.** Fonts 1–3 render `$`
    as a visible character. Use `padSpace()` for value alignment in fonts 1–3. Ghost chars
    work for fonts 4/5. SDK encodes strings as `US_ASCII`, so `0xFF` (API doc §6.3 padding
    byte) cannot be sent through the SDK — it would be mangled to `?`.

12. **LayoutTemplateRegistry has TWO copies** of every template. `registerAllTemplates()` (runtime,
    has R.drawable previews) and `registerAllTemplatesWithoutPreviews()` (unit tests). Keep in sync.

13. **Unit X is a 3-factor formula.** `unitXFor(unit, font, iconPx)` = `unitXLookup[unit] +
    unitXFontOffset[font] + unitXIconOffset[iconPx]`. Each dimension matters: larger fonts
    produce wider value text (needs lower unitX); smaller icons leave more room for value text
    (also needs lower unitX). Missing any dimension causes visible overlap on certain combinations.

14. **`imgDisplay` pixels persist until explicitly erased.** When a field is reconfigured from
    icon→no-icon, the old icon pixels remain on screen. Fix: `activeIconsByZone` tracks the
    last rendered icon per zone. Each frame, zones present in `activeIconsByZone` but absent
    from `currentIconsByZone` get a black `rectf` erase in the ALooK pass. `PendingIcon` must
    carry `iconPx` for the erase dimensions.

15. **`layoutDeleteAll()` removes definitions, NOT pixels.** After switching templates (e.g.
    pyramid → 3-row), zones that no longer exist leave visible garbage. Always call
    `glasses.clear()` immediately after `layoutDeleteAll()` in `saveProfileLayouts()` to
    blank the screen before uploading the new layout definitions.

---

## Development Environment

- **ADB**: Device `KAROO20ALA091101299` visible on Windows host.
- **debugBuild.bat**: `.\gradlew.bat installDebug -x test -x testDebugUnitTest -x lintDebug`. ~50s incremental.
- **Layout preview tool**: `tools/preview_layout.py` — Python 3 + Pillow. ~1s per run.
  - `tools/test9.json` — calibrated 3-row [icon][value][unit] layout
  - `tools/test10.json` — vis-style scratchpad: gauge / bar / zone view
  - Run: `python tools/preview_layout.py --config tools/test9.json --viewer`
  - Supported `extra_cmds` types: `bitmap`, `font`, `text`, `rect`, `rectf`, `rectf_dim`
