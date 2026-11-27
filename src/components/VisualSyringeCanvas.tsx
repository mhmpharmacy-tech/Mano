import React from 'react';
import { Eye, AlertCircle, Droplets } from 'lucide-react';
import type { LiquidDoseVisualizationResult, LiquidFormulation } from '../domain/liquidDoseVisualizerEngine.ts';
import { toPersianDigits } from '../domain/persianNormalizer.ts';

interface VisualSyringeCanvasProps {
  formulation: LiquidFormulation;
  result: LiquidDoseVisualizationResult | null;
  confirmedDoseMl?: number | null;
  showExactValues: boolean;
}

export const VisualSyringeCanvas: React.FC<VisualSyringeCanvasProps> = ({
  formulation,
  result,
  confirmedDoseMl,
  showExactValues,
}) => {
  const maxScale = formulation.maxSyringeScaleMl;
  const displayMl = showExactValues
    ? confirmedDoseMl ?? (result && !result.isBlocked ? result.recommendedMl : 0)
    : 0;
  const clampedMl = Math.min(maxScale, Math.max(0, displayMl));
  const fillRatio = maxScale > 0 ? clampedMl / maxScale : 0;

  const isDrops = formulation.key === 'ACETAMINOPHEN_DROPS_100';
  const majorTickStep = maxScale <= 3 ? 0.5 : maxScale <= 10 ? 2 : 5;
  const majorTicks: number[] = [];
  for (let v = 0; v <= maxScale + 0.001; v += majorTickStep) {
    majorTicks.push(Math.round(v * 10) / 10);
  }

  const barrelX = 64;
  const barrelW = 290;
  const barrelY = 26;
  const barrelH = 56;
  const fillWidth = Math.max(0, Math.min(barrelW, barrelW * fillRatio));

  return (
    <div
      data-testid="visual-syringe-canvas"
      className="rounded-2xl border border-[#DCE7E1] bg-[#F5F9F6] p-4 dark:border-[#23344D] dark:bg-[#16243B]"
    >
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2 text-xs font-bold text-[#105B48] dark:text-[#00D7C7]">
          <Droplets className="h-4 w-4" />
          <span>شبیه‌ساز سرنگ خوراکی مدرج ({formulation.shortLabelFa})</span>
        </div>
        <span className="tabular-nums text-xs font-semibold text-[#546963] dark:text-[#9EB3C4]">
          مقیاس سرنگ: ۰ تا {toPersianDigits(maxScale)} mL
        </span>
      </div>

      <div className="w-full overflow-x-auto">
        <svg
          viewBox="0 0 420 124"
          className="h-28 w-full min-w-[300px]"
          role="img"
          aria-label={`سرنگ مدرج با حجم ${displayMl.toFixed(1)} میلی‌لیتر`}
        >
          {/* Tip / Nozzle on the left (reading LTR inside SVG for clinical barrel alignment) */}
          <rect
            x="24"
            y={barrelY + 18}
            width="40"
            height="20"
            rx="4"
            className="fill-[#DCE7E1] stroke-[#8AA59B] dark:fill-[#243752] dark:stroke-[#557191]"
            strokeWidth="1.5"
          />
          <rect
            x="14"
            y={barrelY + 23}
            width="12"
            height="10"
            rx="2"
            className="fill-[#105B48] dark:fill-[#00D7C7]"
          />

          {/* Outer Glass/Polymer Barrel */}
          <rect
            x={barrelX}
            y={barrelY}
            width={barrelW}
            height={barrelH}
            rx="10"
            className="fill-white stroke-[#8AA59B] dark:fill-[#0B1220] dark:stroke-[#4F6887]"
            strokeWidth="2"
          />

          {/* Liquid Fill inside barrel */}
          {fillWidth > 0 ? (
            <rect
              x={barrelX + 2}
              y={barrelY + 4}
              width={Math.max(4, fillWidth - 2)}
              height={barrelH - 8}
              rx="6"
              className={
                result?.isBlocked
                  ? 'fill-[#C42B3C]/60'
                  : isDrops
                    ? 'fill-[#C6862E]/75 dark:fill-[#F0B85C]/80'
                    : 'fill-[#105B48]/75 dark:fill-[#00D7C7]/75'
              }
            />
          ) : null}

          {/* Plunger Stopper + Rod */}
          <rect
            x={barrelX + fillWidth}
            y={barrelY + 3}
            width="10"
            height={barrelH - 6}
            rx="2"
            className="fill-[#122823] dark:fill-[#D6E6EE]"
          />
          <rect
            x={barrelX + fillWidth + 10}
            y={barrelY + 22}
            width={Math.max(18, 396 - (barrelX + fillWidth + 10))}
            height="12"
            rx="3"
            className="fill-[#B7C9C2] dark:fill-[#334A69]"
          />
          <rect
            x="396"
            y={barrelY + 8}
            width="10"
            height={barrelH - 16}
            rx="3"
            className="fill-[#105B48] dark:fill-[#00D7C7]"
          />

          {/* Tick Marks & Numbers */}
          {majorTicks.map((tick) => {
            const x = barrelX + (tick / maxScale) * barrelW;
            return (
              <g key={tick}>
                <line
                  x1={x}
                  y1={barrelY + barrelH - 12}
                  x2={x}
                  y2={barrelY + barrelH}
                  className="stroke-[#122823] dark:stroke-[#EDF4F7]"
                  strokeWidth="1.6"
                />
                <text
                  x={x}
                  y={barrelY + barrelH + 17}
                  textAnchor="middle"
                  className="fill-[#546963] text-[10px] font-bold dark:fill-[#9EB3C4]"
                >
                  {tick % 1 === 0 ? tick.toFixed(0) : tick.toFixed(1)}
                </text>
              </g>
            );
          })}
        </svg>
      </div>

      <div className="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-3 text-xs">
        <div className="rounded-xl bg-white p-2.5 border border-[#DCE7E1] dark:bg-[#111C2E] dark:border-[#23344D]">
          <div className="text-[#546963] dark:text-[#9EB3C4]">حجم نمایش‌داده‌شده روی سرنگ</div>
          <div className="mt-0.5 tabular-nums text-base font-extrabold text-[#105B48] dark:text-[#00D7C7]">
            {showExactValues ? `${toPersianDigits(displayMl.toFixed(1))} سی‌سی (mL)` : 'منتظر تأیید برچسب'}
          </div>
        </div>

        <div className="rounded-xl bg-white p-2.5 border border-[#DCE7E1] dark:bg-[#111C2E] dark:border-[#23344D]">
          <div className="text-[#546963] dark:text-[#9EB3C4]">واحد نمایش</div>
          <div className="mt-0.5 tabular-nums text-sm font-bold text-[#122823] dark:text-[#EDF4F7]">
            میلی‌لیتر (mL)؛ تعداد قطره محاسبه نمی‌شود
          </div>
        </div>

        <div className="rounded-xl bg-white p-2.5 border border-[#DCE7E1] dark:bg-[#111C2E] dark:border-[#23344D] flex items-center gap-2">
          <Eye className="h-4 w-4 shrink-0 text-[#105B48] dark:text-[#00D7C7]" />
          <span className="leading-5 text-[#546963] dark:text-[#9EB3C4]">
            سرنگ را در سطح چشم بگیرید و خط زیرین منیسک مایع را بخوانید.
          </span>
        </div>
      </div>

      {isDrops ? (
        <div className="mt-2.5 flex items-center gap-2 text-xs font-semibold text-[#9F5F0D] dark:text-[#F5C169]">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>قطره غلیظ ۱۰۰mg/mL فقط با قطره‌چکان اختصاصی یا سرنگ کوچک ۱ تا ۳ سی‌سی کشیده شود.</span>
        </div>
      ) : null}
    </div>
  );
};
