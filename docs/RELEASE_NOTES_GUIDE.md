# Release Notes Guide

## How Release Notes Work

Release notes are displayed on your GitHub release page when users download your APK. Here's how the
system works:

## 📝 Release Notes Come From Git Tag Messages

When you create a git tag, the **tag message** becomes your release notes!

### Example: Creating a Release with Notes

```bash
git tag -a "0.1" -m "First public release

What's New:
- Initial release of K2Look
- ActiveLook glasses support
- Real-time ride metrics on glasses display
- Auto-reconnect during active rides (10min timeout)
- Debug mode with display simulator
- Status tab with connection management

Known Issues:
- None yet

Installation:
Download the APK below and install on your Karoo 2 device."

git push origin 0.1
```

### What Users Will See:

```
K2Look 0.1

First public release

What's New:
- Initial release of K2Look
- ActiveLook glasses support
- Real-time ride metrics on glasses display
- Auto-reconnect during active rides (10min timeout)
- Debug mode with display simulator
- Status tab with connection management

Known Issues:
- None yet

Installation:
Download the APK below and install on your Karoo 2 device.

---

### Installation
1. Download the APK file below
2. Transfer to your Karoo 2 device
3. Install the APK
4. Launch K2Look from your apps

### Links
- Installation Guide
- Report Issues

Built: 2025-01-23
Commit: abc123...

📎 Assets
   K2Look-0.1.apk
```

## 📋 Template for Release Notes

Use this template when creating tags:

```bash
git tag -a "X.X" -m "Brief title

What's New:
- Feature 1
- Feature 2
- Bug fix 1

Changes:
- Improved XYZ
- Updated ABC

Known Issues:
- Issue 1 (if any)

Breaking Changes:
- None (or list them)"
```

## 🎯 Two Parts of Release Notes

### Part 1: Your Custom Notes (from git tag message)

- Comes from: `git tag -a "0.1" -m "Your message here"`
- Shows: What's new, changes, features
- You write this!

### Part 2: Automatic Template (from workflow)

- Comes from: `.github/workflows/release.yml`
- Shows: Installation instructions, links, build info
- Same for every release
- You can edit the workflow file to change this

## 🔄 How the Workflow Uses Tag Messages

In `.github/workflows/release.yml`:

```yaml
- name: Get tag message as release notes
  id: get_tag_message
  run: |
    TAG_MESSAGE=$(git tag -l --format='%(contents)' ${GITHUB_REF#refs/tags/})
    echo "TAG_MESSAGE<<EOF" >> $GITHUB_OUTPUT
    echo "$TAG_MESSAGE" >> $GITHUB_OUTPUT
    echo "EOF" >> $GITHUB_OUTPUT

- name: Create GitHub Release
  with:
    body: |
      ## K2Look ${{ steps.get_version.outputs.VERSION }}

      ${{ steps.get_tag_message.outputs.TAG_MESSAGE }}

      --- (rest of automatic template)
```

## 📝 Best Practices

### DO:

✅ Write clear, user-focused release notes
✅ List new features and bug fixes
✅ Mention breaking changes
✅ Keep it concise but informative
✅ Use bullet points for readability

### DON'T:

❌ Write technical commit messages
❌ Include internal code changes users don't care about
❌ Forget to mention important changes
❌ Make it too long (users won't read it)

## 🎨 Markdown Formatting

You can use Markdown in your tag messages:

```bash
git tag -a "0.2" -m "Second release

## What's New
- **Major feature**: Something awesome
- Bug fix for connection issues

## Changes
- Improved UI responsiveness
- Updated dependencies

## Known Issues
None! 🎉"
```

## 📚 Example Release Notes

### Version 0.1 (First Release)

```
Initial release of K2Look

Features:
- Connect ActiveLook smart glasses to Karoo 2
- Real-time ride metrics display
- Auto-reconnect during active rides
- Debug mode with simulator
- Status and About tabs
```

### Version 0.2 (Feature Update)

```
Second release with improvements

New Features:
- Customizable reconnect timeout
- Improved connection stability
- Better error messages

Bug Fixes:
- Fixed glasses reconnection issue
- Resolved status display bug

Changes:
- Reduced UI margins
- Updated About tab with version info
```

### Version 1.0 (Major Release)

```
First stable release! 🎉

Major Changes:
- Complete UI redesign
- Improved ActiveLook integration
- Better performance

New Features:
- [List features]

Bug Fixes:
- [List fixes]

Breaking Changes:
- [List if any]

Upgrade Notes:
- [Any special instructions]
```

## 🔧 Changing the Automatic Template

If you want to customize the automatic part (installation instructions, etc.):

1. Edit `.github/workflows/release.yml`
2. Find the `Create GitHub Release` step
3. Modify the `body:` section
4. Commit and push changes
5. Next release will use the new template

## 📖 Summary

**Answer to your question:**

> **Where do release notes come from?**

1. **Your custom notes**: From the git tag message you write
2. **Automatic template**: From `.github/workflows/release.yml` (hardcoded)

**Combined result:** Your custom notes + automatic installation/build info

**How to write them:** Use detailed git tag messages with `-m`

**When users see them:** On the GitHub releases page when downloading APKs

---

**Next steps:**

1. Write good release notes in your git tag message
2. Push the tag
3. CI builds and creates the release with your notes!

Simple as that! 🚀

