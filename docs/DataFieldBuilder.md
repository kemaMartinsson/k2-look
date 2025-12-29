# DataField Builder Guide

**K2Look Version 0.10+**

The DataField Builder (select tab **Datafields**) allows you to create custom display layouts for
your glasses using **6 professional templates**.  
Configure which metrics appear on your glasses, choose from multiple layout templates, and customize
how they're displayed.

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

### Step 1: Open the Datafields Tab

1. Launch K2Look on your Karoo 2
2. Tap the **Datafields** tab at the top

### Step 2: Create a New Profile

1. Tap the **⚙️ (gear)** icon next to "Active Profile"
2. Select **"Create New Profile"**
3. Enter a profile name (e.g., "XC Ride", "Gravel Bike", "XC Bike")
4. Tap **"Create"**

**💡 Tip:** Use the same name as your Karoo ride profile for automatic switching!

### Step 3: Choose a Layout Template

Each profile can use one of **6 professional layout templates**. Each template determines how many
data fields you can display and their positioning.

**Available Templates:**

1. **Single Data (1D)** - 1 large centered field
    - Best for: Focusing on one primary metric
    - Example: Power only, Speed only

2. **Two Data (2D)** - 2 full-width stacked fields
    - Best for: Two equally important metrics
    - Example: Speed + Heart Rate

3. **Triangle Layout (3D Triangle)** - 1 top + 2 bottom halves
    - Best for: One primary + two secondary metrics
    - Example: Speed (top), Power + HR (bottom)

4. **Three Rows (3D Full)** - 3 full-width rows ✅ **Default**
    - Best for: Balanced view of 3 metrics
    - Example: Speed, Power, Heart Rate

5. **Four Data (4D)** - 2 full + 2 half-width
    - Best for: Multiple important metrics
    - Example: Speed, HR, Power, Cadence

6. **Six Data (6D)** - 6 half-width fields (3×2 grid)
    - Best for: Maximum data density
    - Example: Speed, HR, Power, Cadence, Time, Distance

**To Change Template:**

1. In your profile screen, tap the **"Layout: [Current Template]"** button
2. The **Layout Template Selector** appears
3. Browse through all 6 templates (scroll down for more)
4. Tap on your desired template
5. Your existing metrics are preserved where possible

**💡 Tip:** Start with "Three Rows" (default) and adjust based on your needs!

---

## 📱 Configuring Metrics

### Understanding Zones

Each template divides the display into **zones**. Depending on your chosen template, you'll have
between 1-6 zones to configure.

**Zone Names Examples:**

- **Top**, **Middle**, **Bottom** (Three Rows template)
- **Top Center**, **Bottom Right**, **Bottom Left** (Triangle template)
- **Top Right**, **Middle Left**, etc. (Six Data template)

Each zone automatically optimizes:

- ✅ **Font size** - Sized appropriately for the zone
- ✅ **Position** - Precisely placed using ActiveLook coordinates
- ✅ **Layout** - Professionally arranged

### Adding a Metric to a Zone

1. Tap on an empty zone card (e.g., "Top - Tap to add field")
2. The **Metric Selector** appears
3. Choose a category (General, Heart Rate, Power, etc.)
4. Select your desired metric
5. The metric appears in the zone

### Configuring Metric Display

After adding a metric, tap the **✏️ (edit)** icon to configure:

### Configuring Metric Display

After adding a metric, tap the **✏️ (edit)** icon to configure:

#### 1. Display Options

**Show Label**

- ☑️ Display field name above value (e.g., "Heart Rate")
- ☐ Hide label (value only)

**Show Unit**

- ☑️ Display unit next to value (e.g., "145 bpm")
- ☐ Hide unit (value only)

#### 2. Icon Options

**Show Icon**

- ☑️ Display ActiveLook icon (28×28px) ✅ **Default**
- ☐ No icon

**Large Icon** (when icon enabled)

- ☐ Small icon (28×28px) ✅ **Default**
- ☑️ Large icon (40×40px) - More prominent

**Note:** Font size is **automatically optimized** based on the zone size. No manual selection
needed!

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

**Using "Three Rows" Template:**

**Top Zone - Speed:**

```
Metric: Speed
Show Label: ✅
Show Unit: ✅
Show Icon: ✅
Large Icon: ☐
Font: MEDIUM (automatic)
```

**Middle Zone - Heart Rate:**

```
Metric: Heart Rate
Show Label: ✅
Show Unit: ✅
Show Icon: ✅
Large Icon: ☐
Font: MEDIUM (automatic)
```

**Display Result on Glasses:**

```
🚴 Speed
   32.5 km/h

❤️ Heart Rate
   145 bpm

⚡ Power
   245 W
```

---

## 📐 Layout Template Reference

### Understanding Templates

Each template is designed by ActiveLook for optimal readability on AR glasses. Templates differ in:

- **Number of zones** (1-6 data fields)
- **Zone sizes** (full-width vs half-width)
- **Font sizes** (automatically optimized)
- **Layout arrangement** (stacked, grid, triangle)

### Template Visual Guide

#### 1D - Single Data

