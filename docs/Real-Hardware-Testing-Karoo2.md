# Real Hardware Testing - Karoo2

**Date:** November 22, 2025  
**Device:** Hammerhead Karoo2 (KAROO20ALA091101299)  
**App Version:** K2Look v1.0 (com.kema.k2look)

---

## 🎉 SUCCESS: App Running on Real Karoo2!

### Test Summary

✅ **Installation Successful**

- App installed via ADB
- Package: `com.kema.k2look`
- Display name: "K2Look"

✅ **App Launch Successful**

- MainActivity launches correctly
- UI displays on Karoo2 screen
- No immediate crashes

---

## Device Information

**Karoo2 Details:**

- Device ID: KAROO20ALA091101299
- Model: Hammerhead k2
- Android Version: 8.1.0 (API 27)
- Architecture: ARM 32-bit (armeabi-v7a)
- Connection: USB ADB
- Status: Connected and authorized

**📋 Full Hardware Profile:** See [Karoo2-Hardware-Profile.md](./Karoo2-Hardware-Profile.md)

**Installation Command:**

```powershell
.\gradlew installDebug
```

**Launch Command:**

```powershell
adb shell am start -n com.kema.k2look/.MainActivity
```

---

## Next Testing Steps

### 1. ActiveLook Glasses Connection

- [ ] Enable Bluetooth on Karoo2
- [ ] Pair ActiveLook glasses
- [ ] Test BLE scanning in app
- [ ] Verify connection to glasses
- [ ] Test basic display output

### 2. Karoo Data Integration

- [ ] Start a ride on Karoo2
- [ ] Verify KarooSystemService connection
- [ ] Check RideState data streaming
- [ ] Confirm metrics are captured:
    - [ ] Speed
    - [ ] Heart rate
    - [ ] Cadence
    - [ ] Power
    - [ ] Distance
    - [ ] Time

### 3. End-to-End Testing

- [ ] Start ride on Karoo2
- [ ] Connect ActiveLook glasses
- [ ] Verify data flows from Karoo → App → Glasses
- [ ] Check display updates on glasses (~1Hz)
- [ ] Monitor connection stability
- [ ] Test during actual cycling

---

## Expected Behavior on Karoo2

### App UI Should Show:

1. **Connection Status Section:**
    - Karoo connection status (Connected/Disconnected)
    - ActiveLook connection status (Connected/Disconnected)

2. **Karoo Data Section:**
    - Current speed
    - Heart rate
    - Cadence
    - Power
    - Distance
    - Ride time

3. **ActiveLook Section:**
    - Scan for glasses button
    - List of discovered glasses
    - Connect/Disconnect controls

### What Works on Real Hardware (vs Emulator):

| Feature               | Emulator          | Karoo2             |
|-----------------------|-------------------|--------------------|
| App Installation      | ✅                 | ✅                  |
| App Launch            | ✅                 | ✅                  |
| UI Display            | ✅                 | ✅                  |
| KarooSystemService    | ❌ (not available) | ✅ (should work)    |
| BLE Scanning          | ❌ (no BLE)        | ✅ (real Bluetooth) |
| ActiveLook Connection | ❌ (no BLE)        | ✅ (with glasses)   |
| RideState Data        | ❌ (mock only)     | ✅ (during ride)    |

---

## Testing Checklist

### Phase 1: Basic Functionality

- [x] Install app on Karoo2
- [x] Launch app successfully
- [ ] Navigate through UI
- [ ] Check all buttons respond
- [ ] Verify no crashes

### Phase 2: Karoo Integration

- [ ] Start a ride on Karoo2
- [ ] Check if app detects ride state
- [ ] Verify metrics appear in app
- [ ] Monitor data updates
- [ ] Check connection stability

### Phase 3: ActiveLook Integration

- [ ] Power on ActiveLook glasses
- [ ] Scan for glasses in app
- [ ] Pair with glasses
- [ ] Test connection stability
- [ ] Send test text to glasses
- [ ] Verify display appears

### Phase 4: Full Pipeline

- [ ] Start ride on Karoo2
- [ ] Connect ActiveLook glasses
- [ ] Verify metrics display on glasses
- [ ] Check update frequency (~1Hz)
- [ ] Test for 10+ minutes
- [ ] Monitor battery impact
- [ ] Check for disconnections

---

## Known Issues / Observations

### Potential Issues to Watch For:

1. **KarooSystemService Connection**
    - May require special permissions on Karoo2
    - Might need to be registered as Karoo Extension
    - Could fail if not in active ride

2. **BLE Permissions**
    - Android 12+ requires BLUETOOTH_SCAN permission
    - May need runtime permission requests
    - Location permission needed for BLE scanning

3. **Background Service**
    - App needs to stay alive during ride
    - May need foreground service
    - Battery optimization might kill app

4. **Data Access**
    - RideState might only be available during active ride
    - Sensor data may not be available if sensors not connected
    - StreamState might be different on real device

---

## Debugging on Karoo2

### View Logs in Real-Time:

```powershell
adb logcat -s K2Look:* ActiveLookService:* KarooDataService:* AndroidRuntime:E
```

### Check App Status:

```powershell
adb shell dumpsys activity com.kema.k2look
```

### Check Permissions:

```powershell
adb shell dumpsys package com.kema.k2look | findstr permission
```

### Restart App:

```powershell
adb shell am force-stop com.kema.k2look
adb shell am start -n com.kema.k2look/.MainActivity
```

### Take Screenshot:

```powershell
adb exec-out screencap -p > karoo-screenshot.png
```

---

## Success Criteria

**Minimum Viable Test (MVP):**

- ✅ App installs on Karoo2
- ✅ App launches without crash
- [ ] UI is visible and usable
- [ ] Can scan for ActiveLook glasses
- [ ] Can connect to glasses
- [ ] Can display test text on glasses

**Full Success:**

- [ ] Karoo ride data flows to app
- [ ] App sends data to ActiveLook glasses
- [ ] Updates occur at ~1Hz
- [ ] Connection remains stable for 30+ minutes
- [ ] No crashes during ride

---

## Next Actions

1. **Immediate Testing:**
    - Navigate through the app UI on Karoo2
    - Check if all screens are accessible
    - Verify buttons and interactions work

2. **ActiveLook Glasses:**
    - Power on glasses
    - Attempt pairing through app
    - Test basic display functionality

3. **Karoo Data Testing:**
    - Start a ride on Karoo2
    - Monitor if app detects ride state
    - Check if metrics populate

4. **Report Findings:**
    - Document what works
    - Note any errors or crashes
    - Capture screenshots
    - Save log files

---

## Status: 🟢 RUNNING ON REAL HARDWARE

**Major Milestone Achieved!** The K2Look app is successfully running on the actual Karoo2 device.
This confirms the build configuration, package structure, and basic compatibility are all working
correctly.

**Next Focus:** Test ActiveLook glasses connection and Karoo data integration.

