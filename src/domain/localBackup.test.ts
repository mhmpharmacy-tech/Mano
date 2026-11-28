import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import {
  createEncryptedLocalBackup,
  LocalBackupError,
  MAX_LOCAL_BACKUP_FILE_BYTES,
  openEncryptedLocalBackup,
  prepareLocalBackupForRestore,
  validateLocalBackupSnapshot,
  type LocalBackupSnapshot,
} from './localBackup.ts';
import { createDefaultTreatmentPlan, type TreatmentMedicationItem } from './treatmentPlan.ts';

const PASSPHRASE = 'example-only passphrase 42';
const NOW = Date.now();

function medication(id: string): TreatmentMedicationItem {
  return {
    id,
    nameWithStrengthFa: 'نام و غلظت رونویسی‌شدهٔ نمونه',
    form: 'SYRUP_SUSPENSION',
    doseAmountFa: 'مقدار ثبت‌شده توسط مراقب',
    intervalHours: null,
    scheduleLabelFa: 'دستور ثبت‌شده از روی برچسب',
    foodRelationFa: '',
    counselingNoteFa: 'یادداشت نمونهٔ ویرایش‌شده',
    isWarningHighlight: false,
    dailyDoseSlots: 2,
    completedSlotTimesFa: { 1: '۱۰:۳۰' },
  };
}

function snapshot(): LocalBackupSnapshot {
  return {
    theme: 'light',
    homeViewStyle: 'calm',
    onboardingSeen: true,
    childProfile: {
      id: 'child_primary',
      aliasFa: 'نمونهٔ خصوصی پشتیبان',
      ageBand: 'TODDLER_1_3Y',
      ageMonthsExact: null,
      sex: null,
      birthDateOrAgeFa: '',
      birthDateEpochMs: null,
      weightKgText: '',
      allergiesFa: 'صرفاً متن ساختگی آزمایش',
      updatedAtLabelFa: '',
    },
    feverLogs: [{
      id: 'temp_sample', tempCelsius: 38.1, timestampMs: NOW - 120_000,
      recordedAtMs: NOW - 60_000, timeLabelFa: '۱۰:۳۰', siteFa: 'زیر بغل', caregiverFa: '', noteFa: '',
    }],
    doseLogs: [{
      id: 'dose_sample', medicationSlug: 'recorded-label', medicationNameFa: 'نام ثبت‌شده',
      dosageFormFa: 'شکل ثبت‌شده', volumeMlText: 'مقدار واردشده', timestampMs: NOW - 360_000,
      recordedAtMs: NOW - 60_000, givenAtLabelFa: 'زمان نمونه', noteFa: 'صرفاً دادهٔ ساختگی',
    }],
    growthMeasurements: [{
      id: 'growth_sample', metricId: 'weight_for_age', sex: 'girl', ageMonths: 18,
      measuredValue: 11, zScore: 0, percentile: 50, bandLabelFa: 'ثبت آموزشی نمونه', recordedAtLabelFa: 'ثبت نمونه',
    }],
    checkedMilestones: ['milestone_sample'],
    vaccineBirthDateEpochMs: null,
    vaccineRecords: { vaccine_sample: { vaccineId: 'vaccine_sample', updatedAtMs: NOW - 60_000, isDeferred: true } },
    vaccineReminders: { enabled: false, leadDaysBefore: 3 },
    checkedMedSafetyIds: ['safety_sample'],
    checkedHygieneIds: ['hygiene_sample'],
    bookmarkedSlugs: ['article_sample'],
    treatmentPlan: {
      ...createDefaultTreatmentPlan(),
      medications: [medication('rx_custom_sample')],
      legacyUnreviewedMedications: [
        medication('rx_azithromycin_200'),
        { ...medication('rx_azithromycin_200'), doseAmountFa: 'ویرایش قدیمی دیگر' },
      ],
    },
    savedVisitDrafts: [{ id: 'draft_sample', titleFa: 'یادداشت نمونه', draftTextFa: 'متن خصوصی نمونه', createdAtLabelFa: 'تاریخ نمونه' }],
    nutritionPlans: [{
      id: 'nutrition_sample', planTitleFa: 'پیش‌نویس نمونه', overviewFa: 'متن ساختگی',
      growthNutritionFocusFa: '', hydrationTipFa: '', ageMonthsSnapshot: 18,
      goalSnapshotFa: '', createdAtLabelFa: '', meals: [{
        mealTypeFa: 'وعدهٔ نمونه', titleFa: '', whySuitableFa: '', textureAndPortionFa: '',
        ingredientsFa: ['متن نمونه'], prepStepsFa: [], safetyAvoidListFa: [], doctorFollowUpHintFa: '',
      }],
    }],
    savedSyrupScans: [{
      id: 'scan_sample', detectedDrugNameFa: 'برچسب نمونه', ingredientKey: 'UNKNOWN', dosageForm: 'UNKNOWN',
      mgAmount: 0, perMlAmount: 0, matchedFormulationIdRaw: '', confidence: 'LOW', safetyNoteFa: '',
      parentVerified: false, createdAtMs: NOW - 60_000, createdAtLabelFa: '',
    }],
    pharmacistConsultations: [{
      id: 'consultation_sample', createdAtMs: NOW - 60_000, createdAtLabelFa: '',
      childAgeBand: 'TODDLER_1_3Y', weightKgText: '', topicFa: '', questionTextFa: 'یادداشت نمونهٔ ارسال‌نشده',
      symptomsSelectedFa: [], urgencyLevel: 'ROUTINE_PHARMACIST', statusFa: 'یادداشت محلی',
      pharmacistReplyFa: 'متن یادداشت تاریخی', repliedAtLabelFa: '',
    }],
    careCards: [{
      id: 'card_sample', titleFa: 'کارت نمونه', subtitleFa: '', cardType: 'CAREGIVER_HANDOFF', badgeFa: '',
      summaryLinesFa: ['ثبت نمونه'], footerNoteFa: '', createdAtLabelFa: '',
      shareUrl: 'https://manokid.ir/care/sample',
    }],
  };
}

