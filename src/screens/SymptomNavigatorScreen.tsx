import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  AlertOctagon,
  ArrowRight,
  Calculator,
  Loader2,
  PhoneCall,
  Sparkles,
  Stethoscope,
  XCircle,
} from 'lucide-react';
import { SYMPTOM_FLOWS } from '../data/contentRepository.ts';
import { AGE_BANDS, type AgeBandId, type SafetyChecklistReport } from '../domain/models.ts';
import type { GatewayResponseMeta } from '../domain/aiGatewayContracts.ts';
import { ageBandFromMonths, evaluateTriage } from '../domain/triageEngine.ts';
import { initialAgeMonthsFromProfile } from '../domain/liquidDoseVisualizerEngine.ts';
import { parseClinicalDecimal } from '../domain/clinicalNumberInput.ts';
import { buildChildContextSummaryFa } from '../domain/promptBuilders.ts';
import {
  ApiClientError,
  isRequestCancelledError,
  requestSafetyChecklist,
} from '../api/geminiClient.ts';
import { useMano } from '../state/ManoStore.tsx';
import { SeverityAlert } from '../components/SeverityAlert.tsx';
import { SourceBadge } from '../components/SourceBadge.tsx';
import { VisitDraftStudioPanel } from '../components/VisitDraftStudioPanel.tsx';
import { GatewayMetaBanner } from '../components/GatewayMetaBanner.tsx';

