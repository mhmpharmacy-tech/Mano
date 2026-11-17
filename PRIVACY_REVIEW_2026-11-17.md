# MANO Kids — child-data privacy deletion audit
**Date:** 2026-11-17  
**Status:** Draft remediation, not approved for clinical/public release

## Problem discovered
The child profile screen previously labeled its existing deletion button "حذف کامل" ("delete completely"), but the handler deleted only the primary child profile. Other pediatric records remained in Preferences DataStore, including fever/medication logs, vaccine records, syrup scans and nutrition plans. The profile summary JSON was constructed using direct string interpolation and could become malformed with user-supplied quotation marks or escaped characters.

## Implemented in this PR

1. **Accurate controls:** Profile-only deletion is labeled "حذف فقط پروفایل". A separate "پاک‌سازی سوابق آفلاین مانو" control requires an explicit confirmation dialog. It is disabled while a cloud account is connected to avoid an immediate or confusing resync.
2. **Atomic offline purge:** `ManoPreferencesRepository.clearAllLocalChildHealthData()` removes child profile, alias, age band and birth date, growth height, visit drafts, nutrition plans, medicine-safety selections, vaccine records and reminder keys, syrup scans, fever/dose logs and checked milestones in one Preferences DataStore transaction. Reminder opt-in becomes `false`.
3. **Separate caches:** The fever widget's SharedPreferences snapshot is cleared and launcher widgets refreshed. Existing local vaccine and fever notifications are dismissed and vaccine WorkManager schedules are asked to cancel. The worker checks reminder opt-in again just before notifying; asynchronous cancellation is still not a guarantee against all device-specific races.
4. **Valid JSON:** `ChildProfilePrivacy.exportProfileSummaryJson` uses Kotlin Serialization's JSON builder rather than unsafe string interpolation.
5. **Regression tests:** Verify persisted values are removed across categories, cosmetic preferences/bookmarks stay, widget cache is reset, unusual Unicode/escaped profile values produce valid JSON and a second UI confirmation is required.

## Scope limitations — must not be misrepresented

- **This is an app-level offline-record purge, not cryptographic secure erasure.** Flash memory, device-level snapshots, logs and historical backups may retain remnants outside app control.
- **Does not delete cloud Firestore documents** or Firebase's possible on-device persistent cache, Google account data, previously shared/exported copies or any third-party AI-provider records. Signing out alone is not cloud-data deletion.
- Local child-health records in **Preferences DataStore remain plaintext** in this patch. Dedicated encryption at rest, a safe migration plan, key-loss recovery and key-rotation policy are not yet implemented.
- A parent-facing cloud deletion/export workflow and verified backend erasure across collections/subcollections are still missing. No claim of GDPR or Iranian-law compliance is made.
- Reminders/notifications may require additional device-specific verification.
- Firebase app configuration, live databases and public endpoints have **not** been modified or deployed by this PR.

## Required prelaunch evidence

- Test UI delete and cancellation under Robolectric plus manual Android device runs across supported API levels.
- Security review: storage threat model, managed encryption at rest, migration from the old plaintext DataStore, app-switcher previews and screenshot permissions where relevant.
- Cloud data inventory, deletion workflow including descendants, audit retention exceptions, staff access revocation, and a written transparent child/parent privacy notice.
- Verify that no potentially sensitive Firebase cache survives an advertised full deletion workflow. Until then call this feature **offline record purge** only.
- Conduct an independent pediatric clinical review of every displayed medical rule, and a legal review suitable for the operating jurisdiction.

**Release decision:** NO GO for patient-data production until the outstanding requirements are addressed and approved.
