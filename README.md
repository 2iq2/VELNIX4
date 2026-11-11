# VELNIX 4.0

Standalone native Android IPTV application. This repository is independent from the legacy VELNIX project.

## Implemented foundation
- Kotlin + Jetpack Compose / Material 3 dark dashboard.
- Channel search and category filtering.
- Local M3U file import through Android's document picker.
- EXTINF parsing for channel name, tvg-id, logo URL, group title, and stream URL.
- Imported playlist persistence on the device.
- Local favorites persistence.
- Media3 ExoPlayer integration for HTTP(S) HLS (.m3u8) and DASH (.mpd) streams.
- Playback error reporting and lifecycle-aware pause/resume.
- EPG programme data model with UTC timestamps and progress calculation.

## Build
Open the repository in Android Studio with JDK 17 and Android SDK Platform 35. Sync Gradle and run the app configuration.

## Current limitations
- The repository does not yet include a verified Gradle wrapper or a successful CI build. No APK is being claimed as built or tested.
- Demo channel names are placeholders; no TV streams or channel credentials are bundled.
- M3U import currently reads a local playlist file. Remote playlist URL/Xtream login support is not implemented.
- The EPG work is currently a data model only; XMLTV ingestion, now/next guide, and reminders remain pending.
- Account/subscription backend integration, Android TV / Google TV navigation, adaptive tablet layouts, and release QA remain pending.
- The player supports common HTTP(S) HLS/DASH stream URLs; DRM, unusual codecs, RTSP, and provider-specific authentication need separate implementation and device testing.

## Security and distribution
- Use only stream URLs and playlists you are authorized to access.
- Never commit IPTV credentials, Supabase service-role keys, private API keys, or customer data.
- APK size depends on real features, assets, native libraries, and ABI packaging. No artificial padding will be added to inflate it.

## Roadmap
1. Complete playlist and channel management, including duplicate handling and robust import feedback.
2. EPG XMLTV ingestion, now/next guide, and reminders.
3. Account/subscription backend integration when configuration is supplied securely.
4. Android TV / Google TV navigation and adaptive layouts.
5. Automated build, device testing, and signed release packaging.
