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

### Viewer Axis Mapping (EMPIRICALLY VERIFIED — Session 5/6)

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
| 1 | 24 | Space to `~` | Full ASCII. Used for labels, battery, time. |
| 2 | 38 | Space to `~` | Full ASCII. Half-width zones. |
| 3 | 64 | Space to `~` | Full ASCII. Full-width zones (3D_FULL). |
| 4 | 75 | Space to `;` | **Digits + punctuation ONLY**. No letters. Two-data zones. |
| 5 | 82 | Space to `;` | **Digits + punctuation ONLY**. No letters. One-data zones. |

**Ghost characters** for alignment (from Visual Assets README):
- `$` = invisible char same width as digit `0`
- `&` = invisible char same width as `:` or `.`
- Layouts designed for `4 digits + separator` (e.g. "12.34", "00:00")
- Right-align example: `&$123`, Left-align: `1.23$`

**Official text positions** (from Visual Assets layout table):

| Font | txtX | txtY | Rotation |
|------|------|------|----------|
| 1 | 62 | 22 | 4 (TOP_LR) |
| 2 | 87 | 38 | 4 (TOP_LR) |
| 3 | 194 | 64 | 4 (TOP_LR) |
| 4 | 172 | 75 | 4 (TOP_LR) |
| 5 | 187 | 106 | 4 (TOP_LR) |

---

## ActiveLook Icon System (EMPIRICALLY VERIFIED — Session 5/6)

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

### Icon y-Centering per Zone (28×28 icon)

| Zone | zone h | icon rel-y = (h−28)/2 |
|------|--------|------------------------|
| y=153 (viewer-top, font1) | 30 | **1** |
| y=89 (viewer-mid, font2) | 35 | **3** |
| y=25 (viewer-bot, font3) | 50 | **11** |

Note: some icon drawables have ~2px top offset. Add +1 to rel-y for affected icons (e.g. power id=19).

---

## [Icon][Value][Unit] Layout Math (CONFIRMED — Session 6)

Viewer-left = high rel-x. Zone: x0=30, width=244 (full safe area).

```
Viewer: [icon][ gap ][  value text  ][       gap       ][unit]
rel-x:   216–243      208 ← flows → 0                    5
```

| Element | rel-x | Notes |
|---------|-------|-------|
| Icon (28×28) | **216** | viewer-LEFT; fills 216..243 |
| Value text (txtX) | **208** | 8px gap from icon edge; flows viewer-right |
| Unit text (ExtraCmd) | **5** | viewer-RIGHT; safe from longest value text |

### Unit Text y-Center (font 1 = 24px tall)

| Zone h | unit rel-y = (h−24)/2 |
|--------|------------------------|
| 30 | **3** |
| 35 | **5** |
| 50 | **13** |

### ExtraCmd Template

```kotlin
LayoutExtraCmd()
    .addSubCommandBitmap(iconId.toByte(), 216.toShort(), iconRelY.toShort())
    .addSubCommandFont(1.toByte())
    .addSubCommandText(5.toShort(), unitRelY.toShort(), unitString)
```

LayoutParameters: `txtX=208`. All other zone params unchanged.

### No Overlap Guarantee
- Font3 "123.4" ≈152px wide: ends at rel-x≈56 → 51px gap before unit at 5 ✓
- Font2 "1234" ≈88px wide: ends at rel-x≈120 → 115px gap ✓
- Font1 "12:34:56" ≈112px wide: ends at rel-x≈96 → 91px gap ✓

---

## Calibrated txtX Values (EMPIRICALLY VERIFIED)

| Scenario | txtX | Notes |
|----------|------|-------|
| No icon (value fills full zone) | **244** | viewer-LEFT edge of 244px zone |
| With icon (28px icon at rel-x=216) | **208** | 8px gap from icon left edge |

`txtY` values: font1=**22**, font2=**38**, font3=**64** (same as official). Calibrated via live tests.

---

## Official ActiveLook Layout Positions (The TRUTH)

