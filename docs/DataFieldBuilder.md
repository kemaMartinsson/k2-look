# DataField Builder Guide

**K2Look Version 1.0.12**

The DataField Builder (select tab *Fields**) allows you to create custom display layouts for
your glasses using **3 templates**.  
Configure which metrics appear on your glasses, choose from 3 layout templates, and customize how they're displayed.

---

## 📊 Available Metrics

K2Look supports **74 real-time metrics** from your Karoo 2:

### General (6 metrics)

- **Elapsed Time** - Ride duration (HH:MM:SS)
- **Ride Time** - Moving time only (HH:MM:SS)
- **Distance** - Total distance (km or miles)
- **Clock** - Current time (HH:MM)
- **Temperature** - Ambient temperature (°C/°F)
- **Karoo Battery** - Karoo 2 battery level (%)

### Heart Rate (6 metrics)

- **Heart Rate** - Current heart rate (bpm)
- **Max Heart Rate** - Maximum HR this ride (bpm)
- **Avg Heart Rate** - Average HR this ride (bpm)
- **HR Zone** - Current training zone (Z1-Z5)
- **% Max HR** - Percentage of max heart rate (%)
- **% HR Reserve** - Percentage of HR reserve (%)

### Power (13 metrics)

- **Power** - Current power output (W)
- **Max Power** - Maximum power this ride (W)
- **Avg Power** - Average power this ride (W)
- **Power 3s** - 3-second smoothed power (W)
- **Power 5s** - 5-second smoothed power (W)
- **Power 10s** - 10-second smoothed power (W)
- **Power 30s** - 30-second smoothed power (W)
- **Norm. Power** - Normalized power (W)
- **Power Zone** - Current power zone (Z1-Z7, based on FTP)
- **% FTP** - Percentage of Functional Threshold Power (%)
- **Int. Factor** - Intensity Factor
- **TSS** - Training Stress Score
- **W/kg** - Watts per kilogram (w/kg)

### Speed (4 metrics)

- **Speed** - Current speed (km/h or mph)
- **Max Speed** - Maximum speed this ride (km/h or mph)
- **Avg Speed** - Average speed this ride (km/h or mph)
- **Speed 3s** - 3-second smoothed speed (km/h or mph)

### Cadence (4 metrics)

- **Cadence** - Current pedaling cadence (rpm)
- **Max Cadence** - Maximum cadence this ride (rpm)
- **Avg Cadence** - Average cadence this ride (rpm)
- **Cadence 3s** - 3-second smoothed cadence (rpm)

### Climbing (2 metrics)

- **VAM** - Vertical Ascent Meters per hour (m/h)
- **Avg VAM** - Average VAM this ride (m/h)

### Elevation (5 metrics)

- **Grade** - Current road gradient (%)
- **Ascent** - Total elevation gained (m or ft)
- **Descent** - Total elevation lost (m or ft)
- **Altitude** - Current elevation (m or ft)
- **VAM 30s** - 30-second rolling VAM (m/h)

### Energy (3 metrics)

- **Energy** - Total energy output (kJ)
- **Calories** - Estimated calories burned (kcal)
- **Cal/hr** - Calorie burn rate (kcal/h)

### Lap (9 metrics)

- **Lap #** - Current lap number
- **Lap Time** - Elapsed time in current lap
- **Lap Dist** - Distance in current lap (km or miles)
- **Lap Speed** - Average speed in current lap (km/h or mph)
- **Lap HR** - Average heart rate in current lap (bpm)
- **Lap Power** - Average power in current lap (W)
- **Lap NP** - Normalized power in current lap (W)
- **Lap Cadence** - Average cadence in current lap (rpm)
- **Lap Ascent** - Elevation gained in current lap (m or ft)

### Last Lap (6 metrics)

- **L.Lap Time** - Elapsed time in last lap
- **L.Lap Dist** - Distance in last lap (km or miles)
- **L.Lap Speed** - Average speed in last lap (km/h or mph)
- **L.Lap HR** - Average heart rate in last lap (bpm)
- **L.Lap Power** - Average power in last lap (W)
- **L.Lap NP** - Normalized power in last lap (W)

### Radar (3 metrics) *(requires Garmin Varia or compatible)*