export const SymptomNavigatorScreen: React.FC<{ initialFlowId?: string }> = ({ initialFlowId }) => {
  const { childProfile, vaccineBirthDateEpochMs, navigate } = useMano();
  const profileAgeMonths = initialAgeMonthsFromProfile(childProfile, vaccineBirthDateEpochMs);
  const [selectedFlowId, setSelectedFlowId] = useState<string>(initialFlowId || SYMPTOM_FLOWS[0].id);
  const [ageBand, setAgeBand] = useState<AgeBandId | null>(() => ageBandFromMonths(profileAgeMonths));
  const [tempText, setTempText] = useState('');
  const [durationHoursText, setDurationHoursText] = useState('');
  const [checkedFlags, setCheckedFlags] = useState<Set<string>>(new Set());

  const [aiChecklist, setAiChecklist] = useState<SafetyChecklistReport | null>(null);
  const [gatewayMeta, setGatewayMeta] = useState<GatewayResponseMeta | null>(null);
  const [policyError, setPolicyError] = useState<string | null>(null);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState<string | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    return () => {
      abortRef.current?.abort();
    };
  }, []);

  const flow = useMemo(
    () => SYMPTOM_FLOWS.find((f) => f.id === selectedFlowId) ?? SYMPTOM_FLOWS[0],
    [selectedFlowId],
  );

  const parsedTemp = useMemo(() => {
    if (!flow.asksTemperature) return null;
    if (!tempText.trim()) return null;
    return parseClinicalDecimal(tempText);
  }, [flow.asksTemperature, tempText]);

  const parsedDuration = useMemo(() => {
    if (!durationHoursText.trim()) return null;
    return parseClinicalDecimal(durationHoursText);
  }, [durationHoursText]);

  const evaluation = useMemo(
    () =>
      evaluateTriage({
        flow,
        ageBand,
        temperatureCelsius: parsedTemp,
        durationHours: parsedDuration,
        selectedRedFlagIds: checkedFlags,
        ageMonthsExact: profileAgeMonths,
      }),
    [flow, ageBand, parsedTemp, parsedDuration, checkedFlags, profileAgeMonths],
  );

  const canUseAi =
    ageBand !== null &&
    evaluation.outcome !== 'URGENT_EMERGENCY' &&
    evaluation.triggeredRule.id !== 'rule_incomplete_input' &&
    evaluation.triggeredRule.id !== 'rule_invalid_input';

  const resetChecklist = () => {
    abortRef.current?.abort();
    abortRef.current = null;
    setAiChecklist(null);
    setGatewayMeta(null);
    setPolicyError(null);
    setAiError(null);
    setAiLoading(false);
  };

  const toggleFlag = (id: string) => {
    resetChecklist();
    setCheckedFlags((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleCancelChecklist = () => {
    abortRef.current?.abort();
    abortRef.current = null;
    setAiLoading(false);
    setAiError('تولید توضیح چک‌لیست ایمنی توسط کاربر لغو شد.');
  };

  const handleRequestChecklist = async () => {
    if (!canUseAi || ageBand === null) return;
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    setAiLoading(true);
    setAiError(null);
    setPolicyError(null);
    setGatewayMeta(null);
    try {
      const selectedLabels = flow.redFlags
        .filter((r) => checkedFlags.has(r.id))
        .map((r) => r.labelFa);
      const result = await requestSafetyChecklist({
        flowId: flow.id,
        flowTitleFa: flow.titleFa,
        urgencyLevel: evaluation.outcome,
        evaluationSummaryFa: evaluation.summaryFa,
        selectedSymptomsFa: selectedLabels,
        childContextSummaryFa: buildChildContextSummaryFa({
          ...childProfile,
          ageBand,
          ageMonthsExact: profileAgeMonths,
          birthDateOrAgeFa: profileAgeMonths === null ? '' : childProfile.birthDateOrAgeFa,
        }),
        signal: controller.signal,
      });
      if (abortRef.current !== controller) return;
      setAiChecklist(result.data);
      setGatewayMeta(result.meta);
      setPolicyError(result.policyError ?? null);
    } catch (err) {
      if (abortRef.current !== controller) return;
      if (isRequestCancelledError(err)) {
        setAiError('درخواست توسط کاربر لغو شد.');
      } else {
        setAiError(
          err instanceof ApiClientError || err instanceof Error
            ? err.message
            : 'خطا در دریافت توضیح چک‌لیست ایمنی.',
        );
      }
    } finally {
      if (abortRef.current === controller) {
        abortRef.current = null;
        setAiLoading(false);
      }
    }
  };

  const selectedFlagLabels = flow.redFlags.filter((r) => checkedFlags.has(r.id)).map((r) => r.labelFa);

  return (
    <div className="space-y-6" data-testid="symptom-navigator-screen">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            راهنمای بررسی علائم و نشانه‌های خطر کودک
          </h1>
          <p className="mt-1 text-xs text-[#546963] dark:text-[#9EB3C4]">
            غربالگری علائم فوری پیش از هر توصیه مراقبت خانگی · ابزار تشخیصی نیست
          </p>
        </div>
        <button
          type="button"
          onClick={() => navigate({ name: 'home' })}
          className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] px-3 text-xs font-bold dark:border-[#23344D]"
        >
          <ArrowRight className="h-4 w-4" />
          <span>بازگشت به خانه</span>
        </button>
      </div>

      {/* Step 1: Choose Symptom Flow */}
      <section className="rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]">
        <h2 className="mb-3 text-sm font-extrabold text-[#105B48] dark:text-[#00D7C7]">
          ۱. علامت اصلی کودک را انتخاب کنید
        </h2>
        <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-3">
          {SYMPTOM_FLOWS.map((f) => {
            const active = f.id === flow.id;
            return (
              <button
                key={f.id}
                type="button"
                data-testid={`symptom-flow-btn-${f.id}`}
                onClick={() => {
                  resetChecklist();
                  setSelectedFlowId(f.id);
                  setCheckedFlags(new Set());
                }}
                className={`flex items-start gap-3 rounded-2xl border p-3.5 text-right transition-colors ${
                  active
                    ? 'border-[#105B48] bg-[#E9F5EB] dark:border-[#00D7C7] dark:bg-[#0F333A]'
                    : 'border-[#DCE7E1] bg-[#FAF7F0]/50 hover:border-[#105B48] dark:border-[#23344D] dark:bg-[#16243B]'
                }`}
              >
                <Stethoscope className="mt-0.5 h-5 w-5 shrink-0 text-[#105B48] dark:text-[#00D7C7]" />
                <div className="min-w-0">
                  <div className="text-sm font-bold text-[#122823] dark:text-[#EDF4F7]">{f.titleFa}</div>
                  <div className="mt-0.5 line-clamp-2 text-xs text-[#546963] dark:text-[#9EB3C4]">
                    {f.shortDescFa}
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      </section>

      {/* Step 2: Child Age Band, Temperature & Red Flags */}
      <section className="grid grid-cols-1 gap-5 lg:grid-cols-12">
        <div className="space-y-4 rounded-3xl border border-[#DCE7E1] bg-white p-5 lg:col-span-6 dark:border-[#23344D] dark:bg-[#111C2E]">
          <h2 className="text-sm font-extrabold text-[#105B48] dark:text-[#00D7C7]">
            ۲. سن کودک و بررسی نشانه‌های خطر
          </h2>

          <div>
            <label className="block text-xs font-bold text-[#122823] dark:text-[#EDF4F7]">بازه سنی کودک</label>
            <div className="mt-2 flex flex-wrap gap-2">
              {AGE_BANDS.map((b) => (
                <button
                  key={b.id}
                  type="button"
                  data-testid={`triage-age-${b.id}`}
                  aria-pressed={ageBand === b.id}
                  disabled={profileAgeMonths !== null}
                  onClick={() => {
                    resetChecklist();
                    setAgeBand(b.id);
                  }}
                  className={`min-h-[40px] rounded-xl border px-3 text-xs font-bold ${
                    ageBand === b.id
                      ? 'border-[#105B48] bg-[#E9F5EB] text-[#105B48] dark:border-[#00D7C7] dark:bg-[#0F333A] dark:text-[#00D7C7]'
                      : 'border-[#DCE7E1] bg-[#F5F9F6] text-[#546963] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#9EB3C4]'
                  }`}
                >
                  {b.labelFa}
                </button>
              ))}
            </div>
            <p data-testid="triage-age-status" className="mt-2 text-xs text-[#546963] dark:text-[#9EB3C4]">
              {profileAgeMonths !== null
                ? `سن واقعی ثبت‌شده در پرونده (${profileAgeMonths} ماه) برای ارزیابی استفاده می‌شود.`
                : ageBand === null
                  ? 'سن کودک ثبت نشده است؛ بازه سنی واقعی را انتخاب کنید.'
                  : 'بازه سنی انتخاب‌شده برای این ارزیابی استفاده می‌شود.'}
            </p>
            {profileAgeMonths !== null ? (
              <button type="button" onClick={() => navigate({ name: 'my-child' })} className="mt-2 min-h-[40px] text-xs font-bold text-[#105B48] dark:text-[#00D7C7]">
                اصلاح سن در پرونده کودک
              </button>
            ) : null}
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {flow.asksTemperature ? (
              <div>
                <label htmlFor="triage-temp-input" className="block text-xs font-bold">
                  دمای ثبت‌شده (°C)
                </label>
                <input
                  id="triage-temp-input"
                  data-testid="triage-temp-input"
                  type="text"
                  inputMode="decimal"
                  value={tempText}
                  onChange={(e) => {
                    resetChecklist();
                    setTempText(e.target.value);
                  }}
                  placeholder="مثلاً 38.3"
                  className="mt-1.5 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm tabular-nums dark:border-[#23344D] dark:bg-[#16243B]"
                />
              </div>
            ) : null}

            <div>
              <label htmlFor="triage-duration-input" className="block text-xs font-bold">
                مدت زمان علامت (ساعت)
              </label>
              <input
                id="triage-duration-input"
                data-testid="triage-duration-input"
                type="text"
                inputMode="numeric"
                value={durationHoursText}
                onChange={(e) => {
                  resetChecklist();
                  setDurationHoursText(e.target.value);
                }}
                placeholder="مثلاً 12"
                className="mt-1.5 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm tabular-nums dark:border-[#23344D] dark:bg-[#16243B]"
              />
            </div>
          </div>

          <div className="space-y-2 pt-2 border-t border-[#DCE7E1] dark:border-[#23344D]">
            <div className="text-xs font-bold text-[#B32334] dark:text-[#FF8594]">
              آیا هیچ‌یک از این نشانه‌ها وجود دارد؟ (در صورت مشاهده تیک بزنید)
            </div>
            {flow.redFlags.map((rf) => (
              <label
                key={rf.id}
                className="flex cursor-pointer items-start gap-2.5 rounded-2xl border border-[#DCE7E1] bg-[#FAF7F0]/50 p-3 text-xs leading-5 dark:border-[#23344D] dark:bg-[#16243B]"
              >
                <input
                  type="checkbox"
                  checked={checkedFlags.has(rf.id)}
                  onChange={() => toggleFlag(rf.id)}
                  className="mt-0.5 h-5 w-5 shrink-0 accent-[#C42B3C]"
                />
                <div>
                  <div className="font-bold text-[#122823] dark:text-[#EDF4F7]">{rf.labelFa}</div>
                  <div className="text-[#546963] dark:text-[#9EB3C4]">{rf.detailFa}</div>
                </div>
              </label>
            ))}
          </div>
        </div>

        {/* Step 3: Real-time Clinical Evaluation Outcome */}
        <div className="space-y-4 lg:col-span-6">
          <div data-testid="triage-outcome-panel" className="space-y-4">
            <SeverityAlert
              severity={
                evaluation.outcome === 'URGENT_EMERGENCY'
                  ? 'danger'
                  : evaluation.outcome === 'NEEDS_TIMELY_REVIEW'
                    ? 'caution'
                    : 'success'
              }
              title={evaluation.titleFa}
              body={evaluation.summaryFa}
              bullets={evaluation.immediateActionsFa}
              testId={`triage-result-${evaluation.outcome}`}
            >
              {evaluation.outcome === 'URGENT_EMERGENCY' ? (
                <div className="flex flex-wrap gap-2 pt-2">
                  <a
                    href="tel:115"
                    className="inline-flex min-h-[42px] items-center gap-2 rounded-xl bg-[#B32334] px-4 text-xs font-bold text-white"
                  >
                    <PhoneCall className="h-4 w-4" />
                    <span>تماس فوری با اورژانس ۱۱۵</span>
                  </a>
                </div>
              ) : null}
            </SeverityAlert>

            {evaluation.canShowSelfCare ? (
              <section className="rounded-3xl border border-[#DCE7E1] bg-white p-5 space-y-3 dark:border-[#23344D] dark:bg-[#111C2E]">
                <h3 className="text-sm font-extrabold text-[#122823] dark:text-[#EDF4F7]">
                  موارد نیازمند مراجعه و پیگیری پزشکی
                </h3>
                <ul className="list-disc space-y-1.5 pr-5 text-sm leading-6 text-[#546963] dark:text-[#9EB3C4]">
                  {evaluation.escalationCriteriaFa.map((c, idx) => (
                    <li key={idx}>{c}</li>
                  ))}
                </ul>

                {flow.avoidActionsFa.length > 0 ? (
                  <div className="rounded-2xl bg-[#FDF4E4] p-3 text-xs leading-6 text-[#4B3410] dark:bg-[#2B2112] dark:text-[#FCE7C0]">
                    <div className="font-bold flex items-center gap-1.5">
                      <AlertOctagon className="h-4 w-4" />
                      <span>موارد پرهیز:</span>
                    </div>
                    <ul className="mt-1 list-disc pr-5">
                      {flow.avoidActionsFa.map((a, i) => (
                        <li key={i}>{a}</li>
                      ))}
                    </ul>
                  </div>
                ) : null}

                <div className="flex flex-wrap gap-2 pt-2">
                  {flow.id === 'fever' && ageBand !== 'NEWBORN' ? (
                    <button
                      type="button"
                      onClick={() => navigate({ name: 'fever-dose' })}
                      className="inline-flex min-h-[42px] items-center gap-1.5 rounded-xl bg-[#105B48] px-4 text-xs font-bold text-white dark:bg-[#00D7C7] dark:text-[#0B1220]"
                    >
                      <Calculator className="h-4 w-4" />
                      <span>محاسبه دوز ایمن تب‌بر</span>
                    </button>
                  ) : null}

                  <button
                    type="button"
                    onClick={() => navigate({ name: 'topic-detail', slug: flow.relatedArticleSlug })}
                    className="inline-flex min-h-[42px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] px-4 text-xs font-bold dark:border-[#23344D]"
                  >
                    <span>مطالعه مقاله مرتبط</span>
                  </button>

                  {evaluation.showPharmacistOption ? (
                    <button
                      type="button"
                      onClick={() => navigate({ name: 'ask-pharmacist', initialTopic: flow.titleFa })}
                      className="inline-flex min-h-[42px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] px-4 text-xs font-bold dark:border-[#23344D]"
                    >
                      <span>ثبت پرسش برای داروساز</span>
                    </button>
                  ) : null}
                </div>
              </section>
            ) : null}

            {/* AI Safety Checklist Add-on (disabled during immediate emergency bypass) */}
            {canUseAi ? (
              <section className="rounded-3xl border border-[#DCE7E1] bg-white p-5 space-y-3 dark:border-[#23344D] dark:bg-[#111C2E]">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div>
                    <h3 className="text-sm font-extrabold text-[#122823] dark:text-[#EDF4F7]">
                      چک‌لیست پایش ایمنی هوشمند
                    </h3>
                    <p className="text-xs text-[#546963] dark:text-[#9EB3C4]">
                      تهیه فهرست پایش خانگی و پرسش‌های مطب با هوش مصنوعی
                    </p>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      data-testid="triage-ai-checklist-btn"
                      disabled={aiLoading}
                      onClick={handleRequestChecklist}
                      className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl bg-[#105B48] px-4 text-xs font-bold text-white disabled:opacity-60 dark:bg-[#00D7C7] dark:text-[#0B1220]"
                    >
                      {aiLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
                      <span>توضیح چک‌لیست ایمنی</span>
                    </button>
                    {aiLoading ? (
                      <button
                        type="button"
                        onClick={handleCancelChecklist}
                        className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-[#E7B5BC] bg-[#FDF0F1] px-3 text-xs font-bold text-[#B32334] dark:border-[#6A2C3B] dark:bg-[#2F161E] dark:text-[#FF8594]"
                      >
                        <XCircle className="h-4 w-4" />
                        <span>لغو</span>
                      </button>
                    ) : null}
                  </div>
                </div>

                {aiError ? <SeverityAlert severity="caution" title="یادآوری" body={aiError} /> : null}
                {gatewayMeta ? <GatewayMetaBanner meta={gatewayMeta} policyError={policyError} /> : null}

                {aiChecklist ? (
                  <div className="space-y-3 rounded-2xl bg-[#F5F9F6] p-4 text-xs leading-6 dark:bg-[#16243B]">
                    <div className="font-extrabold text-[#105B48] dark:text-[#00D7C7]">
                      {aiChecklist.checklistTitleFa}
                    </div>
                    <p>{aiChecklist.urgencySummaryFa}</p>
                    <div>
                      <div className="font-bold">نکات پایش در منزل:</div>
                      <ul className="list-disc pr-5">
                        {aiChecklist.homeMonitoringChecklistFa.map((item, idx) => (
                          <li key={idx}>{item}</li>
                        ))}
                      </ul>
                    </div>
                    <div>
                      <div className="font-bold">پرسش‌های پیشنهادی برای پزشک:</div>
                      <ul className="list-disc pr-5">
                        {aiChecklist.questionsForPediatricianFa.map((item, idx) => (
                          <li key={idx}>{item}</li>
                        ))}
                      </ul>
                    </div>
                    <p className="text-[11px] text-[#546963] dark:text-[#9EB3C4]">
                      {aiChecklist.clinicalSourceCitationFa}
                    </p>
                  </div>
                ) : null}
              </section>
            ) : null}

            {canUseAi && profileAgeMonths !== null ? (
              <VisitDraftStudioPanel
                key={`${flow.id}:${ageBand}:${tempText}:${durationHoursText}:${selectedFlagLabels.join('|')}`}
                defaultComplaintFa={flow.titleFa}
                selectedSymptomsFa={selectedFlagLabels}
              />
            ) : null}
          </div>

          <SourceBadge sourceIds={flow.sourceIds} reviewStatus={flow.reviewStatus} />
        </div>
      </section>
    </div>
  );
};
