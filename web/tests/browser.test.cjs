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
  assert.equal(await page.locator(".urgent-call").getAttribute("href"), "tel:115");

  await page.locator('input[name="warning"][value="breath"]').uncheck();
  await page.locator("#child-age").selectOption("under3");
  await page.locator("#child-temp").fill("۳۸٫۰");
  await page.locator('#redflags-form button[type="submit"]').click();
  assert.match(await page.locator("#screening-result").innerText(), /فوری/);
  assert.ok(await page.locator(".urgent-call").isVisible());

  await page.locator("#child-temp").fill("۳۸/۵");
  await page.locator('#redflags-form button[type="submit"]').click();
  assert.match(await page.locator("#screening-result").innerText(), /بررسی/);
  assert.equal(await page.locator(".urgent-call").count(), 0);

  await page.locator("#topic-search").fill("شربت");
  assert.equal(await page.locator(".path-card:visible").count(), 1);
  await page.locator("#topic-search").press("Escape");
  assert.equal(await page.locator(".path-card:visible").count(), 6);
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


test("Accessibility audit: WCAG 2.1 A/AA critical and serious issues", async () => {
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
  for (const target of [parentUrl, staffUrl]) {
    await page.goto(target);
    await page.addScriptTag({ path: require.resolve("axe-core/axe.min.js") });
    const violations = await page.evaluate(async () => {
      const result = await window.axe.run(document, {
        runOnly: { type: "tag", values: ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"] }
      });
      return result.violations.map(v => ({
        id: v.id, impact: v.impact, description: v.description,
        nodes: v.nodes.map(n => ({target: n.target, failure: n.failureSummary})).slice(0, 10)
      }));
    });
    fs.mkdirSync(out, { recursive: true });
    fs.writeFileSync(path.join(out, target === parentUrl ? "a11y-parent.json" : "a11y-staff.json"),
      JSON.stringify(violations, null, 2));
    const blocking = violations.filter(v => v.impact === "critical" || v.impact === "serious");
    assert.deepEqual(blocking, [], "Serious accessibility barriers on " + target);
  }
  await page.close();
});

test("Two article journeys, age band routing, responsive layouts and accessibility",async()=>{
  const page=await browser.newPage({viewport:{width:390,height:844}});
  const errors=[];
  page.on("pageerror",e=>errors.push(e.message));
  await page.goto(parentUrl);
  await page.locator("#child-age").selectOption("threeToSix");
  await page.locator("#child-temp").fill("۳۹");
  await page.locator('#redflags-form button[type="submit"]').click();
  assert.ok((await page.locator("#screening-result").getAttribute("class")).includes("prompt"));
  assert.equal(await page.locator("#screening-result .urgent-call").count(),0);
  await page.locator('input[name="warning"][value="rash"]').check();
  await page.locator('#redflags-form button[type="submit"]').click();
  assert.equal(await page.locator("#screening-result .urgent-call").getAttribute("href"),"tel:115");
  await page.locator('a[href="./fever-in-children.html"]').first().click();
  assert.ok(page.url().endsWith("fever-in-children.html"));
  await page.locator(".article-question summary").first().click();
  assert.ok(await page.locator(".article-question p").first().isVisible());
  for(const {path:file} of JSON.parse(fs.readFileSync(path.join(root,"clinical-content-status.json"),"utf8")).draftArticles){
    await page.goto(pathToFileURL(path.join(root,file)).href);
    for(const width of [320,390,430,768,1366]){
      await page.setViewportSize({width,height:844});
      assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+2),file+" overflow at "+width);
    }
    await page.setViewportSize({width:390,height:844});
    if(file.startsWith("liquid")) {
      await page.locator(".medicine-checklist input").first().check();
      assert.ok(await page.locator(".medicine-checklist input").first().isChecked());
      assert.equal(await page.locator("form").count(),0);
    }
    await page.addScriptTag({path:require.resolve("axe-core/axe.min.js")});
    const violations=await page.evaluate(async()=> (await window.axe.run(document,{runOnly:{type:"tag",values:["wcag2a","wcag2aa","wcag21a","wcag21aa"]}})).violations.map(v=>({id:v.id,impact:v.impact,nodes:v.nodes.map(n=>({target:n.target,summary:n.failureSummary}))})));
    fs.writeFileSync(path.join(out,file.replace(".html","-a11y.json")),JSON.stringify(violations,null,2));
    assert.deepEqual(violations.filter(v=>v.impact==="serious"||v.impact==="critical"),[],file);
    await page.screenshot({path:path.join(out,file.replace(".html","-mobile.png")),fullPage:true,animations:"disabled"});
    await page.setViewportSize({width:1366,height:900});
    await page.screenshot({path:path.join(out,file.replace(".html","-desktop.png")),fullPage:true,animations:"disabled"});
  }
  assert.deepEqual(errors,[]);
  await page.close();
});

