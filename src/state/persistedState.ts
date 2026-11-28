import type { CareCardEntry, ChildProfile, GrowthMeasurementEntry, NutritionPlan, PharmacistConsultation, SavedSyrupScan, SavedVisitDraft, TimelineDosePoint, TimelineTempPoint, VaccineRecord, VaccineReminderSettings } from '../domain/models.ts';
import { createDefaultTreatmentPlan, normalizePlanForToday, type PediatricTreatmentPlan } from '../domain/treatmentPlan.ts';
import { currentStorageEpoch, LEGACY_EPOCH, readLocalSnapshot, type LocalStoragePort } from './localPersistence.ts';

export const LOCAL_RECOVERY_ISSUE = 'اطلاعات ذخیره‌شده قابل خواندن نیست. برای جلوگیری از از دست رفتن داده‌ها، ذخیرهٔ تغییرات متوقف شده است. فایل پشتیبان معتبر را بازیابی کنید یا داده‌های محلی را آگاهانه پاک کنید.';

export interface PersistedStateLoad {
  state: PersistedState;
  issue: string | null;
}

export type HomeViewStyle = 'calm' | 'orbital';

export interface PersistedState {
  storageEpoch: string;
  theme: 'light' | 'dark';
  homeViewStyle: HomeViewStyle;
  onboardingSeen: boolean;
  childProfile: ChildProfile;
  feverLogs: TimelineTempPoint[];
  doseLogs: TimelineDosePoint[];
  growthMeasurements: GrowthMeasurementEntry[];
  checkedMilestones: string[];
  vaccineBirthDateEpochMs: number | null;
  vaccineRecords: Record<string, VaccineRecord>;
  vaccineReminders: VaccineReminderSettings;
  checkedMedSafetyIds: string[];
  checkedHygieneIds: string[];
  bookmarkedSlugs: string[];
  treatmentPlan: PediatricTreatmentPlan;
  savedVisitDrafts: SavedVisitDraft[];
  nutritionPlans: NutritionPlan[];
  savedSyrupScans: SavedSyrupScan[];
  pharmacistConsultations: PharmacistConsultation[];
  careCards: CareCardEntry[];
}

function defaultChildProfile(): ChildProfile {
  return {
    id: 'child_primary',
    aliasFa: 'کودک من',
    ageBand: 'TODDLER_1_3Y',
    ageMonthsExact: null,
    sex: null, // Never fabricate initial biological sex
    birthDateOrAgeFa: '',
    birthDateEpochMs: null,
    weightKgText: '',
    allergiesFa: '',
    updatedAtLabelFa: 'پروفایل آماده ثبت',
  };
}

export function createInitialPersistedState(storageEpoch = LEGACY_EPOCH): PersistedState {
  const autostart =
    typeof window !== 'undefined' && new URLSearchParams(window.location.search).get('autostart') === '1';

  return {
    storageEpoch,
    theme: 'light',
    homeViewStyle: 'calm',
    onboardingSeen: autostart,
    childProfile: defaultChildProfile(),
    feverLogs: [],
    doseLogs: [],
    growthMeasurements: [], // Initially empty — zero fabricated percentiles
    checkedMilestones: [],
    vaccineBirthDateEpochMs: null,
    vaccineRecords: {},
    vaccineReminders: { enabled: false, leadDaysBefore: 3 },
    checkedMedSafetyIds: [],
    checkedHygieneIds: [],
    bookmarkedSlugs: [],
    treatmentPlan: createDefaultTreatmentPlan(),
    savedVisitDrafts: [],
    nutritionPlans: [],
    savedSyrupScans: [],
    pharmacistConsultations: [],
    careCards: [],
  };
}

type Rule = 'string' | 'number' | 'boolean' | { optional: Rule } | { nullable: Rule } |
  { list: Rule } | { record: Rule } | { shape: Record<string, Rule> };

const optional = (rule: Rule): Rule => ({ optional: rule });
const nullable = (rule: Rule): Rule => ({ nullable: rule });
const list = (rule: Rule): Rule => ({ list: rule });
const shape = (fields: Record<string, Rule>): Rule => ({ shape: fields });
const stringList = list('string');

