# MANO / Manokid — Code, Interaction & Visual Consistency Review
Date: 2026-11-20
Scope: static inspection of `main` source and small isolated remediations on audit branch.
Status: NOT release-cleared. No device/simulator screenshots or Android build were run in this session.

## Verified from code
- Android app uses Kotlin / Jetpack Compose, Persian RTL UI, theme tokens, centralized branded icon artwork, MANO character poses and navigation.
- HomeScreen delegates the six specialized pillar taps to ManoApp; inspected the navigation mapping:
  - Documented Q&A -> FaqRoute
  - MANO Safety -> MedicinesRoute + overlap tab
  - Product comparison -> MedicinesRoute + comparison tab
  - MANO Connect -> AskPharmacistRoute(parent)
  - MANO Follow-up -> AskPharmacistRoute(followup)
  - MANO Pharmacy -> AskPharmacistRoute(staff)
- The above only proves callbacks are wired in source; it does NOT prove correct real-device interaction, data loading, accessibility or screen rendering.
- `Mano3DIcons.kt` centralizes visual icons; `ManoEcosystemHub.kt` selects icon kinds and character poses.
- `WhoGrowthChartCard.kt` was pre-filling 9.6 kg and 75.7 cm when no corresponding user measurements were present.

## Remediations applied on this branch
1. `ManoEcosystemHub.kt`: MANO Connect uses branded `FAMILY` artwork, rather than reusing `PHARMACY`.
2. `WhoGrowthChartCard.kt`: empty unknown weight/height instead of injecting synthetic measurements; WHO percentile remains unavailable until a valid value is provided.
3. `ManoAuditRemediationTest.kt`: regression checks for distinct icon kinds and missing-data growth safety.

## Manual test plan — mandatory before any release
| Priority | Area | Interaction | Pass criterion |
|---|---|---|---|
| P0 | Emergency fever | Enter infant <3 months + fever >=38°C | Urgent referral, no inappropriate self-treatment dose |
| P0 | Syrup scanner | Permission denied / unreadable label / ambiguous concentration | Clear fallback, human confirmation; no invented concentration |
| P0 | WHO growth | Open without measurements, switch age/sex/metric | No synthetic child percentile; labels and units remain clear |
| P0 | Pharmacist access | Try unauthorized staff panel and write action | Server-side authorization enforced |
| P1 | Home orbital menu | Tap all satellite buttons; rotate/pause/restore | Correct navigation, no clipped touch targets on 360dp |
| P1 | Six specialized cards | Tap each card and use Back | Destination matches the mapping above; no dead ends |
| P1 | FAQ | Search with Persian/Arabic letter variants, expand/collapse | Equivalent search results and responsive controls |
| P1 | Forms & DataStore | Save/reopen child profile, fever and dose log offline | No data loss or unexpected overwrites |
| P1 | Vaccines | Change age and reminders | Schedules and permissions behave correctly; local schedule review |
| P1 | Accessibility | 200% font, TalkBack, RTL/LTR, high contrast, dark mode | All actions discoverable and text unclipped |
| P2 | Visual assets | Review six cards, module banners, mascot states at 360/412/600dp | Consistent palette, one subject per asset, readable crops |
| P2 | Performance | Measure cold start, scroll jank, large assets and memory | No obvious user-visible regression vs main |

## Visual consistency guidance
- Preserve core MANO silhouette, face, emblem, and established mint/teal/white palette.
- Use branded `PediatricIconKind` artwork for specialized modules; avoid repeated unrelated pictograms.
- The character can vary its pose according to function, but not identity. Caution pose is required for emergency contexts.
- Avoid cropping faces or mascot emblems in small cards. Check at 1x, 1.5x and 2x font scales.
- Do not regenerate or replace canonical mascot files without visual review of provided reference assets.

## Verification limitations
- GitHub status checks returned no statuses, and no pull-request workflow run was returned for the branch head at review time.
- New regression tests were committed but **not executed** here.
- No actual UI screenshots, physical-device run, Cloud Firestore permission test, clinical signoff, image quality measurements, or deployment verification occurred.
- Do not merge or deploy until Android build/tests, manual end-to-end and safety/privacy reviews are complete.
