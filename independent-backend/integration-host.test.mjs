import {test} from 'node:test';
import assert from 'node:assert/strict';
import {once} from 'node:events';
import {randomBytes} from 'node:crypto';
import {createIntegrationHost} from './integration-host.mjs';
const token=()=>randomBytes(32).toString('base64url');
test('operator perimeter denies default, browser-origin and arbitrary routes before DB',async()=>{
 const env={MANO_INTEGRATION_ENABLED:'true',MANO_INDEPENDENT_STAGE_ENABLED:'true',MANO_STAGE_ACCESS_TOKEN:token(),MANO_INTEGRATION_OPERATOR_TOKEN:token()};
 let connections=0;
 const pool={connect:async()=>{connections++;throw new Error('unexpected');}};
 assert.throws(()=>createIntegrationHost(pool,{}));
 assert.throws(()=>createIntegrationHost(pool,{...env,MANO_INTEGRATION_OPERATOR_TOKEN:env.MANO_STAGE_ACCESS_TOKEN}));
 const s=createIntegrationHost(pool,env);s.listen(0,'127.0.0.1');await once(s,'listening');
 try{
  const base='http://127.0.0.1:'+s.address().port;
  const path='/internal/mano/v1/profiles';
  const gate={'x-mano-operator-token':env.MANO_INTEGRATION_OPERATOR_TOKEN};
  for(const [p,h,status] of [[path,{},404],[path,{...gate,origin:'https://example.test'},404],[path,{...gate,'sec-fetch-site':'same-origin'},404],['/api/health',gate,404],[path,{...gate,'x-mano-stage-token':env.MANO_STAGE_ACCESS_TOKEN},404],[path,gate,401]]){
   const r=await fetch(base+p,{headers:h});assert.equal(r.status,status);assert.equal(r.headers.get('cache-control'),'no-store');await r.text();
  }
  assert.equal(connections,0);
 }finally{await new Promise(resolve=>s.close(resolve));}
});
