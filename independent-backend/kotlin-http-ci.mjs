import {randomBytes,randomUUID} from 'node:crypto';
import {readFile} from 'node:fs/promises';
import {spawn} from 'node:child_process';
import {once} from 'node:events';
import pg from 'pg';
import {digest} from './policy.mjs';
import {startIntegration} from './integration-host.mjs';

const token=()=>randomBytes(32).toString('base64url');
let admin,running,child;
try{
 const url=new URL(process.env.MANO_TEST_ADMIN_URL||'invalid:');
 const stage=new URL(process.env.MANO_TEST_TLS_URL||'invalid:');
 if(url.hostname!=='127.0.0.1'||stage.hostname!=='127.0.0.1'||url.pathname!=='/mano_stage'||stage.pathname!=='/mano_stage')throw new Error('CI loopback required');
 admin=new pg.Pool({connectionString:url.href});
 const account=async role=>{
  const id=randomUUID(),secret=token();
  await admin.query('INSERT INTO mano_stage.accounts(id,role) VALUES($1,$2)',[id,role]);
  await admin.query('INSERT INTO mano_stage.sessions(token_hash,account_id,expires_at) VALUES($1,$2,now()+interval \'20 minutes\')',[digest(secret),id]);
  return {id,secret};
 };
 const a=await account('parent'),b=await account('parent'),p=await account('pharmacist'),pharmacy=randomUUID();
 await admin.query('INSERT INTO mano_stage.pharmacies(id) VALUES($1)',[pharmacy]);
 await admin.query('INSERT INTO mano_stage.memberships(account_id,pharmacy_id,active) VALUES($1,$2,true)',[p.id,pharmacy]);
 const env={MANO_INTEGRATION_ENABLED:'true',MANO_INDEPENDENT_STAGE_ENABLED:'true',
   MANO_INTEGRATION_SYNTHETIC_ONLY:'true',MANO_INTEGRATION_OPERATOR_TOKEN:token(),
   MANO_STAGE_ACCESS_TOKEN:token(),MANO_INTEGRATION_PORT:'0',MANO_STAGE_DATABASE_URL:stage.href,
   MANO_STAGE_DB_TLS:'verify-full',MANO_STAGE_DB_CA:await readFile(process.env.MANO_TEST_TLS_CA,'utf8')};
 running=await startIntegration(env);
 // Pass only ephemeral synthetic user/transport gates to a JVM test subprocess, not an APK.
 child=spawn('gradle',['-p','../android-integration-test','test','--tests','com.example.data.IndependentStageHttpTest','--no-daemon','--rerun-tasks'],{
  stdio:'inherit',env:{...process.env,MANO_KOTLIN_HTTP_REQUIRED:'true',
   MANO_KOTLIN_HTTP_BASE:'http://127.0.0.1:'+running.server.address().port,
   MANO_KOTLIN_HTTP_GATE:env.MANO_INTEGRATION_OPERATOR_TOKEN,
   MANO_KOTLIN_HTTP_PARENT:a.secret,MANO_KOTLIN_HTTP_OTHER:b.secret,
   MANO_KOTLIN_HTTP_PHARMACIST:p.secret,MANO_KOTLIN_HTTP_PHARMACY:pharmacy}
 });
 const [exit]=await once(child,'exit');if(exit!==0)throw new Error('Kotlin HTTP test failed');
 const {rows:[r]}=await admin.query('SELECT count(*)::int AS n FROM mano_stage.sessions WHERE account_id=ANY($1::uuid[]) AND revoked_at IS NOT NULL',[[a.id,b.id,p.id]]);
 if(r.n!==3)throw new Error('Server session revocation not confirmed');
 console.log('KOTLIN HTTP + VERIFIED POSTGRES TLS: PASS');
}catch{console.error('KOTLIN HTTP + VERIFIED POSTGRES TLS: FAIL');process.exitCode=1;}
finally{if(running)await running.close();if(admin)await admin.end();}
