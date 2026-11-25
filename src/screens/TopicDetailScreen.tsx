import React from 'react';
import { ArrowRight, Bookmark, BookmarkCheck } from 'lucide-react';
import { getArticleBySlug } from '../data/contentRepository.ts';
import { toPersianDigits } from '../domain/persianNormalizer.ts';
import { useMano } from '../state/ManoStore.tsx';
import { SeverityAlert } from '../components/SeverityAlert.tsx';
import { SourceBadge } from '../components/SourceBadge.tsx';

export const TopicDetailScreen: React.FC<{ slug: string }> = ({ slug }) => {
  const { navigate, bookmarkedSlugs, toggleBookmark } = useMano();
  const article = getArticleBySlug(slug);

  if (!article) {
    return (
      <div className="space-y-4">
        <SeverityAlert severity="caution" title="مقاله یافت نشد" body="لطفاً از فهرست کتابخانه مطلب دیگری را باز کنید." />
        <button
          type="button"
          onClick={() => navigate({ name: 'topics' })}
          className="min-h-[44px] rounded-xl bg-[#105B48] px-4 text-xs font-bold text-white"
        >
          بازگشت به کتابخانه
        </button>
      </div>
    );
  }

  const bookmarked = bookmarkedSlugs.includes(article.slug);

  return (
    <article className="space-y-6" data-testid="topic-detail-screen">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div className="text-xs text-[#546963] dark:text-[#9EB3C4]">
            {article.categoryFa} · {article.ageRelevanceFa} · {toPersianDigits(article.readingTimeMin)} دقیقه
          </div>
          <h1 className="mt-1 text-xl font-extrabold text-[#122823] dark:text-[#EDF4F7]">{article.titleFa}</h1>
        </div>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => toggleBookmark(article.slug)}
            className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] px-3 text-xs font-bold dark:border-[#23344D]"
          >
            {bookmarked ? <BookmarkCheck className="h-4 w-4 text-[#105B48]" /> : <Bookmark className="h-4 w-4" />}
            <span>{bookmarked ? 'نشان‌شده' : 'ذخیره'}</span>
          </button>
          <button
            type="button"
            onClick={() => navigate({ name: 'topics' })}
            className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] px-3 text-xs font-bold dark:border-[#23344D]"
          >
            <ArrowRight className="h-4 w-4" />
            <span>مقالات</span>
          </button>
        </div>
      </div>

      <SeverityAlert
        severity="success"
        title="نکات کلیدی مقاله در یک نگاه"
        bullets={article.keyTakeawaysFa}
      />

      <div className="space-y-4">
        {article.sections.map((sec, idx) => (
          <section
            key={idx}
            className="rounded-3xl border border-[#DCE7E1] bg-white p-5 space-y-2.5 dark:border-[#23344D] dark:bg-[#111C2E]"
          >
            <h2 className="text-base font-extrabold text-[#105B48] dark:text-[#00D7C7]">{sec.headingFa}</h2>
            <p className="text-sm leading-7 text-[#122823] dark:text-[#EDF4F7]">{sec.bodyFa}</p>
            {sec.bulletsFa && sec.bulletsFa.length > 0 ? (
              <ul className="list-disc space-y-1.5 pr-5 text-sm leading-6 text-[#122823] dark:text-[#EDF4F7]">
                {sec.bulletsFa.map((b, bIdx) => (
                  <li key={bIdx}>{b}</li>
                ))}
              </ul>
            ) : null}
            {sec.warningBoxFa ? (
              <SeverityAlert severity="caution" title="هشدار ایمنی" body={sec.warningBoxFa} />
            ) : null}
          </section>
        ))}
      </div>

      {article.caregiverMistakesFa.length > 0 ? (
        <SeverityAlert
          severity="caution"
          title="اشتباهات رایج که باید از آن‌ها پرهیز کرد"
          bullets={article.caregiverMistakesFa}
        />
      ) : null}

      <SeverityAlert
        severity="danger"
        title="پرچم‌های قرمز نیازمند ویزیت فوری پزشک"
        bullets={article.redFlagsBoxFa}
      />

      <SourceBadge sourceIds={article.sourceIds} reviewStatus={article.reviewStatus} />
    </article>
  );
};
