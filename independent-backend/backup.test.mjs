import {test} from 'node:test';
import assert from 'node:assert/strict';
import {randomBytes} from 'node:crypto';
import {sealDemoBackup,openDemoBackup} from './backup.mjs';
test('synthetic backup round trip, wrong key, tamper detection and data boundary',()=>{
 const key=randomBytes(32),data=[{alias:'demo-test',ageMonths:23}],blob=sealDemoBackup(data,key);
 assert.deepEqual(openDemoBackup(blob,key),data);
 assert.notDeepEqual(sealDemoBackup(data,key),blob);
 assert.throws(()=>openDemoBackup(blob,randomBytes(32)));
 const changed=Buffer.from(blob);changed[changed.length-1]^=1;
 assert.throws(()=>openDemoBackup(changed,key));
 assert.throws(()=>sealDemoBackup([{alias:'name',ageMonths:23}],key));
});
