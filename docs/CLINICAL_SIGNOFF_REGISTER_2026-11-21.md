# MANO — versioned clinical approval register (template)

The project pharmacist has stated willingness to serve as clinical approver. **This does not constitute an item-level signed review of every existing monograph, symptom flow or generated AI claim.** Until concrete evidence is recorded below, items remain `NEEDS_CLINICAL_REVIEW`.

Record for each item:
| Item ID / type | Git commit and content hash | Versioned primary clinical references (edition/date/page) | Age/dose/regulatory check | Off-label designation | Reviewer sign-off & date | Next review due | Status |
|---|---|---|---|---|---|---|---|
| Example: `fever` / symptom | TODO | TODO | TODO | N/A | TODO | TODO | NEEDS_CLINICAL_REVIEW |
| Example: `acetaminophen` / dose monograph | TODO | TODO | TODO | TODO | TODO | TODO | NEEDS_CLINICAL_REVIEW |
| Example: WHO growth LMS / chart | TODO | WHO 2006 standards, verify source tables and method | TODO | N/A | TODO | TODO | NEEDS_CLINICAL_REVIEW |
| Example: complementary feeding / age thresholds | TODO | WHO infant and young-child feeding, current edition | TODO | N/A | TODO | TODO | NEEDS_CLINICAL_REVIEW |

## Approval protocol
1. Freeze the exact content version and source pages; check actual local Iranian formulation labels where relevant.
2. Pharmacist checks medication dose, minimum age, weight, maximum daily mg, concentration, interactions, contraindications, renal/hepatic cautions, pregnancy/lactation when applicable, triage exclusions and patient counseling.
3. Pharmacist explicitly marks `APPROVED` or `REJECTED` for **that item** with date and reason. Do not infer item approval from general permission in chat.
4. Require at least one second reviewer for life-critical dose rules and high-severity red flags before activation; document reviewer role and exceptions.
5. Automatic source change or material clinical content change invalidates the sign-off; set status back to `NEEDS_CLINICAL_REVIEW`.
6. Generative AI output cannot inherit approval solely because its prompt references reviewed material.

Use redacted references where personal reviewer registration details are private. Do not publish a government license number or identification record without explicit approval.