These come from the **Visual Assets README** and are the ONLY correct reference. Our custom
K2Look zone positions must be consistent with these or text rendering will break.

### Zone Position Table (from Visual Assets)

| Zone | Data Type | Main X | Main Y | width | height | font | Chrono X | Chrono Y | ChronoHour X | ChronoHour Y |
|------|-----------|--------|--------|-------|--------|------|----------|----------|--------------|--------------|
| 1D | One data | 59 | 41 | 187 | 163 | 5 | 49 | 93 | 239 | 121 |
| 2D H | Two data | 30 | 129 | 244 | 60 | 4 | 30 | 129 | 203 | 154 |
| 2D L | Two data | 30 | 25 | 244 | 60 | 4 | 30 | 25 | 203 | 50 |
| 3D(triangle) H | Two data | 30 | 129 | 244 | 60 | 4 | 30 | 129 | 203 | 154 |
| 3D Full H | Three data | 30 | 153 | 244 | 50 | 3 | 30 | 153 | 211 | 170 |
| 3D Full M | Three data | 30 | 89 | 244 | 50 | 3 | 30 | 89 | 211 | 106 |
| 3D Full L | Three data | 30 | 25 | 244 | 50 | 3 | 30 | 25 | 211 | 42 |
| 4D Full H | Two data | 30 | 149 | 244 | 60 | 4 | 30 | 149 | 203 | 174 |
| 4D Full L | Two data | 30 | 80 | 244 | 60 | 4 | 30 | 80 | 203 | 105 |
| 3D Half H1 | Half line | 157 | 157 | 117 | 35 | 2 | 168 | 157 | 257 | 166 |
| 3D Half H2 | Half line | 30 | 157 | 117 | 35 | 2 | 41 | 157 | 130 | 166 |
| 3D Half M1 | Half line | 157 | 95 | 117 | 35 | 2 | 168 | 96 | 257 | 105 |
| 3D Half M2 | Half line | 30 | 95 | 117 | 35 | 2 | 41 | 96 | 130 | 105 |
| 3D Half L1 | Half line | 157 | 33 | 117 | 35 | 2 | 168 | 35 | 257 | 45 |
| 3D Half L2 | Half line | 30 | 33 | 117 | 35 | 2 | 41 | 35 | 130 | 45 |

**Critical observation**: Official heights are SMALLER than txtY:
- Font 3: txtY=64, height=50 → 14px overflow (clipped by design)
- Font 4: txtY=75, height=60 → 15px overflow (clipped by design)
- Font 2: txtY=38, height=35 → 3px overflow (clipped by design)

This means **ActiveLook intentionally clips the bottom of text**. The text baseline extends
below the clipping region. This is normal and by design — do NOT "fix" this by enlarging heights.

### Official Layout Examples (from Visual Assets)

| Layout ID | Name | x0 | y0 | width | height | font | txtX | txtY |
|-----------|------|-----|-----|-------|--------|------|------|------|
| 12 | distance_metric_full | 30 | 153 | 244 | 50 | 3 | 194 | 64 |
| 13 | speed_metric_full | 30 | 153 | 244 | 50 | 3 | 194 | 64 |
| 44 | speed_half | 157 | 157 | 117 | 35 | 2 | 87 | 38 |
| 79 | chrono_min_sec_two_data | 30 | 129 | 244 | 60 | 4 | 172 | 75 |
| 86 | speed_two_data | 30 | 129 | 244 | 60 | 4 | 172 | 75 |

---

## Layout Rendering Pipeline

### How a Profile Reaches the Glasses

