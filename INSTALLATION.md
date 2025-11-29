# K2Look Installation Guide

## 📋 Table of Contents

- [What You Need](#what-you-need)
- [Installation Methods](#installation-methods)
    - [Method 1: USB + ADB (Required First Time)](#method-1-usb--adb-required-first-time)
        - [Install K2Look APK](#install-k2look-apk)
        - [Optional: Install CX File Explorer APK](#optional-install-cx-file-explorer-apk)
    - [Method 2: WiFi File Transfer (Optional, After CX File Explorer Installed)](#method-2-wifi-file-transfer-optional-after-cxfe-installed)
- [First Time Setup](#first-time-setup)
- [Troubleshooting](#troubleshooting)

---

## What You Need

- ✅ Hammerhead Karoo 2 device
- ✅ ActiveLook compatible smart glasses
- ✅ A computer (Windows or Mac) with USB cable (for FIRST install)
- ✅ K2Look APK file (download from Releases page) – e.g. `K2Look-v0.X.apk`
- ✅ (Optional) CX File Explorer APK (download from https://cxfileexplorer.com/ on your computer)
- ✅ (Optional after first install) WiFi network shared by Karoo 2 and your PC/phone for future
  manual transfers

> If your goal is simply to keep K2Look updated, you do NOT need CX File Explorer: K2Look’s in‑app
> update feature can fetch new versions directly once installed.

---

## Installation Methods

### Method 1: USB + ADB (Required First Time)

This method is mandatory for the initial installation because Karoo 2 lacks a browser to fetch APKs
directly.

#### 1. Prepare ADB

You'll need to install Android Debug Bridge (ADB) on your computer.

##### Windows Setup

1. **Download Platform Tools:**
    - Go to: https://developer.android.com/studio/releases/platform-tools
    - Click **"Download SDK Platform-Tools for Windows"**
    - Accept the terms and download the ZIP file

2. **Extract the Files:**
    - Right-click the downloaded ZIP file
    - Click **"Extract All..."**
    - Choose a location like `C:\adb`
    - Remember this location!

3. **Add ADB to System Path (Optional but recommended):**
    - Press **Windows key + R**
    - Type: `sysdm.cpl` and press Enter
    - Click the **"Advanced"** tab
    - Click **"Environment Variables"**
    - Under "System Variables", find **"Path"** and click **"Edit"**
    - Click **"New"**
    - Add: `C:\adb` (or wherever you extracted it)
    - Click **"OK"** on all windows

##### Mac Setup

1. **Install Homebrew** (if you don't have it):
    - Open **Terminal** (find it in Applications > Utilities)
    - Paste this command and press Enter:
      ```bash
      /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
      ```
    - Wait for it to complete (may take a few minutes)

2. **Install ADB:**
    - In Terminal, type:
      ```bash
      brew install android-platform-tools
      ```
    - Press Enter and wait for it to complete

#### Enable Developer Mode on Karoo 2

1. **On your Karoo 2:**
    - Go to **Settings**
    - Scroll down to **"About device"** or **"System"**
    - Find **"Build number"**
    - Tap it **7 times quickly**
    - You'll see a message saying "You are now a developer!"

2. **Enable USB Debugging:**
    - Go back to **Settings**
    - You should now see **"Developer options"**
    - Tap it
    - Find **"USB debugging"**
    - Turn it **ON**
    - If prompted, tap **"Allow"**

#### Connect Karoo 2 to Computer

1. **Connect USB Cable:**
    - Use a good quality USB cable
    - Connect your Karoo 2 to your computer
    - On the Karoo 2, you may see a notification about "USB for..."
    - Tap it and select **"File Transfer"** or **"MTP"**

2. **Authorize the Connection:**
    - A popup will appear on your Karoo 2 asking to "Allow USB debugging?"
    - Check **"Always allow from this computer"**
    - Tap **"Allow"** or **"OK"**

#### Install K2Look APK

Download the latest K2Look APK from the Releases page to your computer.

Windows (example):

```cmd
adb devices           # Verify Karoo 2 is listed, name should be KAROO followed by numbers
adb install -r "C:\Users\YourName\Downloads\K2Look-v0.X.apk"
```

Mac/Linux:

```bash
adb devices           # Verify Karoo 2 is listed, name should be KAROO followed by numbers
adb install -r ~/Downloads/K2Look-v0.X.apk
```

`Success` indicates installation complete.

#### Optional: Install CX File Explorer APK

If you want future manual WiFi transfers (without USB) or general file management:

1. On your COMPUTER (not Karoo), visit https://cxfileexplorer.com/ and download the APK.
2. Use ADB to install it the same way:
   Windows:
   ```cmd
   adb install -r "C:\Users\YourName\Downloads\CXFileExplorer.apk"
   ```
   Mac/Linux:
   ```bash
   adb install -r ~/Downloads/CXFileExplorer.apk
   ```
3. After installation you will see **CX File Explorer** in the Karoo apps list.

> You only need to do this once. Future side‑loads can be done over WiFi using CXFE if desired.

---

### Method 2: WiFi File Transfer (Optional, After CXFE Installed)

Use this for manual APK side‑loads AFTER you have already installed CX File Explorer via ADB. Not
required for K2Look updates if you rely on the in‑app updater.

Prerequisites:

- CX File Explorer already installed (from Method 1)
- K2Look already installed (from Method 1)
- Both Karoo 2 and your computer/phone on the same WiFi network

#### Step 1: Find Karoo 2 IP Address

On Karoo 2:

- Settings → WiFi → tap connected network → note IP (e.g. `192.168.1.100`).

#### Step 2: Transfer a New APK

Windows:

1. File Explorer → address bar: `\\192.168.1.100`
2. Drag & drop new APK into a folder (e.g. Downloads).

Mac:

1. Finder → Cmd+K → `smb://192.168.1.100`
2. Connect (use guest if prompted) → drag & drop APK.

Smartphone (optional):

- Use CX File Explorer’s Network feature to connect to the Karoo’s IP and copy the APK.

#### Step 3: Install the New APK

On Karoo 2:

1. Open CX File Explorer → locate APK → tap → Install.
2. Confirm update (the old version is replaced).

> For K2Look specifically, prefer the in‑app update checker (About tab) unless testing a pre‑release
> build.

---

## First Time Setup

After installing K2Look, here's how to set it up:

### 1. Launch K2Look

- On your Karoo 2, find the **"K2Look"** app
- Tap to open it

### 2. Grant Permissions

K2Look needs some permissions to work:

1. **Location Permission:**
    - Tap **"Allow"** when asked
    - This is required for Bluetooth scanning (Android requirement)

2. **Bluetooth Permission** (if asked):
    - Tap **"Allow"**
    - Needed to connect to your glasses

3. **Notification Permission** (Android 13+):
    - Tap **"Allow"** if prompted
    - This lets K2Look show connection status

### 3. Connect to Karoo System

1. In K2Look, tap the **"Connect"** button (top section)
2. Wait a moment - it should show **"Connected"** in green
3. If it doesn't connect:
    - Make sure you're using a Hammerhead Karoo 2 (not Karoo 1)
    - Try closing and reopening the app

### 4. Connect to Your ActiveLook Glasses

1. **Turn on your ActiveLook glasses**
    - Make sure they're charged
    - Turn them on and wait for them to be ready

2. **In K2Look:**
    - Tap **"Scan"** or **"Connect glasses"** button
    - You'll see a list of nearby Bluetooth devices
    - Look for your glasses (usually starts with "ActiveLook" or your glasses brand)
    - Tap on your glasses name

3. **Wait for Connection:**
    - You should see **"Connected"** status
    - Your glasses may show a confirmation message

### 5. Test It Out!

1. **Start a ride on your Karoo 2**
    - Begin pedaling or start a manual ride

2. **Check your glasses:**
    - You should see cycling data appear:
        - Speed (km/h)
        - Heart Rate (bpm) - if you have a heart rate sensor
        - Power (watts) - if you have a power meter
        - Cadence (rpm) - if you have a cadence sensor
        - Distance (km)
        - Time (HH:MM:SS)

3. **Data updates once per second**

**🎉 Congratulations!** K2Look is now set up and ready to use!

---

## Troubleshooting

### K2Look won't install

**"App not installed" error:**

- Make sure you've enabled "Unknown sources" or "Install from unknown sources"
- Go to: Settings > Security > Unknown sources (enable it)
- Try installing again

**"Parse error":**

- The APK file may be corrupted
- Download it again from the Releases page
- Make sure you downloaded the complete file

### Can't find my glasses when scanning

1. **Make sure your glasses are on:**
    - ActiveLook glasses need to be powered on
    - Check if they're charged

2. **Bluetooth is tricky:**
    - Turn Bluetooth off and on again on your Karoo
    - Turn your glasses off and on again
    - Move closer to your Karoo

3. **Check if glasses are connected elsewhere:**
    - Disconnect glasses from your phone if they're paired there
    - Only one device can connect at a time

### No data showing on glasses

1. **Check connections:**
    - Both "Karoo System" and "Glasses" should show "Connected" in K2Look

2. **Start a ride:**
    - K2Look only sends data during an active ride
    - Try starting a ride on your Karoo

3. **Restart everything:**
    - Close K2Look app completely
    - Turn glasses off and on
    - Open K2Look again and reconnect

### Connection keeps dropping

1. **Battery levels:**
    - Low battery on glasses can cause disconnections
    - Charge your glasses

2. **Distance:**
    - Keep your Karoo and glasses within reasonable Bluetooth range
    - Usually works fine when glasses are worn normally

3. **Interference:**
    - Other Bluetooth devices nearby can interfere
    - Try moving away from other devices

### ADB doesn't detect my Karoo

1. **Check USB cable:**
    - Use a good quality USB cable
    - Try a different cable or USB port

2. **USB Debugging:**
    - Make sure USB debugging is enabled on Karoo
    - Look for the authorization popup on your Karoo

3. **Drivers (Windows):**
    - You may need to install USB drivers
    - Download "Google USB Driver" from Android developer site

4. **Try different USB mode:**
    - On Karoo, tap the USB notification
    - Try switching between "File Transfer" and "PTP"

### WiFi file transfer isn't working

1. **Same network:**
    - Computer and Karoo must be on the **same WiFi network**
    - Not guest network vs main network

2. **IP address:**
    - Double-check the IP address on your Karoo
    - It might change if you disconnect/reconnect WiFi

3. **Firewall:**
    - Windows firewall might block the connection
    - Try temporarily disabling firewall to test

4. **File manager permissions:**
    - Make sure CX File Explorer has necessary permissions
    - Try restarting CX File Explorer

---

## Need More Help?

- **Report Issues:** [GitHub Issues](https://github.com/kemaMartinsson/k2-look/issues)
- **Check for Updates:** [Releases Page](https://github.com/kemaMartinsson/k2-look/releases)

---

**Happy cycling!** 🚴‍♂️👓
