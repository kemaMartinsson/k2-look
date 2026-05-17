# Changelog

All notable changes to K2Look will be documented in this file.

## [1.0.10] - 2026-05-17

### Added

- Countdown start animation

## [1.0.9] - 2026-05-15

### Changed

- Zone presentation
  - Changed zone presentation to show current zone incl metric and inactive zones as smaller circles.
- New radar warning icon

### Fixed

- Connect issue
  - Fixed an issue where the app would fail to connect to glasses after new installation.  
- Speed
  - Fixed speed metric that was displayed in `m/s` instead of `km/h` or `mph` depending on user settings.
- Distance
  - Fixed distance metric that was displayed in `m` instead of `km` or `mi` depending on user settings.
- Decimal places
  - Fixed decimal places for metrics showing unnecessary precision.  
    Distance now show one decimal place up till 100km, then no decimal places.
    Other metrics show no decimal places.


## [1.0.8] - 2026-05-02

### Added

- Glasses battery level overlay
  - New toggle in settings to show/hide battery level on glasses
  - Shows battery percentage with icon in top-left corner of glasses display
  - Automatically updates during rides and when battery level changes
  - Icon is changed when battery level is below 10%

## [1.0.7] - 2026-04-26

### Fixed

- Fixed broken glasses connection introduces in 1.0.0 with code splitting and refactoring.

### Removed

- Removed glasses connection timeout. Engo2 stop searching for glasses after ~3min so having a configurable timeout is not really useful.
  
## [1.0.0] - 2026-04-20

### Added

- Updated metrics
  Total:74 metrics
- Added global radar alert
  Cross profile function.
- Sync used K2 profile with K2Look
  K2Look will switch to a profile with same name as K2 during a ride.

### Fixed

- Tab layout
  Stacked instead of one row.
- Fixed positioning of icons/text/units
- Glasses reconnect sequence
  Issues with scan caused constant BLE reconnects.
- Slow down updates
  Changed from every 1 second to every 2 seconds.
  Reduces BLE queue pressure, giving other commands room to execute.
- Enable gestures
  Added, but still glitchy.

### Known issues

- Gestures still glitchy, works but slow.
- Touch seems intermittent, works sometime
Both could be due to command que and delay.

### Todo

- Add more layouts, eg pyramid
- Add gauge
- Look into gesture/touch
- Tweak icon/text/unit alignment

### Technical

- Massive refactoring of codebase making it more modular and maintainable.
- Added comprehensive unit tests for all new features and critical code paths.
- Improved error handling and logging throughout the app.

## [0.13.0] - 2026-03-22

### Added

- **Karoo Sync — Import your Karoo ride profile automatically**
    - K2Look can now read your active Karoo ride profile and generate a matching display layout
    - A **"From Karoo"** suggestion card appears in Profile Management whenever Karoo switches to a
      profile that has no matching K2Look profile (only shown when not riding)
    - An **Import Preview** dialog shows which screens and fields will be created before you confirm
    - Karoo page order is preserved; each page becomes one screen with the correct template (1–6D)
    - Unknown third-party extension fields are skipped gracefully with a log warning
- **Karoo Sync toggle**
    - A **Karoo Sync** switch at the top of Profile Management turns the auto-switch and
      import suggestions on or off (default: on)
    - Setting is persisted between app restarts
- **Expanded metrics: 23 → 74 total across 14 categories**
    - **Elevation** (new): Grade, Ascent, Descent, Altitude, VAM 30s
    - **Energy** (new): Energy (kJ), Calories, Cal/hr
    - **Lap** (new): Lap #, Lap Time, Lap Dist, Lap Speed, Lap HR, Lap Power, Lap NP, Lap Cadence, Lap Ascent
    - **Last Lap** (new): L.Lap Time, L.Lap Dist, L.Lap Speed, L.Lap HR, L.Lap Power, L.Lap NP
    - **Radar** (new): Radar Threat, Radar Targets, Radar Range *(requires Garmin Varia or compatible)*
    - **Shifting** (new): Front Gear, Rear Gear, Drive Battery, Shift Count *(requires Di2/AXS/eTap)*
    - **Navigation** (new): To Turn, To Finish, ETA, Time to End, Heading *(requires active route)*
    - **eBike** (new): Bike Battery, Est. Range, Assist Mode, Motor Power *(requires LEV sensor)*
    - **General** additions: Clock, Temperature, Karoo Battery, Ride Time
    - **Heart Rate** additions: % Max HR, % HR Reserve
    - **Power** additions: Power 5s, Power 10s, Power 30s, Norm. Power, % FTP, Intensity Factor, TSS, W/kg
    - **Speed** addition: Speed 3s
    - **Cadence** addition: Cadence 3s
