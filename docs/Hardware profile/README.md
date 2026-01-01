# Karoo2 Hardware Profile for Android Studio

This directory contains an accurate hardware profile for the Hammerhead Karoo2 GPS cycling computer,
created from real device specifications.

---

## Profile Details

**File:** `Karoo2.xml`  
**Device:** Hammerhead Karoo2 (Model: k2)  
**Source:** Actual device KAROO20ALA091101299

### Accurate Specifications

| Property            | Value                                         | Source                 |
|---------------------|-----------------------------------------------|------------------------|
| **Manufacturer**    | Hammerhead                                    | Real device            |
| **Model**           | k2                                            | `ro.product.model`     |
| **Android Version** | 8.1.0 (API 27)                                | `ro.build.version.sdk` |
| **CPU**             | Qualcomm MSM8909                              | `ro.hardware`          |
| **GPU**             | Adreno 304                                    | Chipset spec           |
| **Architecture**    | armeabi-v7a                                   | `ro.product.cpu.abi`   |
| **RAM**             | 2 GB                                          | Device spec            |
| **Storage**         | 16 GB                                         | Device spec            |
| **Screen**          | 3.2" 800x480 WVGA                             | Device spec            |
| **Screen Density**  | xhdpi (291 dpi)                               | Measured               |
| **Networking**      | Bluetooth, WiFi, NFC                          | Device capabilities    |
| **Sensors**         | Accelerometer, Barometer, Compass, GPS, Light | Device sensors         |

---

## How to Import into Android Studio

### Method 1: Using AVD Manager (Recommended)

1. **Open Android Studio**
2. **Go to AVD Manager:**
    - Click `Tools` → `Device Manager`
    - Or click the phone icon in the toolbar

3. **Import Hardware Profile:**
    - Click `Device Definition` tab
    - Click `Import Hardware Profiles` button
    - Navigate to this folder
    - Select `Karoo2.xml`
    - Click `OK`

4. **Create Virtual Device:**
    - Click `Create Virtual Device`
    - Select `Hammerhead Karoo2` from the list
    - Choose system image (API 27 recommended)
    - Configure AVD settings
    - Click `Finish`

### Method 2: Manual Installation

1. **Copy Profile File:**
   ```powershell
   # Windows
   Copy-Item "Karoo2.xml" "$env:USERPROFILE\.android\devices\"
   
   # Create directory if it doesn't exist
   New-Item -ItemType Directory -Force -Path "$env:USERPROFILE\.android\devices\"
   ```

2. **Restart Android Studio**

3. **Create AVD** using the imported profile

---

## System Images for API 27

To create an emulator that matches the Karoo2:

1. **Download System Image:**
    - In AVD Manager, click `Create Virtual Device`
    - Select the imported `Hammerhead Karoo2` profile
    - Click `Next`
    - In the System Image screen, select the `x86 Images` tab
    - Download `Android 8.1 (Oreo) API Level 27`
    - **Note:** Use x86 for faster emulation on PC, but be aware the Karoo2 is ARM-based

2. **ARM vs x86:**
    - **Karoo2 Real Device:** ARM (armeabi-v7a)
    - **Emulator x86:** Faster but different architecture
    - **Emulator ARM:** Slower but matches real device
    - **For K2Look testing:** x86 is fine for most UI and logic testing
    - **For final testing:** Always test on real Karoo2 hardware

---

## Using the Karoo2 Emulator

### Recommended AVD Settings

```
Name: Hammerhead Karoo2 API 27
Device: Hammerhead Karoo2 (imported)
System Image: Android 8.1 (API 27)
Orientation: Portrait
RAM: 2048 MB
Internal Storage: 16 GB
SD Card: None (not applicable)
```

### Advanced Settings

- **Graphics:** Hardware (GLES 3.0)
- **Boot Option:** Quick Boot
- **Device Frame:** Enable (to see hardware buttons)
- **Keyboard:** Use physical keyboard
- **Network:** WiFi and Mobile data

### What Works vs Real Device

| Feature            | Emulator         | Real Karoo2    |
|--------------------|------------------|----------------|
| UI Testing         | ✅ Perfect        | ✅ Perfect      |
| Screen Size        | ✅ Matches        | ✅ Native       |
| API Level          | ✅ Matches        | ✅ Native       |
| Architecture       | ⚠️ x86 (if used) | ✅ ARM          |
| Bluetooth LE       | ❌ Limited/None   | ✅ Full Support |
| GPS Sensors        | ⚠️ Mock Only     | ✅ Real Data    |
| KarooSystemService | ❌ Not Available  | ✅ Available    |
| Battery Behavior   | ❌ Simulated      | ✅ Real         |
| Performance        | ⚠️ May be faster | ✅ Accurate     |

**Recommendation:** Use emulator for UI development, always test on real Karoo2 for final
validation.

---

## Testing K2Look on Emulator vs Real Device

### Emulator Testing (Good For:)

- ✅ UI layout and design
- ✅ Basic navigation flow
- ✅ Button interactions
- ✅ Mock data display
- ✅ Crash testing
- ✅ Quick iteration

### Real Device Testing (Required For:)

- ✅ Bluetooth LE (ActiveLook glasses)
- ✅ KarooSystemService integration
- ✅ Real ride data streaming
- ✅ Performance validation
- ✅ Battery impact measurement
- ✅ Sensor integration
- ✅ Final release validation

---

## Profile Verification

To verify the profile matches your real Karoo2:

```powershell
# Check device properties
adb shell getprop | findstr "ro.product\|ro.build.version\|ro.hardware"

# Expected output should match:
# ro.product.model: k2
# ro.product.manufacturer: Hammerhead
# ro.build.version.sdk: 27
# ro.hardware: qcom
```

---

## Profile Accuracy

✅ **Verified Against Real Device:** KAROO20ALA091101299

All specifications in this profile have been confirmed against an actual Hammerhead Karoo2 device
via ADB commands and official specifications.

**Last Verified:** November 22, 2025

---

## Troubleshooting

### Profile Not Showing in AVD Manager

1. Check file is in correct location: `~/.android/devices/`
2. Ensure XML is valid (no syntax errors)
3. Restart Android Studio
4. Try reimporting via `Import Hardware Profiles`

### Emulator Won't Start

1. Ensure system image for API 27 is downloaded
2. Check available RAM on host machine
3. Try reducing AVD RAM to 1024 MB
4. Enable hardware acceleration (Intel HAXM or AMD)

### Performance Issues

1. Use x86 system image (faster than ARM)
2. Enable "Use Host GPU"
3. Allocate more RAM to emulator
4. Close other running emulators
5. Consider testing on real Karoo2 instead

---

## Related Documentation

- [Karoo2-Hardware-Profile.md](../Karoo2-Hardware-Profile.md) - Complete technical specifications
- [Karoo2-Quick-Reference.md](../Karoo2-Quick-Reference.md) - Command reference card

---

## Updates

If Hammerhead releases firmware updates that change hardware capabilities or Android version, this
profile should be updated. Check the real device periodically:

```powershell
# Get current device info
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
```

---

**Profile Version:** 1.0  
**Created:** November 22, 2025  
**Based on Device:** KAROO20ALA091101299  
**Status:** ✅ Production Ready

