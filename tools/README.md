# tools/

Desktop utilities for K2Look development.

---

## preview_layout.py — ActiveLook Layout Previewer

Renders a 304×256 ActiveLook display layout to a PNG without needing physical  
glasses. Replaces the 50-second build → install → glasses cycle for coordinate  
calibration.

### Requirements

Python 3.x with [Pillow](https://pillow.readthedocs.io/):

```powershell
pip install Pillow
```

No other dependencies. Uses fonts and icons already present in the repo:
- **Font**: `docs/Activelook-Visual-Assets/fonts/Source_Sans_Pro/SSP-SemiBold-Spacing.otf`
- **Icons**: `docs/Activelook-Visual-Assets/images/` (PNG files named `{id}_name_WxH.png`)

### Usage

Run from the repo root (`C:\Project\k2-look`):

```powershell
# Built-in Test 9 scenario: speed / power / heart-rate (opens PNG automatically)
python tools/preview_layout.py

# Viewer perspective — both axes flipped, matches what you see on the lens
python tools/preview_layout.py --viewer

# Load a custom layout JSON
python tools/preview_layout.py --config tools/test9.json

# Custom output path, suppress auto-open
python tools/preview_layout.py --output C:\Tmp\test.png --no-open

# Hide zone boundary outlines
python tools/preview_layout.py --no-zones
```

All options:

| Flag | Default | Description |
|------|---------|-------------|
| `--config PATH` | built-in Test 9 | JSON layout file (see format below) |
| `--output PATH` | `tools/preview/preview.png` | Output PNG path |
| `--viewer` | off | Flip both axes to simulate lens perspective |
| `--no-zones` | zones on | Hide zone boundary outlines |
| `--no-open` | auto-open on | Don't open the PNG after saving |

Output is written to `tools/preview/` by default, which is gitignored.

---

### JSON Layout Format

`tools/test9.json` is the reference file. Each layout object maps directly to  
a `layoutSave` + `layoutClearAndDisplayExtended` call:

```json
[
  {
    "id": 100,
    "x0": 30, "y0": 153, "width": 250, "height": 40,
    "font": 1,
    "txt_x": 200, "txt_y": 22,
    "value": "25.1",
    "extra_cmds": [
      { "type": "bitmap", "icon_id": 26, "x": 235, "y": 0 },
      { "type": "font",   "font_id": 1 },
      { "type": "text",   "x": 10, "y": 22, "text": "km/h" }
    ]
  }
]
```

**Fields:**

| Field | Description |
|-------|-------------|
| `x0`, `y0` | Zone top-left in display coordinates (origin = top-left of 304×256 display) |
| `width`, `height` | Zone pixel size |
| `font` | Main font ID: 1=24px, 2=38px, 3=64px, 4=75px, 5=82px |
| `txt_x`, `txt_y` | Value text position relative to zone origin — **right-baseline anchor** |
| `value` | Value string to render (e.g. `"25.1"`, `"150"`) |
| `extra_cmds` | Sub-commands applied after value text (icons + unit labels) |

**ExtraCmd types:**

| `"type"` | Fields | Description |
|----------|--------|-------------|
| `"bitmap"` | `icon_id`, `x`, `y` | Paste icon at `(x0+x, y0+y)`. Icon ID matches filename prefix in `images/`. |
| `"font"` | `font_id` | Switch the active font for subsequent `"text"` commands. |
| `"text"` | `x`, `y`, `text` | Draw label at `(x0+x, y0+y)` — **left-baseline anchor** (flows rightward). |

All `x`/`y` in `extra_cmds` are **relative to the zone's `x0`/`y0`**.

---

### Coordinate System

```
(0,0) ─────────────────────────── (303,0)
  │    display coordinates          │
  │    X: left→right (0–303)        │
  │    Y: top→bottom (0–255)        │
  │                                 │
(0,255) ──────────────────────── (303,255)
```

The lens **mirrors the image on both axes** to the viewer:
- viewer-LEFT  = high display-x (x ≈ 303)
- viewer-RIGHT = low  display-x (x ≈ 0)
- viewer-TOP   = high zone y0   (y ≈ 153)

Pass `--viewer` to flip the output to the viewer perspective.

---

### Calibration Workflow

1. Edit `tools/test9.json` (or your own JSON) with new coordinate values.
2. Run `python tools/preview_layout.py --config tools/test9.json` (~1 second).
3. Inspect `tools/preview/preview.png`.
4. Iterate until it looks right, then transfer the values to `DisplayDebugService.kt`.
5. Do a single 50-second build+install to confirm on real hardware.

---

## test9.json

Reference layout JSON mirroring `DisplayDebugService.kt :: testRealisticLayout()`.  
Three zones: speed (font1, top), power (font2, mid), heart-rate (font3, bottom).  
Edit freely — it is never read by the Android build.
