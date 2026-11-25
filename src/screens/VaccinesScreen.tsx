import React, { useMemo, useState } from 'react';
import { ArrowRight, Bell, Calendar, CheckCircle2, Clock, RotateCcw, Syringe } from 'lucide-react';
import type { VaccineFilter } from '../domain/models.ts';
import {
  formatPersianDate,
  parseFlexibleDateToEpochMs,
} from '../domain/vaccinationScheduleEngine.ts';
import { toPersianDigits } from '../domain/persianNormalizer.ts';
import { useMano } from '../state/ManoStore.tsx';
import { SourceBadge } from '../components/SourceBadge.tsx';

const FILTER_OPTIONS: { id: VaccineFilter; labelFa: string }[] = [
  { id: 'ALL', labelFa: 'همه نوبت‌ها' },
  { id: 'ACTIONABLE', labelFa: 'نیازمند اقدام / نزدیک' },
  { id: 'OVERDUE', labelFa: 'عقب‌افتاده' },
  { id: 'ADMINISTERED', labelFa: 'تزریق‌شده' },
];

export const VaccinesScreen: React.FC = () => {
  const {
    vaccineBirthDateEpochMs,
    setVaccineBirthDateEpochMs,
    vaccineSummary,
    vaccineReminders,
    setVaccineReminders,
    markVaccineAdministered,
    clearVaccineRecord,
    toggleVaccineDeferred,
    navigate,
    notify,
  } = useMano();

  const [birthDateInput, setBirthDateInput] = useState(() =>
    vaccineBirthDateEpochMs ? formatPersianDate(vaccineBirthDateEpochMs) : '',
  );
  const [filter, setFilter] = useState<VaccineFilter>('ALL');

  const handleApplyBirthDate = (e: React.FormEvent) => {
    e.preventDefault();
    const epochMs = parseFlexibleDateToEpochMs(birthDateInput);
    if (!epochMs) {
      notify('فرمت تاریخ معتبر نیست. مثال شمسی: ۱۴۰۳/۰۵/۱۲ یا میلادی: 2024-08-03', 'danger');
      return;
    }
    setVaccineBirthDateEpochMs(epochMs);
  };

  const filteredItems = useMemo(() => {
    return vaccineSummary.items.filter((item) => {
      if (filter === 'ADMINISTERED') return item.status === 'ADMINISTERED';
      if (filter === 'OVERDUE') return item.status === 'OVERDUE';
      if (filter === 'ACTIONABLE') return item.status === 'OVERDUE' || item.status === 'DUE_SOON';
      return true;
    });
  }, [vaccineSummary.items, filter]);

  return (
    <div className="space-y-6" data-testid="vaccines-screen">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            جدول واکسیناسیون کشوری کودکان
          </h1>
          <p className="mt-1 text-xs text-[#546963] dark:text-[#9EB3C4]">
            محاسبه خودکار موعد نوبت‌ها بر اساس تاریخ تولد شمسی یا میلادی و ثبت تاریخ تزریق
          </p>
        </div>
        <button
          type="button"
          onClick={() => navigate({ name: 'home' })}
          className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] px-3 text-xs font-bold dark:border-[#23344D]"
        >
          <ArrowRight className="h-4 w-4" />
          <span>خانه</span>
        </button>
      </div>

      {/* Birth Date Input & Summary KPIs */}
      <section className="grid grid-cols-1 gap-5 lg:grid-cols-12">
        <form
          onSubmit={handleApplyBirthDate}
          className="rounded-3xl border border-[#DCE7E1] bg-white p-5 space-y-3 lg:col-span-5 dark:border-[#23344D] dark:bg-[#111C2E]"
        >
          <label htmlFor="vaccine-birthdate-input" className="flex items-center gap-2 text-sm font-extrabold">
            <Calendar className="h-4 w-4 text-[#105B48] dark:text-[#00D7C7]" />
            <span>تاریخ تولد کودک (شمسی یا میلادی)</span>
          </label>
          <div className="flex gap-2">
            <input
              id="vaccine-birthdate-input"
              data-testid="vaccine-birthdate-input"
              type="text"
              value={birthDateInput}
              onChange={(e) => setBirthDateInput(e.target.value)}
              placeholder="مثال: 1403/06/15"
              className="min-h-[44px] flex-1 rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm tabular-nums dark:border-[#23344D] dark:bg-[#16243B]"
            />
            <button
              type="submit"
              data-testid="vaccine-birthdate-apply-btn"
              className="min-h-[44px] rounded-xl bg-[#105B48] px-4 text-xs font-bold text-white dark:bg-[#00D7C7] dark:text-[#0B1220]"
            >
              تنظیم تاریخ
            </button>
          </div>
          {vaccineBirthDateEpochMs ? (
            <p className="text-xs text-[#105B48] dark:text-[#00D7C7]">
              تاریخ تولد فعال: {formatPersianDate(vaccineBirthDateEpochMs)}
            </p>
          ) : null}

          <div className="flex flex-wrap items-center justify-between gap-2 border-t border-[#DCE7E1] pt-3 text-xs dark:border-[#23344D]">
            <label className="flex items-center gap-2 font-bold">
              <input
                type="checkbox"
                checked={vaccineReminders.enabled}
                onChange={(e) =>
                  setVaccineReminders({ ...vaccineReminders, enabled: e.target.checked })
                }
                className="h-4 w-4 accent-[#105B48]"
              />
              <Bell className="h-3.5 w-3.5 text-[#105B48] dark:text-[#00D7C7]" />
              <span>یادآوری پیش از موعد</span>
            </label>
            <select
              value={vaccineReminders.leadDaysBefore}
              onChange={(e) =>
                setVaccineReminders({ ...vaccineReminders, leadDaysBefore: Number(e.target.value) })
              }
              aria-label="تعداد روز یادآور پیش از موعد"
              className="rounded-lg border border-[#DCE7E1] bg-[#F5F9F6] px-2 py-1 text-xs dark:border-[#23344D] dark:bg-[#16243B]"
            >
              <option value={1}>۱ روز قبل</option>
              <option value={3}>۳ روز قبل</option>
              <option value={7}>۷ روز قبل</option>
            </select>
          </div>
        </form>

        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:col-span-7">
          <div className="rounded-3xl border border-[#DCE7E1] bg-white p-4 dark:border-[#23344D] dark:bg-[#111C2E]">
            <div className="text-xs text-[#546963] dark:text-[#9EB3C4]">تزریق‌شده</div>
            <div className="mt-1 tabular-nums text-2xl font-extrabold text-[#105B48] dark:text-[#00D7C7]">
              {toPersianDigits(vaccineSummary.administeredCoreCount)} / {toPersianDigits(vaccineSummary.totalCoreCount)}
            </div>
          </div>
          <div className="rounded-3xl border border-[#DCE7E1] bg-white p-4 dark:border-[#23344D] dark:bg-[#111C2E]">
            <div className="text-xs text-[#546963] dark:text-[#9EB3C4]">نزدیک موعد</div>
            <div className="mt-1 tabular-nums text-2xl font-extrabold text-[#9F5F0D] dark:text-[#F0B85C]">
              {toPersianDigits(vaccineSummary.dueSoonCount)}
            </div>
          </div>
          <div className="rounded-3xl border border-[#DCE7E1] bg-white p-4 dark:border-[#23344D] dark:bg-[#111C2E]">
            <div className="text-xs text-[#546963] dark:text-[#9EB3C4]">عقب‌افتاده</div>
            <div className="mt-1 tabular-nums text-2xl font-extrabold text-[#B32334] dark:text-[#FF8594]">
              {toPersianDigits(vaccineSummary.overdueCount)}
            </div>
          </div>
          <div className="rounded-3xl border border-[#DCE7E1] bg-white p-4 dark:border-[#23344D] dark:bg-[#111C2E]">
            <div className="text-xs text-[#546963] dark:text-[#9EB3C4]">نوبت بعدی</div>
            <div className="mt-1 text-xs font-extrabold text-[#122823] dark:text-[#EDF4F7]">
              {vaccineSummary.nextUpcoming ? vaccineSummary.nextUpcoming.definition.titleFa : 'کامل / نامشخص'}
            </div>
          </div>
        </div>
      </section>

      {/* Filter Bar */}
      <div className="flex flex-wrap gap-2">
        {FILTER_OPTIONS.map((opt) => (
          <button
            key={opt.id}
            type="button"
            onClick={() => setFilter(opt.id)}
            className={`min-h-[40px] rounded-xl border px-3.5 text-xs font-bold ${
              filter === opt.id
                ? 'border-[#105B48] bg-[#E9F5EB] text-[#105B48] dark:border-[#00D7C7] dark:bg-[#0F333A] dark:text-[#00D7C7]'
                : 'border-[#DCE7E1] bg-white text-[#546963] dark:border-[#23344D] dark:bg-[#111C2E]'
            }`}
          >
            {opt.labelFa}
          </button>
        ))}
      </div>

      {/* Vaccine Cards */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        {filteredItems.map((item) => {
          const badgeColors =
            item.status === 'ADMINISTERED'
              ? 'text-[#105B48] dark:text-[#35E0C9]'
              : item.status === 'OVERDUE'
                ? 'text-[#B32334] dark:text-[#FF8594]'
                : item.status === 'DUE_SOON'
                  ? 'text-[#9F5F0D] dark:text-[#F5C169]'
                  : 'text-[#546963] dark:text-[#9EB3C4]';

          const statusText =
            item.status === 'ADMINISTERED'
              ? item.administeredDateEpochMs
                ? `تزریق شده (${formatPersianDate(item.administeredDateEpochMs)})`
                : 'تزریق شده'
              : item.status === 'OVERDUE'
                ? 'نوبت گذشته — پیگیری با مرکز بهداشت'
                : item.status === 'DUE_SOON'
                  ? 'نزدیک موعد تزریق'
                  : item.status === 'DEFERRED'
                    ? 'تعویق موقت با نظر پزشک'
                    : 'پیش رو';

          return (
            <article
              key={item.definition.id}
              data-testid={`vaccine-card-${item.definition.id}`}
              className="rounded-3xl border border-[#DCE7E1] bg-white p-5 space-y-3 dark:border-[#23344D] dark:bg-[#111C2E]"
            >
              <div className="flex items-start justify-between gap-2">
                <div>
                  <div className="text-xs font-semibold text-[#546963] dark:text-[#9EB3C4]">
                    {item.definition.milestoneLabelFa}
                    {item.effectiveDueDateEpochMs
                      ? ` · موعد: ${formatPersianDate(item.effectiveDueDateEpochMs)}`
                      : ''}
                  </div>
                  <h2 className="mt-1 text-base font-extrabold text-[#122823] dark:text-[#EDF4F7]">
                    {item.definition.titleFa}
                  </h2>
                </div>
                <Syringe className="h-5 w-5 shrink-0 text-[#105B48] dark:text-[#00D7C7]" />
              </div>

              <div className={`flex items-center gap-1.5 text-xs font-bold ${badgeColors}`}>
                {item.status === 'ADMINISTERED' ? (
                  <CheckCircle2 className="h-4 w-4" />
                ) : (
                  <Clock className="h-4 w-4" />
                )}
                <span>{statusText}</span>
              </div>

              <div className="rounded-2xl bg-[#F5F9F6] p-3 text-xs leading-6 dark:bg-[#16243B]">
                <div className="font-bold">واکسن‌های شامل‌شده: {item.definition.vaccinesIncludedFa}</div>
                <div className="mt-1 text-[#546963] dark:text-[#9EB3C4]">
                  روش تجویز: {item.definition.administrationMethodFa}
                </div>
                <p className="mt-1 text-[#546963] dark:text-[#9EB3C4]">{item.definition.careNotesFa}</p>
              </div>

              <div className="flex flex-wrap gap-2 pt-1">
                {item.status !== 'ADMINISTERED' ? (
                  <button
                    type="button"
                    data-testid={`vaccine-mark-${item.definition.id}`}
                    onClick={() => markVaccineAdministered(item.definition.id, Date.now())}
                    className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl bg-[#105B48] px-3.5 text-xs font-bold text-white dark:bg-[#00D7C7] dark:text-[#0B1220]"
                  >
                    <CheckCircle2 className="h-4 w-4" />
                    <span>ثبت تزریق امروز</span>
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={() => clearVaccineRecord(item.definition.id)}
                    className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] px-3 text-xs font-bold dark:border-[#23344D]"
                  >
                    <RotateCcw className="h-3.5 w-3.5" />
                    <span>بازگردانی وضعیت</span>
                  </button>
                )}

                {item.status !== 'ADMINISTERED' ? (
                  <button
                    type="button"
                    onClick={() => toggleVaccineDeferred(item.definition.id)}
                    className="inline-flex min-h-[40px] items-center rounded-xl border border-[#DCE7E1] px-3 text-xs font-semibold text-[#546963] dark:border-[#23344D] dark:text-[#9EB3C4]"
                  >
                    {item.record?.isDeferred ? 'رفع تعویق' : 'تعویق موقت با نظر پزشک'}
                  </button>
                ) : null}
              </div>
            </article>
          );
        })}
      </div>

      <SourceBadge sourceIds={['cdc_milestones']} />
    </div>
  );
};
