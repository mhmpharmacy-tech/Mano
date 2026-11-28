// Experimental adapter, deliberately not imported into the application UI.
// Transport is supplied by an authenticated internal test harness; it owns any operator gate.
// Never add operator/stage credentials to a browser, VITE variable, APK or localStorage.
export type StageTransport = (request: {
  method: 'GET' | 'POST' | 'DELETE'; path: string; bearer: string; body?: object; signal: AbortSignal;
}) => Promise<{ status: number; data: unknown }>;
export type StageProfile = { id: string; alias: string; ageMonths: number };
const uuid = /^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/i;
const aliasPattern = /^demo-[a-z0-9-]{1,40}$/;
const record = (value: unknown): value is Record<string, unknown> =>
  !!value && typeof value === 'object' && !Array.isArray(value);
export class IndependentStageClient {
  private session: string | null = null;
  private generation = 0;
  private pending = new Set<AbortController>();
  constructor(private readonly enabled: boolean, private readonly transport: StageTransport) {}
  clearSession() {
    this.generation++;
    this.session = null;
    for (const c of this.pending) c.abort();
    this.pending.clear();
  }
  setSession(token: string) {
    this.clearSession();
    if (!this.enabled || !/^[A-Za-z0-9_-]{43}$/.test(token)) throw new Error('STAGE_DISABLED_OR_INVALID_SESSION');
    this.session = token;
  }
  private async request(method: 'GET' | 'POST' | 'DELETE', path: string, expected: number, body?: object) {
    if (!this.enabled || !this.session) throw new Error('STAGE_UNAVAILABLE');
    const generation = this.generation;
    const controller = new AbortController();
    this.pending.add(controller);
    const timeout = setTimeout(() => controller.abort(), 8000);
    try {
      const result = await this.transport({method,path,bearer:this.session,body,signal:controller.signal});
      if (generation !== this.generation || controller.signal.aborted) throw new Error('STALE_OR_ABORTED');
      if (result.status === 401) this.clearSession();
      if (result.status !== expected) throw new Error('STAGE_REQUEST_REJECTED');
      return result.data;
    } finally {
      clearTimeout(timeout);
      this.pending.delete(controller);
    }
  }
  async list(): Promise<StageProfile[]> {
    const data = await this.request('GET','/internal/mano/v1/profiles',200);
    if (!record(data) || !Array.isArray(data.profiles) || data.profiles.length > 100) throw new Error('INVALID_RESPONSE');
    return data.profiles.map((p: unknown) => {
      if (!record(p) || typeof p.id !== 'string' || !uuid.test(p.id) || typeof p.alias !== 'string' ||
          !aliasPattern.test(p.alias) || typeof p.age_months !== 'number' || !Number.isInteger(p.age_months) ||
          p.age_months < 0 || p.age_months > 215) throw new Error('INVALID_RESPONSE');
      return {id:p.id,alias:p.alias,ageMonths:p.age_months};
    });
  }
  async create(alias: string, ageMonths: number) {
    if (!aliasPattern.test(alias) || !Number.isInteger(ageMonths) || ageMonths < 0 || ageMonths > 215) throw new Error('SYNTHETIC_ONLY');
    const data = await this.request('POST','/internal/mano/v1/profiles',201,{alias,ageMonths});
    if (!record(data) || typeof data.id !== 'string' || !uuid.test(data.id)) throw new Error('INVALID_RESPONSE');
    return data.id;
  }
  async grant(id: string, pharmacyId: string, confirmed: boolean) {
    if (!uuid.test(id)||!uuid.test(pharmacyId)||confirmed!==true) throw new Error('CONSENT_REQUIRED');
    const data = await this.request('POST','/internal/mano/v1/profiles/'+id+'/shares',200,{pharmacyId,consent:'synthetic-v1'});
    if (!record(data)||data.ok!==true) throw new Error('INVALID_RESPONSE');
  }
  async revoke(id: string, pharmacyId: string) {
    if (!uuid.test(id)||!uuid.test(pharmacyId)) throw new Error('INVALID_ID');
    const data=await this.request('DELETE','/internal/mano/v1/profiles/'+id+'/shares/'+pharmacyId,200);
    if (!record(data)||data.ok!==true) throw new Error('INVALID_RESPONSE');
  }
  async logout() {
    const generation=this.generation;
    try {
      const data=await this.request('POST','/internal/mano/v1/session/logout',200);
      if(!record(data)||data.ok!==true)throw new Error('INVALID_RESPONSE');
    } finally {if(generation===this.generation)this.clearSession();}
  }
}
