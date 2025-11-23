# Release Process Guide

## Creating a New Release

### 1. Update Version

Create a new git tag with your version number:

```bash
# Example: Creating version 0.2
git tag -a "0.2" -m "Release 0.2 - Description of changes"
```

### 2. Push the Tag

Push the tag to GitHub to trigger the CI build:

```bash
git push origin 0.2
```

### 3. Automatic Build Process

The GitHub Actions CI will automatically:

- Build the release APK
- Create a GitHub Release
- Attach the APK as `K2Look-0.2.apk`

### 4. Find Your Release

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

