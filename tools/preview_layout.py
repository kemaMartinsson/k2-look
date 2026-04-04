#!/usr/bin/env python3
"""
preview_layout.py — ActiveLook Display Layout Previewer
========================================================
Renders a 304×256 ActiveLook layout to a PNG without needing glasses.
Replaces the 50-second build+install+glasses cycle for coordinate calibration.

USAGE
-----
  python tools/preview_layout.py                     # built-in Test 9 (speed/power/HR)
  python tools/preview_layout.py --viewer            # flip to viewer perspective
  python tools/preview_layout.py --config layouts.json
  python tools/preview_layout.py --output my.png --no-zones

COORDINATE SYSTEM (display space, same as firmware)
-----------------------------------------------------
  Origin (0,0) at TOP-LEFT of display.
  X increases left→right (0–303). Y increases top→bottom (0–255).
  The lens MIRRORS the image on both axes to the viewer.
  viewer-LEFT  = high display-x     viewer-RIGHT = low display-x
  viewer-TOP   = high zone y0       viewer-BOTTOM = low zone y0

RENDERING RULES (empirically verified, Sessions 2–7)
------------------------------------------------------
  Main value text:  RIGHT-BASELINE anchor at (x0+txtX, y0+txtY).
    → text flows LEFTWARD (toward low display-x = viewer-right).
  ExtraCmd bitmap:  pasted at top-left = (x0+rel_x, y0+rel_y).
  ExtraCmd text:    LEFT-BASELINE anchor at  (x0+rel_x, y0+rel_y).
    → text flows RIGHTWARD (toward high display-x = viewer-left).

LAYOUT MATH ([icon][value][unit] — confirmed Session 6)
---------------------------------------------------------
  icon (28×28) right-flush:  rel_x = 216, flows 216→243 display-right
  value txtX = 208            (8px gap left of icon edge)
  unit  rel_x = 5             (viewer-right, low display-x)

JSON FORMAT
-----------
  See tools/test9.json for a reference example.

REQUIREMENTS
------------
  pip install Pillow
"""

import sys
import os
import json
import argparse
from dataclasses import dataclass, field
from typing import Optional, List

try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError:
    print("ERROR: Pillow required.  pip install Pillow")
    sys.exit(1)

# ──────────────────────────────────────────────────────────────────────────────
#  Constants
# ──────────────────────────────────────────────────────────────────────────────

DISPLAY_W = 304
DISPLAY_H = 256

BG_COLOR   = (0,   0,   0)      # black
FG_COLOR   = (0, 220,  50)      # ActiveLook green
DIM_COLOR  = (0,  70,  18)      # dim green — zone outlines / safe-area border
ANNO_COLOR = (80,  80,  80)     # grey — axis tick labels (debug mode)

# ActiveLook font IDs → nominal pixel height (cap-height approx, empirical)
FONT_HEIGHTS = {1: 24, 2: 38, 3: 64, 4: 75, 5: 82}

# Locate project root from this file's location (tools/../)
_SCRIPT_DIR  = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(_SCRIPT_DIR)

FONT_FILE  = os.path.join(PROJECT_ROOT, "docs", "Activelook-Visual-Assets",
                           "fonts", "Source_Sans_Pro", "SSP-SemiBold-Spacing.otf")
ICONS_DIR  = os.path.join(PROJECT_ROOT, "docs", "Activelook-Visual-Assets", "images")

# ──────────────────────────────────────────────────────────────────────────────
#  Data structures
# ──────────────────────────────────────────────────────────────────────────────

@dataclass
class ExtraCmd:
    """One sub-command inside a LayoutExtraCmd chain.

    type:
      "bitmap" — paste icon at (x0+x, y0+y); icon_id selects the image file.
      "font"   — switch current ExtraCmd font to font_id.
      "text"   — draw text at (x0+x, y0+y) with left-baseline anchor.
    """
    type:    str
    icon_id: int = 0     # "bitmap": icon ID (matches filename prefix in ICONS_DIR)
    x:       int = 0     # "bitmap"/"text": rel-x from zone x0
    y:       int = 0     # "bitmap"/"text": rel-y from zone y0
    text:    str = ""    # "text": string to draw
    font_id: int = 1     # "font": font ID to activate


