# KMB Downloader 🚀

**Modern Android video downloader** with a unique workflow:

> **Copy any social media link anywhere on your phone → it automatically appears inside KMB with video title, thumbnail preview, date & time. Download later when you are free.**

---

## ✨ Features & Architecture

- **Auto-saved clipboard links** – the killer feature
  - Just copy a YouTube / TikTok / Instagram / Facebook / X / Reddit link anywhere on your phone.
  - Open KMB (or it auto-checks on return) — URL tracking parameters are automatically cleaned and link is parsed.
  - Video title, thumbnail preview, and duration are loaded automatically.
  - Download whenever you want with one tap.
- **In-App Media Playback & Sharing**:
  - Direct 1-tap playback via default Android video player (`FileProvider`).
  - Share completed downloads directly to WhatsApp, Telegram, etc.
- **Over-The-Air yt-dlp Engine Updater**:
  - Built-in update button to pull the latest `yt-dlp` stable binary without needing to rebuild or reinstall the APK!
- **Download Management**:
  - Cancel downloads in progress directly from notification or in-app UI.
  - Delete completed downloads from history with automatic file cleanup on disk.
- **Scoped Storage & Modern Android Compatible**:
  - Safe for Android 11 through Android 15 (API 35).
  - Proper Android 14+ foreground service lifecycle (`dataSync`).
  - Uses `MediaScannerConnection` so videos automatically index into Gallery / Photos.
- **UI & Animations**:
  - Jetpack Compose Material 3 dark/glow theme.
  - Pulsing glow effects, scale-on-press cards, animated splash screen.
  - Real-time download progress bar and notification.

---

## 📱 Supported Platforms

YouTube • TikTok • Instagram (Reels/Posts) • Facebook • X/Twitter • Reddit • Vimeo • and many more via yt-dlp

---

## 🛠️ How to Build

1. Open the project in **Android Studio** (Ladybug / 2024.2+)
2. Let Gradle sync and download dependencies.
3. Run on device or emulator.

APK location after build:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🛡️ Privacy & Permissions

- **Clipboard**: Checked when app is in the foreground (compliant with Android 10+ privacy policies).
- **Notifications**: Requests `POST_NOTIFICATIONS` runtime permission on Android 13+ to provide live progress bars and cancellation controls.
- **Storage**: Uses app-specific external storage with fallback to public Downloads, fully compliant with modern Scoped Storage rules.
