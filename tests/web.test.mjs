import { test } from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const primary = await readFile(new URL("../index.html", import.meta.url), "utf8");
const preview = await readFile(new URL("../preview/index.html", import.meta.url), "utf8");
const previewCode = await readFile(new URL("../src/app.js", import.meta.url), "utf8");

test("the primary web page boots the React application in Persian RTL", () => {
  assert.match(primary, /<html[^>]*lang=["']fa["']/);
  assert.match(primary, /<html[^>]*dir=["']rtl["']/);
  assert.match(primary, /id=["']root["']/);
  assert.match(primary, /src=["']\/src\/main\.tsx["']/);
  assert.doesNotMatch(primary, /src=["']\/src\/app\.js["']/);
});

test("the separate design preview retains its original interface and demo entry", () => {
  assert.match(preview, /class="hero"/);
  assert.match(preview, /id="supplements-home-card"/);
  assert.match(preview, /src="\/src\/app\.js"/);
  assert.doesNotMatch(preview, /maximum-scale=1/);
});

test("the design demo cannot submit patient information or persist session data", () => {
  assert.doesNotMatch(previewCode, /localStorage|sessionStorage|fetch\(|XMLHttpRequest/);
  assert.match(previewCode, /ارسال واقعی غیرفعال است/);
});
