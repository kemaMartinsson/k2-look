# DataField Builder Guide

**K2Look Version 0.6+**

The DataField Builder (select tab Builder) allows you to create custom display layouts for your
glasses.  
Configure which metrics appear on your glasses and how they're displayed.

---

## 📊 Available Metrics

K2Look supports **22 real-time metrics** from your Karoo 2:

### General (2 metrics)

- **Elapsed Time** - Ride duration (HH:MM:SS)
- **Distance** - Total distance (km or miles)

### Heart Rate (4 metrics)

- **Heart Rate** - Current heart rate (bpm)
- **Max Heart Rate** - Maximum HR this ride (bpm)
- **Avg Heart Rate** - Average HR this ride (bpm)
- **HR Zone** - Current training zone (Z1-Z5)

### Power (4 metrics)

- **Power** - Current power output (watts)
- **Max Power** - Maximum power this ride (watts)
- **Avg Power** - Average power this ride (watts)
- **Power 3s** - 3-second smoothed power (watts)

### Speed (3 metrics)

- **Speed** - Current speed (km/h or mph)
- **Max Speed** - Maximum speed this ride (km/h or mph)
- **Avg Speed** - Average speed this ride (km/h or mph)

### Cadence (3 metrics)

- **Cadence** - Current pedaling cadence (rpm)
- **Max Cadence** - Maximum cadence this ride (rpm)
- **Avg Cadence** - Average cadence this ride (rpm)

### Climbing (2 metrics)

- **VAM** - Vertical Ascent Meters per hour (m/h)
- **Avg VAM** - Average VAM this ride (m/h)

**Total: 22 metrics** - All update in real-time at 1 update per second during your ride.

**Note:** Units (metric/imperial) automatically match your Karoo profile settings.

---

## 🎨 Creating a Profile

### Step 1: Open the Builder Tab

1. Launch K2Look on your Karoo 2
2. Tap the **Builder** tab at the top

### Step 2: Create a New Profile

1. Tap the **⚙️ (gear)** icon next to "Active Profile"
2. Select **"Create New Profile"**
3. Enter a profile name (e.g., "XC Ride", "Gravel Bike", "XC Bike")
4. Tap **"Create"**

**💡 Tip:** Use the same name as your Karoo ride profile for automatic switching!

### Step 3: Configure Your Screen

Each profile has a screen divided into **3 positions**:

- **TOP** - Upper third of display
- **MIDDLE** - Middle third of display
- **BOTTOM** - Lower third of display

---

## 📱 Configuring Metrics

### Adding a Metric

1. Tap on a position card (TOP, MIDDLE, or BOTTOM)
2. The **Metric Configuration** dialog appears

### Metric Configuration Options

#### 1. Select Metric

- Tap **"Select Metric"**
- Choose a category (General, Heart Rate, Power, etc.)
- Select your desired metric

#### 2. Font Size

Choose text size for the metric value:

- **SMALL** (24px / Font 1) - Compact
- **MEDIUM** (35px / Font 2) - Balanced ✅ **Default**
- **LARGE** (49px / Font 3) - Maximum visibility

#### 3. Display Options

**Show Label**

- ☑️ Display field name above value (e.g., "Heart Rate")
- ☐ Hide label (value only)

**Show Unit**

- ☑️ Display unit next to value (e.g., "145 bpm")
- ☐ Hide unit (value only)

#### 4. Icon Options

**Show Icon**

- ☑️ Display ActiveLook icon
- ☐ No icon

**Large Icon** (when icon enabled)

- ☐ Small icon (28×28px) ✅ **Default**
- ☑️ Large icon (40×40px) - More prominent

### Example Configuration

**TOP Position - Heart Rate:**

```
Metric: Heart Rate
Font Size: MEDIUM
Show Label: ✅
Show Unit: ✅
Show Icon: ✅
Large Icon: ☐
```

**Display Result:**

```
❤️ Heart Rate
   145 bpm
```

---

## 🔄 Automatic Profile Switching

K2Look can automatically switch profiles when you start a ride, matching your Karoo profile name.

### How It Works

1. **Create a K2Look profile** with the **same name** as your Karoo ride profile
2. **Start a ride** in Karoo using that profile
3. **K2Look automatically switches** to the matching profile
4. **Your custom layout appears** on your glasses

### Example Setup

**Karoo Profiles:**

- "XC Bike"
- "Gravel Bike"

**K2Look Profiles:** (create matching names)

- "XC Bike" → Speed, Power, HR
- "Gravel Bike" → Speed, HR, Cadence

**Result:** When you select "XC Bike" in Karoo and start riding, K2Look automatically loads your "
Road Bike" profile with Speed/Power/HR displayed on your glasses!

### Important Notes

✅ **Auto-switch happens once**, at ride start  
✅ **Case-insensitive matching** ("XC Bike" = "XC bike")  
✅ **No mid-ride switching** - stays on selected profile if you change Karoo profile during ride  
✅ **Manual override** - You can always manually select a different profile in Builder tab

---

## 🎯 Example Profiles

### XC Bike Profile

**Focus:** Power, speed, and heart rate

