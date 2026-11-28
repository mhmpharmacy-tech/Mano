import assert from "node:assert/strict";
import test from "node:test";
import { Firestore } from "firebase-admin/firestore";
import { Auth } from "firebase-admin/auth";
import { randomBytes } from "node:crypto";
import { AppError, FIRESTORE_DATABASE_ID } from "../src/config";
import { FirebaseAccountDeletionBackend, FirestoreTreeEraser } from "../src/firebaseAccountDeletion";
import { AccountDeletionService } from "../src/accountDeletion";

test("paged deletion finds descendants whose parent documents do not exist and never crosses the owned subtree", async () => {
  const oldProject = process.env.GCLOUD_PROJECT;
  const oldEmulator = process.env.FIRESTORE_EMULATOR_HOST;
  const originalFetch = globalThis.fetch;
  process.env.GCLOUD_PROJECT = "demo-delete-test";
  process.env.FIRESTORE_EMULATOR_HOST = "127.0.0.1:8080";
  const documents = new Set<string>(["users/a", "users/b/profiles/primary"]);
  for (let i = 0; i < 121; i++) documents.add(`users/a/profiles/missing/items/item_${i}`);
  const deletedPages: number[] = [];
  const requests: string[] = [];
  const db = {
    projectId: "demo-delete-test", databaseId: FIRESTORE_DATABASE_ID,
    doc: (path: string) => ({ path, get: async () => ({ exists: documents.has(path) }) }),
    batch: () => {
      const pending: string[] = [];
      return {
        delete: (ref: { path: string }) => pending.push(ref.path),
        commit: async () => { deletedPages.push(pending.length); for (const path of pending) documents.delete(path); },
      };
    },
  } as unknown as Firestore;
  const resourcePrefix = `projects/demo-delete-test/databases/${FIRESTORE_DATABASE_ID}/documents/`;
  const respond = (obj: object) => new Response(JSON.stringify(obj), { status: 200 });
  globalThis.fetch = async (input, init) => {
    const url = new URL(String(input));
    requests.push(url.pathname);
    assert.equal(url.origin, "http://127.0.0.1:8080");
    assert.equal(new Headers(init?.headers).has("Authorization"), false, "Emulator tests must not request production credentials");
    const path = decodeURIComponent(url.pathname).slice(`/v1/${resourcePrefix}`.length);
    if (path.endsWith(":listCollectionIds")) {
      const parent = path.slice(0, -":listCollectionIds".length);
      const ids = [...new Set([...documents].filter(p => p.startsWith(`${parent}/`)).map(p => p.slice(parent.length + 1).split("/")[0]))].sort();
      return respond({ collectionIds: ids });
    }
    assert.equal(url.searchParams.get("showMissing"), "true");
    assert.ok(Number(url.searchParams.get("pageSize")) <= 50);
    const children = [...new Set([...documents].filter(p => p.startsWith(`${path}/`)).map(p => `${path}/${p.slice(path.length + 1).split("/")[0]}`))].sort();
    const offset = Number(url.searchParams.get("pageToken") ?? 0);
    const size = Number(url.searchParams.get("pageSize"));
    return respond({
      documents: children.slice(offset, offset + size).map(p => ({ name: `${resourcePrefix}${p}` })),
      ...(children.length > offset + size ? { nextPageToken: String(offset + size) } : {}),
    });
  };
  try {
    let done = false;
    let total = 0;
    for (let i = 0; i < 10 && !done; i++) {
      const page = await new FirestoreTreeEraser(db).erasePage("users/a", 50);
      assert.ok(page.deleted <= 50);
      done = page.done; total += page.deleted;
    }
    assert.equal(done, true); assert.equal(total, 122);
    assert.deepEqual([...documents], ["users/b/profiles/primary"]);
    assert.ok(deletedPages.every(size => size <= 50));
    assert.ok(requests.some(p => p.includes("/profiles/missing:listCollectionIds")), "Missing parents must not hide nested records");
  } finally {
    globalThis.fetch = originalFetch;
    if (oldProject === undefined) delete process.env.GCLOUD_PROJECT; else process.env.GCLOUD_PROJECT = oldProject;
    if (oldEmulator === undefined) delete process.env.FIRESTORE_EMULATOR_HOST; else process.env.FIRESTORE_EMULATOR_HOST = oldEmulator;
  }
});

