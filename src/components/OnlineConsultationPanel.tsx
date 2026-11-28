import React, { useEffect, useRef, useState } from 'react';
import { CaseCarePanel } from './CaseCarePanel.tsx';
import { useOnline } from '../online/context.ts';
import { onlineErrorMessage } from '../online/config.ts';
import { buildOnlineTriageInput, ONLINE_FLOWS, OnlineIntakeError, onlineRedFlags, selectedOnlineFlow, type OnlineIntakeDraft } from '../online/intake.ts';
import { ONLINE_CONSENT_VERSION, requestConsentedTriage, submitConsentedCase, type IssuedTriage, type ParentCase, type PharmacyOption } from '../online/contracts.ts';
import { parseAcceptingPharmacies, parseParentCases } from '../online/responses.ts';

const inputClass = 'mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 py-2 text-sm dark:border-[#23344D] dark:bg-[#16243B]';
const buttonClass = 'min-h-[44px] rounded-xl border border-[#DCE7E1] px-3 py-2 text-sm font-bold disabled:opacity-50 dark:border-[#23344D]';
const CATEGORY_LABELS = { SYMPTOMS: 'علائم', OTC: 'داروی بدون نسخه', SKIN: 'پوست', SCALP_HAIR: 'پوست سر و مو', SUPPLEMENT: 'مکمل' };
const CASE_STATUS_LABELS = { PENDING: 'در انتظار دریافت', RECEIVED: 'دریافت‌شده', REVIEWED: 'بررسی‌شده توسط داروساز' };

