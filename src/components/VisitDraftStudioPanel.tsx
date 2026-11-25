import React, { useState } from 'react';
import { Copy, FileText, Loader2, Save } from 'lucide-react';
import { useMano } from '../state/ManoStore.tsx';
import { buildDoctorVisitPrompt } from '../domain/promptBuilders.ts';
import { requestVisitDraftStream, ApiClientError } from '../api/geminiClient.ts';
import { SeverityAlert } from './SeverityAlert.tsx';

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
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const handleGenerate = async () => {
    setErrorMsg(null);
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
      const finalDraft = await requestVisitDraftStream(prompt, (partial) => {
        setDraftText(partial);
      });
      setDraftText(finalDraft);
    } catch (err) {
      if (err instanceof ApiClientError || err instanceof Error) {
        setErrorMsg(err.message);
      } else {
        setErrorMsg('خطا در آماده‌سازی خلاصه ویزیت پزشک.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]">
      <div className="flex items-center gap-2 text-base font-extrabold text-[#122823] dark:text-[#EDF4F7]">
        <FileText className="h-5 w-5 text-[#105B48] dark:text-[#00D7C7]" />
        <span>استودیو ساخت خلاصه ویزیت پزشک اطفال</span>
      </div>
      <p className="mt-1 text-xs leading-5 text-[#546963] dark:text-[#9EB3C4]">
        خلاصه ساختاریافته بر اساس پرونده کودک، ثبت‌های ۲۴ ساعته دما و دارو و یادداشت شما (بدون تشخیص پزشکی).
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
            پرسش‌ها یا یادداشت اضافی برای پزشک
          </label>
          <textarea
            rows={2}
            value={parentNotes}
            onChange={(e) => setParentNotes(e.target.value)}
            placeholder="مثلاً زمان شروع علائم، میزان ادرار یا سوال درباره تغییر شربت..."
            className="mt-1.5 w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] p-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
          />
        </div>

        <button
          type="button"
          disabled={loading}
          onClick={handleGenerate}
          className="inline-flex min-h-[44px] items-center justify-center gap-2 rounded-xl bg-[#105B48] px-5 text-sm font-bold text-white hover:bg-[#0C493A] disabled:opacity-60 dark:bg-[#00D7C7] dark:text-[#0B1220]"
        >
          {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <FileText className="h-4 w-4" />}
          <span>{loading ? 'در حال نگارش خلاصه ویزیت...' : 'ساخت خلاصه ساختاریافته ویزیت'}</span>
        </button>

        {errorMsg ? <SeverityAlert severity="caution" title="امکان ارتباط با سرویس هوشمند نبود" body={errorMsg} /> : null}

        {draftText ? (
          <div className="space-y-3 rounded-2xl border border-[#DCE7E1] bg-[#FAF7F0] p-4 dark:border-[#23344D] dark:bg-[#16243B]">
            <div className="whitespace-pre-line text-sm leading-7 text-[#122823] dark:text-[#EDF4F7]">
              {draftText}
            </div>
            <div className="flex flex-wrap gap-2 pt-2 border-t border-[#DCE7E1] dark:border-[#23344D]">
              <button
                type="button"
                onClick={() => saveVisitDraft(complaint || 'خلاصه ویزیت پزشک', draftText)}
                className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl bg-[#105B48] px-3.5 text-xs font-bold text-white dark:bg-[#00D7C7] dark:text-[#0B1220]"
              >
                <Save className="h-4 w-4" />
                <span>ذخیره در پرونده</span>
              </button>
              <button
                type="button"
                onClick={async () => {
                  await navigator.clipboard.writeText(draftText);
                  notify('متن خلاصه ویزیت کپی شد.', 'success');
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
