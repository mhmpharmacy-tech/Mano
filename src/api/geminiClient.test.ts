import test from 'node:test';
import assert from 'node:assert/strict';
import {
  ApiClientError,
  fetchApiHealth,
  isRequestCancelledError,
  requestDocumentedQa,
  requestNutritionPlan,
  requestSafetyChecklist,
  requestSyrupScan,
  requestVisitDraftStream,
} from './geminiClient.ts';

const sensitiveContext = 'SYNTHETIC child alias, age, weight, allergy and caregiver notes';
const requests = [
  {
    name: 'streaming visit draft',
    invoke: (signal?: AbortSignal) => requestVisitDraftStream({
      promptText: sensitiveContext,
      parentNotesFa: sensitiveContext,
      childContextSummaryFa: sensitiveContext,
      tempPointsFa: ['38.2 SYNTHETIC caregiver'],
      dosePointsFa: ['SYNTHETIC medicine history'],
      signal,
    }),
  },
  {
    name: 'nutrition',
    invoke: (signal?: AbortSignal) => requestNutritionPlan({
      ageMonths: 18,
      weightKg: '11',
      allergiesFa: sensitiveContext,
      dietGoalFa: sensitiveContext,
      childContextSummaryFa: sensitiveContext,
      signal,
    }),
  },
  {
    name: 'syrup image scan',
    invoke: (signal?: AbortSignal) => requestSyrupScan({
      imageBase64: 'SYNTHETIC_PRIVATE_IMAGE',
      mimeType: 'image/png',
      childContextSummaryFa: sensitiveContext,
      signal,
    }),
  },
  {
    name: 'safety checklist',
    invoke: (signal?: AbortSignal) => requestSafetyChecklist({
      flowId: 'fever',
      flowTitleFa: 'fever',
      urgencyLevel: 'GENERAL_EDUCATIONAL',
      evaluationSummaryFa: sensitiveContext,
      selectedSymptomsFa: [sensitiveContext],
      childContextSummaryFa: sensitiveContext,
      signal,
    }),
  },
  {
    name: 'documented FAQ',
    invoke: (signal?: AbortSignal) => requestDocumentedQa({
      question: sensitiveContext,
      ageBandFa: '18 months',
      matchedFaqs: [],
      signal,
    }),
  },
];

function assertUnavailable(err: unknown): boolean {
  assert.ok(err instanceof ApiClientError);
  assert.equal(err.status, 503);
  assert.equal(err.code, 'BROWSER_AI_UNAVAILABLE');
  assert.match(err.message, /هیچ اطلاعاتی ارسال نشد/);
  return true;
}

test('every browser AI wrapper rejects locally without transmitting clinical information', async (t) => {
  const network = t.mock.method(globalThis, 'fetch', async () => {
    throw new Error('Unexpected network request containing clinical data');
  });
  for (const request of requests) {
    await t.test(request.name, async () => {
      await assert.rejects(request.invoke(), assertUnavailable);
      assert.equal(network.mock.callCount(), 0);
    });
  }
});

test('browser AI never serializes clinical bodies or emits streaming progress', async (t) => {
  const network = t.mock.method(globalThis, 'fetch', async () => {
    throw new Error('Unexpected network request');
  });
  let progressCalls = 0;
  let sensitiveReads = 0;
  await assert.rejects(requestVisitDraftStream({
    get promptText(): string {
      sensitiveReads += 1;
      throw new Error('Clinical payload must not be read or serialized');
    },
  }, () => { progressCalls += 1; }), assertUnavailable);
  assert.equal(sensitiveReads, 0);
  assert.equal(progressCalls, 0);
  assert.equal(network.mock.callCount(), 0);
});

test('pre-aborted browser requests retain cancellation semantics and never fetch', async (t) => {
  const network = t.mock.method(globalThis, 'fetch', async () => {
    throw new Error('Unexpected network request');
  });
  const controller = new AbortController();
  controller.abort();
  for (const request of [...requests, { name: 'public health', invoke: fetchApiHealth }]) {
    await t.test(request.name, async () => {
      await assert.rejects(request.invoke(controller.signal), (err: unknown) => {
        assert.ok(err instanceof ApiClientError);
        assert.equal(err.code, 'REQUEST_CANCELLED');
        assert.equal(err.status, 499);
        assert.equal(isRequestCancelledError(err), true);
        return true;
      });
    });
  }
  assert.equal(network.mock.callCount(), 0);
});

test('public health diagnostics cannot authorize browser AI uploads', async (t) => {
  const network = t.mock.method(globalThis, 'fetch', async () => Response.json({
    ok: true,
    clinicalServiceEnabled: true,
    aiServiceAvailable: true,
    offlineEnginesIndependent: true,
  }));
  const health = await fetchApiHealth();
  assert.equal(health.aiServiceAvailable, true);
  for (const request of requests) await assert.rejects(request.invoke(), assertUnavailable);
  assert.equal(network.mock.callCount(), 1);
  assert.equal(network.mock.calls[0].arguments[0], '/api/health');
  assert.deepEqual(network.mock.calls[0].arguments[1], { signal: undefined });
});
