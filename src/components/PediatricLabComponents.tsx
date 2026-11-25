import React, { useState } from 'react';
import { SlidersHorizontal, Thermometer } from 'lucide-react';
import {
  LIQUID_FORMULATION_LIST,
  evaluateLiquidDose,
  formatAgeFa,
  formatKg,
  type LiquidFormulation,
} from '../domain/liquidDoseVisualizerEngine.ts';
import { toPersianDigits } from '../domain/persianNormalizer.ts';
import { VisualSyringeCanvas } from './VisualSyringeCanvas.tsx';
import { SeverityAlert } from './SeverityAlert.tsx';

export const InteractiveOralSyringeLabCard: React.FC = () => {
  const [formulation, setFormulation] = useState<LiquidFormulation>(LIQUID_FORMULATION_LIST[1]);
  const [ageMonths, setAgeMonths] = useState(18);
  const [weightKg, setWeightKg] = useState(11);
  const [confirmedLabel, setConfirmedLabel] = useState(false);

  const result = evaluateLiquidDose(formulation, ageMonths, weightKg);

  return (
    <section className="rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]">
      <div className="flex items-center gap-2 text-sm font-extrabold text-[#122823] dark:text-[#EDF4F7]">
        <SlidersHorizontal className="h-4 w-4 text-[#105B48] dark:text-[#00D7C7]" />
        <span>آزمایشگاه بصری سرنگ خوراکی و غلظت مایعات</span>
      </div>
      <p className="mt-1 text-xs leading-5 text-[#546963] dark:text-[#9EB3C4]">
        تفاوت چشمگیر حجم در قطره غلیظ و شربت‌ها را روی سرنگ مدرج مقایسه کنید.
      </p>

      <div className="mt-4 flex flex-wrap gap-2">
        {LIQUID_FORMULATION_LIST.map((f) => (
          <button
            key={f.key}
            type="button"
            onClick={() => {
              setFormulation(f);
              setConfirmedLabel(false);
            }}
            className={`min-h-[40px] rounded-xl border px-3 text-xs font-bold transition-colors ${
              f.key === formulation.key
                ? 'border-[#105B48] bg-[#E9F5EB] text-[#105B48] dark:border-[#00D7C7] dark:bg-[#0F333A] dark:text-[#00D7C7]'
                : 'border-[#DCE7E1] bg-[#F5F9F6] text-[#546963] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#9EB3C4]'
            }`}
          >
            {f.shortLabelFa}
          </button>
        ))}
      </div>

      <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div className="rounded-2xl bg-[#F5F9F6] p-3.5 dark:bg-[#16243B]">
          <div className="flex items-center justify-between text-xs font-bold">
            <span>سن تمرینی:</span>
            <span className="tabular-nums text-[#105B48] dark:text-[#00D7C7]">{formatAgeFa(ageMonths)}</span>
          </div>
          <input
            type="range"
            min={0}
            max={96}
            step={1}
            value={ageMonths}
            aria-label="سن تمرینی به ماه"
            onChange={(e) => {
              setAgeMonths(Number(e.target.value));
              setConfirmedLabel(false);
            }}
            className="mt-2 w-full accent-[#105B48]"
          />
        </div>

        <div className="rounded-2xl bg-[#F5F9F6] p-3.5 dark:bg-[#16243B]">
          <div className="flex items-center justify-between text-xs font-bold">
            <span>وزن تمرینی:</span>
            <span className="tabular-nums text-[#105B48] dark:text-[#00D7C7]">{formatKg(weightKg)}</span>
          </div>
          <input
            type="range"
            min={3}
            max={35}
            step={0.5}
            value={weightKg}
            aria-label="وزن تمرینی به کیلوگرم"
            onChange={(e) => {
              setWeightKg(Number(e.target.value));
              setConfirmedLabel(false);
            }}
            className="mt-2 w-full accent-[#105B48]"
          />
        </div>
      </div>

      <label className="mt-4 flex items-center gap-2.5 rounded-2xl border border-[#DCE7E1] bg-[#FAF7F0] p-3 text-xs font-semibold text-[#122823] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#EDF4F7]">
        <input
          type="checkbox"
          checked={confirmedLabel}
          onChange={(e) => setConfirmedLabel(e.target.checked)}
          className="h-5 w-5 accent-[#105B48]"
        />
        <span>
          تأیید می‌کنم غلظت بطری ({formulation.concentrationLabelFa}) را با برچسب واقعی مطابقت داده‌ام تا عدد تمرینی سرنگ نمایش داده شود.
        </span>
      </label>

      <div className="mt-4 space-y-3">
        <VisualSyringeCanvas
          formulation={formulation}
          result={result}
          showExactValues={confirmedLabel}
        />
        {confirmedLabel ? (
          <SeverityAlert severity={result.alertSeverity} title={result.alertTitleFa} body={result.alertBodyFa} />
        ) : null}
      </div>
    </section>
  );
};

