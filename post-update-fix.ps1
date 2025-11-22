# Post-Update Fix Script
# Automatically applies necessary fixes to reference projects after update-references.ps1

Write-Host "Applying post-update fixes to reference projects..." -ForegroundColor Cyan
Write-Host ""

$ErrorActionPreference = "Continue"

# Fix 1: ActiveLook SDK build.gradle
Write-Host "Fixing ActiveLook SDK build.gradle..." -ForegroundColor Yellow
$activeLookBuildFile = "C:\Project\k2-look\reference\android-sdk\ActiveLookSDK\build.gradle"

if (Test-Path $activeLookBuildFile) {
    $content = Get-Content $activeLookBuildFile -Raw

    # Fix kotlin_version variable
    $content = $content -replace '\$kotlin_version', '1.6.21'

    # Fix deprecated Groovy syntax
    $content = $content -replace "namespace 'com\.activelook\.activelooksdk'", "namespace = 'com.activelook.activelooksdk'"
    $content = $content -replace 'compileSdk 35', 'compileSdk = 35'
    $content = $content -replace 'minSdk 21', 'minSdk = 21'
    $content = $content -replace 'targetSdk 35', 'targetSdk = 35'

    # Comment out maven publication if not already done
    if ($content -notmatch '// afterEvaluate') {
        $content = $content -replace 'afterEvaluate \{', '// Commented out maven publication - not needed for local usage`r`n// afterEvaluate {'
        $content = $content -replace '(\s+)publications \{', '$1// publications {'
        $content = $content -replace '(\s+)release\(MavenPublication\)', '$1// release(MavenPublication)'
        $content = $content -replace '(\s+)from components\.release', '$1// from components.release'
        $content = $content -replace '(\s+)groupId = "\$sdkGroupId"', '$1// groupId = "$sdkGroupId"'
        $content = $content -replace '(\s+)artifactId = "sdk"', '$1// artifactId = "sdk"'
        $content = $content -replace '(\s+)version = "\$sdkVersionName"', '$1// version = "$sdkVersionName"'
    }

    Set-Content $activeLookBuildFile $content -Encoding UTF8
    Write-Host "  ✓ Fixed ActiveLook SDK build.gradle" -ForegroundColor Green
}
else {
    Write-Host "  ✗ ActiveLook SDK build.gradle not found" -ForegroundColor Red
}

Write-Host ""

# Fix 2: karoo-ext lib/build.gradle.kts
Write-Host "Fixing karoo-ext build.gradle.kts..." -ForegroundColor Yellow
$karooExtBuildFile = "C:\Project\k2-look\reference\karoo-ext\lib\build.gradle.kts"

if (Test-Path $karooExtBuildFile) {
    $content = Get-Content $karooExtBuildFile -Raw

    # Comment out dokka imports
    $content = $content -replace '^import org\.jetbrains\.dokka', '// import org.jetbrains.dokka'

    # Replace alias with hardcoded plugins
    $content = $content -replace 'alias\(libs\.plugins\.android\.library\)', 'id("com.android.library")'
    $content = $content -replace 'alias\(libs\.plugins\.jetbrains\.kotlin\.android\)', 'id("org.jetbrains.kotlin.android") version "2.0.0"'
    $content = $content -replace 'alias\(libs\.plugins\.jetbrains\.dokka\)', '// id("org.jetbrains.dokka") version "1.9.20"'
    $content = $content -replace 'alias\(libs\.plugins\.jetbrains\.kotlin\.serialization\)', 'id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"'

    # Comment out maven-publish
    $content = $content -replace '`maven-publish`', '// `maven-publish`'

    # Comment out buildscript
    if ($content -match 'buildscript \{') {
        $content = $content -replace 'buildscript \{', '// buildscript {'
        $content = $content -replace '(\s+)dependencies \{', '$1// dependencies {'
        $content = $content -replace 'classpath\(libs\.jetbrains\.dokka\.android\)', '// classpath("org.jetbrains.dokka:android-documentation-plugin:1.9.20")'
    }

    # Replace libs.* dependencies with hardcoded versions
    $content = $content -replace 'implementation\(libs\.androidx\.core\.ktx\)', 'implementation("androidx.core:core-ktx:1.13.1")'
    $content = $content -replace 'implementation\(libs\.androidx\.appcompat\)', 'implementation("androidx.appcompat:appcompat:1.7.0")'
    $content = $content -replace 'implementation\(libs\.kotlinx\.serialization\.json\)', 'implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")'
    $content = $content -replace 'implementation\(libs\.timber\)', 'implementation("com.jakewharton.timber:timber:5.0.1")'
    $content = $content -replace 'dokkaPlugin\(libs\.jetbrains\.dokka\.android\)', '// dokkaPlugin("org.jetbrains.dokka:android-documentation-plugin:1.9.20")'
    $content = $content -replace 'testImplementation\(libs\.junit\)', 'testImplementation("junit:junit:4.13.2")'
    $content = $content -replace 'androidTestImplementation\(libs\.androidx\.junit\)', 'androidTestImplementation("androidx.test.ext:junit:1.2.1")'

    # Comment out publishing section
    if ($content -match 'publishing \{' -and $content -notmatch '// publishing \{') {
        # This is complex - would need a more sophisticated approach
        Write-Host "  ⚠ Manual review needed for publishing section" -ForegroundColor Yellow
    }

    Set-Content $karooExtBuildFile $content -Encoding UTF8
    Write-Host "  ✓ Fixed karoo-ext build.gradle.kts" -ForegroundColor Green
}
else {
    Write-Host "  ✗ karoo-ext build.gradle.kts not found" -ForegroundColor Red
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Post-update fixes applied!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Test build: .\gradlew :app:assembleDebug" -ForegroundColor Gray
Write-Host "  2. If build fails, check Reference-Fix-Notes.md" -ForegroundColor Gray
Write-Host "============================================" -ForegroundColor Cyan

