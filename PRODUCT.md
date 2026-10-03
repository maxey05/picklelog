# Product

## What it is

Picklelog is an Android-only, local-first pickleball match journal. Players log matches on their phone and get a running W/L record, a weekly play streak, and a shareable card. No account, no login, no server, no sync. Free up to 50 matches, then a single one-time unlock.

Source of truth for scope and requirements: `documentation/PRD.md` (v0.4) and `documentation/RULES.md`.

## Design mode

Operate. The user is completing a task (logging and reviewing matches), often courtside. Scanability, consistency and Material 3 conventions outrank expression. Brand shows up in precise details, not decoration.

## Users

- **The Regular (primary, about 60%).** Plays 2-4 times a week at the same courts, mostly doubles. Wants to remember how often they played and keep a streak alive. Any required field they don't care about kills adoption.
- **The Improver (about 30%).** Plays singles and competitive doubles. Cares about scores, notes, paddle, and who they struggle against. Reads their own history back.
- **The Sharer (about 10%).** Plays socially, takes photos, posts to Instagram Stories. Wants a good-looking share card.

## Purpose and the core promise

Twenty seconds to log a match. Zero setup. The dashboard and streak give a reason to come back; the share card gives a reason to tell someone.

Positioning: Strava's ergonomics (fast logging, photos, streaks, branded share cards) applied to pickleball, without the social graph or a server.

## Brand

Inferred from the PRD and the mockups in this project; edit freely.

- Friendly, quick and unfussy. Not a rating authority, not competitive-serious.
- Mascot: a duck holding a pickleball paddle. The current illustration is a placeholder; the owner is drawing the final one and needs to be able to swap it without touching screen layout.
- Colour direction (owner's brief): dark green, light green and white. Header is dark green.
- Wordmark: "Picklelog", set prominently in the header.

## Visual decisions made so far (Draft 3 mockups)

- Dark green header containing the logo, a compact one-month contribution graph, name, streak, win rate ("WR") and W-L record, then the search bar.
- Contribution graph works like GitHub's: one cell per day, brighter green for more matches in a day. Days run across the horizontal axis (Mon-Sun), one row per week, cells are short rectangles. Shows month and year, with "This month" W/L/% beside it and previous/next month arrows.
- Match list is one continuous list (no week dividers) of full-width rows. Wins have a light green tint and a solid green "W" badge; losses are neutral grey with a muted "L" badge. Game scores sit on the right, with a photo thumbnail when the match has one.
- Sort is a dropdown; filter is an icon-only button that opens a bottom sheet. Active filters show as a count on the button and as removable chips.
- Log match is a plus-only floating button.
- First run: no contribution graph (it appears after the first logged match), duck illustration, "Welcome to Picklelog! Start by logging a match."
- Filtered stats must never look like totals (FR-20); the streak always counts all matches.

## Non-negotiables from the PRD and rules

- Text contrast WCAG AA (4.5:1 body, 3:1 large) in both themes (R74).
- Win/loss is never conveyed by colour alone; always paired with a letter, label or icon, including on the share card (R75, NFR-18).
- Interactive targets at least 48x48dp, including icon buttons that look smaller (R72, NFR-16).
- Font scaling and display size up to 200% without clipping or overlap; text in `sp` (R71, NFR-15). Dense list rows are where this breaks.
- TalkBack: a match row reads as one coherent sentence via `mergeDescendants = true` (R73, NFR-17).
- Light and dark themes via Material 3 colour schemes, following the system setting, with dynamic colour where available (R76, NFR-22).
- Phone portrait is the primary target; tablet and landscape must not break (R77, NFR-20).
- Works fully offline, permanently (NFR-21).
- Only format, date and result are required when logging; everything else is optional by design.

## Platform

Android, Kotlin, Jetpack Compose, Material 3. Share card is rendered by an off-screen WebView in `:ui`. Development is on Windows in VS Code with PowerShell.

## Open items

- Dark theme has not been mocked yet. Draft 3 is light only, and the dark green header will need its own treatment against a dark surface.
- Arrow buttons on the contribution graph were drawn at 38px; they need a 48dp touch area in the real build.
- Final duck illustration pending from the owner.
- Share card design has not been mocked in this round.
