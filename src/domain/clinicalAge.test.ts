import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { resolveClinicalAge, ageBandFromMonths, shouldShowFeverAlert } from './clinicalAge.ts';
import { initialAgeMonthsFromProfile, evaluateLiquidDose, LIQUID_FORMULATIONS, formatAgeFa } from './liquidDoseVisualizerEngine.ts';
import { evaluateTriage } from './triageEngine.ts';
import { SYMPTOM_FLOWS } from '../data/contentRepository.ts';
import { buildTimelineSnapshot, evaluateHighFeverAlert } from './feverDoseTimelineEngine.ts';
import type { ChildProfile } from './models.ts';

const NOW = new Date(2026, 10, 27, 12).getTime();
const profile = (patch: Partial<ChildProfile> = {}): ChildProfile => ({
  id: 'child', aliasFa: 'کودک', ageBand: 'TODDLER_1_3Y', ageMonthsExact: null,
  birthDateEpochMs: null, birthDateOrAgeFa: '', weightKgText: '5', allergiesFa: '', updatedAtLabelFa: '', ...patch,
});

describe('Canonical clinical age', () => {
  it('preserves fractional age below three months across dosing, triage, and fever alerts', () => {
    const child = profile({ ageMonthsExact: 2.7 });
    const age = resolveClinicalAge(child, null, NOW);
    assert.equal(age.ageMonths, 2.7);
    assert.equal(initialAgeMonthsFromProfile(child, null, NOW), 2.7);
    assert.equal(ageBandFromMonths(age.ageMonths), 'NEWBORN');
    assert.equal(formatAgeFa(2.7), '2.7 ماه');
    assert.equal(evaluateLiquidDose(LIQUID_FORMULATIONS.ACETAMINOPHEN_SYRUP_120_5, age.ageMonths, 5).isBlocked, true);
    assert.equal(evaluateTriage({ flow: SYMPTOM_FLOWS.find((flow) => flow.id === 'fever')!, ageBand: child.ageBand, ageMonthsExact: age.ageMonths, temperatureCelsius: 38.1, durationHours: 2, selectedRedFlagIds: new Set() }).outcome, 'URGENT_EMERGENCY');
    assert.equal(shouldShowFeverAlert(38.1, age), true);
    const snapshot = buildTimelineSnapshot([{ id: 'temp', timestampMs: NOW, tempCelsius: 38.1, timeLabelFa: '12:00', siteFa: '', caregiverFa: '', noteFa: '' }], [], NOW);
    assert.match(evaluateHighFeverAlert(snapshot, child)!.titleFa, /زیر ۳ ماه/);
  });

  it('fails closed on DOB/exact age and competing DOB conflicts instead of selecting an older source', () => {
    const birth = new Date(2025, 4, 1).getTime();
    const child = profile({ birthDateEpochMs: birth, ageMonthsExact: 2 });
    assert.equal(resolveClinicalAge(child, birth, NOW).status, 'conflict');
    assert.equal(initialAgeMonthsFromProfile(child, birth, NOW), null);
    assert.equal(resolveClinicalAge(profile({ birthDateEpochMs: birth }), new Date(2025, 4, 2).getTime(), NOW).status, 'conflict');
    const removedDob = profile({ ageMonthsExact: 2.7, birthDateEpochMs: null });
    assert.deepEqual(resolveClinicalAge(removedDob, null, NOW), { status: 'known', ageMonths: 2.7, source: 'exact-age', messageFa: null });
  });

  it('supports one canonical DOB source and rejects invalid saved ages and dates', () => {
    const birth = new Date(2026, 8, 27).getTime();
    const age = resolveClinicalAge(profile({ birthDateEpochMs: birth }), birth, NOW);
    assert.equal(age.source, 'birth-date');
    assert.equal(age.ageMonths, 2);
    for (const badAge of [-1, 145, Number.NaN, Number.POSITIVE_INFINITY]) assert.equal(resolveClinicalAge(profile({ ageMonthsExact: badAge }), null, NOW).status, 'invalid');
    assert.equal(resolveClinicalAge(profile({ birthDateEpochMs: NOW + 86_400_000 }), null, NOW).status, 'invalid');
    assert.equal(resolveClinicalAge(profile(), null, NOW).status, 'unknown');
    assert.equal(shouldShowFeverAlert(38, resolveClinicalAge(profile(), null, NOW)), true);
    assert.equal(shouldShowFeverAlert(37.9, age), false);
    assert.equal(shouldShowFeverAlert(38, resolveClinicalAge(profile({ ageMonthsExact: 18 }), null, NOW)), false);
  });

  it('keeps the under-three-month boundary on the same local calendar as vaccination dates in Tehran', () => {
    const previousTimezone = process.env.TZ;
    try {
      process.env.TZ = 'Asia/Tehran';
      const birth = new Date(2026, 7, 28, 0).getTime();
      const now = new Date(2026, 10, 27, 12).getTime();
      const profile = { birthDateEpochMs: birth, ageMonthsExact: null, birthDateOrAgeFa: '' };
      assert.equal(resolveClinicalAge(profile, birth, now).ageMonths, 2);
      assert.equal(resolveClinicalAge(profile, birth, new Date(2026,10,28,0).getTime()).ageMonths, 3);
      assert.equal(resolveClinicalAge(profile, new Date(2026,7,28,23).getTime(), now).status, 'known');
    } finally {
      if (previousTimezone === undefined) delete process.env.TZ;
      else process.env.TZ = previousTimezone;
    }
  });
});
