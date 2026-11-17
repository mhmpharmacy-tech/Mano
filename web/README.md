# MANO Kids web preview — Persian RTL

This is an **independent, static, educational web MVP**, located at `web/`. It reuses the original MANO character images from the Android app through Git object references: `web/assets/` contains actual local copies, not externally hosted hotlinks.

## Run locally

Open `web/index.html` in a browser or run a static server:

```sh
python3 -m http.server 8080 --directory web
```

Visit http://localhost:8080/. No build steps, backend, or credentials required.

## Included

- Responsive Persian RTL homepage and original MANO mascot.
- Keyword filter across six sections: symptoms, medicines, skin, growth/nutrition, FAQ and pharmacist preparation.
- Deterministic **red-flag information only**, not an algorithm claiming to rule out illness.
- Under-3-month fever >= 38.0°C routes to urgent evaluation messaging.
- Local-only copyable questionnaire template for in-person pharmacist consultations.
- No APIs, no patient account, no clinical data storage, no automatic prescriptions or dosing, no real pharmacist submission. **Do not say the service is live.**
- Search, navigation, FAQ and controls function without third-party libraries.
- `web/staff-demo.html`: a separate **synthetic-only** pharmacist panel with category, status, safety and last-contact filters, comparison of previous/current notes, simulated safety-gated review and printable summary. No real patient data or server integration.

## Test

```sh
node --check web/logic.js
node --check web/app.js
node --test web/tests/smoke.test.cjs
```

The `web/logic.js` helper deliberately returns caution rather than "safe" when no specific red flag is chosen.

## Before public launch

1. Have a licensed pediatric clinical reviewer approve every public-facing paragraph, the warning-sign flow and all references. Revise or delete unverified content.
2. Develop an HTTPS web backend with separate parent and pharmacist roles, trusted staff claims, consent, retention and deletion policy, audit logging and a staged test environment before accepting personal information.
3. Create accessible article pages and a verified content publishing process (editor, review date, version and sources).
4. Perform real mobile/browser accessibility checks, security/privacy checks, performance testing, and user testing with Persian-speaking parents.
5. **Remove** the `<meta name="robots" content="noindex,nofollow">` only after clinical approval and actual public launch readiness.
6. Configure `manokid.ir` static hosting / TLS / canonical redirects in the hosting account. This repository change does **not** deploy a live website.

## Boundaries

The source Android app is Kotlin/Jetpack Compose. The static web starter is not an Android build or a port of its full navigation, and Firebase has **not** been enabled. The website works as an educational preview only.

Design favors a trustworthy teal/blue palette, a restrained child-friendly experience, concise Persian copy and MANO as the brand anchor without claiming AI has issued medical advice.
## Six-module ecosystem internal review

The home page now links Drug, Growth, Skin, Nutrition, Care and Connect, plus eight named draft articles. Growth uses synthetic data only; Connect uses fixed scripted examples and has no live account, intake, triage pass or pharmacist messaging. All clinical material is unapproved.

A local internal-review bundle can be built with Node 22:

```sh
node web/tools/build-preview.cjs ./mano-preview-local
```

Choose a new output directory, open its index.html, and do not publicly host it. The workflow uploads this allowlisted bundle only after smoke and Chromium suites pass. The bundle includes local assets, static pages and the synthetic staff demonstration only, plus SHA-256 checksums. External evidence links require internet when deliberately opened. No real child data should be entered even in local review.

## Internal clinical review
Extract the generated preview ZIP and open clinical-review.html. The 53 review tasks cover the whole home, five registered sections, eight articles, six full modules, 32 entries and the synthetic staff flow. Review all relevant source text, local applicability and actual pages; record approval, changes or hold. Export JSON locally between sessions and import only into the same snapshot. The hash binds the reviewed build, not a digital signature or proof of reviewer credentials. The workspace does not update approval metadata or deploy; do not enter patient information. Source tree without generated review-data.js is not the working review bundle.
