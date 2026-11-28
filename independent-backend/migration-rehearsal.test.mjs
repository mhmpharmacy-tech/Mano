import {test} from 'node:test';
import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import pg from 'pg';
import {rehearseMigration} from './migration-rehearsal.mjs';

test('synthetic migration validates actual PostgreSQL inserts but rolls back all records',async()=>{
 const url=new URL(process.env.MANO_TEST_ADMIN_URL||'invalid:');
 if(url.hostname!=='127.0.0.1'||url.pathname!=='/mano_stage'||url.username!=='postgres')throw new Error('Disposable CI only');
 const pool=new pg.Pool({connectionString:url.href});
 try{
  const id=randomUUID();
  await pool.query("INSERT INTO mano_stage.accounts(id,role) VALUES($1,'parent')",[id]);
  await pool.query("INSERT INTO mano_stage.demo_identity(handle,account_id) VALUES('demo-rehearsal',$1)",[id]);
  const row={sourceId:'demo-source-one',sourceOwnerId:'demo-parent',alias:'demo-migration',ageMonths:16};
  const command={syntheticOnly:true,profiles:[row],ownerMap:{'demo-parent':'demo-rehearsal'}};
  const count=async()=>Number((await pool.query('SELECT count(*) FROM mano_stage.profiles')).rows[0].count);
  const before=await count();
  assert.deepEqual(await rehearseMigration(pool,command),{validated:1,wouldInsert:1,unchanged:0,persisted:0,cutoverAllowed:false});
  assert.equal(await count(),before);
  await assert.rejects(rehearseMigration(pool,{...command,profiles:[row,row]}));
  await assert.rejects(rehearseMigration(pool,{...command,profiles:[row,{...row,sourceId:'demo-second',sourceOwnerId:'demo-missing'}],ownerMap:{...command.ownerMap,'demo-missing':'demo-nonexistent'}}));
  assert.equal(await count(),before);
  await assert.rejects(rehearseMigration(pool,{...command,syntheticOnly:false}));
 }finally{await pool.end();}
});
