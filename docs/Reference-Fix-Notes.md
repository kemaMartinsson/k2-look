# Reference Project Fixes - Important Note

## Issue
When running `update-references.ps1`, the reference projects (android-sdk and karoo-ext) get pulled with their original build configurations that use version catalogs (`libs.*`), which don't work in our local module setup.

## Fixed Files

### 1. `reference/android-sdk/ActiveLookSDK/build.gradle`
**Changes needed after update:**
- Line 5: Replace `version "$kotlin_version"` with `version "1.6.21"`
- Line 10: Replace `namespace 'com.activelook.activelooksdk'` with `namespace = 'com.activelook.activelooksdk'` (Gradle 10 compatibility)
- Line 11: Replace `compileSdk 35` with `compileSdk = 35`
- Lines 14-15: Replace `minSdk 21` and `targetSdk 35` with assignment syntax
- Line 58: Replace `implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version"` with hardcoded `"1.6.21"`
- Lines 44-56: Comment out maven publication

### 2. `reference/karoo-ext/lib/build.gradle.kts`
**Changes needed after update:**
- Lines 1-2: Comment out dokka imports
- Line 7-11: Replace `alias(libs.plugins.*)` with hardcoded plugin IDs
- Lines 17-20: Comment out buildscript/dokka dependencies
- Lines 53-89: Comment out publishing and dokka configuration
- Lines 91-99: Replace `libs.*` dependencies with hardcoded versions

## Quick Fix Script

After running `update-references.ps1`, run this:

```powershell
# Fix ActiveLook SDK
$file1 = "C:\Project\k2-look\reference\android-sdk\ActiveLookSDK\build.gradle"
(Get-Content $file1) -replace '\$kotlin_version', '1.6.21' | Set-Content $file1

# Fix karoo-ext (manual for now - complex replacements)
Write-Host "Run the fixes for karoo-ext manually or wait for Phase 1.3"
```

## Permanent Solution

Create a `post-update-fix.ps1` script that automatically applies these fixes after `update-references.ps1` completes.

## Status
✅ All fixes applied  
✅ Build successful  
✅ No compilation errors  

**Ready for Phase 1.3!** 🚀

*Last fixed: 2024-11-22*

