# Phase A: Android USB bit-perfect feasibility

This document records feasibility evidence only. It does not describe a production
bit-perfect feature and it does not claim that ordinary CDPlaya playback is
bit-perfect.

## 1. Git state

- Required branch: `bit-perfect-feasibility-v1`
- Base commit: `a6b0d5b1860f2116a94204772da2da19a481c0de`
- Base description: merge of the stable Media3 1.10.1 upgrade
- Implementation commit: `fc1c6ff` — Add USB audio feasibility observer and probe
- Verification commit: `855cb26` — Add bit-perfect feasibility verification
- Final HEAD: the report commit that contains this document; its exact hash is
  supplied in the delivery summary because a commit cannot contain its own hash.
- Push status: not pushed
- Final status: Phase A changes committed; the preserved user file below remains
  untracked.
- Preserved unrelated file: untracked `STATUS_REPORT.md`
- Complete diff from the base: 19 application/test files plus this report,
  2,859 inserted lines and 4 deleted lines before the report's final edits.

## 2. Media3 1.10.1 API findings

The resolved `media3-exoplayer-1.10.1-sources.jar` and AAR contain:

- `AudioOutputProvider`, including immutable `FormatConfig`, `OutputConfig`, and
  `FormatSupport` values.
- `AudioTrackAudioOutputProvider.Builder`, including the API-24-gated
  `setAudioTrackBuilderModifier`.
- `ForwardingAudioOutputProvider` and `ForwardingAudioOutput`.
- `AudioTrackAudioOutput.getAudioTrack()`.
- `DefaultAudioSink.Builder.setAudioOutputProvider`.

For raw PCM, `AudioTrackAudioOutputProvider.getOutputConfig` preserves the
encoding, sample rate, and channel configuration supplied in `FormatConfig`.
However, `DefaultAudioSink` first chooses its processing path. With float output
disabled it converts non-PCM16 raw input to PCM16 and runs the ordinary processor
chain. With float output enabled, high-resolution integer PCM is converted to
float and the ordinary processor chain is bypassed. In 1.10.1,
`FormatConfig.enableHighResolutionPcmOutput` is populated directly from the
sink's float-output flag; there is no independent builder switch for preserving
high-resolution integer PCM while retaining ordinary processors.

The default `AudioOutput.canReuseAudioOutput` decision is strict
`OutputConfig.equals`. Encoding, sample rate, channel mask, tunneling, offload,
buffer size, audio attributes, session request, virtual device, playback
parameters, and gapless-offload flags therefore participate in reuse. A changed
sample rate or encoding normally requires a new output without reconstructing
ExoPlayer.

Current online reference pages describe a newer surface in places. For example,
the current `AudioTrackAudioOutputProvider` reference lists
`getAudioCapabilities()`, and the current `AudioTrackAudioOutput` reference
documents a public `audioTrack` field. Neither is present as that API in the
resolved 1.10.1 source; 1.10.1 uses the supported `getAudioTrack()` method.

