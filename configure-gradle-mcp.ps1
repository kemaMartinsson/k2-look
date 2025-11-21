# Gradle MCP Server Configuration Script for Claude Desktop

$homeDir = $env:USERPROFILE
$jarPath = Join-Path $homeDir "mcp-servers\gradle-mcp-server\gradle-mcp-server-all.jar"
$claudeConfigPath = Join-Path $env:APPDATA "Claude\claude_desktop_config.json"
$androidStudioConfigPath = Join-Path $env:APPDATA "Google\AndroidStudio2025.2.1\options\mcp_settings.json"

Write-Host "Configuring Gradle MCP Server..." -ForegroundColor Cyan
Write-Host ""

# Check if JAR exists
if (-not (Test-Path $jarPath)) {
    Write-Host "✗ Error: Gradle MCP Server JAR not found at: $jarPath" -ForegroundColor Red
    Write-Host "  Please run the installation script first." -ForegroundColor Yellow
    exit 1
}
Write-Host "✓ Found Gradle MCP Server JAR" -ForegroundColor Green

# Create Claude config directory if it doesn't exist
$claudeDir = Split-Path $claudeConfigPath -Parent
if (-not (Test-Path $claudeDir)) {
    Write-Host "Creating Claude configuration directory..." -ForegroundColor Gray
    New-Item -ItemType Directory -Path $claudeDir -Force | Out-Null
}

# Check if config file exists
if (Test-Path $claudeConfigPath) {
    Write-Host "✓ Found existing Claude configuration" -ForegroundColor Green

    # Read existing config
    $configText = Get-Content $claudeConfigPath -Raw
    $config = $configText | ConvertFrom-Json

    # Add or update mcpServers
    if (-not $config.mcpServers) {
        $config | Add-Member -NotePropertyName "mcpServers" -NotePropertyValue ([PSCustomObject]@{}) -Force
    }

    # Add gradle-mcp-server
    $gradleServer = [PSCustomObject]@{
        command = "java"
        args = @("-jar", $jarPath)
        env = [PSCustomObject]@{}
        disabled = $false
        autoApprove = @()
    }

    $config.mcpServers | Add-Member -NotePropertyName "gradle-mcp-server" -NotePropertyValue $gradleServer -Force

    # Save updated config
    $config | ConvertTo-Json -Depth 10 | Set-Content $claudeConfigPath -Encoding UTF8
    Write-Host "✓ Updated existing configuration" -ForegroundColor Green
}
else {
    Write-Host "Creating new Claude configuration..." -ForegroundColor Gray

    # Create new config
    $config = [PSCustomObject]@{
        mcpServers = [PSCustomObject]@{
            "gradle-mcp-server" = [PSCustomObject]@{
                command = "java"
                args = @("-jar", $jarPath)
                env = [PSCustomObject]@{}
                disabled = $false
                autoApprove = @()
            }
        }
    }

    # Save new config
    $config | ConvertTo-Json -Depth 10 | Set-Content $claudeConfigPath -Encoding UTF8
    Write-Host "✓ Created new configuration" -ForegroundColor Green
}

Write-Host ""

# Configure Android Studio
$androidStudioDir = Split-Path $androidStudioConfigPath -Parent
if (Test-Path $androidStudioDir) {
    Write-Host "Configuring for Android Studio..." -ForegroundColor Yellow

    if (Test-Path $androidStudioConfigPath) {
        Write-Host "✓ Found existing Android Studio configuration" -ForegroundColor Green

        # Read existing config
        $configText = Get-Content $androidStudioConfigPath -Raw
        $config = $configText | ConvertFrom-Json

        # Add or update mcpServers
        if (-not $config.mcpServers) {
            $config | Add-Member -NotePropertyName "mcpServers" -NotePropertyValue ([PSCustomObject]@{}) -Force
        }

        # Add gradle-mcp-server
        $gradleServer = [PSCustomObject]@{
            command = "java"
            args = @("-jar", $jarPath)
            env = [PSCustomObject]@{}
            disabled = $false
            autoApprove = @()
        }

        $config.mcpServers | Add-Member -NotePropertyName "gradle-mcp-server" -NotePropertyValue $gradleServer -Force

        # Save updated config
        $config | ConvertTo-Json -Depth 10 | Set-Content $androidStudioConfigPath -Encoding UTF8
        Write-Host "✓ Updated existing configuration" -ForegroundColor Green
    }
    else {
        Write-Host "Creating new Android Studio configuration..." -ForegroundColor Gray

        # Create new config
        $config = [PSCustomObject]@{
            mcpServers = [PSCustomObject]@{
                "gradle-mcp-server" = [PSCustomObject]@{
                    command = "java"
                    args = @("-jar", $jarPath)
                    env = [PSCustomObject]@{}
                    disabled = $false
                    autoApprove = @()
                }
            }
        }

        # Save new config
        $config | ConvertTo-Json -Depth 10 | Set-Content $androidStudioConfigPath -Encoding UTF8
        Write-Host "✓ Created new configuration" -ForegroundColor Green
    }
}
else {
    Write-Host "Android Studio configuration directory not found:" -ForegroundColor Yellow
    Write-Host "  $androidStudioDir" -ForegroundColor Gray
    Write-Host "  Skipping Android Studio configuration" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Gradle MCP Server configured successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "Configuration files:" -ForegroundColor Gray
if (Test-Path $claudeConfigPath) {
    Write-Host "  Claude Desktop: $claudeConfigPath" -ForegroundColor Green
}
if (Test-Path $androidStudioConfigPath) {
    Write-Host "  Android Studio: $androidStudioConfigPath" -ForegroundColor Green
}
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Restart Android Studio and/or Claude Desktop" -ForegroundColor Gray
Write-Host "  2. The gradle-mcp-server tools will be available" -ForegroundColor Gray
Write-Host "  3. Test by asking AI about your Gradle project" -ForegroundColor Gray
Write-Host ""
Write-Host "Available tools:" -ForegroundColor Yellow
Write-Host "  - get_gradle_project_info: Query project structure and tasks" -ForegroundColor Gray
Write-Host "  - execute_gradle_task: Run Gradle tasks programmatically" -ForegroundColor Gray
Write-Host "  - run_gradle_tests: Execute tests with detailed results" -ForegroundColor Gray
Write-Host "============================================" -ForegroundColor Cyan