- **Radar Threat** - Active threat indicator
- **Radar Targets** - Number of approaching vehicles
- **Radar Range** - Distance to nearest threat (m)

### Shifting (4 metrics) *(requires AXS, eTap)* **Ki2 not supported**

- **Front Gear** - Current front chainring
- **Rear Gear** - Current rear sprocket
- **Drive Battery** - Electronic drivetrain battery (%)
- **Shift Count** - Total shifts this ride

### Navigation (5 metrics) *(requires active route)*

- **To Turn** - Distance to next turn (km or miles)
- **To Finish** - Distance to finish (km or miles)
- **ETA** - Estimated time of arrival
- **Time to End** - Time remaining to finish
- **Heading** - Current heading direction

### eBike (4 metrics) *(requires LEV/eBike sensor)*

- **Bike Battery** - eBike battery level (%)
- **Est. Range** - Estimated remaining range (km or miles)
- **Assist Mode** - Current assist mode
- **Motor Power** - Motor power output (W)

**Total: 74 metrics** across 14 categories — all update in real-time at 1 update per second during your ride.

**Note:** Units (metric/imperial) automatically match your Karoo profile settings.

---

## 🎨 Creating a Profile

### Step 1: Open the Fields Tab

1. Launch K2Look on your Karoo 2
2. Tap the **Fields** tab at the top

### Step 2: Create a New Profile

1. Tap the **⚙️ (gear)** icon next to "Active Profile"
2. Select **"Create New Profile"**
3. Enter a profile name (e.g., "Training", "Race", "Recovery")
4. Tap **"Create"**

**💡 Tip:** Use the same name as your Karoo ride profile for automatic switching!  
On ride start, K2Look will load the matching profile automatically.  
If no match, it uses the last selected profile.

### Step 3: Choose a Layout Template

Each profile can use one of **3 layout templates** (more planned). Each template determines how
many data fields you can display and their positioning.

**Available Templates:**

1. **Single Data (1D)** - 1 large centered field
    - Best for: Focusing on one primary metric
    - Example: Power only, Speed only

2. **Two Data (2D)** - 2 full-width stacked fields
    - Best for: Two equally important metrics
    - Example: Speed + Heart Rate

3. **Three Rows (3D Full)** - 3 full-width rows ✅ **Default**
    - Best for: Balanced view of 3 metrics
    - Example: Speed, Power, Heart Rate

> ⚙️ **Possible future templates:** Triangle (1 top + 2 half-width), Four Data (4D),
> Five Data (5D), Six Data (6D grid), and Gauge-style templates.

**To Change Template:**

1. In your profile screen, tap the **"Layout: [Current Template]"** button
2. The **Layout Template Selector** appears
3. Browse through all 6 templates (scroll down for more)
4. Tap on your desired template
5. Your existing metrics are preserved where possible

**💡 Tip:** Start with "Three Rows" (default) and adjust based on your needs!

---

## 📱 Multiple Screens

Each profile supports **multiple screens** that you can cycle through during a ride using a hand
gesture or touch button (see Gestures tab).

### Managing Screens

- **Add a screen**: Tap the **＋** icon in the tabs bar — a new screen is created with the default template.
- **Remove a screen**: Tap the **🗑** icon (only visible when there are 2 or more screens).
- **Switch screens**: Tap any **Screen N** tab to select it for editing.

### Per-Screen Configuration

Each screen is independent — you can use a different template and different metrics on every screen:

- **Screen 1** — e.g. Three Rows: Speed, HR, Power (general riding)
- **Screen 2** — e.g. Two Data: Avg Power + Elapsed Time (interval focus)
- **Screen 3** — e.g. Single Data: HR (recovery monitoring)

**During a ride**, use your configured gesture (wave) or touch (button tap) to cycle through screens.

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

#### 1. Select Metric

Tap the metric card to change which data field is displayed. Choose from any of the 74 available
metrics.

#### 2. Choose Visualization Style

K2Look supports **4 visualization styles** for displaying metrics:

##### 📝 **Text** (Default)

Traditional text display with optional label, unit, and icon.

**Supported Metrics:** ✅ ALL 74 metrics

**Display Options:**

