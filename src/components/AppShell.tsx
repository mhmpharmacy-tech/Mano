import React, { useState } from 'react';
import {
  Baby,
  BookOpen,
  Calculator,
  ClipboardList,
  Cpu,
  HelpCircle,
  Home,
  LayoutGrid,
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
  X,
} from 'lucide-react';
import { useMano, type AppRoute } from '../state/ManoStore.tsx';
import { GlobalSearchModal } from './GlobalSearchModal.tsx';
import { HighFeverUrgentAlertDialog } from './HighFeverUrgentAlertDialog.tsx';
import { ManoOnboardingOverlay } from './ManoOnboardingOverlay.tsx';

interface HubDestination {
  labelFa: string;
  descFa: string;
  route: AppRoute;
  Icon: React.FC<{ className?: string }>;
}

const ALL_HUB_DESTINATIONS: HubDestination[] = [
  { labelFa: 'بررسی علائم کودک', descFa: 'ارزیابی تب، سرفه، گوش، چشم و گوارش', route: { name: 'symptoms' }, Icon: Stethoscope },
  { labelFa: 'محاسبه‌گر ایمن تب‌بر', descFa: 'حجم میلی‌لیتر بر پایه وزن و سرنگ مدرج', route: { name: 'fever-dose' }, Icon: Calculator },
  { labelFa: 'اسکنر هوشمند شربت', descFa: 'تشخیص غلظت بطری و هشدار قطره/شربت', route: { name: 'syrup-scanner' }, Icon: ScanLine },
  { labelFa: 'برنامه درمانی روزانه', descFa: 'پیگیری نوبت‌های نسخه با داروساز', route: { name: 'treatment-plan' }, Icon: ClipboardList },
  { labelFa: 'جدول واکسیناسیون', descFa: 'برنامه ایمن‌سازی کشوری و یادآور', route: { name: 'vaccines' }, Icon: Syringe },
  { labelFa: 'رشد و تکامل WHO', descFa: 'منحنی صدک قد و وزن و نقاط عطف', route: { name: 'growth' }, Icon: Scale },
  { labelFa: 'مشاور تغذیه کودک', descFa: 'برنامه غذایی بومی +۶ ماه با فیلتر ایمنی', route: { name: 'nutrition' }, Icon: Utensils },
  { labelFa: 'دارونامه و تداخل', descFa: '۱۰ تک‌نگاشت دارو و چک تداخل', route: { name: 'medicines' }, Icon: Pill },
  { labelFa: 'مکمل و بهداشت', descFa: 'ویتامین D، آهن، پروبیوتیک و بهداشت', route: { name: 'supplements-hygiene' }, Icon: Sparkles },
  { labelFa: 'پوست و سوختگی پوشک', descFa: 'مراقبت ملایم پوست و مقایسه علمی', route: { name: 'skin' }, Icon: Baby },
  { labelFa: 'تجهیزات خانگی کودک', descFa: 'اسپیسر، نبولایزر، دماسنج و سرنگ', route: { name: 'devices' }, Icon: Cpu },
  { labelFa: 'کتابخانه مقالات', descFa: 'راهنماهای مستند مراقبت از کودک', route: { name: 'topics' }, Icon: BookOpen },
  { labelFa: 'پرسش‌های متداول و AI', descFa: '۳۴ پرسش متداول + پاسخ مستند هوشمند', route: { name: 'faq' }, Icon: HelpCircle },
  { labelFa: 'پرسش از داروساز', descFa: 'فرم مشاوره و میز محلی راهنما', route: { name: 'ask-pharmacist' }, Icon: UserCheck },
  { labelFa: 'کارت‌های مراقبت', descFa: 'ساخت خلاصه و دعوت‌نامه داروخانه', route: { name: 'care-cards' }, Icon: QrCode },
  { labelFa: 'پرونده کودک من', descFa: 'سن، وزن، حساسیت و تاریخچه ذخیره‌شده', route: { name: 'my-child' }, Icon: Baby },
];

