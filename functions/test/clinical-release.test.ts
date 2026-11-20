import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";
import { join } from "node:path";

test("Compiled callable entrypoint retains mandatory App Check and all seven clinical gates", () => {
  const entry = readFileSync(join(__dirname, "../src/index.js"), "utf8");
  assert.match(entry, /const ENFORCE_APP_CHECK = true;/);
  assert.match(entry, /process\.env\.MANO_CLINICAL_SERVICE_ENABLED !== "true"/);
  const endpoints = ["listAcceptingPharmacies", "issueConsultationTriagePass", "submitConsultationCase", "getParentConsultation", "listParentConsultations", "getPharmacyInbox", "reviewConsultationCase"];
  for (const name of endpoints) {
    const start = entry.indexOf("exports." + name + " = (0,");
    assert.ok(start >= 0, name);
    const body = entry.slice(start, entry.indexOf("catch (err)", start));
    assert.match(body, /requireClinicalRelease\(\);\s*const \{ caseService \} = createServices\(\)/, name);
  }
});
