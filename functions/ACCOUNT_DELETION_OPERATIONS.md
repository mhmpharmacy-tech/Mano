# Account deletion implementation and operations

This is source for an independently gated deletion workflow. No account, Storage object, Firestore document, Firebase rule or production setting was changed during its implementation or tests. Clinical service release remains a separate decision.

## Policy gate

Deletion requests and the scheduled worker are disabled unless all required policy and inventory settings are valid. A deployment flag is an operator attestation; it is not evidence that a legal/privacy reviewer approved the policy.

| Server setting | Meaning |
|---|---|
| `MANO_ACCOUNT_DELETION_ENABLED=true` | Enables this workflow after review. The default is disabled. |
| `MANO_ACCOUNT_DELETION_POLICY_APPROVED=true` | Records the deployment decision; retain the actual reviewed policy separately. |
| `MANO_ACCOUNT_DELETION_POLICY_VERSION` | Required version, 1–64 letters, digits, dot, underscore or hyphen. |
| `MANO_ACCOUNT_DELETION_CLINICAL_RETENTION_DAYS` | Required finite period, 1–36500 days. The code does not choose the legally appropriate value. |
| `MANO_ACCOUNT_DELETION_BACKUP_RETENTION_DAYS` | Required finite declared backup period, 0–36500 days. This job cannot verify or erase provider-managed backups. |
| `MANO_ACCOUNT_DELETION_STORAGE_MODE` | `none` only when the reviewed inventory has no managed user Storage objects; otherwise `users-prefix-v1`. Unreviewed inventories must remain disabled. |
| `MANO_ACCOUNT_DELETION_STORAGE_BUCKET` | Required exact reviewed bucket for `users-prefix-v1`; must be unset for `none`. Only `users/{uid}/` is implemented. |
| `MANO_ACCOUNT_DELETION_STORAGE_WRITERS_STOPPED=true` | Additionally required for `users-prefix-v1`: every other upload/write route, signed URL, resumable upload and administrative writer has been stopped or verified to honor deletion. |

Do not enable this adapter if the inventory includes other buckets, other user-object prefixes, external processors, or unreviewed write paths. Extend and test the inventory first. A named Firestore deletion lock does **not** protect Cloud Storage: no cross-database Storage rule enforcement is claimed by this source.

The REST traversal reads the initialized Admin SDK project identity and the configured named database, rejects mismatches, validates every returned descendant path, uses `showMissing=true` to find orphan descendants and deletes at most 50 leaves per page. If a future SDK cannot expose its initialized identity, the workflow fails closed for operational review.

## Request and receipt

1. The authenticated caregiver fetches `getAccountDeletionPolicy({})` and reviews the current version, declared retention windows and limitations. Staff accounts are excluded from self-service deletion.
2. The client generates 32 random bytes encoded as a canonical 43-character base64url token. Before submitting, it saves the private receipt containing that token and `jobId = 'del_' + SHA256(UTF8(token)).hex.slice(0,40)`.
3. `requestAccountDeletion({confirmation:'DELETE_MY_ACCOUNT',policyVersion,receiptToken})` requires Auth reauthentication within five minutes, App Check, exact fields and the current approved policy. A body-supplied UID is rejected. The authenticated UID is the only owner used.
4. The server stores a hash of the receipt token. A transaction creates the private job and deletion lock, or returns the existing job for the same receipt. Another receipt cannot silently replace an active job. Directory membership is checked again in that transaction to prevent a staff-onboarding race.
5. `getAccountDeletionStatus({jobId,receiptToken})` works after Auth deletion using the saved private token; App Check remains required. A current authenticated owner can also check its receipt. Receipts expire after 30 days. Wrong owner/token, expired or missing jobs return the same denied result.

The receipt has no child details, UID, credentials, provider errors or receipt hash. It is still a bearer capability: keep the downloaded file private and never put its token in a URL, public issue, screenshot, analytics event or log.

## Processing and completion semantics

The scheduled worker handles bounded steps with a 90-second lease and persisted progress. It disables Auth and revokes refresh tokens first, erases the owned Firestore subtree and operational state in pages, handles the reviewed Storage prefix, marks authoritative clinical cases for their finite retention period, and deletes Auth last. Interrupted pages retry idempotently; transient failures back off and never become success. More than 500 records do not require one oversized Firestore batch.

For `users-prefix-v1`, the worker persists the successful Auth-disable time and enters `WAIT_STORAGE` until at least **65 minutes** have elapsed. It schedules the next attempt instead of sleeping. This covers the ordinary cached Firebase ID-token window; it does not invalidate already issued signed URLs or resumable/admin upload capabilities. The separate reviewed-writers-stopped gate is therefore mandatory. After the first Storage sweep and clinical inventory work, a final bounded Storage sweep must finish before Auth deletion and completion; files recreated after an earlier empty sweep cannot be omitted.

| Status | Meaning |
|---|---|
| `PENDING` | Request and owner lock recorded; processing has not completed. |
| `IN_PROGRESS` | A bounded step or persisted expiry wait is active. A wait is not a failure or completion guarantee. |
| `PARTIAL_FAILURE` | The last step did not complete. `ERASURE_RETRY_REQUIRED` or `INVENTORY_REVIEW_REQUIRED` is safe for the receipt; detailed provider errors are not copied into it. |
| `COMPLETED_WITH_RETENTION` | Current online processing and Auth deletion finished. Authoritative clinical/audit records, the declared provider backup window, private receipt/security metadata and user-controlled local data/exports have separately disclosed retention. This never means “all copies everywhere erased.” |

At the finite clinical deadline, another bounded pass erases retained authoritative cases and their nested audit records. Once clinical purge, declared backup and receipt windows have all elapsed, private job metadata and the UID lock are removed. The backup deadline is a declared policy deadline, **not** evidence that provider backups were physically deleted. Device-local browser data and previously exported files remain outside this server job.

## Before activation

- Review the actual deployed inventory, database/project identity, clinical/legal retention policy, backup controls, storage writer quiescence, incident handling and private receipt support.
- Apply and verify the owner deletion-lock rules and required collection-group indexes for `cases.parentUid` and `staff.uid` in staging. Repository files do not establish deployed enforcement.
- Use synthetic accounts to verify App Check rejection, recent reauthentication, parent A/B isolation, staff rejection/onboarding races, nested missing-parent documents, partial provider failure/retry, token-expiry waiting and the final Storage sweep.
- Verify the exact built commit and scheduled job behavior. The local fake-provider suite does not establish production SDK permissions, storage rule behavior or real-device correctness.
- Keep clinical AI/services disabled until their independent release requirements are met. Deletion rights and private receipt checks are intentionally not tied to clinical enablement or email verification.

Run `npm ci`, `npm run lint` and `npm test` from `functions/` to repeat the local synthetic checks. Do not run any production erasure test against real caregiver or child data.
