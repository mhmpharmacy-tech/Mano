import { spawn } from "node:child_process";
import { mkdir } from "node:fs/promises";
import { chromium } from "playwright";
import assert from "node:assert/strict";

const port = 43591;
const origin = "http://127.0.0.1:" + port;
const server = spawn(process.execPath, ["server.mjs"], {
  env: { ...process.env, PORT: String(port) }, stdio: ["ignore", "pipe", "pipe"]
});
let serverErrors = "";
server.stderr.on("data", c => { serverErrors += c.toString(); });
async function live() {
  for (let t=0;t<50;t++){
    try {const r=await fetch(origin+"/"); if(r.ok) return;}catch{}
    await new Promise(res=>setTimeout(res,200));
  }
  throw Error("Preview server did not become ready: "+serverErrors);
}
const browser = await chromium.launch({headless:true});
await mkdir("web-artifacts",{recursive:true});
try {
  await live();
  const context = await browser.newContext({viewport:{width:412,height:915},deviceScaleFactor:2,locale:"fa-IR"});
  const page = await context.newPage();
  const errors=[];
  page.on("pageerror",e=>errors.push(e.message));
  await page.goto(origin+"/",{waitUntil:"networkidle"});
  await page.locator(".hero .robot").waitFor({state:"visible"});
  assert.equal(await page.locator(".robot").count(),1,"MANO mascot should appear once on home");
  assert.ok(await page.locator(".profile").isVisible());
  assert.ok(await page.locator(".primary .action.symptoms").isVisible());
  assert.ok(await page.locator(".primary .action.pharmacist").isVisible());
  assert.ok(await page.locator(".bottom").isVisible());
  assert.equal(await page.locator(".bottom .tab").count(),5);
  assert.equal(await page.locator(".profilename").textContent(),"پرونده کودک");
  await page.screenshot({path:"web-artifacts/home-412.png",fullPage:true});

  await page.locator(".profile").click();
  await page.locator('input[name="name"]').fill("کودک آزمایشی");
  await page.locator('input[name="months"]').fill("2");
  await page.locator('#profile-form button[type="submit"]').click();
  assert.ok((await page.locator("#care-screen").textContent()).includes("کودک آزمایشی"));
  await page.locator('[data-route="home"]').first().click();
  assert.ok((await page.locator(".profilename").textContent()).includes("کودک آزمایشی"));

  await page.locator(".primary .action.symptoms").click();
  await page.locator('select[name="topic"]').selectOption("تب");
  await page.locator('input[name="age"]').fill("2");
  await page.locator('input[name="temp"]').fill("38.1");
  await page.locator("#symptom-form button[type=submit]").click();
  assert.equal(await page.locator('select[name="topic"]').inputValue(),"تب","Symptom choice should persist after review");
  assert.equal(await page.locator('input[name="temp"]').inputValue(),"38.1","Temperature should persist after review");
  const triage = await page.locator(".care-message.danger").allTextContents();
  assert.ok(triage.some(s=>s.includes("ارزیابی فوری پزشکی")));
  await page.screenshot({path:"web-artifacts/triage-412.png",fullPage:true});

  await page.locator(".bottom button").filter({hasText:"داروساز"}).click();
  await page.locator('textarea[name="question"]').fill("سؤال آزمایشی درباره مصرف دارو");
  await page.locator("#question-form button[type=submit]").click();
  assert.ok((await page.locator("#care-screen").textContent()).includes("ارسال نشده"));
  await page.locator(".bottom button").filter({hasText:"رشد"}).click();
  await page.locator('input[name="kg"]').fill("10.3");
  await page.locator("#growth-form button[type=submit]").click();
  assert.ok((await page.locator("#care-screen").textContent()).includes("۱۰.۳"));

  await page.goto(origin+"/",{waitUntil:"networkidle"});
  assert.equal(await page.locator(".profilename").textContent(),"پرونده کودک","patient state must not persist across refresh");
  assert.ok((await page.locator(".caredesc").first().textContent()).includes("هنوز دارویی ثبت نشده"));

  for (const width of [360,412]) {
    await page.setViewportSize({width,height:780});
    await page.reload({waitUntil:"networkidle"});
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
    assert.ok(overflow < 3,"Unexpected horizontal overflow at "+width+": "+overflow);
    assert.ok(await page.locator(".bottom").isVisible());
    if(width===360)await page.screenshot({path:"web-artifacts/home-360.png",fullPage:true});
  }
  assert.deepEqual(errors,[],"No uncaught browser errors");
  await context.close();
  console.log("Browser checks passed: responsive 360/412px, mascot, nav, profile, triage, drafts, refresh safety");
} finally {
  await browser.close();
  server.kill("SIGTERM");
}
