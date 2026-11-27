import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { parseHashToRoute, routeToHash, type AppRoute } from './routes.ts';

describe('Hash routes', () => {
  it('preserves every top-level destination and its canonical hash', () => {
    const destinations: AppRoute['name'][] = [
      'home', 'symptoms', 'fever-dose', 'syrup-scanner', 'vaccines', 'growth',
      'nutrition', 'treatment-plan', 'medicines', 'topics', 'faq',
      'supplements-hygiene', 'skin', 'devices', 'ask-pharmacist', 'care-cards', 'my-child',
    ];
    for (const name of destinations) {
      const hash = `#/${name}`;
      const route = parseHashToRoute(hash);
      assert.equal(route.name, name);
      assert.equal(routeToHash(route), hash);
    }
  });

  it('round-trips encoded path and query parameters without splitting their content', () => {
    const parameter = 'تب / کودک?dose=2&note=خواب#شب+۱';
    const routes: AppRoute[] = [
      { name: 'symptoms', initialFlowId: parameter },
      { name: 'medicine-detail', slug: parameter },
      { name: 'topic-detail', slug: parameter },
      { name: 'fever-dose', prefillFormulationId: parameter },
      { name: 'ask-pharmacist', initialTopic: parameter },
      { name: 'growth', initialTab: 'milestones' },
      { name: 'supplements-hygiene', initialTab: 'hygiene' },
    ];
    for (const route of routes) {
      const hash = routeToHash(route);
      assert.deepEqual(parseHashToRoute(hash), route);
      assert.equal(routeToHash(parseHashToRoute(hash)), hash);
    }
    assert.equal(routeToHash({ name: 'medicine-detail', slug: 'a/b' }), '#/medicines/a%2Fb');
    assert.equal(routeToHash({ name: 'topic-detail', slug: 'a?b' }), '#/topics/a%3Fb');
  });

  it('preserves the existing defaults for optional values and tabs', () => {
    assert.deepEqual(parseHashToRoute('#/symptoms'), { name: 'symptoms', initialFlowId: undefined });
    assert.deepEqual(parseHashToRoute('#/fever-dose?formulation='), { name: 'fever-dose', prefillFormulationId: undefined });
    assert.deepEqual(parseHashToRoute('#/ask-pharmacist?topic='), { name: 'ask-pharmacist', initialTopic: undefined });
    for (const tab of ['', '?tab=chart', '?tab=unknown']) {
      assert.deepEqual(parseHashToRoute(`#/growth${tab}`), { name: 'growth', initialTab: 'chart' });
    }
    for (const tab of ['', '?tab=supplements', '?tab=unknown']) {
      assert.deepEqual(parseHashToRoute(`#/supplements-hygiene${tab}`), { name: 'supplements-hygiene', initialTab: 'supplements' });
    }
    assert.equal(routeToHash({ name: 'growth', initialTab: 'chart' }), '#/growth');
    assert.equal(routeToHash({ name: 'supplements-hygiene', initialTab: 'supplements' }), '#/supplements-hygiene');
  });

  it('accepts existing hash prefixes and detail URL names', () => {
    for (const hash of ['', '#', '#/', 'home', '#home', '#/home']) {
      assert.deepEqual(parseHashToRoute(hash), { name: 'home' });
    }
    assert.deepEqual(parseHashToRoute('#symptoms/fever'), { name: 'symptoms', initialFlowId: 'fever' });
    assert.deepEqual(parseHashToRoute('#medicines/acetaminophen'), { name: 'medicine-detail', slug: 'acetaminophen' });
    assert.deepEqual(parseHashToRoute('#/topics/fever-care'), { name: 'topic-detail', slug: 'fever-care' });
    assert.deepEqual(parseHashToRoute('#/ask-pharmacist?topic=fever+care'), { name: 'ask-pharmacist', initialTopic: 'fever care' });
  });

  it('returns home for unknown destinations and malformed encoded path parameters', () => {
    assert.deepEqual(parseHashToRoute('#/unknown'), { name: 'home' });
    for (const destination of ['symptoms', 'medicines', 'topics']) {
      for (const malformed of ['%', '%ZZ', '%E0%A4%A', '%ED%A0%80']) {
        assert.deepEqual(parseHashToRoute(`#/${destination}/${malformed}`), { name: 'home' });
      }
    }
  });

  it('keeps URLSearchParams handling of malformed query values', () => {
    assert.deepEqual(parseHashToRoute('#/ask-pharmacist?topic=%ZZ'), { name: 'ask-pharmacist', initialTopic: '%ZZ' });
    assert.deepEqual(parseHashToRoute('#/fever-dose?formulation=%E0%A4%A'), { name: 'fever-dose', prefillFormulationId: '�%A' });
  });
});
