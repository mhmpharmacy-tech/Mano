# Internal clinical review (2026-11-17)

The owner volunteered to provide medical approval. No granular approval decision has been recorded on behalf of the owner. This is a reviewer task queue, not a production authorization system.

47 tasks: whole homepage plus five sections; eight articles; six whole modules and 26 individual entries; one synthetic pharmacist workflow. The full pages and actual behavior must be checked, including claims missing from the source registry. Source links/locators do not prove that evidence is sufficient. Items can receive a self-declared content approval only after five checks; requests for changes/hold require a reason. No clinical sign-off has been simulated.

The generated snapshot identifies bytes of the included clinical pages, logic and shared assets at build time. A verifier recomputes that snapshot on the repository and validates packet format: node web/tools/verify-review.cjs review.json. Review of a changed version must be repeated. The static reviewer UI does not recompute hashes of local alterations. JSON is unsigned and cannot prove identity, authorship, competence or clinical validity. The reviewer ID is self-declared, no credential verification has occurred.

All work is local, with explicit import/export, no server submission. Do not enter patient data. Open clinical-review.html after extracting the private preview ZIP. The preview remains noindex and unapproved. Clinical authorization, validated local sources, appropriate specialist review, privacy and security, operations, live service and release decisions are separate requirements.
