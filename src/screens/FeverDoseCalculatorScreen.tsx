import React, { useMemo, useState } from 'react';
import { ArrowRight, CheckSquare, ClipboardCopy, Save, ShieldCheck } from 'lucide-react';
import {
  LIQUID_FORMULATION_LIST,
  evaluateLiquidDose,
  formatAgeFa,
  formatKg,
  parseLiquidDoseInputs,
  canRevealLiquidDose,
  type LiquidFormulation,
} from '../domain/liquidDoseVisualizerEngine.ts';
import { toPersianDigits } from '../domain/persianNormalizer.ts';
import { useMano } from '../state/ManoStore.tsx';
import { VisualSyringeCanvas } from '../components/VisualSyringeCanvas.tsx';
import { SeverityAlert } from '../components/SeverityAlert.tsx';
import { FeverDoseTimelineCard } from '../components/FeverDoseTimelineCard.tsx';
import { SourceBadge } from '../components/SourceBadge.tsx';

export const FeverDoseCalculatorScreen: React.FC<{ prefillFormulationId?: string }> = ({
  prefillFormulationId,
}) => {
  const { addDoseLog, navigate, notify } = useMano();

  const initialFormulation =
    LIQUID_FORMULATION_LIST.find((f) => f.id === prefillFormulationId) ??
    LIQUID_FORMULATION_LIST[1];

  const [formulation, setFormulation] = useState<LiquidFormulation>(initialFormulation);
  // Explicit, current measurements only. Never infer a dose from an empty profile.
  const [ageText, setAgeText] = useState('');
  const [weightText, setWeightText] = useState('');
  const [labelConfirmed, setLabelConfirmed] = useState(false);
  const { ageMonths, weightKg } = useMemo(
    () => parseLiquidDoseInputs(ageText, weightText),
    [ageText, weightText],
  );
  const result = useMemo(
    () => evaluateLiquidDose(formulation, ageMonths, weightKg),
    [formulation, ageMonths, weightKg],
  );
  const canRevealDose = canRevealLiquidDose(result, labelConfirmed);

  const handleSaveToTimeline = () => {
    if (!canRevealDose) return;
    addDoseLog({
      medicationSlug: formulation.key.startsWith('IBUPROFEN')
        ? 'ibuprofen'
        : formulation.key.startsWith('CETIRIZINE')
          ? 'cetirizine'
          : 'acetaminophen',
      medicationNameFa: formulation.shortLabelFa,
      dosageFormFa: formulation.concentrationLabelFa,
      volumeMlText: `${result.recommendedMl.toFixed(1)} mL (${Math.round(result.recommendedMg)} mg)`,
      noteFa: 'ثبت از محاسبه‌گر وزن مانو کید با سرنگ مدرج',
    });
  };

  const handleCopyCaregiverShare = async () => {
    if (!canRevealDose) return;
    const text = [
      `محاسبه ایمن داروی مایع کودک — مانو کید`,
      `فرآورده: ${formulation.titleFa}`,
      `سن: ${formatAgeFa(ageMonths)} | وزن: ${formatKg(weightKg)}`,
      `حجم پیشنهادی آموزشی: ${result.recommendedMl.toFixed(1)} mL (${Math.round(result.recommendedMg)} mg)`,
      `بازه ایمن بر پایه وزن: ${result.minSafeMl.toFixed(1)} تا ${result.maxSafeMl.toFixed(1)} mL`,
      `فاصله مصرف: ${formulation.intervalFa}`,
      `هشدار: فقط با سرنگ مدرج mL و پس از تطبیق برچسب دارو مصرف شود.`,
    ].join('\n');
    try {
      await navigator.clipboard.writeText(text);
      notify('خلاصه محاسبه دوز برای مراقب یا همسر کپی شد.', 'success');
    } catch {
      notify('امکان کپی خودکار نبود.', 'danger');
    }
  };

  return (
    <div className="space-y-6" data-testid="fever-dose-screen">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            محاسبه‌گر ایمن تب‌بر و داروهای مایع کودک
          </h1>
          <p className="mt-1 text-xs text-[#546963] dark:text-[#9EB3C4]">
            تبدیل وزن واقعی کودک به میلی‌لیتر دقیق با نمایش سرنگ مدرج و قفل خودکار سنین پرخطر
          </p>
        </div>
        <button
          type="button"
          onClick={() => navigate({ name: 'home' })}
          className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] px-3 text-xs font-bold dark:border-[#23344D]"
        >
          <ArrowRight className="h-4 w-4" />
          <span>خانه</span>
        </button>
      </div>

      {/* Step 1: Select Formulation */}
      <section className="rounded-3xl border border-[#DCE7E1] bg-white p-5 space-y-3 dark:border-[#23344D] dark:bg-[#111C2E]">
        <h2 className="text-sm font-extrabold text-[#105B48] dark:text-[#00D7C7]">
          ۱. نوع و غلظت دقیق دارو روی بطری را انتخاب کنید
        </h2>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
          {LIQUID_FORMULATION_LIST.map((item) => {
            const active = item.id === formulation.id;
            return (
              <button
                key={item.id}
                type="button"
                data-testid={`formulation-btn-${item.id}`}
                onClick={() => {
                  setFormulation(item);
                  setLabelConfirmed(false);
                }}
                className={`rounded-2xl border p-3.5 text-right transition-colors ${
                  active
                    ? 'border-[#105B48] bg-[#E9F5EB] dark:border-[#00D7C7] dark:bg-[#0F333A]'
                    : 'border-[#DCE7E1] bg-[#FAF7F0]/50 hover:border-[#105B48] dark:border-[#23344D] dark:bg-[#16243B]'
                }`}
              >
                <div className="text-sm font-bold text-[#122823] dark:text-[#EDF4F7]">{item.shortLabelFa}</div>
                <div className="mt-1 text-xs font-semibold text-[#105B48] dark:text-[#00D7C7]">
                  {item.concentrationLabelFa}
                </div>
              </button>
            );
          })}
        </div>
      </section>

      {/* Step 2: Explicit age and measured weight; no sliders or sample presets. */}
      <section className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <div className="rounded-3xl border border-[#DCE7E1] bg-white p-5 space-y-3 dark:border-[#23344D] dark:bg-[#111C2E]">
          <label htmlFor="dose-age-input" className="block text-sm font-extrabold text-[#122823] dark:text-[#EDF4F7]">۲. سن واقعی کودک (ماه کامل)</label>
          <input id="dose-age-input" data-testid="dose-age-input" type="text" inputMode="numeric"
            value={ageText} autoComplete="off" onChange={(e) => { setAgeText(e.target.value); setLabelConfirmed(false); }}
            className="min-h-[48px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] p-3 dark:border-[#23344D] dark:bg-[#16243B]" />
          <p className="text-xs text-[#546963] dark:text-[#9EB3C4]">سن را به ماه کامل وارد کنید؛ اگر مطمئن نیستید، از داروساز کمک بگیرید.</p>
        </div>
        <div className="rounded-3xl border border-[#DCE7E1] bg-white p-5 space-y-3 dark:border-[#23344D] dark:bg-[#111C2E]">
          <label htmlFor="dose-weight-input" className="block text-sm font-extrabold text-[#122823] dark:text-[#EDF4F7]">۳. وزن اندازه‌گیری‌شده کودک (کیلوگرم)</label>
          <input id="dose-weight-input" data-testid="dose-weight-input" type="text" inputMode="decimal"
            value={weightText} autoComplete="off" onChange={(e) => { setWeightText(e.target.value); setLabelConfirmed(false); }}
            className="min-h-[48px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] p-3 dark:border-[#23344D] dark:bg-[#16243B]" />
          <p className="text-xs text-[#546963] dark:text-[#9EB3C4]">وزن را حدس نزنید؛ اگر اندازه‌گیری نشده است، محاسبه را ادامه ندهید.</p>
        </div>
      </section>

      {/* Step 3: Age Lock or Mandatory Label Confirmation */}
      {result.isBlocked ? (
        <SeverityAlert
          severity={result.alertSeverity}
          title={result.alertTitleFa}
          body={result.alertBodyFa}
          testId="dose-blocked-alert"
        />
      ) : (
        <section className="rounded-3xl border border-[#E8CC99] bg-[#FDF9F1] p-5 dark:border-[#664F27] dark:bg-[#2B2112]">
          <label className="flex cursor-pointer items-start gap-3 text-sm font-bold text-[#4B3410] dark:text-[#FCE7C0]">
            <input
              type="checkbox"
              data-testid="dose-label-confirm-checkbox"
              checked={labelConfirmed}
              onChange={(e) => setLabelConfirmed(e.target.checked)}
              className="mt-1 h-5 w-5 shrink-0 accent-[#105B48]"
            />
            <span>
              تأیید می‌کنم سن و وزن واردشده واقعی و متعلق به همین کودک است و برچسب دارو دقیقاً برابر «{formulation.titleFa}» ({formulation.concentrationLabelFa}) است.
            </span>
          </label>
        </section>
      )}

      {/* Step 4: Visual Syringe & Numeric Output */}
      {canRevealDose && (
        <VisualSyringeCanvas formulation={formulation} result={result} showExactValues={true} />
      )}

      {canRevealDose ? (
        <section
          data-testid="dose-calculated-result"
          className="rounded-3xl border border-[#B6DFC1] bg-[#EBF7EE] p-5 space-y-4 dark:border-[#21594B] dark:bg-[#102B25]"
        >
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <div className="flex items-center gap-2 text-xs font-bold text-[#105B48] dark:text-[#35E0C9]">
                <ShieldCheck className="h-4 w-4" />
                <span>{result.alertTitleFa}</span>
              </div>
              <div className="mt-1 tabular-nums text-2xl font-extrabold text-[#122823] dark:text-[#EDF4F7]">
                {toPersianDigits(result.recommendedMl.toFixed(1))} mL{' '}
                <span className="text-sm font-semibold text-[#546963] dark:text-[#9EB3C4]">
                  (معادل {toPersianDigits(Math.round(result.recommendedMg))} میلی‌گرم)
                </span>
              </div>
              <div className="mt-1 text-xs text-[#113C27] dark:text-[#CBEFD9]">
                بازه مجاز بر پایه وزن: {toPersianDigits(result.minSafeMl.toFixed(1))} تا{' '}
                {toPersianDigits(result.maxSafeMl.toFixed(1))} mL · {formulation.intervalFa}
              </div>
            </div>

            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                data-testid="dose-save-timeline-btn"
                onClick={handleSaveToTimeline}
                className="inline-flex min-h-[44px] items-center gap-2 rounded-xl bg-[#105B48] px-4 text-xs font-bold text-white dark:bg-[#00D7C7] dark:text-[#0B1220]"
              >
                <Save className="h-4 w-4" />
                <span>ثبت این نوبت در خط زمانی ۲۴س</span>
              </button>
              <button
                type="button"
                onClick={handleCopyCaregiverShare}
                className="inline-flex min-h-[44px] items-center gap-2 rounded-xl border border-[#105B48] px-4 text-xs font-bold text-[#105B48] dark:border-[#00D7C7] dark:text-[#00D7C7]"
              >
                <ClipboardCopy className="h-4 w-4" />
                <span>کپی برای مراقب / همسر</span>
              </button>
            </div>
          </div>

          <p className="text-xs leading-5 text-[#113C27] dark:text-[#CBEFD9]">
            <CheckSquare className="ml-1 inline h-3.5 w-3.5" />
            {result.alertBodyFa}
          </p>
        </section>
      ) : null}

      <FeverDoseTimelineCard />

      <SourceBadge sourceIds={['aap_med_safety', 'aap_fever', 'applied12_medication']} />
    </div>
  );
};
