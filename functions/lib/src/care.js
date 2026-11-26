"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.updateCare = updateCare;
const auth_1 = require("./auth");
const config_1 = require("./config");
function field(value, limit) {
    if (typeof value !== "string" || !value.trim() || value.length > limit)
        throw new config_1.AppError("invalid-argument", "Missing or oversized field");
    return value.trim();
}
function optionalField(value, limit) {
    if (value === undefined || value === null)
        return undefined;
    if (typeof value !== "string" || value.length > limit)
        throw new config_1.AppError("invalid-argument", "Invalid or oversized optional field");
    const trimmed = value.trim();
    return trimmed.length > 0 ? trimmed : undefined;
}
function extractStructuredFields(data) {
    return {
        ...(optionalField(data.dosageForm, 80) ? { dosageForm: optionalField(data.dosageForm, 80) } : {}),
        ...(optionalField(data.route, 80) ? { route: optionalField(data.route, 80) } : {}),
        ...(optionalField(data.doseAmount, 120) ? { doseAmount: optionalField(data.doseAmount, 120) } : {}),
        ...(optionalField(data.frequency, 120) ? { frequency: optionalField(data.frequency, 120) } : {}),
        ...(optionalField(data.duration, 120) ? { duration: optionalField(data.duration, 120) } : {}),
        ...(optionalField(data.precautions, 1000) ? { precautions: optionalField(data.precautions, 1000) } : {}),
        ...(optionalField(data.pharmacyNameFa, 140) ? { pharmacyNameFa: optionalField(data.pharmacyNameFa, 140) } : {}),
    };
}
async function updateCare(store, caller, data) {
    const user = (0, auth_1.assertAuthenticated)(caller);
    const pharmacy = (0, auth_1.assertValidId)(data.pharmacyId, "pharmacyId");
    const caseId = (0, auth_1.assertValidId)(data.caseId, "caseId");
    const action = field(data.action, 20);
    const id = (0, auth_1.assertValidId)(data.requestId, "requestId");
    const staff = ["send", "revise", "reply", "close"].includes(action);
    if (["send", "revise", "dose", "close"].includes(action) && data.confirmed !== true)
        throw new config_1.AppError("failed-precondition", "Explicit action confirmation is required");
    if (!staff && !["open", "dose", "question", "checkin"].includes(action))
        throw new config_1.AppError("invalid-argument", "Unknown action");
    return store.runTransaction(async (tx) => {
        if (staff)
            await (0, auth_1.verifyPharmacistAccessInTransaction)(tx, user, pharmacy);
        const path = `pharmacies/${pharmacy}/cases/${caseId}`;
        const record = await tx.get(path);
        if (!record || record.pharmacyId !== pharmacy || (!staff && record.parentUid !== user.uid))
            throw new config_1.AppError("permission-denied", "Case unavailable");
        let card = record.careCard;
        const now = store.nowMs();
        const duplicate = card?.events.find(e => e.id === id && e.actor === user.uid);
        if (duplicate) {
            const textChanged = ["question", "reply", "checkin"].includes(action) &&
                duplicate.text !== field(data.text, 1000);
            const isCardMutation = action === "send" || action === "revise";
            const structured = isCardMutation ? extractStructuredFields(data) : {};
            // A retry of an issued medicine card must never claim success with revised
            // strength or instructions while silently retaining a different version.
            const issuedFieldsChanged = isCardMutation && card != null && (card.child !== field(data.child, 80) ||
                card.medicine !== field(data.medicine, 120) ||
                card.strength !== field(data.strength, 100) ||
                card.instructions !== field(data.instructions, 1500) ||
                card.followUp !== field(data.followUp, 120) ||
                (card.dosageForm ?? undefined) !== structured.dosageForm ||
                (card.route ?? undefined) !== structured.route ||
                (card.doseAmount ?? undefined) !== structured.doseAmount ||
                (card.frequency ?? undefined) !== structured.frequency ||
                (card.duration ?? undefined) !== structured.duration ||
                (card.precautions ?? undefined) !== structured.precautions ||
                (card.pharmacyNameFa ?? undefined) !== structured.pharmacyNameFa);
            if (duplicate.action !== action || textChanged || issuedFieldsChanged) {
                throw new config_1.AppError("failed-precondition", "Request ID was previously used for different content");
            }
            return { ok: true };
        }
        if (action === "send") {
            // Card issuance is a clinical communication. The same reviewed and attested
            // case gate as the signed pharmacist response is mandatory server-side.
            if (record.status !== "REVIEWED" || record.parentResponse?.attestationConfirmed !== true)
                throw new config_1.AppError("failed-precondition", "A pharmacist-reviewed case is required before sending a care card");
            if (card)
                throw new config_1.AppError("failed-precondition", "Cannot issue card");
            const structured = extractStructuredFields(data);
            const shareToken = `mc_${caseId.slice(-6)}_${now.toString(36)}`;
            card = {
                child: field(data.child, 80),
                medicine: field(data.medicine, 120),
                strength: field(data.strength, 100),
                instructions: field(data.instructions, 1500),
                followUp: field(data.followUp, 120),
                ...structured,
                version: 1,
                reviewDateMs: now,
                shareToken,
                openedAt: null,
                closedAt: null,
                events: [],
            };
        }
        if (action === "revise") {
            if (record.status !== "REVIEWED" || record.parentResponse?.attestationConfirmed !== true)
                throw new config_1.AppError("failed-precondition", "A pharmacist-reviewed case is required before revising a care card");
            if (!card)
                throw new config_1.AppError("failed-precondition", "No care card");
            if (card.closedAt)
                throw new config_1.AppError("failed-precondition", "Follow-up closed");
            const structured = extractStructuredFields(data);
            const nextVersion = (card.version ?? 1) + 1;
            card = {
                child: field(data.child, 80),
                medicine: field(data.medicine, 120),
                strength: field(data.strength, 100),
                instructions: field(data.instructions, 1500),
                followUp: field(data.followUp, 120),
                ...structured,
                version: nextVersion,
                reviewDateMs: now,
                shareToken: card.shareToken ?? `mc_${caseId.slice(-6)}_${now.toString(36)}`,
                openedAt: card.openedAt,
                closedAt: card.closedAt,
                events: card.events,
            };
        }
        if (!card)
            throw new config_1.AppError("failed-precondition", "No care card");
        if (action === "open" && card.openedAt)
            return { ok: true };
        if (card.closedAt && action !== "open")
            throw new config_1.AppError("failed-precondition", "Follow-up closed");
        if (card.events.length >= 200 && action !== "close" && action !== "open")
            throw new config_1.AppError("resource-exhausted", "Card history full");
        if (action === "open")
            card.openedAt = now;
        if (action === "close")
            card.closedAt = now;
        const text = ["question", "reply", "checkin"].includes(action)
            ? field(data.text, 1000)
            : action === "revise"
                ? (optionalField(data.text, 500) ?? `بازبینی نسخه ${card.version ?? 2} توسط داروساز`)
                : action === "dose"
                    ? "گزارش والد از مصرف؛ صرفاً زمان ثبت در سامانه ذخیره شد و مقدار/زمان واقعی مصرف تأیید نشده است."
                    : "";
        card.events.push({ id, action, text, at: now, actor: user.uid });
        tx.update(path, { careCard: card, updatedAt: now });
        return { ok: true };
    });
}
//# sourceMappingURL=care.js.map