import {test} from 'node:test';
import assert from 'node:assert/strict';
import {randomBytes,randomUUID} from 'node:crypto';
import {readFile} from 'node:fs/promises';
import pg from 'pg';
import {createStageServer} from './server.mjs';
import {digest,databaseOptions} from './policy.mjs';
import {demoAuth} from './demo-auth.mjs';
import {createIntegrationHost,startIntegration} from './integration-host.mjs';
import {IndependentStageClient} from '../src/online/independentStageClient.ts';
import {once} from 'node:events';
const token=()=>randomBytes(32).toString('base64url');
test('real PostgreSQL: protected stage, parent isolation, pharmacist consent and revocation',async t=>{
  const adminUrl=new URL(process.env.MANO_TEST_ADMIN_URL||'invalid:');
  if(!['127.0.0.1','localhost'].includes(adminUrl.hostname)||adminUrl.pathname!=='/mano_stage') throw new Error('Use an empty local CI database only');
  const admin=new pg.Pool({connectionString:adminUrl.href});
  t.after(()=>admin.end());
  await admin.query("CREATE ROLE mano_stage_app LOGIN PASSWORD 'ci-synthetic-only-password'");
  await admin.query(await readFile(new URL('./schema.sql',import.meta.url),'utf8'));
  const env={MANO_INDEPENDENT_STAGE_ENABLED:'true',MANO_STAGE_ACCESS_TOKEN:token(),
    MANO_STAGE_DATABASE_URL:'postgresql://mano_stage_app:ci-synthetic-only-password@'+adminUrl.host+'/mano_stage',
    MANO_STAGE_DB_TLS:'private-test'};
  const pool=new pg.Pool(databaseOptions(env));t.after(()=>pool.end());
  async function account(role,expired=false){
    const id=randomUUID(),secret=token();
    await admin.query('INSERT INTO mano_stage.accounts(id,role) VALUES($1,$2)',[id,role]);
    await admin.query('INSERT INTO mano_stage.sessions(token_hash,account_id,expires_at) VALUES($1,$2,$3)',
      [digest(secret),id,new Date(Date.now()+(expired?-60000:600000))]);
    return {id,secret};
  }
  const a=await account('parent'),b=await account('parent'),p=await account('pharmacist'),q=await account('pharmacist'),expired=await account('parent',true);
  const f=randomUUID(),other=randomUUID();
  for(const id of [f,other]) await admin.query('INSERT INTO mano_stage.pharmacies(id) VALUES($1)',[id]);
  await admin.query('INSERT INTO mano_stage.memberships(account_id,pharmacy_id,active) VALUES($1,$2,true),($3,$4,true)',[p.id,f,q.id,other]);
  const server=createStageServer(pool,env);
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  t.after(()=>new Promise(resolve=>server.close(resolve)));
  const base='http://127.0.0.1:'+server.address().port;
  async function call(method,path,who,body,gate=true){
    const r=await fetch(base+path,{method,headers:{...(gate?{'x-mano-stage-token':env.MANO_STAGE_ACCESS_TOKEN}:{}),
      ...(who?{authorization:'Bearer '+who.secret}:{}),...(body?{'content-type':'application/json'}:{})},
      ...(body?{body:JSON.stringify(body)}:{})});
    assert.equal(r.headers.get('cache-control'),'no-store');
    return {status:r.status,data:await r.json()};
  }
  assert.equal((await call('GET','/v1/profiles',a,undefined,false)).status,404);
  env.MANO_INDEPENDENT_STAGE_ENABLED='false';
  assert.equal((await call('GET','/v1/profiles',a)).status,404);
  env.MANO_INDEPENDENT_STAGE_ENABLED='true';
  assert.equal((await call('GET','/v1/profiles')).status,401);
  assert.equal((await call('GET','/v1/profiles',expired)).status,401);
  assert.equal((await call('POST','/v1/profiles',a,{alias:'demo-a',ageMonths:12,ownerId:b.id})).status,400);
  assert.equal((await call('POST','/v1/profiles',p,{alias:'demo-a',ageMonths:12})).status,403);
  const created=await call('POST','/v1/profiles',a,{alias:'demo-a',ageMonths:12});
  assert.equal(created.status,201);const id=created.data.id;
  const list=async who=>(await call('GET','/v1/profiles',who)).data.profiles.map(x=>x.id);
  assert.deepEqual(await list(a),[id]);assert.deepEqual(await list(b),[]);assert.deepEqual(await list(p),[]);
  assert.deepEqual((await pool.query('SELECT id FROM mano_stage.profiles')).rows,[]);
  await assert.rejects(pool.query('SELECT token_hash FROM mano_stage.sessions'),{code:'42501'});
  const scoped=await pool.connect();
  try {
    await scoped.query('BEGIN');
    await scoped.query("SELECT set_config('mano_stage.session_digest',$1,true)",[digest(b.secret)]);
    assert.deepEqual((await scoped.query('SELECT id FROM mano_stage.profiles')).rows,[]);
    await scoped.query('ROLLBACK');
  } finally {scoped.release();}
  await admin.query('INSERT INTO mano_stage.demo_identity(handle,account_id) VALUES($1,$2)',['demo-auth',b.id]);
  const deliveries=[];
  const auth=demoAuth(pool,{...env,MANO_STAGE_DEMO_AUTH_ENABLED:'true',MANO_STAGE_OTP_KEY:token()},async msg=>{deliveries.push(msg);});
  assert.equal(demoAuth(pool,env,()=>{}),null);
  await auth.issue('demo-missing');assert.equal(deliveries.length,0);
  const challenge=await auth.issue('demo-auth');assert.equal(deliveries.length,1);
  assert.deepEqual(Object.keys(challenge),['challengeId']);
  await auth.issue('demo-auth');assert.equal(deliveries.length,1);
  const code=deliveries[0].code,wrong=code==='000000'?'000001':'000000';
  assert.equal(await auth.verify(challenge.challengeId,wrong),null);
  const newSession=await auth.verify(challenge.challengeId,code);assert.ok(newSession.token);
  assert.equal(await auth.verify(challenge.challengeId,code),null);
  const signed={secret:newSession.token};
  assert.equal((await call('GET','/v1/profiles',signed)).status,200);
  assert.equal((await call('POST','/v1/session/logout',signed)).status,200);
  assert.equal((await call('GET','/v1/profiles',signed)).status,401);
  await admin.query("UPDATE mano_stage.demo_challenge SET issued_at=now()-interval '2 minutes' WHERE handle='demo-auth'");
  const second=await auth.issue('demo-auth');
  const wrong2=deliveries[1].code==='000000'?'000001':'000000';
  for(let i=0;i<5;i++)assert.equal(await auth.verify(second.challengeId,wrong2),null);
  assert.equal(await auth.verify(second.challengeId,deliveries[1].code),null);
  const path='/v1/profiles/'+id+'/shares',consent={pharmacyId:f,consent:'synthetic-v1'};
  assert.equal((await call('POST',path,b,consent)).status,404);
  assert.equal((await call('POST',path,p,consent)).status,403);
  assert.equal((await call('POST',path,a,consent)).status,200);
  assert.deepEqual(await list(p),[id]);assert.deepEqual(await list(q),[]);
  const operatorToken=token();
  const host=createIntegrationHost(pool,{...env,MANO_INTEGRATION_ENABLED:'true',MANO_INTEGRATION_OPERATOR_TOKEN:operatorToken});
  host.listen(0,'127.0.0.1');await once(host,'listening');
  try{
    const url='http://127.0.0.1:'+host.address().port+'/internal/mano/v1/profiles';
    const get=async who=>{
      const r=await fetch(url,{headers:{'x-mano-operator-token':operatorToken,authorization:'Bearer '+who.secret}});
      assert.equal(r.status,200);return (await r.json()).profiles.map(p=>p.id);
    };
    assert.deepEqual(await get(a),[id]);assert.deepEqual(await get(b),[]);assert.deepEqual(await get(p),[id]);assert.deepEqual(await get(q),[]);
  }finally{await new Promise(resolve=>host.close(resolve));}
  // Real TypeScript adapter over the internal HTTP listener and the actual database.
  const launchEnv={...env,MANO_INTEGRATION_ENABLED:'true',MANO_INTEGRATION_SYNTHETIC_ONLY:'true',
    MANO_INTEGRATION_OPERATOR_TOKEN:token(),MANO_INTEGRATION_PORT:'0',
    MANO_STAGE_DEMO_AUTH_ENABLED:'true',MANO_STAGE_OTP_KEY:token()};
  await assert.rejects(startIntegration({...launchEnv,MANO_INTEGRATION_SYNTHETIC_ONLY:'false'}));
  const otpMessages=[];
  await admin.query('INSERT INTO mano_stage.demo_identity(handle,account_id) VALUES($1,$2)',['demo-integration',q.id]);
  const running=await startIntegration(launchEnv,async msg=>{otpMessages.push(msg);});
  try {
    assert.equal(running.server.address().address,'127.0.0.1');
    const base='http://127.0.0.1:'+running.server.address().port;
    const authRequest=async(action,body,withGate=true)=>{
      const r=await fetch(base+'/internal/mano/v1/auth/'+action,{method:'POST',headers:{'content-type':'application/json',
        ...(withGate?{'x-mano-operator-token':launchEnv.MANO_INTEGRATION_OPERATOR_TOKEN}:{})},body:JSON.stringify(body)});
      assert.equal(r.headers.get('cache-control'),'no-store');return {status:r.status,data:await r.json()};
    };
    assert.equal((await authRequest('issue',{handle:'demo-integration'},false)).status,404);
    assert.equal((await authRequest('issue',{handle:'09123456789'})).status,400);
    assert.equal((await authRequest('issue',{handle:'demo-unknown'})).status,202);
    assert.equal(otpMessages.length,0);
    const challenge=await authRequest('issue',{handle:'demo-integration'});
    assert.equal(challenge.status,202);assert.deepEqual(Object.keys(challenge.data),['challengeId']);
    assert.equal(otpMessages.length,1);
    const proof={challengeId:challenge.data.challengeId,code:otpMessages[0].code};
    const verified=await authRequest('verify',proof);
    assert.equal(verified.status,200);assert.equal(typeof verified.data.token,'string');
    assert.equal((await authRequest('verify',proof)).status,401);
    const transport=async req=>{
      const response=await fetch(base+req.path,{method:req.method,redirect:'error',signal:req.signal,
        headers:{'x-mano-operator-token':launchEnv.MANO_INTEGRATION_OPERATOR_TOKEN,authorization:'Bearer '+req.bearer,
          ...(req.body?{'content-type':'application/json'}:{})},
        ...(req.body?{body:JSON.stringify(req.body)}:{})});
      return {status:response.status,data:await response.json()};
    };
    const parent=new IndependentStageClient(true,transport);
    const pharmacist=new IndependentStageClient(true,transport);
    const otherParent=new IndependentStageClient(true,transport);
    parent.setSession(a.secret);pharmacist.setSession(p.secret);otherParent.setSession(b.secret);
    const otherPharmacy=new IndependentStageClient(true,transport);
    otherPharmacy.setSession(verified.data.token);
    assert.deepEqual(await otherPharmacy.list(),[]);
    await otherPharmacy.logout();
    assert.equal((await call('DELETE',path+'/'+f,b)).status,404);
    assert.ok((await parent.list()).some(row=>row.id===id&&row.ageMonths===12));
    assert.deepEqual(await otherParent.list(),[]);
    assert.ok((await pharmacist.list()).some(row=>row.id===id));
    // A second synthetic profile tests the complete adapter write/consent cycle.
    const adapterId=await otherParent.create('demo-adapter',25);
    assert.ok((await otherParent.list()).some(row=>row.id===adapterId));
    assert.ok(!(await parent.list()).some(row=>row.id===adapterId));
    assert.ok(!(await pharmacist.list()).some(row=>row.id===adapterId));
    await otherParent.grant(adapterId,f,true);
    assert.ok((await pharmacist.list()).some(row=>row.id===adapterId));
    await otherParent.revoke(adapterId,f);
    assert.ok(!(await pharmacist.list()).some(row=>row.id===adapterId));
    await otherParent.logout();
    await assert.rejects(otherParent.list());
    // Logout must invalidate the same bearer even outside this adapter.
    assert.equal((await call('GET','/v1/profiles',b)).status,401);
  }finally{await running.close();}
  await admin.query('UPDATE mano_stage.memberships SET active=false WHERE account_id=$1',[p.id]);
  assert.deepEqual(await list(p),[]);
  await admin.query('UPDATE mano_stage.memberships SET active=true WHERE account_id=$1',[p.id]);
  assert.equal((await call('DELETE',path+'/'+f,b)).status,401);
  assert.equal((await call('DELETE',path+'/'+f,a)).status,200);
  assert.deepEqual(await list(p),[]);
  await admin.query('UPDATE mano_stage.sessions SET revoked_at=now() WHERE account_id=$1',[a.id]);
  assert.equal((await call('GET','/v1/profiles',a)).status,401);
  await assert.rejects(pool.query('UPDATE mano_stage.accounts SET role=\'pharmacist\' WHERE id=$1',[b.id]),{code:'42501'});
  await assert.rejects(pool.query('DELETE FROM mano_stage.profiles WHERE id=$1',[id]),{code:'42501'});
  await assert.rejects(pool.query('SELECT * FROM mano_stage.audit_events'),{code:'42501'});
  const events=(await admin.query('SELECT actor_id,action FROM mano_stage.audit_events')).rows;
  assert.equal(events.filter(e=>e.action==='profile.create').length,2);
  assert.equal(events.filter(e=>e.action==='share.grant').length,2);
  assert.equal(events.filter(e=>e.action==='share.revoke').length,2);
  assert.ok(events.some(e=>e.actor_id===b.id&&e.action==='session.logout'));
});
