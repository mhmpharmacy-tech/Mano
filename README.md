# MANO Kids | مانو کیدز (Android Kotlin + Jetpack Compose)

اپلیکیشن اندرویدی **مانو کیدز** یک همراه آموزشی فارسی و راست‌به‌چپ (RTL) برای والدین و مراقبان کودک است تا نشانه‌های شایع کودکی، علائم هشدار فوری، اصول ایمنی مصرف دارو، نقاط عطف رشد و روش استفاده ایمن از وسایل مراقبتی را بشناسند.

> **وضعیت بالینی و داده‌ها:** متون آموزشی و قواعد تریاژ این برنامه جهت **آگاهی‌رسانی والدین** ارائه شده و جایگزین معاینه پزشک نیستند؛ داده‌های پروفایل کودک، تب‌سنجی، دوزها، اسکن شربت و مشاوره‌ها به صورت محلی (DataStore) ذخیره شده و پس از ورود با حساب گوگل در Cloud Firestore همگام‌سازی می‌شوند.

---

## ۱. راه‌اندازی و اجرا (Run & Test Steps)

- **پیش‌نیازها:** Android SDK (compileSdk 36 / minSdk 24)، JDK 11+ و Gradle (از `gradle` سیستم استفاده شود).
- **ساخت پروژه:**
  ```bash
  gradle :app:assembleDebug
  ```
- **اجرای تست‌های واحد و Robolectric (بدون نیاز به شبیه‌ساز):**
  ```bash
  gradle :app:testDebugUnitTest
  ```

---

## ۲. ساختار ماژول‌ها و دارایی‌های کاراکتر مانو

### ساختار کد (`app/src/main/java/com/example/`)
- `MainActivity.kt`: نقطه ورود با `enableEdgeToEdge()`، همگام‌سازی تم روز/شب (`ManoThemeMode`) و قالب RTL `ManoKidsTheme`.
- `ui/theme/`: توکن‌های رنگی مانو (`Color.kt`)، فونت محلی `Vazirmatn` (`Type.kt`) و `Theme.kt`.
- `domain/model/ManoModels.kt`: مدل‌های تایپ‌شده برای `Article`، `FaqItem`، `SymptomFlow`، `TriageRule`، `MedicationMonograph`، `DoseRule` (با محافظت احتیاطی برای نوزادان و وزن نامشخص)، `MilestoneItem`، `DeviceGuide`، `ConsultationRequestDemo`، `ChildProfileDemo`، `SavedSyrupScan` و `SourceReference`.
- `domain/util/PersianNormalizer.kt`: نرمال‌ساز فارسی (`ي/ی`، `ك/ک`، ارقام فارسی/عربی، اعراب و نیم‌فاصله/ZWNJ به طوری که «شیر خشک» و «شیرخشک» نتیجه یکسان برگردانند) و تشخیص کلیدواژه‌های پرخطر.
- `domain/util/TriageEngine.kt`: موتور قطعی و قابل بازبینی غربالگری علائم (قانون توقف فوری تب ≥ ۳۸.۰ °C در نوزاد زیر ۳ ماه و پرچم‌های قرمز تنفسی/عصبی بدون تجویز دارو).
- `data/`: `ManoContentRepository`، `ManoCloudRepository` (همگام‌سازی Firestore و مهاجرت امن داده‌های آفلاین به حساب کاربری)، `ManoPreferencesRepository` (ذخیره DataStore برای تم، پروفایل و لاگ‌های محلی، اسکن‌های شربت، واکسن‌ها، نقاط عطف و چرخ کهکشانی) و داده‌های پایه (`ManoArticlesSeed`، `ManoFaqSeed`، `ManoSourcesAndClinicalSeed`).
- `ui/components/ManoComponents.kt` و `ManoOrbitalMenu.kt`: کتابخانه کامپوننت‌های برند و چرخ کهکشانی دوحلقه‌ای متناسب با صفحه‌های `360dp`.
- `ui/screens/` و `ui/ManoApp.kt`: ناوبری `@Serializable`، صفحه خانه با کارت فوری علائم خطر و اسکنر غلظت شربت، میز سه‌بعدی با سرنگ تا `20.0 mL` و کلید تغییر تم روز/شب.

### نگاشت دارایی‌های رسمی مانو (`app/src/main/res/drawable/`)
استخراج‌شده از فایل پیوست رسمی کاربر (`MANO_Kids_Gemini_Master_Prompt_07c54aaa.pdf`):
- `mano_hero.png`: کاراکتر اصلی مانو در هیرو صفحه خانه
- `mascot_six_poses.png` + شش حالت مجزا: `mano_pose_welcome.png`، `mano_pose_explain.png`، `mano_pose_listen.png`، `mano_pose_caution.png` (هشدار و اورژانس)، `mano_pose_saved.png`، `mano_pose_wait.png`
- `mascot_medicine.png`: تصویر بخش ایمنی مصرف دارو
- `mascot_skin.png`: تصویر بخش مراقبت پوست و پوشک
- `mascot_growth.png`: تصویر بخش رشد و تکامل
- `family_support.png`: تصویر همراهی خانواده و مانو
- `mano_icon_fg.png` + آیکون‌های تطبیقی و mipmap در `res/mipmap-*/`

