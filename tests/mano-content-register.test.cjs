const test=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const crypto=require('node:crypto');
const root=path.resolve(__dirname,'..');
const registry=require('../review/mano-content-register.json');
test('preview registry denies release and has eight unique drafts',()=>{
 assert.equal(registry.releaseAuthorized,false);
 assert.equal(registry.articles.length,8);
 assert.equal(new Set(registry.articles.map(a=>a.id)).size,8);
});
for(const a of registry.articles)test('version-bound clinical draft '+a.id,()=>{
 assert.match(a.id,/^[a-z-]+$/);
 assert.equal(a.path,'web/'+a.id+'.html');
 const bytes=fs.readFileSync(path.join(root,a.path));
 const html=bytes.toString('utf8');
 assert.equal(crypto.createHash('sha1').update('blob '+bytes.length+'\0').update(bytes).digest('hex'),a.blob);
 assert.match(html,/<meta[^>]+name="robots"[^>]+noindex/);
 assert.equal(a.review.status,'pending');
 assert.equal(a.review.reviewer,null);
 assert.equal(a.review.reviewedAt,null);
 assert.equal(a.review.sourceVerification,'pending');
 assert.equal(a.review.iranApplicability,'pending');
 assert.ok(a.sources.length>0);
 for(const source of a.sources){assert.match(source,/^https:\/\//);assert.ok(html.includes('href="'+source+'"'));}
});
