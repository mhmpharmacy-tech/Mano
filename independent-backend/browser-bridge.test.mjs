import {test} from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import {once} from 'node:events';
import {randomBytes} from 'node:crypto';
import {startBrowserBridge} from './browser-bridge.mjs';

test('browser bridge does not expose server gate and rejects cross-origin or missing access',async()=>{
 const gate=randomBytes(32).toString('base64url');
 const bearer=randomBytes(32).toString('base64url');
 let calls=0;
 const upstream=http.createServer((req,res)=>{
  calls++;
  assert.equal(req.headers['x-mano-operator-token'],gate);
  assert.equal(req.headers.authorization,'Bearer '+bearer);
  assert.equal(req.headers.cookie,undefined);
  assert.equal(req.headers.origin,undefined);
  res.writeHead(200,{'content-type':'application/json','x-secret':gate});
  res.end('{"profiles":[]}');
 });
 upstream.listen(0,'127.0.0.1');await once(upstream,'listening');
 const bridge=await startBrowserBridge({upstreamPort:upstream.address().port,operatorToken:gate,syntheticOnly:true});
 const path='/internal/mano/v1/profiles';
 const h={cookie:'mano_harness='+bridge.cookie,'x-mano-browser-test':'1',authorization:'Bearer '+bearer};
 try{
  for(const [p,headers,method] of [[path,{},'GET'],[path,{...h,origin:'http://other.test'},'GET'],[path,{...h,'sec-fetch-site':'cross-site'},'GET'],[path,{...h,'x-mano-operator-token':gate},'GET'],[path,h,'POST'],['/api/health',h,'GET']]){
   const r=await fetch(bridge.origin+p,{headers,method});assert.equal(r.status,404);await r.text();
  }
  assert.equal(calls,0);
  const r=await fetch(bridge.origin+path,{headers:h});
  assert.equal(r.status,200);assert.equal(r.headers.get('x-secret'),null);assert.equal(r.headers.get('cache-control'),'no-store');
  assert.deepEqual(await r.json(),{profiles:[]});assert.equal(calls,1);
 }finally{await bridge.close();await new Promise(resolve=>upstream.close(resolve));}
});
