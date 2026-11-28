import {randomUUID} from 'node:crypto';
import {uuidPattern} from './policy.mjs';

// Privileged operator-only functions for explicitly synthetic fixtures.
// Never import into the HTTP listener or execute using the application role.
const handlePattern=/^demo-[a-z0-9]{1,40}$/;
async function transaction(pool,fn){
 const client=await pool.connect();
 try{
  await client.query('BEGIN');
  const {rows:[r]}=await client.query("SELECT current_database()='mano_stage' AND current_user<>'mano_stage_app' AND has_table_privilege(current_user,'mano_stage.accounts','UPDATE') AS ok");
  if(!r?.ok)throw new Error('Dedicated synthetic operator required');
  const result=await fn(client);
  await client.query('COMMIT');return result;
 }catch(e){try{await client.query('ROLLBACK');}catch{client.release(true);throw e;}throw e;}
 finally{if(!client.released) {try{client.release();}catch{}}}
}
export async function enrollDemo(pool,command){
 if(!command||command.syntheticOnly!==true||typeof command.handle!=='string'||!handlePattern.test(command.handle)||
    !['parent','pharmacist'].includes(command.role)||
    (command.role==='parent'?command.pharmacyId!==undefined:typeof command.pharmacyId!=='string'||!uuidPattern.test(command.pharmacyId))||
    Object.keys(command).some(k=>!['syntheticOnly','handle','role','pharmacyId'].includes(k)))throw new Error('Invalid synthetic enrollment');
 return transaction(pool,async c=>{
  const id=randomUUID();
  if(command.role==='pharmacist'){
   const q=await c.query('SELECT id FROM mano_stage.pharmacies WHERE id=$1 AND active FOR SHARE',[command.pharmacyId]);
   if(!q.rowCount)throw new Error('Active pharmacy required');
  }
  await c.query('INSERT INTO mano_stage.accounts(id,role) VALUES($1,$2)',[id,command.role]);
  await c.query('INSERT INTO mano_stage.demo_identity(handle,account_id) VALUES($1,$2)',[command.handle,id]);
  // Membership is granted only through the explicitly authorized operator operation.
  if(command.role==='pharmacist')await c.query('INSERT INTO mano_stage.memberships(account_id,pharmacy_id,active) VALUES($1,$2,true)',[id,command.pharmacyId]);
  return {created:true};
 });
}
export async function deactivateDemo(pool,command){
 if(!command||command.syntheticOnly!==true||typeof command.handle!=='string'||!handlePattern.test(command.handle)||
    Object.keys(command).some(k=>!['syntheticOnly','handle'].includes(k)))throw new Error('Invalid synthetic deactivation');
 return transaction(pool,async c=>{
  const {rows:[identity]}=await c.query('SELECT a.id FROM mano_stage.demo_identity i JOIN mano_stage.accounts a ON a.id=i.account_id WHERE i.handle=$1 FOR UPDATE OF a',[command.handle]);
  if(!identity)return {deactivated:false};
  await c.query('UPDATE mano_stage.accounts SET active=false WHERE id=$1',[identity.id]);
  await c.query('UPDATE mano_stage.sessions SET revoked_at=now() WHERE account_id=$1 AND revoked_at IS NULL',[identity.id]);
  await c.query('UPDATE mano_stage.memberships SET active=false WHERE account_id=$1',[identity.id]);
  return {deactivated:true};
 });
}
