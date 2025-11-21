# Setup Reference Projects Script
# This script clones the reference repositories if they don't exist

Write-Host "Setting up reference projects..." -ForegroundColor Cyan
Write-Host ""

$ErrorActionPreference = "Stop"

# Get script directory
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$referenceDir = Join-Path $scriptDir "reference"

# Create reference directory if it doesn't exist
if (-not (Test-Path $referenceDir)) {
    Write-Host "Creating reference directory..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Path $referenceDir | Out-Null
}

# Function to clone or update a repository
function Setup-Repository {
    param (
        [string]$Url,
        [string]$Name,
        [string]$Path
    )

    Write-Host "Setting up $Name..." -ForegroundColor Yellow

    if (Test-Path $Path) {
        Write-Host "  ✓ $Name already exists at: $Path" -ForegroundColor Green
        Write-Host "  Use update-references.ps1 to update it" -ForegroundColor Gray
    }
    else {
        Write-Host "  Cloning from: $Url" -ForegroundColor Gray
        try {
            Push-Location $referenceDir
            git clone $Url
            Write-Host "  ✓ $Name cloned successfully" -ForegroundColor Green
            Pop-Location
        }
        catch {
            Write-Host "  ✗ Error cloning $Name : $_" -ForegroundColor Red
            if (Test-Path $Path) {
                Remove-Item -Path $Path -Recurse -Force
            }
            Pop-Location
            throw
        }
    }
    Write-Host ""
}

Write-Host "Reference directory: $referenceDir" -ForegroundColor Gray
Write-Host ""

# Setup each reference project
try {
    Setup-Repository `
        -Url "https://github.com/ActiveLook/android-sdk.git" `
        -Name "ActiveLook SDK" `
        -Path (Join-Path $referenceDir "android-sdk")

    Setup-Repository `
        -Url "https://github.com/hammerheadnav/karoo-ext.git" `
        -Name "Karoo Extensions" `
        -Path (Join-Path $referenceDir "karoo-ext")

    Setup-Repository `
        -Url "https://github.com/valterc/ki2.git" `
        -Name "Ki2 Reference Project" `
        -Path (Join-Path $referenceDir "ki2")

    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host "All reference projects set up successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "  1. Build the project: .\gradlew :app:assembleDebug" -ForegroundColor Gray
    Write-Host "  2. To update references later: .\update-references.ps1" -ForegroundColor Gray
    Write-Host "============================================" -ForegroundColor Cyan
}
catch {
    Write-Host ""
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host "Setup failed with errors!" -ForegroundColor Red
    Write-Host "Error: $_" -ForegroundColor Red
    Write-Host "============================================" -ForegroundColor Cyan
    exit 1
}

