# Phase 4 performance checkpoint

## Measurement environment

1. **Project branch:** `performance-baseline-checkpoint-v1`
2. **Project commit:** Use `git rev-parse HEAD` immediately before each hardware run and record it here. No hardware run was available while this report was authored.
3. **Device model:** Not measured — no compatible connected device was available.
4. **Android version:** Not measured — no compatible connected device was available.
5. **Build variant:** `benchmark` app build: non-debuggable, shell-profileable, debug-signed locally, release minification/resource-shrinking settings inherited (both currently disabled).
6. **App version:** `1.0` (`versionCode` 1).
7. **Library size:** Not measured — no compatible connected device was available.
8. **Screen refresh rate:** Not measured — no compatible connected device was available.
9. **Battery/charging state:** Not measured — no compatible connected device was available.
10. **Brightness/audio output:** Not measured — no compatible connected device was available.

## Tool versions and run configuration

- Android Gradle Plugin: 9.2.1
- Gradle wrapper: 9.4.1
- Kotlin: 2.2.10
- AndroidX Benchmark/Baseline Profile plugin: 1.4.1
- AndroidX ProfileInstaller: 1.4.1
- AndroidX UIAutomator: 2.4.0
- AndroidX Tracing: 1.3.0
- Default full iterations: 10 (`cdplaya.fullIterations` overrides)
- Default smoke iterations: 2 (`cdplaya.smokeIterations` overrides)

The stable Baseline Profile plugin 1.4.1 reports that its declared compatibility testing predates AGP 9.0. The module and generated tasks compile under AGP 9.2.1, but connected profile collection remains a required device validation.

## Pre-optimization inspection

Pocket Flip used a continuously repeating Compose `Animatable` while playback was active. The animation was advanced by the Compose frame clock, so a 60/120 Hz display could invalidate its meter Canvas at the display cadence. Every draw rebuilt a two-element meter list and recalculated local waveform sampling, smoothing, seeded band movement, and 48 LCD segments. The loop was disposed by theme replacement/collapse, but was not explicitly gated under an expanded-player sheet and had no local-silence gate.

Pocket Cassette used another frame-clock `Animatable` for reel rotation. Its state was read in `PocketCassetteWindow`, causing the artwork, label, window, and mechanism call subtree to participate in high-frequency recomposition. Dynamic reel/tape geometry was recalculated per frame, and a two-element roller `List` was allocated in the draw path. Playback pause stopped the animation, while overlay and explicit lifecycle gates were absent.

Retro Rack used the same frame-clock meter pattern as Pocket Flip and allocated meter levels per draw. Classic Wheel did not contain a comparable continuous visualizer loop and remains the control theme. Modern’s continuous work is limited to user-triggered carousel/drag animations; its waveform bars are derived from the existing 500 ms playback progress state rather than an independent frame loop.

The expanded player is only composed while expanded and the selected theme is a direct `when` branch, so collapse and theme replacement dispose the old theme. Playback progress is collected inside the overlay subtree, not at the app-shell root. Lifecycle-aware route collection already prevents broad stopped-Activity state observation, but delay-based visualizer scheduling would not have been safe; the new limiter therefore retains the lifecycle-aware Compose frame clock and adds an explicit STARTED gate.

Library lists/grids generally use stable item keys. `contentType` is not supplied, but no benchmark evidence was available to justify a speculative list rewrite. Cache-first library publication, the MediaStore refresh, DataStore preferences, controller connection, and reconciliation remain in their existing order.

## Implemented instrumentation and bounded rendering

Constant privacy-safe trace sections cover preferences-ready, cache-first/final library publication, MediaStore index query, library classification/enrichment, artwork repair, index construction, reconciliation planning, playback connection, metadata replacement, queue replacement, and Pocket Flip/Pocket Cassette/Retro Rack update/draw work. No song metadata or paths are included.

All retro visualizer phase sources now use a 30 Hz admission limiter over Compose frame callbacks. A 120 Hz display can still provide smooth frame synchronization, but at most roughly 30 expensive state updates are accepted per second. The loops stop or suspend when playback pauses, the theme is replaced, the player collapses, the Activity falls below STARTED, a full-screen/sheet overlay covers the player, or the local Pocket Flip/Retro Rack waveform energy is below the silence gate. Caller-owned `FloatArray` buffers replace per-update meter-list allocation. Pocket Cassette reel state is read only in its mechanism Canvas, and roller geometry no longer creates a per-draw list.

These changes are supported by source inspection and deterministic cadence/lifecycle tests. They are not claimed as measured CPU, frame-time, power, or thermal improvements.

## Results

### StartupTimingMetric

| Scenario | Result |
|---|---|
| Cold startup | Not measured — no compatible connected device was available. |
| Warm startup | Not measured — no compatible connected device was available. |
| Hot startup | Not measured — no compatible connected device was available. |

### FrameTimingMetric critical journeys

