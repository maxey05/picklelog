# RFC-001 — verification runbook

Everything in this repository was written and checked in a cloud container with
**no Android SDK** and with `dl.google.com`, `repo1.maven.org` and
`services.gradle.org` blocked by network policy. No Gradle build could run
there. So the acceptance criteria split in two, and this file is the honest
accounting of which is which (R108: every claim is accompanied by the command
that proved it).

Written for **VS Code**, not Android Studio — nothing below assumes an IDE.

**Every command in this file is Windows PowerShell**, not bash/cmd — this repo
is developed on Windows. That means: `.\gradlew.bat`, not `./gradlew`;
backslash paths; `$env:VAR`, not `$VAR`; `Copy-Item`/`Remove-Item`/`Select-String`,
not `cp`/`rm`/`grep`. If a command below is ever pasted from somewhere else and
looks like bash, it's wrong for this project — flag it.

---

## Part 1 — already proven

These ran to completion against this exact tree, in an offline Gradle harness
that applies `gradle/rule-checks.gradle.kts` with no plugins.

| AC | Rule | Evidence |
|---|---|---|
| AC-1.2 | R12 | **Proven by compile failure, 2026-09-23.** A one-line probe `import android.content.Context` dropped into `:domain` fails the build: `e: .../domain/.../BoundaryProof.kt:1:8 Unresolved reference 'android'` at `:domain:compileKotlin`. The probe was removed afterwards; `:domain/src` holds only `.gitkeep`. The protection is structural, not conventional — `domain/build.gradle.kts` applies `kotlin.jvm` alone, so the Android SDK is never on that module's compile classpath and no amount of discipline is required to keep it off. |
| AC-1.5 | R2 | `checkNoVersionLiterals` passes clean; fails on `implementation("com.example:widget:1.2.3")`; fails on a bare `"9.9.9"`; does **not** fail on a version inside a comment |
| AC-1.11 | R10 | `checkNoKeystoreCommitted` fails when a `.jks` is staged; passes when `.gitignore` excludes it — and `git ls-files` confirms neither keystore file is tracked |
| AC-1.12 | R32 | `checkNoDestructiveMigration` fails on a source file containing the string |
| AC-1.13 | R49 | `checkNoReadMediaImages` fails on a declared `<uses-permission>`; does **not** fail on the manifest comment documenting the rule |
| AC-1.14 | R5 | `checkNoPrereleaseVersions` fails on a `-rc01` entry added to the catalog |
| AC-1.20 | — | `.github/workflows/ci.yml` parses as YAML; three jobs, API 26/37 matrix |
| AC-1.15 | — | **ktlint 1.0.1 CLI run against this exact tree: exit 0, zero violations.** 1.0.1 is not arbitrary — it is the ktlint the build actually uses (plugin 12.1.1 defaults to it). 18 real violations were found and fixed this way, not one build-failure at a time. |

Three defects were found *by running these*, not by reading them, and all three
are fixed in the tree:

1. The check script flagged its own error messages. It now excludes itself.
2. The manifest comment documenting R49 tripped the R49 check. It now strips XML
   comments before scanning — a comment is not a declaration.
3. **AC-1.5's regex as written in RFC-001 is too narrow.** It anchors a quote at
   each end, so it misses `implementation("com.example:widget:1.2.3")`, which is
   the most common R2 violation there is. The implemented check extracts every
   quoted literal and flags any that *contains* a version. RFC-001 should be
   amended to match (R110).

Also verified by inspection: AC-1.1 (four modules, `rootProject.name`), AC-1.3
(inward-only `implementation(project(...))`), AC-1.4 (every §3.2 coordinate
present), AC-1.9 (Kotlin pinned 2.3.21 with the R1 comment above it), AC-1.16
(`CLAUDE.md`), AC-1.17 (three namespaces), AC-1.18 (Compose plugin at the
Kotlin version), AC-1.19 (`MainActivity.kt` + manifest).

### ktlint: how AC-1.15 was actually closed

