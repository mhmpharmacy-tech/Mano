# MANO Kids — SEO prelaunch plan (FA-IR, health / YMYL)
**Date:** 2026-11-17 · **State:** PRELAUNCH — NOT INDEXABLE · **No domain deployed**

## Search principle
Search quality cannot outrank patient safety. Google explicitly treats information affecting health as high stakes. We need identifiable, genuinely qualified authors/reviewers, directly relevant dated sources, original Persian-language utility and clear conflicts-of-interest policies. Do **not** invent pharmacist reviewers, ratings, medical credentials, approvals, source-update timestamps or "24/7 support" promises.

## Current preview safeguards
- `web/index.html` and `web/staff-demo.html` both contain `<meta name="robots" content="noindex,nofollow">`. Keep it until a separate clinical/public launch approval; where staging remains private, prefer access controls as well. A robots.txt disallow is **not** a substitute for noindex because blocked crawlers cannot see it.
- Single descriptive H1 and HTML title, unique meta descriptions, valid RTL `lang=fa`, accessible semantic anchors and OG presentation metadata. No unverifiable Organization/Physician/MedicalWebPage markup or FAQ rich-result promises.
- Domain ownership does not establish a live site; no canonical URL, sitemap or redirects configured in this PR.

## Candidate Persian-language search-intent architecture
| Cluster | Parent intent | Candidate page only AFTER scientific sign-off | Safety constraint |
|---|---|---|---|
| تب کودک | "تب بچه‌ام چقدر خطرناک است؟" | /fever-in-children/ | Make infant age, danger signs and urgent referral prominent; no false reassurance |
| سرفه طولانی | "چه زمانی سرفه نیاز به پزشک دارد؟" | /persistent-cough-in-children/ | Avoid diagnosing from duration alone |
| سوختگی پوشک | "با راش پوشک چه کنم؟" | /diaper-rash/ | Red-flag escalation and referral differentiation |
| شربت در برابر قطره | "چرا میلی‌لیترها فرق دارند؟" | /liquid-medicine-concentrations/ | Concentration-mix-up education; no auto-dosing |
| یبوست کودک | "چه زمانی برای یبوست مراجعه کنم؟" | /childhood-constipation/ | Age-specific red flags, no automatic medication recommendation |
| شیرخشک | "چه شیری برای نوزاد من مناسب است؟" | /infant-formula-questions/ | Clinical selection and commercial conflicts require review |

These are **content opportunities**, not live page URLs and not recommended treatment advice. Prioritize 1–2 deeply reviewed topics over unreviewed mass publishing.

## Required article publishing contract
Use `web/content/ARTICLE_TEMPLATE.json`. An article can move out of DRAFT only when:
1. Qualified author and pediatric/clinical pharmacist reviewer identities and credentials are verified; the relevant specialist is involved when needed.
2. Every actionable claim has a direct guideline/page URL, access/review date and precise text provenance. Sources in the current register are **candidate pointers**, not approved citations.
3. Age range, emergency signs, local relevance, contraindications, external links and commercial conflicts have been reviewed.
4. Human clinical approval includes a version and revision date; stale guidance triggers review, not automatic "fresh" date inflation.
5. UI supports 320–430px and desktop, keyboard and screen reader; independent safety tests pass.
6. Indexing is enabled only through an **explicit, audited publishing step**, not by editing `noindex` casually.

## Domain and technical launch
- Confirm verified hosting, TLS, DNS, one primary host (likely `manokid.ir` for Iran) and redirect policy for `manokid.com`, `www` and HTTP. Do not point real domains in code before configuration. Google's canonical is a *preference*, not a guaranteed outcome.
- After approval, add absolute self-referencing canonical per approved article, HTTPS 301/308 redirects from duplicate hosts, a sitemap of **approved** canonical pages only, appropriate robots policy, and Search Console verification. Never list staff-demo or patient-specific pages in the sitemap.
- Add schema.org types only with true, supported details. FAQ schema does not guarantee rich results; do not implement for an unreviewed medical preview.
- Check Core Web Vitals using lab and real-user measurements after hosting; inspect network conditions in Iran, compressed images and cache headers. Do not claim performance scores before measurement.

## Sources for this plan
- Google people-first health content: https://developers.google.com/search/docs/fundamentals/creating-helpful-content
- Google noindex: https://developers.google.com/search/docs/crawling-indexing/block-indexing
- Google canonicalization: https://developers.google.com/search/docs/crawling-indexing/consolidate-duplicate-urls
- WCAG 2.2 quick reference: https://www.w3.org/WAI/WCAG22/quickref/

**No automatic indexing or new medical article was enabled.**
