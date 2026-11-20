import assert from "node:assert/strict";
import test from "node:test";
import { PharmacyAdminService } from "../src/admin";
import { ConsultationCaseService } from "../src/cases";
import { OperationalCleanupService } from "../src/cleanup";
import { AppError, TRIAGE_PASS_TTL_MS } from "../src/config";
import { runOutOfBandBootstrap } from "../src/scripts/bootstrap";
import {
  AuditEventDoc,
  CallerContext,
  ConsultationCaseDoc,
  StaffDirectoryDoc,
} from "../src/types";
import { InMemoryAuthService, InMemoryBackendStore } from "./inMemoryStore";

function makeSystemAdmin(uid = "admin_root"): CallerContext {
  return {
    uid,
    token: {
      manoSystemAdmin: true,
      manoStaffRole: "admin",
      manoStaffVerified: true,
      manoPharmacyId: "pharm_a",
    },
  };
}

function makeParent(uid = "parent_alice"): CallerContext {
  return { uid, token: {} };
}

function makePharmacistCaller(
  uid: string,
  pharmacyId: string,
): CallerContext {
  return {
    uid,
    token: {
      manoStaffVerified: true,
      manoStaffRole: "pharmacist",
      manoPharmacyId: pharmacyId,
    },
  };
}

async function setupEnv() {
  const store = new InMemoryBackendStore();
  const authService = new InMemoryAuthService();
  const adminService = new PharmacyAdminService(store, authService);
  const caseService = new ConsultationCaseService(store);
  const sysAdmin = makeSystemAdmin();

  await adminService.upsertPharmacyRegistry(sysAdmin, {
    pharmacyId: "pharm_a",
    displayNameFa: "داروخانه شبانه‌روزی الف",
    active: true,
    acceptingCases: true,
  });
  await adminService.upsertPharmacyRegistry(sysAdmin, {
    pharmacyId: "pharm_b",
    displayNameFa: "داروخانه کودکان ب",
    active: true,
    acceptingCases: true,
  });

  await adminService.onboardPharmacist(sysAdmin, {
    pharmacyId: "pharm_a",
    targetUid: "pharm_uid_1",
    credentialReference: "IR-PHARM-10492",
    credentialVerified: true,
  });

  return { store, authService, adminService, caseService, sysAdmin };
}

test("Gate 1: Revoked staff directory denies BOTH inbox and review even with cached valid custom-claim token", async () => {
  const { store, authService, adminService, caseService, sysAdmin } =
    await setupEnv();
  const parent = makeParent();
  const cachedTokenCaller = makePharmacistCaller("pharm_uid_1", "pharm_a");

  // Issue triage pass and submit consultation case
  const pass = await caseService.issueConsultationTriagePass(parent, {
    ageBandId: "toddler_1_3y",
    symptomFlowId: "constipation",
    temperatureCelsius: 36.7,
    questionSummaryFa: "پرسش غیرفوری درباره یبوست خفیف کودک سه ساله",
  });
  assert.equal(pass.eligibleForNonUrgentConsultation, true);
  assert.ok(pass.triagePassId);

  const createdCase = await caseService.submitConsultationCase(parent, {
    pharmacyId: "pharm_a",
    triagePassId: pass.triagePassId,
    categoryName: "OTC",
    ageBandId: "toddler_1_3y",
    symptomFlowId: "constipation",
    questionSummaryFa: "آیا استفاده از پودر پیدرولاکس در کودک سه ساله مجاز است؟",
    hasUrgentFlag: false,
    consentAccepted: true,
    consentVersion: "v1",
  });
  assert.equal(createdCase.status, "PENDING");

  // Active pharmacist can read inbox and move PENDING -> RECEIVED
  const inboxBeforeRevoke = await caseService.getPharmacyInbox(
    cachedTokenCaller,
    { pharmacyId: "pharm_a" },
  );
  assert.equal(inboxBeforeRevoke.length, 1);

  const received = await caseService.reviewConsultationCase(cachedTokenCaller, {
    pharmacyId: "pharm_a",
    caseId: createdCase.caseId,
    nextStatus: "RECEIVED",
    internalNote: "بررسی اولیه توسط داروساز شیفت آغاز شد.",
  });
  assert.equal(received.status, "RECEIVED");

  // Admin immediately revokes pharmacist
  await adminService.revokePharmacist(sysAdmin, {
    pharmacyId: "pharm_a",
    targetUid: "pharm_uid_1",
    status: "SUSPENDED",
    reason: "تعلیق فوری اعتبار نظام‌پزشکی",
  });
  assert.ok(authService.revokedTokensForUids.includes("pharm_uid_1"));

  // Cached ID token still retains manoStaffVerified=true, manoStaffRole="pharmacist", manoPharmacyId="pharm_a",
  // YET live directory check MUST deny BOTH inbox and review transaction!
  await assert.rejects(
    () =>
      caseService.getPharmacyInbox(cachedTokenCaller, {
        pharmacyId: "pharm_a",
      }),
    (err: unknown) =>
      err instanceof AppError && err.code === "permission-denied",
  );

  await assert.rejects(
    () =>
      caseService.reviewConsultationCase(cachedTokenCaller, {
        pharmacyId: "pharm_a",
        caseId: createdCase.caseId,
        nextStatus: "REVIEWED",
        parentReply: "پاسخ داروساز: توصیه به مصرف مایعات و پیگیری پزشک در صورت تداوم.",
        pharmacistAttestation: true,
      }),
    (err: unknown) =>
      err instanceof AppError && err.code === "permission-denied",
  );

  // Confirm review transaction did NOT advance state after revocation
  const storedCase = store.peekDoc<ConsultationCaseDoc>(
    `pharmacies/pharm_a/cases/${createdCase.caseId}`,
  );
  assert.equal(storedCase?.status, "RECEIVED");
});

