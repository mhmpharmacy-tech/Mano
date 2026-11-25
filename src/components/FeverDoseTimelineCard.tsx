import React, { useState } from 'react';
import { Clock, Copy, Pill, Plus, Thermometer, Trash2, X } from 'lucide-react';
import { useMano } from '../state/ManoStore.tsx';
import {
  buildDoctorTimelineReportFa,
  formatTempC,
  mergedChronologicalEvents,
} from '../domain/feverDoseTimelineEngine.ts';
import { parseClinicalDecimal } from '../domain/clinicalNumberInput.ts';
import { toPersianDigits } from '../domain/persianNormalizer.ts';

interface FeverDoseTimelineCardProps {
  compact?: boolean;
}

export const FeverDoseTimelineCard: React.FC<FeverDoseTimelineCardProps> = ({ compact = false }) => {
  const {
    timelineSnapshot,
    childProfile,
    addTempLog,
    deleteTempLog,
    addDoseLog,
    deleteDoseLog,
    notify,
  } = useMano();

  const [tempModalOpen, setTempModalOpen] = useState(false);
  const [doseModalOpen, setDoseModalOpen] = useState(false);

  const [tempInput, setTempInput] = useState('38.2');
  const [tempSite, setTempSite] = useState('زیر بغل (دیجیتال)');
  const [tempCaregiver, setTempCaregiver] = useState('مادر / پدر');
  const [tempNote, setTempNote] = useState('');

  const [doseMedSlug, setDoseMedSlug] = useState<'acetaminophen' | 'ibuprofen'>('acetaminophen');
  const [doseForm, setDoseForm] = useState('شربت ۱۲۰mg/5mL');
  const [doseMl, setDoseMl] = useState('5.0 mL');
  const [doseNote, setDoseNote] = useState('');

  const events = mergedChronologicalEvents(timelineSnapshot);
  const displayEvents = compact ? events.slice(0, 4) : events;

  const handleSaveTemp = (e: React.FormEvent) => {
    e.preventDefault();
    const parsed = parseClinicalDecimal(tempInput);
    if (parsed === null || parsed < 34 || parsed > 43) {
      notify('لطفاً عدد دمای معتبر بین ۳۴ تا ۴۳ درجه سانتی‌گراد وارد کنید.', 'danger');
      return;
    }
    addTempLog({
      tempCelsius: parsed,
      siteFa: tempSite,
      caregiverFa: tempCaregiver,
      noteFa: tempNote,
    });
    setTempModalOpen(false);
    setTempNote('');
  };

  const handleSaveDose = (e: React.FormEvent) => {
    e.preventDefault();
    if (!doseMl.trim()) {
      notify('مقدار مصرف‌شده (مثلاً ۵ mL) را وارد کنید.', 'danger');
      return;
    }
    addDoseLog({
      medicationSlug: doseMedSlug,
      medicationNameFa: doseMedSlug === 'acetaminophen' ? 'استامینوفن کودکان' : 'ایبوپروفن کودکان',
      dosageFormFa: doseForm,
      volumeMlText: doseMl.trim(),
      noteFa: doseNote,
    });
    setDoseModalOpen(false);
    setDoseNote('');
  };

  const handleCopySummary = async () => {
    const text = buildDoctorTimelineReportFa(timelineSnapshot, childProfile);
    try {
      await navigator.clipboard.writeText(text);
      notify('گزارش ۲۴ ساعته تب و دارو برای ارائه به پزشک کپی شد.', 'success');
    } catch {
      notify('امکان کپی خودکار نبود؛ می‌توانید موارد لیست را مطالعه کنید.', 'caution' as 'info');
    }
  };

  const acetStatus = timelineSnapshot.acetaminophenStatus;
  const ibuStatus = timelineSnapshot.ibuprofenStatus;

  return (
    <section
      data-testid="fever-dose-timeline-card"
      className="rounded-3xl border border-[#DCE7E1] bg-[#FFFFFF] p-5 dark:border-[#23344D] dark:bg-[#111C2E]"
    >
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-base font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            خط زمانی ۲۴ ساعته تب و داروها
          </h2>
          <p className="mt-0.5 text-xs text-[#546963] dark:text-[#9EB3C4]">
            ثبت ساعت دقیق دما و نوبت تب‌بر برای پیشگیری از تداخل و تکرار زودتر از موعد
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={() => setTempModalOpen(true)}
            data-testid="timeline-add-temp-btn"
            className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl bg-[#105B48] px-3.5 text-xs font-bold text-white hover:bg-[#0C493A] dark:bg-[#00D7C7] dark:text-[#0B1220]"
          >
            <Plus className="h-4 w-4" />
            <span>ثبت دما</span>
          </button>

          <button
            type="button"
            onClick={() => setDoseModalOpen(true)}
            data-testid="timeline-add-dose-btn"
            className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3.5 text-xs font-bold text-[#122823] hover:border-[#105B48] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#EDF4F7]"
          >
            <Pill className="h-4 w-4 text-[#105B48] dark:text-[#00D7C7]" />
            <span>ثبت نوبت دارو</span>
          </button>

          {events.length > 0 ? (
            <button
              type="button"
              onClick={handleCopySummary}
              className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] px-3 text-xs font-semibold text-[#546963] hover:text-[#105B48] dark:border-[#23344D] dark:text-[#9EB3C4]"
            >
              <Copy className="h-4 w-4" />
              <span>کپی گزارش پزشک</span>
            </button>
          ) : null}
        </div>
      </div>

      {/* Interval Safety Status Row */}
      <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2">
        {[acetStatus, ibuStatus].map((st) => (
          <div
            key={st.medicationKey}
            className={`rounded-2xl border p-3 text-xs leading-5 ${
              st.severity === 'danger'
                ? 'border-[#E7B5BC] bg-[#FDF0F1] text-[#4C1119] dark:border-[#6A2C3B] dark:bg-[#2F161E] dark:text-[#FFDDE2]'
                : st.severity === 'success'
                  ? 'border-[#B6DFC1] bg-[#EBF7EE] text-[#113C27] dark:border-[#21594B] dark:bg-[#102B25] dark:text-[#CBEFD9]'
                  : 'border-[#DCE7E1] bg-[#F5F9F6] text-[#546963] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#9EB3C4]'
            }`}
          >
            <div className="flex items-center justify-between font-bold">
              <span>{st.medFamilyLabelFa}</span>
              <span className="tabular-nums">
                {toPersianDigits(st.dosesLast24h)} از {toPersianDigits(st.maxDoses24h)} نوبت (۲۴س)
              </span>
            </div>
            <p className="mt-1">{st.statusMessageFa}</p>
          </div>
        ))}
      </div>

      {/* Chronological Event Feed */}
      <div className="mt-4">
        {displayEvents.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-[#DCE7E1] p-5 text-center text-xs text-[#546963] dark:border-[#23344D] dark:text-[#9EB3C4]">
            هنوز دما یا دارویی در ۲۴ ساعت گذشته ثبت نشده است. با دکمه‌های بالا اولین نوبت را ثبت کنید.
          </div>
        ) : (
          <ul className="divide-y divide-[#DCE7E1] dark:divide-[#23344D]">
            {displayEvents.map((ev) => (
              <li key={ev.key} className="flex items-center justify-between gap-3 py-3 text-sm">
                <div className="flex items-center gap-3">
                  <span
                    className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-xl ${
                      ev.kind === 'temp'
                        ? 'bg-[#FDF4E4] text-[#9F5F0D] dark:bg-[#312617] dark:text-[#F0B85C]'
                        : 'bg-[#E9F5EB] text-[#105B48] dark:bg-[#0F333A] dark:text-[#00D7C7]'
                    }`}
                  >
                    {ev.kind === 'temp' ? (
                      <Thermometer className="h-4 w-4" />
                    ) : (
                      <Pill className="h-4 w-4" />
                    )}
                  </span>
                  <div>
                    {ev.kind === 'temp' ? (
                      <>
                        <div className="font-bold tabular-nums text-[#122823] dark:text-[#EDF4F7]">
                          {formatTempC(ev.temp.tempCelsius)} · {ev.temp.siteFa}
                        </div>
                        <div className="text-xs text-[#546963] dark:text-[#9EB3C4] flex items-center gap-1.5">
                          <Clock className="h-3 w-3" />
                          <span className="tabular-nums">{ev.temp.timeLabelFa}</span>
                          <span>·</span>
                          <span>{ev.temp.caregiverFa}</span>
                          {ev.temp.noteFa ? <span>· {ev.temp.noteFa}</span> : null}
                        </div>
                      </>
                    ) : (
                      <>
                        <div className="font-bold text-[#122823] dark:text-[#EDF4F7]">
                          {ev.dose.medicationNameFa} ·{' '}
                          <span className="tabular-nums text-[#105B48] dark:text-[#00D7C7]">
                            {ev.dose.volumeMlText}
                          </span>
                        </div>
                        <div className="text-xs text-[#546963] dark:text-[#9EB3C4] flex items-center gap-1.5">
                          <Clock className="h-3 w-3" />
                          <span className="tabular-nums">{ev.dose.givenAtLabelFa}</span>
                          <span>·</span>
                          <span>{ev.dose.dosageFormFa}</span>
                          {ev.dose.noteFa ? <span>· {ev.dose.noteFa}</span> : null}
                        </div>
                      </>
                    )}
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() =>
                    ev.kind === 'temp' ? deleteTempLog(ev.temp.id) : deleteDoseLog(ev.dose.id)
                  }
                  aria-label="حذف ثبت"
                  className="flex min-h-[40px] min-w-[40px] items-center justify-center rounded-xl text-[#546963] hover:bg-[#FDF0F1] hover:text-[#B32334] dark:text-[#9EB3C4]"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* Add Temp Modal */}
      {tempModalOpen ? (
        <div
          role="dialog"
          aria-modal="true"
          aria-label="ثبت دمای جدید کودک"
          className="fixed inset-0 z-50 flex items-center justify-center bg-[#0B1220]/70 p-4 backdrop-blur-sm"
          onClick={() => setTempModalOpen(false)}
        >
          <form
            onSubmit={handleSaveTemp}
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-md space-y-4 rounded-3xl border border-[#DCE7E1] bg-white p-6 shadow-2xl dark:border-[#23344D] dark:bg-[#111C2E]"
          >
            <div className="flex items-center justify-between">
              <h3 className="text-base font-extrabold text-[#122823] dark:text-[#EDF4F7]">ثبت دمای جدید</h3>
              <button
                type="button"
                onClick={() => setTempModalOpen(false)}
                aria-label="بستن"
                className="flex min-h-[40px] min-w-[40px] items-center justify-center rounded-xl text-[#546963]"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div>
              <label className="block text-xs font-bold text-[#122823] dark:text-[#EDF4F7]">
                عدد دماسنج (°C)
              </label>
              <input
                type="text"
                inputMode="decimal"
                value={tempInput}
                onChange={(e) => setTempInput(e.target.value)}
                placeholder="مثلاً 38.4"
                className="mt-1.5 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm tabular-nums dark:border-[#23344D] dark:bg-[#16243B]"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-[#122823] dark:text-[#EDF4F7]">
                روش و محل اندازه‌گیری
              </label>
              <select
                value={tempSite}
                onChange={(e) => setTempSite(e.target.value)}
                className="mt-1.5 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
              >
                <option value="زیر بغل (دیجیتال)">زیر بغل (دیجیتال)</option>
                <option value="پیشانی (غیرتماسی)">پیشانی (غیرتماسی)</option>
                <option value="گوشی (تیمپانیک)">گوشی (تیمپانیک)</option>
                <option value="مقعدی (نوزاد)">مقعدی (نوزاد)</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-[#122823] dark:text-[#EDF4F7]">
                ثبت‌کننده و یادداشت (اختیاری)
              </label>
              <div className="mt-1.5 grid grid-cols-2 gap-2">
                <input
                  type="text"
                  value={tempCaregiver}
                  onChange={(e) => setTempCaregiver(e.target.value)}
                  placeholder="مادر / پدر"
                  className="min-h-[44px] rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
                />
                <input
                  type="text"
                  value={tempNote}
                  onChange={(e) => setTempNote(e.target.value)}
                  placeholder="حال عمومی..."
                  className="min-h-[44px] rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
                />
              </div>
            </div>

            <button
              type="submit"
              className="min-h-[44px] w-full rounded-xl bg-[#105B48] px-4 text-sm font-bold text-white hover:bg-[#0C493A] dark:bg-[#00D7C7] dark:text-[#0B1220]"
            >
              ذخیره دما در جدول ۲۴ ساعته
            </button>
          </form>
        </div>
      ) : null}

      {/* Add Dose Modal */}
      {doseModalOpen ? (
        <div
          role="dialog"
          aria-modal="true"
          aria-label="ثبت نوبت داروی تب‌بر"
          className="fixed inset-0 z-50 flex items-center justify-center bg-[#0B1220]/70 p-4 backdrop-blur-sm"
          onClick={() => setDoseModalOpen(false)}
        >
          <form
            onSubmit={handleSaveDose}
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-md space-y-4 rounded-3xl border border-[#DCE7E1] bg-white p-6 shadow-2xl dark:border-[#23344D] dark:bg-[#111C2E]"
          >
            <div className="flex items-center justify-between">
              <h3 className="text-base font-extrabold text-[#122823] dark:text-[#EDF4F7]">ثبت نوبت دارو</h3>
              <button
                type="button"
                onClick={() => setDoseModalOpen(false)}
                aria-label="بستن"
                className="flex min-h-[40px] min-w-[40px] items-center justify-center rounded-xl text-[#546963]"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => {
                  setDoseMedSlug('acetaminophen');
                  setDoseForm('شربت ۱۲۰mg/5mL');
                }}
                className={`min-h-[44px] rounded-xl border text-xs font-bold ${
                  doseMedSlug === 'acetaminophen'
                    ? 'border-[#105B48] bg-[#E9F5EB] text-[#105B48] dark:border-[#00D7C7] dark:bg-[#0F333A] dark:text-[#00D7C7]'
                    : 'border-[#DCE7E1] text-[#546963] dark:border-[#23344D]'
                }`}
              >
                خانواده استامینوفن
              </button>
              <button
                type="button"
                onClick={() => {
                  setDoseMedSlug('ibuprofen');
                  setDoseForm('سوسپانسیون ۱۰۰mg/5mL');
                }}
                className={`min-h-[44px] rounded-xl border text-xs font-bold ${
                  doseMedSlug === 'ibuprofen'
                    ? 'border-[#105B48] bg-[#E9F5EB] text-[#105B48] dark:border-[#00D7C7] dark:bg-[#0F333A] dark:text-[#00D7C7]'
                    : 'border-[#DCE7E1] text-[#546963] dark:border-[#23344D]'
                }`}
              >
                خانواده ایبوپروفن
              </button>
            </div>

            <div>
              <label className="block text-xs font-bold text-[#122823] dark:text-[#EDF4F7]">
                حجم یا مقدار مصرف‌شده
              </label>
              <input
                type="text"
                value={doseMl}
                onChange={(e) => setDoseMl(e.target.value)}
                placeholder="مثلاً 5.0 mL یا ۱۶ قطره"
                className="mt-1.5 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-[#122823] dark:text-[#EDF4F7]">یادداشت (اختیاری)</label>
              <input
                type="text"
                value={doseNote}
                onChange={(e) => setDoseNote(e.target.value)}
                placeholder="پس از غذا / سرنگ مدرج..."
                className="mt-1.5 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
              />
            </div>

            <button
              type="submit"
              className="min-h-[44px] w-full rounded-xl bg-[#105B48] px-4 text-sm font-bold text-white hover:bg-[#0C493A] dark:bg-[#00D7C7] dark:text-[#0B1220]"
            >
              ثبت نوبت در جدول ایمنی
            </button>
          </form>
        </div>
      ) : null}
    </section>
  );
};
