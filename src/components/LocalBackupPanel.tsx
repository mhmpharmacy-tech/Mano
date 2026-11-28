import React, { useEffect, useRef, useState } from 'react';
import { Download, FolderOpen, LockKeyhole, X } from 'lucide-react';
import { useMano } from '../state/ManoStore.tsx';
import { LocalBackupError, MAX_LOCAL_BACKUP_FILE_BYTES, openEncryptedLocalBackup, type LocalBackupSnapshot } from '../domain/localBackup.ts';
import { AccessibleDialog } from './AccessibleDialog.tsx';

const fieldClass = 'mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 py-2 text-sm dark:border-[#23344D] dark:bg-[#16243B]';
const actionClass = 'min-h-[44px] rounded-xl bg-[#105B48] px-4 py-2 text-sm font-bold text-white disabled:opacity-50 dark:bg-[#00D7C7] dark:text-[#0B1220]';

export const LocalBackupPanel: React.FC = () => {
  const [mode, setMode] = useState<'export' | 'import' | null>(null);
  return (
    <section data-testid="local-backup-panel" aria-labelledby="local-backup-title" className="space-y-3 rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]">
      <h2 id="local-backup-title" className="flex items-center gap-2 text-base font-extrabold"><LockKeyhole aria-hidden="true" className="h-5 w-5" />پشتیبان رمزدار و بازیابی</h2>
      <p className="text-sm leading-7 text-[#546963] dark:text-[#9EB3C4]">یک فایل رمزدار از ثبت‌های این مرورگر بگیرید و در جای امن نگه دارید. فایل و گذرواژه به سرور ارسال نمی‌شوند. بدون گذرواژه، بازیابی فایل امکان‌پذیر نیست.</p>
      <div className="flex flex-wrap gap-2">
        <button type="button" data-testid="backup-export-open" onClick={() => setMode('export')} className={actionClass}><Download aria-hidden="true" className="ml-2 inline h-4 w-4" />گرفتن پشتیبان</button>
        <button type="button" data-testid="backup-import-open" onClick={() => setMode('import')} className="min-h-[44px] rounded-xl border border-[#DCE7E1] px-4 py-2 text-sm font-bold dark:border-[#23344D]"><FolderOpen aria-hidden="true" className="ml-2 inline h-4 w-4" />بازیابی فایل</button>
      </div>
      {mode ? <LocalBackupDialog mode={mode} onClose={() => setMode(null)} /> : null}
    </section>
  );
};

