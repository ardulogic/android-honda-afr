import matplotlib
import numpy as np
matplotlib.use('Agg')
import matplotlib.pyplot as plt

# ======== CONFIGURABLE CONSTANTS =========

# Tick radial extents
MAJOR_TICK_INNER_RADIUS         = 0.95
MAJOR_TICK_OUTER_RADIUS         = 1.05

MINOR_TICK_INNER_RADIUS         = 0.95
MINOR_TICK_OUTER_RADIUS         = 1.0

MIDDLE_MINOR_TICK_INNER_RADIUS  = MINOR_TICK_INNER_RADIUS
MIDDLE_MINOR_TICK_OUTER_RADIUS  = MINOR_TICK_OUTER_RADIUS

# Tick widths
MAJOR_TICK_WIDTH         = 4.0
MIDDLE_MINOR_TICK_WIDTH  = 4.0
MINOR_TICK_WIDTH         = 1.5
WIDTH_RED_TICK           = 4.0

# Font and layout
FONT_SIZE_LABELS         = 30
LABEL_DISTANCE           = 0.13

# Tick settings
MINOR_TICKS_PER_MAJOR    = 10
NUM_RED_TICKS_AFTER_LAST_MAJOR = 4  # <--- CONFIGURABLE

# Colors
COLOR_MAJOR_TICK         = '#adb1b1'
COLOR_MIDDLE_MINOR_TICK  = '#adb1b1'
COLOR_MINOR_TICK         = '#7c8083'
COLOR_LABELS             = '#adb1b1'
COLOR_RED_TICK           = '#a36564'

# Tick value range and spacing
MAJOR_TICK_START_VAL     = 0
MAJOR_TICK_END_VAL       = 12
MAJOR_TICK_STEP          = 2

# ======== GAUGE SETUP =========

start_deg, end_deg = -225, 15  # full sweep: 270°
minor_step = MAJOR_TICK_STEP / MINOR_TICKS_PER_MAJOR
max_val = MAJOR_TICK_END_VAL + NUM_RED_TICKS_AFTER_LAST_MAJOR * minor_step
GAUGE_RANGE = max_val - MAJOR_TICK_START_VAL

major_vals = np.arange(MAJOR_TICK_START_VAL, MAJOR_TICK_END_VAL + 1, MAJOR_TICK_STEP)
all_ticks = np.round(np.arange(MAJOR_TICK_START_VAL, max_val + minor_step, minor_step), 5)

# Split tick types
minor_vals = np.setdiff1d(all_ticks, major_vals)
middle_minor_vals = []

for m in major_vals[:-1]:
    mid_val = round(m + (MINOR_TICKS_PER_MAJOR // 2) * minor_step, 5)
    if mid_val not in major_vals:
        middle_minor_vals.append(mid_val)

middle_minor_vals = np.array(middle_minor_vals)
slim_minor_vals = np.setdiff1d(minor_vals, middle_minor_vals)

def val_to_angle(v):
    return np.deg2rad(
        start_deg + (v - MAJOR_TICK_START_VAL) / GAUGE_RANGE * (end_deg - start_deg)
    )

# ======== CONFIGURABLE RED ZONE RANGE =========

RED_ZONE_START = MAJOR_TICK_END_VAL - 2 * minor_step
RED_ZONE_END   = MAJOR_TICK_END_VAL + NUM_RED_TICKS_AFTER_LAST_MAJOR * minor_step

def is_red(v):
    return RED_ZONE_START <= v <= RED_ZONE_END

# ======== PLOT SETUP =========

fig = plt.figure(figsize=(6, 6))
ax = plt.subplot(111, polar=True)

ax.set_theta_zero_location("E")
ax.set_theta_direction(-1)
ax.set_axis_off()
ax.set_rlim(0, MAJOR_TICK_OUTER_RADIUS + 0.05)

# ======== DRAW TICKS =========

# Major ticks
for v in major_vals:
    ang = val_to_angle(v)
    red = is_red(v)
    color = COLOR_RED_TICK if red else COLOR_MAJOR_TICK
    width = WIDTH_RED_TICK if red else MAJOR_TICK_WIDTH

    ax.plot([ang, ang], [MAJOR_TICK_INNER_RADIUS, MAJOR_TICK_OUTER_RADIUS],
            lw=width, color=color)

    # Increase spacing for double-digit labels
    if v >= 10:
        label_distance = LABEL_DISTANCE + 0.07
    else:
        label_distance = LABEL_DISTANCE

    ax.text(
        ang, MAJOR_TICK_INNER_RADIUS - label_distance, f"{v}",
        ha='center', va='center',
        fontsize=FONT_SIZE_LABELS, fontweight='bold',
        color=COLOR_LABELS
    )

# Middle minor ticks
for v in middle_minor_vals:
    ang = val_to_angle(v)
    red = is_red(v)
    color = COLOR_RED_TICK if red else COLOR_MIDDLE_MINOR_TICK
    width = WIDTH_RED_TICK if red else MIDDLE_MINOR_TICK_WIDTH
    ax.plot([ang, ang], [MIDDLE_MINOR_TICK_INNER_RADIUS, MIDDLE_MINOR_TICK_OUTER_RADIUS],
            lw=width, color=color)

# Slim minor ticks
for v in slim_minor_vals:
    ang = val_to_angle(v)
    red = is_red(v)
    color = COLOR_RED_TICK if red else COLOR_MINOR_TICK
    width = WIDTH_RED_TICK if red else MINOR_TICK_WIDTH
    ax.plot([ang, ang], [MINOR_TICK_INNER_RADIUS, MINOR_TICK_OUTER_RADIUS],
            lw=width, color=color)

# Center hub
ax.add_artist(plt.Circle((0, 0), 0.1, transform=ax.transData._b, color='black'))

# Save figure
fig.savefig("gauge.png", dpi=140, transparent=True)
