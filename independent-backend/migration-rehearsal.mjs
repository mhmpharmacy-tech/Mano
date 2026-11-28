import {createHash} from 'node:crypto';
import {validProfile} from './policy.mjs';
const fixtureId=/^demo-[a-z0-9-]{1,60}$/;
const handle=/^demo-[a-z0-9]{1,40}$/;
function profileId(sourceId){
 const h=createHash('sha256').update('mano-synthetic-rehearsal-v1:'+sourceId).digest('hex');
 return h.slice(0,8)+'-'+h.slice(8,12)+'-4'+h.slice(13,16)+'-8'+h.slice(17,20)+'-'+h.slice(20,32);
}
// Only a disposable CI database. No Firebase reads, no commits, no real records.
export async function rehearseMigration(pool,command){
 if(!command||command.syntheticOnly!==true||!Array.isArray(command.profiles)||command.profiles.length>1000||
    !command.ownerMap||typeof command.ownerMap!=='object'||Array.isArray(command.ownerMap)||
    Object.keys(command).sort().join(',')!=='ownerMap,profiles,syntheticOnly')throw new Error('Synthetic rehearsal only');
 const ids=new Set();
 for(const r of command.profiles){
  if(!r||Object.keys(r).sort().join(',')!=='ageMonths,alias,sourceId,sourceOwnerId'||
     typeof r.sourceId!=='string'||!fixtureId.test(r.sourceId)||typeof r.sourceOwnerId!=='string'||!fixtureId.test(r.sourceOwnerId)||
     !validProfile({alias:r.alias,ageMonths:r.ageMonths})||ids.has(r.sourceId)||
     !Object.hasOwn(command.ownerMap,r.sourceOwnerId)||typeof command.ownerMap[r.sourceOwnerId]!=='string'||!handle.test(command.ownerMap[r.sourceOwnerId]))throw new Error('Invalid or duplicate synthetic mapping');
  ids.add(r.sourceId);
 }
 const c=await pool.connect();let discard=false;
 try{
  await c.query('BEGIN');
  const {rows:[db]}=await c.query("SELECT current_database()='mano_stage' AND current_user='postgres' AND inet_server_addr() IS NOT NULL AS ok");
  if(!db?.ok)throw new Error('Disposable CI owner required');
  let inserted=0,unchanged=0;
  for(const r of command.profiles){
   const {rows:[owner]}=await c.query("SELECT a.id FROM mano_stage.demo_identity i JOIN mano_stage.accounts a ON a.id=i.account_id WHERE i.handle=$1 AND a.role='parent' AND a.active",[command.ownerMap[r.sourceOwnerId]]);
   if(!owner)throw new Error('Mapped parent is unavailable');
   const id=profileId(r.sourceId);
   const {rows:[target]}=await c.query('SELECT owner_id,alias,age_months FROM mano_stage.profiles WHERE id=$1',[id]);
   if(target){
    if(target.owner_id!==owner.id||target.alias!==r.alias||target.age_months!==r.ageMonths)throw new Error('Target conflict');
    unchanged++;continue;
   }
   await c.query('INSERT INTO mano_stage.profiles(id,owner_id,alias,age_months) VALUES($1,$2,$3,$4)',[id,owner.id,r.alias,r.ageMonths]);
   inserted++;
  }
  return {validated:command.profiles.length,wouldInsert:inserted,unchanged,persisted:0,cutoverAllowed:false};
 }finally{
  try{await c.query('ROLLBACK');}catch(e){discard=true;throw e;}finally{c.release(discard);}
 }
}
