import React, { useRef, useState } from 'react';
import { Clock, Copy, Pill, Plus, Thermometer, Trash2, X } from 'lucide-react';
import { useMano } from '../state/ManoStore.tsx';
import {
  buildDoctorTimelineReportFa,
  formatTempC,
  mergedChronologicalEvents,
} from '../domain/feverDoseTimelineEngine.ts';
import {
  TIMELINE_DOSE_UNITS,
  TIMELINE_TEMPERATURE_SITES,
  formatTimelineDateTimeInput,
  validateTimelineDose,
  validateTimelineTemperature,
} from '../domain/timelineEntryInput.ts';
import { toPersianDigits } from '../domain/persianNormalizer.ts';
import { AccessibleDialog } from './AccessibleDialog.tsx';

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
  const tempTitleRef = useRef<HTMLHeadingElement>(null);
  const doseTitleRef = useRef<HTMLHeadingElement>(null);
  const [tempError, setTempError] = useState<string | null>(null);
  const [doseError, setDoseError] = useState<string | null>(null);

  const [tempInput, setTempInput] = useState('');
  const [tempSite, setTempSite] = useState('');
  const [tempTime, setTempTime] = useState('');
  const [tempCaregiver, setTempCaregiver] = useState('');
  const [tempNote, setTempNote] = useState('');

  const [doseMedSlug, setDoseMedSlug] = useState<'' | 'acetaminophen' | 'ibuprofen'>('');
  const [doseForm, setDoseForm] = useState('');
  const [doseMl, setDoseMl] = useState('');
  const [doseUnit, setDoseUnit] = useState('');
  const [doseTime, setDoseTime] = useState('');
  const [doseNote, setDoseNote] = useState('');
  const [entryNowMs, setEntryNowMs] = useState(() => Date.now());

  const events = mergedChronologicalEvents(timelineSnapshot);
  const displayEvents = compact ? events.slice(0, 4) : events;
  const validTemperature = validateTimelineTemperature(tempInput, tempSite, tempTime, entryNowMs);
  const validDose = validateTimelineDose({ medication: doseMedSlug, formulation: doseForm, amount: doseMl, unit: doseUnit, dateTime: doseTime }, entryNowMs);

  const openTemperatureForm = () => {
    setEntryNowMs(Date.now());
    setTempInput(''); setTempSite(''); setTempTime(''); setTempCaregiver(''); setTempNote('');
    setTempError(null);
    setTempModalOpen(true);
  };
  const openDoseForm = () => {
    setEntryNowMs(Date.now());
    setDoseMedSlug(''); setDoseForm(''); setDoseMl(''); setDoseUnit(''); setDoseTime(''); setDoseNote('');
    setDoseError(null);
    setDoseModalOpen(true);
  };

  const handleSaveTemp = (e: React.FormEvent) => {
    e.preventDefault();
    const validated = validateTimelineTemperature(tempInput, tempSite, tempTime);
    if (!validated) {
      setTempError('دمای اندازه‌گیری‌شده، روش اندازه‌گیری و زمان واقعی معتبر و غیرآینده را وارد کنید.');
      tempTitleRef.current?.focus();
      return;
    }
    addTempLog({
      tempCelsius: validated.temperatureCelsius,
      measuredAtMs: validated.measuredAtMs,
      siteFa: tempSite,
      caregiverFa: tempCaregiver.trim() || 'ثبت‌کننده مشخص نشده',
      noteFa: tempNote,
    });
    setTempModalOpen(false);
    setTempNote('');
  };

  const handleSaveDose = (e: React.FormEvent) => {
    e.preventDefault();
    const validated = validateTimelineDose({ medication: doseMedSlug, formulation: doseForm, amount: doseMl, unit: doseUnit, dateTime: doseTime });
    if (!validated) {
      setDoseError('داروی مصرف‌شده، شکل و غلظت، مقدار مثبت با واحد و زمان واقعی غیرآینده را کامل کنید.');
      doseTitleRef.current?.focus();
      return;
    }
    addDoseLog({
      medicationSlug: doseMedSlug,
      administeredAtMs: validated.administeredAtMs,
      medicationNameFa: doseMedSlug === 'acetaminophen' ? 'استامینوفن کودکان' : 'ایبوپروفن کودکان',
      dosageFormFa: doseForm.trim(),
      volumeMlText: validated.amountText,
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
            onClick={openTemperatureForm}
            data-testid="timeline-add-temp-btn"
            className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl bg-[#105B48] px-3.5 text-xs font-bold text-white hover:bg-[#0C493A] dark:bg-[#00D7C7] dark:text-[#0B1220]"
          >
            <Plus className="h-4 w-4" />
            <span>ثبت دما</span>
          </button>

          <button
            type="button"
            onClick={openDoseForm}
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
      {(!compact || events.length > 0) && <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2">
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
      </div>}

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
        <AccessibleDialog
          id="mano-temperature-entry-dialog"
          label="ثبت دمای جدید کودک"
          initialFocusRef={tempTitleRef}
          fallbackFocusId="mano-main-content"
          onClose={() => setTempModalOpen(false)}
          backdropClassName="fixed inset-0 z-50 flex items-center justify-center bg-[#0B1220]/70 p-4 backdrop-blur-sm"
          panelClassName="max-h-[calc(100dvh-2rem)] w-full max-w-md overflow-y-auto overscroll-contain rounded-3xl border border-[#DCE7E1] bg-white p-5 outline-none dark:border-[#23344D] dark:bg-[#111C2E] sm:p-6"
        >
          <form
            onSubmit={handleSaveTemp}
            className="space-y-4"
          >
            <div className="flex items-center justify-between">
              <h3 ref={tempTitleRef} tabIndex={-1} className="min-w-0 break-words text-base font-extrabold text-[#122823] outline-none dark:text-[#EDF4F7]">ثبت دمای جدید</h3>
              <button
                type="button"
                onClick={() => setTempModalOpen(false)}
                aria-label="بستن"
                className="flex min-h-[44px] min-w-[44px] shrink-0 items-center justify-center rounded-xl text-[#546963]"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            {tempError ? <p role="alert" className="rounded-xl bg-[#FDF0F1] p-3 text-xs leading-6 text-[#B32334]">{tempError}</p> : null}

            <div>
              <label htmlFor="timeline-temp-input" className="block text-xs font-bold text-[#122823] dark:text-[#EDF4F7]">
                عدد دماسنج (°C)
              </label>
              <input
                id="timeline-temp-input"
                data-testid="timeline-temp-input"
                type="text"
                inputMode="decimal"
                value={tempInput}
                onChange={(e) => setTempInput(e.target.value)}
                placeholder="مثلاً 38.4"
                className="mt-1.5 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm tabular-nums dark:border-[#23344D] dark:bg-[#16243B]"
              />
            </div>

            <div>
              <label htmlFor="timeline-temp-site-select" className="block text-xs font-bold text-[#122823] dark:text-[#EDF4F7]">
                روش و محل اندازه‌گیری
              </label>
              <select
                id="timeline-temp-site-select"
                data-testid="timeline-temp-site-select"
                value={tempSite}
                onChange={(e) => setTempSite(e.target.value)}
                className="mt-1.5 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
              >
                <option value="" disabled>روش واقعی اندازه‌گیری را انتخاب کنید</option>
                {TIMELINE_TEMPERATURE_SITES.map((site) => <option key={site} value={site}>{site}</option>)}
              </select>
            </div>

            <div>
              <label htmlFor="timeline-temp-time-input" className="block text-xs font-bold">زمان واقعی اندازه‌گیری</label>
              <input
                id="timeline-temp-time-input"
                data-testid="timeline-temp-time-input"
                type="datetime-local"
                value={tempTime}
                max={formatTimelineDateTimeInput(entryNowMs)}
                onChange={(e) => { setEntryNowMs(Date.now()); setTempTime(e.target.value); }}
                className="mt-1.5 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
              />
            </div>

            <div>
              <p className="block text-xs font-bold text-[#122823] dark:text-[#EDF4F7]">
                ثبت‌کننده و یادداشت (اختیاری)
              </p>
              <div className="mt-1.5 grid grid-cols-1 gap-2 min-[380px]:grid-cols-2">
                <input
                  type="text"
                  aria-label="ثبت‌کننده دما (اختیاری)"
                  value={tempCaregiver}
                  onChange={(e) => setTempCaregiver(e.target.value)}
                  placeholder="مادر / پدر"
                  className="min-h-[44px] rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
                />
                <input
                  type="text"
                  aria-label="یادداشت دما (اختیاری)"
                  value={tempNote}
                  onChange={(e) => setTempNote(e.target.value)}
                  placeholder="حال عمومی..."
                  className="min-h-[44px] rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
                />
              </div>
            </div>

            <button
              type="submit"
              data-testid="timeline-temp-save-btn"
              disabled={!validTemperature}
              className="min-h-[44px] w-full rounded-xl bg-[#105B48] px-4 text-sm font-bold text-white hover:bg-[#0C493A] disabled:opacity-50 dark:bg-[#00D7C7] dark:text-[#0B1220]"
            >
              ذخیره دما در جدول ۲۴ ساعته
            </button>
          </form>
        </AccessibleDialog>
      ) : null}

      {/* Add Dose Modal */}
      {doseModalOpen ? (
        <AccessibleDialog
          id="mano-dose-entry-dialog"
          label="ثبت نوبت داروی تب‌بر"
          describedBy="mano-dose-entry-description"
          initialFocusRef={doseTitleRef}
          fallbackFocusId="mano-main-content"
          onClose={() => setDoseModalOpen(false)}
          backdropClassName="fixed inset-0 z-50 flex items-center justify-center bg-[#0B1220]/70 p-4 backdrop-blur-sm"
          panelClassName="max-h-[calc(100dvh-2rem)] w-full max-w-md overflow-y-auto overscroll-contain rounded-3xl border border-[#DCE7E1] bg-white p-5 outline-none dark:border-[#23344D] dark:bg-[#111C2E] sm:p-6"
        >
          <form
            onSubmit={handleSaveDose}
            className="space-y-4"
          >
            <div className="flex items-center justify-between">
              <h3 ref={doseTitleRef} tabIndex={-1} className="min-w-0 break-words text-base font-extrabold text-[#122823] outline-none dark:text-[#EDF4F7]">ثبت مصرف واقعی دارو</h3>
              <button
                type="button"
                onClick={() => setDoseModalOpen(false)}
                aria-label="بستن"
                className="flex min-h-[44px] min-w-[44px] shrink-0 items-center justify-center rounded-xl text-[#546963]"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <p id="mano-dose-entry-description" className="text-xs leading-6 text-[#546963] dark:text-[#9EB3C4]">
              فقط مقداری را ثبت کنید که واقعاً مصرف شده است. این فرم پیشنهاد یا تأیید دوز نیست؛ مقدار، غلظت و زمان را از مصرف واقعی ثبت کنید.
            </p>
            {doseError ? <p role="alert" className="rounded-xl bg-[#FDF0F1] p-3 text-xs leading-6 text-[#B32334]">{doseError}</p> : null}

            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                data-testid="timeline-dose-acetaminophen-btn"
                aria-pressed={doseMedSlug === 'acetaminophen'}
                onClick={() => {
                  setDoseMedSlug('acetaminophen');
                  setDoseForm('');
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
                data-testid="timeline-dose-ibuprofen-btn"
                aria-pressed={doseMedSlug === 'ibuprofen'}
                onClick={() => {
                  setDoseMedSlug('ibuprofen');
                  setDoseForm('');
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
              <label htmlFor="timeline-dose-form-input" className="block text-xs font-bold">شکل و غلظت واقعی روی برچسب دارو</label>
              <input
                id="timeline-dose-form-input"
                data-testid="timeline-dose-form-input"
                type="text"
                value={doseForm}
                onChange={(e) => setDoseForm(e.target.value)}
                placeholder="عیناً مطابق برچسب داروی مصرف‌شده"
                className="mt-1.5 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
              />
            </div>

            <div>
              <label htmlFor="timeline-dose-amount-input" className="block text-xs font-bold text-[#122823] dark:text-[#EDF4F7]">
                حجم یا مقدار مصرف‌شده
              </label>
              <input
                id="timeline-dose-amount-input"
                data-testid="timeline-dose-amount-input"
                type="text"
                inputMode="decimal"
                value={doseMl}
                onChange={(e) => setDoseMl(e.target.value)}
                placeholder="عدد مقدار واقعاً مصرف‌شده"
                className="mt-1.5 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
              />
            </div>

            <div>
              <label htmlFor="timeline-dose-unit-select" className="block text-xs font-bold">واحد مقدار مصرف‌شده</label>
              <select
                id="timeline-dose-unit-select"
                data-testid="timeline-dose-unit-select"
                value={doseUnit}
                onChange={(e) => setDoseUnit(e.target.value)}
                className="mt-1.5 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
              >
                <option value="" disabled>واحد واقعی را انتخاب کنید</option>
                {TIMELINE_DOSE_UNITS.map((unit) => <option key={unit} value={unit}>{unit}</option>)}
              </select>
            </div>

            <div>
              <label htmlFor="timeline-dose-time-input" className="block text-xs font-bold">زمان واقعی مصرف دارو</label>
              <input
                id="timeline-dose-time-input"
                data-testid="timeline-dose-time-input"
                type="datetime-local"
                value={doseTime}
                max={formatTimelineDateTimeInput(entryNowMs)}
                onChange={(e) => { setEntryNowMs(Date.now()); setDoseTime(e.target.value); }}
                className="mt-1.5 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
              />
            </div>

            <div>
              <label htmlFor="timeline-dose-note-input" className="block text-xs font-bold text-[#122823] dark:text-[#EDF4F7]">یادداشت (اختیاری)</label>
              <input
                id="timeline-dose-note-input"
                type="text"
                value={doseNote}
                onChange={(e) => setDoseNote(e.target.value)}
                placeholder="پس از غذا / سرنگ مدرج..."
                className="mt-1.5 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
              />
            </div>

            <button
              type="submit"
              data-testid="timeline-dose-save-btn"
              disabled={!validDose}
              className="min-h-[44px] w-full rounded-xl bg-[#105B48] px-4 text-sm font-bold text-white hover:bg-[#0C493A] disabled:opacity-50 dark:bg-[#00D7C7] dark:text-[#0B1220]"
            >
              ثبت نوبت در جدول ایمنی
            </button>
          </form>
        </AccessibleDialog>
      ) : null}
    </section>
  );
};