```
DataFieldProfile → LayoutScreen → LayoutDataField[]
    ↓
LayoutBuilder.buildScreenLayouts()
    ↓ (for each field)
    LayoutBuilder.buildLayout()
        ├── Looks up zone from LayoutTemplateRegistry
        ├── Creates ClippingRegion from zone x/y/width/height
        ├── Sets TextConfig from officialTextPositions[zone.font]
        └── additionalCommands = emptyList() (sub-commands disabled, see Bugs 2/3)
    ↓
ActiveLookLayout (data class)
    ↓
ActiveLookLayoutService.saveLayout()
    ├── Creates SDK LayoutParameters object
    └── glasses.layoutSave(layoutParams)
    ↓
At runtime: layoutClearAndDisplay(layoutId, valueString)
```

### Key Files in the Pipeline

| File | Role |
|------|------|
| `LayoutTemplateRegistry.kt` | Zone positions for 1D-6D templates. TWO copies: `registerAllTemplates()` (Android, with R.drawable) and `registerAllTemplatesWithoutPreviews()` (unit tests). MUST be kept in sync. |
| `LayoutBuilder.kt` | Builds `ActiveLookLayout` from zone + field config. Owns `officialTextPositions`. |
| `GraphicCommand.kt` | Sealed class: Image, Text, Line, Circle, Rect, FontChange |
| `ActiveLookLayout.kt` | Data classes: ActiveLookLayout, ClippingRegion, TextConfig |
| `ActiveLookLayoutService.kt` | Saves layouts to glasses via SDK `LayoutParameters`. Entry: `saveAndActivateProfile()`. |
| `ActiveLookLayoutEncoder.kt` | **DEAD CODE** — never called at runtime. SDK handles encoding. Safe to delete. |
| `DisplayDebugService.kt` | Debug test patterns drawn directly on glasses. 9 tests for coordinate verification. |

### ActiveLookLayoutEncoder.kt is DEAD CODE

The encoder uses `ByteOrder.LITTLE_ENDIAN` but is **never instantiated or called** anywhere in
production. `ActiveLookLayoutService.saveLayout()` builds `LayoutParameters` directly via the SDK,
which handles its own encoding with big-endian byte order. The encoder class is vestigial.

---

## Display Rendering Bugs (ALL RESOLVED — Session 4)

### Bug 1: Massive Bottom Clipping — RESOLVED
**Symptom**: Text values clipped at bottom, especially in 3-field layouts.
**Root cause**: Zone heights changed from official (h=50/60/35) to "zero-clipping" (h=64/75/38).
**Fix**: Reverted ALL zone positions in `LayoutTemplateRegistry.kt` (both copies) to official
Visual Assets values. Both `registerAllTemplates()` and `registerAllTemplatesWithoutPreviews()`.

### Bug 2: Strange Characters / Garbled Icons & Labels — RESOLVED
**Symptom**: Small horizontal dashes/garbage characters after value text.
**Root cause**: Saved layout sub-commands (icons, labels via `addSubCommandBitmap`/`addSubCommandText`)
render BEFORE main text. Main text with `opacity=true` draws black background that overwrites
sub-commands. Garbled characters were fragments of partially-overwritten icons/labels.
**Fix**: `buildAdditionalCommands()` now returns `emptyList()`. No sub-commands saved with layouts.
Removed dead code: `buildLabelText()`, `ICON_MARGIN`, `LABEL_FONT` constants.

### Bug 3: Label/Unit Disappears After First Render — RESOLVED
**Symptom**: "km/h" appears initially but vanishes after value update.
**Root cause**: Same as Bug 2. On `layoutClearAndDisplay`, firmware clears zone, renders saved
sub-commands (label visible briefly), then renders main text which overwrites the label.
**Fix**: Same as Bug 2 — no saved sub-commands.

### Future: Labels/Icons via LayoutExtraCmd
The official ActiveLook demo app uses `LayoutExtraCmd` with `layoutClearAndDisplayExtended` to
render labels/icons AFTER main text (drawn on top of value, not under it). This is the correct
approach for future label support. See `reference/android-sdk/debugapp/DebugActivity.java` and
`reference/android-sdk/ActiveLookSDK/.../LayoutExtraCmd.java` for the pattern.

---

## Completed Display Fixes (Previous Sessions)