test("Gate 2 & 3: Admin accounts cannot approve clinical cases; cross-tenant pharmacists are blocked", async () => {
  const { adminService, caseService, sysAdmin } = await setupEnv();
  const parent = makeParent();

  await adminService.onboardPharmacist(sysAdmin, {
    pharmacyId: "pharm_b",
    targetUid: "pharm_b_uid",
    credentialReference: "IR-PHARM-88211",
    credentialVerified: true,
  });

  const pass = await caseService.issueConsultationTriagePass(parent, {
    ageBandId: "school_6_plus",
    symptomFlowId: "supplement_question",
    questionSummaryFa: "سوال درباره تداخل شربت زینک با مولتی‌ویتامین کودک هفت ساله",
  });

  const c = await caseService.submitConsultationCase(parent, {
    pharmacyId: "pharm_a",
    triagePassId: pass.triagePassId!,
    categoryName: "SUPPLEMENT",
    ageBandId: "school_6_plus",
    symptomFlowId: "supplement_question",
    questionSummaryFa: "آیا شربت زینک با مولتی ویتامین کودک هفت ساله تداخل دارد؟",
    hasUrgentFlag: false,
    consentAccepted: true,
    consentVersion: "v1",
  });

  // Admin account rejected from clinical inbox & review even if manoPharmacyId matches
  await assert.rejects(
    () => caseService.getPharmacyInbox(sysAdmin, { pharmacyId: "pharm_a" }),
    (err: unknown) =>
      err instanceof AppError && err.code === "permission-denied",
  );
  await assert.rejects(
    () =>
      caseService.reviewConsultationCase(sysAdmin, {
        pharmacyId: "pharm_a",
        caseId: c.caseId,
        nextStatus: "RECEIVED",
      }),
    (err: unknown) =>
      err instanceof AppError && err.code === "permission-denied",
  );

  // Cross-tenant pharmacist from pharm_b cannot touch pharm_a
  const pharmBStaff = makePharmacistCaller("pharm_b_uid", "pharm_b");
  await assert.rejects(
    () => caseService.getPharmacyInbox(pharmBStaff, { pharmacyId: "pharm_a" }),
    (err: unknown) =>
      err instanceof AppError && err.code === "permission-denied",
  );
  await assert.rejects(
    () =>
      caseService.reviewConsultationCase(pharmBStaff, {
        pharmacyId: "pharm_a",
        caseId: c.caseId,
        nextStatus: "RECEIVED",
      }),
    (err: unknown) =>
      err instanceof AppError && err.code === "permission-denied",
  );
});

