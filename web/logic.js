/* Pure, deterministic warning-sign helper. NOT a diagnosis or comprehensive triage engine. */
(function (root, factory) {
  "use strict";
  const api = factory();
  if (typeof module === "object" && module.exports) module.exports = api;
  else root.ManoWarning = api;
})(typeof window !== "undefined" ? window : this, function () {
  "use strict";

  // Inputs from Persian/Arabic keyboards must never be silently treated as
  // no fever. Either normalize a valid Celsius value or fail closed.
  function parseTemperature(raw) {
    if (raw === "" || raw === undefined || raw === null) return null;
    const clean = String(raw).trim()
      .replace(/[۰-۹]/g, function (digit) { return String(digit.charCodeAt(0) - 1776); })
      .replace(/[٠-٩]/g, function (digit) { return String(digit.charCodeAt(0) - 1632); })
      .replace(/[٫,]/g, ".");
    if (!/^\\d{1,2}(?:\\.\\d{1,2})?$/.test(clean)) return NaN;
    return Number(clean);
  }

  function evaluate(input) {
    const age = input && input.ageBand;
    const warnings = input && Array.isArray(input.warnings) ? input.warnings : [];
    const temp = parseTemperature(input && input.temperature);
    const hasTemp = temp !== null;

    // Red flags must win regardless of whether every field was completed.
    if (warnings.length > 0) {
      return {
        kind: "danger",
        heading: "نیاز به ارزیابی فوری",
        message: "یکی از نشانه‌های مهم هشدار انتخاب شده است. فوراً از اورژانس یا تیم درمان کمک بگیر. در ایران با ۱۱۵ و در کشورهای دیگر با شماره اورژانس محل تماس بگیر؛ این فرم نباید مراجعه را به تأخیر بیندازد.",
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
        message: "دمای ۳۸ درجه سانتی‌گراد یا بالاتر در کودک زیر سه ماه نیازمند ارزیابی فوری پزشکی است. در ایران برای راهنمایی اورژانسی با ۱۱۵ تماس بگیر؛ در کشورهای دیگر با خدمات اورژانس محل تماس بگیر. خودسرانه تب‌بر نده.",
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

  return { evaluate, parseTemperature };
});