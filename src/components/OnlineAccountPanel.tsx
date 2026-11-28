import React, { useEffect, useRef, useState } from 'react';
import { useOnline } from '../online/context.ts';
import { onlineErrorMessage } from '../online/config.ts';
import {
  createDeletionReceiptToken, deletionJobId, parseDeletionReceipt,
  type DeletionPolicy, type DeletionReceipt, type DeletionSummary,
} from '../online/contracts.ts';
import { parseDeletionPolicy, parseDeletionSummary } from '../online/responses.ts';

const inputClass = 'mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 py-2 text-sm dark:border-[#23344D] dark:bg-[#16243B]';
const buttonClass = 'min-h-[44px] rounded-xl border border-[#DCE7E1] px-3 py-2 text-sm font-bold disabled:opacity-50 dark:border-[#23344D]';

function downloadReceipt(receipt: DeletionReceipt) {
  const url = URL.createObjectURL(new Blob([JSON.stringify(receipt, null, 2)], { type: 'application/json' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = 'mano-account-deletion-receipt.json';
  link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

const STATUS_FA: Record<DeletionSummary['status'], string> = {
  PENDING: 'درخواست ثبت شده و منتظر پردازش است',
  IN_PROGRESS: 'حذف در حال انجام است',
  PARTIAL_FAILURE: 'حذف کامل نشده است؛ پیگیری و تلاش مجدد لازم است',
  COMPLETED_WITH_RETENTION: 'پردازش حذف حساب پایان یافته؛ بخشی از سوابق طبق سیاست نگهداری باقی می‌ماند',
};

const AccountForm: React.FC = () => {
  const { controller, session } = useOnline();
  const alive = useRef(true);
  useEffect(() => { alive.current = true; return () => { alive.current = false; }; }, []);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [repeat, setRepeat] = useState('');
  const [create, setCreate] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [policy, setPolicy] = useState<DeletionPolicy | null>(null);
  const [confirmation, setConfirmation] = useState('');
  const [receipt, setReceipt] = useState<DeletionReceipt | null>(null);
  const [receiptSaved, setReceiptSaved] = useState(false);
  const [retentionAccepted, setRetentionAccepted] = useState(false);
  const [summary, setSummary] = useState<DeletionSummary | null>(null);
  const generation = session.generation;
  const isCurrent = () => alive.current && controller.getSession().generation === generation;
  const perform = async (operation: () => Promise<void>) => {
    setBusy(true); setError(''); setNotice('');
    try { await operation(); }
    catch (err) { if (isCurrent()) setError(onlineErrorMessage(err)); }
    finally { if (isCurrent()) { setBusy(false); setPassword(''); setRepeat(''); } }
  };

  return (
    <div className="space-y-4">
      <p className="text-xs leading-6 text-[#546963] dark:text-[#9EB3C4]">ورود، اطلاعات محلی کودک را به حساب یا سرور منتقل نمی‌کند. اطلاعات ورود در حافظهٔ همین تب است و با بستن آن باقی نمی‌ماند. در دستگاه مشترک، پیش از واگذاری دستگاه از حساب خارج شوید و اطلاعات محلی را جداگانه پاک کنید.</p>
      {session.user ? (
        <div className="space-y-3">
          <p className="break-words text-sm" dir="ltr">{session.user.email}</p>
          <p className="text-xs">{session.user.emailVerified ? 'ایمیل تأیید شده است.' : 'ارسال اطلاعات تا تأیید ایمیل بسته است.'}</p>
          <div className="flex flex-wrap gap-2">
            {!session.user.emailVerified && <button type="button" className={buttonClass} disabled={busy} onClick={() => void perform(async () => { await controller.sendVerification(); if (isCurrent()) setNotice('درخواست ایمیل تأیید ارسال شد؛ صندوق ورودی و پوشهٔ هرزنامه را بررسی کنید.'); })}>ارسال ایمیل تأیید</button>}
            <button type="button" className={buttonClass} disabled={busy} onClick={() => void perform(async () => { await controller.refreshUser(); if (isCurrent()) setNotice('وضعیت حساب تازه شد.'); })}>تازه‌کردن وضعیت حساب</button>
            <button type="button" className={buttonClass} disabled={busy} onClick={() => void perform(() => controller.signOut())} data-testid="online-signout-btn">خروج از حساب آنلاین</button>
          </div>
          <details className="rounded-2xl border border-[#E7B5BC] p-3 dark:border-[#6A2C3B]">
            <summary className="min-h-[44px] cursor-pointer text-sm font-bold">درخواست حذف حساب آنلاین</summary>
            <div className="space-y-3 pt-3">
              <p className="text-xs leading-6">حذف حساب آنلاین با پاک‌کردن همین مرورگر فرق دارد. ابتدا سیاست نگهداری و امکان حذف را بررسی کنید. سوابق حرفه‌ای ممکن است تا زمان مشخصی نگه داشته شوند؛ حذف حساب، وعدهٔ پاک‌شدن فوری تمام نسخه‌های پشتیبان نیست.</p>
              <button type="button" className={buttonClass} disabled={busy} onClick={() => void perform(async () => { const next = parseDeletionPolicy(await controller.call<unknown>('getAccountDeletionPolicy', {})); if (isCurrent()) { setPolicy(next); setReceipt(null); setReceiptSaved(false); setRetentionAccepted(false); } })}>بررسی شرایط حذف حساب</button>
              {policy && !policy.available && <p role="status" className="text-xs leading-6">حذف خودکار حساب برای این خدمت هنوز فعال نیست. پاک‌کردن داده‌های محلی، حساب یا سوابق ابری را حذف نمی‌کند.</p>}
              {policy?.available && policy.policyVersion && <>
                <p className="text-xs leading-6">سیاست {policy.policyVersion}: نگهداری سوابق حرفه‌ای تا {policy.clinicalRetentionDays?.toLocaleString('fa-IR')} روز و نسخه‌های پشتیبان تا {policy.backupRetentionDays?.toLocaleString('fa-IR')} روز. رسید خصوصی پیگیری تا {policy.receiptDays.toLocaleString('fa-IR')} روز قابل استفاده است. حساب کارکنان به بررسی جداگانه نیاز دارد.</p>
                <button type="button" className={buttonClass} disabled={busy} onClick={() => void perform(async () => {
                  const receiptToken = receipt?.receiptToken ?? createDeletionReceiptToken();
                  const next: DeletionReceipt = { format: 'mano-account-deletion', version: 1, jobId: await deletionJobId(receiptToken), receiptToken, policyVersion: policy.policyVersion! };
                  if (!isCurrent()) return;
                  downloadReceipt(next); setReceipt(next); setReceiptSaved(false);
                })}>دانلود رسید خصوصی پیش از درخواست</button>
                <p className="text-xs leading-6">رسید، کلید پیگیری حذف است. آن را خصوصی نگه دارید و در پیام عمومی، نشانی وب یا تصویر منتشر نکنید. در صورت قطع ارتباط، از همین فایل برای پیگیری استفاده کنید.</p>
                <label className="flex items-start gap-2 text-xs leading-6"><input type="checkbox" checked={receiptSaved} disabled={!receipt} onChange={e => setReceiptSaved(e.target.checked)} className="mt-1 h-5 w-5 shrink-0" />رسید را ذخیره کرده‌ام و پس از خروج نیز به آن دسترسی دارم.</label>
                <label className="flex items-start gap-2 text-xs leading-6"><input type="checkbox" checked={retentionAccepted} onChange={e => setRetentionAccepted(e.target.checked)} className="mt-1 h-5 w-5 shrink-0" />از باقی‌ماندن سوابق مشمول سیاست نگهداری و جدا بودن داده‌های این مرورگر آگاه هستم.</label>
                <label className="block text-xs font-bold">برای تأیید، عبارت DELETE_MY_ACCOUNT را وارد کنید<input className={inputClass} value={confirmation} onChange={e => setConfirmation(e.target.value)} dir="ltr" autoComplete="off" /></label>
                <label className="block text-xs font-bold">گذرواژه برای تأیید دوبارهٔ هویت<input className={inputClass} type="password" autoComplete="current-password" value={password} onChange={e => setPassword(e.target.value)} /></label>
                <button type="button" className={`${buttonClass} border-[#B32334] text-[#B32334]`} disabled={busy || !receipt || !receiptSaved || !retentionAccepted || confirmation !== 'DELETE_MY_ACCOUNT' || !password} onClick={() => void perform(async () => {
                  if (!receipt || !policy.policyVersion) return;
                  await controller.reauthenticate(password);
                  if (!isCurrent()) return;
                  const result = parseDeletionSummary(await controller.call<unknown>('requestAccountDeletion', { confirmation: 'DELETE_MY_ACCOUNT', policyVersion: policy.policyVersion, receiptToken: receipt.receiptToken }), receipt.jobId);
                  if (isCurrent()) { setSummary(result); setNotice('درخواست ثبت شد؛ نتیجهٔ نهایی را با رسید پیگیری کنید.'); }
                  // Deleted/disabled Firebase users must not attach a stale ID token
                  // to a later receipt-only status request.
                  await controller.signOut();
                })}>ثبت درخواست حذف حساب و خروج</button>
              </>}
            </div>
          </details>
        </div>
      ) : (
        <form className="space-y-3" onSubmit={event => { event.preventDefault(); void perform(async () => {
          if (create && (password.length < 12 || password !== repeat)) { setError('برای حساب جدید، گذرواژهٔ دست‌کم ۱۲ نویسه و تکرار یکسان لازم است.'); return; }
          await controller.signIn(email, password, create);
        }); }}>
          <label className="block text-xs font-bold">ایمیل<input type="email" required autoComplete="username" dir="ltr" value={email} onChange={e => setEmail(e.target.value)} className={inputClass} data-testid="online-email-input" /></label>
          <label className="block text-xs font-bold">گذرواژه<input type="password" required minLength={create ? 12 : 1} maxLength={256} autoComplete={create ? 'new-password' : 'current-password'} value={password} onChange={e => setPassword(e.target.value)} className={inputClass} data-testid="online-password-input" /></label>
          <label className="flex min-h-[44px] items-center gap-2 text-xs"><input type="checkbox" checked={create} onChange={e => { setCreate(e.target.checked); setPassword(''); setRepeat(''); }} className="h-5 w-5" />ساخت حساب جدید</label>
          {create && <label className="block text-xs font-bold">تکرار گذرواژه<input type="password" required minLength={12} maxLength={256} autoComplete="new-password" value={repeat} onChange={e => setRepeat(e.target.value)} className={inputClass} /></label>}
          <div className="flex flex-wrap gap-2">
            <button type="submit" className={buttonClass} disabled={busy || session.pending}>{busy ? 'در حال بررسی…' : create ? 'ساخت حساب' : 'ورود به حساب'}</button>
            <button type="button" className={buttonClass} disabled={busy || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)} onClick={() => void perform(async () => { await controller.resetPassword(email); if (isCurrent()) setNotice('اگر حساب واجد شرایطی با این ایمیل وجود داشته باشد، راهنمای بازیابی برای آن ارسال می‌شود.'); })}>بازیابی گذرواژه</button>
          </div>
        </form>
      )}
      <details className="rounded-2xl border border-[#DCE7E1] p-3 dark:border-[#23344D]">
        <summary className="min-h-[44px] cursor-pointer text-sm font-bold">پیگیری حذف با رسید خصوصی</summary>
        <label className="block pt-2 text-xs leading-6">فایل رسید ذخیره‌شده را انتخاب کنید؛ از این فایل فقط برای خواندن وضعیت همان درخواست استفاده می‌شود. اگر حساب پیش‌تر حذف یا غیرفعال شده ولی هنوز وارد آن هستید، ابتدا از حساب خارج شوید.
          <input type="file" accept="application/json,.json" disabled={busy} className="mt-2 block max-w-full text-xs" onChange={event => {
            const file = event.target.files?.[0]; event.target.value = '';
            if (!file) return;
            void perform(async () => {
              if (file.size > 2048) { setError('فایل رسید معتبر نیست.'); return; }
              const parsed = parseDeletionReceipt(await file.text());
              if (!isCurrent()) return;
              const expectedJobId = await deletionJobId(parsed.receiptToken);
              if (!isCurrent()) return;
              if (parsed.jobId !== expectedJobId) { setError('فایل رسید معتبر نیست.'); return; }
              const result = parseDeletionSummary(await controller.call<unknown>('getAccountDeletionStatus', { jobId: parsed.jobId, receiptToken: parsed.receiptToken }, { anonymousReceipt: true, expectedGeneration: generation }), parsed.jobId);
              if (isCurrent()) setSummary(result);
            });
          }} />
        </label>
      </details>
      {summary && <div role="status" className="space-y-2 rounded-2xl bg-[#F5F9F6] p-3 text-xs leading-6 dark:bg-[#16243B]">
        <p>{STATUS_FA[summary.status] ?? 'وضعیت نامشخص؛ دوباره پیگیری کنید.'}</p>
        {summary.retainedScopes.includes('clinical-case-and-audit-records') && <p>مهلت ثبت‌شده برای نگهداری سوابق حرفه‌ای: {new Date(summary.retainedUntilMs).toLocaleDateString('fa-IR')}.</p>}
        {summary.retainedScopes.includes('managed-backups-declared-policy-not-verified-by-this-job') && <p>مهلت اعلام‌شدهٔ نسخه‌های پشتیبان: {new Date(summary.backupRetentionUntilMs).toLocaleDateString('fa-IR')}. این رسید، پاک‌سازی فیزیکی نسخه‌های پشتیبان را تأیید نمی‌کند.</p>}
        <p>اطلاعات این مرورگر و فایل‌های دانلودشده با حذف حساب آنلاین پاک نمی‌شوند.</p>
        {summary.failureCode && <p>پردازش به پیگیری نیاز دارد؛ موفقیت کامل اعلام نشده است.</p>}
      </div>}
      {error && <p role="alert" className="text-xs leading-6 text-[#B32334] dark:text-[#FF8594]">{error}</p>}
      {notice && <p role="status" className="text-xs leading-6">{notice}</p>}
    </div>
  );
};

export const OnlineAccountPanel: React.FC = () => {
  const { enabled, session } = useOnline();
  return <section className="space-y-3 rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]" data-testid="online-account-panel">
    <h2 className="text-base font-extrabold">حساب آنلاین</h2>
    {enabled ? <AccountForm key={session.generation} /> : <p role="note" className="text-xs leading-6" data-testid="online-unavailable-note">خدمات حساب و ارسال آنلاین در این نسخه فعال نیستند. ابزارها و یادداشت‌های محلی بدون حساب قابل مرورند؛ ثبت اطلاعات واقعی بیمار در این پیش‌نمایش مجاز نیست.</p>}
  </section>;
};
