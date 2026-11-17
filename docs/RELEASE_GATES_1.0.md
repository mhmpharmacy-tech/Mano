# MANO Kids 1.0 — internal release gates
Decision: NO GO for public clinical launch or real patient intake. Keep main unchanged.
Baseline ec54c39bb0b1601a44c6ca72e6c685bbadc09a36: safety, browser and API CI passed (runs 44185938293, 44185938274, 44185938400). Subsequent commits require fresh CI.
## Acceptance matrix
| Area | Required evidence | Status |
|---|---|---|
| Android | Debug build, full emulator tests, real devices, RTL, TalkBack, large text, dark mode, all six cards | Baseline CI passed; device pending |
| Web | Chromium/axe, mobile parents completing tasks, performance on slow connection | Baseline CI passed; human testing pending |
| Brand | Six original licensed WebP files byte-verified, vector fallback tested, human visual approval | Original binary patch pending |
| Clinical | All 54 snapshot-bound review tasks; qualified reviewer identity separately verified; dated sources and Iranian applicability | Pending |
| Iran catalog | Manufacturer label, strength, formulation, identifier, date and independent pharmacist verification per SKU | Empty template; no inferred products |
| Privacy | Threat model, encrypted storage migration, key loss, cloud deletion/export, cache and backups, child consent and retention | Pending |
| API | Emulator callable integration, staff revocation, tenant isolation, pass replay and expiry, audit, clinically approved triage issuer | Policy/rules baseline passed; integration and issuer pending |
| Hosting | TLS, staging access control, headers, domain redirects, rollback, legal review | Pending |
| SEO | Clinical approval before indexation; canonical and sitemap verification | Keep noindex |
| Pilot | Synthetic data only until privacy/clinical gates; observed family usability and pharmacist workflow | Pending |
## Pilot acceptance targets (proposed, not measured)
- 100% of curated urgent scenarios show escalation, no false reassurance.
- Zero cross-tenant reads/writes and unauthorized review transitions in test suite.
- At least 90% of 10–15 participating adults complete core navigation tasks without help; no real health data in this round.
- Zero unresolved critical accessibility or clinical defects.
## Operational controls
Do not enable MANO_CLINICAL_SERVICE_ENABLED; do not deploy or merge to main. Require checks and independent review through repository branch protection before production merge. Do not treat local JSON clinical review packets as signed authorization. Never add patient data to GitHub issues or test fixtures.
Rollback: retain existing main and immutable CI artifacts; any later deployment needs a tested rollback and incident response owner.
