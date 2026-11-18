"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const root = path.resolve(__dirname, "..");
const get = name => fs.readFileSync(path.join(root,name),"utf8");
const home = get("index.html");
const staff = get("staff-demo.html");
const app = get("app.js");

test("SEO: prelaunch pages remain unindexed with distinct titles and no fictional expert signoff",()=>{
  for(const html of [home,staff]){
    const titles=[...html.matchAll(/<title>([^<]+)<\/title>/g)];
    assert.equal(titles.length,1,"Exactly one title per page");
    assert.ok(titles[0][1].length>=15 && titles[0][1].length<=95);
    assert.match(html,/<html lang="fa" dir="rtl">/);
    assert.match(html,/<meta name="robots" content="noindex,nofollow">/);
    assert.ok(!/<link[^>]+rel="canonical"/.test(html),"Do not claim a canonical host before verified deployment");
    assert.ok(!/<script[^>]+src="https?:\/\//.test(html),"No tracking/CDN scripts");
  }
  assert.equal((home.match(/<h1(?:\s|>)/g)||[]).length,1);
  assert.match(home,/<meta name="description" content="[^"]{50,180}">/);
  assert.match(home,/<meta property="og:locale" content="fa_IR">/);
  assert.match(home,/id="editorial"/);
  assert.match(home,/هنوز ثبت نشده/);
  assert.match(home,/بازبینی علمی/);
});

test("Parent flow: emergency route is visible and Arabic/Persian keyboard inputs are supported",()=>{
  assert.match(home,/id="child-temp" type="text" inputmode="decimal"/);
  assert.match(home,/id="temperature-help"/);
  assert.match(home,/id="screening-result"[^>]+aria-live="assertive"/);
  assert.ok(app.includes('action.href = "tel:115"'));
  assert.ok(app.includes('response.kind === "danger"'));
  assert.ok(app.includes('role", urgent ? "alert" : "status"'));
});

test("Visual performance baseline: local branding, lazy offscreen visuals and explicit budget",()=>{
  const assetsDir=path.join(root,"assets");
  const images=fs.readdirSync(assetsDir).filter(f=>/\.(jpe?g|png|webp|avif)$/i.test(f));
  const total=images.reduce((n,name)=>n+fs.statSync(path.join(assetsDir,name)).size,0);
  assert.ok(total < 2_000_000, "Preview images above 2MB; optimize images before adding more: "+total);
  assert.ok(fs.statSync(path.join(assetsDir,"mano_pose_welcome.png")).size<60_000,
    "Hero mascot too heavy for initial mobile render");
  assert.match(home,/class="hero-mascot"[^>]+fetchpriority="high"/);
  assert.ok((home.match(/loading="lazy"/g)||[]).length>=4,"Lazy-load offscreen illustrations");
  assert.match(get("styles.css"),/font-display: swap/);
  assert.ok(!/onerror=|onload=/.test(home),"Avoid inline event handlers");
});

test("Medical content governance: source and review status must not be simulated",()=>{
  const status=JSON.parse(get("clinical-content-status.json"));
  assert.equal(status.status,"NOT_APPROVED_FOR_PUBLIC_CLINICAL_RELEASE");
  assert.equal(status.reviewedAt,null);
  assert.ok(status.contentSections.every(s=>s.reviewStatus==="NEEDS_CLINICAL_REVIEW"));
  assert.ok(!/<script[^>]+type="application\/ld\+json"/.test(home),
    "Do not inject medical schema or fake credentials before clinical approval");
  assert.ok(!fs.existsSync(path.join(root,"sitemap.xml")),
    "Do not publish an indexable sitemap for unapproved pediatric medical content");
});

test("Publishable written content has a non-fictional editorial template and review contract",()=>{
  const schema=JSON.parse(get("content/ARTICLE_TEMPLATE.json"));
  assert.equal(schema.status,"DRAFT_NOT_FOR_PUBLICATION");
  assert.equal(schema.reviewedBy,null);
  assert.equal(schema.clinicalApproved,false);
  assert.equal(schema.sources.length,0);
  assert.ok(Array.isArray(schema.safetyFlags));
  assert.ok(fs.existsSync(path.join(root,"seo/SEARCH_READINESS.md")));
  assert.ok(fs.existsSync(path.join(root,"TEAM_REVIEW_2026-11-17.md")));
});


test("Mascot identity: all six modules have matching local icons and Persian navigation",()=>{
 const ids=["drug","growth","skin","nutrition","care","connect"];
 const icons=get("assets/mano-module-icons.svg");
 assert.ok(Buffer.byteLength(icons)<20000,"SVG icon budget");
 for(const id of ids){
  assert.equal([...icons.matchAll(new RegExp('<symbol id="'+id+'"',"g"))].length,1);
  const html=get("mano-"+id+".html");
  assert.match(html,/<html lang="fa" dir="rtl">/);
  assert.match(html,/href="\.\/mano-family\.css"/);
  const nav=html.match(/<nav class="mano-world-nav"[\s\S]*?<\/nav>/)?.[0];
  assert.ok(nav,id);
  assert.equal((nav.match(/aria-current="page"/g)||[]).length,1);
  assert.ok(nav.includes('href="./mano-'+id+'.html" aria-current="page"'));
  for(const target of ids){
   assert.ok(nav.includes('href="./mano-'+target+'.html"'),target);
   assert.ok(nav.includes('href="./assets/mano-module-icons.svg#'+target+'"'),target);
  }
 }
});

test("Internal preview includes mascot assets with integrity manifest",()=>{
 const os=require("node:os"),crypto=require("node:crypto");
 const tmp=fs.mkdtempSync(path.join(os.tmpdir(),"mano-family-package-"));
 try{
  const dest=path.join(tmp,"preview");
  require("../tools/build-preview.cjs").buildPreview(dest);
  const manifest=JSON.parse(fs.readFileSync(path.join(dest,"PREVIEW_MANIFEST.json"),"utf8"));
  assert.equal(manifest.clinicalServiceEnabled,false);
  for(const name of ["mano-family.css","assets/mano-module-icons.svg"]){
   const data=fs.readFileSync(path.join(dest,name));
   assert.equal(manifest.sha256[name],crypto.createHash("sha256").update(data).digest("hex"));
  }
 }finally{fs.rmSync(tmp,{recursive:true,force:true})}
});
