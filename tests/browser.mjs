import { spawn } from "node:child_process";
import { once } from "node:events";
import { mkdir, readFile } from "node:fs/promises";
import { createServer } from "node:net";
import { fileURLToPath } from "node:url";
import { chromium } from "playwright";
import assert from "node:assert/strict";

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
  assert.match(await urgent.textContent(), /نوزاد زیر ۳ ماه|معاینه فوری/);
  assert.equal(await urgent.getByRole("button", { name: "محاسبه‌گر ایمن تب‌بر بر پایه وزن", exact: true }).count(), 0, "A stale toddler band cannot expose an infant self-treatment action");
  await urgent.getByRole("button", { name: "بستن هشدار", exact: true }).click();
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

  browser = await chromium.launch({ headless: true });
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
  console.log("Browser checks passed: production React entry, missing-input and infant dose locks, reconfirmation, urgent fever, protected AI/audit APIs, separate design preview, responsive 360/412px.");
} finally {
  await browser?.close();
  if (server.exitCode === null) {
    server.kill("SIGTERM");
    await once(server, "exit");
  }
}
