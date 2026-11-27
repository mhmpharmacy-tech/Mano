import React, { useState } from 'react';
import {
  ArrowLeft, Baby, Calculator, ChevronDown, Moon, Search, Stethoscope, Sun,
} from 'lucide-react';
import { useMano } from '../state/ManoStore.tsx';
import { initialAgeMonthsFromProfile } from '../domain/liquidDoseVisualizerEngine.ts';
import { getTreatmentSlotCounts } from '../domain/treatmentPlan.ts';
import { toPersianDigits } from '../domain/persianNormalizer.ts';
import { FEATURES, FEATURE_GROUPS, type FeatureId } from '../navigation/featureCatalog.ts';
import { FEATURE_ICONS } from '../components/featureIcons.ts';
import { FeverDoseTimelineCard } from '../components/FeverDoseTimelineCard.tsx';
import { NonDrugFeverCareSheet } from '../components/NonDrugFeverCareSheet.tsx';

const DAILY_FEATURE_IDS: readonly FeatureId[] = ['growth', 'vaccines', 'treatment-plan', 'syrup-scanner'];
const VISIBLE_FEATURE_IDS = new Set<FeatureId>(['symptoms', 'fever-dose', 'my-child', ...DAILY_FEATURE_IDS]);
const GUIDE_GROUPS = FEATURE_GROUPS.map((group) => ({
  ...group,
  featureIds: group.featureIds.filter((id) => !VISIBLE_FEATURE_IDS.has(id)),
})).filter((group) => group.featureIds.length > 0);

