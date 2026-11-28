import {test} from 'node:test';
import assert from 'node:assert/strict';
import {IndependentStageClient,type StageTransport} from './independentStageClient.ts';
const token='a'.repeat(43);
const id='10000000-0000-4000-8000-000000000001';
test('disabled adapter never invokes transport',async()=>{
 let calls=0;
 const c=new IndependentStageClient(false,async()=>{calls++;return {status:200,data:{profiles:[]}};});
 assert.throws(()=>c.setSession(token));await assert.rejects(c.list());assert.equal(calls,0);
});
test('validated synthetic profile, explicit consent, and no implicit retry',async()=>{
 const calls: Parameters<StageTransport>[0][]=[];
 const c=new IndependentStageClient(true,async r=>{calls.push(r);return r.method==='GET'?{status:200,data:{profiles:[{id,alias:'demo-a',age_months:12}]}}:{status:503,data:{}};});
 c.setSession(token);assert.deepEqual(await c.list(),[{id,alias:'demo-a',ageMonths:12}]);
 await assert.rejects(c.create('real name',12));await assert.rejects(c.grant(id,id,false));assert.equal(calls.length,1);
 await assert.rejects(c.create('demo-a',12));assert.equal(calls.length,2);
 assert.equal(calls[1].path,'/internal/mano/v1/profiles');
});
test('old response never crosses session switch even if transport ignores abort',async()=>{
 let resolve!: (r:{status:number;data:unknown})=>void;
 const c=new IndependentStageClient(true,()=>new Promise(r=>{resolve=r;}));
 c.setSession(token);const pending=c.list();c.setSession('b'.repeat(43));
 resolve({status:200,data:{profiles:[]}});await assert.rejects(pending,/STALE_OR_ABORTED/);
});
test('unauthorized clears session and bad response fails closed',async()=>{
 let calls=0;
 const c=new IndependentStageClient(true,async()=>{calls++;return {status:401,data:{}};});
 c.setSession(token);await assert.rejects(c.list());await assert.rejects(c.list());assert.equal(calls,1);
 const malformed=new IndependentStageClient(true,async()=>({status:200,data:{profiles:[{id,alias:'real child',age_months:12}]}}));
 malformed.setSession(token);await assert.rejects(malformed.list(),/INVALID_RESPONSE/);
});
