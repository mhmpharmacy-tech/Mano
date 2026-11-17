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
  assert.equal(m.draftArticles.length,2);
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
