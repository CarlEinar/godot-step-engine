# ROADMAP — Godot Step Engine

> **Read this at the start of every session.** It is the primary source of truth for what has been built and what comes next.

---

## Project Goal

Build a **reusable Godot 4 addon** (`addons/step_engine/`) that turns a player's real-world walked steps into in-game currency. Any game that drops in this addon gets step tracking, a daily budget, spend/earn mechanics, persistence, and reusable UI widgets — with no platform-specific code in the game itself.

Target platform: **Android** (primary). iOS considered in a later phase.
Art style: **8-bit pixel art** (placeholder rectangles until art phase).
Language: **GDScript** throughout.

---

## Architecture Overview

```
addons/step_engine/
├── plugin.cfg                   # Godot addon manifest
├── plugin.gd                    # Editor plugin entry point
├── core/
│   ├── StepManager.gd           # Autoload: reads Android sensor, emits signals
│   ├── StepCurrency.gd          # Autoload: step budget, spending, earning, save/load
│   └── StepSaveData.gd          # Resource: serialisable save state
├── android/
│   └── step_counter_plugin.aar  # Compiled Android plugin (reads TYPE_STEP_COUNTER)
└── ui/
    ├── StepDisplay.tscn/.gd     # Reusable: shows live step count + daily total
    └── CurrencyDisplay.tscn/.gd # Reusable: shows available step currency + spend button

Godot_2D/
├── project.godot
├── addons/step_engine/          # symlinked or copied from above
├── demo/
│   ├── DemoMain.tscn            # Proof-of-concept scene
│   └── DemoMain.gd
└── export_presets.cfg           # Android export config
```

### Signal flow

```
Android sensor
  └─► StepManager (autoload)
        │  signal steps_updated(session_steps, daily_steps)
        │  signal daily_reset(new_day_steps)
        └─► StepCurrency (autoload)
              │  signal currency_changed(available, spent_today)
              │  signal insufficient_steps(cost, available)
              └─► UI widgets / Game logic
```

### Key design principles

- **Platform abstraction**: `StepManager` switches between a real Android plugin and a desktop mock automatically. Games never talk to the Android layer directly.
- **Steps ≠ currency 1:1**: A configurable `steps_per_unit` ratio (e.g. 100 steps = 1 coin) lets each game tune the economy.
- **Daily budget**: Steps are credited once per day based on total steps walked. Unspent steps do not roll over by default (configurable).
- **Offline-safe**: Steps are saved to disk every session. If the game was closed, the system picks up from saved state + any new steps since last save.
- **Health Connect (future)**: Architecture is designed so `StepManager` can swap in a Health Connect source (gives accurate all-day totals) without changing any game code.

---

## Phase Status Table

| Phase | Name | Status |
|-------|------|--------|
| 0 | Project Setup | ⬜ Not started |
| 1 | Android Step Plugin Integration | ⬜ Not started |
| 2 | StepManager Autoload | ⬜ Not started |
| 3 | StepCurrency System | ⬜ Not started |
| 4 | Data Persistence | ⬜ Not started |
| 5 | Reusable UI Components | ⬜ Not started |
| 6 | Demo Scene | ⬜ Not started |
| 7 | Health Connect Integration | ⬜ Not started |
| 8 | iOS Pedometer (stretch goal) | ⬜ Not started |

Status values: ⬜ Not started · 🔄 In progress · ✅ Complete

---

## Phase 0 — Project Setup

**Goal**: Create the Godot 4 project and folder skeleton. Nothing platform-specific yet.

**Tasks**:
- [ ] Create `Godot_2D/` Godot 4 project (project.godot, default settings)
- [ ] Set project to portrait mode (mobile default), target Android
- [ ] Create folder structure: `addons/step_engine/core/`, `addons/step_engine/ui/`, `addons/step_engine/android/`, `demo/`
- [ ] Create empty `plugin.cfg` and `plugin.gd` for the addon
- [ ] Add Android export preset (no custom template yet — that comes in Phase 1)
- [ ] Commit: `phase-0: project skeleton`

**Acceptance**: Project opens in Godot editor without errors. Folder structure matches the architecture diagram above.

---

## Phase 1 — Android Step Plugin Integration

**Goal**: Get a real step count reading from the Android sensor into GDScript.

**Background**: Godot 4 does not expose Android's `TYPE_STEP_COUNTER` sensor natively. We will adapt the open-source **Godot-Android-Step-Counter-Plugin** (by Dasonic) which wraps this sensor as a Godot 4 Android Plugin v2.

**Tasks**:
- [ ] Evaluate the Dasonic plugin source: does it cover our needs or do we need to fork/extend it?
- [ ] Integrate (or fork) the plugin into `addons/step_engine/android/`
- [ ] Add required Android permission to the export template: `android.permission.ACTIVITY_RECOGNITION`
- [ ] Write a minimal test scene that calls the plugin and prints step count to the output log
- [ ] Verify on a physical Android device (emulators do not have step sensors)
- [ ] Commit: `phase-1: android step counter plugin integrated`