test("Gate 4: Intake requires valid unused non-expired NONURGENT triage pass; urgent newborn fever gets no pass", async () => {
  const { store, caseService } = await setupEnv();
  const alice = makeParent("parent_alice");
  const bob = makeParent("parent_bob");

  // Newborn fever >= 38C does NOT receive a triage pass
  const urgentDecision = await caseService.issueConsultationTriagePass(alice, {
    ageBandId: "newborn",
    symptomFlowId: "fever",
    temperatureCelsius: 38.2,
    questionSummaryFa: "نوزاد دو ماهه تب ۳۸.۲ درجه دارد",
  });
  assert.equal(urgentDecision.outcome, "URGENT_EMERGENCY");
  assert.equal(urgentDecision.eligibleForNonUrgentConsultation, false);
  assert.equal(urgentDecision.triagePassId, null);

  // Client fabricated pass ID is denied
  await assert.rejects(
    () =>
      caseService.submitConsultationCase(alice, {
        pharmacyId: "pharm_a",
        triagePassId: "fake_pass_123",
        categoryName: "OTC",
        ageBandId: "toddler_1_3y",
        symptomFlowId: "constipation",
        questionSummaryFa: "تلاش برای ارسال بدون گذرنامه واقعی سرور",
        hasUrgentFlag: false,
        consentAccepted: true,
        consentVersion: "v1",
      }),
    (err: unknown) =>
      err instanceof AppError && err.code === "permission-denied",
  );

  // Valid pass issued to Alice cannot be stolen by Bob
  const alicePass = await caseService.issueConsultationTriagePass(alice, {
    ageBandId: "preschool_3_6y",
    symptomFlowId: "rash",
    questionSummaryFa: "قرمزی خفیف پوست بدون تب در کودک پنج ساله",
  });
  assert.ok(alicePass.triagePassId);

  await assert.rejects(
    () =>
      caseService.submitConsultationCase(bob, {
        pharmacyId: "pharm_a",
        triagePassId: alicePass.triagePassId!,
        categoryName: "SKIN",
        ageBandId: "preschool_3_6y",
        symptomFlowId: "rash",
        questionSummaryFa: "استفاده غیرمجاز از گذرنامه کاربر دیگر",
        hasUrgentFlag: false,
        consentAccepted: true,
        consentVersion: "v1",
      }),
    (err: unknown) =>
      err instanceof AppError && err.code === "permission-denied",
  );

  // Single-use pass is tied to the evaluated symptom, not just the child's age.
  await assert.rejects(
    () => caseService.submitConsultationCase(alice, {
      pharmacyId: "pharm_a",
      triagePassId: alicePass.triagePassId!,
      categoryName: "SKIN",
      ageBandId: "preschool_3_6y",
      symptomFlowId: "constipation",
      questionSummaryFa: "پرسش درباره مشکل شکمی جدید و متفاوت از تریاژ پوستی",
      hasUrgentFlag: false,
      consentAccepted: true,
      consentVersion: "v1",
    }),
    (err: unknown) => err instanceof AppError && err.code === "invalid-argument",
  );

  // First legitimate submission succeeds and consumes the pass
  await caseService.submitConsultationCase(alice, {
    pharmacyId: "pharm_a",
    triagePassId: alicePass.triagePassId!,
    categoryName: "SKIN",
    ageBandId: "preschool_3_6y",
        symptomFlowId: "rash",
    questionSummaryFa: "قرمزی خفیف پوست پس از تعویض پودر لباسشویی کودک پنج ساله",
    hasUrgentFlag: false,
    consentAccepted: true,
    consentVersion: "v1",
  });

  // Replay of same single-use pass fails
  await assert.rejects(
    () =>
      caseService.submitConsultationCase(alice, {
        pharmacyId: "pharm_a",
        triagePassId: alicePass.triagePassId!,
        categoryName: "SKIN",
        ageBandId: "preschool_3_6y",
        symptomFlowId: "rash",
        questionSummaryFa: "تلاش مجدد با گذرنامه مصرف‌شده برای ارسال تکراری",
        hasUrgentFlag: false,
        consentAccepted: true,
        consentVersion: "v1",
      }),
    (err: unknown) =>
      err instanceof AppError && err.code === "failed-precondition",
  );

  // Expired pass fails
  const expiringPass = await caseService.issueConsultationTriagePass(bob, {
    ageBandId: "toddler_1_3y",
    symptomFlowId: "otc_question",
    questionSummaryFa: "پرسش درباره قطره ویتامین آد کودک دو ساله",
  });
  store.advanceMs(TRIAGE_PASS_TTL_MS + 1000);
  await assert.rejects(
    () =>
      caseService.submitConsultationCase(bob, {
        pharmacyId: "pharm_a",
        triagePassId: expiringPass.triagePassId!,
        categoryName: "SUPPLEMENT",
        ageBandId: "toddler_1_3y",
        symptomFlowId: "otc_question",
        questionSummaryFa: "پرسش درباره قطره ویتامین آد پس از انقضای گذرنامه",
        hasUrgentFlag: false,
        consentAccepted: true,
        consentVersion: "v1",
      }),
    (err: unknown) =>
      err instanceof AppError && err.code === "failed-precondition",
  );
});

