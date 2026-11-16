"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const { isId, isVerifiedPharmacist, validateIntake, validateStaffReview } = require("../consultation-policy");

const auth = { uid: "staff-001", token: { manoStaffVerified:true, manoStaffRole:"pharmacist", manoPharmacyId:"pharmacy_demo" } };
const goodCase = { pharmacyId:"pharmacy_demo",categoryName:"OTC",ageBandId:"toddler_1_3y",questionSummaryFa:"آیا این دو شربت تداخل دارویی دارند؟",consentAccepted:true,privacyNoticeVersion:"v1",hasUrgentFlag:false };

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
});
test("Staff must receive before marking a case reviewed",()=>{
  assert.deepEqual(validateStaffReview({status:"RECEIVED"},"PENDING"),{status:"RECEIVED",decision:null,reviewNoteFa:null,sourceFa:null});
  assert.equal(validateStaffReview({status:"REVIEWED",decision:"REFER",reviewNoteFa:"نیازمند ارزیابی حضوری",sourceFa:"NICE NG143"},"PENDING"),null);
  assert.equal(validateStaffReview({status:"PENDING"},"RECEIVED"),null);
  assert.equal(validateStaffReview({status:"REVIEWED",decision:"REFER",reviewNoteFa:"توضیح کوتاه",sourceFa:"NICE NG143"},"RECEIVED"),null);
  const reviewed=validateStaffReview({status:"REVIEWED",decision:"REFER",reviewNoteFa:"نیازمند ارزیابی حضوری توسط پزشک معالج است.",sourceFa:"NICE NG143"},"RECEIVED");
  assert.equal(reviewed.status,"REVIEWED");
  assert.equal(reviewed.decision,"REFER");
  assert.equal(validateStaffReview({status:"RECEIVED"},"REVIEWED"),null);
});
test("Forged reviewer notes and invalid workflow states cannot pass",()=>{
  assert.equal(validateStaffReview({status:"REVIEWED",decision:"OTHER",reviewNoteFa:"بیش از بیست حرف آزمایشی برای تست",sourceFa:"NICE NG143"},"RECEIVED"),null);
  assert.equal(validateStaffReview({status:"REVIEWED",decision:"EDUCATION",reviewNoteFa:"بیش از بیست حرف آزمایشی برای تست",sourceFa:""},"RECEIVED"),null);
});
