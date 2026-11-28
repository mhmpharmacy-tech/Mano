import { readFile, readdir, stat } from 'node:fs/promises';
import path from 'node:path';

const dist = path.resolve('dist');
const entries = ['index.html', 'preview/index.html', 'care/index.html', 'pharmacist/index.html'];
for (const entry of entries) {
  const html = await readFile(path.join(dist, entry), 'utf8');
  if (!/<html\b/i.test(html)) throw new Error(`Missing built HTML entry: ${entry}`);
  if (entry === 'index.html' && (html.includes('/src/main.tsx') || !/\/assets\/[^"']+\.js/.test(html))) {
    throw new Error('React root must reference the production bundle, not source files.');
  }
  for (const match of html.matchAll(/(src|href)=["']([^"'#]+)["']/g)) {
    const value = match[2];
    if (/^(?:https?:|data:|blob:|mailto:|tel:)/i.test(value)) continue;
    const url = new URL(value, `https://build.invalid/${entry}`);
    if (match[1] === 'href' && !/\.(?:js|css|png|jpe?g|webp|svg|ico|ttf|woff2?)$/i.test(url.pathname)) continue;
    const filename = path.resolve(dist, `.${decodeURIComponent(url.pathname)}`);
    if (!filename.startsWith(`${dist}${path.sep}`)) throw new Error(`Invalid built asset path in ${entry}`);
    if (!(await stat(filename)).isFile()) throw new Error(`Missing built asset referenced by ${entry}`);
  }
}

const secretValues = ['GEMINI_API_KEY', 'MANO_GATEWAY_ACCESS_TOKEN']
  .map((name) => process.env[name]).filter((value): value is string => Boolean(value && value.length >= 16));
async function checkDirectory(directory: string): Promise<void> {
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const filename = path.join(directory, entry.name);
    if (entry.isDirectory()) await checkDirectory(filename);
    else if (/\.(?:js|css|html|map)$/.test(entry.name)) {
      const source = await readFile(filename, 'utf8');
      if (secretValues.some((secret) => source.includes(secret))) {
        throw new Error('A server-only credential was found in the built public files. Rebuild without exposure.');
      }
    }
  }
}
await checkDirectory(dist);
console.log('Verified React, preview, care and pharmacist entries, local asset references and configured server-secret exclusion. No deployment performed.');
