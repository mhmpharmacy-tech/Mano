import React, { useState } from 'react';
import { ArrowRight, BookOpen, CheckCircle2, Cpu, ShieldAlert, Wrench } from 'lucide-react';
import { DEVICE_GUIDES } from '../data/contentRepository.ts';
import { toPersianDigits } from '../domain/persianNormalizer.ts';
import { useMano } from '../state/ManoStore.tsx';
import { InteractiveOralSyringeLabCard, InteractiveThermometerLabCard } from '../components/PediatricLabComponents.tsx';
import { ProductComparisonSection } from '../components/ProductComparisonSection.tsx';
import { SourceBadge } from '../components/SourceBadge.tsx';

export const DevicesScreen: React.FC = () => {
  const { navigate } = useMano();
  const [selectedId, setSelectedId] = useState(DEVICE_GUIDES[0].id);

  const activeGuide = DEVICE_GUIDES.find((d) => d.id === selectedId) ?? DEVICE_GUIDES[0];

  return (
    <div className="space-y-6" data-testid="devices-screen">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            تجهیزات پایش و مراقبت خانگی کودک
          </h1>
          <p className="mt-1 text-xs text-[#546963] dark:text-[#9EB3C4]">
            اصول علمی استفاده از دماسنج دیجیتال، سرنگ خوراکی، اسپیسر، نبولایزر، بخور سرد و ترازوی رشد
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

      {/* Intro Banner */}
      <section className="grid grid-cols-1 items-center gap-5 rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E] md:grid-cols-3">
        <div className="md:col-span-2 space-y-2.5">
          <h2 className="flex items-center gap-2 text-base font-extrabold text-[#105B48] dark:text-[#00D7C7]">
            <Cpu className="h-5 w-5" />
            <span>انتخاب وسیله درست، نیمی از ایمنی درمان خانگی است</span>
          </h2>
          <p className="text-xs leading-6 text-[#546963] dark:text-[#9EB3C4]">
            خطای قاشق آشپزخانه در دوز شربت، اشتباه گرفتن کاربرد دستگاه بخور سرد با نبولایزر دارویی یا استفاده از دماسنج نامناسب سن، از رایج‌ترین خطاهای خانگی هستند. راهنمای هر تجهیز را در ادامه ببینید و در آزمایشگاه تعاملی تمرین کنید.
          </p>
        </div>
        <img
          src="/assets/images/img_devices_kit.jpg"
          alt="تجهیزات خانگی مراقبت از کودک"
          className="h-40 w-full rounded-2xl border border-[#DCE7E1] object-cover dark:border-[#23344D]"
        />
      </section>

      {/* Device Guides Selector & Active Card */}
      <section className="space-y-4 rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]">
        <div className="flex flex-wrap gap-2" role="tablist" aria-label="انتخاب وسیله مراقبتی">
          {DEVICE_GUIDES.map((guide, idx) => (
            <button
              key={guide.id}
              type="button"
              role="tab"
              aria-selected={guide.id === activeGuide.id}
              onClick={() => setSelectedId(guide.id)}
              className={`min-h-[42px] rounded-xl border px-3.5 text-xs font-bold transition-colors ${
                guide.id === activeGuide.id
                  ? 'border-[#105B48] bg-[#E9F5EB] text-[#105B48] dark:border-[#00D7C7] dark:bg-[#0F333A] dark:text-[#00D7C7]'
                  : 'border-[#DCE7E1] bg-[#F5F9F6] text-[#546963] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#9EB3C4]'
              }`}
            >
              {toPersianDigits(idx + 1)}. {guide.titleFa}
            </button>
          ))}
        </div>

        <div className="space-y-4 border-t border-[#DCE7E1] pt-4 dark:border-[#23344D]">
          <div>
            <h3 className="text-base font-extrabold text-[#122823] dark:text-[#EDF4F7]">{activeGuide.titleFa}</h3>
            <p className="mt-1 text-xs leading-6 text-[#546963] dark:text-[#9EB3C4]">{activeGuide.subtitleFa}</p>
          </div>

          <div className="rounded-2xl border border-[#E8CC99] bg-[#FDF9F1] p-3.5 text-xs leading-6 text-[#7D4A06] dark:border-[#664F27] dark:bg-[#2B2112] dark:text-[#F5C169]">
            <strong>نکته تفکیک کلیدی: </strong>
            {activeGuide.distinctionNoteFa}
          </div>

          <div className="rounded-2xl bg-[#F5F9F6] p-4 dark:bg-[#16243B]">
            <h4 className="flex items-center gap-1.5 text-xs font-extrabold text-[#105B48] dark:text-[#00D7C7]">
              <CheckCircle2 className="h-4 w-4" />
              <span>چک‌لیست استفاده اصولی:</span>
            </h4>
            <ul className="mt-2 list-disc space-y-1.5 pr-5 text-xs leading-6">
              {activeGuide.checklistFa.map((item, idx) => (
                <li key={idx}>{item}</li>
              ))}
            </ul>
          </div>

          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            <div className="rounded-2xl border border-[#E7B5BC] bg-[#FDF0F1] p-3.5 text-xs leading-6 text-[#B32334] dark:border-[#6A2C3B] dark:bg-[#2F161E] dark:text-[#FF8594]">
              <div className="flex items-center gap-1.5 font-extrabold">
                <ShieldAlert className="h-4 w-4" />
                <span>خطای رایج والدین:</span>
              </div>
              <p className="mt-1">{activeGuide.commonMistakeFa}</p>
            </div>

            <div className="rounded-2xl border border-[#DCE7E1] bg-[#FAF7F0] p-3.5 text-xs leading-6 text-[#122823] dark:border-[#23344D] dark:bg-[#16243B] dark:text-[#EDF4F7]">
              <div className="flex items-center gap-1.5 font-extrabold text-[#105B48] dark:text-[#00D7C7]">
                <Wrench className="h-4 w-4" />
                <span>نگهداری و نظافت:</span>
              </div>
              <p className="mt-1">{activeGuide.cleaningStorageFa}</p>
            </div>
          </div>

          <div className="flex flex-wrap items-center justify-between gap-3">
            <SourceBadge sourceIds={activeGuide.sourceIds} reviewStatus="VERIFIED" compact />
            <button
              type="button"
              onClick={() => navigate({ name: 'topic-detail', slug: activeGuide.relatedArticleSlug })}
              className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl bg-[#105B48] px-4 text-xs font-bold text-white dark:bg-[#00D7C7] dark:text-[#0B1220]"
            >
              <BookOpen className="h-4 w-4" />
              <span>مطالعه مقاله مرتبط</span>
            </button>
          </div>
        </div>
      </section>

      {/* Interactive Practice Labs */}
      <InteractiveOralSyringeLabCard />
      <InteractiveThermometerLabCard />
      <ProductComparisonSection />
    </div>
  );
};
