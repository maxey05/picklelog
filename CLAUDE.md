# CLAUDE.md — Picklelog

**Read `documentation/RULES.md` first.** It holds all 113 rules; this file is a
pointer plus the four that are most expensive to get wrong. Everything not
restated here is by reference, deliberately — two copies of a rule set diverge
(R111, R112).

Product: Android-only, local-first pickleball match journal. No server, no
account, no sync. Kotlin + Compose + Room, Play Billing 9, share card rendered
by an off-screen WebView. Free to 50 matches, then one $6.99 unlock.

Modules: `:app` → all · `:ui` → `:domain` · `:data` → `:domain` · `:domain` → nothing.

## The four to keep in your head

**R1 — Never raise Kotlin alone.** Pinned to 2.3.21 with KSP 2.3.12 and the
Compose compiler plugin at 2.3.21. Kotlin 2.4.20 exists and is newer, and has no
matching KSP; Room's processor runs on KSP, so raising Kotlin alone breaks the
Room compiler with an opaque processor error nowhere near a version line. The
three move together or not at all.

**R12 — `:domain` has no Android dependency. None.** No `android.*`, no
`androidx.*`, no `Context`, no `Log`. It uses the `kotlin("jvm")` plugin so an
Android import does not resolve in the first place. This is what makes the
streak engine's tests run on the JVM in milliseconds, and it is the project's
most likely accidental violation — the WebView card renderer belongs in `:ui`.

**R32 — `fallbackToDestructiveMigration()` must never appear.** It silently
deletes the user's entire match history on a schema mismatch. Every migration is
versioned, forward-only, and tested with `MigrationTestHelper` against a
*populated* database. `.\gradlew.bat checkNoDestructiveMigration` fails the
build on the string.

**R58 — No failure path may ever revoke Pro.** If `queryPurchasesAsync` fails or
the device is offline, the cached entitlement stands indefinitely. Pro is
withdrawn only on an explicit, successful refund or chargeback response — and
even then every existing match, photo and note stays readable, editable,
deletable and exportable. Design this in from the first line of billing code;
retrofitting it onto code that defaults to `false` is how it gets missed.

## Before you commit

Windows PowerShell, not bash — `.\gradlew.bat`, never `./gradlew`:

```powershell
.\gradlew.bat checkRules      # R2, R5, R10, R32, R49 — the mechanically checkable ones
.\gradlew.bat ktlintCheck     # R-§3 naming and official Kotlin style
.\gradlew.bat build
```

Cite the F-ID and rule IDs in the commit message (R107), e.g.
`F28: WebView measure/layout/draw capture (R16, R53)`.

## Shell: PowerShell, always

Development happens on Windows in VS Code, not Android Studio. Every command
in this repo's docs and in anything a session runs here is **PowerShell**:
`.\gradlew.bat` (not `./gradlew`), backslash paths, `$env:VAR` (not `$VAR`),
`Copy-Item`/`Remove-Item`/`Select-String` (not `cp`/`rm`/`grep`). A `./gradlew`
or a bash-style `$VAR` anywhere in this repo's instructions is a bug — fix it
rather than working around it.

## Three standing habits

- **R101** — no TODOs, placeholders or stubbed returns in anything presented as
  complete. If it cannot be finished, say so and say why.
- **R103** — never invent a version number, an API signature or a library
  capability. Check it, or mark it unverified. R1 exists because this failure
  mode is real and surfaces far from its cause.
- **R110** — when a rule here is wrong, change the rule. These were written
  before the first line of code existed. Ignored rules are worse than no rules.
