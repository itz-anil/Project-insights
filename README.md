# NetPulse — Network Speed Meter (Samsung Galaxy Tab A9)

Status bar में live network speed (MB/s) दिखाने वाला Android app, iPhone जैसे glassy look के साथ।

## Features (v1.0.0) ✅

| Feature | विवरण |
|---|---|
| **Status bar speed** — *mandatory, हमेशा ON* | हर 1 second, MB/s (कम होने पर KB/s); style: Total / Download / Down + Up |
| Notification panel — *optional* | ↓/↑ speed + आज का Wi-Fi/Mobile data; off = एक छोटी silent line |
| Dashboard | Live speed, 60-second graph, आज का usage, last ping, Wi-Fi/LTE details |
| **Data usage** | Billing cycle का Wi-Fi vs Mobile, 14 दिन का chart, top apps (icon के साथ) |
| **Data limit alerts** | Daily limit (सभी networks) और monthly mobile limit — 80% और 100% पर notification |
| **Speed test** | Download, upload, ping, jitter (Cloudflare servers) + पिछले 30 results की history |
| **Floating bubble** — *optional* | Videos/games के ऊपर draggable ↓/↑ speed pill |
| **Quick Settings tile** | Shade में live speed; tap करने पर bubble on/off |
| **Home screen widget** | 4×2 glass widget: download, upload, आज का usage |
| Battery बचत | Screen off पर pause; boot पर auto-start |
| Samsung setup checklist | Notification, battery, status bar icons, usage access, overlay |
| UI | Glass look, Plus Jakarta Sans (text) + JetBrains Mono (numbers), tablet के लिए wide layout |

## Permissions क्यों चाहिए

| Permission | किसलिए |
|---|---|
| Notifications | Status bar speed icon और alerts |
| Usage access | Data usage, per-app usage, limit alerts (Android के अपने आँकड़े) |
| Display over other apps | सिर्फ़ floating bubble के लिए |
| Internet | सिर्फ़ speed test के लिए — कोई ads/tracking नहीं, सारा data device पर रहता है |

## Tab पर install कैसे करें

1. `NetPulse-v1.0.0.apk` (signed release, ~1.3 MB) Tab पर copy करें।
   पहले वाला test version install है तो **पहले उसे uninstall करें** (signing key अलग है)।
2. Tab पर APK खोलें → "Install unknown apps" की permission दें → Install।
3. App खोलें → Notification permission **Allow** करें।
4. **Settings → Samsung setup** checklist पूरी करें, खासकर:
   - Battery → **Unrestricted** और *Never sleeping apps* में NetPulse
   - Settings › Notifications › Advanced settings › **Show notification icons → All**
   - **Usage access** (Data usage screen के लिए)
5. Settings › Extras से Quick Settings tile और Home widget जोड़ें।

## Build (developer)

```bash
./gradlew assembleDebug        # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease      # signed अगर signing/keystore.properties मौजूद है
./gradlew testDebugUnitTest    # unit tests
```

**Release signing:** `signing/` folder (keystore + password) git में नहीं है — इसे सुरक्षित रखें।
आगे के updates इसी key से sign होंगे, वरना पुराना app uninstall करना पड़ेगा।

Requirements: JDK 17+, Android SDK 35. Min Android 8.0 (API 26), target API 35.

## Architecture

```
SpeedMeterService (foreground, specialUse)
  ├─ TrafficSampler      → TrafficStats से हर second bytes/sec
  ├─ SpeedIconRenderer   → speed text को bitmap पर बनाता है (status bar icon)
  ├─ MeterNotifications  → notification (details optional)
  ├─ FloatingBubble      → overlay (optional)
  ├─ SpeedWidget         → home screen widget update
  ├─ DataAlerts          → limit notifications (हर minute check)
  └─ SpeedRepository     → UI के लिए live StateFlow
SpeedTileService         → Quick Settings tile
MainActivity (Compose)
  ├─ Dashboard / Usage / SpeedTest / Settings screens
  └─ MainViewModel ← SettingsRepository, UsageRepository (NetworkStatsManager),
                     SpeedTester + SpeedTestRepository (DataStore)
```

**Status bar icon monochrome क्यों है?** Android सभी status bar icons को एक रंग (white/black)
में tint करता है, इसलिए glass look app, notification और widgets में है।

## Fonts

Plus Jakarta Sans और JetBrains Mono — दोनों SIL Open Font License 1.1 के तहत
(`third_party/fonts/` में license files)।