| Journey | Result |
|---|---|
| Home launch/vertical scroll | Not measured — no compatible connected device was available. |
| Songs list/grid scroll | Not measured — no compatible connected device was available. |
| Albums and artists scroll/detail | Not measured — no compatible connected device was available. |
| Search/query/results scroll | Not measured — no compatible connected device was available. |
| Modern expand/collapse and track swipe | Not measured — no compatible connected device was available. |
| Queue open/manipulation | Not measured — no compatible connected device was available. |
| Theme switching | Not measured — no compatible connected device was available. |
| Pocket Flip sustained | Not measured — no compatible connected device was available. |
| Pocket Cassette sustained | Not measured — no compatible connected device was available. |
| Classic Wheel control | Not measured — no compatible connected device was available. |
| Return to app shell during playback | Not measured — no compatible connected device was available. |

### Visualizer before/after, memory, CPU, and thermal

- Pocket Flip before/after: Not measured — no compatible connected device was available.
- Pocket Cassette before/after: Not measured — no compatible connected device was available.
- Classic Wheel control: Not measured — no compatible connected device was available.
- Memory observations: Not measured — no compatible connected device was available.
- CPU/Perfetto observations: Not measured — no compatible connected device was available.
- Thermal observations: Not measured — no compatible connected device was available.

### Baseline Profile comparison

- No-compilation result: Not measured — no compatible connected device was available.
- Baseline Profile result: Not measured — no compatible connected device was available.
- Generated `baseline-prof.txt`: Not generated — no compatible connected device was available; no rules were fabricated.
- Generated `startup-prof.txt`: Not generated — no compatible connected device was available; no rules were fabricated.

On a connected API 33+ physical device, the plugin copies generated files to `app/src/release/generated/baselineProfiles/`. APK packaging can then be verified by inspecting `assets/dexopt/baseline.prof` and `assets/dexopt/baseline.profm` in the release-like APK.

## Reproduction commands

```powershell
.\gradlew.bat :app:assembleBenchmark
.\gradlew.bat :benchmark:assembleBenchmarkBenchmark

# Reduced smoke suite
.\gradlew.bat :benchmark:connectedBenchmarkBenchmarkAndroidTest `
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.cdplaya.benchmark.MacrobenchmarkSmokeSuite

# Full startup and frame suites
.\gradlew.bat :benchmark:connectedBenchmarkBenchmarkAndroidTest `
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.cdplaya.benchmark.StartupBenchmarks
.\gradlew.bat :benchmark:connectedBenchmarkBenchmarkAndroidTest `
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.cdplaya.benchmark.JourneyBenchmarks

# Generate Baseline and focused Startup Profiles
.\gradlew.bat :app:generateReleaseBaselineProfile

# Same-device compilation comparison
.\gradlew.bat :benchmark:connectedBenchmarkBenchmarkAndroidTest `
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.cdplaya.benchmark.BaselineProfileComparisonBenchmarks
```

Benchmark JSON and traces are written below `benchmark/build/outputs/`; connected reports are below `benchmark/build/reports/androidTests/connected/`. Do not commit these artifacts.

For an additional 60-second Pocket Flip Perfetto capture, install/launch the benchmark build, select Pocket Flip, begin playback, expand it, then run:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
Get-Content -Raw tools\performance\cdplaya-perfetto.pbtx |
  & $adb shell perfetto --txt -c - -o /data/misc/perfetto-traces/cdplaya-phase4.perfetto-trace
& $adb pull /data/misc/perfetto-traces/cdplaya-phase4.perfetto-trace benchmark\build\outputs\perfetto\
```

## Thermal method for the next device run

Use the same physical device, benchmark build, track, playback state, brightness, audio route, refresh-rate mode, starting battery percentage, and approximate starting temperature. Disconnect charging when practical. Let the phone return near baseline between the app shell, Modern, Classic Wheel, Pocket Flip, and Pocket Cassette runs. Record both paused and playing cases, run each expanded theme for ten minutes, and capture at least one Pocket Flip Perfetto trace. Treat all observations as comparative and device-specific, not laboratory energy measurements.

## Automated validation

The final host-side validation completed successfully on July 22, 2026:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug `
  :app:assembleBenchmark :benchmark:assembleBenchmarkBenchmark --stacktrace
```

Gradle reported `BUILD SUCCESSFUL` with 164 actionable tasks (75 executed and 89 up-to-date). This validates unit tests, Kotlin compilation, debug app and instrumentation APK assembly, debug lint, the non-debuggable benchmark app APK, and the benchmark test APK. The merged benchmark manifest contains `<profileable android:shell="true" />` and no `android:debuggable="true"` application override. `git diff --check` passed, and no build directory, APK, app bundle, Perfetto trace, or trace file is tracked by Git.

An ADB device check returned an empty device list. Consequently, `connectedDebugAndroidTest`, connected Macrobenchmark suites, Baseline/Startup Profile generation, and profile-content inspection were intentionally not run; they are device-only checks, not host validation failures.

## Limitations and remaining concerns

- No compatible device was connected, so Macrobenchmark execution, profile generation, APK profile-content inspection, memory metrics, Perfetto evidence, visual cadence validation, and thermal comparisons remain pending.
- The 30 Hz cadence preserves the intended retro motion mathematically, but real-device visual review at 60 Hz and adaptive/120 Hz is still required.
- Baseline Profile plugin 1.4.1’s AGP 9.2.1 warning should be rechecked when a newer stable plugin declares AGP 9 support.
- No additional startup/list/Compose optimization commit was created because no trace or benchmark evidence justified one.
