import { spawn } from "node:child_process";
import { once } from "node:events";
import { mkdir, readFile } from "node:fs/promises";
import { createServer } from "node:net";
import { fileURLToPath } from "node:url";
import { chromium, firefox, webkit } from "playwright";
import assert from "node:assert/strict";

const browserName = process.env.MANO_BROWSER || "chromium";
const browserType = new Map([["chromium", chromium], ["firefox", firefox], ["webkit", webkit]]).get(browserName);
assert.ok(browserType, "MANO_BROWSER must be chromium, firefox or webkit");

const projectDir = fileURLToPath(new URL("../", import.meta.url));
const portReservation = createServer();
portReservation.listen(0, "127.0.0.1");
await once(portReservation, "listening");
const port = portReservation.address().port;
await new Promise((resolve, reject) => portReservation.close(err => err ? reject(err) : resolve()));
const origin = `http://127.0.0.1:${port}`;
const serverOnlyToken = "ci-server-only-token-must-never-reach-browser-8d85a1d0";
const server = spawn(process.execPath, ["--import", "tsx", "server.ts"], {
  cwd: projectDir,
  env: {
    ...process.env,
    NODE_ENV: "production",
    PORT: String(port),
    MANO_NO_LISTEN: "0",
    MANO_CLINICAL_SERVICE_ENABLED: "false",
    MANO_GATEWAY_ACCESS_TOKEN: serverOnlyToken,
    GEMINI_API_KEY: "",
  },
  stdio: ["ignore", "pipe", "pipe"],
});
let serverOutput = "";
server.stdout.on("data", data => { serverOutput += data.toString(); });
server.stderr.on("data", data => { serverOutput += data.toString(); });

async function waitForServer() {
  for (let attempt = 0; attempt < 100; attempt++) {
    if (server.exitCode !== null) throw Error(`Production server exited: ${serverOutput}`);
    try {
      const response = await fetch(`${origin}/api/health`);
      if (response.ok) return;
    } catch { /* Server may still be starting. */ }
    await new Promise(resolve => setTimeout(resolve, 200));
  }
  throw Error(`Production server did not become ready: ${serverOutput}`);
}

async function assertNoPageOverflow(page, name) {
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  assert.ok(overflow < 3, `${name}: unexpected horizontal overflow (${overflow}px)`);
}

async function openRoute(page, routeName, testId) {
  await page.evaluate(route => { window.location.hash = `/${route}`; }, routeName);
  if (testId) await page.getByTestId(testId).waitFor({ state: "visible" });
}

async function savedSnapshot(page) {
  return page.evaluate(() => JSON.parse(localStorage.getItem("mano_kid_v2_store") || "null"));
}

async function waitForSavedAge(page, age) {
  await page.waitForFunction(expected => {
    const state = JSON.parse(localStorage.getItem("mano_kid_v2_store") || "null");
    return state?.childProfile?.ageMonthsExact === expected;
  }, age);
}

