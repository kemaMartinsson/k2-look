# K2Look - Installation Guide

## Download

Go to the [Releases page](https://github.com/YOUR_USERNAME/k2-look/releases) and download the latest
`K2Look-X.X.apk` file.

## Installation on Karoo 2

### Method 1: ADB (Recommended)

1. Enable Developer Mode on your Karoo 2:
    - Go to Settings → About
    - Tap "Build number" 7 times
    - Enable USB Debugging in Developer Options

2. Connect Karoo to your computer via USB

3. Install the APK:

```bash
adb install K2Look-0.1.apk
```

### Method 2: File Transfer

1. Transfer the APK to your Karoo 2 via USB
2. Use a file manager on Karoo to locate and install the APK
3. You may need to enable "Install from Unknown Sources"

## First Launch

1. Open K2Look from your apps
2. Go to the **Status** tab
3. Tap **Connect glasses** to pair your ActiveLook smart glasses
4. Configure auto-reconnect timeout (default: 10 minutes)
5. Start riding! Your metrics will appear on your glasses

## Features

### Status Tab

- View connection status (Karoo service & glasses)
- Configure auto-reconnect settings
- See app version and build info

### Debug Tab

- Enable debug mode for logging
- Test glasses display with simulator
- View current values being sent to glasses

### About Tab

- Release notes
- Feature descriptions
- Help & support information

## Supported Glasses

K2Look works with ActiveLook compatible smart glasses:

- ENGO 2
- Julbo Evad-1
- And other ActiveLook devices

## Troubleshooting

**Glasses won't connect:**

- Make sure Bluetooth is enabled
- Check that glasses are charged
- Try restarting both Karoo and glasses

**Auto-reconnect not working:**

- Ensure you're in an active ride
- Check the timeout setting (Status tab)
- Enable debug mode to see logs

**Need help?**
Open an issue on [GitHub](https://github.com/YOUR_USERNAME/k2-look/issues)

## Uninstall

```bash
adb uninstall com.kema.k2look
```

Or use the Karoo's app management settings.