```
┌─────────────────────┐
│                     │
│     ╔═══════╗       │
│     ║  245  ║       │ (LARGE font)
│     ║   W   ║       │
│     ╚═══════╝       │
│                     │
└─────────────────────┘
Zones: 1 center field
Best for: Focus on single metric
```

#### 2D - Two Data

```
┌─────────────────────┐
│   ╔═══════════╗     │
│   ║   32.5    ║     │ (LARGE font)
│   ║   km/h    ║     │
│   ╚═══════════╝     │
│   ╔═══════════╗     │
│   ║    145    ║     │ (LARGE font)
│   ║    bpm    ║     │
│   ╚═══════════╝     │
└─────────────────────┘
Zones: 2 full-width
Best for: Two primary metrics
```

#### 3D Triangle - Triangle Layout

```
┌─────────────────────┐
│   ╔═══════════╗     │
│   ║   32.5    ║     │ (LARGE font)
│   ║   km/h    ║     │
│   ╚═══════════╝     │
│                     │
│  ╔═════╗  ╔═════╗   │
│  ║ 145 ║  ║ 245 ║   │ (MEDIUM font)
│  ║ bpm ║  ║  W  ║   │
│  ╚═════╝  ╚═════╝   │
└─────────────────────┘
Zones: 1 top + 2 bottom halves
Best for: One primary + two supporting
```

#### 3D Full - Three Rows ✅ Default

```
┌─────────────────────┐
│   ╔═══════════╗     │
│   ║   32.5    ║     │ (MEDIUM font)
│   ║   km/h    ║     │
│   ╚═══════════╝     │
│   ╔═══════════╗     │
│   ║    145    ║     │ (MEDIUM font)
│   ║    bpm    ║     │
│   ╚═══════════╝     │
│   ╔═══════════╗     │
│   ║    245    ║     │ (MEDIUM font)
│   ║     W     ║     │
│   ╚═══════════╝     │
└─────────────────────┘
Zones: 3 equal rows
Best for: Balanced view
```

#### 4D - Four Data

```
┌─────────────────────┐
│   ╔═══════════╗     │
│   ║   32.5    ║     │ (LARGE font)
│   ║   km/h    ║     │
│   ╚═══════════╝     │
│   ╔═══════════╗     │
│   ║    145    ║     │ (LARGE font)
│   ║    bpm    ║     │
│   ╚═══════════╝     │
│  ╔═════╗  ╔═════╗   │
│  ║ 245 ║  ║  89 ║   │ (MEDIUM font)
│  ║  W  ║  ║ rpm ║   │
│  ╚═════╝  ╚═════╝   │
└─────────────────────┘
Zones: 2 full + 2 half-width
Best for: Multiple key metrics
```

#### 6D - Six Data

```
┌─────────────────────┐
│  ╔═════╗  ╔═════╗   │
│  ║ 32  ║  ║ 145 ║   │ (MEDIUM font)
│  ║km/h ║  ║ bpm ║   │
│  ╚═════╝  ╚═════╝   │
│  ╔═════╗  ╔═════╗   │
│  ║ 245 ║  ║  89 ║   │ (MEDIUM font)
│  ║  W  ║  ║ rpm ║   │
│  ╚═════╝  ╚═════╝   │
│  ╔═════╗  ╔═════╗   │
│  ║1:45 ║  ║ 25  ║   │ (MEDIUM font)
│  ║     ║  ║ km  ║   │
│  ╚═════╝  ╚═════╝   │
└─────────────────────┘
Zones: 3×2 grid
Best for: Maximum data
```

### Template Selection Guide

**Choose based on your needs:**

| Template   | Fields | Use Case                     | Readability |
|------------|--------|------------------------------|-------------|
| 1D         | 1      | TT, focused intervals        | ⭐⭐⭐⭐⭐       |
| 2D         | 2      | Simple rides, two priorities | ⭐⭐⭐⭐        |
| Triangle   | 3      | One main + two support       | ⭐⭐⭐⭐        |
| Three Rows | 3      | Balanced general use         | ⭐⭐⭐⭐⭐       |
| Four Data  | 4      | Multiple key metrics         | ⭐⭐⭐         |
| Six Data   | 6      | Maximum information          | ⭐⭐          |

**💡 Tip:** More fields = smaller text. Start with Three Rows and adjust based on your preference.

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

### Road Bike Profile

**Template:** Four Data (4D)  
**Focus:** Speed, power, heart rate, and cadence

- **Top:** Power (large zone, auto-sized font, icon)
- **Second Row:** Heart Rate (large zone, auto-sized font, icon)
- **Bottom Right:** Speed (compact, auto-sized font, icon)
- **Bottom Left:** Cadence (compact, auto-sized font, icon)

### Climbing Profile

**Template:** Triangle Layout (3D Triangle)  
**Focus:** Climbing metrics with primary VAM display

- **Top Center:** VAM (large display, auto-sized font, icon)
- **Bottom Right:** Heart Rate (compact, auto-sized font, icon)
- **Bottom Left:** Power (compact, auto-sized font, icon)

### Endurance Profile

**Template:** Three Rows (3D Full) ✅ Default  
**Focus:** Balanced pacing metrics