const LocalBackupDialog: React.FC<{ mode: 'export' | 'import'; onClose: () => void }> = ({ mode, onClose }) => {
  const { dataGeneration, createLocalBackup, restoreLocalBackup, notify } = useMano();
  const [passphrase, setPassphrase] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [candidate, setCandidate] = useState<LocalBackupSnapshot | null>(null);
  const [reviewedEpoch, setReviewedEpoch] = useState<string | null>(null);
  const [replaceConfirmed, setReplaceConfirmed] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const titleRef = useRef<HTMLHeadingElement>(null);
  const reviewRef = useRef<HTMLHeadingElement>(null);
  const alive = useRef(true);
  useEffect(() => { alive.current = true; return () => { alive.current = false; }; }, []);
  useEffect(() => { if (candidate) reviewRef.current?.focus(); }, [candidate]);
  const close = () => { if (!busy) onClose(); };
  const invalidateReview = () => { setCandidate(null); setReviewedEpoch(null); setReplaceConfirmed(false); setError(null); };
  const passphraseValid = passphrase.length >= 12 && passphrase.length <= 256;

  const exportFile = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!passphraseValid || passphrase !== confirmation) {
      setError('گذرواژه باید ۱۲ تا ۲۵۶ نویسه داشته باشد و با تکرار آن یکسان باشد.');
      return;
    }
    setBusy(true); setError(null);
    try {
      const content = await createLocalBackup(passphrase);
      if (!alive.current) return;
      const url = URL.createObjectURL(new Blob([content], { type: 'application/json' }));
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = `mano-backup-${new Date().toISOString().slice(0, 10)}.manobackup.json`;
      anchor.click();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
      setPassphrase(''); setConfirmation('');
      notify('فایل رمزدار برای دانلود آماده شد. نگهداری فایل و گذرواژه با شماست.', 'info');
      onClose();
    } catch (failure) {
      if (alive.current) setError(failure instanceof LocalBackupError ? failure.message : 'پشتیبان ساخته نشد. فضای ذخیره‌سازی، اندازهٔ ثبت‌ها و دسترسی مرورگر به رمزگذاری را بررسی کنید؛ اطلاعات فعلی تغییر نکرده است.');
    } finally { if (alive.current) setBusy(false); }
  };

  const inspectFile = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!file || file.size > MAX_LOCAL_BACKUP_FILE_BYTES || !passphraseValid) {
      setError('یک فایل پشتیبان مانو تا ۴ مگابایت و گذرواژهٔ ۱۲ تا ۲۵۶ نویسه‌ای آن را انتخاب کنید.');
      return;
    }
    const epoch = dataGeneration;
    setBusy(true); setError(null); setCandidate(null); setReplaceConfirmed(false);
    try {
      const decoded = await openEncryptedLocalBackup(await file.text(), passphrase);
      if (!alive.current) return;
      setCandidate(decoded); setReviewedEpoch(epoch); setPassphrase('');
    } catch {
      if (alive.current) setError('فایل باز نشد: گذرواژه نادرست است، فایل آسیب دیده یا قالب آن پشتیبانی نمی‌شود. اطلاعات فعلی تغییر نکرده است.');
    } finally { if (alive.current) setBusy(false); }
  };

  const restoreFile = async () => {
    if (!candidate || !replaceConfirmed || !reviewedEpoch || busy) return;
    setBusy(true); setError(null);
    try {
      await restoreLocalBackup(candidate, reviewedEpoch);
      if (alive.current) onClose();
    } catch {
      if (alive.current) setError('بازیابی تأیید نشد. ممکن است اطلاعات در تب دیگری پاک یا جایگزین شده باشد، یا فضای مرورگر کافی نباشد. فایل را دوباره بررسی کنید.');
    } finally { if (alive.current) setBusy(false); }
  };

  return (
    <AccessibleDialog id="mano-local-backup-dialog" label={mode === 'export' ? 'گرفتن پشتیبان رمزدار' : 'بازیابی پشتیبان رمزدار'} describedBy="backup-description" initialFocusRef={titleRef} fallbackFocusId="mano-main-content" onClose={close} closeOnEscape={!busy} backdropClassName="fixed inset-0 z-50 flex items-center justify-center bg-[#0B1220]/70 p-4 backdrop-blur-sm" panelClassName="max-h-[calc(100dvh-2rem)] w-full max-w-lg overflow-y-auto overscroll-contain rounded-3xl border border-[#DCE7E1] bg-white p-5 outline-none dark:border-[#23344D] dark:bg-[#111C2E]">
      <div className="flex items-start justify-between gap-3">
        <h2 ref={titleRef} tabIndex={-1} className="text-lg font-extrabold outline-none">{mode === 'export' ? 'گرفتن پشتیبان رمزدار' : 'بازیابی پشتیبان رمزدار'}</h2>
        <button type="button" aria-label="بستن پشتیبان" disabled={busy} onClick={close} className="flex min-h-[44px] min-w-[44px] items-center justify-center rounded-xl disabled:opacity-50"><X aria-hidden="true" className="h-5 w-5" /></button>
      </div>
      <p id="backup-description" className="my-3 text-sm leading-7">{mode === 'export' ? 'گذرواژهٔ طولانی و ویژهٔ این فایل انتخاب کنید. آن را جدا از فایل نگه دارید؛ مانو نسخه‌ای از گذرواژه ندارد.' : 'ابتدا فایل را باز و خلاصه را بررسی کنید. جایگزینی اطلاعات فقط پس از تأیید شما انجام می‌شود؛ فایل پشتیبان، تأیید بالینی محتوا نیست.'}</p>
      {error ? <p role="alert" data-testid="backup-error" className="my-3 rounded-xl bg-[#FDF0F1] p-3 text-sm leading-7 text-[#B32334] dark:bg-[#2F161E] dark:text-[#FF8594]">{error}</p> : null}
      <form onSubmit={mode === 'export' ? exportFile : inspectFile} className="space-y-4" aria-busy={busy}>
        {mode === 'import' ? <div><label htmlFor="backup-file" className="text-sm font-bold">فایل پشتیبان رمزدار</label><input id="backup-file" data-testid="backup-file-input" type="file" accept=".json,.manobackup,application/json" disabled={busy} onChange={event => { setFile(event.target.files?.[0] ?? null); invalidateReview(); }} className={`${fieldClass} max-w-full`} /></div> : null}
        {!candidate ? <>
          <div><label htmlFor="backup-passphrase" className="text-sm font-bold">گذرواژهٔ فایل، حداقل ۱۲ نویسه</label><input id="backup-passphrase" data-testid="backup-passphrase" type="password" autoComplete="new-password" minLength={12} maxLength={256} required disabled={busy} value={passphrase} onChange={event => { setPassphrase(event.target.value); setError(null); }} className={fieldClass} /></div>
          {mode === 'export' ? <div><label htmlFor="backup-confirm-passphrase" className="text-sm font-bold">تکرار گذرواژه</label><input id="backup-confirm-passphrase" data-testid="backup-confirm-passphrase" type="password" autoComplete="new-password" minLength={12} maxLength={256} required disabled={busy} value={confirmation} onChange={event => setConfirmation(event.target.value)} className={fieldClass} /></div> : null}
          <button type="submit" data-testid={mode === 'export' ? 'backup-export-submit' : 'backup-inspect-submit'} disabled={busy || !passphraseValid || (mode === 'export' ? confirmation !== passphrase : !file)} className={actionClass}>{busy ? 'در حال بررسی…' : mode === 'export' ? 'ساخت و دانلود فایل رمزدار' : 'باز کردن و بررسی فایل'}</button>
        </> : null}
      </form>
      {candidate ? <div data-testid="backup-review" className="mt-4 space-y-3 rounded-2xl border border-[#DCE7E1] p-4 text-sm leading-7 dark:border-[#23344D]">
        <h3 ref={reviewRef} tabIndex={-1} className="font-bold outline-none">خلاصهٔ فایل بازشده</h3>
        <p>نام مستعار: <bdi>{candidate.childProfile.aliasFa}</bdi></p>
        <p>ثبت دما: {candidate.feverLogs.length.toLocaleString('fa-IR')} · ثبت دارو: {candidate.doseLogs.length.toLocaleString('fa-IR')} · اندازه‌گیری رشد: {candidate.growthMeasurements.length.toLocaleString('fa-IR')}</p>
        <p>داروهای برنامه: {candidate.treatmentPlan.medications.length.toLocaleString('fa-IR')} · پرسش‌های محلی: {candidate.pharmacistConsultations.length.toLocaleString('fa-IR')}</p>
        <p className="font-bold">تمام ثبت‌های محلی فعلی جایگزین می‌شوند؛ ادغام انجام نمی‌شود. سن، وزن و غلظت بطری‌ها را پس از بازیابی دوباره بررسی کنید. یادآور خودکار فعال نمی‌شود.</p>
        <label className="flex items-start gap-2"><input type="checkbox" data-testid="backup-replace-confirm" checked={replaceConfirmed} disabled={busy} onChange={event => setReplaceConfirmed(event.target.checked)} className="mt-2 h-5 w-5 shrink-0" /><span>خلاصه را بررسی کردم و جایگزینی تمام اطلاعات همین مرورگر را تأیید می‌کنم.</span></label>
        <button type="button" data-testid="backup-restore-submit" disabled={!replaceConfirmed || busy} onClick={restoreFile} className={actionClass}>{busy ? 'در حال بازیابی…' : 'تأیید و جایگزینی اطلاعات محلی'}</button>
      </div> : null}
      {busy ? <p role="status" className="mt-3 text-sm">این کار ممکن است چند لحظه طول بکشد. صفحه را باز نگه دارید.</p> : null}
    </AccessibleDialog>
  );
};
