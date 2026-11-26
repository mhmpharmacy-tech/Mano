/*
 * MANO Kid Care Home — AI Studio Build web entry.
 * Browser-side review UI only. No APIs, invented schedules or clinical decisions.
 * Health entries are intentionally held in memory; refresh clears them.
 */
(function () {
  "use strict";
  var routes = ["home","profile","symptoms","pharmacist","care","growth","vaccines","medicines","treatment","supplements","alerts","search","tools","addDrug"];
  var state = { route:"home", profile:null, measurements:[], vaccines:[], treatment:[], question:"", symptom:null, symptomInput:{topic:"",age:"",temp:"",danger:[]}, lastMessage:"", suppGroup:"supplements", pharmacistCategory:"دارو و فرآورده", sheetProductId:null };
  var shell = document.querySelector(".shell");
  var home = document.querySelector(".main");
  var nav = document.querySelector(".bottom");
  var cover = document.getElementById("cover");
  var dialog = document.getElementById("dialog");
  var host = document.createElement("main");
  host.className = "screen-main";
  host.id = "care-screen";
  host.hidden = true;
  shell.insertBefore(host, nav);

  var DUO_ICONS = {
    header: '<svg aria-hidden="true" viewBox="0 0 64 64"><rect x="3" y="3" width="58" height="58" rx="17" fill="var(--duo-header-bg)" stroke="var(--duo-header-stroke)" stroke-width="1.6"/><path d="M36 47c15-4 18-26 7-32-11 4-14 22-7 32Z" fill="var(--duo-header-leaf)"/><path d="M24 14c12 13 12 33 0 33s-12-20 0-33Z" fill="var(--duo-surface)" stroke="var(--duo-header-ink)" stroke-width="2.8" stroke-linejoin="round"/><path d="M24 29v11M19 34.5h10" stroke="var(--duo-header-ink)" stroke-width="3.1" stroke-linecap="round"/><circle cx="47" cy="46" r="4.2" fill="var(--duo-header-dot)"/></svg>',
    supplements: '<svg aria-hidden="true" viewBox="0 0 64 64"><rect x="3" y="3" width="58" height="58" rx="17" fill="var(--duo-supp-bg)" stroke="var(--duo-supp-stroke)" stroke-width="1.6"/><circle cx="40" cy="23" r="9" fill="var(--duo-supp-orb)"/><path d="M40 9v3M40 34v3M26 23h3M51 23h3M30 13l2 2M48 31l2 2M50 13l-2 2M32 31l-2 2" stroke="var(--duo-supp-ink)" stroke-width="2.3" stroke-linecap="round"/><path d="M24 21c11 11 11 29 0 29s-11-18 0-29Z" fill="var(--duo-surface)" stroke="var(--duo-supp-ink)" stroke-width="2.8" stroke-linejoin="round"/><circle cx="24" cy="39" r="5.3" fill="var(--duo-supp-orb)"/><circle cx="21.5" cy="36" r="2" fill="var(--duo-supp-shine)"/></svg>',
    hygiene: '<svg aria-hidden="true" viewBox="0 0 64 64"><rect x="3" y="3" width="58" height="58" rx="17" fill="var(--duo-hyg-bg)" stroke="var(--duo-hyg-stroke)" stroke-width="1.6"/><circle cx="27.5" cy="34.5" r="12" fill="var(--duo-surface)" stroke="var(--duo-hyg-ink)" stroke-width="2.5"/><circle cx="27.5" cy="34.5" r="8" fill="var(--duo-hyg-bubble)"/><circle cx="43" cy="23" r="6.4" fill="var(--duo-header-bg)" stroke="var(--duo-hyg-ink)" stroke-width="2"/><circle cx="44" cy="41" r="4" fill="var(--duo-hyg-bubble)"/></svg>',
    rec_vit_d3: '<svg aria-hidden="true" viewBox="0 0 64 64"><rect x="3" y="3" width="58" height="58" rx="17" fill="var(--duo-vitd-bg)" stroke="var(--duo-vitd-stroke)" stroke-width="1.6"/><circle cx="40" cy="23" r="9" fill="var(--duo-vitd-orb)"/><path d="M40 9v3M40 34v3M26 23h3M51 23h3M30 13l2 2M48 31l2 2M50 13l-2 2M32 31l-2 2" stroke="var(--duo-vitd-ink)" stroke-width="2.3" stroke-linecap="round"/><path d="M24 21c11 11 11 29 0 29s-11-18 0-29Z" fill="var(--duo-surface)" stroke="var(--duo-vitd-ink)" stroke-width="2.8" stroke-linejoin="round"/><circle cx="24" cy="39" r="5.3" fill="var(--duo-vitd-orb)"/><circle cx="21.5" cy="36" r="2" fill="var(--duo-vitd-shine)"/></svg>',
    rec_iron_drops: '<svg aria-hidden="true" viewBox="0 0 64 64"><rect x="3" y="3" width="58" height="58" rx="17" fill="var(--duo-iron-bg)" stroke="var(--duo-iron-stroke)" stroke-width="1.6"/><rect x="16" y="27" width="23" height="23" rx="6" fill="var(--duo-surface)" stroke="var(--duo-iron-ink)" stroke-width="2.6"/><rect x="17.5" y="36" width="20" height="12.5" rx="4.2" fill="var(--duo-iron-fill)"/><rect x="22" y="20" width="11" height="7" rx="2" fill="var(--duo-iron-ink)"/><rect x="24" y="13" width="7" height="8" rx="3.5" fill="var(--duo-iron-fill)"/><path d="M46 28c7 8 7 19 0 19s-7-11 0-19Z" fill="var(--duo-iron-ink)"/><circle cx="46" cy="18" r="2.7" fill="var(--duo-iron-orb)"/></svg>',
    rec_probiotic: '<svg aria-hidden="true" viewBox="0 0 64 64"><rect x="3" y="3" width="58" height="58" rx="17" fill="var(--duo-prob-bg)" stroke="var(--duo-prob-stroke)" stroke-width="1.6"/><rect x="15" y="22" width="34" height="19" rx="9.5" fill="var(--duo-surface)" stroke="var(--duo-prob-ink)" stroke-width="2.6"/><path d="M32 22h7.5a9.5 9.5 0 0 1 0 19H32Z" fill="var(--duo-prob-fill)"/><path d="M32 22v19" stroke="var(--duo-prob-ink)" stroke-width="2.3"/><circle cx="20" cy="15" r="4.4" fill="var(--duo-prob-flora)"/><circle cx="44" cy="49" r="3" fill="var(--duo-prob-ink)"/><circle cx="29" cy="49" r="3.6" fill="var(--duo-prob-fill)"/></svg>',
    rec_zinc_cream: '<svg aria-hidden="true" viewBox="0 0 64 64"><rect x="3" y="3" width="58" height="58" rx="17" fill="var(--duo-zinc-bg)" stroke="var(--duo-zinc-stroke)" stroke-width="1.6"/><path d="M15 16h20l-3 25H18Z" fill="var(--duo-surface)" stroke="var(--duo-zinc-ink)" stroke-width="2.5" stroke-linejoin="round"/><path d="M21 24h8" stroke="var(--duo-zinc-fill)" stroke-width="3.8" stroke-linecap="round"/><rect x="20.5" y="41" width="9" height="7" rx="2" fill="var(--duo-zinc-ink)"/><path d="M43 23l9.5 3.8-1.2 11.5C50 44.5 43 49 43 49s-7-4.5-8.3-10.7L33.5 26.8Z" fill="var(--duo-zinc-fill)" stroke="var(--duo-zinc-ink)" stroke-width="2.3" stroke-linejoin="round"/><circle cx="43" cy="16.5" r="2.9" fill="var(--duo-zinc-orb)"/></svg>',
    rec_gentle_wash: '<svg aria-hidden="true" viewBox="0 0 64 64"><rect x="3" y="3" width="58" height="58" rx="17" fill="var(--duo-wash-bg)" stroke="var(--duo-wash-stroke)" stroke-width="1.6"/><path d="M28 15v8M22 15h14" stroke="var(--duo-wash-ink)" stroke-width="3.4" stroke-linecap="round"/><rect x="17" y="23" width="23" height="27" rx="9" fill="var(--duo-surface)" stroke="var(--duo-wash-ink)" stroke-width="2.6"/><rect x="19.5" y="31" width="18" height="14" rx="5" fill="var(--duo-wash-fill)"/><circle cx="46" cy="24" r="4.8" fill="var(--duo-wash-bubble)" stroke="var(--duo-wash-ink)" stroke-width="1.7"/><circle cx="49" cy="36" r="3.5" fill="var(--duo-wash-fill)"/></svg>',
    rec_oral_care: '<svg aria-hidden="true" viewBox="0 0 64 64"><rect x="3" y="3" width="58" height="58" rx="17" fill="var(--duo-oral-bg)" stroke="var(--duo-oral-stroke)" stroke-width="1.6"/><path d="M19 23c0-7 10-6.5 12-2.5 2-4 12-4.5 12 2.5.6 10.8-2.5 24-7.6 24-2.5 0-3-9.5-4.4-9.5S29 47 26.6 47C21.5 47 18.4 33.8 19 23Z" fill="var(--duo-surface)" stroke="var(--duo-oral-ink)" stroke-width="2.6" stroke-linejoin="round"/><rect x="13" y="38" width="15" height="9" rx="3.2" fill="var(--duo-oral-fill)"/><circle cx="46" cy="17" r="4.2" fill="var(--duo-oral-spark)"/></svg>'
  };

  var SUPPLEMENTS_HYGIENE_GROUPS = [
    { id: "supplements", label: "مکمل‌ها", count: "۳ مورد", icon: "supplements", pharmacistCategory: "مکمل و بهداشت" },
    { id: "hygiene", label: "بهداشت", count: "۳ مورد", icon: "hygiene", pharmacistCategory: "مکمل و بهداشت" }
  ];

  var SUPPLEMENTS_HYGIENE_PRODUCTS = [
    {
      id: "rec_vit_d3",
      group: "supplements",
      title: "قطره ویتامین A+D یا D3 کودک",
      subtitle: "حمایت از رشد استخوان و ایمنی • بدون افزودنی رنگی",
      categoryTag: "مکمل پایه",
      ageHint: "از ۱۵ روزگی",
      icon: "rec_vit_d3",
      tips: [
        "فرمولاسیون استاندارد با قطره‌چکان مدرج انتخاب کنید تا مقدار روزانه دقیق اندازه‌گیری شود.",
        "در صورت مصرف شیرخشک غنی‌شده یا مولتی‌ویتامین هم‌زمان، مجموع ویتامین A و D دریافتی را با داروساز تطبیق دهید.",
        "بطری را در جای خشک، خنک و دور از نور مستقیم آفتاب نگهداری کنید."
      ]
    },
    {
      id: "rec_iron_drops",
      group: "supplements",
      title: "قطره آهن ملایم (لیپوزومال / سوکروزومیال)",
      subtitle: "کمک به پیشگیری از کم‌خونی با کمترین لک دندانی",
      categoryTag: "مکمل آهن",
      ageHint: "۶ تا ۲۴ ماه",
      icon: "rec_iron_drops",
      tips: [
        "غلظت آهن المنتال در هر میلی‌لیتر روی برچسب هر فرآورده متفاوت است؛ پیش از شروع با داروساز بررسی کنید.",
        "قطره را در عقب زبان بچکانید، بلافاصله کمی آب بدهید و با فاصله ۱ تا ۲ ساعته از لبنیات مصرف شود."
      ]
    },
    {
      id: "rec_probiotic",
      group: "supplements",
      title: "قطره پروبیوتیک شیرخوار و کودک",
      subtitle: "کمک به تعادل فلور روده و آرامش گوارش",
      categoryTag: "گوارش کودک",
      ageHint: "نوزادی تا ۶ سال",
      icon: "rec_probiotic",
      tips: [
        "به شرایط نگهداری (یخچالی یا دمای اتاق زیر ۲۵ درجه) و مهلت مصرف پس از باز شدن بطری دقت کنید.",
        "قطره را داخل شیر یا غذای داغ نریزید تا باکتری‌های مفید زنده بمانند."
      ]
    },
    {
      id: "rec_zinc_cream",
      group: "hygiene",
      title: "کرم محافظ پای کودک (زینک اکساید بدون عطر)",
      subtitle: "لایه محافظت‌کننده پوست در برابر رطوبت و التهاب پوشک",
      categoryTag: "بهداشت پوست",
      ageHint: "بدو تولد+",
      icon: "rec_zinc_cream",
      tips: [
        "محصول فاقد اسانس، الکل و پارابن را روی پوست کاملاً خشک به‌صورت لایه نازک استفاده کنید.",
        "در صورت قرمزی شدید یا دانه‌های حاشیه‌دار بیش از ۳ روز، برای بررسی قارچ با داروساز یا پزشک مشورت کنید."
      ]
    },
    {
      id: "rec_gentle_wash",
      group: "hygiene",
      title: "شامپو و شوینده ملایم سر و بدن کودک",
      subtitle: "فرمول بدون سولفات و بدون اشک • سازگار با پوست حساس",
      categoryTag: "حمام و مو",
      ageHint: "نوزادی تا ۶ سال",
      icon: "rec_gentle_wash",
      tips: [
        "شوینده‌های سندت (Syndet) با pH متعادل پوست کودک از خشکی و خارش جلوگیری می‌کنند.",
        "بلافاصله پس از حمام کوتاه با آب ولرم، از مرطوب‌کننده ساده و بدون عطر استفاده نمایید."
      ]
    },
    {
      id: "rec_oral_care",
      group: "hygiene",
      title: "مسواک انگشتی و خمیردندان فلورایددار کودک",
      subtitle: "مراقبت از اولین دندان شیری و پیشگیری از پوسیدگی",
      categoryTag: "دهان و دندان",
      ageHint: "از اولین دندان",
      icon: "rec_oral_care",
      tips: [
        "تا ۳ سالگی مقدار خمیردندان به‌اندازه «یک دانه برنج» و از ۳ تا ۶ سالگی به‌اندازه «یک نخود کوچک» کافی است.",
        "شیشه‌شیر شبانه هنگام خواب مهم‌ترین عامل پوسیدگی دندان‌های شیری است."
      ]
    }
  ];

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
      +card('<form id="profile-form" class="field-stack"><label>نام مستعار کودک<input type="text" name="name" maxlength="30" value="'+esc(p.name||'')+'" placeholder="مثلاً فرزند من" autocomplete="off" required></label>'
      +'<label>سن دقیق بر حسب ماه<input type="number" inputmode="numeric" min="0" max="216" step="1" name="months" value="'+esc(p.months==null?'':p.months)+'" placeholder="مثلاً ۲۴" required></label>'
      +'<label>تاریخ تولد (اختیاری، فقط برای نمایش)<input type="text" maxlength="30" name="birth" value="'+esc(p.birth||'')+'" placeholder="مثلاً ۱۴۰۳/۰۵/۰۱"></label>'
      +'<button class="solid-cta" type="submit">ثبت در این نشست</button></form>')
      +note('ثبت این فرم در حافظه موقت همین صفحه انجام می‌شود و با بستن یا تازه‌سازی پاک می‌شود. برای پرونده واقعی اتصال امن و رضایت آگاهانه لازم است.','muted')
      +card('<div class="line-between"><strong>اطلاعات ثبت‌شده در این نشست</strong><span class="mini-chip">'+(state.profile?'ثبت موقت':'خالی')+'</span></div>'
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
    var catOptions = ["دارو و فرآورده","ایمنی مصرف","مکمل و بهداشت","پیگیری پس از مشاوره"].map(function(opt){
      return '<option value="'+opt+'"'+(state.pharmacistCategory===opt?' selected':'')+'>'+opt+'</option>';
    }).join('');
    return label('پرسش از داروساز','تفکیک روشن پیش‌نویس از مشاوره انسانی تأییدشده')
      +note('ارسال واقعی غیرفعال است. هیچ داروساز آن‌لاین یا پاسخ بالینی خودکار در این پیش‌نمایش وجود ندارد.','caution')
      +card('<form id="question-form" class="field-stack"><label>موضوع پرسش<select name="category" required>'+catOptions+'</select></label>'
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

  function renderProductRowsHtml(groupId) {
    return SUPPLEMENTS_HYGIENE_PRODUCTS.filter(function(p){return p.group===groupId;}).map(function(p){
      return '<button type="button" class="supp-row" data-product="'+p.id+'" aria-label="'+esc(p.title)+' — '+esc(p.ageHint)+'">'
        +'<span class="supp-row-art">'+DUO_ICONS[p.icon]+'</span>'
        +'<span class="supp-row-body"><span class="supp-row-title" style="display:block">'+esc(p.title)+'</span><span class="supp-row-sub" style="display:block">'+esc(p.subtitle)+'</span></span>'
        +'<span class="supp-row-meta"><span class="supp-age'+(p.group==="hygiene"?' hygiene':'')+'">'+esc(p.ageHint)+'</span><span class="supp-chevron" aria-hidden="true">‹</span></span>'
        +'</button>';
    }).join('');
  }

  function renderSupplementsHomeCard() {
    var container = document.getElementById("supplements-home-card");
    if (!container) return;
    var activeGroup = state.suppGroup === "hygiene" ? "hygiene" : "supplements";
    var verifiedSvg = '<svg class="supp-verified" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M12 2l2.4 2.1 3.2-.3.9 3.1 2.8 1.5-.8 3.1 1.7 2.8-2.3 2.2.2 3.2-3.1.8-1.6 2.8-3.1-.8-3.1.8-1.6-2.8-3.1-.8.2-3.2-2.3-2.2 1.7-2.8-.8-3.1 2.8-1.5.9-3.1 3.2.3L12 2zm-1.2 13.2l5.2-5.2-1.4-1.4-3.8 3.8-1.8-1.8-1.4 1.4 3.2 3.2z"/></svg>';
    var pharmacyIcon = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 7v10M7 12h10"/><rect x="3" y="3" width="18" height="18" rx="5"/></svg>';
    var tabsHtml = SUPPLEMENTS_HYGIENE_GROUPS.map(function(g){
      var sel = g.id === activeGroup;
      return '<button type="button" class="supp-tab'+(sel?' active':'')+'" role="tab" aria-selected="'+(sel?'true':'false')+'" data-supp-group="'+g.id+'">'
        +DUO_ICONS[g.icon]
        +'<span>'+g.label+'</span>'
        +'<span class="supp-count">'+g.count+'</span>'
        +'</button>';
    }).join('');
    container.innerHTML =
      '<div class="supp-header">'
        +'<div class="supp-header-art">'+DUO_ICONS.header+'</div>'
        +'<div class="supp-header-copy">'
          +'<div class="supp-title-row"><h2>مکمل‌ها و بهداشت کودک</h2>'+verifiedSvg+'</div>'
          +'<p class="supp-subtitle">دسته‌بندی ایمن روزمره • برای نکات انتخاب لمس کنید</p>'
        +'</div>'
        +'<button type="button" class="supp-all-btn" data-supp-all="1">همه راهنماها</button>'
      +'</div>'
      +'<div class="supp-tabs" role="tablist" aria-label="دسته‌بندی مکمل و بهداشت">'+tabsHtml+'</div>'
      +'<div class="supp-list" role="tabpanel">'+renderProductRowsHtml(activeGroup)+'</div>'
      +'<div class="supp-footer">'
        +'<button type="button" class="solid-cta supp-cta" data-supp-pharmacist="1">'+pharmacyIcon+'<span>پرسش از داروساز</span></button>'
      +'</div>';
  }

  function openProductSheet(productId) {
    var product = SUPPLEMENTS_HYGIENE_PRODUCTS.find(function(item){return item.id===productId;});
    if (!product || !cover || !dialog) return;
    state.sheetProductId = product.id;
    var pharmacyIcon = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 7v10M7 12h10"/><rect x="3" y="3" width="18" height="18" rx="5"/></svg>';
    dialog.innerHTML =
      '<div class="sheet-handle" aria-hidden="true"></div>'
      +'<div class="sheet-head">'
        +'<div class="sheet-art">'+DUO_ICONS[product.icon]+'</div>'
        +'<div class="sheet-head-copy">'
          +'<h3 id="sheet-title">'+esc(product.title)+'</h3>'
          +'<div class="sheet-chips"><span class="sheet-chip">'+esc(product.categoryTag)+'</span><span class="supp-age'+(product.group==="hygiene"?' hygiene':'')+'">'+esc(product.ageHint)+'</span></div>'
        +'</div>'
        +'<button type="button" class="sheet-close" data-sheet-close="1" aria-label="بستن">×</button>'
      +'</div>'
      +'<p class="sheet-sub">'+esc(product.subtitle)+'</p>'
      +'<div class="sheet-tips"><strong>نکات ایمن انتخاب و مصرف</strong><ul>'+product.tips.map(function(t){return '<li>'+esc(t)+'</li>';}).join('')+'</ul></div>'
      +'<div class="sheet-disclaimer" role="note">برند تجاری یا دوز اختصاصی در این کارت تعیین نمی‌شود؛ پیش از شروع یا ترکیب مکمل‌ها با داروساز مشورت کنید.</div>'
      +'<button type="button" class="solid-cta supp-cta" data-sheet-pharmacist="1">'+pharmacyIcon+'<span>پرسش از داروساز درباره این محصول</span></button>';
    cover.classList.add("show");
  }

  function closeSheet() {
    if (!cover) return;
    cover.classList.remove("show");
    state.sheetProductId = null;
  }

  function renderSupplements() {
    return label('مکمل‌ها و بهداشت کودک','انتخاب آگاهانه با راهنمایی داروساز و آیکون‌های تفکیک‌شده')
      +card('<h2>مکمل‌های تغذیه‌ای کودک</h2><div class="supp-list" style="padding:0">'+renderProductRowsHtml("supplements")+'</div>')
      +card('<h2>بهداشت پوست، مو و دهان</h2><div class="supp-list" style="padding:0">'+renderProductRowsHtml("hygiene")+'</div>')
      +card('<button type="button" class="solid-cta supp-cta" data-supp-pharmacist="1">پرسش تخصصی از داروساز</button>')
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
    if(sub) sub.textContent=p?(fa(p.months)+' ماه · ثبت موقت در این نشست'):'برای شخصی‌سازی، اطلاعات کودک را وارد کنید';
    var details=document.querySelectorAll(".caredesc");
    if(details[0]) details[0].textContent=state.treatment.length?fa(state.treatment.length)+' مورد در پیش‌نویس درمان این نشست':'هنوز دارویی ثبت نشده است.';
    if(details[1]) details[1].textContent=state.vaccines.length?fa(state.vaccines.length)+' سابقه خوداظهاری در این نشست':'نمایش موعد بعدی فقط پس از تکمیل سوابق معتبر';
    var tag=document.getElementById("preview-tag");if(tag)tag.textContent="پیش‌نمایش داخلی";
    renderSupplementsHomeCard();
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
    var productBtn=event.target.closest("[data-product]");
    if(productBtn){event.preventDefault();openProductSheet(productBtn.getAttribute("data-product"));return;}
    var pharmBtn=event.target.closest("[data-supp-pharmacist]");
    if(pharmBtn){event.preventDefault();state.pharmacistCategory="مکمل و بهداشت";go("pharmacist");return;}
    var route=event.target.closest("[data-route]");
    if(route){event.preventDefault();go(route.getAttribute("data-route"));return;}
    var action=event.target.closest("[data-action]");
    if(action&&action.getAttribute("data-action")==="clear-session") {
      state.profile=null;state.treatment=[];state.vaccines=[];state.measurements=[];state.question="";state.symptom=null;state.symptomInput={topic:"",age:"",temp:"",danger:[]};state.pharmacistCategory="دارو و فرآورده";
      showMessage("داده‌های موقت پاک شدند");render();
    }
  });
  var suppHomeCard=document.getElementById("supplements-home-card");
  if(suppHomeCard){
    suppHomeCard.addEventListener("click",function(event){
      var tab=event.target.closest("[data-supp-group]");
      if(tab){state.suppGroup=tab.getAttribute("data-supp-group");renderSupplementsHomeCard();return;}
      var productBtn=event.target.closest("[data-product]");
      if(productBtn){openProductSheet(productBtn.getAttribute("data-product"));return;}
      if(event.target.closest("[data-supp-all]")){go("supplements");return;}
      if(event.target.closest("[data-supp-pharmacist]")){state.pharmacistCategory="مکمل و بهداشت";go("pharmacist");return;}
    });
  }
  if(dialog){
    dialog.addEventListener("click",function(event){
      if(event.target.closest("[data-sheet-close]")){closeSheet();return;}
      if(event.target.closest("[data-sheet-pharmacist]")){
        closeSheet();
        state.pharmacistCategory="مکمل و بهداشت";
        go("pharmacist");
      }
    });
  }
  host.addEventListener("submit",function(event){
    event.preventDefault();var f=event.target;
    if(f.id==="profile-form"){
      var months=Number(value(f,"months"));var name=value(f,"name");
      if(!name||!Number.isInteger(months)||months<0||months>216){showMessage("سن ماهانه یا نام مستعار معتبر نیست");return;}
      state.profile={name:name,months:months,birth:value(f,"birth")};showMessage("پرونده موقت ثبت شد");render();return;
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
  window.show=function(key){closeSheet();go(key);};
  window.hide=function(){if(cover&&cover.classList.contains("show")){closeSheet();}else{go("home");}};
  window.switchTab=function(button,key){closeSheet();if(key==="pharmacist"){state.pharmacistCategory="دارو و فرآورده";}go(key);};
  window.screenAlert=function(){closeSheet();go("symptoms");};
  window.addEventListener("hashchange",function(){closeSheet();var k=sanitizeRoute(location.hash.replace("#",""));if(state.route!==k){state.route=k;render();}});
  document.addEventListener("keydown",function(e){if(e.key==="Escape"){if(cover&&cover.classList.contains("show")){closeSheet();}else if(state.route!=="home"){go("home");}}});
  state.route=sanitizeRoute(window.location.hash.replace("#",""));
  render();
})();
