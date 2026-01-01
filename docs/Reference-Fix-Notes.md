# Reference Project Fixes - Historical Note

> **⚠️ IMPORTANT:** As of 2024-11-22, reference projects (android-sdk and karoo-ext) are **vendored
directly in the repository**.
> The `update-references.ps1` and `post-update-fix.ps1` scripts exist but are **no longer actively
used** in normal development.
> This document is kept for historical reference and for maintainers who may need to update the
> vendored dependencies.

## Current Status (2026-01-01)

✅ **Reference projects are committed to the repo**  
✅ **`post-update-fix.ps1` script exists** and can automatically fix issues  
✅ **No manual fixes needed** for regular development  
⚠️ **Only relevant when updating vendored dependencies**

## Background: Why This Document Exists

When the reference projects were originally managed as git submodules (deprecated approach), they
would pull with upstream build configurations that used version catalogs (`libs.*`) which didn't
work in our local module setup. Manual fixes were required after each update.

**This is no longer an issue** because:

1. Reference projects are now vendored (committed directly to repo)
2. The `post-update-fix.ps1` script automates all fixes
3. Updates are rare and controlled by maintainers

---

## For Maintainers: Updating Vendored Dependencies

If you need to update the vendored reference projects to newer versions:

### Step 1: Run Update Script

```powershell
.\update-references.ps1
```

### Step 2: Run Post-Update Fixes

```powershell
.\post-update-fix.ps1
```

### Step 3: Verify Build

```powershell
.\gradlew :app:assembleDebug
```

---

## Manual Fixes (If Automation Fails)

### 1. `reference/android-sdk/ActiveLookSDK/build.gradle`

**Changes needed after update:**

- Line 5: Replace `version "$kotlin_version"` with `version "1.6.21"`
- Line 10: Replace `namespace 'com.activelook.activelooksdk'` with
  `namespace = 'com.activelook.activelooksdk'` (Gradle 10 compatibility)
- Line 11: Replace `compileSdk 35` with `compileSdk = 35`
- Lines 14-15: Replace `minSdk 21` and `targetSdk 35` with assignment syntax
- Line 58: Replace `implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version"` with
  hardcoded `"1.6.21"`
- Lines 44-56: Comment out maven publication

### 2. `reference/karoo-ext/lib/build.gradle.kts`

**Changes needed after update:**

- Lines 1-2: Comment out dokka imports
- Line 7-11: Replace `alias(libs.plugins.*)` with hardcoded plugin IDs:
    - `id("com.android.library")`
    - `id("org.jetbrains.kotlin.android") version "2.0.0"`
    - `id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"`
- Lines 17-20: Comment out buildscript/dokka dependencies
- Lines 53-89: Comment out publishing and dokka configuration
- Lines 91-99: Replace `libs.*` dependencies with hardcoded versions:
    - `androidx.core:core-ktx:1.13.1`
    - `androidx.appcompat:appcompat:1.7.0`
    - `kotlinx-serialization-json:1.6.3`
    - `timber:5.0.1`
    - `junit:4.13.2`

---

## Current Versions (Vendored)

- **ActiveLook Android SDK**: v4.5.6
- **Karoo Extensions**: v1.1.7
- **Last vendored**: 2024-11-22
- **Build status**: ✅ Working
- **Post-fix automation**: ✅ Implemented

---

## Why Keep Vendored Approach?

✅ **No authentication required** - Works without GitHub tokens  
✅ **Deterministic builds** - Same code everywhere  
✅ **Offline builds** - No network dependency  
✅ **CI/CD friendly** - No secrets management needed  
✅ **Custom patches possible** - Can fix issues locally

---

*Document purpose: Historical reference and maintainer guide*  
*Last reviewed: 2026-01-01*

