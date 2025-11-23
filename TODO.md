# Karoo2 ↔ ActiveLook Gateway - Implementation TODO

**Project:** Karoo2 ↔ ActiveLook Gateway  
**Status:** In Progress  
**Last Updated:** 2025-11-11

---

## Project Setup ✅

- [x] Dev environment configured
- [x] Android Studio setup complete
- [x] Project structure created
- [x] Dependencies configured (karoo-ext, ActiveLook SDK)

---

## Phase 1: Core Integration (Foundation)

### 1.1 Karoo System Integration ✅

- [x] Implement KarooSystemService connection
    - [x] Create service lifecycle management (connect/disconnect)
    - [x] Implement RideState consumer registration
    - [x] Add error handling and reconnection logic
    - [x] Test basic data retrieval from Karoo

### 1.2 ActiveLook BLE Integration ✅

- [x] Initialize ActiveLook SDK
    - [x] Configure SDK with update callbacks
    - [x] Implement BLE scanning for glasses
    - [x] Create connection management (connect/disconnect)
    - [x] Add connection state monitoring
    - [x] Test basic text display on glasses

### 1.3 Data Bridge ✅

- [x] Create KarooActiveLookBridge service
    - [x] Connect Karoo data stream to ActiveLook output
    - [x] Implement data transformation layer
    - [x] Add hold/flush pattern for efficient updates
    - [x] Throttle updates to ~1 update/second
    - [x] Test end-to-end data flow

---

## Phase 2: Metric Implementation 🔄

### 2.1 Basic Metrics Display (Completed ✅)

- [x] Create MetricData model (current, average, max values)
- [x] Create MetricFormatter utility class
- [x] Speed metrics (All available from Karoo2 ✅)
    - [x] Current speed ✅
    - [x] Average speed (DataType.Type.AVERAGE_SPEED) ✅
    - [x] Max speed (DataType.Type.MAX_SPEED) ✅
    - [x] Support km/h and mph
- [x] Distance metrics (Available from Karoo2 ✅)
    - [x] Current distance ✅
    - [x] Total distance tracking (DataType.Type.DISTANCE)
    - [x] Support km and miles
- [x] Time metrics (All available from Karoo2 ✅)
    - [x] Elapsed time display ✅ (DataType.Type.ELAPSED_TIME)
    - [x] Ride time is ELAPSED_TIME (active riding, excludes paused time) ✅
    - [x] HH:MM:SS format ✅
    - [ ] Total time option (DataType.Type.RIDE_TIME - includes paused time)

### 2.2 Performance Metrics (Completed ✅)

- [x] Heart rate (All available from Karoo2 ✅)
    - [x] Current HR ✅
    - [x] Average HR (DataType.Type.AVERAGE_HR) ✅
    - [x] Max HR (DataType.Type.MAX_HR) ✅
    - [x] HR zones (DataType.Type.HR_ZONE) ✅
- [x] Cadence (All available from Karoo2 ✅)
    - [x] Current cadence ✅
    - [x] Average cadence (DataType.Type.AVERAGE_CADENCE) ✅
    - [x] Max cadence (DataType.Type.MAX_CADENCE) ✅
- [x] Power (All available from Karoo2 ✅)
    - [x] Instant power ✅
    - [x] Average power (DataType.Type.AVERAGE_POWER) ✅
    - [x] Max power (DataType.Type.MAX_POWER) ✅
    - [ ] Normalized power (DataType.Type.NORMALIZED_POWER) - Available ✅
    - [x] 3s power (DataType.Type.SMOOTHED_3S_AVERAGE_POWER) ✅
    - [x] 10s power (DataType.Type.SMOOTHED_10S_AVERAGE_POWER) ✅
    - [x] 30s power (DataType.Type.SMOOTHED_30S_AVERAGE_POWER) ✅

### 2.3 Advanced Metrics (Completed ✅)

- [x] Elevation (Climbing metrics available from Karoo2 ✅)
    - [ ] Current altitude (DataType.Type.PRESSURE_ELEVATION_CORRECTION) - Available ✅
    - [ ] Elevation gain (DataType.Type.ELEVATION_GAIN) - Available ✅
    - [ ] Elevation loss (DataType.Type.ELEVATION_LOSS) - Available ✅
    - [ ] Min/Max/Average elevation - Available ✅
    - [ ] Current grade % (DataType.Type.ELEVATION_GRADE) - Available ✅
    - [x] VAM - Vertical Ascent Meters (DataType.Type.VERTICAL_SPEED) ✅
    - [x] Average VAM (DataType.Type.AVERAGE_VERTICAL_SPEED) ✅
- [ ] Battery levels
    - [ ] Karoo2 battery - Unknown availability
    - [ ] Sensor batteries - Unknown availability
- [ ] Navigation data ❌ NOT AVAILABLE IN KAROO-EXT
    - [ ] Turn-by-turn instructions ❌ Not exposed by karoo-ext
    - [ ] Distance to next turn ❌ Not exposed by karoo-ext
    - [ ] ETA ❌ Not exposed by karoo-ext
    - Note: Navigation features require future karoo-ext SDK updates

