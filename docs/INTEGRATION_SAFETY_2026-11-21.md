# MANO / Manokid — integrated internal safety review (2026-11-21)

**NOT RELEASE-CLEARED. DO NOT MERGE OR DEPLOY.**

## Integration scope
- Based on `main` commit `d2beaddc154fc945ec8d03ff1121ddcec63d278b` (Android v14).
- Carries PR #10 fixes: pediatric age/weight inputs never synthesized, measured WHO growth only, sources/review status, three distinct Orbit illustrations, scanner handoff and clinical regression tests.
- Retains newest main improvements: profile validation and calendar sync, frosted glass image frame, modern 3D dock, IME padding, regression tests, version 14.
- PR #10's dedicated branded FAQ Orbit intentionally supersedes latest generic FAQ banner.
- Carries PR #9 default-off clinical endpoints and mandatory App Check, plus a compiled-entrypoint regression test. `MANO_CLINICAL_SERVICE_ENABLED` must remain unset/false.

## Release gates (all require explicit verification)
- [ ] Android assembleDebug and full Robolectric / Firebase emulator tests on this *integrated commit*
- [ ] Functions Node/TypeScript build, security and compiled clinical-gate tests
- [ ] Human approval of each pediatric/medication source claim and red-flag criteria; no blanket transfer of previous approvals
- [ ] Measured pediatric weight, exact age and verified concentration before any numerical dose; never use fabricated defaults
- [ ] WHO growth data matching six source tables; review length/height transition at 24 months and interpolated LMS
- [ ] Real-device screenshots: 360dp, 412dp, RTL, accessibility 200%, TalkBack; validate canonical MANO mascot identity
- [ ] E2E: urgent infant fever, caregiver consent, ambiguous syrup scan, offline states, consultation staff authorization
- [ ] Confirm App Check, Firestore isolation, local Android backup/deletion behavior, privacy/legal requirements
- [ ] Verify Iranian formulation labels and intended operating context

Passing CI is necessary but **not equivalent to clinical sign-off**. No release pipeline, production service or real patient intake has been authorized.
