import React, { useState } from 'react';
import { ArrowRight, Copy, Plus, Printer, QrCode, Trash2 } from 'lucide-react';
import type { CareCardEntry } from '../domain/models.ts';
import { AGE_BANDS } from '../domain/models.ts';
import { currentTimeFa } from '../domain/treatmentPlan.ts';
import { formatTempC } from '../domain/feverDoseTimelineEngine.ts';
import { useMano } from '../state/ManoStore.tsx';

export const CareCardsScreen: React.FC = () => {
  const {
    childProfile,
    timelineSnapshot,
    treatmentPlan,
    careCards,
    addCareCard,
    deleteCareCard,
    notify,
    navigate,
  } = useMano();

  const [cardType, setCardType] = useState<CareCardEntry['cardType']>('CAREGIVER_HANDOFF');
  const [customTitle, setCustomTitle] = useState('');
  const [customNotes, setCustomNotes] = useState('');

  const ageLabel = AGE_BANDS.find((b) => b.id === childProfile.ageBand)?.labelFa ?? 'کودک';

  const handleGenerateCard = (e: React.FormEvent) => {
    e.preventDefault();
    const lines: string[] = [];
    lines.push(`نام/لقب کودک: ${childProfile.aliasFa} · رده سنی: ${ageLabel}`);
    if (childProfile.weightKgText) lines.push(`وزن ثبت‌شده: ${childProfile.weightKgText} کیلوگرم`);
    if (childProfile.allergiesFa) lines.push(`حساسیت شناخته‌شده: ${childProfile.allergiesFa}`);
    const latestTemp = timelineSnapshot.tempPoints[timelineSnapshot.tempPoints.length - 1];
    if (latestTemp) {
      lines.push(
        `آخرین تب ثبت‌شده: ${formatTempC(latestTemp.tempCelsius)} در ساعت ${latestTemp.timeLabelFa}`,
      );
    }
    const latestDose = timelineSnapshot.dosePoints[timelineSnapshot.dosePoints.length - 1];
    if (latestDose) {
      lines.push(
        `آخرین تب‌بر: ${latestDose.medicationNameFa} (${latestDose.volumeMlText}) در ساعت ${latestDose.givenAtLabelFa}`,
      );
    }
    if (treatmentPlan.medications.length > 0) {
      lines.push(
        `داروهای برنامه درمانی امروز: ${treatmentPlan.medications.map((m) => m.nameWithStrengthFa).join('، ')}`,
      );
    }
    if (customNotes.trim()) lines.push(`یادداشت والد: ${customNotes.trim()}`);

    const defaults: Record<CareCardEntry['cardType'], { title: string; subtitle: string; badge: string; footer: string }> = {
      CAREGIVER_HANDOFF: {
        title: 'کارت تحویل مراقبت به مراقب دوم / مادربزرگ',
        subtitle: 'خلاصه شفاف ساعت آخرین دارو، تب و حساسیت‌های کودک',
        badge: 'تحویل مراقب',
        footer: 'پیش از هر دوز جدید، سرنگ مدرج mL و زمان آخرین مصرف بررسی شود.',
      },
      CLINIC_SUMMARY: {
        title: 'کارت خلاصه ویزیت پزشک اطفال',
        subtitle: 'مرور سریع وضعیت ۲۴ ساعته کودک برای مطب یا اورژانس',
        badge: 'ویزیت پزشک',
        footer: 'تنظیم‌شده از گزارش ثبت‌شده در اپلیکیشن مانو کید.',
      },
      PHARMACY_INVITE: {
        title: 'کارت همراه مراجعه به داروخانه',
        subtitle: 'مشخصات وزن، سن، حساسیت و اقلام برنامه درمانی کودک',
        badge: 'داروخانه',
        footer: 'برای تطبیق غلظت بطری شربت یا قطره با داروساز هم‌اندیشی کنید.',
      },
      PHARMACY_PROTOCOL: {
        title: 'پروتکل پیگیری دارویی',
        subtitle: 'دستورالعمل خلاصه مراقبت خانگی',
        badge: 'پروتکل',
        footer: 'در صورت تداوم یا تشدید علائم با پزشک مشورت کنید.',
      },
    };

    const meta = defaults[cardType];
    addCareCard({
      id: `card_${Date.now()}`,
      titleFa: customTitle.trim() || meta.title,
      subtitleFa: meta.subtitle,
      cardType,
      badgeFa: meta.badge,
      summaryLinesFa: lines,
      footerNoteFa: meta.footer,
      createdAtLabelFa: `امروز ${currentTimeFa()}`,
    });

    setCustomTitle('');
    setCustomNotes('');
  };

  const handleCopy = async (card: CareCardEntry) => {
    const text = [card.titleFa, card.subtitleFa, ...card.summaryLinesFa, card.footerNoteFa].join('\n');
    try {
      await navigator.clipboard.writeText(text);
      notify('متن کارت مراقبت کپی شد.', 'success');
    } catch {
      notify('امکان کپی خودکار در این مرورگر فعال نبود.', 'info');
    }
  };

  return (
    <div className="space-y-6" data-testid="care-cards-screen">
      <div className="no-print flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            کارت‌های مراقبت و تحویل کودک
          </h1>
          <p className="mt-1 text-xs text-[#546963] dark:text-[#9EB3C4]">
            ساخت خلاصه یک‌صفحه‌ای و قابل چاپ از آخرین تب، دوز دارو و حساسیت کودک برای مراقب دوم، داروخانه یا مطب
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

      {/* Card Builder */}
      <form
        onSubmit={handleGenerateCard}
        className="no-print space-y-4 rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]"
      >
        <div className="flex items-center gap-2 text-sm font-extrabold text-[#105B48] dark:text-[#00D7C7]">
          <QrCode className="h-5 w-5" />
          <span>ساخت کارت مراقبت خودکار از پرونده فعلی</span>
        </div>

        <div className="flex flex-wrap gap-2">
          {(
            [
              { id: 'CAREGIVER_HANDOFF', label: 'کارت تحویل به مراقب دوم' },
              { id: 'CLINIC_SUMMARY', label: 'خلاصه ویزیت پزشک' },
              { id: 'PHARMACY_INVITE', label: 'کارت مراجعه به داروخانه' },
            ] as const
          ).map((opt) => (
            <button
              key={opt.id}
              type="button"
              onClick={() => setCardType(opt.id)}
              className={`min-h-[40px] rounded-xl border px-3.5 text-xs font-bold transition-colors ${
                cardType === opt.id
                  ? 'border-[#105B48] bg-[#E9F5EB] text-[#105B48] dark:border-[#00D7C7] dark:bg-[#0F333A] dark:text-[#00D7C7]'
                  : 'border-[#DCE7E1] bg-[#F5F9F6] text-[#546963] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#9EB3C4]'
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div>
            <label htmlFor="card-title-input" className="block text-xs font-bold">
              عنوان دلخواه کارت (اختیاری)
            </label>
            <input
              id="card-title-input"
              type="text"
              value={customTitle}
              onChange={(e) => setCustomTitle(e.target.value)}
              placeholder="مثلاً کارت مراقبت شبانه"
              className="mt-1 min-h-[42px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
            />
          </div>
          <div>
            <label htmlFor="card-note-input" className="block text-xs font-bold">
              یادداشت تکمیلی (اختیاری)
            </label>
            <input
              id="card-note-input"
              type="text"
              value={customNotes}
              onChange={(e) => setCustomNotes(e.target.value)}
              placeholder="مثلاً نوبت بعدی قطره ساعت ۱۸ است"
              className="mt-1 min-h-[42px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
            />
          </div>
        </div>

        <button
          type="submit"
          className="inline-flex min-h-[44px] items-center gap-2 rounded-xl bg-[#105B48] px-5 text-xs font-bold text-white dark:bg-[#00D7C7] dark:text-[#0B1220]"
        >
          <Plus className="h-4 w-4" />
          <span>ساخت و ذخیره کارت</span>
        </button>
      </form>

      {/* Saved Cards */}
      <section className="space-y-4">
        {careCards.length === 0 ? (
          <div className="rounded-3xl border border-[#DCE7E1] bg-white p-8 text-center text-xs text-[#546963] dark:border-[#23344D] dark:bg-[#111C2E] dark:text-[#9EB3C4]">
            هنوز کارتی ساخته نشده است. با دکمه بالا اولین کارت خلاصه مراقبت را بسازید.
          </div>
        ) : (
          careCards.map((card) => (
            <article
              key={card.id}
              className="space-y-3 rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]"
            >
              <div className="flex flex-wrap items-start justify-between gap-2 border-b border-[#DCE7E1] pb-3 dark:border-[#23344D]">
                <div>
                  <div className="text-xs font-bold text-[#105B48] dark:text-[#00D7C7]">
                    {card.badgeFa} · {card.createdAtLabelFa}
                  </div>
                  <h2 className="mt-0.5 text-base font-extrabold text-[#122823] dark:text-[#EDF4F7]">
                    {card.titleFa}
                  </h2>
                  <p className="text-xs text-[#546963] dark:text-[#9EB3C4]">{card.subtitleFa}</p>
                </div>

                <div className="no-print flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => handleCopy(card)}
                    className="inline-flex min-h-[38px] items-center gap-1 rounded-xl border border-[#DCE7E1] px-3 text-xs font-bold dark:border-[#23344D]"
                  >
                    <Copy className="h-3.5 w-3.5" />
                    <span>کپی متن</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => window.print()}
                    className="inline-flex min-h-[38px] items-center gap-1 rounded-xl border border-[#DCE7E1] px-3 text-xs font-bold dark:border-[#23344D]"
                  >
                    <Printer className="h-3.5 w-3.5" />
                    <span>چاپ</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => deleteCareCard(card.id)}
                    aria-label="حذف کارت"
                    className="flex min-h-[38px] min-w-[38px] items-center justify-center rounded-xl border border-[#E7B5BC] text-[#B32334] dark:border-[#6A2C3B] dark:text-[#FF8594]"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              </div>

              <ul className="space-y-1.5 text-xs leading-6 text-[#122823] dark:text-[#EDF4F7]">
                {card.summaryLinesFa.map((line, idx) => (
                  <li key={idx}>• {line}</li>
                ))}
              </ul>

              <div className="rounded-2xl bg-[#F5F9F6] p-3 text-[11px] font-semibold text-[#546963] dark:bg-[#16243B] dark:text-[#9EB3C4]">
                {card.footerNoteFa}
              </div>
            </article>
          ))
        )}
      </section>
    </div>
  );
};
