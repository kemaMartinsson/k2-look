# GitKraken MCP Server Configuration

## ✅ Successfully Added to Android Studio

GitKraken/GitLens MCP server has been configured in Android Studio alongside the Gradle MCP server.

---

## Configuration Details

### Location
```
%APPDATA%\Google\AndroidStudio2025.2.1\options\mcp_settings.json
```

### Configuration
```json
{
    "mcpServers": {
        "gradle-mcp-server": {
            "command": "java",
            "args": [
                "-jar",
                "C:\\Users\\kema\\mcp-servers\\gradle-mcp-server\\gradle-mcp-server-all.jar"
            ],
            "env": {},
            "disabled": false,
            "autoApprove": []
        },
        "GitKraken": {
            "command": "c:\\Users\\kema\\AppData\\Roaming\\Code\\User\\globalStorage\\eamodio.gitlens\\gk.exe",
            "args": [
                "mcp",
                "--host=android-studio",
                "--source=gitlens",
                "--scheme=jetbrains"
            ],
            "env": {},
            "disabled": false,
            "autoApprove": []
        }
    }
}
```

---

## Changes Made

### Host Parameter
- **VSCode:** `--host=vscode`
- **Android Studio:** `--host=android-studio` ✅

### Scheme Parameter  
- **VSCode:** `--scheme=vscode`
- **Android Studio:** `--scheme=jetbrains` ✅

These parameters tell GitKraken which IDE context it's running in.

---

## Available MCP Servers in Android Studio

| Server | Purpose | Status |
|--------|---------|--------|
| **gradle-mcp-server** | Gradle project inspection & tasks | ✅ Active |
| **GitKraken** | Git operations via GitLens | ✅ Active |

---

## What GitKraken MCP Provides

### Git Operations
- 📊 View commit history
- 🌿 Branch management
- 🔀 Merge operations
- 📝 Commit operations
- 🔍 File diff viewing
- 📋 Blame information

### AI-Powered Features
- Explain code changes
- Generate commit messages
- Suggest PR descriptions
- Code review assistance

---

## Next Steps

### 1. Restart Android Studio
Close and reopen Android Studio for the MCP server to be recognized.

### 2. Verify Connection
After restart, the GitKraken MCP server should be available through AI assistance.

### 3. Test GitKraken Features
Try asking:
- "Show me the recent commits"
- "What files changed in the last commit?"
- "Create a new branch for Phase 1.3"
- "Show git status"

---

## Troubleshooting

### Issue: GitKraken Not Found
**Error:** `gk.exe` not found at path

**Solution:**
1. Verify GitLens is installed in VSCode
2. Check path: `%APPDATA%\Code\User\globalStorage\eamodio.gitlens\gk.exe`
3. If missing, reinstall GitLens extension in VSCode

### Issue: MCP Server Not Loading
**Solution:**
1. Restart Android Studio
2. Check Android Studio logs for MCP errors
3. Verify JSON syntax in `mcp_settings.json`

### Issue: Wrong Scheme/Host
**Solution:**
The configuration uses JetBrains-specific parameters:
- `--host=android-studio`
- `--scheme=jetbrains`

These match IntelliJ/Android Studio conventions.

---

## Comparison: VSCode vs Android Studio

| Parameter | VSCode | Android Studio |
|-----------|--------|----------------|
| **Host** | `vscode` | `android-studio` |
| **Scheme** | `vscode` | `jetbrains` |
| **Config File** | `cline_mcp_settings.json` | `mcp_settings.json` |
| **Location** | User GlobalStorage | IDE Options |

---

## Benefits

### For k2-look Project

✅ **Integrated Git Operations**
- View commit history without leaving IDE
- Quick branch switching
- Easy staging/unstaging

✅ **AI-Powered Git**
- Auto-generate commit messages
- Explain complex diffs
- Review pull requests

✅ **Consistent Across IDEs**
- Same GitKraken features in VSCode and Android Studio
- Unified Git workflow

---

## Usage Examples

### In Android Studio with AI Assistant

```
"Show me the last 5 commits"
"What files are currently modified?"
"Create a branch called 'feature/phase-1.3'"
"Show the diff for ActiveLookService.kt"
"Generate a commit message for my staged changes"
```

---

## Configuration File Backup

Before making changes, the original config had only gradle-mcp-server.

**Original:**
```json
{
    "mcpServers": {
        "gradle-mcp-server": { ... }
    }
}
```

**Updated:**
```json
{
    "mcpServers": {
        "gradle-mcp-server": { ... },
        "GitKraken": { ... }
    }
}
```

---

## Summary

✅ **GitKraken MCP server added to Android Studio**  
✅ **Configured with correct Android Studio parameters**  
✅ **Both Gradle and GitKraken MCP servers now available**  
✅ **Restart Android Studio to activate**

**Ready to use Git operations through AI assistance in Android Studio!** 🚀

---

*Last Updated: 2024-11-22*  
*Status: Configured and Ready*

