# MANO / Manokid — integrated internal safety review (2026-11-21)

**MERGED INTO `main` FOR SOURCE DEVELOPMENT ONLY (PR #11, merge commit `212b4c5c5c165a8c3707a5f9019c0ca04fb75c2e`). NOT RELEASE-CLEARED. DO NOT DEPLOY.**

## Integration scope
- Based on `main` commit `d2beaddc154fc945ec8d03ff1121ddcec63d278b` (Android v14).
- Carries PR #10 fixes: pediatric age/weight inputs never synthesized, measured WHO growth only, sources/review status, three distinct Orbit illustrations, scanner handoff and clinical regression tests.
- Retains newest main improvements: profile validation and calendar sync, frosted glass image frame, modern 3D dock, IME padding, regression tests, version 14.
- PR #10's dedicated branded FAQ Orbit intentionally supersedes latest generic FAQ banner.
- Carries PR #9 default-off clinical endpoints and mandatory App Check, plus a compiled-entrypoint regression test. `MANO_CLINICAL_SERVICE_ENABLED` must remain unset/false.

## Release gates (all require explicit verification)
- [x] Android `assembleDebug` build passed on the integrated PR #11 head (`c7ff9b6b40dcc5cff64dbe85a2ad9a3a782ca439`).
- [x] Integrated Android Robolectric / Firebase emulator suite succeeded on PR #11 head (GitHub Actions run #44816409418).
- [ ] Final real-device QA, clinical sign-off, and separately authorized release/deployment verification remain pending.
- [x] Functions Node/TypeScript build and 16/16 backend/security tests, including compiled clinical-gate regression, passed on PR #11 head.
- [ ] Human approval of each pediatric/medication source claim and red-flag criteria; no blanket transfer of previous approvals
- [ ] Measured pediatric weight, exact age and verified concentration before any numerical dose; never use fabricated defaults
- [ ] WHO growth data matching six source tables; review length/height transition at 24 months and interpolated LMS
- [ ] Real-device screenshots: 360dp, 412dp, RTL, accessibility 200%, TalkBack; validate canonical MANO mascot identity
- [ ] E2E: urgent infant fever, caregiver consent, ambiguous syrup scan, offline states, consultation staff authorization
- [ ] Confirm App Check, Firestore isolation, local Android backup/deletion behavior, privacy/legal requirements
- [ ] Verify Iranian formulation labels and intended operating context

Passing CI is necessary but **not equivalent to clinical sign-off**. Merging source code into `main` does not approve a clinical release. No production deployment or real patient intake is authorized by this merge. External deployment configuration must be separately verified.

## Hardening merged from PR #13 (2026-11-21)

- [x] Merged CI-verified clinical/privacy hardening at `f7465401ccae0af2bc607f5cbd72c40f867fd94d` (PR #13).
- [x] Exact-head CI: 127/127 Android Robolectric/Firebase emulator tests and 16/16 Functions tests succeeded in GitHub Actions run #44822786422.
- [x] Syrup scanner refuses missing/inconsistent strengths, mismatched AI formulation hints, unrecognized oral forms and LOW/MEDIUM confidence for automatic syringe filling; saved scans must be reverified in current child context.
- [x] Uncertain 3–12 month age or under-six-month infants cannot receive generated solid-food plans; registered allergies block model-generated plans pending individual review.
- [x] Dose log distinguishes caregiver-administered time from entry time, including backwards-compatible legacy Firestore records.
- [x] Correct newborn fever >=38°C timeline escalation and server pass symptom-flow binding.
- [x] No automatic copying of unscoped offline child data into another Google account; profile deletion clears local birth/height metadata after cloud deletion succeeds. Cloud and local timelines / visit drafts / syrup scans / vaccines no longer co-display.
- [x] Disable Android Auto Backup and device-to-device extraction for child health data.
- [x] Old direct Firestore consultations cannot be created or marked reviewed by caregiver clients; authoritative consultation remains behind disabled-by-default Firebase Functions.
- [x] Unreviewed interaction pairs show CAUTION rather than a green success; legacy content no longer assumes item-level human approval.
- [x] Main push/pull request tests include `firestore.rules` and may be started manually via GitHub Actions.

### Remaining release blockers

- [ ] Individual, auditable clinical approval for every pediatric red flag, medication dose, nutrition policy, contraindication, interaction, WHO method and citation.
- [ ] Review behavior for offline-only/shared-device use, complete account deletion, data retention and migration authorization.
- [ ] Validate actual child age and birth-date input against verified date of birth; verify dose formulations and strengths against Iranian labels.
- [ ] Real-device UX/security: right-to-left navigation, 360dp, 200% font/TalkBack, backend staging settings, Firebase App Check and Firestore policy consistency.
- [ ] Confirm deployed Cloud Functions, database rules and client distribution status independently; repository CI does not attest production configuration.
- [ ] Final manual go/no-go and explicit production deployment authorization.

**Release decision: NO-GO.** Merging these audited *source-code changes* into `main` is not an instruction to deploy, activate clinical endpoints, or accept real patient cases.