### Fix 1: Text Position Formula (LayoutBuilder.kt)
**Was**: Generic formula `txtX = zone.width - 10, txtY = zone.height / 2`
**Now**: Per-font lookup table `officialTextPositions[zone.font]` using official ActiveLook values.

### Fix 2: Garbled Label Text (LayoutBuilder.kt + GraphicCommand.kt)
**Was**: Labels rendered with fonts 4/5 which lack letters → garbage glyphs.
**Now**: FontChange bracketing: `FontChange(1)` → `Text(label, font=1)` → `FontChange(zone.font)`.
Added `GraphicCommand.FontChange(fontId: Int)` sealed class variant.

### Fix 3: FontSize Heights (FontSize.kt)
**Was**: MEDIUM=35, LARGE=50 (wrong pixel values)
**Now**: MEDIUM=38, LARGE=64 (correct font pixel heights)

### Fix 4: Template Sync (LayoutTemplateRegistry.kt)
**Was**: Unit-test templates out of sync with Android templates.
**Now**: Both copies kept in sync and reverted to official ActiveLook Visual Assets values.

### Fix 5: FontChange Wiring (ActiveLookLayoutService.kt)
**Was**: FontChange was dead code in encoder.
**Now**: `GraphicCommand.FontChange` → `layoutParams.addSubCommandFont(fontId)` in `saveLayout()`.

### Fix 6: Template Values Reverted to Official (LayoutTemplateRegistry.kt) — Session 4
**Was**: "Zero-clipping" experiment changed heights to match font size (h=64/75/38), breaking geometry.
**Now**: ALL zone positions reverted to exact official ActiveLook Visual Assets values in both copies.

### Fix 7: Sub-commands Disabled (LayoutBuilder.kt) — Session 4
**Was**: Saved sub-commands (FontChange→Text→FontChange for labels, Image for icons) rendered
BEFORE main text, causing garbled characters and disappearing labels.
**Now**: `buildAdditionalCommands()` returns `emptyList()`. Labels/icons temporarily disabled.
Future: use `LayoutExtraCmd` + `layoutClearAndDisplayExtended` for labels drawn AFTER main text.

---

## LayoutTemplateRegistry.kt — MATCHES OFFICIAL VALUES

The file has TWO identical sets of templates that MUST stay in sync:

1. `registerAllTemplatesWithoutPreviews()` — used in unit tests (no R.drawable access)
2. `registerAllTemplates()` — used at runtime (includes `preview = R.drawable.layout_preview_*`)

**Current values match the official ActiveLook Visual Assets table** (reverted in Session 4).

---

## Active Feature: Import Karoo Profile

**Branch**: `feature/import-k2-profiles`
**Spec**: `docs/Feature-Profile-Import.md`
**Status**: Designed, not yet implemented. Display fixes completed in Session 4.

### What It Does

Reads the user's Karoo ride profile (pages + fields) from the SDK and auto-generates a matching
K2Look/ActiveLook layout. Eliminates manual setup.

### Key Design Decisions

1. **Never prompt during a ride.** `RideState.Idle` check required before showing any import UI.
2. **Event-driven, no polling.** `ActiveRideProfile` is a `KarooEvent` — push-based.
3. **Auto-switch already works.** `KarooActiveLookBridge.tryAutoSwitchProfile()` handles name match.

### Karoo Sync Toggle

A **Karoo Sync** on/off toggle (default: on). Stored as `karoo_sync_enabled` boolean in
`SharedPreferences`. Exposed as `karooSyncEnabled: StateFlow<Boolean>` in `LayoutBuilderViewModel`.

### Mapping Logic

**Template selection** (by field count): 1→1D, 2→2D, 3→3D_FULL, 4→4D, 5→5D, 6+→6D (truncate).
**Field mapping**: `RideProfile.Page.Element.dataTypeId` == `DataField.karooStreamType`.
**Zone assignment**: declaration order. Ignore Karoo grid positions.

### Files To Create/Modify