export const InteractiveThermometerLabCard: React.FC = () => {
  const [temp, setTemp] = useState(38.1);
  const [under3Months, setUnder3Months] = useState(false);

  const isUrgentNewborn = under3Months && temp >= 38.0;
  const isHighFever = temp >= 39.0;

  return (
    <section className="rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]">
      <div className="flex items-center gap-2 text-sm font-extrabold text-[#122823] dark:text-[#EDF4F7]">
        <Thermometer className="h-4 w-4 text-[#C6862E]" />
        <span>آزمایشگاه خوانش دماسنج و آستانه سن</span>
      </div>

      <div className="mt-3 flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-[#F5F9F6] p-3.5 dark:bg-[#16243B]">
        <div>
          <span className="text-xs text-[#546963] dark:text-[#9EB3C4]">دمای شبیه‌سازی‌شده:</span>
          <div className="tabular-nums text-2xl font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            {toPersianDigits(temp.toFixed(1))}°C
          </div>
        </div>
        <label className="flex items-center gap-2 text-xs font-bold text-[#122823] dark:text-[#EDF4F7]">
          <input
            type="checkbox"
            checked={under3Months}
            onChange={(e) => setUnder3Months(e.target.checked)}
            className="h-5 w-5 accent-[#C42B3C]"
          />
          <span>شیرخوار زیر ۳ ماه</span>
        </label>
      </div>

      <input
        type="range"
        min={35.5}
        max={41.0}
        step={0.1}
        value={temp}
        aria-label="دمای تمرینی"
        onChange={(e) => setTemp(Number(e.target.value))}
        className="mt-3 w-full accent-[#C6862E]"
      />

      <div className="mt-3">
        {isUrgentNewborn ? (
          <SeverityAlert
            severity="danger"
            title="آستانه اورژانس نوزاد زیر ۳ ماه (۳۸٫۰°C یا بالاتر)"
            body="در نوزاد زیر ۳ ماه، عدد ۳۸ درجه سانتی‌گراد یا بالاتر نیازمند ارزیابی فوری حضوری در اورژانس است."
          />
        ) : isHighFever ? (
          <SeverityAlert
            severity="danger"
            title="تب بالا (۳۹٫۰°C یا بیشتر)"
            body="علاوه بر پایش دقیق هوشیاری و تنفس، برای ارزیابی علت تب و معاینه حضوری اقدام کنید."
          />
        ) : temp >= 38.0 ? (
          <SeverityAlert
            severity="caution"
            title="تب خفیف تا متوسط در کودک بالای ۳ ماه"
            body="حال عمومی کودک، شیر خوردن و ادرار مهم‌تر از عدد دماسنج هستند؛ لباس سبک و مایعات فراوان کمک‌کننده است."
          />
        ) : (
          <SeverityAlert
            severity="info"
            title="محدوده دمای طبیعی یا لب‌مرزی"
            body="اگر دماسنج زیر بغل بوده، عدد را همراه زمان ثبت و با توجه به وضعیت عمومی کودک دنبال کنید."
          />
        )}
      </div>
    </section>
  );
};