@dataclass
class Layout:
    """Mirrors one LayoutParameters save + the layoutClearAndDisplayExtended call.

    Zone geometry  : x0, y0, width, height  (display coordinates)
    Value text pos : txt_x, txt_y            (relative to x0, y0; right-baseline anchor)
    Value text      : value                  (the string to render as the main data value)
    Extra commands  : extra_cmds             (icon / unit-label sub-commands)
    """
    id:     int
    x0:     int
    y0:     int
    width:  int
    height: int
    font:   int           # main font ID (1–5)
    txt_x:  int           # right edge of value text, relative to x0
    txt_y:  int           # baseline of value text, relative to y0
    value:  str = ""
    extra_cmds: List[ExtraCmd] = field(default_factory=list)


# ──────────────────────────────────────────────────────────────────────────────
#  Font + Icon caches
# ──────────────────────────────────────────────────────────────────────────────

_font_cache: dict = {}

def get_font(font_id: int, override_px: Optional[int] = None) -> ImageFont.FreeTypeFont:
    """Return a cached SourceSansPro instance sized for this font ID."""
    px  = override_px or FONT_HEIGHTS.get(font_id, 24)
    key = (font_id, px)
    if key not in _font_cache:
        try:
            _font_cache[key] = ImageFont.truetype(FONT_FILE, px)
        except Exception as e:
            print(f"  [warn] Cannot load font ({e}) — falling back to PIL default")
            _font_cache[key] = ImageFont.load_default()
    return _font_cache[key]


_icon_cache: dict = {}

def load_icon(icon_id: int) -> Optional[Image.Image]:
    """Load and green-tint an icon PNG by its numeric ID prefix.

    Icon files are named like '26_speed_28x28.png' in ICONS_DIR.
    The source PNGs are greyscale-on-black (no transparency channel that
    encodes the shape — all pixels are fully opaque, max luminance ~102/255).
    We derive the green intensity from the luminance: black = background = 0
    intensity, bright pixels = icon shape = full intensity.  The luminance is
    normalised to the per-image maximum so faint icons still render clearly.
    Returns None if no matching file is found.
    """
    if icon_id in _icon_cache:
        return _icon_cache[icon_id]

    result = None
    prefix = f"{icon_id}_"
    try:
        for fname in sorted(os.listdir(ICONS_DIR)):
            if fname.startswith(prefix) and fname.lower().endswith(".png"):
                src = Image.open(os.path.join(ICONS_DIR, fname)).convert("L")  # greyscale
                pixels = src.load()
                w, h   = src.size
                # Normalise: find the brightest pixel so the icon fills the
                # full green range even if source levels are compressed (0–102).
                max_lum = max(src.getextrema()[1], 1)  # avoid div-by-zero
                out = Image.new("RGBA", src.size, (0, 0, 0, 255))
                op  = out.load()
                for py in range(h):
                    for px in range(w):
                        lum  = pixels[px, py]
                        norm = lum / max_lum          # 0.0 (black) → 1.0 (icon)
                        gr   = round(FG_COLOR[1] * norm)
                        gb   = round(FG_COLOR[2] * norm)
                        op[px, py] = (0, gr, gb, 255)
                result = out
                break
    except Exception as e:
        print(f"  [warn] Icon {icon_id}: {e}")

    _icon_cache[icon_id] = result
    return result


# ──────────────────────────────────────────────────────────────────────────────
#  Renderer
# ──────────────────────────────────────────────────────────────────────────────

