import assert from 'node:assert/strict';
import test from 'node:test';
import { checkedChecklistItemsToday, toggleChecklistItemToday } from './dailyChecklist.ts';

const beforeMidnight = new Date(2026, 10, 27, 23, 59).getTime();
const afterMidnight = new Date(2026, 10, 28, 0, 1).getTime();

test('daily checkmarks expire at device midnight, matching the local care-plan calendar', () => {
  const values = toggleChecklistItemToday([], 'synthetic-routine', beforeMidnight);
  assert.deepEqual(checkedChecklistItemsToday(values, beforeMidnight), ['synthetic-routine']);
  assert.deepEqual(checkedChecklistItemsToday(values, afterMidnight), []);
});

test('undated legacy checks cannot fabricate completion today and are not mutated when read', () => {
  const legacy = ['synthetic-routine'];
  assert.deepEqual(checkedChecklistItemsToday(legacy, beforeMidnight), []);
  assert.deepEqual(legacy, ['synthetic-routine']);
});

test('toggling preserves other current checks while starting the next day empty', () => {
  let values = toggleChecklistItemToday([], 'one', beforeMidnight);
  values = toggleChecklistItemToday(values, 'two', beforeMidnight);
  values = toggleChecklistItemToday(values, 'one', beforeMidnight);
  assert.deepEqual(checkedChecklistItemsToday(values, beforeMidnight), ['two']);
  values = toggleChecklistItemToday(values, 'one', afterMidnight);
  assert.deepEqual(checkedChecklistItemsToday(values, afterMidnight), ['one']);
});
