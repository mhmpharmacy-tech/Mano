import assert from 'node:assert/strict';
import {randomBytes,randomUUID} from 'node:crypto';
import {readFile} from 'node:fs/promises';
import pg from 'pg';
import ts from 'typescript';
import {chromium} from 'playwright';
import {digest} from './policy.mjs';
import {startIntegration} from './integration-host.mjs';
import {startBrowserBridge} from './browser-bridge.mjs';

const token=()=>randomBytes(32).toString('base64url');
let admin,running,bridge,browser;
try{
 const url=new URL(process.env.MANO_TEST_ADMIN_URL||'invalid:');
 const stage=new URL(process.env.MANO_TEST_TLS_URL||'invalid:');
 if(url.hostname!=='127.0.0.1'||stage.hostname!=='127.0.0.1'||url.pathname!=='/mano_stage'||stage.pathname!=='/mano_stage')throw new Error('CI only');
 admin=new pg.Pool({connectionString:url.href});
 const account=async role=>{
  const id=randomUUID(),secret=token();
  await admin.query('INSERT INTO mano_stage.accounts(id,role) VALUES($1,$2)',[id,role]);
  await admin.query("INSERT INTO mano_stage.sessions(token_hash,account_id,expires_at) VALUES($1,$2,now()+interval '20 minutes')",[digest(secret),id]);
  return {id,secret};
 };
 const a=await account('parent'),b=await account('parent'),p=await account('pharmacist'),pharmacy=randomUUID();
 await admin.query('INSERT INTO mano_stage.pharmacies(id) VALUES($1)',[pharmacy]);
 await admin.query('INSERT INTO mano_stage.memberships(account_id,pharmacy_id,active) VALUES($1,$2,true)',[p.id,pharmacy]);
 const env={MANO_INTEGRATION_ENABLED:'true',MANO_INDEPENDENT_STAGE_ENABLED:'true',
   MANO_INTEGRATION_SYNTHETIC_ONLY:'true',MANO_INTEGRATION_OPERATOR_TOKEN:token(),
   MANO_STAGE_ACCESS_TOKEN:token(),MANO_STAGE_CARE_ENABLED:'true',MANO_INTEGRATION_PORT:'0',MANO_STAGE_DATABASE_URL:stage.href,
   MANO_STAGE_DB_TLS:'verify-full',MANO_STAGE_DB_CA:await readFile(process.env.MANO_TEST_TLS_CA,'utf8')};
 running=await startIntegration(env);
 bridge=await startBrowserBridge({upstreamPort:running.server.address().port,operatorToken:env.MANO_INTEGRATION_OPERATOR_TOKEN,syntheticOnly:true,careEnabled:true});
 browser=await chromium.launch({headless:true});
 const context=await browser.newContext();
 await context.addCookies([{name:'mano_harness',value:bridge.cookie,url:bridge.origin,httpOnly:true,sameSite:'Strict'}]);
 const page=await context.newPage();
 let leaked=false;
 page.on('request',req=>{if(req.headers()['x-mano-operator-token']||req.headers()['x-mano-stage-token'])leaked=true;});
 await page.route(bridge.origin+'/harness',r=>r.fulfill({contentType:'text/html',body:'<!doctype html><title>MANO synthetic harness</title>'}));
 for(const name of ['independentStageClient','independentBrowserTransport']){
  const source=await readFile(new URL('../src/online/'+name+'.ts',import.meta.url),'utf8');
  const js=ts.transpileModule(source,{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.ESNext}}).outputText;
  await page.route(bridge.origin+'/'+name+'.js',r=>r.fulfill({contentType:'text/javascript',body:js}));
 }
 await page.goto(bridge.origin+'/harness');
 const result=await page.evaluate(async({a,b,p,pharmacy})=>{
  const {IndependentStageClient}=await import('/independentStageClient.js');
  const {privateBrowserTransport}=await import('/independentBrowserTransport.js');
  const clients=[a,b,p].map(t=>{const c=new IndependentStageClient(true,privateBrowserTransport(true,true));c.setSession(t);return c;});
  const [parent,other,pharmacist]=clients;
  const id=await parent.create('demo-browser',24);
  const checks=[];
  checks.push((await parent.list()).some(x=>x.id===id));
  checks.push(!(await other.list()).some(x=>x.id===id));
  checks.push(!(await pharmacist.list()).some(x=>x.id===id));
  try{await other.grant(id,pharmacy,true);checks.push(false);}catch{checks.push(true);}
  await parent.grant(id,pharmacy,true);
  checks.push((await pharmacist.list()).some(x=>x.id===id));
  const card=await pharmacist.issueCare(id,pharmacy,true);
  checks.push((await parent.listCare(id)).some(x=>x.id===card&&x.state==='issued'));
  try{await other.listCare(id);checks.push(false);}catch{checks.push(true);}
  await parent.transitionCare(id,card,'acknowledged');
  await parent.transitionCare(id,card,'followup_requested');
  checks.push((await pharmacist.listCare(id)).some(x=>x.id===card&&x.state==='followup_requested'));
  await pharmacist.transitionCare(id,card,'closed');
  checks.push((await parent.listCare(id)).some(x=>x.id===card&&x.state==='closed'));
  await parent.revoke(id,pharmacy);
  try{await pharmacist.listCare(id);checks.push(false);}catch{checks.push(true);}
  checks.push(!(await pharmacist.list()).some(x=>x.id===id));
  for(const c of clients){await c.logout();try{await c.list();checks.push(false);}catch{checks.push(true);}}
  checks.push(!document.cookie.includes('mano_harness'));
  return checks;
 },{a:a.secret,b:b.secret,p:p.secret,pharmacy});
 assert.equal(result.length,15);assert.ok(result.every(Boolean));assert.equal(leaked,false);
 const {rows:[r]}=await admin.query('SELECT count(*)::int AS n FROM mano_stage.sessions WHERE account_id=ANY($1::uuid[]) AND revoked_at IS NOT NULL',[[a.id,b.id,p.id]]);
 assert.equal(r.n,3);
 console.log('BROWSER + PRIVATE BRIDGE + VERIFIED POSTGRES TLS: PASS');
}catch(e){console.error('BROWSER PRIVATE INTEGRATION: FAIL',e.name);process.exitCode=1;}
finally{if(browser)await browser.close();if(bridge)await bridge.close();if(running)await running.close();if(admin)await admin.end();}
