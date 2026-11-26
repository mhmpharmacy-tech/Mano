import type { AiProhibitedAction } from '../../src/domain/aiGatewayContracts.ts';

export interface InputSecurityCheckResult {
  safe: boolean;
  sanitizedText: string;
  violations: AiProhibitedAction[];
  reasons: string[];
}

export interface ImageDocumentValidationResult {
  valid: boolean;
  cleanBase64: string;
  mimeType: 'image/jpeg' | 'image/png' | 'image/webp';
  byteLength: number;
  reasonFa?: string;
}

const INJECTION_PATTERNS: Array<{ regex: RegExp; action: AiProhibitedAction; label: string }> = [
  {
    regex: /ignore\s+(all\s+)?(previous|prior|above|system)\s+(instructions?|rules?|prompts?)/i,
    action: 'PROMPT_INJECTION',
    label: 'en_ignore_instructions',
  },
  {
    regex: /(disregard|forget|bypass|override)\s+(all\s+)?(safety|clinical|system|rules?|instructions?|constraints?|urgency|emergency)/i,
    action: 'PROMPT_INJECTION',
    label: 'en_bypass_safety',
  },
  {
    regex: /(you\s+are\s+now|act\s+as|pretend\s+to\s+be|developer\s+mode|jailbreak|dan\s+mode)/i,
    action: 'PROMPT_INJECTION',
    label: 'en_role_hijack',
  },
  {
    regex: /<\s*\/?\s*(system|developer|assistant|untrusted_data|prompt|instruction)\b/i,
    action: 'PROMPT_INJECTION',
    label: 'delimiter_spoofing',
  },
  {
    regex: /دستور(العمل)?\s*(های)?\s*(قبلی|سیستم|ایمنی)\s*را\s*(نادیده|لغو|فراموش|رد)\s*کن/i,
    action: 'PROMPT_INJECTION',
    label: 'fa_ignore_instructions',
  },
  {
    regex: /(قوانین\s*ایمنی|محدودیت\s*ها|پروتکل\s*اورژانس)\s*را\s*(لغو|نادیده|دور\s*بزن|متوقف)/i,
    action: 'OVERRIDE_URGENT_ESCALATION',
    label: 'fa_override_safety',
  },
  {
    regex: /(نوزاد\s*زیر\s*[۳3]\s*ماه.*(طبیعی\s*(بگو|است)|بگو\s*طبیعی|نیازی?\s*به\s*اورژانس\s*نیست|اورژانس\s*(نیاز|لازم)\s*نیست)|اورژانس\s*را\s*(لغو|حذف|مخفی)\s*کن|do\s+not\s+escalate|no\s+emergency\s+needed)/i,
    action: 'OVERRIDE_URGENT_ESCALATION',
    label: 'override_urgent_escalation_request',
  },
  {
    regex: /(تجویز\s*(آنتی‌بیوتیک|آموکسی|دارو|دوز)\s*(بنویس|تعیین\s*کن|صادر\s*کن|انجام\s*بده)|یک\s*دوز\s*(دقیق|جدید)\s*(محاسبه|تعیین|تجویز)\s*کن|prescribe\s+(me\s+)?(an\s+)?(antibiotic|medication|dose)|determine\s+(the\s+)?exact\s+dose)/i,
    action: 'INDEPENDENT_PRESCRIBING_OR_DOSE',
    label: 'prescribing_request',
  },
  {
    regex: /(تشخیص\s*قطعی\s*(پزشکی|بیماری)\s*(بده|بگو|صادر\s*کن)|give\s+(me\s+)?a\s+definitive\s+diagnosis|diagnose\s+(my\s+child|this))/i,
    action: 'INDEPENDENT_DIAGNOSIS',
    label: 'diagnosis_request',
  },
  {
    regex: /(برنامه\s*درمانی\s*را\s*(خودکار|به‌طور\s*خودکار)\s*(تغییر|عوض|حذف|جایگزین|به‌روزرسانی)\s*(کن|بده)|automatically\s+modify\s+(the\s+)?treatment\s+plan)/i,
    action: 'AUTO_MODIFY_TREATMENT_PLAN',
    label: 'auto_modify_treatment_plan_request',
  },
  {
    regex: /(منبع|رفرنس|مقاله)\s*(جعلی|ساختگی)\s*(بساز|اضافه\s*کن)|fabricate\s+(a\s+)?(reference|citation|doi|study)/i,
    action: 'FABRICATE_REFERENCES',
    label: 'fabricate_reference_request',
  },
];

function stripControlChars(raw: string): string {
  let out = '';
  for (let i = 0; i < raw.length; i += 1) {
    const code = raw.charCodeAt(i);
    const isDisallowedControl =
      (code >= 0 && code <= 8) ||
      code === 11 ||
      code === 12 ||
      (code >= 14 && code <= 31) ||
      code === 127;
    out += isDisallowedControl ? ' ' : raw[i];
  }
  return out;
}

export function sanitizeUntrustedText(raw: unknown, maxLength: number): string {
  if (typeof raw !== 'string') return '';
  return stripControlChars(raw).trim().slice(0, maxLength);
}

