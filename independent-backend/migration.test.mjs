import {test} from 'node:test';
import assert from 'node:assert/strict';
import {planInventory} from './migration-plan.mjs';
test('synthetic migration inventory is explicitly non-writing and fails closed',()=>{
 const result=planInventory({mode:'synthetic',counts:{profiles:2,doseLogs:4,consultations:1}});
 assert.equal(result.writes,0);assert.equal(result.cutoverAllowed,false);
 assert.equal(result.rows.length,3);
 assert.throws(()=>planInventory({mode:'production',counts:{profiles:1}}));
 assert.throws(()=>planInventory({mode:'synthetic',counts:{unknown:1}}));
 assert.throws(()=>planInventory({mode:'synthetic',counts:{profiles:-1}}));
});
