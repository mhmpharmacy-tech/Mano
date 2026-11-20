# MANO Kids — three-module internal prototype

Status: NO GO for patient-data or clinical production. No separate human expert teams reviewed this.

- Drug: six education cards, Persian/English search, category filter, per-item source IDs, safety discussion. No Iranian label verification, dose table or calculator; no personalized prescribing.
- Growth: three synthetic sample series, two metrics, accessible table and graph. No WHO numerical reference data, percentile or Z-score. The chart is NOT a growth standard and no clinical classification is emitted. No real data input. Before building actual monitoring: validate authoritative WHO LMS files, age in days, sex, method, corrected age where appropriate, reference choice, precision, boundary cases and independent comparator tests. Add privacy/consent/storage review before collecting real data.
- Skin: five topics, descriptions and escalation, links to actual NHS reference image galleries. The icon is symbolic, not a lesion or diagnostic image. No clinical image licensing claim, photo upload, image matching or product ranking.
- Visual: consistent brand, original local mascot assets, compact responsive layout. No new custom clinical images.
- SEO: three unique titles/descriptions, internal links, noindex and no sitemap, no fabricated credentials.
- Code/UX: local search, synthetic chart, accessible details, tests across five widths and axe automated checks on default states. Manual assistive technology and real-device family testing remain required.

## Source register
- APAP: AAP HealthyChildren — https://www.healthychildren.org/English/safety-prevention/at-home/medication-safety/Pages/Acetaminophen-for-Fever-and-Pain.aspx — Age and measuring instructions
- IBU: AAP HealthyChildren — https://www.healthychildren.org/English/safety-prevention/at-home/medication-safety/Pages/Ibuprofen-for-Fever-and-Pain.aspx — Age and measurement cautions
- OTC: AAP HealthyChildren — https://www.healthychildren.org/English/safety-prevention/at-home/medication-safety/Pages/Choosing-Over-the-Counter-Medicines-for-Your-Child.aspx — Combination products and age cautions
- AMOX: MedlinePlus — https://medlineplus.gov/druginfo/meds/a685001.html — Precautions and adverse effects
- ABX: CDC — https://www.cdc.gov/antibiotic-use/about/ — Use as prescribed; viral illness and side effects
- CET: MedlinePlus — https://www.medlineplus.gov/druginfo/meds/a623043.html — Precautions; anaphylaxis; drowsiness
- ORS: AAP HealthyChildren — https://www.healthychildren.org/english/health-issues/conditions/abdominal/pages/treating-dehydration-with-electrolyte-solution.aspx — Using electrolyte replacement solution at home
- COLD: CDC — https://www.cdc.gov/common-cold/treatment/ — OTC cold medicine limitations
- WHO_GROWTH: WHO — https://www.who.int/tools/child-growth-standards — Standards from birth to five years
- CDC_GROWTH: CDC — https://www.cdc.gov/growth-chart-training/hcp/using-growth-charts/who-using.html — Assessment; serial measurements; US transition
- ECZEMA: NHS — https://www.nhs.uk/conditions/atopic-eczema/ — Urgent advice; emollients; treatment
- HIVES: NHS — https://www.nhs.uk/conditions/hives/ — Symptoms and emergency warning signs
- IMPETIGO: NHS — https://www.nhs.uk/conditions/impetigo/ — Symptoms; prevention of spread
- RASHES: NHS — https://www.nhs.uk/symptoms/rashes-babies-and-children/ — Images in different skin tones, urgent signs
- NAPPY: NHS — https://www.nhs.uk/baby/caring-for-a-newborn/nappy-rash/ — Symptoms and care

Clinical content is draft. International sources do not establish product availability, local dosing authorization or Iranian clinical pathways. Validate on the exact local label and with licensed clinical reviewers. No deployment, main merge or live patient data collection.

## Second-pass safeguards
- FDA direct overdose/liver risk source: https://www.fda.gov/consumers/consumer-updates/dont-overuse-acetaminophen (search excerpt); NHS pediatric ibuprofen cautions: https://www.nhs.uk/medicines/ibuprofen-for-children/ (search excerpt). Both are tagged as requiring human clinical verification.
- Source labels are human-readable; field and label are grouped together for mobile/desktop.
- CSP blocks module form submission and network connect APIs; no external clinical images are requested. Native details and static educational table remain readable without JavaScript. No claim of complete hosted privacy or accessibility audit.
