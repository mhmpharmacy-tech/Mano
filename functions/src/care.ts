import { assertAuthenticated, assertValidId, verifyPharmacistAccessInTransaction } from "./auth";
import { AppError } from "./config";
import { BackendStore, CallerContext, ConsultationCaseDoc } from "./types";

export type CareCardEvent = { id: string; action: string; text: string; at: number; actor: string };
export interface CareCard {
  medicine: string; strength: string; instructions: string; child: string; followUp: string;
  openedAt: number | null; closedAt: number | null; events: CareCardEvent[];
}
/** Parent-visible event stream never exposes Firebase staff or caregiver UIDs. */
export type PublicCareCard = Omit<CareCard, "events"> & {
  events: Array<Omit<CareCardEvent, "actor">>;
};

function field(value: unknown, limit: number): string {
  if (typeof value !== "string" || !value.trim() || value.length > limit)
    throw new AppError("invalid-argument", "Missing or oversized field");
  return value.trim();
}
export async function updateCare(store: BackendStore, caller: CallerContext | null, data: Record<string,unknown>) {
  const user = assertAuthenticated(caller);
  const pharmacy = assertValidId(data.pharmacyId, "pharmacyId");
  const caseId = assertValidId(data.caseId, "caseId");
  const action = field(data.action, 20);
  const id = assertValidId(data.requestId, "requestId");
  const staff = ["send","reply","close"].includes(action);
  if (["send","dose"].includes(action) && data.confirmed !== true)
    throw new AppError("failed-precondition", "Explicit action confirmation is required");
  if (!staff && !["open","dose","question","checkin"].includes(action))
    throw new AppError("invalid-argument", "Unknown action");
  return store.runTransaction(async tx => {
    if (staff) await verifyPharmacistAccessInTransaction(tx,user,pharmacy);
    const path = `pharmacies/${pharmacy}/cases/${caseId}`;
    const record = await tx.get<ConsultationCaseDoc & {careCard?:CareCard}>(path);
    if (!record || record.pharmacyId !== pharmacy || (!staff && record.parentUid !== user.uid))
      throw new AppError("permission-denied","Case unavailable");
    let card = record.careCard;
    const now = store.nowMs();
    const duplicate = card?.events.find(e => e.id === id && e.actor === user.uid);
    if (duplicate) {
      const textChanged = ["question","reply","checkin"].includes(action) &&
        duplicate.text !== field(data.text,1000);
      // A retry of an issued medicine card must never claim success with revised
      // strength or instructions while silently retaining the first version.
      const issuedFieldsChanged = action === "send" && card != null && (
        card.child !== field(data.child,80) ||
        card.medicine !== field(data.medicine,120) ||
        card.strength !== field(data.strength,100) ||
        card.instructions !== field(data.instructions,1500) ||
        card.followUp !== field(data.followUp,120)
      );
      if (duplicate.action !== action || textChanged || issuedFieldsChanged) {
        throw new AppError("failed-precondition","Request ID was previously used for different content");
      }
      return {ok:true};
    }
    if (action === "send") {
      // Card issuance is a clinical communication. The same reviewed and attested
      // case gate as the signed pharmacist response is mandatory server-side.
      if (record.status !== "REVIEWED" || record.parentResponse?.attestationConfirmed !== true)
        throw new AppError("failed-precondition","A pharmacist-reviewed case is required before sending a care card");
      if (card) throw new AppError("failed-precondition","Cannot issue card");
      card = {child:field(data.child,80), medicine:field(data.medicine,120), strength:field(data.strength,100),
        instructions:field(data.instructions,1500), followUp:field(data.followUp,120),
        openedAt:null, closedAt:null, events:[]};
    }
    if (!card) throw new AppError("failed-precondition","No care card");
    if (action === "open" && card.openedAt) return {ok:true};
    if (card.closedAt && action !== "open") throw new AppError("failed-precondition","Follow-up closed");
    if (card.events.length >= 200 && action !== "close" && action !== "open") throw new AppError("resource-exhausted","Card history full");
    if (action === "open") card.openedAt = now;
    if (action === "close") card.closedAt = now;
    const text = ["question","reply","checkin"].includes(action)
      ? field(data.text,1000)
      : action === "dose"
        ? "گزارش والد از مصرف؛ صرفاً زمان ثبت در سامانه ذخیره شد و مقدار/زمان واقعی مصرف تأیید نشده است."
        : "";
    card.events.push({id, action, text, at:now, actor:user.uid});
    tx.update(path,{careCard:card,updatedAt:now});
    return {ok:true};
  });
}
