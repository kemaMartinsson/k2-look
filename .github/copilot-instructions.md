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

## Active Feature: Import Karoo Profile

**Branch**: `feature/import-k2-profiles`  
**Spec**: `docs/Feature-Profile-Import.md`  
**Status**: Designed, not yet implemented. Ready to code.

### What It Does

Reads the user's Karoo ride profile (pages + fields) from the SDK and auto-generates a matching
K2Look/ActiveLook layout. Eliminates manual setup — the user configured Karoo already, K2Look
mirrors it.

### Key Design Decisions

1. **Never prompt during a ride.** `RideState.Idle` check required before showing any import UI.
   Dialog dismissed if ride starts while open.
2. **Event-driven, no polling.** `ActiveRideProfile` is a `KarooEvent` — `addConsumer` fires
   immediately on subscribe and again on every profile change. Already flows through
   `KarooDataService._activeRideProfile: MutableStateFlow<RideProfile?>`.
3. **Auto-switch already works.** `KarooActiveLookBridge.tryAutoSwitchProfile()` already
   switches the active K2Look profile when the Karoo profile changes and a name match exists.
   The new feature fills the gap when no match exists yet.

### Karoo Sync Toggle

A **Karoo Sync** on/off toggle (default: on). Stored as `karoo_sync_enabled` boolean in
`SharedPreferences`. Exposed as `karooSyncEnabled: StateFlow<Boolean>` in `LayoutBuilderViewModel`.
Rendered as a `Switch` at the top of `ProfileManagementScreen`.

When **disabled**: no auto-switch, no "From Karoo" suggestions — fully manual profile management.

### Behaviour by Situation

| Situation | Behaviour |
|---|---|
| Karoo profile changes → matching K2Look profile exists → **sync on** | Auto-switch silently ✅ **Already works** |
| Karoo profile changes → matching K2Look profile exists → **sync off** | Do nothing. |
| Karoo profile changes → no match → **during ride** | Do nothing. No prompt. |
| Karoo profile changes → no match → **not riding** → **sync on** | Surface suggestion in ProfileManagementScreen |
| Karoo profile changes → no match → **not riding** → **sync off** | Do nothing. |
| User creates K2Look profile → name matches Karoo profile → **sync on** | Show "Import from Karoo" button inline |

### Files To Create/Modify

| File | Status | What |
|---|---|---|
| `app/.../sharing/KarooProfileImporter.kt` | **TODO — create** | Maps `RideProfile` → `DataFieldProfile` |
| `app/.../data/SettingsRepository.kt` | **TODO — create** | `karoo_sync_enabled` boolean pref (default `true`) |
| `app/.../viewmodel/LayoutBuilderViewModel.kt` | **TODO — modify** | Add `importFromKaroo()`, `karooSyncEnabled` toggle, expose `activeRideProfile` + `rideState` in `UiState` |
| `app/.../screens/ProfileManagementScreen.kt` | **TODO — modify** | Karoo Sync `Switch` + "From Karoo" suggestion section + import preview dialog |
| `app/.../screens/DataFieldBuilderTab.kt` | **TODO — modify** | Pass `activeRideProfile`, `rideState`, `karooSyncEnabled` down to ProfileManagementScreen |

### Mapping Logic

**Template selection** (by number of fields on the Karoo page):

| Fields | LayoutTemplate |
|---|---|
| 1 | `1D` |
| 2 | `2D` |
| 3 | `3D_FULL` |
| 4 | `4D` |
| 5 | `5D` |
| 6+ | `6D` (truncate to 6) |

**Field mapping**: `RideProfile.Page.Element.dataTypeId` == `DataField.karooStreamType`.
Look up in `DataFieldRegistry.ALL_FIELDS`. Skip unknowns (third-party extensions) with a log warning.

**Zone assignment**: assign fields to template zones in declaration order. Karoo grid positions
have no reliable mapping to ActiveLook fixed zones — ignore them.

### Key SDK Facts

- `ActiveRideProfile` event: fires on subscribe (current value) + on every change. **Push-based.**
- `RideState`: `Idle`, `Recording`, `Paused`. Already in `KarooDataService.rideState`.
- SDK is **read-only** for profiles — cannot write back to Karoo.
- Only the **active** Karoo profile is accessible. Cannot list all profiles.

---

## Other Completed Work (This Session)

- Fixed 4 bugs in `UpdateDownloader.kt` (thread leak, wrong-thread Compose update, false
  `onComplete(true)`, silent `ActivityNotFoundException`)
- Fixed 2 issues in `UpdateChecker.kt` (connection leak, prerelease filter)
- Created `docs/Feature-Profile-Import.md` (full feature spec)
- Decided against Gist-based sharing and TOON format (wrong tools for this use case)

---

## Development Environment

- **Devcontainer**: Podman with `--userns=keep-id`
- **SSH alias**: `~/.ssh/config` maps `github-private` → `github.com` (also in `postCreateCommand`)
- **Git performance**: `core.untrackedCache=true`, `feature.manyFiles=true` set; ~3-4s for
  `git status` is the floor due to Podman userns stat overhead
- **ADB**: Device `KAROO20ALA091101299` visible on host (Windows). Container cannot reach it
  directly — use host PowerShell for `adb logcat`
