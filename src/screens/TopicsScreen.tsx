import React, { useMemo, useState } from 'react';
import { ArrowRight, BookOpen, ChevronLeft, Clock, Search } from 'lucide-react';
import { ALL_ARTICLES } from '../data/contentRepository.ts';
import { scoreMatch, toPersianDigits } from '../domain/persianNormalizer.ts';
import { useMano } from '../state/ManoStore.tsx';

export const TopicsScreen: React.FC = () => {
  const { navigate } = useMano();
  const [query, setQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('همه');

  const categories = useMemo(
    () => ['همه', ...Array.from(new Set(ALL_ARTICLES.map((a) => a.categoryFa)))],
    [],
  );

  const visible = useMemo(() => {
    return ALL_ARTICLES.filter((a) => {
      const catOk = selectedCategory === 'همه' || a.categoryFa === selectedCategory;
      const queryOk =
        !query.trim() ||
        scoreMatch(query, [a.titleFa, a.subtitleFa, a.categoryFa, ...a.keyTakeawaysFa]) > 0;
      return catOk && queryOk;
    });
  }, [query, selectedCategory]);

  return (
    <div className="space-y-6" data-testid="topics-screen">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            کتابخانه مقالات و راهنماهای علمی کودک
          </h1>
          <p className="mt-1 text-xs text-[#546963] dark:text-[#9EB3C4]">
            مطالب مستند با خلاصه نکات کلیدی، اشتباهات رایج والدین و نشانه‌های مراجعه به پزشک
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

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap gap-2">
          {categories.map((cat) => (
            <button
              key={cat}
              type="button"
              onClick={() => setSelectedCategory(cat)}
              className={`min-h-[38px] rounded-xl border px-3 text-xs font-bold ${
                selectedCategory === cat
                  ? 'border-[#105B48] bg-[#E9F5EB] text-[#105B48] dark:border-[#00D7C7] dark:bg-[#0F333A] dark:text-[#00D7C7]'
                  : 'border-[#DCE7E1] bg-white text-[#546963] dark:border-[#23344D] dark:bg-[#111C2E]'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>

        <div className="relative w-full sm:w-64">
          <Search className="pointer-events-none absolute right-3 top-3 h-4 w-4 text-[#546963]" />
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="جست‌وجو در مقالات..."
            className="min-h-[40px] w-full rounded-xl border border-[#DCE7E1] bg-white pr-9 pl-3 text-xs dark:border-[#23344D] dark:bg-[#111C2E]"
          />
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        {visible.map((article) => (
          <button
            key={article.slug}
            type="button"
            onClick={() => navigate({ name: 'topic-detail', slug: article.slug })}
            className="flex flex-col justify-between rounded-3xl border border-[#DCE7E1] bg-white p-5 text-right transition-colors hover:border-[#105B48] dark:border-[#23344D] dark:bg-[#111C2E] dark:hover:border-[#00D7C7]"
          >
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-xs text-[#546963] dark:text-[#9EB3C4]">
                <BookOpen className="h-3.5 w-3.5 text-[#105B48] dark:text-[#00D7C7]" />
                <span>{article.categoryFa}</span>
                <span>·</span>
                <Clock className="h-3.5 w-3.5" />
                <span className="tabular-nums">{toPersianDigits(article.readingTimeMin)} دقیقه مطالعه</span>
              </div>
              <h2 className="text-base font-extrabold text-[#122823] dark:text-[#EDF4F7]">
                {article.titleFa}
              </h2>
              <p className="text-xs leading-5 text-[#546963] dark:text-[#9EB3C4]">{article.subtitleFa}</p>
            </div>

            <div className="mt-4 flex items-center justify-between text-xs font-bold text-[#105B48] dark:text-[#00D7C7]">
              <span>{article.ageRelevanceFa}</span>
              <ChevronLeft className="h-4 w-4" />
            </div>
          </button>
        ))}
      </div>
    </div>
  );
};
