import React, { useMemo, useState } from 'react';
import { ArrowRight, CheckSquare, Plus, Scale, Trash2 } from 'lucide-react';
import { DEVELOPMENTAL_MILESTONES } from '../data/contentRepository.ts';
import {
  GROWTH_METRICS,
  evaluateWhoGrowth,
  type ChildSexId,
  type GrowthMetricId,
} from '../domain/whoGrowthStandardsEngine.ts';
import { parseClinicalDecimal } from '../domain/clinicalNumberInput.ts';
import { resolveClinicalAge } from '../domain/clinicalAge.ts';
import { toPersianDigits } from '../domain/persianNormalizer.ts';
import { useMano } from '../state/ManoStore.tsx';
import { WhoGrowthChartCard } from '../components/WhoGrowthChartCard.tsx';
import { SeverityAlert } from '../components/SeverityAlert.tsx';
import { SourceBadge } from '../components/SourceBadge.tsx';

export const GrowthScreen: React.FC<{ initialTab?: 'chart' | 'milestones' }> = ({
  initialTab = 'chart',
}) => {
  const {
    childProfile,
    vaccineBirthDateEpochMs,
    growthMeasurements,
    addGrowthMeasurement,
    deleteGrowthMeasurement,
    checkedMilestones,
    toggleMilestone,
    navigate,
    notify,
  } = useMano();

  const [activeTab, setActiveTab] = useState<'chart' | 'milestones'>(initialTab);
  const profileAge = resolveClinicalAge(childProfile, vaccineBirthDateEpochMs);
  const ageConflict = profileAge.status === 'conflict' || profileAge.status === 'invalid';
  // Zero fabricated sex or initial measurement! Explicitly starts null unless profile already has sex.
  const [selectedSex, setSelectedSex] = useState<ChildSexId | null>(childProfile.sex ?? null);
  const [metricId, setMetricId] = useState<GrowthMetricId>('weight_for_age');
  const [ageMonthsText, setAgeMonthsText] = useState(() =>
    profileAge.ageMonths !== null
      ? String(profileAge.ageMonths)
      : '',
  );
  const [measuredValueText, setMeasuredValueText] = useState('');
  const [heightMethodConfirmed, setHeightMethodConfirmed] = useState(false);

  const parsedAge = parseClinicalDecimal(ageMonthsText);
  const parsedVal = parseClinicalDecimal(measuredValueText);

  const currentAssessment = useMemo(() => {
    if (ageConflict || !selectedSex || parsedAge === null || parsedVal === null) return null;
    if (metricId === 'height_for_age' && !heightMethodConfirmed) return null;
    return evaluateWhoGrowth(metricId, selectedSex, parsedAge, parsedVal);
  }, [ageConflict, selectedSex, metricId, parsedAge, parsedVal, heightMethodConfirmed]);

  const handleRecordMeasurement = (e: React.FormEvent) => {
    e.preventDefault();
    if (ageConflict) {
      notify(profileAge.messageFa || 'ابتدا اختلاف سن پرونده کودک را اصلاح کنید.', 'danger');
      return;
    }
    if (!selectedSex) {
      notify('لطفاً ابتدا جنسیت کودک را برای انتخاب جدول استاندارد WHO انتخاب کنید.', 'danger');
      return;
    }
    if (metricId === 'height_for_age' && !heightMethodConfirmed) {
      notify('لطفاً روش صحیح اندازه‌گیری قد (خوابیده زیر ۲ سال / ایستاده بالای ۲ سال) را تأیید کنید.', 'danger');
      return;
    }
    if (parsedAge === null || parsedAge < 0 || parsedAge > 60) {
      notify('سن باید بین ۰ تا ۶۰ ماه باشد.', 'danger');
      return;
    }
    const cfg = GROWTH_METRICS[metricId];
    if (parsedVal === null || parsedVal < cfg.minValid || parsedVal > cfg.maxValid) {
      notify(`مقدار ${cfg.labelFa} باید بین ${cfg.minValid} تا ${cfg.maxValid} ${cfg.unitFa} باشد.`, 'danger');
      return;
    }
    const evaluated = evaluateWhoGrowth(metricId, selectedSex, parsedAge, parsedVal);
    if (!evaluated) return;

    addGrowthMeasurement({
      id: `gm_${Date.now()}`,
      metricId,
      sex: selectedSex,
      ageMonths: parsedAge,
      measuredValue: parsedVal,
      zScore: evaluated.zScore,
      percentile: evaluated.percentile,
      bandLabelFa: evaluated.bandLabelFa,
      recordedAtLabelFa: 'هم‌اکنون',
    });
  };

  return (
    <div className="space-y-6" data-testid="growth-screen">
      {ageConflict ? (
        <SeverityAlert
          severity="caution"
          title="سن پرونده نیاز به اصلاح دارد"
          body={profileAge.messageFa || 'پیش از ثبت اندازه‌گیری، سن پرونده کودک را اصلاح کنید.'}
          testId="growth-age-conflict"
        />
      ) : null}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            پایش رشد WHO و نقاط عطف تکامل کودک
          </h1>
          <p className="mt-1 text-xs text-[#546963] dark:text-[#9EB3C4]">
            منحنی رسمی سازمان جهانی بهداشت (۰ تا ۶۰ ماه) و چک‌لیست تکاملی برای گفت‌وگو با پزشک
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

      {/* Tab Switcher */}
      <div className="flex gap-2">
        <button
          type="button"
          onClick={() => setActiveTab('chart')}
          className={`min-h-[44px] rounded-xl border px-4 text-xs font-bold ${
            activeTab === 'chart'
              ? 'border-[#105B48] bg-[#E9F5EB] text-[#105B48] dark:border-[#00D7C7] dark:bg-[#0F333A] dark:text-[#00D7C7]'
              : 'border-[#DCE7E1] bg-white text-[#546963] dark:border-[#23344D] dark:bg-[#111C2E]'
          }`}
        >
          منحنی رشد استاندارد WHO
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('milestones')}
          className={`min-h-[44px] rounded-xl border px-4 text-xs font-bold ${
            activeTab === 'milestones'
              ? 'border-[#105B48] bg-[#E9F5EB] text-[#105B48] dark:border-[#00D7C7] dark:bg-[#0F333A] dark:text-[#00D7C7]'
              : 'border-[#DCE7E1] bg-white text-[#546963] dark:border-[#23344D] dark:bg-[#111C2E]'
          }`}
        >
          نقاط عطف تکامل ({toPersianDigits(checkedMilestones.length)})
        </button>
      </div>

      {activeTab === 'chart' ? (
        <div className="space-y-5">
          {/* Measurement Form */}
          <form
            onSubmit={handleRecordMeasurement}
            className="rounded-3xl border border-[#DCE7E1] bg-white p-5 space-y-4 dark:border-[#23344D] dark:bg-[#111C2E]"
          >
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div>
                <div className="text-xs font-bold text-[#122823] dark:text-[#EDF4F7]">
                  ۱. جنسیت کودک (الزامی برای جداول WHO)
                </div>
                <div className="mt-2 flex gap-2">
                  <button
                    type="button"
                    data-testid="growth-sex-boy"
                    onClick={() => setSelectedSex('boy')}
                    className={`min-h-[44px] flex-1 rounded-xl border text-xs font-bold ${
                      selectedSex === 'boy'
                        ? 'border-[#105B48] bg-[#E9F5EB] text-[#105B48] dark:border-[#00D7C7] dark:bg-[#0F333A] dark:text-[#00D7C7]'
                        : 'border-[#DCE7E1] bg-[#F5F9F6] text-[#546963] dark:border-[#23344D] dark:bg-[#16243B]'
                    }`}
                  >
                    پسر
                  </button>
                  <button
                    type="button"
                    data-testid="growth-sex-girl"
                    onClick={() => setSelectedSex('girl')}
                    className={`min-h-[44px] flex-1 rounded-xl border text-xs font-bold ${
                      selectedSex === 'girl'
                        ? 'border-[#105B48] bg-[#E9F5EB] text-[#105B48] dark:border-[#00D7C7] dark:bg-[#0F333A] dark:text-[#00D7C7]'
                        : 'border-[#DCE7E1] bg-[#F5F9F6] text-[#546963] dark:border-[#23344D] dark:bg-[#16243B]'
                    }`}
                  >
                    دختر
                  </button>
                </div>
              </div>

              <div>
                <div className="text-xs font-bold text-[#122823] dark:text-[#EDF4F7]">۲. شاخص اندازه‌گیری</div>
                <div className="mt-2 flex gap-2">
                  <button
                    type="button"
                    onClick={() => {
                      setMetricId('weight_for_age');
                      setHeightMethodConfirmed(false);
                    }}
                    className={`min-h-[44px] flex-1 rounded-xl border text-xs font-bold ${
                      metricId === 'weight_for_age'
                        ? 'border-[#105B48] bg-[#E9F5EB] text-[#105B48] dark:border-[#00D7C7] dark:bg-[#0F333A] dark:text-[#00D7C7]'
                        : 'border-[#DCE7E1] bg-[#F5F9F6] text-[#546963] dark:border-[#23344D] dark:bg-[#16243B]'
                    }`}
                  >
                    وزن برای سن (kg)
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setMetricId('height_for_age');
                      setHeightMethodConfirmed(false);
                    }}
                    className={`min-h-[44px] flex-1 rounded-xl border text-xs font-bold ${
                      metricId === 'height_for_age'
                        ? 'border-[#105B48] bg-[#E9F5EB] text-[#105B48] dark:border-[#00D7C7] dark:bg-[#0F333A] dark:text-[#00D7C7]'
                        : 'border-[#DCE7E1] bg-[#F5F9F6] text-[#546963] dark:border-[#23344D] dark:bg-[#16243B]'
                    }`}
                  >
                    قد برای سن (cm)
                  </button>
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div>
                <label htmlFor="growth-age-input" className="block text-xs font-bold">
                  سن کودک (۰ تا ۶۰ ماه)
                </label>
                <input
                  id="growth-age-input"
                  data-testid="growth-age-input"
                  type="text"
                  inputMode="decimal"
                  value={ageMonthsText}
                  onChange={(e) => setAgeMonthsText(e.target.value)}
                  placeholder="مثلاً 12"
                  className="mt-1.5 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm tabular-nums dark:border-[#23344D] dark:bg-[#16243B]"
                />
              </div>
              <div>
                <label htmlFor="growth-value-input" className="block text-xs font-bold">
                  {metricId === 'weight_for_age' ? 'وزن (کیلوگرم)' : 'قد (سانتی‌متر)'}
                </label>
                <input
                  id="growth-value-input"
                  data-testid="growth-value-input"
                  type="text"
                  inputMode="decimal"
                  value={measuredValueText}
                  onChange={(e) => setMeasuredValueText(e.target.value)}
                  placeholder={metricId === 'weight_for_age' ? 'مثلاً 9.5' : 'مثلاً 75.5'}
                  className="mt-1.5 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm tabular-nums dark:border-[#23344D] dark:bg-[#16243B]"
                />
              </div>
              <div className="flex items-end">
                <button
                  type="submit"
                  data-testid="growth-save-btn"
                  className="flex min-h-[44px] w-full items-center justify-center gap-2 rounded-xl bg-[#105B48] px-4 text-xs font-bold text-white dark:bg-[#00D7C7] dark:text-[#0B1220]"
                >
                  <Plus className="h-4 w-4" />
                  <span>ثبت و رسم روی نمودار</span>
                </button>
              </div>
            </div>

            {metricId === 'height_for_age' ? (
              <label className="flex items-start gap-2.5 rounded-2xl border border-[#E8CC99] bg-[#FDF9F1] p-3 text-xs font-bold text-[#4B3410] dark:border-[#664F27] dark:bg-[#2B2112] dark:text-[#FCE7C0]">
                <input
                  type="checkbox"
                  checked={heightMethodConfirmed}
                  onChange={(e) => setHeightMethodConfirmed(e.target.checked)}
                  className="mt-0.5 h-5 w-5 accent-[#105B48]"
                />
                <span>
                  تأیید می‌کنم که قد کودک زیر ۲۴ ماه به‌صورت خوابیده (طول بدن) و بالای ۲۴ ماه به‌صورت ایستاده بدون کفش اندازه گرفته شده است (تفاوت ۰٫۷ سانتی‌متر استاندارد WHO).
                </span>
              </label>
            ) : null}
          </form>

          {!selectedSex ? (
            <SeverityAlert
              severity="info"
              title="جنسیت کودک هنوز انتخاب نشده است"
              body="برای نمایش دقیق منحنی رشد استاندارد سازمان جهانی بهداشت (WHO) بدون پیش‌فرض تصادفی، ابتدا جنسیت کودک را در بالا انتخاب کنید."
              testId="growth-sex-required-alert"
            />
          ) : (
            <>
              <WhoGrowthChartCard
                metricId={metricId}
                sex={selectedSex}
                assessment={currentAssessment}
                historyPoints={growthMeasurements}
              />

              {currentAssessment ? (
                <SeverityAlert
                  severity={currentAssessment.needsPediatricReview ? 'caution' : 'success'}
                  title={`تفسیر آموزشی: ${currentAssessment.bandLabelFa}`}
                  body={currentAssessment.guidanceFa}
                />
              ) : null}
            </>
          )}

          {/* Recorded Growth History */}
          <section className="rounded-3xl border border-[#DCE7E1] bg-white p-5 space-y-3 dark:border-[#23344D] dark:bg-[#111C2E]">
            <h2 className="flex items-center gap-2 text-sm font-extrabold">
              <Scale className="h-4 w-4 text-[#105B48] dark:text-[#00D7C7]" />
              <span>تاریخچه اندازه‌گیری‌های ثبت‌شده ({toPersianDigits(growthMeasurements.length)})</span>
            </h2>
            {growthMeasurements.length === 0 ? (
              <p className="text-xs text-[#546963] dark:text-[#9EB3C4]">
                هنوز اندازه‌گیری ثبت نشده است. با فرم بالا اولین وزن یا قد کودک را ثبت کنید تا روند رشد شکل بگیرد.
              </p>
            ) : (
              <ul className="divide-y divide-[#DCE7E1] dark:divide-[#23344D]">
                {growthMeasurements.map((entry) => (
                  <li key={entry.id} className="flex items-center justify-between gap-2 py-2.5 text-xs">
                    <div>
                      <span className="font-bold">
                        {entry.metricId === 'weight_for_age' ? 'وزن' : 'قد'} ·{' '}
                        {toPersianDigits(entry.measuredValue)} {entry.metricId === 'weight_for_age' ? 'kg' : 'cm'}
                      </span>
                      <span className="mx-1.5 text-[#546963]">·</span>
                      <span className="text-[#546963] dark:text-[#9EB3C4]">
                        سن {toPersianDigits(entry.ageMonths)} ماه · {entry.bandLabelFa}
                      </span>
                    </div>
                    <button
                      type="button"
                      onClick={() => deleteGrowthMeasurement(entry.id)}
                      aria-label="حذف اندازه‌گیری"
                      className="flex min-h-[36px] min-w-[36px] items-center justify-center rounded-lg text-[#546963] hover:text-[#B32334]"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
      ) : (
        <div className="space-y-4">
          <SeverityAlert
            severity="info"
            title="چک‌لیست نقاط عطف تکامل (ابزار گفت‌وگو با پزشک)"
            body="این موارد تشخیص یا غربالگری قطعی نیستند؛ در صورت از دست رفتن مهارتی که کودک قبلاً داشته یا نگرانی درباره گفتار و حرکت، موضوع را با پزشک اطفال مطرح کنید."
          />

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            {DEVELOPMENTAL_MILESTONES.map((ms) => {
              const checked = checkedMilestones.includes(ms.id);
              return (
                <article
                  key={ms.id}
                  className="rounded-3xl border border-[#DCE7E1] bg-white p-5 space-y-3 dark:border-[#23344D] dark:bg-[#111C2E]"
                >
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <div className="text-xs font-semibold text-[#105B48] dark:text-[#00D7C7]">
                        {ms.ageRangeLabelFa} · {ms.domainLabelFa}
                      </div>
                      <h3 className="mt-1 text-sm font-extrabold text-[#122823] dark:text-[#EDF4F7]">
                        مهارت‌های مورد انتظار این بازه
                      </h3>
                    </div>
                    <button
                      type="button"
                      onClick={() => toggleMilestone(ms.id)}
                      className={`inline-flex min-h-[38px] items-center gap-1.5 rounded-xl border px-3 text-xs font-bold ${
                        checked
                          ? 'border-[#105B48] bg-[#E9F5EB] text-[#105B48] dark:border-[#00D7C7] dark:bg-[#0F333A] dark:text-[#00D7C7]'
                          : 'border-[#DCE7E1] text-[#546963] dark:border-[#23344D]'
                      }`}
                    >
                      <CheckSquare className="h-4 w-4" />
                      <span>{checked ? 'مشاهده شد' : 'علامت‌گذاری'}</span>
                    </button>
                  </div>

                  <ul className="list-disc space-y-1 pr-5 text-xs leading-6 text-[#122823] dark:text-[#EDF4F7]">
                    {[...ms.motorSkillsFa, ...ms.languageSocialFa].map((s, idx) => (
                      <li key={idx}>{s}</li>
                    ))}
                  </ul>

                  <div className="rounded-2xl bg-[#FDF4E4] p-3 text-xs text-[#4B3410] dark:bg-[#2B2112] dark:text-[#FCE7C0]">
                    <span className="font-bold">چه زمانی با پزشک مطرح شود: </span>
                    {ms.whenToDiscussFa}
                  </div>
                </article>
              );
            })}
          </div>
        </div>
      )}

      <SourceBadge sourceIds={['who_growth', 'cdc_milestones', 'nelson8_growth', 'nelson8_development']} />
    </div>
  );
};
