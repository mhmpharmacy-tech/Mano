# MANO pharmacist API — safety-gated foundation (NOT DEPLOYED)

This is a draft architecture implementation, not a ready-to-use clinical service. It is independent of the Android app's current Firestore users/{uid}/consultations collection and the web preview's synthetic-only staff panel.

## Runtime
- Firebase Cloud Functions v2, Node 22, Firebase Admin SDK.
- Callable endpoints with Firebase Authentication and App Check enforcement.
- Entire service disabled by default; activation requires MANO_CLINICAL_SERVICE_ENABLED=true only after formal review. A valid MANO_FIRESTORE_DATABASE_ID must also be configured to target the intended named Firestore database; the service never silently defaults to another database.
- Multi-tenant storage: pharmacies/{pharmacyId}/cases/{caseId}.
- Public Firestore rules do not grant access to this namespace; only Admin SDK within the trusted Cloud Functions service may read or write.
- Staff role requires verified server-issued custom claims: manoStaffVerified=true, manoStaffRole=pharmacist, and manoPharmacyId matching case pharmacy. Administrative accounts cannot approve clinical case decisions.
- Workflow: PENDING -> RECEIVED -> REVIEWED, with append-only audit event on each transition.
- Parent only reads own case via callable function; urgent requests are rejected rather than queued.

## Endpoints
- manoSubmitCase: signed-in parent, versioned explicit consent, nonurgent only, pharmacy accepting requests.
- manoGetMyCase: owner UID checked on the server.
- manoListStaffCases: 25-case pharmacy-scoped inbox; verified staff only.
- manoReviewCase: atomic, auditable state transition with licensed/verified pharmacist identity, private clinical note, source and separately written explicitly approved parent-facing reply. The parent's case view never exposes private notes, staff UIDs or raw Firestore documents.

## Local checks
Run from repository root:
    cd functions
    npm install
    npm run lint
    npm test

## Mandatory prelaunch gates
1. Do not deploy or enable until clinical and security/privacy reviewers have approved data collection.
2. Publish an approved consent notice v1; establish retention, erasure, access logs and incident response. Staff internal notes and parent-facing replies must be separately authored, reviewed and stored. No clinical response is posted without explicit pharmacist attestation.
3. Create and revoke staff custom claims through a trusted administrative onboarding process; clients cannot mint their own claims.
4. Provision pharmacy registry documents out of band with verified ownership (active=true, acceptingCases=true).
5. Add server-side rate limiting, spam prevention, alerts, role revocation propagation and independent security testing. App Check does not replace these.
6. Test Firebase Emulator integration, permission denials, tenant separation, transaction races, ownership and audit immutability.
7. Build real authenticated parent/staff frontends with clear response-time expectations and emergency escalation paths. Client-supplied hasUrgentFlag=false is not a trustworthy triage determination; before enabling intake, enforce an independently validated, server-verifiable safety screen.
8. Verify cloud region, access from Iran, costs, local legal requirements, data residency and service availability.
9. Deploy Firestore rules to the correct database and confirm the default deny covers every pharmacy case and audit path.

Do not mistake this scaffold for a live pharmacist service. Root firebase.json intentionally does not register the functions folder for deployment.
