# Picklelog

A local-first pickleball match journal for Android. Every match lives on the
device: no account, no login, no server, no sync.

**Status: M0.** RFC-001 (project foundation) only. There is no product code yet
beyond a single screen that exists to prove the toolchain builds and launches.

## Build

Windows PowerShell (this project is developed on Windows, in VS Code — not
Android Studio, and not bash):

```powershell
gradle wrapper --gradle-version 9.6.0   # once, if gradlew is not yet committed
.\gradlew.bat build
.\gradlew.bat checkRules
.\gradlew.bat :app:installDebug
```

Requires JDK 17+ on PATH and an Android SDK with platform 36. See
`VERIFICATION.md` for first-time setup, PATH troubleshooting, and the RFC-001
acceptance checklist.

## Layout

```
app/      Android application — applicationId com.maxeydev.picklelog
domain/   pure Kotlin, kotlin("jvm") — no Android dependency, ever (R12)
data/     Android library — Room, DataStore, files
ui/       Android library — Compose, ViewModels, WebView card renderer
```

## Documentation

`documentation/` (gitignored — local reference, not pushed to GitHub):

| File | What it is |
|---|---|
| `PRD.md` | Product requirements |
| `FEATURES.md` | F1–F67 feature list, priorities, dependency map |
| `RULES.md` | R1–R113, how code is written |
| `RFCS.md` | Master roadmap |
| `picklelog-RFCs/RFCs/RFC-001..020` | Numbered implementation RFCs |

`claude-outputs/` (also gitignored) holds working copies from Claude sessions.
`CLAUDE.md` at the repo root is the short pointer version for agent sessions.
