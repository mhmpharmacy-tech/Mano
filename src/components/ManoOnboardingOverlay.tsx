import React, { useState } from 'react';
import { ArrowLeft, Check, ShieldCheck } from 'lucide-react';
import { useMano } from '../state/ManoStore.tsx';

const SLIDES = [
  {
    titleFa: 'همراه آرام والدین در مراقبت علمی کودک',
    subtitleFa: 'مانو کید راهنمای کاربردی تب، دوز ایمن داروهای مایع، رشد، واکسیناسیون و علائم کودکان است.',
    image: '/assets/images/mano_pose_welcome.png',
    bulletsFa: [
      'تمرکز بر ایمنی نوزاد و کودک با قوانین سخت‌گیرانه سن و وزن',
      'بر پایه منابع معتبر AAP، WHO، NICE و کتب مرجع داروسازی اطفال',
    ],
  },
  {
    titleFa: 'محاسبه دقیق میلی‌لیتر بدون خطای قاشق',
    subtitleFa: 'تفاوت غلظت قطره (۱۰۰mg/1mL) و شربت‌ها را با تصویر سرنگ مدرج و اسکنر برچسب بررسی کنید.',
    image: '/assets/images/mano_pose_explain.png',
    bulletsFa: [
      'قفل خودکار ایبوپروفن زیر ۶ ماه و تمام دوزها برای نوزاد زیر ۳ ماه',
      'ثبت ۲۴ ساعته دما و زمان نوبت دارو برای جلوگیری از تکرار ناخواسته',
    ],
  },
  {
    titleFa: 'مرز روشن آموزش با تشخیص پزشکی',
    subtitleFa: 'مانو کید جایگزین معاینه حضوری پزشک یا تماس با اورژانس ۱۱۵ نیست.',
    image: '/assets/images/mano_pose_caution.png',
    bulletsFa: [
      'پرچم‌های قرمز (تب نوزاد زیر ۳ ماه، تنگی نفس، کبودی) بلافاصله نمایش داده می‌شوند',
      'اطلاعات کودک به‌صورت خصوصی در حافظه همین دستگاه نگهداری می‌شود',
    ],
  },
];

export const ManoOnboardingOverlay: React.FC = () => {
  const { onboardingSeen, completeOnboarding } = useMano();
  const [step, setStep] = useState(0);

  if (onboardingSeen) return null;

  const slide = SLIDES[step];
  const isLast = step === SLIDES.length - 1;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="آشنایی با مانو کید"
      className="fixed inset-0 z-50 flex items-center justify-center bg-[#0B1220]/70 p-4 backdrop-blur-sm"
    >
      <div className="w-full max-w-lg rounded-3xl border border-[#DCE7E1] bg-[#FFFFFF] p-6 shadow-2xl dark:border-[#23344D] dark:bg-[#111C2E]">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-[#546963] dark:text-[#9EB3C4]">
            آشنایی کوتاه · گام {step + 1} از {SLIDES.length}
          </span>
          <button
            type="button"
            onClick={completeOnboarding}
            data-testid="onboarding-skip-btn"
            className="min-h-[44px] px-3 text-xs font-semibold text-[#546963] hover:text-[#105B48] dark:text-[#9EB3C4] dark:hover:text-[#00D7C7]"
          >
            رد کردن
          </button>
        </div>

        <div className="mt-3 flex flex-col items-center text-center">
          <div className="flex h-36 w-36 items-center justify-center rounded-3xl bg-[#E9F5EB] p-3 dark:bg-[#0F333A]">
            <img
              src={slide.image}
              alt={slide.titleFa}
              className="h-full w-full object-contain"
              referrerPolicy="no-referrer"
            />
          </div>

          <h2 className="mt-4 text-xl font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            {slide.titleFa}
          </h2>
          <p className="mt-2 text-sm leading-6 text-[#546963] dark:text-[#9EB3C4]">{slide.subtitleFa}</p>

          <ul className="mt-4 w-full space-y-2 rounded-2xl bg-[#F5F9F6] p-4 text-right text-sm text-[#122823] dark:bg-[#16243B] dark:text-[#EDF4F7]">
            {slide.bulletsFa.map((b, idx) => (
              <li key={idx} className="flex items-start gap-2">
                <ShieldCheck className="mt-1 h-4 w-4 shrink-0 text-[#105B48] dark:text-[#00D7C7]" />
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
      </div>
    </div>
  );
};