- **Five Data (5D) layout template** — one wide top field + 2×2 grid below (7 templates total)

### Changed

- **Profiles are now fully equal — no "system profile" or "read-only" concept**
    - Removed `isDefault` and `isReadOnly` flags from the profile model
    - The Default profile is seeded on first install and behaves exactly like any user profile
    - Old installs are migrated transparently: the Default profile is written to storage once
      on the first launch after update, with no duplicates
- **Duplicate profile name validation**
    - Creating or duplicating a profile with an existing name (case-insensitive) is now
      rejected with a clear error message
- **Last-profile guard**
    - The delete button is disabled (greyed out) only when one profile remains, instead of
      the previous logic that blocked deleting "the default" profile

### Fixed

- **"Send to Glasses" — button now works and gives feedback**
    - Renamed from "Build & Send" to **"Send to Glasses"** — clearer description of what it does
    - Pressing the button now immediately updates the glasses display: `--` placeholders when
      not riding, live values during a ride
    - A green confirmation banner appears in the app after a successful send
    - Button shows "Send to Glasses (not connected)" when glasses are not connected

### Technical

- **KarooActiveLookBridge refactored** — formatter methods extracted to `BridgeMetricFormatters.kt`
  as package-level functions; removed 15 duplicate private methods and one duplicate public accessor
  that had been left behind from the original extraction

## [0.12.5] - 2026-03-113

### Fixed 

- Fixed tabs. Text took too much space and made the UI look bad. Now the tabs are smaller and more compact, allowing more space for the content and improving the overall appearance of the app.
- Fixed hardcoded disable button for sending config to glasses. Now the button is enabled and functional if glasses are connected, allowing users to send their custom configurations to the glasses directly from the app.
- Changed default profile. Not readonly anymore, since, well. You can have a profile named `Default` so... yeah. It's just a default profile that you can edit and customize as you wish, and it will be the one used when you don't have any other profile selected or active on Karoo 2.

## [0.12.4] - 2026-01-01

### Fixed

- Updates after failed full build validations
- Excluding activelook sdk and karoo-ext from tests

## [0.12.3] - 2026-01-01

### Changed

- **Updating docs and README.md.**
- **Update CI workflow, adding full build validation.**

## [0.12.2] - 2026-01-01

### Changed

- **Updating docs and README.md.**
    - Improved README.md
    - Updating INSTALLATION.md

## [0.12.1] - 2025-12-31

### Changed

- **Improved Update Dialog UX**
    - "GitHub Release Page" text is now a clickable link that opens the release page in browser
    - "Download" button directly downloads and installs the APK
    - Users can either download directly or view full release notes on GitHub

### Fixed

- **Version Comparison for Patch Releases**
    - Fixed version code extraction to properly handle patch versions (0.12.1, 0.12.2, etc.)
    - Version comparison now correctly identifies newer patch releases
    - Example: 0.12 (code 1200) < 0.12.1 (code 1201) ✓

## [0.12] - 2025-12-31

### Changed

- **Improved Update Dialog UI**
    - Simplified and cleaned up update notification dialog
    - Removed messy markdown rendering from update messages
    - Cleaner layout with better spacing and professional appearance
    - "View Release" button now opens GitHub Release page in browser instead of downloading
    - Users can download APK and view full CHANGELOG directly from GitHub

### Fixed

