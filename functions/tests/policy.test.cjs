"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const { isId, isVerifiedPharmacist, validateIntake, validateStaffReview, buildParentCaseView, isValidTriagePass } = require("../consultation-policy");

const auth = { uid: "staff-001", token: { manoStaffVerified:true, manoStaffRole:"pharmacist", manoPharmacyId:"pharmacy_demo" } };
const goodCase = { pharmacyId:"pharmacy_demo",categoryName:"OTC",ageBandId:"toddler_1_3y",questionSummaryFa:"آیا این دو شربت تداخل دارویی دارند؟",consentAccepted:true,privacyNoticeVersion:"v1",hasUrgentFlag:false,triagePassId:"pass_123" };

test("IDs cannot traverse paths or use arbitrary document selectors",()=>{
  assert.equal(isId("pharmacy_demo"),true);
  assert.equal(isId("../private"),false);
  assert.equal(isId("a/b"),false);
  assert.equal(isId(""),false);
});
test("Only explicitly verified pharmacist of exact pharmacy may review",()=>{
  assert.equal(isVerifiedPharmacist(auth,"pharmacy_demo"),true);
  assert.equal(isVerifiedPharmacist(auth,"pharmacy_other"),false);
  assert.equal(isVerifiedPharmacist({uid:"staff-001",token:{...auth.token,manoStaffVerified:false}},"pharmacy_demo"),false);
  assert.equal(isVerifiedPharmacist({uid:"staff-001",token:{...auth.token,manoStaffRole:"parent"}},"pharmacy_demo"),false);
  assert.equal(isVerifiedPharmacist(null,"pharmacy_demo"),false);
});
test("Intake accepts only consented, nonurgent, bounded requests",()=>{
  const parsed=validateIntake(goodCase);
  assert.equal(parsed.pharmacyId,"pharmacy_demo");
  assert.equal(parsed.questionSummaryFa,goodCase.questionSummaryFa);
  assert.equal(validateIntake({...goodCase,hasUrgentFlag:true}),null);
  assert.equal(validateIntake({...goodCase,consentAccepted:false}),null);
  assert.equal(validateIntake({...goodCase,privacyNoticeVersion:"unknown"}),null);
  assert.equal(validateIntake({...goodCase,ageBandId:"not_real"}),null);
  assert.equal(validateIntake({...goodCase,questionSummaryFa:"x".repeat(601)}),null);
  assert.equal(validateIntake({...goodCase,pharmacyId:"../bad"}),null);
  assert.equal(validateIntake({...goodCase,triagePassId:""}),null);
});

test("The clinical reviewer must be a verified pharmacist, never merely a pharmacy admin", () => {
  const admin = { uid:"admin-001", token:{...auth.token,manoStaffRole:"admin"} };
  assert.equal(isVerifiedPharmacist(admin,"pharmacy_demo"),false);
});

const reviewedPayload = Object.freeze({
  status:"REVIEWED",
  decision:"REFER",
  reviewNoteFa:"یادداشت داخلی فقط جهت مرور بالینی داروساز.",
  parentReplyFa:"این پیام برای خانواده نوشته شده و باید فقط پس از تأیید ارسال شود.",
  parentReplyApproved:true,
  sourceFa:"NICE NG143",
});

