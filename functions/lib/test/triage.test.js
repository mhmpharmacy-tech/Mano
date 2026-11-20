"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
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
(0, node_test_1.default)("every red flag selectable on Android prevents a nonurgent pass", async () => {
    const { readFile } = await Promise.resolve().then(() => __importStar(require("node:fs/promises")));
    const seeds = await readFile("../app/src/main/java/com/example/data/ManoSourcesAndClinicalSeed.kt", "utf8");
    const flags = [...seeds.matchAll(/RedFlagOption\("([^"]+)",\s*"[^"]+",\s*(true|false)\)/g)];
    strict_1.default.ok(flags.length >= 20);
    for (const [, id, critical] of flags) {
        const decision = (0, triage_1.evaluateServerTriage)({ ageBandId: "toddler_1_3y", selectedRedFlagIds: [id] });
        strict_1.default.equal(decision.outcome, critical === "true" ? "URGENT_EMERGENCY" : "NEEDS_TIMELY_REVIEW", id);
    }
});
(0, node_test_1.default)("unknown flags and invalid durations cannot authorize an online case", () => {
    for (const durationHours of [-1, 1.5, Infinity, NaN]) {
        strict_1.default.throws(() => (0, triage_1.evaluateServerTriage)({ ageBandId: "infant_3_12m", durationHours }));
    }
    strict_1.default.throws(() => (0, triage_1.evaluateServerTriage)({ ageBandId: "infant_3_12m", selectedRedFlagIds: ["unknown"] }));
});
(0, node_test_1.default)("persistent fever and young infant fever require medical assessment", () => {
    strict_1.default.equal((0, triage_1.evaluateServerTriage)({ ageBandId: "infant_3_12m", symptomFlowId: "cough", temperatureCelsius: 39 }).outcome, "NEEDS_TIMELY_REVIEW");
    strict_1.default.equal((0, triage_1.evaluateServerTriage)({ ageBandId: "toddler_1_3y", symptomFlowId: "fever", durationHours: 120 }).outcome, "NEEDS_TIMELY_REVIEW");
    strict_1.default.equal((0, triage_1.evaluateServerTriage)({ ageBandId: "newborn", symptomFlowId: "cough", temperatureCelsius: 38, durationHours: -1 }).outcome, "URGENT_EMERGENCY");
});
(0, node_test_1.default)("infant screening uses completed months and keeps the cautious default for older clients", () => {
    const infant = { ageBandId: "infant_3_12m", symptomFlowId: "cough", temperatureCelsius: 39 };
    for (const ageMonthsExact of [undefined, null, 3, 6]) {
        strict_1.default.equal((0, triage_1.evaluateServerTriage)({ ...infant, ageMonthsExact }).outcome, "NEEDS_TIMELY_REVIEW");
    }
    for (const ageMonthsExact of [7, 11]) {
        strict_1.default.equal((0, triage_1.evaluateServerTriage)({ ...infant, ageMonthsExact }).outcome, "NONURGENT");
    }
    for (const ageMonthsExact of [2, 12, 3.5]) {
        strict_1.default.throws(() => (0, triage_1.evaluateServerTriage)({ ...infant, ageMonthsExact }));
    }
    strict_1.default.throws(() => (0, triage_1.evaluateServerTriage)({ ...infant, ageBandId: "newborn", temperatureCelsius: 37, ageMonthsExact: 3 }));
});
//# sourceMappingURL=triage.test.js.map