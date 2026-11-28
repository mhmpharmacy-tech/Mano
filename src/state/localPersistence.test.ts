import test from 'node:test';
import assert from 'node:assert/strict';
import {
  clearLocalSnapshot, currentStorageEpoch, LEGACY_EPOCH, persistLocalSnapshot,
  readLocalSnapshot, replaceLocalSnapshot, STORAGE_EPOCH_KEY, STORAGE_KEY, STORAGE_RESTORE_KEY, type LocalStoragePort, type StorageLock,
} from './localPersistence.ts';

function setup() {
  const data = new Map<string, string>();
  const storage: LocalStoragePort = {
    getItem: (key) => data.get(key) ?? null,
    setItem: (key, value) => { data.set(key, value); },
    removeItem: (key) => { data.delete(key); },
  };
  let queue = Promise.resolve();
  const lock: StorageLock = <T>(operation: () => T) => {
    const result = queue.then(operation);
    queue = result.then(() => undefined, () => undefined);
    return result;
  };
  return { storage, lock };
}

test('a stale tab cannot restore deleted clinical data, even if its write was already queued', async () => {
  const { storage, lock } = setup();
  const old = { storageEpoch: LEGACY_EPOCH, alias: 'SYNTHETIC_CHILD', allergy: 'SYNTHETIC_ALLERGY' };
  await persistLocalSnapshot(storage, lock, old);
  // A last write before deletion is removed; a queued write after it is refused.
  const before = persistLocalSnapshot(storage, lock, { ...old, theme: 'dark' });
  const deletion = clearLocalSnapshot(storage, lock, 'deleted-generation');
  const after = persistLocalSnapshot(storage, lock, { ...old, theme: 'light' });
  await before;
  await deletion;
  assert.deepEqual(await after, { saved: false, epoch: 'deleted-generation' });
  assert.equal(storage.getItem(STORAGE_KEY), null);
  assert.equal(currentStorageEpoch(storage), 'deleted-generation');
});

test('new drafts may persist after deletion while old tab snapshots remain rejected', async () => {
  const { storage, lock } = setup();
  await clearLocalSnapshot(storage, lock, 'new-generation');
  const fresh = { storageEpoch: 'new-generation', alias: 'NEW_SYNTHETIC_CHILD' };
  assert.equal((await persistLocalSnapshot(storage, lock, fresh)).saved, true);
  assert.equal((await persistLocalSnapshot(storage, lock, { storageEpoch: LEGACY_EPOCH, alias: 'OLD_CHILD' })).saved, false);
  assert.deepEqual(readLocalSnapshot(storage).snapshot, fresh);
});

test('legacy snapshots migrate only before a deletion tombstone exists', () => {
  const { storage } = setup();
  storage.setItem(STORAGE_KEY, JSON.stringify({ alias: 'LEGACY_SYNTHETIC_CHILD' }));
  assert.equal(readLocalSnapshot(storage).snapshot !== null, true);
  storage.setItem(STORAGE_EPOCH_KEY, 'deleted');
  assert.deepEqual(readLocalSnapshot(storage), { epoch: 'deleted', snapshot: null });
});

test('deletion failures are surfaced instead of falsely reporting success', async () => {
  const { storage, lock } = setup();
  const blocked = { ...storage, setItem() { throw new Error('storage unavailable'); } };
  await assert.rejects(clearLocalSnapshot(blocked, lock, 'next'), /storage unavailable/);
});

test('restore commits a recoverable snapshot and rejects every older tab write', async () => {
  const { storage, lock } = setup();
  const old = { storageEpoch: LEGACY_EPOCH, records: ['OLD_SYNTHETIC'] };
  const replacement = { storageEpoch: 'restored', records: ['RESTORED_SYNTHETIC'] };
  await persistLocalSnapshot(storage, lock, old);
  await replaceLocalSnapshot(storage, lock, LEGACY_EPOCH, replacement);
  assert.deepEqual(readLocalSnapshot(storage).snapshot, replacement, 'Committed staged data survives a restart before promotion');
  assert.deepEqual(await persistLocalSnapshot(storage, lock, old), { saved: false, epoch: 'restored' });
  await persistLocalSnapshot(storage, lock, replacement);
  assert.equal(storage.getItem(STORAGE_RESTORE_KEY), null);
  assert.deepEqual(readLocalSnapshot(storage).snapshot, replacement);
});

