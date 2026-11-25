import React from 'react';
import { ArrowRight, Baby, Pill, ShieldCheck, Stethoscope } from 'lucide-react';
import { useMano } from '../state/ManoStore.tsx';
import { SeverityAlert } from '../components/SeverityAlert.tsx';
import { SourceBadge } from '../components/SourceBadge.tsx';
import { ProductComparisonSection } from '../components/ProductComparisonSection.tsx';

export const SkinScreen: React.FC = () => {
  const { navigate } = useMano();

  return (
    <div className="space-y-6" data-testid="skin-screen">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            مراقبت پوست کودک و پیشگیری از سوختگی پوشک
          </h1>
          <p className="mt-1 text-xs text-[#546963] dark:text-[#9EB3C4]">
            انتخاب صحیح محافظ زینک اکساید، پرهیز از کورتون خودسرانه زیر پوشک و بررسی علائم عفونت پوستی
          </p>
        </div>
        <button
          type="button"
          onClick={() => navigate({ name: 'home' })}
          className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] px-3 text-xs font-bold dark:border-[#23344D]"
        >
          <ArrowRight className="h-4 w-4" />
          <span>خانه</span>
        </button>
      </div>

      {/* Hero Care Card */}
      <section className="grid grid-cols-1 items-center gap-5 rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E] md:grid-cols-3">
        <div className="md:col-span-2 space-y-3">
          <h2 className="flex items-center gap-2 text-base font-extrabold text-[#105B48] dark:text-[#00D7C7]">
            <Baby className="h-5 w-5" />
            <span>چهار اصل طلایی مراقبت از سد محافظتی پوست نوزاد</span>
          </h2>
          <ul className="list-disc space-y-1.5 pr-5 text-xs leading-6 text-[#122823] dark:text-[#EDF4F7]">
            <li>تعویض منظم پوشک و شست‌وشوی ملایم با آب ولرم و خشک کردن ضربه‌ای با حوله نخی نرم.</li>
            <li>هواخوری کوتاه‌مدت پوست بین تعویض‌ها و پرهیز از دستمال مرطوب عطردار یا الکلی روی پوست ملتهب.</li>
            <li>استفاده از لایه محافظ زینک اکساید بدون عطر یا پترولاتوم خالص در هر تعویض پوشک.</li>
            <li>مرطوب‌سازی پوست بدن در ۳ دقیقه پس از حمام با کرم ساده بدون اسانس برای پیشگیری از خشکی و اگزما.</li>
          </ul>
          <div className="flex flex-wrap gap-2 pt-1">
            <button
              type="button"
              onClick={() => navigate({ name: 'symptoms', initialFlowId: 'diaper_rash' })}
              className="inline-flex min-h-[42px] items-center gap-1.5 rounded-xl bg-[#105B48] px-4 text-xs font-bold text-white dark:bg-[#00D7C7] dark:text-[#0B1220]"
            >
              <Stethoscope className="h-4 w-4" />
              <span>بررسی علائم سوختگی پوشک</span>
            </button>
            <button
              type="button"
              onClick={() => navigate({ name: 'medicine-detail', slug: 'topical-zinc-vs-steroid-cream' })}
              className="inline-flex min-h-[42px] items-center gap-1.5 rounded-xl border border-[#DCE7E1] px-4 text-xs font-bold dark:border-[#23344D]"
            >
              <Pill className="h-4 w-4 text-[#105B48] dark:text-[#00D7C7]" />
              <span>تفاوت زینک اکساید و پماد کورتونی</span>
            </button>
          </div>
        </div>

        <div className="flex justify-center">
          <img
            src="/assets/images/ic_brand_skin.png"
            alt="مراقبت پوست و ناحیه پوشک کودک"
            className="h-44 w-44 rounded-3xl border border-[#DCE7E1] bg-[#F5F9F6] object-contain p-4 dark:border-[#23344D] dark:bg-[#16243B]"
          />
        </div>
      </section>

      <SeverityAlert
        severity="danger"
        title="هشدار حیاتی: ممنوعیت مصرف خودسرانه کورتون‌های قوی زیر پوشک"
        body="پوست مرطوب و پوشیده‌شده زیر پوشک جذب کورتون موضعی (مانند بتامتازون یا کلوبتازول) را چند برابر می‌کند و می‌تواند به نازک شدن پوست، تشدید عفونت قارچی یا عوارض هورمونی منجر شود. استفاده از پودر تالک نیز به دلیل خطر استنشاق ریوی در نوزادان ممنوع است."
      />

      {/* Related Medication Shortcuts */}
      <section className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        {[
          {
            slug: 'zinc-oxide-cream',
            title: 'کرم محافظ زینک اکساید',
            desc: 'سد فیزیکی استاندارد در برابر رطوبت و تحریک پوست',
          },
          {
            slug: 'petrolatum-barrier',
            title: 'وازلین دارویی / اوسرین خالص',
            desc: 'نرم‌کننده بدون عطر برای پیشگیری و پوست حساس',
          },
          {
            slug: 'topic-detail',
            articleSlug: 'diaper-rash-care',
            title: 'مقاله کامل سوختگی پوشک',
            desc: 'تفکیک التهاب تماسی از عفونت قارچی کاندیدا',
          },
        ].map((card) => (
          <button
            key={card.title}
            type="button"
            onClick={() =>
              card.articleSlug
                ? navigate({ name: 'topic-detail', slug: card.articleSlug })
                : navigate({ name: 'medicine-detail', slug: card.slug })
            }
            className="rounded-3xl border border-[#DCE7E1] bg-white p-4 text-right transition-colors hover:border-[#105B48] dark:border-[#23344D] dark:bg-[#111C2E] dark:hover:border-[#00D7C7]"
          >
            <div className="flex items-center gap-2 text-xs font-bold text-[#105B48] dark:text-[#00D7C7]">
              <ShieldCheck className="h-4 w-4" />
              <span>راهنمای تخصصی</span>
            </div>
            <h3 className="mt-1.5 text-sm font-extrabold text-[#122823] dark:text-[#EDF4F7]">{card.title}</h3>
            <p className="mt-1 text-xs leading-5 text-[#546963] dark:text-[#9EB3C4]">{card.desc}</p>
          </button>
        ))}
      </section>

      <ProductComparisonSection />

      <SourceBadge sourceIds={['applied12_diaper']} reviewStatus="VERIFIED" />
    </div>
  );
};
