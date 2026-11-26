import React, { useEffect, useRef, useState } from 'react';
import { Copy, FileText, Loader2, Save, XCircle } from 'lucide-react';
import { useMano } from '../state/ManoStore.tsx';
import { buildChildContextSummaryFa, buildDoctorVisitPrompt } from '../domain/promptBuilders.ts';
import { formatTempC } from '../domain/feverDoseTimelineEngine.ts';
import type { GatewayResponseMeta } from '../domain/aiGatewayContracts.ts';
import {
  ApiClientError,
  isRequestCancelledError,
  requestVisitDraftStream,
} from '../api/geminiClient.ts';
import { SeverityAlert } from './SeverityAlert.tsx';
import { GatewayMetaBanner } from './GatewayMetaBanner.tsx';

interface VisitDraftStudioPanelProps {
  defaultComplaintFa?: string;
  selectedSymptomsFa?: string[];
}

export const VisitDraftStudioPanel: React.FC<VisitDraftStudioPanelProps> = ({
  defaultComplaintFa = '',
  selectedSymptomsFa = [],
}) => {
  const { childProfile, timelineSnapshot, saveVisitDraft, notify } = useMano();
  const [complaint, setComplaint] = useState(defaultComplaintFa);
  const [parentNotes, setParentNotes] = useState('');
  const [loading, setLoading] = useState(false);
  const [draftText, setDraftText] = useState('');
  const [gatewayMeta, setGatewayMeta] = useState<GatewayResponseMeta | null>(null);
  const [policyError, setPolicyError] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    return () => {
      abortRef.current?.abort();
    };
  }, []);

  const handleCancel = () => {
    abortRef.current?.abort();
    abortRef.current = null;
    setLoading(false);
    setErrorMsg('تولید پیش‌نویس توسط کاربر لغو شد.');
  };

  const handleGenerate = async () => {
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    setErrorMsg(null);
    setPolicyError(null);
    setGatewayMeta(null);
    setLoading(true);
    setDraftText('');

    try {
      const prompt = buildDoctorVisitPrompt({
        profile: childProfile,
        timelineSnapshot,
        chiefComplaintFa: complaint,
        selectedSymptomsFa,
        parentNotesFa: parentNotes,
      });

      const tempPointsFa = timelineSnapshot.tempPoints
        .slice(-8)
        .map((t) => `ساعت ${t.timeLabelFa}: ${formatTempC(t.tempCelsius)} (${t.siteFa}) — ${t.caregiverFa}`);
      const dosePointsFa = timelineSnapshot.dosePoints
        .slice(-8)
        .map(
          (d) =>
            `ساعت ${d.givenAtLabelFa}: ${d.medicationNameFa} ${d.volumeMlText}${d.noteFa ? ` (${d.noteFa})` : ''}`,
        );

      const result = await requestVisitDraftStream(
        {
          promptText: prompt,
          chiefComplaintFa: complaint,
          selectedSymptomsFa,
          parentNotesFa: parentNotes,
          childContextSummaryFa: buildChildContextSummaryFa(childProfile),
          tempPointsFa,
          dosePointsFa,
          signal: controller.signal,
        },
        (partial) => {
          setDraftText(partial);
        },
      );

      setDraftText(result.data.formattedDraftFa);
      setGatewayMeta(result.meta);
      setPolicyError(result.policyError ?? null);
    } catch (err) {
      if (isRequestCancelledError(err)) {
        setErrorMsg('درخواست خلاصه ویزیت لغو شد.');
      } else if (err instanceof ApiClientError || err instanceof Error) {
        setErrorMsg(err.message);
      } else {
        setErrorMsg('خطا در آماده‌سازی خلاصه ویزیت پزشک.');
      }
    } finally {
      if (abortRef.current === controller) {
        abortRef.current = null;
      }
      setLoading(false);
    }
  };

  return (
    <section className="rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]">
      <div className="flex items-center gap-2 text-base font-extrabold text-[#122823] dark:text-[#EDF4F7]">
        <FileText className="h-5 w-5 text-[#105B48] dark:text-[#00D7C7]" />
        <span>استودیو ساخت پیش‌نویس ویزیت پزشک یا داروساز</span>
      </div>
      <p className="mt-1 text-xs leading-5 text-[#546963] dark:text-[#9EB3C4]">
        Gemini صرفاً دستیار تنظیم ساختاریافته اطلاعات ثبت‌شده شما برای مرور پزشک/داروساز است؛ تشخیص یا دوز دارو صادر نمی‌کند.
      </p>

      <div className="mt-4 space-y-3">
        <div>
          <label className="block text-xs font-bold text-[#122823] dark:text-[#EDF4F7]">
            علت اصلی مراجعه یا دغدغه فعلی
          </label>
          <input
            type="text"
            value={complaint}
            onChange={(e) => setComplaint(e.target.value)}
            placeholder="مثلاً تب از دیروز و بی‌اشتهایی..."
            className="mt-1.5 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
          />
        </div>

        <div>
          <label className="block text-xs font-bold text-[#122823] dark:text-[#EDF4F7]">
            پرسش‌ها یا یادداشت اضافی برای پزشک یا داروساز
          </label>
          <textarea
            rows={2}
            value={parentNotes}
            onChange={(e) => setParentNotes(e.target.value)}
            placeholder="مثلاً زمان شروع علائم، میزان ادرار یا سوال درباره تغییر شربت..."
            className="mt-1.5 w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] p-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
          />
        </div>

        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            disabled={loading}
            onClick={handleGenerate}
            data-testid="visit-draft-generate-btn"
            className="inline-flex min-h-[44px] items-center justify-center gap-2 rounded-xl bg-[#105B48] px-5 text-sm font-bold text-white hover:bg-[#0C493A] disabled:opacity-60 dark:bg-[#00D7C7] dark:text-[#0B1220]"
          >
            {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <FileText className="h-4 w-4" />}
            <span>{loading ? 'در حال تنظیم پیش‌نویس ویزیت...' : 'آماده‌سازی پیش‌نویس برای پزشک'}</span>
          </button>

          {loading ? (
            <button
              type="button"
              onClick={handleCancel}
              data-testid="visit-draft-cancel-btn"
              className="inline-flex min-h-[44px] items-center gap-1.5 rounded-xl border border-[#E7B5BC] bg-[#FDF0F1] px-4 text-xs font-bold text-[#B32334] dark:border-[#6A2C3B] dark:bg-[#2F161E] dark:text-[#FF8594]"
            >
              <XCircle className="h-4 w-4" />
              <span>لغو درخواست</span>
            </button>
          ) : null}
        </div>

        {errorMsg ? <SeverityAlert severity="caution" title="وضعیت آماده‌سازی پیش‌نویس" body={errorMsg} /> : null}

        {gatewayMeta ? <GatewayMetaBanner meta={gatewayMeta} policyError={policyError} /> : null}

        {draftText ? (
          <div className="space-y-3 rounded-2xl border border-[#DCE7E1] bg-[#FAF7F0] p-4 dark:border-[#23344D] dark:bg-[#16243B]">
            <div className="whitespace-pre-line text-sm leading-7 text-[#122823] dark:text-[#EDF4F7]">
              {draftText}
            </div>
            <div className="flex flex-wrap gap-2 pt-2 border-t border-[#DCE7E1] dark:border-[#23344D]">
              <button
                type="button"
                onClick={() => saveVisitDraft(complaint || 'پیش‌نویس ویزیت پزشک', draftText)}
                className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl bg-[#105B48] px-3.5 text-xs font-bold text-white dark:bg-[#00D7C7] dark:text-[#0B1220]"
              >
                <Save className="h-4 w-4" />
                <span>ذخیره در پرونده</span>
              </button>
              <button
                type="button"
                onClick={async () => {
                  await navigator.clipboard.writeText(draftText);
                  notify('متن پیش‌نویس ویزیت کپی شد.', 'success');
                }}
                className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] px-3.5 text-xs font-bold text-[#122823] dark:border-[#23344D] dark:text-[#EDF4F7]"
              >
                <Copy className="h-4 w-4" />
                <span>کپی متن</span>
              </button>
            </div>
          </div>
        ) : null}
      </div>
    </section>
  );
};
