import type { AppRoute } from './routes.ts';

export type FeatureId = Exclude<AppRoute['name'], 'home' | 'medicine-detail' | 'topic-detail'>;
export type FeatureGroupId = 'care' | 'tracking' | 'learn' | 'profile';
export type FeatureIconKey =
  | 'Stethoscope' | 'Calculator' | 'ScanLine' | 'Scale' | 'Syringe'
  | 'ClipboardList' | 'HelpCircle' | 'Pill' | 'Utensils' | 'Sparkles'
  | 'Baby' | 'Cpu' | 'BookOpen' | 'UserCheck' | 'QrCode';

export interface FeatureDefinition<Id extends FeatureId = FeatureId> {
  labelFa: string;
  descriptionFa: string;
  route: Extract<AppRoute, { name: Id }>;
  iconKey: FeatureIconKey;
  imagePath?: string;
  group: FeatureGroupId;
  availability: 'local' | 'local-draft';
}

export type FeatureCatalog = { [Id in FeatureId]: FeatureDefinition<Id> };

export const FEATURES: FeatureCatalog = {
  symptoms: {
    labelFa: 'بررسی علائم',
    descriptionFa: 'مرور آموزشی علائم و نشانه‌های خطر',
    route: { name: 'symptoms' },
    iconKey: 'Stethoscope',
    imagePath: '/assets/images/mano_symptoms_orbit.webp',
    group: 'care',
    availability: 'local',
  },
  'fever-dose': {
    labelFa: 'محاسبه آموزشی حجم دارو',
    descriptionFa: 'نمایش حجم بر پایه سن، وزن و غلظت واردشده',
    route: { name: 'fever-dose' },
    iconKey: 'Calculator',
    imagePath: '/assets/images/ic_brand_fever.png',
    group: 'care',
    availability: 'local',
  },
  'syrup-scanner': {
    labelFa: 'برچسب دارو',
    descriptionFa: 'ورود دستی و تطبیق غلظت',
    route: { name: 'syrup-scanner' },
    iconKey: 'ScanLine',
    imagePath: '/assets/images/ic_brand_scanner.png',
    group: 'care',
    availability: 'local',
  },
  growth: {
    labelFa: 'رشد کودک',
    descriptionFa: 'ثبت قد و وزن و مرور نقاط عطف رشد',
    route: { name: 'growth' },
    iconKey: 'Scale',
    imagePath: '/assets/images/mano_growth_orbit.webp',
    group: 'tracking',
    availability: 'local',
  },
  vaccines: {
    labelFa: 'جدول واکسن‌ها',
    descriptionFa: 'مرور برنامه واکسن و ثبت نوبت‌ها در این دستگاه',
    route: { name: 'vaccines' },
    iconKey: 'Syringe',
    imagePath: '/assets/images/ic_brand_vaccines.png',
    group: 'tracking',
    availability: 'local',
  },
  'treatment-plan': {
    labelFa: 'برنامه مراقبت',
    descriptionFa: 'ثبت برنامه و مرور نوبت‌های روزانه',
    route: { name: 'treatment-plan' },
    iconKey: 'ClipboardList',
    imagePath: '/assets/images/ic_brand_reminders.png',
    group: 'tracking',
    availability: 'local',
  },
  medicines: {
    labelFa: 'راهنمای داروها',
    descriptionFa: 'مرور اطلاعات داروها و هم‌پوشانی مواد مؤثره',
    route: { name: 'medicines' },
    iconKey: 'Pill',
    imagePath: '/assets/images/ic_brand_medicines.png',
    group: 'care',
    availability: 'local',
  },
  faq: {
    labelFa: 'پرسش‌های متداول',
    descriptionFa: 'مرور پرسش‌ها و پاسخ‌های آموزشی',
    route: { name: 'faq' },
    iconKey: 'HelpCircle',
    imagePath: '/assets/images/mano_questions_orbit.webp',
    group: 'learn',
    availability: 'local',
  },
  nutrition: {
    labelFa: 'تغذیه کودک',
    descriptionFa: 'مرور نکات تغذیه؛ ساخت برنامه آنلاین فعال نیست',
    route: { name: 'nutrition' },
    iconKey: 'Utensils',
    group: 'learn',
    availability: 'local',
  },
  'supplements-hygiene': {
    labelFa: 'مکمل و بهداشت کودک',
    descriptionFa: 'مرور نکات مکمل‌ها و بهداشت کودک',
    route: { name: 'supplements-hygiene' },
    iconKey: 'Sparkles',
    group: 'care',
    availability: 'local',
  },
  skin: {
    labelFa: 'پوست و سوختگی پوشک',
    descriptionFa: 'مرور نکات مراقبت پوست و سوختگی پوشک',
    route: { name: 'skin' },
    iconKey: 'Baby',
    group: 'care',
    availability: 'local',
  },
  devices: {
    labelFa: 'تجهیزات خانگی کودک',
    descriptionFa: 'آشنایی با دماسنج، سرنگ و ابزارهای خانگی',
    route: { name: 'devices' },
    iconKey: 'Cpu',
    group: 'care',
    availability: 'local',
  },
  topics: {
    labelFa: 'کتابخانه مقالات',
    descriptionFa: 'مقالات آموزشی مراقبت از کودک',
    route: { name: 'topics' },
    iconKey: 'BookOpen',
    group: 'learn',
    availability: 'local',
  },
  'ask-pharmacist': {
    labelFa: 'یادداشت پرسش دارویی',
    descriptionFa: 'ذخیره پیش‌نویس در این دستگاه؛ برای داروساز ارسال نمی‌شود',
    route: { name: 'ask-pharmacist' },
    iconKey: 'UserCheck',
    group: 'learn',
    availability: 'local-draft',
  },
  'care-cards': {
    labelFa: 'کارت‌های مراقبت',
    descriptionFa: 'ساخت و ذخیره خلاصه قابل چاپ از ثبت‌های این دستگاه',
    route: { name: 'care-cards' },
    iconKey: 'QrCode',
    group: 'tracking',
    availability: 'local',
  },
  'my-child': {
    labelFa: 'پرونده کودک',
    descriptionFa: 'مرور و ویرایش پرونده ذخیره‌شده در این دستگاه',
    route: { name: 'my-child' },
    iconKey: 'Baby',
    group: 'profile',
    availability: 'local',
  },
};

