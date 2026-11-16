/* Pure, deterministic warning-sign helper. NOT a diagnosis or comprehensive triage engine. */
(function (root, factory) {
  "use strict";
  const api = factory();
  if (typeof module === "object" && module.exports) module.exports = api;
  else root.ManoWarning = api;
})(typeof window !== "undefined" ? window : this, function () {
  "use strict";

  function evaluate(input) {
    const age = input && input.ageBand;
    const warnings = input && Array.isArray(input.warnings) ? input.warnings : [];
    const rawTemp = input && input.temperature;
    const hasTemp = rawTemp !== "" && rawTemp !== null && rawTemp !== undefined;
    const temp = hasTemp ? Number(rawTemp) : null;

    // Red flags must win regardless of whether every field was completed.
    if (warnings.length > 0) {
      return {
        kind: "danger",
        heading: "نیاز به ارزیابی فوری",
        message: "یکی از نشانه‌های مهم هشدار انتخاب شده است. برای بررسی فوری توسط اورژانس یا تیم درمان اقدام کن. در ایران در موارد اورژانسی با ۱۱۵ تماس بگیر. از این صفحه برای تأخیر در مراجعه استفاده نکن.",
      };
    }
    if (age !== "under3" && age !== "older") {
      return { kind: "invalid", heading: "گروه سنی را انتخاب کن", message: "برای نمایش راهنمای اولیه، گروه سنی را مشخص کن." };
    }
    if (hasTemp && (!Number.isFinite(temp) || temp < 30 || temp > 45)) {
      return { kind: "invalid", heading: "دمای ثبت‌شده را بررسی کن", message: "عدد دما باید با درجه سانتی‌گراد و در بازه قابل ثبت وارد شود. اگر حال کودک نامناسب است، منتظر اصلاح فرم نمان." };
    }
    if (age === "under3" && temp !== null && temp >= 38) {
      return {
        kind: "danger",
        heading: "تب در کودک زیر سه ماه؛ ارزیابی فوری",
        message: "دمای ۳۸ درجه سانتی‌گراد یا بالاتر در کودک زیر سه ماه نیازمند ارزیابی فوری پزشکی است. خودسرانه تب‌بر نده.",
      };
    }
    if (age === "under3") {
      return {
        kind: "caution",
        heading: "در سن کمتر از سه ماه احتیاط بیشتری لازم است",
        message: "اگر احتمال تب، کاهش شیرخوردن، بی‌حالی یا هر نگرانی درباره حال عمومی وجود دارد، برای ارزیابی سریع پزشکی اقدام کن. نبود پرچم انتخاب‌شده سلامت کودک را اثبات نمی‌کند.",
      };
    }
    return {
      kind: "caution",
      heading: "این ارزیابی به معنی بی‌خطر بودن وضعیت نیست",
      message: "در این فرم نشانه هشدار فوری ثبت نشده، اما فرم جایگزین معاینه و شرح‌حال کامل نیست. اگر علائم شدید، رو به بدترشدن یا نگران‌کننده‌اند، با پزشک مشورت کن.",
    };
  }

  return { evaluate };
});