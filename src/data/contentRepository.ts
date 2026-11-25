import { ARTICLES_CATALOG as ALL_ARTICLES } from './articles.ts';
import { FAQS_CATALOG as ALL_FAQS } from './faqs.ts';
import { MEDICATIONS_CATALOG as ALL_MEDICATIONS } from './medications.ts';
import { SCIENTIFIC_PRODUCT_COMPARISONS } from './productComparison.ts';
import { ALL_SOURCES, CLINICAL_REVIEWER_IDENTITY_FA } from './sources.ts';
import {
  DAILY_HYGIENE_CHECKLIST,
  RECOMMENDED_SUPPLEMENTS_HYGIENE_PRODUCTS,
  SUPPLEMENTS_AND_HYGIENE_GUIDES,
} from './supplementsHygiene.ts';
import {
  DEMO_PHARMACIST_CASES,
  DEVELOPMENTAL_MILESTONES,
  DEVICE_GUIDES,
  SYMPTOM_FLOWS,
} from './symptomFlows.ts';
import { searchKnowledgeBase } from '../domain/symptomSearch.ts';

export {
  ALL_ARTICLES,
  ALL_ARTICLES as ARTICLES_CATALOG,
  ALL_FAQS,
  ALL_FAQS as FAQS_CATALOG,
  ALL_MEDICATIONS,
  ALL_MEDICATIONS as MEDICATIONS_CATALOG,
  ALL_SOURCES,
  CLINICAL_REVIEWER_IDENTITY_FA,
  DAILY_HYGIENE_CHECKLIST,
  DEMO_PHARMACIST_CASES,
  DEVELOPMENTAL_MILESTONES,
  DEVICE_GUIDES,
  RECOMMENDED_SUPPLEMENTS_HYGIENE_PRODUCTS,
  SCIENTIFIC_PRODUCT_COMPARISONS,
  SUPPLEMENTS_AND_HYGIENE_GUIDES,
  SYMPTOM_FLOWS,
};

const sourceMap = new Map(ALL_SOURCES.map((s) => [s.id, s]));
const articleMap = new Map(ALL_ARTICLES.map((a) => [a.slug, a]));
const medicationMap = new Map(ALL_MEDICATIONS.map((m) => [m.slug, m]));
const flowMap = new Map(SYMPTOM_FLOWS.map((f) => [f.id, f]));

export function getSourceById(id: string) {
  return sourceMap.get(id);
}

export function getSourceCitationLabel(id: string): string {
  const s = sourceMap.get(id);
  return s ? `${s.organization} — ${s.titleFa}` : id;
}

export function getArticleBySlug(slug: string) {
  return articleMap.get(slug);
}

export function getMedicationBySlug(slug: string) {
  return medicationMap.get(slug);
}

export function getSymptomFlowById(id: string) {
  return flowMap.get(id);
}

export function runGlobalSearch(query: string) {
  return searchKnowledgeBase(query, {
    articles: ALL_ARTICLES,
    medications: ALL_MEDICATIONS,
    faqs: ALL_FAQS,
    symptomFlows: SYMPTOM_FLOWS,
    supplements: SUPPLEMENTS_AND_HYGIENE_GUIDES,
  });
}
