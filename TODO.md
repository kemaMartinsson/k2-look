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

### 1.1 Karoo System Integration
- [ ] Implement KarooSystemService connection
  - [ ] Create service lifecycle management (connect/disconnect)
  - [ ] Implement RideState consumer registration
  - [ ] Add error handling and reconnection logic
  - [ ] Test basic data retrieval from Karoo

### 1.2 ActiveLook BLE Integration
- [ ] Initialize ActiveLook SDK
  - [ ] Configure SDK with update callbacks
  - [ ] Implement BLE scanning for glasses
  - [ ] Create connection management (connect/disconnect)
  - [ ] Add connection state monitoring
  - [ ] Test basic text display on glasses

### 1.3 Data Bridge
- [ ] Create KarooActiveLookBridge service
  - [ ] Connect Karoo data stream to ActiveLook output
  - [ ] Implement data transformation layer
  - [ ] Add hold/flush pattern for efficient updates
  - [ ] Throttle updates to ~1 update/second
  - [ ] Test end-to-end data flow

---

## Phase 2: Metric Implementation

### 2.1 Basic Metrics Display
- [ ] Speed metrics
  - [ ] Current speed
  - [ ] Average speed
  - [ ] Max speed
- [ ] Distance metrics
  - [ ] Current distance
  - [ ] Total distance
- [ ] Time metrics
  - [ ] Ride time
  - [ ] Elapsed time

### 2.2 Performance Metrics
- [ ] Heart rate
  - [ ] Current HR
  - [ ] Average HR
  - [ ] Max HR
  - [ ] HR zones (optional)
- [ ] Cadence
  - [ ] Current cadence
  - [ ] Average cadence
  - [ ] Max cadence
- [ ] Power
  - [ ] Instant power
  - [ ] Average power
  - [ ] Max power
  - [ ] 3s/10s/30s power

### 2.3 Advanced Metrics
- [ ] Elevation
  - [ ] Current altitude
  - [ ] Elevation gain
  - [ ] Elevation loss
- [ ] Battery levels
  - [ ] Karoo2 battery
  - [ ] Sensor batteries
- [ ] Navigation data
  - [ ] Turn-by-turn instructions
  - [ ] Distance to next turn
  - [ ] ETA

---

## Phase 3: Display Management

### 3.1 Layout System
- [ ] Design layout templates
  - [ ] Performance layout (speed, HR, power)
  - [ ] Training layout (time, distance, cadence)
  - [ ] Navigation layout (turn instructions, distance)
  - [ ] Custom layout support
- [ ] Implement layout engine
  - [ ] Upload layouts to glasses memory
  - [ ] Switch between layouts
  - [ ] Page cycling logic
- [ ] Optimize display area
  - [ ] Implement 30px horizontal margins
  - [ ] Implement 25px vertical margins
  - [ ] Use transparent backgrounds (black = transparent)

### 3.2 Graphical Resources
- [ ] Create icon set
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

**Sprint 1: Foundation (Current)**
- Focus: Establish core integration between Karoo2 and ActiveLook
- Priority Tasks:
  1. Implement KarooSystemService connection
  2. Initialize ActiveLook SDK
  3. Create basic data bridge
  4. Display first metric (speed) on glasses

---

## Notes & Decisions

### Technical Decisions
- Using Kotlin for implementation (karoo-ext native language)
- ActiveLook SDK 4.5.6 from JitPack
- karoo-ext 1.1.6 from GitHub Packages
- Target Android API level: 28+ (Karoo2 compatible)

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
