"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const strict_1 = __importDefault(require("node:assert/strict"));
const node_test_1 = __importDefault(require("node:test"));
const firestore_1 = require("firebase-admin/firestore");
const node_crypto_1 = require("node:crypto");
const config_1 = require("../src/config");
const firebaseAccountDeletion_1 = require("../src/firebaseAccountDeletion");
const accountDeletion_1 = require("../src/accountDeletion");
(0, node_test_1.default)("paged deletion finds descendants whose parent documents do not exist and never crosses the owned subtree", async () => {
    const oldProject = process.env.GCLOUD_PROJECT;
    const oldEmulator = process.env.FIRESTORE_EMULATOR_HOST;
    const originalFetch = globalThis.fetch;
    process.env.GCLOUD_PROJECT = "demo-delete-test";
    process.env.FIRESTORE_EMULATOR_HOST = "127.0.0.1:8080";
    const documents = new Set(["users/a", "users/b/profiles/primary"]);
    for (let i = 0; i < 121; i++)
        documents.add(`users/a/profiles/missing/items/item_${i}`);
    const deletedPages = [];
    const requests = [];
    const db = {
        projectId: "demo-delete-test", databaseId: config_1.FIRESTORE_DATABASE_ID,
        doc: (path) => ({ path, get: async () => ({ exists: documents.has(path) }) }),
        batch: () => {
            const pending = [];
            return {
                delete: (ref) => pending.push(ref.path),
                commit: async () => { deletedPages.push(pending.length); for (const path of pending)
                    documents.delete(path); },
            };
        },
    };
    const resourcePrefix = `projects/demo-delete-test/databases/${config_1.FIRESTORE_DATABASE_ID}/documents/`;
    const respond = (obj) => new Response(JSON.stringify(obj), { status: 200 });
    globalThis.fetch = async (input, init) => {
        const url = new URL(String(input));
        requests.push(url.pathname);
        strict_1.default.equal(url.origin, "http://127.0.0.1:8080");
        strict_1.default.equal(new Headers(init?.headers).has("Authorization"), false, "Emulator tests must not request production credentials");
        const path = decodeURIComponent(url.pathname).slice(`/v1/${resourcePrefix}`.length);
        if (path.endsWith(":listCollectionIds")) {
            const parent = path.slice(0, -":listCollectionIds".length);
            const ids = [...new Set([...documents].filter(p => p.startsWith(`${parent}/`)).map(p => p.slice(parent.length + 1).split("/")[0]))].sort();
            return respond({ collectionIds: ids });
        }
        strict_1.default.equal(url.searchParams.get("showMissing"), "true");
        strict_1.default.ok(Number(url.searchParams.get("pageSize")) <= 50);
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
            const page = await new firebaseAccountDeletion_1.FirestoreTreeEraser(db).erasePage("users/a", 50);
            strict_1.default.ok(page.deleted <= 50);
            done = page.done;
            total += page.deleted;
        }
        strict_1.default.equal(done, true);
        strict_1.default.equal(total, 122);
        strict_1.default.deepEqual([...documents], ["users/b/profiles/primary"]);
        strict_1.default.ok(deletedPages.every(size => size <= 50));
        strict_1.default.ok(requests.some(p => p.includes("/profiles/missing:listCollectionIds")), "Missing parents must not hide nested records");
    }
    finally {
        globalThis.fetch = originalFetch;
        if (oldProject === undefined)
            delete process.env.GCLOUD_PROJECT;
        else
            process.env.GCLOUD_PROJECT = oldProject;
        if (oldEmulator === undefined)
            delete process.env.FIRESTORE_EMULATOR_HOST;
        else
            process.env.FIRESTORE_EMULATOR_HOST = oldEmulator;
    }
});
(0, node_test_1.default)("unexpected inventory paths fail closed before deletion", async () => {
    const oldProject = process.env.GCLOUD_PROJECT;
    const oldEmulator = process.env.FIRESTORE_EMULATOR_HOST;
    const originalFetch = globalThis.fetch;
    process.env.GCLOUD_PROJECT = "demo-delete-test";
    process.env.FIRESTORE_EMULATOR_HOST = "127.0.0.1:8080";
    let deletes = 0;
    const db = { projectId: "demo-delete-test", databaseId: config_1.FIRESTORE_DATABASE_ID, doc: () => ({ get: async () => ({ exists: false }) }), batch: () => ({ delete: () => { deletes++; }, commit: async () => { } }) };
    globalThis.fetch = async (input) => new Response(JSON.stringify(String(input).includes(":listCollectionIds")
        ? { collectionIds: ["profiles"] }
        : { documents: [{ name: `projects/demo-delete-test/databases/${config_1.FIRESTORE_DATABASE_ID}/documents/users/b/profiles/primary` }] }), { status: 200 });
    try {
        await strict_1.default.rejects(new firebaseAccountDeletion_1.FirestoreTreeEraser(db).erasePage("users/a", 50), (err) => err instanceof config_1.AppError && err.code === "failed-precondition");
        strict_1.default.equal(deletes, 0);
    }
    finally {
        globalThis.fetch = originalFetch;
        if (oldProject === undefined)
            delete process.env.GCLOUD_PROJECT;
        else
            process.env.GCLOUD_PROJECT = oldProject;
        if (oldEmulator === undefined)
            delete process.env.FIRESTORE_EMULATOR_HOST;
        else
            process.env.FIRESTORE_EMULATOR_HOST = oldEmulator;
    }
});
(0, node_test_1.default)("inventory refuses a different SDK project or database before any network or deletion", async () => {
    const oldProject = process.env.GCLOUD_PROJECT;
    const originalFetch = globalThis.fetch;
    process.env.GCLOUD_PROJECT = "demo-delete-test";
    let requests = 0;
    globalThis.fetch = async () => { requests++; throw new Error("Network must not be reached"); };
    try {
        for (const identity of [
            { projectId: "other-project", databaseId: config_1.FIRESTORE_DATABASE_ID },
            { projectId: "demo-delete-test", databaseId: "other-database" },
            { databaseId: config_1.FIRESTORE_DATABASE_ID },
        ]) {
            strict_1.default.throws(() => new firebaseAccountDeletion_1.FirestoreTreeEraser(identity), (err) => err instanceof config_1.AppError && err.code === "failed-precondition");
        }
        strict_1.default.equal(requests, 0);
    }
    finally {
        globalThis.fetch = originalFetch;
        if (oldProject === undefined)
            delete process.env.GCLOUD_PROJECT;
        else
            process.env.GCLOUD_PROJECT = oldProject;
    }
});
(0, node_test_1.default)("initialized installed SDK identity is usable without contacting a database; arbitrary roots/page sizes are denied", async () => {
    const oldProject = process.env.GCLOUD_PROJECT;
    const originalFetch = globalThis.fetch;
    process.env.GCLOUD_PROJECT = "demo-delete-test";
    let requests = 0;
    globalThis.fetch = async () => { requests++; throw new Error("Network must not be reached"); };
    const db = new firestore_1.Firestore({ projectId: "demo-delete-test", databaseId: config_1.FIRESTORE_DATABASE_ID });
    try {
        const eraser = new firebaseAccountDeletion_1.FirestoreTreeEraser(db);
        for (const [path, size] of [["pharmacies", 50], ["users", 50], ["users/a", 0], ["users/a", 51], ["users/a", 1.5]]) {
            await strict_1.default.rejects(eraser.erasePage(path, size), (err) => err instanceof config_1.AppError && err.code === "failed-precondition");
        }
        strict_1.default.equal(requests, 0);
    }
    finally {
        globalThis.fetch = originalFetch;
        await db.terminate();
        if (oldProject === undefined)
            delete process.env.GCLOUD_PROJECT;
        else
            process.env.GCLOUD_PROJECT = oldProject;
    }
});
(0, node_test_1.default)("membership appearing between Auth preflight and deletion transaction prevents a job and lock from being created", async () => {
    const writes = [];
    const query = { get: async () => ({ empty: true }) };
    const db = {
        collection: (collection) => ({ doc: (id) => ({ path: `${collection}/${id}` }) }),
        collectionGroup: (name) => ({ where: (field, op, uid) => {
                strict_1.default.equal(name, "staff");
                strict_1.default.equal(field, "uid");
                strict_1.default.equal(op, "==");
                strict_1.default.equal(uid, "parent_a");
                return { limit: () => query };
            } }),
        runTransaction: async (fn) => fn({
            get: async (ref) => ref === query ? { empty: false } : { exists: false },
            create: (ref) => { writes.push(ref.path); },
        }),
    };
    const auth = { getUser: async () => ({ customClaims: {} }) };
    const backend = new firebaseAccountDeletion_1.FirebaseAccountDeletionBackend(db, auth);
    const service = new accountDeletion_1.AccountDeletionService(backend, {
        version: "test-policy", clinicalRetentionDays: 10, backupRetentionDays: 40,
        storageMode: "none", storageBucket: null, storageWritersStopped: false,
    });
    await strict_1.default.rejects(service.request({ uid: "parent_a", token: { auth_time: Math.floor(Date.now() / 1000) } }, {
        confirmation: "DELETE_MY_ACCOUNT", policyVersion: "test-policy", receiptToken: (0, node_crypto_1.randomBytes)(32).toString("base64url"),
    }), (err) => err instanceof config_1.AppError && err.code === "failed-precondition");
    strict_1.default.deepEqual(writes, []);
});
//# sourceMappingURL=accountDeletion.inventory.test.js.map