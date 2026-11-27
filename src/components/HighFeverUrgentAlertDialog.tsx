import React from 'react';
import { PhoneCall, ShieldAlert, Stethoscope, Calculator, X } from 'lucide-react';
import { useMano } from '../state/ManoStore.tsx';
import { formatTempC } from '../domain/feverDoseTimelineEngine.ts';
import { resolveClinicalAge } from '../domain/clinicalAge.ts';

export const HighFeverUrgentAlertDialog: React.FC = () => {
  const { activeHighFeverAlert, dismissHighFeverAlert, childProfile, vaccineBirthDateEpochMs, navigate } = useMano();

  if (!activeHighFeverAlert) return null;

  const age = resolveClinicalAge(childProfile, vaccineBirthDateEpochMs);
  const ageUnconfirmed = age.status !== 'known';
  const isYoungInfant = age.status === 'known' && age.ageMonths! < 3;

  return (
    <div
      role="alertdialog"
      aria-modal="true"
      aria-label="هشدار دمای بالا ثبت شد"
      className="fixed inset-0 z-50 flex items-center justify-center bg-[#0B1220]/75 p-4 backdrop-blur-sm"
    >
      <div className="w-full max-w-lg rounded-3xl border border-[#E7B5BC] bg-[#FFFFFF] p-6 shadow-2xl dark:border-[#6A2C3B] dark:bg-[#111C2E]">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#FDF0F1] text-[#B32334] dark:bg-[#3A1A23] dark:text-[#FF8594]">
              <ShieldAlert className="h-6 w-6" />
            </span>
            <div>
              <h2 className="text-base font-extrabold text-[#B32334] dark:text-[#FF8594]">
                ثبت دمای بالا ({formatTempC(activeHighFeverAlert.tempCelsius)})
              </h2>
              <p className="text-xs text-[#546963] dark:text-[#9EB3C4]">
                پایش هوشیاری، تنفس و آبرسانی کودک در اولویت است
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={dismissHighFeverAlert}
            aria-label="بستن هشدار"
            className="flex min-h-[44px] min-w-[44px] items-center justify-center rounded-xl text-[#546963] hover:bg-[#F5F9F6] dark:text-[#9EB3C4] dark:hover:bg-[#16243B]"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="mt-4 space-y-3 rounded-2xl bg-[#FDF0F1] p-4 text-sm leading-6 text-[#4C1119] dark:bg-[#2F161E] dark:text-[#FFDDE2]">
          {isYoungInfant ? (
            <p className="font-bold">
              هشدار اورژانس نوزاد زیر ۳ ماه: هر تب ۳۸٫۰ درجه یا بالاتر در نوزاد زیر ۳ ماه نیازمند معاینه فوری حضوری در اورژانس است. بدون نظر پزشک هیچ دارویی ندهید.
            </p>
          ) : ageUnconfirmed ? (
            <p className="font-bold">سن کودک تأیید نشده یا با تاریخ تولد ناسازگار است. تب ۳۸ درجه یا بیشتر می‌تواند در شیرخوار زیر ۳ ماه نیازمند ارزیابی فوری باشد؛ برای معاینه حضوری با پزشک یا اورژانس تماس بگیرید و خودسرانه دارو ندهید.</p>
          ) : (
            <p>
              این عدد تب همراه با حال عمومی کودک مهم است. اگر کودک دچار تنگی نفس، کبودی لب‌ها، تشنج، بی‌حالی شدید یا راش محونشونده است، فوراً با اورژانس ۱۱۵ تماس بگیرید یا مراجعه حضوری داشته باشید.
            </p>
          )}
        </div>

        <div className="mt-5 grid grid-cols-1 gap-2.5 sm:grid-cols-2">
          <a
            href="tel:115"
            className="inline-flex min-h-[44px] items-center justify-center gap-2 rounded-xl bg-[#B32334] px-4 text-sm font-bold text-white hover:bg-[#921B29]"
          >
            <PhoneCall className="h-4 w-4" />
            <span>تماس با اورژانس ۱۱۵</span>
          </a>

          <button
            type="button"
            onClick={() => {
              dismissHighFeverAlert();
              navigate({ name: 'symptoms', initialFlowId: 'fever' });
            }}
            className="inline-flex min-h-[44px] items-center justify-center gap-2 rounded-xl border border-[#105B48] px-4 text-sm font-bold text-[#105B48] hover:bg-[#E9F5EB] dark:border-[#00D7C7] dark:text-[#00D7C7] dark:hover:bg-[#0F333A]"
          >
            <Stethoscope className="h-4 w-4" />
            <span>بررسی علائم خطر تب</span>
          </button>

          {!isYoungInfant && !ageUnconfirmed ? (
            <button
              type="button"
              onClick={() => {
                dismissHighFeverAlert();
                navigate({ name: 'fever-dose' });
              }}
              className="sm:col-span-2 inline-flex min-h-[44px] items-center justify-center gap-2 rounded-xl bg-[#105B48] px-4 text-sm font-bold text-white hover:bg-[#0C493A] dark:bg-[#00D7C7] dark:text-[#0B1220]"
            >
              <Calculator className="h-4 w-4" />
              <span>محاسبه‌گر ایمن تب‌بر بر پایه وزن</span>
            </button>
          ) : null}
        </div>
      </div>
    </div>
  );
};
