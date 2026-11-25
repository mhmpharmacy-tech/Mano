import React, { useState } from 'react';
import { ArrowLeftRight, CheckCircle2, Shield, AlertTriangle, Verified } from 'lucide-react';
import { SCIENTIFIC_PRODUCT_COMPARISONS } from '../data/productComparison.ts';
import { CLINICAL_REVIEWER_IDENTITY_FA } from '../data/contentRepository.ts';

export const ProductComparisonSection: React.FC = () => {
  const [selectedIdx, setSelectedIdx] = useState(0);
  const activeCategory = SCIENTIFIC_PRODUCT_COMPARISONS[selectedIdx] ?? SCIENTIFIC_PRODUCT_COMPARISONS[0];

  return (
    <section
      data-testid="scientific-product-comparison-section"
      className="space-y-4 rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]"
    >
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="flex items-center gap-2 text-base font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            <ArrowLeftRight className="h-5 w-5 text-[#105B48] dark:text-[#00D7C7]" />
            <span>مقایسه علمی محصولات سلامت کودک (بدون تبلیغ تجاری)</span>
          </h2>
          <p className="mt-1 text-xs leading-5 text-[#546963] dark:text-[#9EB3C4]">
            مقایسه بی‌طرفانه ابزارهای اندازه‌گیری، اشکال تب‌بر، محلول‌های آبرسانی و وسایل تنفسی بر پایه AAP، WHO و FDA.
          </p>
        </div>
        <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-[#105B48] dark:text-[#00D7C7]">
          <Verified className="h-4 w-4" />
          <span>مستند بالینی</span>
        </span>
      </div>

      <div className="flex flex-wrap gap-2" role="tablist" aria-label="دسته‌بندی مقایسه علمی">
        {SCIENTIFIC_PRODUCT_COMPARISONS.map((cat, idx) => (
          <button
            key={cat.id}
            type="button"
            role="tab"
            aria-selected={idx === selectedIdx}
            onClick={() => setSelectedIdx(idx)}
            className={`min-h-[40px] whitespace-nowrap rounded-xl border px-3.5 text-xs font-bold transition-colors ${
              idx === selectedIdx
                ? 'border-[#105B48] bg-[#E9F5EB] text-[#105B48] dark:border-[#00D7C7] dark:bg-[#0F333A] dark:text-[#00D7C7]'
                : 'border-[#DCE7E1] bg-[#F5F9F6] text-[#546963] hover:text-[#122823] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#9EB3C4]'
            }`}
          >
            {cat.chipLabelFa}
          </button>
        ))}
      </div>

      <div className="border-t border-[#DCE7E1] pt-3 dark:border-[#23344D]">
        <h3 className="text-sm font-bold text-[#105B48] dark:text-[#00D7C7]">{activeCategory.titleFa}</h3>
        <p className="mt-1 text-xs leading-5 text-[#546963] dark:text-[#9EB3C4]">{activeCategory.summaryFa}</p>
        <div className="mt-1.5 text-[11px] text-[#546963] dark:text-[#9EB3C4]">
          مرجع علمی: {activeCategory.sourceCitationFa} · {CLINICAL_REVIEWER_IDENTITY_FA}
        </div>
      </div>

      <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
        {activeCategory.options.map((option, idx) => {
          const isDanger = option.prosFa.length === 0;
          const borderStyle = option.isGoldStandard
            ? 'border-[#B6DFC1] bg-[#F5FBF6] dark:border-[#21594B] dark:bg-[#102B25]'
            : isDanger
              ? 'border-[#E7B5BC] bg-[#FDF6F6] dark:border-[#6A2C3B] dark:bg-[#2F161E]'
              : 'border-[#E8CC99] bg-[#FDF9F1] dark:border-[#664F27] dark:bg-[#2B2112]';

          return (
            <div key={idx} className={`rounded-2xl border p-4 space-y-2.5 ${borderStyle}`}>
              <div className="flex items-start gap-2">
                {option.isGoldStandard ? (
                  <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-[#105B48] dark:text-[#00D7C7]" />
                ) : isDanger ? (
                  <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-[#B32334] dark:text-[#FF8594]" />
                ) : (
                  <Shield className="mt-0.5 h-5 w-5 shrink-0 text-[#9F5F0D] dark:text-[#F5C169]" />
                )}
                <div>
                  <h4 className="text-sm font-bold text-[#122823] dark:text-[#EDF4F7]">{option.nameFa}</h4>
                  <div className="mt-0.5 text-xs text-[#546963] dark:text-[#9EB3C4]">{option.technicalSpecFa}</div>
                  <div className="mt-1 text-xs font-bold text-[#105B48] dark:text-[#00D7C7]">
                    نتیجه علمی: {option.clinicalVerdictFa}
                  </div>
                </div>
              </div>

              {option.prosFa.length > 0 ? (
                <div>
                  <div className="text-xs font-bold text-[#105B48] dark:text-[#35E0C9]">مزایای بالینی و ایمنی:</div>
                  <ul className="mt-1 space-y-1 text-xs leading-5 text-[#122823] dark:text-[#EDF4F7]">
                    {option.prosFa.map((pro, pIdx) => (
                      <li key={pIdx}>✓ {pro}</li>
                    ))}
                  </ul>
                </div>
              ) : null}

              {option.cautionsFa.length > 0 ? (
                <div>
                  <div
                    className={`text-xs font-bold ${
                      isDanger ? 'text-[#B32334] dark:text-[#FF8594]' : 'text-[#9F5F0D] dark:text-[#F5C169]'
                    }`}
                  >
                    هشدارها و محدودیت‌های ایمنی:
                  </div>
                  <ul className="mt-1 space-y-1 text-xs leading-5 text-[#122823] dark:text-[#EDF4F7]">
                    {option.cautionsFa.map((warn, wIdx) => (
                      <li key={wIdx}>• {warn}</li>
                    ))}
                  </ul>
                </div>
              ) : null}
            </div>
          );
        })}
      </div>
    </section>
  );
};