Official reference points used alongside the resolved sources and installed
stubs: [Android bit-perfect playback guidance](https://developer.android.com/media/platform/improve-audio-playback),
[AudioManager](https://developer.android.com/reference/android/media/AudioManager),
[AudioTrack](https://developer.android.com/reference/android/media/AudioTrack),
[DefaultAudioSink.Builder](https://developer.android.com/reference/androidx/media3/exoplayer/audio/DefaultAudioSink.Builder),
and [AudioTrackAudioOutput](https://developer.android.com/reference/androidx/media3/exoplayer/audio/AudioTrackAudioOutput).

### Android platform contracts

The installed API 36 stubs confirm that the preferred-mixer API introduced in API
34 consists of:

- `AudioManager.getSupportedMixerAttributes`
- `setPreferredMixerAttributes`
- `getPreferredMixerAttributes`
- `clearPreferredMixerAttributes`
- preferred-attribute listener registration/removal
- `AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT`

The supported USB output types are USB device, USB accessory, and USB headset.
The setter and clearer require the normal `MODIFY_AUDIO_SETTINGS` manifest
permission. A match must use the same media `AudioAttributes`, device, encoding,
sample rate, and channel mask. `AudioTrack.getFormat()` provides encoding,
sample rate, position mask, and index mask; routing is truthful only while the
track is playing, using `getRoutedDevice()` through API 35 and the potentially
multi-route `getRoutedDevices()` API on API 36.

## 3. Current CDPlaya path

```text
Source Format (analytics callback)
  -> Media3 decoder
  -> decoded raw PCM
  -> DefaultAudioSink integer/float conversion policy
  -> persistent EqualizerAudioProcessor
  -> AudioOutputProvider.FormatConfig
  -> AudioOutputProvider.OutputConfig
  -> AudioTrackAudioOutput
  -> platform AudioTrack
  -> Android routed output device
```

The live `EqualizerRenderersFactory` forces `setEnableFloatOutput(false)` and
installs exactly one persistent `EqualizerAudioProcessor`. Media3 1.10.1
`DefaultAudioSink` therefore converts high-resolution decoded PCM to PCM16 before
the ordinary processor chain. `EqualizerAudioProcessor.onConfigure` independently
rejects every encoding other than PCM16. Its exact bypass tests prove only that
the PCM16 bytes supplied to it are copied exactly; they do not prove preservation
of a high-resolution source.

`DefaultAudioSink.Builder.setAudioOutputProvider` is the single-sink integration
point. A forwarding provider can observe the post-processor `FormatConfig`, the
resolved `OutputConfig`, and output lifecycle while leaving delegation unchanged.
When the returned output is `AudioTrackAudioOutput`, Media3 1.10.1 exposes the
underlying `AudioTrack` through the public `getAudioTrack()` method; reflection is
not required.

### Evidence caveats

- Source format is not final output format.
- PCM16 processor bypass is not proof of high-resolution preservation.
- A supported mixer attribute is not proof that it was activated.
- A successful preferred-attribute set call is not proof that AudioTrack matched.
- A DAC sample-rate display is secondary evidence.
- No analog-output claim is made.
- No support is inferred for untested devices.
- Device and vendor bit-perfect support is optional.

### Boundary verification

## 4. Processor-backed matrix

The device-side sink configuration test uses the same
`EqualizerRenderersFactory`, persistent `EqualizerAudioProcessor`, forwarding
provider, and Media3 1.10.1 classes as the service.

| Decoded source | Processor input | Processor output | Media3 output | Actual AudioTrack | Processor buffers | Result |
|---|---|---|---|---|---|---|
| PCM16, 44.1 kHz, stereo | PCM16, 44.1 kHz, stereo | PCM16, 44.1 kHz, stereo | PCM16, 44.1 kHz, stereo | Not created by configuration-only case | 0 | Preserved at PCM16 boundary |
| PCM16, 48 kHz, stereo | PCM16, 48 kHz, stereo | PCM16, 48 kHz, stereo | PCM16, 48 kHz, stereo | Not created by configuration-only case | 0 | Preserved at PCM16 boundary |
| packed PCM24, 96 kHz, stereo | PCM16, 96 kHz, stereo | PCM16, 96 kHz, stereo | PCM16, 96 kHz, stereo | Not created by configuration-only case | 0 | High-resolution encoding lost before processor |
| packed PCM24, 192 kHz, stereo | PCM16, 192 kHz, stereo | PCM16, 192 kHz, stereo | PCM16, 192 kHz, stereo | Not created by configuration-only case | 0 | High-resolution encoding lost before processor |

The processor buffer counter is captured during real playback, but the
configuration-only matrix deliberately does not feed audio buffers. Existing
PCM16 exact-bypass tests remain unchanged. Their conclusion is:

```text
Processor bypass is byte-exact for the PCM16 bytes it receives.
```

They do not establish:

```text
The original high-resolution source reached AudioTrack without conversion.
```

## 5. Processor-free matrix

| Requested raw PCM | Processor input | Processor output | Media3 OutputConfig | Actual created AudioTrack | Processor buffers | Result |
|---|---|---|---|---|---|---|
| PCM16, 44.1 kHz, stereo | Not installed | Not installed | PCM16, 44.1 kHz, stereo | PCM16, 44.1 kHz, stereo | 0 | Exact format created |
| PCM16, 48 kHz, stereo | Not installed | Not installed | PCM16, 48 kHz, stereo | PCM16, 48 kHz, stereo | 0 | Exact format created |
| packed PCM24, 96 kHz, stereo | Not installed | Not installed | packed PCM24, 96 kHz, stereo | packed PCM24, 96 kHz, stereo | 0 | Exact format created |
| packed PCM24, 192 kHz, stereo | Not installed | Not installed | packed PCM24, 192 kHz, stereo | packed PCM24, 192 kHz, stereo | 0 | Exact format created |
| packed PCM24, 96 kHz through processor-free float sink | Not installed | Not installed | float PCM, 96 kHz, stereo | Not created by configuration-only case | 0 | Float is a separate representation, not integer equivalence |

This proves that `AudioTrackAudioOutputProvider` can preserve high-resolution
integer PCM when it receives that final raw format. It does not provide a
supported switch that makes `DefaultAudioSink` retain packed integer PCM:
`DefaultAudioSink` converts to PCM16 when float output is disabled and converts
high-resolution input to float when float output is enabled.

### Exact matching implementation

The matcher compares encoding, sample rate, and channel mask and requires an
attribute whose behavior is explicitly `MIXER_BEHAVIOR_BIT_PERFECT`. It never
chooses a nearest rate, alternate encoding, wider container, float substitute,
or default mixer behavior. Platform list order is the deterministic tie-breaker
for otherwise identical exact matches.

The debug-only activation sequence is:

1. Reject non-API-34, zero/multiple USB outputs, unknown output facts, offload,
   tunneling, non-unity speed, active EQ/limiter, and ReplayGain/player gain.
2. Query and snapshot every supported attribute for the single USB output.
3. Select one exact bit-perfect attribute.
4. Register the listener, set the preferred attribute, and query it back.
5. Set the same USB output as ExoPlayer's preferred device.
6. Stop/prepare the existing authoritative ExoPlayer, preserving its playlist,
   position, play intent, session, repeat, and shuffle state so the matching
   AudioTrack is created after the preferred attribute is established.
7. Confirm actual AudioTrack format and route while playback writes.
8. Clear the exact tracked media-attributes/device pair on stop, output release,
   playback error/end, USB removal, explicit stop, activation failure, or service
   destruction.

Cleanup is idempotent. The post-clear query must return null before cleanup is
reported confirmed. The release listener, preferred-attribute listener, raw
device object, and selected platform attribute remain internal and are never
placed in the public snapshot.

### Test fixtures

The JVM test generator creates 40 ms stereo RIFF/WAVE files containing a
low-amplitude 997 Hz sine and an initial impulse. No binary fixture is stored and
no fixture enters a production APK.

| Generated fixture | SHA-256 |
|---|---|
| PCM16, 44.1 kHz | `d7ed2a9b06dfb7efe224106edce61005b2da2b9a28d6734f4e46dd2584650283` |
| PCM16, 48 kHz | `a055c095e0714d50f6bafda84f2be2e60f1a48cde9c3de4ecb0f792f26a41a07` |
| packed PCM24, 96 kHz | `78cf7803c733ddd3019d8f96b3cd45a1cca1aabd809e5f8a3998d3ace2b4ae34` |
| packed PCM24, 192 kHz | `9373b70353694bd5986c8fde6c5d339526abc0e6d9936407b369799c8190622d` |

FLAC decoder fixtures were not added. The decisive sink/provider tests use
Media3 raw `Format` values after the decoder boundary, avoiding production APK
bloat while isolating the question under test.

## 6. USB mixer capability

Primary connected device:

- Model: Samsung `SM-S908U1`
- Android: 16
- API: 36
- Current route during validation: built-in speaker
- Connected USB audio output: none
- USB DAC safe label: not available
- Supported USB mixer attributes: not queryable without a USB output
- Bit-perfect attributes: not queryable
- Exact matches: not queryable
- Unsupported formats: no hardware-specific conclusion
- Ambiguities: none observed because zero USB sinks were enumerated; the probe
  rejects rather than guesses if multiple USB sinks are present
- Preferred attribute set/query/listener: not attempted
- AudioTrack USB route confirmation: not attempted
- DAC display: not available
- Cleanup: no preferred USB attribute was set

API-36 enumeration returned the truthful no-USB result without attempting
activation. The existing 45-case Android instrumentation suite passed before
the change, and the focused four-case feasibility matrix passed after the
change. No USB DAC was connected, so this phase cannot claim that the S22 Ultra
plus a particular DAC activates Android's bit-perfect mixer behavior.

The Galaxy S9+ Android 10 and Galaxy Tab S9 were not connected during this
workspace run. API-29 safety is enforced by the separate API-34 implementation
class, the SDK-gated factory, lint, min-SDK compilation, and JVM fake tests, but
new physical regression on those devices remains outstanding.

## 7. Experimental activation

- Preconditions implemented: debug build, API 34+, exactly one USB sink, known
  non-offloaded/non-tunneled output, unity playback speed, inactive equalizer
  and limiter, ReplayGain off, and player volume 1.0.
- Set/query/listener result: not attempted because no USB audio sink was
  connected.
- AudioTrack format/route/playback result: not tested with USB hardware.
- Cleanup result: no preferred USB attribute was present or set; API-36 safe
  enumeration completed without activation.
- Failure behavior: pure fake-backend tests cover no/multiple USB devices, no
  bit-perfect match, setter failure, listener failure, confirmation mismatch,
  idempotent success cleanup, and post-clear confirmation. Runtime exceptions
  converge on cleanup, and normal preferred-device routing is cleared only if
  this probe applied it.

## 8. Architecture decision

```text
Can CDPlaya retain one ExoPlayer and one persistent sink?
NO

Can the existing persistent equalizer processor remain installed during
high-resolution direct playback?
NO

Can Media3 1.10.1 expose an exact PCM format suitable for Android preferred
mixer attributes?
PARTIALLY

Can the tested Android device and USB DAC activate Android's BIT_PERFECT
mixer behavior?
NOT TESTED

Is controlled player reconstruction required for the production design?
YES
```

Production-option disposition:

- One persistent sink: rejected if it means the current unchanging,
  processor-backed sink.
- One player with dynamically recreated `AudioOutput`: useful for ordinary
  format/rate changes, but insufficient for removing the processor or preserving
  packed high-resolution integer PCM.
- One player with switchable sink configuration: not exposed as a supported
  live mutation by the resolved Media3 1.10.1 builder/sink contracts.
- Controlled player reconstruction: recommended for Phase B, retaining exactly
  one authoritative player/session at a time and restoring all queue/session
  state.
- Another supported Media3 approach: remains an option only if Phase B proves it
  with resolved APIs and target hardware; this phase does not authorize a custom
  sink or direct USB stack.

Rationale:

- One authoritative ExoPlayer, PlaybackService, and MediaLibrarySession can be
  retained at any instant.
- One *unchanging processor-backed sink* cannot serve both normal EQ playback
  and high-resolution direct playback. The current sink converts high-resolution
  integer PCM to PCM16 before the persistent equalizer, even when the equalizer
  is bypassed.
- Media3's provider can expose and create exact PCM16 and packed PCM24
  AudioTracks, but the stock `DefaultAudioSink` does not independently enable
  high-resolution integer output. Its public high-resolution switch is tied to
  float output and bypasses the ordinary processor chain.
- Phase B should therefore prototype controlled reconstruction of the single
  authoritative player/renderers/sink for a processor-free exact session, with
  full queue/session/history restoration. It must choose only a format actually
  exposed by the tested USB mixer's bit-perfect attributes. A packed-24-only DAC
  may require another supported Media3 audio-sink approach; this phase does not
  authorize a custom production sink.
- The experimental same-player stop/prepare path proves output recreation can be
  requested without creating a second player, but it does not solve removal of
  the persistent processor or stock-sink packed-integer conversion. Production
  should not rely on that path alone.

## 9. Automated verification

Pre-change:

- `.\gradlew.bat --no-daemon :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:assembleBenchmark :app:assembleDebugAndroidTest --stacktrace`
  passed in 4m06s. JVM: 582 tests, 0 failures, 0 errors, 4 skipped.
- `.\gradlew.bat --no-daemon :app:connectedDebugAndroidTest --stacktrace`
  passed in 57s: 45 tests on `SM-S908U1`/API 36, 0 failures, 0 skips.

Post-change focused evidence:

- `.\gradlew.bat --no-daemon :app:dependencyInsight --configuration debugRuntimeClasspath --dependency androidx.media3:media3-exoplayer`
  passed and resolved every Media3 module to stable 1.10.1.
- `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests 'com.example.cdplaya.player.feasibility.*' --stacktrace`
  passed in 53s: 32 tests, 0 failures/errors/skips.
- The focused connected feasibility command passed in 44s: 4 tests, 0
  failures/errors/skips. The first harness iteration exposed a null platform
  spatializer in a standalone context-backed provider; the test-only provider
  was corrected to use Media3's supported nullable-context constructor.
  Production retains ExoPlayer's context-backed provider.
- `.\gradlew.bat --no-daemon :app:lintDebug --stacktrace` passed in 4m12s after
  correcting three new explicit API-guard/opt-in errors. No lint baseline,
  assertion, API guard, or warning policy was weakened.

Final ordinary aggregate:

- `.\gradlew.bat --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:assembleBenchmark :benchmark:assembleBenchmarkBenchmark :app:assembleDebugAndroidTest --stacktrace`
  passed in 5m17s. JVM XML totals: 614 tests, 0 failures, 0 errors,
  4 skipped, 125 suites. Lint: 0 errors, 74 warnings, 1 hint. Debug,
  minified release/R8, app benchmark, benchmark-module, and instrumentation
  APK assembly all passed.
- `.\gradlew.bat --no-daemon :app:connectedDebugAndroidTest --stacktrace`
  passed in 54s on `SM-S908U1`/API 36: 49 tests, 0 failures, 0 errors,
  0 skipped (20.693s test time). The test-services `No UID` app-ops line is a
  harmless runner setup message and did not fail the task.

Opt-in existing performance verification:

- `.\gradlew.bat --no-daemon '-Dequalizer.performance=true' '-Dequalizer.longRun=true' :app:testDebugUnitTest --tests 'com.example.cdplaya.player.equalizer.performance.*' --stacktrace`
  ran 5 tests in 42s: 4 passed and 1 failed. The sixty-minute equivalent
  long-run test passed (`3,600` equivalent seconds, `42,188` calls,
  `172,802,048` frames) with no buffer growth or state loss. The existing
  processor allocation benchmark failed its unchanged 16-byte/call threshold
  for `graphic-moderate` at 29.192 bytes/call.
- `.\gradlew.bat --no-daemon '-Dequalizer.performance=true' :app:testDebugUnitTest --tests 'com.example.cdplaya.player.equalizer.performance.EqualizerProcessorBenchmarkTest' --rerun-tasks --stacktrace`
  reproduced the unchanged assertion in 2m26s, this time for
  `graphic-worst-high-rate-surround` at 29.328 bytes/call. Its median real-time
  factor was 0.0682 and maximum was 0.1116. The scenario-to-scenario movement
  indicates JVM allocation-measurement sensitivity, but the failure is retained
  as a real outstanding verification result. No unrelated performance code or
  assertion was changed.

## 10. Device validation

| Device | Android/API | APK SHA-256 | Route and scenarios | Result / not tested |
|---|---|---|---|---|
| Samsung Galaxy S22 Ultra `SM-S908U1` | Android 16 / API 36 | debug `2B529E6D14A9514BCE37C0A4A90444ADD4A930A4B98802053EF4FD2846DF1FFB` | Built-in speaker; 49 connected cases, four feasibility cases, exact final APK cold launch | Install succeeded; cold launch succeeded in 793 ms; process remained alive; no fatal startup log. No USB DAC, USB route, mixer set/query/listener, playback fixture, disconnect/reconnect, DAC display, or audible-gap result. |
| Samsung Galaxy S9+ | Not connected / expected Android 10 target | Not installed | None | Physical API-29 regression not performed. |
| Samsung Galaxy Tab S9 | Not connected; version not inferred | Not installed | None | Additional modern-device regression not performed. |

Final APK artifacts:

| Artifact | SHA-256 |
|---|---|
| `app-debug.apk` | `2B529E6D14A9514BCE37C0A4A90444ADD4A930A4B98802053EF4FD2846DF1FFB` |
| `app-release-unsigned.apk` | `75F0581D364807C3DEBE20631FA62764D9A2935CC3C8E035F420BDB55BC7D837` |
| `app-benchmark.apk` | `C6A213F0BF4AE4867CD77E53C898D67C72A4ECBA01B0E5B2B39A9577962429F8` |
| `benchmark-benchmarkBenchmark.apk` | `2AE21E05061C75B0AECBED05BCFDEF4FD4007F0A1B61E752C526D168B4F6913B` |
| `app-debug-androidTest.apk` | `E1B50BAA4634CE2A3784D3F6FBCB9AD67A7F2C3EC12C53666756AD13C280DEB5` |

## 11. Files changed

| File | Purpose and runtime impact | Scope / Phase A reason |
|---|---|---|
| `app/src/main/AndroidManifest.xml` | Adds normal `MODIFY_AUDIO_SETTINGS`; no runtime prompt. | Production manifest contract required by the probed API. |
| `app/src/main/java/com/example/cdplaya/player/PlaybackService.kt` | Owns the single provider/controller, explicit activation, state-preserving output recreation, and cleanup callbacks. | Probe invocation is debug-rejected outside debug builds; authoritative ownership remains production-safe. |
| `app/src/main/java/com/example/cdplaya/player/equalizer/EqualizerAudioProcessor.kt` | Publishes input/output format and buffer count only while observing; DSP is unchanged. | Low-cost dormant production instrumentation needed to answer the processor boundary question. |
| `app/src/main/java/com/example/cdplaya/player/equalizer/EqualizerRenderersFactory.kt` | Installs the forwarding provider in the existing sink. | Dormant production-safe observation point; no second sink/player. |
| `app/src/main/java/com/example/cdplaya/player/feasibility/BitPerfectFeasibilityModels.kt` | Framework-free session evidence and enums. | Non-persisted Phase A facts. |
| `app/src/main/java/com/example/cdplaya/player/feasibility/BitPerfectFeasibilityReportFormatter.kt` | Produces sanitized copied diagnostics. | Experimental/debug reporting without raw identifiers. |
| `app/src/main/java/com/example/cdplaya/player/feasibility/BitPerfectFeasibilityRuntimeBridge.kt` | Bounded in-process state and service control bridge. | Dormant unless explicitly invoked; no ownership duplication. |
| `app/src/main/java/com/example/cdplaya/player/feasibility/ExactMixerAttributeMatcher.kt` | Strict encoding/rate/mask/behavior matching. | Pure Phase A eligibility evidence. |
| `app/src/main/java/com/example/cdplaya/player/feasibility/FeasibilityAudioOutputProvider.kt` | Delegates Media3 behavior while observing format, real AudioTrack, reuse, and lifecycle. | Production-safe forwarding seam; activation remains experimental. |
| `app/src/main/java/com/example/cdplaya/player/feasibility/UsbMixerFeasibilityController.kt` | API-gated USB enumeration, set/query/listen/clear, and exception-safe cleanup. | Experimental Android 14+ probe with API-29-safe loading. |
| `app/src/main/java/com/example/cdplaya/ui/settings/DiagnosticsScreen.kt` | Adds explicit debug-only Observe, Exact probe, Stop/Clear, and Copy controls. | No preference, badge, or production claim. |
| `app/src/test/java/com/example/cdplaya/player/feasibility/BitPerfectFeasibilityArchitectureTest.kt` | Enforces one service/player/session and no production preference/badge. | Phase A scope guard. |
| `app/src/test/java/com/example/cdplaya/player/feasibility/BitPerfectFeasibilityStateTest.kt` | Tests safe defaults, immutability, bounded events, and exact actual-track confirmation. | Phase A state integrity. |
| `app/src/test/java/com/example/cdplaya/player/feasibility/DeterministicWavFixtureGeneratorTest.kt` | Generates and hashes copyright-free fixtures. | Test-only evidence; no APK asset. |
| `app/src/test/java/com/example/cdplaya/player/feasibility/ExactMixerAttributeMatcherTest.kt` | Covers exact matches and every mismatch/ambiguity class. | Pure Phase A selection proof. |
| `app/src/test/java/com/example/cdplaya/player/feasibility/FeasibilityAudioOutputProviderTest.kt` | Covers delegation, snapshots, reuse, lifecycle, and failure publication. | Phase A observer verification. |
| `app/src/test/java/com/example/cdplaya/player/feasibility/UsbMixerFeasibilityControllerTest.kt` | Covers SDK/device rejection, set/listener/query failures, and idempotent cleanup. | Hardware-independent cleanup proof. |
| `app/src/androidTest/java/com/example/cdplaya/player/feasibility/BitPerfectFeasibilityInstrumentationTest.kt` | Exercises real Media3 sink/provider and actual AudioTrack creation plus safe API-36 USB inspection. | Device-only Phase A boundary evidence. |
| `docs/audio/phase-a-bit-perfect-feasibility.md` | Records contracts, matrices, commands, caveats, hashes, and architecture decision. | Phase A deliverable; no runtime impact. |

## 12. Privacy and cleanup audit

- No USB address, serial, stable ID, URI, file path, or product name is stored in
  feasibility state or copied diagnostics.
- No raw device identifier is copied to the clipboard or committed.
- Only safe route categories leave the Android adapter/provider.
- Product labels are intentionally omitted from copied evidence.
- Preferred mixer state is media-usage/device-pair scoped, session-only, and
  cleared on success and failure paths.
- Listener removal and provider/output release are idempotent and tested.
- Service destruction, USB removal, playback error/end, explicit stop, and
  output release call cleanup.
- Startup clears a stale media preferred attribute only for USB outputs visible
  to the same UID; it does not clear arbitrary non-USB or untracked pairs.
- No USB output was connected and no preferred mixer attribute was discovered
  or set during this run, so no stale attribute remained from validation.

## 13. Known limitations

- No USB DAC was connected; supported attributes, exact matches, set/query
  results, callbacks, USB route, disconnect/reconnect, audible gaps, and DAC
  display evidence remain untested.
- No physical Android 10 or Tab S9 regression was possible in this run.
- No decoded FLAC/WAV fixture was played through a service-owned ExoPlayer queue;
  the decisive comparisons isolate the post-decoder raw-PCM sink/provider
  boundary.
- Processor-backed cases configure the real sink but do not create or feed the
  AudioTrack. Processor-free cases create and inspect actual AudioTracks.
- Queue/history/session continuity across controlled reconstruction is not
  implemented or proven; Phase B must prove it before production adoption.
- The stock Media3 1.10.1 sink cannot preserve packed high-resolution integer PCM
  while retaining the ordinary processor chain, and its float path is not
  equivalent to packed integer PCM.
- Audible gap/click/pop behavior requires a connected DAC and manual playback.
- The existing opt-in processor allocation microbenchmark failed its unchanged
  16-byte/call threshold twice in different scenarios; this Phase A did not
  change DSP performance code or weaken the assertion.
- Phase B should wait for supported USB hardware if it is expected to claim
  device/HAL activation. The application-architecture prototype can proceed,
  but production eligibility and UX cannot be approved from this run alone.

## 14. Final result

Phase A result: APP ARCHITECTURE FEASIBLE — SUPPORTED USB HARDWARE STILL REQUIRED
