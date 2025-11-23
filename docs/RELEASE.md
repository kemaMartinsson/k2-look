# Release Process Guide

## Creating a New Release

### 1. Update CHANGELOG.md

Add your release notes to `CHANGELOG.md` at the top of the file:

```markdown
# Changelog

## [0.2] - 2025-01-24

### Added
- New feature 1
- New feature 2

### Changed
- Improved XYZ
- Updated ABC

### Fixed
- Bug fix 1
- Bug fix 2

---

## [0.1] - 2025-01-23
... (previous versions below)
```

### 2. Update strings.xml (Optional but Recommended)

Copy the relevant release notes to `app/src/main/res/values/strings.xml`:

```xml
<string name="release_notes">
    Version 0.2 - 2025-01-24\n\n
    Added:\n
    • New feature 1\n
    • New feature 2\n\n
    ...
</string>
```

This ensures the About tab in the app shows current release notes.

### 3. Create Git Tag

Create a tag (message can be brief now, CHANGELOG has the details):

```bash
git tag -a "0.2" -m "Release 0.2"
**📝 Note:** Release notes now come from `CHANGELOG.md`, not the git tag message!

### 4. Push Changes and Tag

Commit and push everything:

```bash
git add CHANGELOG.md app/src/main/res/values/strings.xml
git commit -m "Release 0.2"
git push

# Push the tag to trigger CI build
git push origin 0.2
```

### 5. Automatic Build Process

The GitHub Actions CI will automatically:

- Build the release APK
- Extract release notes from CHANGELOG.md
- Create a GitHub Release
- Attach the APK as `K2Look-0.2.apk`

### 6. Find Your Release

Go to: https://github.com/YOUR_USERNAME/k2-look/releases

## Manual Release Build (Local)

If you want to build locally:

```bash
# Build release APK
./gradlew assembleRelease

# Find the APK at:
# app/build/outputs/apk/release/app-release.apk
```

## Adding APK Signing (Optional)

### 1. Generate a Keystore

```bash
keytool -genkey -v -keystore keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias k2look
```

**Important:** Add `keystore.jks` to `.gitignore` - never commit it!

### 2. Add Secrets to GitHub

Go to: Repository Settings → Secrets and variables → Actions

Add these secrets:

- `KEYSTORE_FILE`: Base64 encoded keystore file
- `KEYSTORE_PASSWORD`: Your keystore password
- `KEY_ALIAS`: Your key alias (e.g., "k2look")
- `KEY_PASSWORD`: Your key password

To encode keystore:

```bash
base64 -w 0 keystore.jks > keystore.base64
```

### 3. Uncomment Signing in Workflow

Edit `.github/workflows/release.yml` and uncomment the environment variables in the "Build Release
APK" step.

## Version Naming Convention

Recommended format:

- `0.1` - Initial release
- `0.2` - Minor updates
- `1.0` - First stable release
- `1.1` - Feature updates
- `1.1.1` - Bug fixes

## Testing Before Release

Always test the debug build first:

```bash
./gradlew installDebug
```

Then create the release when you're confident.

