(function (root, factory) {
  const api = factory();
  if (typeof module === "object" && module.exports) module.exports = api;
  else api.init(root.document, root.MANO_REVIEW);
})(typeof window !== "undefined" ? window : this, function () {
  "use strict";
  const keys = ["sources", "age", "safety", "iran", "limits"];
  function validateDecision(d) {
    if (!d || !["approved","changes","hold"].includes(d.status) || !Array.isArray(d.checks) || d.checks.some(k=>!keys.includes(k)) || new Set(d.checks).size!==d.checks.length || typeof d.note!=="string" || d.note.length>1200) throw Error("تصمیم یا یادداشت نامعتبر است.");
    if(d.status==="approved" && !keys.every(k=>d.checks.includes(k))) throw Error("برای تأیید، همه معیارها لازم‌اند.");
    if(d.status!=="approved" && !d.note.trim()) throw Error("دلیل اصلاح یا توقف را بنویسید.");
    if(typeof d.at!=="string" || !Number.isFinite(Date.parse(d.at)) || new Date(d.at).toISOString()!==d.at) throw Error("زمان ثبت نامعتبر است.");
    return {status:d.status,checks:keys.filter(k=>d.checks.includes(k)),note:d.note.trim(),at:d.at};
  }
  function validatePacket(packet, queue) {
    if(!queue || !packet || packet.schemaVersion!==1 || packet.snapshot!==queue.snapshot || packet.releaseAuthorized!==false || packet.kind!=="MANO_CONTENT_REVIEW_DRAFT") throw Error("فایل با نسخه فعلی سازگار نیست.");
    if(typeof packet.reviewer!=="string" || !packet.reviewer.trim() || packet.reviewer.length>80 || !Array.isArray(packet.decisions) || packet.decisions.length>queue.items.length) throw Error("شناسه بازبین یا تصمیم‌ها نامعتبر است.");
    const seen=new Set(), ids=new Set(queue.items.map(x=>x.id));
    const decisions=packet.decisions.map(d=>{if(!d||!ids.has(d.id)||seen.has(d.id))throw Error("شناسه مورد نامعتبر است.");seen.add(d.id);return {id:d.id,...validateDecision(d)}});
    return {schemaVersion:1,kind:packet.kind,snapshot:queue.snapshot,releaseAuthorized:false,reviewer:packet.reviewer.trim(),decisions};
  }
  function init(doc,queue) {
    if(!queue || !Array.isArray(queue.items))return;
    const w=doc.defaultView, by=id=>doc.getElementById(id), decisions=new Map(), checks=Array.from(doc.querySelectorAll("[data-check]"));
    const group=by("review-group"),select=by("review-item"),note=by("review-note"),status=by("review-decision"),reviewer=by("reviewer");
    let current=null,dirty=false;
    const message=t=>{by("review-message").textContent=t};
    function progress(){const all=[...decisions.values()];by("review-progress").textContent=all.length.toLocaleString("fa-IR")+" از "+queue.items.length.toLocaleString("fa-IR")+" ثبت شده | تأیید محتوایی: "+all.filter(d=>d.status==="approved").length.toLocaleString("fa-IR");by("review-export").disabled=!all.length}
    function display(id){
      current=queue.items.find(x=>x.id===id)||null;dirty=false;by("review-detail").hidden=!current;if(!current)return;
      by("review-title").textContent=current.title;by("review-link").href="./"+current.page;by("review-local").textContent=current.local;
      const ul=by("review-refs");ul.replaceChildren();
      if(!current.refs.length){const li=doc.createElement("li");li.textContent="منبع مستقیم در این سطح ثبت نشده؛ کل صفحه و منابع آن بررسی شود.";ul.append(li)}
      for(const r of current.refs){const li=doc.createElement("li"),a=doc.createElement("a"),small=doc.createElement("small");a.href=r.url;a.target="_blank";a.rel="noopener noreferrer";a.textContent=r.publisher+" / "+r.id;small.textContent=(r.topic?r.topic+" — ":"")+r.locator+" | "+r.coverage+" | "+(r.verifiedOn||"بازبینی انسانی ثبت نشده");li.append(a,small);ul.append(li)}
      const d=decisions.get(id);note.value=d?.note||"";status.value=d?.status||"";checks.forEach(c=>c.checked=Boolean(d?.checks.includes(c.dataset.check)));
    }
    function populate(preferred){
      const items=queue.items.filter(x=>group.value==="all"||x.group===group.value);select.replaceChildren(...items.map(x=>{const o=doc.createElement("option");o.value=x.id;o.textContent=(decisions.has(x.id)?"[ثبت‌شده] ":"")+x.title;return o}));if(items.some(x=>x.id===preferred))select.value=preferred;display(select.value);progress();
    }
    by("review-version").textContent=queue.snapshot;
    [group,select,reviewer,by("review-file"),by("review-import")].forEach(x=>x.disabled=false);
    populate();
    let oldGroup=group.value;
    group.addEventListener("change",()=>{if(dirty&&!w.confirm("تغییر ثبت‌نشده کنار گذاشته شود؟")){group.value=oldGroup;return}oldGroup=group.value;populate(current?.id)});
    select.addEventListener("change",()=>{if(dirty&&!w.confirm("تغییر ثبت‌نشده کنار گذاشته شود؟")){select.value=current.id;return}display(select.value)});
    [status,note,...checks].forEach(x=>x.addEventListener("input",()=>dirty=true));
    by("review-save").addEventListener("click",()=>{try{if(!current)throw Error("موردی انتخاب نشده.");decisions.set(current.id,validateDecision({status:status.value,checks:checks.filter(x=>x.checked).map(x=>x.dataset.check),note:note.value,at:new Date().toISOString()}));populate(current.id);message("در نشست ثبت شد؛ برای نگهداری فایل خروجی بگیرید.")}catch(e){message(e.message)}});
    by("review-export").addEventListener("click",()=>{try{if(dirty)throw Error("ابتدا تغییرات فعلی را ثبت کنید.");const packet=validatePacket({schemaVersion:1,kind:"MANO_CONTENT_REVIEW_DRAFT",snapshot:queue.snapshot,releaseAuthorized:false,reviewer:reviewer.value,decisions:[...decisions].map(([id,d])=>({id,...d}))},queue);const url=w.URL.createObjectURL(new w.Blob([JSON.stringify(packet,null,2)],{type:"application/json"}));const a=doc.createElement("a");a.href=url;a.download="MANO-review-"+queue.snapshot.slice(0,12)+".json";a.click();w.setTimeout(()=>w.URL.revokeObjectURL(url),1000);message("فایل محلی تهیه شد؛ انتشار همچنان غیرفعال است.")}catch(e){message(e.message)}});
    by("review-import").addEventListener("click",async()=>{try{const file=by("review-file").files[0];if(!file||file.size>500000)throw Error("فایل معتبر انتخاب کنید.");const packet=validatePacket(JSON.parse(await file.text()),queue);if((decisions.size||dirty)&&!w.confirm("تصمیم‌های نشست جایگزین شوند؟"))return;decisions.clear();packet.decisions.forEach(({id,...d})=>decisions.set(id,d));reviewer.value=packet.reviewer;populate();message("فایل همین نسخه بار شد؛ اصالت امضا بررسی نشده.")}catch(e){message(e.message)}});
    message("آماده بازبینی؛ تأیید انتشار فعال نیست.");
  }
  return {validateDecision,validatePacket,init,keys};
});
