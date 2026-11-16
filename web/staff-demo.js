/* MANO pharmacist workflow UX preview.
   Synthetic cases only; no authentication, network requests, persistence or real approvals. */
(function () {
  "use strict";
  const fixtures = [
    {
      id: "DEMO-101", alias: "پرونده نمایشی الف", category: "symptoms", topic: "تب در نوزاد",
      age: "۲ ماه", safety: true, status: "pending", lastContact: "2026-11-16",
      lastLabel: "۱۴۰۵/۰۸/۲۵", followup: "", previous: "تماس قبلی ثبت نشده است.",
      current: "والد به‌صورت فرضی تب ۳۸ درجه را گزارش کرده و درباره قدم بعدی پرسیده است.",
      caution: "مثال آموزشی: تب ≥۳۸ درجه در نوزاد زیر ۳ ماه نیازمند ارزیابی فوری پزشکی است.",
      note: "", decision: ""
    },
    {
      id: "DEMO-102", alias: "پرونده نمایشی ب", category: "skin", topic: "راش ناحیه پوشک",
      age: "۱ سال", safety: false, status: "reviewed", lastContact: "2026-11-15",
      lastLabel: "۱۴۰۵/۰۸/۲۴", followup: "2026-11-19",
      previous: "قرمزی خفیف بدون تب (متن کاملاً ساختگی).",
      current: "پرسش نمایشی درباره مدت استفاده از کرم محافظ و مراقبت پوست.",
      caution: "تشدید علائم، تب یا نشانه عفونت نیازمند ارزیابی تخصصی است.",
      note: "نمونه یادداشت ساختگی برای نمایش چیدمان پنل؛ تایید واقعی نیست.",
      decision: "education"
    },
    {
      id: "DEMO-103", alias: "پرونده نمایشی پ", category: "medicine", topic: "اختلاف غلظت قطره و شربت",
      age: "۳ سال", safety: true, status: "pending", lastContact: "2026-11-13",
      lastLabel: "۱۴۰۵/۰۸/۲۲", followup: "",
      previous: "درباره تفاوت میلی‌گرم و میلی‌لیتر پرسیده شده است.",
      current: "والد فرضی از دو بسته با غلظت متفاوت نام می‌برد.",
      caution: "احتمال خطای غلظت دارو؛ قبل از مصرف بعدی نیازمند تطبیق دقیق دارو با متخصص است.",
      note: "", decision: ""
    },
    {
      id: "DEMO-104", alias: "پرونده نمایشی ت", category: "nutrition", topic: "کم‌اشتهایی کودک",
      age: "۴ سال", safety: false, status: "pending", lastContact: "2026-11-11",
      lastLabel: "۱۴۰۵/۰۸/۲۰", followup: "2026-11-18",
      previous: "پرسش نمایشی درباره وعده‌های غذایی.",
      current: "آخرین تماس فرضی: استمرار کم‌اشتهایی بدون اطلاعات کافی برای نتیجه‌گیری.",
      caution: "روند رشد، سابقه و علائم همراه باید بررسی شوند؛ تصمیم خودکار وجود ندارد.",
      note: "", decision: ""
    },
    {
      id: "DEMO-105", alias: "پرونده نمایشی ث", category: "symptoms", topic: "سرفه طولانی",
      age: "۶ سال", safety: false, status: "reviewed", lastContact: "2026-11-10",
      lastLabel: "۱۴۰۵/۰۸/۱۹", followup: "",
      previous: "شکایت نمایشی از سرفه شبانه.",
      current: "پرسش درباره زمان مناسب ارزیابی بالینی.",
      caution: "علائم تنفسی رو به بدترشدن نیازمند ارزیابی سریع‌تر است.",
      note: "پاسخ قبلی فرضی فقط برای نمایش چیدمان پنل است.",
      decision: "clarify"
    }
  ];

  let cases = fixtures.map(function (entry) { return Object.assign({}, entry); });
  let activeId = cases[0].id;

  const $ = function (id) { return document.getElementById(id); };
  const labels = {
    symptoms: "علائم", medicine: "دارو", skin: "پوست", nutrition: "تغذیه",
    pending: "در انتظار", reviewed: "بررسی‌شده (آزمایشی)",
    education: "آموزش و پیگیری", refer: "ارجاع پزشکی", clarify: "اطلاعات بیشتر"
  };
  function el(tag, cls, text) {
    const element = document.createElement(tag);
    if (cls) element.className = cls;
    if (text !== undefined) element.textContent = String(text);
    return element;
  }

  function filterCases() {
    const cat = $("filter-category").value;
    const stat = $("filter-status").value;
    const safe = $("filter-safety").value;
    const contact = $("filter-contact").value;
    return cases.filter(function (c) {
      if (cat !== "all" && c.category !== cat) return false;
      if (stat === "pending" && c.status !== "pending") return false;
      if (stat === "reviewed" && c.status !== "reviewed") return false;
      if (stat === "followup" && !c.followup) return false;
      if (safe === "flagged" && !c.safety) return false;
      if (safe === "normal" && c.safety) return false;
      if (contact === "recent" && c.lastContact < "2026-11-15") return false;
      if (contact === "older" && c.lastContact >= "2026-11-15") return false;
      return true;
    });
  }

  function renderKpis() {
    const data = [
      ["کل پرونده‌ها", cases.length],
      ["در انتظار بررسی", cases.filter(function (c) { return c.status === "pending"; }).length],
      ["پرچم هشدار", cases.filter(function (c) { return c.safety; }).length],
      ["پیگیری برنامه‌ریزی‌شده", cases.filter(function (c) { return Boolean(c.followup); }).length]
    ];
    const holder = $("staff-kpis");
    holder.replaceChildren();
    data.forEach(function (item) {
      const box = el("div","kpi");
      box.append(el("span","num",item[1]),el("span","caption",item[0]));
      holder.append(box);
    });
  }

  function renderCases() {
    const visible = filterCases();
    const list = $("case-list");
    list.replaceChildren();
    $("case-count").textContent = "(" + visible.length + " مورد)";
    $("case-empty").hidden = visible.length > 0;

    // If a filter hides the selected case, choose another visible sample.
    if (!visible.some(function (c) { return c.id === activeId; })) {
      activeId = visible.length ? visible[0].id : null;
    }
    visible.forEach(function (c) {
      const button = el("button","case-button");
      button.type = "button";
      button.setAttribute("role", "listitem");
      button.setAttribute("aria-pressed", String(c.id === activeId));
      const heading = el("div","case-heading");
      heading.append(el("strong","",c.alias),el("span","",c.id));
      button.append(heading,el("span","case-desc",c.topic + " · " + c.age));
      const tags = el("div","case-tags");
      tags.append(el("span","case-tag",labels[c.category]),el("span","case-tag"+(c.status==="reviewed"?" done":""),labels[c.status]));
      if (c.safety) tags.append(el("span","case-tag flag","دارای هشدار"));
      button.append(tags);
      button.addEventListener("click",function () {
        activeId = c.id;
        render();
      });
      list.append(button);
    });
  }

  function info(label, value) {
    const box = el("div","info-box");
    box.append(el("small","",label),el("strong","",value));
    return box;
  }

  function renderDetail() {
    const holder = $("case-detail");
    const form = $("review-form");
    holder.replaceChildren();
    const c = cases.find(function (a) { return a.id === activeId; });
    form.hidden = !c;
    if (!c) {
      holder.append(el("p","detail-lead","هیچ پرونده‌ای با فیلترهای انتخاب‌شده وجود ندارد."));
      return;
    }
    const heading = el("div","detail-title");
    heading.append(el("h2","",c.alias),el("small","",c.id));
    holder.append(heading,el("p","detail-lead",c.topic + " · " + c.age + " · داده آزمایشی غیرواقعی"));
    const summary = el("div","detail-info");
    summary.append(info("دسته",labels[c.category]),info("آخرین تماس",c.lastLabel),info("وضعیت",labels[c.status]));
    holder.append(summary);

    const safety = el("div","safety-call"+(!c.safety?" safe":""),c.safety?"هشدار ایمنی: "+c.caution:"نکته ایمنی: "+c.caution);
    holder.append(safety,el("h3","","مقایسه دو مرحله مشاوره"));
    const compare = el("div","timeline-compare");
    for (const item of [["خلاصه قبلی",c.previous],["اطلاعات جدید",c.current]]) {
      const box = el("div","timeline-box");
      box.append(el("strong","",item[0]),el("p","",item[1]));
      compare.append(box);
    }
    holder.append(compare);
    if (c.note) {
      holder.append(el("h3","","آخرین یادداشت نمایشی"),el("p","detail-lead",c.note));
    }
    $("review-decision").value = c.decision || "";
    $("review-note").value = c.note || "";
    $("review-followup").value = c.followup || "";
    $("review-confirm").checked = false;
    $("review-message").hidden = true;
  }

  function render() { renderKpis(); renderCases(); renderDetail(); }

  ["filter-category","filter-status","filter-safety","filter-contact"].forEach(function (id) {
    $(id).addEventListener("change", render);
  });

  $("review-form").addEventListener("submit", function (event) {
    event.preventDefault();
    const c = cases.find(function (x) { return x.id === activeId; });
    if (!c) return;
    const decision = $("review-decision").value;
    const note = $("review-note").value.trim();
    const confirmed = $("review-confirm").checked;
    const message = $("review-message");
    message.hidden = false;

    if (!confirmed || !decision || note.length < 15) {
      message.textContent = "برای ثبت نمایشی، تصمیم، یادداشت حداقل ۱۵ حرفی و تیک بازبینی لازم است.";
      message.className = "screening-result alert";
      return;
    }
    if (c.safety && decision === "education") {
      message.textContent = "در پرونده دارای هشدار، انتخاب آموزش بدون ارجاع یا بررسی بیشتر در این نمونه مجاز نیست.";
      message.className = "screening-result alert";
      return;
    }
    c.status = "reviewed";
    c.note = note.slice(0,800);
    c.decision = decision;
    c.followup = $("review-followup").value;
    render();
    const msg = $("review-message");
    msg.hidden = false;
    msg.textContent = "تصمیم فقط در حافظه همین صفحه شبیه‌سازی شد. هیچ داده‌ای روی سرور ذخیره نشده است.";
    msg.className = "screening-result";
  });

  $("reset-demo").addEventListener("click", function () {
    cases = fixtures.map(function (c) { return Object.assign({}, c); });
    activeId = cases[0].id;
    for (const id of ["filter-category","filter-status","filter-safety","filter-contact"]) $(id).value="all";
    render();
  });
  $("print-case").addEventListener("click",function () {
    if (activeId) window.print();
  });
  render();
})();