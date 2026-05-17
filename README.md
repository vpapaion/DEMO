# Vaios Android Developer Demo

A compact native Android demo app designed for a GitHub portfolio. The goal is not to be a large product, but to show Android programming discipline in a small codebase that a reviewer can inspect quickly.

## What the app demonstrates

- Native Android `Activity` lifecycle.
- Programmatic UI without drag-and-drop layout builders.
- Custom drawing through `SkillRadarView`.
- Separation into `domain`, `data`, `ai` and `ui` packages.
- Local persistence with `SharedPreferences`.
- Background execution with `ExecutorService` and safe UI updates through `Handler`.
- Share intent integration.
- GitHub Actions CI that builds an installable APK automatically.

## Main demo idea

The app presents a small Android developer portfolio dashboard. It contains a local AI-style reviewer that scores the technical maturity of the demo and suggests concrete improvements such as Jetpack Compose, Room, Retrofit, WorkManager and UI tests.

The AI part is intentionally local and deterministic. It does not call an external API, so the app can run offline and can be reviewed easily.

## Build locally

```bash
gradle --no-daemon assembleDebug
```

The APK will be created here:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Build on GitHub

The repository includes `.github/workflows/android-build.yml`.

After every push, GitHub Actions builds the APK and uploads it as an artifact named:

```text
Vaios-Android-Demo-debug-apk
```

To download it:

1. Open the repository on GitHub.
2. Go to **Actions**.
3. Open the latest successful workflow run.
4. Download the artifact **Vaios-Android-Demo-debug-apk**.
5. Extract the zip and install the `.apk` on your Android phone.

## Suggested next upgrades

- Convert UI to Kotlin + Jetpack Compose.
- Add Room database and migration tests.
- Add Retrofit with a mock GitHub API datasource.
- Add WorkManager for background sync.
- Add unit tests and UI tests.
- Add release signing and GitHub Releases.
