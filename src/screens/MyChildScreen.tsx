import React, { useState } from 'react';
import {
  ArrowRight,
  Baby,
  Bookmark,
  BookOpen,
  FileText,
  Pill,
  Save,
  ShieldCheck,
  Trash2,
  Utensils,
} from 'lucide-react';
import { AGE_BANDS } from '../domain/models.ts';
import { ageBandFromMonths, resolveClinicalAge } from '../domain/clinicalAge.ts';
import { getArticleBySlug, getMedicationBySlug } from '../data/contentRepository.ts';
import { toPersianDigits } from '../domain/persianNormalizer.ts';
import { parseClinicalDecimal } from '../domain/clinicalNumberInput.ts';
import { useMano } from '../state/ManoStore.tsx';
import { SeverityAlert } from '../components/SeverityAlert.tsx';
import { LocalBackupPanel } from '../components/LocalBackupPanel.tsx';
import { OnlineAccountPanel } from '../components/OnlineAccountPanel.tsx';

export const MyChildScreen: React.FC = () => {
  const {
    childProfile,
    vaccineBirthDateEpochMs,
    updateChildProfile,
    savedVisitDrafts,
    deleteVisitDraft,
    nutritionPlans,
    savedSyrupScans,
    bookmarkedSlugs,
    toggleBookmark,
    clearAllSavedLocalData,
    navigate,
  } = useMano();

  const [aliasFa, setAliasFa] = useState(childProfile.aliasFa);
  const [ageMonthsStr, setAgeMonthsStr] = useState(
    childProfile.ageMonthsExact !== null && childProfile.ageMonthsExact !== undefined
      ? String(childProfile.ageMonthsExact)
      : '',
  );
  const [sex, setSex] = useState<'boy' | 'girl' | 'unset'>(childProfile.sex ?? 'unset');
  const [weightKgText, setWeightKgText] = useState(childProfile.weightKgText);
  const [allergiesFa, setAllergiesFa] = useState(childProfile.allergiesFa);
  const [confirmClear, setConfirmClear] = useState(false);
  const [clearBirthDate, setClearBirthDate] = useState(false);
  const [ageError, setAgeError] = useState<string | null>(null);
  const currentAge = resolveClinicalAge(childProfile, vaccineBirthDateEpochMs);
  const hasBirthDate = childProfile.birthDateEpochMs != null || vaccineBirthDateEpochMs != null;
  const parsedAge = parseClinicalDecimal(ageMonthsStr);
  const draftAge = resolveClinicalAge({
    ...childProfile,
    ageMonthsExact: parsedAge,
    birthDateOrAgeFa: '',
    birthDateEpochMs: clearBirthDate ? null : childProfile.birthDateEpochMs,
  }, clearBirthDate ? null : vaccineBirthDateEpochMs);
  const derivedBand = ageBandFromMonths(draftAge.ageMonths);

  const handleSaveProfile = (e: React.FormEvent) => {
    e.preventDefault();
    const parsedMonths = parseClinicalDecimal(ageMonthsStr);
    if (ageMonthsStr.trim() && (parsedMonths === null || parsedMonths < 0 || parsedMonths > 144)) {
      setAgeError('سن را به صورت عدد واقعی از ۰ تا ۱۴۴ ماه وارد کنید؛ اعشار سن به بالا گرد نمی‌شود.');
      return;
    }
    if (draftAge.status === 'conflict' || draftAge.status === 'invalid') {
      setAgeError(draftAge.messageFa);
      return;
    }
    setAgeError(null);
    updateChildProfile({
      aliasFa: aliasFa.trim() || 'کودک من',
      ageBand: derivedBand ?? childProfile.ageBand,
      ageMonthsExact: draftAge.source === 'birth-date' ? null : parsedMonths,
      birthDateOrAgeFa: '',
      birthDateEpochMs: clearBirthDate ? null : childProfile.birthDateEpochMs ?? vaccineBirthDateEpochMs,
      sex: sex === 'unset' ? null : sex,
      weightKgText: weightKgText.trim(),
      allergiesFa: allergiesFa.trim(),
    });
    if (draftAge.source === 'birth-date') setAgeMonthsStr('');
    setClearBirthDate(false);
  };

  return (
    <div className="space-y-6" data-testid="my-child-screen">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-extrabold text-[#122823] dark:text-[#EDF4F7]">
            پرونده سلامت کودک و آرشیو ذخیره‌شده
          </h1>
          <p className="mt-1 text-xs text-[#546963] dark:text-[#9EB3C4]">
            تنها مشخصات واقعی کودک را ثبت کنید؛ اطلاعات دقیق سن، وزن و حساسیت برای ایمنی تمام محاسبات استفاده می‌شوند.
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

      {/* Privacy Notice */}
      <SeverityAlert
        severity="info"
        title="سوابق و پیش‌نویس‌ها در مرورگر همین دستگاه ذخیره می‌شوند"
        body="این پرونده مستقل از حساب آنلاین است. ورود یا خروج از حساب، این ثبت‌ها را به سرور منتقل یا از دستگاه پاک نمی‌کند. در دستگاه مشترک، از اطلاعات واقعی بیمار استفاده نکنید. ارسال آنلاین فقط از فرم جداگانه و با رضایت شما انجام می‌شود؛ پاسخ هوش مصنوعی در این نسخه فعال نیست."
      />
      <OnlineAccountPanel />
      {(currentAge.status === 'conflict' || currentAge.status === 'invalid') && !clearBirthDate ? (
        <SeverityAlert severity="caution" title="سن پرونده نیاز به اصلاح دارد" body={currentAge.messageFa ?? ''} testId="age-profile-conflict" />
      ) : null}

      {/* Profile Editor */}
      <form
        onSubmit={handleSaveProfile}
        className="space-y-4 rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]"
      >
        <div className="flex items-center justify-between gap-2">
          <h2 className="flex items-center gap-2 text-base font-extrabold text-[#105B48] dark:text-[#00D7C7]">
            <Baby className="h-5 w-5" />
            <span>مشخصات پایه کودک</span>
          </h2>
          <span className="text-xs text-[#546963] dark:text-[#9EB3C4]">{childProfile.updatedAtLabelFa}</span>
        </div>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <div>
            <label htmlFor="child-alias" className="block text-xs font-bold">
              نام مستعار کودک (مثلاً مهراد)
            </label>
            <input
              id="child-alias"
              type="text"
              value={aliasFa}
              onChange={(e) => setAliasFa(e.target.value)}
              className="mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
            />
          </div>

          <div>
            <label htmlFor="child-age-band" className="block text-xs font-bold">
              رده سنی بر پایه سن واقعی
            </label>
            <select
              id="child-age-band"
              value={derivedBand ?? ''}
              disabled
              className="mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
            >
              <option value="">سن ثبت نشده یا نیازمند اصلاح است</option>
              {AGE_BANDS.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.labelFa}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="child-age-months" className="block text-xs font-bold">
              سن دقیق به ماه (اختیاری)
            </label>
            <input
              id="child-age-months"
              type="text"
              inputMode="decimal"
              value={ageMonthsStr}
              onChange={(e) => {
                setAgeMonthsStr(e.target.value);
                setAgeError(null);
              }}
              placeholder="مثلاً ۱۸"
              className="mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
            />
          </div>

          <div>
            <label htmlFor="child-sex" className="block text-xs font-bold">
              جنسیت بیولوژیک (برای استاندارد WHO)
            </label>
            <select
              id="child-sex"
              value={sex}
              onChange={(e) => setSex(e.target.value as 'boy' | 'girl' | 'unset')}
              className="mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
            >
              <option value="unset">انتخاب‌نشده</option>
              <option value="girl">دختر</option>
              <option value="boy">پسر</option>
            </select>
          </div>

          <div>
            <label htmlFor="child-weight" className="block text-xs font-bold">
              وزن فعلی (کیلوگرم)
            </label>
            <input
              id="child-weight"
              type="text"
              inputMode="decimal"
              value={weightKgText}
              onChange={(e) => setWeightKgText(e.target.value)}
              placeholder="مثلاً ۱۱٫۵"
              className="mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
            />
          </div>

          <div>
            <label htmlFor="child-allergies" className="block text-xs font-bold">
              حساسیت دارویی یا غذایی
            </label>
            <input
              id="child-allergies"
              type="text"
              value={allergiesFa}
              onChange={(e) => setAllergiesFa(e.target.value)}
              placeholder="مثلاً حساسیت به پروتئین گاوی یا فاقد حساسیت"
              className="mt-1 min-h-[44px] w-full rounded-xl border border-[#DCE7E1] bg-[#F5F9F6] px-3 text-sm dark:border-[#23344D] dark:bg-[#16243B]"
            />
          </div>
        </div>

        {hasBirthDate ? (
          <div className="space-y-2 rounded-2xl border border-[#DCE7E1] p-3 text-xs leading-6 dark:border-[#23344D]">
            <p>تاریخ تولد در پرونده واکسیناسیون ثبت شده است. با نگه‌داشتن آن، سن از تاریخ تولد محاسبه می‌شود و سن عددی جداگانه ذخیره نمی‌شود.</p>
            <label className="flex items-start gap-2">
              <input type="checkbox" data-testid="child-clear-birthdate" checked={clearBirthDate} onChange={(e) => { setClearBirthDate(e.target.checked); setAgeError(null); }} className="mt-1 h-5 w-5 accent-[#105B48]" />
              <span>حذف تاریخ تولد ثبت‌شده و استفاده از سن عددی را تأیید می‌کنم؛ زمان‌بندی واکسن تا ثبت دوباره تاریخ تولد غیرفعال می‌شود.</span>
            </label>
          </div>
        ) : null}
        {ageError ? <SeverityAlert severity="caution" title="سن ذخیره نشد" body={ageError} testId="child-age-error" /> : null}

        <button
          type="submit"
          className="inline-flex min-h-[44px] items-center gap-2 rounded-xl bg-[#105B48] px-5 text-xs font-bold text-white dark:bg-[#00D7C7] dark:text-[#0B1220]"
        >
          <Save className="h-4 w-4" />
          <span>ذخیره مشخصات کودک</span>
        </button>
      </form>

      <LocalBackupPanel />

      {/* Bookmarks */}
      <section className="space-y-3 rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]">
        <h2 className="flex items-center gap-2 text-sm font-extrabold text-[#122823] dark:text-[#EDF4F7]">
          <Bookmark className="h-4 w-4 text-[#105B48] dark:text-[#00D7C7]" />
          <span>مطالب و داروهای نشان‌شده ({toPersianDigits(bookmarkedSlugs.length)})</span>
        </h2>

        {bookmarkedSlugs.length === 0 ? (
          <p className="text-xs text-[#546963] dark:text-[#9EB3C4]">
            هنوز مقاله‌ای را نشان نکرده‌اید. از کتابخانه مقالات یا دارونامه می‌توانید موارد مهم را ذخیره کنید.
          </p>
        ) : (
          <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
            {bookmarkedSlugs.map((slug) => {
              const art = getArticleBySlug(slug);
              const med = getMedicationBySlug(slug);
              const label = art?.titleFa || med?.nameFa || slug;
              return (
                <div
                  key={slug}
                  className="flex items-center justify-between gap-2 rounded-2xl border border-[#DCE7E1] bg-[#F5F9F6] px-3.5 py-2.5 dark:border-[#23344D] dark:bg-[#16243B]"
                >
                  <button
                    type="button"
                    onClick={() =>
                      art
                        ? navigate({ name: 'topic-detail', slug })
                        : med
                          ? navigate({ name: 'medicine-detail', slug })
                          : navigate({ name: 'topics' })
                    }
                    className="flex items-center gap-2 text-xs font-bold text-[#105B48] hover:underline dark:text-[#00D7C7]"
                  >
                    {art ? <BookOpen className="h-4 w-4" /> : <Pill className="h-4 w-4" />}
                    <span>{label}</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => toggleBookmark(slug)}
                    aria-label="حذف نشان"
                    className="text-xs text-[#B32334]"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              );
            })}
          </div>
        )}
      </section>

      {/* Saved Visit Drafts & Nutrition Plans & Syrup Scans */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <section className="space-y-3 rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]">
          <h2 className="flex items-center gap-2 text-sm font-extrabold">
            <FileText className="h-4 w-4 text-[#105B48] dark:text-[#00D7C7]" />
            <span>پیش‌نویس‌های ویزیت پزشک ({toPersianDigits(savedVisitDrafts.length)})</span>
          </h2>
          {savedVisitDrafts.length === 0 ? (
            <p className="text-xs text-[#546963] dark:text-[#9EB3C4]">
              هنوز خلاصه ویزیتی ذخیره نشده است. از بالای بخش علائم یا داروها می‌توانید خلاصه ویزیت بسازید.
            </p>
          ) : (
            <div className="space-y-2">
              {savedVisitDrafts.map((d) => (
                <div
                  key={d.id}
                  className="rounded-2xl border border-[#DCE7E1] bg-[#F5F9F6] p-3 text-xs leading-5 dark:border-[#23344D] dark:bg-[#16243B]"
                >
                  <div className="flex items-center justify-between font-bold">
                    <span>{d.titleFa}</span>
                    <button
                      type="button"
                      onClick={() => deleteVisitDraft(d.id)}
                      aria-label="حذف پیش‌نویس"
                      className="text-[#B32334]"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </div>
                  <p className="mt-1 whitespace-pre-line text-[#546963] dark:text-[#9EB3C4]">{d.draftTextFa}</p>
                </div>
              ))}
            </div>
          )}
        </section>

        <section className="space-y-3 rounded-3xl border border-[#DCE7E1] bg-white p-5 dark:border-[#23344D] dark:bg-[#111C2E]">
          <h2 className="flex items-center gap-2 text-sm font-extrabold">
            <Utensils className="h-4 w-4 text-[#105B48] dark:text-[#00D7C7]" />
            <span>تاریخچه تغذیه و اسکن شربت</span>
          </h2>
          <div className="space-y-2 text-xs">
            <div className="flex items-center justify-between rounded-2xl bg-[#F5F9F6] p-3 dark:bg-[#16243B]">
              <span>برنامه‌های غذایی ذخیره‌شده:</span>
              <strong className="tabular-nums">{toPersianDigits(nutritionPlans.length)} مورد</strong>
            </div>
            <div className="flex items-center justify-between rounded-2xl bg-[#F5F9F6] p-3 dark:bg-[#16243B]">
              <span>بطری‌های شربت اسکن‌شده و تاییدشده:</span>
              <strong className="tabular-nums">{toPersianDigits(savedSyrupScans.length)} مورد</strong>
            </div>
            <div className="flex flex-wrap gap-2 pt-1">
              <button
                type="button"
                onClick={() => navigate({ name: 'nutrition' })}
                className="min-h-[38px] rounded-xl border border-[#DCE7E1] px-3 font-bold dark:border-[#23344D]"
              >
                مشاور تغذیه
              </button>
              <button
                type="button"
                onClick={() => navigate({ name: 'syrup-scanner' })}
                className="min-h-[38px] rounded-xl border border-[#DCE7E1] px-3 font-bold dark:border-[#23344D]"
              >
                اسکنر شربت
              </button>
            </div>
          </div>
        </section>
      </div>

      {/* Clear Local Data */}
      <section className="flex flex-wrap items-center justify-between gap-3 rounded-3xl border border-[#E7B5BC] bg-[#FDF6F6] p-5 dark:border-[#6A2C3B] dark:bg-[#2F161E]">
        <div className="flex items-center gap-2.5 text-xs">
          <ShieldCheck className="h-5 w-5 text-[#B32334] dark:text-[#FF8594]" />
          <span>پاک‌سازی کامل داده‌های ذخیره‌شده کودک روی همین مرورگر</span>
        </div>

        {confirmClear ? (
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => {
                clearAllSavedLocalData();
                setConfirmClear(false);
              }}
              className="min-h-[40px] rounded-xl bg-[#C42B3C] px-4 text-xs font-bold text-white"
            >
              تایید حذف همه‌چیز
            </button>
            <button
              type="button"
              onClick={() => setConfirmClear(false)}
              className="min-h-[40px] rounded-xl border border-[#DCE7E1] bg-white px-3 text-xs font-bold dark:border-[#23344D] dark:bg-[#111C2E]"
            >
              انصراف
            </button>
          </div>
        ) : (
          <button
            type="button"
            onClick={() => setConfirmClear(true)}
            className="min-h-[40px] rounded-xl border border-[#C42B3C] px-4 text-xs font-bold text-[#B32334] dark:text-[#FF8594]"
          >
            پاک‌سازی کامل داده‌های محلی
          </button>
        )}
      </section>
    </div>
  );
};