export const HomeScreen: React.FC = () => {
  const {
    navigate, theme, toggleTheme, childProfile, vaccineBirthDateEpochMs,
    treatmentPlan, vaccineSummary, setSearchOpen,
  } = useMano();
  const [comfortSheetOpen, setComfortSheetOpen] = useState(false);
  const slotCounts = getTreatmentSlotCounts(treatmentPlan);
  const knownAgeMonths = initialAgeMonthsFromProfile(childProfile, vaccineBirthDateEpochMs);
  const ageLabel = knownAgeMonths === null ? 'سن کودک ثبت نشده است' : `${toPersianDigits(knownAgeMonths)} ماه`;
  const profileHasDetails = knownAgeMonths !== null || Boolean(childProfile.weightKgText.trim()) || Boolean(childProfile.allergiesFa.trim());

  const renderFeature = (id: FeatureId) => {
    const feature = FEATURES[id];
    const Icon = FEATURE_ICONS[feature.iconKey];
    return (
      <button
        key={id}
        type="button"
        onClick={() => navigate(feature.route)}
        data-testid={`home-tool-${id}`}
        className="group flex min-h-[92px] items-start gap-3 rounded-2xl border border-[#DCE7E1] bg-white p-3.5 text-right transition-colors hover:border-[#105B48] hover:bg-[#F5F9F6] dark:border-[#23344D] dark:bg-[#111C2E] dark:hover:border-[#00D7C7] dark:hover:bg-[#16243B]"
      >
        <Icon aria-hidden="true" className="mt-0.5 h-5 w-5 shrink-0 text-[#105B48] dark:text-[#00D7C7]" />
        <span className="min-w-0">
          <span className="block text-sm font-bold leading-6 text-[#122823] dark:text-[#EDF4F7]">{feature.labelFa}</span>
          <span className="mt-1 block text-xs leading-5 text-[#546963] dark:text-[#9EB3C4]">{feature.descriptionFa}</span>
        </span>
      </button>
    );
  };

  return (
    <div className="space-y-5" data-testid="home-screen">
      <section aria-labelledby="home-heading" className="space-y-4 rounded-3xl border border-[#DCE7E1] bg-white p-4 sm:p-6 dark:border-[#23344D] dark:bg-[#111C2E]">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <span className="text-xs font-semibold text-[#546963] dark:text-[#9EB3C4]">راهنمای آموزشی مانو</span>
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={() => setSearchOpen(true)}
              data-testid="home-search-btn"
              className="inline-flex min-h-[44px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-xs font-bold text-[#122823] hover:border-[#105B48] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#EDF4F7]"
            >
              <Search aria-hidden="true" className="h-4 w-4 text-[#105B48] dark:text-[#00D7C7]" />
              <span>جست‌وجو</span>
            </button>
            <button
              type="button"
              onClick={toggleTheme}
              data-testid="home-theme-toggle-btn"
              aria-label={theme === 'dark' ? 'تغییر به حالت روشن' : 'تغییر به حالت تیره'}
              className="inline-flex min-h-[44px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-xs font-bold text-[#122823] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#EDF4F7]"
            >
              {theme === 'dark' ? <Sun aria-hidden="true" className="h-4 w-4 text-[#F0B85C]" /> : <Moon aria-hidden="true" className="h-4 w-4 text-[#105B48]" />}
              <span>{theme === 'dark' ? 'روشن' : 'تیره'}</span>
            </button>
          </div>
        </div>
        <div className="flex flex-wrap items-center gap-3 sm:gap-5">
          <div className="min-w-0 flex-1 basis-48 space-y-2">
            <h1 id="home-heading" className="text-balance text-xl font-extrabold leading-8 text-[#122823] sm:text-3xl sm:leading-10 dark:text-[#EDF4F7]">
              برای مراقبت امروز، از کجا شروع کنیم؟
            </h1>
            <p className="text-sm leading-6 text-[#546963] dark:text-[#9EB3C4]">راهنما را بخوانید و ثبت‌های روزانه را در همین دستگاه مرور کنید.</p>
          </div>
          <img
            src="/assets/images/img_mano_hero_clean.png"
            alt=""
            className="h-20 w-20 shrink-0 rounded-2xl bg-[#E9F5EB]/70 object-contain p-1 sm:h-28 sm:w-28 dark:bg-[#16243B]"
            referrerPolicy="no-referrer"
          />
        </div>
        <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
          <button
            type="button"
            onClick={() => navigate(FEATURES.symptoms.route)}
            data-testid="home-cta-symptoms"
            className="flex min-h-[56px] items-center gap-3 rounded-2xl bg-[#105B48] px-4 py-3 text-right text-sm font-bold text-white hover:bg-[#0C493A] dark:bg-[#00D7C7] dark:text-[#0B1220]"
          >
            <Stethoscope aria-hidden="true" className="h-5 w-5 shrink-0" />
            <span className="flex-1">{FEATURES.symptoms.labelFa}</span>
            <ArrowLeft aria-hidden="true" className="h-4 w-4 shrink-0" />
          </button>
          <button
            type="button"
            onClick={() => navigate(FEATURES['fever-dose'].route)}
            data-testid="home-cta-fever-dose"
            className="flex min-h-[56px] items-center gap-3 rounded-2xl border border-[#DCE7E1] bg-[#F5F9F6] px-4 py-3 text-right text-sm font-bold text-[#122823] hover:border-[#105B48] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#EDF4F7]"
          >
            <Calculator aria-hidden="true" className="h-5 w-5 shrink-0 text-[#105B48] dark:text-[#00D7C7]" />
            <span className="flex-1">{FEATURES['fever-dose'].labelFa}</span>
            <ArrowLeft aria-hidden="true" className="h-4 w-4 shrink-0" />
          </button>
        </div>
      </section>

      <button
        type="button"
        onClick={() => navigate(FEATURES['my-child'].route)}
        data-testid="home-child-profile-btn"
        className="flex min-h-[76px] w-full flex-wrap items-center gap-3 rounded-2xl border border-[#DCE7E1] bg-[#F2F6F1] px-4 py-3 text-right hover:border-[#105B48] dark:border-[#23344D] dark:bg-[#16243B]"
      >
        <Baby aria-hidden="true" className="h-5 w-5 shrink-0 text-[#105B48] dark:text-[#00D7C7]" />
        <span className="min-w-0 flex-1 basis-36 space-y-1">
          <span className="block break-words text-sm font-bold">{profileHasDetails ? childProfile.aliasFa : 'پرونده آموزشی کودک'}</span>
          <span data-testid="home-child-age" className="block text-xs leading-5 text-[#546963] dark:text-[#9EB3C4]">
            {ageLabel}{childProfile.weightKgText.trim() ? ` · ${toPersianDigits(childProfile.weightKgText.trim())} کیلوگرم` : ''}
          </span>
        </span>
        <span className="shrink-0 text-xs font-bold text-[#105B48] dark:text-[#00D7C7]">{profileHasDetails ? 'ویرایش' : 'ثبت مشخصات'}</span>
        <ArrowLeft aria-hidden="true" className="h-4 w-4 shrink-0 text-[#105B48] dark:text-[#00D7C7]" />
      </button>

      <section aria-labelledby="home-daily-tools-heading" className="space-y-3">
        <h2 id="home-daily-tools-heading" className="text-base font-extrabold">ابزارهای روزانه</h2>
        <div className="grid grid-cols-1 gap-2.5 min-[380px]:grid-cols-2 lg:grid-cols-4">{DAILY_FEATURE_IDS.map(renderFeature)}</div>
      </section>

      <section aria-labelledby="home-records-heading" className="space-y-3 rounded-3xl border border-[#DCE7E1] bg-white p-4 sm:p-5 dark:border-[#23344D] dark:bg-[#111C2E]">
        <h2 id="home-records-heading" className="text-base font-extrabold">ثبت‌های امروز</h2>
        <div className="grid gap-3 sm:grid-cols-2">
          <div className="space-y-1 rounded-2xl bg-[#FAF7F0] p-3 dark:bg-[#16243B]">
            <h3 className="text-sm font-bold">{FEATURES['treatment-plan'].labelFa}</h3>
            <p className="text-xs leading-6 text-[#546963] dark:text-[#9EB3C4]">
              {slotCounts.totalDailySlots > 0
                ? `${toPersianDigits(slotCounts.completedTodaySlots)} از ${toPersianDigits(slotCounts.totalDailySlots)} نوبت امروز ثبت شده`
                : 'هنوز برنامه‌ای ثبت نشده است.'}
            </p>
          </div>
          <div className="space-y-1 rounded-2xl bg-[#FAF7F0] p-3 dark:bg-[#16243B]">
            <h3 className="text-sm font-bold">{FEATURES.vaccines.labelFa}</h3>
            <p data-testid="home-vaccine-next-summary" className="text-xs leading-6 text-[#546963] dark:text-[#9EB3C4]">
              {vaccineBirthDateEpochMs === null
                ? 'تاریخ تولد کودک را برای زمان‌بندی واکسن‌ها ثبت کنید.'
                : vaccineSummary.nextUpcoming
                  ? `موعد بعدی: ${vaccineSummary.nextUpcoming.definition.titleFa}`
                  : 'در جدول فعلی نوبت آینده‌ای نمایش داده نمی‌شود.'}
            </p>
          </div>
        </div>
      </section>

      <FeverDoseTimelineCard compact />

      <section aria-labelledby="home-guides-heading" className="space-y-3">
        <div>
          <h2 id="home-guides-heading" className="text-base font-extrabold">راهنماهای بیشتر</h2>
          <p className="mt-1 text-xs leading-6 text-[#546963] dark:text-[#9EB3C4]">برای دیدن ابزارهای هر بخش، عنوان آن را باز کنید.</p>
        </div>
        {GUIDE_GROUPS.map((group) => (
          <details key={group.id} data-testid={`home-guide-group-${group.id}`} className="group rounded-2xl border border-[#DCE7E1] bg-white dark:border-[#23344D] dark:bg-[#111C2E]">
            <summary className="flex min-h-[56px] cursor-pointer list-none items-center justify-between gap-3 rounded-2xl px-4 py-3 text-sm font-bold marker:content-none [&::-webkit-details-marker]:hidden">
              <span>{group.titleFa}</span>
              <ChevronDown aria-hidden="true" className="h-4 w-4 shrink-0 text-[#546963] transition-transform group-open:rotate-180 dark:text-[#9EB3C4]" />
            </summary>
            <div className="grid gap-2 px-3 pb-3 sm:grid-cols-2">{group.featureIds.map(renderFeature)}</div>
          </details>
        ))}
        <button
          type="button"
          onClick={() => setComfortSheetOpen(true)}
          data-testid="home-fever-comfort-btn"
          className="flex min-h-[48px] w-full items-center justify-between gap-3 rounded-2xl border border-[#DCE7E1] bg-[#F5F9F6] px-4 py-3 text-right text-sm font-bold text-[#105B48] hover:border-[#105B48] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#00D7C7]"
        >
          <span>مراقبت غیردارویی تب</span>
          <ArrowLeft aria-hidden="true" className="h-4 w-4 shrink-0" />
        </button>
      </section>
      <NonDrugFeverCareSheet open={comfortSheetOpen} onClose={() => setComfortSheetOpen(false)} />
    </div>
  );
};
