import React, { useMemo, useState } from 'react';
import { ArrowRight, Calculator, Camera, Loader2, ScanLine, ShieldAlert } from 'lucide-react';
import {
  LIQUID_FORMULATION_LIST,
  initialAgeMonthsFromProfile,
  type LiquidFormulation,
} from '../domain/liquidDoseVisualizerEngine.ts';
import {
  defaultDenominatorMl,
  defaultNumeratorMg,
  reconcileEditableSyrup,
  reconcileFromAiPayload,
} from '../domain/syrupConcentrationMatcher.ts';
import type { SyrupDosageForm, SyrupIngredientKey, SyrupScanConfidence } from '../domain/models.ts';
import { buildChildContextSummaryFa } from '../domain/promptBuilders.ts';
import { requestSyrupScan, ApiClientError } from '../api/geminiClient.ts';
import { useMano } from '../state/ManoStore.tsx';
import { SeverityAlert } from '../components/SeverityAlert.tsx';
import { VisualSyringeCanvas } from '../components/VisualSyringeCanvas.tsx';
import { SourceBadge } from '../components/SourceBadge.tsx';

export const SyrupScannerScreen: React.FC = () => {
  const { childProfile, vaccineBirthDateEpochMs, savedSyrupScans, saveSyrupScan, navigate } =
    useMano();

  const [drugNameFa, setDrugNameFa] = useState('شربت استامینوفن کودکان');
  const [ingredientKey, setIngredientKey] = useState<SyrupIngredientKey>('ACETAMINOPHEN');
  const [dosageForm, setDosageForm] = useState<SyrupDosageForm>('SYRUP');
  const [mgAmountText, setMgAmountText] = useState('120');
  const [perMlAmountText, setPerMlAmountText] = useState('5');
  const [confidence, setConfidence] = useState<SyrupScanConfidence>('HIGH');
  const [aiSafetyNote, setAiSafetyNote] = useState('');
  const [presetOverride, setPresetOverride] = useState<LiquidFormulation | null>(
    LIQUID_FORMULATION_LIST[1],
  );
  // Mandatory parent verification checkbox — resets to false whenever any field changes!
  const [parentVerified, setParentVerified] = useState(false);

  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [scanning, setScanning] = useState(false);
  const [scanError, setScanError] = useState<string | null>(null);

  const reconciled = useMemo(() => {
    const mg = Number.parseFloat(mgAmountText);
    const perMl = Number.parseFloat(perMlAmountText);
    return reconcileEditableSyrup({
      drugNameFa,
      ingredientKey,
      dosageForm,
      mgAmount: Number.isFinite(mg) ? mg : 0,
      perMlAmount: Number.isFinite(perMl) ? perMl : 0,
      confidence,
      aiSafetyNoteFa: aiSafetyNote,
      presetOverride,
      isPediatricFeverOrAllergyLiquid: true,
      childProfile,
      birthDateEpochMs: vaccineBirthDateEpochMs,
    });
  }, [
    drugNameFa,
    ingredientKey,
    dosageForm,
    mgAmountText,
    perMlAmountText,
    confidence,
    aiSafetyNote,
    presetOverride,
    childProfile,
    vaccineBirthDateEpochMs,
  ]);

  const applyPreset = (f: LiquidFormulation) => {
    setPresetOverride(f);
    setDrugNameFa(f.titleFa);
    setIngredientKey(
      f.key.startsWith('IBUPROFEN')
        ? 'IBUPROFEN'
        : f.key.startsWith('CETIRIZINE')
          ? 'CETIRIZINE'
          : 'ACETAMINOPHEN',
    );
    setDosageForm(
      f.key === 'ACETAMINOPHEN_DROPS_100'
        ? 'DROPS'
        : f.key === 'IBUPROFEN_SYRUP_100_5' || f.key === 'ACETAMINOPHEN_SYRUP_160_5'
          ? 'SUSPENSION'
          : 'SYRUP',
    );
    setMgAmountText(String(defaultNumeratorMg(f)));
    setPerMlAmountText(String(defaultDenominatorMl(f)));
    setConfidence('HIGH');
    setAiSafetyNote('');
    setParentVerified(false);
  };

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setScanError(null);
    setParentVerified(false);

    const reader = new FileReader();
    reader.onload = async () => {
      const dataUrl = typeof reader.result === 'string' ? reader.result : '';
      if (!dataUrl) return;
      setPreviewUrl(dataUrl);
      setScanning(true);
      try {
        const payload = await requestSyrupScan({
          imageBase64: dataUrl,
          mimeType: file.type || 'image/jpeg',
          childContextSummaryFa: buildChildContextSummaryFa(childProfile),
        });
        const rec = reconcileFromAiPayload(payload, childProfile, vaccineBirthDateEpochMs);
        setPresetOverride(rec.matchedFormulation);
        setDrugNameFa(rec.drugNameFa);
        setIngredientKey(rec.ingredientKey);
        setDosageForm(rec.dosageForm);
        setMgAmountText(String(rec.mgAmount || ''));
        setPerMlAmountText(String(rec.perMlAmount || ''));
        setConfidence(rec.confidence);
        setAiSafetyNote(rec.safetyNoteFa);
        setParentVerified(false);
      } catch (err) {
        setScanError(
          err instanceof ApiClientError || err instanceof Error
            ? err.message
            : 'تحلیل تصویر برچسب با خطا مواجه شد. می‌توانید غلظت بطری را به‌صورت دستی در پایین انتخاب کنید.',
        );
      } finally {
        setScanning(false);
      }
    };
    reader.readAsDataURL(file);
  };

  const handleOpenCalculator = () => {
    if (!parentVerified || !reconciled.matchedFormulation) return;
    saveSyrupScan({
      id: `scan_${Date.now()}`,
      detectedDrugNameFa: reconciled.drugNameFa,
      ingredientKey: reconciled.ingredientKey,
      dosageForm: reconciled.dosageForm,
      mgAmount: reconciled.mgAmount,
      perMlAmount: reconciled.perMlAmount,
      matchedFormulationIdRaw: reconciled.matchedFormulationIdRaw,
      confidence: reconciled.confidence,
      safetyNoteFa: reconciled.safetyNoteFa,
      parentVerified: true,
      createdAtMs: Date.now(),
      createdAtLabelFa: 'هم‌اکنون',
    });
    navigate({ name: 'fever-dose', prefillFormulationId: reconciled.matchedFormulation.id });
  };

  const ageMonths = initialAgeMonthsFromProfile(childProfile, vaccineBirthDateEpochMs);

  return (
    <div className="space-y-6" data-testid="syrup-scanner-screen">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            اسکنر هوشمند برچسب شربت و تطبیق غلظت
          </h1>
          <p className="mt-1 text-xs text-[#546963] dark:text-[#9EB3C4]">
            خواندن غلظت میلی‌گرم بر میلی‌لیتر از تصویر بطری یا انتخاب کالیبره‌شده با تأیید اجباری والد
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

      {/* Image Upload & Calibrated Quick Presets */}
      <section className="grid grid-cols-1 gap-5 lg:grid-cols-12">
        <div className="space-y-4 rounded-3xl border border-[#DCE7E1] bg-white p-5 lg:col-span-6 dark:border-[#23344D] dark:bg-[#111C2E]">
          <h2 className="flex items-center gap-2 text-sm font-extrabold text-[#105B48] dark:text-[#00D7C7]">
            <Camera className="h-4 w-4" />
            <span>۱. بارگذاری تصویر برچسب دارو یا انتخاب سریع فرمولاسیون</span>
          </h2>

          <label className="flex min-h-[110px] cursor-pointer flex-col items-center justify-center rounded-2xl border-2 border-dashed border-[#DCE7E1] bg-[#F5F9F6] p-4 text-center hover:border-[#105B48] dark:border-[#23344D] dark:bg-[#16243B]">
            <input
              type="file"
              accept="image/*"
              onChange={handleImageUpload}
              className="sr-only"
            />
            {scanning ? (
              <div className="flex items-center gap-2 text-xs font-bold text-[#105B48] dark:text-[#00D7C7]">
                <Loader2 className="h-5 w-5 animate-spin" />
                <span>در حال خواندن غلظت روی برچسب...</span>
              </div>
            ) : (
              <>
                <ScanLine className="h-7 w-7 text-[#105B48] dark:text-[#00D7C7]" />
                <span className="mt-2 text-xs font-bold text-[#122823] dark:text-[#EDF4F7]">
                  انتخاب عکس برچسب شربت یا قطره (تحلیل با هوش مصنوعی)
                </span>
                <span className="mt-0.5 text-[11px] text-[#546963] dark:text-[#9EB3C4]">
                  یا در پایین از دکمه‌های فرمولاسیون استاندارد استفاده کنید
                </span>
              </>
            )}
          </label>

          {previewUrl ? (
            <img
              src={previewUrl}
              alt="پیش‌نمایش برچسب شربت"
              className="max-h-48 w-full rounded-2xl border border-[#DCE7E1] object-contain dark:border-[#23344D]"
            />
          ) : null}

          {scanError ? <SeverityAlert severity="caution" title="نتیجه اسکن تصویر" body={scanError} /> : null}

          <div>
            <div className="mb-2 text-xs font-bold text-[#122823] dark:text-[#EDF4F7]">
              فرمولاسیون‌های استاندارد رایج (برای تطبیق فوری):
            </div>
            <div className="flex flex-wrap gap-2">
              {LIQUID_FORMULATION_LIST.map((f) => (
                <button
                  key={f.id}
                  type="button"
                  data-testid={`scanner-preset-${f.id}`}
                  onClick={() => applyPreset(f)}
                  className={`min-h-[38px] rounded-xl border px-3 text-xs font-bold ${
                    presetOverride?.id === f.id
                      ? 'border-[#105B48] bg-[#E9F5EB] text-[#105B48] dark:border-[#00D7C7] dark:bg-[#0F333A] dark:text-[#00D7C7]'
                      : 'border-[#DCE7E1] bg-[#F5F9F6] text-[#546963] dark:border-[#23344D] dark:bg-[#16243B]'
                  }`}
                >
                  {f.shortLabelFa}
                </button>
              ))}
            </div>
          </div>

          {/* Editable Verification Fields — editing any resets parentVerified */}
          <div className="grid grid-cols-1 gap-3 pt-2 border-t border-[#DCE7E1] sm:grid-cols-2 dark:border-[#23344D]">
            <div>
              <label className="block text-xs font-bold">مقدار ماده مؤثره (mg)</label>
              <input
                type="number"
                value={mgAmountText}
                onChange={(e) => {
                  setMgAmountText(e.target.value);
                  setPresetOverride(null);
                  setParentVerified(false);
                }}
                className="mt-1 min-h-[42px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm tabular-nums dark:border-[#23344D] dark:bg-[#16243B]"
              />
            </div>
            <div>
              <label className="block text-xs font-bold">در هر حجم (mL)</label>
              <input
                type="number"
                value={perMlAmountText}
                onChange={(e) => {
                  setPerMlAmountText(e.target.value);
                  setPresetOverride(null);
                  setParentVerified(false);
                }}
                className="mt-1 min-h-[42px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm tabular-nums dark:border-[#23344D] dark:bg-[#16243B]"
              />
            </div>
          </div>
        </div>

        {/* Verification & Preview Column */}
        <div className="space-y-4 lg:col-span-6">
          <div className="rounded-3xl border border-[#DCE7E1] bg-white p-5 space-y-4 dark:border-[#23344D] dark:bg-[#111C2E]">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 className="text-sm font-extrabold text-[#122823] dark:text-[#EDF4F7]">
                {reconciled.drugNameFa}
              </h2>
              <span className="tabular-nums text-xs font-extrabold text-[#105B48] dark:text-[#00D7C7]">
                {reconciled.concentrationDisplayFa}
              </span>
            </div>

            {reconciled.mixUpWarningFa ? (
              <SeverityAlert
                severity={reconciled.warningSeverity}
                title="هشدار ایمنی غلظت"
                body={reconciled.mixUpWarningFa}
              />
            ) : null}

            {/* Mandatory Unchecked Verification Checkbox */}
            <label className="flex cursor-pointer items-start gap-3 rounded-2xl border border-[#E8CC99] bg-[#FDF9F1] p-4 text-xs font-bold leading-5 text-[#4B3410] dark:border-[#664F27] dark:bg-[#2B2112] dark:text-[#FCE7C0]">
              <input
                type="checkbox"
                data-testid="scanner-parent-verify-checkbox"
                checked={parentVerified}
                onChange={(e) => setParentVerified(e.target.checked)}
                className="mt-0.5 h-5 w-5 shrink-0 accent-[#105B48]"
              />
              <span>
                من والد کودک با نگاه مستقیم به بطری و جعبه دارو تأیید می‌کنم که نام ماده مؤثره و غلظت ({reconciled.concentrationDisplayFa}) دقیقاً با برچسب واقعی مطابقت دارد.
              </span>
            </label>

            {reconciled.matchedFormulation ? (
              <VisualSyringeCanvas
                formulation={reconciled.matchedFormulation}
                result={reconciled.dosePreview}
                showExactValues={
                  parentVerified &&
                  ageMonths !== null &&
                  Boolean(reconciled.dosePreview && !reconciled.dosePreview.isBlocked)
                }
              />
            ) : (
              <SeverityAlert
                severity="caution"
                title="تطبیق قطعی با جدول کالیبره نشد"
                body="اطمینان حاصل کنید مقدار mg، حجم mL و شکل دارویی (قطره یا شربت) با یکی از فرمولاسیون‌های استاندارد مطابقت دارد یا با داروساز مشورت کنید."
              />
            )}

            <button
              type="button"
              data-testid="scanner-forward-calculator-btn"
              disabled={!parentVerified || !reconciled.matchedFormulation}
              onClick={handleOpenCalculator}
              className="flex min-h-[46px] w-full items-center justify-center gap-2 rounded-xl bg-[#105B48] px-4 text-sm font-bold text-white hover:bg-[#0C493A] disabled:cursor-not-allowed disabled:opacity-45 dark:bg-[#00D7C7] dark:text-[#0B1220]"
            >
              <Calculator className="h-4 w-4" />
              <span>انتقال به محاسبه‌گر دوز و ثبت نوبت</span>
            </button>
          </div>

          {savedSyrupScans.length > 0 ? (
            <section className="rounded-3xl border border-[#DCE7E1] bg-white p-4 space-y-2 dark:border-[#23344D] dark:bg-[#111C2E]">
              <h3 className="text-xs font-extrabold text-[#546963] dark:text-[#9EB3C4]">
                آخرین بطری‌های بررسی‌شده
              </h3>
              {savedSyrupScans.slice(0, 3).map((s) => (
                <div key={s.id} className="flex items-center justify-between text-xs py-1">
                  <span className="font-bold">{s.detectedDrugNameFa}</span>
                  <span className="tabular-nums text-[#546963] dark:text-[#9EB3C4]">
                    {s.mgAmount}mg / {s.perMlAmount}mL
                  </span>
                </div>
              ))}
            </section>
          ) : null}
        </div>
      </section>

      <div className="rounded-2xl border border-[#DCE7E1] bg-[#F5F9F6] p-3 text-xs text-[#546963] flex items-center gap-2 dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#9EB3C4]">
        <ShieldAlert className="h-4 w-4 shrink-0 text-[#C6862E]" />
        <span>هیچ تصویر اسکن‌شده‌ای جایگزین خواندن مستقیم برچسب دارو توسط والد یا داروساز نیست.</span>
      </div>

      <SourceBadge sourceIds={['aap_med_safety', 'applied12_medication']} />
    </div>
  );
};
