import assert from 'node:assert/strict';
import test from 'node:test';
import {
  clearLocalSnapshot, LEGACY_EPOCH, readLocalSnapshot, replaceLocalSnapshot,
  STORAGE_EPOCH_KEY, STORAGE_KEY, STORAGE_RESTORE_KEY, type LocalStoragePort, type StorageLock,
} from './localPersistence.ts';
import { createInitialPersistedState, loadPersistedState, LOCAL_RECOVERY_ISSUE } from './persistedState.ts';

function setup(epoch = LEGACY_EPOCH) {
  const values = new Map<string, string>();
  if (epoch !== LEGACY_EPOCH) values.set(STORAGE_EPOCH_KEY, epoch);
  const storage: LocalStoragePort = {
    getItem: key => values.get(key) ?? null,
    setItem: (key, value) => { values.set(key, value); },
    removeItem: key => { values.delete(key); },
  };
  const lock: StorageLock = async operation => operation();
  return { storage, lock };
}

test('empty or server-side state starts writable without clinical defaults', () => {
  const { storage } = setup('empty-generation');
  const loaded = loadPersistedState({ storage });
  assert.equal(loaded.issue, null);
  assert.equal(loaded.state.storageEpoch, 'empty-generation');
  assert.equal(loaded.state.childProfile.ageMonthsExact, null);
  assert.equal(loaded.state.childProfile.weightKgText, '');
  assert.deepEqual(loaded.state.treatmentPlan.medications, []);
  assert.equal(loadPersistedState({ storage: null }).issue, null);
});

test('partial legacy snapshots merge missing optional state without overwriting source records', () => {
  const { storage } = setup();
  const raw = JSON.stringify({ childProfile: { aliasFa: 'KEEP_SYNTHETIC', allergiesFa: 'KEEP_ALLERGY' }, bookmarkedSlugs: ['synthetic-topic'] });
  storage.setItem(STORAGE_KEY, raw);
  const loaded = loadPersistedState({ storage, search: '?autostart=1' });
  assert.equal(loaded.issue, null);
  assert.equal(loaded.state.childProfile.aliasFa, 'KEEP_SYNTHETIC');
  assert.equal(loaded.state.childProfile.allergiesFa, 'KEEP_ALLERGY');
  assert.deepEqual(loaded.state.bookmarkedSlugs, ['synthetic-topic']);
  assert.equal(loaded.state.onboardingSeen, true);
  assert.equal(storage.getItem(STORAGE_KEY), raw);
});

test('malformed current-generation medication data stays preserved and produces a recovery lock', () => {
  for (const epoch of [LEGACY_EPOCH, 'restored-generation']) {
    const { storage } = setup(epoch);
    const original = createInitialPersistedState(epoch);
    const raw = JSON.stringify({
      ...original,
      childProfile: { ...original.childProfile, aliasFa: 'KEEP_SYNTHETIC' },
      feverLogs: [{ id: 'keep-log', tempCelsius: 37.2, timestampMs: Date.now() - 1000, timeLabelFa: 'KEEP_TIME', siteFa: 'KEEP_SITE', caregiverFa: '', noteFa: 'KEEP_NOTE' }],
      treatmentPlan: { ...original.treatmentPlan, medications: null },
    });
    storage.setItem(STORAGE_KEY, raw);
    const loaded = loadPersistedState({ storage });
    assert.equal(loaded.issue, LOCAL_RECOVERY_ISSUE);
    assert.equal(loaded.state.storageEpoch, epoch);
    assert.equal(loaded.state.onboardingSeen, true, 'Recovery controls must remain accessible without onboarding');
    assert.equal(storage.getItem(STORAGE_KEY), raw, 'Recoverable profile and log bytes must not be silently replaced by defaults');
  }
});

test('malformed JSON and invalid collection shapes all remain untouched for explicit recovery', () => {
  const examples = ['{"storageEpoch":', 'null', '"not-an-object"', ...[
    { feverLogs: null }, { growthMeasurements: {} }, { childProfile: 'invalid' },
    { savedVisitDrafts: [{ id: 'synthetic', draftTextFa: null }] },
    { vaccineRecords: { synthetic: null } }, { checkedHygieneIds: [null] },
  ].map(patch => JSON.stringify({ ...createInitialPersistedState(), ...patch }))];
  for (const raw of examples) {
    const { storage } = setup();
    storage.setItem(STORAGE_KEY, raw);
    const loaded = loadPersistedState({ storage });
    // JSON null is a genuinely absent snapshot, not a partial patient record.
    if (raw !== 'null') assert.equal(loaded.issue, LOCAL_RECOVERY_ISSUE, raw);
    assert.equal(storage.getItem(STORAGE_KEY), raw);
  }
});