| File | Status | What |
|---|---|---|
| `app/.../sharing/KarooProfileImporter.kt` | **TODO** | Maps `RideProfile` → `DataFieldProfile` |
| `app/.../data/SettingsRepository.kt` | **TODO** | `karoo_sync_enabled` boolean pref |
| `app/.../viewmodel/LayoutBuilderViewModel.kt` | **TODO** | `importFromKaroo()`, sync toggle, expose profile + ride state |
| `app/.../screens/ProfileManagementScreen.kt` | **TODO** | Sync Switch + "From Karoo" suggestion + preview dialog |
| `app/.../screens/DataFieldBuilderTab.kt` | **TODO** | Pass activeRideProfile, rideState, karooSyncEnabled down |

---

## Completed Work (All Sessions)

### Session 1 — Bug Fixes & Feature Design
- Fixed 4 bugs in `UpdateDownloader.kt` (thread leak, wrong-thread Compose update, false
  `onComplete(true)`, silent `ActivityNotFoundException`)
- Fixed 2 issues in `UpdateChecker.kt` (connection leak, prerelease filter)
- Created `docs/Feature-Profile-Import.md` (full feature spec)
- Decided against Gist-based sharing and TOON format

### Session 2 — Display Root Cause Analysis & Fixes
- Analyzed calibration photos from ActiveLook glasses
- Identified 3 root causes: wrong text position formula, garbled labels (font charset), wrong FontSize heights
- Fixed `LayoutBuilder.kt`: official per-font text position lookup table + FontChange label bracketing
- Fixed `GraphicCommand.kt`: added FontChange sealed class variant
- Fixed `FontSize.kt`: corrected MEDIUM=38, LARGE=64
- Fixed `ActiveLookLayoutService.kt`: wired FontChange → addSubCommandFont
- Synced LayoutTemplateRegistry unit-test and Android copies
- Created `docs/FIX_DISPLAY.md` (full analysis and fix documentation)

### Session 3 — Template Tuning (EXPERIMENTAL — reverted in Session 4)
- Changed 3D_FULL height from 50→64 (zero-clipping experiment)
- Changed ALL templates: font 4 zones h=60→75, font 2 zones h=35→38
- Repositioned y values for all templates
- **Result: WORSE** — massive bottom clipping reported. Reverted in Session 4.

### Session 4 — Display Bug Resolution
- **Bug 1 fix**: Reverted ALL templates in `LayoutTemplateRegistry.kt` (both copies) to official
  ActiveLook Visual Assets values. 12 template blocks updated.
- **Bug 2/3 root cause**: Discovered saved sub-commands render BEFORE main text. Main text with
  `opacity=true` overwrites sub-commands. Official demo app uses `LayoutExtraCmd` (drawn AFTER
  main text) instead — this is the correct approach.
- **Bug 2/3 fix**: `buildAdditionalCommands()` returns `emptyList()`. Removed dead code:
  `buildLabelText()`, `ICON_MARGIN`, `LABEL_FONT` constants, `IconSize` import.
- **DisplayDebugService**: Updated test 6 to use official 3D_FULL values (h=50, y=153/89/25).
- Labels/icons temporarily disabled. Future work: implement via `LayoutExtraCmd` +
  `layoutClearAndDisplayExtended` for labels drawn AFTER main text.

### Session 5 — Coordinate System & Icon ID Discovery
- Empirically confirmed **both display axes are mirrored** to the viewer.
- viewer-LEFT = high display-x/rel-x. viewer-TOP = high zone y0. Documented in copilot-instructions.
- Discovered `docs/ActiveLook-Icon-Reference.md` contains **wrong icon IDs** — firmware does not match.
- Confirmed `docs/Activelook-Visual-Assets/README.md` Image table is the **only correct icon ID source**.
- Added test 7 (txtY calibration), test 8 (icon grid 3×3), test 9 (imgList firmware query).
- Verified key cycling icon IDs empirically: speed=26, power=19, heart-beat=12, cadence=4, distance=9.
- Wired tests 7-9 into `MainViewModel.kt` and `DebugTab.kt`.
- Created `debugBuild.bat` — fast install skipping tests (~50s incremental).

