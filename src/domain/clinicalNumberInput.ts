import { toAsciiDigits } from './persianNormalizer.ts';

export function parseClinicalDecimal(raw: string): number | null {
  if (!raw || !raw.trim()) return null;
  const ascii = toAsciiDigits(raw)
    .replace(/٫/g, '.')
    .replace(/,/g, '.')
    .replace(/\s+/g, '')
    .trim();
  if (!/^-?\d+(?:\.\d+)?$/.test(ascii)) return null;
  const num = Number(ascii);
  return Number.isFinite(num) ? num : null;
}

export function parseClinicalInteger(raw: string): number | null {
  if (!raw || !raw.trim()) return null;
  const ascii = toAsciiDigits(raw).replace(/\s+/g, '').trim();
  if (!/^-?\d+$/.test(ascii)) return null;
  const num = Number.parseInt(ascii, 10);
  return Number.isFinite(num) ? num : null;
}
