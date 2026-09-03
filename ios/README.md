# MotoSpeed for iPhone

Native SwiftUI GPS speedometer for iPhone (iOS 16 or newer).

## Features

- Large GPS speed display in portrait and landscape
- GPS drift below 3 km/h displayed as zero
- White through 80, green 81-99, yellow 100-109, orange 110-119, red 120-129
- Flashing red display at 130 km/h and above
- Automatic 0-100 km/h timer: ready while stopped, starts above 3 km/h, freezes at 100 km/h, rearms after two seconds stopped
- Maximum speed and GPS accuracy
- Screen remains awake while the app is active

## Free manual installation

The IPA produced by GitHub Actions is unsigned. Use Sideloadly on Windows or macOS to sign it with your own Apple ID and install it on a connected iPhone. A free Apple ID installation normally expires after seven days and must then be refreshed/reinstalled.

Alternatively, open `MotoSpeed.xcodeproj` in Xcode on a Mac, choose your Personal Team under Signing & Capabilities, connect the iPhone, and press Run.

For easy permanent distribution to other people, sign and publish through TestFlight or the App Store with an Apple Developer Program membership.
