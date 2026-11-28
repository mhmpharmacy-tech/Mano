import {test} from 'node:test';
import assert from 'node:assert/strict';
import {randomBytes,randomUUID} from 'node:crypto';
import {readFile} from 'node:fs/promises';
import pg from 'pg';
import {createStageServer} from './server.mjs';
import {digest,databaseOptions} from './policy.mjs';
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
  const path='/v1/profiles/'+id+'/shares',consent={pharmacyId:f,consent:'synthetic-v1'};
  assert.equal((await call('POST',path,b,consent)).status,404);
  assert.equal((await call('POST',path,p,consent)).status,403);
  assert.equal((await call('POST',path,a,consent)).status,200);
  assert.deepEqual(await list(p),[id]);assert.deepEqual(await list(q),[]);
  await admin.query('UPDATE mano_stage.memberships SET active=false WHERE account_id=$1',[p.id]);
  assert.deepEqual(await list(p),[]);
  await admin.query('UPDATE mano_stage.memberships SET active=true WHERE account_id=$1',[p.id]);
  assert.equal((await call('DELETE',path+'/'+f,b)).status,404);
  assert.equal((await call('DELETE',path+'/'+f,a)).status,200);
  assert.deepEqual(await list(p),[]);
  await admin.query('UPDATE mano_stage.sessions SET revoked_at=now() WHERE account_id=$1',[a.id]);
  assert.equal((await call('GET','/v1/profiles',a)).status,401);
  await assert.rejects(pool.query('UPDATE mano_stage.accounts SET role=\'pharmacist\' WHERE id=$1',[b.id]),{code:'42501'});
  await assert.rejects(pool.query('DELETE FROM mano_stage.profiles WHERE id=$1',[id]),{code:'42501'});
});
