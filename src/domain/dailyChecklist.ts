import { currentDayKey } from './treatmentPlan.ts';

/** Undated legacy checkmarks are retained in storage but never count as today. */
export function checkedChecklistItemsToday(savedIds: readonly string[], nowMs = Date.now()): string[] {
  const prefix = `${currentDayKey(nowMs)}|`;
  return [...new Set(savedIds.filter(id => id.startsWith(prefix)).map(id => id.slice(prefix.length)))];
}

export function toggleChecklistItemToday(savedIds: readonly string[], id: string, nowMs = Date.now()): string[] {
  const prefix = `${currentDayKey(nowMs)}|`;
  const scoped = `${prefix}${id}`;
  const today = savedIds.filter(value => value.startsWith(prefix));
  return today.includes(scoped) ? today.filter(value => value !== scoped) : [...today, scoped];
}
