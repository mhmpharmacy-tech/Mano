import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";
import { assertVerifiedCaregiver } from "../src/auth";
import { AppError } from "../src/config";
import { CallerContext } from "../src/types";

test("parent clinical access requires the exact signed verified-email claim, not a client string or staff shortcut", () => {
  assert.throws(() => assertVerifiedCaregiver(null), (err: unknown) => err instanceof AppError && err.code === "unauthenticated");
  for (const token of [{}, { email_verified: false }, { email_verified: "true" }, { manoStaffVerified: true }]) {
    assert.throws(() => assertVerifiedCaregiver({ uid: "synthetic-parent", token }),
      (err: unknown) => err instanceof AppError && err.code === "permission-denied");
  }
  const verified: CallerContext = { uid: "synthetic-parent", token: { email_verified: true } };
  assert.equal(assertVerifiedCaregiver(verified), verified);
});

test("all deployed parent entrypoints apply the server email guard after the independent clinical release gate", () => {
  const entry = readFileSync(join(__dirname, "../src/index.js"), "utf8");
  for (const name of ["listAcceptingPharmacies", "issueConsultationTriagePass", "submitConsultationCase", "getParentConsultation", "listParentConsultations"]) {
    const start = entry.indexOf("exports." + name + " = (0,");
    assert.ok(start >= 0, name);
    const body = entry.slice(start, entry.indexOf("catch (err)", start));
    assert.match(body, /requireClinicalRelease\(\)/, name);
    assert.match(body, /assertVerifiedCaregiver\)\(extractCaller\(request\)\)/, name);
  }
});

test("deletion and private receipt entrypoints retain their own policy and do not require clinical release or email verification", () => {
  const entry = readFileSync(join(__dirname, "../src/index.js"), "utf8");
  for (const name of ["getAccountDeletionPolicy", "requestAccountDeletion", "getAccountDeletionStatus"]) {
    const start = entry.indexOf("exports." + name + " = (0,");
    assert.ok(start >= 0, name);
    const body = entry.slice(start, entry.indexOf("catch (err)", start));
    assert.doesNotMatch(body, /requireClinicalRelease|assertVerifiedCaregiver/, name);
    assert.match(body, /CALLABLE_OPTIONS/, name);
  }
});
