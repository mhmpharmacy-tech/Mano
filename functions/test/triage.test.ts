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

test("every red flag selectable in the current React client prevents a nonurgent pass", async () => {
  const { readFile } = await import("node:fs/promises");
  const seeds = await readFile("../src/data/symptomFlows.ts", "utf8");
  const blocks = [...seeds.matchAll(/redFlags:\s*\[([\s\S]*?)\]/g)];
  assert.ok(blocks.length >= 9, "Every shipped React symptom flow must be represented");
  const flags = blocks.flatMap(([, block]) => [...block.matchAll(/\{\s*id:\s*'([^']+)'[\s\S]*?isImmediateEmergency:\s*(true|false)/g)]);
  assert.ok(flags.length >= 20);
  for (const [, id, critical] of flags) {
    const decision = evaluateServerTriage({ ageBandId: "toddler_1_3y", selectedRedFlagIds: [id] });
    assert.equal(decision.outcome, critical === "true" ? "URGENT_EMERGENCY" : "NEEDS_TIMELY_REVIEW", id);
  }
});

test("unknown flags and invalid durations cannot authorize an online case", () => {
  for (const durationHours of [-1, 1.5, Infinity, NaN]) {
    assert.throws(() => evaluateServerTriage({ageBandId: "infant_3_12m", durationHours}));
  }
  assert.throws(() => evaluateServerTriage({ageBandId: "infant_3_12m", selectedRedFlagIds: ["unknown"]}));
});

test("persistent fever and young infant fever require medical assessment", () => {
  assert.equal(evaluateServerTriage({ageBandId: "infant_3_12m", symptomFlowId: "cough", temperatureCelsius: 39}).outcome, "NEEDS_TIMELY_REVIEW");
  assert.equal(evaluateServerTriage({ageBandId: "toddler_1_3y", symptomFlowId: "fever", durationHours: 120}).outcome, "NEEDS_TIMELY_REVIEW");
  assert.equal(evaluateServerTriage({ageBandId: "newborn", symptomFlowId: "cough", temperatureCelsius: 38, durationHours: -1}).outcome, "URGENT_EMERGENCY");
});

test("infant screening uses completed months and keeps the cautious default for older clients", () => {
  const infant = {ageBandId: "infant_3_12m", symptomFlowId: "cough", temperatureCelsius: 39};
  for (const ageMonthsExact of [undefined, null, 3, 6]) {
    assert.equal(evaluateServerTriage({...infant, ageMonthsExact}).outcome, "NEEDS_TIMELY_REVIEW");
  }
  for (const ageMonthsExact of [7, 11]) {
    assert.equal(evaluateServerTriage({...infant, ageMonthsExact}).outcome, "NONURGENT");
  }
  for (const ageMonthsExact of [2, 12, 3.5]) {
    assert.throws(() => evaluateServerTriage({...infant, ageMonthsExact}));
  }
  assert.throws(() => evaluateServerTriage({...infant, ageBandId: "newborn", temperatureCelsius: 37, ageMonthsExact: 3}));
});

test("eye, nasal and wet cough pathways preserve infant and duration referrals", () => {
  for (const symptomFlowId of ["eye", "runny_nose", "wet_cough", "ear"]) {
    assert.equal(evaluateServerTriage({ageBandId: "newborn", symptomFlowId}).outcome, "NEEDS_TIMELY_REVIEW");
    assert.equal(evaluateServerTriage({ageBandId: "toddler_1_3y", symptomFlowId}).outcome, "NONURGENT");
  }
  for (const [symptomFlowId, hours] of [["runny_nose", 240], ["wet_cough", 336], ["ear", 72]] as const) {
    const input = {ageBandId: "toddler_1_3y", symptomFlowId};
    assert.equal(evaluateServerTriage({...input, durationHours: hours - 1}).outcome, "NONURGENT");
    assert.equal(evaluateServerTriage({...input, durationHours: hours}).outcome, "NEEDS_TIMELY_REVIEW");
  }
});

test("ear symptoms in an infant require clinician review", () => {
  assert.equal(evaluateServerTriage({ageBandId: "infant_3_12m", symptomFlowId: "ear"}).outcome, "NEEDS_TIMELY_REVIEW");
});
