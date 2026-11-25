const PERSIAN_DIGITS = ['۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'];

export function toAsciiDigits(input: string): string {
  let out = '';
  for (let i = 0; i < input.length; i++) {
    const code = input.charCodeAt(i);
    if (code >= 0x06f0 && code <= 0x06f9) {
      out += String.fromCharCode(48 + (code - 0x06f0));
    } else if (code >= 0x0660 && code <= 0x0669) {
      out += String.fromCharCode(48 + (code - 0x0660));
    } else {
      out += input[i];
    }
  }
  return out;
}

export function toPersianDigits(input: string | number): string {
  const str = String(input);
  let out = '';
  for (let i = 0; i < str.length; i++) {
    const ch = str[i];
    if (ch >= '0' && ch <= '9') {
      out += PERSIAN_DIGITS[Number(ch)];
    } else if (ch === '.') {
      out += '٫';
    } else {
      out += ch;
    }
  }
  return out;
}

export function normalizePersian(raw: string): string {
  const ascii = toAsciiDigits(raw)
    .replace(/\u200c/g, ' ') // zero-width non-joiner -> space
    .replace(/\u064a/g, 'ی') // Arabic yeh
    .replace(/\u0649/g, 'ی') // alef maksura
    .replace(/\u0643/g, 'ک') // Arabic kaf
    .replace(/\u0629/g, 'ه') // teh marbuta
    .replace(/[\u064b-\u065f\u0670]/g, '') // Arabic diacritics
    .replace(/[\u0622\u0623\u0625]/g, 'ا') // hamza alef variants
    .replace(/[^\p{L}\p{N}\s.٫-]/gu, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .toLowerCase();
  return ascii;
}

export function tokenizeNormalized(raw: string): string[] {
  const norm = normalizePersian(raw);
  if (!norm) return [];
  return norm.split(' ').filter((t) => t.length >= 2);
}

export function scoreMatch(query: string, fields: string[]): number {
  const normQuery = normalizePersian(query);
  if (!normQuery) return 0;
  const queryTokens = tokenizeNormalized(normQuery);
  let score = 0;

  fields.forEach((field, idx) => {
    const normField = normalizePersian(field);
    if (!normField) return;
    const weight = idx === 0 ? 45 : idx === 1 ? 25 : 15;
    if (normField === normQuery) {
      score += weight * 3;
    } else if (normField.includes(normQuery)) {
      score += weight * 2;
    }
    queryTokens.forEach((tok) => {
      if (normField.includes(tok)) {
        score += weight;
      }
    });
  });

  return score;
}