const ConsultationForm: React.FC = () => {
  const { controller, session } = useOnline();
  const alive = useRef(true);
  useEffect(() => { alive.current = true; return () => { alive.current = false; }; }, []);
  const generation = session.generation;
  const isCurrent = () => alive.current && controller.getSession().generation === generation;
  const [draft, setDraft] = useState<OnlineIntakeDraft>({ ageMonths: '', flowId: '', temperature: '', duration: '', question: '', selectedFlags: [], flagsReviewed: false });
  const [consent, setConsent] = useState(false);
  const [category, setCategory] = useState('');
  const [pharmacy, setPharmacy] = useState('');
  const [pharmacies, setPharmacies] = useState<PharmacyOption[]>([]);
  const [issued, setIssued] = useState<IssuedTriage | null>(null);
  const [cases, setCases] = useState<ParentCase[]>([]);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const selectedFlow = selectedOnlineFlow(draft.flowId);
  const change = (patch: Partial<OnlineIntakeDraft>) => { setDraft(previous => ({ ...previous, ...patch })); setConsent(false); setIssued(null); setNotice(''); setError(''); };
  const perform = async (operation: () => Promise<void>) => {
    setBusy(true); setError(''); setNotice('');
    try { await operation(); }
    catch (err) { if (isCurrent()) setError(err instanceof OnlineIntakeError ? err.message : onlineErrorMessage(err)); }
    finally { if (isCurrent()) setBusy(false); }
  };
  const refreshCases = async () => {
    const rows = parseParentCases(await controller.call<unknown>('listParentConsultations', { limit: 25 }, { clinical: true, expectedGeneration: generation }));
    if (isCurrent()) {
      setCases(rows);
      if (rows.length === 0) setNotice('هنوز پرسش آنلاینی برای این حساب ثبت نشده است.');
    }
  };

  return <div className="space-y-4">
    <p className="text-xs leading-6">این بخش برای پرسش غیرفوری است و زمان پاسخ تضمین‌شده‌ای ندارد. در تنگی نفس، تشنج، کبودی یا کاهش هوشیاری، منتظر پاسخ آنلاین نمانید و با <a className="font-bold underline" href="tel:115">۱۱۵</a> تماس بگیرید. اطلاعات زیر مستقل از یادداشت‌های محلی وارد می‌شوند.</p>
    <div className="flex flex-wrap gap-2">
      <button type="button" className={buttonClass} disabled={busy} onClick={() => void perform(async () => {
        const rows = parseAcceptingPharmacies(await controller.call<unknown>('listAcceptingPharmacies', {}, { clinical: true }));
        if (isCurrent()) { setPharmacies(rows); setPharmacy(''); setIssued(null); setConsent(false); if (rows.length === 0) setNotice('داروخانهٔ پذیرنده‌ای در فهرست فعلی نیست؛ از مسیر حضوری استفاده کنید.'); }
      })}>دریافت فهرست داروخانه‌های پذیرنده</button>
      <button type="button" className={buttonClass} disabled={busy} onClick={() => void perform(refreshCases)}>دریافت سوابق همین حساب</button>
    </div>
    <form className="space-y-4" onSubmit={event => { event.preventDefault(); void perform(async () => {
      if (!pharmacy || !category) throw new OnlineIntakeError('داروخانهٔ پذیرنده و دستهٔ پرسش را انتخاب کنید.');
      const result = await requestConsentedTriage(controller, controller.transport, buildOnlineTriageInput(draft), consent);
      if (isCurrent()) setIssued(result);
    }); }}>
      <div className="grid gap-3 sm:grid-cols-2">
        <label className="block text-xs font-bold">داروخانهٔ مقصد<select className={inputClass} value={pharmacy} required disabled={busy || pharmacies.length === 0} onChange={e => { setPharmacy(e.target.value); setConsent(false); setIssued(null); }}><option value="">انتخاب کنید</option>{pharmacies.map(row => <option key={row.pharmacyId} value={row.pharmacyId}>{row.displayNameFa}</option>)}</select></label>
        <label className="block text-xs font-bold">دستهٔ پرسش<select className={inputClass} value={category} required disabled={busy} onChange={e => { setCategory(e.target.value); setConsent(false); setIssued(null); }}><option value="">انتخاب کنید</option>{Object.entries(CATEGORY_LABELS).map(([id, label]) => <option key={id} value={id}>{label}</option>)}</select></label>
        <label className="block text-xs font-bold">موضوع و نشانه‌های مرتبط<select className={inputClass} value={draft.flowId} required disabled={busy} onChange={e => change({ flowId: e.target.value, temperature: '', duration: '', selectedFlags: [], flagsReviewed: false })}><option value="">انتخاب کنید</option>{ONLINE_FLOWS.map(flow => <option key={flow.id} value={flow.id}>{flow.titleFa}</option>)}</select></label>
        <label className="block text-xs font-bold">سن واقعی کودک به ماه<input className={inputClass} value={draft.ageMonths} inputMode="decimal" required maxLength={12} disabled={busy} onChange={e => change({ ageMonths: e.target.value })} placeholder="اعشار سن پذیرفته می‌شود" /></label>
        {selectedFlow?.asksTemperature && <label className="block text-xs font-bold">دمای اندازه‌گیری‌شده، درجهٔ سانتی‌گراد<input className={inputClass} value={draft.temperature} inputMode="decimal" required maxLength={12} disabled={busy} onChange={e => change({ temperature: e.target.value })} /></label>}
        {selectedFlow?.asksDuration && <label className="block text-xs font-bold">مدت واقعی علائم، ساعت کامل<input className={inputClass} value={draft.duration} inputMode="numeric" required maxLength={8} disabled={busy} onChange={e => change({ duration: e.target.value })} /></label>}
      </div>
      {selectedFlow && <fieldset className="space-y-2 rounded-2xl border border-[#E7B5BC] p-3 dark:border-[#6A2C3B]">
        <legend className="px-1 text-sm font-bold">نشانه‌های هشدار را مرور کنید</legend>
        {onlineRedFlags(draft.flowId).map(flag => <label key={flag.id} className="flex min-h-[44px] items-start gap-2 text-xs leading-6"><input type="checkbox" className="mt-1 h-5 w-5 shrink-0" checked={draft.selectedFlags.includes(flag.id)} disabled={busy} onChange={e => change({ selectedFlags: e.target.checked ? [...draft.selectedFlags, flag.id] : draft.selectedFlags.filter(id => id !== flag.id), flagsReviewed: false })} /><span>{flag.labelFa}</span></label>)}
        <label className="flex min-h-[44px] items-start gap-2 border-t border-[#E7B5BC] pt-2 text-xs leading-6 dark:border-[#6A2C3B]"><input type="checkbox" className="mt-1 h-5 w-5 shrink-0" checked={draft.flagsReviewed} required disabled={busy} onChange={e => change({ flagsReviewed: e.target.checked })} />فهرست را مرور کرده‌ام و نشانه‌های مشاهده‌شده را مشخص کرده‌ام. این فهرست جایگزین معاینه نیست.</label>
      </fieldset>}
      <label className="block text-xs font-bold">شرح پرسش، ۸ تا ۶۰۰ نویسه<textarea className={`${inputClass} min-h-28`} value={draft.question} required minLength={8} maxLength={600} disabled={busy} onChange={e => change({ question: e.target.value })} placeholder="فقط اطلاعات لازم؛ نام، کد ملی، نشانی یا تصویر کودک را وارد نکنید." /></label>
      <label className="flex min-h-[44px] items-start gap-2 text-xs leading-6"><input type="checkbox" className="mt-1 h-5 w-5 shrink-0" checked={consent} required disabled={busy} onChange={e => { setConsent(e.target.checked); if (!e.target.checked) setIssued(null); }} /><span>رضایت می‌دهم سن/ردهٔ سنی، دما و مدت واردشده، نشانه‌ها و متن همین پرسش با شناسهٔ حساب، ابتدا برای بررسی مقدماتی به سرور و سپس با اقدام جداگانهٔ من به داروخانهٔ انتخاب‌شده ارسال شود. سیاست رضایت: {ONLINE_CONSENT_VERSION}. داده‌های محلی دیگری ضمیمه نمی‌شوند.</span></label>
      <button type="submit" className={`${buttonClass} bg-[#105B48] text-white dark:bg-[#00D7C7] dark:text-[#0B1220]`} disabled={busy || !consent || !pharmacy || !category}>{busy ? 'در حال بررسی…' : 'ارسال همین اطلاعات برای بررسی مقدماتی'}</button>
      {issued && <div role={issued.decision.outcome === 'NONURGENT' ? 'status' : 'alert'} className="space-y-2 rounded-2xl border border-[#E7B5BC] p-3 text-xs leading-6 dark:border-[#6A2C3B]">
        <p className="font-bold">{issued.decision.titleFa}</p><p>{issued.decision.summaryFa}</p>
        {issued.decision.emergencyActionsFa.map((action, index) => <p key={index}>{action}</p>)}
        {issued.decision.outcome !== 'NONURGENT' && <a href="tel:115" className="inline-flex min-h-[44px] items-center font-bold underline">تماس با اورژانس ۱۱۵ در وضعیت فوری</a>}
        {issued.decision.outcome === 'NONURGENT' && issued.decision.eligibleForNonUrgentConsultation && <button type="button" className={buttonClass} disabled={busy || !consent} onClick={() => void perform(async () => {
          const currentPass = issued; setIssued(null);
          const result = await submitConsentedCase(controller, controller.transport, buildOnlineTriageInput(draft), currentPass, consent, pharmacy, category);
          const [safe] = parseParentCases([result], pharmacy);
          if (isCurrent()) { setCases(previous => [safe, ...previous.filter(row => row.caseId !== safe.caseId)]); setConsent(false); setDraft({ ageMonths: '', flowId: '', temperature: '', duration: '', question: '', selectedFlags: [], flagsReviewed: false }); setNotice('پرسش ثبت شد؛ این پیام به معنی بررسی یا تأیید درمان توسط داروساز نیست.'); }
        })}>ارسال نهایی پرسش به داروخانهٔ انتخاب‌شده</button>}
      </div>}
    </form>
    {error && <p role="alert" className="text-xs leading-6 text-[#B32334] dark:text-[#FF8594]">{error}</p>}
    {notice && <p role="status" className="text-xs leading-6">{notice}</p>}
    {cases.length > 0 && <div className="space-y-3"><h3 className="text-sm font-bold">سوابق آنلاین همین حساب</h3>{cases.map(item => <article key={`${item.pharmacyId}-${item.caseId}`} className="space-y-2 rounded-2xl bg-[#F5F9F6] p-3 text-xs leading-6 dark:bg-[#16243B]"><p>{CASE_STATUS_LABELS[item.status]}</p><p className="whitespace-pre-wrap break-words">{item.questionSummaryFa}</p>{item.parentResponse ? <p className="whitespace-pre-wrap break-words">پاسخ داروساز: {item.parentResponse.replyTextFa}</p> : <p>هنوز پاسخی برای مراقب منتشر نشده است.</p>}<CaseCarePanel row={item} pharmacist={false} refresh={refreshCases} /></article>)}</div>}
  </div>;
};

