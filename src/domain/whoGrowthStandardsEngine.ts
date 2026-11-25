import {
  WHO_HEIGHT_BOYS,
  WHO_HEIGHT_GIRLS,
  WHO_LENGTH_BOYS,
  WHO_LENGTH_GIRLS,
  WHO_WEIGHT_BOYS,
  WHO_WEIGHT_GIRLS,
  type WhoLmsPoint,
} from './whoReferenceData.ts';

export type GrowthMetricId = 'weight_for_age' | 'height_for_age';
export type ChildSexId = 'boy' | 'girl';

export interface GrowthMetricConfig {
  id: GrowthMetricId;
  labelFa: string;
  shortLabelFa: string;
  unitFa: string;
  minValid: number;
  maxValid: number;
}

export const GROWTH_METRICS: Record<GrowthMetricId, GrowthMetricConfig> = {
  weight_for_age: {
    id: 'weight_for_age',
    labelFa: 'وزن نسبت به سن',
    shortLabelFa: 'وزن (kg)',
    unitFa: 'kg',
    minValid: 1.0,
    maxValid: 40.0,
  },
  height_for_age: {
    id: 'height_for_age',
    labelFa: 'قد نسبت به سن',
    shortLabelFa: 'قد (cm)',
    unitFa: 'cm',
    minValid: 35.0,
    maxValid: 140.0,
  },
};

export interface WhoPercentileCurves {
  agesMonths: number[];
  p3: number[];
  p15: number[];
  p50: number[];
  p85: number[];
  p97: number[];
}

export interface WhoGrowthAssessment {
  metric: GrowthMetricConfig;
  sex: ChildSexId;
  ageMonths: number;
  measuredValue: number;
  zScore: number;
  percentile: number;
  p3: number;
  p15: number;
  p50: number;
  p85: number;
  p97: number;
  bandLabelFa: string;
  guidanceFa: string;
  needsPediatricReview: boolean;
}

const Z_P3 = -1.8807936;
const Z_P15 = -1.0364334;
const Z_P50 = 0.0;
const Z_P85 = 1.0364334;
const Z_P97 = 1.8807936;

const HEIGHT_BOYS_LMS: WhoLmsPoint[] = [
  ...WHO_LENGTH_BOYS.filter((p) => p.ageMonths < 24),
  ...WHO_HEIGHT_BOYS,
];
const HEIGHT_GIRLS_LMS: WhoLmsPoint[] = [
  ...WHO_LENGTH_GIRLS.filter((p) => p.ageMonths < 24),
  ...WHO_HEIGHT_GIRLS,
];

export function lmsTableFor(metricId: GrowthMetricId, sex: ChildSexId): WhoLmsPoint[] {
  if (metricId === 'weight_for_age') {
    return sex === 'boy' ? WHO_WEIGHT_BOYS : WHO_WEIGHT_GIRLS;
  }
  return sex === 'boy' ? HEIGHT_BOYS_LMS : HEIGHT_GIRLS_LMS;
}

export function interpolateLms(
  metricId: GrowthMetricId,
  sex: ChildSexId,
  ageMonths: number,
): WhoLmsPoint {
  if (!Number.isFinite(ageMonths) || ageMonths < 0 || ageMonths > 60) {
    throw new Error('Age must be finite and within 0–60 months');
  }
  // Never interpolate across the 0.7 cm switch from supine length to standing height at 24 months
  const table =
    metricId === 'height_for_age' && ageMonths < 24
      ? sex === 'boy'
        ? WHO_LENGTH_BOYS
        : WHO_LENGTH_GIRLS
      : lmsTableFor(metricId, sex);

  const exact = table.find((p) => Math.abs(p.ageMonths - ageMonths) < 1e-6);
  if (exact) return exact;

  const upperIndex = table.findIndex((p) => p.ageMonths > ageMonths);
  if (upperIndex <= 0) return table[0];
  const a = table[upperIndex - 1];
  const b = table[upperIndex];
  const ratio = (ageMonths - a.ageMonths) / (b.ageMonths - a.ageMonths);
  return {
    ageMonths,
    l: a.l + (b.l - a.l) * ratio,
    m: a.m + (b.m - a.m) * ratio,
    s: a.s + (b.s - a.s) * ratio,
  };
}

export function valueForZ(lms: WhoLmsPoint, z: number): number {
  if (Math.abs(lms.l) < 1e-5) {
    return lms.m * Math.exp(lms.s * z);
  }
  const inner = Math.max(0.05, 1.0 + lms.l * lms.s * z);
  return lms.m * Math.pow(inner, 1.0 / lms.l);
}

export function zScoreForValue(lms: WhoLmsPoint, measuredValue: number): number {
  if (measuredValue <= 0) return -4.0;
  if (Math.abs(lms.l) < 1e-5) {
    return Math.log(measuredValue / lms.m) / lms.s;
  }
  return (Math.pow(measuredValue / lms.m, lms.l) - 1.0) / (lms.l * lms.s);
}

