"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const strict_1 = __importDefault(require("node:assert/strict"));
const node_fs_1 = require("node:fs");
const node_path_1 = require("node:path");
const node_test_1 = __importDefault(require("node:test"));
const auth_1 = require("../src/auth");
const config_1 = require("../src/config");
(0, node_test_1.default)("parent clinical access requires the exact signed verified-email claim, not a client string or staff shortcut", () => {
    strict_1.default.throws(() => (0, auth_1.assertVerifiedCaregiver)(null), (err) => err instanceof config_1.AppError && err.code === "unauthenticated");
    for (const token of [{}, { email_verified: false }, { email_verified: "true" }, { manoStaffVerified: true }]) {
        strict_1.default.throws(() => (0, auth_1.assertVerifiedCaregiver)({ uid: "synthetic-parent", token }), (err) => err instanceof config_1.AppError && err.code === "permission-denied");
    }
    const verified = { uid: "synthetic-parent", token: { email_verified: true } };
    strict_1.default.equal((0, auth_1.assertVerifiedCaregiver)(verified), verified);
});
(0, node_test_1.default)("all deployed parent entrypoints apply the server email guard after the independent clinical release gate", () => {
    const entry = (0, node_fs_1.readFileSync)((0, node_path_1.join)(__dirname, "../src/index.js"), "utf8");
    for (const name of ["listAcceptingPharmacies", "issueConsultationTriagePass", "submitConsultationCase", "getParentConsultation", "listParentConsultations"]) {
        const start = entry.indexOf("exports." + name + " = (0,");
        strict_1.default.ok(start >= 0, name);
        const body = entry.slice(start, entry.indexOf("catch (err)", start));
        strict_1.default.match(body, /requireClinicalRelease\(\)/, name);
        strict_1.default.match(body, /assertVerifiedCaregiver\)\(extractCaller\(request\)\)/, name);
    }
});
(0, node_test_1.default)("deletion and private receipt entrypoints retain their own policy and do not require clinical release or email verification", () => {
    const entry = (0, node_fs_1.readFileSync)((0, node_path_1.join)(__dirname, "../src/index.js"), "utf8");
    for (const name of ["getAccountDeletionPolicy", "requestAccountDeletion", "getAccountDeletionStatus"]) {
        const start = entry.indexOf("exports." + name + " = (0,");
        strict_1.default.ok(start >= 0, name);
        const body = entry.slice(start, entry.indexOf("catch (err)", start));
        strict_1.default.doesNotMatch(body, /requireClinicalRelease|assertVerifiedCaregiver/, name);
        strict_1.default.match(body, /CALLABLE_OPTIONS/, name);
    }
});
//# sourceMappingURL=auth.boundaries.test.js.map