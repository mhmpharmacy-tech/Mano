import React from 'react';
import { ShieldCheck, ShieldAlert, WifiOff } from 'lucide-react';
import type { GatewayResponseMeta } from '../domain/aiGatewayContracts.ts';

interface GatewayMetaBannerProps {
  meta: GatewayResponseMeta;
  policyError?: string | null;
}

export const GatewayMetaBanner: React.FC<GatewayMetaBannerProps> = ({ meta, policyError }) => {
  const isUnknown = meta.verificationState === 'UNKNOWN';
  const isReview = meta.verificationState === 'REQUIRE_REVIEW' || meta.fallbackUsed;

  const badgeLabel = isUnknown
    ? 'نامشخص / نیازمند اطلاعات بیشتر (UNKNOWN)'
    : isReview
      ? meta.fallbackUsed
        ? 'نسخه ایمن آفلاین — نیازمند بررسی (REQUIRE_REVIEW)'
        : 'نیازمند بررسی دقیق والد یا پزشک (REQUIRE_REVIEW)'
      : 'پیش‌نویس دستیار — نیازمند تایید (ASSISTANT_DRAFT)';

  const containerStyle = isUnknown || isReview
    ? 'border-[#E8CC99] bg-[#FDF9F1] text-[#4B3410] dark:border-[#664F27] dark:bg-[#2B2112] dark:text-[#FCE7C0]'
    : 'border-[#DCE7E1] bg-[#F5F9F6] text-[#122823] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#EDF4F7]';

  return (
    <div
      data-testid="ai-gateway-meta-banner"
      className={`rounded-2xl border p-3 text-[11px] leading-5 space-y-1 ${containerStyle}`}
    >
      <div className="flex flex-wrap items-center justify-between gap-2 font-bold">
        <span className="inline-flex items-center gap-1.5">
          {meta.fallbackUsed ? (
            <WifiOff className="h-3.5 w-3.5 shrink-0" />
          ) : isReview ? (
            <ShieldAlert className="h-3.5 w-3.5 shrink-0" />
          ) : (
            <ShieldCheck className="h-3.5 w-3.5 shrink-0 text-[#105B48] dark:text-[#00D7C7]" />
          )}
          <span>{badgeLabel}</span>
        </span>
        <span className="font-mono text-[10px] opacity-80" dir="ltr">
          Audit: {meta.auditId} · {meta.modelUsed === 'NONE' ? 'OFFLINE_ENGINE' : meta.modelUsed}
        </span>
      </div>

      {policyError ? <p className="font-semibold text-[#B32334] dark:text-[#FF8594]">{policyError}</p> : null}

      {meta.warningsFa.length > 0 ? (
        <ul className="list-disc pr-4 space-y-0.5 opacity-90">
          {meta.warningsFa.map((w, idx) => (
            <li key={idx}>{w}</li>
          ))}
        </ul>
      ) : null}

      <p className="opacity-80">{meta.disclaimerFa}</p>
    </div>
  );
};
