# Gallery (with ASsound audio engine)

A photo/video gallery app with a built-in system-wide audio equalizer,
built by merging two projects into one APK.

## What this app is

- **Home / core app**: the original Gallery app (photos, videos, crop,
  frame capture, fullscreen video playback, wallpaper, YouTube tab,
  file management, etc.) — unchanged, still the app you land on when
  you open it.
- **Audio Equalizer**: the old in-gallery equalizer screen has been
  removed. In its place, the 3-dot menu → **Audio Equalizer** now
  opens **ASsound**, a full Compose-based audio engine (formerly a
  separate app called "Tunex"), bundled into this same APK. All of
  ASsound's original functionality — 10-band EQ, sound profiles, bass
  boost, virtualizer, reverb, loudness enhancer, background service,
  etc. — works exactly as it did standalone. Only its entry point
  changed: it's now reached from Gallery's menu instead of being its
  own launcher icon.
- **App identity**: the app's name, icon, and launcher come from the
  original Gallery app. There is only one launcher icon / one app on
  the device — ASsound is a screen inside it, not a separate app.

## Project layout

```
app/src/main/java/com/elvira/gallery/   Gallery feature set (Java)
app/src/main/java/com/assound/          ASsound audio engine (Kotlin/Compose,
                                         formerly com.tunex)
```

Both packages live in the same Gradle module (`app`) and compile into
a single APK with applicationId `com.elvira.gallery`.

## How the two are wired together

- Gallery's `MainActivity` 3-dot menu → **Audio Equalizer** item now
  starts `com.assound.MainActivity` (ASsound's home screen) instead of
  the old `EqualizerActivity`.
- Gallery's own video player already broadcasts
  `AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION` /
  `ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION` for its playback
  sessions (this used to be picked up by Gallery's own
  `GlobalEqualizerService`, which has been removed). ASsound's
  `AudioEffectReceiver` + `AudioProcessingService` listen for those
  same system broadcasts, so Gallery's in-app video audio is
  automatically shaped by ASsound's equalizer with no extra wiring.
- ASsound's own boot-start behavior (`BootReceiver`) is preserved as-is.

## Building

This project is meant to be built with its own Gradle wrapper (works
well in Termux):

```bash
./gradlew assembleDebug
```

APKs (per-ABI + universal) will be in `app/build/outputs/apk/debug/`.

A GitHub Actions workflow is included at
`.github/workflows/build.yml` (adapted from the original Gallery
project's workflow) that builds a debug APK on every push and uploads
it as a build artifact.

## Notes / things worth double-checking after your first build

- This merge was done by hand-editing and combining the two source
  trees; it has **not** been compiled in this environment (no network
  access to fetch Gradle/AGP/dependencies here). Please run a build
  and fix any small issues Gradle reports — most likely candidates are
  dependency version mismatches, since two independent dependency
  sets were combined.
- `minSdk` was set to **29** (ASsound's original requirement, higher
  than Gallery's original 26) so the audio-effect session APIs behave
  consistently. If you need to support older devices down to API 26,
  you'll need to verify ASsound's audio code still works there and
  lower `minSdk` in `app/build.gradle`.
- Release builds have `minifyEnabled true` / `shrinkResources true`
  (inherited from ASsound's original release config). `proguard-rules.pro`
  has a broad keep-rule for `com.elvira.gallery.**` to avoid
  accidentally stripping Gallery's classes, but it's worth testing a
  release build specifically before publishing.
