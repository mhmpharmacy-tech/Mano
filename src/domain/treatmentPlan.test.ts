import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { createElement } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { TreatmentPlanEditorModal } from '../components/TreatmentPlanEditorSheets.tsx';
import {
  buildTreatmentCaregiverShareText,
  createDefaultTreatmentPlan,
  getTreatmentSlotCounts,
  normalizePlanForToday,
  treatmentMedicationEntryError,
  type PediatricTreatmentPlan,
  type TreatmentMedicationItem,
} from './treatmentPlan.ts';

const TODAY = '2026-11-27';

function medication(id: string): TreatmentMedicationItem {
  return {
    id,
    nameWithStrengthFa: `اطلاعات واردشده ${id}`,
    form: 'SYRUP_SUSPENSION',
    doseAmountFa: 'مقدار رونویسی‌شده از برچسب',
    intervalHours: null,
    scheduleLabelFa: 'زمان‌بندی واردشده توسط مراقب',
    foodRelationFa: '',
    counselingNoteFa: 'یادداشت ویرایش‌شده مراقب',
    isWarningHighlight: false,
    dailyDoseSlots: 2,
    completedSlotTimesFa: { 1: '۱۰:۳۰' },
  };
}

function legacyPlan(): PediatricTreatmentPlan {
  return {
    ...createDefaultTreatmentPlan(),
    pharmacyNameFa: 'داروخانه شبانه‌روزی دکتر رضایی',
    pharmacistBadgeFa: 'تأیید دکتر داروساز (نظام: ۱۲۳۴۵)',
    fallbackChildSummaryFa: 'آراد عزیز • ۴ ساله (۱۶ کیلوگرم)',
    prescriptionDateFa: 'نسخه فعال • قابل ویرایش بر اساس دستور پزشک',
    storageInstructionFa: 'تمام داروهای این نسخه در دمای محیط (زیر ۲۵ درجه سانتی‌گراد)، دور از نور مستقیم و دسترس کودکان نگهداری شوند (نیازی به یخچال نیست؛ سوسپانسیون آماده‌شده حداکثر تا ۱۰ روز قابل مصرف است).',
    smartRemindersEnabled: true,
    lastCheckoffDayKey: TODAY,
    medications: [
      medication('rx_azithromycin_200'),
      medication('rx_acetaminophen_160'),
      medication('rx_saline_spray_09'),
      medication('rx_custom_caregiver'),
    ],
  };
}