`ktlintKotlinScriptCheck` failed the first real build on a single violation
(`app/build.gradle.kts:5:5 Missing space after //`). Fixing that one line would
only have surfaced the next. Instead the whole tree was linted at once with the
ktlint CLI — pinned to **1.0.1**, because that is what plugin 12.1.1 bundles
(`KtlintExtension.kt` at tag `v12.1.1`: `objectFactory.property { set("1.0.1") }`).
Version choice mattered: ktlint 1.8.0 reports 32 violations on this tree, 1.0.1
reports 18. Formatting to 1.8.0's rules would have rewritten 14 sites the build
does not police, for nothing.

17 of the 18 were auto-corrected with `ktlint --format`. The 18th was
`MainActivity.kt:27 function-naming` on the `@Composable ToolchainProofScreen()`.
**That one was not "fixed" by renaming** — Compose requires PascalCase for
composables, so renaming would satisfy the linter by breaking the convention.
The linter was corrected instead, via `.editorconfig`:

```
ktlint_function_naming_ignore_when_annotated_with = Composable
```

The formatter rewrote `gradle/rule-checks.gradle.kts`, which is the file that
enforces R2/R5/R32/R49/R10 — so a cosmetic reformat there could silently
disarm the project's own guardrails. It was checked three ways rather than
assumed:

1. **Token diff:** 880 tokens before, 881 after. The single added token is a
   trailing comma in `fun report(...)`'s parameter list — a Kotlin no-op, and
   already permitted by `ij_kotlin_allow_trailing_comma`.
2. **`checkRules` still passes** on a clean tree.
3. **Every check was re-proven by making it fail**, per RFC-001 §6 — a real
   version literal, a version inside a comment (must *not* trip it),
   `fallbackToDestructiveMigration`, an `-rc01` catalog entry, a declared
   `READ_MEDIA_IMAGES`, and that same permission inside an XML comment (must
   *not* trip it). 8 tests, 8 expected outcomes.

Closed as a side effect: the catalog's `ktlintPlugin` row was marked
**UNVERIFIED** (R103). It is now verified — 12.1.1 resolves, runs, and bundles
ktlint 1.0.1. Note in the catalog warns that bumping the plugin changes the
bundled ktlint and will surface new rules.

---

## Part 2 — needs a real toolchain

Work through these in order. Paste the output back and the remaining ACs close.

### Step 0 — prerequisites

```powershell
java -version        # need 17+; the Gradle toolchain targets 17
gradle -version      # only needed once, for step 1
```

If either prints "not recognized as the name of a cmdlet...": that error text
is PowerShell's version of "command not found" — it means the program isn't on
`PATH` for *this* session, not that the syntax is wrong.