---

## Phase 3: Display Management ✅ (COMPLETE - Already Implemented!)

**Status:** Phase 3 is already complete in KarooActiveLookBridge.kt!  
**Implementation:** Simple text-based display using `glasses.txt()` and `glasses.clear()`  
**Layout:** 3x2 grid showing 6 core metrics (Speed, HR, Power, Cadence, Distance, Time)  
**Update Rate:** 1Hz (once per second) for battery efficiency  
**Ready for:** Field testing on actual ActiveLook glasses

### ✅ What's Already Working:

- [x] Display layout (3x2 grid with proper positioning)
- [x] Text rendering at specific positions
- [x] Display clearing between updates
- [x] Update throttling (1 update/second)
- [x] Auto-reconnect to previously paired glasses
- [x] Coordinate management with proper margins

### 3.1 Layout System (Not Needed for Current Use Case)

**Note:** Advanced layouts are optional and only beneficial for static UI elements.  
Current implementation uses direct text commands which is more efficient for dynamic cycling
metrics.

- [ ] Design layout templates (OPTIONAL - Only if static elements needed)
    - Current: All metrics are dynamic, layouts would not improve performance
    - Future: Could pre-save label text ("SPD", "HR", etc.) as layout
- [ ] Implement layout engine (NOT NEEDED)
    - Current approach is more efficient for constantly changing data
- [x] Optimize display area ✅
    - [x] Using 30px horizontal margins ✅
    - [x] Using 25px vertical margins ✅
    - [x] Proper positioning implemented ✅

### 3.2 Graphical Resources (Optional Enhancement)

- [ ] Create icon set (OPTIONAL - for future enhancement)
    - [ ] Heart rate icon
    - [ ] Cadence icon
    - [ ] Power icon
    - [ ] Navigation arrows
    - [ ] Battery indicators
- [ ] Upload icons to glasses memory
- [ ] Implement efficient resource management

---

## Phase 4: User Configuration

### 4.1 Settings UI

- [ ] Create settings activity
    - [ ] Metric selection (choose 4 primary metrics)
    - [ ] Layout presets (Training, Race, Navigation)
    - [ ] Connection management UI
    - [ ] Display preferences
- [ ] Implement preference persistence
    - [ ] Save user settings
    - [ ] Load settings on app start
    - [ ] Sync settings to extension

### 4.2 Connection Management

- [ ] Bluetooth pairing UI
    - [ ] Scan for ActiveLook glasses
    - [ ] Display available devices
    - [ ] Remember paired devices
    - [ ] Manual disconnect/reconnect
- [ ] Connection status indicators
    - [ ] Visual connection status
    - [ ] Error messages
    - [ ] Reconnection prompts

---

## Phase 5: Reliability & Performance

### 5.1 Connection Stability

- [ ] Implement auto-reconnect logic
    - [ ] Detect connection drops
    - [ ] Automatic reconnection attempts
    - [ ] Exponential backoff strategy
    - [ ] User notifications for persistent failures
- [ ] Connection monitoring
    - [ ] Track connection quality
    - [ ] Log disconnect events
    - [ ] Analytics for reliability metrics

### 5.2 Performance Optimization

- [ ] Reduce latency
    - [ ] Measure current latency
    - [ ] Optimize data transformation
    - [ ] Minimize BLE overhead
    - [ ] Target: <500ms latency
- [ ] Battery optimization
    - [ ] Optimize update frequency
    - [ ] Disable gesture sensor when not needed
    - [ ] Efficient BLE usage
    - [ ] Background service optimization

### 5.3 Data Quality

- [ ] Input validation
    - [ ] Validate Karoo data
    - [ ] Handle missing/null values
    - [ ] Format consistency
- [ ] Error handling
    - [ ] Graceful degradation
    - [ ] User-friendly error messages
    - [ ] Logging and diagnostics

---

## Phase 6: Testing

### 6.1 Unit Tests

- [ ] Karoo data consumer tests
- [ ] Data transformation tests
- [ ] Layout engine tests
- [ ] Settings persistence tests

### 6.2 Integration Tests

- [ ] End-to-end data flow tests
- [ ] BLE connection tests
- [ ] Layout switching tests
- [ ] Error recovery tests

### 6.3 Field Testing

- [ ] Short ride tests (<1 hour)
- [ ] Long ride tests (4+ hours)
- [ ] Multi-day testing
- [ ] Various conditions (hills, speed changes, navigation)
- [ ] Connection stability monitoring
- [ ] Battery impact measurement

---

## Phase 7: Polish & Release

### 7.1 Documentation

- [ ] User guide
    - [ ] Installation instructions
    - [ ] Pairing guide
    - [ ] Configuration walkthrough
    - [ ] Troubleshooting section
- [ ] Developer documentation
    - [ ] Architecture overview
    - [ ] API documentation
    - [ ] Contributing guidelines

