import React, { useMemo, useState } from 'react';
import {
  ArrowRight,
  CheckCircle2,
  ClipboardCheck,
  Info,
  Send,
  ShieldAlert,
  Stethoscope,
  UserCheck,
} from 'lucide-react';
import { DEMO_PHARMACIST_CASES } from '../data/contentRepository.ts';
import { AGE_BANDS, type ConsultationUrgencyLevel } from '../domain/models.ts';
import { currentTimeFa } from '../domain/treatmentPlan.ts';
import { toPersianDigits } from '../domain/persianNormalizer.ts';
import { useMano } from '../state/ManoStore.tsx';
import { SeverityAlert } from '../components/SeverityAlert.tsx';

const TOPIC_OPTIONS: { id: string; labelFa: string }[] = [
  { id: 'fever', labelFa: 'تب و انتخاب/تنظیم تب‌بر' },
  { id: 'syrup', labelFa: 'اسکن شربت و تطبیق غلظت mL' },
  { id: 'supplement', labelFa: 'مکمل‌های کودک (ویتامین D، آهن، پروبیوتیک)' },
  { id: 'skin', labelFa: 'پوست و سوختگی ناحیه پوشک' },
  { id: 'scalp_hair', labelFa: 'شست‌وشوی سر و مراقبت پوستی' },
  { id: 'digestive', labelFa: 'گوارش، یبوست و محلول ORS' },
  { id: 'other', labelFa: 'سایر پرسش‌های دارویی' },
];

const SYMPTOM_CHIPS = [
  { label: 'تب زیر ۳۸٫۵', urgent: false },
  { label: 'سرفه خفیف', urgent: false },
  { label: 'سوال درباره دوز/غلظت بطری', urgent: false },
  { label: 'قرمزی پوشک', urgent: false },
  { label: 'نوزاد زیر ۳ ماه با تب', urgent: true },
  { label: 'تنفس تند یا سخت', urgent: true },
  { label: 'بی‌حالی شدید یا کم‌آبی', urgent: true },
];

