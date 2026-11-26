"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const strict_1 = __importDefault(require("node:assert/strict"));
const node_test_1 = __importDefault(require("node:test"));
const node_fs_1 = require("node:fs");
const node_path_1 = require("node:path");
(0, node_test_1.default)("Compiled callable entrypoint retains mandatory App Check and all seven clinical gates", () => {
    const entry = (0, node_fs_1.readFileSync)((0, node_path_1.join)(__dirname, "../src/index.js"), "utf8");
    strict_1.default.match(entry, /const ENFORCE_APP_CHECK = true;/);
    strict_1.default.match(entry, /process\.env\.MANO_CLINICAL_SERVICE_ENABLED !== "true"/);
    const endpoints = ["listAcceptingPharmacies", "issueConsultationTriagePass", "submitConsultationCase", "getParentConsultation", "listParentConsultations", "getPharmacyInbox", "reviewConsultationCase"];
    for (const name of endpoints) {
        const start = entry.indexOf("exports." + name + " = (0,");
        strict_1.default.ok(start >= 0, name);
        const body = entry.slice(start, entry.indexOf("catch (err)", start));
        strict_1.default.match(body, /requireClinicalRelease\(\);\s*const \{ caseService \} = createServices\(\)/, name);
    }
});
//# sourceMappingURL=clinical-release.test.js.map