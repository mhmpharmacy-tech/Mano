# MANO Kids — clinical content release gate

All clinical content in the static web prototype is **pending scientific approval**.

The machine-readable `clinical-content-status.json` registers the site-level release status, section-level risk, and candidate source URLs. Those URLs are not proof that the content was checked. The status must only be changed after an appropriately qualified reviewer records the decision, exact source versions, dates, and content edits.

Before any public healthcare release:
1. Check every warning-sign path and emergency escalation in the intended jurisdiction, including infant fever.
2. Verify every patient-facing claim against its exact dated guideline text (not merely a home-page link), and record the reviewer.
3. Confirm readability for families, accessibility and UI semantics on mobile screen sizes.
4. Approve a child-data privacy notice, clinical service limitations, hosting region and retention/deletion strategy.
5. Ensure no user-facing screen claims automated dosing, diagnosis or pharmacist sign-off.
6. Require a passing CI check. `noindex,nofollow` must remain while the site is unreviewed.
7. Keep the synthetic staff panel separate from patient submissions. No user data goes to Firebase from this web prototype.

**A green automated test is not medical certification.** This branch does not deploy to `manokid.ir`.
