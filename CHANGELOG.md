# Changelog

All notable changes to K2Look will be documented in this file.

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

