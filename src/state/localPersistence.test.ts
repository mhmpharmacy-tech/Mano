import test from 'node:test';
import assert from 'node:assert/strict';
import {
  clearLocalSnapshot, currentStorageEpoch, LEGACY_EPOCH, persistLocalSnapshot,
  readLocalSnapshot, STORAGE_EPOCH_KEY, STORAGE_KEY, type LocalStoragePort, type StorageLock,
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