test("unexpected inventory paths fail closed before deletion", async () => {
  const oldProject = process.env.GCLOUD_PROJECT;
  const oldEmulator = process.env.FIRESTORE_EMULATOR_HOST;
  const originalFetch = globalThis.fetch;
  process.env.GCLOUD_PROJECT = "demo-delete-test";
  process.env.FIRESTORE_EMULATOR_HOST = "127.0.0.1:8080";
  let deletes = 0;
  const db = { projectId: "demo-delete-test", databaseId: FIRESTORE_DATABASE_ID, doc: () => ({ get: async () => ({ exists: false }) }), batch: () => ({ delete: () => { deletes++; }, commit: async () => {} }) } as unknown as Firestore;
  globalThis.fetch = async input => new Response(JSON.stringify(String(input).includes(":listCollectionIds")
    ? { collectionIds: ["profiles"] }
    : { documents: [{ name: `projects/demo-delete-test/databases/${FIRESTORE_DATABASE_ID}/documents/users/b/profiles/primary` }] }), { status: 200 });
  try {
    await assert.rejects(new FirestoreTreeEraser(db).erasePage("users/a", 50), (err: unknown) => err instanceof AppError && err.code === "failed-precondition");
    assert.equal(deletes, 0);
  } finally {
    globalThis.fetch = originalFetch;
    if (oldProject === undefined) delete process.env.GCLOUD_PROJECT; else process.env.GCLOUD_PROJECT = oldProject;
    if (oldEmulator === undefined) delete process.env.FIRESTORE_EMULATOR_HOST; else process.env.FIRESTORE_EMULATOR_HOST = oldEmulator;
  }
});

test("inventory refuses a different SDK project or database before any network or deletion", async () => {
  const oldProject = process.env.GCLOUD_PROJECT;
  const originalFetch = globalThis.fetch;
  process.env.GCLOUD_PROJECT = "demo-delete-test";
  let requests = 0;
  globalThis.fetch = async () => { requests++; throw new Error("Network must not be reached"); };
  try {
    for (const identity of [
      { projectId: "other-project", databaseId: FIRESTORE_DATABASE_ID },
      { projectId: "demo-delete-test", databaseId: "other-database" },
      { databaseId: FIRESTORE_DATABASE_ID },
    ]) {
      assert.throws(() => new FirestoreTreeEraser(identity as unknown as Firestore),
        (err: unknown) => err instanceof AppError && err.code === "failed-precondition");
    }
    assert.equal(requests, 0);
  } finally {
    globalThis.fetch = originalFetch;
    if (oldProject === undefined) delete process.env.GCLOUD_PROJECT; else process.env.GCLOUD_PROJECT = oldProject;
  }
});

test("initialized installed SDK identity is usable without contacting a database; arbitrary roots/page sizes are denied", async () => {
  const oldProject = process.env.GCLOUD_PROJECT;
  const originalFetch = globalThis.fetch;
  process.env.GCLOUD_PROJECT = "demo-delete-test";
  let requests = 0;
  globalThis.fetch = async () => { requests++; throw new Error("Network must not be reached"); };
  const db = new Firestore({ projectId: "demo-delete-test", databaseId: FIRESTORE_DATABASE_ID });
  try {
    const eraser = new FirestoreTreeEraser(db);
    for (const [path, size] of [["pharmacies", 50], ["users", 50], ["users/a", 0], ["users/a", 51], ["users/a", 1.5]] as const) {
      await assert.rejects(eraser.erasePage(path, size), (err: unknown) => err instanceof AppError && err.code === "failed-precondition");
    }
    assert.equal(requests, 0);
  } finally {
    globalThis.fetch = originalFetch;
    await db.terminate();
    if (oldProject === undefined) delete process.env.GCLOUD_PROJECT; else process.env.GCLOUD_PROJECT = oldProject;
  }
});

test("membership appearing between Auth preflight and deletion transaction prevents a job and lock from being created", async () => {
  const writes: string[] = [];
  const query = { get: async () => ({ empty: true }) };
  const db = {
    collection: (collection: string) => ({ doc: (id: string) => ({ path: `${collection}/${id}` }) }),
    collectionGroup: (name: string) => ({ where: (field: string, op: string, uid: string) => {
      assert.equal(name, "staff"); assert.equal(field, "uid"); assert.equal(op, "=="); assert.equal(uid, "parent_a");
      return { limit: () => query };
    } }),
    runTransaction: async <T>(fn: (tx: unknown) => Promise<T>) => fn({
      get: async (ref: unknown) => ref === query ? { empty: false } : { exists: false },
      create: (ref: { path: string }) => { writes.push(ref.path); },
    }),
  } as unknown as Firestore;
  const auth = { getUser: async () => ({ customClaims: {} }) } as unknown as Auth;
  const backend = new FirebaseAccountDeletionBackend(db, auth);
  const service = new AccountDeletionService(backend, {
    version: "test-policy", clinicalRetentionDays: 10, backupRetentionDays: 40,
    storageMode: "none", storageBucket: null, storageWritersStopped: false,
  });
  await assert.rejects(service.request({ uid: "parent_a", token: { auth_time: Math.floor(Date.now() / 1000) } }, {
    confirmation: "DELETE_MY_ACCOUNT", policyVersion: "test-policy", receiptToken: randomBytes(32).toString("base64url"),
  }), (err: unknown) => err instanceof AppError && err.code === "failed-precondition");
  assert.deepEqual(writes, []);
});
