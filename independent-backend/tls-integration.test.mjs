import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {randomBytes} from 'node:crypto';
import pg from 'pg';
import {databaseOptions} from './policy.mjs';
import {startIntegration} from './integration-host.mjs';

const token=()=>randomBytes(32).toString('base64url');
test('verified TLS transport starts, rejects untrusted CA and checks encryption',async()=>{
 const url=new URL(process.env.MANO_TEST_TLS_URL||'invalid:');
 if(url.hostname!=='127.0.0.1'||url.pathname!=='/mano_stage'||url.username!=='mano_stage_app')throw new Error('Local CI only');
 const ca=await readFile(process.env.MANO_TEST_TLS_CA,'utf8');
 const wrong=await readFile(process.env.MANO_TEST_TLS_WRONG_CA,'utf8');
 const env={MANO_INTEGRATION_ENABLED:'true',MANO_INDEPENDENT_STAGE_ENABLED:'true',
  MANO_INTEGRATION_SYNTHETIC_ONLY:'true',MANO_INTEGRATION_OPERATOR_TOKEN:token(),
  MANO_STAGE_ACCESS_TOKEN:token(),MANO_STAGE_DATABASE_URL:url.href,
  MANO_STAGE_DB_TLS:'verify-full',MANO_STAGE_DB_CA:ca,MANO_INTEGRATION_PORT:'0'};
 const pool=new pg.Pool(databaseOptions(env));
 try {
  const {rows}=await pool.query('SELECT ssl,version FROM pg_stat_ssl WHERE pid=pg_backend_pid()');
  assert.equal(rows[0].ssl,true);assert.match(rows[0].version,/^TLS/);
 }finally{await pool.end();}
 const running=await startIntegration(env);
 try{
  assert.equal(running.server.address().address,'127.0.0.1');
  const r=await fetch('http://127.0.0.1:'+running.server.address().port+'/internal/mano/v1/profiles');
  assert.equal(r.status,404);
 }finally{await running.close();}
 await assert.rejects(startIntegration({...env,MANO_STAGE_DB_CA:wrong}));
});
