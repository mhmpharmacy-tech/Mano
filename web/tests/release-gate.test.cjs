"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const root = path.resolve(__dirname, "..");

test("Public health content has an explicit source registry and unreviewed status",()=>{
  const manifest = JSON.parse(fs.readFileSync(path.join(root,"clinical-content-status.json"),"utf8"));
  const html = fs.readFileSync(path.join(root,"index.html"),"utf8");
  assert.equal(manifest.schemaVersion,1);
  assert.ok(manifest.contentSections.length>=5);
  for (const section of manifest.contentSections) {
    assert.ok(html.includes('id="'+section.id+'"'),"Section missing: "+section.id);
    assert.ok(["NEEDS_CLINICAL_REVIEW","APPROVED","RETIRED"].includes(section.reviewStatus));
    for(const ref of section.references){
      assert.ok(Object.hasOwn(manifest.sourceRegister,ref),"Unregistered clinical source: "+ref);
    }
  }
  if (manifest.status !== "APPROVED_FOR_PUBLIC_CLINICAL_RELEASE") {
    assert.match(html,/<meta name="robots" content="noindex,nofollow">/,
      "An unreviewed medical preview must never become indexed by removing noindex");
  } else {
    assert.ok(typeof manifest.reviewerName === "string" && manifest.reviewerName.trim().length>4);
    assert.ok(typeof manifest.reviewedAt === "string" && /^\d{4}-\d{2}-\d{2}$/.test(manifest.reviewedAt));
    assert.ok(manifest.contentSections.every(s=>s.reviewStatus==="APPROVED"),"Every clinical section must be reviewed");
    assert.ok(Object.values(manifest.sourceRegister).every(r=>typeof r.verifiedOn==="string" && r.verifiedOn.length>0));
  }
});
