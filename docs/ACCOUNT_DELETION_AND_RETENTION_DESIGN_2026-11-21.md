# Account / child-data deletion — separate from profile removal

**Current confirmed behavior:** deleting `users/{uid}/profiles/primary` deletes only that document. It does NOT erase doseLogs, feverLogs, vaccines, drafts, syrupScans, bookmarks or authoritative pharmacy consultation/audit documents. This is not an account deletion workflow. The Android app now offers a separately confirmed *local-only* health-data wipe; never imply that it deletes cloud data.

## Required verified account deletion design — not implemented / not deployed
1. Provide visible `Request deletion of my account and data` in-app and on a public website; verify current user with a recent Firebase Authentication sign-in / token and reauthentication.
2. Create an idempotent server-side deletion job scoped to the authenticated UID, not a user-supplied UID. Log state only with non-identifying job IDs: RECEIVED → AUTHORIZED → IN_PROGRESS → COMPLETED / PARTIAL_FAILURE.
3. Inventory all collections: `users/{uid}/profiles`, doseLogs, feverLogs, vaccines, bookmarks, visitDrafts, syrupScans, consultations, plus `pharmacies/{pharmacyId}/cases`, triage passes, staff audit logs and any storage or backups. Validate actual database ID and all deployed environments.
4. Determine which child health records can be erased and which pharmacist legal/clinical audit records may need retention under applicable laws. Separate retained minimal data from deletable user-facing records; policy needs legal counsel, not a code guess.
5. Delete nested Firestore subcollections with a privileged recursive deletion job and pagination/retry tracking. Do not just delete the parent document. Remove UID-linked data in all storage locations, while respecting the approved retention schedule.
6. Keep a short-lived non-PHI deletion receipt with job status and completion date. Delete/revoke Firebase Auth identity last, only after data operations and appropriate verifications. Ensure retry after partial failure.
7. Revoke app tokens, clear local DataStore and widget summaries, clear pending notification/backup remnants, ensure migration is permanently blocked for deleted profiles.
8. Test: deletion from one device while second device logged in; Firestore nested orphan collections; >500 docs; service interruption; bad/expired credential; account A vs B; audit retention; idempotent retry; restore from backup.
9. Publish privacy policy with contact and deletion URL; if distributed via Google Play, provide in-app and out-of-app account-deletion entry points and disclose retention conditions.
10. Do not activate the clinical service, add production data migrations, or modify production data before staging security review and explicit authorization.

Important Firebase behavior: https://firebase.google.com/docs/firestore/manage-data/delete-data
Google Play account deletion requirements: https://support.google.com/googleplay/android-developer/answer/10144311

## Current local-only erasure behavior
- `ManoPreferencesRepository.clearLocalChildHealthData()` removes DataStore values for child profiles, weight/height metadata, logs, nutrition, vaccination, drafts, scans, safety checklists, bookmarks and onboarding.
- `FeverTimelineWidgetStore.clearStoredSummary()` clears any old fever/dose summary shown on the home-screen widget.
- `ManoSensitiveCacheCleaner.clear()` removes MANO's temporary child-health PDF exports (`visit_draft_pdfs/`), syrup-label photos (`scanned_syrups/`) and cached shared care images (`shared_cards/`) on the explicitly confirmed local wipe. Unrelated visual/app caches are retained; a cache cleanup failure must not be reported as a complete wipe.
- The active in-memory visit draft, nutrition output, scanned label, triage checklist and documented Q&A are reset when the signed-in UID changes, and when local erasure is confirmed. Persistent guest records remain until separately erased.
- Explicitly copied pharmacist follow-up text is marked sensitive for Android 13+ clipboard previews, but clipboard contents can still be accessible to applications or remain until replaced; users are warned.
- A two-step explicit UI confirmation distinguishes *local data removal* from *cloud profile document removal*. The latter still leaves other cloud collections intact.
- This local reset does **not** guarantee erasure of the Firebase SDK on-disk Firestore cache. In-app copy instructs caregivers to sign out and use Android Settings > Apps > Manokid > Storage > Clear storage for full device-level app data removal.
- No irreversible erasure of production cloud data has been attempted.