---

## ۳. قراردادهای API برنامه‌ریزی‌شده برای نسخه تولیدی (Planned API Contracts)

در این پیش‌نمایش اندرویدی، هیچ بک‌اند یا درگاه داروخانه‌ای شبیه‌سازی‌نشده واقعی فرض نشده است. قراردادهای زیر برای پیاده‌سازی سرور آینده در نظر گرفته شده‌اند:
- `GET /v1/content/articles?ageBand=&category=&q=` — دریافت مقالات تأییدشده همراه متادیتای بازبینی و تاریخ بازبینی بعدی
- `GET /v1/content/faqs?q=` — جست‌وجوی پرسش‌های پرتکرار تأییدشده
- `GET /v1/content/medications/{slug}` — مونوگراف دارو همراه فیلدهای اجباری غلظت و وضعیت قفل `DoseRule`
- `POST /v1/consultations` (نیازمند JWT + رضایت‌نامه صریح) — ثبت درخواست مشاوره غیرفوری برای داروساز
- `GET /v1/pharmacist/cases` و `PATCH /v1/pharmacist/cases/{id}` (نیازمند RBAC نقش داروساز) — بررسی، تأیید و پاسخ‌دهی بالینی
- `PUT /v1/guardian/child-profile` و `DELETE /v1/guardian/child-profile` — ذخیره رمزنگاری‌شده پروفایل والد با امکان خروجی و حذف کامل

---

## ۴. ریسک‌های حریم خصوصی، داده‌ها و محدودیت‌های شناخته‌شده

- **محرمانگی داده‌های کودک و کنترل دسترسی سرور:** داده‌های کاربر از طریق `CredentialManager` (ورود با گوگل) و قوانین مالکیت `firestore.rules` ایزوله می‌شوند و مسیرهای داروخانه (`pharmacies/{pharmacyId}/cases/{caseId}` و `pharmacies/{pharmacyId}/staff/{uid}`) برای کلاینت‌ها بسته هستند. برای فعال‌سازی کامل پنل داروساز، سرویس Cloud Functions باید علاوه‌بر Custom Claims، رکورد فعال داروساز (`status: "ACTIVE"`, `credentialVerified: true`, `revokedAt: null`) را به‌صورت زنده در درخواست `inbox` و درون Transaction ثبت نظر بررسی کند و تغییر وضعیت مشاوره‌ها تنها از سمت سرور (Admin SDK) مجاز باشد.
- **محدودیت‌های تأیید علمی:** تمام متون آموزشی و محاسبات راهنمای دوز باید پیش از انتشار عمومی توسط داروساز و متخصص اطفال بازبینی و امضا شوند؛ محاسبه دوز برای نوزادان زیر ۳ ماه (و ایبوپروفن زیر ۶ ماه با سن دقیق نامشخص) قفل احتیاطی دارد.
- **دامنه‌ها:** ماژول `فروشگاه` از بخش آموزشی جداست و App Links دامنه‌های `manokid.ir` و `manokid.com` صرفاً روی `HTTPS` تنظیم شده‌اند.

---

## ۵. چک‌لیست تست پذیرش (Acceptance Checklist)

- [x] **مسیر A (تب ۲ ماهه ۳۸ درجه):** هدایت فوری به ارزیابی اورژانس/۱۱۵، بدون توصیه خانگی یا دوز دارو (`ManoJourneysUnitTest` و `ExampleRobolectricTest`).
- [x] **مسیر B (یبوست کودک ۳ ساله):** دریافت راهکارهای حمایتی غیرتشخیصی، معیارهای مراجعه به پزشک و دکمه پرسش از داروساز.
- [x] **مسیر C (داروهای کودک):** نمایش راهنمای سرنگ مدرج `mL`، هشدار «شکل و غلظت دارو را بررسی کنید» و پیش‌نمایش قفل‌شده محاسبه دوز.
- [x] **مسیر D (رشد و تکامل):** چک‌لیست نقاط عطف متناسب با سن، یادآوری عدم جایگزینی با غربالگری تخصصی و placeholder صادقانه نمودار رشد.
- [x] **مسیر E (پوست و پوشک):** راهنمای سوختگی پوشک همراه تصویر مانو پوستی و مرز شفاف آموزش با خرید.
- [x] **مسیر F (جست‌وجوی FAQ):** تطابق یکسان «شیر خشک» و «شیرخشک»، فیلتر سن، باز و بسته شدن آکاردئون و بازگشت ناوبری.
- [x] **RTL و دسترسی‌پذیری:** چیدمان کامل RTL فارسی با جداسازی LTR برای واحدها و URLها، هدف‌های لمسی ≥ 48dp و تست‌پذیری کامل با `testTag`.