test("Disabled JavaScript never enables native warning form submission",async()=>{
  const context=await browser.newContext({javaScriptEnabled:false,viewport:{width:390,height:844}});
  const page=await context.newPage();
  await page.goto(parentUrl);
  assert.ok(await page.locator("#warning-submit").isDisabled());
  assert.ok(await page.locator("#warning-nojs").isVisible());
  assert.equal(await page.locator('meta[http-equiv="Content-Security-Policy"]').getAttribute("content"),"form-action 'none'; base-uri 'none'");
  await page.locator('input[name="warning"][value="breath"]').check();
  await page.locator("#child-temp").fill("۳۸");
  await page.locator("#child-temp").press("Enter");
  assert.equal(page.url(),parentUrl,"No health-related query string or navigation");
  await context.close();
});

test("Encyclopedia search, reset, navigation and no-data article operation",async()=>{
 const page=await browser.newPage({viewport:{width:390,height:844}});
 await page.goto(parentUrl);
 assert.equal(await page.locator(".library-card:visible").count(),8);
 await page.locator("#library-search").fill("كم‌اشتهايي");
 // Exact visible Persian title is also guaranteed to find the article.
 await page.locator("#library-search").fill("کم اشتهایی");
 assert.equal(await page.locator(".library-card:visible").count(),1);
 await page.locator(".library-card:visible").click();
 assert.ok(page.url().endsWith("child-appetite-growth.html"));
 assert.equal(await page.locator("form,script").count(),0);
 await page.locator(".article-question summary").first().click();
 assert.ok(await page.locator(".article-question p").isVisible());
 await page.locator(".article-back").click();
 await page.locator("#library-search").fill("xyz-no-match");
 assert.equal(await page.locator(".library-card:visible").count(),0);
 assert.ok(await page.locator("#library-empty").isVisible());
 await page.locator("#library-search").press("Escape");
 assert.equal(await page.locator(".library-card:visible").count(),8);
 await page.close();
});

test("MANO Drug, Growth and Skin: functional filters, synthetic chart and responsive a11y",async()=>{
 const page=await browser.newPage({viewport:{width:390,height:844}});const errors=[];page.on("pageerror",e=>errors.push(e.message));
 await page.goto(parentUrl);assert.equal(await page.locator(".home-module-card").count(),6);
 await page.locator('a.home-module-card[href="./mano-drug.html"]').click();
 assert.equal(await page.locator("[data-entry]:visible").count(),6);
 await page.locator("#module-search").fill("استامينوفن");
 assert.equal(await page.locator("[data-entry]:visible").count(),1);
 await page.locator("#module-reset").click();
 await page.locator("#module-category").selectOption("rx");
 assert.equal(await page.locator("[data-entry]:visible").count(),1);
 await page.locator("[data-entry]:visible summary").click();
 assert.ok(await page.locator("[data-entry]:visible dl").isVisible());
 await page.locator('a[href="./mano-growth.html"]').click();
 assert.equal(await page.locator("#growth-rows tr").count(),5);
 await page.locator("#growth-example").selectOption("b");await page.locator("#growth-measure").selectOption("height");
 assert.match(await page.locator("#growth-caption").innerText(),/طول/);assert.equal(await page.locator("#growth-chart circle").count(),5);
 await page.locator('a[href="./mano-skin.html"]').click();assert.equal(await page.locator("[data-entry]:visible").count(),5);
 await page.locator("#module-category").selectOption("raised");assert.equal(await page.locator("[data-entry]:visible").count(),1);await page.locator("#module-reset").click();
 for(const name of ["mano-drug","mano-growth","mano-skin"]){
  await page.goto(pathToFileURL(path.join(root,name+".html")).href);
  for(const width of [320,390,430,768,1366]){
   await page.setViewportSize({width,height:844});
   assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+2),name+" overflow "+width);
  }
  await page.setViewportSize({width:390,height:844});await page.addScriptTag({path:require.resolve("axe-core/axe.min.js")});
  const violations=await page.evaluate(async()=> (await window.axe.run(document,{runOnly:{type:"tag",values:["wcag2a","wcag2aa","wcag21a","wcag21aa"]}})).violations.map(v=>({id:v.id,impact:v.impact,nodes:v.nodes.map(n=>({target:n.target,summary:n.failureSummary}))})));
  fs.writeFileSync(path.join(out,name+"-a11y.json"),JSON.stringify(violations,null,2));
  assert.deepEqual(violations.filter(v=>["critical","serious"].includes(v.impact)),[],name);
  await settleVisuals(page);await page.screenshot({path:path.join(out,name+"-mobile.png"),fullPage:true,animations:"disabled"});
  await page.setViewportSize({width:1366,height:900});await settleVisuals(page);await page.screenshot({path:path.join(out,name+"-desktop.png"),fullPage:true,animations:"disabled"});
 }
 assert.deepEqual(errors,[]);await page.close();
});