**Acceptance**: Running on a real Android device, the output log shows a live step count that increments when the device is walked with.

**Known risk**: Custom Android export templates require Godot's Android build tools to be installed (`gradle`, Android SDK). Document the setup steps clearly.

---

## Phase 2 — StepManager Autoload

**Goal**: A GDScript singleton that abstracts the Android plugin behind a clean API.

**StepManager API**:
```gdscript
# Signals
signal steps_updated(session_steps: int, daily_steps: int)
signal daily_reset(previous_day_total: int)

# Properties
var session_steps: int       # steps counted since app opened this session
var daily_steps: int         # total steps for today (session + any saved from earlier today)
var is_tracking: bool        # false on desktop / if plugin unavailable

# Methods
func start_tracking() -> void
func stop_tracking() -> void
func get_daily_steps() -> int
func get_session_steps() -> int
```

**Desktop mock**: When running in the Godot editor or on desktop, `StepManager` exposes a `debug_add_steps(n: int)` method so developers can simulate step earning without a phone.

**Tasks**:
- [ ] Write `addons/step_engine/core/StepManager.gd`
- [ ] Detect platform at runtime (`OS.get_name() == "Android"`)
- [ ] On Android: connect to the step counter plugin via `Engine.get_singleton()`
- [ ] On desktop: enter mock mode, expose `debug_add_steps()`
- [ ] Emit `steps_updated` on every sensor tick
- [ ] Detect day rollover (compare saved date with `Time.get_date_dict_from_system()`) and emit `daily_reset`
- [ ] Register as autoload in `plugin.gd`
- [ ] Commit: `phase-2: StepManager autoload`

**Acceptance**: On desktop, calling `StepManager.debug_add_steps(500)` causes `steps_updated` to fire with correct values. On Android, walking increments `session_steps`.

---

## Phase 3 — StepCurrency System

**Goal**: Convert steps into spendable in-game currency. This is the core economy layer.

**StepCurrency API**:
```gdscript
# Config (set per-game, e.g. in project settings or a config resource)
var steps_per_unit: int = 100       # 100 steps = 1 currency unit
var daily_rollover: bool = false    # unspent currency expires at midnight

# Signals
signal currency_changed(available: int, spent_today: int)
signal insufficient_steps(cost: int, available: int)
signal daily_budget_set(budget: int)   # emitted after daily_reset

# Properties
var available: int      # current spendable currency
var spent_today: int    # currency spent today
var budget_today: int   # total currency earned from today's steps

# Methods
func spend(amount: int) -> bool       # returns false if insufficient
func refund(amount: int) -> void      # add back after a cancelled action
func preview_cost(steps: int) -> int  # how much currency N steps buys
```

**Tasks**:
- [ ] Write `addons/step_engine/core/StepCurrency.gd`
- [ ] Listen to `StepManager.steps_updated` and `StepManager.daily_reset`
- [ ] Implement `spend()`, `refund()`, budget calculation
- [ ] Respect `steps_per_unit` and `daily_rollover` configuration
- [ ] Register as autoload in `plugin.gd`
- [ ] Commit: `phase-3: StepCurrency system`

**Acceptance**: On desktop mock — adding 1000 steps with `steps_per_unit = 100` yields `available = 10`. `spend(5)` reduces it to 5. `spend(10)` returns false and emits `insufficient_steps`.

---

## Phase 4 — Data Persistence

**Goal**: Step state survives app restarts, device reboots, and multi-session play.

**The reboot problem**: Android's `TYPE_STEP_COUNTER` counts steps since last device reboot (not since the game opened). We must save the baseline count at each session start so we can compute the delta correctly after a reboot.

**Save data structure** (`StepSaveData.gd` — extends `Resource`):
```gdscript
var save_date: String          # "YYYY-MM-DD" of last save
var session_baseline: int      # sensor value at last session start
var daily_steps_banked: int    # steps accumulated before current session
var currency_available: int
var currency_spent_today: int
```

**Tasks**:
- [ ] Write `addons/step_engine/core/StepSaveData.gd`
- [ ] Save to `user://step_engine_save.tres` on `steps_updated` (throttled — max once per 60 seconds) and on app exit
- [ ] Load on `StepManager._ready()`, handle missing file (first run)
- [ ] Detect reboot: if saved `session_baseline > current_sensor_value`, assume reboot and reset baseline
- [ ] Detect new day on load: if `save_date != today`, call daily reset logic
- [ ] Commit: `phase-4: data persistence`

**Acceptance**: Close the app with 500 steps. Reopen — `daily_steps` still shows 500 (plus any new steps). Simulate a new day by changing device date — system correctly resets budget.

---

## Phase 5 — Reusable UI Components

**Goal**: Plug-and-play UI scenes any game can instance into its HUD.

**Components**:

