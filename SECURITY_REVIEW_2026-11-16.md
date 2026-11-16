# MANO Kids — safety remediation notes (2026-11-16)

## Purpose
This branch is a safety-first change set; it is **not** a production release or a clinical validation.

## What changed
1. Firestore consultations: parents can create a `PENDING` request and read their own records. They can no longer update review states or delete review records from the client. A trusted backend using Firebase Admin SDK must perform pharmacist-authorized state transitions. **No pharmacist review service is implemented by this patch.**
2. Client: removed buttons and methods that allowed the parent to advance consultation status, including demo mode.
3. Pediatric liquid dose visualization: `LiquidDoseVisualizerEngine.evaluate` is fail-closed and returns `isBlocked=true`, zero numeric recommendation and a context-sensitive warning for every formulation/age. Reverse inference from a dragged syringe is disabled. Scanner auto-fill and engine-driven 3D dose prefill are thereby blocked. This is **not** equivalent to a complete clinical safety review of all medication content or manual 3D interactions.
4. The 3D syringe view now ignores incoming personalized dose prefills and labels displayed volume as **training only**, while the scanner offers a pharmacist route for matched formulations even when autofill is blocked. The scanner still needs UX and clinical verification.\n5. AI syrup-label extraction no longer includes child health context in the prompt, since extracting a medicine label does not require it. Parent notice and consent to send images to the AI provider still require review.\n6. Backup: `allowBackup=false`, legacy backup rules and Android 12+ cloud/device-transfer exclusions reduce propagation of locally stored pediatric records. This does **not** encrypt DataStore on-device or remove backups previously created.

## Deployment warning
- Merging the PR alone does **not** deploy `firestore.rules` to the Firebase project. Deploy and verify rules separately using the correct Firebase database/project after running emulator tests.
- Until the rules are deployed, a previously released Android client may still use the old permissions. Review live rules before treating the issue as resolved.
- Current local DataStore persists child information in plaintext serialization. Production requires an explicit data minimization, encryption and retention decision, plus a privacy review.
- Do not publish auto-dose guidance or claim pharmacist sign-off without clinical sign-off and a vetted staff backend.
- Screens remain educational/demo; authentication and access controls require an end-to-end privacy/security review.

## Validation still required
- Run Firestore emulator tests: `node --test firestore.test.js` while a Firestore emulator is running at `127.0.0.1:8085` with dependencies installed.
- Run `gradle :app:testDebugUnitTest` and `gradle :app:assembleDebug` with the Android SDK and Gradle installed.
- Manually test syringe lab, syrup scanner, 3D prefill, consultation submission and read-only status on mobile.
- Review Firebase production rules, App Check, authentication, handling of child health data and audit logs.
- Verify backup behavior on representative Android 11/12+ devices before release.

No emulator, Android build, or security penetration test was executed by the assistant before opening this draft PR.