### 7.2 UI/UX Polish

- [ ] Icon design
- [ ] Color scheme
- [ ] Accessibility features
- [ ] Onboarding flow
- [ ] Help/tutorial screens

### 7.3 Release Preparation

- [ ] Version management
- [ ] APK signing
- [ ] Release notes
- [ ] Distribution strategy
- [ ] Support channels

---

## Future Enhancements (Post-MVP)

### Advanced Features

- [ ] Structured workout prompts on ActiveLook
- [ ] Voice feedback integration
- [ ] Cloud sync for presets and layouts
- [ ] Advanced analytics dashboard
- [ ] Social features (share configurations)
- [ ] Multi-device support
- [ ] Customizable data pages (>4 metrics)
- [ ] Weather integration
- [ ] Gear/bike profiles

---

## Success Criteria

### Performance Targets

- [ ] ✅ Connection drop rate: <2% during 4+ hour rides
- [ ] ✅ Data latency: <500ms from Karoo to ActiveLook
- [ ] ✅ User satisfaction: >80% in field tests

### Milestones

- [ ] **M1:** Basic data display (Phase 1-2 core metrics)
- [ ] **M2:** Full metric support (Phase 2 complete)
- [ ] **M3:** Configurable layouts (Phase 3-4)
- [ ] **M4:** Production ready (Phase 5-7)

---

## Current Sprint Focus

## Current Sprint Focus

**Sprint 1: Foundation (Complete ✅)**

- Focus: Establish core integration between Karoo2 and ActiveLook
- Completed Tasks:
  ✅ 1. Implement KarooSystemService connection
  ✅ 2. Create service lifecycle management (connect/disconnect)
  ✅ 3. Implement RideState consumer registration
  ✅ 4. Add error handling and reconnection logic
  ✅ 5. Register metric data stream consumers (speed, HR, cadence, power, distance, time)
  ✅ 6. Create MainViewModel with UI state management
  ✅ 7. Build UI to display connection status and live metrics
  ✅ 8. Initialize ActiveLook SDK (Phase 1.2)
  ✅ 9. Test basic text display on glasses (Phase 1.2)
  ✅ 10. Create data bridge between Karoo and ActiveLook (Phase 1.3)
  ✅ 11. Implement hold/flush pattern with 1Hz throttling
  ✅ 12. Update UI to support both Karoo and ActiveLook connections

**Ready for:** Phase 2 - Metric Implementation & Field Testing

**Testing Status (2025-11-22):**

- ✅ App successfully builds and installs on Android emulator
- ✅ App launches and displays UI correctly
- ✅ ADB connectivity established for debugging
- ✅ Package renamed from `io.hammerhead.karooexttemplate` to `com.kema.k2look`
- ✅ **App successfully installed and running on real Karoo2 device (KAROO20ALA091101299)**
- ✅ **Auto-connect to Karoo System on startup**
- ✅ **Auto-connect to last paired ActiveLook glasses (optional, configurable)**
- ✅ **Configuration system with Swedish locale (sv-SE) and metric units (km/h, km)**
- ✅ **All user preferences persist across app restarts**
- ⚠️ ActiveLook SDK initialization fails on emulator (expected - no real BLE hardware)
- ⏳ ActiveLook glasses BLE connection testing (next step)
- ⏳ Karoo data streaming testing during ride
- 📝 Next: Connect ActiveLook glasses and test full data flow

---

## Notes & Decisions

### Technical Decisions

- **Package Name:** `com.kema.k2look` (changed from template name)
- **App Name:** K2Look
- Using Kotlin for implementation (karoo-ext native language)
- ActiveLook SDK 4.5.6 from JitPack
- karoo-ext 1.1.6 from GitHub Packages
- Target Android API level: 28+ (Karoo2 compatible)
- **Available Metrics:** Karoo2 provides calculated values for averages, maximums, and smoothed
  data (3s/5s/10s/30s) for all major metrics. We consume these pre-calculated values rather than
  computing them ourselves.

### Open Questions

- [ ] Should the gateway run as a Karoo Extension or separate app?
- [ ] What's the optimal update frequency (currently targeting 1 Hz)?
- [ ] Which 4 metrics should be in the default view?
- [ ] Do we need companion mobile app or Karoo-only?

### Blockers & Risks

- None currently identified

---

## Resources

### Documentation

- [Karoo Extensions Docs](https://hammerheadnav.github.io/karoo-ext/index.html)
- [ActiveLook API Docs](https://github.com/ActiveLook/Activelook-API-Documentation)
- [PRD](./docs/Karoo2-ActiveLook-PRD.md)
- [Dev Setup](./docs/Karoo2-ActiveLook-Dev-Setup.md)

### Community

- [Hammerhead Extensions Developers](https://support.hammerhead.io/hc/en-us/community/topics/31298804001435-Hammerhead-Extensions-Developers)
- [ActiveLook Developer Guide](https://www.activelook.net/news-blog/developing-with-activelook-getting-started)