- **Show Label** - Display field name above value (e.g., "Heart Rate")
- **Show Unit** - Display unit next to value (e.g., "145 bpm")
- **Show Icon** - Display ActiveLook icon (28×28px or 40×40px)
- **Large Icon** - Use 40×40px icon instead of 28×28px

**Font Size:** Font 2 (medium) by default. Enable **Large Font** to force Font 3 (large) for any individual field.

**Best For:** Maximum information density, traditional display

---

##### ⊙ **Gauge (coming feature)**

Circular progress gauge showing current value as a percentage of max range.

**Supported Metrics:**

- ✅ Heart Rate (40-200 bpm range)
- ✅ Power (0-400W range)
- ✅ Power 3s (0-400W range)
- ✅ Cadence (0-120 rpm range)
- ✅ Speed (0-60 km/h range)
- ✅ VAM (0-2000 m/h range)
- ✅ Max Heart Rate
- ✅ Avg Heart Rate
- ✅ Max Power
- ✅ Avg Power
- ✅ Max Speed
- ✅ Avg Speed
- ✅ Max Cadence
- ✅ Avg Cadence
- ✅ Avg VAM
- ✅ Distance (generic 0-100 range)
- ❌ Elapsed Time (not suitable)

**Visual Style:**

- Circular arc gauge (70px outer radius, 55px inner radius = 15px thick arc)
- **Arc span**: ~270° (3/4 circle) - from 3 o'clock to 12 o'clock position
- Fills clockwise as value increases
- Shows current numeric value in center

**Technical Details:**

- Uses ActiveLook's 16-portion circle system (each portion = 22.5°)
- Default: portions 3-14 (11 portions = 247.5° ≈ 270°)

**Best For:** Quick visual feedback, monitoring intensity zones

---

##### ▬ **Bar (coming feature)**

Horizontal or vertical progress bar showing current value as percentage of max range.

**Supported Metrics:** Same as Gauge (all except Elapsed Time)

**Visual Style:**

- Horizontal bar (244px × 20px)
- Fills left-to-right as value increases
- Optional border for clarity

**Best For:** Linear progress visualization, compact display

---

##### ▦ **Zoned Bar**

Multi-zone progress bar with color coding for training zones.

**Supported Metrics:**

- ✅ **Heart Rate** - 5 HR zones (Z1-Z5) based on Karoo HR zone settings
- ✅ **HR Zone** - Displays current HR zone
- ✅ **Power** - 7 power zones (Z1-Z7) based on FTP (Functional Threshold Power)
- ✅ **Power Zone** - Displays current power zone
- ❌ All other metrics (no zone definitions)

**Visual Style:**

- Multi-segment bar with zone boundaries
- Color changes based on current zone
- Shows both numeric value and zone indicator

**HR Zones (5 zones):**

- Z1: Recovery (50-60% max HR)
- Z2: Endurance (60-70% max HR)
- Z3: Tempo (70-80% max HR)
- Z4: Threshold (80-90% max HR)
- Z5: Maximum (90-100% max HR)

**Power Zones (7 zones based on FTP):**

- Z1: Active Recovery (0-55% FTP)
- Z2: Endurance (55-75% FTP)
- Z3: Tempo (75-90% FTP)
- Z4: Lactate Threshold (90-105% FTP)
- Z5: VO2 Max (105-120% FTP)
- Z6: Anaerobic Capacity (120-150% FTP)
- Z7: Neuromuscular Power (150%+ FTP)

**Note:** Zone boundaries are automatically adjusted based on your Karoo profile settings (Max HR
and FTP).

**Best For:** Training zone monitoring, structured workouts, FTP-based training plans

---

#### 3. Display Options (TEXT Style Only)

When using **Text** visualization, customize the display:

**Show Label**

- ☑️ Display field name above value (e.g., "Heart Rate")
- ☐ Hide label (value only)

**Show Unit**

- ☑️ Display unit next to value (e.g., "145 bpm")
- ☐ Hide unit (value only)

**Show Icon**

- ☑️ Display ActiveLook icon
- ☐ No icon

**Large Icon** (when icon enabled)

- ☐ Small icon (28×28px) ✅ **Default**
- ☑️ Large icon (40×40px) - More prominent

**Large Font**

