# MANO Kid — private review release gates

This is a synthetic-only review, not production. No public ingress, clinical use, Firebase deletion, or main merge.

## Evidence
- Baseline 7d5787ca: Kotlin HTTP care loop and verified PostgreSQL TLS passed (46157188814); private React browser run failed with a generic error. Web and Kotlin validation passed (46157188799, 46157188805).
- 2b80ea3d: explicit production React environment in the isolated bundle, project-root resolution, and bounded diagnostic phase names. This revision requires a fresh browser run. Do not waive the UI gate.

## Private test sequence
1. Verify all CI jobs for the exact selected commit. Verify PostgreSQL care-card schema, care events, RLS, cross-pharmacy denial, consent revocation, Kotlin, and actual React buttons. Do not substitute an older green commit for current evidence.
2. On a dedicated synthetic execution environment only, use the existing liara-integration-check.mjs read-only check with the matching commit files and runtime role. Default verify-full. Explicit private-test is permitted only for synthetic data on the isolated private network. Never use real patient data in this mode. The check starts temporary loopback listeners and closes them. No deployment or database migration.
3. Before applying care-loop.sql, back up the dedicated synthetic database and verify restore, check that the schema owner (not mano_stage_app) runs the one-time migration, and review that the new tables/functions match the selected commit. Keep MANO_STAGE_CARE_ENABLED=false until verified. Do not apply schema.sql again to an existing database.
4. Only after provider confirmation of a TLS-capable endpoint, verified hostname and trusted CA, test actual Liara TLS with verify-full; do not expose PostgreSQL to the Internet as a workaround. Rotate any previously exposed admin credentials. Keep credentials out of the repository and client bundles.
5. Real account onboarding requires approved SMS provider credentials, registered sender/template, explicit identity proofing for pharmacists, abuse limits and reviewed account recovery. Existing demo OTP is not real registration. No SMS network calls in CI.
6. The React workspace is a private review component and Kotlin is a JVM-tested adapter. Real app screen wiring and full APK/device validation remain open. Do not claim that a synthetic browser run proves the live app works.
7. Rollback: leave the live application unchanged. Disable the care feature, stop the private process, revoke synthetic sessions, and preserve the synthetic database for investigation. Do not delete or migrate real records.

## Provider support question (Persian)
سلام. برای PostgreSQL مدیریت‌شده mano-db روی شبکه خصوصی، آیا TLS با اعتبارسنجی کامل گواهی و نام میزبان (verify-full) پشتیبانی می‌شود؟ در آزمون SSLRequest قبلی پاسخ عدم پشتیبانی دریافت شد. لطفاً روش رسمی فعال‌سازی TLS، نام میزبان معتبر، CA مورد اعتماد، نیاز به ری‌استارت یا تغییر پلن و هزینه احتمالی را اعلام کنید. بدون تأیید قبلی هیچ تغییر یا هزینه‌ای اعمال نشود. دیتابیس نباید عمومی شود.
