import type {
  AgeBandId,
  CareCardEntry,
  ChildProfile,
  GrowthMeasurementEntry,
  NutritionMealItem,
  NutritionPlan,
  PharmacistConsultation,
  SavedSyrupScan,
  SavedVisitDraft,
  TimelineDosePoint,
  TimelineTempPoint,
  VaccineRecord,
  VaccineReminderSettings,
} from './models.ts';
import type { PediatricTreatmentPlan, TreatmentMedicationItem } from './treatmentPlan.ts';

/** Backup data never supplies the storage generation, a session, or a live alert. */
export interface LocalBackupSnapshot {
  theme: 'light' | 'dark';
  homeViewStyle: 'calm' | 'orbital';
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

export const LOCAL_BACKUP_FORMAT = 'mano-local-backup';
export const LOCAL_BACKUP_VERSION = 1;
export const MAX_LOCAL_BACKUP_FILE_BYTES = 4 * 1024 * 1024;
export const MAX_LOCAL_BACKUP_PLAINTEXT_BYTES = 2 * 1024 * 1024;
export const MIN_BACKUP_PASSPHRASE_LENGTH = 12;
export const MAX_BACKUP_PASSPHRASE_LENGTH = 256;
const KDF_ITERATIONS = 600_000;
const MAX_NODES = 100_000;
const FUTURE_SCHEDULE_MS = 20 * 366 * 86_400_000;
const encoder = new TextEncoder();

export type LocalBackupErrorCode =
  | 'BACKUP_INVALID_DATA'
  | 'BACKUP_INVALID_FILE'
  | 'BACKUP_UNSUPPORTED'
  | 'BACKUP_TOO_LARGE'
  | 'BACKUP_PASSWORD_REQUIRED'
  | 'BACKUP_DECRYPT_FAILED'
  | 'BACKUP_CRYPTO_UNAVAILABLE';

const ERROR_MESSAGES: Record<LocalBackupErrorCode, string> = {
  BACKUP_INVALID_DATA: 'ساختار یا مقادیر اطلاعات پشتیبان معتبر نیست؛ هیچ اطلاعاتی جایگزین نشد.',
  BACKUP_INVALID_FILE: 'فایل انتخاب‌شده پشتیبان معتبر مانو نیست؛ هیچ اطلاعاتی جایگزین نشد.',
  BACKUP_UNSUPPORTED: 'نسخه یا روش رمزگذاری این فایل پشتیبانی نمی‌شود؛ هیچ اطلاعاتی جایگزین نشد.',
  BACKUP_TOO_LARGE: 'حجم اطلاعات از حد مجاز پشتیبان بیشتر است؛ هیچ اطلاعاتی حذف یا کوتاه نشد.',
  BACKUP_PASSWORD_REQUIRED: 'رمز پشتیبان باید بین ۱۲ تا ۲۵۶ نویسه باشد و فقط فاصله نباشد.',
  BACKUP_DECRYPT_FAILED: 'رمز درست نیست یا فایل آسیب دیده است؛ هیچ اطلاعاتی جایگزین نشد.',
  BACKUP_CRYPTO_UNAVAILABLE: 'رمزگذاری در این مرورگر در دسترس نیست؛ از مرورگر به‌روز و اتصال امن استفاده کنید.',
};

export class LocalBackupError extends Error {
  readonly code: LocalBackupErrorCode;
  constructor(code: LocalBackupErrorCode) {
    super(ERROR_MESSAGES[code]);
    this.name = 'LocalBackupError';
    this.code = code;
  }
}

interface ValidationContext { nowMs: number; nodes: number }
type Parser<T> = (input: unknown, context: ValidationContext) => T;
type Shape<T> = { [Key in keyof T]-?: Parser<T[Key]> };

function invalid(): never { throw new LocalBackupError('BACKUP_INVALID_DATA'); }

function bump(context: ValidationContext) {
  context.nodes += 1;
  if (context.nodes > MAX_NODES) throw new LocalBackupError('BACKUP_TOO_LARGE');
}

function plainObject(input: unknown): Record<string, unknown> {
  if (typeof input !== 'object' || input === null || Array.isArray(input)) return invalid();
  const prototype = Object.getPrototypeOf(input);
  if (prototype !== Object.prototype && prototype !== null) return invalid();
  const descriptors = Object.getOwnPropertyDescriptors(input);
  for (const [key, descriptor] of Object.entries(descriptors)) {
    if (key === '__proto__' || key === 'prototype' || key === 'constructor' || !('value' in descriptor)) return invalid();
  }
  if (Object.getOwnPropertySymbols(input).length) return invalid();
  return input as Record<string, unknown>;
}

function object<T extends object>(shape: Shape<T>): Parser<T> {
  return (input, context) => {
    bump(context);
    const record = plainObject(input);
    for (const key of Object.getOwnPropertyNames(record)) if (!Object.hasOwn(shape, key)) return invalid();
    const result: Record<string, unknown> = {};
    for (const [key, parser] of Object.entries(shape) as [string, Parser<unknown>][]) {
      const value = parser(Object.hasOwn(record, key) ? record[key] : undefined, context);
      if (value !== undefined) result[key] = value;
    }
    return result as T;
  };
}

function optional<T>(parser: Parser<T>): Parser<T | undefined> {
  return (input, context) => input === undefined ? undefined : parser(input, context);
}

function nullable<T>(parser: Parser<T>): Parser<T | null> {
  return (input, context) => input === null ? null : parser(input, context);
}

function text(maximum = 2_000, minimum = 0): Parser<string> {
  return (input, context) => {
    bump(context);
    if (typeof input !== 'string' || input.length < minimum || input.length > maximum) return invalid();
    return input;
  };
}

function number(minimum: number, maximum: number, integer = false): Parser<number> {
  return (input, context) => {
    bump(context);
    if (typeof input !== 'number' || !Number.isFinite(input) || input < minimum || input > maximum || (integer && !Number.isSafeInteger(input))) return invalid();
    return input;
  };
}

const bool: Parser<boolean> = (input, context) => {
  bump(context);
  if (typeof input !== 'boolean') return invalid();
  return input;
};

function enumeration<const T extends string>(values: readonly T[]): Parser<T> {
  return (input, context) => {
    bump(context);
    if (typeof input !== 'string' || !values.includes(input as T)) return invalid();
    return input as T;
  };
}

function list<T>(parser: Parser<T>, maximum = 1_000): Parser<T[]> {
  return (input, context) => {
    bump(context);
    if (!Array.isArray(input) || Object.getPrototypeOf(input) !== Array.prototype) return invalid();
    if (input.length > maximum) throw new LocalBackupError('BACKUP_TOO_LARGE');
    if (Object.getOwnPropertySymbols(input).length) return invalid();
    for (const [key, descriptor] of Object.entries(Object.getOwnPropertyDescriptors(input))) {
      if (!('value' in descriptor) || (key !== 'length' && !/^(0|[1-9]\d*)$/.test(key))) return invalid();
    }
    const result: T[] = [];
    for (let index = 0; index < input.length; index += 1) {
      if (!Object.hasOwn(input, index)) return invalid();
      result.push(parser(input[index], context));
    }
    return result;
  };
}

function records<T extends { id: string }>(parser: Parser<T>, maximum = 1_000): Parser<T[]> {
  const parse = list(parser, maximum);
  return (input, context) => {
    const result = parse(input, context);
    if (new Set(result.map((item) => item.id)).size !== result.length) return invalid();
    return result;
  };
}

const id = text(128, 1);
const ageBand = enumeration<AgeBandId>(['NEWBORN', 'INFANT_3_12M', 'TODDLER_1_3Y', 'PRESCHOOL_3_6Y', 'SCHOOL_6_12Y']);
const pastTimestamp: Parser<number> = (input, context) => number(1, context.nowMs, true)(input, context);
const scheduleTimestamp: Parser<number> = (input, context) => number(1, context.nowMs + FUTURE_SCHEDULE_MS, true)(input, context);

const childProfile = object<ChildProfile>({
  id,
  aliasFa: text(256),
  ageBand,
  ageMonthsExact: optional(nullable(number(0, 144))),
  sex: optional(nullable(enumeration(['boy', 'girl']))),
  birthDateOrAgeFa: text(256),
  birthDateEpochMs: optional(nullable(pastTimestamp)),
  weightKgText: text(64),
  allergiesFa: text(4_000),
  updatedAtLabelFa: text(256),
});

const temperature = object<TimelineTempPoint>({
  id,
  tempCelsius: number(34, 43),
  timestampMs: pastTimestamp,
  recordedAtMs: optional(pastTimestamp),
  timeLabelFa: text(256),
  siteFa: text(256, 1),
  caregiverFa: text(256),
  noteFa: text(4_000),
  isSyncedCloud: optional(bool),
});

const dose = object<TimelineDosePoint>({
  id,
  medicationSlug: text(256, 1),
  medicationNameFa: text(500, 1),
  dosageFormFa: text(500, 1),
  volumeMlText: text(128, 1),
  timestampMs: pastTimestamp,
  recordedAtMs: optional(pastTimestamp),
  givenAtLabelFa: text(256),
  noteFa: text(4_000),
  isSyncedCloud: optional(bool),
});

const growth = object<GrowthMeasurementEntry>({
  id,
  metricId: enumeration(['weight_for_age', 'height_for_age']),
  sex: enumeration(['boy', 'girl']),
  ageMonths: number(0, 60),
  measuredValue: number(1, 140),
  zScore: number(-100, 100),
  percentile: number(0, 100),
  bandLabelFa: text(500),
  recordedAtLabelFa: text(256),
});

const vaccine = object<VaccineRecord>({
  vaccineId: id,
  administeredDateEpochMs: optional(nullable(pastTimestamp)),
  customDueDateEpochMs: optional(nullable(scheduleTimestamp)),
  isDeferred: optional(bool),
  facilityNameFa: optional(text(500)),
  lotNumber: optional(text(256)),
  noteFa: optional(text(4_000)),
  updatedAtMs: pastTimestamp,
});

const vaccineRecords: Parser<Record<string, VaccineRecord>> = (input, context) => {
  bump(context);
  const source = plainObject(input);
  if (Object.keys(source).length > 500) return invalid();
  const result: Record<string, VaccineRecord> = {};
  for (const key of Object.keys(source)) {
    id(key, context);
    const entry = vaccine(source[key], context);
    if (entry.vaccineId !== key) return invalid();
    result[key] = entry;
  }
  return result;
};

const completedSlots: Parser<Record<number, string>> = (input, context) => {
  bump(context);
  const source = plainObject(input);
  const result: Record<number, string> = {};
  if (Object.keys(source).length > 6) return invalid();
  for (const key of Object.keys(source)) {
    if (!/^[1-6]$/.test(key)) return invalid();
    result[Number(key)] = text(128, 1)(source[key], context);
  }
  return result;
};

const medication = object<TreatmentMedicationItem>({
  id,
  nameWithStrengthFa: text(500, 1),
  form: enumeration(['SYRUP_SUSPENSION', 'DROPS', 'NASAL_SPRAY', 'OINTMENT_CREAM', 'TABLET_SACHET']),
  doseAmountFa: text(500, 1),
  intervalHours: nullable(number(0.01, 8_760)),
  scheduleLabelFa: text(1_000, 1),
  foodRelationFa: text(1_000),
  counselingNoteFa: text(4_000),
  isWarningHighlight: bool,
  dailyDoseSlots: number(1, 6, true),
  completedSlotTimesFa: completedSlots,
});

const dayKey: Parser<string> = (input, context) => {
  const value = text(10, 10)(input, context);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return invalid();
  const parsed = new Date(`${value}T00:00:00Z`);
  if (!Number.isFinite(parsed.getTime()) || parsed.toISOString().slice(0, 10) !== value) return invalid();
  return value;
};

const treatmentPlan = object<PediatricTreatmentPlan>({
  pharmacyNameFa: text(500),
  pharmacistBadgeFa: text(500),
  fallbackChildSummaryFa: text(1_000),
  prescriptionDateFa: text(1_000),
  storageInstructionFa: text(4_000),
  smartRemindersEnabled: bool,
  lastCheckoffDayKey: dayKey,
  medications: records(medication, 250),
  // Archive entries may contain several edited revisions of the same legacy ID.
  legacyUnreviewedMedications: optional(list(medication, 500)),
});

const visitDraft = object<SavedVisitDraft>({
  id,
  titleFa: text(500),
  draftTextFa: text(50_000),
  createdAtLabelFa: text(256),
});

const meal = object<NutritionMealItem>({
  mealTypeFa: text(500),
  titleFa: text(500),
  whySuitableFa: text(4_000),
  textureAndPortionFa: text(4_000),
  ingredientsFa: list(text(2_000), 100),
  prepStepsFa: list(text(4_000), 100),
  safetyAvoidListFa: list(text(4_000), 100),
  doctorFollowUpHintFa: text(4_000),
});

const nutrition = object<NutritionPlan>({
  id,
  planTitleFa: text(500),
  overviewFa: text(8_000),
  growthNutritionFocusFa: text(4_000),
  hydrationTipFa: text(4_000),
  meals: list(meal, 24),
  ageMonthsSnapshot: number(0, 144),
  goalSnapshotFa: text(2_000),
  createdAtLabelFa: text(256),
});

const syrupScan = object<SavedSyrupScan>({
  id,
  detectedDrugNameFa: text(500),
  ingredientKey: enumeration(['ACETAMINOPHEN', 'IBUPROFEN', 'CETIRIZINE', 'UNKNOWN']),
  dosageForm: enumeration(['DROPS', 'SYRUP', 'SUSPENSION', 'UNKNOWN']),
  mgAmount: number(0, 100_000),
  perMlAmount: number(0, 10_000),
  matchedFormulationIdRaw: text(256),
  confidence: enumeration(['HIGH', 'MEDIUM', 'LOW']),
  safetyNoteFa: text(4_000),
  parentVerified: bool,
  createdAtMs: pastTimestamp,
  createdAtLabelFa: text(256),
});

const consultation = object<PharmacistConsultation>({
  id,
  createdAtMs: pastTimestamp,
  createdAtLabelFa: text(256),
  childAgeBand: ageBand,
  weightKgText: text(64),
  topicFa: text(500),
  questionTextFa: text(8_000),
  symptomsSelectedFa: list(text(1_000), 100),
  urgencyLevel: enumeration(['EMERGENCY_BYPASS', 'NEEDS_PHYSICIAN_TODAY', 'ROUTINE_PHARMACIST']),
  statusFa: text(500),
  pharmacistReplyFa: optional(text(20_000)),
  repliedAtLabelFa: optional(text(256)),
});

const safeUrl: Parser<string> = (input, context) => {
  const value = text(2_000, 1)(input, context);
  let parsed: URL;
  try { parsed = new URL(value); } catch { return invalid(); }
  if (parsed.protocol !== 'https:' || parsed.username || parsed.password) return invalid();
  return value;
};

const careCard = object<CareCardEntry>({
  id,
  titleFa: text(500),
  subtitleFa: text(2_000),
  cardType: enumeration(['PHARMACY_INVITE', 'CAREGIVER_HANDOFF', 'CLINIC_SUMMARY', 'PHARMACY_PROTOCOL']),
  badgeFa: text(500),
  summaryLinesFa: list(text(4_000), 100),
  footerNoteFa: text(4_000),
  createdAtLabelFa: text(256),
  shareUrl: optional(safeUrl),
});

const snapshot = object<LocalBackupSnapshot>({
  theme: enumeration(['light', 'dark']),
  homeViewStyle: enumeration(['calm', 'orbital']),
  onboardingSeen: bool,
  childProfile,
  feverLogs: records(temperature, 5_000),
  doseLogs: records(dose, 5_000),
  growthMeasurements: records(growth, 5_000),
  checkedMilestones: list(id, 1_000),
  vaccineBirthDateEpochMs: nullable(pastTimestamp),
  vaccineRecords,
  vaccineReminders: object<VaccineReminderSettings>({ enabled: bool, leadDaysBefore: number(0, 30, true) }),
  checkedMedSafetyIds: list(id, 1_000),
  checkedHygieneIds: list(id, 1_000),
  bookmarkedSlugs: list(text(256, 1), 1_000),
  treatmentPlan,
  savedVisitDrafts: records(visitDraft, 500),
  nutritionPlans: records(nutrition, 500),
  savedSyrupScans: records(syrupScan, 500),
  pharmacistConsultations: records(consultation, 1_000),
  careCards: records(careCard, 1_000),
});

/** Strict JSON allowlist. Invalid values fail the whole backup; no records are truncated. */
export function validateLocalBackupSnapshot(input: unknown, nowMs = Date.now()): LocalBackupSnapshot {
  if (!Number.isSafeInteger(nowMs) || nowMs <= 0) return invalid();
  const result = snapshot(input, { nowMs, nodes: 0 });
  if (encoder.encode(JSON.stringify(result)).byteLength > MAX_LOCAL_BACKUP_PLAINTEXT_BYTES) {
    throw new LocalBackupError('BACKUP_TOO_LARGE');
  }
  return result;
}

/**
 * Encryption protects confidentiality/integrity with the passphrase, not a clinical
 * signature. A restored copy cannot certify a bottle, cloud sync, or a scheduler.
 * Saved entered text and historical records, including the legacy archive, stay intact.
 */
export function prepareLocalBackupForRestore(input: unknown, nowMs = Date.now()): LocalBackupSnapshot {
  const result = validateLocalBackupSnapshot(input, nowMs);
  result.feverLogs = result.feverLogs.map((entry) => ({ ...entry, isSyncedCloud: false }));
  result.doseLogs = result.doseLogs.map((entry) => ({ ...entry, isSyncedCloud: false }));
  result.savedSyrupScans = result.savedSyrupScans.map((entry) => ({ ...entry, parentVerified: false }));
  result.vaccineReminders.enabled = false;
  result.treatmentPlan.smartRemindersEnabled = false;
  return result;
}

interface BackupEncryption {
  algorithm: 'AES-GCM';
  kdf: 'PBKDF2-SHA256';
  iterations: number;
  salt: string;
  iv: string;
}
interface BackupEnvelope {
  format: typeof LOCAL_BACKUP_FORMAT;
  version: typeof LOCAL_BACKUP_VERSION;
  encryption: BackupEncryption;
  ciphertext: string;
}

function webCrypto(): Crypto {
  if (!globalThis.crypto?.subtle || !globalThis.crypto.getRandomValues) {
    throw new LocalBackupError('BACKUP_CRYPTO_UNAVAILABLE');
  }
  return globalThis.crypto;
}

function checkPassphrase(passphrase: string): void {
  if (typeof passphrase !== 'string' || passphrase.length > MAX_BACKUP_PASSPHRASE_LENGTH * 2 ||
      Array.from(passphrase).length < MIN_BACKUP_PASSPHRASE_LENGTH ||
      Array.from(passphrase).length > MAX_BACKUP_PASSPHRASE_LENGTH || !passphrase.trim() || /[\uD800-\uDFFF]/u.test(passphrase)) {
    throw new LocalBackupError('BACKUP_PASSWORD_REQUIRED');
  }
}

function base64(bytes: Uint8Array): string {
  const chunks: string[] = [];
  for (let index = 0; index < bytes.length; index += 32_768) {
    chunks.push(String.fromCharCode(...bytes.subarray(index, index + 32_768)));
  }
  return btoa(chunks.join(''));
}

function bytesFromBase64(value: string, minimumBytes: number, maximumBytes: number): Uint8Array<ArrayBuffer> {
  if (!value.length || value.length > Math.ceil(maximumBytes / 3) * 4 || value.length % 4 !== 0 || !/^[A-Za-z0-9+/]*={0,2}$/.test(value)) {
    throw new LocalBackupError('BACKUP_INVALID_FILE');
  }
  let binary: string;
  try { binary = atob(value); } catch { throw new LocalBackupError('BACKUP_INVALID_FILE'); }
  if (binary.length < minimumBytes || binary.length > maximumBytes || btoa(binary) !== value) {
    throw new LocalBackupError('BACKUP_INVALID_FILE');
  }
  return Uint8Array.from(binary, (character) => character.charCodeAt(0));
}

function authenticatedHeader(encryption: BackupEncryption): Uint8Array<ArrayBuffer> {
  // Fixed ordering also makes parsing whitespace/order-independent without
  // allowing a different version or cryptographic parameter to escape the tag.
  return encoder.encode(JSON.stringify({
    format: LOCAL_BACKUP_FORMAT,
    version: LOCAL_BACKUP_VERSION,
    encryption: {
      algorithm: encryption.algorithm,
      kdf: encryption.kdf,
      iterations: encryption.iterations,
      salt: encryption.salt,
      iv: encryption.iv,
    },
  }));
}

async function deriveKey(crypto: Crypto, passphrase: string, salt: Uint8Array<ArrayBuffer>, usage: 'encrypt' | 'decrypt'): Promise<CryptoKey> {
  const passwordBytes = encoder.encode(passphrase);
  try {
    const material = await crypto.subtle.importKey('raw', passwordBytes, 'PBKDF2', false, ['deriveKey']);
    return await crypto.subtle.deriveKey(
      { name: 'PBKDF2', hash: 'SHA-256', iterations: KDF_ITERATIONS, salt },
      material,
      { name: 'AES-GCM', length: 256 },
      false,
      [usage],
    );
  } finally {
    passwordBytes.fill(0);
  }
}

export async function createEncryptedLocalBackup(input: unknown, passphrase: string): Promise<string> {
  checkPassphrase(passphrase);
  const data = validateLocalBackupSnapshot(input);
  const plaintext = encoder.encode(JSON.stringify(data));
  const crypto = webCrypto();
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const encryption: BackupEncryption = {
    algorithm: 'AES-GCM', kdf: 'PBKDF2-SHA256', iterations: KDF_ITERATIONS,
    salt: base64(salt), iv: base64(iv),
  };
  try {
    const key = await deriveKey(crypto, passphrase, salt, 'encrypt');
    const ciphertext = await crypto.subtle.encrypt({ name: 'AES-GCM', iv, tagLength: 128, additionalData: authenticatedHeader(encryption) }, key, plaintext);
    const envelope: BackupEnvelope = {
      format: LOCAL_BACKUP_FORMAT, version: LOCAL_BACKUP_VERSION,
      encryption, ciphertext: base64(new Uint8Array(ciphertext)),
    };
    return JSON.stringify(envelope);
  } finally {
    plaintext.fill(0);
  }
}

function parseEnvelope(fileText: string): BackupEnvelope {
  if (typeof fileText !== 'string') throw new LocalBackupError('BACKUP_INVALID_FILE');
  if (fileText.length > MAX_LOCAL_BACKUP_FILE_BYTES || encoder.encode(fileText).byteLength > MAX_LOCAL_BACKUP_FILE_BYTES) {
    throw new LocalBackupError('BACKUP_TOO_LARGE');
  }
  let input: unknown;
  try { input = JSON.parse(fileText); } catch { throw new LocalBackupError('BACKUP_INVALID_FILE'); }
  const context: ValidationContext = { nowMs: Date.now(), nodes: 0 };
  let record: Record<string, unknown>;
  try { record = plainObject(input); } catch { throw new LocalBackupError('BACKUP_INVALID_FILE'); }
  if (record.format !== LOCAL_BACKUP_FORMAT) throw new LocalBackupError('BACKUP_INVALID_FILE');
  if (record.version !== LOCAL_BACKUP_VERSION) throw new LocalBackupError('BACKUP_UNSUPPORTED');
  let encryption: Record<string, unknown>;
  try { encryption = plainObject(record.encryption); } catch { throw new LocalBackupError('BACKUP_INVALID_FILE'); }
  if (encryption.algorithm !== 'AES-GCM' || encryption.kdf !== 'PBKDF2-SHA256' || encryption.iterations !== KDF_ITERATIONS) {
    throw new LocalBackupError('BACKUP_UNSUPPORTED');
  }
  try {
    return object<BackupEnvelope>({
      format: enumeration([LOCAL_BACKUP_FORMAT]),
      version: (value) => value === LOCAL_BACKUP_VERSION ? LOCAL_BACKUP_VERSION : invalid(),
      encryption: object<BackupEncryption>({
        algorithm: enumeration(['AES-GCM']),
        kdf: enumeration(['PBKDF2-SHA256']),
        iterations: number(KDF_ITERATIONS, KDF_ITERATIONS, true),
        salt: text(24, 24),
        iv: text(16, 16),
      }),
      ciphertext: text(Math.ceil((MAX_LOCAL_BACKUP_PLAINTEXT_BYTES + 16) / 3) * 4, 24),
    })(record, context);
  } catch {
    throw new LocalBackupError('BACKUP_INVALID_FILE');
  }
}

export async function openEncryptedLocalBackup(fileText: string, passphrase: string): Promise<LocalBackupSnapshot> {
  const envelope = parseEnvelope(fileText);
  checkPassphrase(passphrase);
  const crypto = webCrypto();
  const salt = bytesFromBase64(envelope.encryption.salt, 16, 16);
  const iv = bytesFromBase64(envelope.encryption.iv, 12, 12);
  const ciphertext = bytesFromBase64(envelope.ciphertext, 16, MAX_LOCAL_BACKUP_PLAINTEXT_BYTES + 16);
  let decrypted: ArrayBuffer;
  try {
    const key = await deriveKey(crypto, passphrase, salt, 'decrypt');
    decrypted = await crypto.subtle.decrypt({ name: 'AES-GCM', iv, tagLength: 128, additionalData: authenticatedHeader(envelope.encryption) }, key, ciphertext);
  } catch {
    throw new LocalBackupError('BACKUP_DECRYPT_FAILED');
  }
  const bytes = new Uint8Array(decrypted);
  try {
    if (bytes.byteLength > MAX_LOCAL_BACKUP_PLAINTEXT_BYTES) throw new LocalBackupError('BACKUP_TOO_LARGE');
    let parsed: unknown;
    try { parsed = JSON.parse(new TextDecoder('utf-8', { fatal: true }).decode(bytes)); } catch { throw new LocalBackupError('BACKUP_INVALID_DATA'); }
    return prepareLocalBackupForRestore(parsed);
  } finally {
    bytes.fill(0);
  }
}
