# MANO Kids — coordinated five-team review (2026-11-17)
**Stage:** internal preview; 3 independent draft PRs; no production deployment. `manokid.ir` is not configured in this branch.

## Product-level definition of done
The parent must recognize the purpose and limits in under one screen, reach a relevant category in at most two taps, recognize emergency symptoms without receiving false reassurance, and never mistake synthetic pharmacist workflow for a live service. No personal health data is submitted by the public web preview.

| Team | Existing verified baseline | Changes implemented this iteration | Acceptance test and remaining gate |
|---|---|---|---|
| **Visual** | Original MANO character, blue/teal/cream, real screenshot baseline | Increased smallest mobile text sizes, stronger focus, established an editorial transparency block, high-visibility urgent action, reserved illustration shape | Desktop/320–430px responsive screenshot QA, no detected serious contrast issue; image compression remains a launch optimization |
| **Code** | Node smoke, Chromium E2E, axe-core, no network/persistence in demo | Added Persian/Arabic decimal parser, danger-first tests, automatic SEO/content and image budgets | Green GitHub workflow on latest head; external threat model, device QA and real monitoring not yet done |
| **Context / Content** | 5 draft content sections, initial candidate NICE/AAP/WHO links, no claimed clinical sign-off | Added visible provenance status, editorial article contract, explicit non-diagnostic text and age-linked emergency escalation | Each medical claim must have source, date, signed specialist review; no content marked reviewed |
| **SEO** | fa-IR markup, descriptive meta, index blocked | OG labels, content-intent architecture, prelaunch indexing safeguards and canonical/sitemap plan | Keep noindex; do not issue unverified clinical schema, sitemap, fake authorship or redirects before verified domain and clinical release |
| **UI/UX** | Clear landing/6 pathways, local search, FAQ, training-only pharmacist form | Persian numerals in fever field, programmatically announced danger result, direct 115 action labelled **Iran only**, Escape search reset, larger touch/readability | End-to-end cases: under-3-month fever (including Persian digits), emergency warning regardless of missing age, validation errors cannot look safe, fully keyboard operable |

## Cross-functional product guardrails
1. **Critical safety:** no automated doses, diagnosis, normality guarantee or fake staff approval.
2. **Privacy:** no child-data analytics, local storage, form transmission or tracking in this static web preview. Demonstration pharmacist cases are synthetic only.
3. **Editorial:** the website is **NOT_APPROVED_FOR_PUBLIC_CLINICAL_RELEASE**; the editorial section states that no named reviewer has signed off.
4. **Performance:** keep static files small and original mascot bundled locally. Current uncompressed imagery is approximately 1.8MB combined; real image compression and HTTP cache strategy remain open.
5. **Operations:** GitHub CI passing is not equivalent to production monitoring. Firebase staff service is separate PR #3 and intentionally disabled; Android is separate PR #1.

## Release decision matrix
| Critical gate | Owner | Required evidence | Current |
|---|---|---|---|
| Pediatric medical content approval | Clinical content lead | Signed review of every item; dated references | BLOCKED |
| Child privacy, secure handling, cloud deletion | Security + privacy | Threat model and integration tests | BLOCKED |
| Production staff onboarding and audited consultations | Backend + pharmacy operations | Verified pharmacist workflow, role revocation, support SLA | BLOCKED |
| Iran usability and accessibility | UX/Visual | Parent sessions on Android/iOS, manual screen-reader QA, limited connectivity | PENDING |
| Search launch and domains | SEO/DevOps | HTTPS, canonical/redirect, Search Console, reviewed sitemap | BLOCKED |
| Automated static QA | Code/UX | Green Node, Chromium, axe, visual screenshots | VERIFY LATEST CI |

## Suggested practical sequencing
1. Close static web QA and localized input validation first.
2. Clinical pharmacist + pediatrics review the **warning messages before any public exposure**.
3. Research 5–8 actual parents' top questions and validate navigation labels; no patient histories in research data.
4. Reduce heavy images with reversible asset optimization, re-test for visual identity preservation.
5. Only after external prerequisites: staff onboarding, private-by-default backend, monitored limited pilot.

This document names team *responsibilities*, not five actual employees or completed human expert reviews.