function assertObject(input: unknown): asserts input is Record<string, unknown> {
  if (!input || typeof input !== 'object' || Array.isArray(input) ||
      (Object.getPrototypeOf(input) !== Object.prototype && Object.getPrototypeOf(input) !== null)) {
    throw new Error('INVALID_LOCAL_SNAPSHOT');
  }
}

/** Structural recovery guard, deliberately separate from strict backup import limits. */
function assertShape(input: unknown, rule: Rule): void {
  if (typeof rule === 'string') {
    if (typeof input !== rule || (rule === 'number' && !Number.isFinite(input))) throw new Error('INVALID_LOCAL_SNAPSHOT');
  } else if ('optional' in rule) {
    if (input !== undefined) assertShape(input, rule.optional);
  } else if ('nullable' in rule) {
    if (input !== null) assertShape(input, rule.nullable);
  } else if ('list' in rule) {
    if (!Array.isArray(input)) throw new Error('INVALID_LOCAL_SNAPSHOT');
    for (const value of input) assertShape(value, rule.list);
  } else if ('record' in rule) {
    assertObject(input);
    for (const value of Object.values(input)) assertShape(value, rule.record);
  } else {
    assertObject(input);
    for (const [key, valueRule] of Object.entries(rule.shape)) assertShape(input[key], valueRule);
  }
}

const medicationShape = shape({
  id: 'string', nameWithStrengthFa: 'string', form: 'string', doseAmountFa: 'string',
  intervalHours: nullable('number'), scheduleLabelFa: 'string', foodRelationFa: 'string',
  counselingNoteFa: 'string', isWarningHighlight: 'boolean', dailyDoseSlots: 'number',
  completedSlotTimesFa: { record: 'string' },
});
const planShape = shape({
  pharmacyNameFa: 'string', pharmacistBadgeFa: 'string', fallbackChildSummaryFa: 'string',
  prescriptionDateFa: 'string', storageInstructionFa: 'string', smartRemindersEnabled: 'boolean',
  lastCheckoffDayKey: 'string', medications: list(medicationShape),
  legacyUnreviewedMedications: optional(list(medicationShape)),
});
const persistedShape = shape({
  storageEpoch: 'string', theme: 'string', homeViewStyle: 'string', onboardingSeen: 'boolean',
  childProfile: shape({
    id: 'string', aliasFa: 'string', ageBand: 'string', birthDateOrAgeFa: 'string',
    weightKgText: 'string', allergiesFa: 'string', updatedAtLabelFa: 'string',
    ageMonthsExact: optional(nullable('number')), sex: optional(nullable('string')),
    birthDateEpochMs: optional(nullable('number')),
  }),
  feverLogs: list(shape({ id: 'string', tempCelsius: 'number', timestampMs: 'number', timeLabelFa: 'string', siteFa: 'string', caregiverFa: 'string', noteFa: 'string', recordedAtMs: optional('number'), isSyncedCloud: optional('boolean') })),
  doseLogs: list(shape({ id: 'string', medicationSlug: 'string', medicationNameFa: 'string', dosageFormFa: 'string', volumeMlText: 'string', timestampMs: 'number', givenAtLabelFa: 'string', noteFa: 'string', recordedAtMs: optional('number'), isSyncedCloud: optional('boolean') })),
  growthMeasurements: list(shape({ id: 'string', metricId: 'string', sex: 'string', ageMonths: 'number', measuredValue: 'number', zScore: 'number', percentile: 'number', bandLabelFa: 'string', recordedAtLabelFa: 'string' })),
  checkedMilestones: stringList, vaccineBirthDateEpochMs: nullable('number'),
  vaccineRecords: { record: shape({ vaccineId: 'string', updatedAtMs: 'number', administeredDateEpochMs: optional(nullable('number')), customDueDateEpochMs: optional(nullable('number')), isDeferred: optional('boolean'), facilityNameFa: optional('string'), lotNumber: optional('string'), noteFa: optional('string') }) },
  vaccineReminders: shape({ enabled: 'boolean', leadDaysBefore: 'number' }),
  checkedMedSafetyIds: stringList, checkedHygieneIds: stringList, bookmarkedSlugs: stringList,
  treatmentPlan: planShape,
  savedVisitDrafts: list(shape({ id: 'string', titleFa: 'string', draftTextFa: 'string', createdAtLabelFa: 'string' })),
  nutritionPlans: list(shape({
    id: 'string', planTitleFa: 'string', overviewFa: 'string', growthNutritionFocusFa: 'string', hydrationTipFa: 'string', ageMonthsSnapshot: 'number', goalSnapshotFa: 'string', createdAtLabelFa: 'string',
    meals: list(shape({ mealTypeFa: 'string', titleFa: 'string', whySuitableFa: 'string', textureAndPortionFa: 'string', ingredientsFa: stringList, prepStepsFa: stringList, safetyAvoidListFa: stringList, doctorFollowUpHintFa: 'string' })),
  })),
  savedSyrupScans: list(shape({ id: 'string', detectedDrugNameFa: 'string', ingredientKey: 'string', dosageForm: 'string', mgAmount: 'number', perMlAmount: 'number', matchedFormulationIdRaw: 'string', confidence: 'string', safetyNoteFa: 'string', parentVerified: 'boolean', createdAtMs: 'number', createdAtLabelFa: 'string' })),
  pharmacistConsultations: list(shape({ id: 'string', createdAtMs: 'number', createdAtLabelFa: 'string', childAgeBand: 'string', weightKgText: 'string', topicFa: 'string', questionTextFa: 'string', symptomsSelectedFa: stringList, urgencyLevel: 'string', statusFa: 'string', pharmacistReplyFa: optional('string'), repliedAtLabelFa: optional('string') })),
  careCards: list(shape({ id: 'string', titleFa: 'string', subtitleFa: 'string', cardType: 'string', badgeFa: 'string', summaryLinesFa: stringList, footerNoteFa: 'string', createdAtLabelFa: 'string', shareUrl: optional('string') })),
});

