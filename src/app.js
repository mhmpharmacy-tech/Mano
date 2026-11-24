/*
 * MANO Kid Care Home — AI Studio Build web entry.
 * Browser-side review UI only. No APIs, invented schedules or clinical decisions.
 * User-entered health entries remain in memory; only a fixed synthetic QA marker may survive refresh.
 */
(function () {
  "use strict";
  var routes = ["home","profile","symptoms","pharmacist","care","growth","vaccines","medicines","treatment","supplements","alerts","search","tools","addDrug"];
  var state = { route:"home", profile:null, measurements:[], vaccines:[], treatment:[], question:"", symptom:null, symptomInput:{topic:"",age:"",temp:"",danger:[]}, lastMessage:"" };
  var demoKey="mano.qa.fixed-demo.v1", demoActive=false;
  function demoProfile(){return {name:"نمونه ساختگی QA",months:24,birth:""};}
  // Only the literal flag is persisted: never user input, clinical data, or a patient identifier.
  function restoreDemo(){try{if(window.sessionStorage.getItem(demoKey)==="1"){state.profile=demoProfile();demoActive=true;}}catch(_){/* memory-only fallback */}}
  function setDemoMarker(enabled){try{if(enabled){window.sessionStorage.setItem(demoKey,"1");return window.sessionStorage.getItem(demoKey)==="1";}window.sessionStorage.removeItem(demoKey);return window.sessionStorage.getItem(demoKey)===null;}catch(_){return false;}}
  function connectionText(){return navigator.onLine===false?"اتصال اینترنت در دسترس نیست؛ ثبت موقت این صفحه به اینترنت وابسته نیست.":"ثبت موقت مستقل از اینترنت است؛ هیچ همگام‌سازی ابری انجام نمی‌شود.";}
  function refreshConnection(){var el=document.getElementById("profile-connection");if(el)el.textContent=connectionText();}
  restoreDemo();
  var shell = document.querySelector(".shell");
  var home = document.querySelector(".main");
  var nav = document.querySelector(".bottom");
  var host = document.createElement("main");
  host.className = "screen-main";
  host.id = "care-screen";
  host.hidden = true;
  shell.insertBefore(host, nav);

  function esc(v) { return String(v == null ? "" : v).replace(/[&<>"']/g,function(c){return {"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c];}); }
  function fa(v) { return String(v == null ? "" : v).replace(/[0-9]/g,function(c){return "۰۱۲۳۴۵۶۷۸۹"[Number(c)];}); }
  function label(title, desc) { return '<div class="screen-heading"><button type="button" class="back-link" data-route="home" aria-label="بازگشت به خانه">←</button><div class="heading-copy"><div class="screen-eyebrow">MANO Kid · Care Home</div><h1>'+title+'</h1><p>'+desc+'</p></div></div>'; }
  function card(content, extra) { return '<section class="detail-card '+(extra || '')+'">'+content+'</section>'; }
  function note(msg, kind) { return '<div class="care-message '+(kind||'')+'" role="note">'+msg+'</div>'; }
  function btn(text, destination, subtle) { return '<button type="button" class="'+(subtle?'outline-cta':'solid-cta')+'" data-route="'+destination+'">'+text+'</button>'; }
  function blank(text) { return '<div class="blank-state">'+text+'</div>'; }
  function noServer() { return note('این نسخه برای بازبینی طراحی است. داده‌های این صفحه فقط تا زمان تازه‌سازی باز می‌مانند؛ به سرور، پرونده واقعی یا داروساز ارسال نمی‌شوند.','muted'); }
  function headerHint() { return '<div class="screen-disclaimer">پیش‌نمایش داخلی · اتصال بالینی غیرفعال</div>'; }
  function actions() {
    return [
      ["symptoms","بررسی علائم","غربالگری اولیه و علائم هشدار","coral","◉"],
      ["pharmacist","پرسش دارویی","پیش‌نویس پرسش برای داروساز","forest","●"],
      ["treatment","برنامه درمان","ثبت اطلاعات نسخه، بدون محاسبه دوز","gold","▤"],
      ["growth","رشد کودک","ثبت قد و وزن اندازه‌گیری‌شده","sage","↗"],
      ["vaccines","واکسیناسیون","ثبت سوابق؛ بدون موعد ساختگی","blue","✳"],
      ["supplements","مکمل و بهداشت","راهنمای انتخاب محصول","mint","✿"],
      ["medicines","راهنمای دارو","بدون دوز یا فرآورده تأییدنشده","peach","◧"],
      ["profile","پرونده کودک","اطلاعات اولیه در این نشست","slate","◌"]
    ];
  }
  function quickLinks(filter) {
    return '<div class="screen-tools">'+actions().filter(function(item){return !filter || filter(item);}).map(function(item) {
      return '<button type="button" class="screen-tool '+item[3]+'" data-route="'+item[0]+'"><span class="screen-tool-glyph" aria-hidden="true">'+item[4]+'</span><strong>'+item[1]+'</strong><small>'+item[2]+'</small></button>';
    }).join('')+'</div>';
  }

  function renderProfile() {
    var p=state.profile||{};
    return label('پرونده کودک','بدون عکس کودک، با کنترل کامل والدین بر اطلاعات')
      +note('<span id="profile-connection" role="status">'+connectionText()+'</span>','muted')
      +card('<form id="profile-form" class="field-stack"><label>نام مستعار کودک<input type="text" name="name" maxlength="30" value="'+esc(p.name||'')+'" placeholder="مثلاً فرزند من" autocomplete="off" required></label>'
      +'<label>سن دقیق بر حسب ماه<input type="number" inputmode="numeric" min="0" max="216" step="1" name="months" value="'+esc(p.months==null?'':p.months)+'" placeholder="مثلاً ۲۴" required></label>'
      +'<label>تاریخ تولد (اختیاری، فقط برای نمایش)<input type="text" maxlength="30" name="birth" value="'+esc(p.birth||'')+'" placeholder="مثلاً ۱۴۰۳/۰۵/۰۱"></label>'
      +'<button class="solid-cta" type="submit">ثبت در این نشست</button></form>')
      +note('داده‌های واردشده در این فرم با بستن یا تازه‌سازی پاک می‌شود. اطلاعات واقعی کودک را در این پیش‌نمایش وارد نکنید. برای پرونده واقعی اتصال امن و رضایت آگاهانه لازم است.','muted')
      +(!state.profile&&!state.measurements.length&&!state.vaccines.length&&!state.treatment.length&&!state.question&&!state.symptom?card('<h2>آزمون بازیابی پرونده</h2><p class="muted-text">فقط یک پرونده کاملاً ساختگی و ثابت ایجاد می‌شود؛ هیچ اطلاعات واردشده‌ای ذخیره نمی‌شود. نشانگر آزمایشی تا پایان تب مرورگر باقی می‌ماند.</p><button type="button" class="outline-cta" data-action="demo-profile">ایجاد نمونه ساختگی و آزمون بازیابی</button>'):'')
      +card('<div class="line-between"><strong>اطلاعات ثبت‌شده در این نشست</strong><span class="mini-chip">'+(state.profile?(demoActive?'نمونه ساختگی QA':'ثبت موقت'):'خالی')+'</span></div>'
      +(state.profile?'<p class="muted-text">'+esc(p.name)+' · '+fa(p.months)+' ماه</p>':blank('هنوز هیچ پرونده‌ای ثبت نشده است.'))
      +'<button type="button" class="text-danger" data-action="clear-session">پاک‌سازی تمام داده‌های این نشست</button>');
  }

  function renderSymptoms() {
    var result=state.symptom;
    return label('بررسی علائم کودک','پرسش‌های ضروری برای تشخیص علائم هشدار، نه تشخیص بیماری')
      +card('<form class="field-stack" id="symptom-form">'
      +'<label>علامت اصلی<select name="topic" required><option value="">انتخاب کنید</option><option value="تب">تب</option><option value="سرفه یا دشواری تنفس">سرفه یا دشواری تنفس</option><option value="آبریزش یا گرفتگی بینی">آبریزش یا گرفتگی بینی</option><option value="چشم قرمز یا ترشح چشم">چشم قرمز یا ترشح چشم</option><option value="استفراغ یا اسهال">استفراغ یا اسهال</option><option value="سایر">سایر</option></select></label>'
      +'<div class="form-two"><label>سن کودک (ماه)<input name="age" type="number" min="0" max="216" inputmode="numeric" step="1" value="'+esc(state.profile?state.profile.months:'')+'" placeholder="سن دقیق" required></label>'
      +'<label>دمای اندازه‌گیری‌شده (°C)<input name="temp" type="number" min="30" max="44" step="0.1" inputmode="decimal" placeholder="اختیاری"></label></div>'
      +'<fieldset class="flags"><legend>آیا هرکدام از این علائم وجود دارد؟</legend>'
      +'<label><input type="checkbox" name="danger" value="breath"> دشواری شدید در تنفس یا کبودی</label>'
      +'<label><input type="checkbox" name="danger" value="seizure"> تشنج</label>'
      +'<label><input type="checkbox" name="danger" value="conscious"> کاهش هوشیاری یا پاسخ‌ندادن</label>'
      +'<label><input type="checkbox" name="danger" value="severe"> وضعیت عمومی بسیار بد یا علائم شدید نگران‌کننده</label></fieldset>'
      +'<button class="solid-cta" type="submit">بررسی علائم هشدار</button></form>')
      +(result?card('<h2>نتیجه غربالگری اولیه</h2>'
      +note(result.urgent?'یک یا چند نشانه پرخطر انتخاب یا ثبت شده است. ارزیابی فوری پزشکی لازم است. در شرایط تهدیدکننده حیات فوراً با خدمات اورژانس محلی تماس بگیرید.':'براساس پاسخ‌های واردشده، این ابزار اجازه رد خطر یا تشخیص بیماری را ندارد. اگر وضعیت کودک نگران‌کننده است با پزشک یا داروساز مشورت کنید.', result.urgent?'danger':'caution')
      +(result.reasons.length?'<p class="muted-text">دلیل هشدار: '+esc(result.reasons.join('، '))+'</p>':'')
      +btn('پرسش دارویی غیرفوری','pharmacist',true)):'')
      +note('به‌ویژه در شیرخوار زیر ۳ ماه، دمای ۳۸°C یا بالاتر نیازمند ارزیابی فوری پزشکی است. این فرم جایگزین ویزیت نیست.','danger')
      +noServer();
  }

  function renderPharmacist() {
    return label('پرسش از داروساز','تفکیک روشن پیش‌نویس از مشاوره انسانی تأییدشده')
      +note('ارسال واقعی غیرفعال است. هیچ داروساز آن‌لاین یا پاسخ بالینی خودکار در این پیش‌نمایش وجود ندارد.','caution')
      +card('<form id="question-form" class="field-stack"><label>موضوع پرسش<select name="category" required><option value="دارو و فرآورده">دارو و فرآورده</option><option value="ایمنی مصرف">ایمنی مصرف</option><option value="مکمل و بهداشت">مکمل و بهداشت</option><option value="پیگیری پس از مشاوره">پیگیری پس از مشاوره</option></select></label>'
      +'<label>پرسش خود را بنویسید<textarea name="question" rows="5" maxlength="1200" placeholder="برای پیش‌نمایش از اطلاعات واقعی بیمار استفاده نکنید." required>'+esc(state.question)+'</textarea></label>'
      +'<button class="solid-cta" type="submit">نگه‌داشتن پیش‌نویس در این نشست</button></form>')
      +(state.question?card('<h2>پیش‌نویس ثبت‌شده</h2><p class="muted-text">'+esc(state.question)+'</p><span class="mini-chip">ارسال نشده</span>'):'')
      +noServer();
  }

  function renderGrowth() {
    return label('رشد کودک','ثبت اندازه‌گیری‌های واقعی؛ بدون درصدک ساختگی')
      +card('<form id="growth-form" class="field-stack"><div class="form-two"><label>وزن (kg)<input type="number" name="kg" min="0.5" max="250" step="0.01" inputmode="decimal" placeholder="مثلاً ۱۲٫۵"></label><label>قد (cm)<input type="number" name="cm" min="20" max="230" step="0.1" inputmode="decimal" placeholder="مثلاً ۸۵"></label></div><p class="field-hint">حداقل یکی از دو اندازه را وارد کنید.</p><button type="submit" class="solid-cta">ثبت موقت اندازه‌گیری</button></form>')
      +card('<h2>سوابق این نشست</h2>'+(state.measurements.length?
       '<div class="records">'+state.measurements.map(function(m){return '<div class="record-line"><strong>'+esc(m.when)+'</strong><span>'+(m.kg?fa(m.kg)+' کیلوگرم':'')+(m.cm?' · '+fa(m.cm)+' سانتی‌متر':'')+'</span></div>';}).join('')+'</div>'
       :blank('هنوز اندازه‌ای ثبت نشده است.')) )
      +note('برای تفسیر دقیق رشد به سن، جنس مناسب نمودار، اندازه‌گیری صحیح و استاندارد رسمی WHO نیاز است. این پیش‌نمایش وضعیت طبیعی یا غیرطبیعی اعلام نمی‌کند.','caution')
      +noServer();
  }

  function renderVaccines() {
    return label('واکسیناسیون','ثبت سوابق والدین؛ بدون محاسبه خودکار موعد')
      +card('<form class="field-stack" id="vaccine-form"><label>نام واکسن ثبت‌شده<input type="text" name="name" maxlength="70" placeholder="نام درج‌شده در کارت واکسن" required></label><label>تاریخ تزریق ثبت‌شده<input type="date" name="date" required></label><button type="submit" class="solid-cta">ثبت موقت سابقه</button></form>')
      +card('<h2>سوابق واردشده</h2>'+(state.vaccines.length?'<div class="records">'+state.vaccines.map(function(x){return '<div class="record-line"><strong>'+esc(x.name)+'</strong><span dir="ltr">'+esc(x.date)+'</span></div>';}).join('')+'</div>':blank('هنوز سوابقی ثبت نشده است.')))
      +note('این موارد خوداظهاری‌اند و تأیید نشده‌اند. تعیین واکسن بعدی به سن دقیق، کشور، برنامه رسمی و تاریخچه تأییدشده نیاز دارد.','caution')+noServer();
  }

  function renderTreatment() {
    return label('برنامه مراقبت و درمان','ثبت اطلاعات نسخه؛ بدون تولید دوز یا یادآوری ساختگی')
      +card('<form class="field-stack" id="treatment-form"><label>نام دارو یا فرآورده<input name="medicine" required maxlength="90" placeholder="عین نام و قدرت روی نسخه" autocomplete="off"></label><label>دستور ثبت‌شده در نسخه<input name="directions" required maxlength="220" placeholder="فقط آنچه داروساز یا پزشک تأیید کرده است"></label><button class="solid-cta" type="submit">افزودن به پیش‌نویس این نشست</button></form>')
      +card('<h2>داروهای ثبت‌شده</h2>'+(state.treatment.length?'<div class="records">'+state.treatment.map(function(m){return '<div class="record-line"><strong>'+esc(m.name)+'</strong><span>'+esc(m.directions)+'</span></div>';}).join('')+'</div>':blank('هیچ دارویی ثبت نشده است.')))
      +note('این فهرست نسخه پزشکی، تأیید داروساز، محاسبه دوز یا دستور اجرایی نیست. ذخیره ابری، یادآور و تأیید بالینی در این پیش‌نمایش وجود ندارد.','caution')+noServer();
  }

  function renderMedicines() {
    return label('راهنمای اطلاعات دارویی','ورود از مسیر ایمن؛ بدون بانک دارویی ساختگی')
      +card('<h2>اطلاعات دارویی کودک</h2><p class="muted-text">در این نسخه بانک فرآورده‌های ایران، دوز وزنی یا تداخل‌های دارویی هنوز به سرویس معتبر متصل نشده‌اند. هیچ دوز یا مشخصات فرآورده‌ای حدس زده نمی‌شود.</p>'
      +'<div class="button-stack">'+btn('ثبت اطلاعات نسخه','treatment')+btn('ثبت پرسش برای داروساز','pharmacist',true)+'</div>')
      +note('برای تصمیم درباره دارو باید نام فرآورده، غلظت، شکل دارویی، سن و وزن معتبر، داروهای هم‌زمان، حساسیت‌ها و شرایط بالینی بررسی شوند.','caution');
  }

  function renderSupplements() {
    return label('مکمل‌ها و بهداشت کودک','انتخاب آگاهانه با راهنمایی داروساز')
      +card('<h2>دسته‌بندی‌های کاربردی</h2><div class="records"><div class="record-line"><strong>مکمل‌های تغذیه‌ای</strong><span>فرمولاسیون، سن مجاز، ضرورت مصرف و تداخل‌ها</span></div><div class="record-line"><strong>پوست و بهداشت</strong><span>ترکیبات، تحمل‌پذیری، شرایط استفاده</span></div><div class="record-line"><strong>بهداشت دهان و وسایل کودک</strong><span>انتخاب متناسب با سن و نیاز</span></div></div>'
      +btn('پرسش تخصصی از داروساز','pharmacist'))
      +note('برندها، موجودی و اثربخشی محصولات تا زمان تطبیق با داده رسمی و بررسی انسانی نمایش داده نمی‌شوند.','muted');
  }

  function renderSearch() {
    return label('جست‌وجوی بخش‌ها','جست‌وجوی واقعی امکانات برنامه، نه بانک دارویی متصل‌نشده')
      +card('<label class="field-stack">نام بخش<input id="section-search" type="search" placeholder="مثلاً رشد، واکسن، دارو" autocomplete="off" aria-label="جستجوی بخش‌های برنامه"></label>')
      +'<div id="search-results">'+quickLinks()+'</div>';
  }

  function renderAlerts() {
    return label('اعلان‌ها','یادآورها فقط با برنامه معتبر و رضایت والد فعال می‌شوند')
      +card(blank('اعلان فعالی وجود ندارد. این نسخه به سرویس اعلان و زمان‌بندی متصل نیست.'))
      +btn('مشاهده برنامه درمان','treatment')+noServer();
  }

  function renderTools() {
    return label('ابزارهای MANO Kid','مسیرهای کوتاه و ساده برای نیازهای واقعی خانواده')
       +quickLinks()+note('هر بخش به‌صورت مستقل باز می‌شود. داده‌های این نسخه با پرونده واقعی همگام نمی‌شوند.','muted');
  }

  function renderScreen(key) {
    switch(key) {
      case "profile":return renderProfile();
      case "symptoms":return renderSymptoms();
      case "pharmacist":return renderPharmacist();
      case "growth":return renderGrowth();
      case "vaccines":return renderVaccines();
      case "medicines":return renderMedicines();
      case "care":case "treatment":case "addDrug":return renderTreatment();
      case "supplements":return renderSupplements();
      case "search":return renderSearch();
      case "alerts":return renderAlerts();
      default:return renderTools();
    }
  }

  function syncHome() {
    var p=state.profile;
    var name=document.querySelector(".profilename");
    var sub=document.querySelector(".profilesub");
    if(name) name.textContent=p?(p.name+' · پرونده کودک'):'پرونده کودک';
    if(sub) sub.textContent=p?(fa(p.months)+' ماه · '+(demoActive?'نمونه ساختگی QA':'ثبت موقت در این نشست')):'برای شخصی‌سازی، اطلاعات کودک را وارد کنید';
    var details=document.querySelectorAll(".caredesc");
    if(details[0]) details[0].textContent=state.treatment.length?fa(state.treatment.length)+' مورد در پیش‌نویس درمان این نشست':'هنوز دارویی ثبت نشده است.';
    if(details[1]) details[1].textContent=state.vaccines.length?fa(state.vaccines.length)+' سابقه خوداظهاری در این نشست':'نمایش موعد بعدی فقط پس از تکمیل سوابق معتبر';
    var tag=document.getElementById("preview-tag");if(tag)tag.textContent="پیش‌نمایش داخلی";
  }

  function updateBottom(key) {
    var map={home:"home",profile:"profile",growth:"growth",pharmacist:"pharmacist",care:"care",treatment:"care",addDrug:"care",vaccines:"care",symptoms:"home",medicines:"pharmacist",supplements:"care",tools:"home",alerts:"home",search:"home"};
    Array.prototype.forEach.call(nav.querySelectorAll("button"),function(tab) {
      var attr=tab.getAttribute("onclick")||"";
      var selected=attr.indexOf("'"+(map[key]||"home")+"'")>=0;
      tab.classList.toggle("sel",selected);
      if(selected)tab.setAttribute("aria-current","page");else tab.removeAttribute("aria-current");
    });
  }
  function render() {
    var key=state.route;
    home.hidden=key!=="home"; host.hidden=key==="home";
    if(key==="home")syncHome();else{
      host.innerHTML=headerHint()+renderScreen(key);
      if(key==="search")wireSearch();
      if(key==="symptoms"){
        var f=host.querySelector("#symptom-form");
        var keep=state.symptomInput;
        if(f&&keep){
          f.elements.namedItem("topic").value=keep.topic||"";
          f.elements.namedItem("age").value=keep.age!==""?keep.age:(state.profile?String(state.profile.months):"");
          f.elements.namedItem("temp").value=keep.temp||"";
          Array.prototype.forEach.call(f.querySelectorAll('input[name="danger"]'),function(x){x.checked=keep.danger.includes(x.value);});
        }
      }
    }
    updateBottom(key);
    window.scrollTo({top:0,behavior:"instant"});
    document.title="MANO Kid — "+(key==="home"?"خانه":key);
  }
  function sanitizeRoute(k) {
    return routes.indexOf(k)>-1?k:"home";
  }
  function go(route) {
    shell.querySelectorAll(".status-toast").forEach(function(t){t.remove();});
    state.route=sanitizeRoute(route);
    var hash=state.route==="home"?"":("#"+state.route);
    if(window.location.hash!==hash)window.location.hash=hash;
    render();
  }
  function wireSearch() {
    var input=document.getElementById("section-search");
    if(!input)return;
    input.addEventListener("input",function(){
      var q=input.value.trim().toLocaleLowerCase("fa");
      document.getElementById("search-results").innerHTML=quickLinks(function(item) {
        return !q || (item[1]+" "+item[2]).toLocaleLowerCase("fa").indexOf(q)!==-1;
      });
    });
  }
  function showMessage(s) {
    var e=document.createElement("div");
    e.className="status-toast";e.textContent=s;e.setAttribute("role","status");
    shell.appendChild(e);
    window.setTimeout(function(){e.remove();},3400);
  }
  function value(form,name){return String(new FormData(form).get(name)||"").trim();}
  host.addEventListener("click",function(event){
    var route=event.target.closest("[data-route]");
    if(route){event.preventDefault();go(route.getAttribute("data-route"));return;}
    var action=event.target.closest("[data-action]");
    if(action&&action.getAttribute("data-action")==="demo-profile"){
      if(state.profile||state.measurements.length||state.vaccines.length||state.treatment.length||state.question||state.symptom)return;
      if(!setDemoMarker(true)){showMessage("ذخیره نشانگر آزمایشی ممکن نیست؛ هیچ پرونده‌ای ایجاد نشد");return;}
      state.profile=demoProfile();demoActive=true;render();showMessage("نمونه ساختگی ثبت شد؛ بازیابی فقط در همین تب");return;
    }
    if(action&&action.getAttribute("data-action")==="clear-session") {
      var markerCleared=setDemoMarker(false);
      demoActive=false;
      state.profile=null;state.treatment=[];state.vaccines=[];state.measurements=[];state.question="";state.symptom=null;state.symptomInput={topic:"",age:"",temp:"",danger:[]};
      render();showMessage(markerCleared?"داده‌های موقت پاک شدند":"حافظه موقت پاک شد؛ حذف نشانگر آزمایشی قابل تأیید نیست، تب را ببندید");
    }
  });
  host.addEventListener("submit",function(event){
    event.preventDefault();var f=event.target;
    if(f.id==="profile-form"){
      var rawMonths=value(f,"months"),months=Number(rawMonths),name=value(f,"name");
      if(!name||rawMonths===""||!Number.isInteger(months)||months<0||months>216){showMessage("سن ماهانه یا نام مستعار معتبر نیست");return;}
      if(demoActive){showMessage("ابتدا نمونه ساختگی و داده‌های نشست را پاک کنید");return;}
      state.profile={name:name,months:months,birth:value(f,"birth")};render();showMessage("پرونده موقت ثبت شد؛ ارسال یا ذخیره ابری انجام نشد");return;
    }
    if(f.id==="symptom-form"){
      var data=new FormData(f), age=Number(data.get("age")), tempS=value(f,"temp"),temp=tempS===""?null:Number(tempS);
      if(!Number.isInteger(age)||age<0||age>216||temp!==null&&(!Number.isFinite(temp)||temp<30||temp>44)){showMessage("سن یا دمای واردشده معتبر نیست");return;}
      var dangers=data.getAll("danger"), reasons=[];
      if(dangers.length)reasons.push("علائم خطر انتخاب‌شده");
      if(age<3&&temp!==null&&temp>=38)reasons.push("تب ۳۸ درجه یا بالاتر در شیرخوار زیر ۳ ماه");
      state.symptomInput={topic:value(f,"topic"),age:String(age),temp:tempS,danger:dangers.slice()};state.symptom={urgent:reasons.length>0,reasons:reasons};render();return;
    }
    if(f.id==="question-form"){state.question=value(f,"question");showMessage("پیش‌نویس فقط در این نشست حفظ شد");render();return;}
    if(f.id==="growth-form"){
      var kgS=value(f,"kg"),cmS=value(f,"cm");
      var kg=kgS?Number(kgS):null,cm=cmS?Number(cmS):null;
      if(kg===null&&cm===null||kg!==null&&(!Number.isFinite(kg)||kg<0.5||kg>250)||cm!==null&&(!Number.isFinite(cm)||cm<20||cm>230)){showMessage("وزن یا قد نامعتبر است");return;}
      state.measurements.unshift({kg:kg,cm:cm,when:new Date().toLocaleDateString("fa-IR")});showMessage("اندازه‌گیری موقت ثبت شد");render();return;
    }
    if(f.id==="vaccine-form"){state.vaccines.unshift({name:value(f,"name"),date:value(f,"date")});showMessage("سابقه خوداظهاری ثبت شد");render();return;}
    if(f.id==="treatment-form"){state.treatment.unshift({name:value(f,"medicine"),directions:value(f,"directions")});showMessage("پیش‌نویس دارویی ثبت شد؛ تأیید بالینی نشده است");render();return;}
  });
  window.show=function(key){go(key);};
  window.hide=function(){go("home");};
  window.switchTab=function(button,key){go(key);};
  window.screenAlert=function(){go("symptoms");};
  window.addEventListener("online",refreshConnection);
  window.addEventListener("offline",refreshConnection);
  window.addEventListener("hashchange",function(){var k=sanitizeRoute(location.hash.replace("#",""));if(state.route!==k){state.route=k;render();}});
  document.addEventListener("keydown",function(e){if(e.key==="Escape"&&state.route!=="home")go("home");});
  state.route=sanitizeRoute(window.location.hash.replace("#",""));
  render();
})();