describe('caregiver medication draft integrity', () => {
  it('starts empty with no child, pharmacy, clinical signoff or universal storage advice', () => {
    const plan = createDefaultTreatmentPlan();
    assert.deepEqual(plan.medications, []);
    assert.deepEqual(plan.legacyUnreviewedMedications, []);
    assert.deepEqual(getTreatmentSlotCounts(plan), { totalDailySlots: 0, completedTodaySlots: 0 });
    assert.equal(plan.pharmacyNameFa, '');
    assert.equal(plan.pharmacistBadgeFa, '');
    assert.equal(plan.storageInstructionFa, '');
    assert.equal(plan.smartRemindersEnabled, false);
    assert.match(plan.prescriptionDateFa, /پیش‌نویس محلی/);
    assert.doesNotMatch(plan.fallbackChildSummaryFa, /آراد|۱۶/);
    assert.notEqual(plan.medications, createDefaultTreatmentPlan().medications);
  });

  it('quarantines all known demo IDs even on the same day and preserves edited rows intact', () => {
    const input = legacyPlan();
    const oldRows = input.medications.slice(0, 3);
    const customRow = input.medications[3];
    const normalized = normalizePlanForToday(input, TODAY);

    assert.deepEqual(normalized.medications, [customRow]);
    assert.deepEqual(normalized.legacyUnreviewedMedications, oldRows);
    assert.equal(normalized.medications[0], customRow);
    assert.equal(normalized.legacyUnreviewedMedications?.[0], oldRows[0]);
    assert.deepEqual(getTreatmentSlotCounts(normalized), { totalDailySlots: 2, completedTodaySlots: 1 });
    assert.equal(input.medications.length, 4);
    assert.equal(input.legacyUnreviewedMedications?.length, 0);
    assert.equal(normalizePlanForToday(normalized, TODAY), normalized);
  });

  it('keeps an existing archive when another legacy row is quarantined', () => {
    const olderEditedRow = { ...medication('rx_azithromycin_200'), doseAmountFa: 'متن ویرایش قدیمی' };
    const input = legacyPlan();
    input.legacyUnreviewedMedications = [olderEditedRow];
    const normalized = normalizePlanForToday(input, TODAY);
    assert.deepEqual(normalized.legacyUnreviewedMedications, [olderEditedRow, ...input.medications.slice(0, 3)]);
  });

  it('removes exact fake metadata and the unavailable reminder preference without needing demo rows', () => {
    const input = legacyPlan();
    input.medications = [];
    const normalized = normalizePlanForToday(input, TODAY);
    const defaults = createDefaultTreatmentPlan();
    assert.equal(normalized.pharmacyNameFa, defaults.pharmacyNameFa);
    assert.equal(normalized.pharmacistBadgeFa, defaults.pharmacistBadgeFa);
    assert.equal(normalized.fallbackChildSummaryFa, defaults.fallbackChildSummaryFa);
    assert.equal(normalized.prescriptionDateFa, defaults.prescriptionDateFa);
    assert.equal(normalized.storageInstructionFa, '');
    assert.equal(normalized.smartRemindersEnabled, false);
  });

  it('preserves non-demo metadata instead of overwriting saved custom text', () => {
    const input = {
      ...legacyPlan(),
      pharmacyNameFa: 'نام ثبت‌شده توسط مراقب',
      pharmacistBadgeFa: 'متن اختصاصی ثبت‌شده',
      fallbackChildSummaryFa: 'نام اختصاصی کودک',
      prescriptionDateFa: 'تاریخ ثبت‌شده',
      storageInstructionFa: 'متن اختصاصی روی برچسب',
    };
    const normalized = normalizePlanForToday(input, TODAY);
    assert.equal(normalized.pharmacyNameFa, input.pharmacyNameFa);
    assert.equal(normalized.pharmacistBadgeFa, input.pharmacistBadgeFa);
    assert.equal(normalized.fallbackChildSummaryFa, input.fallbackChildSummaryFa);
    assert.equal(normalized.prescriptionDateFa, input.prescriptionDateFa);
    assert.equal(normalized.storageInstructionFa, input.storageInstructionFa);
  });

  it('resets only active daily checkoffs on a new day and retains the quarantined historical record', () => {
    const migrated = normalizePlanForToday(legacyPlan(), TODAY);
    const normalized = normalizePlanForToday(migrated, '2026-11-28');
    assert.equal(normalized.lastCheckoffDayKey, '2026-11-28');
    assert.deepEqual(normalized.medications[0].completedSlotTimesFa, {});
    assert.deepEqual(getTreatmentSlotCounts(normalized), { totalDailySlots: 2, completedTodaySlots: 0 });
    assert.equal(normalized.legacyUnreviewedMedications, migrated.legacyUnreviewedMedications);
    assert.deepEqual(normalized.legacyUnreviewedMedications?.[0].completedSlotTimesFa, { 1: '۱۰:۳۰' });
    assert.deepEqual(migrated.medications[0].completedSlotTimesFa, { 1: '۱۰:۳۰' });
  });

  it('shares only active caregiver entries with a clear draft status and no fabricated authority', () => {
    const plan = normalizePlanForToday(legacyPlan(), TODAY);
    const share = buildTreatmentCaregiverShareText(plan, null);
    assert.match(share, /پیش‌نویس محلی • بدون تأیید پزشک یا داروساز/);
    assert.match(share, /rx_custom_caregiver/);
    assert.doesNotMatch(share, /rx_azithromycin_200|rx_acetaminophen_160|rx_saline_spray_09/);
    assert.doesNotMatch(share, /رضایی|۱۲۳۴۵|۱ سی‌سی = ۲۰ قطره|شرایط نگهداری:/);
    assert.doesNotMatch(share, /نکته داروساز:/);
  });

  it('requires entered medication instructions and an explicit positive integer slot count', () => {
    const complete = medication('rx_custom');
    assert.equal(treatmentMedicationEntryError(complete), null);
    for (const field of ['nameWithStrengthFa', 'doseAmountFa', 'scheduleLabelFa'] as const) {
      assert.ok(treatmentMedicationEntryError({ ...complete, [field]: '  ' }));
    }
    for (const dailyDoseSlots of [0, -1, 1.5, 7, Number.NaN, Infinity]) {
      assert.ok(treatmentMedicationEntryError({ ...complete, dailyDoseSlots }));
    }
  });

  it('renders new entries without prefilled timing, food advice or a slot count', () => {
    const markup = renderToStaticMarkup(createElement(TreatmentPlanEditorModal, {
      initialItem: null,
      onClose: () => {},
      onSave: () => {},
    }));
    for (const field of ['treatment-med-schedule', 'treatment-med-food', 'treatment-med-slots']) {
      assert.match(markup, new RegExp(`<input[^>]*id="${field}"[^>]*value=""`));
    }
    assert.match(markup, /id="treatment-med-slots"[^>]*required=""[^>]*min="1"[^>]*max="6"/);
    assert.doesNotMatch(markup, /هر ۸ ساعت|همراه غذا یا بعد از غذا/);
  });
});