test("Module fallbacks: clinical content remains readable without JavaScript",async()=>{
 const context=await browser.newContext({javaScriptEnabled:false,viewport:{width:320,height:800}});
 const page=await context.newPage();
 for(const name of ["mano-drug","mano-skin"]){
  await page.goto(pathToFileURL(path.join(root,name+".html")).href);
  assert.ok(await page.locator("#module-search").isDisabled());
  assert.ok(await page.locator("[data-entry] summary").first().isVisible());
  await page.locator("[data-entry] summary").first().click();
  assert.ok(await page.locator("[data-entry] details[open]").first().isVisible());
 }
 await page.goto(pathToFileURL(path.join(root,"mano-growth.html")).href);
 assert.ok(await page.locator("#growth-example").isDisabled());
 assert.equal(await page.locator("#growth-rows tr").count(),5);
 await context.close();
});

test("Nutrition, Care and Connect: safe learning flows, responsive screens and accessible content",async()=>{
 const page=await browser.newPage({viewport:{width:390,height:844}});const errors=[];page.on("pageerror",e=>errors.push(e.message));
 await page.goto(parentUrl);
 await page.locator('a.home-module-card[href="./mano-nutrition.html"]').click();
 assert.equal(await page.locator("[data-entry]:visible").count(),5);
 await page.locator("#module-category").selectOption("safety");assert.equal(await page.locator("[data-entry]:visible").count(),2);
 await page.locator("#module-reset").click();await page.locator("#module-search").fill("عسل");assert.equal(await page.locator("[data-entry]:visible").count(),1);
 await page.locator('a[href="./mano-care.html"]').click();assert.equal(await page.locator("[data-entry]:visible").count(),5);
 await page.locator("#module-category").selectOption("warning");assert.equal(await page.locator("[data-entry]:visible").count(),2);
 await page.locator(".care-checklist input").first().check();assert.equal(await page.locator(".care-checklist input:checked").count(),1);
 await page.locator('a[href="./mano-connect.html"]').click();assert.equal(await page.locator("[data-connect-case]:visible").count(),1);
 await page.locator("#connect-example").selectOption("urgent");
 assert.equal(await page.locator('[data-connect-case="urgent"]:visible').count(),1);assert.equal(await page.locator('[data-connect-case="urgent"] .urgent-call').getAttribute("href"),"tel:115");
 assert.equal(await page.locator("[data-connect-case]:visible details").count(),0);
 assert.match(await page.locator("#connect-status").innerText(),/اقدام فوری/);
 assert.equal(await page.locator("#connect-status").getAttribute("role"),"alert");
 await page.locator("#connect-example").selectOption("unknown");assert.equal(await page.locator('[data-route="uncertain"]:visible').count(),1);
 assert.equal(await page.locator("form,textarea").count(),0);
 assert.match(await page.locator("#connect-status").innerText(),/ایمنی تأیید نشده/);
 await page.locator("#connect-reset").click();assert.equal(await page.locator('[data-connect-case="label"]:visible').count(),1);
 await page.locator('[data-connect-case="label"] summary').click();assert.ok(await page.locator('[data-connect-case="label"] details[open]').isVisible());
 for(const name of ["mano-nutrition","mano-care","mano-connect"]){
  await page.goto(pathToFileURL(path.join(root,name+".html")).href);
  for(const width of [320,390,430,768,1366]){await page.setViewportSize({width,height:844});assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+2),name+" overflow "+width)}
  await page.setViewportSize({width:390,height:844});
  await page.addScriptTag({path:require.resolve("axe-core/axe.min.js")});
  const states=name==="mano-connect"?["label","urgent","unknown"]:["default"];
  const reports={};
  for(const state of states){
   if(name==="mano-connect")await page.locator("#connect-example").selectOption(state);
   const v=await page.evaluate(async()=> (await window.axe.run(document,{runOnly:{type:"tag",values:["wcag2a","wcag2aa","wcag21a","wcag21aa"]}})).violations.map(v=>({id:v.id,impact:v.impact,nodes:v.nodes.map(n=>({target:n.target,summary:n.failureSummary}))})));
   reports[state]=v;assert.deepEqual(v.filter(v=>["critical","serious"].includes(v.impact)),[],name+" "+state);
  }
  fs.writeFileSync(path.join(out,name+"-a11y.json"),JSON.stringify(reports,null,2));
  if(name==="mano-connect")await page.locator("#connect-example").selectOption("label");
  await settleVisuals(page);await page.screenshot({path:path.join(out,name+"-mobile.png"),fullPage:true,animations:"disabled"});
  await page.setViewportSize({width:1366,height:900});await settleVisuals(page);await page.screenshot({path:path.join(out,name+"-desktop.png"),fullPage:true,animations:"disabled"});
 }
 assert.deepEqual(errors,[]);await page.close();
});
test("Additional modules retain educational content when JavaScript is disabled",async()=>{
 const context=await browser.newContext({javaScriptEnabled:false,viewport:{width:320,height:800}});const page=await context.newPage();
 for(const name of ["mano-nutrition","mano-care"]){await page.goto(pathToFileURL(path.join(root,name+".html")).href);assert.ok(await page.locator("#module-search").isDisabled());assert.equal(await page.locator("[data-entry]:visible").count(),5)}
 await page.goto(pathToFileURL(path.join(root,"mano-connect.html")).href);assert.ok(await page.locator("#connect-example").isDisabled());assert.equal(await page.locator("[data-connect-case]:visible").count(),4);assert.equal(await page.locator("form").count(),0);await context.close();
});

