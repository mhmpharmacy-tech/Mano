import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { existsSync } from 'node:fs';
import * as icons from 'lucide-react';
import {
  FEATURES, FEATURE_GROUPS, HUB_FEATURE_IDS, HOME_TOOL_FEATURE_IDS, HOME_SECONDARY_FEATURE_IDS,
  type FeatureId,
} from './featureCatalog.ts';
import { parseHashToRoute, routeToHash } from './routes.ts';

describe('Feature navigation catalog', () => {
  const destinationIds: FeatureId[] = [
    'symptoms', 'fever-dose', 'syrup-scanner', 'vaccines', 'growth', 'nutrition',
    'treatment-plan', 'medicines', 'topics', 'faq', 'supplements-hygiene', 'skin',
    'devices', 'ask-pharmacist', 'care-cards', 'my-child',
  ];

  it('makes every tool reachable through its existing hash destination', () => {
    assert.deepEqual(Object.keys(FEATURES).sort(), [...destinationIds].sort());
    for (const id of destinationIds) {
      const feature = FEATURES[id];
      assert.equal(feature.route.name, id);
      assert.equal(parseHashToRoute(routeToHash(feature.route)).name, id);
    }
  });

  it('groups every destination exactly once under a titled section', () => {
    assert.equal(new Set(FEATURE_GROUPS.map((group) => group.id)).size, FEATURE_GROUPS.length);
    assert.equal(HUB_FEATURE_IDS.length, destinationIds.length);
    assert.deepEqual([...HUB_FEATURE_IDS].sort(), [...destinationIds].sort());
    for (const group of FEATURE_GROUPS) {
      assert.ok(group.titleFa.trim());
      assert.ok(group.featureIds.length > 0);
      for (const id of group.featureIds) assert.equal(FEATURES[id].group, group.id);
    }
  });

  it('provides usable labels, supported icons and existing image assets', () => {
    for (const feature of Object.values(FEATURES)) {
      assert.ok(feature.labelFa.trim());
      assert.ok(feature.descriptionFa.trim());
      assert.ok(Object.hasOwn(icons, feature.iconKey), `Missing icon: ${feature.iconKey}`);
      if (feature.imagePath) {
        assert.ok(existsSync(new URL(`../../public${feature.imagePath}`, import.meta.url)), `Missing image: ${feature.imagePath}`);
      }
    }
  });

  it('keeps home shortcuts unique and covers the tools beyond the child profile', () => {
    const shortcuts = [...HOME_TOOL_FEATURE_IDS, ...HOME_SECONDARY_FEATURE_IDS];
    assert.equal(new Set(shortcuts).size, shortcuts.length);
    assert.deepEqual([...shortcuts, 'my-child'].sort(), [...destinationIds].sort());
    for (const id of HOME_TOOL_FEATURE_IDS) assert.ok(FEATURES[id].imagePath);
  });

  it('describes local drafts and unavailable online features truthfully', () => {
    assert.equal(FEATURES['ask-pharmacist'].availability, 'local-draft');
    assert.match(FEATURES['ask-pharmacist'].descriptionFa, /ارسال نمی‌شود/);
    assert.match(FEATURES.nutrition.descriptionFa, /آنلاین فعال نیست/);
    assert.match(FEATURES['syrup-scanner'].descriptionFa, /دستی/);
    for (const feature of Object.values(FEATURES)) {
      assert.doesNotMatch(`${feature.labelFa} ${feature.descriptionFa}`, /هوشمند|پیشگیری خطا|محاسبه‌گر ایمن|\bAI\b/);
    }
  });
});
