# Feature: Import Karoo Profile

**Status**: Draft  
**Priority**: High  
**Complexity**: Medium  
**Target Version**: TBD

---

## Overview

K2Look can read the user's Karoo ride profile configuration (pages + data fields) from the SDK and
auto-generate a matching ActiveLook layout. This eliminates the main setup friction: the user has
already decided what metrics they want on their Karoo — K2Look should just mirror that.

There are two entry points, both **non-intrusive and never triggered during an active ride**:

1. **On profile creation** — user names a new K2Look profile to match an existing Karoo profile →
   import button appears immediately.
2. **In profile management (idle only)** — Karoo profiles without a matching K2Look profile are
   surfaced as suggestions the user can act on at any time.

Auto-switching on Karoo profile change (when a match already exists) is **already implemented**
in `KarooActiveLookBridge.tryAutoSwitchProfile()`.

---

## Karoo Sync Toggle

A **Karoo Sync** toggle (enabled by default) lets users opt out of all automatic Karoo-driven
behaviour. Users who manage K2Look profiles independently — ignoring Karoo's active profile
entirely — should be able to turn this off once and never see sync-related UI again.

| Toggle state | Effect |
|---|---|
| **Enabled** (default) | Auto-switch on profile change; suggestions shown in ProfileManagementScreen |
| **Disabled** | No auto-switch; no "From Karoo" suggestions; manual profile selection only |

The toggle is a boolean preference stored in `SharedPreferences` (key: `karoo_sync_enabled`,
default `true`). Exposed as `karooSyncEnabled: StateFlow<Boolean>` in `LayoutBuilderViewModel`
and rendered as a `Switch` at the top of the **ProfileManagementScreen**.

---

## Behaviour by Situation

| Situation | Behaviour |
|---|---|
| Karoo profile changes → matching K2Look profile exists → **sync enabled** | Auto-switch silently ✅ Already works |
| Karoo profile changes → matching K2Look profile exists → **sync disabled** | Do nothing. |
| Karoo profile changes → no match → **during ride** | Do nothing. No prompt. |
| Karoo profile changes → no match → **not riding** → **sync enabled** | Show unmatched profile in ProfileManagementScreen as a suggestion |
| Karoo profile changes → no match → **not riding** → **sync disabled** | Do nothing. |
| User creates K2Look profile → name matches Karoo profile → **sync enabled** | Show "Import from Karoo" button in the create dialog |

The rule is simple: **never interrupt a ride**. Suggestions only appear in ProfileManagementScreen,
which the user opens deliberately when not riding.

---

## User Stories

> **Setup flow**: I create a K2Look profile named "Race Day". K2Look detects it matches my Karoo
> profile, shows an Import button, reads my Karoo pages, and generates a matching layout. Done.

> **Discovery flow**: I open Profile Management while not riding. I see a "From Karoo" section
> showing my Karoo profiles that have no K2Look match. I tap "Import 'Gravel'" and it generates
> one automatically.

---

## What the Karoo SDK Exposes

The Karoo SDK (v1.1.5+) provides the `ActiveRideProfile` event, delivering the full current
`RideProfile`. `KarooDataService` already consumes it via `activeRideProfile: StateFlow<RideProfile?>`.

```kotlin
data class RideProfile(
    val id: String,
    val name: String,
    val pages: List<Page>,
    val indoor: Boolean,
    val defaultActivityType: String,   // RIDE, EBIKE, GRAVEL, etc.
) {
    data class Page(
        val mapPage: Boolean,
        val elements: List<Element>,
    ) {
        data class Element(
            val dataTypeId: String,    // Same IDs used in DataFieldRegistry.karooStreamType
            val gridSize: Pair<Int, Int>
        )
    }
}
```

**Note**: The SDK only delivers `ActiveRideProfile` — the currently active profile, not the full
list of all configured Karoo profiles. Suggestions in ProfileManagementScreen are therefore limited
to the profile that was active when the app last connected.

---

## Import Flow

```
1. Trigger (either entry point above)
2. Read all non-map pages from the matched RideProfile
3. For each page:
   a. Count elements → pick closest LayoutTemplate (see table below)
   b. Map each element.dataTypeId → DataField via DataFieldRegistry
   c. Assign fields to template zones in order
4. Build DataFieldProfile: one LayoutScreen per page
5. Show preview card: "3 screens — Speed+HR+Power / Distance+Time / Cadence"
6. [Import] → save via ProfileRepository + activate  |  [Cancel] → discard
```

---

## Mapping Logic

### Template Selection (by field count)

| Karoo page fields | LayoutTemplate |
|---|---|
| 1 | `1D` |
| 2 | `2D` |
| 3 | `3D_FULL` |
| 4 | `4D` |
| 5 | `5D` |
| 6 | `6D` |
| >6 | `6D` — first 6 fields used, rest skipped |

### Field Mapping

`DataFieldRegistry.ALL_FIELDS` keyed by `karooStreamType` = `dataTypeId`. Unknown IDs (third-party
extensions) are silently skipped.

### Zone Assignment

Fields assigned to template zones in declaration order. Karoo grid positions are not mapped
(no reliable translation to ActiveLook fixed zones).

---

## Ride State Guard

Import can only be triggered when `RideState` is `Idle`. The ViewModel checks this before showing
import UI. If a ride starts while the import dialog is open, the dialog is dismissed.

`RideState` is already available via `KarooDataService.rideState: StateFlow<RideState>`.

---

## Limitations

- Only the **active** Karoo profile is readable (SDK limitation).
- Map pages are skipped.
- Pages with >6 fields are truncated to 6.
- Third-party data fields are skipped.
- SDK is read-only — K2Look cannot write back to Karoo.

---

## Files Affected

| File | Change |
|------|--------|
| `sharing/KarooProfileImporter.kt` | New — maps `RideProfile` → `DataFieldProfile` |
| `viewmodel/LayoutBuilderViewModel.kt` | Add `importFromKaroo()`, expose ride state + unmatched profile suggestion |
| `screens/ProfileManagementScreen.kt` | "From Karoo" suggestion section + import preview dialog |
| `screens/DataFieldBuilderTab.kt` | Pass `activeRideProfile` + `rideState` to ProfileManagementScreen |

---

## Out of Scope

- Listing all Karoo profiles (SDK only exposes the active one).
- Automatic re-sync when Karoo profile fields change mid-ride.
- File-based export/import.
- Mapping third-party extension fields.