- **Update Installation Issues**
    - Fixed "App not installed" error when trying to install updates
    - Changed approach from automatic download/install to browser-based download
    - Avoids signature mismatch issues between debug and release builds
    - Users can now properly download and install updates from GitHub Releases
    - Added permission checking for installing unknown apps (Android 8.0+)

- **Version Comparison Logic**
    - Fixed false update notifications when already on latest version
    - Improved semantic version comparison for 0.x releases
    - Added detailed logging for version checks

- **Dialog UI Improvements**
    - Centered text alignment in "You're up to date!" dialog
    - Better padding and spacing throughout dialogs
    - Improved visual consistency across all update dialogs

## [0.11] - 2025-12-31

### Added

- **Proper resource cleanup on "Forget Glasses"**
    - Forgetting connected glasses now clears all associated resources
    - Prevents residual data from interfering with future connections
    - Ensures a clean state for re-pairing or switching glasses

### Changed

- **Activelook SDK compliance report**
    - Updated compliance report to reflect latest changes

## [0.10] - 2025-12-30

### Added

- **DataField Builder**: Create custom data field configurations with intuitive UI
    - Select from 20+ available metrics (HR, Power, Speed, Cadence, VAM, etc.)
    - Configure font size (Small, Medium, Large)
    - Toggle labels, units, and icons for each metric
    - Icon size selection (28x28px or 40x40px)
    - Live preview of configured metrics

- **Advanced Visualizations**: Multiple display styles for metrics
    - **Gauges**: Circular arc visualizations (Power, Heart Rate, Cadence)
    - **Progress Bars**: Horizontal/vertical bars for linear metrics
    - **Zoned Bars**: Color-coded zones for Heart Rate and Power zones
    - Automatic zone calculation based on Karoo 2 profile settings

- **Layout Templates**: Six official ActiveLook layout templates
    - 1D: Single large data field
    - 2D: Two data fields (top/bottom)
    - 3D Triangle: Three fields in triangle layout
    - 3D Full: Three fields stacked vertically
    - 4D: Four fields in grid layout
    - 5D: Five fields with mixed sizes
    - 6D: Six fields in compact grid
    - Visual preview for each template
    - Precise positioning based on ActiveLook specifications

- **Profile Management**: Multiple configurable profiles
    - Create unlimited custom profiles
    - Name profiles to match Karoo 2 bike profiles
    - Automatic profile switching based on active Karoo 2 profile
    - Duplicate and delete profiles
    - Each profile supports multiple screens
    - Per-screen layout template selection

- **Multi-Screen Support**: Navigate between different metric screens
    - Add/remove screens within profiles
    - Each screen can have 1-6 data fields
    - Independent layout template per screen
    - Screen naming for easy identification

- **Gesture Controls**: Hand gesture support for glasses interaction
    - **Double Tap**: Cycle through screens
    - **Swipe Up/Down**: Cycle through screens (alternative)
    - **Swipe Left/Right**: Adjust display brightness
    - Configure gesture actions in dedicated Gestures tab
    - Touch sensor support for compatible glasses

- **Enhanced Metrics**: Additional cycling data fields
    - VAM (Vertical Ascent Meters per hour)
    - Average VAM
    - Heart Rate Zones (Z1-Z5)
    - Power Zones (Z1-Z7)
    - Max metrics for HR, Power, Speed, Cadence
    - Average metrics for HR, Power, Speed, Cadence

### Changed

- Renamed "Builder" tab to "Datafields" for clarity
- Improved metric selection workflow with instant configuration
- Enhanced UI with better spacing and scrollable content
- Updated status display to show connection stages (Scanning, Found, Connecting, Connected)
- Moved Debug functions to modal dialog accessible from About tab
- Reorganized tabs: Status, Datafields, Gestures, About

### Fixed

- Metric selector now properly saves Gauge, Bar, and Zone Bar configurations
- Screen updates correctly after adding/editing metrics
- Profile selector properly displays and switches between profiles
- Zone bar calculations for HR and Power zones
- Bluetooth permission requests on app startup
- Connection stability improvements

