# Initial QA remediation — review only

Base: 7e37fef3a103edde5c160d1dd99bb53f64cd4a0f. Scope: AI Studio web preview (not Android or Firebase). No merge/deploy. No real child data may be entered.

- MK-001: profile is in-memory, no connectivity gate; adds live advisory without resetting form and strict empty-age validation. Reported production network failure not reproduced.
- MK-002: fixed synthetic fixture only: literal session marker, read-back check, explicit failure, clear/reload. Real patient storage deliberately disabled; cloud/offline durable storage remains open pending privacy/security architecture and integration tests. Offline full reload not supported.
- MK-003/004: current web dashboard has no 3D scene or sleep tool. No deletion necessary here. Actual device/version still needs verification.
- MK-005/006: smaller art, simpler decoration, unchanged mascot and tap targets. Visual acceptance pending.

Added browser scenarios for offline form, age zero, no user data persisted, synthetic fixture restore after reconnect, clear/reload. Tests added, not claimed run. All QA items remain open until human review and device evidence. Do not merge or deploy.
