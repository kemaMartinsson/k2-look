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

- ✅ ActiveLook compatible smart glasses
- ✅ A computer (Windows or Mac) with USB cable (for FIRST install)
- ✅ K2Look APK file (download
  from [https://github.com/kemaMartinsson/k2-look/releases](Releases page)) – e.g. `K2Look-vX.X.apk`
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
    - Keep them within a few meters of your Karoo 2

2. **In K2Look Status Tab:**
    - Tap **"Scan for Glasses"** button
    - You'll see the connection process in three stages:

   **Stage 1: Scanning**
    - K2Look is scanning for nearby Bluetooth devices
    - You'll see "1. Scanning for glasses..." with a spinner
    - This usually takes 2-5 seconds

   **Stage 2: Found**
    - When your glasses are detected, you'll see:
        - "✓ 1. Scanning" (completed)
        - "✓ 2. Found glasses" (completed)
        - "Found X device(s)" showing number of glasses detected
    - If multiple devices are found, select your glasses from the list

   **Stage 3: Connecting**
    - K2Look is establishing connection to your selected glasses
    - You'll see:
        - "✓ 1. Scanning" (completed)
        - "✓ 2. Found glasses" (completed)
        - "⟳ 3. Connecting..." (in progress)
    - This may take 5-10 seconds
    - Your glasses may show a confirmation message or flash

3. **Connection Complete:**
    - You should see **"Glasses: [Your Glasses Name]"** with a green checkmark
    - Both "Karoo service" and "Glasses" should show as "Connected"

**Troubleshooting Connection:**

- If scanning doesn't find your glasses:
    - Make sure glasses are powered on
    - Move glasses closer to the Karoo 2
    - Turn Bluetooth off and on in Karoo settings
    - Try restarting your glasses
- If connection fails at Stage 3:
    - Wait for timeout (about 15 seconds)
    - Try scanning again
    - Make sure glasses aren't connected to another device (phone, etc.)

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

**If Stage 1 (Scanning) doesn't complete:**

1. **Check Bluetooth permissions:**
    - Make sure K2Look has Bluetooth permissions
    - Go to Karoo Settings > Apps > K2Look > Permissions
    - Enable all required permissions

2. **Check Location permission:**
    - Bluetooth scanning on Android requires Location permission
    - Make sure Location is enabled in K2Look permissions

**If Stage 2 (Found) shows "Found 0 device(s)":**

1. **Make sure your glasses are on:**
    - ActiveLook glasses need to be powered on
    - Check if they're charged (charge if battery is low)
    - Some glasses show a light when powered on

2. **Distance matters:**
    - Keep glasses within 2-3 meters (6-10 feet) of your Karoo
    - Remove any obstacles between Karoo and glasses

3. **Check if glasses are connected elsewhere:**
    - Disconnect glasses from your phone if they're paired there
    - Only one device can connect to glasses at a time
    - Turn glasses off and on to reset connections

4. **Try Bluetooth reset:**
    - On Karoo: Settings > Bluetooth > Turn off, wait 5 seconds, turn on
    - Turn glasses off, wait 10 seconds, turn on
    - Try scanning again

**If Stage 3 (Connecting) fails or times out:**

1. **Glasses might be paired but not connected:**
    - Try the "Forget glasses" option in K2Look
    - Restart your glasses
    - Scan and connect again

2. **Bluetooth interference:**
    - Move away from other Bluetooth devices
    - WiFi routers can sometimes interfere
    - Try in a different location

3. **Factory reset glasses (last resort):**
    - Check your glasses manual for reset procedure
    - This will clear all pairing information
    - You'll need to re-pair with K2Look

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
