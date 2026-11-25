import type {
  Article,
  FaqItem,
  MedicationMonograph,
  SupplementOrHygieneGuide,
  SymptomFlow,
} from './models.ts';
import { scoreMatch } from './persianNormalizer.ts';

export interface SearchHit<T> {
  item: T;
  score: number;
}

export interface GlobalSearchResults {
  articles: SearchHit<Article>[];
  medications: SearchHit<MedicationMonograph>[];
  faqs: SearchHit<FaqItem>[];
  symptomFlows: SearchHit<SymptomFlow>[];
  supplements: SearchHit<SupplementOrHygieneGuide>[];
}

export function searchKnowledgeBase(
  query: string,
  catalog: {
    articles: Article[];
    medications: MedicationMonograph[];
    faqs: FaqItem[];
    symptomFlows: SymptomFlow[];
    supplements: SupplementOrHygieneGuide[];
  },
): GlobalSearchResults {
  const rank = <T,>(items: T[], extract: (item: T) => string[]): SearchHit<T>[] =>
    items
      .map((item) => ({ item, score: scoreMatch(query, extract(item)) }))
      .filter((h) => h.score > 0)
      .sort((a, b) => b.score - a.score);

  return {
    symptomFlows: rank(catalog.symptomFlows, (f) => [f.titleFa, f.shortDescFa, ...f.redFlags.map((r) => r.labelFa)]),
    medications: rank(catalog.medications, (m) => [m.nameFa, m.genericNameEn, m.categoryFa, m.shortSummaryFa]),
    articles: rank(catalog.articles, (a) => [a.titleFa, a.subtitleFa, a.categoryFa, ...a.keyTakeawaysFa]),
    faqs: rank(catalog.faqs, (q) => [q.questionFa, q.shortAnswerFa, ...q.keywords]),
    supplements: rank(catalog.supplements, (s) => [s.titleFa, s.categoryFa, s.summaryFa]),
  };
}

export function findMatchingFaqsForQa(query: string, faqs: FaqItem[], limit = 4): FaqItem[] {
  return faqs
    .map((faq) => ({
      faq,
      score: scoreMatch(query, [
        faq.questionFa,
        faq.keywords.join(' '),
        faq.shortAnswerFa,
        faq.clinicalDetailFa,
      ]),
    }))
    .filter((x) => x.score >= 40)
    .sort((a, b) => b.score - a.score)
    .slice(0, limit)
    .map((x) => x.faq);
}
