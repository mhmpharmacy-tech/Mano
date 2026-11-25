import { describe, it } from 'node:test';
import assert from 'node:assert/strict';

import { parseClinicalDecimal } from './clinicalNumberInput.ts';
import { normalizePersian, scoreMatch, toAsciiDigits, toPersianDigits } from './persianNormalizer.ts';
import { evaluateTriage } from './triageEngine.ts';
import {
  calculateFeverDose,
  evaluatePriorDoseConflict,
  evaluateWeightNotices,
  isFormulationAllowedForAge,
} from './feverDoseCalculatorEngine.ts';
import {
  LIQUID_FORMULATIONS,
  evaluateLiquidDose,
  parseAgeMonthsFromText,
} from './liquidDoseVisualizerEngine.ts';
import { evaluateWhoGrowth, interpolateLms } from './whoGrowthStandardsEngine.ts';
import {
  gregorianToJalali,
  jalaliToGregorian,
  parseFlexibleDateToEpochMs,
  resolveVaccineSchedule,
} from './vaccinationScheduleEngine.ts';
import { evaluateMedicationOverlap } from './medicationOverlapEngine.ts';
import { reconcileEditableSyrup, reconcileFromAiPayload } from './syrupConcentrationMatcher.ts';
import { buildTimelineSnapshot, evaluateHighFeverAlert } from './feverDoseTimelineEngine.ts';
import type { MedicationMonograph, SymptomFlow } from './models.ts';

const SAMPLE_FEVER_FLOW: SymptomFlow = {
  id: 'fever',
  titleFa: 'تب کودک',
  shortDescFa: 'ارزیابی تب',
  iconKey: 'thermostat',
  heroImageKey: 'fever',
  redFlags: [
    {
      id: 'seizure',
      labelFa: 'تشنج یا لرزش غیرطبیعی',
      detailFa: '',
      isImmediateEmergency: true,
    },
    {
      id: 'poor_feeding',
      labelFa: 'کاهش شدید شیر خوردن',
      detailFa: '',
      isImmediateEmergency: false,
    },
  ],
  educationalStepsFa: ['مایعات فراوان بدهید'],
  escalationCriteriaFa: ['تداوم بیش از ۷۲ ساعت'],
  avoidActionsFa: ['پاشویه با الکل ممنوع'],
  relatedArticleSlug: 'fever-guide',
  relatedMedicationSlugs: ['acetaminophen-pediatric'],
  sourceIds: [],
  reviewStatus: 'VERIFIED',
};

describe('PersianNormalizer & ClinicalNumberInput', () => {
  it('normalizes Arabic characters and Persian digits', () => {
    assert.equal(normalizePersian('كودك\u200cتب‌دار ۱۲'), 'کودک تب دار 12');
    assert.equal(toAsciiDigits('۱۲۳٫۴'), '123٫4');
    assert.equal(toPersianDigits('12.5'), '۱۲٫۵');
    assert.equal(parseClinicalDecimal('۱۲٫۵'), 12.5);
    assert.equal(parseClinicalDecimal('abc'), null);
  });

  it('scores Persian fuzzy search properly', () => {
    const score = scoreMatch('تب كودك', ['راهنمای تب کودک', 'توصیه خانگی']);
    assert.ok(score > 40);
  });
});

describe('TriageEngine clinical safety gates', () => {
  it('forces urgent emergency and hides self-care for NEWBORN (<3m) with fever >= 38.0°C', () => {
    const res = evaluateTriage({
      flow: SAMPLE_FEVER_FLOW,
      ageBand: 'NEWBORN',
      temperatureCelsius: 38.2,
      durationHours: 2,
      selectedRedFlagIds: new Set(),
      ageMonthsExact: 1,
    });
    assert.equal(res.outcome, 'URGENT_EMERGENCY');
    assert.equal(res.canShowSelfCare, false);
    assert.equal(res.showPharmacistOption, false);
    assert.equal(res.triggeredRule.id, 'rule_infant_under_3m_fever_38');
  });

  it('bypasses self-care immediately on checked critical red flag', () => {
    const res = evaluateTriage({
      flow: SAMPLE_FEVER_FLOW,
      ageBand: 'TODDLER_1_3Y',
      temperatureCelsius: 38.5,
      durationHours: 12,
      selectedRedFlagIds: new Set(['seizure']),
    });
    assert.equal(res.outcome, 'URGENT_EMERGENCY');
    assert.equal(res.canShowSelfCare, false);
    assert.equal(res.triggeredRule.id, 'rule_critical_red_flag_bypass');
  });

  it('shows self-care and pharmacist option when non-urgent toddler has mild short fever', () => {
    const res = evaluateTriage({
      flow: SAMPLE_FEVER_FLOW,
      ageBand: 'TODDLER_1_3Y',
      temperatureCelsius: 38.4,
      durationHours: 10,
      selectedRedFlagIds: new Set(),
      ageMonthsExact: 24,
    });
    assert.equal(res.outcome, 'GENERAL_EDUCATIONAL');
    assert.equal(res.canShowSelfCare, true);
    assert.equal(res.showPharmacistOption, true);
  });
});

