// Pure, non-writing inventory planner. Accept only aggregate synthetic counts.
export const collectionMap=Object.freeze({
  profiles:'child_profiles (owner/consent revalidation)',
  doseLogs:'dose_events (actual event timestamps and provenance)',
  consultations:'legacy_read_only (no authoritative case promotion)',
  bookmarks:'bookmarks',
  feverLogs:'temperature_events (method and timestamp validation)',
  visitDrafts:'unverified_visit_drafts',
  vaccineRecords:'vaccine_events (clinical review)',
  syrupScans:'unverified_label_candidates'
});
export function planInventory(input){
  if(!input || input.mode!=='synthetic' || !input.counts || typeof input.counts!=='object' || Array.isArray(input.counts)) throw new Error('Synthetic aggregate inventory only');
  const rows=Object.entries(input.counts).map(([source,count])=>{
    if(!Object.hasOwn(collectionMap,source)||!Number.isSafeInteger(count)||count<0) throw new Error('Unknown collection or invalid count');
    return {source,count,target:collectionMap[source],status:'review-required'};
  });
  return {writes:0,cutoverAllowed:false,rows};
}
