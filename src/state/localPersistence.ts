export const STORAGE_KEY = 'mano_kid_v2_store';
export const STORAGE_EPOCH_KEY = 'mano_kid_v2_store_epoch';
export const STORAGE_LOCK_NAME = 'mano-kid-local-data';
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
  const raw = storage.getItem(STORAGE_KEY);
  const snapshot = raw ? JSON.parse(raw) as T : null;
  const currentEpoch = currentStorageEpoch(storage);
  if (epoch !== currentEpoch || !snapshot || (snapshot.storageEpoch ?? LEGACY_EPOCH) !== epoch) {
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
    return { saved: true, epoch };
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
    return nextEpoch;
  });
}