export const AskPharmacistScreen: React.FC<{ initialTopic?: string }> = ({ initialTopic }) => {
  const {
    childProfile,
    pharmacistConsultations,
    addPharmacistConsultation,
    replyPharmacistConsultation,
    navigate,
  } = useMano();

  const initialLabel =
    TOPIC_OPTIONS.find((t) => t.id === initialTopic)?.labelFa ?? TOPIC_OPTIONS[0].labelFa;

  const [tab, setTab] = useState<'form' | 'history' | 'desk'>('form');
  const [topicFa, setTopicFa] = useState(initialLabel);
  const [weightKgText, setWeightKgText] = useState(childProfile.weightKgText);
  const [questionTextFa, setQuestionTextFa] = useState('');
  const [selectedSymptoms, setSelectedSymptoms] = useState<string[]>([]);
  const [replyDrafts, setReplyDrafts] = useState<Record<string, string>>({});

  const urgencyLevel: ConsultationUrgencyLevel = useMemo(() => {
    const hasUrgentChip = selectedSymptoms.some(
      (s) => SYMPTOM_CHIPS.find((c) => c.label === s)?.urgent,
    );
    if (hasUrgentChip) return 'EMERGENCY_BYPASS';
    if (childProfile.ageBand === 'NEWBORN') return 'NEEDS_PHYSICIAN_TODAY';
    return 'ROUTINE_PHARMACIST';
  }, [selectedSymptoms, childProfile.ageBand]);

  const ageBandLabel = AGE_BANDS.find((b) => b.id === childProfile.ageBand)?.labelFa ?? 'کودک';

  const toggleSymptomChip = (label: string) => {
    setSelectedSymptoms((prev) =>
      prev.includes(label) ? prev.filter((item) => item !== label) : [...prev, label],
    );
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!questionTextFa.trim()) return;
    addPharmacistConsultation({
      id: `cons_${Date.now()}`,
      createdAtMs: Date.now(),
      createdAtLabelFa: `امروز ${currentTimeFa()}`,
      childAgeBand: childProfile.ageBand,
      weightKgText: weightKgText.trim() || 'ثبت‌نشده',
      topicFa,
      questionTextFa: questionTextFa.trim(),
      symptomsSelectedFa: selectedSymptoms,
      urgencyLevel,
      statusFa: 'ثبت محلی در همین دستگاه (ارسال آنلاین فعال نیست)',
    });
    setQuestionTextFa('');
    setSelectedSymptoms([]);
    setTab('history');
  };

  return (
    <div className="space-y-6" data-testid="ask-pharmacist-screen">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            ثبت پرسش مراقبتی و میز راهنمای داروساز
          </h1>
          <p className="mt-1 text-xs text-[#546963] dark:text-[#9EB3C4]">
            تنظیم یادداشت ساختارمند سوال دارویی برای مراجعه به داروخانه یا پزشک، به همراه پرونده‌های آموزشی نمونه
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

      {/* Mandatory Honesty Banner: Local-Only Storage */}
      <SeverityAlert
        severity="info"
        title="شفافیت فنی: ذخیره‌سازی محلی در همین مرورگر"
        body="در این نسخه وب، پرسش‌ها و پاسخ‌های بخش «پرسش از داروساز» صرفاً در حافظه همین دستگاه (localStorage) ذخیره می‌شوند و به سرور یا داروساز آنلاین ارسال نمی‌گردند. در شرایط فوری یا برای نسخه، به داروخانه یا پزشک مراجعه کنید."
      />

      {/* 3-Tab Bar */}
      <div className="grid grid-cols-3 gap-2" role="tablist" aria-label="تب‌های پرسش از داروساز">
        {[
          { id: 'form', label: 'ثبت پرسش جدید' },
          { id: 'history', label: `سابقه و نمونه‌ها (${toPersianDigits(pharmacistConsultations.length)})` },
          { id: 'desk', label: 'میز تمرینی داروساز' },
        ].map((t) => (
          <button
            key={t.id}
            type="button"
            role="tab"
            aria-selected={tab === t.id}
            onClick={() => setTab(t.id as typeof tab)}
            className={`min-h-[44px] rounded-2xl border px-2 text-xs font-extrabold transition-colors ${
              tab === t.id
                ? 'border-[#105B48] bg-[#105B48] text-white dark:border-[#00D7C7] dark:bg-[#00D7C7] dark:text-[#0B1220]'
                : 'border-[#DCE7E1] bg-white text-[#546963] dark:border-[#23344D] dark:bg-[#111C2E] dark:text-[#9EB3C4]'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'form' ? (
        <section className="space-y-4 rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]">
          <div className="flex items-center gap-3 rounded-2xl bg-[#F5F9F6] p-3.5 dark:bg-[#16243B]">
            <img
              src="/assets/images/img_pharmacist_avatar.jpg"
              alt="راهنمای داروساز کودک"
              className="h-12 w-12 shrink-0 rounded-2xl object-cover border border-[#DCE7E1]"
            />
            <div>
              <div className="text-sm font-extrabold text-[#122823] dark:text-[#EDF4F7]">
                تنظیم برگه پرسش برای داروساز داروخانه
              </div>
              <div className="text-xs text-[#546963] dark:text-[#9EB3C4]">
                رده سنی پرونده: {ageBandLabel}
                {childProfile.allergiesFa ? ` · آلرژی: ${childProfile.allergiesFa}` : ''}
              </div>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <label htmlFor="pharm-topic" className="block text-xs font-bold">
                  موضوع مشاوره
                </label>
                <select
                  id="pharm-topic"
                  value={topicFa}
                  onChange={(e) => setTopicFa(e.target.value)}
                  className="mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
                >
                  {TOPIC_OPTIONS.map((opt) => (
                    <option key={opt.id} value={opt.labelFa}>
                      {opt.labelFa}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label htmlFor="pharm-weight" className="block text-xs font-bold">
                  وزن فعلی کودک (کیلوگرم)
                </label>
                <input
                  id="pharm-weight"
                  type="text"
                  inputMode="decimal"
                  value={weightKgText}
                  onChange={(e) => setWeightKgText(e.target.value)}
                  placeholder="مثلاً ۱۱٫۵"
                  className="mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
                />
              </div>
            </div>

            <div>
              <span className="block text-xs font-bold">نشانه‌ها یا وضعیت فعلی (برای غربالگری فوریت):</span>
              <div className="mt-2 flex flex-wrap gap-2">
                {SYMPTOM_CHIPS.map((chip) => {
                  const active = selectedSymptoms.includes(chip.label);
                  return (
                    <button
                      key={chip.label}
                      type="button"
                      onClick={() => toggleSymptomChip(chip.label)}
                      className={`min-h-[38px] rounded-xl border px-3 text-xs font-bold transition-colors ${
                        active
                          ? chip.urgent
                            ? 'border-[#C42B3C] bg-[#FDF0F1] text-[#B32334] dark:bg-[#2F161E] dark:text-[#FF8594]'
                            : 'border-[#105B48] bg-[#E9F5EB] text-[#105B48] dark:border-[#00D7C7] dark:bg-[#0F333A] dark:text-[#00D7C7]'
                          : 'border-[#DCE7E1] bg-[#F5F9F6] text-[#546963] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#9EB3C4]'
                      }`}
                    >
                      {chip.label}
                    </button>
                  );
                })}
              </div>
            </div>

            {urgencyLevel === 'EMERGENCY_BYPASS' ? (
              <SeverityAlert
                severity="danger"
                title="هشدار فوری: این مورد نباید منتظر پاسخ داروساز بماند"
                body="نشانه‌های انتخاب‌شده می‌توانند علامت وضعیت اورژانسی باشند. لطفاً بدون معطلی با اورژانس (۱۱۵) تماس بگیرید یا فوراً به نزدیک‌ترین مرکز درمانی مراجعه کنید."
              />
            ) : urgencyLevel === 'NEEDS_PHYSICIAN_TODAY' ? (
              <SeverityAlert
                severity="caution"
                title="نوزاد زیر ۳ ماه: اولویت با ویزیت حضوری پزشک اطفال"
                body="برای شیرخواران زیر ۳ ماه، هر تغییر بالینی یا داروی جدید باید ابتدا توسط پزشک معاینه و تایید شود."
              />
            ) : null}

            <div>
              <label htmlFor="pharm-question" className="block text-xs font-bold">
                شرح دقیق سوال، نام دارو یا غلظت روی بطری
              </label>
              <textarea
                id="pharm-question"
                rows={4}
                value={questionTextFa}
                onChange={(e) => setQuestionTextFa(e.target.value)}
                placeholder="مثلاً: روی شیشه استامینوفن نوشته ۱۲۰ میلی‌گرم در ۵ میلی‌لیتر؛ وزن کودک ۱۲ کیلوگرم است و تداخل با شربت سرفه را می‌خواهم بررسی کنم..."
                className="mt-1 w-full rounded-2xl border border-[#DCE7E1] bg-[#F5F9F6] p-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
              />
            </div>

            <button
              type="submit"
              disabled={!questionTextFa.trim()}
              className="flex min-h-[46px] w-full items-center justify-center gap-2 rounded-xl bg-[#105B48] px-4 text-sm font-bold text-white disabled:opacity-50 dark:bg-[#00D7C7] dark:text-[#0B1220]"
            >
              <Send className="h-4 w-4" />
              <span>ذخیره پرسش در این دستگاه</span>
            </button>
          </form>
        </section>
      ) : null}

      {tab === 'history' ? (
        <div className="space-y-5">
          <section className="space-y-3">
            <h2 className="text-sm font-extrabold text-[#122823] dark:text-[#EDF4F7]">
              پرسش‌های ذخیره‌شده شما ({toPersianDigits(pharmacistConsultations.length)})
            </h2>
            {pharmacistConsultations.length === 0 ? (
              <div className="rounded-3xl border border-[#DCE7E1] bg-white p-6 text-center text-xs text-[#546963] dark:border-[#23344D] dark:bg-[#111C2E] dark:text-[#9EB3C4]">
                هنوز پرسشی روی این دستگاه ثبت نکرده‌اید.
              </div>
            ) : (
              pharmacistConsultations.map((c) => (
                <article
                  key={c.id}
                  className="space-y-2.5 rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]"
                >
                  <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
                    <span className="font-extrabold text-[#105B48] dark:text-[#00D7C7]">{c.topicFa}</span>
                    <span className="text-[#546963] dark:text-[#9EB3C4]">
                      {c.createdAtLabelFa} · {c.statusFa}
                    </span>
                  </div>
                  <p className="text-sm leading-6 text-[#122823] dark:text-[#EDF4F7]">{c.questionTextFa}</p>
                  <div className="text-xs text-[#546963] dark:text-[#9EB3C4]">
                    وزن: {c.weightKgText}
                    {c.symptomsSelectedFa.length > 0 ? ` · علائم: ${c.symptomsSelectedFa.join('، ')}` : ''}
                  </div>
                  {c.pharmacistReplyFa ? (
                    <div className="rounded-2xl border border-[#B6DFC1] bg-[#EBF7EE] p-3 text-xs leading-6 text-[#105B48] dark:border-[#21594B] dark:bg-[#102B25] dark:text-[#35E0C9]">
                      <strong>یادداشت پاسخ ({c.repliedAtLabelFa}): </strong>
                      {c.pharmacistReplyFa}
                    </div>
                  ) : null}
                </article>
              ))
            )}
          </section>

          <section className="space-y-3">
            <h2 className="flex items-center gap-2 text-sm font-extrabold text-[#122823] dark:text-[#EDF4F7]">
              <Info className="h-4 w-4 text-[#C6862E]" />
              <span>نمونه‌های آموزشی (داده‌های آزمایشی غیرواقعی)</span>
            </h2>
            {DEMO_PHARMACIST_CASES.map((demo) => (
              <article
                key={demo.id}
                className="space-y-2 rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]"
              >
                <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
                  <strong className="font-extrabold text-[#122823] dark:text-[#EDF4F7]">{demo.aliasFa}</strong>
                  <span className="text-[#546963] dark:text-[#9EB3C4]">
                    {demo.categoryFa} · {demo.statusFa}
                  </span>
                </div>
                <p className="text-xs leading-6 text-[#546963] dark:text-[#9EB3C4]">{demo.parentNoteFa}</p>
                <div className="rounded-2xl bg-[#F5F9F6] p-3 text-xs leading-6 dark:bg-[#16243B]">
                  {demo.suggestedReplyFa}
                </div>
                <div className="text-[11px] text-[#546963] dark:text-[#9EB3C4]">
                  پیگیری: {demo.followUpScheduleFa} · {demo.referenceTitle}
                </div>
              </article>
            ))}
          </section>
        </div>
      ) : null}

      {tab === 'desk' ? (
        <section className="space-y-4 rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]">
          <div className="flex items-center gap-2 text-sm font-extrabold text-[#105B48] dark:text-[#00D7C7]">
            <UserCheck className="h-5 w-5" />
            <span>میز راهنمای تمرینی (ثبت یادداشت پاسخ محلی روی پرونده‌های همین دستگاه)</span>
          </div>
          {pharmacistConsultations.length === 0 ? (
            <div className="rounded-2xl bg-[#F5F9F6] p-5 text-center text-xs text-[#546963] dark:bg-[#16243B] dark:text-[#9EB3C4]">
              ابتدا از تب «ثبت پرسش جدید» یک مورد آزمایشی ثبت کنید تا بتوانید پاسخ یا یادداشت داروساز را تمرین و ذخیره کنید.
            </div>
          ) : (
            <div className="space-y-4">
              {pharmacistConsultations.map((c) => (
                <div
                  key={c.id}
                  className="space-y-3 rounded-2xl border border-[#DCE7E1] bg-[#F5F9F6] p-4 dark:border-[#23344D] dark:bg-[#16243B]"
                >
                  <div className="flex items-center justify-between text-xs font-bold">
                    <span>{c.topicFa}</span>
                    <span className="text-[#546963] dark:text-[#9EB3C4]">{c.createdAtLabelFa}</span>
                  </div>
                  <p className="text-xs leading-6">{c.questionTextFa}</p>
                  {c.urgencyLevel === 'EMERGENCY_BYPASS' ? (
                    <div className="flex items-center gap-1.5 text-xs font-bold text-[#B32334] dark:text-[#FF8594]">
                      <ShieldAlert className="h-4 w-4" />
                      <span>پرچم قرمز اورژانسی علامت‌گذاری شده است</span>
                    </div>
                  ) : null}
                  <div className="flex flex-col gap-2 sm:flex-row">
                    <input
                      type="text"
                      value={replyDrafts[c.id] ?? c.pharmacistReplyFa ?? ''}
                      onChange={(e) => setReplyDrafts((prev) => ({ ...prev, [c.id]: e.target.value }))}
                      placeholder="ثبت یادداشت راهنمایی یا توصیه داروساز..."
                      className="min-h-[42px] flex-1 rounded-xl border border-[#DCE7E1] bg-white px-3 text-xs dark:border-[#23344D] dark:bg-[#111C2E]"
                    />
                    <button
                      type="button"
                      onClick={() => {
                        const text = (replyDrafts[c.id] ?? c.pharmacistReplyFa ?? '').trim();
                        if (text) replyPharmacistConsultation(c.id, text);
                      }}
                      className="inline-flex min-h-[42px] items-center justify-center gap-1.5 rounded-xl bg-[#105B48] px-4 text-xs font-bold text-white dark:bg-[#00D7C7] dark:text-[#0B1220]"
                    >
                      <ClipboardCheck className="h-4 w-4" />
                      <span>ثبت یادداشت پاسخ</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          <div className="flex flex-wrap gap-2 pt-2">
            <button
              type="button"
              onClick={() => navigate({ name: 'care-cards' })}
              className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] px-3 text-xs font-bold dark:border-[#23344D]"
            >
              <CheckCircle2 className="h-4 w-4 text-[#105B48] dark:text-[#00D7C7]" />
              <span>ساخت کارت خلاصه مراجعه به داروخانه</span>
            </button>
            <button
              type="button"
              onClick={() => navigate({ name: 'symptoms' })}
              className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] px-3 text-xs font-bold dark:border-[#23344D]"
            >
              <Stethoscope className="h-4 w-4 text-[#105B48] dark:text-[#00D7C7]" />
              <span>بررسی علائم بالینی</span>
            </button>
          </div>
        </section>
      ) : null}
    </div>
  );
};
