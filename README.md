# MotoGauge

Auto-rotating motorcycle dashboard for Android with a large GPS speedometer and side-by-side left/right lean indicators.

## Features

- GPS speed in km/h (no account, maps, or internet required)
- Screen-aligned fused orientation from Android's rotation-vector sensor
- Current left/right lean plus session maximums
- One-tap upright calibration
- Automatic portrait/landscape layout and orientation-independent lean axis
- Stationary GPS drift below 3 km/h suppressed to zero
- Keeps the display awake while riding

> **Safety:** The lean figure is an estimate from the phone sensors. Mount flex, vibration and sensor fusion can affect it. Do not look at or operate the phone while riding.

## Build

Requires JDK 17, Android SDK 35 and Gradle 8.9.

```bash
gradle :app:assembleRelease
```

The installable APK is generated at `app/build/outputs/apk/release/app-release.apk`.

## Use

Mount the phone securely facing the rider in portrait or landscape, open MotoGauge, grant precise location access and enable GPS. Hold the motorcycle upright and tap **CALIBRATE UPRIGHT** before riding. If you rotate the phone, the app detects the layout change and re-zeros automatically.

## License

MIT
