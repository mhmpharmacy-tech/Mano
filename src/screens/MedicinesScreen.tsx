import React, { useMemo, useState } from 'react';
import { ArrowRight, ArrowLeftRight, CheckSquare, ChevronLeft, Pill, Search, ShieldCheck } from 'lucide-react';
import { ALL_MEDICATIONS } from '../data/contentRepository.ts';
import { evaluateMedicationOverlap } from '../domain/medicationOverlapEngine.ts';
import {
  MEDICATION_CHECK_CATEGORIES,
  MEDICATION_SAFETY_CHECKLIST_ITEMS,
  evaluateChecklistProgress,
} from '../domain/medicationSafetyChecklist.ts';
import { scoreMatch, toPersianDigits } from '../domain/persianNormalizer.ts';
import { useMano } from '../state/ManoStore.tsx';
import { SeverityAlert } from '../components/SeverityAlert.tsx';

export const MedicinesScreen: React.FC = () => {
  const { checkedMedSafetyIds, toggleMedSafetyItem, navigate } = useMano();
  const [query, setQuery] = useState('');
  const [firstSlug, setFirstSlug] = useState(ALL_MEDICATIONS[0].slug);
  const [secondSlug, setSecondSlug] = useState(ALL_MEDICATIONS[2].slug);

  const filteredMeds = useMemo(() => {
    if (!query.trim()) return ALL_MEDICATIONS;
    return ALL_MEDICATIONS.filter(
      (m) =>
        scoreMatch(query, [m.nameFa, m.genericNameEn, m.categoryFa, ...m.aliasesSearchTermsFa]) > 0,
    );
  }, [query]);

  const firstMed = ALL_MEDICATIONS.find((m) => m.slug === firstSlug) ?? ALL_MEDICATIONS[0];
  const secondMed = ALL_MEDICATIONS.find((m) => m.slug === secondSlug) ?? ALL_MEDICATIONS[1];
  const overlapResult = useMemo(
    () => evaluateMedicationOverlap(firstMed, secondMed),
    [firstMed, secondMed],
  );

  const checklistProgress = useMemo(
    () => evaluateChecklistProgress(new Set(checkedMedSafetyIds)),
    [checkedMedSafetyIds],
  );

  return (
    <div className="space-y-6" data-testid="medicines-screen">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            دارونامه اطفال، بررسی تداخل و چک‌لیست ایمنی
          </h1>
          <p className="mt-1 text-xs text-[#546963] dark:text-[#9EB3C4]">
            ۱۰ راهنمای تخصصی داروی کودک همراه با بررسی‌گر تداخل و ۸ گام ایمنی پیش از خوراندن دارو
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

      {/* Pairwise Overlap Checker */}
      <section className="rounded-3xl border border-[#DCE7E1] bg-white p-5 space-y-4 dark:border-[#23344D] dark:bg-[#111C2E]">
        <div className="flex items-center gap-2 text-sm font-extrabold text-[#105B48] dark:text-[#00D7C7]">
          <ArrowLeftRight className="h-5 w-5" />
          <span>بررسی‌گر سریع هم‌پوشانی و تداخل دو دارو</span>
        </div>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div>
            <label className="block text-xs font-bold">داروی اول</label>
            <select
              value={firstSlug}
              onChange={(e) => setFirstSlug(e.target.value)}
              className="mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
            >
              {ALL_MEDICATIONS.map((m) => (
                <option key={m.slug} value={m.slug}>
                  {m.nameFa}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-xs font-bold">داروی دوم</label>
            <select
              value={secondSlug}
              onChange={(e) => setSecondSlug(e.target.value)}
              className="mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
            >
              {ALL_MEDICATIONS.map((m) => (
                <option key={m.slug} value={m.slug}>
                  {m.nameFa}
                </option>
              ))}
            </select>
          </div>
        </div>

        <SeverityAlert
          severity={overlapResult.severity}
          title={overlapResult.titleFa}
          body={overlapResult.summaryFa}
          bullets={overlapResult.actionStepsFa}
        />
      </section>

      {/* Searchable 10 Medications Grid */}
      <section className="space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-base font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            فهرست داروهای کودک ({toPersianDigits(filteredMeds.length)})
          </h2>
          <div className="relative w-full sm:w-72">
            <Search className="pointer-events-none absolute right-3 top-3 h-4 w-4 text-[#546963]" />
            <input
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="جست‌وجو: استامینوفن، سیتیریزین، قطره آهن..."
              className="min-h-[42px] w-full rounded-xl border border-[#DCE7E1] bg-white pr-9 pl-3 text-xs dark:border-[#23344D] dark:bg-[#111C2E]"
            />
          </div>
        </div>

        <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
          {filteredMeds.map((med) => (
            <button
              key={med.slug}
              type="button"
              onClick={() => navigate({ name: 'medicine-detail', slug: med.slug })}
              className="flex items-start justify-between gap-3 rounded-3xl border border-[#DCE7E1] bg-white p-5 text-right transition-colors hover:border-[#105B48] dark:border-[#23344D] dark:bg-[#111C2E] dark:hover:border-[#00D7C7]"
            >
              <div className="flex items-start gap-3">
                <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-[#E9F5EB] text-[#105B48] dark:bg-[#0F333A] dark:text-[#00D7C7]">
                  <Pill className="h-5 w-5" />
                </span>
                <div>
                  <div className="text-xs font-semibold text-[#546963] dark:text-[#9EB3C4]">
                    {med.categoryFa} · {med.genericNameEn}
                  </div>
                  <h3 className="mt-1 text-base font-extrabold text-[#122823] dark:text-[#EDF4F7]">
                    {med.nameFa}
                  </h3>
                  <p className="mt-1 text-xs leading-5 text-[#546963] dark:text-[#9EB3C4]">
                    {med.shortSummaryFa}
                  </p>
                </div>
              </div>
              <ChevronLeft className="mt-1 h-5 w-5 shrink-0 text-[#546963]" />
            </button>
          ))}
        </div>
      </section>

      {/* 8-Step Medication Safety Checklist */}
      <section className="rounded-3xl border border-[#DCE7E1] bg-white p-5 space-y-4 dark:border-[#23344D] dark:bg-[#111C2E]">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div>
            <h2 className="flex items-center gap-2 text-base font-extrabold">
              <ShieldCheck className="h-5 w-5 text-[#105B48] dark:text-[#00D7C7]" />
              <span>چک‌لیست ۸ مرحله‌ای ایمنی مصرف دارو</span>
            </h2>
            <p className="mt-0.5 text-xs text-[#546963] dark:text-[#9EB3C4]">
              {checklistProgress.statusHeadlineFa} — {checklistProgress.statusSublineFa}
            </p>
          </div>
          <span className="tabular-nums text-sm font-extrabold text-[#105B48] dark:text-[#00D7C7]">
            {toPersianDigits(checklistProgress.checkedCount)} از {toPersianDigits(checklistProgress.totalCount)}
          </span>
        </div>

        <div className="h-2 w-full overflow-hidden rounded-full bg-[#DCE7E1] dark:bg-[#23344D]">
          <div
            className="h-full bg-[#105B48] transition-all dark:bg-[#00D7C7]"
            style={{ width: `${Math.round(checklistProgress.progressFraction * 100)}%` }}
          />
        </div>

        <div className="space-y-4">
          {MEDICATION_CHECK_CATEGORIES.map((cat) => (
            <div key={cat.id} className="space-y-2">
              <h3 className="text-xs font-extrabold text-[#105B48] dark:text-[#00D7C7]">{cat.labelFa}</h3>
              <div className="space-y-2">
                {MEDICATION_SAFETY_CHECKLIST_ITEMS.filter((i) => i.category === cat.id).map((item) => {
                  const checked = checkedMedSafetyIds.includes(item.id);
                  return (
                    <label
                      key={item.id}
                      className={`flex cursor-pointer items-start gap-3 rounded-2xl border p-3 text-xs leading-5 ${
                        checked
                          ? 'border-[#B6DFC1] bg-[#EBF7EE] dark:border-[#21594B] dark:bg-[#102B25]'
                          : 'border-[#DCE7E1] bg-[#F5F9F6] dark:border-[#23344D] dark:bg-[#16243B]'
                      }`}
                    >
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() => toggleMedSafetyItem(item.id)}
                        className="mt-0.5 h-5 w-5 shrink-0 accent-[#105B48]"
                      />
                      <div>
                        <div className="font-bold text-[#122823] dark:text-[#EDF4F7]">
                          <CheckSquare className="ml-1 inline h-3.5 w-3.5 text-[#105B48] dark:text-[#00D7C7]" />
                          {item.stepNumberFa}: {item.titleFa}
                        </div>
                        <div className="mt-0.5 text-[#546963] dark:text-[#9EB3C4]">{item.detailTipFa}</div>
                      </div>
                    </label>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
};