export const AppShell: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { route, navigate, theme, toggleTheme, setSearchOpen, toasts } = useMano();
  const [hubOpen, setHubOpen] = useState(false);

  const isNavActive = (name: AppRoute['name']) => route.name === name;

  const navLinkClass = (active: boolean) =>
    `whitespace-nowrap shrink-0 text-sm font-semibold transition-colors underline-offset-8 ${
      active
        ? 'text-[#105B48] underline decoration-2 dark:text-[#00D7C7]'
        : 'text-[#546963] hover:text-[#122823] hover:underline dark:text-[#9EB3C4] dark:hover:text-[#EDF4F7]'
    }`;

  return (
    <div className="min-h-screen bg-[#FFFBF6] text-[#122823] dark:bg-[#0B1220] dark:text-[#EDF4F7]">
      {/* Strict 3-Zone Top Header Bar */}
      <header className="sticky top-0 z-30 border-b border-[#DCE7E1] bg-[#FFFBF6]/92 backdrop-blur-md dark:border-[#23344D] dark:bg-[#0B1220]/92">
        <div className="mx-auto flex h-14 max-w-6xl items-center justify-between gap-8 px-4 sm:px-6">
          {/* Zone 1: Single-line Brand Title */}
          <button
            type="button"
            onClick={() => navigate({ name: 'home' })}
            className="font-display-fa whitespace-nowrap shrink-0 text-xl tracking-tight text-[#105B48] dark:text-[#00D7C7]"
          >
            مانو کید
          </button>

          {/* Zone 2: 5 concise single-line text nav links (Desktop) */}
          <nav aria-label="ناوبری اصلی" className="hidden md:flex items-center gap-6">
            <button type="button" onClick={() => navigate({ name: 'home' })} className={navLinkClass(isNavActive('home'))}>
              خانه
            </button>
            <button type="button" onClick={() => navigate({ name: 'symptoms' })} className={navLinkClass(isNavActive('symptoms'))}>
              بررسی علائم
            </button>
            <button type="button" onClick={() => navigate({ name: 'fever-dose' })} className={navLinkClass(isNavActive('fever-dose'))}>
              محاسبه‌گر تب
            </button>
            <button type="button" onClick={() => navigate({ name: 'vaccines' })} className={navLinkClass(isNavActive('vaccines'))}>
              واکسیناسیون
            </button>
            <button type="button" onClick={() => navigate({ name: 'medicines' })} className={navLinkClass(isNavActive('medicines') || isNavActive('medicine-detail'))}>
              دارو و راهنما
            </button>
          </nav>

          {/* Zone 3: Single Primary Action */}
          <div className="shrink-0">
            <button
              type="button"
              onClick={() => setHubOpen(true)}
              data-testid="open-hub-drawer-btn"
              className="inline-flex min-h-[40px] items-center gap-2 whitespace-nowrap rounded-xl bg-[#105B48] px-4 text-xs font-bold text-white hover:bg-[#0C493A] dark:bg-[#00D7C7] dark:text-[#0B1220]"
            >
              <LayoutGrid className="h-4 w-4" />
              <span>ابزارها و پرونده</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Content Container */}
      <main className="mx-auto max-w-6xl px-4 pb-24 pt-5 sm:px-6 md:pb-12">{children}</main>

      {/* Mobile Bottom Tab Bar (5 tabs, compact 56px height) */}
      <nav
        aria-label="ناوبری موبایل"
        className="fixed bottom-0 left-0 right-0 z-30 border-t border-[#DCE7E1] bg-[#FFFFFF]/95 backdrop-blur-md dark:border-[#23344D] dark:bg-[#111C2E]/95 md:hidden"
      >
        <div className="grid h-14 grid-cols-5">
          {[
            { label: 'خانه', route: { name: 'home' } as AppRoute, active: isNavActive('home'), Icon: Home },
            { label: 'علائم', route: { name: 'symptoms' } as AppRoute, active: isNavActive('symptoms'), Icon: Stethoscope },
            { label: 'تب‌بر', route: { name: 'fever-dose' } as AppRoute, active: isNavActive('fever-dose'), Icon: Calculator },
            { label: 'رشد', route: { name: 'growth' } as AppRoute, active: isNavActive('growth'), Icon: Scale },
          ].map((tab) => (
            <button
              key={tab.label}
              type="button"
              onClick={() => navigate(tab.route)}
              className={`flex flex-col items-center justify-center gap-0.5 text-[10px] font-bold ${
                tab.active
                  ? 'text-[#105B48] dark:text-[#00D7C7]'
                  : 'text-[#546963] dark:text-[#9EB3C4]'
              }`}
            >
              <tab.Icon className="h-5 w-5" />
              <span>{tab.label}</span>
            </button>
          ))}
          <button
            type="button"
            onClick={() => setHubOpen(true)}
            className="flex flex-col items-center justify-center gap-0.5 text-[10px] font-bold text-[#546963] dark:text-[#9EB3C4]"
          >
            <LayoutGrid className="h-5 w-5" />
            <span>همه</span>
          </button>
        </div>
      </nav>

      {/* Slide-Over Hub Drawer */}
      {hubOpen ? (
        <div
          role="dialog"
          aria-modal="true"
          aria-label="فهرست کامل بخش‌های مانو کید"
          className="fixed inset-0 z-50 flex justify-end bg-[#0B1220]/70 backdrop-blur-sm"
          onClick={() => setHubOpen(false)}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            className="flex h-full w-full max-w-md flex-col border-r border-[#DCE7E1] bg-[#FFFFFF] dark:border-[#23344D] dark:bg-[#111C2E]"
          >
            <div className="flex items-center justify-between border-b border-[#DCE7E1] px-5 py-4 dark:border-[#23344D]">
              <div>
                <h2 className="text-base font-extrabold text-[#122823] dark:text-[#EDF4F7]">
                  اکوسیستم ابزارهای مانو کید
                </h2>
                <p className="text-xs text-[#546963] dark:text-[#9EB3C4]">دسترسی سریع به تمام بخش‌های بالینی و مراقبتی</p>
              </div>
              <button
                type="button"
                onClick={() => setHubOpen(false)}
                aria-label="بستن منو"
                className="flex min-h-[44px] min-w-[44px] items-center justify-center rounded-xl text-[#546963] hover:bg-[#F5F9F6] dark:hover:bg-[#16243B]"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="flex items-center gap-2 border-b border-[#DCE7E1] px-5 py-3 dark:border-[#23344D]">
              <button
                type="button"
                onClick={() => {
                  setHubOpen(false);
                  setSearchOpen(true);
                }}
                data-testid="drawer-search-btn"
                className="flex min-h-[44px] flex-1 items-center gap-2 rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-xs font-bold text-[#122823] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#EDF4F7]"
              >
                <Search className="h-4 w-4 text-[#105B48] dark:text-[#00D7C7]" />
                <span>جست‌وجوی دانش بالینی</span>
              </button>

              <button
                type="button"
                onClick={toggleTheme}
                data-testid="theme-toggle-btn"
                aria-label={theme === 'dark' ? 'تغییر به حالت روشن' : 'تغییر به حالت تیره'}
                className="inline-flex min-h-[44px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3.5 text-xs font-bold text-[#122823] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#EDF4F7]"
              >
                {theme === 'dark' ? <Sun className="h-4 w-4 text-[#F0B85C]" /> : <Moon className="h-4 w-4 text-[#105B48]" />}
                <span>{theme === 'dark' ? 'حالت روشن' : 'حالت تیره'}</span>
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-4">
              <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                {ALL_HUB_DESTINATIONS.map((dest) => (
                  <button
                    key={dest.labelFa}
                    type="button"
                    onClick={() => {
                      setHubOpen(false);
                      navigate(dest.route);
                    }}
                    className="flex items-start gap-2.5 rounded-2xl border border-[#DCE7E1] bg-[#FAF7F0]/40 p-3 text-right hover:border-[#105B48] hover:bg-[#E9F5EB]/50 dark:border-[#23344D] dark:bg-[#16243B]/60 dark:hover:border-[#00D7C7]"
                  >
                    <dest.Icon className="mt-0.5 h-5 w-5 shrink-0 text-[#105B48] dark:text-[#00D7C7]" />
                    <div className="min-w-0">
                      <div className="text-xs font-bold text-[#122823] dark:text-[#EDF4F7]">{dest.labelFa}</div>
                      <div className="mt-0.5 text-[11px] leading-4 text-[#546963] dark:text-[#9EB3C4]">{dest.descFa}</div>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>
      ) : null}

      {/* Modals & Non-blocking Toast Host */}
      <ManoOnboardingOverlay />
      <GlobalSearchModal />
      <HighFeverUrgentAlertDialog />

      <div
        aria-live="polite"
        className="pointer-events-none fixed bottom-20 left-4 right-4 z-50 flex flex-col items-center gap-2 md:bottom-6"
      >
        {toasts.map((t) => (
          <div
            key={t.id}
            className={`pointer-events-auto max-w-md rounded-2xl border px-4 py-2.5 text-xs font-bold shadow-lg ${
              t.tone === 'danger'
                ? 'border-[#E7B5BC] bg-[#FDF0F1] text-[#B32334] dark:border-[#6A2C3B] dark:bg-[#2F161E] dark:text-[#FF8594]'
                : t.tone === 'success'
                  ? 'border-[#B6DFC1] bg-[#EBF7EE] text-[#105B48] dark:border-[#21594B] dark:bg-[#102B25] dark:text-[#35E0C9]'
                  : 'border-[#DCE7E1] bg-white text-[#122823] dark:border-[#23344D] dark:bg-[#111C2E] dark:text-[#EDF4F7]'
            }`}
          >
            {t.text}
          </div>
        ))}
      </div>
    </div>
  );
};