const PharmacistDesk: React.FC = () => {
  const { controller, session } = useOnline();
  const generation = session.generation;
  const pharmacyId = session.user?.pharmacyId;
  const alive = useRef(true);
  useEffect(() => { alive.current = true; return () => { alive.current = false; }; }, []);
  const isCurrent = () => alive.current && controller.getSession().generation === generation;
  const [cases, setCases] = useState<ParentCase[]>([]);
  const [selected, setSelected] = useState('');
  const [reply, setReply] = useState('');
  const [note, setNote] = useState('');
  const [attested, setAttested] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const perform = async (operation: () => Promise<void>) => {
    setBusy(true); setError(''); setNotice('');
    try { await operation(); }
    catch (err) { if (isCurrent()) setError(onlineErrorMessage(err)); }
    finally { if (isCurrent()) setBusy(false); }
  };
  const refreshInbox = async () => {
      if (!pharmacyId) return;
      const rows = parseParentCases(await controller.call<unknown>('getPharmacyInbox', { pharmacyId, limit: 30 }, { clinical: true, expectedGeneration: generation }), pharmacyId);
      if (isCurrent()) { setCases(rows); setSelected(''); setReply(''); setNote(''); setAttested(false); if (rows.length === 0) setNotice('در فهرست فعلی پرسشی وجود ندارد.'); }
  };
  const updateCase = async (item: ParentCase, reviewed: boolean) => {
    if (!pharmacyId || item.pharmacyId !== pharmacyId) return;
    const result = await controller.call<unknown>('reviewConsultationCase', {
      pharmacyId, caseId: item.caseId, nextStatus: reviewed ? 'REVIEWED' : 'RECEIVED',
      ...(reviewed ? { parentReply: reply.trim(), internalNote: note.trim(), pharmacistAttestation: attested } : {}),
    }, { clinical: true });
    const [row] = parseParentCases([result], pharmacyId);
    if (row.caseId !== item.caseId) throw new Error('INVALID_RESPONSE');
    if (isCurrent()) { setCases(previous => previous.map(current => current.caseId === row.caseId ? row : current)); setSelected(''); setReply(''); setNote(''); setAttested(false); setNotice(reviewed ? 'پاسخ داروساز ثبت و برای مراقب منتشر شد.' : 'دریافت پرسش ثبت شد؛ هنوز پاسخی منتشر نشده است.'); }
  };
  return <div className="space-y-4">
    <h3 className="text-sm font-bold">میز آنلاین داروساز</h3>
    <p className="text-xs leading-6">این بخش فقط برای داروساز تأییدشده و عضو فعال داروخانهٔ حساب است. حساب مدیریتی، دسترسی بررسی بالینی ندارد. «دریافت پرسش» به معنی تأیید درمان نیست.</p>
    <button type="button" className={buttonClass} disabled={busy} onClick={() => void perform(refreshInbox)}>دریافت درخواست‌های داروخانهٔ من</button>
    <div className="space-y-3">{cases.map(item => <article key={item.caseId} className="space-y-3 rounded-2xl bg-[#F5F9F6] p-3 text-xs leading-6 dark:bg-[#16243B]">
      <p className="font-bold">{CASE_STATUS_LABELS[item.status]}</p><p className="whitespace-pre-wrap break-words">{item.questionSummaryFa}</p>
      {item.status === 'PENDING' && <button type="button" className={buttonClass} disabled={busy} onClick={() => void perform(() => updateCase(item, false))}>ثبت دریافت این پرسش</button>}
      {item.status === 'RECEIVED' && selected !== item.caseId && <button type="button" className={buttonClass} disabled={busy} onClick={() => { setSelected(item.caseId); setReply(''); setNote(''); setAttested(false); }}>نوشتن پاسخ برای این پرسش</button>}
      {selected === item.caseId && item.status === 'RECEIVED' && <form className="space-y-3" onSubmit={event => { event.preventDefault(); if (!attested || reply.trim().length < 10 || reply.trim().length > 2000) return; void perform(() => updateCase(item, true)); }}>
        <label className="block text-xs font-bold">پاسخ قابل مشاهده برای مراقب<textarea className={`${inputClass} min-h-28`} minLength={10} maxLength={2000} required value={reply} disabled={busy} onChange={event => { setReply(event.target.value); setAttested(false); }} /></label>
        <label className="block text-xs font-bold">یادداشت داخلی اختیاری؛ برای مراقب منتشر نمی‌شود<textarea className={inputClass} maxLength={1000} value={note} disabled={busy} onChange={event => { setNote(event.target.value); setAttested(false); }} /></label>
        <label className="flex min-h-[44px] items-start gap-2 text-xs leading-6"><input type="checkbox" className="mt-1 h-5 w-5 shrink-0" required checked={attested} disabled={busy} onChange={event => setAttested(event.target.checked)} />این پرسش و متن پاسخ فعلی را شخصاً بررسی کرده‌ام و انتشار آن را با مسئولیت حرفه‌ای خود تأیید می‌کنم.</label>
        <button type="submit" className={buttonClass} disabled={busy || !attested || reply.trim().length < 10}>ثبت بررسی و انتشار پاسخ برای مراقب</button>
      </form>}
      {item.parentResponse && <p className="whitespace-pre-wrap break-words">پاسخ منتشرشده: {item.parentResponse.replyTextFa}</p>}
      <CaseCarePanel row={item} pharmacist refresh={refreshInbox} />
    </article>)}</div>
    {error && <p role="alert" className="text-xs leading-6 text-[#B32334] dark:text-[#FF8594]">{error}</p>}
    {notice && <p role="status" className="text-xs leading-6">{notice}</p>}
  </div>;
};