### Session 6 — [Icon][Value][Unit] Layout Math Calibrated
- Confirmed icon y-centering formula per zone: rel-y = (zoneH − 28) / 2.
- Computed and documented `[icon][value][unit]` layout math: icon rel-x=216, txtX=208, unit rel-x=5.
- Unit text y-center per zone: rel-y = (zoneH − 24) / 2 using font1 (24px).
- Confirmed large icon IDs = small_id + 32 empirically.
- Power icon (id=19) clips ~2px at top — drawable has internal offset; use rel-y + 1.
- `cfgSet("ALooK")` requirement confirmed: must be called before any bitmap commands.

### Session 9 — Config Context Fix + Test 9 Hardware Calibration
- **Root cause of "no visible change" found**: `cfgSet("ALooK")` activates the **system config**.
  `layoutSave` calls made while in that context are **silently ignored** (no error, no visual
  feedback). All prior test 9 coordinate edits had zero effect because every `layoutSave` was
  discarded. This is a fundamental firmware rule — not a bug in the app code.
- **Fix**: Restructured test 9 to call `cfgWrite("K2LDBG", 4, 0)` before all `layoutSave`
  calls, then `cfgSet("ALooK")` immediately before the render calls (needed for icon bitmaps).
- **Layout IDs are global across configs**: Layouts saved while in a user config (K2LDBG)
  are accessible when rendering under `cfgSet("ALooK")` context. IDs 53/54/55 used for
  T9_TOP/T9_MID/T9_BOT (replaced old 50/51/52 which caused stale-layout issues vs test 8).
- **Values confirmed**: 25.1 / 250 / 150 render correctly in three zones.
- **Icons confirmed**: id=26 (speed), id=19 (power), id=12 (heart-beat) render at x=216.
  Test 8 (direct `imgDisplay` grid) confirms Visual Assets README IDs match firmware — the
  momentary "wrong icon" impression in test 9 was a misidentification on the small green OLED.
- **ExtraCmd text is right-anchored**: Unit at rel-x=5 appeared almost entirely outside the
  clipping region (text spans leftward from anchor). Updated unit x positions: 80 (km/h),
  50 (W), 75 (bpm). Not yet hardware-verified.
- **Test 10 button added** to `DebugTab.kt`.
- **Production impact**: `ActiveLookLayoutService.saveLayout()` must also use `cfgWrite` (not
  `cfgSet("ALooK")`) before calling `layoutSave`. Check this before shipping label/icon support.

### Session 8 — Test 9 Calibration Applied + Vis-Style Preview Extension
- **Test 9 fully calibrated**: All coordinates from `tools/test9.json` translated into
  `testRealisticLayout()` in `DisplayDebugService.kt`. Three evenly-spaced rows using
  `x0=0, width=304` (full display width, no 30px margin). Calibrated values:
  - Top (font1, y=190): txtX=235, txtY=10 | icon=26 at (250,5) | unit "km/h" at (135,10)
  - Mid (font2, y=110): txtX=235, txtY=7  | icon=51 at (244,0) | unit "W" at (150,15)
  - Bot (font3, y=25):  txtX=230, txtY=0  | icon=44 at (236,0) | unit "bpm" at (80,25)
- **`tools/test10.json` created**: Calibration scratchpad for three new visual styles:
  - GAUGE: 7 power-zone segments (Z1–Z7), active zone filled, others dim
  - BAR: horizontal effort % progress bar with border + fill
  - ZONE VIEW: 5 HR zone segments, active zone filled, others dim
