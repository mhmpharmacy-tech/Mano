# MANOKID Pharmacy — 50-perspective synthesis and practical pilot gates

Status: **planning + safe demo features**, not proven commercial traction. The "50 pharmacies" were simulated personas, **not interviews**.

## Decision
Build the smallest verified bridge between a child's caregiver and their own pharmacy without making clinical intake, paid ranking, referral traffic, sales attribution or on-call availability claims.

### Four paths (not four different applications)
| Path | Hypothetical target | Current function | Still needed |
|---|---|---|---|
| MANO Start | Low-tech, low-budget, traditional | Generic educational sharing, with no child data or sign-in; staff-verified shareable pharmacy introduction link | Verified QR asset, print template, app store/website landing page |
| MANO Connect | New, family-neighborhood shops | Existing parent consultation flow, signed-in server-listed pharmacy selection, one-way verified invite deep-link | Parent-controlled favorite pharmacy, follow-up reminders, safety-first routing |
| MANO Clinical Desk | Busy, pediatric clinic-adjacent | Existing server-gated pharmacist inbox and state workflow | Capacity windows, shift ownership, bounded request queue and service metrics |
| MANO Pro | Multi-branch / advanced | **Not implemented** | Aggregated KPI and permissions, software integration after value demonstrated |

**No exclusivity:** the caregiver can select another verified pharmacy. Invite IDs are public navigation hints; they never authorize access, bypass authentication, grant staff privileges or submit clinical requests by themselves.

## Implemented in this branch
- Verified staff panel includes an educational-only share card (MANO Start) without creating patient data.
- Staff with a server-verified pharmacy ID can copy/share `https://manokid.ir/pharmacist?pharmacy=PHARMACY_ID` using Android's share sheet. No made-up QR code, no tracking token.
- URL parser accepts only known MANOKID hosts and a strict public pharmacy ID. Parent route waits for a *live accepting* pharmacy returned by server.
- Failed/unverified invitation stays unresolved; it must not automatically select some other shop. Parent can explicitly switch to a verified pharmacy.
- Submission UI requires a verified selected pharmacy when signed in. Anonymous mode clearly labels a local-only demonstration.
- Inbox counters are explicitly counts in the **currently loaded list/filter**, not monthly performance, patient growth or monetary revenue.
- No forced pharmacy signup, account-data collection, payment, paid ranking, unlicensed medicine delivery or clinical activation.

## Mandatory before distributing invitation links
1. Verify both website domains + Android App Links/assetlinks on actual production signing certificate and Android devices.
2. Ensure `/pharmacist?pharmacy=...` has a valid hosted web fallback if app is not installed. Until then, do not print links on public pharmacy collateral.
3. Add a server-signed, non-PHI referral event only after separate privacy review and explicit consent. Never count opening a link as a pharmacy visit or sale.
4. Validate pharmacy registry and onboarding process, capacity/acceptingCases state, staffing and genuine reply schedule.
5. Complete real-device, privacy/data deletion, clinical signoff and branch-protection release gates documented elsewhere.

## 12-pharmacy study, 4 archetypes
Recruit **three pharmacies each**: (a) busy pediatric-clinic adjacent, (b) new low-traffic, (c) established traditional, (d) price-sensitive / low-connectivity. Vary region, ownership, digital confidence and visit volume; document selection bias and actual characteristics.

Baseline: 2 weeks **before** introducing new features. Run 2 weeks staged staff training with **synthetic data** and safe educational material. After independent production privacy/security and clinical signoff, pilot with consenting adults and no real patient identifiers in public logs. Never jump from demo to real patient data by default.

### KPI definitions — measure, do not invent
| KPI | Numerator | Denominator / note |
|---|---|---|
| Staff activation | Pharmacies completing one actual validated starter action | Onboarded pharmacies; exclude demos |
| Family educational use | Consenting families voluntarily opening provided educational card | Distributed cards with reliable exposure; report as observed where measurable |
| Pharmacist workload | Observed minutes per completed nonurgent interaction | Compare with within-site baseline; safety outcomes as guardrail |
| Nonurgent intake appropriateness | Valid nonurgent cases submitted to verified pharmacy | All attempted cases, exclude correctly blocked urgent flags |
| Referral conversion | Consenting families who actually visit the referring pharmacy | Verified attributable referrals; **not** link click count |
| Retention | Consenting family revisits over 30 days | Distinguish educational return vs verified physical revisit |
| Willingness-to-pay | Owners making a concrete paid commitment at a stated price | Owners offered the same conditions, not a satisfaction survey |

### Stop conditions
Immediately stop live onboarding for: wrongly routed urgent triage, broken pharmacy authorization, cross-account data visibility, unverified medicine strength auto-fill, unauthorized access, privacy deletion inconsistency, or staff capacity/response claims without operational backing.

### Pricing experiments (hypotheses only)
- **Start:** low-friction entry without fee or contract, no clinical inbox required.
- **Connect:** optional subscription after proving return visits or useful follow-up.
- **Clinical Desk:** price according to measured time saved, not raw case volume or medicine sales.
- **Pro:** build only if several multi-branch customers request and fund integrations.

Do not present suggested prices or conversion rates as measured market data.
