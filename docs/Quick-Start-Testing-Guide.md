# Quick Start Guide - Deployment & Testing

**Version:** Phase 1 Complete  
**Last Updated:** 2024-11-22

---

## Prerequisites

### Required Hardware:

- ✅ Karoo2 cycling computer
- ✅ ActiveLook AR glasses (e.g., Engo 2)
- ✅ Development machine with Android Studio
- ✅ USB cable for Karoo connection

### Required Software:

- ✅ Android Studio (latest version)
- ✅ ADB (Android Debug Bridge)
- ✅ Java 17+
- ✅ Project built successfully

---

## Building the APK

### Debug Build (Development):

```bash
cd C:\Project\k2-look
.\gradlew :app:assembleDebug
```

**Output:** `app/build/outputs/apk/debug/app-debug.apk`

### Release Build (Production):

```bash
.\gradlew :app:assembleRelease
```

**Output:** `app/build/outputs/apk/release/app-release-unsigned.apk`

---

## Installing on Karoo2

### Method 1: ADB (Recommended for Development)

1. **Enable Developer Mode on Karoo2:**
    - Go to Settings → About
    - Tap "Build number" 7 times
    - Developer options will appear

2. **Enable USB Debugging:**
    - Settings → Developer options
    - Enable "USB debugging"

3. **Connect Karoo2 to Computer:**
   ```bash
   # Verify connection
   adb devices
   
   # Should show:
   # List of devices attached
   # <serial>    device
   ```

4. **Install the APK:**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   
   # Or to reinstall (if already installed):
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

5. **Launch the App:**
   ```bash
   adb shell am start -n io.hammerhead.karooexttemplate/.MainActivity
   ```

### Method 2: File Transfer (Alternative)

1. Copy APK to Karoo2 via USB storage mode
2. Use a file manager app on Karoo to locate the APK
3. Tap to install (requires "Install from Unknown Sources" enabled)

---

## First-Time Setup

### 1. Launch the App

Find "Karoo Extension Template" in the Karoo app drawer and launch it.

### 2. Grant Permissions

The app will request:

- **Bluetooth** - Required for ActiveLook connection
- **Location** (Android requirement for BLE scanning)

### 3. Connect to Karoo System

1. Tap **"Connect"** in the Karoo Connection card
2. Wait for status to show "Connected" (green)
3. Verify you see "Ride State: Idle"

### 4. Pair ActiveLook Glasses

1. Turn on your ActiveLook glasses
2. Tap **"Scan"** in the ActiveLook Glasses card
3. Wait for your glasses to appear in the list
4. Tap on your glasses to connect
5. Wait for status to show "Connected" or "Streaming to Glasses ✓"

---

## Testing Checklist

### Basic Connection Test ✅

- [ ] App launches without crashing
- [ ] "Connect to Karoo" button works
- [ ] Karoo connection succeeds
- [ ] "Scan" for glasses works
- [ ] Glasses appear in discovered list
- [ ] Tap-to-connect works
- [ ] Glasses connection succeeds

### Data Display Test ✅

- [ ] Metrics show "--" when idle
- [ ] Start a ride on Karoo
- [ ] Metrics update with live data:
    - [ ] Speed (km/h)
    - [ ] Heart Rate (bpm) - if sensor connected
    - [ ] Cadence (rpm) - if sensor connected
    - [ ] Power (w) - if power meter connected
    - [ ] Distance (km)
    - [ ] Time (HH:MM:SS)

### ActiveLook Display Test ✅

**With glasses connected and ride recording:**

- [ ] Glasses display shows data
- [ ] All 6 metrics visible
- [ ] Labels readable (SPD, HR, PWR, CAD, DIST, TIME)
- [ ] Values readable
- [ ] Updates approximately every 1 second
- [ ] No flickering or ghosting

### Stability Test ✅

- [ ] Ride for 10+ minutes
- [ ] Connection stays stable
- [ ] No disconnections
- [ ] Data updates consistently
- [ ] App doesn't crash

### Edge Cases Test ⚠️

- [ ] What happens if glasses disconnect?
- [ ] What happens if Karoo disconnects?
- [ ] Reconnection works?
- [ ] Pause ride - does display update?
- [ ] Resume ride - streaming continues?
- [ ] End ride - graceful handling?

---

## Troubleshooting

### App Won't Install

**Error: "INSTALL_FAILED_UPDATE_INCOMPATIBLE"**

```bash
# Uninstall old version first
adb uninstall io.hammerhead.karooexttemplate
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Error: "INSTALL_FAILED_INSUFFICIENT_STORAGE"**

- Free up space on Karoo2
- Remove unused apps or rides

### Karoo Connection Issues

**"Error: Failed to connect"**

- Restart the app
- Restart Karoo2
- Check karoo-ext SDK is compatible with Karoo firmware

**"Disconnected" status**

- Check Karoo isn't in airplane mode
- Verify app has necessary permissions
- Check LogCat for error details:
  ```bash
  adb logcat | grep "KarooDataService"
  ```

### ActiveLook Connection Issues

**No glasses found during scan**

- Turn glasses off and on
- Check glasses are charged
- Verify Bluetooth is enabled
- Ensure location permission granted (Android BLE requirement)
- Try moving closer to glasses

**Connection fails**

- Unpair glasses from Android Bluetooth settings
- Restart glasses
- Restart app
- Try scanning again

**Display not showing data**

- Check bridge status shows "Streaming to Glasses ✓"
- Verify ride is recording (not idle)
- Check glasses brightness setting
- Try disconnecting and reconnecting

### Data Not Updating

**Metrics show "--"**

- Ensure ride is recording (not paused/idle)
- Check sensors are connected in Karoo
- Verify Karoo connection status is "Connected"

**Values frozen**

- Check LogCat for errors:
  ```bash
  adb logcat | grep "KarooActiveLookBridge"
  ```
- Try disconnecting and reconnecting
- Restart app

---

## Viewing Logs

### Real-Time Logging:

```bash
# All app logs
adb logcat | grep "io.hammerhead.karooexttemplate"

