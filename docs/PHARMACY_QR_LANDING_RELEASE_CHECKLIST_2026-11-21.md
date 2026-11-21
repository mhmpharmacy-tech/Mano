# MANOKID Pharmacy — QR starter card and web fallback deployment checklist

**Release state: NOT DEPLOYED; NO-GO for public QR distribution.** This branch creates source code only.

## What exists in source
- Verified staff-only pharmacy QR generated **offline on-device** from `ManoDomainLinks.pharmacyInvitationUrl`. The QR contains only a public registry ID in an allowlisted HTTPS link, never parent/child identifiers, health data, authentication secrets or tracking codes.
- Android native print/save-as-PDF A4 handout with pharmacy name, branded MANOKID heading, QR, URL, nonurgent caveat, emergency warning and predeployment notice.
- RTL `public/pharmacist/index.html` browser fallback, compatible with Android intent link to `com.aistudio.manokids.rnkvpx`; validates a single strict pharmacy ID, never claims that a pharmacy is active, collects no form data, requests no backend, and sends no analytics.
- JS security test `scripts/verify-pharmacy-landing.mjs`, runnable via `node scripts/verify-pharmacy-landing.mjs`. Android/Functions test suite covers QR round-trip and malicious input.
- The server remains the **only** authority on active pharmacist staff, pharmacy registry, acceptingCases, triage and case submission. The web page never accepts clinical requests.

## Before any public printing or QR campaign
1. Verify ownership/control of `manokid.ir` and `manokid.com` and choose the canonical host `manokid.ir`. Ensure HTTPS certificates, redirect HTTP to HTTPS, enforce HSTS only after HTTPS validated.
2. Serve the repository's **public static root** from a reviewed website build. Map `/pharmacist` and `/pharmacist/` to `public/pharmacist/index.html` **without dropping `?pharmacy=ID`**. Keep a non-404 root page; the `/` fallback is currently provided as a source link only.
3. On Android with app installed, test both opening QR in Chrome and alternate browsers, and a not-installed device using the static fallback. The system may open the app directly if App Links are actually verified; never assume that from repository files alone.
4. Serve `/.well-known/assetlinks.json` with correct JSON MIME type, using the actual *production release/Play signing* SHA-256 fingerprint and package. **Remove the placeholder fingerprint**, confirm that the website publishes the actual file, and verify with `adb shell pm get-app-links com.aistudio.manokids.rnkvpx` on a test device. Do not copy a debug signing fingerprint into production.
5. Confirm the server-side pharmacy registry includes the intended verified pharmacy and its acceptance state. If paused/unknown, the app must NOT silently route the caregiver to a different pharmacy.
6. Validate parent consent, no urgent symptoms in the intake pathway, no tracking without explicit privacy review, no collection of child data from the static webpage, and no third-party QR generation traffic.
7. Check QR scan readability printed at 2.5cm, 3.5cm and 4.5cm, in bright light and dim light, low-cost laser prints and after print resizing. Browser fallback must be tested on 360dp RTL and with large fonts; print card must remain readable A4 and from Android Print to PDF.
8. Record exact Android SHA, domain configuration evidence, team reviewer, timestamp, screenshots with **synthetic** data and result for each scenario. Require manual release approval; the QR is an advertising entry point and must not be distributed until the complete landing and app distribution channels work.
9. Remove `noindex,nofollow` only after public-launch approval and SEO/privacy review. Add a real store installation path *only when published*, never a speculative app store link.
10. Confirm current public medicine delivery and online pharmacy legal constraints separately; this QR does not process prescriptions, sell or ship drugs, allocate shifts, book consultations or guarantee response.

## Remaining functionality, deliberately NOT implemented
- Live hosted page deployment and Android App Links production verification.
- Operational enable/pause acceptance by pharmacy shift + quotas and SLA.
- Paid pharmacy directory/ranking, chargeable membership, visits/conversion attribution, shared patient data and analytics.
- Full cloud-account deletion, release-approved clinical content, production case enrollment.

## Smoke checks
```bash
node scripts/verify-pharmacy-landing.mjs
gradle :app:assembleDebug :app:testDebugUnitTest
```

**No production Firebase deployment or clinical service flag change authorized.**