function errorCode(code: LocalBackupError['code']) {
  return (error: unknown) => error instanceof LocalBackupError && error.code === code;
}

describe('local backup schema and restore boundary', () => {
  it('copies every supported record without adding age, weight, prescriptions or approval', () => {
    const input = snapshot();
    const validated = validateLocalBackupSnapshot(input, NOW);
    assert.deepEqual(validated, input);
    assert.notEqual(validated.childProfile, input.childProfile);
    assert.equal(validated.childProfile.ageMonthsExact, null);
    assert.equal(validated.childProfile.weightKgText, '');
    assert.deepEqual(validated.treatmentPlan.legacyUnreviewedMedications, input.treatmentPlan.legacyUnreviewedMedications);
    validated.childProfile.aliasFa = 'تغییر کپی';
    assert.notEqual(input.childProfile.aliasFa, validated.childProfile.aliasFa);
  });

  it('keeps conflicting entered age/birth data for correction rather than inventing a reconciliation', () => {
    const input = snapshot();
    input.childProfile.ageMonthsExact = 2.7;
    input.childProfile.birthDateEpochMs = NOW - 400 * 86_400_000;
    input.vaccineBirthDateEpochMs = NOW - 500 * 86_400_000;
    const restored = prepareLocalBackupForRestore(input, NOW);
    assert.equal(restored.childProfile.ageMonthsExact, 2.7);
    assert.equal(restored.childProfile.birthDateEpochMs, input.childProfile.birthDateEpochMs);
    assert.equal(restored.vaccineBirthDateEpochMs, input.vaccineBirthDateEpochMs);
  });

  it('does not carry forward bottle verification, cloud sync or unavailable reminder switches', () => {
    const input = snapshot();
    input.feverLogs[0].isSyncedCloud = true;
    input.doseLogs[0].isSyncedCloud = true;
    input.savedSyrupScans[0].parentVerified = true;
    input.vaccineReminders.enabled = true;
    input.treatmentPlan.smartRemindersEnabled = true;
    const restored = prepareLocalBackupForRestore(input, NOW);
    assert.equal(restored.feverLogs[0].isSyncedCloud, false);
    assert.equal(restored.doseLogs[0].isSyncedCloud, false);
    assert.equal(restored.savedSyrupScans[0].parentVerified, false);
    assert.equal(restored.vaccineReminders.enabled, false);
    assert.equal(restored.treatmentPlan.smartRemindersEnabled, false);
    assert.equal(input.savedSyrupScans[0].parentVerified, true, 'Validation must not mutate live state');
    assert.deepEqual(restored.treatmentPlan.medications, input.treatmentPlan.medications);
    assert.deepEqual(restored.savedVisitDrafts, input.savedVisitDrafts);
    assert.deepEqual(restored.pharmacistConsultations, input.pharmacistConsultations);
  });

  it('rejects storage/session/approval injection, unknown nested fields and inherited objects', () => {
    for (const extra of [
      { storageEpoch: 'attacker-generation' }, { session: 'forged-session' },
      { activeHighFeverAlert: { tempCelsius: 40 } }, { clinicalApproval: true },
    ]) assert.throws(() => validateLocalBackupSnapshot({ ...snapshot(), ...extra }, NOW), errorCode('BACKUP_INVALID_DATA'));
    const injected = snapshot();
    Object.assign(injected.childProfile, { clinicalApproval: true });
    assert.throws(() => validateLocalBackupSnapshot(injected, NOW), errorCode('BACKUP_INVALID_DATA'));
    const inherited = Object.create({ theme: 'light' });
    Object.assign(inherited, snapshot());
    assert.throws(() => validateLocalBackupSnapshot(inherited, NOW), errorCode('BACKUP_INVALID_DATA'));
  });

  it('rejects prototype-pollution names and accessor properties without invoking the accessor', () => {
    const input = snapshot();
    input.vaccineRecords = JSON.parse('{"__proto__":{"vaccineId":"__proto__","updatedAtMs":1}}');
    assert.throws(() => validateLocalBackupSnapshot(input, NOW), errorCode('BACKUP_INVALID_DATA'));
    const other = snapshot();
    let getterCalled = false;
    Object.defineProperty(other.childProfile, 'aliasFa', { enumerable: true, get() { getterCalled = true; return 'bad'; } });
    assert.throws(() => validateLocalBackupSnapshot(other, NOW), errorCode('BACKUP_INVALID_DATA'));
    assert.equal(getterCalled, false);
    assert.equal(Object.hasOwn(Object.prototype, 'vaccineId'), false);
    const arrayAccessor = snapshot();
    Object.defineProperty(arrayAccessor.checkedMilestones, '0', { enumerable: true, get() { getterCalled = true; return 'bad'; } });
    assert.throws(() => validateLocalBackupSnapshot(arrayAccessor, NOW), errorCode('BACKUP_INVALID_DATA'));
    assert.equal(getterCalled, false, 'Array accessors are not JSON data either');
    const hidden = snapshot();
    Object.defineProperty(hidden.childProfile, 'hiddenSession', { value: 'not allowed', enumerable: false });
    assert.throws(() => validateLocalBackupSnapshot(hidden, NOW), errorCode('BACKUP_INVALID_DATA'));
  });

  it('rejects duplicate active identities, bad enums, fake numbers and unsafe link schemes', () => {
    const duplicate = snapshot();
    duplicate.doseLogs.push({ ...duplicate.doseLogs[0] });
    assert.throws(() => validateLocalBackupSnapshot(duplicate, NOW), errorCode('BACKUP_INVALID_DATA'));
    const badEnum = snapshot();
    Object.assign(badEnum.savedSyrupScans[0], { confidence: 'APPROVED' });
    assert.throws(() => validateLocalBackupSnapshot(badEnum, NOW), errorCode('BACKUP_INVALID_DATA'));
    for (const value of [NaN, Infinity, -1, 145, '2']) {
      const badNumber = snapshot();
      Object.assign(badNumber.childProfile, { ageMonthsExact: value });
      assert.throws(() => validateLocalBackupSnapshot(badNumber, NOW), errorCode('BACKUP_INVALID_DATA'));
    }
    for (const url of ['javascript:alert(1)', 'data:text/html,test', 'http://manokid.ir/', 'https://user:password@example.test/']) {
      const unsafe = snapshot(); unsafe.careCards[0].shareUrl = url;
      assert.throws(() => validateLocalBackupSnapshot(unsafe, NOW), errorCode('BACKUP_INVALID_DATA'));
    }
  });

  it('rejects future clinical event timestamps but accepts a future vaccine due date', () => {
    const input = snapshot();
    input.feverLogs[0].timestampMs = NOW + 1;
    assert.throws(() => validateLocalBackupSnapshot(input, NOW), errorCode('BACKUP_INVALID_DATA'));
    input.feverLogs[0].timestampMs = NOW - 1;
    input.vaccineRecords.vaccine_sample.customDueDateEpochMs = NOW + 30 * 86_400_000;
    assert.equal(validateLocalBackupSnapshot(input, NOW).vaccineRecords.vaccine_sample.customDueDateEpochMs, NOW + 30 * 86_400_000);
    input.vaccineRecords.vaccine_sample.vaccineId = 'another_id';
    assert.throws(() => validateLocalBackupSnapshot(input, NOW), errorCode('BACKUP_INVALID_DATA'));
  });

  it('rejects missing fields, excess records, oversized text and aggregate payloads without silently dropping entries', () => {
    const missing: Partial<LocalBackupSnapshot> = snapshot(); delete missing.doseLogs;
    assert.throws(() => validateLocalBackupSnapshot(missing, NOW), errorCode('BACKUP_INVALID_DATA'));
    const excess = snapshot(); excess.checkedMilestones = Array(1_001).fill('sample');
    assert.throws(() => validateLocalBackupSnapshot(excess, NOW), errorCode('BACKUP_TOO_LARGE'));
    const long = snapshot(); long.childProfile.aliasFa = 'x'.repeat(257);
    assert.throws(() => validateLocalBackupSnapshot(long, NOW), errorCode('BACKUP_INVALID_DATA'));
    const aggregate = snapshot();
    aggregate.savedVisitDrafts = Array.from({ length: 45 }, (_, index) => ({ id: `draft_${index}`, titleFa: '', draftTextFa: 'x'.repeat(50_000), createdAtLabelFa: '' }));
    assert.throws(() => validateLocalBackupSnapshot(aggregate, NOW), errorCode('BACKUP_TOO_LARGE'));
  });
});

