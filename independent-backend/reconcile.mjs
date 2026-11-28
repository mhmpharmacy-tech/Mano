import {createHash} from 'node:crypto';
import {validProfile} from './policy.mjs';
// Only synthetic fixtures. No DB or Firebase calls. Duplicate and unknown owner records quarantine.
export function reconcileDemoProfiles(source,ownerMap,target=[]){
  if(!Array.isArray(source)||!Array.isArray(target)||source.length>10000||target.length>10000||
     !ownerMap||typeof ownerMap!=='object')throw new Error('Invalid synthetic inventory');
  const validId=x=>typeof x==='string'&&/^demo-[a-z0-9-]{1,60}$/.test(x);
  const seen=new Set(),existing=new Map(),out=[],rejected=[];
  const checksum=r=>createHash('sha256').update(JSON.stringify([r.sourceId,r.ownerId,r.alias,r.ageMonths])).digest('hex');
  for(const r of target){
    if(!r||!validId(r.sourceId)||!validId(r.ownerId)||!validProfile({alias:r.alias,ageMonths:r.ageMonths})||existing.has(r.sourceId))throw new Error('Invalid target inventory');
    existing.set(r.sourceId,checksum(r));
  }
  const occurrences=new Map();
  for(const r of source) if(validId(r?.sourceId))occurrences.set(r.sourceId,(occurrences.get(r.sourceId)||0)+1);
  for(let i=0;i<source.length;i++){
    const r=source[i];
    const reject=reason=>rejected.push({index:i,reason});
    if(!r||Object.keys(r).sort().join(',')!=='ageMonths,alias,sourceId,sourceOwnerId'||!validId(r.sourceId)||!validId(r.sourceOwnerId)||!validProfile({alias:r.alias,ageMonths:r.ageMonths})){reject('INVALID_SYNTHETIC_RECORD');continue;}
    if(occurrences.get(r.sourceId)!==1){reject('DUPLICATE_SOURCE');continue;}
    if(!Object.hasOwn(ownerMap,r.sourceOwnerId)||!validId(ownerMap[r.sourceOwnerId])){reject('UNMAPPED_OWNER');continue;}
    const mapped={sourceId:r.sourceId,ownerId:ownerMap[r.sourceOwnerId],alias:r.alias,ageMonths:r.ageMonths};
    const hash=checksum(mapped);
    if(existing.has(r.sourceId)&&existing.get(r.sourceId)!==hash){reject('TARGET_CONFLICT');continue;}
    seen.add(r.sourceId);
    out.push({sourceId:r.sourceId,hash,status:existing.has(r.sourceId)?'unchanged':'would_insert'});
  }
  return {sourceCount:source.length,targetCount:target.length,rows:out,quarantine:rejected,
    targetOnlyCount:[...existing.keys()].filter(id=>!seen.has(id)).length,writes:0,cutoverAllowed:false};
}
