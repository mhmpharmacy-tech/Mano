import React, { useState } from 'react';
import { X } from 'lucide-react';
import {
  TREATMENT_DOSAGE_FORMS,
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
    initialItem?.scheduleLabelFa ?? 'هر ۸ ساعت',
  );
  const [foodRelationFa, setFoodRelationFa] = useState(
    initialItem?.foodRelationFa ?? 'همراه غذا یا بعد از غذا',
  );
  const [counselingNoteFa, setCounselingNoteFa] = useState(initialItem?.counselingNoteFa ?? '');
  const [dailyDoseSlots, setDailyDoseSlots] = useState(initialItem?.dailyDoseSlots ?? 3);
  const [isWarningHighlight, setIsWarningHighlight] = useState(
    initialItem?.isWarningHighlight ?? false,
  );
  const [errorFa, setErrorFa] = useState<string | null>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!nameWithStrengthFa.trim() || !doseAmountFa.trim()) {
      setErrorFa('نام دارو (همراه غلظت) و مقدار هر نوبت باید وارد شوند.');
      return;
    }

    onSave({
      id: initialItem?.id ?? `rx_custom_${Date.now()}`,
      nameWithStrengthFa: nameWithStrengthFa.trim(),
      form,
      doseAmountFa: doseAmountFa.trim(),
      intervalHours: Math.max(4, Math.round(24 / Math.max(1, dailyDoseSlots))),
      scheduleLabelFa: scheduleLabelFa.trim() || 'طبق دستور نسخه',
      foodRelationFa: foodRelationFa.trim() || 'طبق برچسب دارو',
      counselingNoteFa: counselingNoteFa.trim(),
      isWarningHighlight,
      dailyDoseSlots: Math.min(6, Math.max(1, dailyDoseSlots)),
      completedSlotTimesFa: initialItem?.completedSlotTimesFa ?? {},
    });
    onClose();
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label={initialItem ? 'ویرایش داروی نسخه' : 'افزودن داروی جدید به برنامه درمانی'}
      className="fixed inset-0 z-50 flex items-center justify-center bg-[#0B1220]/70 p-4 backdrop-blur-sm"
      onClick={onClose}
    >
      <form
        onSubmit={handleSubmit}
        onClick={(e) => e.stopPropagation()}
        className="max-h-[90vh] w-full max-w-lg space-y-4 overflow-y-auto rounded-3xl border border-[#DCE7E1] bg-white p-6 shadow-2xl dark:border-[#23344D] dark:bg-[#111C2E]"
      >
        <div className="flex items-center justify-between border-b border-[#DCE7E1] pb-3 dark:border-[#23344D]">
          <h3 className="text-base font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            {initialItem ? 'ویرایش داروی نسخه' : 'افزودن داروی جدید به برنامه درمانی'}
          </h3>
          <button
            type="button"
            onClick={onClose}
            aria-label="بستن"
            className="flex min-h-[40px] min-w-[40px] items-center justify-center rounded-xl text-[#546963]"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {errorFa ? (
          <div className="rounded-xl bg-[#FDF0F1] p-3 text-xs font-bold text-[#B32334]">
            {errorFa}
          </div>
        ) : null}

        <div>
          <label className="block text-xs font-bold">نام دارو و غلظت دقیق</label>
          <input
            type="text"
            value={nameWithStrengthFa}
            onChange={(e) => setNameWithStrengthFa(e.target.value)}
            placeholder="مثلاً شربت آموکسی‌سیلین ۲۵۰ میلی‌گرم / ۵ سی‌سی"
            className="mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
          />
        </div>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div>
            <label className="block text-xs font-bold">شکل دارویی</label>
            <select
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
            <label className="block text-xs font-bold">تعداد نوبت در روز (۱ تا ۶)</label>
            <input
              type="number"
              min={1}
              max={6}
              value={dailyDoseSlots}
              onChange={(e) => setDailyDoseSlots(Number(e.target.value) || 1)}
              className="mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm tabular-nums dark:border-[#23344D] dark:bg-[#16243B]"
            />
          </div>
        </div>

        <div>
          <label className="block text-xs font-bold">مقدار هر نوبت (مثلاً ۵ سی‌سی با سرنگ)</label>
          <input
            type="text"
            value={doseAmountFa}
            onChange={(e) => setDoseAmountFa(e.target.value)}
            placeholder="مثلاً ۵ سی‌سی با سرنگ مدرج"
            className="mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
          />
        </div>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div>
            <label className="block text-xs font-bold">فاصله زمانی</label>
            <input
              type="text"
              value={scheduleLabelFa}
              onChange={(e) => setScheduleLabelFa(e.target.value)}
              className="mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
            />
          </div>
          <div>
            <label className="block text-xs font-bold">ارتباط با غذا</label>
            <input
              type="text"
              value={foodRelationFa}
              onChange={(e) => setFoodRelationFa(e.target.value)}
              className="mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
            />
          </div>
        </div>

        <div>
          <label className="block text-xs font-bold">توصیه داروساز / نگهداری</label>
          <textarea
            rows={2}
            value={counselingNoteFa}
            onChange={(e) => setCounselingNoteFa(e.target.value)}
            placeholder="مثلاً تکان دادن بطری، تکمیل دوره یا شرایط نگهداری..."
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
    </div>
  );
};
