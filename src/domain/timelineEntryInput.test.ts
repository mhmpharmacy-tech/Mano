import test from 'node:test';
import assert from 'node:assert/strict';
import {
  formatTimelineDateTimeInput,
  parseTimelineEventTime,
  validateTimelineDose,
  validateTimelineTemperature,
} from './timelineEntryInput.ts';

const now = new Date(2026, 10, 27, 12, 31).getTime();
const actualTime = '2026-11-27T12:30';
const actualMs = new Date(2026, 10, 27, 12, 30).getTime();

test('timeline timestamps preserve an explicitly supplied local event time', () => {
  assert.equal(parseTimelineEventTime(actualTime, now), actualMs);
  assert.equal(formatTimelineDateTimeInput(actualMs), actualTime);
  for (const invalid of ['', '12:30', '2026-02-30T12:30', '2026-13-01T12:30', '2026-11-27T24:00', '2026-11-27T12:32', '2026-11-28T12:30', `${actualTime}Z`]) {
    assert.equal(parseTimelineEventTime(invalid, now), null, invalid);
  }
});

test('temperature documentation requires an actual measurement, selected site and past time', () => {
  assert.equal(validateTimelineTemperature('', '', '', now), null);
  assert.equal(validateTimelineTemperature('38.2', '', actualTime, now), null);
  assert.equal(validateTimelineTemperature('38.2', 'زیر بغل (دیجیتال)', '', now), null);
  assert.equal(validateTimelineTemperature('38.2', 'invented method', actualTime, now), null);
  for (const raw of ['abc', 'NaN', '0', '33.9', '43.1']) {
    assert.equal(validateTimelineTemperature(raw, 'زیر بغل (دیجیتال)', actualTime, now), null, raw);
  }
  assert.deepEqual(validateTimelineTemperature('۳۸٫۱', 'روش اندازه‌گیری نامشخص', actualTime, now), {
    temperatureCelsius: 38.1, measuredAtMs: actualMs,
  });
});

test('dose documentation cannot fabricate missing medication, strength, amount, unit or time', () => {
  const actualDose = { medication: 'acetaminophen', formulation: 'شربت ۱۲۰ mg/5mL', amount: '۲٫۵', unit: 'mL', dateTime: actualTime };
  for (const missing of ['medication', 'formulation', 'amount', 'unit', 'dateTime'] as const) {
    assert.equal(validateTimelineDose({ ...actualDose, [missing]: '' }, now), null, missing);
  }
  for (const amount of ['abc', 'NaN', 'Infinity', '-2', '0', '2.5 mL']) {
    assert.equal(validateTimelineDose({ ...actualDose, amount }, now), null, amount);
  }
  assert.equal(validateTimelineDose({ ...actualDose, unit: 'unknown' }, now), null);
  assert.equal(validateTimelineDose({ ...actualDose, medication: 'unknown' }, now), null);
  assert.equal(validateTimelineDose({ ...actualDose, dateTime: '2026-11-27T12:32' }, now), null);
  assert.deepEqual(validateTimelineDose(actualDose, now), {
    amount: 2.5, administeredAtMs: actualMs, amountText: '2.5 mL',
  });
  assert.equal(validateTimelineDose({ ...actualDose, amount: '0.5', unit: 'عدد' }, now)?.amountText, '0.5 عدد');
  assert.equal(validateTimelineDose({ ...actualDose, amount: '0.5', unit: 'قطره' }, now), null);
});