def render_layouts(
    layouts: List[Layout],
    show_zones:      bool = True,
    show_safe_area:  bool = True,
    viewer_perspective: bool = False,
    output_path:     str  = "preview.png",
    open_after:      bool = True,
) -> str:
    """Render all layouts onto the 304×256 canvas and save to output_path.

    viewer_perspective=True applies a coordinate-space transform so elements are
    repositioned to where the rider's eye sees them — without pixel-flipping glyphs
    or icons internally.  It is the SVG equivalent of transform="rotate(180)" applied
    to the coordinate system: each element's draw point is mirrored on both axes, text
    anchors are swapped (rs↔ls), and icon bitmaps are rotated 180° so they remain
    readable through the optical mirror.
    """

    canvas = Image.new("RGB", (DISPLAY_W, DISPLAY_H), BG_COLOR)
    draw   = ImageDraw.Draw(canvas)
    vp = viewer_perspective

    def cx(x: int, y: int) -> tuple:
        """Map a display-space point to canvas pixel coords."""
        if vp:
            return (DISPLAY_W - 1 - x, DISPLAY_H - 1 - y)
        return (x, y)

    def rect_box(x0: int, y0: int, x1: int, y1: int) -> list:
        """Return a PIL [left, top, right, bottom] rectangle, reordered after any flip."""
        if vp:
            ax0 = DISPLAY_W - 1 - x1
            ay0 = DISPLAY_H - 1 - y1
            ax1 = DISPLAY_W - 1 - x0
            ay1 = DISPLAY_H - 1 - y0
            return [ax0, ay0, ax1, ay1]
        return [x0, y0, x1, y1]

    def flip_anchor(anchor: str) -> str:
        """Swap l↔r in a Pillow anchor string when the x-axis is mirrored."""
        if not vp:
            return anchor
        return anchor.translate(str.maketrans("lr", "rl"))

    # Safe-area border (30px left/right margins, 25px top/bottom)
    if show_safe_area:
        draw.rectangle(rect_box(30, 25, 273, 231), outline=DIM_COLOR)

    for layout in layouts:
        x0, y0 = layout.x0, layout.y0
        w,  h  = layout.width, layout.height

        # Zone boundary outline
        if show_zones:
            draw.rectangle(
                rect_box(x0, y0, x0 + w - 1, y0 + h - 1),
                outline=DIM_COLOR
            )

        # ── Main value text ────────────────────────────────────────────────
        # Display space: RIGHT-BASELINE anchor at (x0+txt_x, y0+txt_y).
        # Viewer space:  the right edge maps to the LEFT edge → anchor becomes "ls".
        if layout.value:
            font   = get_font(layout.font)
            abs_tx = x0 + layout.txt_x
            abs_ty = y0 + layout.txt_y
            draw.text(cx(abs_tx, abs_ty), layout.value,
                      font=font, fill=FG_COLOR, anchor=flip_anchor("rs"))

        # ── ExtraCmd sub-commands ──────────────────────────────────────────
        cur_font_id = 1
        for cmd in layout.extra_cmds:

            if cmd.type == "font":
                cur_font_id = cmd.font_id

            elif cmd.type == "bitmap":
                abs_x = x0 + cmd.x
                abs_y = y0 + cmd.y
                icon  = load_icon(cmd.icon_id)
                if icon:
                    iw, ih = icon.size
                    if vp:
                        # Icon PNGs are designed to look correct TO THE RIDER after the
                        # optical lens flip — i.e. they are already stored in viewer-correct
                        # orientation.  The lens un-flips them on the way to the eye.
                        # So in --viewer mode we only reposition the icon; no pixel flip.
                        px = DISPLAY_W - 1 - (abs_x + iw - 1)
                        py = DISPLAY_H - 1 - (abs_y + ih - 1)
                    else:
                        px, py = abs_x, abs_y
                    canvas.paste(icon, (px, py), icon)
                else:
                    # Placeholder box when icon file not found
                    iw = 40 if cmd.icon_id >= 32 else 28
                    if vp:
                        px = DISPLAY_W - 1 - (abs_x + iw - 1)
                        py = DISPLAY_H - 1 - (abs_y + iw - 1)
                    else:
                        px, py = abs_x, abs_y
                    draw.rectangle(
                        [px, py, px + iw - 1, py + iw - 1],
                        outline=FG_COLOR
                    )
                    small = get_font(1, 10)
                    draw.text((px + 2, py + 2), f"#{cmd.icon_id}",
                              font=small, fill=FG_COLOR)

            elif cmd.type == "text":
                abs_x = x0 + cmd.x
                abs_y = y0 + cmd.y
                font  = get_font(cur_font_id)
                # Display space: LEFT-BASELINE ("ls") — text flows toward high display-x.
                # Viewer space:  that point maps to the RIGHT edge → anchor becomes "rs".
                draw.text(cx(abs_x, abs_y), cmd.text,
                          font=font, fill=FG_COLOR, anchor=flip_anchor("ls"))

    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
    canvas.save(output_path)
    print(f"Saved → {output_path}")

    if open_after:
        try:
            canvas.show()
        except Exception:
            pass  # silently skip if no viewer is available

    return output_path


# ──────────────────────────────────────────────────────────────────────────────
#  Built-in scenario: Test 9 (mirrors DisplayDebugService.kt testRealisticLayout)
# ──────────────────────────────────────────────────────────────────────────────

