/* Static, nonclinical module interactions; no patient inputs or persistence. */
(function(root,factory){
 const api=factory();
 if(typeof module==="object"&&module.exports)module.exports=api;
 else api.init(root.document);
})(typeof window!=="undefined"?window:this,function(){
 "use strict";
 const examples=Object.freeze({
   a:Object.freeze([[2,4.8,55],[4,6.1,61],[6,6.7,65],[9,7.8,70],[12,8.5,74]].map(Object.freeze)),
   b:Object.freeze([[2,5.2,57],[4,6.5,62],[6,7.1,66],[9,7.2,69],[12,7.5,73]].map(Object.freeze)),
   c:Object.freeze([[2,4.5,54],[4,5.8,59],[6,6.6,65],[9,8.2,71],[12,9.4,76]].map(Object.freeze))
 });
 function normalize(v){return String(v??"").normalize("NFKC").replace(/[يى]/g,"ی").replace(/ك/g,"ک").replace(/[\u064B-\u065F\u200C\u200D\s]/g,"").toLowerCase()}
 function matches(text,category,query,filter){return (filter==="all"||category===filter)&&normalize(text).includes(normalize(query))}
 function growthSeries(example,metric){
  if(!Object.hasOwn(examples,example)||!["weight","height"].includes(metric))return null;
  const index=metric==="weight"?1:2;
  return examples[example].map(row=>({month:row[0],value:row[index]}));
 }
 const fa=n=>Number(n).toLocaleString("fa-IR",{maximumFractionDigits:1});
 // Fixed educational example identifiers, never real patient or triage inputs.
 const connectExamples=Object.freeze({label:"training",feeding:"training",urgent:"emergency",unknown:"uncertain"});
 function exampleRoute(id){return Object.hasOwn(connectExamples,id)?connectExamples[id]:null}

 // Fixed educational examples only; integer hundredths of mg / mL preserve exact comparisons.
 const labelExamples = Object.freeze({"apap32":{"title":"استامینوفن؛ 160 mg/5 mL","provenance":"FDA · نمونه تاریخی آمریکا","form":"oral-liquid","volume":500,"ingredients":[["acetaminophen",16000]]},"apap100":{"title":"استامینوفن؛ 80 mg/0.8 mL","provenance":"FDA · نمونه تاریخی آمریکا","form":"oral-liquid","volume":80,"ingredients":[["acetaminophen",8000]]},"apap80":{"title":"استامینوفن؛ 80 mg/mL","provenance":"FDA · نمونه تاریخی آمریکا","form":"oral-liquid","volume":100,"ingredients":[["acetaminophen",8000]]},"apapMath":{"title":"استامینوفن؛ 32 mg/mL","provenance":"فقط بیان ریاضی label A؛ نه فرآورده واقعی","form":"oral-liquid","volume":100,"ingredients":[["acetaminophen",3200]]},"co200":{"title":"کوآموکسی‌کلاو؛ 200/28.5 mg/5 mL","provenance":"DailyMed · نمونه برچسب آمریکا","form":"oral-suspension-reconstituted","volume":500,"ingredients":[["amoxicillin",20000],["clavulanic-acid",2850]]},"co400":{"title":"کوآموکسی‌کلاو؛ 400/57 mg/5 mL","provenance":"DailyMed · نمونه برچسب آمریکا","form":"oral-suspension-reconstituted","volume":500,"ingredients":[["amoxicillin",40000],["clavulanic-acid",5700]]},"co600":{"title":"کوآموکسی‌کلاو؛ 600/42.9 mg/5 mL","provenance":"DailyMed · نمونه برچسب آمریکا","form":"oral-suspension-reconstituted","volume":500,"ingredients":[["amoxicillin",60000],["clavulanic-acid",4290]]}});
 function compareLabels(first,second){
  if(!Object.hasOwn(labelExamples,first)||!Object.hasOwn(labelExamples,second))return null;
  const a=labelExamples[first],b=labelExamples[second];
  const sameIngredients=a.ingredients.length===b.ingredients.length&&a.ingredients.every((i,k)=>i[0]===b.ingredients[k][0]);
  const result={status:"different-ingredients-or-form",titles:[a.title,b.title],provenance:[a.provenance,b.provenance],rows:[]};
  if(!sameIngredients||a.form!==b.form)return result;
  const equal=a.ingredients.every((i,k)=>i[1]*b.volume===b.ingredients[k][1]*a.volume);
  const sameRatio=a.ingredients.every((i,k)=>i[1]*b.ingredients[0][1]===b.ingredients[k][1]*a.ingredients[0][1]);
  result.status=equal?"same-strength-not-equivalence":!sameRatio?"different-ratio":"different-strength";
  result.rows=a.ingredients.map((i,k)=>({ingredient:i[0],a:i[1]/a.volume,b:b.ingredients[k][1]/b.volume}));
  return result;
 }
 function initLabelLab(doc){
  const a=doc.getElementById("label-a"),b=doc.getElementById("label-b"),out=doc.getElementById("label-result");
  if(!a||!b||!out)return;
  const titleText={"same-strength-not-equivalence":"غلظت عددی برابر؛ هم‌ارزی یا جایگزینی تأیید نشده","different-ratio":"تفاوت نسبت اجزای ترکیبی؛ جایگزینی تأیید نشده","different-strength":"غلظت متفاوت؛ حجم مصرف قبلی را تکرار نکنید","different-ingredients-or-form":"مقایسه عددی متوقف شد؛ ماده مؤثره یا شکل دارویی متفاوت است."};
  const names={"acetaminophen":"استامینوفن","amoxicillin":"آموکسی‌سیلین","clavulanic-acid":"اسید کلاوولانیک"};
  const number=n=>Number(n).toLocaleString("fa-IR",{maximumFractionDigits:3});
  function render(){
   const result=compareLabels(a.value,b.value);
   out.replaceChildren();
   const h=doc.createElement("strong");h.id="label-status";h.textContent=result?titleText[result.status]:"نمونه نامعتبر؛ مقایسه متوقف شد.";out.append(h);
   out.dataset.result=result?.status||"invalid";
   if(!result)return;
   for(let i=0;i<2;i++){const p=doc.createElement("p");p.textContent=(i===0?"A: ":"B: ")+result.titles[i]+" — "+result.provenance[i];out.append(p)}
   if(result.rows.length){
    const table=doc.createElement("table"),cap=doc.createElement("caption");cap.textContent="قدرت برچسب؛ mg/mL (نه دوز)";table.append(cap);
    const head=doc.createElement("thead"),tr=doc.createElement("tr");for(const text of ["ماده مؤثره","نمونه A","نمونه B"]){const th=doc.createElement("th");th.scope="col";th.textContent=text;tr.append(th)}head.append(tr);table.append(head);
    const body=doc.createElement("tbody");for(const row of result.rows){const tr=doc.createElement("tr"),th=doc.createElement("th");th.scope="row";th.textContent=names[row.ingredient];tr.append(th);for(const n of [row.a,row.b]){const td=doc.createElement("td");td.textContent=number(n);tr.append(td)}body.append(tr)}table.append(body);out.append(table);
   }
   const p=doc.createElement("p");p.textContent="هیچ نتیجه‌ای در این ابزار مجوز درمان یا جایگزینی نیست؛ برچسب و دستور مصرف باید توسط داروساز بررسی شود.";out.append(p);
  }
  a.addEventListener("change",render);b.addEventListener("change",render);a.disabled=b.disabled=false;render();
 }
 function init(doc){
  initLabelLab(doc);
  const connect=doc.getElementById("connect-example"),connectReset=doc.getElementById("connect-reset");
  if(connect&&connectReset){
   const cases=Array.from(doc.querySelectorAll("[data-connect-case]")),status=doc.getElementById("connect-status");
   const showCase=()=>{
    const route=exampleRoute(connect.value);
    for(const item of cases){
     item.hidden=!route||item.dataset.connectCase!==connect.value;
     if(item.hidden)item.querySelectorAll("details").forEach(d=>d.open=false);
    }
    status.setAttribute("role",route==="emergency"?"alert":"status");
    status.setAttribute("aria-live",route==="emergency"?"assertive":"polite");
    status.setAttribute("aria-atomic","true");
    status.textContent=route==="emergency"?"مثال ساختگی اورژانسی: اقدام فوری؛ منتظر مشاوره نمانید.":route==="uncertain"?"مثال ساختگی با اطلاعات ناکافی: ایمنی تأیید نشده است.":route?"نمایش یک مثال ساختگی؛ هیچ پرونده‌ای ارسال نمی‌شود.":"نمونه نامعتبر است؛ هیچ تصمیمی صادر نمی‌شود.";
   };
   connect.addEventListener("change",showCase);
   connectReset.addEventListener("click",()=>{connect.value="label";for(const item of cases)item.querySelectorAll("details").forEach(d=>d.open=false);showCase();connect.focus()});
   connect.disabled=connectReset.disabled=false;showCase();
  }

  const q=doc.getElementById("module-search"),cat=doc.getElementById("module-category"),reset=doc.getElementById("module-reset");
  if(q&&cat&&reset){
   const cards=Array.from(doc.querySelectorAll("[data-entry]"));
   const update=()=>{let count=0;cards.forEach(card=>{card.hidden=!matches(card.textContent,card.dataset.category,q.value,cat.value);if(!card.hidden)count++});doc.getElementById("module-count").textContent=fa(count)+" مورد آموزشی";doc.getElementById("module-empty").hidden=count!==0};
   q.addEventListener("input",update);cat.addEventListener("change",update);
   reset.addEventListener("click",()=>{q.value="";cat.value="all";update();q.focus()});
   q.addEventListener("keydown",e=>{if(e.key==="Escape"){q.value="";update()}});
   q.disabled=cat.disabled=reset.disabled=false;update();
  }
  const ex=doc.getElementById("growth-example"),metric=doc.getElementById("growth-measure");
  if(!ex||!metric)return;
  const chart=doc.getElementById("growth-chart"),rows=doc.getElementById("growth-rows");
  function el(name,attrs,text){const n=doc.createElementNS("http://www.w3.org/2000/svg",name);for(const [k,v] of Object.entries(attrs))n.setAttribute(k,String(v));if(text!==undefined)n.textContent=text;return n}
  function render(){
   const series=growthSeries(ex.value,metric.value);if(!series)return;
   const weight=metric.value==="weight",unit=weight?"وزن (کیلوگرم)":"طول (سانتی‌متر)";
   const upper=weight?12:90,step=weight?3:15,lower=weight?0:45;
   const px=m=>48+m/12*350,py=v=>204-(v-lower)/(upper-lower)*164;
   chart.replaceChildren();
   for(let v=lower;v<=upper;v+=step){chart.append(el("line",{x1:48,x2:398,y1:py(v),y2:py(v),stroke:"#d9e8e5"}),el("text",{x:38,y:py(v)+4,"text-anchor":"end",fill:"#405c67","font-size":12},fa(v)))}
   for(let m=0;m<=12;m+=2)chart.append(el("text",{x:px(m),y:224,"text-anchor":"middle",fill:"#405c67","font-size":12},fa(m)));
   chart.append(el("text",{x:222,y:244,"text-anchor":"middle",fill:"#405c67","font-size":12},"سن (ماه)"));
   chart.append(el("polyline",{points:series.map(x=>px(x.month)+","+py(x.value)).join(" "),fill:"none",stroke:"#08636c","stroke-width":3,"stroke-linejoin":"round"}));
   for(const d of series)chart.append(el("circle",{cx:px(d.month),cy:py(d.value),r:5,fill:"#08636c",stroke:"#fff","stroke-width":1.5}));
   chart.setAttribute("aria-label","نمودار داده ساختگی؛ "+unit+"؛ مقادیر دقیق در جدول زیر");
   rows.replaceChildren(...series.map(x=>{const tr=doc.createElement("tr");for(const value of [x.month,x.value]){const td=doc.createElement("td");td.textContent=fa(value);tr.append(td)}return tr}));
   doc.getElementById("growth-unit").textContent=unit;
   doc.getElementById("growth-caption").textContent=ex.options[ex.selectedIndex].textContent+" (ساختگی) — "+unit;
  }
  ex.addEventListener("change",render);metric.addEventListener("change",render);ex.disabled=metric.disabled=false;render();
 }
 return {normalize,matches,growthSeries,exampleRoute,compareLabels,init};
});
