import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import vm from "node:vm";

const html = readFileSync(new URL("../public/pharmacist/index.html", import.meta.url), "utf8");
const scripts = [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)];
assert.equal(scripts.length, 1, "Use exactly one bundled inline script without a tracking CDN");
assert.match(html, /lang="fa" dir="rtl"/);
assert.match(html, /name="robots" content="noindex, nofollow"/);
assert.match(html, /connect-src 'none'/);
assert.match(html, /aria-live="polite"/);
assert.doesNotMatch(html, /<script[^>]+src=/i);
assert.doesNotMatch(html, /https:\/\/(?:api\.qrserver|chart\.googleapis|quickchart)/i);

function load(search) {
  const elements = new Map();
  let launched = null;
  let copied = null;
  for (const id of ["pharmacy-status", "pharmacy-id", "open-app", "copy-link", "feedback"]) {
    elements.set(id, {
      textContent: "",
      hidden: id === "pharmacy-id",
      disabled: id === "open-app" || id === "copy-link",
      handlers: {},
      addEventListener(type, fn) { this.handlers[type] = fn; },
    });
  }
  const sandbox = {
    URLSearchParams,
    document: { getElementById(id) { return elements.get(id); } },
    window: { location: { search, assign(url) { launched = url; } } },
    navigator: { clipboard: { async writeText(value) { copied = value; } } },
  };
  vm.runInNewContext(scripts[0][1], sandbox, { timeout: 1000 });
  return {
    get: id => elements.get(id),
    copied: () => copied,
    launched: () => launched,
  };
}

for (const invalid of [
  "",
  "?pharmacy=../shop",
  "?pharmacy=shop%26child%3D1",
  "?pharmacy=shop&pharmacy=second",
  "?pharmacy=%3Cscript%3E",
  "?pharmacy=" + "x".repeat(65),
  "?pharmacy=%D8%AF%D8%A7%D8%B1%D9%88",
]) {
  const page = load(invalid);
  assert.equal(page.get("open-app").disabled, true, "Invalid ID must never open an app: " + invalid);
  assert.equal(page.get("copy-link").disabled, true);
  assert.equal(page.get("pharmacy-id").hidden, true);
  assert.match(page.get("pharmacy-status").textContent, /معتبر نیست/);
}

const good = load("?pharmacy=pharm_2026-12");
assert.equal(good.get("open-app").disabled, false);
assert.equal(good.get("copy-link").disabled, false);
assert.equal(good.get("pharmacy-id").textContent, "ID: pharm_2026-12");
assert.match(good.get("pharmacy-status").textContent, /فقط داخل اپ/);
await good.get("copy-link").handlers.click();
assert.equal(good.copied(), "https://manokid.ir/pharmacist?pharmacy=pharm_2026-12");
good.get("open-app").handlers.click();
assert.equal(
  good.launched(),
  "intent://manokid.ir/pharmacist?pharmacy=pharm_2026-12#Intent;scheme=https;package=com.aistudio.manokids.rnkvpx;end",
);

console.log("Verified pharmacy landing: seven invalid cases rejected; valid copy + Android intent supported; no external tracking.");
