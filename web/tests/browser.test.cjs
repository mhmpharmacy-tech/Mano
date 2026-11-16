"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { pathToFileURL } = require("node:url");
const { chromium } = require("playwright");

const root = path.resolve(__dirname, "..");
const out = path.join(root, "browser-artifacts");
const parentUrl = pathToFileURL(path.join(root, "index.html")).href;
const staffUrl = pathToFileURL(path.join(root, "staff-demo.html")).href;
let browser;

async function settleVisuals(page) {
  // Full-page screenshots may skip images using loading="lazy" if the
  // user has not scrolled to them. Load every actual visual first.
  const images = page.locator("img");
  const total = await images.count();
  for (let i = 0; i < total; i++) {
    const item = images.nth(i);
    await item.scrollIntoViewIfNeeded();
    const ready = await item.evaluate(async (img) => {
      try { await img.decode(); return img.naturalWidth > 0; } catch (_) { return false; }
    });
    assert.ok(ready, "Image must load before visual QA screenshot #" + i);
  }
  await page.evaluate(() => {
    document.documentElement.style.scrollBehavior = "auto";
    window.scrollTo(0, 0);
  });
  await page.waitForFunction(() => window.scrollY < 3);
}

test.before(async () => {
  fs.mkdirSync(out, { recursive: true });
  browser = await chromium.launch({ headless: true, args: ["--no-sandbox"] });
});

test.after(async () => {
  if (browser) await browser.close();
});

test("Desktop: original MANO assets, functional category search, no JS errors", async () => {
  const page = await browser.newPage({ viewport: { width: 1366, height: 900 }, deviceScaleFactor: 1 });
  const errors = [];
  page.on("pageerror", err => errors.push(err.message));
  await page.goto(parentUrl);
  await page.locator(".hero-mascot").waitFor({ state: "visible" });
  assert.equal(await page.locator("#path-grid > .path-card:visible").count(), 6);
  const mascot = await page.locator(".hero-mascot").evaluate(el => ({
    complete: el.complete,
    naturalWidth: el.naturalWidth,
  }));
  assert.ok(mascot.complete && mascot.naturalWidth > 0, "Original MANO character image must load");
  await page.locator("#topic-search").fill("شربت");
  assert.equal(await page.locator("#path-grid > .path-card:visible").count(), 1);
  assert.match(await page.locator("#topic-count").innerText(), /۱|1/);
  await page.locator("#topic-search").fill("موضوع کاملاً ناموجود");
  assert.equal(await page.locator("#path-grid > .path-card:visible").count(), 0);
  assert.ok(await page.locator("#no-matches").isVisible());
  await page.locator("#topic-search").fill("");
  await settleVisuals(page);
  await page.screenshot({ path: path.join(out, "home-desktop.png"), fullPage: true, animations: "disabled" });
  assert.deepEqual(errors, []);
  await page.close();
});

test("Mobile: RTL menu, content, and layout fit 390px", async () => {
  const page = await browser.newPage({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 1 });
  const errors = [];
  page.on("pageerror", err => errors.push(err.message));
  await page.goto(parentUrl);
  assert.equal(await page.locator("html").getAttribute("dir"), "rtl");
  await page.locator("#menu-button").click();
  assert.equal(await page.locator("#menu-button").getAttribute("aria-expanded"), "true");
  assert.ok(await page.locator("#mobile-nav").isVisible());
  await page.locator('#mobile-nav a[href="#faq"]').click();
  assert.equal(await page.locator("#mobile-nav").isVisible(), false);
  const dimensions = await page.evaluate(() => ({
    viewport: window.innerWidth,
    page: document.documentElement.scrollWidth,
  }));
  assert.ok(dimensions.page <= dimensions.viewport + 2,
    "Mobile width overflow: " + JSON.stringify(dimensions));
  await settleVisuals(page);
  await page.screenshot({ path: path.join(out, "home-mobile.png"), fullPage: true, animations: "disabled" });
  assert.deepEqual(errors, []);
  await page.close();
});

test("Public warning form: under-three-month fever and red flag have urgent priority", async () => {
  const page = await browser.newPage();
  await page.goto(parentUrl);
  await page.locator("#child-age").selectOption("under3");
  await page.locator("#child-temp").fill("38.0");
  await page.locator('#redflags-form button[type="submit"]').click();
  assert.ok(await page.locator("#screening-result").isVisible());
  assert.ok((await page.locator("#screening-result").getAttribute("class")).includes("alert"));
  assert.match(await page.locator("#screening-result").innerText(), /فوری/);

  await page.locator("#child-age").selectOption("older");
  await page.locator("#child-temp").fill("");
  await page.locator('input[name="warning"][value="breath"]').check();
  await page.locator('#redflags-form button[type="submit"]').click();
  assert.match(await page.locator("#screening-result").innerText(), /اورژانس|فوری/);
  await page.close();
});

test("Staff demo: filters, flag warning, simulated reviewer decision and reset", async () => {
  const page = await browser.newPage({ viewport: { width: 1366, height: 900 } });
  const errors = [];
  page.on("pageerror", err => errors.push(err.message));
  await page.goto(staffUrl);
  assert.equal(await page.locator("#case-list button.case-button").count(), 5);
  await page.locator("#filter-category").selectOption("medicine");
  assert.equal(await page.locator("#case-list button.case-button").count(), 1);
  await page.locator("#filter-category").selectOption("all");
  await page.locator("#filter-safety").selectOption("flagged");
  assert.equal(await page.locator("#case-list button.case-button").count(), 2);
  await page.locator("#filter-safety").selectOption("all");

  // The first synthetic case has a critical warning: education-only must be rejected.
  await page.locator("#case-list button.case-button").first().click();
  await page.locator("#review-decision").selectOption("education");
  await page.locator("#review-note").fill("این متن نمونه طول کافی برای ثبت تصمیم دارد.");
  await page.locator("#review-confirm").check();
  await page.locator('#review-form button[type="submit"]').click();
  assert.match(await page.locator("#review-message").innerText(), /مجاز نیست/);
  await page.locator("#review-decision").selectOption("refer");
  await page.locator('#review-form button[type="submit"]').click();
  assert.match(await page.locator("#review-message").innerText(), /هیچ داده‌ای روی سرور/);
  await settleVisuals(page);
  await page.screenshot({ path: path.join(out, "staff-demo-desktop.png"), fullPage: true, animations: "disabled" });

  await page.locator("#reset-demo").click();
  assert.equal(await page.locator("#case-list button.case-button").count(), 5);
  assert.deepEqual(errors, []);
  await page.close();
});


test("Responsive widths: public and staff pages never spill horizontally", async () => {
  const page = await browser.newPage({ viewport: { width: 320, height: 780 } });
  for (const target of [parentUrl, staffUrl]) {
    await page.goto(target);
    for (const width of [320, 360, 390, 430, 768, 1024]) {
      await page.setViewportSize({ width, height: 800 });
      await page.evaluate(() => document.fonts.ready);
      const box = await page.evaluate(() => ({
        scrollWidth: document.documentElement.scrollWidth,
        viewportWidth: window.innerWidth,
        mainWidth: document.querySelector("main")?.getBoundingClientRect().width || 0,
      }));
      assert.ok(box.scrollWidth <= box.viewportWidth + 2,
        "Responsive overflow at " + width + " on " + target + ": " + JSON.stringify(box));
    }
  }
  await page.close();
});
