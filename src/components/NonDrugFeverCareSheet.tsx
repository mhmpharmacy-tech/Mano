import React, { useRef } from 'react';
import { Droplets, ThermometerSun, X } from 'lucide-react';
import { SeverityAlert } from './SeverityAlert.tsx';
import { AccessibleDialog } from './AccessibleDialog.tsx';

interface NonDrugFeverCareSheetProps {
  open: boolean;
  onClose: () => void;
}

export const NonDrugFeverCareSheet: React.FC<NonDrugFeverCareSheetProps> = ({ open, onClose }) => {
  const titleRef = useRef<HTMLHeadingElement>(null);
  if (!open) return null;

  return (
    <AccessibleDialog
      id="mano-non-drug-fever-dialog"
      label="مراقبت‌های غیردارویی تب کودک"
      describedBy="mano-non-drug-fever-description"
      initialFocusRef={titleRef}
      onClose={onClose}
      closeOnBackdrop
      backdropClassName="fixed inset-0 z-50 flex items-center justify-center bg-[#0B1220]/70 p-4 backdrop-blur-sm"
      panelClassName="max-h-[calc(100dvh-2rem)] w-full max-w-xl overflow-y-auto overscroll-contain rounded-3xl border border-[#DCE7E1] bg-[#FFFFFF] p-5 shadow-2xl outline-none dark:border-[#23344D] dark:bg-[#111C2E] sm:p-6"
    >
        <p id="mano-non-drug-fever-description" className="sr-only">مرور روش‌های مراقبت و هشدارها. برای بستن، کلید Escape را بزنید.</p>
        <div className="flex items-center justify-between border-b border-[#DCE7E1] pb-3 dark:border-[#23344D]">
          <div className="flex items-center gap-2.5">
            <ThermometerSun className="h-5 w-5 text-[#105B48] dark:text-[#00D7C7]" />
            <h2 ref={titleRef} tabIndex={-1} className="text-base font-extrabold text-[#122823] outline-none dark:text-[#EDF4F7]">
              مراقبت‌های غیردارویی تب کودک
            </h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="بستن"
            className="flex min-h-[44px] min-w-[44px] items-center justify-center rounded-xl text-[#546963] hover:bg-[#F5F9F6] dark:text-[#9EB3C4] dark:hover:bg-[#16243B]"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="mt-4 space-y-4 text-sm leading-6 text-[#122823] dark:text-[#EDF4F7]">
          <div className="rounded-2xl bg-[#F5F9F6] p-4 dark:bg-[#16243B]">
            <h3 className="mb-2 flex items-center gap-2 font-bold text-[#105B48] dark:text-[#00D7C7]">
              <Droplets className="h-4 w-4" />
              <span>کارهای ایمن و کمک‌کننده:</span>
            </h3>
            <ul className="list-disc space-y-1.5 pr-5">
              <li>کودک را یک لایه لباس نخی سبک بپوشانید؛ هرگز چند پتو روی کودک لرزدار نیندازید.</li>
              <li>مایعات مناسب سن (شیر مادر، شیرخشک، آب یا ORS در صورت نیاز) را جرعه‌جرعه و مکرر پیشنهاد دهید.</li>
              <li>دمای اتاق را معتدل و آرام نگه دارید و تعداد پوشک خیس یا ادرار کودک را زیر نظر بگیرید.</li>
              <li>اگر تن‌شویه ملایم انجام می‌دهید فقط با آب ولرم باشد و در صورت لرز یا بی‌قراری شدید کودک متوقف شود.</li>
            </ul>
          </div>

          <SeverityAlert
            severity="danger"
            title="روش‌های ممنوع و خطرناک"
            bullets={[
              'هرگز از الکل، سرکه یا آب یخ برای پاشویه استفاده نکنید (خطر مسمومیت پوستی/تنفسی و لرز شدید).',
              'هرگز آسپرین به کودک یا نوجوان ندهید (خطر سندرم ری).',
              'تب ۳۸ درجه یا بالاتر در نوزاد زیر ۳ ماه نیازمند معاینه فوری اورژانسی است، نه صرفاً مراقبت خانگی.',
            ]}
          />

          <button
            type="button"
            onClick={onClose}
            className="min-h-[44px] w-full rounded-xl bg-[#105B48] px-4 text-sm font-bold text-white hover:bg-[#0C493A] dark:bg-[#00D7C7] dark:text-[#0B1220]"
          >
            متوجه شدم
          </button>
        </div>
    </AccessibleDialog>
  );
};
