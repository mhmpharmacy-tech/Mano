export type AlertSeverity = 'danger' | 'caution' | 'success' | 'info';

export type AgeBandId = 'NEWBORN' | 'INFANT_3_12M' | 'TODDLER_1_3Y' | 'PRESCHOOL_3_6Y' | 'SCHOOL_6_12Y';

export interface AgeBand {
  id: AgeBandId;
  labelFa: string;
  rangeMonthsText: string;
}

export const AGE_BANDS: AgeBand[] = [
  { id: 'NEWBORN', labelFa: 'نوزاد و زیر ۳ ماه', rangeMonthsText: '۰ تا ۳ ماه' },
  { id: 'INFANT_3_12M', labelFa: 'شیرخوار ۳ تا ۱۲ ماه', rangeMonthsText: '۳ تا ۱۲ ماه' },
  { id: 'TODDLER_1_3Y', labelFa: 'نوپا ۱ تا ۳ سال', rangeMonthsText: '۱۲ تا ۳۶ ماه' },
  { id: 'PRESCHOOL_3_6Y', labelFa: 'پیش‌دبستانی ۳ تا ۶ سال', rangeMonthsText: '۳ تا ۶ سال' },
  { id: 'SCHOOL_6_12Y', labelFa: 'کودک ۶ تا ۱۲ سال', rangeMonthsText: '۶ تا ۱۲ سال' },
];

export type ReviewStatus = 'VERIFIED' | 'NEEDS_CLINICAL_REVIEW';

export interface EvidenceSource {
  id: string;
  titleFa: string;
  organization: string;
  year: string;
  categoryFa: string;
  summaryFa: string;
  url?: string;
}

export interface RedFlagItem {
  id: string;
  labelFa: string;
  detailFa: string;
  isImmediateEmergency: boolean;
}

export interface SymptomFlow {
  id: string;
  titleFa: string;
  shortDescFa: string;
  iconKey: string;
  heroImageKey: string;
  asksTemperature?: boolean;
  redFlags: RedFlagItem[];
  educationalStepsFa: string[];
  escalationCriteriaFa: string[];
  avoidActionsFa: string[];
  relatedArticleSlug: string;
  relatedMedicationSlugs: string[];
  sourceIds: string[];
  reviewStatus: ReviewStatus;
}

export type TriageOutcome = 'URGENT_EMERGENCY' | 'NEEDS_TIMELY_REVIEW' | 'GENERAL_EDUCATIONAL';

export interface TriageRule {
  id: string;
  descriptionFa: string;
  outcome: TriageOutcome;
  reviewStatus: ReviewStatus;
}

export interface TriageEvaluationResult {
  outcome: TriageOutcome;
  titleFa: string;
  summaryFa: string;
  immediateActionsFa: string[];
  escalationCriteriaFa: string[];
  relatedArticleSlug: string;
  canShowSelfCare: boolean;
  triggeredRule: TriageRule;
  showPharmacistOption: boolean;
}

export interface MedicationMonograph {
  slug: string;
  nameFa: string;
  genericNameEn: string;
  categoryFa: string;
  shortSummaryFa: string;
  aliasesSearchTermsFa: string[];
  storageAndShelfLifeFa: string;
  treatsFa: string;
  dosageFormsFa: string[];
  dosageFormAlertFa: string;
  keyPrecautionsFa: string[];
  commonSideEffectsFa: string[];
  measurementGuidanceFa: string;
  avoidWithFa: string[];
  whatNotToGiveFa: string[];
  refusalTipsFa: string[];
  whenToContactClinicianFa: string[];
  ageMinimumNoteFa: string;
  sourceIds: string[];
  isCriticalSafetyFocus?: boolean;
}

export interface ArticleSection {
  headingFa: string;
  bodyFa: string;
  bulletsFa?: string[];
  warningBoxFa?: string;
}

export interface Article {
  slug: string;
  titleFa: string;
  subtitleFa: string;
  categoryFa: string;
  readingTimeMin: number;
  ageBands: AgeBandId[];
  ageRelevanceFa: string;
  keyTakeawaysFa: string[];
  caregiverMistakesFa: string[];
  sections: ArticleSection[];
  redFlagsBoxFa: string[];
  relatedSlugs: string[];
  nextStepCtaLabelFa?: string;
  nextStepRouteKey?: string;
  sourceIds: string[];
  reviewStatus: ReviewStatus;
  lastReviewedFa: string;
  isHighRisk?: boolean;
}

