import React, { useState } from 'react';
import {
  Baby,
  BookOpen,
  Calculator,
  ClipboardList,
  Cpu,
  HelpCircle,
  Moon,
  Pill,
  QrCode,
  Scale,
  ScanLine,
  Search,
  Sparkles,
  Stethoscope,
  Sun,
  Syringe,
  UserCheck,
  Utensils,
} from 'lucide-react';
import { useMano } from '../state/ManoStore.tsx';
import { AGE_BANDS } from '../domain/models.ts';
import { getTreatmentSlotCounts, resolvePlanChildSummary } from '../domain/treatmentPlan.ts';
import { toPersianDigits } from '../domain/persianNormalizer.ts';
import { FeverDoseTimelineCard } from '../components/FeverDoseTimelineCard.tsx';
import { NonDrugFeverCareSheet } from '../components/NonDrugFeverCareSheet.tsx';
import { CLINICAL_REVIEWER_IDENTITY_FA } from '../data/contentRepository.ts';

export const HomeScreen: React.FC = () => {
  const {
    navigate,
    theme,
    toggleTheme,
    homeViewStyle,
    setHomeViewStyle,
    childProfile,
    treatmentPlan,
    vaccineSummary,
    setSearchOpen,
  } = useMano();

  const [comfortSheetOpen, setComfortSheetOpen] = useState(false);

  const slotCounts = getTreatmentSlotCounts(treatmentPlan);
  const ageBandLabel = AGE_BANDS.find((b) => b.id === childProfile.ageBand)?.labelFa ?? '';

  const orbitalOrGridTools = [
    {
      title: 'بررسی علائم',
      desc: 'تب، سرفه، گوش، چشم، گوارش',
      img: '/assets/images/mano_symptoms_orbit.webp',
      route: { name: 'symptoms' } as const,
      Icon: Stethoscope,
    },
    {
      title: 'تب‌بر و سرنگ',
      desc: 'محاسبه دقیق میلی‌لیتر بر پایه وزن',
      img: '/assets/images/ic_brand_fever.png',
      route: { name: 'fever-dose' } as const,
      Icon: Calculator,
    },
    {
      title: 'اسکنر شربت',
      desc: 'تطبیق غلظت بطری و پیشگیری خطا',
      img: '/assets/images/ic_brand_scanner.png',
      route: { name: 'syrup-scanner' } as const,
      Icon: ScanLine,
    },
    {
      title: 'رشد WHO',
      desc: 'نمودار قد/وزن و نقاط عطف',
      img: '/assets/images/mano_growth_orbit.webp',
      route: { name: 'growth' } as const,
      Icon: Scale,
    },
    {
      title: 'واکسیناسیون',
      desc: 'جدول کشوری و نوبت‌های پیش رو',
      img: '/assets/images/ic_brand_vaccines.png',
      route: { name: 'vaccines' } as const,
      Icon: Syringe,
    },
    {
      title: 'برنامه درمانی',
      desc: `${toPersianDigits(slotCounts.completedTodaySlots)} از ${toPersianDigits(slotCounts.totalDailySlots)} نوبت امروز`,
      img: '/assets/images/ic_brand_reminders.png',
      route: { name: 'treatment-plan' } as const,
      Icon: ClipboardList,
    },
    {
      title: 'پرسش‌های متداول',
      desc: '۳۴ سوال رایج + پاسخ مستند هوشمند',
      img: '/assets/images/mano_questions_orbit.webp',
      route: { name: 'faq' } as const,
      Icon: HelpCircle,
    },
    {
      title: 'دارونامه و تداخل',
      desc: '۱۰ دارو و بررسی هم‌پوشانی',
      img: '/assets/images/ic_brand_medicines.png',
      route: { name: 'medicines' } as const,
      Icon: Pill,
    },
  ];

  const secondarySections = [
    { label: 'مشاور تغذیه (+۶ ماه)', route: { name: 'nutrition' } as const, Icon: Utensils },
    { label: 'مکمل و بهداشت کودک', route: { name: 'supplements-hygiene' } as const, Icon: Sparkles },
    { label: 'پوست و سوختگی پوشک', route: { name: 'skin' } as const, Icon: Baby },
    { label: 'تجهیزات و تب‌سنجی', route: { name: 'devices' } as const, Icon: Cpu },
    { label: 'کتابخانه مقالات علمی', route: { name: 'topics' } as const, Icon: BookOpen },
    { label: 'پرسش از داروساز', route: { name: 'ask-pharmacist' } as const, Icon: UserCheck },
    { label: 'کارت‌های مراقبت', route: { name: 'care-cards' } as const, Icon: QrCode },
    { label: 'مراقبت غیردارویی تب', route: null, Icon: Sparkles, onClick: () => setComfortSheetOpen(true) },
  ];

  return (
    <div className="space-y-6" data-testid="home-screen">
      {/* Hero Split Banner */}
      <section className="grid grid-cols-1 items-stretch gap-5 rounded-3xl border border-[#DCE7E1] bg-white p-5 sm:p-7 lg:grid-cols-12 dark:border-[#23344D] dark:bg-[#111C2E]">
        <div className="flex flex-col justify-between space-y-5 lg:col-span-7">
          <div className="space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <span className="text-xs font-semibold text-[#546963] dark:text-[#9EB3C4]">
                همراه علمی و آرام والدین · {CLINICAL_REVIEWER_IDENTITY_FA}
              </span>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setSearchOpen(true)}
                  data-testid="home-search-btn"
                  className="inline-flex min-h-[38px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-xs font-bold text-[#122823] hover:border-[#105B48] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#EDF4F7]"
                >
                  <Search className="h-3.5 w-3.5 text-[#105B48] dark:text-[#00D7C7]" />
                  <span>جست‌وجو</span>
                </button>
                <button
                  type="button"
                  onClick={toggleTheme}
                  data-testid="home-theme-toggle-btn"
                  aria-label="تغییر تم روشن یا تیره"
                  className="inline-flex min-h-[38px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-xs font-bold text-[#122823] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#EDF4F7]"
                >
                  {theme === 'dark' ? (
                    <Sun className="h-3.5 w-3.5 text-[#F0B85C]" />
                  ) : (
                    <Moon className="h-3.5 w-3.5 text-[#105B48]" />
                  )}
                  <span>{theme === 'dark' ? 'روشن' : 'تیره'}</span>
                </button>
              </div>
            </div>

            <h1 className="text-balance text-2xl font-extrabold leading-tight text-[#122823] sm:text-3xl dark:text-[#EDF4F7]">
              مراقبت دقیق و آرام کودک در خانه، بر پایه شواهد بالینی
            </h1>
            <p className="max-w-2xl text-sm leading-7 text-[#546963] dark:text-[#9EB3C4]">
              از غربالگری فوری علائم خطر و محاسبه دقیق میلی‌لیتر تب‌بر بر پایه وزن تا جدول واکسیناسیون، نمودار رشد WHO و برنامه درمانی روزانه.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <button
              type="button"
              onClick={() => navigate({ name: 'symptoms' })}
              data-testid="home-cta-symptoms"
              className="inline-flex min-h-[46px] items-center gap-2 rounded-xl bg-[#105B48] px-5 text-sm font-bold text-white hover:bg-[#0C493A] dark:bg-[#00D7C7] dark:text-[#0B1220]"
            >
              <Stethoscope className="h-4 w-4" />
              <span>بررسی علائم کودک</span>
            </button>

            <button
              type="button"
              onClick={() => navigate({ name: 'fever-dose' })}
              data-testid="home-cta-fever-dose"
              className="inline-flex min-h-[46px] items-center gap-2 rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-5 text-sm font-bold text-[#122823] hover:border-[#105B48] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#EDF4F7]"
            >
              <Calculator className="h-4 w-4 text-[#105B48] dark:text-[#00D7C7]" />
              <span>محاسبه دوز تب‌بر</span>
            </button>
          </div>
        </div>

        {/* Visual Companion & Child Summary Panel */}
        <div className="flex flex-col justify-between gap-4 rounded-2xl bg-[#E9F5EB]/70 p-4 lg:col-span-5 dark:bg-[#16243B]">
          <div className="flex items-center gap-4">
            <img
              src="/assets/images/img_mano_hero_clean.png"
              alt="تصویر مانو همراه سلامت کودک"
              className="h-24 w-24 shrink-0 rounded-2xl bg-white/80 object-contain p-1.5 dark:bg-[#0B1220]"
              referrerPolicy="no-referrer"
            />
            <div className="min-w-0 space-y-1">
              <div className="text-xs font-semibold text-[#105B48] dark:text-[#00D7C7]">پرونده فعال کودک</div>
              <div className="truncate text-base font-extrabold text-[#122823] dark:text-[#EDF4F7]">
                {childProfile.aliasFa}
              </div>
              <div className="text-xs text-[#546963] dark:text-[#9EB3C4]">
                {ageBandLabel}
                {childProfile.weightKgText ? ` · ${toPersianDigits(childProfile.weightKgText)} کیلوگرم` : ''}
              </div>
            </div>
          </div>

          <div className="flex flex-wrap items-center justify-between gap-2 border-t border-[#DCE7E1] pt-3 text-xs dark:border-[#23344D]">
            <button
              type="button"
              onClick={() => navigate({ name: 'my-child' })}
              className="font-bold text-[#105B48] hover:underline dark:text-[#00D7C7]"
            >
              ویرایش مشخصات و حساسیت‌های کودک
            </button>
            <div className="flex items-center gap-1 rounded-lg bg-white/80 p-1 dark:bg-[#0B1220]">
              <button
                type="button"
                onClick={() => setHomeViewStyle('calm')}
                className={`rounded-md px-2.5 py-1 text-[11px] font-bold ${
                  homeViewStyle === 'calm'
                    ? 'bg-[#105B48] text-white dark:bg-[#00D7C7] dark:text-[#0B1220]'
                    : 'text-[#546963] dark:text-[#9EB3C4]'
                }`}
              >
                نمای آرام
              </button>
              <button
                type="button"
                onClick={() => setHomeViewStyle('orbital')}
                className={`rounded-md px-2.5 py-1 text-[11px] font-bold ${
                  homeViewStyle === 'orbital'
                    ? 'bg-[#105B48] text-white dark:bg-[#00D7C7] dark:text-[#0B1220]'
                    : 'text-[#546963] dark:text-[#9EB3C4]'
                }`}
              >
                مداری سریع
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* Primary Tools Grid / Orbital Showcase */}
      <section className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            {homeViewStyle === 'orbital' ? 'مدار ابزارهای فوری مانو کید' : 'ابزارهای کلیدی مراقبت امروز'}
          </h2>
          <span className="text-xs text-[#546963] dark:text-[#9EB3C4]">
            {resolvePlanChildSummary(treatmentPlan, childProfile)}
          </span>
        </div>

        <div
          className={`grid gap-3 ${
            homeViewStyle === 'orbital'
              ? 'grid-cols-2 sm:grid-cols-4'
              : 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-4'
          }`}
        >
          {orbitalOrGridTools.map((tool) => (
            <button
              key={tool.title}
              type="button"
              onClick={() => navigate(tool.route)}
              data-testid={`home-tool-${tool.route.name}`}
              className="group flex items-center gap-3.5 rounded-2xl border border-[#DCE7E1] bg-white p-4 text-right transition-colors hover:border-[#105B48] hover:bg-[#F5F9F6] dark:border-[#23344D] dark:bg-[#111C2E] dark:hover:border-[#00D7C7] dark:hover:bg-[#16243B]"
            >
              <img
                src={tool.img}
                alt={tool.title}
                className="h-12 w-12 shrink-0 rounded-xl bg-[#E9F5EB] object-contain p-1 dark:bg-[#0F333A]"
                referrerPolicy="no-referrer"
              />
              <div className="min-w-0">
                <div className="text-sm font-extrabold text-[#122823] group-hover:text-[#105B48] dark:text-[#EDF4F7] dark:group-hover:text-[#00D7C7]">
                  {tool.title}
                </div>
                <div className="mt-0.5 text-xs leading-5 text-[#546963] dark:text-[#9EB3C4]">
                  {tool.desc}
                </div>
              </div>
            </button>
          ))}
        </div>
      </section>

      {/* Today's Status & 24h Fever Timeline */}
      <div className="grid grid-cols-1 gap-5 lg:grid-cols-12">
        <div className="lg:col-span-8">
          <FeverDoseTimelineCard compact />
        </div>

        <div className="space-y-4 lg:col-span-4">
          <section className="rounded-3xl border border-[#DCE7E1] bg-white p-5 space-y-3 dark:border-[#23344D] dark:bg-[#111C2E]">
            <h3 className="text-sm font-extrabold text-[#122823] dark:text-[#EDF4F7]">خلاصه پایش ایمنی</h3>

            <div className="rounded-2xl bg-[#F5F9F6] p-3 text-xs space-y-1 dark:bg-[#16243B]">
              <div className="font-bold text-[#105B48] dark:text-[#00D7C7]">برنامه درمانی امروز</div>
              <div className="tabular-nums text-[#122823] dark:text-[#EDF4F7]">
                {toPersianDigits(slotCounts.completedTodaySlots)} از {toPersianDigits(slotCounts.totalDailySlots)} نوبت ثبت شده
              </div>
              <button
                type="button"
                onClick={() => navigate({ name: 'treatment-plan' })}
                className="pt-1 font-bold text-[#105B48] hover:underline dark:text-[#00D7C7]"
              >
                باز کردن کارت نسخه
              </button>
            </div>

            <div className="rounded-2xl bg-[#F5F9F6] p-3 text-xs space-y-1 dark:bg-[#16243B]">
              <div className="font-bold text-[#105B48] dark:text-[#00D7C7]">وضعیت واکسیناسیون</div>
              <div className="text-[#122823] dark:text-[#EDF4F7]">
                {vaccineSummary.nextUpcoming
                  ? `موعد بعدی: ${vaccineSummary.nextUpcoming.definition.titleFa}`
                  : 'تاریخ تولد کودک را برای زمان‌بندی واکسن‌ها ثبت کنید'}
              </div>
              <button
                type="button"
                onClick={() => navigate({ name: 'vaccines' })}
                className="pt-1 font-bold text-[#105B48] hover:underline dark:text-[#00D7C7]"
              >
                مشاهده جدول واکسن‌ها
              </button>
            </div>
          </section>
        </div>
      </div>

      {/* Additional Clinical & Educational Hubs */}
      <section className="rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]">
        <h2 className="mb-3 text-sm font-extrabold text-[#122823] dark:text-[#EDF4F7]">
          سایر بخش‌های تخصصی و راهنماهای خانگی
        </h2>
        <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-4">
          {secondarySections.map((sec) => (
            <button
              key={sec.label}
              type="button"
              onClick={() => (sec.onClick ? sec.onClick() : sec.route && navigate(sec.route))}
              className="flex min-h-[48px] items-center gap-2 rounded-2xl border border-[#DCE7E1] bg-[#FAF7F0]/50 px-3 py-2.5 text-right text-xs font-bold text-[#122823] hover:border-[#105B48] hover:bg-[#E9F5EB]/60 dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#EDF4F7] dark:hover:border-[#00D7C7]"
            >
              <sec.Icon className="h-4 w-4 shrink-0 text-[#105B48] dark:text-[#00D7C7]" />
              <span className="truncate">{sec.label}</span>
            </button>
          ))}
        </div>
      </section>

      <NonDrugFeverCareSheet open={comfortSheetOpen} onClose={() => setComfortSheetOpen(false)} />
    </div>
  );
};
