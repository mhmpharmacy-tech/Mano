import assert from "node:assert/strict";
import test from "node:test";
import { evaluateServerTriage, normalizePersian } from "../src/triage";

test("normalizePersian unifies Arabic forms, digits, and ZWNJ spacing", () => {
  assert.equal(normalizePersian("شیر‌خشک كودك ي ٢٣"), "شیر خشک کودک ی 23");
  assert.equal(normalizePersian("تب ۳۸.۵ درجه"), "تب 38.5 درجه");
});

test("evaluateServerTriage blocks newborn fever >= 38.0C with URGENT_EMERGENCY", () => {
  const result = evaluateServerTriage({
    ageBandId: "newborn",
    symptomFlowId: "fever",
    temperatureCelsius: 38.0,
    questionSummaryFa: "تب ۳۸ درجه در نوزاد دو ماهه از دیشب",
    hasUrgentFlag: false,
  });

  assert.equal(result.outcome, "URGENT_EMERGENCY");
  assert.equal(result.ruleId, "rule_infant_under_3m_fever_38");
});

test("evaluateServerTriage blocks critical red flags and high-risk Persian text", () => {
  const flagBlocked = evaluateServerTriage({
    ageBandId: "toddler_1_3y",
    symptomFlowId: "cough",
    selectedRedFlagIds: ["breathing_difficulty"],
    questionSummaryFa: "سرفه خفیف کودک سه ساله",
  });
  assert.equal(flagBlocked.outcome, "URGENT_EMERGENCY");
  assert.equal(flagBlocked.ruleId, "rule_critical_red_flag_bypass");

  const textBlocked = evaluateServerTriage({
    ageBandId: "toddler_1_3y",
    symptomFlowId: "general",
    questionSummaryFa: "کودکم دچار تشنج و کبودی لب شد",
  });
  assert.equal(textBlocked.outcome, "URGENT_EMERGENCY");
  assert.equal(textBlocked.ruleId, "rule_high_risk_text_signal");
});

test("evaluateServerTriage allows calm non-urgent pediatric OTC/constipation question", () => {
  const result = evaluateServerTriage({
    ageBandId: "toddler_1_3y",
    symptomFlowId: "constipation",
    temperatureCelsius: 36.8,
    questionSummaryFa: "کودک سه ساله دچار یبوست خفیف شده؛ سوال درباره مایعات و پودر پیدرولاکس",
    hasUrgentFlag: false,
  });

  assert.equal(result.outcome, "NONURGENT");
  assert.equal(result.ruleId, "rule_nonurgent_pass_eligible");
});
