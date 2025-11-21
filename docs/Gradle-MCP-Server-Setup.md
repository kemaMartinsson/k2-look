# Gradle MCP Server Configuration for Android Studio

## Status: Downloaded ✅

The Gradle MCP Server JAR has been downloaded to:
```
%USERPROFILE%\mcp-servers\gradle-mcp-server\gradle-mcp-server-all.jar
```

---

## Configuration Options

### Option 1: GitHub Copilot in Android Studio (If Installed)

If you have GitHub Copilot extension in Android Studio:

1. **Open Settings**: `File → Settings` (or `Ctrl+Alt+S`)
2. **Navigate to**: `Tools → MCP Servers` (if available)
3. **Add New Server**:
   - Name: `gradle-mcp-server`
   - Command: `java`
   - Arguments: `-jar %USERPROFILE%\mcp-servers\gradle-mcp-server\gradle-mcp-server-all.jar`

### Option 2: Claude Desktop App (Recommended)

Configure in Claude Desktop to use with all your development work:

**Configuration File Location:**
```
%USERPROFILE%\AppData\Roaming\Claude\claude_desktop_config.json
```

**Add this configuration:**
```json
{
  "mcpServers": {
    "gradle-mcp-server": {
      "command": "java",
      "args": [
        "-jar",
        "C:\\Users\\YourUsername\\mcp-servers\\gradle-mcp-server\\gradle-mcp-server-all.jar"
      ],
      "env": {},
      "disabled": false,
      "autoApprove": []
    }
  }
}
```

**Replace `C:\\Users\\YourUsername` with your actual home path.**

### Option 3: VSCode with Cline Extension

**Configuration File Location:**
```
%APPDATA%\Code\User\globalStorage\saoudrizwan.claude-dev\settings\cline_mcp_settings.json
```

Or for VSCodium:
```
%APPDATA%\VSCodium\User\globalStorage\saoudrizwan.claude-dev\settings\cline_mcp_settings.json
```

**Add same configuration as Claude Desktop.**

---

## Automatic Configuration Script

Run this script to automatically configure for Claude Desktop:

### configure-gradle-mcp.ps1

```powershell
# Gradle MCP Server Configuration Script for Claude Desktop

$homeDir = $env:USERPROFILE
$jarPath = Join-Path $homeDir "mcp-servers\gradle-mcp-server\gradle-mcp-server-all.jar"
$claudeConfigPath = Join-Path $env:APPDATA "Claude\claude_desktop_config.json"

Write-Host "Configuring Gradle MCP Server for Claude Desktop..." -ForegroundColor Cyan
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

# Convert Windows path to JSON-safe format
$jarPathJson = $jarPath -replace '\\', '\\\\'

# Check if config file exists
if (Test-Path $claudeConfigPath) {
    Write-Host "✓ Found existing Claude configuration" -ForegroundColor Green
    
    # Read existing config
    $config = Get-Content $claudeConfigPath -Raw | ConvertFrom-Json
    
    # Add or update gradle-mcp-server
    if (-not $config.mcpServers) {
        $config | Add-Member -NotePropertyName "mcpServers" -NotePropertyValue @{} -Force
    }
    
    $config.mcpServers | Add-Member -NotePropertyName "gradle-mcp-server" -NotePropertyValue @{
        command = "java"
        args = @("-jar", $jarPath)
        env = @{}
        disabled = $false
        autoApprove = @()
    } -Force
    
    # Save updated config
    $config | ConvertTo-Json -Depth 10 | Set-Content $claudeConfigPath -Encoding UTF8
    Write-Host "✓ Updated existing configuration" -ForegroundColor Green
}
else {
    Write-Host "Creating new Claude configuration..." -ForegroundColor Gray
    
    # Create new config
    $config = @{
        mcpServers = @{
            "gradle-mcp-server" = @{
                command = "java"
                args = @("-jar", $jarPath)
                env = @{}
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
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Gradle MCP Server configured successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "Configuration file: $claudeConfigPath" -ForegroundColor Gray
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Restart Claude Desktop app" -ForegroundColor Gray
Write-Host "  2. The gradle-mcp-server tools will be available" -ForegroundColor Gray
Write-Host "  3. Test by asking Claude about your Gradle project" -ForegroundColor Gray
Write-Host "============================================" -ForegroundColor Cyan
```

---

## Available Tools After Configuration

Once configured, you'll have access to these tools:

### 1. `get_gradle_project_info`
Query project structure, tasks, dependencies, and build configuration.

**Example usage:**
```
"What tasks are available in the k2-look project?"
"Show me the project structure and dependencies"
"What's the Gradle version being used?"
```

### 2. `execute_gradle_task`
Run Gradle tasks programmatically.

**Example usage:**
```
"Build the debug APK"
"Clean and rebuild the project"
"Run the tests"
```

### 3. `run_gradle_tests`
Execute tests and get detailed hierarchical results.

**Example usage:**
```
"Run all tests and show results"
"Execute KarooDataServiceTest"
```

---

## Verification

After configuration, verify it's working:

1. **Restart Claude Desktop** (if using that option)

2. **Test the connection:**
   ```
   Ask Claude: "Can you inspect the Gradle project at C:\Project\k2-look?"
   ```

3. **Expected response:** Claude should be able to query project info using the MCP server

---

## Troubleshooting

### Issue: "Server not found"
- **Solution**: Verify JAR path exists: `%USERPROFILE%\mcp-servers\gradle-mcp-server\gradle-mcp-server-all.jar`
- Check Java is installed: `java -version`

### Issue: "Connection failed"
- **Solution**: Restart the MCP client (Claude Desktop, VSCode, etc.)
- Check the configuration file syntax is valid JSON

### Issue: "Java not found"
- **Solution**: Install JDK 17 or higher
- Add Java to PATH environment variable

---

## Benefits for k2-look Project

Once configured, AI assistance can:

✅ **Automatically build the project**
```
"Build the k2-look app"
```

✅ **Analyze dependencies**
```
"What dependencies does the app module use?"
"Show me all implementation dependencies"
```

✅ **Run specific tasks**
```
"Run assembleDebug"
"Execute tests for KarooDataService"
```

✅ **Inspect build configuration**
```
"What's the compile SDK version?"
"Show me all build variants"
```

✅ **Troubleshoot build issues**
```
"Why is the build failing?"
"What tasks are available?"
```

---

## Next Steps

1. Run the configuration script (see below)
2. Restart your MCP client
3. Test by asking about your Gradle project
4. Enjoy enhanced AI-powered Gradle support!

---

*Last Updated: 2025-11-21*