test("Gate 5: Sequential workflow, attestation requirement, separate notes, audit events, and parent ownership", async () => {
  const { store, caseService } = await setupEnv();
  const alice = makeParent("parent_alice");
  const bob = makeParent("parent_bob");
  const pharmacist = makePharmacistCaller("pharm_uid_1", "pharm_a");

  const pass = await caseService.issueConsultationTriagePass(alice, {
    ageBandId: "infant_3_12m",
    symptomFlowId: "otc_question",
    questionSummaryFa: "پرسش درباره روش شستشوی بینی با سرم در شیرخوار شش ماهه",
  });

  const c = await caseService.submitConsultationCase(alice, {
    pharmacyId: "pharm_a",
    triagePassId: pass.triagePassId!,
    categoryName: "OTC",
    ageBandId: "infant_3_12m",
    symptomFlowId: "otc_question",
    questionSummaryFa: "آیا سرم شستشو برای گرفتگی بینی شیرخوار شش ماهه مناسب است؟",
    hasUrgentFlag: false,
    consentAccepted: true,
    consentVersion: "v1",
  });

  // Cannot skip PENDING -> REVIEWED
  await assert.rejects(
    () =>
      caseService.reviewConsultationCase(pharmacist, {
        pharmacyId: "pharm_a",
        caseId: c.caseId,
        nextStatus: "REVIEWED",
        parentReply: "شستشو با سرم فیزیولوژی مجاز است.",
        pharmacistAttestation: true,
      }),
    (err: unknown) =>
      err instanceof AppError && err.code === "failed-precondition",
  );

  // PENDING -> RECEIVED cannot publish parent reply yet
  await assert.rejects(
    () =>
      caseService.reviewConsultationCase(pharmacist, {
        pharmacyId: "pharm_a",
        caseId: c.caseId,
        nextStatus: "RECEIVED",
        parentReply: "نباید در مرحله دریافت پاسخ منتشر شود.",
      }),
    (err: unknown) =>
      err instanceof AppError && err.code === "invalid-argument",
  );

  // Valid PENDING -> RECEIVED with private internal note
  await caseService.reviewConsultationCase(pharmacist, {
    pharmacyId: "pharm_a",
    caseId: c.caseId,
    nextStatus: "RECEIVED",
    internalNote: "یادداشت داخلی داروساز: عدم وجود تنگی نفس بررسی شود.",
  });

  // RECEIVED -> REVIEWED requires explicit pharmacistAttestation=true
  await assert.rejects(
    () =>
      caseService.reviewConsultationCase(pharmacist, {
        pharmacyId: "pharm_a",
        caseId: c.caseId,
        nextStatus: "REVIEWED",
        parentReply: "استفاده از قطره یا اسپری سرم شستشوی بینی پیش از شیر خوردن کمک‌کننده است.",
        pharmacistAttestation: false,
      }),
    (err: unknown) =>
      err instanceof AppError && err.code === "failed-precondition",
  );

  // Valid RECEIVED -> REVIEWED
  const reviewed = await caseService.reviewConsultationCase(pharmacist, {
    pharmacyId: "pharm_a",
    caseId: c.caseId,
    nextStatus: "REVIEWED",
    internalNote: "یادداشت داخلی محرمانه: پاسخ نهایی تایید و امضا شد.",
    parentReply:
      "قطره سدیم کلراید ۰.۹٪ برای تمیز کردن ملایم بینی پیش از تغذیه مناسب است؛ در صورت خس‌خس یا تنگی نفس به پزشک مراجعه کنید.",
    pharmacistAttestation: true,
  });
  assert.equal(reviewed.status, "REVIEWED");
  assert.equal(reviewed.internalNotes.length, 2);

  // Terminal REVIEWED lock
  await assert.rejects(
    () =>
      caseService.reviewConsultationCase(pharmacist, {
        pharmacyId: "pharm_a",
        caseId: c.caseId,
        nextStatus: "RECEIVED",
      }),
    (err: unknown) =>
      err instanceof AppError && err.code === "failed-precondition",
  );

  // Parent read strips internalNotes and staff UIDs!
  const parentView = await caseService.getParentConsultation(alice, {
    pharmacyId: "pharm_a",
    caseId: c.caseId,
  });
  assert.equal(parentView.status, "REVIEWED");
  assert.ok(parentView.parentResponse?.replyTextFa.includes("سدیم کلراید"));
  assert.equal("internalNotes" in parentView, false);
  assert.equal("reviewedByUid" in parentView, false);

  const listAlice = await caseService.listParentConsultations(alice, {});
  assert.equal(listAlice.length, 1);
  assert.equal("internalNotes" in listAlice[0], false);

  // Cross-parent read denied
  await assert.rejects(
    () =>
      caseService.getParentConsultation(bob, {
        pharmacyId: "pharm_a",
        caseId: c.caseId,
      }),
    (err: unknown) =>
      err instanceof AppError && err.code === "permission-denied",
  );

  // Audit events written atomically on every transition
  const auditEvents = store.listDocsAtPrefix<AuditEventDoc>(
    `pharmacies/pharm_a/cases/${c.caseId}/auditEvents/`,
  );
  auditEvents.sort((a, b) => a.createdAt - b.createdAt);
  assert.deepEqual(
    auditEvents.map((e) => e.action),
    [
      "CASE_CREATED",
      "STATUS_PENDING_TO_RECEIVED",
      "STATUS_RECEIVED_TO_REVIEWED",
    ],
  );
  assert.equal(auditEvents[2].attestationConfirmed, true);
  assert.equal(auditEvents[2].internalNoteRecorded, true);
  assert.equal(auditEvents[2].parentReplyPublished, true);

  // Verify live directory schema fields match spec
  const staffDoc = store.peekDoc<StaffDirectoryDoc>(
    "pharmacies/pharm_a/staff/pharm_uid_1",
  );
  assert.equal(staffDoc?.status, "ACTIVE");
  assert.equal(staffDoc?.credentialVerified, true);
  assert.equal(staffDoc?.revokedAt, null);
});

