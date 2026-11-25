import React, { useMemo, useState } from 'react';
import {
  ArrowRight,
  ChevronDown,
  ChevronUp,
  HelpCircle,
  Loader2,
  Search,
  Sparkles,
  Stethoscope,
} from 'lucide-react';
import { ALL_FAQS, getSourceCitationLabel } from '../data/contentRepository.ts';
import { AGE_BANDS, type AgeBandId, type DocumentedQaResponse } from '../domain/models.ts';
import { scoreMatch, toPersianDigits } from '../domain/persianNormalizer.ts';
import { findMatchingFaqsForQa } from '../domain/symptomSearch.ts';
import { ApiClientError, requestDocumentedQa } from '../api/geminiClient.ts';
import { useMano } from '../state/ManoStore.tsx';
import { SeverityAlert } from '../components/SeverityAlert.tsx';
import { SourceBadge } from '../components/SourceBadge.tsx';

export const FaqScreen: React.FC = () => {
  const { childProfile, navigate } = useMano();
  const [query, setQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('همه');
  const [selectedAgeBand, setSelectedAgeBand] = useState<AgeBandId | 'ALL'>('ALL');
  const [expandedId, setExpandedId] = useState<string | null>(ALL_FAQS[0]?.id ?? null);

  // Documented AI Q&A state
  const [aiQuestion, setAiQuestion] = useState('');
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState<string | null>(null);
  const [aiResult, setAiResult] = useState<DocumentedQaResponse | null>(null);
  const [noMatchHint, setNoMatchHint] = useState(false);

  const categories = useMemo(
    () => ['همه', ...Array.from(new Set(ALL_FAQS.map((f) => f.categoryFa)))],
    [],
  );

  const filteredFaqs = useMemo(() => {
    return ALL_FAQS.filter((faq) => {
      if (selectedCategory !== 'همه' && faq.categoryFa !== selectedCategory) return false;
      if (selectedAgeBand !== 'ALL' && !faq.ageBands.includes(selectedAgeBand)) return false;
      if (!query.trim()) return true;
      return (
        scoreMatch(query, [
          faq.questionFa,
          faq.shortAnswerFa,
          faq.clinicalDetailFa,
          ...faq.keywords,
        ]) > 0
      );
    });
  }, [query, selectedCategory, selectedAgeBand]);

  const activeAgeLabel =
    AGE_BANDS.find((b) => b.id === childProfile.ageBand)?.labelFa ?? 'کودک';

  const handleDocumentedQaSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = aiQuestion.trim();
    if (!trimmed) return;

    setAiError(null);
    setNoMatchHint(false);
    setAiResult(null);

    const matched = findMatchingFaqsForQa(trimmed, ALL_FAQS, 4);
    if (matched.length === 0) {
      setNoMatchHint(true);
      return;
    }

    setAiLoading(true);
    try {
      const response = await requestDocumentedQa({
        question: trimmed,
        ageBandFa: activeAgeLabel,
        matchedFaqs: matched,
        sourceLookup: getSourceCitationLabel,
      });
      setAiResult(response);
    } catch (err) {
      if (err instanceof ApiClientError) {
        setAiError(err.message);
      } else {
        setAiError('در حال حاضر ارتباط با سرویس پاسخ مستند برقرار نشد.');
      }
    } finally {
      setAiLoading(false);
    }
  };

  return (
    <div className="space-y-6" data-testid="faq-screen">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            پرسش‌های متداول مراقبت از کودک و پاسخ مستند
          </h1>
          <p className="mt-1 text-xs text-[#546963] dark:text-[#9EB3C4]">
            {toPersianDigits(ALL_FAQS.length)} پرسش و پاسخ بالینی مرور‌شده بر پایه منابع معتبر اطفال
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

      {/* Grounded Documented AI Q&A Card */}
      <section className="overflow-hidden rounded-3xl border border-[#DCE7E1] bg-white dark:border-[#23344D] dark:bg-[#111C2E]">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#DCE7E1] bg-[#E9F5EB]/60 px-5 py-4 dark:border-[#23344D] dark:bg-[#0F333A]/80">
          <div className="flex items-center gap-2.5">
            <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-[#105B48] text-white dark:bg-[#00D7C7] dark:text-[#0B1220]">
              <Sparkles className="h-5 w-5" />
            </span>
            <div>
              <h2 className="text-sm font-extrabold text-[#122823] dark:text-[#EDF4F7]">
                پاسخ‌گوی مستند بر پایه منابع مانو کید
              </h2>
              <p className="text-xs text-[#546963] dark:text-[#9EB3C4]">
                ابتدا نزدیک‌ترین پرسش‌های تاییدشده پایگاه دانش پیدا می‌شوند و پاسخ فقط بر اساس همان منابع تنظیم می‌شود.
              </p>
            </div>
          </div>
          <span className="text-xs font-bold text-[#105B48] dark:text-[#00D7C7]">
            رده سنی فعلی: {activeAgeLabel}
          </span>
        </div>

        <form onSubmit={handleDocumentedQaSubmit} className="space-y-3 p-5">
          <label htmlFor="documented-qa-input" className="block text-xs font-bold text-[#122823] dark:text-[#EDF4F7]">
            پرسش خود را بنویسید (مثال: برای تب ۳۹ درجه چه زمانی باید به پزشک مراجعه کرد؟)
          </label>
          <div className="flex flex-col gap-2 sm:flex-row">
            <input
              id="documented-qa-input"
              type="text"
              value={aiQuestion}
              onChange={(e) => setAiQuestion(e.target.value)}
              placeholder="سوال درباره تب، قطره آهن، واکسن، یبوست یا سرفه..."
              className="min-h-[44px] flex-1 rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3.5 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
            />
            <button
              type="submit"
              disabled={aiLoading || !aiQuestion.trim()}
              className="inline-flex min-h-[44px] items-center justify-center gap-2 rounded-xl bg-[#105B48] px-5 text-xs font-bold text-white disabled:opacity-50 dark:bg-[#00D7C7] dark:text-[#0B1220]"
            >
              {aiLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
              <span>{aiLoading ? 'در حال جست‌وجو و تنظیم...' : 'دریافت پاسخ مستند'}</span>
            </button>
          </div>

          {noMatchHint ? (
            <SeverityAlert
              severity="caution"
              title="مورد منطبق مستقیمی در پرسش‌های متداول یافت نشد"
              body="برای جلوگیری از پاسخ غیردقیق، پاسخ‌گوی مستند فقط به موضوعات موجود در پایگاه دانش مانو کید پاسخ می‌دهد. لطفاً با واژه‌های ساده‌تر جست‌وجو کنید یا به بخش بررسی علائم / پرسش از داروساز مراجعه نمایید."
            />
          ) : null}

          {aiError ? (
            <SeverityAlert
              severity="caution"
              title="سرویس هوش مصنوعی در دسترس نیست"
              body={aiError}
            />
          ) : null}

          {aiResult ? (
            <div className="space-y-3 rounded-2xl border border-[#B6DFC1] bg-[#F5FBF6] p-4 dark:border-[#21594B] dark:bg-[#102B25]">
              <h3 className="text-sm font-extrabold text-[#105B48] dark:text-[#35E0C9]">خلاصه پاسخ مستند</h3>
              <p className="text-sm leading-6 text-[#122823] dark:text-[#EDF4F7]">{aiResult.directAnswerFa}</p>

              {aiResult.actionableStepsFa.length > 0 ? (
                <div>
                  <div className="text-xs font-bold text-[#105B48] dark:text-[#00D7C7]">گام‌های عملی ایمن:</div>
                  <ul className="mt-1 list-disc space-y-1 pr-5 text-xs leading-6">
                    {aiResult.actionableStepsFa.map((step, i) => (
                      <li key={i}>{step}</li>
                    ))}
                  </ul>
                </div>
              ) : null}

              {aiResult.whenToVisitPediatricianFa.length > 0 ? (
                <div>
                  <div className="text-xs font-bold text-[#B32334] dark:text-[#FF8594]">
                    چه زمانی مراجعه به پزشک لازم است:
                  </div>
                  <ul className="mt-1 list-disc space-y-1 pr-5 text-xs leading-6">
                    {aiResult.whenToVisitPediatricianFa.map((sign, i) => (
                      <li key={i}>{sign}</li>
                    ))}
                  </ul>
                </div>
              ) : null}

              {aiResult.citedSourcesFa.length > 0 ? (
                <div className="border-t border-[#B6DFC1] pt-2 text-[11px] text-[#546963] dark:border-[#21594B] dark:text-[#9EB3C4]">
                  منابع استنادی: {aiResult.citedSourcesFa.join(' • ')}
                </div>
              ) : null}

              <p className="text-[11px] text-[#546963] dark:text-[#9EB3C4]">{aiResult.safetyDisclaimerFa}</p>
            </div>
          ) : null}
        </form>
      </section>

      {/* Search & Filters */}
      <section className="space-y-3 rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]">
        <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
          <div className="relative md:col-span-2">
            <Search className="pointer-events-none absolute right-3 top-3 h-4 w-4 text-[#546963]" />
            <input
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="جست‌وجو در ۳۴ پرسش و پاسخ: تب، دندان، واکسن، آهن، عسل..."
              className="min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] pr-9 pl-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
            />
          </div>
          <select
            value={selectedAgeBand}
            onChange={(e) => setSelectedAgeBand(e.target.value as AgeBandId | 'ALL')}
            aria-label="فیلتر رده سنی"
            className="min-h-[44px] rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-xs font-bold dark:border-[#23344D] dark:bg-[#16243B]"
          >
            <option value="ALL">همه رده‌های سنی</option>
            {AGE_BANDS.map((band) => (
              <option key={band.id} value={band.id}>
                {band.labelFa}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-wrap gap-2">
          {categories.map((cat) => (
            <button
              key={cat}
              type="button"
              onClick={() => setSelectedCategory(cat)}
              className={`min-h-[38px] rounded-xl border px-3 text-xs font-bold transition-colors ${
                selectedCategory === cat
                  ? 'border-[#105B48] bg-[#E9F5EB] text-[#105B48] dark:border-[#00D7C7] dark:bg-[#0F333A] dark:text-[#00D7C7]'
                  : 'border-[#DCE7E1] bg-[#F5F9F6] text-[#546963] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#9EB3C4]'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>
      </section>

      {/* Accordion FAQ List */}
      <section className="space-y-3">
        <div className="flex items-center justify-between text-xs text-[#546963] dark:text-[#9EB3C4]">
          <span>نتایج یافت‌شده: {toPersianDigits(filteredFaqs.length)} پرسش</span>
          <button
            type="button"
            onClick={() => navigate({ name: 'ask-pharmacist' })}
            className="inline-flex items-center gap-1 font-bold text-[#105B48] hover:underline dark:text-[#00D7C7]"
          >
            <Stethoscope className="h-3.5 w-3.5" />
            <span>طرح پرسش در بخش داروساز</span>
          </button>
        </div>

        {filteredFaqs.length === 0 ? (
          <div className="rounded-3xl border border-[#DCE7E1] bg-white p-8 text-center text-sm text-[#546963] dark:border-[#23344D] dark:bg-[#111C2E] dark:text-[#9EB3C4]">
            موردی با فیلترهای انتخابی پیدا نشد. عبارت جست‌وجو یا رده سنی را تغییر دهید.
          </div>
        ) : (
          filteredFaqs.map((faq) => {
            const isOpen = expandedId === faq.id;
            return (
              <article
                key={faq.id}
                className="overflow-hidden rounded-3xl border border-[#DCE7E1] bg-white dark:border-[#23344D] dark:bg-[#111C2E]"
              >
                <button
                  type="button"
                  aria-expanded={isOpen}
                  onClick={() => setExpandedId(isOpen ? null : faq.id)}
                  className="flex min-h-[60px] w-full items-center justify-between gap-3 px-5 py-4 text-right hover:bg-[#F5F9F6]/60 dark:hover:bg-[#16243B]/60"
                >
                  <div className="flex items-start gap-3">
                    <HelpCircle
                      className={`mt-0.5 h-5 w-5 shrink-0 ${
                        faq.isHighRisk ? 'text-[#C42B3C]' : 'text-[#105B48] dark:text-[#00D7C7]'
                      }`}
                    />
                    <div>
                      <div className="text-[11px] font-semibold text-[#546963] dark:text-[#9EB3C4]">
                        {faq.categoryFa} · {faq.ageRelevanceFa}
                      </div>
                      <h3 className="mt-0.5 text-sm font-extrabold leading-6 text-[#122823] dark:text-[#EDF4F7]">
                        {faq.questionFa}
                      </h3>
                    </div>
                  </div>
                  {isOpen ? (
                    <ChevronUp className="h-5 w-5 shrink-0 text-[#546963]" />
                  ) : (
                    <ChevronDown className="h-5 w-5 shrink-0 text-[#546963]" />
                  )}
                </button>

                {isOpen ? (
                  <div className="space-y-4 border-t border-[#DCE7E1] px-5 py-4 dark:border-[#23344D]">
                    <p className="text-sm font-semibold leading-6 text-[#105B48] dark:text-[#35E0C9]">
                      {faq.shortAnswerFa}
                    </p>
                    <p className="text-xs leading-6 text-[#122823] dark:text-[#EDF4F7]">
                      {faq.clinicalDetailFa}
                    </p>

                    {faq.whatToDoFa.length > 0 ? (
                      <div className="rounded-2xl bg-[#F5F9F6] p-3.5 dark:bg-[#16243B]">
                        <div className="text-xs font-extrabold text-[#105B48] dark:text-[#00D7C7]">
                          اقدامات عملی ایمن:
                        </div>
                        <ul className="mt-1.5 list-disc space-y-1 pr-5 text-xs leading-6">
                          {faq.whatToDoFa.map((item, idx) => (
                            <li key={idx}>{item}</li>
                          ))}
                        </ul>
                      </div>
                    ) : null}

                    {faq.whenToSeeDoctorFa.length > 0 ? (
                      <SeverityAlert
                        severity={faq.isHighRisk ? 'danger' : 'caution'}
                        title="چه زمانی مراجعه حضوری به پزشک یا اورژانس ضروری است؟"
                        bullets={faq.whenToSeeDoctorFa}
                      />
                    ) : null}

                    <SourceBadge sourceIds={faq.sourceIds} reviewStatus="VERIFIED" compact />
                  </div>
                ) : null}
              </article>
            );
          })
        )}
      </section>
    </div>
  );
};
