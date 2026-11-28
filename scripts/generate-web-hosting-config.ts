import { writeFile } from 'node:fs/promises';
import path from 'node:path';
import { createWebHostingConfig } from '../server/webHostingConfig.ts';

const outputFlag = process.argv.slice(2);
if (outputFlag.length > 1 || (outputFlag[0] && !outputFlag[0].startsWith('--out='))) {
  throw new Error('Usage: tsx scripts/generate-web-hosting-config.ts [--out=firebase.web.generated.json]');
}
const output = outputFlag[0]?.slice('--out='.length) || 'firebase.web.generated.json';
if (!/^firebase\.web(?:\.[a-z0-9-]+)?\.json$/.test(output)) {
  throw new Error('Output must be a firebase.web*.json file in the project directory.');
}
await writeFile(path.resolve(output), `${JSON.stringify(createWebHostingConfig(process.env), null, 2)}\n`, { flag: 'w' });
console.log(`Prepared ${output}; no hosting or backend was deployed.`);
