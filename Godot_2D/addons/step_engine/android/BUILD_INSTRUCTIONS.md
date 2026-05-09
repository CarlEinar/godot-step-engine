# Building the Step Counter Android Plugin

This folder contains the Kotlin source for the `StepCounterPlugin` Godot 4 Android plugin.
After building, the `.aar` files go into `addons/step_engine/bin/`.

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Android Studio | Latest stable | Easiest option — handles all Gradle setup |
| JDK | 17+ | Bundled with Android Studio |
| Android SDK | API 35 (compileSdk) | Install via Android Studio SDK Manager |

---

## Method A — Android Studio (recommended)

1. Open Android Studio.
2. Choose **File → Open** and select this folder (`addons/step_engine/android/`).
3. Wait for Gradle sync to finish.
4. In the **Build** menu:
   - For a release build: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
   - Or via the Gradle panel: run `:plugin → Tasks → build → assembleRelease`
5. The output `.aar` will be at:
   ```
   plugin/build/outputs/aar/StepCounterPlugin-release.aar
   plugin/build/outputs/aar/StepCounterPlugin-debug.aar
   ```
6. Copy both files to:
   ```
   addons/step_engine/bin/release/StepCounterPlugin-release.aar
   addons/step_engine/bin/debug/StepCounterPlugin-debug.aar
   ```

---

## Method B — Command line (Gradle required)

From this folder (`addons/step_engine/android/`), run:

```powershell
# Windows
.\gradlew.bat assembleRelease
.\gradlew.bat assembleDebug
```

If `gradlew.bat` is not present, generate it first with system Gradle (Gradle 8+):
```powershell
gradle wrapper --gradle-version 8.9
```

Then copy the `.aar` files as shown in step 6 above.

---

## Godot Setup (after building)

1. In Godot editor, open the project.
2. Go to **Project → Export → Add → Android**.
3. In the Android export preset, enable **"Use Custom Build"**.
   - This generates a `Godot_2D/android/` folder with Gradle files (gitignored).
4. The `export_plugin.gd` (automatically active when the addon is enabled) will include
   the `.aar` in the Android build via `_get_android_libraries()`.
5. Make sure the addon is enabled: **Project → Project Settings → Plugins → Step Engine ✓**

---

## Testing on Device

1. Enable **USB debugging** on your Android phone.
2. Connect it via USB.
3. In Godot, go to **Project → Export**, select the Android preset.
4. Click **"Export and Run"** (or use the Remote Debug button).
5. Open the demo scene (`demo/TestSteps.tscn`).
6. Tap **Start Tracking** — the step count should increment as you walk.

> **Note**: Android emulators do not have a step counter sensor.
> A physical device is required for testing.

---

## Troubleshooting

**"Plugin not loaded"** at runtime:
- Confirm the `.aar` is in `addons/step_engine/bin/debug/` or `bin/release/`
- Confirm "Use Custom Build" is enabled in the Android export preset
- Confirm the addon is enabled in Project Settings → Plugins

**"No step sensor"**:
- Some budget phones omit the hardware step counter sensor
- Try a different device

**Build error: `Unresolved reference: GodotPlugin`**:
- Gradle cannot resolve the Godot dependency
- Check that `org.godotengine:godot:4.4.0.stable` exists on Maven Central
- Visit https://central.sonatype.com/artifact/org.godotengine/godot/versions to find an available version
- Update the version in `plugin/build.gradle.kts`

**Permission not granted**:
- The first run shows the ACTIVITY_RECOGNITION system dialog
- If denied, go to Android Settings → Apps → [YourApp] → Permissions and grant it manually