describe('encrypted local backup', () => {
  it('round-trips every supported category, keeps child details encrypted and resets imported trust', async () => {
    const input = snapshot();
    input.savedSyrupScans[0].parentVerified = true;
    const file = await createEncryptedLocalBackup(input, PASSPHRASE);
    const envelope = JSON.parse(file);
    assert.deepEqual(Object.keys(envelope).sort(), ['ciphertext', 'encryption', 'format', 'version']);
    assert.equal(envelope.format, 'mano-local-backup');
    assert.equal(envelope.version, 1);
    assert.equal(envelope.encryption.iterations, 600_000);
    assert.ok(!file.includes(input.childProfile.aliasFa));
    assert.ok(!file.includes(input.savedVisitDrafts[0].draftTextFa));
    assert.ok(!file.includes(PASSPHRASE));
    assert.deepEqual(await openEncryptedLocalBackup(file, PASSPHRASE), prepareLocalBackupForRestore(input));
  });

  it('uses independent salt and IV for repeated exports', async () => {
    const input = snapshot();
    const first = JSON.parse(await createEncryptedLocalBackup(input, PASSPHRASE));
    const second = JSON.parse(await createEncryptedLocalBackup(input, PASSPHRASE));
    assert.notEqual(first.encryption.salt, second.encryption.salt);
    assert.notEqual(first.encryption.iv, second.encryption.iv);
    assert.notEqual(first.ciphertext, second.ciphertext);
  });

  it('rejects the wrong passphrase and changes to authenticated ciphertext or IV', async () => {
    const file = await createEncryptedLocalBackup(snapshot(), PASSPHRASE);
    await assert.rejects(openEncryptedLocalBackup(file, 'incorrect passphrase 42'), errorCode('BACKUP_DECRYPT_FAILED'));
    const ciphertext = JSON.parse(file);
    ciphertext.ciphertext = `${ciphertext.ciphertext[0] === 'A' ? 'B' : 'A'}${ciphertext.ciphertext.slice(1)}`;
    await assert.rejects(openEncryptedLocalBackup(JSON.stringify(ciphertext), PASSPHRASE), errorCode('BACKUP_DECRYPT_FAILED'));
    const iv = JSON.parse(file);
    iv.encryption.iv = `${iv.encryption.iv[0] === 'A' ? 'B' : 'A'}${iv.encryption.iv.slice(1)}`;
    await assert.rejects(openEncryptedLocalBackup(JSON.stringify(iv), PASSPHRASE), errorCode('BACKUP_DECRYPT_FAILED'));
  });

  it('rejects unsupported formats, versions, algorithms and attacker-controlled KDF work factors', async () => {
    const file = await createEncryptedLocalBackup(snapshot(), PASSPHRASE);
    for (const changes of [
      { version: 2 },
      { encryption: { ...JSON.parse(file).encryption, iterations: 2_147_483_647 } },
      { encryption: { ...JSON.parse(file).encryption, iterations: 1 } },
      { encryption: { ...JSON.parse(file).encryption, algorithm: 'AES-CBC' } },
    ]) await assert.rejects(openEncryptedLocalBackup(JSON.stringify({ ...JSON.parse(file), ...changes }), PASSPHRASE), errorCode('BACKUP_UNSUPPORTED'));
    await assert.rejects(openEncryptedLocalBackup(JSON.stringify({ ...JSON.parse(file), format: 'other-app' }), PASSPHRASE), errorCode('BACKUP_INVALID_FILE'));
  });

  it('rejects malformed/oversized files and unexpected plaintext metadata before importing any state', async () => {
    await assert.rejects(openEncryptedLocalBackup('not JSON', PASSPHRASE), errorCode('BACKUP_INVALID_FILE'));
    await assert.rejects(openEncryptedLocalBackup(' '.repeat(MAX_LOCAL_BACKUP_FILE_BYTES + 1), PASSPHRASE), errorCode('BACKUP_TOO_LARGE'));
    const file = await createEncryptedLocalBackup(snapshot(), PASSPHRASE);
    const withMetadata = { ...JSON.parse(file), childAlias: 'not permitted' };
    await assert.rejects(openEncryptedLocalBackup(JSON.stringify(withMetadata), PASSPHRASE), errorCode('BACKUP_INVALID_FILE'));
    const invalidBase64 = JSON.parse(file); invalidBase64.ciphertext = 'x'.repeat(25);
    await assert.rejects(openEncryptedLocalBackup(JSON.stringify(invalidBase64), PASSPHRASE), errorCode('BACKUP_INVALID_FILE'));
  });

  it('requires a bounded exact passphrase and handles Persian and emoji passphrases without normalization', async () => {
    for (const passphrase of ['', 'short', ' '.repeat(12), 'x'.repeat(257), '\uD800'.repeat(12)]) {
      await assert.rejects(createEncryptedLocalBackup(snapshot(), passphrase), errorCode('BACKUP_PASSWORD_REQUIRED'));
    }
    const persian = 'رمز نمونهٔ ساختگی 🔒 مانو';
    const file = await createEncryptedLocalBackup(snapshot(), persian);
    assert.deepEqual(await openEncryptedLocalBackup(file, persian), prepareLocalBackupForRestore(snapshot()));
    await assert.rejects(openEncryptedLocalBackup(file, `${persian} `), errorCode('BACKUP_DECRYPT_FAILED'));
  });
});