export function zScoreToPercentile(z: number): number {
  const clamped = Math.min(4.0, Math.max(-4.0, z));
  const x = Math.abs(clamped);
  const t = 1.0 / (1.0 + 0.2316419 * x);
  const poly =
    ((((1.330274429 * t - 1.821255978) * t + 1.781477937) * t - 0.356563782) * t + 0.31938153) * t;
  const pdf = 0.3989422804014327 * Math.exp(-0.5 * x * x);
  const cdfPos = 1.0 - pdf * poly;
  const cdf = clamped >= 0 ? cdfPos : 1.0 - cdfPos;
  return Math.min(99.9, Math.max(0.1, cdf * 100.0));
}

export function buildWhoCurves(
  metricId: GrowthMetricId,
  sex: ChildSexId,
  minMonth = 0,
  maxMonth = 60,
  stepMonths = 2,
): WhoPercentileCurves {
  const ages: number[] = [];
  let cur = Math.min(60, Math.max(0, minMonth));
  const end = Math.min(60, Math.max(cur, maxMonth));
  while (cur <= end + 1e-6) {
    ages.push(Math.min(60, Number(cur.toFixed(4))));
    cur += stepMonths;
  }
  if (ages[ages.length - 1] !== end) {
    ages.push(end);
  }

  const p3: number[] = [];
  const p15: number[] = [];
  const p50: number[] = [];
  const p85: number[] = [];
  const p97: number[] = [];

  ages.forEach((month) => {
    const lms = interpolateLms(metricId, sex, month);
    p3.push(valueForZ(lms, Z_P3));
    p15.push(valueForZ(lms, Z_P15));
    p50.push(valueForZ(lms, Z_P50));
    p85.push(valueForZ(lms, Z_P85));
    p97.push(valueForZ(lms, Z_P97));
  });

  return { agesMonths: ages, p3, p15, p50, p85, p97 };
}

export function evaluateWhoGrowth(
  metricId: GrowthMetricId,
  sex: ChildSexId,
  ageMonths: number,
  measuredValue: number,
): WhoGrowthAssessment | null {
  const metric = GROWTH_METRICS[metricId];
  if (!Number.isFinite(ageMonths) || ageMonths < 0 || ageMonths > 60) return null;
  if (
    !Number.isFinite(measuredValue) ||
    measuredValue < metric.minValid ||
    measuredValue > metric.maxValid
  ) {
    return null;
  }

  const lms = interpolateLms(metricId, sex, ageMonths);
  const z = zScoreForValue(lms, measuredValue);
  const percentile = zScoreToPercentile(z);
  const p3 = valueForZ(lms, Z_P3);
  const p15 = valueForZ(lms, Z_P15);
  const p50 = valueForZ(lms, Z_P50);
  const p85 = valueForZ(lms, Z_P85);
  const p97 = valueForZ(lms, Z_P97);

  let bandLabelFa: string;
  let guidanceFa: string;
  let needsPediatricReview = false;

  if (percentile < 3.0) {
    bandLabelFa = 'زیر صدک ۳ (P3)';
    guidanceFa =
      'این عدد پایین‌تر از صدک ۳ جدول WHO است. یک اندازه‌گیری به تنهایی تشخیص محسوب نمی‌شود؛ روند وزن‌گیری/قد در نوبت‌های قبلی را با پزشک اطفال یا مراقب سلامت بررسی کنید.';
    needsPediatricReview = true;
  } else if (percentile < 15.0) {
    bandLabelFa = 'بازه صدک ۳ تا ۱۵ (P3–P15)';
    guidanceFa =
      'در محدوده میانی-پایین استاندارد WHO قرار دارد. حفظ پیوستگی منحنی فردی کودک در طول زمان مهم‌تر از عدد تکی است.';
  } else if (percentile <= 85.0) {
    bandLabelFa = 'محدوده میانی استاندارد (P15–P85)';
    guidanceFa =
      'نقطه بین خطوط صدک ۱۵ و ۸۵ است. این نتیجه سلامت رشد را تأیید نمی‌کند؛ افت روند یا نگرانی والد حتی در این بازه نیاز به بررسی دارد.';
  } else if (percentile <= 97.0) {
    bandLabelFa = 'بازه صدک ۸۵ تا ۹۷ (P85–P97)';
    guidanceFa =
      'این اندازه‌گیری بین خطوط صدک ۸۵ و ۹۷ است؛ به‌تنهایی نشانهٔ سلامت یا بیماری نیست. وزن نسبت به سن برای تشخیص چاقی کافی نیست.';
  } else {
    bandLabelFa = 'بالای صدک ۹۷ (P97)';
    guidanceFa =
      'این مقدار بالاتر از صدک ۹۷ جدول مرجع WHO است. برای بررسی دقیق روند رشد و تغذیه با پزشک اطفال مشورت کنید.';
    needsPediatricReview = true;
  }

  return {
    metric,
    sex,
    ageMonths,
    measuredValue,
    zScore: Math.round(z * 100) / 100,
    percentile: Math.round(percentile * 10) / 10,
    p3,
    p15,
    p50,
    p85,
    p97,
    bandLabelFa,
    guidanceFa,
    needsPediatricReview,
  };
}
