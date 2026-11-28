export interface OnlineFirebaseConfig {
  apiKey: string;
  authDomain: string;
  projectId: string;
  appId: string;
  region: string;
  appCheckSiteKey: string;
}

export type OnlineConfiguration =
  | { enabled: false; reason: 'disabled' | 'incomplete' }
  | { enabled: true; firebase: OnlineFirebaseConfig };

/** These are public Firebase app identifiers, never server credentials. */
export function readOnlineConfiguration(env: Record<string, unknown> = {}): OnlineConfiguration {
  if (env.VITE_MANO_ONLINE_ENABLED !== 'true') return { enabled: false, reason: 'disabled' };
  const text = (key: string) => typeof env[key] === 'string' ? (env[key] as string).trim() : '';
  const projectId = text('VITE_FIREBASE_PROJECT_ID');
  const authDomain = text('VITE_FIREBASE_AUTH_DOMAIN');
  const apiKey = text('VITE_FIREBASE_API_KEY');
  const appId = text('VITE_FIREBASE_APP_ID');
  const region = text('VITE_FIREBASE_FUNCTIONS_REGION') || 'us-central1';
  const appCheckSiteKey = text('VITE_FIREBASE_APPCHECK_SITE_KEY');
  if (!/^[a-z][a-z0-9-]{4,28}[a-z0-9]$/.test(projectId) ||
      authDomain !== `${projectId}.firebaseapp.com` ||
      !/^AIza[\w-]{20,100}$/.test(apiKey) ||
      !/^1:\d+:web:[a-zA-Z0-9]+$/.test(appId) ||
      !/^[a-z]+(?:-[a-z]+)+\d$/.test(region) ||
      !/^[\w-]{20,150}$/.test(appCheckSiteKey)) {
    return { enabled: false, reason: 'incomplete' };
  }
  return { enabled: true, firebase: { apiKey, authDomain, projectId, appId, region, appCheckSiteKey } };
}

export function onlineErrorMessage(error: unknown): string {
  const code = error && typeof error === 'object' && 'code' in error ? String(error.code) : '';
  if (code === 'STALE_SESSION') return 'حساب تغییر کرده است؛ دوباره وارد بخش شوید. نتیجهٔ حساب قبلی نمایش داده نشد.';
  if (code === 'CONSENT_REQUIRED') return 'پیش از ارسال، رضایت روشن برای همین اطلاعات لازم است.';
  if (code === 'EMAIL_NOT_VERIFIED') return 'پیش از ارسال اطلاعات، ایمیل حساب را تأیید و وضعیت را تازه کنید.';
  if (code === 'PASS_EXPIRED') return 'اطلاعات یا زمان بررسی تغییر کرده است؛ بررسی مقدماتی را دوباره انجام دهید.';
  if (code === 'OFFLINE_UNAVAILABLE') return 'خدمات آنلاین در این نسخه در دسترس نیستند؛ اطلاعاتی ارسال نشد.';
  if (code.endsWith('/failed-precondition')) return 'این خدمت فعلاً قابل استفاده نیست یا شرایط لازم کامل نشده است. ارسال دوبارهٔ خودکار انجام نمی‌شود.';
  if (code.endsWith('/resource-exhausted') || code.endsWith('/too-many-requests')) return 'تعداد درخواست‌ها زیاد شده است؛ کمی بعد دوباره تلاش کنید.';
  if (code.endsWith('/permission-denied') || code.endsWith('/unauthenticated')) return 'دسترسی این حساب تأیید نشد؛ دوباره وارد حساب مناسب شوید.';
  if (code.startsWith('auth/')) return 'عملیات حساب انجام نشد. ایمیل، گذرواژه و اتصال را بررسی کنید؛ اطلاعات ورود در این دستگاه ذخیره نمی‌شود.';
  return 'درخواست تأیید نشد. اتصال را بررسی کنید؛ پس از ارسال پرسش، پیش از تلاش مجدد سوابق آنلاین را ببینید.';
}
