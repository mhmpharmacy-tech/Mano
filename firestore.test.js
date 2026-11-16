const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validPastDate() {
  return new Date(Date.now() - 2000);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- UNAUTHENTICATED DENIAL ---

test("Unauthenticated user cannot read or write any user subcollections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/profiles`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/doseLogs`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/consultations`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/bookmarks`).get());
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/doseLogs`).doc("log1").set({
      userId: ALICE_UID,
      medicationSlug: "acetaminophen-pediatric",
      medicationNameFa: "استامینوفن",
      dosageFormFa: "قطره خوراکی",
      volumeMlText: "1.5 mL",
      givenAtLabelFa: "ساعت ۱۰:۰۰",
      noteFa: "",
      createdAt: validPastDate(),
      updatedAt: validPastDate(),
    })
  );
});

// --- CHILD PROFILE ---

test("Owner can create, read, and update child profile; ghost field and cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = validPastDate();

  const aliceProfileRef = aliceDb.collection(`users/${ALICE_UID}/profiles`).doc("primary");
  await assertSucceeds(
    aliceProfileRef.set({
      userId: ALICE_UID,
      aliasFa: "نیما",
      birthDateOrAgeFa: "۲ سال و ۴ ماه",
      ageBandId: "toddler_1_3y",
      weightKgText: "13",
      weightDateFa: "امروز",
      allergiesFa: "ندارد",
      currentMedsFa: "قطره آهن",
      consentGranted: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Read own profile succeeds
  await assertSucceeds(aliceProfileRef.get());

  // Cross-user read & write denied
  await assertFails(bobDb.collection(`users/${ALICE_UID}/profiles`).doc("primary").get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/profiles`).get());

  // Partial update succeeds while keeping createdAt immutable
  await assertSucceeds(
    aliceProfileRef.update({
      weightKgText: "13.5",
      updatedAt: validPastDate(),
    })
  );

  // Shadow/ghost field rejected
  await assertFails(
    aliceProfileRef.update({
      isAdmin: true,
      updatedAt: validPastDate(),
    })
  );

  // Mutating createdAt rejected
  await assertFails(
    aliceProfileRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: validPastDate(),
    })
  );
});

// --- MEDICATION DOSE LOGS ---

