import {test} from 'node:test';
import assert from 'node:assert/strict';
import {randomBytes} from 'node:crypto';
import {bearer,databaseOptions,digest,sameToken,validProfile,validShare} from './policy.mjs';
const token=()=>randomBytes(32).toString('base64url');
test('session token and deployment gate reject malformed or mismatched credentials',()=>{
  const a=token(),b=token();
  assert.equal(bearer('Bearer '+a),a);
  assert.equal(bearer('Bearer '+a+' '),null);
  assert.equal(sameToken(a,a),true);assert.equal(sameToken(a,b),false);
  assert.equal(sameToken(undefined,a),false);assert.equal(digest(a).length,64);
});
test('synthetic profiles forbid arbitrary data, ownership claims and invalid ages',()=>{
  assert.ok(validProfile({alias:'demo-alpha',ageMonths:23}));
  for(const body of [{alias:'demo-alpha',ageMonths:23,ownerId:'x'},{alias:'real name',ageMonths:23},
    {alias:'demo-alpha',ageMonths:-1},{alias:'demo-alpha',ageMonths:216},{alias:'demo-alpha',ageMonths:2.4},
    {alias:'demo-alpha',ageMonths:23,allergies:'x'},null]) assert.ok(!validProfile(body));
});
test('explicit versioned consent only',()=>{
  const pharmacyId='00000000-0000-0000-0000-000000000001';
  assert.ok(validShare({pharmacyId,consent:'synthetic-v1'}));
  assert.ok(!validShare({pharmacyId,consent:true}));
  assert.ok(!validShare({pharmacyId,consent:'synthetic-v1',role:'admin'}));
});
test('runtime rejects admin credentials, wrong databases, and SSL override in URI',()=>{
  const url='postgresql://mano_stage_app:fake@mano-db:5432/mano_stage';
  const e={MANO_STAGE_DATABASE_URL:url,MANO_STAGE_DB_TLS:'private-test'};
  assert.equal(databaseOptions(e).ssl,false);
  assert.deepEqual(databaseOptions({...e,MANO_STAGE_DB_TLS:'verify-full'}).ssl,{rejectUnauthorized:true});
  for(const u of [url.replace('mano_stage_app','root'),url.replace('/mano_stage','/postgres'),url+'?sslmode=disable',url.replace('mano-db','example.com')])
    assert.throws(()=>databaseOptions({...e,MANO_STAGE_DATABASE_URL:u}));
  assert.throws(()=>databaseOptions({MANO_STAGE_DATABASE_URL:url}));
});