- **TOP:** Power (LARGE font, icon)
- **MIDDLE:** Speed (MEDIUM font, icon)
- **BOTTOM:** Heart Rate (MEDIUM font, icon)

### Climbing Profile

**Focus:** Climbing metrics and effort

- **TOP:** VAM (LARGE font, icon)
- **MIDDLE:** Heart Rate + Zone (MEDIUM font, icon)
- **BOTTOM:** Power (MEDIUM font, icon)

### Endurance Profile

**Focus:** Pacing and time

- **TOP:** Avg Speed (LARGE font, icon)
- **MIDDLE:** Heart Rate (MEDIUM font, icon)
- **BOTTOM:** Elapsed Time (MEDIUM font, icon)

### Training Profile

**Focus:** Training zones and power

- **TOP:** HR Zone (LARGE font, no icon) - "Z3"
- **MIDDLE:** Power 3s (LARGE font, icon)
- **BOTTOM:** Avg Power (MEDIUM font, icon)

---

## 📝 Profile Management

### Selecting a Profile

1. In Builder tab, tap the **profile dropdown**
2. Select a profile from the list
3. Profile is immediately applied to glasses (if connected)

### Editing a Profile

1. Select the profile you want to edit
2. Tap on any position (TOP/MIDDLE/BOTTOM) to reconfigure
3. Changes save automatically
4. Tap **"Apply to Glasses"** to update display

### Duplicating a Profile

1. Tap the **⚙️ (gear)** icon
2. Select **"Duplicate Profile"**
3. Choose which profile to duplicate
4. Enter a new name
5. Modify as needed

### Deleting a Profile

1. Tap the **⚙️ (gear)** icon
2. Select **"Delete Profile"**
3. Choose profile to delete
4. Confirm deletion

**Note:** The "Default" profile cannot be deleted.

---

## 💡 Tips & Best Practices

### Font Size Selection

- **SMALL** - Use when showing 3+ short values
- **MEDIUM** - Best balance for most metrics ✅
- **LARGE** - Use for primary metric you glance at most

### Icon Usage

- **Icons improve recognition** at a glance
- **Small icons (28×28)** - Good for all metrics ✅
- **Large icons (40×40)** - Use for main focus metric

### Label & Unit Display

- **Show both** - Best clarity for beginners ✅
- **Hide labels** - More space for larger values
- **Hide units** - When metric is obvious (bpm for HR)

### Profile Organization

- **Match Karoo names** for automatic switching
- **Create bike-specific profiles** (Road, MTB, Gravel)
- **Create activity profiles** (Training, Race, Recovery)
- **Test with simulation mode** before real rides

---

## 🔧 Troubleshooting

### Profile doesn't auto-switch

- ✅ Check K2Look profile name **exactly matches** Karoo profile name
- ✅ Ensure you **start the ride** in Karoo (auto-switch happens on ride start)
- ✅ Check Builder tab to see which profile is active

### Metrics show "--" or "N/A"

- `--` = No data yet (sensor connecting or no movement)
- `...` = Searching for sensor
- `N/A` = Sensor not available (e.g., no power meter)

### Changes don't appear on glasses

- ✅ Ensure glasses are **connected** (check Status tab)
- ✅ Tap **"Apply to Glasses"** after making changes
- ✅ Check if you're in **simulation mode** (disable for real ride)

### Large values get cut off

- ✅ Reduce **font size** (LARGE → MEDIUM)
- ✅ Disable **"Show Label"** for more space
- ✅ Use **abbreviations** where possible

---

## 🚀 Quick Start

**5-Minute Setup:**

1. **Create your first profile:**
    - Builder tab → ⚙️ → Create New Profile → "My Ride"

2. **Configure 3 metrics:**
    - TOP: Speed (MEDIUM font, icon)
    - MIDDLE: Heart Rate (MEDIUM font, icon)
    - BOTTOM: Power (MEDIUM font, icon)

3. **Connect glasses:**
    - Status tab → "Scan for Glasses" → Select your glasses

4. **Test with simulation:**
    - Debug tab → Enable "Simulation Mode"
    - Check glasses display

5. **Go ride!**
    - Start a ride in Karoo
    - Your metrics appear on glasses
    - Focus on the road, not your Karoo screen

---

## 📚 Additional Resources

- **Installation Guide:** `INSTALLATION.md` (in release)
- **Changelog:** `CHANGELOG.md`
- **Troubleshooting:** README.md
- **GitHub Issues:** Report bugs or request features

---

## ✨ Summary

**DataField Builder gives you:**

- ✅ **22 real-time metrics** from Karoo
- ✅ **Custom layouts** with 3 configurable positions
- ✅ **Automatic profile switching** based on Karoo profile
- ✅ **Flexible display options** (fonts, icons, labels)
- ✅ **Multiple profiles** for different bikes/rides
- ✅ **Live updates** at 1 update per second during rides

**Create your perfect heads-up display and keep your eyes on the road!** 🚴‍♂️👓

---

**Version:** 0.10+  
**Last Updated:** 2025-12-28  
**Compatible with:** Karoo 2 + ActiveLook Glasses

