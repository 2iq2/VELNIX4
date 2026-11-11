# VELNIX 4.3.0

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
- XMLTV parser for programme channel IDs, titles, descriptions, and UTC-normalized start/end timestamps.
- Local XMLTV file import and persistence, matching programme data to M3U channels by `tvg-id`, with current/next programme labels and a live progress indicator on channel cards.
- Multi-day EPG guide screen with day navigation, channel programme schedules, local-time display, programme descriptions, and a one-tap action to open the channel player.
- Local programme reminders with a five-minute advance notification, Android notification permission handling, and reminder cancellation.
- Supabase email/password sign-up and sign-in using the project publishable key; access/refresh session tokens are stored in app-private preferences, with refresh attempted when the access token is near expiry.
- Supabase password-recovery email request flow.
- Account screen with signed-in email, sign-out, local display-name editing, and explicit free-plan status.

## Build
Open the repository in Android Studio with JDK 17 and Android SDK Platform 35. Sync Gradle and run the app configuration.

## Current limitations
- The repository does not yet include a verified Gradle wrapper or a successful CI build. No APK is being claimed as built or tested.
- Demo channel names are placeholders; no TV streams or channel credentials are bundled.
- M3U import currently reads a local playlist file. Remote playlist URL/Xtream login support is not implemented.
- XMLTV import currently supports local XML files; remote EPG URL refresh remains pending. Android may delay inexact alarms due to battery optimization, and scheduled reminders are not yet restored after a device reboot.
- Password recovery currently requests the email only; the native deep-link flow to complete password replacement still needs implementation and Supabase redirect configuration.
- Cloud profile metadata synchronization, paid subscription entitlements, payment processing, Android TV / Google TV navigation, adaptive tablet layouts, and release QA remain pending.
- The player supports common HTTP(S) HLS/DASH stream URLs; DRM, unusual codecs, RTSP, and provider-specific authentication need separate implementation and device testing.

## Security and distribution
- Use only stream URLs and playlists you are authorized to access.
- Never commit IPTV credentials, Supabase service-role keys, private API keys, or customer data.
- APK size depends on real features, assets, native libraries, and ABI packaging. No artificial padding will be added to inflate it.

## Roadmap
1. Complete playlist and channel management, including duplicate handling and robust import feedback.
2. Remote EPG refresh and reliable reminder restoration.
3. Complete Supabase recovery deep links and synchronize profile metadata.
4. Add subscription tables, secure entitlement checks, and payment integration only after a provider is selected.
5. Android TV / Google TV navigation and adaptive layouts.
6. Automated build, device testing, and signed release packaging.
