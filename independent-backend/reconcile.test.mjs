import {test} from 'node:test';
import assert from 'node:assert/strict';
import {reconcileDemoProfiles} from './reconcile.mjs';
test('migration dry run quarantines duplicates, missing owners and conflicts without writes',()=>{
 const row={sourceId:'demo-one',sourceOwnerId:'demo-parent',alias:'demo-a',ageMonths:10};
 const owners={'demo-parent':'demo-new-parent'};
 const target=[{sourceId:'demo-one',ownerId:'demo-new-parent',alias:'demo-a',ageMonths:10}];
 const a=reconcileDemoProfiles([row],owners,target);
 assert.equal(a.rows[0].status,'unchanged');assert.equal(a.writes,0);
 assert.equal(reconcileDemoProfiles([row],owners).rows[0].status,'would_insert');
 assert.equal(reconcileDemoProfiles([row,row],owners).quarantine.length,2);
 assert.equal(reconcileDemoProfiles([row],{}).quarantine[0].reason,'UNMAPPED_OWNER');
 assert.equal(reconcileDemoProfiles([{...row,ageMonths:11}],owners,target).quarantine[0].reason,'TARGET_CONFLICT');
 assert.equal(a.cutoverAllowed,false);
});
