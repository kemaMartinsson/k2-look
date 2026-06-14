# K2Look Design Spec: Simulator Mode + Save Logs To File

Date: 2026-06-14
Status: Draft approved in chat, pending final user review of this file
Scope: Design only (no implementation in this spec)

## 1. Problem Statement

Users need reliable bug-report logs after rides, but current logcat output is too volatile and gets overwritten. Existing "Debug Mode" also mixes two concerns:
- simulator/testing behavior
- logging behavior

This makes ride diagnostics harder and led to confusing behavior (for example auto-disable during rides).

## 2. Goals

1. Split current debug concept into two independent user-facing features.
2. Rename the existing debug toggle to Simulator Mode.
3. Add a separate Save logs to file toggle that can stay enabled during rides.
4. Persist app log output to on-device files for later adb retrieval.
5. Retain the latest 7 log files by file count (not day count).

## 3. Non-Goals

1. No in-app share/export flow in this iteration (option 2 selected by user).
2. No changes to BLE, layout rendering, or ride metric pipelines.
3. No broad infrastructure rewrite across all app logging in one step unless needed for safe integration.

## 4. Product Decisions (Locked)

1. Retrieval mode: Option 2 (save only; advanced users pull via adb).
2. Log scope: include app logs through the app logger path (user selected "everything the app logs through android.util.Log").
3. Retention strategy: keep latest 7 files (not 7 days).
4. Ride behavior: remove auto-disable debug behavior; simulator and file logging are separated.
5. UX naming: Debug Mode is renamed to Simulator Mode.

## 5. Architecture

### 5.1 Simulator Mode Feature

Purpose:
- synthetic metrics generation
- display debug test tooling

Behavior:
- independent toggle
- no ride-triggered forced disable
- continues to gate simulator-only actions

### 5.2 Log Capture Feature

Purpose:
- persistent diagnostic logging suitable for bug reports after ride completion

Behavior:
- independent Save logs to file toggle
- can remain enabled while riding
- writes logs to files in app-scoped storage

### 5.3 Ownership Boundaries

1. ViewModel/UI state layer:
- owns the two user toggles and wiring to services

2. Logger file service:
- owns file creation, append, flush/close, and rotation (keep 7)
- never crashes app flows on write failure

3. Existing runtime services:
- no functional changes beyond logging integration points

## 6. Data Flow

### 6.1 Logging Path

1. App emits log event.
2. App logger wrapper forwards to normal logcat output.
3. If Save logs to file is enabled, same event is appended to active log file.
4. On error writing file, fallback keeps logcat path active and disables file sink for current session.

### 6.2 File Location and Naming

Storage location:
- app external files directory under a logs folder
- app-scoped path (no additional dangerous permission required)

File naming:
- per-session timestamped file, example:
  - k2look_YYYYMMDD_HHmmss.log

### 6.3 Rotation / Retention

Policy:
- retain newest 7 files by creation or sortable timestamp in filename
- delete older files during logger startup and after creating a new file

## 7. UX / Settings

1. Rename Debug Mode -> Simulator Mode in debug dialog.
2. Add Save logs to file switch in same debug dialog.
3. Add helper text for adb retrieval workflow.
4. Persist switch state in settings repository with dedicated key.

## 8. Error Handling and Reliability

1. All file operations wrapped in try/catch.
2. Logging failure does not propagate to user-critical flows.
3. File sink can self-disable on repeated failures while keeping logcat path active.
4. Writer access must be thread-safe to prevent line corruption.

## 9. Testing Strategy

### 9.1 Unit Tests

1. Settings persistence for new save-to-file flag.
2. File rotation algorithm keeps 7 newest files.
3. Log formatter includes timestamp/level/tag/message and optional throwable.
4. Toggle ON/OFF behavior starts/stops appending correctly.

### 9.2 Manual Validation (Karoo)

1. Enable Save logs to file.
2. Perform real ride with simulator mode off.
3. Disable Save logs to file.
4. Pull logs via adb and verify ride timeline events.
5. Repeat enough sessions to exceed 7 files and verify retention behavior.

## 10. Risks and Mitigations

1. Risk: partial integration if some code paths still call raw Log directly.
- Mitigation: implementation plan includes migration checklist for high-value logging paths first.

2. Risk: storage I/O latency.
- Mitigation: buffered writer and lightweight formatting; no blocking work on critical UI paths.

3. Risk: user confusion between simulator and logging.
- Mitigation: explicit naming and helper descriptions in Debug dialog.

## 11. Rollout Notes

1. Backward compatible with existing app behavior except removal of forced debug auto-disable.
2. Existing debug test functions remain under Simulator Mode gating.
3. No permission model changes expected for scoped app external files usage.

## 12. Open Items for Implementation Plan

1. Exact logger wrapper API shape and adoption strategy (centralized wrapper vs staged migration).
2. Final adb pull command examples for docs/release notes.
3. UI copy finalization for switch descriptions.

---

This design is approved conceptually in chat and is now awaiting explicit review of this written spec before implementation planning.