- **`preview_layout.py` extended** with rect/rectf/rectf_dim primitive support:
  - `"rect"` — outline rectangle at (x0+x, y0+y)→(x0+x2, y0+y2)
  - `"rectf"` — filled rectangle, full FG_COLOR
  - `"rectf_dim"` — filled rectangle, DIM_COLOR (inactive segments/background)
  - All three auto-flip in `--viewer` mode via existing `rect_box()` helper
  - `ExtraCmd` dataclass gained `x2: int` and `y2: int` fields
  - JSON loader updated to parse `x2`/`y2`

### Session 7 — ExtraCmd Coordinate Bug Fix + Layout Preview Tool
- **Bug fixed**: `layoutClearAndDisplayExtended` was called with `x=170, y=190` instead of zone
  origins `x=30, y=153/89/25`. With x=170, value "25.1" placed at abs-x=370 (off right edge of
  304px display); unit floated at abs-x=180 with no value visible nearby.
- **Fix**: All three render calls in `testRealisticLayout()` corrected to use zone origins.
- **Key rule confirmed**: The `x0/y0` saved in `LayoutParameters` is a default that is **never
  used at render time**. The firmware uses the `x, y` from the `layoutClearAndDisplayExtended`
  call. ExtraCmd positions are relative to those call-time coordinates, not the saved defaults.
- **Unit vertical alignment**: `unit rel-y = txtY` from LayoutParameters → unit baseline
  top-aligns with value baseline. top=22, mid=38, bot=38.
- **Built `tools/preview_layout.py`**: Python desktop layout previewer. Renders 304×256 PNG
  in ~1 second. Replaces the 50-second build+install+glasses cycle for coordinate calibration.
  Uses real SourceSansPro font + real icon PNGs. `--viewer` flag renders in rider perspective
  (coordinate-transform approach — elements repositioned, text anchors swapped, icons rotated
  180° so they stay readable — NOT a raw pixel flip).
- **`tools/test9.json`**: Reference JSON mirroring `testRealisticLayout()`. Edit → render → copy
  confirmed values to Kotlin.
- **`tools/preview/.gitignore`**: Output PNGs ignored from git.
- **`tools/README.md`**: Full docs for the preview tool.

---

## DisplayDebugService.kt — Test Reference

| Test | What it draws | What answer it gives |
|------|---------------|---------------------|
| 1 | Display boundary + safe area + corner labels | Where is (0,0)? Y direction? |
| 2 | Same text at same point with all 8 rotations | How does rotation affect anchor? |
| 3 | Two identical zones with different width values | Is width a SIZE or a right-edge COORDINATE? |
| 4 | Two fonts side by side | Does each font render at correct size? |
| 5 | K2Look vs Official text positions on same zone | Do officialTextPositions match the real result? |
| 6 | Full 3D_FULL layout (3 zones, values displayed) | Does the complete layout look correct? |
| 7 | txtY=38 center calibration | Is font 3 vertically centered at txtY=38? |
| 8 | Icon grid (IDs 0–5, 12, 19, 26 across 3 rows) | Do icon IDs match README? Correct positions? |
| 9 | Realistic layout — [icon][value][unit] with cycling data (speed/power/HR) | Does the full production layout render correctly across all three zones? |
| 10 | Visual styles: gauge (7 zones), bar (effort %), zone view (5 HR zones) | Do rect/rectf primitives render? Do segment positions look right? |

---

## Key Gotchas & Hard-Won Lessons

1. **ActiveLookLayoutEncoder is dead code.** Never instantiated. All encoding goes through
   SDK's `LayoutParameters` → `addSubCommand*()` in `ActiveLookLayoutService.saveLayout()`.

2. **Font 4 and 5 cannot render letters.** Only Space through `;` (0x20–0x3B). Any label or
   unit text MUST use FontChange sub-command to switch to font 1/2/3 first.

3. **LayoutTemplateRegistry has TWO copies** of every template. `registerAllTemplates()` (runtime,
   has R.drawable previews) and `registerAllTemplatesWithoutPreviews()` (unit tests). Keep in sync.

4. **Official heights intentionally clip text.** txtY > zone height is by design. Don't "fix" it.

