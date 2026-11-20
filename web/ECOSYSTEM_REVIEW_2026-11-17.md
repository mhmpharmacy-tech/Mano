# MANO Kids — ecosystem sprint / six connected educational modules
Status: INTERNAL DRAFT — not clinically authorized for use, not deployed.
Five workstreams are responsibilities, not five separate human teams.

## Scope and product decisions
- Nutrition (5 cards): complementary feeding around six months, responsive feeding, choking prevention, no honey before one year, formula preparation. No personalized meal plans, nutrient calculations or brand rankings.
- Care (5 cards): safe sleep, dehydration, suspected poisoning, fever documentation, measuring liquid medicine. A reminder checklist does not certify a safe home or authorize treatment.
- Connect (4 scripted educational examples): liquid medicine ambiguity, supplement overlap, urgent breathing difficulty, missing information. No patient inputs, staff communication, automatic nonurgent classification, triage pass or token. Urgent example displays referral; uncertain example cannot issue clinical clearance. Native no-JS view shows all fixed examples.
- Six home destinations and cross-links among them. Local mascot assets reused; no newly commissioned images or clinical photographs.
- Visual: distinct topic accent surfaces, responsive cards, compact typography. Code: static CSP connect/form restriction, explicit disabled controls before JS; no backend integration. Context: source registry, no human review. SEO: distinct titles/descriptions and noindex. UX: source links, fixed examples, accessible search, keyboard and no-JS fallback.
- Sources consulted 2026-11-17: 
  - NUT_WHO: https://www.who.int/news-room/fact-sheets/detail/infant-and-young-child-feeding — Complementary feeding — Relevant full web-page section opened; clinical review pending
  - NUT_CDC_SOLIDS: https://www.cdc.gov/infant-toddler-nutrition/foods-and-drinks/when-what-and-how-to-introduce-solid-foods.html — Introduction of complementary foods — Search excerpt reviewed; verify full page before clinical release
  - NUT_CDC_CHOKING: https://www.cdc.gov/infant-toddler-nutrition/foods-and-drinks/choking-hazards.html/ — Choking prevention; foods and preparation; March 11, 2026 — Relevant full web-page sections opened
  - NUT_CDC_AVOID: https://www.cdc.gov/infant-toddler-nutrition/foods-and-drinks/foods-and-drinks-to-avoid-or-limit.html — Honey before 12 months; added sugar; April 14, 2026 — Relevant full web-page sections opened
  - CARE_SLEEP: https://www.healthychildren.org/english/ages-stages/baby/sleep/pages/a-parents-guide-to-safe-sleep.aspx?form=HealthyChildren — Back to sleep; firm, flat sleep surface; loose bedding — Full web page opened; relevant sections searched; human review pending
  - CARE_DEHYDRATION: https://www.nhs.uk/conditions/dehydration/ — Urgent advice; immediate action required — Relevant full web-page sections opened
  - CARE_POISON: https://www.nhs.uk/conditions/Poisoning/ — Immediate advice; while waiting for medical help; prevention — Relevant full web-page sections opened
  - CONNECT_TEACHBACK: https://www.ahrq.gov/patient-safety/reports/engage/interventions/teachback-mod.html — Slides 11, 13, 15 and 16: teach-back — Relevant full web-page sections opened
- Context-specific recommendations must be reviewed against Iranian guidance, language, product label, emergency pathways, feeding conditions and child-safety rules. No claim of local regulatory compliance.

## Backend readiness and clinical boundaries
PR #3 remains disabled, unintegrated and draft; clinical intake requires a single-use server-issued nonurgent pass, issuer not implemented. A client-side scripted example must NEVER be used as an issuer or permission signal. Prerequisites: signed pediatric protocol, real verified pharmacist onboarding/revocation, availability and escalation policy, consent, retention/deletion, abuse controls, transaction tests/end-to-end audit and production security review. No patient-data collection, main merge, production deployment or public indexing.

## Acceptance and metrics for later parent pilot
Automated tests: local file links, source mappings, noindex, five viewport widths, default plus urgent/uncertain Connect a11y, no-JS fallbacks and routing. Passing CI does not test clinical validity.
Proposed pilot (not measured): parent can locate topic within two navigation actions and paraphrase next action; evaluate task completion, time to correct destination, emergency routing comprehension, comprehension of no-live-service label and false-reassurance incidents. No telemetry or personal data in this preview. Test with synthetic cases and consented observers; actual targets and clinical safety criteria require approval.

## Second-pass reviewer package and accessibility
- Explicit screen-reader status for emergency and uncertain example, hidden examples close previously expanded notes.
- The allowlisted build-preview script checks registry, draft status, local links and noindex; packages only 16 HTML pages (home, staff demo, 8 articles, 6 modules), local scripts/styles/assets, registry, usage instructions and file checksums. No source repository, backend, CI reports, credentials or secrets included.
- Review ZIP is generated by CI only after smoke and browser jobs succeed. Do not publish the ZIP to a public host; noindex is not access control.