1. **StepDisplay** (`StepDisplay.tscn`): Shows today's step count with a progress bar toward a configurable daily goal. Updates live via `StepManager.steps_updated`.

2. **CurrencyDisplay** (`CurrencyDisplay.tscn`): Shows available currency (icon + number). Animates on change (tween). Shows "insufficient" flash on `StepCurrency.insufficient_steps`.

3. **SpendButton** (`SpendButton.tscn`): A button with a cost label. Disables itself when `available < cost`. Calls `StepCurrency.spend()` on press.

All components use placeholder colored rectangles and Label nodes — no final art required.

**Tasks**:
- [ ] Create `addons/step_engine/ui/StepDisplay.tscn` + `.gd`
- [ ] Create `addons/step_engine/ui/CurrencyDisplay.tscn` + `.gd`
- [ ] Create `addons/step_engine/ui/SpendButton.tscn` + `.gd`
- [ ] All components auto-connect to autoloads in `_ready()` — no wiring required in consuming scenes
- [ ] Export `@export var daily_goal: int = 10000` on StepDisplay for per-game customisation
- [ ] Commit: `phase-5: reusable UI components`

**Acceptance**: Instance `StepDisplay` into any scene — it shows live step data with no additional setup.

---

## Phase 6 — Demo Scene

**Goal**: Prove the full system works end-to-end with a minimal playable prototype.

**Demo concept**: A simple "task board" where each task costs a certain number of step-currency units. Completing a task deducts currency and shows a reward. No game logic — just proves the economy layer works.

**Scene**: `demo/DemoMain.tscn`
- Shows `StepDisplay` (today's steps, progress bar)
- Shows `CurrencyDisplay` (available currency)
- Lists 3–4 tasks with varying costs (`SpendButton` for each)
- On spend: task card goes grey ("completed"), shows a placeholder reward
- Desktop: has a "Simulate +100 steps" debug button that calls `StepManager.debug_add_steps(100)`

**Tasks**:
- [ ] Create `demo/DemoMain.tscn` and `demo/DemoMain.gd`
- [ ] Wire up the three UI components
- [ ] Add task cards with spend buttons
- [ ] Add desktop debug button
- [ ] Test on Android device end-to-end
- [ ] Commit: `phase-6: demo scene`

**Acceptance**: On Android — walk 100 steps, open the app, see currency, spend it on a task. On desktop — use debug button to simulate steps, spend currency.

---

## Phase 7 — Health Connect Integration

**Goal**: Read the player's full daily step total from Android Health Connect (not just in-session steps). This gives accurate "you walked 6000 steps today" data even if the game was not open.

**Why this matters**: The step sensor only counts steps while the device is on. Health Connect aggregates data from all sources (phone sensor, fitness tracker, Google Fit). For a game that rewards yesterday's walk, Health Connect is the right source.

**Tasks**:
- [ ] Write a custom Android plugin that calls the Health Connect API (Java/Kotlin, Godot Plugin v2)
- [ ] Request `android.permission.health.READ_STEPS` at runtime (Health Connect shows its own permission dialog)
- [ ] Expose `get_today_steps_async()` to GDScript (returns via signal, since it's async)
- [ ] Update `StepManager` to prefer Health Connect total over sensor delta when available
- [ ] Fall back gracefully if Health Connect is not installed or permission denied
- [ ] Commit: `phase-7: health connect integration`

**Acceptance**: On Android with Health Connect installed and permission granted, `StepManager.daily_steps` reflects the full-day total from Health Connect, not just in-session steps.

---

## Phase 8 — iOS Pedometer (Stretch Goal)

**Goal**: Support iOS using `CMPedometer` (Core Motion framework) via a GDExtension plugin.

This phase is a stretch goal — only pursue if Android support is solid and there is a clear game need.

**Acceptance**: Same `StepManager` API works on iOS with no game-side code changes.

---

## How to Integrate This Addon into a New Game

1. Copy `addons/step_engine/` into your new Godot project.
2. Enable the plugin in **Project → Project Settings → Plugins**.
3. `StepManager` and `StepCurrency` are added as autoloads automatically.
4. Configure `StepCurrency.steps_per_unit` to tune the economy.
5. Instance `CurrencyDisplay` and `StepDisplay` into your HUD.
6. Use `StepCurrency.spend(cost)` in your game logic.
7. Listen to `StepCurrency.currency_changed` to update any custom UI.

---

## Godot Project Root

> To be updated once the project is created in Phase 0.

`Godot_2D/` — relative to repo root.

---

## Android Build Setup (Required for Phase 1+)

Before Android testing, ensure the following is installed and configured on the development machine:

- Android Studio (for SDK tools and emulator)
- Android SDK Platform-Tools
- Godot Android export templates (download via **Editor → Manage Export Templates**)
- Java JDK 17+
- In Godot: **Editor → Editor Settings → Export → Android** — set SDK path

> This only needs to be done once per development machine. Document exact paths here after first setup.
