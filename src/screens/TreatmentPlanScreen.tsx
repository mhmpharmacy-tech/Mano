import React, { useState } from 'react';
import {
  ArrowRight,
  Bell,
  CheckCircle2,
  ClipboardCopy,
  Edit3,
  Plus,
  Printer,
  RotateCcw,
  Trash2,
} from 'lucide-react';
import {
  TREATMENT_DOSAGE_FORMS,
  buildTreatmentCaregiverShareText,
  getTreatmentSlotCounts,
  resolvePlanChildSummary,
  type TreatmentMedicationItem,
} from '../domain/treatmentPlan.ts';
import { toPersianDigits } from '../domain/persianNormalizer.ts';
import { useMano } from '../state/ManoStore.tsx';
import { TreatmentPlanEditorModal } from '../components/TreatmentPlanEditorSheets.tsx';
import { SeverityAlert } from '../components/SeverityAlert.tsx';

export const TreatmentPlanScreen: React.FC = () => {
  const {
    treatmentPlan,
    childProfile,
    toggleTreatmentSlot,
    upsertTreatmentMedication,
    deleteTreatmentMedication,
    resetTreatmentToday,
    navigate,
    notify,
  } = useMano();

  const [editingItem, setEditingItem] = useState<TreatmentMedicationItem | null>(null);
  const [editorOpen, setEditorOpen] = useState(false);

  const counts = getTreatmentSlotCounts(treatmentPlan);
  const childSummary = resolvePlanChildSummary(treatmentPlan, childProfile);
  const legacyRows = treatmentPlan.legacyUnreviewedMedications ?? [];

  const handleCopy = async () => {
    const text = buildTreatmentCaregiverShareText(treatmentPlan, childProfile);
    try {
      await navigator.clipboard.writeText(text);
      notify('متن کامل برنامه درمانی برای مراقب یا همسر کپی شد.', 'success');
    } catch {
      notify('امکان کپی خودکار نبود.', 'danger');
    }
  };

  return (
    <div className="space-y-6" data-testid="treatment-plan-screen">
      <div className="no-print flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            یادداشت دارویی و ثبت نوبت‌های امروز
          </h1>
          <p className="mt-1 text-xs text-[#546963] dark:text-[#9EB3C4]">
            ثبت نوبت‌های دارویی امروز برای پیشگیری از فراموشی یا دوباره‌کاری بین والدین
          </p>
        </div>

        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => {
              setEditingItem(null);
              setEditorOpen(true);
            }}
            data-testid="treatment-add-med-btn"
            className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl bg-[#105B48] px-3.5 text-xs font-bold text-white dark:bg-[#00D7C7] dark:text-[#0B1220]"
          >
            <Plus className="h-4 w-4" />
            <span>افزودن دارو</span>
          </button>
          <button
            type="button"
            onClick={handleCopy}
            className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] px-3 text-xs font-bold dark:border-[#23344D]"
          >
            <ClipboardCopy className="h-4 w-4" />
            <span>کپی برنامه</span>
          </button>
          <button
            type="button"
            onClick={() => window.print()}
            className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] px-3 text-xs font-bold dark:border-[#23344D]"
          >
            <Printer className="h-4 w-4" />
            <span>چاپ</span>
          </button>
          <button
            type="button"
            onClick={() => navigate({ name: 'home' })}
            className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] px-3 text-xs font-bold dark:border-[#23344D]"
          >
            <ArrowRight className="h-4 w-4" />
            <span>خانه</span>
          </button>
        </div>
      </div>

      {legacyRows.length > 0 ? (
        <aside data-testid="treatment-legacy-notice" className="rounded-2xl border border-[#D6C9A5] bg-[#FDF9F1] p-4 text-xs leading-6 text-[#4B3410] dark:border-[#5B4729] dark:bg-[#2B2112] dark:text-[#FCE7C0]">
          <p className="font-bold">نمونه‌های دارویی قدیمی از برنامه فعال کنار گذاشته شدند.</p>
          <p>این موارد دستور مصرف شما یا نسخه تأییدشده نیستند. اطلاعات قبلی برای بازبینی نگهداری شده و در نوبت‌های امروز محاسبه نمی‌شود.</p>
          <details className="mt-2">
            <summary className="cursor-pointer font-bold">مشاهده اطلاعات قدیمی ({toPersianDigits(legacyRows.length)} مورد)</summary>
            <ul className="mt-2 space-y-2">
              {legacyRows.map((med, index) => (
                <li key={`${med.id}-${index}`} className="rounded-xl border border-current/20 p-3">
                  <p className="font-bold">{med.nameWithStrengthFa}</p>
                  <p>متن قبلی مقدار: {med.doseAmountFa}</p>
                  <p>متن قبلی زمان‌بندی: {med.scheduleLabelFa}</p>
                  {med.foodRelationFa ? <p>متن قبلی درباره غذا: {med.foodRelationFa}</p> : null}
                  {med.counselingNoteFa ? <p>یادداشت قبلی: {med.counselingNoteFa}</p> : null}
                </li>
              ))}
            </ul>
          </details>
        </aside>
      ) : null}

      <section className="rx-paper rounded-3xl border p-6 shadow-sm space-y-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <div className="text-xs font-bold opacity-80">
              ثبت شخصی مراقب · بدون تأیید پزشک یا داروساز
            </div>
            <h2 className="mt-1 text-lg font-extrabold">{childSummary}</h2>
            <p className="text-xs opacity-80">{treatmentPlan.prescriptionDateFa}</p>
          </div>

          <div className="text-left">
            <div className="text-xs font-semibold opacity-80">پیشرفت نوبت‌های امروز</div>
            <div data-testid="treatment-progress-counter" className="tabular-nums text-2xl font-extrabold">
              {toPersianDigits(counts.completedTodaySlots)} / {toPersianDigits(counts.totalDailySlots)}
            </div>
          </div>
        </div>

        <div className="rx-gold-rule h-px w-full" />

        {/* Controls Strip */}
        <div className="no-print flex flex-wrap items-center justify-between gap-3 text-xs">
          <label className="flex items-center gap-2 font-bold">
            <input
              type="checkbox"
              checked={false}
              disabled
              className="h-4 w-4 accent-[#105B48]"
            />
            <Bell className="h-4 w-4" />
            <span>یادآور خودکار در دسترس نیست؛ ثبت نوبت‌ها دستی است.</span>
          </label>
          <button
            type="button"
            onClick={resetTreatmentToday}
            className="inline-flex items-center gap-1 font-bold hover:underline"
          >
            <RotateCcw className="h-3.5 w-3.5" />
            <span>بازنشانی تیک‌های امروز</span>
          </button>
        </div>

        {/* Medications List */}
        <div className="space-y-4">
          {treatmentPlan.medications.length === 0 ? (
            <div data-testid="treatment-empty-state" className="rounded-2xl border border-dashed border-[#D6C9A5] bg-white/70 p-6 text-center dark:border-[#2F4B56] dark:bg-[#16243B]">
              <h3 className="text-base font-extrabold">هنوز دارویی ثبت نکرده‌اید</h3>
              <p className="mt-2 text-sm leading-7 opacity-80">پس از دریافت دستور مکتوب پزشک یا برچسب داروخانه، اطلاعات دارو را خودتان وارد کنید.</p>
              <p className="mt-1 text-xs leading-6 opacity-80">این یادداشت محلی، نسخه یا پیشنهاد درمان نیست.</p>
            </div>
          ) : null}
          {treatmentPlan.medications.map((med) => {
            const formLabel = TREATMENT_DOSAGE_FORMS.find((f) => f.id === med.form)?.labelFa ?? med.form;
            const slots = Array.from({ length: Math.min(6, Math.max(1, med.dailyDoseSlots)) }, (_, i) => i + 1);

            return (
              <article
                key={med.id}
                data-testid={`treatment-med-${med.id}`}
                className="rounded-2xl border border-[#D6C9A5] bg-white/85 p-4 space-y-3 dark:border-[#2F4B56] dark:bg-[#16243B]"
              >
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div>
                    <span className="text-xs font-bold opacity-75">{formLabel} · {med.scheduleLabelFa}</span>
                    <h3 className="text-base font-extrabold">{med.nameWithStrengthFa}</h3>
                    <div className="mt-1 text-sm font-bold text-[#105B48] dark:text-[#00D7C7]">
                      مقدار مصرف: {med.doseAmountFa}
                    </div>
                    {med.foodRelationFa ? <div className="text-xs opacity-80">دستور ثبت‌شده درباره غذا: {med.foodRelationFa}</div> : null}
                  </div>

                  <div className="no-print flex items-center gap-1.5">
                    <button
                      type="button"
                      onClick={() => {
                        setEditingItem(med);
                        setEditorOpen(true);
                      }}
                      aria-label={`ویرایش ${med.nameWithStrengthFa}`}
                      className="flex min-h-[38px] min-w-[38px] items-center justify-center rounded-xl border border-[#DCE7E1] dark:border-[#23344D]"
                    >
                      <Edit3 className="h-4 w-4" />
                    </button>
                    <button
                      type="button"
                      onClick={() => deleteTreatmentMedication(med.id)}
                      aria-label={`حذف ${med.nameWithStrengthFa}`}
                      className="flex min-h-[38px] min-w-[38px] items-center justify-center rounded-xl border border-[#DCE7E1] text-[#B32334] dark:border-[#23344D]"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>

                {med.counselingNoteFa ? (
                  <p className="rounded-xl bg-[#FDF9F1] p-3 text-xs leading-5 text-[#4B3410] dark:bg-[#2B2112] dark:text-[#FCE7C0]">
                    {med.counselingNoteFa}
                  </p>
                ) : null}

                {/* Daily Dose Slot Buttons */}
                <div className="flex flex-wrap items-center gap-2 pt-1">
                  <span className="text-xs font-bold">ثبت نوبت‌های امروز:</span>
                  {slots.map((slotNum) => {
                    const doneTime = med.completedSlotTimesFa?.[slotNum];
                    return (
                      <button
                        key={slotNum}
                        type="button"
                        data-testid={`treatment-slot-${med.id}-${slotNum}`}
                        onClick={() => toggleTreatmentSlot(med.id, slotNum)}
                        className={`inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border px-3 text-xs font-bold tabular-nums transition-colors ${
                          doneTime
                            ? 'border-[#105B48] bg-[#105B48] text-white dark:border-[#00D7C7] dark:bg-[#00D7C7] dark:text-[#0B1220]'
                            : 'border-[#D6C9A5] bg-white text-[#133B2E] hover:border-[#105B48] dark:border-[#365363] dark:bg-[#111C2E] dark:text-[#EDF4F7]'
                        }`}
                      >
                        <CheckCircle2 className="h-4 w-4" />
                        <span>
                          نوبت {toPersianDigits(slotNum)}
                          {doneTime ? ` (${doneTime})` : ''}
                        </span>
                      </button>
                    );
                  })}
                </div>
              </article>
            );
          })}
        </div>

        {treatmentPlan.storageInstructionFa ? (
          <div className="rounded-2xl border border-[#D6C9A5]/70 bg-white/60 p-4 text-xs leading-6 dark:border-[#2F4B56] dark:bg-[#111C2E]/70">
            <span className="font-bold">شرایط نگهداری ثبت‌شده: </span>
            {treatmentPlan.storageInstructionFa}
          </div>
        ) : null}
      </section>

      <SeverityAlert
        severity="caution"
        title="این کارت جایگزین نسخه پزشک نیست"
        body="همیشه مقدار دارو را پیش از مصرف با دستور مکتوب پزشک یا برچسب داروخانه تطبیق دهید و از سرنگ مدرج mL استفاده کنید."
      />

      {editorOpen ? (
        <TreatmentPlanEditorModal
          initialItem={editingItem}
          onClose={() => setEditorOpen(false)}
          onSave={upsertTreatmentMedication}
        />
      ) : null}
    </div>
  );
};
