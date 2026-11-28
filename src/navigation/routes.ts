export type AppRoute =
  | { name: 'home' }
  | { name: 'symptoms'; initialFlowId?: string }
  | { name: 'fever-dose'; prefillFormulationId?: string }
  | { name: 'syrup-scanner' }
  | { name: 'vaccines' }
  | { name: 'growth'; initialTab?: 'chart' | 'milestones' }
  | { name: 'nutrition' }
  | { name: 'treatment-plan' }
  | { name: 'medicines' }
  | { name: 'medicine-detail'; slug: string }
  | { name: 'topics' }
  | { name: 'topic-detail'; slug: string }
  | { name: 'faq' }
  | { name: 'supplements-hygiene'; initialTab?: 'supplements' | 'hygiene' }
  | { name: 'skin' }
  | { name: 'devices' }
  | { name: 'ask-pharmacist'; initialTopic?: string }
  | { name: 'care-cards'; cardId?: string }
  | { name: 'my-child' };

export function routeToHash(route: AppRoute): string {
  switch (route.name) {
    case 'home':
      return '#/home';
    case 'care-cards':
      return route.cardId ? `#/care-cards?card=${encodeURIComponent(route.cardId)}` : '#/care-cards';
    case 'symptoms':
      return route.initialFlowId ? `#/symptoms/${encodeURIComponent(route.initialFlowId)}` : '#/symptoms';
    case 'fever-dose':
      return route.prefillFormulationId
        ? `#/fever-dose?formulation=${encodeURIComponent(route.prefillFormulationId)}`
        : '#/fever-dose';
    case 'medicine-detail':
      return `#/medicines/${encodeURIComponent(route.slug)}`;
    case 'topic-detail':
      return `#/topics/${encodeURIComponent(route.slug)}`;
    case 'growth':
      return route.initialTab === 'milestones' ? '#/growth?tab=milestones' : '#/growth';
    case 'supplements-hygiene':
      return route.initialTab === 'hygiene'
        ? '#/supplements-hygiene?tab=hygiene'
        : '#/supplements-hygiene';
    case 'ask-pharmacist':
      return route.initialTopic
        ? `#/ask-pharmacist?topic=${encodeURIComponent(route.initialTopic)}`
        : '#/ask-pharmacist';
    default:
      return `#/${route.name}`;
  }
}

export function parseHashToRoute(hash: string): AppRoute {
  try {
    const clean = hash.replace(/^#\/?/, '');
    if (!clean || clean === 'home') return { name: 'home' };
    const [pathPart, queryPart] = clean.split('?');
    const segs = pathPart.split('/').filter(Boolean);
    const query = new URLSearchParams(queryPart || '');
    const head = segs[0];
    switch (head) {
      case 'symptoms':
        return { name: 'symptoms', initialFlowId: segs[1] ? decodeURIComponent(segs[1]) : undefined };
      case 'fever-dose':
        return {
          name: 'fever-dose',
          prefillFormulationId: query.get('formulation') || undefined,
        };
      case 'syrup-scanner':
        return { name: 'syrup-scanner' };
      case 'vaccines':
        return { name: 'vaccines' };
      case 'growth':
        return {
          name: 'growth',
          initialTab: query.get('tab') === 'milestones' ? 'milestones' : 'chart',
        };
      case 'nutrition':
        return { name: 'nutrition' };
      case 'treatment-plan':
        return { name: 'treatment-plan' };
      case 'medicines':
        return segs[1]
          ? { name: 'medicine-detail', slug: decodeURIComponent(segs[1]) }
          : { name: 'medicines' };
      case 'topics':
        return segs[1]
          ? { name: 'topic-detail', slug: decodeURIComponent(segs[1]) }
          : { name: 'topics' };
      case 'faq':
        return { name: 'faq' };
      case 'supplements-hygiene':
        return {
          name: 'supplements-hygiene',
          initialTab: query.get('tab') === 'hygiene' ? 'hygiene' : 'supplements',
        };
      case 'skin':
        return { name: 'skin' };
      case 'devices':
        return { name: 'devices' };
      case 'ask-pharmacist':
        return { name: 'ask-pharmacist', initialTopic: query.get('topic') || undefined };
      case 'care-cards': {
        const cardId = query.get('card');
        return cardId ? { name: 'care-cards', cardId } : { name: 'care-cards' };
      }
      case 'my-child':
        return { name: 'my-child' };
      default:
        return { name: 'home' };
    }
  } catch {
    // A malformed encoded path must not prevent the app from rendering.
    return { name: 'home' };
  }
}