export interface FaqItem {
  id: string;
  categoryFa: string;
  questionFa: string;
  shortAnswerFa: string;
  whatToDoFa: string[];
  clinicalDetailFa: string;
  whenToSeeDoctorFa: string[];
  ageRelevanceFa: string;
  ageBands: AgeBandId[];
  sourceIds: string[];
  keywords: string[];
  isHighRisk?: boolean;
}

export interface SupplementOrHygieneGuide {
  id: string;
  tab: 'supplements' | 'hygiene';
  shortTitleFa: string;
  titleFa: string;
  categoryFa: string;
  ageGroupFa: string;
  ageFilters: ('0_6m' | '6_24m' | '2_6y')[];
  accentRole: 'gold' | 'coral' | 'teal' | 'sky' | 'plum' | 'sage';
  summaryFa: string;
  howToUseTitleFa: string;
  howToUseFa: string[];
  safetyChecksFa: string[];
  avoidOrCautionFa: string[];
  pharmacistNoteFa: string;
  sourceIds: string[];
  sourceLabelFa: string;
}

export interface HomeDeviceGuide {
  id: string;
  titleFa: string;
  subtitleFa: string;
  distinctionNoteFa: string;
  checklistFa: string[];
  commonMistakeFa: string;
  cleaningStorageFa: string;
  relatedArticleSlug: string;
  sourceIds: string[];
}

export interface DevelopmentalMilestone {
  id: string;
  ageBand: AgeBandId;
  ageRangeLabelFa: string;
  domainLabelFa: string;
  minMonths: number;
  maxMonths: number;
  motorSkillsFa: string[];
  languageSocialFa: string[];
  whenToDiscussFa: string;
}

export interface PharmacistCaseDemo {
  id: string;
  aliasFa: string;
  categoryFa: string;
  ageLabelFa: string;
  riskFlagFa: string;
  statusFa: string;
  createdDateFa: string;
  parentNoteFa: string;
  suggestedReplyFa: string;
  followUpScheduleFa: string;
  referenceTitle: string;
}

export interface ChildProfile {
  id: string;
  aliasFa: string;
  ageBand: AgeBandId;
  ageMonthsExact?: number | null;
  sex?: 'boy' | 'girl' | null;
  birthDateOrAgeFa: string;
  birthDateEpochMs?: number | null;
  weightKgText: string;
  allergiesFa: string;
  updatedAtLabelFa: string;
}

export interface TimelineTempPoint {
  id: string;
  tempCelsius: number;
  timestampMs: number;
  recordedAtMs?: number;
  timeLabelFa: string;
  siteFa: string;
  caregiverFa: string;
  noteFa: string;
  isSyncedCloud?: boolean;
}

export interface TimelineDosePoint {
  id: string;
  medicationSlug: string;
  medicationNameFa: string;
  dosageFormFa: string;
  volumeMlText: string;
  timestampMs: number;
  recordedAtMs?: number;
  givenAtLabelFa: string;
  noteFa: string;
  isSyncedCloud?: boolean;
}

export type ConsultationUrgencyLevel = 'EMERGENCY_BYPASS' | 'NEEDS_PHYSICIAN_TODAY' | 'ROUTINE_PHARMACIST';

export interface PharmacistConsultation {
  id: string;
  createdAtMs: number;
  createdAtLabelFa: string;
  childAgeBand: AgeBandId;
  weightKgText: string;
  topicFa: string;
  questionTextFa: string;
  symptomsSelectedFa: string[];
  urgencyLevel: ConsultationUrgencyLevel;
  statusFa: string;
  pharmacistReplyFa?: string;
  repliedAtLabelFa?: string;
}

export interface CareCardEntry {
  id: string;
  titleFa: string;
  subtitleFa: string;
  cardType: 'PHARMACY_INVITE' | 'CAREGIVER_HANDOFF' | 'CLINIC_SUMMARY' | 'PHARMACY_PROTOCOL';
  badgeFa: string;
  summaryLinesFa: string[];
  footerNoteFa: string;
  createdAtLabelFa: string;
  shareUrl?: string;
}

export type VaccineStatus = 'ADMINISTERED' | 'OVERDUE' | 'DUE_SOON' | 'UPCOMING' | 'DEFERRED';
export type VaccineFilter = 'ALL' | 'ACTIONABLE' | 'OVERDUE' | 'ADMINISTERED';

