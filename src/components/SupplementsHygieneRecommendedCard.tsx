import React, { useState } from 'react';
import {
  Baby,
  ChevronLeft,
  Droplets,
  Shield,
  Sparkles,
  Stethoscope,
  Sun,
  Verified,
  X,
} from 'lucide-react';
import {
  RECOMMENDED_SUPPLEMENTS_HYGIENE_PRODUCTS,
  type RecommendedChildCareProduct,
} from '../data/supplementsHygiene.ts';

interface SupplementsHygieneRecommendedCardProps {
  onAskPharmacist: (categoryId: string) => void;
  onOpenAllGuides?: () => void;
}

function ProductIcon({ kind }: { kind: RecommendedChildCareProduct['iconKind'] }) {
  switch (kind) {
    case 'sun':
      return <Sun className="h-5 w-5" />;
    case 'drop':
      return <Droplets className="h-5 w-5" />;
    case 'child':
      return <Baby className="h-5 w-5" />;
    case 'shield':
      return <Shield className="h-5 w-5" />;
    case 'sparkles':
      return <Sparkles className="h-5 w-5" />;
    default:
      return <Sparkles className="h-5 w-5" />;
  }
}

export const SupplementsHygieneRecommendedCard: React.FC<SupplementsHygieneRecommendedCardProps> = ({
  onAskPharmacist,
  onOpenAllGuides,
}) => {
  const [selectedProduct, setSelectedProduct] = useState<RecommendedChildCareProduct | null>(null);

  return (
    <section
      data-testid="supplements-hygiene-recommended-card"
      className="overflow-hidden rounded-3xl border border-[#DCE7E1] bg-white dark:border-[#23344D] dark:bg-[#111C2E]"
    >
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#DCE7E1] bg-[#E9F5EB]/70 px-5 py-4 dark:border-[#23344D] dark:bg-[#0F333A]/80">
        <div className="flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-[#105B48]/15 text-[#105B48] dark:bg-[#00D7C7]/15 dark:text-[#00D7C7]">
            <Sparkles className="h-5 w-5" />
          </span>
          <div>
            <div className="flex items-center gap-1.5">
              <h3 className="text-sm font-extrabold text-[#122823] dark:text-[#EDF4F7]">
                مکمل‌ها و بهداشت کودک (استاندارد داروخانه‌ای)
              </h3>
              <Verified className="h-4 w-4 text-[#C6862E] dark:text-[#F0B85C]" />
            </div>
            <p className="text-xs text-[#546963] dark:text-[#9EB3C4]">
              نکات انتخاب علمی محصول · بدون معرفی برند تجاری · لمس هر مورد برای راهنما
            </p>
          </div>
        </div>

        {onOpenAllGuides ? (
          <button
            type="button"
            onClick={onOpenAllGuides}
            className="min-h-[40px] px-3 text-xs font-bold text-[#105B48] hover:underline dark:text-[#00D7C7]"
          >
            همه راهنماها
          </button>
        ) : null}
      </div>

      <div className="divide-y divide-[#DCE7E1]/70 px-4 dark:divide-[#23344D]/70">
        {RECOMMENDED_SUPPLEMENTS_HYGIENE_PRODUCTS.map((product) => (
          <button
            key={product.id}
            type="button"
            onClick={() => setSelectedProduct(product)}
            className="flex min-h-[60px] w-full items-center justify-between gap-3 py-3 text-right transition-colors hover:bg-[#F5F9F6] dark:hover:bg-[#16243B] px-2 rounded-xl"
          >
            <div className="flex items-center gap-3 min-w-0">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[#F5F9F6] text-[#105B48] dark:bg-[#16243B] dark:text-[#00D7C7]">
                <ProductIcon kind={product.iconKind} />
              </span>
              <div className="min-w-0">
                <div className="truncate text-sm font-bold text-[#122823] dark:text-[#EDF4F7]">
                  {product.titleFa}
                </div>
                <div className="truncate text-xs text-[#546963] dark:text-[#9EB3C4]">
                  {product.subtitleFa} · {product.ageHintFa}
                </div>
              </div>
            </div>
            <ChevronLeft className="h-4 w-4 shrink-0 text-[#546963] dark:text-[#9EB3C4]" />
          </button>
        ))}
      </div>

      <div className="p-4 pt-2">
        <button
          type="button"
          onClick={() => onAskPharmacist('supplement')}
          className="flex min-h-[44px] w-full items-center justify-center gap-2 rounded-xl bg-[#105B48] px-4 text-sm font-bold text-white hover:bg-[#0C493A] dark:bg-[#00D7C7] dark:text-[#0B1220]"
        >
          <Stethoscope className="h-4 w-4" />
          <span>پرسش از داروساز درباره مکمل و بهداشت</span>
        </button>
      </div>

      {selectedProduct ? (
        <div
          role="dialog"
          aria-modal="true"
          aria-label={selectedProduct.titleFa}
          className="fixed inset-0 z-50 flex items-center justify-center bg-[#0B1220]/70 p-4 backdrop-blur-sm"
          onClick={() => setSelectedProduct(null)}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-md space-y-4 rounded-3xl border border-[#DCE7E1] bg-white p-6 shadow-2xl dark:border-[#23344D] dark:bg-[#111C2E]"
          >
            <div className="flex items-start justify-between gap-2">
              <div>
                <div className="text-xs font-semibold text-[#105B48] dark:text-[#00D7C7]">
                  {selectedProduct.categoryTagFa} · {selectedProduct.ageHintFa}
                </div>
                <h4 className="mt-1 text-base font-extrabold text-[#122823] dark:text-[#EDF4F7]">
                  {selectedProduct.titleFa}
                </h4>
              </div>
              <button
                type="button"
                onClick={() => setSelectedProduct(null)}
                aria-label="بستن"
                className="flex min-h-[40px] min-w-[40px] items-center justify-center rounded-xl text-[#546963]"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <p className="text-xs leading-5 text-[#546963] dark:text-[#9EB3C4]">{selectedProduct.subtitleFa}</p>

            <ul className="list-disc space-y-2 pr-5 text-sm leading-6 text-[#122823] dark:text-[#EDF4F7]">
              {selectedProduct.selectionTipsFa.map((tip, idx) => (
                <li key={idx}>{tip}</li>
              ))}
            </ul>

            <button
              type="button"
              onClick={() => {
                const cat = selectedProduct.pharmacistCategoryId;
                setSelectedProduct(null);
                onAskPharmacist(cat);
              }}
              className="flex min-h-[44px] w-full items-center justify-center gap-2 rounded-xl bg-[#105B48] px-4 text-sm font-bold text-white dark:bg-[#00D7C7] dark:text-[#0B1220]"
            >
              <span>پرسش درباره این محصول</span>
            </button>
          </div>
        </div>
      ) : null}
    </section>
  );
};
