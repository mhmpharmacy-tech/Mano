# MANO / Manokid — real-device release QA protocol

Status: **NOT RUN on real devices. NO-GO until every mandatory gate passes.** All clinical endpoints remain default disabled. Use synthetic child data only.

## Test devices and evidence
- A low-end Android 9–11 phone at ~360dp logical width; an Android 14–16 phone at ~412dp; a tablet if supported.
- Persian (fa-IR), RTL system, 100% and 200% font sizes, TalkBack; both light and dark modes.
- Record: device model/OS/build/locale/font %, app commit, UTC timestamp, expected/actual, screenshot without PHI, video only with synthetic data, test owner and pass/fail.
- **Do not include real patient data, credentials, child names, phone numbers, Firebase config secrets or medical records in public issues/artifacts.**

## Mandatory scenarios
1. First launch in airplane mode: no account and no child health data → app navigates; no synthetic age, weight, WHO percentile or dose.
2. Child profile without exact age/weight: syringe volume stays disabled; newborn safety warnings retain highest priority.
3. Label scanner: acetaminophen vs ibuprofen mismatch, 120/5 vs 160/5 mismatch, 100 mg/mL drops, unknown strength, inconsistent OCR fields, LOW/MEDIUM confidence → no automatic mL filling.
4. Rescan saved bottle after switching child profile: previous verification cannot be reused.
5. Newborn/under-3mo at ≥38°C: immediate emergency banner AND dialog; record 38.0 and a subsequent 37.2 → no stale re-alert after the normal reading.
6. Infant 3–12mo with unknown exact age and infant <6mo: generative solid-food plan must be blocked.
7. Documented food allergy: generative nutrition plan blocked until separately reviewed; unreviewed answer not presented as signed-off evidence.
8. Dose given 14:00 and entered at 18:00: display interval from 14:00; invalid/future HH:mm is rejected; prior-day time resolves correctly.
9. Offline caregiver A data → sign in account B → verify no A fever/dose/vaccine/scan/draft/birth date/height is presented to B. Repeat after sign-out and process restart.
10. Profile deletion: confirmation required, wording explicitly says *profile only*. Check cloud profile absent; other cloud records remain and are not misrepresented as erased.
11. **Mano DataStore / widget erasure:** confirmation required; profile, vaccine, dose, fever, nutrition plans, scan data, drafts, milestone checks and widget summaries absent after restart while offline. Cloud records remain untouched; Firebase SDK cache is not guaranteed cleared by this in-app action. Test Android system Clear storage separately.
12. Account deletion: **NOT IMPLEMENTED**; separate verified server-side deletion and retention process required. Do not claim support.
13. Firestore emulator and staging: user A cannot access B; owners cannot mark any clinical consultation as reviewed or bypass triage; staff access restricted by pharmacy ID.
14. App Check: authorized signed build accepted, unauthorized client rejected; verify deployed server setting in Firebase console (repository code alone insufficient).
15. Accessibility: all action icons have labels, focus order correct in RTL, contrast accessible in dark mode, 200% typography no clipping, interactive hit-target ≥48dp.
16. UI/brand: same MANO character silhouette and glasses/logo/colors across all six pillars; Orbit images distinct per section, no cropped faces; tablet and older device scroll behavior.
17. Deep links for manokid.ir/manokid.com: verified Android assetlinks.json on both hosts, correct release signing SHA-256, safe fallback for malformed URLs.
18. Share report: recipients via Android chooser; no inadvertent logging of child info; exported data requires explicit user action.
19. Firestore rules + Android functions + production configuration compared after any approved deployment; no direct production enablement without separate authorization.
20. Regression after a cleared app store, a fresh install, app update and rotating the device.

## Manual release sign-off
| Gate | Owner | Required evidence | Status |
|---|---|---|---|
| Android & Firebase CI on exact PR SHA | Engineering | Actions success URLs | Pending current PR |
| Device matrix / RTL / accessibility | QA + caregiver | Redacted screenshots, test log | Pending |
| Per-item clinical evidence review | Pharmacist | Versioned item signatures | Pending record |
| Parent/child privacy and deletion | Security/privacy | Retention map + verified account erasure in staging | Pending |
| Real medicines in Iranian formulations | Pharmacist + label reviewer | Source/photo, expiry, approved strength table | Pending |
| Main protection & no bypass | Repo admin | Settings screenshot | Pending |
| Production release | Account owner | Explicit written release authorization | **NO-GO** |

## Main branch protection — GitHub Settings > Rules > Rulesets
1. New branch ruleset named `mano-main-required-ci` targeting `main`; set Active.
2. Require a pull request with at least one approving reviewer; require status checks `functions` and `Android debug build and Robolectric` from GitHub Actions.
3. Require branch to be up to date, block force pushes and deletions, restrict bypasses, and disallow direct pushes.
4. Recheck that `main` is shown as Protected after save. This integration cannot create repo rulesets, so an administrator must apply it in the GitHub UI.

References:
- GitHub: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/creating-rulesets-for-a-repository
- Firebase App Check: https://firebase.google.com/docs/app-check/enable-enforcement
