"use strict";
const test=require("node:test"),assert=require("node:assert/strict");
const fs=require("node:fs"),path=require("node:path"),os=require("node:os");
const {buildReviewQueue}=require("../tools/review-queue.cjs");
const {validatePacket,validateDecision,keys}=require("../review.js");
const queue=buildReviewQueue(),at="2026-11-17T10:00:00.000Z";
const d={id:queue.items[0].id,status:"approved",checks:[...keys],note:"",at};
const packet={schemaVersion:1,kind:"MANO_CONTENT_REVIEW_DRAFT",snapshot:queue.snapshot,reviewer:"EXAMPLE",releaseAuthorized:false,decisions:[d]};
test("Every registered clinical page and entry appears in the queue",()=>{
 assert.equal(queue.items.length,54);
 assert.equal(queue.items.filter(i=>i.group==="article").length,8);
 assert.equal(queue.items.filter(i=>i.group==="module").length,6);
 assert.equal(queue.items.filter(i=>i.group==="entry").length,33);
 assert.match(queue.snapshot,/^[a-f0-9]{64}$/);
 assert.ok(queue.hashes["logic.js"]);
});
test("No approval without all checks, no changes or hold without reason",()=>{
 assert.throws(()=>validateDecision({...d,checks:[]}));
 assert.throws(()=>validateDecision({...d,status:"changes",note:""}));
 assert.equal(validateDecision({...d,status:"hold",checks:[],note:"Review required"}).status,"hold");
});
test("Review packet is version-bound and cannot authorize release",()=>{
 assert.equal(validatePacket(packet,queue).releaseAuthorized,false);
 for(const bad of [{...packet,snapshot:"old-version"},{...packet,releaseAuthorized:true},{...packet,decisions:[d,d]},{...packet,decisions:[{...d,id:"unknown-item"}]},{...packet,reviewer:" "},{...packet,decisions:[{...d,checks:[]}]}])assert.throws(()=>validatePacket(bad,queue));
});
test("Changing a clinical behavior file invalidates the review",()=>{
 const tmp=fs.mkdtempSync(path.join(os.tmpdir(),"mano-review-"));
 try {
  const root=path.resolve(__dirname,"..");
  for(const n of Object.keys(queue.hashes)){const dst=path.join(tmp,n);fs.mkdirSync(path.dirname(dst),{recursive:true});fs.copyFileSync(path.join(root,n),dst)}
  fs.appendFileSync(path.join(tmp,"logic.js"),"\n// revision test\n");
  const newer=buildReviewQueue(tmp);assert.notEqual(newer.snapshot,queue.snapshot);assert.throws(()=>validatePacket(packet,newer));
 }finally{fs.rmSync(tmp,{recursive:true,force:true})}
});
