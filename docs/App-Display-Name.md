# App Display Name vs Package Name

## Quick Answer

**Yes!** The app display name and package name are completely independent.

---

## Configuration

### Display Name (What Users See)

**Location:** `app/src/main/res/values/strings.xml`

```xml

<resources>
    <string name="app_name">K2Look</string>
    <string name="extension_name">K2Look Extension</string>
</resources>
```

This controls:

- ✅ App name shown in launcher
- ✅ App name in app list
- ✅ Name shown in notifications
- ✅ Extension name in Karoo Extensions menu

**Current Value:** `K2Look`

---

### Package Name (Internal Identifier)

**Location:** `app/build.gradle.kts`

```kotlin
android {
    namespace = "com.kema.k2look"

    defaultConfig {
        applicationId = "com.kema.k2look"
    }
}
```

This controls:

- ✅ Unique app identifier on device
- ✅ Google Play Store identifier
- ✅ Internal Android system references
- ✅ Package structure for code

**Current Value:** `com.kema.k2look`

---

## How to Change Display Name

1. Edit `app/src/main/res/values/strings.xml`
2. Change `<string name="app_name">YOUR NAME HERE</string>`
3. Rebuild: `.\gradlew installDebug`
4. Done! ✅

**No need to:**

- ❌ Change package name
- ❌ Move files
- ❌ Update imports
- ❌ Modify code

---

## Examples

You can have any display name with any package name:

| Display Name   | Package Name    | Result                         |
|----------------|-----------------|--------------------------------|
| K2Look         | com.kema.k2look | ✅ Current setup                |
| Karoo Glasses  | com.kema.k2look | ✅ Different name, same package |
| ActiveLook Hub | com.kema.k2look | ✅ Different name, same package |
| K2             | com.kema.k2look | ✅ Short name, same package     |

---

## Best Practices

### Display Name

- Keep it short (8-12 characters ideal)
- Use readable characters
- Avoid special characters
- Consider launcher icon width
- Make it memorable

### Package Name

- Use reverse domain notation
- Keep it lowercase
- No spaces or special characters
- Should not change after release
- Uniquely identifies your app

---

## Current Configuration

✅ **Display Name:** K2Look  
✅ **Package Name:** com.kema.k2look  
✅ **Extension Name:** K2Look Extension

Perfect for your Karoo2 ↔ ActiveLook gateway app!

