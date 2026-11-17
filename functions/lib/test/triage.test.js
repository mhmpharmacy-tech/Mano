"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const strict_1 = __importDefault(require("node:assert/strict"));
const node_test_1 = __importDefault(require("node:test"));
const triage_1 = require("../src/triage");
(0, node_test_1.default)("normalizePersian unifies Arabic forms, digits, and ZWNJ spacing", () => {
    strict_1.default.equal((0, triage_1.normalizePersian)("شیر‌خشک كودك ي ٢٣"), "شیر خشک کودک ی 23");
    strict_1.default.equal((0, triage_1.normalizePersian)("تب ۳۸.۵ درجه"), "تب 38.5 درجه");
});
(0, node_test_1.default)("evaluateServerTriage blocks newborn fever >= 38.0C with URGENT_EMERGENCY", () => {
    const result = (0, triage_1.evaluateServerTriage)({
        ageBandId: "newborn",
        symptomFlowId: "fever",
        temperatureCelsius: 38.0,
        questionSummaryFa: "تب ۳۸ درجه در نوزاد دو ماهه از دیشب",
        hasUrgentFlag: false,
    });
    strict_1.default.equal(result.outcome, "URGENT_EMERGENCY");
    strict_1.default.equal(result.ruleId, "rule_infant_under_3m_fever_38");
});
(0, node_test_1.default)("evaluateServerTriage blocks critical red flags and high-risk Persian text", () => {
    const flagBlocked = (0, triage_1.evaluateServerTriage)({
        ageBandId: "toddler_1_3y",
        symptomFlowId: "cough",
        selectedRedFlagIds: ["breathing_difficulty"],
        questionSummaryFa: "سرفه خفیف کودک سه ساله",
    });
    strict_1.default.equal(flagBlocked.outcome, "URGENT_EMERGENCY");
    strict_1.default.equal(flagBlocked.ruleId, "rule_critical_red_flag_bypass");
    const textBlocked = (0, triage_1.evaluateServerTriage)({
        ageBandId: "toddler_1_3y",
        symptomFlowId: "general",
        questionSummaryFa: "کودکم دچار تشنج و کبودی لب شد",
    });
    strict_1.default.equal(textBlocked.outcome, "URGENT_EMERGENCY");
    strict_1.default.equal(textBlocked.ruleId, "rule_high_risk_text_signal");
});
(0, node_test_1.default)("evaluateServerTriage allows calm non-urgent pediatric OTC/constipation question", () => {
    const result = (0, triage_1.evaluateServerTriage)({
        ageBandId: "toddler_1_3y",
        symptomFlowId: "constipation",
        temperatureCelsius: 36.8,
        questionSummaryFa: "کودک سه ساله دچار یبوست خفیف شده؛ سوال درباره مایعات و پودر پیدرولاکس",
        hasUrgentFlag: false,
    });
    strict_1.default.equal(result.outcome, "NONURGENT");
    strict_1.default.equal(result.ruleId, "rule_nonurgent_pass_eligible");
});
//# sourceMappingURL=triage.test.js.map