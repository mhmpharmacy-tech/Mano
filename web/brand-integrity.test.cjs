const test=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const crypto=require('node:crypto');
const lock=require('./brand-lock.json');
const engine=require('./mano-character.js');
for(const [name,expected] of Object.entries(lock.assets)){
 test('protected asset '+name,()=>{
  const bytes=fs.readFileSync(path.join(__dirname,'assets',name));
  const hash=crypto.createHash('sha1').update('blob '+bytes.length+'\0').update(bytes).digest('hex');
  assert.equal(hash,expected);
 });
}
test('all states use protected assets',()=>{
 for(const name of Object.values(engine.states)) assert.ok(Object.hasOwn(lock.assets,name));
});
test('known state applies',()=>{
 const attrs={};assert.equal(engine.apply({setAttribute:(k,v)=>attrs[k]=v},'skin'),true);
 assert.equal(attrs.src,'./assets/mascot_skin.png');
 assert.equal(attrs['data-mano-state'],'skin');
});
test('unknown states cannot mutate image',()=>{
 for(const state of ['__proto__','constructor','https://example.com','unknown']) assert.equal(engine.apply({setAttribute:()=>assert.fail('unexpected mutation')},state),false);
});
test('missing image fails safely',()=>assert.equal(engine.apply(null,'welcome'),false));

for(const [page,state] of Object.entries({'index':'welcome','mano-drug':'medicine','mano-growth':'listen','mano-skin':'skin','mano-nutrition':'welcome','mano-care':'welcome','mano-connect':'listen'})){
 test('page integration '+page,()=>{
  const html=fs.readFileSync(path.join(__dirname,page+'.html'),'utf8');
  assert.ok(html.includes('src="./mano-character.js" defer'));
  assert.ok(html.includes('href="./mano-character.css"'));
  const img=html.match(new RegExp('<img[^>]*data-mano-state="'+state+'"[^>]*>'));
  assert.ok(img);
  assert.ok(img[0].includes('src="./assets/'+engine.states[state]+'"'));
  assert.ok(html.includes('noindex'));
 });
}