**JDK 17** — real winget package, confirmed against
[winget.run](https://winget.run/pkg/EclipseAdoptium/Temurin.17.JDK):

```powershell
winget install EclipseAdoptium.Temurin.17.JDK
```

**Gradle — there is no winget package.** `winget install Gradle.Gradle`, which
an earlier draft of this file told you to run, does not exist: Gradle has
[no official winget listing](https://github.com/gradle/gradle/issues/18576),
the request has been open since 2021 with no package published. That was a
wrong guess on my part, not something wrong on your end — sorry for the dead
end. Two real options instead:

**Option A — manual zip, no package manager (recommended, this is what
`gradle.org`'s own Windows install instructions use):**

```powershell
# 1. Download https://services.gradle.org/distributions/gradle-9.6.0-bin.zip
#    in a browser (matches the pin in gradle/wrapper/gradle-wrapper.properties).
# 2. Extract it, then add it to PATH for this session:
Expand-Archive $HOME\Downloads\gradle-9.6.0-bin.zip -DestinationPath C:\Gradle
$env:Path += ";C:\Gradle\gradle-9.6.0\bin"
gradle -version
```
Gradle only needs to exist system-wide for the ONE bootstrap command in Step 1
below. After that, everything runs through the repo's own `.\gradlew.bat`
wrapper and this manual install is never touched again — no need to add it to
your permanent `PATH` if you'd rather not.

**Option B — Chocolatey, if you already have it** (confirmed real package,
current release tracked at
[community.chocolatey.org/packages/gradle](https://community.chocolatey.org/packages/gradle)):

```powershell
choco install gradle
```

Whichever you used, open a **new** PowerShell window before re-checking
`java -version` / `gradle -version` — `winget`/`choco` update `PATH` in the
registry, but a PowerShell process only re-reads `PATH` at launch, so the
window you installed from won't see it. Confirm without guessing:

```powershell
winget list --id EclipseAdoptium.Temurin.17.JDK
$env:Path -split ';' | Select-String gradle
```

Android SDK — this machine already has **Android Studio installed**
(`C:\Program Files\Android\Android Studio`, from `winget install
Google.AndroidStudio`) but it has never been launched, so its Setup Wizard
never ran and the SDK itself was never downloaded — installing the app and
installing the SDK are two separate steps.

**The SDK must land at a path with no spaces anywhere in it — not even in
the Windows username.** This was hit twice on the real machine, so it is not
hypothetical:

- `C:\Program Files\Android` looks fine but is UAC-protected; SDK package
  downloads there fail on a silent permission/license error instead of
  prompting for elevation.
- `%LOCALAPPDATA%\Android\Sdk` (i.e. `C:\Users\Emgi Reyes\AppData\Local\...`)
  is *writable*, but this account's username itself contains a space
  ("Emgi Reyes"). `sdkmanager.bat` (and `avdmanager.bat`) build their
  classpath from a small `*-classpath.jar` whose manifest `Class-Path` is
  resolved relative to its own absolute location; a space anywhere in that
  path breaks the resolution and produces exactly this failure:
  `Error: Could not find or load main class
  com.android.sdklib.tool.sdkmanager.SdkManagerCli` /
  `ClassNotFoundException`. This is a long-documented Windows-only bug in
  the Android cmdline-tools launchers, not a corrupt download — see
  [Stack Overflow #60727326](https://stackoverflow.com/questions/60727326/sdkmanager-error-could-not-find-or-load-main-class-com-android-sdklib-tool-sdk),
  [flutter/flutter#56117](https://github.com/flutter/flutter/issues/56117),
  [kivy/buildozer#1398](https://github.com/kivy/buildozer/issues/1398).

So the only safe destination on this machine is a short path straight off
`C:\`, e.g. **`C:\Android`** — no `Program Files`, no `AppData`, no
`C:\Users\<name>\...` at all.

**Option A — run the Setup Wizard once (fastest, since Studio is already
installed).** You do not need to use Android Studio as your editor
afterward; this is a one-time download step, then back to VS Code.

```powershell
Start-Process "C:\Program Files\Android\Android Studio\bin\studio64.exe"
```

Click through: *Do not import settings* -> *Standard* install type. On the
"Verify Settings" / SDK components screen, the **"Android SDK Location"**
field will suggest either `AppData\Local\Android\Sdk` or `Program Files` —
**change it to `C:\Android`** before clicking Next. Accept the SDK component
licenses -> *Finish*. Confirm it landed there, then close Android Studio:

```powershell
Test-Path "C:\Android\platform-tools\adb.exe"   # expect True
```

**Option B — command-line tools only, Android Studio never opens.** Download
`commandlinetools-win` from developer.android.com, unzip so the path ends up
`C:\Android\cmdline-tools\latest` (the zip extracts to a folder called
`cmdline-tools`; if `bin` isn't directly inside `latest`, move the inner
folder up and rename it), then:

```powershell
$env:ANDROID_HOME = "C:\Android"
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" `
  "platform-tools" "platforms;android-37" `
  "emulator" "system-images;android-26;default;x86_64" `
  "system-images;android-37;google_apis;x86_64"
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" --licenses
```

If cmdline-tools were already extracted somewhere under a space-containing
path (e.g. `%LOCALAPPDATA%\Android\Sdk\cmdline-tools`), don't re-download —
just move the folder:

```powershell
New-Item -ItemType Directory -Force -Path C:\Android | Out-Null
Move-Item "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools" "C:\Android\cmdline-tools"
```

Then point the build at wherever the SDK actually has packages +
accepted licenses, from the repo root
(`C:\emgiStuff\codingProjects\picklelog`):

```powershell
cd C:\emgiStuff\codingProjects\picklelog
"sdk.dir=C:\\Android" | Out-File -Encoding ascii local.properties
```

> **What actually resolved this on the real machine:** the `sdkmanager.bat`
> route above kept failing on the space-in-username classpath bug, but a
> `licenses\android-sdk-license` file and `build-tools\36.0.0` turned up
> anyway under `%LOCALAPPDATA%\Android\Sdk` — Android Studio's own SDK
> Manager GUI wrote them there directly (it runs inside its own JVM process,
> not through the fragile `.bat` launcher, so the space in the username
> never bites it). Since the license was already accepted, Gradle/AGP's
> built-in SDK auto-download (`android.builder.sdkDownload`, on by default)
> fetched the missing platform itself, straight from the Gradle daemon's
> own JVM, during the build — no `sdkmanager` invocation needed at all. So
> the `C:\Android` / `cmdline-tools` path above turned out to be
> unnecessary; the working `local.properties` was:
> ```
> sdk.dir=C:\\Users\\Emgi Reyes\\AppData\\Local\\Android\\Sdk
> ```
>
> **Second amendment (R110):** that first real build then failed at
> `:app:checkDebugAarMetadata` — Compose BOM 2026.09.00 resolves Compose
> artifacts at 1.12.1, which require `compileSdk >= 37`. `compileSdk` and
> `targetSdk` are now **37** (not 36) in `app/build.gradle.kts`,
> `data/build.gradle.kts` and `ui/build.gradle.kts` — see RULES.md and
> RFC-001 AC-1.6 for the full amendment. Gradle auto-downloaded
> `platforms;android-37` the same way it did `android-36`, using the same
> already-accepted `android-sdk-license` — no manual step needed. If
> pre-seeding the SDK instead of letting Gradle auto-download mid-build,
> ask `sdkmanager` for `platforms;android-37` (not `android-36`); let it
> resolve its own matching `build-tools` version rather than hardcoding
> one, since the exact point release shifts.
> If your machine ends up in the same spot — a `licenses` folder with at
> least `android-sdk-license` already present under a space-containing SDK
> path — prefer pointing `local.properties` there over fighting
> `sdkmanager.bat`.

`local.properties` is already gitignored — it is machine-specific and must not
be committed.

> **Gotcha — `PropertyEscape`, and it fails the build, not just lint.** In a
> Java `.properties` file the colon is a key/value separator as well as `=`, so
> a Windows path needs **both** the backslashes and the drive colon escaped:
> ```
> sdk.dir=C\:\\Users\\Emgi Reyes\\AppData\\Local\\Android\\Sdk
> ```
> Writing `C:\\Users\\...` (backslashes escaped, colon not) *parses to the
> identical path* — verified with `java.util.Properties`, both forms yield
> `C:\Users\Emgi Reyes\AppData\Local\Android\Sdk` — so the SDK resolves
> fine and the build compiles, dexes and packages happily. It then dies much
> later at `:app:lintDebug` with `PropertyEscape`, because lint treats it as an
> error and `lintDebug` is wired into `check`, which `build` depends on. The
> escaped form above is also exactly what Android Studio writes, so prefer it.
> This is a local-only trap: CI has no `local.properties` (it uses
> `ANDROID_HOME`), so it never reproduces there.
>
> **And it survives the fix, once.** Lint does not register `local.properties`
> as a task input, so after correcting the file Gradle still considered
> `lintReportDebug` up-to-date and replayed the *cached* failure — the report
> on disk was timestamped four minutes **before** the fix and still quoted the
> old unescaped line. The build output gives it away: `lintReportDebug` does
> not re-run, and the task counts collapse to something like
> `259 actionable tasks: 7 executed, 252 up-to-date`. Fixing the file is not
> enough; the stale lint state has to go too:
>
> ```powershell
> Remove-Item -Recurse -Force app\build\intermediates\lint-cache, `
>   app\build\intermediates\lint_intermediate_text_report, `
>   app\build\intermediates\lint_partial_results, `
>   app\build\intermediates\lint_report_lint_model, `
>   app\build\intermediates\lint_return_value, `
>   app\build\intermediates\lint_vital_partial_results, `
>   app\build\intermediates\lint_vital_report_lint_model -ErrorAction SilentlyContinue
> Remove-Item -Force app\build\reports\lint-results-debug.* -ErrorAction SilentlyContinue
> ```
>
> `.\gradlew.bat clean build` also works but rebuilds everything. If this
> recurs often, the surgical suppression is a `lint.xml` with
> `<issue id="PropertyEscape"><ignore regexp="local\.properties" /></issue>`,
> which keeps the check live for every other properties file.

**VS Code extensions worth having:** *Kotlin Language* (fwcd), *Gradle for
Java*, and *Android iOS Emulator* or just `adb` from the terminal. None are
required to build.

### Step 1 — generate the Gradle wrapper (closes half of AC-1.7)

`gradle-wrapper.jar` is a binary that could not be downloaded in the container.
`gradle/wrapper/gradle-wrapper.properties` is already committed and pins 9.6.0,
so this one command produces the rest and respects that pin:

```powershell
gradle wrapper --gradle-version 9.6.0
git add gradlew gradlew.bat gradle/wrapper/gradle-wrapper.jar
```

Then confirm a clean clone needs no local Gradle:

```powershell
.\gradlew.bat --version      # expect Gradle 9.6.0, JVM 17+
```

> **Amendment (R110):** RFC-001 v1.1's file list never included `gradle.properties`.
> Without it, the Gradle daemon runs on the JVM's default heap, which is not
> enough to dex a Compose-heavy `:app` -- confirmed on the real machine as
> `Task :app:mergeExtDexDebug FAILED > ERROR: D8: java.lang.OutOfMemoryError:
> Java heap space`. `gradle.properties` now exists at the repo root:
> ```properties
> org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m
> org.gradle.parallel=true
> org.gradle.caching=true
> android.useAndroidX=true
> android.nonTransitiveRClass=true
> ```
> It is a normal tracked file, not gitignored -- unlike `local.properties`
> (machine-specific) and the keystore files (secrets), heap settings are a
> repo-wide default every clone needs, so they belong in version control.

### Step 2 — generate the upload keystore (closes AC-1.11) — DONE

**Status: complete.** `keystore/upload-keystore.jks` exists, and
`keystore/keystore.properties` supplies all four keys the guarded signing
config reads (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`);
`storeFile` resolves to a real file and `keyAlias` is `picklelog-upload`.
`git ls-files` shows no `.jks`, `.keystore` or `keystore.properties` tracked,
and `.gitignore` covers all three plus `local.properties`. So release builds
sign with the real upload key rather than falling back to debug signing.
Passwords were never read into the Claude session — only key *presence* and
value *length* were checked.

Run this yourself. The password and the distinguished-name prompts are yours;
they were deliberately kept out of the Claude session.

```powershell
keytool -genkeypair -v -keystore keystore\upload-keystore.jks `
  -alias picklelog-upload -keyalg RSA -keysize 2048 -validity 10000
Copy-Item keystore\keystore.properties.template keystore\keystore.properties
# then fill in storePassword and keyPassword in keystore\keystore.properties
```

Confirm nothing leaked:

```powershell
git status --short keystore\      # expect ONLY README.md and the .template
.\gradlew.bat checkNoKeystoreCommitted
```

> **Back it up somewhere else today.** Gitignoring it inside the repo satisfies
> "never committed", but it still lives on one disk. Lose that disk with no
> other copy and Picklelog can never be updated under the same Play listing
> again. A password manager's file attachment is enough.

### Step 3 — first build (closes AC-1.6 partly, AC-1.7, AC-1.8)

```powershell
.\gradlew.bat build --stacktrace
```

This is also the moment **R1's pin gets verified for real**. If KSP 2.3.12 is
not the right build suffix, this is where it surfaces. Three catalog entries
were flagged as unverifiable from the container and need confirming here:

```powershell
.\gradlew.bat :app:dependencies --configuration releaseRuntimeClasspath > deps.txt
Select-String -Path deps.txt -Pattern "activity-compose"
```

| Entry | Why it is unverified | What to do |
|---|---|---|
| `activityCompose = "latest.release"` | Not in RULES.md §1.1 or RFC-001 §3.2 — a genuine gap. It is **not** managed by the Compose BOM. | Read the resolved version above, pin it in the catalog. It ships, so it should not stay dynamic. |
| `ktlintPlugin = "12.1.1"` | AC-1.15 requires the plugin; no version was ever pinned for it. This number was not checked against a live source. | Confirm on plugins.gradle.org and correct. |
| `ksp = "2.3.12"` | RFC-001 §3.2 asks for a re-check against `github.com/google/ksp/releases`; that host was blocked. | Confirm the exact published build suffix. |

### Step 4 — prove the `:domain` boundary (closes AC-1.2) — DONE

**Status: complete, 2026-09-23.** Recorded output:

```
> Task :domain:compileKotlin FAILED
e: file:///C:/emgiStuff/codingProjects/picklelog/domain/src/main/kotlin/com/maxeydev/picklelog/domain/BoundaryProof.kt:1:8 Unresolved reference 'android'.
```

That failure is the pass condition, not a defect — `:domain` cannot see the
Android SDK, which is exactly what R12 requires. The probe file was removed by
the last line of the block below; `:domain/src` now contains only `.gitkeep`.

The permanent protection is the `kotlin("jvm")` plugin choice. This
demonstrates once that it actually works:

```powershell
"import android.content.Context" | Out-File -Encoding utf8 `
  domain\src\main\kotlin\com\maxeydev\picklelog\domain\BoundaryProof.kt
.\gradlew.bat :domain:compileKotlin     # MUST fail: unresolved reference
Remove-Item domain\src\main\kotlin\com\maxeydev\picklelog\domain\BoundaryProof.kt
```

Paste the failing output into the PR description. RFC-001 asks for it recorded.

### Step 5 — launch on both API levels (closes AC-1.6, AC-1.19)

**Is the emulator required? No, not for both levels.** AC-1.6's actual intent
is "the app launches successfully on a low-API and a high-API device" -- the
emulator is just one way to get a device at a specific API level, not a
requirement in its own right. A physical phone satisfies one side of that on
its own (whatever Android version the phone is running), and the CI matrix
(`api-level: [26, 37]`, already updated in `ci.yml`) proves the other side
once the repo is pushed and Step 7 goes green. So the practical path is:
install and launch on the physical phone now, record its API level as the
local proof for whichever of 26/37 it happens to run, and let CI cover the
other level -- only set up an emulator locally if you want both levels proven
on this machine before pushing.

**Prerequisites — neither is installed by the Step 0 SDK setup.** Skipping
these produces two misleading errors: `DeviceException: No connected devices!`
from Gradle, and `'adb' is not recognized` from PowerShell. Neither indicates a
problem with the app.

**(a) `adb` is not on PATH.** It ships inside the SDK but nothing adds it:

```powershell
$env:Path += ";$env:LOCALAPPDATA\Android\Sdk\platform-tools"
adb version        # confirm it resolves
```

That lasts for the current terminal only. To make it permanent, add
`%LOCALAPPDATA%\Android\Sdk\platform-tools` to your user PATH in
System Properties -> Environment Variables.

**(b) There is no device to install onto.** `platforms/` being populated does
not mean an emulator exists — those are compile-time API jars. An emulator
additionally needs a *system image* plus an *AVD*, and this SDK has no
`system-images/` directory at all. Two ways forward:

*Physical device (fastest).* Enable Developer options (Settings -> About
phone -> tap Build number 7 times) -> USB debugging (Settings -> System ->
Developer options -> USB debugging), plug the phone in via USB, and pick
"File Transfer" or "PTP" if the phone shows a USB-mode prompt (a
charging-only mode can hide the device from adb on some phones). A dialog
saying "Allow USB debugging?" with an RSA key fingerprint appears on the
phone -- accept it there; skipping this is the single most common cause of
a phone that is plugged in but invisible to adb.

```powershell
adb devices
```

Expected output is one line with the device id and the word `device`:

```
List of devices attached
ABC123DEF456    device
```

Anything else is a specific, fixable problem, not "it doesn't work":

- **Empty list.** `adb` isn't on PATH in *this* terminal (the `$env:Path`
  fix from part (a) only applies to the terminal it was run in -- open a new
  PowerShell window and it's gone unless you added it to the permanent user
  PATH), or the phone hasn't been put into USB debugging mode, or the cable
  is charge-only.
- **`unauthorized`.** The RSA prompt either never appeared or was dismissed
  instead of accepted. Unplug, replug, and accept the dialog on the phone
  screen when it shows up. If it never shows up, revoke USB debugging
  authorizations in Developer options and replug.
- **`offline`.** Usually a flaky USB driver or cable. Run
  `adb kill-server; adb start-server; adb devices` (unplug/replug too) or
  try a different USB cable/port -- some phones need the OEM driver, which
  Windows normally fetches automatically on first plug-in.
- **More than one device listed** (e.g. an old emulator entry alongside the
  phone). Pass `-d` to target the only USB-attached device, or `-s <id>` with
  the id from `adb devices`, e.g. `adb -d shell am start ...`.
- **`INSTALL_FAILED_OLDER_SDK` / similar from `installDebug`.** The phone's
  Android version is below `minSdk 26` (Android 8.0). Check with
  `adb shell getprop ro.build.version.sdk` -- if it's under 26, this phone
  cannot run the app at all and can't be used for Step 5; an emulator (or a
  different phone) is then required for the low-API side specifically.
- **`INSTALL_FAILED_UPDATE_INCOMPATIBLE` / signature mismatch.** A debug
  build was already installed with a different signing key on this phone at
  some point. Run `adb uninstall com.maxeydev.picklelog` and reinstall.

Once `adb devices` shows exactly one `device` entry, proceed to the install
commands below.

*Emulator.* `sdkmanager`/`avdmanager` live under `C:\Android\cmdline-tools`
(kept off any path containing a space — see the Step 0 gotcha), but the SDK
they manage is the one in `%LOCALAPPDATA%`, so `--sdk_root` must point there:

```powershell
$sdk  = "$env:LOCALAPPDATA\Android\Sdk"
$tools = "C:\Android\cmdline-tools\latest\bin"

# 1. See what images actually exist — do not guess the package id
& "$tools\sdkmanager.bat" --sdk_root="$sdk" --list | Select-String "system-images;android-(26|37)"

# 2. Install the two you need (substitute the exact ids from step 1)
& "$tools\sdkmanager.bat" --sdk_root="$sdk" "system-images;android-26;default;x86_64"
& "$tools\sdkmanager.bat" --sdk_root="$sdk" "<the android-37 id from step 1>"

# 3. Create one AVD per API level
& "$tools\avdmanager.bat" create avd -n picklelog-api26 -k "system-images;android-26;default;x86_64" -d pixel
& "$tools\avdmanager.bat" create avd -n picklelog-api37 -k "<the android-37 id>" -d pixel

# 4. Boot one (leave this window open; it blocks)
& "$sdk\emulator\emulator.exe" -avd picklelog-api26
```

Then, in a second terminal, with the device listed by `adb devices`:

```powershell
.\gradlew.bat :app:installDebug
adb shell am start -W -n com.maxeydev.picklelog/.MainActivity
adb shell pidof com.maxeydev.picklelog      # non-empty = it is still alive
```

Do this on an API 26 emulator and an API 37 emulator (AC-1.6 was amended from 36 to 37 -- see RULES.md R110 note). The CI device matrix
covers it too, once the repo is pushed — but the first run should be local so a
failure is debuggable.

### Step 6 — release build installs and launches (closes AC-1.10)

Deliberately manual, not in CI: wiring it there needs signing secrets on the
runner, which is more setup than M0 warrants (RFC-001 §3.3).

```powershell
.\gradlew.bat :app:assembleRelease
adb install -r app\build\outputs\apk\release\app-release.apk
adb shell am start -W -n com.maxeydev.picklelog/.MainActivity
adb shell pidof com.maxeydev.picklelog
```

**A release build that compiles and then crashes on launch is the normal R8
failure**, which is exactly why this AC demands an install rather than a green
compile. If it crashes, the missing keep rule will be in
`app/proguard-rules.pro` — those rules are generic placeholders, written before
any Room entity or `@Serializable` class exists.

### Step 7 — CI green

Push and confirm all three jobs pass. Expect the device matrix to be the flaky
one; re-run before treating a red as a real regression.

---

## Open items this RFC leaves behind

- **Four test dependencies and `activity-compose` use `latest.release`.** A
  rebuild months from now can silently resolve a different version. Accepted for
  the test libraries (owner decision #4); **not** acceptable for
  `activity-compose`, which ships. Pin it in step 3.
- **`documentation/` already holds PRD.md, FEATURES.md, RULES.md, RFCS.md and
  the full RFC set** — this repo is the canonical location, not the Claude
  project. `CLAUDE.md`'s pointer at `documentation/RULES.md` (AC-1.16) resolves
  correctly.
- **AC-1.5's regex in RFC-001 §4 is narrower than the implemented check.** Amend
  the RFC (R110).
- **R37's two backup-rules files are not here.** They belong to F48 / RFC-003,
  not to this RFC — recorded so the absence reads as scope, not oversight.