export interface FeatureGroup {
  id: FeatureGroupId;
  titleFa: string;
  featureIds: readonly FeatureId[];
}

export const FEATURE_GROUPS: readonly FeatureGroup[] = [
  {
    id: 'care',
    titleFa: 'مراقبت و دارو',
    featureIds: ['symptoms', 'fever-dose', 'syrup-scanner', 'medicines', 'supplements-hygiene', 'skin', 'devices'],
  },
  {
    id: 'tracking',
    titleFa: 'ثبت و پیگیری',
    featureIds: ['growth', 'vaccines', 'treatment-plan', 'care-cards'],
  },
  {
    id: 'learn',
    titleFa: 'آموزش و پرسش',
    featureIds: ['nutrition', 'topics', 'faq', 'ask-pharmacist'],
  },
  {
    id: 'profile',
    titleFa: 'پرونده کودک',
    featureIds: ['my-child'],
  },
];

export const HUB_FEATURE_IDS: readonly FeatureId[] = FEATURE_GROUPS.flatMap((group) => group.featureIds);

export const HOME_TOOL_FEATURE_IDS: readonly FeatureId[] = [
  'symptoms', 'fever-dose', 'syrup-scanner', 'growth', 'vaccines', 'treatment-plan', 'faq', 'medicines',
];

export const HOME_SECONDARY_FEATURE_IDS: readonly FeatureId[] = [
  'nutrition', 'supplements-hygiene', 'skin', 'devices', 'topics', 'ask-pharmacist', 'care-cards',
];