test("Owner can create, list, update, and delete dose logs; cross-user and invalid payload denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = validPastDate();

  const logRef = aliceDb.collection(`users/${ALICE_UID}/doseLogs`).doc("log_1");
  await assertSucceeds(
    logRef.set({
      userId: ALICE_UID,
      medicationSlug: "acetaminophen-pediatric",
      medicationNameFa: "استامینوفن کودکان",
      dosageFormFa: "شربت 120mg/5mL با سرنگ",
      volumeMlText: "5.0 mL",
      givenAtLabelFa: "۱۴:۳۰",
      noteFa: "بعد از غذا",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/doseLogs`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/doseLogs`).doc("log_1").get());

  // Spoofed userId denied
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/doseLogs`).doc("log_spoof").set({
      userId: BOB_UID,
      medicationSlug: "acetaminophen-pediatric",
      medicationNameFa: "استامینوفن",
      dosageFormFa: "شربت",
      volumeMlText: "5 mL",
      givenAtLabelFa: "۱۰:۰۰",
      noteFa: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(logRef.delete());
});

// --- PHARMACIST CONSULTATIONS ---

test("Consultation enforces PENDING start, sequential status progression, and terminal REVIEWED lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = validPastDate();
  const docRef = aliceDb.collection(`users/${ALICE_UID}/consultations`).doc("cons_1");

  // Cannot skip directly to REVIEWED on create
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/consultations`).doc("cons_bad").set({
      userId: ALICE_UID,
      categoryName: "OTC",
      ageBandId: "toddler_1_3y",
      questionSummaryFa: "آیا مصرف قطره هم‌زمان مجاز است؟",
      hasUrgentFlag: false,
      consentAccepted: true,
      status: "REVIEWED",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid PENDING create succeeds
  await assertSucceeds(
    docRef.set({
      userId: ALICE_UID,
      categoryName: "OTC",
      ageBandId: "toddler_1_3y",
      questionSummaryFa: "آیا مصرف هم‌زمان شربت و قطره آهن تداخل دارد؟",
      hasUrgentFlag: false,
      consentAccepted: true,
      status: "PENDING",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cannot jump PENDING -> REVIEWED directly
  await assertFails(docRef.update({ status: "REVIEWED", updatedAt: validPastDate() }));

  // PENDING -> RECEIVED -> REVIEWED succeeds
  await assertSucceeds(docRef.update({ status: "RECEIVED", updatedAt: validPastDate() }));
  await assertSucceeds(docRef.update({ status: "REVIEWED", updatedAt: validPastDate() }));

  // Once REVIEWED, further updates locked
  await assertFails(docRef.update({ status: "PENDING", updatedAt: validPastDate() }));
});

// --- ARTICLE BOOKMARKS ---

test("Bookmarks enforce slug == docId and immutable updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bmRef = aliceDb.collection(`users/${ALICE_UID}/bookmarks`).doc("fever-care");

  await assertSucceeds(
    bmRef.set({
      userId: ALICE_UID,
      articleSlug: "fever-care",
      createdAt: validPastDate(),
    })
  );

  await assertFails(bmRef.update({ articleSlug: "other-slug" }));
  await assertSucceeds(bmRef.delete());
});

// --- FEVER LOGS ---

test("Owner can create valid feverLogs; out-of-range temperature and cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = validPastDate();

  const feverRef = aliceDb.collection(`users/${ALICE_UID}/feverLogs`).doc("temp_1");
  await assertSucceeds(
    feverRef.set({
      userId: ALICE_UID,
      tempCelsius: 38.4,
      siteFa: "زیربغلی",
      recordedAtLabelFa: "۲۲:۱۵",
      caregiverFa: "مادر",
      noteFa: "پیش از مصرف شربت",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Invalid temperature outside physiological bounds rejected
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/feverLogs`).doc("temp_bad").set({
      userId: ALICE_UID,
      tempCelsius: 49.0,
      siteFa: "زیربغلی",
      recordedAtLabelFa: "۲۲:۳۰",
      caregiverFa: "مادر",
      noteFa: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(bobDb.collection(`users/${ALICE_UID}/feverLogs`).doc("temp_1").get());
  await assertSucceeds(feverRef.delete());
});

// --- DOCTOR VISIT DRAFTS ---

test("Owner can create, list, update, and delete visitDrafts; cross-user, unauth, and ghost fields fail", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = validPastDate();

  await assertFails(unauthDb.collection(`users/${ALICE_UID}/visitDrafts`).get());

  const draftRef = aliceDb.collection(`users/${ALICE_UID}/visitDrafts`).doc("draft_1");
  await assertSucceeds(
    draftRef.set({
      userId: ALICE_UID,
      titleFa: "پیش‌نویس ویزیت تب کودک",
      symptomContextFa: "تب ۳۸.۹ درجه به مدت ۱۲ ساعت",
      timelineSummaryFa: "۲ ثبت دما (اوج ۳۸.۹°C) • ۱ نوبت استامینوفن",
      draftTextFa: "خلاصه وضعیت کودک برای ارائه به پزشک متخصص اطفال...",
      parentNoteFa: "بی‌اشتهایی خفیف از صبح",
      modelName: "gemini-3.8-flash",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/visitDrafts`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/visitDrafts`).doc("draft_1").get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/visitDrafts`).get());

  // Valid partial update succeeds while preserving createdAt and modelName
  await assertSucceeds(
    draftRef.update({
      titleFa: "پیش‌نویس ویزیت (ویرایش‌شده)",
      draftTextFa: "متن به‌روزرسانی‌شده توسط والد پیش از ویزیت.",
      updatedAt: validPastDate(),
    })
  );

  // Shadow/ghost field rejected
  await assertFails(
    draftRef.update({
      isVerifiedByDoctor: true,
      updatedAt: validPastDate(),
    })
  );

  // Mutating immutable modelName rejected
  await assertFails(
    draftRef.update({
      modelName: "other-model",
      updatedAt: validPastDate(),
    })
  );

  await assertSucceeds(draftRef.delete());
});

// --- VACCINE RECORDS ---

test("Owner can create, list, update, and delete vaccineRecords; negative epochs, ghost fields, and cross-user fail", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = validPastDate();

  await assertFails(unauthDb.collection(`users/${ALICE_UID}/vaccineRecords`).get());

  const recRef = aliceDb.collection(`users/${ALICE_UID}/vaccineRecords`).doc("2m_pentavalent_opv_rota");
  await assertSucceeds(
    recRef.set({
      userId: ALICE_UID,
      vaccineId: "2m_pentavalent_opv_rota",
      customDueDateEpochMs: 1750000000000,
      administeredDateEpochMs: 0,
      isDeferred: false,
      facilityOrLotFa: "مرکز بهداشت شماره ۲",
      noteFa: "نوبت اول ۲ ماهگی",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/vaccineRecords`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/vaccineRecords`).doc("2m_pentavalent_opv_rota").get());

  // Valid update marking administered date
  await assertSucceeds(
    recRef.update({
      administeredDateEpochMs: 1750086400000,
      noteFa: "تزریق با موفقیت انجام شد",
      updatedAt: validPastDate(),
    })
  );

  // Negative epoch rejected
  await assertFails(
    recRef.update({
      administeredDateEpochMs: -1,
      updatedAt: validPastDate(),
    })
  );

  // Ghost field rejected
  await assertFails(
    recRef.update({
      adminOverride: true,
      updatedAt: validPastDate(),
    })
  );

  await assertSucceeds(recRef.delete());
});

// --- SYRUP SCANS ---

test("Owner can create, read, update, and delete confirmed syrupScans; unconfirmed, cross-user, unauth, and ghost fields fail", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = validPastDate();

  await assertFails(unauthDb.collection(`users/${ALICE_UID}/syrupScans`).get());

  const scanRef = aliceDb.collection(`users/${ALICE_UID}/syrupScans`).doc("scan_1");
  await assertSucceeds(
    scanRef.set({
      userId: ALICE_UID,
      drugNameFa: "شربت استامینوفن کودکان ۱۲۰/۵",
      activeIngredientKey: "ACETAMINOPHEN",
      dosageForm: "SYRUP",
      mgAmount: 120,
      perMlAmount: 5,
      mgPerMl: 24,
      matchedFormulationId: "acetaminophen_syrup_120_5",
      confidenceLevel: "HIGH",
      concentrationDisplayFa: "120 mg / 5 mL",
      safetyNoteFa: "غلظت استاندارد شربت استامینوفن کودکان.",
      confirmedByParent: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cannot persist unconfirmed scan
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/syrupScans`).doc("scan_unconfirmed").set({
      userId: ALICE_UID,
      drugNameFa: "قطره استامینوفن",
      activeIngredientKey: "ACETAMINOPHEN",
      dosageForm: "DROPS",
      mgAmount: 100,
      perMlAmount: 1,
      mgPerMl: 100,
      matchedFormulationId: "acetaminophen_drops_100",
      confidenceLevel: "HIGH",
      concentrationDisplayFa: "100 mg / 1 mL",
      safetyNoteFa: "",
      confirmedByParent: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/syrupScans`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/syrupScans`).doc("scan_1").get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/syrupScans`).get());

  // Valid partial update of edited strength
  await assertSucceeds(
    scanRef.update({
      mgAmount: 160,
      perMlAmount: 5,
      mgPerMl: 32,
      matchedFormulationId: "acetaminophen_syrup_160_5",
      concentrationDisplayFa: "160 mg / 5 mL",
      updatedAt: validPastDate(),
    })
  );

  // Negative/zero concentration rejected
  await assertFails(
    scanRef.update({
      mgPerMl: 0,
      updatedAt: validPastDate(),
    })
  );

  // Ghost field rejected
  await assertFails(
    scanRef.update({
      autoAdminister: true,
      updatedAt: validPastDate(),
    })
  );

  await assertSucceeds(scanRef.delete());
});


