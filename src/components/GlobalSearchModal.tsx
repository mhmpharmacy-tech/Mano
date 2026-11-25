import React, { useEffect, useMemo, useRef, useState } from 'react';
import { BookOpen, Pill, Search, Stethoscope, HelpCircle, Sparkles, X } from 'lucide-react';
import { runGlobalSearch } from '../data/contentRepository.ts';
import { useMano } from '../state/ManoStore.tsx';

export const GlobalSearchModal: React.FC = () => {
  const { searchOpen, setSearchOpen, navigate } = useMano();
  const [query, setQuery] = useState('');
  const inputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        setSearchOpen(!searchOpen);
      } else if (e.key === 'Escape' && searchOpen) {
        setSearchOpen(false);
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [searchOpen, setSearchOpen]);

  useEffect(() => {
    if (searchOpen) {
      setTimeout(() => inputRef.current?.focus(), 40);
    }
  }, [searchOpen]);

  const results = useMemo(() => {
    if (!query.trim()) return null;
    return runGlobalSearch(query);
  }, [query]);

  if (!searchOpen) return null;

  const totalHits = results
    ? results.symptomFlows.length +
      results.medications.length +
      results.articles.length +
      results.faqs.length +
      results.supplements.length
    : 0;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="جست‌وجوی سریع در دانش بالینی مانو کید"
      className="fixed inset-0 z-50 flex items-start justify-center bg-[#0B1220]/70 p-4 pt-12 backdrop-blur-sm"
      onClick={() => setSearchOpen(false)}
    >
      <div
        className="w-full max-w-2xl overflow-hidden rounded-3xl border border-[#DCE7E1] bg-[#FFFFFF] shadow-2xl dark:border-[#23344D] dark:bg-[#111C2E]"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center gap-3 border-b border-[#DCE7E1] px-4 py-3 dark:border-[#23344D]">
          <Search className="h-5 w-5 shrink-0 text-[#105B48] dark:text-[#00D7C7]" />
          <input
            ref={inputRef}
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="جست‌وجوی علامت، دارو (استامینوفن، ایبوپروفن)، سرفه، واکسن..."
            aria-label="عبارت جست‌وجو"
            className="min-h-[44px] w-full bg-transparent text-sm text-[#122823] placeholder-[#546963] focus:outline-none dark:text-[#EDF4F7] dark:placeholder-[#9EB3C4]"
          />
          <button
            type="button"
            onClick={() => setSearchOpen(false)}
            aria-label="بستن جست‌وجو"
            className="flex min-h-[44px] min-w-[44px] items-center justify-center rounded-xl text-[#546963] hover:bg-[#F5F9F6] dark:text-[#9EB3C4] dark:hover:bg-[#16243B]"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="max-h-[65vh] overflow-y-auto p-4 space-y-4">
          {!results ? (
            <div className="space-y-3">
              <p className="text-xs font-semibold text-[#546963] dark:text-[#9EB3C4]">
                پیشنهادهای پرکاربرد والدین:
              </p>
              <div className="flex flex-wrap gap-2">
                {['تب نوزاد', 'استامینوفن', 'سرفه', 'اسهال ORS', 'واکسن', 'قطره آهن'].map((chip) => (
                  <button
                    key={chip}
                    type="button"
                    onClick={() => setQuery(chip)}
                    className="min-h-[40px] rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-xs font-semibold text-[#122823] hover:border-[#105B48] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#EDF4F7]"
                  >
                    {chip}
                  </button>
                ))}
              </div>
            </div>
          ) : totalHits === 0 ? (
            <div className="py-8 text-center text-sm text-[#546963] dark:text-[#9EB3C4]">
              نتیجه‌ای برای «{query}» پیدا نشد. عبارت ساده‌تری مثل «تب»، «شربت»، «سرفه» یا «پوشک» را امتحان کنید.
            </div>
          ) : (
            <div className="space-y-4">
              {results.symptomFlows.length > 0 ? (
                <div>
                  <h3 className="mb-2 flex items-center gap-1.5 text-xs font-bold text-[#105B48] dark:text-[#00D7C7]">
                    <Stethoscope className="h-4 w-4" />
                    <span>مسیرهای بررسی علائم</span>
                  </h3>
                  <div className="space-y-1.5">
                    {results.symptomFlows.slice(0, 4).map(({ item }) => (
                      <button
                        key={item.id}
                        type="button"
                        onClick={() => {
                          setSearchOpen(false);
                          navigate({ name: 'symptoms', initialFlowId: item.id });
                        }}
                        className="flex w-full items-center justify-between rounded-xl border border-[#DCE7E1] p-3 text-right hover:bg-[#F5F9F6] dark:border-[#23344D] dark:hover:bg-[#16243B]"
                      >
                        <div>
                          <div className="text-sm font-bold text-[#122823] dark:text-[#EDF4F7]">{item.titleFa}</div>
                          <div className="text-xs text-[#546963] dark:text-[#9EB3C4]">{item.shortDescFa}</div>
                        </div>
                      </button>
                    ))}
                  </div>
                </div>
              ) : null}

              {results.medications.length > 0 ? (
                <div>
                  <h3 className="mb-2 flex items-center gap-1.5 text-xs font-bold text-[#105B48] dark:text-[#00D7C7]">
                    <Pill className="h-4 w-4" />
                    <span>راهنمای داروها</span>
                  </h3>
                  <div className="space-y-1.5">
                    {results.medications.slice(0, 4).map(({ item }) => (
                      <button
                        key={item.slug}
                        type="button"
                        onClick={() => {
                          setSearchOpen(false);
                          navigate({ name: 'medicine-detail', slug: item.slug });
                        }}
                        className="flex w-full items-center justify-between rounded-xl border border-[#DCE7E1] p-3 text-right hover:bg-[#F5F9F6] dark:border-[#23344D] dark:hover:bg-[#16243B]"
                      >
                        <div>
                          <div className="text-sm font-bold text-[#122823] dark:text-[#EDF4F7]">{item.nameFa}</div>
                          <div className="text-xs text-[#546963] dark:text-[#9EB3C4]">{item.shortSummaryFa}</div>
                        </div>
                      </button>
                    ))}
                  </div>
                </div>
              ) : null}

              {results.articles.length > 0 ? (
                <div>
                  <h3 className="mb-2 flex items-center gap-1.5 text-xs font-bold text-[#105B48] dark:text-[#00D7C7]">
                    <BookOpen className="h-4 w-4" />
                    <span>مقالات علمی</span>
                  </h3>
                  <div className="space-y-1.5">
                    {results.articles.slice(0, 4).map(({ item }) => (
                      <button
                        key={item.slug}
                        type="button"
                        onClick={() => {
                          setSearchOpen(false);
                          navigate({ name: 'topic-detail', slug: item.slug });
                        }}
                        className="flex w-full items-center justify-between rounded-xl border border-[#DCE7E1] p-3 text-right hover:bg-[#F5F9F6] dark:border-[#23344D] dark:hover:bg-[#16243B]"
                      >
                        <div>
                          <div className="text-sm font-bold text-[#122823] dark:text-[#EDF4F7]">{item.titleFa}</div>
                          <div className="text-xs text-[#546963] dark:text-[#9EB3C4]">{item.categoryFa}</div>
                        </div>
                      </button>
                    ))}
                  </div>
                </div>
              ) : null}

              {results.faqs.length > 0 ? (
                <div>
                  <h3 className="mb-2 flex items-center gap-1.5 text-xs font-bold text-[#105B48] dark:text-[#00D7C7]">
                    <HelpCircle className="h-4 w-4" />
                    <span>پرسش‌های متداول ({results.faqs.length})</span>
                  </h3>
                  <div className="space-y-1.5">
                    {results.faqs.slice(0, 4).map(({ item }) => (
                      <button
                        key={item.id}
                        type="button"
                        onClick={() => {
                          setSearchOpen(false);
                          navigate({ name: 'faq' });
                        }}
                        className="w-full rounded-xl border border-[#DCE7E1] p-3 text-right hover:bg-[#F5F9F6] dark:border-[#23344D] dark:hover:bg-[#16243B]"
                      >
                        <div className="text-sm font-semibold text-[#122823] dark:text-[#EDF4F7]">{item.questionFa}</div>
                      </button>
                    ))}
                  </div>
                </div>
              ) : null}

              {results.supplements.length > 0 ? (
                <div>
                  <h3 className="mb-2 flex items-center gap-1.5 text-xs font-bold text-[#105B48] dark:text-[#00D7C7]">
                    <Sparkles className="h-4 w-4" />
                    <span>مکمل‌ها و بهداشت کودک</span>
                  </h3>
                  <div className="space-y-1.5">
                    {results.supplements.slice(0, 3).map(({ item }) => (
                      <button
                        key={item.id}
                        type="button"
                        onClick={() => {
                          setSearchOpen(false);
                          navigate({ name: 'supplements-hygiene', initialTab: item.tab });
                        }}
                        className="w-full rounded-xl border border-[#DCE7E1] p-3 text-right hover:bg-[#F5F9F6] dark:border-[#23344D] dark:hover:bg-[#16243B]"
                      >
                        <div className="text-sm font-semibold text-[#122823] dark:text-[#EDF4F7]">{item.titleFa}</div>
                      </button>
                    ))}
                  </div>
                </div>
              ) : null}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
