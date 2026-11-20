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
- [ ] Complete Robolectric / Firebase emulator suite and release-candidate revalidation (see GitHub Actions run #44816409418).
- [x] Functions Node/TypeScript build and 16/16 backend/security tests, including compiled clinical-gate regression, passed on PR #11 head.
- [ ] Human approval of each pediatric/medication source claim and red-flag criteria; no blanket transfer of previous approvals
- [ ] Measured pediatric weight, exact age and verified concentration before any numerical dose; never use fabricated defaults
- [ ] WHO growth data matching six source tables; review length/height transition at 24 months and interpolated LMS
- [ ] Real-device screenshots: 360dp, 412dp, RTL, accessibility 200%, TalkBack; validate canonical MANO mascot identity
- [ ] E2E: urgent infant fever, caregiver consent, ambiguous syrup scan, offline states, consultation staff authorization
- [ ] Confirm App Check, Firestore isolation, local Android backup/deletion behavior, privacy/legal requirements
- [ ] Verify Iranian formulation labels and intended operating context

Passing CI is necessary but **not equivalent to clinical sign-off**. Merging source code into `main` does not approve a clinical release. No production deployment or real patient intake is authorized by this merge. External deployment configuration must be separately verified.
