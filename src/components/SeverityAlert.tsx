import React from 'react';
import { AlertTriangle, CheckCircle2, Info, ShieldAlert } from 'lucide-react';
import type { AlertSeverity } from '../domain/models.ts';

interface SeverityAlertProps {
  severity: AlertSeverity;
  title: string;
  body?: string;
  bullets?: string[];
  className?: string;
  testId?: string;
  children?: React.ReactNode;
}

const STYLE_MAP: Record<
  AlertSeverity,
  { container: string; title: string; iconWrap: string; Icon: React.FC<{ className?: string }> }
> = {
  danger: {
    container:
      'bg-[#FDF0F1] dark:bg-[#2F161E] border-[#E7B5BC] dark:border-[#6A2C3B] text-[#4C1119] dark:text-[#FFDDE2]',
    title: 'text-[#B32334] dark:text-[#FF8594]',
    iconWrap: 'bg-[#FBE0E4] dark:bg-[#491F2B] text-[#B32334] dark:text-[#FF8594]',
    Icon: ShieldAlert,
  },
  caution: {
    container:
      'bg-[#FDF4E4] dark:bg-[#2B2112] border-[#E8CC99] dark:border-[#664F27] text-[#4B3410] dark:text-[#FCE7C0]',
    title: 'text-[#9F5F0D] dark:text-[#F5C169]',
    iconWrap: 'bg-[#FAE8C8] dark:bg-[#3E2F18] text-[#9F5F0D] dark:text-[#F5C169]',
    Icon: AlertTriangle,
  },
  success: {
    container:
      'bg-[#EBF7EE] dark:bg-[#102B25] border-[#B6DFC1] dark:border-[#21594B] text-[#113C27] dark:text-[#CBEFD9]',
    title: 'text-[#105B48] dark:text-[#35E0C9]',
    iconWrap: 'bg-[#D5EEDF] dark:bg-[#174137] text-[#105B48] dark:text-[#35E0C9]',
    Icon: CheckCircle2,
  },
  info: {
    container:
      'bg-[#EEF6FB] dark:bg-[#112438] border-[#C1DCEB] dark:border-[#25496B] text-[#14374F] dark:text-[#D1E8FA]',
    title: 'text-[#1B5C85] dark:text-[#6CC0F5]',
    iconWrap: 'bg-[#DAECF7] dark:bg-[#1A3550] text-[#1B5C85] dark:text-[#6CC0F5]',
    Icon: Info,
  },
};

export const SeverityAlert: React.FC<SeverityAlertProps> = ({
  severity,
  title,
  body,
  bullets,
  className = '',
  testId,
  children,
}) => {
  const palette = STYLE_MAP[severity];
  const Icon = palette.Icon;

  return (
    <div
      role={severity === 'danger' ? 'alert' : 'status'}
      data-testid={testId}
      className={`rounded-2xl border p-4 ${palette.container} ${className}`}
    >
      <div className="flex items-start gap-3">
        <span
          aria-hidden="true"
          className={`mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl ${palette.iconWrap}`}
        >
          <Icon className="h-5 w-5" />
        </span>
        <div className="min-w-0 flex-1 space-y-1.5">
          <h3 className={`text-sm font-bold leading-6 ${palette.title}`}>{title}</h3>
          {body ? <p className="text-sm leading-6 whitespace-pre-line">{body}</p> : null}
          {bullets && bullets.length > 0 ? (
            <ul className="list-disc space-y-1 pr-5 text-sm leading-6">
              {bullets.map((item, idx) => (
                <li key={idx}>{item}</li>
              ))}
            </ul>
          ) : null}
          {children ? <div className="pt-2">{children}</div> : null}
        </div>
      </div>
    </div>
  );
};