export interface VaccineScheduleEntry {
  id: string;
  titleFa: string;
  milestoneLabelFa: string;
  recommendedAgeDays: number;
  vaccinesIncludedFa: string;
  administrationMethodFa: string;
  careNotesFa: string;
  isOptionalSeasonal?: boolean;
}

export interface VaccineRecord {
  vaccineId: string;
  administeredDateEpochMs?: number | null;
  customDueDateEpochMs?: number | null;
  isDeferred?: boolean;
  facilityNameFa?: string;
  lotNumber?: string;
  noteFa?: string;
  updatedAtMs: number;
}

export interface VaccineReminderSettings {
  enabled: boolean;
  leadDaysBefore: number;
}

export interface VaccineResolvedItem {
  definition: VaccineScheduleEntry;
  record?: VaccineRecord;
  effectiveDueDateEpochMs: number | null;
  administeredDateEpochMs: number | null;
  daysUntilDue: number | null;
  status: VaccineStatus;
  dueDateSourceFa: string;
}

export interface VaccineDashboardSummary {
  items: VaccineResolvedItem[];
  totalCoreCount: number;
  administeredCoreCount: number;
  dueSoonCount: number;
  overdueCount: number;
  nextUpcoming: VaccineResolvedItem | null;
  dueForReminder: VaccineResolvedItem[];
}

export type SyrupIngredientKey = 'ACETAMINOPHEN' | 'IBUPROFEN' | 'CETIRIZINE' | 'UNKNOWN';
export type SyrupDosageForm = 'DROPS' | 'SYRUP' | 'SUSPENSION' | 'UNKNOWN';
export type SyrupScanConfidence = 'HIGH' | 'MEDIUM' | 'LOW';

export interface GeminiSyrupScanPayload {
  detectedDrugNameFa: string;
  activeIngredientKey: string;
  dosageForm: string;
  mgAmount: number;
  perMlAmount: number;
  mgPerMl: number;
  matchedFormulationId: string;
  confidenceLevel: string;
  concentrationDisplayFa: string;
  safetyNoteFa: string;
  isPediatricFeverOrAllergyLiquid: boolean;
}

export interface SavedSyrupScan {
  id: string;
  detectedDrugNameFa: string;
  ingredientKey: SyrupIngredientKey;
  dosageForm: SyrupDosageForm;
  mgAmount: number;
  perMlAmount: number;
  matchedFormulationIdRaw: string;
  confidence: SyrupScanConfidence;
  safetyNoteFa: string;
  parentVerified: boolean;
  createdAtMs: number;
  createdAtLabelFa: string;
}

export interface NutritionMealItem {
  mealTypeFa: string;
  titleFa: string;
  whySuitableFa: string;
  textureAndPortionFa: string;
  ingredientsFa: string[];
  prepStepsFa: string[];
  safetyAvoidListFa: string[];
  doctorFollowUpHintFa: string;
}

export interface NutritionPlan {
  id: string;
  planTitleFa: string;
  overviewFa: string;
  growthNutritionFocusFa: string;
  hydrationTipFa: string;
  meals: NutritionMealItem[];
  ageMonthsSnapshot: number;
  goalSnapshotFa: string;
  createdAtLabelFa: string;
}

export interface SafetyChecklistReport {
  checklistTitleFa: string;
  urgencySummaryFa: string;
  immediateEmergencySignsFa: string[];
  sameDayPediatricianVisitSignsFa: string[];
  homeMonitoringChecklistFa: string[];
  questionsForPediatricianFa: string[];
  clinicalSourceCitationFa: string;
}

export interface DocumentedQaResponse {
  directAnswerFa: string;
  actionableStepsFa: string[];
  whenToVisitPediatricianFa: string[];
  citedSourcesFa: string[];
  safetyDisclaimerFa: string;
}

export interface SavedVisitDraft {
  id: string;
  titleFa: string;
  draftTextFa: string;
  createdAtLabelFa: string;
}

export interface GrowthMeasurementEntry {
  id: string;
  metricId: 'weight_for_age' | 'height_for_age';
  sex: 'boy' | 'girl';
  ageMonths: number;
  measuredValue: number;
  zScore: number;
  percentile: number;
  bandLabelFa: string;
  recordedAtLabelFa: string;
}
