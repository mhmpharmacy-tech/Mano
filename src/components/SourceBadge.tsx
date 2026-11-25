import React from 'react';
import { BookOpenCheck, ExternalLink } from 'lucide-react';
import type { ReviewStatus } from '../domain/models.ts';
import { CLINICAL_REVIEWER_IDENTITY_FA, getSourceById } from '../data/contentRepository.ts';

interface SourceBadgeProps {
  sourceIds?: string[];
  reviewStatus?: ReviewStatus;
  compact?: boolean;
  className?: string;
}

export const SourceBadge: React.FC<SourceBadgeProps> = ({
  sourceIds = [],
  reviewStatus = 'NEEDS_CLINICAL_REVIEW',
  compact = false,
  className = '',
}) => {
  const sources = sourceIds.map((id) => getSourceById(id)).filter((s): s is NonNullable<typeof s> => Boolean(s));

  const statusText =
    reviewStatus === 'VERIFIED'
      ? `تأیید بالینی: ${CLINICAL_REVIEWER_IDENTITY_FA}`
      : 'محتوای آموزشی مستند (نیازمند تأیید موردی پزشک/داروساز برای شرایط خاص کودک)';

  if (compact) {
    return (
      <div className={`flex flex-wrap items-center gap-2 text-xs text-[#546963] dark:text-[#9EB3C4] ${className}`}>
        <BookOpenCheck className="h-3.5 w-3.5 text-[#105B48] dark:text-[#00D7C7]" />
        <span>{sources.length > 0 ? sources.map((s) => s.organization).join(' · ') : 'منابع استاندارد اطفال'}</span>
        <span aria-hidden="true">·</span>
        <span>{statusText}</span>
      </div>
    );
  }

  return (
    <section
      aria-label="منابع علمی و وضعیت بررسی"
      className={`rounded-2xl border border-[#DCE7E1] bg-[#F5F9F6] p-4 dark:border-[#23344D] dark:bg-[#16243B] ${className}`}
    >
      <div className="flex flex-wrap items-center justify-between gap-2 pb-2 border-b border-[#DCE7E1] dark:border-[#23344D]">
        <div className="flex items-center gap-2 text-xs font-bold text-[#105B48] dark:text-[#00D7C7]">
          <BookOpenCheck className="h-4 w-4 shrink-0" />
          <span>منابع علمی و شفافیت بالینی</span>
        </div>
        <span className="text-xs text-[#546963] dark:text-[#9EB3C4]">{statusText}</span>
      </div>

      {sources.length > 0 ? (
        <ul className="mt-2.5 space-y-2">
          {sources.map((src) => (
            <li
              key={src.id}
              className="flex flex-wrap items-center justify-between gap-2 text-xs leading-5 text-[#122823] dark:text-[#EDF4F7]"
            >
              <div>
                <span className="font-semibold">{src.titleFa}</span>
                <span className="mx-1.5 text-[#546963] dark:text-[#9EB3C4]">·</span>
                <span className="text-[#546963] dark:text-[#9EB3C4]">
                  {src.organization} ({src.year})
                </span>
              </div>
              {src.url ? (
                <a
                  href={src.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1 font-medium text-[#105B48] underline-offset-4 hover:underline dark:text-[#00D7C7]"
                >
                  <span>مشاهده مرجع</span>
                  <ExternalLink className="h-3 w-3" />
                </a>
              ) : null}
            </li>
          ))}
        </ul>
      ) : (
        <p className="mt-2 text-xs text-[#546963] dark:text-[#9EB3C4]">
          بر پایه دستورالعمل‌های بالینی AAP، WHO، NICE و کتب مرجع Nelson / Applied Therapeutics.
        </p>
      )}
    </section>
  );
};
