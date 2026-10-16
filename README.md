# NetPulse — Network Speed Meter (Samsung Galaxy Tab A9)

Status bar में live network speed (MB/s) दिखाने वाला Android app, iPhone जैसे glassy look के साथ।

## Phase 1 (यह version) ✅

| Feature | Status |
|---|---|
| **Status bar speed** (हर 1 second) — *mandatory, हमेशा ON* | ✅ |
| Icon style: Total / Download / Down + Up | ✅ |
| Notification panel details — *optional* (↓/↑ speed + session data) | ✅ |
| Dashboard: live speed, 60-second graph, Wi-Fi/LTE details | ✅ |
| Screen off होने पर pause (battery बचत) | ✅ |
| Boot पर auto-start | ✅ |
| Samsung setup checklist (notification, battery, status bar icons) | ✅ |
| Glass UI + fonts: Plus Jakarta Sans (UI), JetBrains Mono (numbers) | ✅ |

## Phase 2 (अगला)

- Data usage history (Wi-Fi vs Mobile, daily/monthly) + per-app usage
- Mobile data limit alerts
- Speed test (download, upload, ping, jitter) + history
- Floating bubble, Quick Settings tile, Home screen widget

## Tab पर install कैसे करें

1. GitHub पर **Actions → Android build** खोलें, latest run से `NetPulse-debug-apk` download करें
   (या Android Studio से खुद build करें)।
2. Tab पर APK खोलें → "Install unknown apps" की permission दें → Install।
3. App खोलें → Notification permission **Allow** करें।
4. **Settings → Samsung setup** checklist पूरी करें, खासकर:
   - Battery → **Unrestricted** और *Never sleeping apps* में NetPulse
   - Settings › Notifications › Advanced settings › **Show notification icons → All**

## Build (developer)

```bash
./gradlew assembleDebug        # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # unit tests
```

Requirements: JDK 17+, Android SDK 35. Min Android 8.0 (API 26), target API 35.

## Architecture

```
SpeedMeterService (foreground, specialUse)
  ├─ TrafficSampler      → TrafficStats से हर second bytes/sec
  ├─ SpeedIconRenderer   → speed text को bitmap पर बनाता है (status bar icon)
  ├─ MeterNotifications  → notification (details optional)
  └─ SpeedRepository     → UI के लिए live StateFlow
MainActivity (Compose)
  ├─ DashboardScreen / SettingsScreen
  └─ MainViewModel ← SettingsRepository (DataStore)
```

**Status bar icon monochrome क्यों है?** Android सभी status bar icons को एक रंग (white/black)
में tint करता है, इसलिए glass look app, notification और widgets में है।

## Fonts

Plus Jakarta Sans और JetBrains Mono — दोनों SIL Open Font License 1.1 के तहत
(`third_party/fonts/` में license files)।
