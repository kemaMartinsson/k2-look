# Private Repository with Public Releases - Setup Guide

## ✅ Yes, This Works!

GitHub allows **private repositories** to have **public releases**. This means:

- ✅ Your source code stays private
- ✅ Compiled APKs are publicly downloadable
- ✅ Users can't see your code, only download releases
- ✅ No need to make the whole repo public

## How It's Configured

### 1. GitHub Actions Permissions

**File:** `.github/workflows/release.yml`

Added explicit permissions for private repos:

```yaml
permissions:
  contents: write  # Required to create releases
  packages: write  # Optional, for future
```

### 2. Repository Settings

Your repo can remain **Private**:

- Go to: Settings → General → Danger Zone
- Repository visibility: **Private** ✅
- Releases are automatically public!

### 3. What Users See

**With Private Repo:**

- ❌ Can't view source code
- ❌ Can't see commits/branches
- ❌ Can't clone the repository
- ✅ **CAN download releases/APKs**
- ✅ **CAN see release notes**
- ✅ **CAN report issues** (if you enable)

## URLs for Users

Even with a private repo, these URLs work publicly:

### Public Release Page:

```
https://github.com/YOUR_USERNAME/k2-look/releases
```

### Direct APK Download:

```
https://github.com/YOUR_USERNAME/k2-look/releases/download/0.1/K2Look-0.1.apk
```

Users can share these links and download APKs without GitHub accounts!

## Optional: Enable Public Issues

If you want users to report bugs without accessing code:

1. Go to: Settings → General → Features
2. Enable **Issues** checkbox
3. Issues will be public, but code stays private

## Testing Private + Public Releases

1. Make your repo private:
   ```
   Settings → Danger Zone → Change repository visibility → Private
   ```

2. Push a tag to trigger release:
   ```bash
   git tag -a "0.1" -m "First release"
   git push origin 0.1
   ```

3. Check release page in **incognito/private window**:
   ```
   https://github.com/YOUR_USERNAME/k2-look/releases
   ```

You should see:

- ✅ Release page loads
- ✅ APK is downloadable
- ❌ Source code tab gives 404
- ❌ Can't browse files

## Why This Is Great

### For You:

- Keep development private
- Control who sees the code
- Professional release management
- Free GitHub Actions (2000 min/month for private repos)

### For Users:

- Easy APK downloads
- Clear installation instructions
- No need for GitHub account
- Professional release experience

## Important Notes

1. **GitHub Actions Minutes:**
    - Private repos get 2000 free minutes/month
    - Each build takes ~2-3 minutes
    - ~600+ builds per month for free!

2. **Release Assets Are Public:**
    - Once you create a release, the APK is public
    - Anyone with the link can download it
    - This is intentional and desired!

3. **Collaborators:**
    - Add collaborators in: Settings → Collaborators
    - They get full code access
    - Issues/releases remain public

## Recommendation

**Keep your repo private for now:**

- Source code stays protected
- Releases are still public
- You can always make it public later
- No downside to privacy!

## Summary

✅ **Private repo + Public releases = Perfect for K2Look!**

Users get APKs, you keep code private. Best of both worlds! 🎉

