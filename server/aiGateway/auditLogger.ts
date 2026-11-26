import { createHash, randomUUID } from 'node:crypto';
import type {
  AiGatewayOutcome,
  AiProhibitedAction,
  AiResponsibility,
  AiVerificationState,
  GatewayAuditLogEntry,
} from '../../src/domain/aiGatewayContracts.ts';

export class AuditTrailRecorder {
  private readonly entries: GatewayAuditLogEntry[] = [];
  private readonly maxEntries: number;
  private readonly emitConsole: boolean;

  constructor(options?: { maxEntries?: number; emitConsole?: boolean }) {
    this.maxEntries = Math.max(20, options?.maxEntries ?? 300);
    this.emitConsole = options?.emitConsole ?? false;
  }

  static hashClientKey(rawClientKey: string): string {
    return createHash('sha256')
      .update(rawClientKey || 'anonymous')
      .digest('hex')
      .slice(0, 12);
  }

  static newAuditId(): string {
    return `ai_${randomUUID().slice(0, 12)}`;
  }

  record(input: {
    auditId?: string;
    endpoint: string;
    responsibility: AiResponsibility;
    model: string;
    outcome: AiGatewayOutcome;
    verificationState: AiVerificationState;
    fallbackUsed: boolean;
    attempts: number;
    latencyMs: number;
    clientKey: string;
    policyViolations?: AiProhibitedAction[];
    validationNotes?: string[];
  }): GatewayAuditLogEntry {
    const entry: GatewayAuditLogEntry = {
      auditId: input.auditId ?? AuditTrailRecorder.newAuditId(),
      timestampIso: new Date().toISOString(),
      endpoint: input.endpoint,
      responsibility: input.responsibility,
      model: input.model,
      outcome: input.outcome,
      verificationState: input.verificationState,
      fallbackUsed: input.fallbackUsed,
      attempts: input.attempts,
      latencyMs: Math.max(0, Math.round(input.latencyMs)),
      clientKeyHash: AuditTrailRecorder.hashClientKey(input.clientKey),
      policyViolations: [...new Set(input.policyViolations ?? [])],
      validationNotes: (input.validationNotes ?? []).slice(0, 8).map((n) => n.slice(0, 160)),
    };

    this.entries.push(entry);
    if (this.entries.length > this.maxEntries) {
      this.entries.shift();
    }

    if (this.emitConsole) {
      console.info('[AI_GATEWAY_AUDIT]', JSON.stringify(entry));
    }

    return entry;
  }

  listRecent(limit = 50): GatewayAuditLogEntry[] {
    const safeLimit = Math.min(this.maxEntries, Math.max(1, limit));
    return this.entries.slice(-safeLimit).reverse();
  }
}
