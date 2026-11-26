import React, { useEffect, useMemo, useRef, useState } from 'react';
import { ArrowRight, Loader2, ShieldAlert, Utensils, XCircle } from 'lucide-react';
import { initialAgeMonthsFromProfile } from '../domain/liquidDoseVisualizerEngine.ts';
import { buildChildContextSummaryFa } from '../domain/promptBuilders.ts';
import type { GatewayResponseMeta } from '../domain/aiGatewayContracts.ts';
import {
  ApiClientError,
  isRequestCancelledError,
  requestNutritionPlan,
} from '../api/geminiClient.ts';
import { useMano } from '../state/ManoStore.tsx';
import { SeverityAlert } from '../components/SeverityAlert.tsx';
import { SourceBadge } from '../components/SourceBadge.tsx';
import { GatewayMetaBanner } from '../components/GatewayMetaBanner.tsx';

const NO_ALLERGY_TOKENS = new Set([
  '',
  'ندارد',
  'ثبت نشده',
  'موردی ثبت نشده',
  'هیچ',
  'ندارد/ثبت نشده',
  'خیر',
  'none',
]);

export const NutritionAdvisorScreen: React.FC = () => {
  const { childProfile, vaccineBirthDateEpochMs, nutritionPlans, saveNutritionPlan, navigate } =
    useMano();

  const inferredAge = initialAgeMonthsFromProfile(childProfile, vaccineBirthDateEpochMs);
  const [ageMonths, setAgeMonths] = useState<number>(
    inferredAge ?? (childProfile.ageBand === 'NEWBORN' ? 2 : 12),
  );
  const [weightKg, setWeightKg] = useState(childProfile.weightKgText || '');
  const [allergiesFa, setAllergiesFa] = useState(childProfile.allergiesFa || '');
  const [dietGoalFa, setDietGoalFa] = useState('برنامه غذایی متعادل و متنوع برای رشد سالم');

  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [gatewayMeta, setGatewayMeta] = useState<GatewayResponseMeta | null>(null);
  const [policyError, setPolicyError] = useState<string | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    return () => {
      abortRef.current?.abort();
    };
  }, []);

  const isUnder6Months = ageMonths < 6;
  const hasRecordedAllergy = !NO_ALLERGY_TOKENS.has(allergiesFa.trim().toLowerCase());
  const isBlocked = isUnder6Months || hasRecordedAllergy;

  const activePlan = useMemo(() => nutritionPlans[0] ?? null, [nutritionPlans]);

  const handleCancel = () => {
    abortRef.current?.abort();
    abortRef.current = null;
    setLoading(false);
    setErrorMsg('درخواست راهنمای آموزشی تغذیه توسط کاربر لغو شد.');
  };

  const handleGenerate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isBlocked) return;

    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    setLoading(true);
    setErrorMsg(null);
    setPolicyError(null);
    setGatewayMeta(null);
    try {
      const result = await requestNutritionPlan({
        ageMonths,
        weightKg: weightKg.trim(),
        allergiesFa: allergiesFa.trim(),
        dietGoalFa: dietGoalFa.trim(),
        childContextSummaryFa: buildChildContextSummaryFa(childProfile),
        signal: controller.signal,
      });
      saveNutritionPlan(result.plan);
      setGatewayMeta(result.meta);
      setPolicyError(result.policyError ?? null);
    } catch (err) {
      if (isRequestCancelledError(err)) {
        setErrorMsg('درخواست توسط کاربر لغو شد.');
      } else {
        setErrorMsg(err instanceof ApiClientError || err instanceof Error ? err.message : 'خطا در تولید نمونه‌های آموزشی تغذیه.');
      }
    } finally {
      if (abortRef.current === controller) {
        abortRef.current = null;
      }
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6" data-testid="nutrition-screen">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            راهنمای آموزشی تغذیه بومی کودک
          </h1>
          <p className="mt-1 text-xs text-[#546963] dark:text-[#9EB3C4]">
            ساده‌سازی نمونه‌های آموزشی تغذیه ۶ ماه به بالا با قفل‌های ایمنی قطعی سن، آلرژی و پیشگیری از خفگی (تجویز بالینی نیست)
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

      <form
        onSubmit={handleGenerate}
        className="rounded-3xl border border-[#DCE7E1] bg-white p-5 space-y-4 dark:border-[#23344D] dark:bg-[#111C2E]"
      >
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <div>
            <label className="block text-xs font-bold">سن کودک (ماه)</label>
            <input
              type="number"
              min={0}
              max={144}
              value={ageMonths}
              onChange={(e) => setAgeMonths(Math.max(0, Number(e.target.value) || 0))}
              className="mt-1.5 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm tabular-nums dark:border-[#23344D] dark:bg-[#16243B]"
            />
          </div>
          <div>
            <label className="block text-xs font-bold">وزن فعلی (کیلوگرم، اختیاری)</label>
            <input
              type="text"
              value={weightKg}
              onChange={(e) => setWeightKg(e.target.value)}
              placeholder="مثلاً 9.5"
              className="mt-1.5 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm tabular-nums dark:border-[#23344D] dark:bg-[#16243B]"
            />
          </div>
          <div>
            <label className="block text-xs font-bold">سابقه حساسیت غذایی</label>
            <input
              type="text"
              value={allergiesFa}
              onChange={(e) => setAllergiesFa(e.target.value)}
              placeholder="ندارد (یا نام حساسیت)"
              className="mt-1.5 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
            />
          </div>
        </div>

        <div>
          <label className="block text-xs font-bold">هدف یا تمرکز تغذیه‌ای</label>
          <input
            type="text"
            value={dietGoalFa}
            onChange={(e) => setDietGoalFa(e.target.value)}
            className="mt-1.5 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
          />
        </div>

        {isUnder6Months ? (
          <SeverityAlert
            severity="danger"
            title="قفل ایمنی زیر ۶ ماه: تغذیه تکمیلی غیرفعال است"
            body="طبق دستورالعمل WHO و وزارت بهداشت، در شیرخواران زیر ۶ ماه غذای کمکی شروع نمی‌شود و تغذیه منحصراً با شیر مادر یا شیر خشک طبق نظر پزشک کودک انجام می‌شود."
            testId="nutrition-under6m-lock"
          />
        ) : hasRecordedAllergy ? (
          <SeverityAlert
            severity="danger"
            title="محدودیت ایمنی آلرژی غذایی"
            body="به دلیل ثبت حساسیت یا آلرژی غذایی، پیشنهاد برنامه غذایی خودکار غیرفعال شده است تا ریسک واکنش آلرژیک ایجاد نشود. برنامه تغذیه را مستقیماً با پزشک کودک تنظیم کنید."
            testId="nutrition-allergy-lock"
          />
        ) : (
          <div className="flex flex-wrap gap-2">
            <button
              type="submit"
              disabled={loading}
              className="inline-flex min-h-[44px] items-center gap-2 rounded-xl bg-[#105B48] px-5 text-sm font-bold text-white disabled:opacity-60 dark:bg-[#00D7C7] dark:text-[#0B1220]"
            >
              {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Utensils className="h-4 w-4" />}
              <span>{loading ? 'در حال تنظیم نمونه‌های ایمن...' : 'دریافت نمونه‌های آموزشی تغذیه'}</span>
            </button>
            {loading ? (
              <button
                type="button"
                onClick={handleCancel}
                className="inline-flex min-h-[44px] items-center gap-1.5 rounded-xl border border-[#E7B5BC] bg-[#FDF0F1] px-4 text-xs font-bold text-[#B32334] dark:border-[#6A2C3B] dark:bg-[#2F161E] dark:text-[#FF8594]"
              >
                <XCircle className="h-4 w-4" />
                <span>لغو</span>
              </button>
            ) : null}
          </div>
        )}

        {errorMsg ? <SeverityAlert severity="caution" title="وضعیت راهنمای تغذیه" body={errorMsg} /> : null}
        {gatewayMeta ? <GatewayMetaBanner meta={gatewayMeta} policyError={policyError} /> : null}
      </form>

      {ageMonths < 12 && !isUnder6Months ? (
        <div className="flex items-center gap-2 rounded-2xl border border-[#E7B5BC] bg-[#FDF0F1] p-3 text-xs font-bold text-[#B32334] dark:border-[#6A2C3B] dark:bg-[#2F161E] dark:text-[#FF8594]">
          <ShieldAlert className="h-4 w-4 shrink-0" />
          <span>محدودیت زیر ۱۲ ماه: عسل، شیر گاو به عنوان نوشیدنی، نمک افزوده، انگور و آجیل کامل اکیداً ممنوع هستند.</span>
        </div>
      ) : null}

      {activePlan ? (
        <section className="space-y-4">
          <div className="rounded-3xl border border-[#DCE7E1] bg-white p-5 space-y-2 dark:border-[#23344D] dark:bg-[#111C2E]">
            <h2 className="text-base font-extrabold text-[#105B48] dark:text-[#00D7C7]">
              {activePlan.planTitleFa}
            </h2>
            <p className="text-sm leading-6 text-[#122823] dark:text-[#EDF4F7]">{activePlan.overviewFa}</p>
            <div className="grid grid-cols-1 gap-2 pt-2 text-xs sm:grid-cols-2">
              <div className="rounded-2xl bg-[#F5F9F6] p-3 dark:bg-[#16243B]">
                <span className="font-bold">تمرکز رشد: </span>
                {activePlan.growthNutritionFocusFa}
              </div>
              <div className="rounded-2xl bg-[#F5F9F6] p-3 dark:bg-[#16243B]">
                <span className="font-bold">آبرسانی: </span>
                {activePlan.hydrationTipFa}
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            {activePlan.meals.map((meal, idx) => (
              <article
                key={idx}
                className="rounded-3xl border border-[#DCE7E1] bg-white p-5 space-y-2.5 dark:border-[#23344D] dark:bg-[#111C2E]"
              >
                <div className="text-xs font-bold text-[#105B48] dark:text-[#00D7C7]">{meal.mealTypeFa}</div>
                <h3 className="text-base font-extrabold text-[#122823] dark:text-[#EDF4F7]">{meal.titleFa}</h3>
                <p className="text-xs leading-5 text-[#546963] dark:text-[#9EB3C4]">{meal.whySuitableFa}</p>
                <div className="rounded-xl bg-[#F5F9F6] p-2.5 text-xs dark:bg-[#16243B]">
                  <span className="font-bold">بافت و اندازه سهم: </span>
                  {meal.textureAndPortionFa}
                </div>
                <div className="text-xs">
                  <div className="font-bold">مواد لازم:</div>
                  <p className="text-[#546963] dark:text-[#9EB3C4]">{meal.ingredientsFa.join('، ')}</p>
                </div>
                <div className="text-xs">
                  <div className="font-bold">روش تهیه:</div>
                  <ul className="list-disc pr-5 text-[#122823] dark:text-[#EDF4F7]">
                    {meal.prepStepsFa.map((s, sIdx) => (
                      <li key={sIdx}>{s}</li>
                    ))}
                  </ul>
                </div>
                {meal.safetyAvoidListFa.length > 0 ? (
                  <div className="text-xs text-[#B32334] dark:text-[#FF8594]">
                    <span className="font-bold">موارد منع ایمنی: </span>
                    {meal.safetyAvoidListFa.join('، ')}
                  </div>
                ) : null}
              </article>
            ))}
          </div>
        </section>
      ) : null}

      <SourceBadge sourceIds={['nelson8_growth']} />
    </div>
  );
};