test("Staff must receive before review and explicitly approve public reply",()=>{
  assert.deepEqual(
    validateStaffReview({status:"RECEIVED"},"PENDING"),
    {status:"RECEIVED",decision:null,reviewNoteFa:null,parentReplyFa:null,sourceFa:null}
  );
  assert.equal(validateStaffReview(reviewedPayload,"PENDING"),null);
  assert.equal(validateStaffReview({status:"PENDING"},"RECEIVED"),null);
  assert.equal(validateStaffReview({status:"RECEIVED"},"REVIEWED"),null);
  assert.equal(validateStaffReview({...reviewedPayload,parentReplyApproved:false},"RECEIVED"),null);
  assert.equal(validateStaffReview({...reviewedPayload,parentReplyFa:""},"RECEIVED"),null);
  const review = validateStaffReview(reviewedPayload,"RECEIVED");
  assert.equal(review.status,"REVIEWED");
  assert.equal(review.decision,"REFER");
  assert.equal(review.reviewNoteFa,reviewedPayload.reviewNoteFa);
  assert.equal(review.parentReplyFa,reviewedPayload.parentReplyFa);
});
test("Forged reviewer notes and invalid workflow states cannot pass",()=>{
  assert.equal(validateStaffReview({...reviewedPayload,decision:"OTHER"},"RECEIVED"),null);
  assert.equal(validateStaffReview({...reviewedPayload,sourceFa:""},"RECEIVED"),null);
  assert.equal(validateStaffReview({...reviewedPayload,reviewNoteFa:"too short"},"RECEIVED"),null);
});
test("Parent-visible result never includes private review note, staff identity or raw Firestore fields",()=>{
  const record = {
    categoryName:"OTC",
    ageBandId:"toddler_1_3y",
    status:"REVIEWED",
    decision:"REFER",
    reviewNoteFa:"CONFIDENTIAL STAFF NOTE",
    parentReplyFa:"پیام صریح تأییدشده و مخصوص والد اینجا نشان داده می‌شود.",
    sourceFa:"NICE NG143",
    reviewedByUid:"staff_private_uid",
    ownerUid:"parent_uid",
    pharmacyId:"pharmacy_demo",
    updatedAt:{toMillis:()=>456},
    audit:{sensitive:"data"}
  };
  const view = buildParentCaseView("case_test",record);
  assert.equal(view.parentReplyFa,record.parentReplyFa);
  assert.equal(view.updatedAtMs,456);
  assert.equal(view.reviewNoteFa,undefined);
  assert.equal(Object.hasOwn(view,"reviewNoteFa"),false);
  assert.equal(Object.hasOwn(view,"reviewedByUid"),false);
  assert.equal(Object.hasOwn(view,"ownerUid"),false);
  assert.equal(Object.hasOwn(view,"audit"),false);
  const awaiting = buildParentCaseView("case_test",{...record,status:"RECEIVED"});
  assert.equal(awaiting.parentReplyFa,null);
  assert.equal(awaiting.sourceFa,null);
  assert.equal(awaiting.decision,null);
});


test("Only a trusted, current, single-use nonurgent triage pass can authorize intake",()=>{
  const now = Date.now();
  const verified = {
    ownerUid:"parent_123",
    pharmacyId:"pharmacy_demo",
    classification:"NONURGENT",
    issuerService:"mano-triage-v1",
    verified:true,
    validUntil:{toMillis:()=>now+60000},
    consumedAt:null,
  };
  assert.equal(isValidTriagePass(verified,"parent_123","pharmacy_demo",now),true);
  assert.equal(isValidTriagePass({...verified,ownerUid:"parent_456"},"parent_123","pharmacy_demo",now),false);
  assert.equal(isValidTriagePass({...verified,pharmacyId:"pharmacy_other"},"parent_123","pharmacy_demo",now),false);
  assert.equal(isValidTriagePass({...verified,classification:"URGENT"},"parent_123","pharmacy_demo",now),false);
  assert.equal(isValidTriagePass({...verified,issuerService:"client"},"parent_123","pharmacy_demo",now),false);
  assert.equal(isValidTriagePass({...verified,verified:false},"parent_123","pharmacy_demo",now),false);
  assert.equal(isValidTriagePass({...verified,consumedAt:{toMillis:()=>now}},"parent_123","pharmacy_demo",now),false);
  assert.equal(isValidTriagePass({...verified,validUntil:{toMillis:()=>now-1}},"parent_123","pharmacy_demo",now),false);
  assert.equal(isValidTriagePass({...verified,validUntil:{toMillis:()=>now}},"parent_123","pharmacy_demo",now),false);
  assert.equal(isValidTriagePass(null,"parent_123","pharmacy_demo",now),false);
});