export const OnlineConsultationPanel: React.FC = () => {
  const { enabled, session } = useOnline();
  return <section className="space-y-3 rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]" data-testid="online-consultation-panel">
    <h2 className="text-base font-extrabold">پرسش آنلاین غیرفوری</h2>
    {!enabled ? <p className="text-xs leading-6">ارسال آنلاین در این نسخه فعال نیست؛ یادداشت محلیِ پایین برای مراجعهٔ حضوری است و برای داروساز ارسال نمی‌شود.</p> : !session.user ? <p className="text-xs leading-6">برای استفاده از این بخش، از «پرونده کودک» وارد حساب آنلاین شوید. اطلاعات محلی به‌صورت خودکار ارسال نمی‌شود.</p> : !session.user.emailVerified ? <p className="text-xs leading-6">ایمیل را در بخش حساب تأیید کنید و وضعیت را تازه کنید. تا آن زمان اطلاعات مراقبتی ارسال نمی‌شود.</p> : session.user.staffRole ? session.user.staffRole === 'pharmacist' && session.user.staffVerified && session.user.pharmacyId ? <PharmacistDesk key={session.generation} /> : <p className="text-xs leading-6">دسترسی بالینی این حساب کارکنان تأیید نشده است. حساب مدیریتی نیز نمی‌تواند پاسخ بالینی را تأیید کند.</p> : <ConsultationForm key={session.generation} />}
    {enabled && !session.pending && !session.user?.emailVerified && (
      <a className={`inline-flex items-center ${buttonClass}`} href="#/my-child">ورود یا تأیید حساب</a>
    )}
  </section>;
};