describe('FeverDoseCalculatorEngine & LiquidDoseVisualizerEngine', () => {
  it('blocks home fever dosing under 3 months', () => {
    const res = calculateFeverDose('UNDER_3_MONTHS', LIQUID_FORMULATIONS.ACETAMINOPHEN_DROPS_100, 5.0);
    assert.equal(res.isBlocked, true);
    assert.equal(res.recommendedMl, 0);
  });

  it('locks ibuprofen under 6 months while permitting acetaminophen 3–6m', () => {
    assert.equal(isFormulationAllowedForAge('THREE_TO_SIX_MONTHS', LIQUID_FORMULATIONS.IBUPROFEN_SYRUP_100_5), false);
    assert.equal(isFormulationAllowedForAge('THREE_TO_SIX_MONTHS', LIQUID_FORMULATIONS.ACETAMINOPHEN_DROPS_100), true);
    const ibu = calculateFeverDose('THREE_TO_SIX_MONTHS', LIQUID_FORMULATIONS.IBUPROFEN_SYRUP_100_5, 7.0);
    assert.equal(ibu.isBlocked, true);
  });

  it('calculates safe Acetaminophen 120/5 dose for 11kg toddler over 6 months', () => {
    const res = calculateFeverDose('OVER_6_MONTHS', LIQUID_FORMULATIONS.ACETAMINOPHEN_SYRUP_120_5, 11.0);
    assert.equal(res.isBlocked, false);
    // 11 * 12.5 = 137.5 mg -> /24 = 5.73 mL -> snaps to 5.5 mL (or 6.0 within 110–165mg range)
    assert.ok(res.recommendedMl >= 5.0 && res.recommendedMl <= 6.5);
    assert.equal(res.maxDosesPer24h, 5);
  });

  it('detects prior acetaminophen dose under 4h as hard conflict and high weight drops warning', () => {
    const conflict = evaluatePriorDoseConflict(
      LIQUID_FORMULATIONS.ACETAMINOPHEN_SYRUP_120_5,
      'ACETAMINOPHEN_UNDER_4H',
    );
    assert.equal(conflict.isHardConflict, true);
    const notice = evaluateWeightNotices('OVER_6_MONTHS', 14.0, LIQUID_FORMULATIONS.ACETAMINOPHEN_DROPS_100);
    assert.equal(notice.showDropsToSyrupSuggestion, true);
  });

  it('evaluates Cetirizine age bands and parses Persian age strings', () => {
    const child4y = evaluateLiquidDose(LIQUID_FORMULATIONS.CETIRIZINE_SYRUP_5_5, 48, 16);
    assert.equal(child4y.isBlocked, false);
    assert.equal(child4y.recommendedMl, 2.5);
    const child7y = evaluateLiquidDose(LIQUID_FORMULATIONS.CETIRIZINE_SYRUP_5_5, 84, 24);
    assert.equal(child7y.recommendedMl, 5.0);
    assert.equal(parseAgeMonthsFromText('۲ سال و ۳ ماه'), 27);
  });
});

describe('WhoGrowthStandardsEngine', () => {
  it('computes P50 near WHO newborn boy median (~3.35kg) and handles 24m length/height split', () => {
    const res = evaluateWhoGrowth('weight_for_age', 'boy', 0, 3.3464);
    assert.ok(res);
    assert.ok(Math.abs(res.percentile - 50.0) < 0.5);
    assert.equal(res.needsPediatricReview, false);

    const len23_5 = interpolateLms('height_for_age', 'boy', 23.5);
    const ht24 = interpolateLms('height_for_age', 'boy', 24.0);
    assert.ok(len23_5.m > ht24.m - 0.5); // 24m length table is slightly higher than standing height
  });
});

