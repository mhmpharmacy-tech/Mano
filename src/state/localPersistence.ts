export const STORAGE_KEY = 'mano_kid_v2_store';
export const STORAGE_EPOCH_KEY = 'mano_kid_v2_store_epoch';
export const STORAGE_LOCK_NAME = 'mano-kid-local-data';
export const STORAGE_RESTORE_KEY = 'mano_kid_v2_pending_restore';
export const LEGACY_EPOCH = 'legacy';

export type LocalStoragePort = Pick<Storage, 'getItem' | 'setItem' | 'removeItem'>;
export type StorageLock = <T>(operation: () => T) => Promise<T>;

export function currentStorageEpoch(storage: LocalStoragePort): string {
  return storage.getItem(STORAGE_EPOCH_KEY) || LEGACY_EPOCH;
}

export function readLocalSnapshot<T extends { storageEpoch?: string }>(storage: LocalStoragePort): {
  epoch: string;
  snapshot: T | null;
} {
  const epoch = currentStorageEpoch(storage);
  // A restore writes its payload before atomically moving the generation
  // pointer. If the browser stops before promotion, the committed payload
  // remains readable and old tabs still cannot overwrite it.
  const prepared = storage.getItem(STORAGE_RESTORE_KEY);
  let restored: T | null = null;
  let corruptPrepared = false;
  if (prepared) {
    try {
      const candidate = JSON.parse(prepared) as T;
      if (!candidate || typeof candidate !== 'object' || Array.isArray(candidate) || typeof candidate.storageEpoch !== 'string') {
        corruptPrepared = true;
      } else if (candidate.storageEpoch === epoch) restored = candidate;
    } catch { corruptPrepared = true; }
  }
  const raw = restored ? null : storage.getItem(STORAGE_KEY);
  const snapshot = restored ?? (raw ? JSON.parse(raw) as T : null);
  const currentEpoch = currentStorageEpoch(storage);
  if (epoch !== currentEpoch) return { epoch: currentEpoch, snapshot: null };
  if (!snapshot || (snapshot.storageEpoch ?? LEGACY_EPOCH) !== epoch) {
    // A failed, uncommitted preparation can be ignored when the canonical
    // current generation is readable. Without that fallback, preserve an
    // unreadable committed restore rather than treating it as an empty store.
    if (corruptPrepared) throw new SyntaxError('INVALID_PREPARED_SNAPSHOT');
    return { epoch: currentEpoch, snapshot: null };
  }
  return { epoch, snapshot };
}

export function persistLocalSnapshot<T extends { storageEpoch: string }>(
  storage: LocalStoragePort,
  lock: StorageLock,
  snapshot: T,
): Promise<{ saved: boolean; epoch: string }> {
  return lock(() => {
    const epoch = currentStorageEpoch(storage);
    if (snapshot.storageEpoch !== epoch) return { saved: false, epoch };
    const serialized = JSON.stringify(snapshot);
    if (storage.getItem(STORAGE_KEY) !== serialized) storage.setItem(STORAGE_KEY, serialized);
    storage.removeItem(STORAGE_RESTORE_KEY);
    return { saved: true, epoch };
  });
}

/** Replace records only if the generation reviewed by the caregiver is current. */
export function replaceLocalSnapshot<T extends { storageEpoch: string }>(
  storage: LocalStoragePort,
  lock: StorageLock,
  expectedEpoch: string,
  replacement: T,
): Promise<string> {
  return lock(() => {
    let current: { epoch: string; snapshot: T | null } = {
      epoch: currentStorageEpoch(storage), snapshot: null,
    };
    try {
      current = readLocalSnapshot<T>(storage);
    } catch (error) {
      // Explicit replacement is also the recovery path for corrupt JSON.
      // A storage access failure remains an error, and the old bytes are not
      // touched until a valid prepared replacement is committed below.
      if (!(error instanceof SyntaxError)) throw error;
    }
    if (current.epoch !== expectedEpoch || replacement.storageEpoch === expectedEpoch || !replacement.storageEpoch) {
      throw new Error('LOCAL_DATA_CHANGED');
    }
    // Preserve an earlier committed restore before reusing the staging slot.
    if (current.snapshot && storage.getItem(STORAGE_RESTORE_KEY)) {
      storage.setItem(STORAGE_KEY, JSON.stringify(current.snapshot));
    }
    storage.setItem(STORAGE_RESTORE_KEY, JSON.stringify(replacement));
    // This single atomic write commits the prepared snapshot. No fallible
    // cleanup follows it; ordinary persistence promotes it under the same lock.
    storage.setItem(STORAGE_EPOCH_KEY, replacement.storageEpoch);
    return replacement.storageEpoch;
  });
}

export function clearLocalSnapshot(
  storage: LocalStoragePort,
  lock: StorageLock,
  nextEpoch: string,
): Promise<string> {
  return lock(() => {
    // Keep a non-clinical tombstone so tabs holding an older snapshot cannot
    // resurrect it. The same cross-tab lock protects both deletion and writes.
    storage.setItem(STORAGE_EPOCH_KEY, nextEpoch);
    storage.removeItem(STORAGE_KEY);
    storage.removeItem(STORAGE_RESTORE_KEY);
    return nextEpoch;
  });
}
