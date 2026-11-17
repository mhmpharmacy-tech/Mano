# MANO Kids Cloud Functions (Trusted Clinical Triage & Pharmacy Consultations)

This package implements the server-side Firebase Cloud Functions (v2) backend for **MANO Kids** non-urgent pediatric pharmacist consultations.

## Architecture & Security Model

- **Named Firestore Database:** All Admin SDK operations target `ai-studio-android-c61bb361-5d09-41d6-8602-26bbc07b52d7` (`getFirestore(app, FIRESTORE_DATABASE_ID)`), matching `app/src/main/res/values/firebase_applet_config.xml`.
- **Multi-tenant storage:** `pharmacies/{pharmacyId}/cases/{caseId}`.
- **Public rules isolation:** Public `firestore.rules` do not grant client access to `pharmacies/**`, `triagePasses/**`, or `rateLimits/**`; only Admin SDK within the trusted Cloud Functions service may read or write them.
- **Dual-layer staff verification:**
  - Staff role requires verified server-issued custom claims: `manoStaffVerified=true`, `manoStaffRole="pharmacist"`, and `manoPharmacyId` matching the case pharmacy. Administrative accounts (`admin`) cannot approve clinical case decisions.
  - The same UID must also have an **active, credential-verified directory record** at `pharmacies/{pharmacyId}/staff/{uid}` with fields `uid`, `pharmacyId`, `role: "pharmacist"`, `status: "ACTIVE"`, `credentialVerified: true`, `revokedAt: null`. `getPharmacyInbox` verifies this record live on every request; `reviewConsultationCase` reads it inside the Firestore transaction before committing. An old valid custom-claim token never overrides a revoked staff directory record.
- **Workflow & audit immutability:** `PENDING -> RECEIVED -> REVIEWED`, with an append-only `auditEvents/{eventId}` record written inside the same transaction on every transition. Internal staff notes (`internalNotes`) and parent-facing replies (`parentResponse`) are separately authored, stored, and filtered; no clinical response is posted without explicit `pharmacistAttestation=true`.
- **Parent access & intake gates:** Parents only read their own cases (`getParentConsultation`, `listParentConsultations`, stripping all internal staff notes). Consultation submission (`submitConsultationCase`) requires a server-issued single-use `NONURGENT` triage pass (`issueConsultationTriagePass`), not merely client-selected `hasUrgentFlag=false`.

## Commands

Run from `/functions` (or with `npm --prefix functions <script>` from repository root):

```bash
npm install
npm run lint
npm run build
npm test
# Optional out-of-band bootstrap of an active pharmacy & initial system admin claim:
MANO_BOOTSTRAP_PHARMACY_ID=mano_central_pharmacy MANO_BOOTSTRAP_ADMIN_UID=<admin_uid> npm run bootstrap:pharmacy
```

## Exported Callables & Schedules

- `listAcceptingPharmacies`: Returns verified, currently accepting pharmacies (`active=true`, `acceptingCases=true`) without exposing `pharmacies/**` in public Firestore rules.
- `issueConsultationTriagePass`: Evaluates pediatric symptoms/temperature/red flags deterministically and issues a single-use expiring `NONURGENT` pass only when no urgent signals exist.
- `submitConsultationCase`: Consumes a single-use `NONURGENT` triage pass atomically inside a transaction, checks `pharmacies/{pharmacyId}` (`active=true`, `acceptingCases=true`), enforces consent `v1` and rate limits, creates the `PENDING` case, and appends `CASE_CREATED` audit event.
- `getParentConsultation` / `listParentConsultations`: Parent-owned reads with `internalNotes` and staff UIDs stripped (backed by `firestore.indexes.json` collectionGroup index).
- `getPharmacyInbox`: Verifies pharmacist custom claims and reads `pharmacies/{pharmacyId}/staff/{uid}` live on every request before returning tenant cases.
- `reviewConsultationCase`: Executes `PENDING -> RECEIVED -> REVIEWED` transitions inside `db.runTransaction(...)`, re-reading `pharmacies/{pharmacyId}/staff/{uid}` inside the transaction before commit, enforcing `pharmacistAttestation=true` on `REVIEWED`, and writing append-only `auditEvents`.
- `upsertPharmacyRegistry` / `onboardPharmacyStaff` / `revokePharmacyStaff`: Trusted system-admin callables (`manoSystemAdmin=true`) for out-of-band pharmacy provisioning, credentialed onboarding, and immediate suspension + refresh-token revocation.
- `cleanupExpiredConsultationState`: Scheduled job (`every 6 hours`) purging expired `triagePasses` and stale `rateLimits` buckets.

## Mandatory Prelaunch Gates

1. Do not deploy or enable until clinical and security/privacy reviewers have approved data collection.
2. Publish an approved consent notice `v1`; establish retention, erasure, access logs and incident response. Staff internal notes and parent-facing replies must be separately authored, reviewed and stored. No clinical response is posted without explicit pharmacist attestation.
3. Create/revoke staff custom claims **and** active staff directory records through a trusted administrative onboarding process; clients cannot mint their own claims or modify membership. Verify pharmacist credentials and provide an immediate suspension process. Integration tests must verify a revoked record denies both inbox and review even when a cached token retains the old claims.
4. Provision pharmacy registry documents out of band with verified ownership (`active=true`, `acceptingCases=true`).
5. Add server-side rate limiting, spam prevention, alerts, role revocation propagation and independent security testing. App Check does not replace these.
6. Test Firebase Emulator integration, permission denials, tenant separation, transaction races, ownership and audit immutability.
