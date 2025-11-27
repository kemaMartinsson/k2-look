# In-App Update Feature Configuration

The K2Look app now includes an in-app update checker that allows users to download and install new
versions directly from within the app.

## Features

✅ **Manual Check** - "Check for Updates" button in the About tab  
✅ **Auto-Check** - Optional automatic check on app start (once per day)  
✅ **Notifications** - Shows a notification when a new version is available  
✅ **Download & Install** - Downloads APK and prompts for installation  
✅ **Release Notes** - Displays release notes from GitHub releases

## Configuration

### 1. Update GitHub Repository Details

Edit `app/src/main/kotlin/com/kema/k2look/update/UpdateChecker.kt` and replace:

```kotlin
private const val GITHUB_OWNER = "YourGitHubUsername"  // Your GitHub username
private const val GITHUB_REPO = "k2-look"              // Your repository name
```

### 2. How It Works

1. **Checking**: Queries GitHub API for the latest release
2. **Comparing**: Compares version code with current app version
3. **Downloading**: Uses Android DownloadManager to download APK
4. **Installing**: Opens system installer with the downloaded APK

### 3. Release Requirements

For updates to work, your GitHub releases must:

- ✅ Have an APK file attached as a release asset
- ✅ Use semantic versioning (e.g., `v1.0`, `1.2.3`)
- ✅ Include release notes in the release body
- ✅ Be published (not draft)

### 4. Version Code Calculation

The update checker extracts version codes from version strings:

- `1.0` → 10000
- `1.2` → 10200
- `1.2.3` → 10203

This matches your current git-based version code scheme.

## User Experience

### About Tab

- Toggle "Auto-check for updates"
- Button to manually check for updates
- Shows current version

### Update Dialog

- Version number
- Release notes
- Download/Later buttons
- Download progress

### Notifications

- Only shown once per version
- Can be dismissed (won't show again for that version)
- Clicking opens the app

## Permissions Required

The following permissions are automatically added:

- `INTERNET` - To check GitHub API
- `REQUEST_INSTALL_PACKAGES` - To install APK (Android 8.0+)
- `POST_NOTIFICATIONS` - To show update notifications (Android 13+)

## Testing

### Test Update Check Locally

1. Lower your app's version code in `build.gradle.kts`
2. Create a GitHub release with higher version
3. Attach an APK to the release
4. Open the app and click "Check for Updates"

### Test Auto-Check

1. Enable "Auto-check for updates" in About tab
2. Close and reopen the app after 24 hours
3. Or clear app data to reset the check timer

## Privacy

- Update checks only occur when requested or when auto-check is enabled
- No personal data is transmitted
- Only queries public GitHub API
- No analytics or tracking

## Troubleshooting

### "No APK found in release"

- Ensure the release has a `.apk` file attached
- Check that the file extension is exactly `.apk`

### "Cannot install APK"

- User must allow "Install from Unknown Sources" for your app
- On Android 8.0+, a permission dialog will appear automatically

### "Check failed"

- Verify GitHub repository details are correct
- Check internet connection
- Ensure GitHub release is public

## Security Notes

- APKs are downloaded over HTTPS
- FileProvider is used for secure file sharing (Android 7.0+)
- User explicitly confirms installation
- Download happens in system Downloads folder (visible to user)

