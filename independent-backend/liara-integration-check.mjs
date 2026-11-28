import assert from 'node:assert/strict';
import {randomBytes} from 'node:crypto';
import pg from 'pg';
import {databaseOptions} from './policy.mjs';
import {startIntegration} from './integration-host.mjs';
import {startBrowserBridge} from './browser-bridge.mjs';

const token=()=>randomBytes(32).toString('base64url');
let running,bridge,pool;
try{
 if(process.argv[2]!=='--synthetic-only'||!process.env.PGPASSWORD)throw new Error('Explicit synthetic mode and password required');
 const env={MANO_INTEGRATION_ENABLED:'true',MANO_INDEPENDENT_STAGE_ENABLED:'true',
  MANO_INTEGRATION_SYNTHETIC_ONLY:'true',MANO_INTEGRATION_OPERATOR_TOKEN:token(),
  MANO_STAGE_ACCESS_TOKEN:token(),MANO_INTEGRATION_PORT:'0',
  MANO_STAGE_DATABASE_URL:'postgresql://mano_stage_app:'+encodeURIComponent(process.env.PGPASSWORD)+'@mano-db:5432/mano_stage',
  MANO_STAGE_DB_TLS:process.env.MANO_STAGE_DB_TLS||'verify-full',
  MANO_STAGE_DB_CA:process.env.MANO_STAGE_DB_CA};
 delete process.env.PGPASSWORD;
 pool=new pg.Pool(databaseOptions(env));
 const {rows:[r]}=await pool.query('SELECT ssl FROM pg_stat_ssl WHERE pid=pg_backend_pid()');
 if(env.MANO_STAGE_DB_TLS==='verify-full')assert.equal(r.ssl,true);
 console.log('DATABASE TRANSPORT:',r.ssl?'TLS':'SYNTHETIC PRIVATE TEST ONLY');
 running=await startIntegration(env);
 assert.equal(running.server.address().address,'127.0.0.1');
 const base='http://127.0.0.1:'+running.server.address().port;
 const route='/internal/mano/v1/profiles';
 let res=await fetch(base+route,{signal:AbortSignal.timeout(5000)});
 assert.equal(res.status,404);await res.text();
 res=await fetch(base+route,{headers:{'x-mano-operator-token':env.MANO_INTEGRATION_OPERATOR_TOKEN},signal:AbortSignal.timeout(5000)});
 assert.equal(res.status,401);await res.text();
 res=await fetch(base+'/internal/mano/v1/auth/issue',{method:'POST',headers:{'x-mano-operator-token':env.MANO_INTEGRATION_OPERATOR_TOKEN,'content-type':'application/json'},body:'{"handle":"demo-parentone"}',signal:AbortSignal.timeout(5000)});
 assert.equal(res.status,404);await res.text();
 bridge=await startBrowserBridge({upstreamPort:running.server.address().port,operatorToken:env.MANO_INTEGRATION_OPERATOR_TOKEN,syntheticOnly:true});
 res=await fetch(bridge.origin+route,{signal:AbortSignal.timeout(5000)});
 assert.equal(res.status,404);await res.text();
 const headers={cookie:'mano_harness='+bridge.cookie,'x-mano-browser-test':'1'};
 res=await fetch(bridge.origin+route,{headers:{...headers,origin:'http://invalid.example'},signal:AbortSignal.timeout(5000)});
 assert.equal(res.status,404);await res.text();
 res=await fetch(bridge.origin+route,{headers,signal:AbortSignal.timeout(5000)});
 assert.equal(res.status,401);await res.text();
 console.log('MANO INTEGRATION READ-ONLY CHECK: PASS');
}catch(e){console.error('MANO INTEGRATION CHECK: FAIL',e.code||e.name);process.exitCode=1;}
finally{if(bridge)await bridge.close();if(running)await running.close();if(pool)await pool.end();}
