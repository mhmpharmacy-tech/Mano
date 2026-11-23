import { readFile, mkdir, copyFile } from "node:fs/promises";
import { resolve, join } from "node:path";

const root = process.cwd();
const html = await readFile(resolve(root, "index.html"), "utf8");
const js = await readFile(resolve(root, "src/app.js"), "utf8");
const css = await readFile(resolve(root, "src/app.css"), "utf8");
const conditions = [
  [html.includes('src="/src/app.js"'), "app module is linked"],
  [html.includes('href="/src/app.css"'), "stylesheet is linked"],
  [html.includes("data:image/"), "original approved MANO illustration exists"],
  [html.includes('class="robot"'), "canonical character is present"],
  [!html.includes("آراد"), "demo child is not hard-coded"],
  [!html.includes("maximum-scale=1"), "zoom is accessible"],
  [js.includes("window.show="), "existing home controls are wired"],
  [js.includes("state.profile=null"), "clear-session controls exist"],
  [js.includes("state.measurements.unshift"), "actual measurement recording is available"],
  [js.includes("state.vaccines.unshift"), "vaccine record entry is available"],
  [js.includes("state.treatment.unshift"), "care plan draft entry is available"],
  [css.includes(".screen-main"), "secondary pages use same design system"],
];
for (const [ok, what] of conditions) if (!ok) throw Error("Web build gate failed: " + what);
const dest = resolve(root, "dist");
await mkdir(join(dest, "src"), { recursive: true });
await Promise.all([
  copyFile(resolve(root, "index.html"), join(dest, "index.html")),
  copyFile(resolve(root, "src/app.js"), join(dest, "src/app.js")),
  copyFile(resolve(root, "src/app.css"), join(dest, "src/app.css")),
]);
console.log("MANO Kid Build ready: dist/ (client-only preview; no patient server or real clinical connection)");
