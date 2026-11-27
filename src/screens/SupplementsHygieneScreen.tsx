import React, { useMemo, useRef, useState } from 'react';
import {
  ArrowRight,
  CheckCircle2,
  ChevronLeft,
  ListChecks,
  Search,
  Sparkles,
  Stethoscope,
  X,
} from 'lucide-react';
import {
  DAILY_HYGIENE_CHECKLIST,
  SUPPLEMENTS_AND_HYGIENE_GUIDES,
} from '../data/contentRepository.ts';
import type { SupplementOrHygieneGuide } from '../domain/models.ts';
import { scoreMatch, toPersianDigits } from '../domain/persianNormalizer.ts';
import { useMano } from '../state/ManoStore.tsx';
import { SourceBadge } from '../components/SourceBadge.tsx';
import { SupplementsHygieneRecommendedCard } from '../components/SupplementsHygieneRecommendedCard.tsx';
import { SeverityAlert } from '../components/SeverityAlert.tsx';
import { AccessibleDialog } from '../components/AccessibleDialog.tsx';

type AgeFilter = 'ALL' | '0_6m' | '6_24m' | '2_6y';

export const SupplementsHygieneScreen: React.FC<{ initialTab?: 'supplements' | 'hygiene' }> = ({
  initialTab = 'supplements',
}) => {
  const { checkedHygieneIds, toggleHygieneItem, navigate } = useMano();
  const [activeTab, setActiveTab] = useState<'supplements' | 'hygiene'>(initialTab);
  const [ageFilter, setAgeFilter] = useState<AgeFilter>('ALL');
  const [query, setQuery] = useState('');
  const [selectedGuide, setSelectedGuide] = useState<SupplementOrHygieneGuide | null>(null);
  const [checklistModalOpen, setChecklistModalOpen] = useState(false);
  const guideTitleRef = useRef<HTMLHeadingElement>(null);
  const checklistTitleRef = useRef<HTMLHeadingElement>(null);

  const filteredGuides = useMemo(() => {
    return SUPPLEMENTS_AND_HYGIENE_GUIDES.filter((g) => {
      if (g.tab !== activeTab) return false;
      if (ageFilter !== 'ALL' && !g.ageFilters.includes(ageFilter)) return false;
      if (!query.trim()) return true;
      return scoreMatch(query, [g.titleFa, g.shortTitleFa, g.categoryFa, g.summaryFa]) > 0;
    });
  }, [activeTab, ageFilter, query]);

  const completedCount = DAILY_HYGIENE_CHECKLIST.filter((c) => checkedHygieneIds.includes(c.id)).length;

  return (
    <div className="space-y-6" data-testid="supplements-hygiene-screen">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            راهنمای آموزشی مکمل و بهداشت کودک
          </h1>
          <p className="mt-1 text-xs text-[#546963] dark:text-[#9EB3C4]">
            مرور موضوعات ویتامین، آهن، بهداشت دهان، حمام و پوشک؛ محتوای نیازمند تأیید بالینی
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

      {/* Daily Routine Progress Card */}
      <section className="flex flex-wrap items-center justify-between gap-4 rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]">
        <div className="flex items-center gap-3">
          <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#E9F5EB] text-[#105B48] dark:bg-[#0F333A] dark:text-[#00D7C7]">
            <ListChecks className="h-5 w-5" />
          </span>
          <div>
            <h2 className="text-sm font-extrabold text-[#122823] dark:text-[#EDF4F7]">
              چک‌لیست روزانه مکمل و بهداشت کودک
            </h2>
            <p className="text-xs text-[#546963] dark:text-[#9EB3C4]">
              انجام‌شده: {toPersianDigits(completedCount)} از {toPersianDigits(DAILY_HYGIENE_CHECKLIST.length)} مورد روزانه
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={() => setChecklistModalOpen(true)}
          data-testid="supplements-open-checklist-btn"
          aria-haspopup="dialog"
          className="min-h-[44px] rounded-xl bg-[#105B48] px-4 text-xs font-bold text-white hover:bg-[#0C493A] dark:bg-[#00D7C7] dark:text-[#0B1220]"
        >
          تیک زدن برنامه امروز
        </button>
      </section>

      {/* Tabs + Age Filter + Search */}
      <section className="space-y-3 rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]">
        <div className="grid grid-cols-2 gap-2" role="tablist" aria-label="بخش‌های مکمل و بهداشت">
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'supplements'}
            onClick={() => setActiveTab('supplements')}
            className={`min-h-[44px] rounded-2xl border text-xs font-extrabold transition-colors ${
              activeTab === 'supplements'
                ? 'border-[#105B48] bg-[#105B48] text-white dark:border-[#00D7C7] dark:bg-[#00D7C7] dark:text-[#0B1220]'
                : 'border-[#DCE7E1] bg-[#F5F9F6] text-[#546963] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#9EB3C4]'
            }`}
          >
            مکمل‌های پایه کودک
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'hygiene'}
            onClick={() => setActiveTab('hygiene')}
            className={`min-h-[44px] rounded-2xl border text-xs font-extrabold transition-colors ${
              activeTab === 'hygiene'
                ? 'border-[#105B48] bg-[#105B48] text-white dark:border-[#00D7C7] dark:bg-[#00D7C7] dark:text-[#0B1220]'
                : 'border-[#DCE7E1] bg-[#F5F9F6] text-[#546963] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#9EB3C4]'
            }`}
          >
            بهداشت، دهان، حمام و پوشک
          </button>
        </div>

        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap gap-2">
            {(
              [
                { id: 'ALL', label: 'همه سنین' },
                { id: '0_6m', label: '۰ تا ۶ ماه' },
                { id: '6_24m', label: '۶ تا ۲۴ ماه' },
                { id: '2_6y', label: '۲ تا ۶ سال' },
              ] as const
            ).map((f) => (
              <button
                key={f.id}
                type="button"
                onClick={() => setAgeFilter(f.id)}
                className={`min-h-[38px] rounded-xl border px-3 text-xs font-bold ${
                  ageFilter === f.id
                    ? 'border-[#105B48] bg-[#E9F5EB] text-[#105B48] dark:border-[#00D7C7] dark:bg-[#0F333A] dark:text-[#00D7C7]'
                    : 'border-[#DCE7E1] bg-[#F5F9F6] text-[#546963] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#9EB3C4]'
                }`}
              >
                {f.label}
              </button>
            ))}
          </div>

          <div className="relative w-full sm:w-64">
            <Search className="pointer-events-none absolute right-3 top-3 h-4 w-4 text-[#546963]" />
            <input
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="جست‌وجو: آهن، ویتامین، مسواک..."
              className="min-h-[40px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] pr-9 pl-3 text-xs dark:border-[#23344D] dark:bg-[#16243B]"
            />
          </div>
        </div>
      </section>

      {/* Guides Grid */}
      <section className="grid grid-cols-1 gap-3 md:grid-cols-2">
        {filteredGuides.map((guide) => (
          <button
            key={guide.id}
            type="button"
            onClick={() => setSelectedGuide(guide)}
            data-testid={`supplement-guide-${guide.id}`}
            aria-haspopup="dialog"
            className="flex items-start justify-between gap-3 rounded-3xl border border-[#DCE7E1] bg-white p-5 text-right transition-colors hover:border-[#105B48] dark:border-[#23344D] dark:bg-[#111C2E] dark:hover:border-[#00D7C7]"
          >
            <div className="space-y-1.5">
              <div className="text-xs font-semibold text-[#105B48] dark:text-[#00D7C7]">
                {guide.ageGroupFa} · {guide.sourceLabelFa}
              </div>
              <h3 className="text-base font-extrabold text-[#122823] dark:text-[#EDF4F7]">{guide.titleFa}</h3>
              <p className="text-xs leading-5 text-[#546963] dark:text-[#9EB3C4]">{guide.summaryFa}</p>
            </div>
            <ChevronLeft className="mt-1 h-5 w-5 shrink-0 text-[#546963]" />
          </button>
        ))}
      </section>

      {/* Non-commercial Recommended Categories */}
      <SupplementsHygieneRecommendedCard
        onAskPharmacist={(cat) => navigate({ name: 'ask-pharmacist', initialTopic: cat })}
      />

      {/* Guide Detail Modal */}
      {selectedGuide ? (
        <AccessibleDialog
          id="mano-supplement-guide-dialog"
          label={selectedGuide.titleFa}
          initialFocusRef={guideTitleRef}
          fallbackFocusId="mano-main-content"
          onClose={() => setSelectedGuide(null)}
          closeOnBackdrop
          backdropClassName="fixed inset-0 z-50 flex items-center justify-center bg-[#0B1220]/70 p-4 backdrop-blur-sm"
          panelClassName="max-h-[calc(100dvh-2rem)] w-full max-w-xl space-y-4 overflow-y-auto overscroll-contain rounded-3xl border border-[#DCE7E1] bg-white p-5 outline-none dark:border-[#23344D] dark:bg-[#111C2E] sm:p-6"
        >
            <div className="flex items-start justify-between gap-2">
              <div className="min-w-0 flex-1 break-words">
                <span className="text-xs font-bold text-[#105B48] dark:text-[#00D7C7]">
                  {selectedGuide.categoryFa} · {selectedGuide.ageGroupFa}
                </span>
                <h2 ref={guideTitleRef} tabIndex={-1} className="mt-1 text-lg font-extrabold text-[#122823] outline-none dark:text-[#EDF4F7]">
                  {selectedGuide.titleFa}
                </h2>
              </div>
              <button
                type="button"
                onClick={() => setSelectedGuide(null)}
                aria-label="بستن"
                className="flex min-h-[44px] min-w-[44px] shrink-0 items-center justify-center rounded-xl text-[#546963]"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="rounded-2xl bg-[#F5F9F6] p-4 dark:bg-[#16243B]">
              <h3 className="text-xs font-extrabold text-[#105B48] dark:text-[#00D7C7]">
                {selectedGuide.howToUseTitleFa}
              </h3>
              <ul className="mt-2 list-disc space-y-1.5 pr-5 text-xs leading-6">
                {selectedGuide.howToUseFa.map((item, idx) => (
                  <li key={idx}>{item}</li>
                ))}
              </ul>
            </div>

            <SeverityAlert
              severity="caution"
              title="هشدارها و نکات احتیاط"
              bullets={[...selectedGuide.safetyChecksFa, ...selectedGuide.avoidOrCautionFa]}
            />

            <div className="flex items-start gap-2 rounded-2xl border border-[#DCE7E1] bg-[#FAF7F0] p-3.5 text-xs leading-6 dark:border-[#23344D] dark:bg-[#16243B]">
              <Sparkles className="mt-0.5 h-4 w-4 shrink-0 text-[#C6862E]" />
              <div>
                <strong className="font-bold">یادداشت آموزشی: </strong>
                {selectedGuide.pharmacistNoteFa}
              </div>
            </div>

            <SourceBadge sourceIds={selectedGuide.sourceIds} reviewStatus="NEEDS_CLINICAL_REVIEW" compact />

            <button
              type="button"
              onClick={() => {
                setSelectedGuide(null);
                navigate({
                  name: 'ask-pharmacist',
                  initialTopic: selectedGuide.tab === 'supplements' ? 'supplement' : 'skin',
                });
              }}
              className="flex min-h-[44px] w-full items-center justify-center gap-2 rounded-xl bg-[#105B48] px-4 text-xs font-bold text-white dark:bg-[#00D7C7] dark:text-[#0B1220]"
            >
              <Stethoscope className="h-4 w-4" />
              <span>نوشتن پرسش درباره این موضوع</span>
            </button>
        </AccessibleDialog>
      ) : null}

      {/* Daily Checklist Modal */}
      {checklistModalOpen ? (
        <AccessibleDialog
          id="mano-hygiene-checklist-dialog"
          label="چک‌لیست روزانه بهداشت و مکمل"
          initialFocusRef={checklistTitleRef}
          fallbackFocusId="mano-main-content"
          onClose={() => setChecklistModalOpen(false)}
          closeOnBackdrop
          backdropClassName="fixed inset-0 z-50 flex items-center justify-center bg-[#0B1220]/70 p-4 backdrop-blur-sm"
          panelClassName="max-h-[calc(100dvh-2rem)] w-full max-w-md space-y-4 overflow-y-auto overscroll-contain rounded-3xl border border-[#DCE7E1] bg-white p-5 outline-none dark:border-[#23344D] dark:bg-[#111C2E] sm:p-6"
        >
            <div className="flex items-center justify-between gap-2">
              <h3 ref={checklistTitleRef} tabIndex={-1} className="min-w-0 break-words text-base font-extrabold text-[#122823] outline-none dark:text-[#EDF4F7]">
                روتین روزانه مکمل و بهداشت
              </h3>
              <button
                type="button"
                onClick={() => setChecklistModalOpen(false)}
                aria-label="بستن"
                className="flex min-h-[44px] min-w-[44px] shrink-0 items-center justify-center rounded-xl text-[#546963]"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="space-y-2">
              {DAILY_HYGIENE_CHECKLIST.map((item) => {
                const checked = checkedHygieneIds.includes(item.id);
                return (
                  <label
                    key={item.id}
                    className={`flex cursor-pointer items-center gap-3 rounded-2xl border p-3 text-xs font-bold ${
                      checked
                        ? 'border-[#B6DFC1] bg-[#EBF7EE] text-[#105B48] dark:border-[#21594B] dark:bg-[#102B25] dark:text-[#35E0C9]'
                        : 'border-[#DCE7E1] bg-[#F5F9F6] text-[#122823] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#EDF4F7]'
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={() => toggleHygieneItem(item.id)}
                      className="h-5 w-5 accent-[#105B48]"
                    />
                    <CheckCircle2 className={`h-4 w-4 ${checked ? 'opacity-100' : 'opacity-30'}`} />
                    <span>{item.labelFa}</span>
                  </label>
                );
              })}
            </div>

            <button
              type="button"
              onClick={() => setChecklistModalOpen(false)}
              className="min-h-[44px] w-full rounded-xl bg-[#105B48] text-xs font-bold text-white dark:bg-[#00D7C7] dark:text-[#0B1220]"
            >
              تایید و بستن
            </button>
        </AccessibleDialog>
      ) : null}
    </div>
  );
};
