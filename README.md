# Traffic Flow & Intersection Simulator — 2D Grid Edition
### CMSC335, Dec 2023 — Project 3 (2D Refactor)
**Author:** John Leckie

---

## What's New (vs. the original 1D version)

| Feature | Original | 2D Grid Edition |
|---|---|---|
| Road layout | 1D linear road | 6×6 intersection grid (36 lights) |
| Car navigation | Forward only, wraps at end | Random turns at each intersection (left / straight / right) |
| Visualization | Text fields only | Animated graphical canvas + text table |
| Intersections | Up to 7 | Fixed 36 (6×6), all threaded |
| Traffic light display | PNG icons in a row | Colored circles on the canvas + optional icons |
| Car representation | Text only | Colored rectangles with direction arrows |
| Two-lane roads | No | Yes (lane offset by direction) |

---

## Project Structure

```
TrafficSim/
├── TrafficSim.iml               ← IntelliJ module file
├── .idea/
│   ├── misc.xml
│   └── modules.xml
├── README.md
└── src/
    ├── TrafficAnalysisGUI.java  ← Main class (entry point)
    ├── GridCanvas.java          ← Animated road grid painter
    ├── CarInfoPane.java         ← Scrollable car data table
    ├── Car.java                 ← Car state + info row
    ├── CarMovement.java         ← Car thread (movement, turns, light reaction)
    ├── TrafficLight.java        ← Intersection model
    ├── TrafficLightDisplay.java ← Light color-cycling thread
    ├── CurrentTime.java         ← Clock thread
    ├── Direction.java           ← Enum: NORTH/EAST/SOUTH/WEST + turn logic
    ├── GridPosition.java        ← 2D grid coordinate value class
    └── resources/
        ├── TrafficLight.png     ← App window icon (placeholder — replace freely)
        ├── redLight.png         ← Red light icon   (placeholder — replace with yours)
        ├── YellowLight.png      ← Yellow light icon (placeholder — replace with yours)
        └── GreenLight.png       ← Green light icon  (placeholder — replace with yours)
```

---

## Opening in IntelliJ IDEA

1. **File → Open** — select the `TrafficSim/` folder (the one containing `TrafficSim.iml`)
2. IntelliJ will detect the module file automatically.
3. If prompted, set the **Project SDK** to JDK 11 or later:
   - File → Project Structure → SDKs → add your JDK if not listed
4. **Right-click `TrafficAnalysisGUI.java`** → **Run 'TrafficAnalysisGUI.main()'**

---

## Replacing the Traffic Light PNGs

The `src/resources/` folder contains generated placeholder PNGs.
To use your own graphics:

1. Place your files in `src/resources/` with **exactly** these names:
   - `redLight.png`
   - `YellowLight.png`
   - `GreenLight.png`
   - `TrafficLight.png`  (window icon — optional)
2. In IntelliJ, right-click `src/resources/` → **Mark Directory As → Resources Root**
   (if it isn't already). The resource loader uses `getResource("resources/<name>")`.
3. Run the project — your icons will appear in the car info pane.
   The GridCanvas renders light color independently of the PNGs, so the
   simulation works correctly even if PNGs are missing.

---

## Simulation Controls

| Button | Action |
|---|---|
| **Start** | Resets and restarts the simulation from scratch |
| **Stop** | Halts all threads; shows "SIMULATION STOPPED" in the car table |
| **Pause** | Freezes all motion without stopping threads |
| **Continue** | Resumes from pause |
| **Add Car** | Adds one car (up to 6 maximum) |

---

## How It Works

### Grid
- 6×6 = 36 intersections, each with a `TrafficLight` and a `TrafficLightDisplay` thread.
- Light timing is randomized per intersection (green: 10–16 s, red: 6–10 s, yellow: 3 s).
- Intersections are 100 pixels apart on the canvas.

### Cars
- Each car starts at a random intersection with a random heading.
- At every intersection, the car randomly chooses to go **straight, left, or right**.
- If the preferred direction would leave the grid, alternate directions are tried;
  as a last resort the car U-turns (always stays on the grid).
- Cars react to lights:
  - 🟢 **Green** → random speed 40–70 km/h
  - 🟡 **Yellow** → slowed to 15–30 km/h
  - 🔴 **Red** → stops at the intersection until light changes
- Movement is animated in 10 sub-steps per segment for smooth motion.

### Two-lane roads
- Cars are offset perpendicular to the road center line based on heading,
  so eastbound and westbound (or northbound and southbound) traffic
  appears in separate lanes. Collisions are not modeled.

---

## Known Limitations / Future Work
- Swing thread safety: car threads update Swing components directly (same as original).
  A production app would use `SwingUtilities.invokeLater()` for all UI updates.
- Cars do not queue behind each other at red lights — each car independently
  snaps to its intersection and waits.
- No acceleration/deceleration model (same as original).