export function loadPersistedState(options: {
  fallbackEpoch?: string;
  storage?: LocalStoragePort | null;
  search?: string;
} = {}): PersistedStateLoad {
  let epoch = options.fallbackEpoch ?? LEGACY_EPOCH;
  try {
    const storage = options.storage === undefined
      ? (typeof window === 'undefined' ? null : window.localStorage)
      : options.storage;
    if (!storage) return { state: createInitialPersistedState(epoch), issue: null };
    epoch = currentStorageEpoch(storage);
    const { snapshot: parsed } = readLocalSnapshot<Partial<PersistedState>>(storage);
    const fresh = createInitialPersistedState(epoch);
    if (!parsed) return { state: fresh, issue: null };
    assertObject(parsed);
    if (parsed.childProfile !== undefined) assertObject(parsed.childProfile);
    // Validate before day normalization: that migration can legitimately clear
    // yesterday's checkoffs, but must not conceal an invalid medication row.
    if (parsed.treatmentPlan !== undefined) assertShape(parsed.treatmentPlan, planShape);
    const search = options.search ?? (typeof window === 'undefined' ? '' : window.location.search);
    const autostart = new URLSearchParams(search).get('autostart') === '1';
    const merged: PersistedState = {
      ...fresh,
      ...parsed,
      storageEpoch: epoch,
      childProfile: {
        ...fresh.childProfile,
        ...(parsed.childProfile || {}),
      },
      treatmentPlan: normalizePlanForToday(parsed.treatmentPlan || fresh.treatmentPlan),
      onboardingSeen: autostart ? true : Boolean(parsed.onboardingSeen),
    };
    assertShape(merged, persistedShape);
    return { state: merged, issue: null };
  } catch {
    // Never turn unreadable current-generation records into an empty writable
    // snapshot. The provider makes this fallback read-only until explicit recovery.
    const state = createInitialPersistedState(epoch);
    state.onboardingSeen = true;
    return { state, issue: LOCAL_RECOVERY_ISSUE };
  }
}

