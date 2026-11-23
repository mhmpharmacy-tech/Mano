# MANO Kid Care Home v3 — Android source integration

Date: 2026-11-23. Scope: **native Compose UI only**. Release status: **NO-GO** pending separate human review.

## What changed

- Introduced a calm, mobile-first Care Home layout, preserving the existing official MANO mascot asset with round eyes.
- Page structure: brand, real profile/empty state, short MANO introduction, two priority actions, today's care, direct tools, and safety notice.
- Added one reusable compact brand intro for symptom navigation, pharmacist requests, growth tracking and child records. Existing flows remain authoritative and are not replaced with generated mock screens.
- All actions invoke existing navigation callbacks. No network, database, permission, dose engine, Firestore rule or API contract was changed.
- Existing old orbital home remains callable for regression/backward compatibility. `ManoApp` opts into the new home.

## Design contract

- Persian RTL; retain Vazirmatn Material 3 type scale and the official robot (do not redraw eyes, silhouette or M emblem).
- Calm cream/off-white, forest-green ink, muted sage, sparing coral/blue and gold signals. No child photos; one functional icon per feature.
- Minimum interactive target 48dp; verify TalkBack, 360dp width, 200% system font and dark theme on actual devices before release.
- No fake child name, height, weight, clinical urgency status, vaccine date or medication reminder.
- Call-to-action paths: Home→Symptoms, Home→Ask Pharmacist, Home→Medicines, Home→Vaccines, Home→Growth, Home→Fever Dose Calculator, Home→Child Profile.
- In unverified / missing data states, prompt to complete real records rather than manufacturing a summary.

## Clinical safety and communication

- Symptom pathway is educational and not a diagnosis; existing pediatric triage engine and urgent-stop conditions continue unchanged.
- Pharmacist request is not described as a live or human-approved response until actually processed by a verified pharmacist.
- Dose entry remains linked to existing calculator safeguards; do not prefill absent exact age, weight or verified formulation.
- Vaccine due labels are only displayed when provided by existing vaccination summary.
- All pre-existing server and user-consent gates remain as configured, default disabled where specified.

## Follow-up QA before any production approval

1. Exact head Android debug assembly and Robolectric suite; compare failures with prior baseline.
2. Tests with no child profile, registered child, guest, signed-in adult, offline first run, switching child/account and sign-out.
3. Verify age/weight/vaccine summaries never cross accounts or bypass data validation.
4. Device matrix: 360dp and 412dp, light/dark, Persian RTL, 200% font, TalkBack focus, icon descriptions and 48dp targets.
5. Visually inspect original MANO eyes and crop on every target density; avoid multiple decorative icons per card.
6. Clinical source review and device/security/consent checks remain pending under docs/RELEASE_DEVICE_QA_2026-11-21.md.

**Source merges do not authorize production deployment, patient intake, or enabling clinical endpoints.**