def test9_scenario() -> List[Layout]:
    """
    Exact copy of DisplayDebugService.kt :: testRealisticLayout() (Session 7 state).
      Top zone  font1  y=153  speed   25.1 km/h   icon=26 (speed 28×28)
      Mid zone  font2  y=89   power   250  W       icon=51 (power 40×40)
      Bot zone  font3  y=25   HR      150  bpm     icon=12 (heart-beat 28×28)
    """
    return [
        Layout(
            id=100, x0=30, y0=153, width=250, height=40,
            font=1, txt_x=200, txt_y=22, value="25.1",
            extra_cmds=[
                ExtraCmd(type="bitmap", icon_id=26, x=235, y=0),   # speed 28×28
                ExtraCmd(type="font",   font_id=1),
                ExtraCmd(type="text",   x=10, y=22, text="km/h"),
            ],
        ),
        Layout(
            id=101, x0=30, y0=89, width=244, height=35,
            font=2, txt_x=196, txt_y=38, value="250",
            extra_cmds=[
                ExtraCmd(type="bitmap", icon_id=51, x=235, y=0),   # power 40×40
                ExtraCmd(type="font",   font_id=1),
                ExtraCmd(type="text",   x=20, y=38, text="W"),
            ],
        ),
        Layout(
            id=102, x0=30, y0=25, width=244, height=50,
            font=3, txt_x=200, txt_y=38, value="150",
            extra_cmds=[
                ExtraCmd(type="bitmap", icon_id=12, x=235, y=0),   # heart-beat 28×28
                ExtraCmd(type="font",   font_id=1),
                ExtraCmd(type="text",   x=20, y=38, text="bpm"),
            ],
        ),
    ]


# ──────────────────────────────────────────────────────────────────────────────
#  JSON config loader
# ──────────────────────────────────────────────────────────────────────────────

def load_layouts_from_json(path: str) -> List[Layout]:
    """
    Load layouts from a JSON file (see tools/test9.json for the example format).

    JSON schema (array of layout objects):
    [
      {
        "id": 100, "x0": 30, "y0": 153, "width": 250, "height": 40,
        "font": 1, "txt_x": 200, "txt_y": 22, "value": "25.1",
        "extra_cmds": [
          {"type": "bitmap", "icon_id": 26, "x": 235, "y": 0},
          {"type": "font",   "font_id": 1},
          {"type": "text",   "x": 10, "y": 22, "text": "km/h"}
        ]
      }
    ]
    """
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    layouts = []
    for d in data:
        cmds = []
        for c in d.get("extra_cmds", []):
            cmds.append(ExtraCmd(
                type    = c["type"],
                icon_id = c.get("icon_id", 0),
                x       = c.get("x", 0),
                y       = c.get("y", 0),
                text    = c.get("text", ""),
                font_id = c.get("font_id", 1),
            ))
        layouts.append(Layout(
            id     = d.get("id", 0),
            x0     = d["x0"],
            y0     = d["y0"],
            width  = d["width"],
            height = d["height"],
            font   = d.get("font", 1),
            txt_x  = d.get("txt_x", 0),
            txt_y  = d.get("txt_y", 0),
            value  = d.get("value", ""),
            extra_cmds = cmds,
        ))
    return layouts


# ──────────────────────────────────────────────────────────────────────────────
#  CLI entry point
# ──────────────────────────────────────────────────────────────────────────────

def main() -> None:
    parser = argparse.ArgumentParser(
        description="ActiveLook layout previewer — renders 304×256 to PNG",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    parser.add_argument(
        "--config", metavar="PATH",
        help="JSON layout config (see tools/test9.json for format). "
             "Omit to run the built-in Test 9 scenario."
    )
    _default_out = os.path.join(_SCRIPT_DIR, "preview", "preview.png")
    parser.add_argument(
        "--output", metavar="PATH", default=_default_out,
        help="Output PNG file path (default: tools/preview/preview.png)"
    )
    parser.add_argument(
        "--viewer", action="store_true",
        help="Flip both axes to simulate viewer perspective on the lens"
    )
    parser.add_argument(
        "--no-zones", dest="show_zones", action="store_false",
        help="Hide zone boundary outlines"
    )
    parser.add_argument(
        "--no-open", dest="open_after", action="store_false",
        help="Do not auto-open the PNG after saving"
    )
    args = parser.parse_args()

    if args.config:
        print(f"Loading config: {args.config}")
        layouts = load_layouts_from_json(args.config)
    else:
        print("No --config — running built-in Test 9 scenario:")
        print("  Top  font1  y=153  speed  25.1 km/h")
        print("  Mid  font2  y=89   power  250 W")
        print("  Bot  font3  y=25   HR     150 bpm")
        layouts = test9_scenario()

    render_layouts(
        layouts,
        show_zones        = args.show_zones,
        viewer_perspective= args.viewer,
        output_path       = args.output,
        open_after        = args.open_after,
    )

    mode = "viewer perspective" if args.viewer else "display coordinates"
    print(f"Done  [{mode}]")


if __name__ == "__main__":
    main()
