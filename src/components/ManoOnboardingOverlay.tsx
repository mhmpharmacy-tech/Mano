import React, { useLayoutEffect, useRef, useState } from 'react';
import { ArrowLeft, Check, Info } from 'lucide-react';
import { useMano } from '../state/ManoStore.tsx';
import { AccessibleDialog } from './AccessibleDialog.tsx';

const SLIDES = [
  {
    titleFa: 'آشنایی با مانو کید',
    subtitleFa: 'پیش‌نمایشی آموزشی برای مرور موضوعات مراقبت کودک و آشنایی با ابزارها.',
    image: '/assets/images/mano_pose_welcome.png',
    bulletsFa: [
      'این نسخه برای تصمیم درمانی یا ثبت اطلاعات واقعی بیمار نیست.',
      'می‌توانید این آشنایی را رد کنید و مستقیم به بخش‌ها بروید.',
    ],
  },
  {
    titleFa: 'ابزارها را با اطلاعات نمونه بشناسید',
    subtitleFa: 'راهنماهای علائم، ثبت‌های روزانه، رشد، واکسن و برچسب دارو را مرور کنید.',
    image: '/assets/images/mano_pose_explain.png',
    bulletsFa: [
      'محدودیت‌های ابزارها و هشدارهای ورودی را هنگام مرور در نظر بگیرید.',
      'تحلیل آنلاین تصویر و پاسخ هوش مصنوعی در این پیش‌نمایش در دسترس نیست.',
    ],
  },
  {
    titleFa: 'اطلاعات نمونه در همین مرورگر می‌ماند',
    subtitleFa: 'مانو کید جایگزین معاینه حضوری پزشک یا تماس با اورژانس ۱۱۵ نیست.',
    image: '/assets/images/mano_pose_caution.png',
    bulletsFa: [
      'ذخیره‌سازی این نسخه در مرورگر دستگاه است؛ حساب کاربری یا پروندهٔ درمانی نیست.',
      'در دستگاه مشترک، اطلاعات واقعی بیمار ثبت نکنید.',
    ],
  },
];

export const ManoOnboardingOverlay: React.FC = () => {
  const { onboardingSeen, completeOnboarding, activeHighFeverAlert } = useMano();
  const [step, setStep] = useState(0);
  const titleRef = useRef<HTMLHeadingElement>(null);

  useLayoutEffect(() => {
    titleRef.current?.focus();
  }, [step, onboardingSeen, activeHighFeverAlert]);

  if (onboardingSeen || activeHighFeverAlert) return null;

  const slide = SLIDES[step];
  const isLast = step === SLIDES.length - 1;

  return (
    <AccessibleDialog
      id="mano-onboarding-dialog"
      label="آشنایی با مانو کید"
      describedBy="mano-onboarding-description"
      initialFocusRef={titleRef}
      fallbackFocusId="mano-main-content"
      onClose={completeOnboarding}
      backdropClassName="fixed inset-0 z-50 flex items-center justify-center bg-[#0B1220]/70 p-4 backdrop-blur-sm"
      panelClassName="max-h-[calc(100dvh-2rem)] w-full max-w-lg overflow-y-auto rounded-3xl border border-[#DCE7E1] bg-[#FFFFFF] p-5 shadow-2xl outline-none dark:border-[#23344D] dark:bg-[#111C2E] sm:p-6"
    >
        <div className="sticky top-0 z-10 flex items-center justify-between gap-3 bg-white pb-2 dark:bg-[#111C2E]">
          <span aria-live="polite" className="text-xs font-semibold text-[#546963] dark:text-[#9EB3C4]">
            آشنایی اختیاری · گام {(step + 1).toLocaleString('fa-IR')} از {SLIDES.length.toLocaleString('fa-IR')}
          </span>
          <button
            type="button"
            onClick={completeOnboarding}
            data-testid="onboarding-skip-btn"
            className="min-h-[44px] shrink-0 rounded-xl border border-[#DCE7E1] px-3 text-xs font-bold text-[#105B48] hover:bg-[#F5F9F6] dark:border-[#23344D] dark:text-[#00D7C7] dark:hover:bg-[#16243B]"
          >
            رد کردن
          </button>
        </div>

        <div className="mt-3 flex flex-col items-center text-center">
          <div className="flex h-36 w-36 items-center justify-center rounded-3xl bg-[#E9F5EB] p-3 dark:bg-[#0F333A]">
            <img
              src={slide.image}
              alt=""
              className="h-full w-full object-contain"
              referrerPolicy="no-referrer"
            />
          </div>

          <h2 ref={titleRef} tabIndex={-1} className="mt-4 text-xl font-extrabold text-[#122823] outline-none dark:text-[#EDF4F7]">
            {slide.titleFa}
          </h2>
          <p id="mano-onboarding-description" className="mt-2 text-sm leading-6 text-[#546963] dark:text-[#9EB3C4]">{slide.subtitleFa}</p>

          <ul className="mt-4 w-full space-y-2 rounded-2xl bg-[#F5F9F6] p-4 text-right text-sm text-[#122823] dark:bg-[#16243B] dark:text-[#EDF4F7]">
            {slide.bulletsFa.map((b, idx) => (
              <li key={idx} className="flex items-start gap-2">
                <Info aria-hidden="true" className="mt-1 h-4 w-4 shrink-0 text-[#105B48] dark:text-[#00D7C7]" />
                <span>{b}</span>
              </li>
            ))}
          </ul>
        </div>

        <div className="mt-6 flex items-center justify-between gap-3">
          <div className="flex items-center gap-1.5" aria-hidden="true">
            {SLIDES.map((_, i) => (
              <span
                key={i}
                className={`h-2 rounded-full transition-all ${
                  i === step
                    ? 'w-6 bg-[#105B48] dark:bg-[#00D7C7]'
                    : 'w-2 bg-[#DCE7E1] dark:bg-[#23344D]'
                }`}
              />
            ))}
          </div>

          <div className="flex items-center gap-2">
            {step > 0 ? (
              <button
                type="button"
                onClick={() => setStep((s) => s - 1)}
                className="min-h-[44px] rounded-xl border border-[#DCE7E1] px-4 text-sm font-semibold text-[#122823] hover:bg-[#F5F9F6] dark:border-[#23344D] dark:text-[#EDF4F7] dark:hover:bg-[#16243B]"
              >
                قبلی
              </button>
            ) : null}

            {isLast ? (
              <button
                type="button"
                onClick={completeOnboarding}
                data-testid="onboarding-finish-btn"
                className="inline-flex min-h-[44px] items-center gap-2 rounded-xl bg-[#105B48] px-5 text-sm font-bold text-white hover:bg-[#0C493A] dark:bg-[#00D7C7] dark:text-[#0B1220]"
              >
                <Check className="h-4 w-4" />
                <span>ورود به برنامه</span>
              </button>
            ) : (
              <button
                type="button"
                onClick={() => setStep((s) => s + 1)}
                data-testid="onboarding-next-btn"
                className="inline-flex min-h-[44px] items-center gap-2 rounded-xl bg-[#105B48] px-5 text-sm font-bold text-white hover:bg-[#0C493A] dark:bg-[#00D7C7] dark:text-[#0B1220]"
              >
                <span>ادامه</span>
                <ArrowLeft className="h-4 w-4" />
              </button>
            )}
          </div>
        </div>
    </AccessibleDialog>
  );
};
