/* Only presentation interactions; no analytics, account, upload, or patient data storage. */
(function () {
  "use strict";
  const byId = (id) => document.getElementById(id);

  const menuButton = byId("menu-button");
  const mobileNav = byId("mobile-nav");
  if (menuButton && mobileNav) {
    menuButton.addEventListener("click", function () {
      const willOpen = mobileNav.hidden;
      mobileNav.hidden = !willOpen;
      menuButton.setAttribute("aria-expanded", String(willOpen));
      menuButton.setAttribute("aria-label", willOpen ? "بستن فهرست" : "بازکردن فهرست");
    });
    mobileNav.querySelectorAll("a").forEach(function (link) {
      link.addEventListener("click", function () {
        mobileNav.hidden = true;
        menuButton.setAttribute("aria-expanded", "false");
        menuButton.setAttribute("aria-label", "بازکردن فهرست");
      });
    });
  }

  const search = byId("topic-search");
  const cards = Array.from(document.querySelectorAll(".path-card"));
  const count = byId("topic-count");
  const empty = byId("no-matches");

  function normalizePersian(text) {
    return String(text || "")
      .replace(/ي/g, "ی")
      .replace(/ك/g, "ک")
      .replace(/[\u064B-\u065F]/g, "")
      .replace(/\s+/g, " ")
      .trim().toLocaleLowerCase("fa-IR");
  }

  if (search) {
    search.addEventListener("input", function () {
      const q = normalizePersian(search.value);
      let visible = 0;
      cards.forEach(function (card) {
        const content = normalizePersian(card.textContent + " " + (card.dataset.keywords || ""));
        const matches = !q || content.includes(q);
        card.hidden = !matches;
        if (matches) visible++;
      });
      count.textContent = visible + " دسته برای مرور";
      empty.hidden = visible !== 0;
    });
  }

  const form = byId("redflags-form");
  const result = byId("screening-result");
  if (form && result) {
    form.addEventListener("submit", function (event) {
      event.preventDefault();
      const ageBand = byId("child-age").value;
      const temperature = byId("child-temp").value.trim();
      const warnings = Array.from(form.querySelectorAll('input[name="warning"]:checked')).map(function (item) {
        return item.value;
      });
      const response = window.ManoWarning.evaluate({ ageBand: ageBand, temperature: temperature, warnings: warnings });
      result.hidden = false;
      result.className = "screening-result" + (response.kind === "danger" ? " alert" : "");
      result.replaceChildren();

      const title = document.createElement("strong");
      title.textContent = response.heading;
      const paragraph = document.createElement("p");
      paragraph.textContent = response.message;
      paragraph.style.margin = "0";
      result.append(title, paragraph);
      result.scrollIntoView({ behavior: "smooth", block: "nearest" });
    });
  }

  const copyButton = byId("copy-question");
  const feedback = byId("copy-feedback");
  const questionTemplate = [
    "قالب پرسش برای مشاوره حضوری با داروساز (بدون نام و مشخصات هویتی):",
    "گروه سنی کودک: ...",
    "موضوع اصلی: ...",
    "مدت شروع علائم: ...",
    "علائم همراه و دمای ثبت‌شده: ...",
    "داروها و مکمل‌های فعلی با نام و غلظت: ...",
    "حساسیت دارویی شناخته‌شده: ...",
    "سؤال مشخص من از داروساز: ...",
    "در صورت علائم خطر، به جای این قالب فوراً ارزیابی پزشکی لازم است.",
  ].join("\n");

  if (copyButton && feedback) {
    copyButton.addEventListener("click", async function () {
      try {
        if (!navigator.clipboard || !navigator.clipboard.writeText) throw new Error("Clipboard API unavailable");
        await navigator.clipboard.writeText(questionTemplate);
        feedback.textContent = "قالب پرسش کپی شد؛ آن را فقط در مسیر امن یا هنگام مراجعه حضوری تکمیل کن.";
      } catch (_) {
        // Some browsers deny clipboard on local files; show a selectable textarea.
        let box = byId("copy-fallback");
        if (!box) {
          box = document.createElement("textarea");
          box.id = "copy-fallback";
          box.setAttribute("aria-label", "قالب پرسش برای کپی دستی");
          box.style.cssText = "display:block;width:100%;min-height:170px;margin-top:12px;border:1px solid #9accc7;border-radius:12px;padding:10px";
          feedback.after(box);
        }
        box.value = questionTemplate;
        box.focus();
        box.select();
        feedback.textContent = "کپی خودکار در این مرورگر مجاز نیست؛ متن زیر را به‌صورت دستی کپی کن.";
      }
    });
  }
})();