test("Local review board: deliberate approval, file resume, responsive layout",async()=>{
 const os=require("node:os"),tmp=fs.mkdtempSync(path.join(os.tmpdir(),"mano-review-ui-"));
 const {buildPreview}=require("../tools/build-preview.cjs");
 const folder=path.join(tmp,"preview");buildPreview(folder);
 const page=await browser.newPage({viewport:{width:390,height:844},acceptDownloads:true});
 const errors=[];page.on("pageerror",e=>errors.push(e.message));
 try{
  await page.goto(pathToFileURL(path.join(folder,"clinical-review.html")).href);
  assert.equal(await page.locator("#review-item option").count(),47);
  await page.locator("#reviewer").fill("EXAMPLE");
  await page.locator("#review-group").selectOption("article");assert.equal(await page.locator("#review-item option").count(),8);
  await page.locator("#review-decision").selectOption("approved");await page.locator("#review-save").click();
  assert.match(await page.locator("#review-message").innerText(),/معیارها/);
  assert.ok(await page.locator("#review-export").isDisabled());
  for(const box of await page.locator("[data-check]").all())await box.check();
  await page.locator("#review-save").click();assert.ok(await page.locator("#review-export").isEnabled());
  const event=page.waitForEvent("download");await page.locator("#review-export").click();const download=await event;
  const filename=path.join(tmp,"decision.json");await download.saveAs(filename);
  const data=JSON.parse(fs.readFileSync(filename,"utf8"));assert.equal(data.releaseAuthorized,false);assert.equal(data.decisions.length,1);
  await page.reload();assert.ok(await page.locator("#review-export").isDisabled());
  await page.locator("#review-file").setInputFiles(filename);await page.locator("#review-import").click();
  await page.waitForFunction(()=>!document.getElementById("review-export").disabled);
  await page.locator("#review-group").selectOption("article");
  assert.equal(await page.locator("#review-decision").inputValue(),"approved");
  for(const width of [320,390,430,768,1366]){await page.setViewportSize({width,height:844});assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+2),"review overflow "+width)}
  await page.setViewportSize({width:390,height:844});await page.addScriptTag({path:require.resolve("axe-core/axe.min.js")});
  const v=await page.evaluate(async()=> (await window.axe.run(document,{runOnly:{type:"tag",values:["wcag2a","wcag2aa","wcag21a","wcag21aa"]}})).violations.map(v=>({id:v.id,impact:v.impact,nodes:v.nodes.map(n=>({target:n.target,summary:n.failureSummary}))})));
  fs.writeFileSync(path.join(out,"clinical-review-a11y.json"),JSON.stringify(v,null,2));assert.deepEqual(v.filter(v=>["critical","serious"].includes(v.impact)),[]);
  await settleVisuals(page);await page.screenshot({path:path.join(out,"clinical-review-mobile.png"),fullPage:true,animations:"disabled"});
  assert.deepEqual(errors,[]);
 }finally{await page.close();fs.rmSync(tmp,{recursive:true,force:true})}
});
