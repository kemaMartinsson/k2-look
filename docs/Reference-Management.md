# Reference Management Scripts

This document explains the reference management scripts for k2-look project.

## Overview

The k2-look project uses local clones of upstream repositories as dependencies and reference
implementations. These scripts help manage those reference projects.

---

## Scripts Available

### 1. `setup-references.ps1` / `setup-references.sh`

**Purpose:** Initial setup - clones all reference repositories

**When to use:**

- First time setting up the project
- After cloning the repository without reference projects
- If reference projects were accidentally deleted

**What it does:**

1. Creates `reference/` directory if missing
2. Clones ActiveLook SDK from GitHub
3. Clones Karoo Extensions from GitHub
4. Clones Ki2 reference project from GitHub
5. Skips any that already exist

**Usage:**

Windows (PowerShell):

```powershell
.\setup-references.ps1
```

Linux/Mac (Bash):

```bash
chmod +x setup-references.sh
./setup-references.sh
```

**Output:**

```
Setting up reference projects...

Reference directory: C:\Project\k2-look\reference

Setting up ActiveLook SDK...
  Cloning from: https://github.com/ActiveLook/android-sdk.git
  ✓ ActiveLook SDK cloned successfully

Setting up Karoo Extensions...
  Cloning from: https://github.com/hammerheadnav/karoo-ext.git
  ✓ Karoo Extensions cloned successfully

Setting up Ki2 Reference Project...
  Cloning from: https://github.com/valterc/ki2.git
  ✓ Ki2 Reference Project cloned successfully

============================================
All reference projects set up successfully!

Next steps:
  1. Build the project: .\gradlew :app:assembleDebug
  2. To update references later: .\update-references.ps1
============================================
```

---

### 2. `update-references.ps1` / `update-references.sh`

**Purpose:** Update existing reference repositories to latest versions

**When to use:**

- Weekly/monthly to get latest bug fixes
- When upstream adds new features you need
- After build errors that might be fixed upstream
- Before starting a new development phase

**What it does:**

1. Fetches latest changes from remote
2. Checks for local modifications
3. Stashes local changes if any
4. Pulls latest updates
5. Shows the latest commit for each repo
6. Reports any errors

**Usage:**

Windows (PowerShell):

```powershell
.\update-references.ps1
```

Linux/Mac (Bash):

```bash
chmod +x update-references.sh
./update-references.sh
```

**Output:**

```
Updating reference projects...

Reference directory: C:\Project\k2-look\reference

Updating ActiveLook SDK...
  Fetching latest changes...
  Current branch: main
  Pulling latest changes...
  Latest commit: abc1234 - Fix BLE connection timeout (2 days ago by ActiveLook)
  ✓ ActiveLook SDK updated successfully

Updating Karoo Extensions...
  Fetching latest changes...
  Current branch: main
  Pulling latest changes...
  Latest commit: def5678 - Add new data types (1 week ago by Hammerhead)
  ✓ Karoo Extensions updated successfully

Updating Ki2 Reference Project...
  Fetching latest changes...
  Current branch: main
  Pulling latest changes...
  Latest commit: ghi9012 - Update UI layout (3 days ago by valterc)
  ✓ Ki2 Reference Project updated successfully

============================================
All reference projects updated successfully!

Next steps:
  1. Review changes: git log in each reference directory
  2. Rebuild project: .\gradlew clean :app:assembleDebug
  3. Test the app to ensure compatibility
============================================
```

---

## Best Practices

### When to Update

**✅ GOOD times to update:**

- At the start of a new sprint
- When you need a specific new feature
- During scheduled maintenance windows
- After backing up your work

**❌ AVOID updating:**

- In the middle of implementing a feature
- Right before a demo or release
- When you have uncommitted local changes
- If the build is currently broken

### After Updating

Always follow these steps after running `update-references`:

1. **Review Changes**
   ```bash
   cd reference/android-sdk
   git log --oneline --since="1 month ago"
   ```

2. **Check for Breaking Changes**
    - Read CHANGELOG.md in each repo
    - Look for "breaking changes" in commit messages
    - Check if API signatures changed

3. **Rebuild Project**
   ```bash
   .\gradlew clean :app:assembleDebug
   ```

4. **Test Thoroughly**
    - Test basic connection to Karoo
    - Test data streaming
    - Test ActiveLook connection (when implemented)

5. **Fix Any Issues**
    - Update our code if APIs changed
    - Adjust build configurations if needed
    - Update documentation