export function inspectUntrustedTextInput(
  raw: unknown,
  maxLength: number,
): InputSecurityCheckResult {
  const sanitizedText = sanitizeUntrustedText(raw, maxLength);
  if (!sanitizedText) {
    return { safe: true, sanitizedText: '', violations: [], reasons: [] };
  }

  const violations: AiProhibitedAction[] = [];
  const reasons: string[] = [];

  for (const rule of INJECTION_PATTERNS) {
    if (rule.regex.test(sanitizedText)) {
      violations.push(rule.action);
      reasons.push(rule.label);
    }
  }

  return {
    safe: violations.length === 0,
    sanitizedText,
    violations: [...new Set(violations)],
    reasons,
  };
}

export function wrapUntrustedBlock(sourceLabel: string, value: string): string {
  const escaped = value.replace(/[<>]/g, (ch) => (ch === '<' ? '‹' : '›'));
  return `<untrusted_data source="${sourceLabel}">\n${escaped}\n</untrusted_data>`;
}

const ALLOWED_IMAGE_MIMES = new Set(['image/jpeg', 'image/png', 'image/webp']);

function detectImageMagicMime(buf: Uint8Array): 'image/jpeg' | 'image/png' | 'image/webp' | null {
  if (buf.length < 12) return null;
  // JPEG: FF D8 FF
  if (buf[0] === 0xff && buf[1] === 0xd8 && buf[2] === 0xff) {
    return 'image/jpeg';
  }
  // PNG: 89 50 4E 47 0D 0A 1A 0A
  if (
    buf[0] === 0x89 &&
    buf[1] === 0x50 &&
    buf[2] === 0x4e &&
    buf[3] === 0x47 &&
    buf[4] === 0x0d &&
    buf[5] === 0x0a &&
    buf[6] === 0x1a &&
    buf[7] === 0x0a
  ) {
    return 'image/png';
  }
  // WEBP: RIFF....WEBP
  if (
    buf[0] === 0x52 &&
    buf[1] === 0x49 &&
    buf[2] === 0x46 &&
    buf[3] === 0x46 &&
    buf[8] === 0x57 &&
    buf[9] === 0x45 &&
    buf[10] === 0x42 &&
    buf[11] === 0x50
  ) {
    return 'image/webp';
  }
  return null;
}

export function validateUntrustedImageDocument(
  rawBase64: unknown,
  declaredMime: unknown,
  maxBytes: number,
): ImageDocumentValidationResult {
  if (typeof rawBase64 !== 'string' || rawBase64.trim().length < 24) {
    return {
      valid: false,
      cleanBase64: '',
      mimeType: 'image/jpeg',
      byteLength: 0,
      reasonFa: 'تصویر برچسب دارو ارسال نشده یا نامعتبر است.',
    };
  }

  let dataPart = rawBase64.trim();
  let headerMime: string | null = null;
  if (dataPart.startsWith('data:')) {
    const commaIdx = dataPart.indexOf(',');
    if (commaIdx === -1) {
      return {
        valid: false,
        cleanBase64: '',
        mimeType: 'image/jpeg',
        byteLength: 0,
        reasonFa: 'فرمت داده تصویر نامعتبر است.',
      };
    }
    const header = dataPart.slice(5, commaIdx);
    headerMime = header.split(';')[0]?.trim().toLowerCase() || null;
    dataPart = dataPart.slice(commaIdx + 1).replace(/\s+/g, '');
  } else {
    dataPart = dataPart.replace(/\s+/g, '');
  }

  const normalizedDeclared =
    typeof declaredMime === 'string' ? declaredMime.trim().toLowerCase() : null;
  const candidateMime = headerMime || normalizedDeclared || 'image/jpeg';

  if (!ALLOWED_IMAGE_MIMES.has(candidateMime)) {
    return {
      valid: false,
      cleanBase64: '',
      mimeType: 'image/jpeg',
      byteLength: 0,
      reasonFa: 'فقط تصاویر با فرمت استاندارد JPEG، PNG یا WEBP مجاز هستند.',
    };
  }

  if (!/^[A-Za-z0-9+/]+={0,2}$/.test(dataPart)) {
    return {
      valid: false,
      cleanBase64: '',
      mimeType: 'image/jpeg',
      byteLength: 0,
      reasonFa: 'محتوای کدگذاری‌شده تصویر معتبر نیست.',
    };
  }

  const decoded = Buffer.from(dataPart, 'base64');
  if (decoded.byteLength < 12 || decoded.byteLength > maxBytes) {
    return {
      valid: false,
      cleanBase64: '',
      mimeType: 'image/jpeg',
      byteLength: decoded.byteLength,
      reasonFa: `حجم تصویر باید بین ۱۲ بایت تا ${Math.round(maxBytes / (1024 * 1024))} مگابایت باشد.`,
    };
  }

  const magicMime = detectImageMagicMime(decoded);
  if (!magicMime) {
    return {
      valid: false,
      cleanBase64: '',
      mimeType: 'image/jpeg',
      byteLength: decoded.byteLength,
      reasonFa: 'فایل ارسالی یک تصویر معتبر (JPEG/PNG/WEBP) نیست.',
    };
  }

  return {
    valid: true,
    cleanBase64: dataPart,
    mimeType: magicMime,
    byteLength: decoded.byteLength,
  };
}
