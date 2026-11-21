import { assertAuthenticated, assertValidId, verifyPharmacistAccessInTransaction } from "./auth";
import { AppError } from "./config";
import { BackendStore, CallerContext, ConsultationCaseDoc } from "./types";

type Event = { id: string; action: string; text: string; at: number; actor: string };
export interface CareCard {
  medicine: string; strength: string; instructions: string; child: string; followUp: string;
  openedAt: number | null; closedAt: number | null; events: Event[];
}
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
  if (!staff && !["open","dose","question","checkin"].includes(action))
    throw new AppError("invalid-argument", "Unknown action");
  return store.runTransaction(async tx => {
    if (staff) await verifyPharmacistAccessInTransaction(tx,user,pharmacy);
    const path = `pharmacies/${pharmacy}/cases/${caseId}`;
    const record = await tx.get<ConsultationCaseDoc & {careCard?:CareCard}>(path);
    if (!record || (!staff && record.parentUid !== user.uid))
      throw new AppError("permission-denied","Case unavailable");
    let card = record.careCard;
    const now = store.nowMs();
    if (card?.events.some(e => e.id === id && e.actor === user.uid)) return {ok:true};
    if (action === "send") {
      if (card || data.confirmed !== true) throw new AppError("failed-precondition","Cannot issue card");
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
    const text = ["question","reply","checkin"].includes(action) ? field(data.text,1000) : "";
    card.events.push({id, action, text, at:now, actor:user.uid});
    tx.update(path,{careCard:card,updatedAt:now});
    return {ok:true};
  });
}
