"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { evaluate, parseTemperature } = require("../logic.js");
const root = path.resolve(__dirname, "..");

test("Immediate warning sign overrides missing age and temperature", () => {
  const output = evaluate({ ageBand: "", temperature: "", warnings: ["breath"] });
  assert.equal(output.kind, "danger");
});
test("Child under 3 months with >=38 C fever requires urgent review", () => {
  assert.equal(evaluate({ ageBand: "under3", temperature: "38.0", warnings: [] }).kind, "danger");
  assert.equal(evaluate({ ageBand: "under3", temperature: "39.3", warnings: [] }).kind, "danger");
});
test("Persian and Arabic numerals are parsed without silencing an infant fever", () => {
  for (const value of ["۳۸٫۰", "٣٨,٠", "۳۸.۰", "38.0"]) {
    assert.equal(parseTemperature(value), 38);
    const result = evaluate({ ageBand: "under3", temperature: value, warnings: [] });
    assert.equal(result.kind, "danger", "Expected urgent infant fever for " + value);
  }
  assert.equal(parseTemperature("٣٨,٥"), 38.5);
  assert.equal(parseTemperature(""), null);
  for (const invalid of ["۳۸/۵", "۳۸ درجه", "1e2", "NaN", "38..0", ""]) {
    if (invalid !== "") assert.equal(Number.isNaN(parseTemperature(invalid)), true, invalid);
  }
  assert.equal(evaluate({ ageBand: "under3", temperature: "۳۸/۵", warnings: [] }).kind, "invalid");
  assert.equal(evaluate({ ageBand: "", temperature: "نامشخص", warnings: ["breath"] }).kind, "danger");
});
test("Missing age without emergency sign never returns false reassurance", () => {
  assert.equal(evaluate({ ageBand: "", temperature: "", warnings: [] }).kind, "invalid");
});
test("Invalid temperature is rejected conservatively", () => {
  assert.equal(evaluate({ ageBand: "older", temperature: "abc", warnings: [] }).kind, "invalid");
  assert.equal(evaluate({ ageBand: "under3", temperature: "29", warnings: [] }).kind, "invalid");
});
test("Ordinary outcomes do not claim child is safe or prescribe a dose", () => {
  for (const input of [
    { ageBand: "older", temperature: "", warnings: [] },
    { ageBand: "under3", temperature: "37.1", warnings: [] },
  ]) {
    const response = evaluate(input);
    assert.equal(response.kind, "caution");
    assert.ok(!response.message.includes("بی‌خطر است"));
    assert.ok(!response.message.includes("میلی‌لیتر"));
  }
});
test("Public preview includes all reachable major anchors and scripts", () => {
  const html = fs.readFileSync(path.join(root, "index.html"), "utf8");
  for (const id of ["home", "pathways", "screening", "medicine", "skin", "nutrition", "faq", "pharmacist", "about", "screening-result"]) {
    assert.match(html, new RegExp('id="' + id + '"'));
  }
  for (const name of ["app.js", "logic.js", "styles.css"]) {
    assert.ok(fs.existsSync(path.join(root, name)));
    assert.ok(html.includes(name));
  }
});
test("Original brand assets are copied locally and web preview avoids patient-data submission", () => {
  const html = fs.readFileSync(path.join(root, "index.html"), "utf8");
  for (const match of html.matchAll(/src="\.\/assets\/([^"]+)"/g)) {
    assert.ok(fs.existsSync(path.join(root, "assets", match[1])), "Missing image: " + match[1]);
  }
  const script = fs.readFileSync(path.join(root, "app.js"), "utf8");
  assert.ok(!/\bfetch\s*\(|\bXMLHttpRequest\b|\blocalStorage\b|\bsessionStorage\b/.test(script), "No network or storage API is allowed in public preview");
  assert.match(html, /noindex,nofollow/);
  assert.match(html, /مشاوره واقعی داروساز/);
});
test("Staff workspace uses synthetic fixture only and never submits records", () => {
  const html = fs.readFileSync(path.join(root, "staff-demo.html"), "utf8");
  const script = fs.readFileSync(path.join(root, "staff-demo.js"), "utf8");
  for (const id of ["case-list", "case-detail", "review-form", "filter-category", "filter-status", "filter-safety", "filter-contact", "review-confirm"]) {
    assert.ok(html.includes('id="' + id + '"'), "Missing staff control " + id);
  }
  assert.match(html, /noindex,nofollow/);
  assert.match(html, /داده ساختگی/);
  assert.ok(!/\bfetch\s*\(|\bXMLHttpRequest\b|\blocalStorage\b|\bsessionStorage\b/.test(script), "Demo must not persist or transmit health data");
  assert.ok(script.includes('c.safety && decision === "education"'), "Safety-flag cases must not silently approve educational resolution");
});

test("Fever thresholds distinguish age bands and red flags take precedence",()=>{
  assert.equal(evaluate({ageBand:"under3",temperature:"۳۸",warnings:[]}).kind,"danger");
  assert.equal(evaluate({ageBand:"threeToSix",temperature:"۳۹",warnings:[]}).kind,"prompt");
  assert.equal(evaluate({ageBand:"threeToSix",temperature:"۳۸٫۹",warnings:[]}).kind,"caution");
  assert.equal(evaluate({ageBand:"older",temperature:"۳۹",warnings:[]}).kind,"caution");
  assert.equal(evaluate({ageBand:"threeToSix",temperature:"۳۹",warnings:["rash"]}).kind,"danger");
});
test("Draft article pages are linked, unindexed, traceable and have no claimed approval",()=>{
  const m=JSON.parse(fs.readFileSync(path.join(root,"clinical-content-status.json"),"utf8"));
  const home=fs.readFileSync(path.join(root,"index.html"),"utf8");
  assert.equal(m.draftArticles.length,8);
  for(const a of m.draftArticles){
    const html=fs.readFileSync(path.join(root,a.path),"utf8");
    assert.ok(home.includes('./'+a.path));
    assert.ok(html.includes('noindex,nofollow'));
    assert.equal((html.match(/<h1(?:\s|>)/g)||[]).length,1);
    assert.ok(html.includes("در انتظار تأیید بالینی"));
    assert.equal(a.humanReviewer,null);
    assert.equal(a.reviewStatus,"NEEDS_CLINICAL_REVIEW");
    for(const r of a.references) assert.ok(html.includes(m.sourceRegister[r].url),r);
    for(const c of a.claims) assert.ok(a.references.includes(c.source)&&c.locator);
    assert.ok(!/<script\b|<form\b/.test(html),"Standalone draft articles contain no scripts or data submission");
  }
});

test("Warning form fails closed before local JavaScript is ready",()=>{
  const html=fs.readFileSync(path.join(root,"index.html"),"utf8");
  assert.match(html,/id="warning-submit"[^>]*disabled/);
  assert.ok(html.includes("form-action 'none'"));
  assert.ok(html.includes('id="warning-nojs"'));
});

test("Encyclopedia links match the registered eight draft articles",()=>{
 const home=fs.readFileSync(path.join(root,"index.html"),"utf8");
 const m=JSON.parse(fs.readFileSync(path.join(root,"clinical-content-status.json"),"utf8"));
 assert.equal((home.match(/class="library-card tone-/g)||[]).length,8);
 const metas=new Set();
 for(const a of m.draftArticles){
  assert.match(a.path,/^[a-z0-9-]+\.html$/);
  assert.ok(home.includes('href="./'+a.path+'"'));
  const html=fs.readFileSync(path.join(root,a.path),"utf8");
  const desc=html.match(/<meta name="description" content="([^"]+)"/)[1];
  assert.ok(!metas.has(desc),"Duplicate description");metas.add(desc);
  for(const match of html.matchAll(/href="\.\/([a-z0-9-]+\.html)(?:#[^"]*)?"/g)){assert.ok(fs.existsSync(path.join(root,match[1])),"Missing internal article link")}
  for(const match of html.matchAll(/src="\.\/assets\/([^"]+)"/g)){assert.ok(fs.existsSync(path.join(root,"assets",match[1])),"Missing local article asset")}
  assert.ok(!/<script\b|<form\b/.test(html));
 }
});

test("Six modules remain draft, source-linked and not clinical tools",()=>{
 const m=JSON.parse(fs.readFileSync(path.join(root,"clinical-content-status.json"),"utf8"));
 const home=fs.readFileSync(path.join(root,"index.html"),"utf8");
 assert.equal(m.draftModules.length,6);
 for(const mod of m.draftModules){
  const html=fs.readFileSync(path.join(root,mod.path),"utf8");
  assert.ok(home.includes('./'+mod.path));assert.match(html,/noindex,nofollow/);
  assert.equal(mod.humanReviewer,null);assert.equal(mod.clinicalUseEnabled,false);assert.ok(!/<form\b|<input[^>]+type="file"/.test(html));
  for(const e of mod.entries)for(const c of e.claims){assert.ok(c.locator);assert.ok(html.includes(m.sourceRegister[c.source].url));}
 }
 const mod=require("../modules.js");
 assert.equal(mod.matches("ستیریزین Cetirizine","allergy","ستيري زين","all"),true);
 assert.equal(mod.matches("ستیریزین","allergy","ستیریزین","pain"),false);
 assert.equal(mod.growthSeries("a","weight")[0].value,4.8);
 assert.equal(mod.growthSeries("b","height")[0].value,57);
 for(const bad of ["__proto__","constructor","other"])assert.equal(mod.growthSeries(bad,"weight"),null);
 assert.equal(mod.growthSeries("a","percentile"),null);
 const data=mod.growthSeries("a","weight");data[0].value=999;assert.equal(mod.growthSeries("a","weight")[0].value,4.8);
 const script=fs.readFileSync(path.join(root,"modules.js"),"utf8");
 assert.ok(!/\bfetch\s*\(|\bXMLHttpRequest\b|\blocalStorage\b|\bsessionStorage\b|\bsendBeacon\b/.test(script));
});

test("Module pages cannot submit forms or start clinical network requests",()=>{for(const name of ["mano-drug","mano-growth","mano-skin","mano-nutrition","mano-care","mano-connect"]){const html=fs.readFileSync(path.join(root,name+".html"),"utf8");assert.ok(html.includes("connect-src 'none'; form-action 'none'; base-uri 'none'; object-src 'none'"));assert.ok(!/<form\b|name="(?:patient|child|weight|age)"/.test(html));}});

test("Six module pages interlink and every clinical claim has a registered link",()=>{
 const m=JSON.parse(fs.readFileSync(path.join(root,"clinical-content-status.json"),"utf8"));
 for(const a of m.draftModules){
  const html=fs.readFileSync(path.join(root,a.path),"utf8");
  for(const b of m.draftModules.filter(b=>b.path!==a.path))assert.ok(html.includes('href="./'+b.path+'"'),a.path+" missing "+b.path);
  for(const match of html.matchAll(/src="\.\/assets\/([^"]+)"/g))assert.ok(fs.existsSync(path.join(root,"assets",match[1])));
 }
 const mod=require("../modules.js");
 assert.equal(mod.exampleRoute("urgent"),"emergency");
 assert.equal(mod.exampleRoute("unknown"),"uncertain");
 for(const id of ["__proto__","constructor","","other"])assert.equal(mod.exampleRoute(id),null);
 const c=m.draftModules.find(x=>x.path==="mano-connect.html");
 assert.equal(c.submissionEnabled,false);assert.equal(c.triagePassIssuerEnabled,false);
 const html=fs.readFileSync(path.join(root,c.path),"utf8");
 assert.ok(!/<form\b|<textarea\b|<input\b/.test(html));
 assert.ok(html.includes('data-route="emergency"')&&html.includes('data-route="uncertain"'));
});

test("Static preview packaging is allowlisted and remains unapproved",()=>{
 const os=require("node:os"),crypto=require("node:crypto");
 const {buildPreview}=require("../tools/build-preview.cjs");
 const tmp=fs.mkdtempSync(path.join(os.tmpdir(),"mano-preview-test-"));
 try{
  const dest=path.join(tmp,"preview"),result=buildPreview(dest);
  assert.equal(result.articleCount,8);assert.equal(result.moduleCount,6);
  const m=JSON.parse(fs.readFileSync(path.join(dest,"PREVIEW_MANIFEST.json"),"utf8"));
  assert.equal(m.clinicalServiceEnabled,false);
  for(const [name,sha] of Object.entries(m.sha256))assert.equal(crypto.createHash("sha256").update(fs.readFileSync(path.join(dest,name))).digest("hex"),sha);
  for(const name of [".git","functions","tests","browser-artifacts","sitemap.xml"])assert.equal(fs.existsSync(path.join(dest,name)),false);
  assert.throws(()=>buildPreview(dest),/new directory/);
 }finally{fs.rmSync(tmp,{recursive:true,force:true})}
});
