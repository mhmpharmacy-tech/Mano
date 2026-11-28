import {createCipheriv,createDecipheriv,randomBytes} from 'node:crypto';
const context=Buffer.from('mano-synthetic-backup-v1');
const validKey=key=>Buffer.isBuffer(key)&&key.length===32;
function validate(records){
 if(!Array.isArray(records)||records.length>10000||records.some(r=>!r||Object.keys(r).sort().join(',')!=='ageMonths,alias'||typeof r.alias!=='string'||!/^demo-[a-z0-9]{1,40}$/.test(r.alias)||!Number.isInteger(r.ageMonths)||r.ageMonths<0||r.ageMonths>215)) throw new Error('Synthetic records only');
 return records;
}
export function sealDemoBackup(records,key){
 if(!validKey(key)) throw new Error('32-byte key required');
 const iv=randomBytes(12),cipher=createCipheriv('aes-256-gcm',key,iv);cipher.setAAD(context);
 const ciphertext=Buffer.concat([cipher.update(JSON.stringify(validate(records)),'utf8'),cipher.final()]);
 return Buffer.concat([iv,cipher.getAuthTag(),ciphertext]);
}
export function openDemoBackup(blob,key){
 if(!validKey(key)||!Buffer.isBuffer(blob)||blob.length<30||blob.length>1000000) throw new Error('Invalid backup');
 const decipher=createDecipheriv('aes-256-gcm',key,blob.subarray(0,12));decipher.setAAD(context);decipher.setAuthTag(blob.subarray(12,28));
 return validate(JSON.parse(Buffer.concat([decipher.update(blob.subarray(28)),decipher.final()]).toString('utf8')));
}
