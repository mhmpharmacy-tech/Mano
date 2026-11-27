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

  await weight.fill("12");
  await result.waitFor({ state: "hidden" });
  assert.equal(await checkbox.isChecked(), false, "Changing measured weight must invalidate previous confirmation");
  await checkbox.check();
  await result.waitFor({ state: "visible" });
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