describe('VaccinationScheduleEngine', () => {
  it('round-trips Jalali and Gregorian dates and computes overdue/due status', () => {
    const [gy, gm, gd] = jalaliToGregorian(1404, 2, 15);
    const [jy, jm, jd] = gregorianToJalali(gy, gm, gd);
    assert.deepEqual([jy, jm, jd], [1404, 2, 15]);

    const birthMs = parseFlexibleDateToEpochMs('1404/01/01');
    assert.ok(birthMs !== null);
    const sixtyFiveDaysLater = birthMs + 65 * 86_400_000;
    const summary = resolveVaccineSchedule([], birthMs, { enabled: true, leadDaysBefore: 3 }, sixtyFiveDaysLater);
    const twoMonth = summary.items.find((i) => i.definition.id === '2m_pentavalent_opv_rota');
    assert.equal(twoMonth?.status, 'OVERDUE');
  });
});

describe('MedicationOverlapEngine & SyrupConcentrationMatcher', () => {
  it('flags hidden acetaminophen overlap in cold syrups as danger', () => {
    const m1 = { slug: 'acetaminophen-pediatric', nameFa: 'استامینوفن' } as MedicationMonograph;
    const m2 = { slug: 'otc-cough-cold-caution', nameFa: 'شربت سرماخوردگی' } as MedicationMonograph;
    const overlap = evaluateMedicationOverlap(m1, m2);
    assert.equal(overlap.severity, 'danger');
  });

  it('rejects auto-fill when OCR mg/mL disagrees with mgAmount/perMlAmount', () => {
    const rec = reconcileFromAiPayload({
      detectedDrugNameFa: 'شربت استامینوفن',
      activeIngredientKey: 'ACETAMINOPHEN',
      dosageForm: 'SYRUP',
      mgAmount: 120,
      perMlAmount: 5,
      mgPerMl: 100, // inconsistent!
      matchedFormulationId: 'acetaminophen_syrup_120_5',
      confidenceLevel: 'HIGH',
      concentrationDisplayFa: '120 mg / 5 mL',
      safetyNoteFa: '',
      isPediatricFeverOrAllergyLiquid: true,
    });
    assert.equal(rec.confidence, 'LOW');
    assert.equal(rec.canAutoFillSyringe, false);
  });

  it('matches calibrated acetaminophen drops and warns on high concentration', () => {
    const rec = reconcileEditableSyrup({
      drugNameFa: 'قطره استامینوفن',
      ingredientKey: 'ACETAMINOPHEN',
      dosageForm: 'DROPS',
      mgAmount: 100,
      perMlAmount: 1,
      confidence: 'HIGH',
      childProfile: {
        id: 'c1',
        aliasFa: 'آوا',
        ageBand: 'INFANT_3_12M',
        ageMonthsExact: 8,
        birthDateOrAgeFa: '۸ ماه',
        weightKgText: '8.0',
        allergiesFa: '',
        updatedAtLabelFa: 'امروز',
      },
    });
    assert.equal(rec.matchedFormulation?.key, 'ACETAMINOPHEN_DROPS_100');
    assert.equal(rec.warningSeverity, 'danger');
    assert.equal(rec.canAutoFillSyringe, true);
  });
});

describe('FeverDoseTimelineEngine high-fever alerts', () => {
  it('triggers urgent high fever alert at 38.0°C for NEWBORN (<3m)', () => {
    const now = Date.now();
    const snapshot = buildTimelineSnapshot(
      [
        {
          id: 't1',
          tempCelsius: 38.1,
          timestampMs: now - 10 * 60_000,
          timeLabelFa: '14:10',
          siteFa: 'زیربغلی',
          caregiverFa: 'مادر',
          noteFa: '',
        },
      ],
      [],
      now,
    );
    const alert = evaluateHighFeverAlert(snapshot, {
      id: 'nb',
      aliasFa: 'نی‌نی',
      ageBand: 'NEWBORN',
      ageMonthsExact: 2,
      birthDateOrAgeFa: '۲ ماه',
      weightKgText: '4.5',
      allergiesFa: '',
      updatedAtLabelFa: 'امروز',
    });
    assert.ok(alert !== null);
    assert.ok(alert.titleFa.includes('زیر ۳ ماه'));
  });
});