- ☐ Font 2 (medium) ✅ **Default** — standard readable size
- ☑️ Font 3 (large) — bigger digits, useful when focusing on a single key metric

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

### Template Selection Guide

**Choose based on your needs:**

| Template   | Fields | Use Case                     | Readability |
|------------|--------|------------------------------|-------------|
| 1D         | 1      | TT, focused intervals        | ⭐⭐⭐⭐⭐       |
| 2D         | 2      | Simple rides, two priorities | ⭐⭐⭐⭐        |
| Three Rows | 3      | Balanced general use         | ⭐⭐⭐⭐⭐       |

---

## 🔄 Automatic Profile Switching

K2Look can automatically switch profiles when you start a ride or mid ride, matching your Karoo profile name.

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

✅ **Auto-switch on startup** — if Karoo already has an active profile when K2Look opens  
✅ **Auto-switch on profile change** — before and during a ride  
✅ **Case-insensitive matching** ("XC Bike" = "XC bike")  
✅ **Manual override** - You can always manually select a different profile in the Fields tab

---

## 📝 Profile Management

### Selecting a Profile

1. In Fields tab, tap the **profile dropdown**
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

## ⚙️ Global Settings

Two global toggles are available at the top of the **Fields** tab. They apply across all profiles and screens.

### ⚠ Radar Warning

- **Default:** On
- When enabled, K2Look overlays a warning icon on the glasses display whenever a radar threat is detected (e.g. Garmin Varia approaching vehicle).
- The icon is rendered outside the data layout zones and does not displace any metric.
- Threat levels 1–4 are supported; the icon style changes with severity.
- If no radar device is connected nothing is displayed.

### 🔋 Glasses Battery

- **Default:** On
- When enabled, the current glasses battery level is shown as a small percentage overlay in the top-left corner of the display.
- Updates automatically during rides and whenever the battery level changes.
- The icon changes style when battery drops below 10%.
- Turn off if you prefer an uncluttered display and do not need battery monitoring.

---

## 💡 Tips & Best Practices

### Debug a layout

- Use **Simulation Mode** in the Debug tab to see your layout on glasses without starting a ride  
  Toggle debug mode and start **Simulation mode** to see live updates on glasses as you edit your profile.
- Test different templates and visualization styles to find what works best for you

### Template Selection

- **Single Data (1D)** - Perfect for TT/time trials focusing on one metric (power or speed)
- **Two Data (2D)** - Great for simple rides with two priorities
- **Three Rows (3D Full)** - Best balanced option for most rides ✅ **Recommended**

### Font Sizes

Font sizes default to **Font 2 (medium)** for all fields. You can override this per field:

- **Default (Font 2)** — balanced readability, fits icons and units comfortably
- **Large Font (Font 3)** — bigger digits for a key metric you want to read at a glance

Enable **Large Font** in the field's edit dialog (✏️). Works with TEXT visualization only.

### Visualization Style Selection

Choose the right visualization style for each metric:

**📝 Use TEXT when:**

- ✅ You want maximum information (label + value + unit + icon)
- ✅ Displaying time, distance, or non-range metrics
- ✅ You prefer traditional numeric display
- ✅ Multiple metrics in small zones (compact)

**⊙ Use GAUGE (coming soon) when:**

- ✅ Monitoring intensity (Heart Rate, Power)
- ✅ You want quick visual feedback without reading numbers
- ✅ Staying in target zones is important
- ✅ Single focus metric (larger zones work best)

**▬ Use BAR (coming soon) when:**

- ✅ Linear progress visualization is intuitive for the metric
- ✅ Compact display needed (bars use less space than gauges)
- ✅ Multiple bars stacked to compare metrics

**▦ Use ZONED BAR when:**

- ✅ Training in specific HR zones (Z1-Z5)
- ✅ Following structured workout plans
- ✅ Zone boundaries are more important than exact values

**💡 Pro Tip:** Mix styles! Use Gauge for Heart Rate (zone awareness), Text for Speed (precision),
and Bar for Power (progress).

### Icon Usage

- **Icons improve recognition** at a glance
- **Small icons (28×28)** - Good for all metrics ✅
- **Large icons (40×40)** - Use for main focus metric

### Unit Display

- **Hide units** - When metric is obvious (bpm for HR)
- **Note:** Labels/units only configurable with TEXT visualization