test('restore refuses a review from before deletion without touching the new generation', async () => {
  const { storage, lock } = setup();
  await clearLocalSnapshot(storage, lock, 'deleted');
  await assert.rejects(replaceLocalSnapshot(storage, lock, LEGACY_EPOCH, { storageEpoch: 'stale-restore', records: ['SYNTHETIC'] }), /LOCAL_DATA_CHANGED/);
  assert.deepEqual(readLocalSnapshot(storage), { epoch: 'deleted', snapshot: null });
});

test('failed restore preparation or pointer commit preserves the previous records', async () => {
  for (const failedKey of [STORAGE_RESTORE_KEY, STORAGE_EPOCH_KEY]) {
    const { storage, lock } = setup();
    const old = { storageEpoch: LEGACY_EPOCH, records: ['KEEP_SYNTHETIC'] };
    await persistLocalSnapshot(storage, lock, old);
    const blocked = { ...storage, setItem(key: string, value: string) { if (key === failedKey) throw new Error('storage full'); storage.setItem(key, value); } };
    await assert.rejects(replaceLocalSnapshot(blocked, lock, LEGACY_EPOCH, { storageEpoch: 'new', records: ['NEW_SYNTHETIC'] }), /storage full/);
    assert.deepEqual(readLocalSnapshot(storage).snapshot, old);
  }
});

test('a second failed restore cannot destroy the first unpromoted restore', async () => {
  const { storage, lock } = setup();
  const first = { storageEpoch: 'first', records: ['FIRST_SYNTHETIC'] };
  await replaceLocalSnapshot(storage, lock, LEGACY_EPOCH, first);
  const blocked = { ...storage, setItem(key: string, value: string) { if (key === STORAGE_EPOCH_KEY) throw new Error('blocked'); storage.setItem(key, value); } };
  await assert.rejects(replaceLocalSnapshot(blocked, lock, 'first', { storageEpoch: 'second', records: ['SECOND_SYNTHETIC'] }), /blocked/);
  assert.deepEqual(readLocalSnapshot(storage).snapshot, first);
});

test('deletion also erases prepared backup payloads and blocks queued restores', async () => {
  const { storage, lock } = setup();
  await replaceLocalSnapshot(storage, lock, LEGACY_EPOCH, { storageEpoch: 'restored', sensitive: 'SYNTHETIC' });
  const deletion = clearLocalSnapshot(storage, lock, 'deleted');
  const stale = replaceLocalSnapshot(storage, lock, 'restored', { storageEpoch: 'stale', sensitive: 'SYNTHETIC' });
  await deletion;
  await assert.rejects(stale, /LOCAL_DATA_CHANGED/);
  assert.equal(storage.getItem(STORAGE_RESTORE_KEY), null);
  assert.deepEqual(readLocalSnapshot(storage), { epoch: 'deleted', snapshot: null });
});

test('uncommitted corrupt staging does not hide readable current-generation records', () => {
  const { storage } = setup();
  const current = { storageEpoch: LEGACY_EPOCH, records: ['KEEP_SYNTHETIC'] };
  storage.setItem(STORAGE_KEY, JSON.stringify(current));
  storage.setItem(STORAGE_RESTORE_KEY, '{broken-staging');
  assert.deepEqual(readLocalSnapshot(storage).snapshot, current);
});

test('unreadable committed staged restore is not mistaken for an empty store', async () => {
  const { storage, lock } = setup();
  storage.setItem(STORAGE_KEY, JSON.stringify({ storageEpoch: LEGACY_EPOCH, records: ['ORIGINAL_SYNTHETIC'] }));
  storage.setItem(STORAGE_RESTORE_KEY, '{broken-committed-staging');
  storage.setItem(STORAGE_EPOCH_KEY, 'committed');
  assert.throws(() => readLocalSnapshot(storage), /INVALID_PREPARED_SNAPSHOT/);
  const replacement = { storageEpoch: 'recovered', records: ['RECOVERED_SYNTHETIC'] };
  await replaceLocalSnapshot(storage, lock, 'committed', replacement);
  assert.deepEqual(readLocalSnapshot(storage).snapshot, replacement);
});
