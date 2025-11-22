# Reference Project Build Fixes - FAQ

## Q: Are changes to `reference/android-sdk/ActiveLookSDK/build.gradle` important?

**A: YES - Absolutely critical!** Without these fixes, your app **will not build**.

---

## Q: Should I commit these changes to my repo?

**A: NO - Do NOT commit them!** Here's why:

### Why NOT to Commit

1. **Already in .gitignore** - The `reference/` folder is excluded from your repo
2. **External repositories** - These are separate git repos you cloned for reference
3. **Can't push upstream** - You don't have write access to ActiveLook's or Hammerhead's repos
4. **Will be overwritten** - Running `update-references.ps1` pulls fresh code from GitHub

### What Happens

```
Your Repo (k2-look)
├── app/                    ← Your code (commit this)
├── docs/                   ← Your docs (commit this)
├── reference/              ← External repos (DON'T commit)
│   ├── android-sdk/        ← Cloned from GitHub
│   └── karoo-ext/          ← Cloned from GitHub
└── .gitignore              ← Excludes reference/
```

---

## Q: How do I keep these fixes when updating?

**A: Use the automated fix script!** ✅

### The Solution

We've created `post-update-fix.ps1` that automatically applies all necessary fixes after updating reference projects.

**How it works:**
```powershell
# 1. Update references (pulls latest from GitHub)
.\update-references.ps1

# 2. Automatically applies fixes
#    - Replaces $kotlin_version with "1.6.21"
#    - Fixes deprecated Groovy syntax
#    - Comments out maven publication
#    - Replaces version catalog aliases
#    - Updates karoo-ext dependencies

# 3. Build your app
.\gradlew :app:assembleDebug
```

The `update-references.ps1` script now **automatically calls** `post-update-fix.ps1` after updating!

---

## Q: What if the automated fixes fail?

**A: Manual fixes are documented** in `docs/Reference-Fix-Notes.md`

### Manual Fix Process

1. **Check the error message** from Gradle
2. **Open Reference-Fix-Notes.md** for step-by-step instructions
3. **Apply fixes manually** to the files listed
4. **Test build:** `.\gradlew :app:assembleDebug`

---

## Q: Can I modify the reference projects?

**A: Yes, but only for local testing!**

### Safe Modifications
- ✅ Change versions for testing
- ✅ Add debug logging
- ✅ Experiment with features

### But Remember
- ⚠️ Changes will be lost when you run `update-references.ps1`
- ⚠️ Changes won't be pushed to upstream repos
- ⚠️ Use stash or branches if you need to save changes

---

## Q: What files get modified by the fix script?

### ActiveLook SDK (`reference/android-sdk/ActiveLookSDK/build.gradle`)
```groovy
// Before (won't build)
version "$kotlin_version"
namespace 'com.activelook.activelooksdk'
implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version"

// After (builds successfully)
version "1.6.21"
namespace = 'com.activelook.activelooksdk'
implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.6.21"
```

### Karoo Extensions (`reference/karoo-ext/lib/build.gradle.kts`)
```kotlin
// Before (won't build)
alias(libs.plugins.android.library)
implementation(libs.androidx.core.ktx)

// After (builds successfully)
id("com.android.library")
implementation("androidx.core:core-ktx:1.13.1")
```

---

## Q: Why do these fixes need to exist?

**A: Different build contexts**

### Upstream Projects (GitHub)
- Use **version catalogs** (`libs.versions.toml`)
- Use **Gradle variables** defined in parent projects
- Have **publishing configurations** for Maven/GitHub Packages

### Your Project (Local Modules)
- **No version catalogs** (they're project-specific)
- **No parent build context** (different gradle structure)
- **No publishing** (just consuming the libraries)

---

## Q: Will this be a problem forever?

**A: No - Only until upstream projects update their build scripts**

### Potential Solutions

1. **Wait for upstream** to use hardcoded versions (unlikely)
2. **Contribute PRs** to make builds more flexible (requires approval)
3. **Continue using automated fixes** (works perfectly now)

The automated fix script solves this permanently for your workflow! ✅

---

## Quick Reference Card

| Action | Command | Auto-Fixes? |
|--------|---------|-------------|
| **Initial Setup** | `.\setup-references.ps1` | ✅ Yes |
| **Update References** | `.\update-references.ps1` | ✅ Yes (Windows) |
| **Manual Fix** | `.\post-update-fix.ps1` | ✅ Always |
| **Verify Build** | `.\gradlew :app:assembleDebug` | - |

---

## Conclusion

✅ **Changes are CRITICAL for building**  
❌ **Do NOT commit to your repo**  
✅ **Use automated fix script**  
✅ **Reference/ is in .gitignore**  
✅ **Rebuild after updating**

**The automated solution works perfectly - no manual intervention needed!** 🎉

---

*Last Updated: 2024-11-22*  
*Scripts: update-references.ps1, post-update-fix.ps1*