### Profile Organization

- **Match Karoo names** for automatic switching
- **Create profiles for different needs** (Training, Race, Recovery, Long Ride)
- **Easy to reconfigure** - Takes just seconds to change metrics and layouts
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

- ✅ Check K2Look profile name **exactly matches** Karoo profile name (case-insensitive)
- ✅ Ensure **Karoo Sync** is enabled in the Profiles screen
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

3. **Configure zones with different visualization styles:**
    - **Top zone:** Tap → Select "Speed" → Visualization: **📝 Text** (traditional)
    - **Middle zone:** Tap → Select "Heart Rate" → Visualization: **⊙ Gauge** (visual feedback)
    - **Bottom zone:** Tap → Select "Power" → Visualization: **▬ Bar** (progress)

4. **Connect glasses:**
    - Status tab → "Scan for Glasses" → Select your glasses

5. **Test with simulation:**
    - Debug tab → Enable "Simulation Mode"
    - Check glasses display
    - See gauges and bars animate with simulated data

6. **Go ride!**
    - Start a ride in Karoo
    - Your custom layout appears on glasses
    - Focus on the road, not your Karoo screen

---

## 📊 Visualization Style Quick Reference

| Style         | Symbol | Best For             | Metrics Supported                      | Key Features                            |
|---------------|--------|----------------------|----------------------------------------|-----------------------------------------|
| **Text**      | 📝     | All-purpose display  | ✅ All 74 metrics                                | Label, unit, icon, precise values       |
| **Gauge (coming soon)**     | ⊙      | Intensity monitoring | ✅ Numeric metrics with a defined range           | Circular arc, visual zones, at-a-glance |
| **Bar (coming soon)**       | ▬      | Progress tracking    | ✅ Numeric metrics with a defined range           | Linear fill, compact, stackable         |
| **Zoned Bar** | ▦      | Training zones       | ✅ HR & Power zones (4 metrics)                  | Color-coded zones, workout guidance     |

### Metric Support Matrix

