# RFC-002 — verification runbook

Same situation as RFC-001's runbook, and the same honesty rule (R108): everything below
was written in a cloud container with **no Android SDK** and with `dl.google.com`,
`repo.maven.apache.org` and `services.gradle.org` blocked. No Gradle build ran. The shell
available on the Windows machine during this session was a Linux VM with the repo folder
mounted, which cannot execute Windows `build-tools`. So **nothing in RFC-002 is verified
yet** beyond what a text scan can prove.

**Every command here is Windows PowerShell.** `.\gradlew.bat`, backslash paths,
`Select-String` rather than `grep`. A `./gradlew` anywhere in this file is a bug.

---

## 0. What changed in RFC-001's output, and why

Three edits to already-accepted RFC-001 files were forced by RFC-002. Each is an R110
amendment, not a preference.

| Change | Why |
|---|---|
| `gradle.properties` gains `android.builtInKotlin=false` | **KSP does not support AGP 9's built-in Kotlin** ([google/ksp#2615](https://github.com/google/ksp/issues/2615) — a maintainer: *"this is a temporary thing and we do plan to support AGP 9 and built in kotlin"*). Room's compiler runs on KSP. Without this line nothing in `:data` can generate. The opt-out is documented by JetBrains and **disappears in AGP 10**. |
| `alias(libs.plugins.kotlin.android)` uncommented in `:app`, `:data`, `:ui` | The other half of the same change: with built-in Kotlin off, the Kotlin Gradle Plugin has to be applied explicitly again. |
| `app/proguard-rules.pro` serialization keeps scoped to `com.maxeydev.picklelog.data.profile` | AC-1.10 deferred tightening until real `@Serializable` classes existed. They now do, in exactly one package. The Room keeps were **left alone deliberately** — narrowing them is only provable by a release build that installs and launches, which is Step 6 below. |

