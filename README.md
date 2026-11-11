# VELNIX 4.0

A standalone Android IPTV application project. This repository is independent from the legacy VELNIX repository.

## Project status
Initial native Android foundation. The current build contains the Compose dashboard shell and project configuration. Real IPTV source import, authenticated accounts, subscriptions, EPG synchronization, and production playback are separate implementation milestones and are not represented as completed features yet.

## Stack
- Kotlin
- Jetpack Compose + Material 3
- MVVM-ready package structure
- AndroidX Media3 dependencies reserved for the playback layer

## Build
Open this repository in the latest stable Android Studio with Android SDK Platform 35 installed, then sync Gradle and run the `app` configuration.

## Security
Never commit IPTV credentials, Supabase service-role keys, private API keys, or customer data. Use local properties or a secure backend for secrets.

## Planned milestones
1. Native UI foundation and responsive phone/tablet layouts.
2. Media3 HLS/DASH playback and player lifecycle.
3. M3U and Xtream-compatible source integration.
4. EPG, favorites, reminders, and search.
5. Account/subscription integration and production QA.
6. Android TV / Google TV navigation and release packaging.

The APK size is determined by real assets, native libraries, and packaging configuration; this project will not use artificial padding to inflate it.
