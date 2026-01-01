# Changelog

All notable changes to K2Look will be documented in this file.

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