Four catalog entries were added, all of which RFC-002 needs and none of which existed:
`kotlinx-coroutines-core` (`:domain` returns `Flow` from a plain `kotlin("jvm")` module, so
nothing else put coroutines on its classpath), `kotlinx-coroutines-test`,
`androidx.datastore:datastore` (the **typed** artifact — the catalog only had
`datastore-preferences`, which cannot back §3.5's `DataStore<UserProfile>`), and
`androidx.room:room-testing` for AC-2.12. Coroutines is pinned to **1.11.0**, verified
against the project's releases page.

---

## 1. Already established

| Claim | How |
|---|---|
| AC-2.21's branch is live: `@OptIn` **is** required | `kotlin.uuid.Uuid` is stable *since Kotlin 2.4*; `ExperimentalUuidApi` exists since 2.0 and Kotlin 2.3.0's release notes still call UUID support Experimental ([KT-81395](https://youtrack.jetbrains.com/issue/KT-81395)). R1 pins this project at **2.3.21**. Applied as `@file:OptIn(ExperimentalUuidApi::class)` per file — never the blanket `freeCompilerArgs` opt-in AC-2.21 forbids. |
| `AppInstant` must not point at `kotlinx.datetime.Instant` | kotlinx-datetime moved `Instant` to the stdlib; 0.8.0 ships `toDeprecatedInstant()`, and Kotlin **2.3.0 stabilized `kotlin.time.Instant` and `kotlin.time.Clock`**. RFC §3.1 flagged this as "unverified, re-check at implementation time" — it was real. The typealias now targets `kotlin.time.Instant`. One line, one file: §3.1 working as designed. |
| AC-2.25 holds right now | `Select-String -Path domain\src -Pattern "kotlinx.datetime|kotlin.time.Instant" -Recurse` matches **only** `domain/datetime/DateTime.kt`. Wired into `checkDomainDateTimeIndirection` so it stays true. |

Everything else needs a toolchain.

---

## 2. Run these in order

### Step 1 — the make-or-break

```powershell
cd C:\emgiStuff\codingProjects\picklelog
.\gradlew.bat :data:kspDebugKotlin --stacktrace
```

This single command settles three open questions at once:

1. **Does `android.builtInKotlin=false` actually restore KSP?** If AGP 9.4.0 has changed the
   property name or already removed it, this is where it shows.
2. **Is `ksp = "2.3.12"` the right published build suffix?** RFC-001 left this unverified and
   its Step 3 never closed it. An opaque annotation-processor failure here is R1's predicted
   failure mode.
3. **Does the nested-relation design compile?** RFC §4.1 specified `PersonWithRoleEntity` as
   "PersonEntity fields + role + slot, **via the junction**" while its own previous paragraph
   correctly said `@Junction` can't split by `role`. Those contradict. The implementation drops
   `@Junction` and nests a `@Relation` inside `PersonWithRoleEntity` instead. **If that reading
   is wrong, Room says so here, by name.** Paste the error rather than working around it.

### Step 2 — JVM tests (AC-2.23, AC-2.24, AC-2.16)

```powershell
.\gradlew.bat :domain:test :data:testDebugUnitTest
```

Covers roster validation (8 cases), name normalization including the NFC and
Turkish-locale cases, and the profile JSON key set.

> If `UserProfileSerializerTest` fails to *resolve* `androidx.datastore.core.Serializer`
> rather than failing an assertion, the typed DataStore artifact isn't on the unit-test
> classpath. Move that one file to `data\src\androidTest\` and re-run — the assertions are
> unchanged either way.

### Step 3 — commit the exported schema (AC-2.11)

Step 1 generates it. Confirm and commit:

```powershell
Get-ChildItem -Recurse data\schemas
git add data\schemas
.\gradlew.bat checkSchemaJsonCommitted
```

`checkSchemaJsonCommitted` reads `PICKLELOG_DB_VERSION` out of `PicklelogDatabase.kt`, so it
follows the version automatically rather than hardcoding `1.json`.

### Step 4 — all mechanical checks, including the two new ones

```powershell
.\gradlew.bat checkRules
```

Now runs seven tasks: RFC-001's five plus `checkDomainDateTimeIndirection` (AC-2.25) and
`checkSchemaJsonCommitted` (AC-2.11). Prove the new pair the way RFC-001 §6 proved the
others — make each one fail, then revert:

```powershell
# AC-2.25 must fail on a direct import in :domain
Add-Content domain\src\main\kotlin\com\maxeydev\picklelog\domain\match\GameScore.kt `
  "`nimport kotlinx.datetime.LocalDate"
.\gradlew.bat checkDomainDateTimeIndirection    # MUST fail
git checkout domain\src\main\kotlin\com\maxeydev\picklelog\domain\match\GameScore.kt

# AC-2.11 must fail when the schema is absent
Rename-Item data\schemas data\schemas-hidden
.\gradlew.bat checkSchemaJsonCommitted          # MUST fail
Rename-Item data\schemas-hidden data\schemas
```

### Step 5 — style

```powershell
.\gradlew.bat ktlintCheck
```

38 new Kotlin files were written to the repo's existing conventions (4-space indent,
120 columns, no star imports, trailing commas, `kotlin.*` imports last) but **were never run
through ktlint**. Expect violations; `.\gradlew.bat ktlintFormat` fixes the mechanical ones.
Per RFC-001's own note, the bundled engine is ktlint **1.0.1**, not whatever is newest.

### Step 6 — instrumented tests (AC-2.5 — the M0 exit criterion)

Needs a device. `adb devices` must show exactly one `device` — RFC-001's VERIFICATION.md
Step 5 has the full troubleshooting list if it doesn't.

```powershell
.\gradlew.bat :data:connectedDebugAndroidTest
```

| Test | Closes |
|---|---|
| `a match round trips through a Flow with opponents partner games and photos intact` | **AC-2.5 — the M0 exit** |
| `a singles match carrying a partner is rejected before anything is written` | AC-2.6, AC-2.24 |
| `a match with zero opponents saves on either format` | AC-2.6 |
| `deleting a match cascades to its rows and leaves every person intact` | AC-2.7 |
| `a write that fails part way leaves no partial match behind` | AC-2.9 |
| `Dave and dave resolve to one person row` | AC-2.3 |
| `the original display name survives a later differently cased entry` | AC-2.4 |
| `two concurrent calls for the same new name create exactly one person` | AC-2.22 |
| `the exported schema opens and validates against a populated database` | AC-2.12, R80 |

`MigrationTestHelper`'s two-argument constructor is the most likely compile fix in this
file — the overload set has moved between Room versions. If it doesn't resolve, the
`(Instrumentation, Class, List<AutoMigrationSpec>)` form is the fallback.

### Step 7 — whole build and a release install

```powershell
.\gradlew.bat build
.\gradlew.bat :app:assembleRelease
adb install -r app\build\outputs\apk\release\app-release.apk
adb shell am start -W -n com.maxeydev.picklelog/.MainActivity
adb shell pidof com.maxeydev.picklelog
```

The release install matters more now than it did at RFC-001: this is the first version with
Room-generated code and a `@Serializable` class, which is exactly what R8 strips wrongly.
A release build that compiles and crashes on launch is the normal failure.

---

## 3. Acceptance criteria not closed by any command above

| AC | Status |
|---|---|
| AC-2.1, AC-2.2, AC-2.8, AC-2.10, AC-2.13, AC-2.17, AC-2.19, AC-2.20, AC-2.26 | Structural — satisfied by construction, confirmed by Step 1 compiling and by reading the tree. AC-2.10 in particular: every query is `@Query` with bound parameters, no string concatenation anywhere including tests. |
| **AC-2.14** | **Implemented but untested.** `DatabaseBackup` copies the database (plus `-wal`/`-shm`) into `context.noBackupFilesDir`, which Android excludes from cloud backup *by construction* — so this closes without borrowing R37's backup-rules XML, which belongs to RFC-003. It cannot be exercised until a version 2 exists. Re-test it with the first real migration; that is also when R80's populated-database requirement gets teeth. |
| AC-2.15, AC-2.18 | Implemented; no Play Billing dependency is referenced from `:domain` or `:data` — confirm with `.\gradlew.bat :data:dependencies` and `Select-String billing`. |

---

## 4. Known risks this RFC leaves behind

- **`android.builtInKotlin=false` is a dated workaround, not a fix.** AGP 10 removes it. The
  project needs KSP to ship built-in-Kotlin support before then, or a different processor path.
  Watch google/ksp#2615.
- **`activityCompose`, `junit` and the AndroidX Test libraries are still `latest.release`.**
  RFC-001 flagged this and said to pin them "before RFC-002's test suite is load-bearing."
  It is now load-bearing. Read the resolved versions from `.\gradlew.bat :data:dependencies`
  and pin them.
- **The same person can be recorded twice on one match** (as opponent twice, or as both partner
  and opponent). The schema permits it — `match_person`'s key is `(match_id, role, slot)` — and
  AC-2.24 does not list it, so no rule was added rather than inventing one. It is a product
  question for RFC-003's UI: reject it, or allow it.
- **`GameScore` is unvalidated.** RFC §3.3 says game numbers are "1-based, contiguous" and
  scores "non-negative", but no AC requires enforcement, so none was added (R106). The
  `(match_id, game_number)` primary key prevents duplicates and nothing else.
