import type { OnlineFirebaseConfig } from './config.ts';
import { assertDispatchIdentity, OnlineBoundaryError, type CallableName, type DispatchIdentity, type SessionPort } from './contracts.ts';

const MAX_RESPONSE_BYTES = 1024 * 1024;
const CALLABLE_NAMES = new Set<CallableName>([
  'listAcceptingPharmacies', 'issueConsultationTriagePass', 'submitConsultationCase',
  'listParentConsultations', 'getPharmacyInbox', 'reviewConsultationCase', 'updateCareCard',
  'getAccountDeletionPolicy', 'requestAccountDeletion', 'getAccountDeletionStatus',
]);
const ERROR_STATUS = new Set(['INVALID_ARGUMENT', 'FAILED_PRECONDITION', 'OUT_OF_RANGE', 'UNAUTHENTICATED', 'PERMISSION_DENIED', 'NOT_FOUND', 'ALREADY_EXISTS', 'ABORTED', 'RESOURCE_EXHAUSTED', 'CANCELLED', 'DATA_LOSS', 'UNKNOWN', 'INTERNAL', 'UNIMPLEMENTED', 'UNAVAILABLE', 'DEADLINE_EXCEEDED']);

interface CallableCredentials {
  idToken: string | null;
  appCheckToken: string;
  getAuthUid(): string | null;
}

async function boundedResponse(response: Response): Promise<Record<string, unknown>> {
  if (Number(response.headers.get('content-length')) > MAX_RESPONSE_BYTES) throw new OnlineBoundaryError('INVALID_RESPONSE');
  if (!response.body) throw new OnlineBoundaryError('INVALID_RESPONSE');
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let size = 0;
  let text = '';
  try {
    for (;;) {
      const next = await reader.read();
      if (next.done) break;
      size += next.value.byteLength;
      if (size > MAX_RESPONSE_BYTES) {
        await reader.cancel();
        throw new OnlineBoundaryError('INVALID_RESPONSE');
      }
      text += decoder.decode(next.value, { stream: true });
    }
    text += decoder.decode();
  } finally { reader.releaseLock(); }
  let body: unknown;
  try { body = JSON.parse(text); } catch { throw new OnlineBoundaryError('INVALID_RESPONSE'); }
  if (!body || typeof body !== 'object' || Array.isArray(body)) throw new OnlineBoundaryError('INVALID_RESPONSE');
  return body as Record<string, unknown>;
}

/**
 * Firebase callable HTTP protocol with credentials bound to one captured user.
 * The Auth SDK is never allowed to select a newly switched account's token for
 * an older clinical payload. Tokens and request bodies are not logged or saved.
 */
export async function dispatchFirebaseCallable(
  port: SessionPort, config: OnlineFirebaseConfig, name: CallableName, data: unknown,
  identity: DispatchIdentity, prepareCredentials: () => Promise<CallableCredentials>,
  fetcher: typeof fetch = fetch,
): Promise<unknown> {
  if (!port.enabled) throw new OnlineBoundaryError('OFFLINE_UNAVAILABLE');
  if (!CALLABLE_NAMES.has(name)) throw new OnlineBoundaryError('INVALID_INPUT');
  const credentials = await prepareCredentials();
  assertDispatchIdentity(port, identity, credentials.getAuthUid());
  if (!credentials.appCheckToken || (identity.uid !== null && !credentials.idToken) || (identity.uid === null && credentials.idToken !== null)) {
    throw new OnlineBoundaryError('STALE_SESSION');
  }
  // Serialization happens only after the session is rechecked following SDK,
  // Auth token and App Check work; no retry can submit a clinical question twice.
  const body = JSON.stringify({ data });
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 25_000);
  try {
    assertDispatchIdentity(port, identity, credentials.getAuthUid());
    const response = await fetcher(`https://${config.region}-${config.projectId}.cloudfunctions.net/${name}`, {
      method: 'POST', credentials: 'omit', cache: 'no-store', redirect: 'error',
      signal: controller.signal,
      headers: {
        'Content-Type': 'application/json', 'X-Firebase-AppCheck': credentials.appCheckToken,
        ...(credentials.idToken ? { Authorization: `Bearer ${credentials.idToken}` } : {}),
      },
      body,
    });
    const result = await boundedResponse(response);
    if (result.error && typeof result.error === 'object' && !Array.isArray(result.error)) {
      const status = (result.error as Record<string, unknown>).status;
      const code = typeof status === 'string' && ERROR_STATUS.has(status) ? status.toLowerCase().replace(/_/g, '-') : 'internal';
      throw new OnlineBoundaryError(`functions/${code}`);
    }
    if (!response.ok) throw new OnlineBoundaryError('functions/unavailable');
    if (Object.hasOwn(result, 'result')) return result.result;
    if (Object.hasOwn(result, 'data')) return result.data;
    throw new OnlineBoundaryError('INVALID_RESPONSE');
  } finally { clearTimeout(timeout); }
}
