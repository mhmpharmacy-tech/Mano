export const DEFAULT_SUPPORTED_GEMINI_MODELS = [
  'gemini-2.5-flash',
  'gemini-2.5-flash-lite',
  'gemini-2.5-pro',
  'gemini-2.0-flash',
] as const;

export const DEFAULT_GEMINI_MODEL = 'gemini-2.5-flash';

export interface AiGatewayConfig {
  apiKey: string | null;
  model: string;
  modelConfigError: string | null;
  allowedModels: string[];
  timeoutMs: number;
  maxRetries: number;
  retryBaseDelayMs: number;
  rateLimitMaxRequests: number;
  rateLimitWindowMs: number;
  budgetMaxRequests: number;
  budgetMaxChars: number;
  budgetWindowMs: number;
  maxPromptChars: number;
  maxImageBytes: number;
}

function parsePositiveInt(
  raw: string | undefined,
  fallback: number,
  min: number,
  max: number,
): number {
  if (!raw || !raw.trim()) return fallback;
  const parsed = Number.parseInt(raw.trim(), 10);
  if (!Number.isFinite(parsed)) return fallback;
  return Math.min(max, Math.max(min, parsed));
}

function isPlausibleSupportedModelId(modelId: string, allowlist: readonly string[]): boolean {
  return allowlist.includes(modelId);
}

export function resolveAiGatewayConfig(
  env: Record<string, string | undefined> = process.env,
): AiGatewayConfig {
  const apiKey = env.GEMINI_API_KEY?.trim() || null;

  const envAllowlist = (env.GEMINI_ALLOWED_MODELS || '')
    .split(',')
    .map((m) => m.trim())
    .filter(Boolean);

  // Intersect custom env allowlist with verified supported model catalog unless explicitly empty
  const supportedSet = new Set<string>(DEFAULT_SUPPORTED_GEMINI_MODELS);
  const allowedModels =
    envAllowlist.length > 0
      ? envAllowlist.filter((m) => supportedSet.has(m))
      : [...DEFAULT_SUPPORTED_GEMINI_MODELS];

  const effectiveAllowedModels =
    allowedModels.length > 0 ? allowedModels : [DEFAULT_GEMINI_MODEL];

  const requestedModel = env.GEMINI_MODEL?.trim() || DEFAULT_GEMINI_MODEL;
  const modelSupported = isPlausibleSupportedModelId(requestedModel, effectiveAllowedModels);

  return {
    apiKey,
    model: modelSupported ? requestedModel : DEFAULT_GEMINI_MODEL,
    modelConfigError: modelSupported
      ? null
      : `شناسه مدل پیکربندی‌شده («${requestedModel}») در فهرست مدل‌های مجاز و پشتیبانی‌شده سرور نیست.`,
    allowedModels: effectiveAllowedModels,
    timeoutMs: parsePositiveInt(env.GEMINI_TIMEOUT_MS, 25_000, 100, 60_000),
    maxRetries: parsePositiveInt(env.GEMINI_MAX_RETRIES, 2, 0, 4),
    retryBaseDelayMs: parsePositiveInt(env.GEMINI_RETRY_BASE_MS, 250, 10, 5_000),
    rateLimitMaxRequests: parsePositiveInt(env.GEMINI_RATE_LIMIT_MAX, 20, 1, 1_000),
    rateLimitWindowMs: parsePositiveInt(env.GEMINI_RATE_LIMIT_WINDOW_MS, 60_000, 1_000, 3_600_000),
    budgetMaxRequests: parsePositiveInt(env.GEMINI_BUDGET_MAX_REQUESTS, 400, 1, 100_000),
    budgetMaxChars: parsePositiveInt(env.GEMINI_BUDGET_MAX_CHARS, 600_000, 500, 50_000_000),
    budgetWindowMs: parsePositiveInt(env.GEMINI_BUDGET_WINDOW_MS, 86_400_000, 1_000, 604_800_000),
    maxPromptChars: parsePositiveInt(env.GEMINI_MAX_PROMPT_CHARS, 4_000, 200, 20_000),
    maxImageBytes: parsePositiveInt(env.GEMINI_MAX_IMAGE_BYTES, 5 * 1024 * 1024, 1_024, 8 * 1024 * 1024),
  };
}
