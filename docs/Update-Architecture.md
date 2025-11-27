# In-App Update System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        K2Look App                                │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     1. APP LAUNCH                                │
│                                                                  │
│  MainActivity.onCreate()                                         │
│         │                                                        │
│         ├─→ Auto-check enabled? ────No───→ Skip                 │
│         │                                                        │
│         └─→ Yes                                                  │
│              │                                                   │
│              ├─→ Last check < 24hrs? ──Yes──→ Skip              │
│              │                                                   │
│              └─→ No                                              │
│                   │                                              │
│                   └─→ UpdateChecker.checkForUpdate()            │
│                        │                                         │
│                        ├─→ Query GitHub API                      │
│                        ├─→ Parse release JSON                    │
│                        └─→ Compare versions                      │
│                             │                                    │
│                             ├─→ No update → End                  │
│                             │                                    │
│                             └─→ Update found                     │
│                                  │                               │
│                                  └─→ Already dismissed?          │
│                                       │                          │
│                                       ├─→ Yes → Skip             │
│                                       │                          │
│                                       └─→ No                     │
│                                            │                     │
│                                            └─→ Show Notification │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                  2. MANUAL CHECK (About Tab)                     │
│                                                                  │
│  User taps "Check for Updates"                                   │
│         │                                                        │
│         └─→ UpdateChecker.checkForUpdate()                      │
│              │                                                   │
│              ├─→ Show "Checking..." dialog                       │
│              │                                                   │
│              └─→ GitHub API Response                             │
│                   │                                              │
│                   ├─→ No update                                  │
│                   │    └─→ Show "You're up to date" dialog       │
│                   │                                              │
│                   └─→ Update available                           │
│                        └─→ Show Update Dialog                    │
│                             ├─ Version                           │
│                             ├─ Release notes                     │
│                             └─ Download / Later buttons          │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    3. DOWNLOAD & INSTALL                         │
│                                                                  │
│  User taps "Download"                                            │
│         │                                                        │
│         └─→ UpdateDownloader.downloadUpdate()                   │
│              │                                                   │
│              ├─→ DownloadManager.enqueue()                       │
│              │    ├─ Title: "K2Look Update"                      │
│              │    ├─ Destination: Downloads folder               │
│              │    └─ Show notification                           │
│              │                                                   │
│              └─→ Wait for download complete                      │
│                   │                                              │
│                   └─→ BroadcastReceiver triggered                │
│                        │                                         │
│                        ├─→ Check download status                 │
│                        │                                         │
│                        └─→ Success                               │
│                             │                                    │
│                             └─→ installApk()                     │
│                                  │                               │
│                                  ├─→ FileProvider.getUriForFile()│
│                                  │                               │
│                                  └─→ Intent.ACTION_VIEW          │
│                                       │                          │
│                                       └─→ System Installer Opens │
│                                            │                     │
│                                            └─→ User installs APK │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                   4. USER ACTIONS & STATES                       │
│                                                                  │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐   │
│  │   Latest     │     │   Update     │     │  Downloading │   │
│  │   Version    │────→│  Available   │────→│      APK     │   │
│  └──────────────┘     └──────────────┘     └──────────────┘   │
│         ↑                    │                      │          │
│         │                    │                      ↓          │
│         │                    ↓              ┌──────────────┐   │
│         │             ┌──────────────┐      │   Ready to   │   │
│         └─────────────│   Dismissed  │      │   Install    │   │
│                       └──────────────┘      └──────────────┘   │
│                              │                      │          │
│                              └──────────────────────┘          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     5. DATA FLOW                                 │
│                                                                  │
│  GitHub API                                                      │
│      │                                                           │
│      │ JSON Response                                             │
│      ↓                                                           │
│  UpdateChecker                                                   │
│      │                                                           │
│      │ AppUpdate object                                          │
│      ↓                                                           │
│  ┌──────────────┐                                               │
│  │  AboutTab    │ ←─────────────────┐                          │
│  │  (UI)        │                    │                          │
│  └──────────────┘                    │                          │
│      │                               │                          │
│      │ User action                   │                          │
│      ↓                               │                          │
│  UpdateDownloader                    │                          │
│      │                               │                          │
│      │ Download complete             │                          │
│      ↓                               │                          │
│  System Installer  ──────Success─────┘                          │
│                                                                  │
│  PreferencesManager (Shared state)                              │
│  ├─ auto_check_updates: Boolean                                 │
│  ├─ last_update_check: Long                                     │
│  └─ dismissed_update_version: String?                           │
└─────────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

| Component                     | Responsibility                                      |
|-------------------------------|-----------------------------------------------------|
| **UpdateChecker**             | Query GitHub API, parse releases, compare versions  |
| **UpdateDownloader**          | Download APK using DownloadManager, trigger install |
| **UpdateNotificationManager** | Create and show system notifications                |
| **UpdateDialogs**             | UI components for update prompts                    |
| **PreferencesManager**        | Store user preferences and state                    |
| **MainActivity**              | Trigger auto-check on app launch                    |
| **AboutTab**                  | UI for manual check and settings                    |

## Security Flow

```
1. HTTPS Request → GitHub API (trusted source)
2. JSON Response → Parse and validate
3. Download APK → system Downloads folder (user visible)
4. FileProvider → Secure URI generation
5. System Installer → User explicitly confirms
```

## Error Handling

```
Network Error → Silent fail (background) / Show error (manual)
Parse Error → Log and skip
Download Fail → Notify user
Install Cancel → User choice, can retry
```