| Metric | TEXT | GAUGE *(planned)* | ZONED BAR |
|--------|------|-------------------|-----------|
| **General** | | | |
| Elapsed Time | ✅ | ❌ | ❌ |
| Ride Time | ✅ | ❌ | ❌ |
| Distance | ✅ | 🔜 | ❌ |
| Clock | ✅ | ❌ | ❌ |
| Temperature | ✅ | 🔜 | ❌ |
| Karoo Battery | ✅ | 🔜 | ❌ |
| **Heart Rate** | | | |
| Heart Rate | ✅ | 🔜 | ✅ |
| Max HR | ✅ | 🔜 | ❌ |
| Avg HR | ✅ | 🔜 | ❌ |
| HR Zone | ✅ | ❌ | ✅ |
| % Max HR | ✅ | 🔜 | ❌ |
| % HR Reserve | ✅ | 🔜 | ❌ |
| **Power** | | | |
| Power | ✅ | 🔜 | ❌ |
| Max Power | ✅ | 🔜 | ❌ |
| Avg Power | ✅ | 🔜 | ❌ |
| Power 3s | ✅ | 🔜 | ❌ |
| Power 5s | ✅ | 🔜 | ❌ |
| Power 10s | ✅ | 🔜 | ❌ |
| Power 30s | ✅ | 🔜 | ❌ |
| Norm. Power | ✅ | 🔜 | ❌ |
| Power Zone | ✅ | ❌ | ✅ |
| % FTP | ✅ | 🔜 | ❌ |
| Int. Factor | ✅ | 🔜 | ❌ |
| TSS | ✅ | 🔜 | ❌ |
| W/kg | ✅ | 🔜 | ❌ |
| **Speed** | | | |
| Speed | ✅ | 🔜 | ❌ |
| Max Speed | ✅ | 🔜 | ❌ |
| Avg Speed | ✅ | 🔜 | ❌ |
| Speed 3s | ✅ | 🔜 | ❌ |
| **Cadence** | | | |
| Cadence | ✅ | 🔜 | ❌ |
| Max Cadence | ✅ | 🔜 | ❌ |
| Avg Cadence | ✅ | 🔜 | ❌ |
| Cadence 3s | ✅ | 🔜 | ❌ |
| **Climbing** | | | |
| VAM | ✅ | 🔜 | ❌ |
| Avg VAM | ✅ | 🔜 | ❌ |
| **Elevation** | | | |
| Grade | ✅ | 🔜 | ❌ |
| Ascent | ✅ | 🔜 | ❌ |
| Descent | ✅ | 🔜 | ❌ |
| Altitude | ✅ | 🔜 | ❌ |
| VAM 30s | ✅ | 🔜 | ❌ |
| **Energy** | | | |
| Energy | ✅ | 🔜 | ❌ |
| Calories | ✅ | 🔜 | ❌ |
| Cal/hr | ✅ | 🔜 | ❌ |
| **Lap** | | | |
| Lap # | ✅ | ❌ | ❌ |
| Lap Time | ✅ | ❌ | ❌ |
| Lap Dist | ✅ | 🔜 | ❌ |
| Lap Speed | ✅ | 🔜 | ❌ |
| Lap HR | ✅ | 🔜 | ❌ |
| Lap Power | ✅ | 🔜 | ❌ |
| Lap NP | ✅ | 🔜 | ❌ |
| Lap Cadence | ✅ | 🔜 | ❌ |
| Lap Ascent | ✅ | 🔜 | ❌ |
| **Last Lap** | | | |
| L.Lap Time | ✅ | ❌ | ❌ |
| L.Lap Dist | ✅ | 🔜 | ❌ |
| L.Lap Speed | ✅ | 🔜 | ❌ |
| L.Lap HR | ✅ | 🔜 | ❌ |
| L.Lap Power | ✅ | 🔜 | ❌ |
| L.Lap NP | ✅ | 🔜 | ❌ |
| **Radar** *(requires Garmin Varia or compatible)* | | | |
| Radar Threat | ✅ | ❌ | ❌ |
| Radar Targets | ✅ | ❌ | ❌ |
| Radar Range | ✅ | 🔜 | ❌ |
| **Shifting** *(requires AXS / eTap / Ki2)* | | | |
| Front Gear | ✅ | ❌ | ❌ |
| Rear Gear | ✅ | ❌ | ❌ |
| Drive Battery | ✅ | 🔜 | ❌ |
| Shift Count | ✅ | 🔜 | ❌ |
| **Navigation** *(requires active route)* | | | |
| To Turn | ✅ | 🔜 | ❌ |
| To Finish | ✅ | 🔜 | ❌ |
| ETA | ✅ | ❌ | ❌ |
| Time to End | ✅ | ❌ | ❌ |
| Heading | ✅ | ❌ | ❌ |
| **eBike** *(requires LEV/eBike sensor)* | | | |
| Bike Battery | ✅ | 🔜 | ❌ |
| Est. Range | ✅ | 🔜 | ❌ |
| Assist Mode | ✅ | ❌ | ❌ |
| Motor Power | ✅ | 🔜 | ❌ |

**Legend:**

- ✅ Fully supported
- 🔜 Planned (not yet available in UI)
- ❌ Not applicable

---

## 📚 Additional Resources

- **Installation Guide:** `INSTALLATION.md` (in release)
- **Changelog:** `CHANGELOG.md`
- **Troubleshooting:** README.md
- **GitHub Issues:** Report bugs or request features

---

## ✨ Summary

**DataField Builder gives you:**

- ✅ **3 layout templates** (1-3 data fields per screen) + more planned
- ✅ **74 real-time metrics** from Karoo across 14 categories
- ✅ **Visual template selector** with preview images
- ✅ **Automatic font sizing** optimized per zone
- ✅ **Automatic profile switching** based on Karoo profile
- ✅ **Flexible display options** (icons, labels, units, large font per field)
- ✅ **Global radar warning overlay** — auto-triggered by Garmin Varia and compatible sensors
- ✅ **Glasses battery overlay** — always-on percentage indicator with low-battery icon
- ✅ **Multiple profiles** for different bikes/rides
- ✅ **Live updates** at 1 update per second during rides

**Create your perfect heads-up display and keep your eyes on the road!** 🚴‍♂️👓

---

**Version:** 0.10+  
**Last Updated:** 2026-04-19  
**Compatible with:** Karoo 2 + ActiveLook Glasses

