import React from 'react';
import { ArrowRight, Calculator, Bookmark, BookmarkCheck } from 'lucide-react';
import { getMedicationBySlug } from '../data/contentRepository.ts';
import { useMano } from '../state/ManoStore.tsx';
import { SeverityAlert } from '../components/SeverityAlert.tsx';
import { SourceBadge } from '../components/SourceBadge.tsx';

export const MedicineDetailScreen: React.FC<{ slug: string }> = ({ slug }) => {
  const { navigate, bookmarkedSlugs, toggleBookmark } = useMano();
  const med = getMedicationBySlug(slug);

  if (!med) {
    return (
      <div className="space-y-4">
        <SeverityAlert severity="caution" title="دارو پیدا نشد" body="لطفاً به فهرست دارونامه برگردید." />
        <button
          type="button"
          onClick={() => navigate({ name: 'medicines' })}
          className="min-h-[44px] rounded-xl bg-[#105B48] px-4 text-xs font-bold text-white"
        >
          بازگشت به دارونامه
        </button>
      </div>
    );
  }

  const bookmarked = bookmarkedSlugs.includes(med.slug);

  return (
    <div className="space-y-6" data-testid="medicine-detail-screen">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div className="text-xs font-semibold text-[#105B48] dark:text-[#00D7C7]">
            {med.categoryFa} · {med.genericNameEn}
          </div>
          <h1 className="mt-1 text-xl font-extrabold text-[#122823] dark:text-[#EDF4F7]">{med.nameFa}</h1>
        </div>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => toggleBookmark(med.slug)}
            className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] px-3 text-xs font-bold dark:border-[#23344D]"
          >
            {bookmarked ? <BookmarkCheck className="h-4 w-4 text-[#105B48]" /> : <Bookmark className="h-4 w-4" />}
            <span>{bookmarked ? 'نشان‌شده' : 'ذخیره'}</span>
          </button>
          <button
            type="button"
            onClick={() => navigate({ name: 'medicines' })}
            className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] px-3 text-xs font-bold dark:border-[#23344D]"
          >
            <ArrowRight className="h-4 w-4" />
            <span>دارونامه</span>
          </button>
        </div>
      </div>

      <SeverityAlert severity="danger" title="هشدار حیاتی شکل دارویی و غلظت" body={med.dosageFormAlertFa} />

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <section className="rounded-3xl border border-[#DCE7E1] bg-white p-5 space-y-2.5 dark:border-[#23344D] dark:bg-[#111C2E]">
          <h2 className="text-sm font-extrabold text-[#105B48] dark:text-[#00D7C7]">کاربرد و رده سنی مجاز</h2>
          <p className="text-sm leading-6">{med.treatsFa}</p>
          <p className="text-xs font-bold text-[#9F5F0D] dark:text-[#F5C169]">{med.ageMinimumNoteFa}</p>
          <div className="pt-2 text-xs text-[#546963] dark:text-[#9EB3C4]">
            <span className="font-bold">شرایط نگهداری: </span>
            {med.storageAndShelfLifeFa}
          </div>
        </section>

        <section className="rounded-3xl border border-[#DCE7E1] bg-white p-5 space-y-2.5 dark:border-[#23344D] dark:bg-[#111C2E]">
          <h2 className="text-sm font-extrabold text-[#105B48] dark:text-[#00D7C7]">اشکال دارویی و روش اندازه‌گیری</h2>
          <ul className="list-disc space-y-1 pr-5 text-xs leading-6">
            {med.dosageFormsFa.map((f, i) => (
              <li key={i}>{f}</li>
            ))}
          </ul>
          <p className="rounded-xl bg-[#F5F9F6] p-3 text-xs leading-5 dark:bg-[#16243B]">
            {med.measurementGuidanceFa}
          </p>
        </section>

        <section className="rounded-3xl border border-[#DCE7E1] bg-white p-5 space-y-2 dark:border-[#23344D] dark:bg-[#111C2E]">
          <h2 className="text-sm font-extrabold text-[#105B48] dark:text-[#00D7C7]">نکات ایمنی کلیدی</h2>
          <ul className="list-disc space-y-1.5 pr-5 text-xs leading-6">
            {med.keyPrecautionsFa.map((p, i) => (
              <li key={i}>{p}</li>
            ))}
          </ul>
        </section>

        <section className="rounded-3xl border border-[#DCE7E1] bg-white p-5 space-y-2 dark:border-[#23344D] dark:bg-[#111C2E]">
          <h2 className="text-sm font-extrabold text-[#B32334] dark:text-[#FF8594]">موارد پرهیز و تداخل</h2>
          <ul className="list-disc space-y-1.5 pr-5 text-xs leading-6">
            {[...med.avoidWithFa, ...med.whatNotToGiveFa].map((a, i) => (
              <li key={i}>{a}</li>
            ))}
          </ul>
        </section>
      </div>

      {med.slug === 'acetaminophen-pediatric' || med.slug === 'ibuprofen-pediatric' ? (
        <button
          type="button"
          onClick={() => navigate({ name: 'fever-dose' })}
          className="inline-flex min-h-[44px] items-center gap-2 rounded-xl bg-[#105B48] px-5 text-sm font-bold text-white dark:bg-[#00D7C7] dark:text-[#0B1220]"
        >
          <Calculator className="h-4 w-4" />
          <span>محاسبه حجم ایمن با وزن کودک</span>
        </button>
      ) : null}

      <SourceBadge sourceIds={med.sourceIds} />
    </div>
  );
};
