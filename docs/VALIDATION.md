# Validation

Validation date: 2026-10-22. Device: TCL Smart TV Pro (G08), Google TV / Android 14, with the existing 1920x1080 display override. The panel reports 3840x2160; native 4K rendering on this device has not been separately validated.

## Executed checks

| Check | Observed result |
|---|---|
| `gradlew.bat --offline --no-daemon --max-workers=2 testDebugUnitTest lintRelease assembleRelease` | BUILD SUCCESSFUL; 28 JVM tests passed |
| Final launcher-art build: `gradlew.bat --offline --no-daemon --max-workers=2 lintRelease assembleRelease` | BUILD SUCCESSFUL |
| Release lint | Zero errors; one `ViewConstructor` warning for the programmatically created DvdView, which is not inflated from layout XML |
| APK permissions | `aapt dump permissions` lists the package and no requested permissions |
| APK signature | `apksigner verify --verbose` succeeds; APK Signature Scheme v2 |
| Independent production-Java comparison with decoded footage | 27 samples, including 18 held-out frames; maximum position error 0 px and maximum channel error 1 RGB level |
| Actual installed preview captures | Seven ticks: 0, 108, 231, 600, 1500, 5555, 8765; all bounding boxes match, maximum channel error 1, minimum foreground-mask intersection-over-union 98.9458% |
| Live preview | Motion observed; injected Back dismissed the preview |
| Device preservation | Original Pipes screensaver component and enabled setting unchanged; original asleep state restored after preview |

The final installed APK is 42,228 bytes. SHA-256:

```text
172ef0ca1b28e9c2c2c45592e9e06aa513605c0606a7c2741a9bd9bd55a3d0cf
```

The release uses the local debug signing key for sideloading. It is not a Play Store release, and signing keys are not published.

The JVM tests cover reflections, corners, extreme elapsed times, measured color progression, reference fixtures, pause/resume, callback cancellation, refresh-rate-independent simulation, viewport fitting and malformed capture extras. `tools/check_reference.py` compiles the production Java simulation and compares it with committed measurements from the original footage. These measurements provide evidence independent of a second implementation of the motion formula.

`tools/verify_tv_preview.py` waits for both position and color to settle before scoring device captures. Early attempts captured the Android transition fade or checked sleep while the device was still entering sleep; those attempts failed and were retained locally. The final run passed after readiness checks were corrected. Source footage, full logs and device evidence stay in ignored `work/`; the public preview image is an actual app capture.

## Limits and unverified checks

- The new DreamService is built and shares the tested renderer, but its actual activation and automatic idle startup were not tested.
- Real-device D-pad keys, other TVs, native 4K output, sustained frame pacing, long-duration memory use and power consumption have not been measured. JVM checks establish simulation timing, not a hardware performance benchmark.
- The final clipped reference frame at 150 seconds differs by 10 px / two motion ticks. Its cause remains unresolved; it is preserved as an excluded diagnostic. No claim of complete ten-hour frame identity is made.
- Only 78 opening color states are measured. Subsequent colors are deterministic generated pastels; the source's full random sequence is unknown.
