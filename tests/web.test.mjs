import { test } from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const index = await readFile(resolve("index.html"), "utf8");
const js = await readFile(resolve("src/app.js"), "utf8");
const css = await readFile(resolve("src/app.css"), "utf8");

test("approved visual identity survives AI Studio import", () => {
  assert.match(index, /MANO\s*<em>Kid<\/em>/);
  assert.match(index, /class="robot"/);
  assert.match(index, /data:image\/[a-zA-Z+.-]+;base64,/);
  assert.match(index, /class="hero"/);
  assert.match(index, /class="bottom"/);
  assert.doesNotMatch(index, /آراد|۴ سال و ۳ ماه/);
  assert.doesNotMatch(index, /maximum-scale=1/);
  assert.match(css, /--deep:#0e5840/);
  assert.match(css, /\.screen-main/);
});

test("all approved home actions lead to a real interactive view", () => {
  for (const key of ["symptoms","pharmacist","profile","growth","vaccines","care","medicines","search","alerts","addDrug","tools"]) {
    assert.match(index, new RegExp("show\\('"+key+"'\\)|switchTab\\(this,'"+key+"'\\)"));
  }
  for (const form of ["profile-form","symptom-form","question-form","growth-form","vaccine-form","treatment-form"]) {
    assert.ok(js.includes('id="'+form+'"'), form + " not implemented");
  }
  for (const label of ["بررسی علائم کودک","واکسیناسیون","رشد کودک","پرسش از داروساز","برنامه مراقبت و درمان","مکمل‌ها و بهداشت کودک"]) {
    assert.ok(js.includes(label) || index.includes(label), label + " missing");
  }
});

test("no fabricated pediatric dosing, vaccine deadline or real pharmacist confirmation", () => {
  assert.match(js, /age<3&&temp!==null&&temp>=38/);
  assert.match(js, /ارزیابی فوری پزشکی/);
  assert.match(js, /بدون درصدک ساختگی/);
  assert.match(js, /بدون محاسبه خودکار موعد/);
  assert.match(js, /ارسال واقعی غیرفعال است/);
  assert.match(js, /state\.profile=null/);
  assert.match(js, /بستن یا تازه‌سازی پاک می‌شود/);
  assert.doesNotMatch(js, /localStorage|sessionStorage|fetch\(|XMLHttpRequest/);
});

test("preview server serves only whitelisted app assets", async () => {
  process.env.MANO_NO_LISTEN = "1";
  const { server } = await import("../server.mjs");
  await new Promise((res) => server.listen(0, "127.0.0.1", res));
  const origin = "http://127.0.0.1:" + server.address().port;
  try {
    const page = await fetch(origin + "/");
    assert.equal(page.status, 200);
    assert.match(page.headers.get("content-type"), /text\/html/);
    assert.match(await page.text(), /Care Home/);
    for (const path of ["/src/app.js", "/src/app.css"]) {
      const res = await fetch(origin + path);
      assert.equal(res.status, 200, path);
      assert.ok((await res.text()).length > 500);
    }
    for (const sensitive of ["/firebase.json","/.env","/app/build.gradle.kts","/firestore.rules","/metadata.json","/package.json"]) {
      const res = await fetch(origin + sensitive);
      assert.equal(res.status,404,"Unexpected exposure: "+sensitive);
    }
    const forbidden = await fetch(origin + "/src/app.js", { method:"POST" });
    assert.equal(forbidden.status,405);
  } finally {
    await new Promise((res) => server.close(res));
  }
});
