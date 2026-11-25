import React from 'react';
import {
  buildWhoCurves,
  type ChildSexId,
  type GrowthMetricId,
  type WhoGrowthAssessment,
} from '../domain/whoGrowthStandardsEngine.ts';
import type { GrowthMeasurementEntry } from '../domain/models.ts';
import { toPersianDigits } from '../domain/persianNormalizer.ts';

interface WhoGrowthChartCardProps {
  metricId: GrowthMetricId;
  sex: ChildSexId;
  assessment: WhoGrowthAssessment | null;
  historyPoints: GrowthMeasurementEntry[];
}

export const WhoGrowthChartCard: React.FC<WhoGrowthChartCardProps> = ({
  metricId,
  sex,
  assessment,
  historyPoints,
}) => {
  const curves = buildWhoCurves(metricId, sex, 0, 60, 2);

  const width = 620;
  const height = 280;
  const padLeft = 46;
  const padRight = 24;
  const padTop = 20;
  const padBottom = 34;
  const plotW = width - padLeft - padRight;
  const plotH = height - padTop - padBottom;

  const allVals = [...curves.p3, ...curves.p97];
  if (assessment) allVals.push(assessment.measuredValue);
  historyPoints
    .filter((h) => h.metricId === metricId && h.sex === sex)
    .forEach((h) => allVals.push(h.measuredValue));

  const minVal = Math.floor(Math.min(...allVals) * 0.92);
  const maxVal = Math.ceil(Math.max(...allVals) * 1.05);
  const valRange = Math.max(1, maxVal - minVal);

  const xForMonth = (m: number) => padLeft + (m / 60) * plotW;
  const yForVal = (v: number) => padTop + plotH - ((v - minVal) / valRange) * plotH;

  const buildPath = (arr: number[]) =>
    arr
      .map((v, idx) => {
        const m = curves.agesMonths[idx];
        return `${idx === 0 ? 'M' : 'L'} ${xForMonth(m).toFixed(1)} ${yForVal(v).toFixed(1)}`;
      })
      .join(' ');

  const areaP15P85 = [
    ...curves.p85.map((v, i) => `${i === 0 ? 'M' : 'L'} ${xForMonth(curves.agesMonths[i]).toFixed(1)} ${yForVal(v).toFixed(1)}`),
    ...[...curves.p15]
      .reverse()
      .map((v, ri) => {
        const i = curves.agesMonths.length - 1 - ri;
        return `L ${xForMonth(curves.agesMonths[i]).toFixed(1)} ${yForVal(v).toFixed(1)}`;
      }),
    'Z',
  ].join(' ');

  const yTicks = [0, 0.25, 0.5, 0.75, 1].map((t) => minVal + t * valRange);
  const xMonthsTicks = [0, 12, 24, 36, 48, 60];

  const matchingHistory = historyPoints.filter((h) => h.metricId === metricId && h.sex === sex);

  return (
    <div
      data-testid="who-growth-chart-card"
      className="rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]"
    >
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <div>
          <h3 className="text-sm font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            منحنی استاندارد سازمان جهانی بهداشت (WHO) — {metricId === 'weight_for_age' ? 'وزن برای سن' : 'قد برای سن'} (
            {sex === 'boy' ? 'پسر' : 'دختر'})
          </h3>
          <p className="text-xs text-[#546963] dark:text-[#9EB3C4]">
            خطوط P3 تا P97 · ناحیه سبز: محدوده میانی P15–P85 · قضاوت رشد بر پایه روند چندنوبته است
          </p>
        </div>
      </div>

      <div className="w-full overflow-x-auto">
        <svg
          viewBox={`0 0 ${width} ${height}`}
          className="w-full min-w-[440px]"
          role="img"
          aria-label="نمودار صدک رشد استاندارد WHO"
        >
          {/* Horizontal gridlines */}
          {yTicks.map((val, idx) => {
            const y = yForVal(val);
            return (
              <g key={idx}>
                <line
                  x1={padLeft}
                  y1={y}
                  x2={width - padRight}
                  y2={y}
                  className="stroke-[#DCE7E1] dark:stroke-[#23344D]"
                  strokeDasharray="3 3"
                />
                <text
                  x={padLeft - 6}
                  y={y + 4}
                  textAnchor="end"
                  className="fill-[#546963] text-[10px] tabular-nums dark:fill-[#9EB3C4]"
                >
                  {val.toFixed(0)}
                </text>
              </g>
            );
          })}

          {/* 24-month transition line for Height-for-Age (Supine -> Standing) */}
          {metricId === 'height_for_age' ? (
            <g>
              <line
                x1={xForMonth(24)}
                y1={padTop}
                x2={xForMonth(24)}
                y2={height - padBottom}
                className="stroke-[#C6862E]/70"
                strokeDasharray="4 4"
                strokeWidth="1.5"
              />
              <text
                x={xForMonth(24)}
                y={padTop + 12}
                textAnchor="middle"
                className="fill-[#9F5F0D] text-[9px] font-bold dark:fill-[#F0B85C]"
              >
                ۲۴ ماه (ایستاده)
              </text>
            </g>
          ) : null}

          {/* P15-P85 Shaded Band */}
          <path d={areaP15P85} className="fill-[#105B48]/12 dark:fill-[#00D7C7]/12" />

          {/* Curves P3, P50, P97 */}
          <path
            d={buildPath(curves.p3)}
            fill="none"
            className="stroke-[#C42B3C]/65 dark:stroke-[#FF6B7D]/70"
            strokeWidth="1.5"
            strokeDasharray="4 3"
          />
          <path
            d={buildPath(curves.p50)}
            fill="none"
            className="stroke-[#105B48] dark:stroke-[#00D7C7]"
            strokeWidth="2.2"
          />
          <path
            d={buildPath(curves.p97)}
            fill="none"
            className="stroke-[#C6862E]/75 dark:stroke-[#F0B85C]/80"
            strokeWidth="1.5"
            strokeDasharray="4 3"
          />

          {/* X-axis months */}
          {xMonthsTicks.map((m) => (
            <text
              key={m}
              x={xForMonth(m)}
              y={height - 10}
              textAnchor="middle"
              className="fill-[#546963] text-[10px] tabular-nums dark:fill-[#9EB3C4]"
            >
              {m}m
            </text>
          ))}

          {/* Saved History Points */}
          {matchingHistory.map((pt) => (
            <circle
              key={pt.id}
              cx={xForMonth(pt.ageMonths)}
              cy={yForVal(pt.measuredValue)}
              r="4.5"
              className="fill-[#546963] stroke-white dark:fill-[#9EB3C4] dark:stroke-[#0B1220]"
              strokeWidth="1.5"
            />
          ))}

          {/* Active Assessment Point */}
          {assessment ? (
            <g>
              <circle
                cx={xForMonth(assessment.ageMonths)}
                cy={yForVal(assessment.measuredValue)}
                r="7"
                className="fill-[#105B48]/25 dark:fill-[#00D7C7]/25"
              />
              <circle
                cx={xForMonth(assessment.ageMonths)}
                cy={yForVal(assessment.measuredValue)}
                r="4.5"
                className="fill-[#105B48] stroke-white dark:fill-[#00D7C7] dark:stroke-[#0B1220]"
                strokeWidth="2"
              />
            </g>
          ) : null}
        </svg>
      </div>

      {assessment ? (
        <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
          <div className="rounded-2xl bg-[#F5F9F6] p-3 dark:bg-[#16243B]">
            <div className="text-xs text-[#546963] dark:text-[#9EB3C4]">محدوده روی نمودار WHO</div>
            <div className="mt-1 text-sm font-extrabold text-[#105B48] dark:text-[#00D7C7]">
              {assessment.bandLabelFa}
            </div>
          </div>
          <div className="rounded-2xl bg-[#F5F9F6] p-3 dark:bg-[#16243B]">
            <div className="text-xs text-[#546963] dark:text-[#9EB3C4]">صدک تقریبی</div>
            <div className="mt-1 tabular-nums text-sm font-extrabold text-[#122823] dark:text-[#EDF4F7]">
              {Math.abs(assessment.zScore) >= 3
                ? 'خارج از محدوده میانی (|Z| ≥ ۳ — نیازمند بررسی پزشک)'
                : `حدود صدک ${toPersianDigits(assessment.percentile.toFixed(0))}`}
            </div>
          </div>
          <div className="rounded-2xl bg-[#F5F9F6] p-3 dark:bg-[#16243B]">
            <div className="text-xs text-[#546963] dark:text-[#9EB3C4]">میانه سن (P50)</div>
            <div className="mt-1 tabular-nums text-sm font-bold text-[#122823] dark:text-[#EDF4F7]">
              {toPersianDigits(assessment.p50.toFixed(1))} {assessment.metric.unitFa}
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
};