# Karoo service logs
adb logcat | grep "KarooDataService"

# ActiveLook service logs
adb logcat | grep "ActiveLookService"

# Bridge logs
adb logcat | grep "KarooActiveLookBridge"

# ViewModel logs
adb logcat | grep "MainViewModel"
```

### Save Logs to File:

```bash
adb logcat > k2-look-logs.txt
```

### Clear Logs:

```bash
adb logcat -c
```

---

## Performance Monitoring

### Check Update Frequency:

Look for "Data flushed to glasses" messages in LogCat. Should appear ~1 per second.

```bash
adb logcat | grep "flushed to glasses"
```

### Monitor Connection State:

```bash
adb logcat | grep "state changed"
```

### Check for Errors:

```bash
adb logcat *:E | grep "karooexttemplate"
```

---

## Expected Behavior

### Normal Operation:

1. App launches → Shows "Disconnected" status
2. Connect Karoo → Status changes to "Connected" (green)
3. Scan for glasses → Shows scanning status
4. Connect glasses → Status shows "Connected" or "Fully Connected"
5. Start ride → Status changes to "Streaming to Glasses ✓"
6. Data updates every ~1 second
7. End ride → Streaming continues (or stops, based on config)
8. Disconnect → Clean shutdown

### Typical Log Flow:

```
I/KarooActiveLookBridge: Initializing KarooActiveLookBridge...
I/ActiveLookService: Initializing ActiveLook SDK...
I/ActiveLookService: ✓ ActiveLook SDK initialized successfully
I/KarooActiveLookBridge: ✓ Bridge initialized
I/KarooDataService: Connecting to KarooSystem...
I/KarooDataService: Successfully connected to KarooSystem
I/ActiveLookService: Starting BLE scan for ActiveLook glasses...
D/ActiveLookService: Discovered glasses: Engo 2 (XX:XX:XX:XX:XX:XX)
I/ActiveLookService: Connecting to glasses: Engo 2 (XX:XX:XX:XX:XX:XX)...
I/ActiveLookService: ✓ Successfully connected to Engo 2
I/KarooActiveLookBridge: Starting data streaming to glasses...
I/KarooActiveLookBridge: ✓ Streaming started (1 update/second)
D/KarooActiveLookBridge: ✓ Data flushed to glasses
D/KarooActiveLookBridge: ✓ Data flushed to glasses
...
```

---

## Field Testing Protocol

### Short Test Ride (15-30 minutes):

1. ✅ Install and connect everything
2. ✅ Start a ride
3. ✅ Monitor display updates
4. ✅ Note any issues
5. ✅ End ride and review logs

### Medium Test Ride (1-2 hours):

1. Test connection stability over time
2. Test various speeds and conditions
3. Test sensor dropout scenarios
4. Measure battery impact
5. Collect detailed logs

### Long Test Ride (3+ hours):

1. Full endurance test
2. Monitor battery levels (Karoo + Glasses)
3. Test in various conditions (hills, flats, sprints)
4. Document any disconnections
5. Gather user experience feedback

---

## Data Collection

### Metrics to Track:

- Connection uptime (%)
- Disconnect events (count & causes)
- Data update frequency (actual vs target 1Hz)
- Battery drain (Karoo + glasses)
- Display readability (subjective)
- Latency (Karoo update → glasses display)

### Useful Commands:

```bash
# Battery stats
adb shell dumpsys battery

# App memory usage
adb shell dumpsys meminfo io.hammerhead.karooexttemplate

# CPU usage
adb shell top | grep karooexttemplate
```

---

## Post-Test Analysis

After each test ride:

1. **Save logs:**
   ```bash
   adb logcat > ride-test-YYYY-MM-DD.txt
   ```

2. **Document issues:**
    - What went wrong?
    - When did it happen?
    - How often?
    - Can it be reproduced?

3. **Measure success:**
    - Connection stability %
    - Data accuracy
    - Display readability
    - Battery impact
    - User satisfaction

4. **Create GitHub issues** for any bugs found

---

## Success Criteria

### Phase 1 Field Testing Goals:

- [ ] 95%+ connection stability during 1-hour ride
- [ ] All metrics display correctly
- [ ] 1Hz update frequency maintained
- [ ] No crashes or forced restarts
- [ ] Acceptable battery drain (<10% extra)
- [ ] Display readable in various lighting conditions

---

## Support & Feedback

### Reporting Issues:

1. Collect logs (see above)
2. Document steps to reproduce
3. Note Karoo firmware version
4. Note glasses model and firmware
5. Create detailed bug report

### Feature Requests:

Document in TODO.md with:

- Use case
- Priority
- Technical feasibility
- Target phase

---

## Quick Reference Commands

```bash
# Build
.\gradlew :app:assembleDebug

# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch
adb shell am start -n io.hammerhead.karooexttemplate/.MainActivity

# Logs
adb logcat | grep "KarooActiveLookBridge"

# Uninstall
adb uninstall io.hammerhead.karooexttemplate

# Restart ADB
adb kill-server
adb start-server
```

---

**Ready to test!** 🚴‍♂️👓

Good luck with your field testing. Remember to start small (short test rides) and gradually increase
duration as stability is confirmed.

---

*End of Quick Start Guide*