async function recordedTime(page) {
  return page.evaluate(() => {
    const now = new Date(Date.now() - 60_000);
    const pad = number => String(number).padStart(2, "0");
    return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`;
  });
}

async function assertShortScreenModal(page, dialog, { requireScroll = true } = {}) {
  const rect = await dialog.boundingBox();
  const viewport = page.viewportSize();
  assert.ok(rect.x >= 0 && rect.y >= 0 && rect.x + rect.width <= viewport.width + 1 && rect.y + rect.height <= viewport.height + 1, "The modal panel must fit the short viewport");
  if (requireScroll) assert.equal(await dialog.evaluate(element => element.scrollHeight > element.clientHeight), true, "Long modal content must remain scrollable");
  assert.equal(await page.locator("#root").evaluate(element => element.inert), true);
  assert.equal(await page.evaluate(() => document.body.style.overflow), "hidden");
  await dialog.evaluate(panel => {
    const controls = [...panel.querySelectorAll('a[href],button,input,select,textarea,[tabindex]')].filter(element => element.tabIndex >= 0 && !element.matches(':disabled') && element.getClientRects().length);
    controls.at(-1).focus();
  });
  await page.keyboard.press("Tab");
  assert.equal(await dialog.evaluate(panel => document.activeElement === [...panel.querySelectorAll('a[href],button,input,select,textarea,[tabindex]')].find(element => element.tabIndex >= 0 && !element.matches(':disabled') && element.getClientRects().length)), true);
  await page.keyboard.press("Shift+Tab");
  assert.equal(await dialog.evaluate(panel => document.activeElement === [...panel.querySelectorAll('a[href],button,input,select,textarea,[tabindex]')].filter(element => element.tabIndex >= 0 && !element.matches(':disabled') && element.getClientRects().length).at(-1)), true);
}

async function verifyRemainingFormDialogs(browser, errors) {
  const context = await browser.newContext({ viewport: { width: 320, height: 480 }, locale: "fa-IR", reducedMotion: "reduce" });
  context.setDefaultTimeout(12_000);
  const page = await context.newPage();
  page.on("pageerror", error => errors.push(error.message));
  await page.goto(`${origin}/?autostart=1`, { waitUntil: "networkidle" });

  const tempTrigger = page.getByTestId("timeline-add-temp-btn");
  await tempTrigger.click();
  const temperature = page.locator("#mano-temperature-entry-dialog");
  await temperature.waitFor();
  assert.equal(await temperature.locator("h3").evaluate(element => element === document.activeElement), true, "A new temperature form must initially announce its heading");
  await page.getByTestId("timeline-temp-input").fill("37.3");
  await page.getByLabel("ثبت‌کننده دما (اختیاری)", { exact: true }).fill("qa-caregiver-draft");
  await page.getByLabel("یادداشت دما (اختیاری)", { exact: true }).fill("qa-temperature-draft");
  await page.mouse.click(2, 2);
  assert.ok(await temperature.isVisible(), "A backdrop click must not discard unsaved temperature input");
  assert.equal(await page.getByTestId("timeline-temp-input").inputValue(), "37.3");
  await page.keyboard.press("Control+k");
  assert.equal(await page.locator("#mano-search-dialog").count(), 0, "Global search must not obscure an entry form");
  await assertShortScreenModal(page, temperature);
  await page.screenshot({ path: "web-artifacts/temperature-entry-dialog-short.png" });
  await page.keyboard.press("Escape");
  await temperature.waitFor({ state: "hidden" });
  assert.equal(await tempTrigger.evaluate(element => element === document.activeElement), true);
  assert.equal((await savedSnapshot(page)).feverLogs.length, 0, "Cancelling a draft must not create a temperature record");
  await tempTrigger.click();
  assert.equal(await page.getByTestId("timeline-temp-input").inputValue(), "", "Reopening an explicitly cancelled form must not reuse its measurement");
  await page.keyboard.press("Escape");

  const doseTrigger = page.getByTestId("timeline-add-dose-btn");
  await doseTrigger.click();
  const dose = page.locator("#mano-dose-entry-dialog");
  await dose.waitFor();
  assert.equal(await dose.locator("h3").evaluate(element => element === document.activeElement), true);
  await page.getByTestId("timeline-dose-form-input").fill("qa-unsaved-formulation");
  await page.mouse.click(2, 2);
  assert.ok(await dose.isVisible(), "A backdrop click must not discard unsaved medication input");
  assert.equal(await page.getByTestId("timeline-dose-form-input").inputValue(), "qa-unsaved-formulation");
  assert.ok(await page.getByTestId("timeline-dose-save-btn").isDisabled(), "Partial input must still respect administration validation");
  await assertShortScreenModal(page, dose);
  await page.screenshot({ path: "web-artifacts/dose-entry-dialog-short.png" });
  await page.keyboard.press("Escape");
  await dose.waitFor({ state: "hidden" });
  assert.equal(await doseTrigger.evaluate(element => element === document.activeElement), true);
  assert.equal((await savedSnapshot(page)).doseLogs.length, 0);

  await openRoute(page, "treatment-plan", "treatment-plan-screen");
  const treatmentTrigger = page.getByTestId("treatment-add-med-btn");
  await treatmentTrigger.click();
  const treatment = page.locator("#mano-treatment-editor-dialog");
  await treatment.waitFor();
  assert.equal(await treatment.locator("h3").evaluate(element => element === document.activeElement), true);
  await page.getByLabel("نام دارو و غلظت دقیق", { exact: true }).fill("qa-unsaved-care-entry");
  await page.mouse.click(2, 2);
  assert.ok(await treatment.isVisible(), "A backdrop click must not discard the care-plan editor");
  await assertShortScreenModal(page, treatment);
  await page.getByLabel("تعداد نوبت قابل ثبت در روز (۱ تا ۶)", { exact: true }).fill("2");
  await page.getByLabel("مقدار هر نوبت طبق دستور", { exact: true }).fill("qa-copied-label-amount");
  await treatment.getByRole("button", { name: "ذخیره در برنامه درمانی", exact: true }).click();
  assert.ok(await treatment.isVisible());
  assert.equal(await page.getByLabel("دستور زمان‌بندی مصرف", { exact: true }).evaluate(element => element === document.activeElement), true, "Invalid submission must move focus to the missing schedule");
  assert.equal((await savedSnapshot(page)).treatmentPlan.medications.length, 0);
  await page.screenshot({ path: "web-artifacts/treatment-editor-dialog-short.png" });
  await page.keyboard.press("Escape");
  await treatment.waitFor({ state: "hidden" });
  assert.equal(await treatmentTrigger.evaluate(element => element === document.activeElement), true);

  await openRoute(page, "supplements-hygiene", "supplements-hygiene-screen");
  const guideTrigger = page.locator('[data-testid^="supplement-guide-"]').first();
  await guideTrigger.click();
  const guide = page.locator("#mano-supplement-guide-dialog");
  await guide.waitFor();
  assert.equal(await guide.locator("h2").evaluate(element => element === document.activeElement), true);
  await assertShortScreenModal(page, guide, { requireScroll: false });
  await page.screenshot({ path: "web-artifacts/supplement-guide-dialog-short.png" });
  await page.keyboard.press("Escape");
  await guide.waitFor({ state: "hidden" });
  assert.equal(await guideTrigger.evaluate(element => element === document.activeElement), true);

  const checklistTrigger = page.getByTestId("supplements-open-checklist-btn");
  await checklistTrigger.click();
  const checklist = page.locator("#mano-hygiene-checklist-dialog");
  await checklist.waitFor();
  assert.equal(await checklist.locator("h3").evaluate(element => element === document.activeElement), true);
  await assertShortScreenModal(page, checklist, { requireScroll: false });
  const checklistItem = checklist.getByRole("checkbox").first();
  await checklistItem.check();
  await page.keyboard.press("Escape");
  await checklist.waitFor({ state: "hidden" });
  assert.equal(await checklistTrigger.evaluate(element => element === document.activeElement), true);
  await page.waitForFunction(() => JSON.parse(localStorage.getItem("mano_kid_v2_store") || "null")?.checkedHygieneIds.length === 1);
  await checklistTrigger.click();
  assert.equal(await checklist.getByRole("checkbox").first().isChecked(), true, "An explicitly checked routine must survive reopening its dialog");
  await page.keyboard.press("Escape");

  const productTrigger = page.locator('[data-testid^="supplement-product-"]').first();
  await productTrigger.click();
  const product = page.locator("#mano-supplement-product-dialog");
  await product.waitFor();
  assert.equal(await product.locator("h4").evaluate(element => element === document.activeElement), true);
  await assertShortScreenModal(page, product, { requireScroll: false });
  await page.screenshot({ path: "web-artifacts/supplement-product-dialog-short.png" });
  await page.keyboard.press("Escape");
  await product.waitFor({ state: "hidden" });
  assert.equal(await productTrigger.evaluate(element => element === document.activeElement), true);
  assert.equal(await page.locator("#root").evaluate(element => element.inert), false);
  assert.equal(await page.evaluate(() => document.body.style.overflow), "");
  await assertNoPageOverflow(page, "320px supplement dialogs and restored page");
  await context.close();
  console.log("Remaining form dialog browser checks passed: six short-screen modal contracts, dirty-input backdrop protection, native validation focus, explicit cancellation and retained routine records.");
}

async function verifyShortScreenCareDialogs(browser, errors) {
  const context = await browser.newContext({ viewport: { width: 320, height: 480 }, locale: "fa-IR", reducedMotion: "reduce" });
  context.setDefaultTimeout(12_000);
  const page = await context.newPage();
  page.on("pageerror", error => errors.push(error.message));
  await page.goto(`${origin}/?autostart=1`, { waitUntil: "networkidle" });
  const comfortTrigger = page.getByTestId("home-fever-comfort-btn");
  await comfortTrigger.click();
  const comfort = page.locator("#mano-non-drug-fever-dialog");
  await comfort.waitFor();
  assert.equal(await comfort.getAttribute("role"), "dialog");
  assert.equal(await comfort.locator("h2").evaluate(element => element === document.activeElement), true, "Care guidance must focus its heading before keyboard traversal");
  await assertShortScreenModal(page, comfort);
  await page.screenshot({ path: "web-artifacts/non-drug-fever-dialog-short.png" });
  await page.keyboard.press("Escape");
  await comfort.waitFor({ state: "hidden" });
  assert.equal(await comfortTrigger.evaluate(element => element === document.activeElement), true);
  assert.equal(await page.locator("#root").evaluate(element => element.inert), false);
  assert.equal(await page.evaluate(() => document.body.style.overflow), "");
  await comfortTrigger.click();
  await comfort.getByRole("button", { name: "متوجه شدم", exact: true }).click();
  await comfort.waitFor({ state: "hidden" });
  assert.equal(await comfortTrigger.evaluate(element => element === document.activeElement), true, "The bottom acknowledgement must remain reachable and restore focus");

  await page.getByTestId("timeline-add-temp-btn").click();
  await page.getByTestId("timeline-temp-input").fill("38.1");
  await page.getByTestId("timeline-temp-site-select").selectOption("زیر بغل (دیجیتال)");
  await page.getByTestId("timeline-temp-time-input").fill(await recordedTime(page));
  await page.getByTestId("timeline-temp-save-btn").click();
  const urgent = page.getByRole("alertdialog", { name: "هشدار دمای بالا ثبت شد", exact: true });
  await urgent.waitFor();
  assert.equal(await urgent.locator("h2").evaluate(element => element === document.activeElement), true);
  assert.match(await urgent.textContent(), /سن کودک تأیید نشده/);
  assert.equal(await urgent.getByRole("link", { name: "تماس با اورژانس ۱۱۵", exact: true }).getAttribute("href"), "tel:115");
  assert.equal(await urgent.getByRole("button", { name: /حجم دارو|محاسبه‌گر.*تب‌بر/ }).count(), 0, "Unknown infant age must not expose a self-treatment action");
  await assertShortScreenModal(page, urgent);
  await page.keyboard.press("Escape");
  assert.ok(await urgent.isVisible(), "Escape cannot acknowledge an emergency");
  await page.keyboard.press("Control+k");
  assert.equal(await page.locator("#mano-search-dialog").count(), 0);
  assert.equal(await page.locator("#mano-tools-dialog").count(), 0);
  await page.mouse.click(2, 2);
  assert.ok(await urgent.isVisible(), "Backdrop clicks cannot acknowledge an emergency");
  assert.equal(await page.locator("#root").evaluate(element => element.inert), true);
  await page.screenshot({ path: "web-artifacts/urgent-fever-dialog-short.png" });
  await urgent.getByRole("button", { name: "متوجه شدم", exact: true }).focus();
  await page.keyboard.press("Enter");
  await urgent.waitFor({ state: "hidden" });
  assert.equal(await page.evaluate(() => {
    const focused = document.activeElement;
    return focused instanceof HTMLElement && focused.isConnected && !focused.closest('[inert]') &&
      Boolean(focused.closest('main')) &&
      (focused.id === 'mano-main-content' || focused.dataset.testid === 'timeline-add-temp-btn');
  }), true, "Explicit acknowledgement must restore the actual temperature opener or its connected main-content fallback");
  assert.equal(await page.locator("#root").evaluate(element => element.inert), false);
  assert.equal(await page.evaluate(() => document.body.style.overflow), "");
  await page.waitForFunction(() => JSON.parse(localStorage.getItem("mano_kid_v2_store") || "null")?.feverLogs.length === 1);
  assert.equal((await savedSnapshot(page)).feverLogs[0].tempCelsius, 38.1, "Acknowledging the warning must preserve the actual temperature record");
  await page.keyboard.press("Control+k");
  await page.locator("#mano-search-dialog").waitFor();
  await page.keyboard.press("Escape");
  await context.close();
  console.log("Short-screen care dialog browser checks passed: heading focus, both keyboard traps, inert background, scrolling, explicit emergency acknowledgement and preserved measurements.");
}

async function verifyKeyboardAndTaskNavigation(browser, errors) {
  const context = await browser.newContext({ viewport: { width: 360, height: 820 }, locale: "fa-IR", reducedMotion: "reduce" });
  context.setDefaultTimeout(12_000);
  const page = await context.newPage();
  page.on("pageerror", error => errors.push(error.message));
  await page.goto(`${origin}/?autostart=1`, { waitUntil: "networkidle" });
  await page.getByTestId("home-screen").waitFor();
  for (const width of [360, 412]) {
    await page.setViewportSize({ width, height: 820 });
    await page.evaluate(() => window.scrollTo(0, 0));
    const navTop = (await page.getByRole("navigation", { name: "ناوبری موبایل" }).boundingBox()).y;
    for (const id of ["home-cta-symptoms", "home-cta-fever-dose"]) {
      const rect = await page.getByTestId(id).boundingBox();
      assert.ok(rect && rect.y >= 0 && rect.y + rect.height <= navTop, `${id} must be reachable in the first ${width}px viewport`);
      assert.ok(rect.height >= 44, "Primary touch targets must remain usable on mobile");
    }
    await assertNoPageOverflow(page, `Task-first home ${width}px`);
  }

  const mainHash = new URL(page.url()).hash;
  await page.reload({ waitUntil: "networkidle" });
  await page.keyboard.press("Tab");
  const skip = page.getByTestId("skip-to-main-link");
  assert.equal(await skip.evaluate(element => element === document.activeElement), true, "The first keyboard stop must offer a visible skip link");
  await page.keyboard.press("Enter");
  assert.equal(await page.locator("#mano-main-content").evaluate(element => element === document.activeElement), true);
  assert.equal(new URL(page.url()).hash, mainHash, "Skipping to main must not overwrite application routing");

  const trigger = page.getByTestId("open-hub-drawer-btn");
  await trigger.focus();
  await page.keyboard.press("Enter");
  const tools = page.locator("#mano-tools-dialog");
  await tools.waitFor();
  assert.ok(await tools.getByRole("button", { name: "بستن منو", exact: true }).evaluate(element => element === document.activeElement), "Drawer must receive useful initial keyboard focus");
  assert.equal(await page.locator("#root").evaluate(element => element.inert), true, "Background controls must be inert while the drawer is open");
  assert.equal(await page.evaluate(() => document.body.style.overflow), "hidden");
  assert.equal(await trigger.getAttribute("aria-expanded"), "true");
  await tools.evaluate(panel => {
    const controls = [...panel.querySelectorAll('a[href],button,input,select,textarea,[tabindex]')].filter(element => element.tabIndex >= 0 && !element.matches(':disabled') && element.getClientRects().length);
    controls.at(-1).focus();
  });
  await page.keyboard.press("Tab");
  assert.equal(await tools.evaluate(panel => document.activeElement === [...panel.querySelectorAll('a[href],button,input,select,textarea,[tabindex]')].find(element => element.tabIndex >= 0 && !element.matches(':disabled') && element.getClientRects().length)), true, "Tab must wrap inside the modal");
  await page.keyboard.press("Shift+Tab");
  assert.equal(await tools.evaluate(panel => document.activeElement === [...panel.querySelectorAll('a[href],button,input,select,textarea,[tabindex]')].filter(element => element.tabIndex >= 0 && !element.matches(':disabled') && element.getClientRects().length).at(-1)), true, "Shift+Tab must wrap inside the modal");
  await page.keyboard.press("Escape");
  await tools.waitFor({ state: "hidden" });
  assert.equal(await trigger.evaluate(element => element === document.activeElement), true, "Closing the drawer must restore its trigger focus");
  assert.equal(await page.locator("#root").evaluate(element => element.inert), false);
  assert.equal(await page.evaluate(() => document.body.style.overflow), "");

  await trigger.click();
  await page.getByTestId("drawer-search-btn").click();
  const search = page.locator("#mano-search-dialog");
  await search.waitFor();
  const searchInput = search.getByRole("searchbox", { name: "عبارت جست‌وجو", exact: true });
  assert.equal(await searchInput.evaluate(element => element === document.activeElement), true);
  assert.equal(await tools.count(), 0, "Moving from tools to search must leave exactly one active modal");
  assert.equal(await page.locator("#root").evaluate(element => element.inert), true);
  await searchInput.fill("تب");
  await page.keyboard.press("Escape");
  await search.waitFor({ state: "hidden" });
  assert.equal(await page.evaluate(() => document.activeElement === document.body), false, "Search must restore focus to a connected control or main content");
  assert.equal(await page.locator("#root").evaluate(element => element.inert), false);

  const destinations = [
    ["symptoms", "symptom-navigator-screen"], ["fever-dose", "fever-dose-screen"],
    ["syrup-scanner", "syrup-scanner-screen"], ["medicines", "medicines-screen"],
    ["supplements-hygiene", "supplements-hygiene-screen"], ["skin", "skin-screen"],
    ["devices", "devices-screen"], ["growth", "growth-screen"],
    ["vaccines", "vaccines-screen"], ["treatment-plan", "treatment-plan-screen"],
    ["care-cards", "care-cards-screen"], ["nutrition", "nutrition-screen"],
    ["topics", "topics-screen"], ["faq", "faq-screen"],
    ["ask-pharmacist", "ask-pharmacist-screen"], ["my-child", "my-child-screen"],
  ];
  for (const [id, screenId] of destinations) {
    await trigger.click();
    await tools.waitFor();
    assert.equal(await tools.locator("section[aria-labelledby]").count(), 4, "The complete catalog must remain grouped by user task");
    assert.equal(await tools.locator('[data-testid^="hub-destination-"]').count(), 16);
    await tools.getByTestId(`hub-destination-${id}`).click();
    await page.getByTestId(screenId).waitFor();
    assert.equal(new URL(page.url()).hash, `#/${id}`, `${id} must open its real routed screen`);
    assert.equal(await tools.count(), 0);
  }

  await openRoute(page, "fever-dose", "fever-dose-screen");
  await page.getByTestId("dose-age-input").fill("18");
  await page.getByTestId("dose-weight-input").fill("11");
  await page.getByTestId("formulation-btn-acetaminophen_drops_100").click();
  await page.getByTestId("dose-label-confirm-checkbox").check();
  await page.getByTestId("dose-calculated-result").waitFor();
  const syringeText = await page.getByTestId("visual-syringe-canvas").textContent();
  assert.match(syringeText, /mL/);
  assert.doesNotMatch(syringeText, /قطره استاندارد|۲۰\s*قطره|20\s*(?:drops|قطره)/, "Generic drops-per-mL conversions must not appear even on the drops formulation");

  await openRoute(page, "home", "home-screen");
  for (const group of ["care", "tracking", "learn"]) {
    const details = page.getByTestId(`home-guide-group-${group}`);
    assert.equal(await details.getAttribute("open"), null, "Secondary guide groups should initially reduce visual load");
    await details.locator("summary").click();
    assert.equal(await details.evaluate(element => element.open), true);
  }
  assert.ok(await page.getByTestId("home-tool-faq").isVisible());
  const reduced = await page.getByTestId("home-guide-group-care").locator("summary svg").evaluate(element => ({
    matches: matchMedia('(prefers-reduced-motion: reduce)').matches,
    duration: getComputedStyle(element).transitionDuration,
  }));
  assert.equal(reduced.matches, true);
  assert.ok(reduced.duration.split(',').every(value => Number.parseFloat(value) <= 0.001), "Reduced-motion preference must suppress decorative transitions");

  await page.setViewportSize({ width: 360, height: 820 });
  await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
  await assertNoPageOverflow(page, "200% text-size home reflow");
  for (const control of await page.locator('header button').all()) {
    const rect = await control.boundingBox();
    if (rect) assert.ok(rect.x >= 0 && rect.x + rect.width <= 361, "Header controls must not clip at 200% text size");
  }
  await page.evaluate(() => { window.scrollTo(0, 0); document.activeElement?.blur(); });
  await page.keyboard.press("Control+Home");
  for (let stop = 0; stop < 14; stop++) {
    await page.keyboard.press("Tab");
    const focused = await page.evaluate(() => {
      const element = document.activeElement;
      const rect = element.getBoundingClientRect();
      const nav = document.querySelector('nav[aria-label="ناوبری موبایل"]');
      return { inMain: Boolean(element.closest('main')), top: rect.top, bottom: rect.bottom, headerBottom: document.querySelector('header').getBoundingClientRect().bottom, navTop: nav.getBoundingClientRect().top };
    });
    if (focused.inMain) assert.ok(focused.top >= focused.headerBottom - 1 && focused.bottom <= focused.navTop + 1, "Keyboard focus must remain visible between the sticky header and fixed bottom navigation");
  }
  await page.screenshot({ path: "web-artifacts/react-home-font-200.png", fullPage: true });
  await context.close();

  const onboardingContext = await browser.newContext({ viewport: { width: 320, height: 480 }, locale: "fa-IR", reducedMotion: "reduce" });
  const onboardingPage = await onboardingContext.newPage();
  onboardingPage.on("pageerror", error => errors.push(error.message));
  await onboardingPage.goto(origin, { waitUntil: "networkidle" });
  const onboarding = onboardingPage.locator("#mano-onboarding-dialog");
  await onboarding.waitFor();
  assert.equal(await onboarding.getByRole("heading", { name: "آشنایی با مانو کید", exact: true }).evaluate(element => element === document.activeElement), true, "Onboarding must focus its heading");
  const onboardingRect = await onboarding.boundingBox();
  assert.ok(onboardingRect.y >= 0 && onboardingRect.y + onboardingRect.height <= 481, "Short-screen onboarding must fit within the viewport");
  assert.equal(await onboarding.evaluate(element => element.scrollHeight > element.clientHeight), true, "Long onboarding content must scroll rather than clip");
  assert.equal(await onboardingPage.locator("#root").evaluate(element => element.inert), true);
  await onboardingPage.keyboard.press("Escape");
  await onboarding.waitFor({ state: "hidden" });
  assert.equal(await onboardingPage.locator("#mano-main-content").evaluate(element => element === document.activeElement), true, "Optional onboarding must be dismissible with focus moved to main content");
  await assertNoPageOverflow(onboardingPage, "320px short-screen home");
  await onboardingPage.screenshot({ path: "web-artifacts/react-home-short-screen.png", fullPage: true });
  await onboardingContext.close();
  console.log("UX browser checks passed: first-viewport tasks, grouped16routes, modal keyboard/inert/focus contracts, reduced motion, 200% text reflow and optional short-screen onboarding.");
}

async function verifyCarePlanTrust(browser, errors) {
  const context = await browser.newContext({ viewport: { width: 412, height: 915 }, locale: "fa-IR" });
  context.setDefaultTimeout(12_000);
  const page = await context.newPage();
  page.on("pageerror", error => errors.push(error.message));
  await page.goto(`${origin}/?autostart=1#/treatment-plan`, { waitUntil: "networkidle" });
  const screen = page.getByTestId("treatment-plan-screen");
  await screen.waitFor();
  await page.getByTestId("treatment-empty-state").waitFor();
  assert.match(await page.getByTestId("treatment-progress-counter").textContent(), /۰\s*\/\s*۰/);
  assert.equal(await page.locator('article[data-testid^="treatment-med-"]').count(), 0, "A fresh care plan must not contain demo medicines");
  assert.doesNotMatch(await screen.textContent(), /داروخانه شبانه‌روزی دکتر رضایی|۱۲۳۴۵|آزیترومایسین|آراد/);
  const reminder = screen.getByRole("checkbox", { name: /یادآور خودکار/ });
  assert.ok(await reminder.isDisabled(), "An unimplemented reminder service cannot be enabled");
  assert.equal(await reminder.isChecked(), false);
  await page.screenshot({ path: "web-artifacts/care-plan-empty.png", fullPage: true });

  await page.getByTestId("treatment-add-med-btn").click();
  const editor = page.getByRole("dialog", { name: "افزودن داروی جدید به برنامه درمانی", exact: true });
  await editor.waitFor();
  const slots = page.getByLabel("تعداد نوبت قابل ثبت در روز (۱ تا ۶)", { exact: true });
  const schedule = page.getByLabel("دستور زمان‌بندی مصرف", { exact: true });
  const food = page.getByLabel("دستور درباره غذا (اختیاری)", { exact: true });
  const dose = page.getByLabel("مقدار هر نوبت طبق دستور", { exact: true });
  for (const input of [slots, schedule, food, dose]) {
    assert.equal(await input.inputValue(), "", "A new entry must not invent a dose, schedule, food instruction or count");
  }
  const save = editor.getByRole("button", { name: "ذخیره در برنامه درمانی", exact: true });
  const customName = "داروی ثبت‌شده مراقب — نمونه آزمایشی";
  await page.getByLabel("نام دارو و غلظت دقیق", { exact: true }).fill(customName);
  await dose.fill("مقدار رونویسی‌شده از برچسب آزمایشی");
  await slots.fill("2");
  await save.click();
  assert.ok(await editor.isVisible(), "A medication cannot be saved without entered timing instructions");
  assert.equal(await schedule.evaluate(input => input.validity.valueMissing), true);
  await schedule.fill("زمان‌بندی واردشده از دستور مکتوب مراقب");
  for (const invalidCount of ["", "0", "7", "1.5"]) {
    await slots.fill(invalidCount);
    await save.click();
    assert.ok(await editor.isVisible(), `Slot count ${JSON.stringify(invalidCount)} must not be silently defaulted or clamped`);
    assert.equal(await slots.evaluate(input => input.validity.valid), false);
    assert.equal((await savedSnapshot(page)).treatmentPlan.medications.length, 0);
  }
  await slots.fill("2");
  await save.click();
  await editor.waitFor({ state: "hidden" });
  await page.waitForFunction(() => JSON.parse(localStorage.getItem("mano_kid_v2_store") || "null")?.treatmentPlan.medications.length === 1);
  const created = (await savedSnapshot(page)).treatmentPlan.medications[0];
  assert.equal(created.nameWithStrengthFa, customName);
  assert.equal(created.dailyDoseSlots, 2);
  assert.equal(created.foodRelationFa, "", "Optional food advice must remain absent unless entered");
  assert.equal(created.intervalHours, null, "A slot count must not become an inferred administration interval");
  await page.getByTestId(`treatment-slot-${created.id}-1`).click();
  assert.match(await page.getByTestId("treatment-progress-counter").textContent(), /۱\s*\/\s*۲/);
  await page.waitForFunction(id => JSON.parse(localStorage.getItem("mano_kid_v2_store") || "null")?.treatmentPlan.medications.find(med => med.id === id)?.completedSlotTimesFa[1], created.id);

  // Simulate an older saved installation: preserve edited examples in an archive,
  // while keeping genuine caregiver entries and their existing checkoffs active.
  const legacyIds = ["rx_azithromycin_200", "rx_acetaminophen_160", "rx_saline_spray_09"];
  await page.evaluate(ids => {
    const state = JSON.parse(localStorage.getItem("mano_kid_v2_store"));
    const legacyRows = ids.map((id, index) => ({
      id, nameWithStrengthFa: `متن ویرایش‌شده نمونه قدیمی ${index + 1}`,
      form: "SYRUP_SUSPENSION", doseAmountFa: `مقدار قدیمی حفظ‌شده ${index + 1}`,
      intervalHours: null, scheduleLabelFa: `زمان‌بندی قدیمی حفظ‌شده ${index + 1}`,
      foodRelationFa: "دستور قدیمی درباره غذا", counselingNoteFa: "یادداشت تاریخی مراقب",
      isWarningHighlight: false, dailyDoseSlots: 2, completedSlotTimesFa: { 1: "۱۰:۳۰" },
    }));
    state.treatmentPlan = {
      ...state.treatmentPlan,
      pharmacyNameFa: "داروخانه شبانه‌روزی دکتر رضایی",
      pharmacistBadgeFa: "تأیید دکتر داروساز (نظام: ۱۲۳۴۵)",
      fallbackChildSummaryFa: "آراد عزیز • ۴ ساله (۱۶ کیلوگرم)",
      prescriptionDateFa: "نسخه فعال • قابل ویرایش بر اساس دستور پزشک",
      smartRemindersEnabled: true,
      legacyUnreviewedMedications: [],
      medications: [...legacyRows, ...state.treatmentPlan.medications],
    };
    localStorage.setItem("mano_kid_v2_store", JSON.stringify(state));
  }, legacyIds);
  await page.reload({ waitUntil: "networkidle" });
  const archive = page.getByTestId("treatment-legacy-notice");
  await archive.waitFor();
  assert.equal(await page.locator('article[data-testid^="treatment-med-"]').count(), 1);
  await page.getByTestId(`treatment-med-${created.id}`).waitFor();
  for (const id of legacyIds) {
    assert.equal(await page.getByTestId(`treatment-med-${id}`).count(), 0, "Old examples must never remain active medications");
    assert.equal(await page.getByTestId(`treatment-slot-${id}-1`).count(), 0, "Archived records must not expose daily checkoff controls");
  }
  assert.match(await page.getByTestId("treatment-progress-counter").textContent(), /۱\s*\/\s*۲/);
  await archive.locator("summary").click();
  for (let index = 1; index <= legacyIds.length; index++) {
    assert.match(await archive.textContent(), new RegExp(`متن ویرایش‌شده نمونه قدیمی ${index}`));
    assert.match(await archive.textContent(), new RegExp(`مقدار قدیمی حفظ‌شده ${index}`));
  }
  assert.doesNotMatch(await screen.textContent(), /داروخانه شبانه‌روزی دکتر رضایی|۱۲۳۴۵|آراد/);
  assert.ok(await reminder.isDisabled());
  assert.equal(await reminder.isChecked(), false);
  await page.getByTestId(`treatment-slot-${created.id}-2`).click();
  assert.match(await page.getByTestId("treatment-progress-counter").textContent(), /۲\s*\/\s*۲/);
  await page.waitForFunction(id => {
    const plan = JSON.parse(localStorage.getItem("mano_kid_v2_store") || "null")?.treatmentPlan;
    return plan?.medications.find(med => med.id === id)?.completedSlotTimesFa[2] && plan.legacyUnreviewedMedications?.length === 3;
  }, created.id);
  const migrated = (await savedSnapshot(page)).treatmentPlan;
  assert.equal(migrated.pharmacyNameFa, "");
  assert.equal(migrated.pharmacistBadgeFa, "");
  assert.equal(migrated.smartRemindersEnabled, false);
  assert.equal(migrated.legacyUnreviewedMedications[0].doseAmountFa, "مقدار قدیمی حفظ‌شده 1");
  await page.screenshot({ path: "web-artifacts/care-plan-legacy-archive.png", fullPage: true });
  await page.reload({ waitUntil: "networkidle" });
  await page.getByTestId(`treatment-med-${created.id}`).waitFor();
  assert.equal((await savedSnapshot(page)).treatmentPlan.legacyUnreviewedMedications.length, 3, "Repeated migration must not duplicate or erase archived entries");
  assert.match(await page.getByTestId("treatment-progress-counter").textContent(), /۲\s*\/\s*۲/);
  await context.close();
  console.log("Care-plan trust browser checks passed: empty draft, honest reminders, explicit instructions/counts, preserved legacy archive, active custom checkoffs.");
}

async function verifyClinicalPrivacyAndDeletion(browser, errors) {
  const context = await browser.newContext({ viewport: { width: 412, height: 915 }, locale: "fa-IR" });
  context.setDefaultTimeout(12_000);
  context.on("page", tab => tab.on("pageerror", error => errors.push(error.message)));
  const browserAiRequests = [];
  await context.route("**/api/gemini/**", async route => {
    browserAiRequests.push({ url: route.request().url(), method: route.request().method(), body: route.request().postData() });
    await route.abort("blockedbyclient");
  });
  const page = await context.newPage();
  const originalAlias = "qa-only-child-alias-A";
  const originalAllergy = "qa-only-allergy-A";
  await page.goto(`${origin}/?autostart=1#/my-child`, { waitUntil: "networkidle" });
  await page.getByTestId("my-child-screen").waitFor();
  await page.locator("#child-alias").fill(originalAlias);
  await page.locator("#child-age-months").fill("2.7");
  await page.locator("#child-weight").fill("5");
  await page.locator("#child-allergies").fill(originalAllergy);
  await page.getByRole("button", { name: "ذخیره مشخصات کودک", exact: true }).click();
  await waitForSavedAge(page, 2.7);
  assert.equal((await savedSnapshot(page)).childProfile.ageBand, "NEWBORN", "Fractional infant age must determine the newborn age band");
  await openRoute(page, "fever-dose", "fever-dose-screen");
  assert.equal(await page.getByTestId("dose-age-input").inputValue(), "2.7", "Profile age must never round 2.7 months up to three");
  assert.ok(await page.getByTestId("dose-age-input").getAttribute("readonly") !== null, "Profile age must be corrected through the profile rather than silently overridden");
  assert.equal(await page.getByTestId("dose-calculated-result").count(), 0);
  await page.getByTestId("dose-blocked-alert").waitFor();

  // Enter a real competing source, then verify that an inconsistent edit is rejected.
  await openRoute(page, "vaccines", "vaccines-screen");
  await page.getByTestId("vaccine-birthdate-input").fill("2025-05-01");
  await page.getByTestId("vaccine-birthdate-apply-btn").click();
  await page.waitForFunction(() => JSON.parse(localStorage.getItem("mano_kid_v2_store") || "null")?.vaccineBirthDateEpochMs != null);
  await openRoute(page, "my-child", "my-child-screen");
  await page.locator("#child-age-months").fill("2");
  await page.getByRole("button", { name: "ذخیره مشخصات کودک", exact: true }).click();
  await page.getByTestId("child-age-error").waitFor();
  assert.notEqual((await savedSnapshot(page)).childProfile.ageMonthsExact, 2, "A conflicting profile edit must not be silently accepted");

  // Represent a legacy record that already contains contradictory age sources.
  await page.evaluate(() => {
    const state = JSON.parse(localStorage.getItem("mano_kid_v2_store"));
    state.childProfile.ageMonthsExact = 2;
    state.childProfile.ageBand = "TODDLER_1_3Y";
    state.childProfile.birthDateEpochMs = Date.UTC(2025, 4, 1);
    state.vaccineBirthDateEpochMs = state.childProfile.birthDateEpochMs;
    localStorage.setItem("mano_kid_v2_store", JSON.stringify(state));
  });
  await page.reload({ waitUntil: "networkidle" });
  await openRoute(page, "fever-dose", "fever-dose-screen");
  await page.getByTestId("age-profile-conflict").waitFor();
  assert.equal(await page.getByTestId("dose-calculated-result").count(), 0, "Contradictory legacy data must not unlock a home-treatment dose");
  assert.equal(await page.getByTestId("dose-label-confirm-checkbox").count(), 0);
  await openRoute(page, "my-child", "my-child-screen");
  await page.getByTestId("child-clear-birthdate").check();
  await page.getByRole("button", { name: "ذخیره مشخصات کودک", exact: true }).click();
  await page.waitForFunction(() => {
    const state = JSON.parse(localStorage.getItem("mano_kid_v2_store") || "null");
    return state?.childProfile?.ageMonthsExact === 2 && state.vaccineBirthDateEpochMs == null && state.childProfile.birthDateEpochMs == null;
  });

  // Older snapshots may retain an outdated age band; numerical age must still win.
  await page.evaluate(() => {
    const state = JSON.parse(localStorage.getItem("mano_kid_v2_store"));
    state.childProfile.ageBand = "TODDLER_1_3Y";
    localStorage.setItem("mano_kid_v2_store", JSON.stringify(state));
  });
  await page.reload({ waitUntil: "networkidle" });
  await openRoute(page, "home", "home-screen");
  await page.getByTestId("home-screen").waitFor();
  await page.getByTestId("timeline-add-temp-btn").click();
  const tempDialog = page.getByRole("dialog", { name: "ثبت دمای جدید کودک" });
  assert.equal(await page.getByTestId("timeline-temp-input").inputValue(), "", "Temperature must not be fabricated");
  assert.equal(await page.getByTestId("timeline-temp-site-select").inputValue(), "");
  assert.equal(await page.getByTestId("timeline-temp-time-input").inputValue(), "");
  assert.ok(await page.getByTestId("timeline-temp-save-btn").isDisabled(), "An empty temperature form cannot save a measurement");
  assert.equal((await savedSnapshot(page)).feverLogs.length, 0);
  await tempDialog.getByRole("button", { name: "بستن", exact: true }).click();

  await page.getByTestId("timeline-add-dose-btn").click();
  const doseDialog = page.getByRole("dialog", { name: "ثبت نوبت داروی تب‌بر" });
  assert.equal(await page.getByTestId("timeline-dose-form-input").inputValue(), "", "A drug formulation must not be fabricated");
  assert.equal(await page.getByTestId("timeline-dose-amount-input").inputValue(), "", "An administered amount must not be fabricated");
  assert.equal(await page.getByTestId("timeline-dose-unit-select").inputValue(), "");
  assert.equal(await page.getByTestId("timeline-dose-time-input").inputValue(), "");
  assert.ok(await page.getByTestId("timeline-dose-save-btn").isDisabled(), "An empty dose form cannot save an administration");
  assert.equal((await savedSnapshot(page)).doseLogs.length, 0);
  await page.getByTestId("timeline-dose-acetaminophen-btn").click();
  await page.getByTestId("timeline-dose-form-input").fill("شربت آزمایشی ۱۲۰mg/5mL");
  await page.getByTestId("timeline-dose-amount-input").fill("1");
  await page.getByTestId("timeline-dose-unit-select").selectOption("mL");
  await page.getByTestId("timeline-dose-time-input").fill(await recordedTime(page));
  await page.getByTestId("timeline-dose-save-btn").click();
  await doseDialog.waitFor({ state: "hidden" });
  await page.waitForFunction(() => JSON.parse(localStorage.getItem("mano_kid_v2_store") || "null")?.doseLogs.length === 1);

  await page.getByTestId("timeline-add-temp-btn").click();
  await page.getByTestId("timeline-temp-input").fill("38.1");
  await page.getByTestId("timeline-temp-site-select").selectOption("زیر بغل (دیجیتال)");
  await page.getByTestId("timeline-temp-time-input").fill(await recordedTime(page));
  await page.getByTestId("timeline-temp-save-btn").click();
  const urgent = page.getByRole("alertdialog", { name: "هشدار دمای بالا ثبت شد" });
  await urgent.waitFor();
  await page.keyboard.press("Escape");
  assert.ok(await urgent.isVisible(), "Escape must not silently dismiss an urgent medical alert");
  await page.keyboard.press("Control+k");
  assert.ok(await urgent.isVisible());
  assert.equal(await page.locator("#mano-search-dialog").count(), 0, "Global search cannot obscure an urgent alert");
  assert.equal(await page.locator("#mano-tools-dialog").count(), 0);
  assert.match(await urgent.textContent(), /نوزاد زیر ۳ ماه|معاینه فوری/);
  assert.equal(await urgent.getByRole("button", { name: "محاسبه‌گر ایمن تب‌بر بر پایه وزن", exact: true }).count(), 0, "A stale toddler band cannot expose an infant self-treatment action");
  await urgent.getByRole("button", { name: "متوجه شدم", exact: true }).click();
  await page.getByTestId("timeline-add-temp-btn").click();
  assert.equal(await page.getByTestId("timeline-temp-input").inputValue(), "", "A new record must not reuse a previous measured value");
  await tempDialog.getByRole("button", { name: "بستن", exact: true }).click();
  console.log("Extended clinical browser checks passed: fractional age, DOB reconciliation, empty timeline forms, exact-age neonatal fever.");

  // Same-origin tabs must both forget saved records and dirty form drafts.
  await openRoute(page, "my-child", "my-child-screen");
  const tabB = await context.newPage();
  await tabB.goto(`${origin}/?autostart=1#/my-child`, { waitUntil: "networkidle" });
  assert.equal(await tabB.locator("#child-alias").inputValue(), originalAlias);
  await tabB.locator("#child-alias").fill("qa-unsaved-child-alias-B");
  await tabB.locator("#child-allergies").fill("qa-unsaved-allergy-B");
  await page.getByRole("button", { name: "پاک‌سازی کامل داده‌های محلی", exact: true }).click();
  await page.getByRole("button", { name: "تایید حذف همه‌چیز", exact: true }).click();
  for (const tab of [page, tabB]) {
    await tab.waitForFunction(() => {
      const alias = document.querySelector("#child-alias");
      const allergy = document.querySelector("#child-allergies");
      return alias && allergy && !alias.value.startsWith("qa-") && allergy.value === "";
    });
    assert.equal(await tab.locator("#child-age-months").inputValue(), "", "Deletion must reset visible age drafts in every tab");
  }
  await tabB.getByTestId("open-hub-drawer-btn").click();
  await tabB.getByTestId("theme-toggle-btn").click();
  await tabB.waitForFunction(() => JSON.parse(localStorage.getItem("mano_kid_v2_store") || "null")?.theme === "dark");
  const afterTheme = await savedSnapshot(tabB);
  assert.ok(!JSON.stringify(afterTheme).includes("qa-"), "Editing an old tab must not resurrect deleted names, allergies or drafts");
  for (const key of ["feverLogs", "doseLogs", "growthMeasurements", "savedVisitDrafts", "savedSyrupScans", "nutritionPlans", "pharmacistConsultations", "careCards"]) {
    assert.equal(afterTheme[key].length, 0, `Deleted ${key} must stay empty after another tab writes`);
  }
  await tabB.getByRole("button", { name: "بستن منو", exact: true }).click();
  await tabB.close();
  console.log("Cross-tab deletion browser checks passed: saved history and visible drafts cleared, old-tab theme edit cannot restore them.");

  await openRoute(page, "nutrition", "nutrition-screen");
  assert.equal(await page.getByTestId("nutrition-age-input").inputValue(), "", "Nutrition must not infer a fictional age after deletion");
  await page.getByTestId("nutrition-age-required-lock").waitFor();
  assert.equal(await page.getByTestId("nutrition-generate-btn").count(), 0);
  await openRoute(page, "my-child", "my-child-screen");
  await page.locator("#child-alias").fill("qa-only-ai-private-alias");
  await page.locator("#child-age-months").fill("18");
  await page.locator("#child-weight").fill("11");
  await page.locator("#child-allergies").fill("ندارد");
  await page.getByRole("button", { name: "ذخیره مشخصات کودک", exact: true }).click();
  await waitForSavedAge(page, 18);

  const localAiError = /هیچ اطلاعاتی ارسال نشد/;
  await openRoute(page, "nutrition", "nutrition-screen");
  await page.getByTestId("nutrition-generate-btn").click();
  await page.getByText(localAiError).first().waitFor();
  await openRoute(page, "symptoms", "symptom-navigator-screen");
  await page.getByTestId("triage-temp-input").fill("38.1");
  await page.getByTestId("triage-duration-input").fill("12");
  await page.getByTestId("triage-ai-checklist-btn").click();
  await page.getByText(localAiError).first().waitFor();
  await page.getByTestId("visit-draft-generate-btn").click();
  assert.equal(await page.getByText(localAiError).count(), 2, "Both safety checklist and visit draft must report local rejection");

  await openRoute(page, "faq", "faq-screen");
  const canonicalQuestion = await page.locator("article button[aria-expanded]").first().innerText();
  await page.locator("#documented-qa-input").fill(canonicalQuestion);
  await page.getByRole("button", { name: "دریافت پاسخ مستند", exact: true }).click();
  await page.getByText(localAiError).first().waitFor();
  await openRoute(page, "syrup-scanner", "syrup-scanner-screen");
  await page.locator('input[type="file"]').setInputFiles({
    name: "synthetic-pixel.png", mimeType: "image/png",
    buffer: Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=", "base64"),
  });
  await page.getByText(localAiError).first().waitFor();
  assert.deepEqual(browserAiRequests, [], "Every browser AI action must reject locally without even attempting a POST or sending child context");
  console.log("Browser AI privacy checks passed: five real UI actions, zero AI network requests or sensitive bodies.");
  await context.close();
}

let browser;
await mkdir(new URL("../web-artifacts/", import.meta.url), { recursive: true });
try {
  await waitForServer();
  const apiResponse = await fetch(`${origin}/api/gemini/visit-draft`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({}),
  });
  assert.equal(apiResponse.status, 503, "Clinical AI must be unavailable by default, even with a server token configured");
  const auditResponse = await fetch(`${origin}/api/ai-gateway/audit`);
  assert.ok([401, 403, 503].includes(auditResponse.status), "Anonymous users must not read gateway audit events");
  assert.ok(!(await (await fetch(`${origin}/api/health`)).text()).includes(serverOnlyToken), "Health response must not disclose server credentials");

  browser = await browserType.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 412, height: 915 }, deviceScaleFactor: 1, locale: "fa-IR" });
  const page = await context.newPage();
  const errors = [];
  page.on("pageerror", error => errors.push(error.message));

  await page.goto(`${origin}/?autostart=1`, { waitUntil: "networkidle" });
  await page.getByTestId("home-screen").waitFor({ state: "visible" });
  assert.equal(await page.locator("html").getAttribute("dir"), "rtl");
  assert.ok(await page.getByRole("navigation", { name: "ناوبری موبایل" }).isVisible());
  assert.equal(await page.locator(".shell").count(), 0, "Primary entry must mount React rather than the separate design demo");
  assert.match(await page.getByTestId("home-child-age").textContent(), /ثبت نشده|نامشخص/);
  assert.doesNotMatch(await page.getByTestId("home-vaccine-next-summary").textContent(), /موعد بعدی/);
  const brokenHomeImages = await page.locator("img").evaluateAll(images => images.filter(image => !image.complete || image.naturalWidth === 0).map(image => image.getAttribute("src")));
  assert.deepEqual(brokenHomeImages, [], "The main product's local visual assets must load");

  const mainHtml = await readFile(new URL("../dist/index.html", import.meta.url), "utf8");
  const bundleUrls = [...mainHtml.matchAll(/(?:src|href)="([^\"]+\.js)"/g)].map(match => match[1]);
  assert.ok(bundleUrls.length > 0, "Build must contain a bundled React entry");
  for (const bundleUrl of bundleUrls) {
    const code = await (await fetch(`${origin}${bundleUrl}`)).text();
    assert.ok(!code.includes(serverOnlyToken), "Server token must not be bundled into browser JavaScript");
  }

  for (const width of [360, 412]) {
    await page.setViewportSize({ width, height: 820 });
    await assertNoPageOverflow(page, `React home ${width}px`);
    await page.screenshot({ path: `web-artifacts/react-home-${width}.png`, fullPage: true });
  }

  await page.getByTestId("home-cta-fever-dose").click();
  const age = page.getByTestId("dose-age-input");
  const weight = page.getByTestId("dose-weight-input");
  const checkbox = page.getByTestId("dose-label-confirm-checkbox");
  const result = page.getByTestId("dose-calculated-result");
  const save = page.getByTestId("dose-save-timeline-btn");
  await age.waitFor({ state: "visible" });
  assert.equal(await age.inputValue(), "", "A missing age must remain empty");
  assert.equal(await weight.inputValue(), "", "A missing measured weight must remain empty");
  assert.equal(await result.count(), 0, "A fresh profile must not receive a numerical dose");
  assert.equal(await checkbox.count(), 0, "Bottle confirmation cannot bypass missing age or weight");
  assert.equal(await save.count(), 0, "A fabricated dose cannot be saved");

  await age.fill("18");
  assert.equal(await result.count(), 0, "Age alone must not reveal a dose");
  await weight.fill("11");
  await checkbox.waitFor({ state: "visible" });
  assert.equal(await result.count(), 0, "Valid measurements still require bottle confirmation");
  await checkbox.check();
  await result.waitFor({ state: "visible" });
  assert.ok(await save.isVisible());
  assert.ok(await save.isDisabled(), "A calculated dose is not an administered dose until explicitly confirmed");
  await page.getByTestId("dose-administered-confirm").check();
  assert.equal(await save.isDisabled(), false);

  await weight.fill("12");
  await result.waitFor({ state: "hidden" });
  assert.equal(await checkbox.isChecked(), false, "Changing measured weight must invalidate previous confirmation");
  await checkbox.check();
  await result.waitFor({ state: "visible" });
  assert.equal(await page.getByTestId("dose-administered-confirm").isChecked(), false, "Changing the proposed dose must also invalidate administration confirmation");
  assert.ok(await save.isDisabled());
  await age.fill("19");
  await result.waitFor({ state: "hidden" });
  assert.equal(await checkbox.isChecked(), false, "Changing age must invalidate previous confirmation");
  await weight.fill("");
  await page.getByTestId("dose-blocked-alert").waitFor({ state: "visible" });
  assert.equal(await checkbox.count(), 0, "Clearing a measurement must restore the missing-data lock");

  await age.fill("2");
  await weight.fill("5");
  assert.match(await page.getByTestId("dose-blocked-alert").textContent(), /۳ ماه|معاینه فوری/);
  assert.equal(await result.count(), 0, "A two-month-old must not receive a home treatment dose");
  assert.equal(await checkbox.count(), 0);

  await age.fill("4");
  await page.getByTestId("formulation-btn-ibuprofen_syrup_100_5").click();
  assert.match(await page.getByTestId("dose-blocked-alert").textContent(), /۶ ماه/);
  assert.equal(await result.count(), 0, "Ibuprofen must stay locked below six months");
  for (const width of [360, 412]) {
    await page.setViewportSize({ width, height: 820 });
    await assertNoPageOverflow(page, `React dose ${width}px`);
  }
  await page.screenshot({ path: "web-artifacts/react-dose-infant-locked.png", fullPage: true });

  await page.getByRole("navigation", { name: "ناوبری موبایل" }).getByRole("button", { name: "خانه", exact: true }).click();
  await page.getByTestId("home-cta-symptoms").click();
  assert.equal(await page.getByTestId("triage-temp-input").inputValue(), "", "Triage must not invent a measured temperature");
  assert.equal(await page.getByTestId("triage-duration-input").inputValue(), "", "Triage must not invent symptom duration");
  assert.equal(await page.getByTestId("triage-result-GENERAL_EDUCATIONAL").count(), 0, "Unknown age and measurements must not produce a home-care conclusion");
  await page.getByTestId("triage-temp-input").fill("38.1");
  await page.getByTestId("triage-duration-input").fill("12");
  assert.equal(await page.getByTestId("triage-result-GENERAL_EDUCATIONAL").count(), 0, "Recorded fever without an explicitly selected age must not assume a toddler");
  await page.getByTestId("triage-age-NEWBORN").click();
  await page.getByTestId("triage-result-URGENT_EMERGENCY").waitFor({ state: "visible" });
  assert.match(await page.getByTestId("triage-outcome-panel").textContent(), /فوری|اورژانس/);
  await page.screenshot({ path: "web-artifacts/react-infant-fever-triage.png", fullPage: true });

  await verifyClinicalPrivacyAndDeletion(browser, errors);
  await verifyCarePlanTrust(browser, errors);
  await verifyKeyboardAndTaskNavigation(browser, errors);
  await verifyShortScreenCareDialogs(browser, errors);
  await verifyRemainingFormDialogs(browser, errors);

  // Keep the previous approved visual demo available, isolated and clearly separate.
  await page.goto(`${origin}/preview/`, { waitUntil: "networkidle" });
  await page.locator(".hero .robot").waitFor({ state: "visible" });
  assert.equal(await page.getByTestId("home-screen").count(), 0);
  assert.equal(await page.locator(".profilename").textContent(), "پرونده کودک");
  await page.locator(".profile").click();
  await page.locator('input[name="name"]').fill("کودک آزمایشی");
  await page.locator('input[name="months"]').fill("2");
  await page.locator('#profile-form button[type="submit"]').click();
  assert.match(await page.locator("#care-screen").textContent(), /کودک آزمایشی/);
  await page.reload({ waitUntil: "networkidle" });
  assert.equal(await page.locator('input[name="name"]').inputValue(), "", "Design demo profile must clear after a reload");
  await page.locator('[data-route="home"]').first().click();
  assert.equal(await page.locator(".profilename").textContent(), "پرونده کودک", "Design demo data must clear after a reload");
  for (const width of [360, 412]) {
    await page.setViewportSize({ width, height: 820 });
    await assertNoPageOverflow(page, `Design demo ${width}px`);
    await page.screenshot({ path: `web-artifacts/design-preview-${width}.png`, fullPage: true });
  }

  assert.deepEqual(errors, [], "Both entrypoints must run without uncaught browser errors");
  await context.close();
  console.log(`Browser checks passed (${browserName}): production React entry, missing-input and infant dose locks, reconfirmation, urgent fever, protected AI/audit APIs, separate design preview, responsive 360/412px.`);
} finally {
  await browser?.close();
  if (server.exitCode === null) {
    server.kill("SIGTERM");
    await once(server, "exit");
  }
}
