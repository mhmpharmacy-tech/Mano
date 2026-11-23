import http from "node:http";
import { createReadStream } from "node:fs";
import { stat } from "node:fs/promises";
import { resolve, join } from "node:path";

/**
 * Zero-dependency preview server for Google AI Studio Build.
 * Strict static allowlist prevents accidental exposure of .env, Gradle,
 * Firebase/Firestore rules, local patient information or Android assets.
 */
const base = resolve(process.cwd());
const files = new Map([
  ["/", ["index.html", "text/html; charset=utf-8"]],
  ["/index.html", ["index.html", "text/html; charset=utf-8"]],
  ["/src/app.js", ["src/app.js", "text/javascript; charset=utf-8"]],
  ["/src/app.css", ["src/app.css", "text/css; charset=utf-8"]],
  ["/favicon.ico", ["", ""]],
]);
const port = Number(process.env.PORT || 3000);
if (!Number.isInteger(port) || port < 1 || port > 65535) throw Error("Invalid PORT");

export const server = http.createServer(async (request, response) => {
  if (request.method !== "GET" && request.method !== "HEAD") {
    response.writeHead(405, { "Allow": "GET, HEAD" }); response.end(); return;
  }
  let pathname;
  try { pathname = new URL(request.url, "http://localhost").pathname; } catch {
    response.writeHead(400); response.end("Bad request"); return;
  }
  const entry = files.get(pathname);
  if (!entry || !entry[0]) { response.writeHead(404, { "Content-Type": "text/plain; charset=utf-8" }); response.end("Not found"); return; }
  const filePath = resolve(join(base, entry[0]));
  if (!filePath.startsWith(base + "/")) { response.writeHead(403); response.end(); return; }
  try {
    const info = await stat(filePath);
    response.writeHead(200, {
      "Content-Type": entry[1], "Content-Length": info.size,
      "X-Content-Type-Options": "nosniff",
      "Referrer-Policy": "no-referrer",
      "Cache-Control": "no-store",
      "Content-Security-Policy": "default-src 'none'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; font-src 'self'; connect-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'"
    });
    if (request.method === "HEAD") { response.end(); return; }
    createReadStream(filePath).pipe(response);
  } catch { response.writeHead(404); response.end("Not found"); }
});

if (process.env.MANO_NO_LISTEN !== "1") {
  server.listen(port, "0.0.0.0", () => console.log("MANO Kid preview: http://localhost:" + port));
}
