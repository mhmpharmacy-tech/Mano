import { useRef, useState } from 'react';
import type { CareState, IndependentStageClient, StageCareCard, StageProfile } from '../online/independentStageClient.ts';

type Props = { enabled: boolean; client: IndependentStageClient; role: 'parent' | 'pharmacist'; pharmacyId: string };
const labels: Record<CareState,string> = {
  issued:'صادر شده', acknowledged:'دریافت تأیید شد', followup_requested:'در انتظار پیگیری', closed:'پیگیری بسته شد',
};
// Internal review component only. The parent must remount on account changes.
// No app routes, real records, clinical instructions, credentials or Firebase writes.
export function IndependentCarePreview(props: Props) {
  if (!props.enabled) return null;
  return <CareWorkspace {...props} />;
}
function CareWorkspace({client,role,pharmacyId}: Props) {
  const [view,setView]=useState<'home'|'profile'|'care'>('home');
  const [profiles,setProfiles]=useState<StageProfile[]>([]);
  const [selected,setSelected]=useState('');
  const [cards,setCards]=useState<StageCareCard[]>([]);
  const [consent,setConsent]=useState(false);
  const [busy,setBusy]=useState(false);
  const [message,setMessage]=useState('');
  const locked=useRef(false);
  async function run(operation:()=>Promise<void>) {
    if(locked.current)return;
    locked.current=true;setBusy(true);setMessage('');
    try{await operation();}catch{setCards([]);setProfiles([]);setSelected('');setConsent(false);setMessage('عملیات تأیید نشد. پیش از تکرار، فهرست را تازه کنید.');}
    finally{locked.current=false;setBusy(false);}
  }
  async function loadProfiles(){
    setSelected('');setCards([]);setConsent(false);
    setProfiles(await client.list());
  }
  async function selectProfile(id:string){
    setSelected(id);setCards([]);setConsent(false);
    setCards(await client.listCare(id));setView('care');
  }
  async function refreshCards(){setCards(await client.listCare(selected));}
  const btn='rounded-xl border border-[#DCE7E1] bg-white px-4 py-3 text-sm font-semibold text-[#105B48] disabled:opacity-50';
  return <section dir="rtl" className="space-y-4 text-[#122823]" aria-label="پیش‌نمایش داخلی چرخه مراقبت">
    <p role="note" className="rounded-xl bg-[#FDF4E4] p-3 text-sm">محیط آزمایشی با داده ساختگی؛ برای درمان یا اطلاعات بیمار استفاده نشود.</p>
    <h2 className="text-lg font-extrabold">{role==='parent'?'مراقبت خانواده':'پنل آزمایشی داروساز'}</h2>
    <nav aria-label="مسیر آزمایشی مراقبت" className="flex flex-wrap gap-2">
      <button className={btn} disabled={busy} onClick={()=>setView('home')}>خانه</button>
      <button className={btn} disabled={busy} onClick={()=>void run(async()=>{await loadProfiles();setView('profile');})}>پرونده کودک</button>
      <button className={btn} disabled={busy||!selected} onClick={()=>void run(async()=>{await refreshCards();setView('care');})}>کارت مراقبت</button>
    </nav>
    {view==='home'&&<div className="rounded-2xl border border-[#DCE7E1] bg-[#F5F9F6] p-4">
      <h3 className="font-bold">ادامه مراقبت قبلی</h3>
      <button className={btn} disabled={busy} onClick={()=>void run(async()=>{await loadProfiles();setView('profile');})}>انتخاب پرونده آزمایشی</button>
    </div>}
    {view==='profile'&&<div className="space-y-2">
      {role==='parent'&&<button className={btn} disabled={busy} onClick={()=>void run(async()=>{await client.create('demo-review-'+crypto.randomUUID().slice(0,8),24);await loadProfiles();})}>ساخت پرونده نمونه</button>}
      {profiles.length===0&&<p>پرونده قابل مشاهده وجود ندارد.</p>}
      {profiles.map(p=><button key={p.id} className={btn+' block w-full text-right'} disabled={busy} onClick={()=>void run(()=>selectProfile(p.id))}>{p.alias} ـ {p.ageMonths} ماه</button>)}
    </div>}
    {view==='care'&&selected&&<div className="space-y-3">
      {role==='parent'&&<div className="rounded-xl border border-[#DCE7E1] p-3">
        <label className="flex items-center gap-2"><input type="checkbox" checked={consent} disabled={busy} onChange={e=>setConsent(e.target.checked)} /> رضایت آزمایشی برای همین داروخانه</label>
        <div className="flex flex-wrap gap-2">
          <button className={btn} disabled={busy||!consent} onClick={()=>void run(async()=>{await client.grant(selected,pharmacyId,consent);setConsent(false);await refreshCards();})}>اعطای دسترسی</button>
          <button className={btn} disabled={busy} onClick={()=>void run(async()=>{await client.revoke(selected,pharmacyId);setConsent(false);await refreshCards();})}>لغو دسترسی</button>
        </div>
      </div>}
      {role==='pharmacist'&&<div className="rounded-xl border border-[#DCE7E1] p-3">
        <label className="flex items-center gap-2"><input type="checkbox" checked={consent} disabled={busy} onChange={e=>setConsent(e.target.checked)} /> تأیید صدور کارت نمونه</label>
        <button className={btn} disabled={busy||!consent} onClick={()=>void run(async()=>{await client.issueCare(selected,pharmacyId,consent);setConsent(false);await refreshCards();})}>صدور کارت مراقبت آزمایشی</button>
      </div>}
      <button className={btn} disabled={busy} onClick={()=>void run(refreshCards)}>تازه‌سازی کارت‌ها</button>
      {cards.length===0&&<p>کارت قابل مشاهده وجود ندارد.</p>}
      {cards.map(c=>{
        const next=role==='parent'?(c.state==='issued'?'acknowledged':c.state==='acknowledged'?'followup_requested':null):c.state==='followup_requested'?'closed':null;
        return <article key={c.id} className="rounded-2xl border border-[#DCE7E1] bg-white p-4 space-y-2">
          <h3 className="font-bold">کارت مراقبت نمونه</h3><p>{labels[c.state]}</p>
          {next&&<button className={btn} disabled={busy} onClick={()=>void run(async()=>{await client.transitionCare(selected,c.id,next);await refreshCards();})}>{next==='acknowledged'?'تأیید دریافت':next==='followup_requested'?'درخواست پیگیری':'بستن پیگیری آزمایشی'}</button>}
        </article>;
      })}
    </div>}
    <p role="status">{busy?'در حال بررسی…':message}</p>
  </section>;
}