test("Gate 6: Rate limiting blocks rapid case intake spam and recovers after window", async () => {
  const { store, caseService } = await setupEnv();
  const parent = makeParent("spam_parent");

  for (let i = 0; i < 5; i++) {
    const pass = await caseService.issueConsultationTriagePass(parent, {
      ageBandId: "toddler_1_3y",
      symptomFlowId: "constipation",
      questionSummaryFa: `سوال عادی شماره ${i + 1} درباره تغذیه کودک`,
    });
    await caseService.submitConsultationCase(parent, {
      pharmacyId: "pharm_a",
      triagePassId: pass.triagePassId!,
      categoryName: "OTC",
      ageBandId: "toddler_1_3y",
        symptomFlowId: "constipation",
      questionSummaryFa: `سوال عادی شماره ${i + 1} درباره تغذیه و مصرف آب کودک`,
      hasUrgentFlag: false,
      consentAccepted: true,
      consentVersion: "v1",
    });
  }

  const extraPass = await caseService.issueConsultationTriagePass(parent, {
    ageBandId: "toddler_1_3y",
    symptomFlowId: "constipation",
    questionSummaryFa: "سوال ششم بیش از حد مجاز",
  });

  await assert.rejects(
    () =>
      caseService.submitConsultationCase(parent, {
        pharmacyId: "pharm_a",
        triagePassId: extraPass.triagePassId!,
        categoryName: "OTC",
        ageBandId: "toddler_1_3y",
        symptomFlowId: "constipation",
        questionSummaryFa: "سوال ششم که باید توسط محدودیت نرخ مسدود شود",
        hasUrgentFlag: false,
        consentAccepted: true,
        consentVersion: "v1",
      }),
    (err: unknown) =>
      err instanceof AppError && err.code === "resource-exhausted",
  );

  // Advance clock beyond 15m rate-limit window; fresh non-expired pass works
  store.advanceMs(16 * 60 * 1000);
  const recoveredPass = await caseService.issueConsultationTriagePass(parent, {
    ageBandId: "toddler_1_3y",
    symptomFlowId: "constipation",
    questionSummaryFa: "سوال جدید پس از باز شدن پنجره زمانی",
  });
  const recovered = await caseService.submitConsultationCase(parent, {
    pharmacyId: "pharm_a",
    triagePassId: recoveredPass.triagePassId!,
    categoryName: "OTC",
    ageBandId: "toddler_1_3y",
        symptomFlowId: "constipation",
    questionSummaryFa: "سوال جدید پس از سپری شدن زمان محدودیت نرخ ارسال",
    hasUrgentFlag: false,
    consentAccepted: true,
    consentVersion: "v1",
  });
  assert.equal(recovered.status, "PENDING");
});