- **Top:** Avg Speed (auto-sized font, icon)
- **Middle:** Heart Rate (auto-sized font, icon)
- **Bottom:** Elapsed Time (auto-sized font, icon)

### Training Profile

**Template:** Six Data (6D)  
**Focus:** Maximum data for intervals

- **Top Row:** HR Zone | Power 3s
- **Middle Row:** Avg Power | Speed
- **Bottom Row:** Cadence | Elapsed Time

### Minimalist Profile

**Template:** Single Data (1D)  
**Focus:** One primary metric only

- **Center:** Speed (extra large display, auto-sized font, icon)

---

## 📝 Profile Management

### Selecting a Profile

1. In Datafields tab, tap the **profile dropdown**
2. Select a profile from the list
3. Profile (with its template and metrics) is immediately applied to glasses (if connected)

### Editing a Profile

1. Select the profile you want to edit
2. **Change template** (optional): Tap "Layout: [Template Name]" button
3. **Configure zones**: Tap on any zone to add/edit metrics
4. Changes save automatically
5. Tap **"Apply to Glasses"** to update display

**Note:** When changing templates, existing metrics are preserved in compatible zones where
possible.

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

### Template Selection

- **Single Data (1D)** - Perfect for TT/time trials focusing on one metric (power or speed)
- **Two Data (2D)** - Great for simple rides with two priorities
- **Triangle (3D Triangle)** - Excellent for one main + two supporting metrics
- **Three Rows (3D Full)** - Best balanced option for most rides ✅ **Recommended**
- **Four Data (4D)** - Ideal when you need 4 key metrics
- **Six Data (6D)** - Maximum information for data-driven training

### Font Sizes (Automatic)

Font sizes are **automatically optimized** based on your template:

- **Large zones** get bigger fonts for better visibility
- **Small zones** get compact fonts to fit all information
- **No manual adjustment needed** - it just works! ✅

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

### Can't see all templates in selector

- ✅ **Scroll down** - The template selector shows 4 templates initially
- ✅ Look for **"↓ Scroll for more"** indicator at the bottom left
- ✅ Swipe up to see templates 5 and 6

### Template changed but metrics disappeared

- ✅ Some zones from your old template may not exist in the new template
- ✅ Metrics are preserved where zone types match
- ✅ You may need to **re-add metrics** to the new zones

### Profile doesn't auto-switch

- ✅ Check K2Look profile name **exactly matches** Karoo profile name
- ✅ Ensure you **start the ride** in Karoo (auto-switch happens on ride start)
- ✅ Check Datafields tab to see which profile is active

### Metrics show "--" or "N/A"

- `--` = No data yet (sensor connecting or no movement)
- `...` = Searching for sensor
- `N/A` = Sensor not available (e.g., no power meter)

### Changes don't appear on glasses

- ✅ Ensure glasses are **connected** (check Status tab)
- ✅ Tap **"Apply to Glasses"** after making changes
- ✅ Check if you're in **simulation mode** (disable for real ride)

### Text appears too small/large

- ✅ Font sizes are **automatic** based on template zones
- ✅ Try a **different template** if current one doesn't suit your needs
- ✅ **Three Rows** template offers good readability for most users

---

## 🚀 Quick Start

**5-Minute Setup:**

1. **Create your first profile:**
    - Datafields tab → ⚙️ → Create New Profile → "My Ride"

2. **Choose a template:**
    - Tap **"Layout: Three Rows"** button
    - Browse templates (scroll to see all 6)
    - Select **"Three Rows"** (or your preferred template)

3. **Configure zones:**
    - **Top zone:** Tap → Select "Speed"
    - **Middle zone:** Tap → Select "Heart Rate"
    - **Bottom zone:** Tap → Select "Power"

4. **Connect glasses:**
    - Status tab → "Scan for Glasses" → Select your glasses

5. **Test with simulation:**
    - Debug tab → Enable "Simulation Mode"
    - Check glasses display

6. **Go ride!**
    - Start a ride in Karoo
    - Your custom layout appears on glasses
    - Focus on the road, not your Karoo screen

**💡 Pro Tip:** Try different templates to find what works best for your riding style!

---

## 📚 Additional Resources

- **Installation Guide:** `INSTALLATION.md` (in release)
- **Changelog:** `CHANGELOG.md`
- **Troubleshooting:** README.md
- **GitHub Issues:** Report bugs or request features

---

## ✨ Summary

**DataField Builder gives you:**

- ✅ **6 professional layout templates** (1-6 data fields per screen)
- ✅ **22 real-time metrics** from Karoo
- ✅ **Visual template selector** with preview images
- ✅ **Automatic font sizing** optimized per zone
- ✅ **Automatic profile switching** based on Karoo profile
- ✅ **Flexible display options** (icons, labels, units)
- ✅ **Multiple profiles** for different bikes/rides
- ✅ **Live updates** at 1 update per second during rides

**Create your perfect heads-up display and keep your eyes on the road!** 🚴‍♂️👓

---

**Version:** 0.7+  
**Last Updated:** 2025-12-29  
**Compatible with:** Karoo 2 + ActiveLook Glasses

