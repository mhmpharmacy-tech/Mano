# MANOKID care cards — staging and release gates

**Status: source development only. Not deployed or clinically release-cleared.**

## Required setup (do not guess configuration)
1. Configure Firebase Hosting for the SAME approved Firebase project and Firestore database as Android.
2. Register the web app and its intended real domain for Google Sign-In and Firebase App Check using reCAPTCHA Enterprise. Supply `/care/app-check.json` at deployment with `{"siteKey":"<registered public web site key>"}`; the site key is public, but the associated site/domain configuration and API protection must be checked. Hosting provides `/__/firebase/init.json`.
3. Serve `/care/` over HTTPS with `Cache-Control: no-store`, test CSP and referral privacy, and verify the right Firebase Functions region and authenticated session.
4. Keep `MANO_CLINICAL_SERVICE_ENABLED` unset/false. Do not enable it until separate clinical, security, privacy and real-device/web checks are recorded and production deployment is expressly authorized.

## Care-card security contract
- Server MUST authenticate the caller, verify pharmacy staff custom claims **and active staff directory membership** for pharmacist actions, and match parent UID for family actions.
- A pharmacist cannot issue a care card while the originating case is `PENDING` or `RECEIVED`. Issuance requires `REVIEWED` status and `parentResponse.attestationConfirmed===true`, plus explicit issuance confirmation. These checks are on the server, not only the Android/web buttons.
- Family endpoints remove internal event actor UIDs. Internal staff views still retain them for authorized audit purposes; never publish these in family payloads, screenshots or debug logs.
- Idempotency keys are scoped to actor and case. Reusing a key for a different action or text is rejected, avoiding false success. Retry with unchanged action and text only.
- A family medicine-use report is **not** a measured administered dose, dose quantity, or exact time of taking medicine. The record captures only the server's receipt time. Android/web must display this limitation and explicitly confirm the action. It must not populate a dose timeline automatically.
- Closing follow-up prevents new dose reports and messages. Do not imply that viewing a care card is proof of adherence.

## Staging verification, synthetic data only
- Verify PENDING → RECEIVED → REVIEWED clinical gate. Try issuing cards before attestation: must fail even when a client forges `confirmed=true`.
- Verify staff from a different pharmacy, revoked/suspended staff, unrelated parents, signed-out users: all denied.
- Verify parent list + individual reads never expose `actor`, staff UID or `internalNotes`; staff inbox still has authorized audit fields.
- Confirm the same request ID does not duplicate an event, and a reused ID with a different action or text fails.
- Check explicit family confirmation for usage reports; they must not claim actual dosage or time of ingestion.
- Test Google sign-in/out between caregiver A and B on web and Android; no stale card or history from A should display for B.
- Verify Android 360dp, 412dp, 200% fonts, RTL, TalkBack; web narrow screen, keyboard focus and accessible controls.
- Test real hosting fallback, app links and 404 cases before printing pharmacy QR invitations.

## Remaining release blockers
Full account deletion/retention, real device + web browser QA, deployed App Check and Firestore rule verification, human-reviewed medicine strength/photos, protected `main` branch, and explicit production authorization remain independent NO-GO gates.
