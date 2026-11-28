import {test} from 'node:test';
import assert from 'node:assert/strict';
import {IndependentDemoAuth,type DemoAuthTransport} from './independentDemoAuth.ts';
const id='10000000-0000-4000-8000-000000000001';
test('synthetic challenge and verification require strict payload and explicit enablement',async()=>{
 const calls:Parameters<DemoAuthTransport>[0][]=[];
 const transport:DemoAuthTransport=async r=>{
   calls.push(r);
   return r.path.endsWith('/issue')?{status:202,data:{challengeId:id}}:{status:200,data:{token:'a'.repeat(43),expiresIn:1800}};
 };
 await assert.rejects(new IndependentDemoAuth(false,transport).issue('demo-parent'));
 const auth=new IndependentDemoAuth(true,transport);
 await assert.rejects(auth.issue('09123456789'));assert.equal(calls.length,0);
 assert.equal(await auth.issue('demo-parent'),id);
 await assert.rejects(auth.verify(id,'123'));assert.equal(calls.length,1);
 assert.equal(await auth.verify(id,'123456'),'a'.repeat(43));
 const bad=new IndependentDemoAuth(true,async()=>({status:202,data:{challengeId:id,code:'123456'}}));
 await assert.rejects(bad.issue('demo-parent'));
});
test('cancelled challenge cannot return a stale session',async()=>{
 let resolve!:(r:{status:number;data:unknown})=>void;
 const auth=new IndependentDemoAuth(true,()=>new Promise(r=>{resolve=r;}));
 const pending=auth.verify(id,'123456');auth.cancel();
 resolve({status:200,data:{token:'a'.repeat(43),expiresIn:1800}});
 await assert.rejects(pending);
});
