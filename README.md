# VELNIX 4.0

Standalone Android IPTV application. This repository is separate from the legacy VELNIX repository.

## Current foundation
- Kotlin + Jetpack Compose / Material 3.
- Dark VELNIX dashboard with channel search, category filters, and favorites (in-memory for this milestone).
- Media3 ExoPlayer integration for user-supplied HTTP(S) HLS (.m3u8) and DASH (.mpd) streams.
- Basic M3U playlist parser for EXTINF entries.
- EPG programme data model with UTC timestamps and progress calculation.

## Build
Open the repository in Android Studio with JDK 17 and Android SDK Platform 35. Sync Gradle and run the app configuration.

## Important
- Demo channel names are placeholders; no TV streams or channel credentials are bundled.
- Use only stream URLs and playlists you are authorized to access.
- The player currently accepts one stream URL manually. Playlist import, persistence, Xtream integration, EPG ingestion, accounts, subscriptions, Android TV UX, and release QA remain future milestones.
- Never commit IPTV credentials, Supabase service-role keys, private API keys, or customer data.
- APK size depends on actual assets, native libraries, and ABI packaging. No artificial padding will be added to inflate it.

## Roadmap
1. Native dashboard and adaptive layouts.
2. HLS/DASH playback (initial integration present).
3. M3U import workflow and channel persistence.
4. EPG ingestion, now/next guide, and reminders.
5. Account/subscription backend integration.
6. Android TV / Google TV navigation and release validation.
