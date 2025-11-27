# Quick Setup - In-App Updates

## 🎯 3-Step Setup

### Step 1: Configure Repository

Edit `app/src/main/kotlin/com/kema/k2look/update/UpdateChecker.kt`:

```kotlin
private const val GITHUB_OWNER = "your-username"  // Your GitHub username
private const val GITHUB_REPO = "k2-look"         // Your repo name
```

### Step 2: Create a GitHub Release

```bash
# Tag your release
git tag -a v1.0 -m "Release notes here"
git push origin v1.0
```

Then on GitHub:

1. Go to Releases → Create new release
2. Select the tag you just created
3. **Attach your APK file** (Important!)
4. Add release notes
5. Click "Publish release"

### Step 3: Test

1. Open app → About tab
2. Tap "Check for Updates"
3. Should show your release!

## 📱 User Features

**About Tab:**

- ✅ "Check for Updates" button
- ✅ Auto-check toggle (on by default)
- ✅ Shows current version

**Auto-Check:**

- Checks once per day on app start
- Shows notification when update available
- User can disable in settings

**Update Process:**

1. User gets notification OR manually checks
2. Sees update dialog with release notes
3. Taps "Download"
4. APK downloads
5. System installer opens
6. User installs update

## 🔧 Key Files

| File                  | Purpose                          |
|-----------------------|----------------------------------|
| `UpdateChecker.kt`    | ← **Configure GitHub repo here** |
| `UpdateDownloader.kt` | Downloads APK                    |
| `UpdateDialogs.kt`    | UI components                    |
| `AboutTab.kt`         | Update UI in About tab           |
| `MainActivity.kt`     | Auto-check on launch             |

## ✅ Checklist

- [ ] Set GitHub username in `UpdateChecker.kt`
- [ ] Set repo name in `UpdateChecker.kt`
- [ ] Create GitHub release with tag
- [ ] Attach APK to release
- [ ] Add release notes
- [ ] Publish release (not draft!)
- [ ] Test "Check for Updates" in app

## 💡 Tips

- Use version tags like `v1.0`, `v1.1`, etc.
- APK file must have `.apk` extension
- Release must be **published** (not draft)
- User needs to allow "Unknown Sources" to install
- Auto-check runs max once per 24 hours

## 🎉 Done!

Your app now has professional in-app update functionality!

