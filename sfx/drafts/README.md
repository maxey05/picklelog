# Draft sound effects

Synthesized placeholders for the Picklelog sound effects plan, modelled on ../match_saved.wav: soft-attack sine chimes, C major pentatonic, 44.1 kHz 16-bit mono. Levels are matched to the reference, with tap cues about 6 dB quieter, confirmations about 3.5 dB quieter and celebrations about 2 dB quieter. File lengths include quiet reverb tails.

| File | Plan ID | Tier | Length | Description |
|---|---|---|---|---|
| pock.wav | G1 | tap | 100 ms | Base motif: dry hollow paddle-on-ball pock. Also the sound-switch demo |
| onboarding_step_1.wav | O1 | tap | 477 ms | Marimba pluck C, pager page 1 |
| onboarding_step_2.wav | O1 | tap | 478 ms | Marimba pluck D, pager page 2 |
| onboarding_step_3.wav | O1 | tap | 479 ms | Marimba pluck E, pager page 3 |
| onboarding_step_4.wav | O1 | tap | 476 ms | Marimba pluck G, pager page 4 |
| name_required.wav | O2 | confirm | 380 ms | Rubbery two-wobble boing, 380 ms, matches the shake |
| welcome.wav | O3 | celebration | 1366 ms | Pock, rising C-E-G marimba with chime, one duck peep, ~700 ms |
| new_match.wav | H1 | tap | 226 ms | Dry pock plus a faint 200 ms upward air swoosh |
| log_another.wav | H1 | tap | 83 ms | Shorter pock alone, for the snackbar action |
| streak_up.wav | H2 | celebration | 1291 ms | Flame whump peaking at 225 ms, then seven fading sparkle ticks; 900 ms |
| tally.wav | H3 | tap | 1016 ms | One soft marimba run rising over ~350 ms |
| streak_milestone.wav | H4 | celebration | 1989 ms | Paddle-smash crack then C-E-G-C marimba fanfare, 1.2 s |
| streak_milestone_major.wav | H4 | celebration | 1989 ms | Same, plus shimmer tail, for 26 and 52 weeks |
| first_match.wav | H5 | celebration | 1646 ms | Soft shell crack, two duckling peeps, short rising pluck; 900 ms |
| shuffle.wav | H6 | tap | 122 ms | Soft card-riffle, 120 ms |
| tick_select.wav | H7/E1-shared | tap | 46 ms | Shared toggle/chip tick, higher on select, 60 ms |
| tick_clear.wav | H7 | tap | 48 ms | Shared tick, lower on clear, 60 ms |
| sweep.wav | H7/G4 | tap | 200 ms | Soft 200 ms brush; filter Reset and Clear cache |
| drawer_open.wav | H8 | tap | 150 ms | 150 ms soft slide, rising |
| drawer_close.wav | H8 | tap | 150 ms | 150 ms soft slide, falling |
| result_win.wav | E1 | tap | 457 ms | Bright pock plus a quick rising third (C to E) |
| result_loss.wav | E1 | tap | 324 ms | Same pock one whole step lower, one warm note, no falling tone |
| format_singles.wav | E2 | tap | 79 ms | One pock |
| format_doubles.wav | E2 | tap | 141 ms | Two pocks 60 ms apart |
| soft_error.wav | E3 | confirm | 140 ms | One low wood-block bonk, 120 ms, no buzz |
| match_saved.wav | E4 | confirm | 944 ms | Pock, then rising two-note chime G to C in the reference's sine-bell timbre; 350 ms core |
| match_saved_run.wav | E4-alt | confirm | 995 ms | Alternative closer to the reference: pock into a quick A-C-D-E-G pentatonic chime run |
| match_updated.wav | E5 | tap | 91 ms | The pock only, quieter |
| pop_in.wav | E6 | tap | 90 ms | Rising bubble pop, 100 ms |
| pop_out.wav | E6 | tap | 90 ms | Falling bubble pop, 100 ms |
| snap.wav | E7 | tap | 70 ms | 70 ms snap tick |
| photo_added.wav | E8 | tap | 80 ms | Soft snap, 80 ms |
| whisk.wav | E8 | tap | 120 ms | Quick downward swish for photo remove, 120 ms |
| delete.wav | D1 | confirm | 260 ms | 250 ms soft paper crumple, falling slightly, neutral |
| sheet_up.wav | D2 | tap | 200 ms | The new_match swoosh without the pock |
| card_ready.wav | S1 | confirm | 856 ms | Camera-shutter click plus a short sparkle, 400 ms |
| card_swap.wav | S2 | tap | 90 ms | 90 ms tick, quieter than card_ready |
| share_send.wav | S3 | confirm | 257 ms | 250 ms upward whoosh |
| pro_unlocked.wav | P1 | celebration | 2348 ms | C-E-G-C-E marimba arpeggio with chime layer and one friendly quack, 1.6 s |
| pro_restored.wav | P2 | celebration | 1781 ms | First three notes of pro_unlocked, warmer, 900 ms |
| toggle_on.wav | G2 | tap | 49 ms | Pock pitched up, 70 ms |
| toggle_off.wav | G2 | tap | 51 ms | Pock pitched down, 70 ms |
| reminder_on.wav | G3 | confirm | 719 ms | Tiny bell, 250 ms |
| erase_done.wav | G5 | confirm | 420 ms | Soft 400 ms poof, neutral |
| export_ready.wav | G6 | confirm | 245 ms | Soft packed thump and zip click, 300 ms |
| import_done.wav | G7 | confirm | 944 ms | Two-note confirm chime, same timbre as match_saved |
| duck_pet_1.wav | G8 | tap | 100 ms | Peep |
| duck_pet_2.wav | G8 | tap | 179 ms | Chirp (two quick peeps) |
| duck_pet_3.wav | G8 | tap | 170 ms | Soft quack |
| duck_pet_4.wav | G8 | tap | 289 ms | Happy double-quack |
| reminder_notification.wav | Y1 | celebration | 992 ms | Pock-pock and a soft quack, under 1.2 s |

Demos (timing approximated from the code's animation delays, assuming Home appears 350 ms after Save):

- demo_save_flow_plain.wav: match_saved, then tally
- demo_save_flow_streak.wav: match_saved, tally at half volume, streak_up
- demo_save_flow_milestone.wav: match_saved, tally at half volume, streak_milestone