test('structurally valid local records are not constrained by backup-only URL or export limits', () => {
  const { storage } = setup();
  const snapshot = createInitialPersistedState();
  snapshot.careCards = [{ id: 'local-card', titleFa: 'SYNTHETIC', subtitleFa: '', cardType: 'CAREGIVER_HANDOFF', badgeFa: '', summaryLinesFa: [], footerNoteFa: '', createdAtLabelFa: '', shareUrl: 'http://localhost:3000/care/' }];
  const raw = JSON.stringify({ ...snapshot, futureSchemaNote: 'KEEP_UNKNOWN_METADATA' });
  storage.setItem(STORAGE_KEY, raw);
  const loaded = loadPersistedState({ storage });
  assert.equal(loaded.issue, null);
  assert.equal(loaded.state.careCards[0].shareUrl, 'http://localhost:3000/care/');
  assert.equal(storage.getItem(STORAGE_KEY), raw);
});

test('deletion tombstones do not recover an older generation or trigger a false corruption lock', async () => {
  const { storage, lock } = setup();
  storage.setItem(STORAGE_KEY, JSON.stringify(createInitialPersistedState()));
  await clearLocalSnapshot(storage, lock, 'deleted-generation');
  const loaded = loadPersistedState({ storage });
  assert.equal(loaded.issue, null);
  assert.equal(loaded.state.storageEpoch, 'deleted-generation');
  assert.deepEqual(loaded.state.feverLogs, []);
});

test('explicit replacement recovers corrupt JSON only after a new generation is committed', async () => {
  const { storage, lock } = setup('broken-generation');
  const raw = '{"recoverable-original":';
  storage.setItem(STORAGE_KEY, raw);
  assert.equal(loadPersistedState({ storage }).issue, LOCAL_RECOVERY_ISSUE);
  const replacement = createInitialPersistedState('recovered-generation');
  replacement.childProfile.aliasFa = 'RESTORED_SYNTHETIC';
  await replaceLocalSnapshot(storage, lock, 'broken-generation', replacement);
  assert.equal(storage.getItem(STORAGE_KEY), raw, 'Prepared recovery does not destructively rewrite old bytes before promotion');
  const loaded = loadPersistedState({ storage });
  assert.equal(loaded.issue, null);
  assert.equal(loaded.state.childProfile.aliasFa, 'RESTORED_SYNTHETIC');
  assert.equal(loaded.state.storageEpoch, 'recovered-generation');
  assert.deepEqual(readLocalSnapshot(storage).snapshot, replacement);
});

test('failure to commit a corrupt-state replacement preserves the original corrupt bytes and lock', async () => {
  const { storage, lock } = setup('broken-generation');
  const raw = '{"KEEP_SYNTHETIC":';
  storage.setItem(STORAGE_KEY, raw);
  const blocked: LocalStoragePort = {
    ...storage,
    setItem(key, value) { if (key === STORAGE_EPOCH_KEY) throw new Error('storage unavailable'); storage.setItem(key, value); },
  };
  await assert.rejects(replaceLocalSnapshot(blocked, lock, 'broken-generation', createInitialPersistedState('new-generation')), /storage unavailable/);
  assert.equal(storage.getItem(STORAGE_KEY), raw);
  assert.equal(loadPersistedState({ storage }).issue, LOCAL_RECOVERY_ISSUE);
});

test('unavailable browser storage cannot create an apparently writable empty state', () => {
  const { storage } = setup();
  const blocked = { ...storage, getItem() { throw new Error('blocked'); } };
  const loaded = loadPersistedState({ storage: blocked, fallbackEpoch: 'known-generation' });
  assert.equal(loaded.issue, LOCAL_RECOVERY_ISSUE);
  assert.equal(loaded.state.storageEpoch, 'known-generation');
});

test('a corrupted committed restore payload remains locked and byte-preserved until explicit recovery', () => {
  const { storage } = setup('committed');
  const original = JSON.stringify(createInitialPersistedState(LEGACY_EPOCH));
  const staged = '{"RESTORED_SYNTHETIC":';
  storage.setItem(STORAGE_KEY, original);
  storage.setItem(STORAGE_RESTORE_KEY, staged);
  const loaded = loadPersistedState({ storage });
  assert.equal(loaded.issue, LOCAL_RECOVERY_ISSUE);
  assert.equal(storage.getItem(STORAGE_KEY), original);
  assert.equal(storage.getItem(STORAGE_RESTORE_KEY), staged);
});
