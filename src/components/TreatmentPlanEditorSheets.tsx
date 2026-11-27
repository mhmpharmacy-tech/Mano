import React, { useRef, useState } from 'react';
import { X } from 'lucide-react';
import { AccessibleDialog } from './AccessibleDialog.tsx';
import {
  TREATMENT_DOSAGE_FORMS,
  treatmentMedicationEntryError,
  type TreatmentDosageFormId,
  type TreatmentMedicationItem,
} from '../domain/treatmentPlan.ts';

interface TreatmentPlanEditorModalProps {
  initialItem: TreatmentMedicationItem | null;
  onClose: () => void;
  onSave: (item: TreatmentMedicationItem) => void;
}

export const TreatmentPlanEditorModal: React.FC<TreatmentPlanEditorModalProps> = ({
  initialItem,
  onClose,
  onSave,
}) => {
  const [nameWithStrengthFa, setNameWithStrengthFa] = useState(
    initialItem?.nameWithStrengthFa ?? '',
  );
  const [form, setForm] = useState<TreatmentDosageFormId>(initialItem?.form ?? 'SYRUP_SUSPENSION');
  const [doseAmountFa, setDoseAmountFa] = useState(initialItem?.doseAmountFa ?? '');
  const [scheduleLabelFa, setScheduleLabelFa] = useState(
    initialItem?.scheduleLabelFa ?? '',
  );
  const [foodRelationFa, setFoodRelationFa] = useState(
    initialItem?.foodRelationFa ?? '',
  );
  const [counselingNoteFa, setCounselingNoteFa] = useState(initialItem?.counselingNoteFa ?? '');
  const [dailyDoseSlots, setDailyDoseSlots] = useState(initialItem ? String(initialItem.dailyDoseSlots) : '');
  const [isWarningHighlight, setIsWarningHighlight] = useState(
    initialItem?.isWarningHighlight ?? false,
  );
  const [errorFa, setErrorFa] = useState<string | null>(null);
  const titleRef = useRef<HTMLHeadingElement>(null);
  const formRef = useRef<HTMLFormElement>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const slotCount = Number(dailyDoseSlots);
    const error = treatmentMedicationEntryError({ nameWithStrengthFa, doseAmountFa, scheduleLabelFa, dailyDoseSlots: slotCount });
    if (error) {
      setErrorFa(error);
      const fieldId = !nameWithStrengthFa.trim() ? 'treatment-med-name'
        : !doseAmountFa.trim() ? 'treatment-med-dose'
          : !scheduleLabelFa.trim() ? 'treatment-med-schedule' : 'treatment-med-slots';
      formRef.current?.querySelector<HTMLElement>(`#${fieldId}`)?.focus();
      return;
    }

    onSave({
      id: initialItem?.id ?? `rx_custom_${Date.now()}`,
      nameWithStrengthFa: nameWithStrengthFa.trim(),
      form,
      doseAmountFa: doseAmountFa.trim(),
      intervalHours: null,
      scheduleLabelFa: scheduleLabelFa.trim(),
      foodRelationFa: foodRelationFa.trim(),
      counselingNoteFa: counselingNoteFa.trim(),
      isWarningHighlight,
      dailyDoseSlots: slotCount,
      completedSlotTimesFa: initialItem?.completedSlotTimesFa ?? {},
    });
    onClose();
  };

  return (
    <AccessibleDialog
      id="mano-treatment-editor-dialog"
      label={initialItem ? 'ویرایش داروی نسخه' : 'افزودن داروی جدید به برنامه درمانی'}
      describedBy="mano-treatment-editor-description"
      initialFocusRef={titleRef}
      fallbackFocusId="mano-main-content"
      onClose={onClose}
      backdropClassName="fixed inset-0 z-50 flex items-center justify-center bg-[#0B1220]/70 p-4 backdrop-blur-sm"
      panelClassName="max-h-[calc(100dvh-2rem)] w-full max-w-lg overflow-y-auto overscroll-contain rounded-3xl border border-[#DCE7E1] bg-white p-5 outline-none dark:border-[#23344D] dark:bg-[#111C2E] sm:p-6"
    >
      <form
        ref={formRef}
        onSubmit={handleSubmit}
        className="space-y-4"
      >
        <div className="flex items-center justify-between gap-2 border-b border-[#DCE7E1] pb-3 dark:border-[#23344D]">
          <h3 ref={titleRef} tabIndex={-1} className="min-w-0 break-words text-base font-extrabold text-[#122823] outline-none dark:text-[#EDF4F7]">
            {initialItem ? 'ویرایش داروی نسخه' : 'افزودن داروی جدید به برنامه درمانی'}
          </h3>
          <button
            type="button"
            onClick={onClose}
            aria-label="بستن"
            className="flex min-h-[44px] min-w-[44px] shrink-0 items-center justify-center rounded-xl text-[#546963]"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <p id="mano-treatment-editor-description" className="text-xs leading-6 text-[#546963] dark:text-[#9EB3C4]">
          فقط دستور مکتوب پزشک یا برچسب داروخانه را ثبت کنید. این فرم مقدار یا فاصله مصرف پیشنهاد نمی‌دهد و تأیید درمان نیست.
        </p>

        {errorFa ? (
          <div role="alert" className="rounded-xl bg-[#FDF0F1] p-3 text-xs font-bold text-[#B32334]">
            {errorFa}
          </div>
        ) : null}

        <div>
          <label htmlFor="treatment-med-name" className="block text-xs font-bold">نام دارو و غلظت دقیق</label>
          <input
            id="treatment-med-name"
            type="text"
            required
            value={nameWithStrengthFa}
            onChange={(e) => setNameWithStrengthFa(e.target.value)}
            placeholder="از روی برچسب دارو وارد کنید"
            className="mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
          />
        </div>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div>
            <label htmlFor="treatment-med-form" className="block text-xs font-bold">شکل دارویی</label>
            <select
              id="treatment-med-form"
              value={form}
              onChange={(e) => setForm(e.target.value as TreatmentDosageFormId)}
              className="mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
            >
              {TREATMENT_DOSAGE_FORMS.map((f) => (
                <option key={f.id} value={f.id}>
                  {f.labelFa}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="treatment-med-slots" className="block text-xs font-bold">تعداد نوبت قابل ثبت در روز (۱ تا ۶)</label>
            <input
              id="treatment-med-slots"
              type="number"
              required
              min={1}
              max={6}
              step={1}
              value={dailyDoseSlots}
              onChange={(e) => setDailyDoseSlots(e.target.value)}
              aria-describedby="treatment-med-slots-help"
              className="mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm tabular-nums dark:border-[#23344D] dark:bg-[#16243B]"
            />
            <p id="treatment-med-slots-help" className="mt-1 text-xs leading-5 text-[#546963] dark:text-[#9EB3C4]">
              فقط برای ثبت دستی؛ از این تعداد، فاصله زمانی مصرف محاسبه نمی‌شود.
            </p>
          </div>
        </div>

        <div>
          <label htmlFor="treatment-med-dose" className="block text-xs font-bold">مقدار هر نوبت طبق دستور</label>
          <input
            id="treatment-med-dose"
            type="text"
            required
            value={doseAmountFa}
            onChange={(e) => setDoseAmountFa(e.target.value)}
            placeholder="مقدار و واحد را عیناً از دستور وارد کنید"
            className="mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
          />
        </div>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div>
            <label htmlFor="treatment-med-schedule" className="block text-xs font-bold">دستور زمان‌بندی مصرف</label>
            <input
              id="treatment-med-schedule"
              type="text"
              required
              value={scheduleLabelFa}
              onChange={(e) => setScheduleLabelFa(e.target.value)}
              className="mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
            />
          </div>
          <div>
            <label htmlFor="treatment-med-food" className="block text-xs font-bold">دستور درباره غذا (اختیاری)</label>
            <input
              id="treatment-med-food"
              type="text"
              value={foodRelationFa}
              onChange={(e) => setFoodRelationFa(e.target.value)}
              className="mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
            />
          </div>
        </div>

        <div>
          <label htmlFor="treatment-med-notes" className="block text-xs font-bold">یادداشت دستور مصرف / نگهداری (اختیاری)</label>
          <textarea
            id="treatment-med-notes"
            rows={2}
            value={counselingNoteFa}
            onChange={(e) => setCounselingNoteFa(e.target.value)}
            placeholder="فقط نکته درج‌شده در نسخه یا برچسب دارو"
            className="mt-1 w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] p-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
          />
        </div>

        <label className="flex items-center gap-2 text-xs font-bold">
          <input
            type="checkbox"
            checked={isWarningHighlight}
            onChange={(e) => setIsWarningHighlight(e.target.checked)}
            className="h-5 w-5 accent-[#C6862E]"
          />
          <span>برجسته‌سازی با کادر هشدار ایمنی دارویی</span>
        </label>

        <div className="flex gap-2 pt-2">
          <button
            type="submit"
            className="flex-1 min-h-[44px] rounded-xl bg-[#105B48] text-sm font-bold text-white dark:bg-[#00D7C7] dark:text-[#0B1220]"
          >
            ذخیره در برنامه درمانی
          </button>
          <button
            type="button"
            onClick={onClose}
            className="min-h-[44px] rounded-xl border border-[#DCE7E1] px-4 text-sm font-semibold dark:border-[#23344D]"
          >
            انصراف
          </button>
        </div>
      </form>
    </AccessibleDialog>
  );
};
