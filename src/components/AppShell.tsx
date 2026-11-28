import React, { useEffect, useRef, useState } from 'react';
import {
  Calculator,
  Home,
  LayoutGrid,
  Moon,
  Scale,
  Search,
  Stethoscope,
  Sun,
  X,
} from 'lucide-react';
import { useMano, type AppRoute } from '../state/ManoStore.tsx';
import { FEATURES, FEATURE_GROUPS } from '../navigation/featureCatalog.ts';
import { FEATURE_ICONS } from './featureIcons.ts';
import { AccessibleDialog } from './AccessibleDialog.tsx';
import { GlobalSearchModal } from './GlobalSearchModal.tsx';
import { HighFeverUrgentAlertDialog } from './HighFeverUrgentAlertDialog.tsx';
import { ManoOnboardingOverlay } from './ManoOnboardingOverlay.tsx';

export const AppShell: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const {
    route, navigate, theme, toggleTheme, searchOpen, setSearchOpen, toasts,
    persistenceIssue, onboardingSeen, activeHighFeverAlert,
  } = useMano();
  const [hubOpen, setHubOpen] = useState(false);
  const hubVisible = hubOpen && onboardingSeen && !searchOpen && !activeHighFeverAlert;

  const headerRef = useRef<HTMLElement>(null);
  const mobileNavRef = useRef<HTMLElement>(null);
  useEffect(() => {
    // Firefox can scroll a focused control underneath sticky/fixed chrome.
    // Measure actual chrome (including large-text reflow) rather than fixed pixels.
    const root = document.documentElement;
    const header = headerRef.current;
    const nav = mobileNavRef.current;
    if (!header || !nav) return;
    let frame = 0;
    const measure = () => {
      root.style.setProperty('--mano-focus-top', `${Math.ceil(header.getBoundingClientRect().height) + 4}px`);
      root.style.setProperty('--mano-focus-bottom', `${Math.ceil(nav.getBoundingClientRect().height) + 4}px`);
    };
    const reveal = () => {
      const target = document.activeElement;
      const main = document.getElementById('mano-main-content');
      if (!(target instanceof HTMLElement) || target === main || !main?.contains(target) ||
          target.closest('[role="dialog"]')) return;
      const rect = target.getBoundingClientRect();
      const top = Math.max(0, header.getBoundingClientRect().bottom) + 4;
      const navRect = nav.getBoundingClientRect();
      const bottom = (navRect.height ? navRect.top : window.innerHeight) - 4;
      if (bottom <= top) return;
      const delta = rect.height > bottom - top || rect.top < top
        ? rect.top - top : rect.bottom > bottom ? rect.bottom - bottom : 0;
      if (Math.abs(delta) > 0.5) window.scrollBy({ top: delta, behavior: 'instant' });
    };
    const onFocus = () => {
      cancelAnimationFrame(frame);
      reveal();
      frame = requestAnimationFrame(reveal);
    };
    const observer = new ResizeObserver(measure);
    observer.observe(header); observer.observe(nav);
    measure();
    document.addEventListener('focusin', onFocus);
    return () => {
      cancelAnimationFrame(frame); observer.disconnect();
      document.removeEventListener('focusin', onFocus);
      root.style.removeProperty('--mano-focus-top');
      root.style.removeProperty('--mano-focus-bottom');
    };
  }, []);

  const isNavActive = (name: AppRoute['name']) => route.name === name;

  const navLinkClass = (active: boolean) =>
    `whitespace-nowrap shrink-0 text-sm font-semibold transition-colors underline-offset-8 ${
      active
        ? 'text-[#105B48] underline decoration-2 dark:text-[#00D7C7]'
        : 'text-[#546963] hover:text-[#122823] hover:underline dark:text-[#9EB3C4] dark:hover:text-[#EDF4F7]'
    }`;

  return (
    <div className="min-h-screen bg-[#FFFBF6] text-[#122823] dark:bg-[#0B1220] dark:text-[#EDF4F7]">
      <a
        href="#mano-main-content"
        data-testid="skip-to-main-link"
        onClick={(event) => {
          event.preventDefault();
          document.getElementById('mano-main-content')?.focus();
        }}
        className="sr-only right-4 top-3 z-[60] rounded-xl bg-[#105B48] text-sm font-bold text-white focus:not-sr-only focus:fixed focus:px-4 focus:py-3 dark:bg-[#00D7C7] dark:text-[#0B1220]"
      >
        رفتن به محتوای اصلی
      </a>
      {/* Strict 3-Zone Top Header Bar */}
      <header ref={headerRef} className="sticky top-0 z-30 border-b border-[#DCE7E1] bg-[#FFFBF6]/92 backdrop-blur-md dark:border-[#23344D] dark:bg-[#0B1220]/92">
        <div className="mx-auto flex min-h-14 max-w-6xl flex-wrap items-center justify-between gap-2 px-4 py-2 sm:px-6">
          {/* Zone 1: Single-line Brand Title */}
          <button
            type="button"
            onClick={() => navigate({ name: 'home' })}
            className="font-display-fa whitespace-nowrap shrink-0 text-xl tracking-tight text-[#105B48] dark:text-[#00D7C7]"
          >
            مانو کید
          </button>

          {/* Zone 2: 5 concise single-line text nav links (Desktop) */}
          <nav aria-label="ناوبری اصلی" className="hidden flex-wrap items-center justify-center gap-x-4 gap-y-2 md:flex">
            <button type="button" onClick={() => navigate({ name: 'home' })} aria-current={isNavActive('home') ? 'page' : undefined} className={navLinkClass(isNavActive('home'))}>
              خانه
            </button>
            <button type="button" onClick={() => navigate({ name: 'symptoms' })} aria-current={isNavActive('symptoms') ? 'page' : undefined} className={navLinkClass(isNavActive('symptoms'))}>
              بررسی علائم
            </button>
            <button type="button" onClick={() => navigate({ name: 'fever-dose' })} aria-current={isNavActive('fever-dose') ? 'page' : undefined} className={navLinkClass(isNavActive('fever-dose'))}>
              محاسبه‌گر تب
            </button>
            <button type="button" onClick={() => navigate({ name: 'vaccines' })} aria-current={isNavActive('vaccines') ? 'page' : undefined} className={navLinkClass(isNavActive('vaccines'))}>
              واکسیناسیون
            </button>
            <button type="button" onClick={() => navigate({ name: 'medicines' })} aria-current={isNavActive('medicines') || isNavActive('medicine-detail') ? 'page' : undefined} className={navLinkClass(isNavActive('medicines') || isNavActive('medicine-detail'))}>
              دارو و راهنما
            </button>
          </nav>

          {/* Zone 3: Single Primary Action */}
          <div className="shrink-0">
            <button
              type="button"
              onClick={() => setHubOpen(true)}
              data-testid="open-hub-drawer-btn"
              aria-haspopup="dialog"
              aria-expanded={Boolean(hubVisible)}
              aria-controls={hubVisible ? 'mano-tools-dialog' : undefined}
              className="inline-flex min-h-[40px] items-center gap-2 whitespace-nowrap rounded-xl bg-[#105B48] px-4 text-xs font-bold text-white hover:bg-[#0C493A] dark:bg-[#00D7C7] dark:text-[#0B1220]"
            >
              <LayoutGrid className="h-4 w-4" />
              <span>ابزارها و پرونده</span>
            </button>
          </div>
        </div>
      </header>

      <div className="border-b border-[#DCE7E1] bg-[#FDF4E4] px-4 py-2 text-center text-[11px] leading-5 text-[#122823] dark:border-[#23344D] dark:bg-[#312617] dark:text-[#EDF4F7]" role="note">
        <p>پیش‌نمایش آموزشی؛ برای تصمیم درمانی یا اطلاعات واقعی بیمار استفاده نشود.</p>
        <details className="mx-auto max-w-3xl">
          <summary className="cursor-pointer font-semibold underline underline-offset-4">درباره این پیش‌نمایش</summary>
          <p className="pt-1">اطلاعات نمونه در مرورگر همین دستگاه ذخیره می‌شود. پاسخ هوش مصنوعی و تحلیل آنلاین تصویر فعال نیستند؛ ابزارهای محلی قابل مرورند.</p>
        </details>
      </div>
      {persistenceIssue ? (
        <div role="alert" className="border-b border-[#E7B5BC] bg-[#FDF0F1] px-4 py-3 text-center text-xs leading-6 text-[#B32334]">
          {persistenceIssue}
        </div>
      ) : null}

      {/* Main Content Container */}
      <main id="mano-main-content" tabIndex={-1} className="mx-auto max-w-6xl px-4 pb-24 pt-5 outline-none sm:px-6 md:pb-12">{children}</main>

      {/* Mobile Bottom Tab Bar (5 tabs, compact 56px height) */}
      <nav
        ref={mobileNavRef}
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
              aria-current={tab.active ? 'page' : undefined}
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
            aria-haspopup="dialog"
            aria-expanded={Boolean(hubVisible)}
            aria-controls={hubVisible ? 'mano-tools-dialog' : undefined}
            className="flex flex-col items-center justify-center gap-0.5 text-[10px] font-bold text-[#546963] dark:text-[#9EB3C4]"
          >
            <LayoutGrid className="h-5 w-5" />
            <span>همه</span>
          </button>
        </div>
      </nav>

      {/* Slide-Over Hub Drawer */}
      {hubVisible ? (
        <AccessibleDialog
          id="mano-tools-dialog"
          label="فهرست کامل بخش‌های مانو کید"
          describedBy="mano-tools-description"
          onClose={() => setHubOpen(false)}
          closeOnBackdrop
          backdropClassName="fixed inset-0 z-50 flex justify-end bg-[#0B1220]/70 backdrop-blur-sm"
          panelClassName="flex h-full w-full max-w-md flex-col border-r border-[#DCE7E1] bg-[#FFFFFF] outline-none dark:border-[#23344D] dark:bg-[#111C2E]"
        >
            <div className="flex items-center justify-between border-b border-[#DCE7E1] px-5 py-4 dark:border-[#23344D]">
              <div>
                <h2 className="text-base font-extrabold text-[#122823] dark:text-[#EDF4F7]">
                  ابزارها و پرونده
                </h2>
                <p id="mano-tools-description" className="mt-1 text-xs leading-5 text-[#546963] dark:text-[#9EB3C4]">ابزارهای آموزشی و ثبت‌های همین دستگاه</p>
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
                <span>جست‌وجوی مطالب</span>
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

            <div className="min-h-0 flex-1 space-y-5 overflow-y-auto p-4">
              {FEATURE_GROUPS.map((group) => (
                <section key={group.id} aria-labelledby={`mano-tools-group-${group.id}`}>
                  <h3 id={`mano-tools-group-${group.id}`} className="mb-2 text-xs font-bold text-[#546963] dark:text-[#9EB3C4]">{group.titleFa}</h3>
                  <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                  {group.featureIds.map((id) => {
                    const dest = FEATURES[id];
                    const Icon = FEATURE_ICONS[dest.iconKey];
                    return (
                  <button
                    key={id}
                    type="button"
                    data-testid={`hub-destination-${id}`}
                    aria-current={isNavActive(dest.route.name) ? 'page' : undefined}
                    onClick={() => {
                      setHubOpen(false);
                      navigate(dest.route);
                    }}
                    className="flex items-start gap-2.5 rounded-2xl border border-[#DCE7E1] bg-[#FAF7F0]/40 p-3 text-right hover:border-[#105B48] hover:bg-[#E9F5EB]/50 dark:border-[#23344D] dark:bg-[#16243B]/60 dark:hover:border-[#00D7C7]"
                  >
                    <Icon className="mt-0.5 h-5 w-5 shrink-0 text-[#105B48] dark:text-[#00D7C7]" />
                    <div className="min-w-0">
                      <div className="text-xs font-bold text-[#122823] dark:text-[#EDF4F7]">{dest.labelFa}</div>
                      <div className="mt-0.5 text-[11px] leading-5 text-[#546963] dark:text-[#9EB3C4]">{dest.descriptionFa}</div>
                    </div>
                  </button>
                    );
                  })}
                  </div>
                </section>
              ))}
            </div>
        </AccessibleDialog>
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
