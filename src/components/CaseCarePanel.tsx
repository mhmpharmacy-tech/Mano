import React, { useRef, useState } from 'react';
import { cardFields } from '../online/caseCareCard.ts';
import type { ParentCase } from '../online/contracts.ts';
import { useOnline } from '../online/context.ts';
import { onlineErrorMessage } from '../online/config.ts';

const button = 'min-h-[44px] rounded-xl border border-[#DCE7E1] px-3 text-xs font-bold disabled:opacity-50 dark:border-[#23344D]';
const input = 'block w-full rounded-xl border border-[#DCE7E1] bg-transparent px-3 py-2 dark:border-[#23344D]';

export function CaseCarePanel({ row, pharmacist, refresh }: { row: ParentCase; pharmacist: boolean; refresh: () => Promise<void> }) {
  const { controller, session } = useOnline();
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const [confirmed, setConfirmed] = useState(false);
  const attempt = useRef<{ payload: string; id: string } | null>(null);
  const inFlight = useRef(false);
  const card = row.careCard;
  if (row.status !== 'REVIEWED' || (!card && !pharmacist)) return null;

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (inFlight.current || (!card && !confirmed)) return;
    const form = event.currentTarget;
    const data = new FormData(form);
    const action = card ? (pharmacist ? 'reply' : 'checkin') : 'send';
    const values: Record<string, string> = {};
    for (const key of card ? ['text'] : Object.keys(cardFields)) {
      const value = data.get(key);
      if (typeof value !== 'string' || !value.trim()) { setError('همهٔ فیلدهای الزامی را تکمیل کنید.'); return; }
      values[key] = value.trim();
    }
    const generation = session.generation;
    const payload = { pharmacyId: row.pharmacyId, caseId: row.caseId, action, ...values, ...(!card ? { confirmed } : {}) };
    const fingerprint = JSON.stringify(payload);
    inFlight.current = true;
    setBusy(true);
    setError('');
    setNotice('');
    let saved = false;
    try {
      if (attempt.current?.payload !== fingerprint) attempt.current = { payload: fingerprint, id: crypto.randomUUID() };
      const result = await controller.call<{ ok?: boolean }>('updateCareCard', { ...payload, requestId: attempt.current.id }, { clinical: true, expectedGeneration: generation });
      if (result?.ok !== true) throw new Error('INVALID_RESPONSE');
      saved = true;
      form.reset();
      setConfirmed(false);
      setNotice('ثبت شد.');
      await refresh();
      attempt.current = null;
    } catch (err) {
      if (controller.getSession().generation === generation) {
        setError(saved ? 'ثبت انجام شد. برای مشاهدهٔ وضعیت تازه، فهرست پرونده‌ها را دوباره دریافت کنید.' : onlineErrorMessage(err));
      }
    } finally {
      inFlight.current = false;
      if (controller.getSession().generation === generation) setBusy(false);
    }
  }

  return <section className="space-y-3 border-t border-[#DCE7E1] pt-3 text-xs leading-6 dark:border-[#23344D]">
    <h4 className="font-bold">کارت مراقبت داروخانه</h4>
    {card && <>
      <dl>{card.fields.map(field => <div key={field.label}><dt className="font-bold">{field.label}</dt><dd className="whitespace-pre-wrap break-words">{field.value}</dd></div>)}</dl>
      <ul>{card.events.map((entry, index) => <li key={index} className="whitespace-pre-wrap break-words">{new Date(entry.at).toLocaleString('fa-IR')} · {entry.label} {entry.text}</li>)}</ul>
    </>}
    {card?.closed ? <p>پیگیری این کارت پایان یافته است.</p> :
      <details><summary className="cursor-pointer font-bold">{!card ? 'صدور کارت برای این خانواده' : pharmacist ? 'پاسخ به پیگیری خانواده' : 'ارسال پیگیری برای داروخانه'}</summary>
        <form className="space-y-3 pt-2" onSubmit={submit}>
          <fieldset disabled={busy} className="space-y-2">
            {card ? <label className="block">{pharmacist ? 'پاسخ برای خانواده' : 'پیام برای داروساز'}<textarea className={input} name="text" required maxLength={1000} /></label> :
              <>
                {Object.entries(cardFields).map(([key, [label, max]]) => <label className="block" key={key}>{label}<textarea className={input} name={key} required maxLength={max} onChange={() => setConfirmed(false)} /></label>)}
                <label className="flex items-start gap-2"><input type="checkbox" required checked={confirmed} onChange={event => setConfirmed(event.target.checked)} />مشخصات کودک، دارو و متن کارت را بررسی کردم و ارسال آن برای همین خانواده را تأیید می‌کنم.</label>
              </>}
            <button className={button} type="submit" disabled={!card && !confirmed}>{!card ? 'تأیید و ارسال کارت' : 'ارسال پیام'}</button>
          </fieldset>
        </form>
      </details>}
    {notice && <p role="status">{notice}</p>}
    {error && <p role="alert">{error}</p>}
  </section>;
}