### Technical

- Added comprehensive unit tests for layouts and visualizations
- Fixed R resource handling in unit tests
- Improved error handling and user feedback
- Enhanced data field registry with proper ID mappings

### Known Issues

- None

## [0.9] - 2025-12-28

### Added

- Added comprehensive runtime permission checking on app startup
    - Bluetooth permissions
    - Location permissions
    - Notification permissions
- User-friendly permission dialogs with clear explanations
    - Initial rationale dialog explaining why each permission is needed
    - Retry dialog if user denies permissions once
    - Settings redirect dialog for permanently denied permissions
- Android version-aware permission requests
- Graceful handling of permission denial scenarios
- Privacy-conscious messaging: explicitly states location data is not collected

#### Auto-Connect Functionality

- Fixed critical bug: Glasses were being discovered but never automatically connected
- Implemented auto-connect logic that automatically connects to the first discovered glasses
- Proper scan job tracking to prevent multiple concurrent scan operations
- 30-second scan timeout with automatic cleanup
- Improved connection reliability and speed

#### Multi-Stage Pairing Status Display

- Visual feedback for all pairing stages:
    - Stage 1: Scanning - Shows spinner while searching for glasses
    - Stage 2: Found - Shows checkmark when glasses are discovered
    - Stage 3: Connecting - Shows spinner during connection establishment
- Connected state displays glasses name with checkmark icon
- Disconnected state shows clear status with Connect glasses button
- Real-time device count display during scanning
- Color-coded status indicators (primary/success, secondary/in-progress, error/disconnected)

#### Forget Glasses Feature

- New Forget glasses button in Auto-Reconnect Settings
- Clears saved glasses address and connection history
- Disconnects if currently connected
- Button state shows whether glasses are saved
- Helpful description text explaining the action

### Fixed

- Fixed glasses discovery not triggering connection
- Fixed UI showing Connecting... indefinitely
- Resolved race conditions in scan job management
- Fixed stale connection state display

- Fixed glasses status display text: Changed from Glasses <Name>: to
  Glasses: <Name>
- Improved status text formatting and spacing
- Better visual hierarchy in status cards
- Proper icon sizing and alignment

## [0.8] - 2025-10-29

### Added

- Updated README.md
- New detailed INSTALLATION.md
- Cleaning up source folder

## [0.7] - 2025-10-27

### Added

- Check for updates directly from the About tab
- Auto-check for updates on app launch (once per day)
- Get notified when new versions are available
- One-tap download and install
- View release notes before updating

### Changed

- Enhanced About tab with update controls
- Added user preferences for auto-check
- Better user experience with clear update dialogs

### Fixed

- Fixed CI/CD pipeline reliability
- Resolved build configuration issues
- Improved overall stability

## [0.5] - 2025-10-25

### Added

- Updated tabs UX
- **Run as service**
- Configurable autostart
- *Optional* glasses disconnect on ride end

## [0.1] - 2025-01-23

### Added

- Initial release of K2Look
- ActiveLook smart glasses support for Karoo 2
- Real-time ride metrics display on glasses
- Auto-reconnect during active rides (configurable 1-60 min timeout)
- Debug mode with display simulator
- Status tab with connection management
- About tab with app info and help

### Features

- Connect ENGO 2, Julbo Evad-1, and other ActiveLook glasses
- Display speed, heart rate, cadence, power, distance, and time
- System back button integration
- Git tag-based versioning
- Scrollable tabs for all content

### Known Issues

- None

---

## How to Use This File

1. Add new versions at the top (newest first)
2. Use format: `## [VERSION] - YYYY-MM-DD`
3. Group changes under: Added, Changed, Fixed, Removed
4. This file is used for:
    - GitHub release notes (automatically)
    - App's About tab release notes (manually copy)
    - User documentation

## Version Format

- `[0.1]` - Initial release
- `[0.2]` - Minor updates
- `[1.0]` - First stable release
- `[1.1]` - Feature updates
- `[1.1.1]` - Bug fixes

