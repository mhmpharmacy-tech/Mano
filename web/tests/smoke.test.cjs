"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { evaluate } = require("../logic.js");
const root = path.resolve(__dirname, "..");

test("Immediate warning sign overrides missing age and temperature", () => {
  const output = evaluate({ ageBand: "", temperature: "", warnings: ["breath"] });
  assert.equal(output.kind, "danger");
});
test("Child under 3 months with >=38 C fever requires urgent review", () => {
  assert.equal(evaluate({ ageBand: "under3", temperature: "38.0", warnings: [] }).kind, "danger");
  assert.equal(evaluate({ ageBand: "under3", temperature: "39.3", warnings: [] }).kind, "danger");
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