### Handling Local Modifications

If you've made changes to reference projects for testing:

**Option 1: Keep your changes**

```bash
cd reference/android-sdk
git stash                    # Script does this automatically
.\update-references.ps1      # Updates to latest
git stash pop               # Reapply your changes
```

**Option 2: Discard your changes**

```bash
cd reference/android-sdk
git reset --hard HEAD       # Discard all changes
.\update-references.ps1     # Updates to latest
```

---

## Troubleshooting

### Error: "Repository not found"

**Problem:** Reference directory doesn't exist
**Solution:** Run `setup-references.ps1` first

### Error: "Local changes detected"

**Problem:** You have uncommitted changes in reference repos
**Solution:** Script automatically stashes them, but review with:

```bash
cd reference/android-sdk
git stash list
git stash show stash@{0}
```

### Error: "Failed to pull"

**Problem:** Network issues or merge conflicts
**Solution:**

```bash
cd reference/[problem-repo]
git status                  # Check what's wrong
git pull origin main       # Try manual pull
git reset --hard origin/main  # If necessary, discard local changes
```

### Build Fails After Update

**Problem:** Upstream changes broke compatibility
**Solution:**

1. Check git log for breaking changes
2. Revert to previous commit:
   ```bash
   cd reference/[problem-repo]
   git log --oneline
   git checkout <previous-commit-hash>
   ```
3. Report issue to upstream project
4. Wait for fix or adjust our code

---

## Reference Projects Details

### ActiveLook SDK

- **Repository:** https://github.com/ActiveLook/android-sdk
- **Current Version:** 4.5.6
- **Branch:** main
- **Purpose:** BLE communication with ActiveLook glasses
- **Update Frequency:** Monthly recommended

### Karoo Extensions

- **Repository:** https://github.com/hammerheadnav/karoo-ext
- **Current Version:** 1.1.7
- **Branch:** main
- **Purpose:** Integration with Karoo System
- **Update Frequency:** Bi-weekly recommended

### Ki2 Reference

- **Repository:** https://github.com/valterc/ki2
- **Current Version:** Latest
- **Branch:** main
- **Purpose:** Reference implementation for Karoo extensions
- **Update Frequency:** As needed for reference

---

## Advanced Usage

### Update Only One Repository

Instead of using the script:

```bash
cd reference/android-sdk
git fetch origin
git pull origin main
```

### Check for Updates Without Pulling

```bash
cd reference/android-sdk
git fetch origin
git log HEAD..origin/main --oneline
```

### View Changes Before Updating

```bash
cd reference/android-sdk
git fetch origin
git diff HEAD..origin/main
```

### Create a Backup Before Updating

```bash
# Backup reference directory
cp -r reference reference-backup-$(date +%Y%m%d)

# Run update
.\update-references.ps1

# If problems occur, restore:
rm -rf reference
mv reference-backup-20241121 reference
```

---

## Integration with Project

The k2-look project uses these reference repositories as **local project modules**, not remote
artifacts:

**settings.gradle.kts:**

```kotlin
include(":activelook-sdk")
project(":activelook-sdk").projectDir = file("reference/android-sdk/ActiveLookSDK")

include(":karoo-ext")
project(":karoo-ext").projectDir = file("reference/karoo-ext/lib")
```

**app/build.gradle.kts:**

```kotlin
dependencies {
    implementation(project(":karoo-ext"))
    implementation(project(":activelook-sdk"))
    // ... other dependencies
}
```

This approach provides:

- ✅ No need for remote artifact repositories
- ✅ Full source code access for debugging
- ✅ Faster builds (no network downloads)
- ✅ Ability to modify locally for testing
- ✅ Works offline

---

## Maintenance Schedule

Recommended update schedule:

| Task              | Frequency         | Command                              |
|-------------------|-------------------|--------------------------------------|
| Check for updates | Weekly            | `git fetch` in each reference dir    |
| Update references | Bi-weekly         | `.\update-references.ps1`            |
| Test after update | After each update | `.\gradlew clean :app:assembleDebug` |
| Review changelogs | Monthly           | Check GitHub releases                |

---

## Support

If you encounter issues:

1. Check this documentation
2. Review error messages carefully
3. Check upstream repository issues
4. Ask in project discussions
5. Create an issue with details

---

## Version History

- **v1.0** (2024-11-21) - Initial scripts created
    - setup-references.ps1/sh
    - update-references.ps1/sh
    - Documentation added

