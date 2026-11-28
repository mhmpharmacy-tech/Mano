import type { Auth, User } from 'firebase/auth';
import type { FirebaseApp } from 'firebase/app';
import type { AppCheck } from 'firebase/app-check';
import type { OnlineConfiguration } from './config.ts';
import {
  guardedCallable, OnlineBoundaryError, type CallableName, type CallableTransport,
  type OnlineSession, type OnlineUser,
} from './contracts.ts';
import { dispatchFirebaseCallable } from './callableProtocol.ts';

interface FirebaseRuntime { app: FirebaseApp; auth: Auth; appCheck: AppCheck }
let runtimePromise: Promise<FirebaseRuntime> | null = null;

export class OnlineController {
  readonly enabled: boolean;
  private snapshot: OnlineSession = { generation: 0, user: null, connected: false, pending: false };
  private readonly listeners = new Set<() => void>();
  private observerSequence = 0;
  private runtime: FirebaseRuntime | null = null;
  private connection: Promise<FirebaseRuntime> | null = null;

  constructor(private readonly configuration: OnlineConfiguration) { this.enabled = configuration.enabled; }

  getSession = (): OnlineSession => this.snapshot;
  subscribe = (listener: () => void) => { this.listeners.add(listener); return () => { this.listeners.delete(listener); }; };

  private publish(next: OnlineSession) {
    this.snapshot = next;
    for (const listener of this.listeners) listener();
  }

  private async updateUser(user: User | null) {
    const sequence = ++this.observerSequence;
    // Invalidate old account results before any asynchronous token inspection.
    const changedUid = this.snapshot.user?.uid !== user?.uid;
    if (changedUid) this.publish({ ...this.snapshot, user: null, generation: this.snapshot.generation + 1, pending: Boolean(user), connected: true });
    let nextUser: OnlineUser | null = null;
    if (user) {
      try {
        const result = await user.getIdTokenResult();
        const claims = result.claims;
        nextUser = {
          uid: user.uid, email: user.email, emailVerified: user.emailVerified,
          staffRole: claims.manoStaffRole === 'pharmacist' || claims.manoStaffRole === 'admin' ? claims.manoStaffRole : null,
          staffVerified: claims.manoStaffVerified === true,
          pharmacyId: typeof claims.manoPharmacyId === 'string' && /^[\w-]{1,128}$/.test(claims.manoPharmacyId) ? claims.manoPharmacyId : null,
        };
      } catch { nextUser = null; }
    }
    if (sequence !== this.observerSequence || this.runtime?.auth.currentUser?.uid !== user?.uid) return;
    const previous = this.snapshot.user;
    const changedPrivileges = previous && nextUser && (previous.emailVerified !== nextUser.emailVerified || previous.staffRole !== nextUser.staffRole || previous.staffVerified !== nextUser.staffVerified || previous.pharmacyId !== nextUser.pharmacyId);
    this.publish({ generation: this.snapshot.generation + (changedPrivileges ? 1 : 0), user: nextUser, connected: true, pending: false });
  }

  /** SDK and reCAPTCHA initialization requires an explicit account action. */
  private async connect(): Promise<FirebaseRuntime> {
    if (!this.configuration.enabled) throw new OnlineBoundaryError('OFFLINE_UNAVAILABLE');
    if (this.connection) return this.connection;
    const config = this.configuration.firebase;
    this.publish({ ...this.snapshot, pending: true });
    if (!runtimePromise) {
      runtimePromise = (async () => {
        const [appModule, authModule, checkModule] = await Promise.all([
          import('firebase/app'), import('firebase/auth'), import('firebase/app-check'),
        ]);
        const app = appModule.initializeApp({ apiKey: config.apiKey, authDomain: config.authDomain, projectId: config.projectId, appId: config.appId }, 'mano-online');
        const auth = authModule.initializeAuth(app, { persistence: authModule.inMemoryPersistence });
        const appCheck = checkModule.initializeAppCheck(app, { provider: new checkModule.ReCaptchaV3Provider(config.appCheckSiteKey), isTokenAutoRefreshEnabled: false });
        return { app, auth, appCheck };
      })().catch(error => { runtimePromise = null; throw error; });
    }
    this.connection = runtimePromise.then(async runtime => {
      this.runtime = runtime;
      const { onIdTokenChanged } = await import('firebase/auth');
      onIdTokenChanged(runtime.auth, user => { void this.updateUser(user); });
      await runtime.auth.authStateReady();
      await this.updateUser(runtime.auth.currentUser);
      return runtime;
    }).catch(error => { this.connection = null; this.publish({ ...this.snapshot, pending: false }); throw error; });
    return this.connection;
  }

  readonly transport: CallableTransport = {
    call: async (name, data, identity) => {
      if (!this.configuration.enabled) throw new OnlineBoundaryError('OFFLINE_UNAVAILABLE');
      return dispatchFirebaseCallable(this, this.configuration.firebase, name, data, identity, async () => {
        const runtime = await this.connect();
        const capturedUser = runtime.auth.currentUser;
        const { getToken } = await import('firebase/app-check');
        const [idToken, appCheck] = await Promise.all([
          capturedUser ? capturedUser.getIdToken() : null,
          getToken(runtime.appCheck, false),
        ]);
        return { idToken, appCheckToken: appCheck.token, getAuthUid: () => runtime.auth.currentUser?.uid ?? null };
      });
    },
  };

  call<T>(name: CallableName, data: unknown, options: { clinical?: boolean; anonymousReceipt?: boolean; expectedGeneration?: number } = {}): Promise<T> {
    return guardedCallable<T>(this, this.transport, name, data, options);
  }

  async signIn(email: string, password: string, create = false): Promise<void> {
    const runtime = await this.connect();
    const auth = await import('firebase/auth');
    if (create) await auth.createUserWithEmailAndPassword(runtime.auth, email.trim(), password);
    else await auth.signInWithEmailAndPassword(runtime.auth, email.trim(), password);
  }

  async signOut(): Promise<void> {
    ++this.observerSequence;
    this.publish({ generation: this.snapshot.generation + 1, user: null, connected: this.snapshot.connected, pending: false });
    if (this.runtime) await (await import('firebase/auth')).signOut(this.runtime.auth);
  }

  async sendVerification(): Promise<void> {
    const user = this.runtime?.auth.currentUser;
    if (!user || user.uid !== this.snapshot.user?.uid) throw new OnlineBoundaryError('STALE_SESSION');
    await (await import('firebase/auth')).sendEmailVerification(user);
  }

  async refreshUser(): Promise<void> {
    const user = this.runtime?.auth.currentUser;
    if (!user || user.uid !== this.snapshot.user?.uid) throw new OnlineBoundaryError('STALE_SESSION');
    await user.reload();
    await user.getIdToken(true);
    await this.updateUser(user);
  }

  async resetPassword(email: string): Promise<void> {
    const runtime = await this.connect();
    await (await import('firebase/auth')).sendPasswordResetEmail(runtime.auth, email.trim());
  }

  async reauthenticate(password: string): Promise<void> {
    const user = this.runtime?.auth.currentUser;
    if (!user?.email || user.uid !== this.snapshot.user?.uid) throw new OnlineBoundaryError('STALE_SESSION');
    const { EmailAuthProvider, reauthenticateWithCredential } = await import('firebase/auth');
    await reauthenticateWithCredential(user, EmailAuthProvider.credential(user.email, password));
    if (this.runtime?.auth.currentUser?.uid !== user.uid) throw new OnlineBoundaryError('STALE_SESSION');
  }
}
