import {randomBytes} from 'node:crypto';
import {once} from 'node:events';
import pg from 'pg';
import {createStageServer} from './server.mjs';
import {demoAuth} from './demo-auth.mjs';
import {databaseOptions} from './policy.mjs';

const token=()=>randomBytes(32).toString('base64url');
const pharmacy='20000000-0000-4000-8000-000000000001';
let pool,server,profileId,shared=false;
const sessions=[];
const env={MANO_INDEPENDENT_STAGE_ENABLED:'true',MANO_STAGE_ACCESS_TOKEN:token(),
 MANO_STAGE_DEMO_AUTH_ENABLED:'true',MANO_STAGE_OTP_KEY:token(),
 MANO_STAGE_DB_TLS:'private-test'};
let step='configuration';
function check(ok,label){step=label;if(!ok)throw new Error('Check failed');console.log('PASS:',label);}
try{
 if(process.argv[2]!=='--synthetic-only'||!process.env.PGPASSWORD)throw new Error('Explicit synthetic mode and password required');
 env.MANO_STAGE_DATABASE_URL='postgresql://mano_stage_app:'+encodeURIComponent(process.env.PGPASSWORD)+'@mano-db:5432/mano_stage';
 delete process.env.PGPASSWORD;
 pool=new pg.Pool(databaseOptions(env));
 pool.on('error',()=>{console.error('POOL ERROR');process.exitCode=1;});
 const delivery=new Map();
 const auth=demoAuth(pool,env,async m=>{delivery.set(m.id,m.code);});
 server=createStageServer(pool,env);
 server.listen(0,'127.0.0.1');await once(server,'listening');
 const base='http://127.0.0.1:'+server.address().port;
 const call=async(method,path,secret,body)=>{
  const r=await fetch(base+path,{method,headers:{'x-mano-stage-token':env.MANO_STAGE_ACCESS_TOKEN,authorization:'Bearer '+secret,...(body?{'content-type':'application/json'}:{})},...(body?{body:JSON.stringify(body)}:{}),signal:AbortSignal.timeout(8000)});
  return {status:r.status,data:await r.json()};
 };
 const list=async secret=>{const r=await call('GET','/v1/profiles',secret);if(r.status!==200)throw new Error('List failed');return r.data.profiles.map(p=>p.id);};
 try{
  for(const handle of ['demo-parentone','demo-parenttwo','demo-pharmacist']){
   step='OTP '+handle;
   const {challengeId}=await auth.issue(handle);
   const code=delivery.get(challengeId);delivery.delete(challengeId);
   if(!code)throw new Error('Demo identity absent or cooldown');
   const session=await auth.verify(challengeId,code);
   if(!session?.token)throw new Error('OTP failed');
   sessions.push(session.token);
   check(await auth.verify(challengeId,code)===null,'OTP '+handle+' / replay rejected');
  }
  const [a,b,p]=sessions;
  step='create synthetic profile';
  const r=await call('POST','/v1/profiles',a,{alias:'demo-smoke-'+randomBytes(6).toString('hex'),ageMonths:24});
  if(r.status===201)profileId=r.data.id;
  check(r.status===201&&!!profileId,'parent creates profile');
  check((await list(a)).includes(profileId),'owner can read');
  check(!(await list(b)).includes(profileId),'other parent denied');
  check(!(await list(p)).includes(profileId),'pharmacist denied before consent');
  const path='/v1/profiles/'+profileId+'/shares';
  check((await call('POST',path,b,{pharmacyId:pharmacy,consent:'synthetic-v1'})).status===404,'other parent cannot grant');
  step='grant consent';
  const grant=await call('POST',path,a,{pharmacyId:pharmacy,consent:'synthetic-v1'});
  if(grant.status===200)shared=true;
  check(grant.status===200,'owner grants consent');
  check((await list(p)).includes(profileId),'pharmacist can read after consent');
  check((await call('DELETE',path+'/'+pharmacy,a)).status===200,'owner revokes consent');
  shared=false;
  check(!(await list(p)).includes(profileId),'pharmacist denied after revocation');
  for(const secret of sessions){
   check((await call('POST','/v1/session/logout',secret)).status===200,'logout');
   check((await call('GET','/v1/profiles',secret)).status===401,'revoked session denied');
  }
  console.log('MANO SYNTHETIC SMOKE: PASS');
 }finally{
  if(shared&&profileId&&sessions[0]){
   try{const r=await call('DELETE','/v1/profiles/'+profileId+'/shares/'+pharmacy,sessions[0]);if(r.status!==200)throw new Error();}catch{console.error('CLEANUP: REVIEW DEMO SHARE IN PGADMIN');process.exitCode=1;}
  }
  for(const secret of sessions){try{await call('POST','/v1/session/logout',secret);}catch{console.error('CLEANUP: DEMO SESSION EXPIRES IN 30 MIN');process.exitCode=1;}}
 }
}catch(e){console.error('MANO SYNTHETIC SMOKE: FAIL at',step,'('+ (e.code||e.name)+')');process.exitCode=1;}
finally{if(server?.listening)await new Promise(resolve=>server.close(resolve));if(pool)await pool.end();}
