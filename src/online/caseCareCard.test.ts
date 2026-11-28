import assert from 'node:assert/strict';
import { test } from 'node:test';
import { parseParentCases } from './responses.ts';

const card = {
  child: 'کودک نمونه', medicine: 'نمونه', strength: 'غلظت نمونه', instructions: 'متن داروساز', followUp: 'موعد نمونه',
  precautions: 'احتیاط نمونه', closedAt: null, shareToken: 'private-token',
  events: [{ action: 'checkin', text: 'پیام خانواده', at: 500, actor: 'private-account' }],
};
const reviewed = {
  caseId: 'case1', pharmacyId: 'pharmacy1', questionSummaryFa: 'پرسش نمونه',
  status: 'REVIEWED', createdAt: 100,
  parentResponse: { replyTextFa: 'متن پاسخ', respondedByRole: 'pharmacist', respondedAt: 200, attestationConfirmed: true },
};

test('role views retain the care instructions and family message without internal identifiers', () => {
  for (const expectedPharmacy of [undefined, reviewed.pharmacyId]) {
    const [row] = parseParentCases([{ ...reviewed, careCard: card }], expectedPharmacy);
    assert.ok(row.careCard);
    assert.equal(row.careCard?.closed, false);
    assert.ok(row.careCard.fields.some(field => field.value === card.precautions));
    assert.deepEqual(row.careCard.events, [{ at: 500, label: 'پیگیری خانواده', text: 'پیام خانواده' }]);
    assert.doesNotMatch(JSON.stringify(row), /private-account|private-token/);
  }
  const [closed] = parseParentCases([{ ...reviewed, careCard: { ...card, closedAt: 1000 } }]);
  assert.equal(closed.careCard?.closed, true);
});
test('incomplete instructions and cards on unreviewed cases cannot be displayed', () => {
  assert.throws(() => parseParentCases([{ ...reviewed, careCard: { ...card, strength: '' } }]));
  assert.throws(() => parseParentCases([{ ...reviewed, status: 'PENDING', parentResponse: null, careCard: card }]));
  assert.throws(() => parseParentCases([{ ...reviewed, careCard: { ...card, events: [{ action: 'unrecognized', text: '', at: 1 }] } }]));
});