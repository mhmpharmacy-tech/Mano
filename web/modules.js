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
 function init(doc){
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
 return {normalize,matches,growthSeries,exampleRoute,init};
});