5. **Sub-command coordinates are relative** to the clipping region, not absolute display coords.

6. **`layoutClearAndDisplay` replays saved sub-commands** (text, font, images) BEFORE drawing
   main text. Main text with `opacity=true` overwrites sub-commands — this is why labels disappeared.
   Use `LayoutExtraCmd` + `layoutClearAndDisplayExtended` for overlays drawn AFTER main text.

7. **SDK byte order**: `LayoutParameters.toBytes()` uses `CommandData` which is big-endian.
   The abandoned `ActiveLookLayoutEncoder` uses little-endian. These are NOT the same thing —
   but since the encoder is dead code, it doesn't matter at runtime.

8. **Config naming**: K2Look configs are `"K2L" + CRC32(profileId)` = 11 chars + NUL = 12 bytes.
   Fits the ActiveLook 12-byte config name field exactly.

9. **LayoutDataField defaults**: `showLabel=true`, `showUnit=true`, `showIcon=true`. All three
   are ON by default. Currently no sub-commands are generated (buildAdditionalCommands returns
   emptyList). When LayoutExtraCmd support is added, these flags will control extra command generation.

10. **Icon IDs in `docs/ActiveLook-Icon-Reference.md` are WRONG.** Do not use. Correct IDs are
    in `docs/Activelook-Visual-Assets/README.md` Image table only. Verified empirically against firmware.

11. **`cfgSet("ALooK")` is required before `imgDisplay` and bitmap ExtraCmd render calls.**
    Without it, icon bitmaps are silently skipped. No error is returned.

12. **`cfgSet("ALooK")` makes `layoutSave` a silent no-op.** The ALooK config is a read-only
    system config — `layoutSave` calls while in that context are discarded without any error or
    feedback. ALL layout saves MUST happen in a user-owned config created with
    `cfgWrite("name", n, 0)`. Pattern: `cfgWrite("K2LDBG", 4, 0)` → `layoutSave(...)×N` →
    `cfgSet("ALooK")` → `layoutClearAndDisplayExtended(...)`. Layout IDs are global across
    configs — save in K2LDBG, render under ALooK context, firmware still finds the layouts.

13. **`layoutClearAndDisplayExtended` x/y are the zone origin for that render call** — they
    override whatever `x0/y0` was saved in `LayoutParameters`. The saved `x0/y0` is a default
    that is never used at render time. ExtraCmd bitmap/text positions are all relative to the
    call-time x/y. If these don't match the saved `x0/y0`, sub-command elements appear detached
    from the main value text.

---

## Development Environment

- **Devcontainer**: Podman with `--userns=keep-id`
- **SSH alias**: `~/.ssh/config` maps `github-private` → `github.com` (also in `postCreateCommand`)
- **Git performance**: `core.untrackedCache=true`, `feature.manyFiles=true` set; ~3-4s for
  `git status` is the floor due to Podman userns stat overhead
- **ADB**: Device `KAROO20ALA091101299` visible on host (Windows). Container cannot reach it
  directly — use host PowerShell for `adb logcat`
- **debugBuild.bat**: `c:\Project\k2-look\debugBuild.bat` — runs `.\gradlew.bat installDebug -x test -x testDebugUnitTest -x lintDebug`. ~50s incremental, skips all tests.
- **Layout preview tool**: `tools/preview_layout.py` — Python 3 + Pillow. ~1s per run.
  `pip install Pillow` required once. Output folder (`tools/preview/`) is gitignored.
  - `tools/test9.json` — calibrated 3-row [icon][value][unit] layout (speed/power/HR)
  - `tools/test10.json` — vis-style scratchpad: gauge / bar / zone view
  - Run: `python tools/preview_layout.py --config tools/test9.json --viewer`
  - Run from tools/: `python preview_layout.py --conf=test9.json --viewer`
  - Supported `extra_cmds` types: `bitmap`, `font`, `text`, `rect`, `rectf`, `rectf_dim`
