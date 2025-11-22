﻿# Update Reference Projects Script
# This script pulls the latest updates from the reference repositories

Write-Host "Updating reference projects..." -ForegroundColor Cyan
Write-Host ""

$ErrorActionPreference = "Continue"
$hasErrors = $false

# Function to update a git repository
function Update-Repository {
    param (
        [string]$Path,
        [string]$Name
    )

    Write-Host "Updating $Name..." -ForegroundColor Yellow

    if (Test-Path $Path) {
        Push-Location $Path

        try {
            # Fetch latest changes
            Write-Host "  Fetching latest changes..." -ForegroundColor Gray
            git fetch origin

            # Get current branch
            $currentBranch = git rev-parse --abbrev-ref HEAD
            Write-Host "  Current branch: $currentBranch" -ForegroundColor Gray

            # Check for local changes
            $status = git status --porcelain
            if ($status) {
                Write-Host "  WARNING: Local changes detected in $Name" -ForegroundColor Red
                Write-Host "  Stashing local changes..." -ForegroundColor Gray
                git stash save "Auto-stash before update $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
            }

            # Pull latest changes
            Write-Host "  Pulling latest changes..." -ForegroundColor Gray
            git pull origin $currentBranch

            # Show latest commit
            $latestCommit = git log -1 --pretty=format:"%h - %s (%cr by %an)"
            Write-Host "  Latest commit: $latestCommit" -ForegroundColor Green

            Write-Host "  ✓ $Name updated successfully" -ForegroundColor Green
            Write-Host ""
        }
        catch {
            Write-Host "  ✗ Error updating $Name : $_" -ForegroundColor Red
            Write-Host ""
            $script:hasErrors = $true
        }
        finally {
            Pop-Location
        }
    }
    else {
        Write-Host "  ✗ Repository not found at: $Path" -ForegroundColor Red
        Write-Host "  Run setup-references.ps1 to clone the repositories" -ForegroundColor Yellow
        Write-Host ""
        $script:hasErrors = $true
    }
}

# Get script directory
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$referenceDir = Join-Path $scriptDir "reference"

Write-Host "Reference directory: $referenceDir" -ForegroundColor Gray
Write-Host ""

# Update each reference project
Update-Repository -Path (Join-Path $referenceDir "android-sdk") -Name "ActiveLook SDK"
Update-Repository -Path (Join-Path $referenceDir "karoo-ext") -Name "Karoo Extensions"
Update-Repository -Path (Join-Path $referenceDir "ki2") -Name "Ki2 Reference Project"

# Summary
Write-Host "============================================" -ForegroundColor Cyan
if ($hasErrors) {
    Write-Host "Update completed with errors!" -ForegroundColor Red
    Write-Host "Please review the errors above and fix them manually." -ForegroundColor Yellow
    exit 1
}
else {
    Write-Host "All reference projects updated successfully!" -ForegroundColor Green
    Write-Host ""

    # Run post-update fixes
    Write-Host "Running post-update fixes..." -ForegroundColor Yellow
    $fixScript = Join-Path $scriptDir "post-update-fix.ps1"
    if (Test-Path $fixScript) {
        & $fixScript
    }
    else {
        Write-Host "⚠ post-update-fix.ps1 not found - skipping automatic fixes" -ForegroundColor Yellow
    }

    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "  1. Review changes: git log in each reference directory" -ForegroundColor Gray
    Write-Host "  2. Rebuild project: .\gradlew clean :app:assembleDebug" -ForegroundColor Gray
    Write-Host "  3. Test the app to ensure compatibility" -ForegroundColor Gray
}
Write-Host "============================================" -ForegroundColor Cyan