test("Gate 7: Public pharmacy discovery, out-of-band bootstrap, and expired doc cleanup", async () => {
  const { store, authService, caseService } = await setupEnv();
  const parent = makeParent("parent_lookup");

  const pharmacies = await caseService.listAcceptingPharmacies(parent);
  assert.equal(pharmacies.length, 2);
  assert.equal(pharmacies[0].acceptingCases, true);

  // Out-of-band bootstrap creates active pharmacy & grants manoSystemAdmin claim
  const bootstrapped = await runOutOfBandBootstrap(store, authService, {
    pharmacyId: "pharm_bootstrapped",
    pharmacyNameFa: "داروخانه مرکزی تاییدشده",
    adminUid: "operator_uid_99",
  });
  assert.equal(bootstrapped.pharmacy.active, true);
  assert.equal(bootstrapped.adminGrantedUid, "operator_uid_99");
  assert.equal((await authService.getCustomClaims("operator_uid_99")).manoSystemAdmin, true);

  // Issue a triage pass & advance beyond 24h so cleanup purges both expired pass & rate limit bucket
  await caseService.issueConsultationTriagePass(parent, {
    ageBandId: "toddler_1_3y",
    symptomFlowId: "constipation",
    questionSummaryFa: "سوال تستی برای پاکسازی سند منقضی",
  });
  store.advanceMs(25 * 60 * 60 * 1000);

  const cleanup = new OperationalCleanupService(store);
  const summary = await cleanup.purgeExpiredOperationalDocs(50);
  assert.ok(summary.deletedTriagePasses >= 1);
  assert.ok(summary.deletedRateLimits >= 1);
});


test("case intake re-evaluates structured measurements after a nonurgent screening", async () => {
  const {caseService} = await setupEnv();
  const parent = makeParent();
  const screening = {ageBandId: "newborn", symptomFlowId: "fever", temperatureCelsius: 37.1,
    durationHours: 2, selectedRedFlagIds: [], questionSummaryFa: "پرسش درباره پیگیری دمای کودک"};
  const pass = await caseService.issueConsultationTriagePass(parent, screening);
  assert.ok(pass.triagePassId);
  const request = {...screening, pharmacyId: "pharm_a", triagePassId: pass.triagePassId,
    categoryName: "SYMPTOMS", hasUrgentFlag: false, consentAccepted: true, consentVersion: "v1"};
  await assert.rejects(caseService.submitConsultationCase(parent, {...request, temperatureCelsius: 38}),
    (error: unknown) => error instanceof AppError && error.code === "failed-precondition");
  const result = await caseService.submitConsultationCase(parent, request);
  assert.equal(result.questionSummaryFa, screening.questionSummaryFa);
});
