export interface ScientificProductOption {
  nameFa: string;
  technicalSpecFa: string;
  clinicalVerdictFa: string;
  prosFa: string[];
  cautionsFa: string[];
  isGoldStandard: boolean;
}

export interface ScientificComparisonCategory {
  id: string;
  chipLabelFa: string;
  titleFa: string;
  summaryFa: string;
  sourceCitationFa: string;
  options: ScientificProductOption[];
}

export const SCIENTIFIC_PRODUCT_COMPARISONS: ScientificComparisonCategory[] = [
  {
    id: 'dosing_tools',
    chipLabelFa: 'ابزار اندازه‌گیری دوز',
    titleFa: 'مقایسه علمی ابزارهای اندازه‌گیری داروی مایع کودک',
    summaryFa:
      'خطای حجمی قاشق‌های خانگی یکی از شایع‌ترین علل مسمومیت یا کم‌اثر شدن دارو در کودکان است.',
    sourceCitationFa: 'AAP / FDA / ISMP — Safe Pediatric Liquid Medication Dosing',
    options: [
      {
        nameFa: 'سرنگ خوراکی مدرج به میلی‌لیتر (mL)',
        technicalSpecFa: 'دقت ۰٫۱ تا ۰٫۲ میلی‌لیتر • استاندارد طلایی بیمارستانی و خانگی',
        clinicalVerdictFa: 'انتخاب اول و ایمن‌ترین ابزار برای تمام شربت‌ها و سوسپانسیون‌ها',
        prosFa: [
          'خوانش دقیق خط زیرین منیسک مایع بر حسب mL',
          'هدایت آرام مایع به سمت داخل گونه کودک (کاهش خطر پریدن در گلو)',
        ],
        cautionsFa: [
          'درمانگر یا والد باید کلاهک سرنگ را پیش از مصرف کاملاً جدا کند تا خطر بلع کلاهک وجود نداشته باشد.',
        ],
        isGoldStandard: true,
      },
      {
        nameFa: 'قطره‌چکان مدرج اختصاصی داخل جعبه',
        technicalSpecFa: 'مخصوص قطره‌های غلیظ نوزادی (مثلاً استامینوفن ۱۰۰mg/mL یا قطره آهن)',
        clinicalVerdictFa: 'مناسب فقط برای همان بطری دارو که قطره‌چکان داخل جعبه‌اش قرار دارد',
        prosFa: ['طراحی‌شده متناسب با غلظت و کشش سطحی همان قطره دارویی'],
        cautionsFa: ['هرگز قطره‌چکان یک دارو را برای داروی دیگر یا به جای سرنگ شربت استفاده نکنید.'],
        isGoldStandard: false,
      },
      {
        nameFa: 'قاشق چای‌خوری، مرباخوری یا غذاخوری خانگی',
        technicalSpecFa: 'حجم غیراستاندارد (از ۲ تا ۹ میلی‌لیتر متغیر)',
        clinicalVerdictFa: 'غیرمجاز برای داروی کودک — خطر خطای ۲ تا ۳ برابری دوز',
        prosFa: [],
        cautionsFa: [
          'به دلیل تفاوت اندازه قاشق‌های خانگی، احتمال اوردوز یا دوز ناکافی بسیار بالاست.',
          'ریزش دارو از لبه قاشق هنگام حرکت کودک دقت مصرف را از بین می‌برد.',
        ],
        isGoldStandard: false,
      },
    ],
  },
  {
    id: 'antipyretic_forms',
    chipLabelFa: 'غلظت تب‌برها',
    titleFa: 'مقایسه علمی اشکال دارویی استامینوفن و ایبوپروفن کودکان',
    summaryFa:
      'جابه‌جایی حجم قطره و شربت بدون توجه به غلظت (mg/mL) خطرناک‌ترین خطای دارویی خانگی است.',
    sourceCitationFa: 'AAP / FDA — Pediatric Antipyretic Concentration Safety',
    options: [
      {
        nameFa: 'شربت / سوسپانسیون استاندارد استامینوفن (120mg/5mL یا 160mg/5mL)',
        technicalSpecFa: '۲۴ تا ۳۲ میلی‌گرم در هر ۱ میلی‌لیتر (سنجش با سرنگ خوراکی mL)',
        clinicalVerdictFa: 'فرم استاندارد کودکان با قابلیت تنظیم دقیق بر اساس وزن واقعی',
        prosFa: [
          'حجم ایمن‌تر و حاشیه خطای کمتر نسبت به قطره‌های فوق‌غلیظ',
          'تطبیق آسان با سرنگ خوراکی ۵ میلی‌لیتری',
        ],
        cautionsFa: ['پیش از کشیدن دارو در سرنگ، تفاوت ۱۲۰mg/5mL و ۱۶۰mg/5mL روی جعبه را بررسی کنید.'],
        isGoldStandard: true,
      },
      {
        nameFa: 'قطره خوراکی غلیظ استامینوفن (100mg/1mL)',
        technicalSpecFa: '۱۰۰ میلی‌گرم در هر ۱ میلی‌لیتر (حدود ۴ برابر غلیظ‌تر از شربت!)',
        clinicalVerdictFa: 'صرفاً برای شیرخواران کوچک با حجم بسیار کم و با قطره‌چکان مدرج خودش',
        prosFa: ['نیاز به حجم مایع کمتر برای شیرخواران'],
        cautionsFa: [
          'هرگز حجم شربت (مثلاً ۵ میلی‌لیتر) را با قطره ۱۰۰mg/mL به کودک ندهید؛ موجب مسمومیت شدید کبدی می‌شود.',
        ],
        isGoldStandard: false,
      },
      {
        nameFa: 'سوسپانسیون ایبوپروفن کودکان (100mg/5mL)',
        technicalSpecFa: '۲۰ میلی‌گرم در هر ۱ میلی‌لیتر • فاصله دوز ۶ تا ۸ ساعت',
        clinicalVerdictFa: 'فقط برای کودکان بالای ۶ ماه و بدون کم‌آبی یا استفراغ مکرر',
        prosFa: ['اثر ضدالتهابی و کنترل تب طولانی‌تر در کودکان بالای ۶ ماه'],
        cautionsFa: ['در نوزادان زیر ۶ ماه، کودک دچار کم‌آبی، اسهال/استفراغ شدید یا مشکلات کلیوی ممنوع است.'],
        isGoldStandard: false,
      },
    ],
  },
  {
    id: 'hydration_ors',
    chipLabelFa: 'آبرسانی و ORS',
    titleFa: 'مقایسه علمی محلول‌های جبران آب و الکترولیت در اسهال و استفراغ',
    summaryFa:
      'تعادل سدیم و گلوکز در روده کودک تنها با اسمولاریته استاندارد قابل بازیابی ایمن است.',
    sourceCitationFa: 'WHO / UNICEF — Low-Osmolarity Oral Rehydration Salts Guideline',
    options: [
      {
        nameFa: 'محلول استاندارد ORS کم‌اسمولاریته (WHO Formula)',
        technicalSpecFa: 'اسمولاریته حدود ۲۴۵ mOsm/L • نسبت دقیق سدیم و گلوکز',
        clinicalVerdictFa: 'استاندارد طلایی پیشگیری و درمان کم‌آبی خفیف تا متوسط کودکان',
        prosFa: [
          'جذب فعال آب و الکترولیت از دیواره روده حتی در حضور اسهال',
          'کاهش نیاز به سرم‌تراپی وریدی در صورت مصرف جرعه‌جرعه و منظم',
        ],
        cautionsFa: [
          'پودر ORS باید دقیقاً در حجم آب جوشیده سردشده درج‌شده روی بسته حل شود (نه غلیظ‌تر و نه رقیق‌تر).',
        ],
        isGoldStandard: true,
      },
      {
        nameFa: 'آبمیوه‌های صنعتی، نوشابه یا محلول‌های خانگی پرشکر',
        technicalSpecFa: 'اسمولاریته بسیار بالا (>600 mOsm/L) و سدیم ناکافی',
        clinicalVerdictFa: 'نامناسب و تشدیدکننده اسهال اسموتیک در کودک',
        prosFa: [],
        cautionsFa: ['قند بالا آب را به داخل روده می‌کشد و اسهال و کم‌آبی کودک را تشدید می‌کند.'],
        isGoldStandard: false,
      },
    ],
  },
  {
    id: 'skin_barrier',
    chipLabelFa: 'محافظ پوست و پوشک',
    titleFa: 'مقایسه علمی فرآورده‌های محافظ سد پوستی و تعویض پوشک',
    summaryFa:
      'پوست ناحیه پوشک شیرخوار نازک و نفوذپذیر است و نیاز به سد محافظ بی‌اثر و فاقد عطر دارد.',
    sourceCitationFa: 'AAD / NICE — Neonatal & Infant Diaper Dermatitis Care',
    options: [
      {
        nameFa: 'خمیر/پماد زینک اکساید (Zinc Oxide) یا وازلین خالص دارویی',
        technicalSpecFa: 'فاقد عطر، الکل و نگهدارنده‌های حساسیت‌زا • سد فیزیکی رطوبت',
        clinicalVerdictFa: 'استاندارد علمی محافظت و ترمیم سوختگی ساده پوشک',
        prosFa: [
          'ایجاد لایه محافظ در برابر آنزیم‌های تحریک‌کننده ادرار و مدفوع',
          'عدم جذب سیستمیک و ایمنی بالا در مصرف روزانه',
        ],
        cautionsFa: [
          'در ضایعات قارچی شدید (قرمزی چین‌ها با دانه‌های اقماری) یا عفونت باکتریایی، معاینه پزشک لازم است.',
        ],
        isGoldStandard: true,
      },
      {
        nameFa: 'پمادهای کورتونی قوی یا ترکیبی خودسرانه (مثل بتامتازون/کلوتریمازول)',
        technicalSpecFa: 'جذب پوستی چندبرابری در ناحیه بسته پوشک',
        clinicalVerdictFa: 'مصرف خودسرانه بدون نسخه پزشک اکیداً ممنوع است',
        prosFa: [],
        cautionsFa: ['نازک شدن پوست، جذب سیستمیک کورتون و تشدید عفونت‌های قارچی در محیط مرطوب پوشک.'],
        isGoldStandard: false,
      },
    ],
  },
  {
    id: 'respiratory_devices',
    chipLabelFa: 'وسایل تنفسی و بخور',
    titleFa: 'مقایسه علمی وسایل مرطوب‌کننده هوا و داروهای استنشاقی کودک',
    summaryFa: 'انتخاب وسیله مناسب تنفسی از سوختگی با بخار داغ و رسوب ناکافی دارو جلوگیری می‌کند.',
    sourceCitationFa: 'AAP / GINA — Pediatric Aerosol & Humidifier Safety',
    options: [
      {
        nameFa: 'آسان‌نفس (Spacer) ماسک‌دار یا نبولایزر استاندارد با سرم نمکی',
        technicalSpecFa: 'ذرات میکرونی هدفمند یا مرطوب‌سازی ایمن مجاری تنفسی',
        clinicalVerdictFa: 'استاندارد علمی برای رساندن اسپری تجویزی یا نرم کردن ترشحات',
        prosFa: [
          'رسیدن دارو به ریه به جای رسوب در دهان و گلو (با اسپیسر متناسب با صورت کودک)',
          'ایمنی کامل دمایی بدون خطر سوختگی',
        ],
        cautionsFa: ['مخزن نبولایزر و اسپیسر باید طبق دستورالعمل شست‌وشو و خشک شود تا آلودگی قارچی ایجاد نشود.'],
        isGoldStandard: true,
      },
      {
        nameFa: 'دستگاه بخور داغ در اتاق کودک',
        technicalSpecFa: 'تولید بخار با دمای جوش',
        clinicalVerdictFa: 'توصیه نمی‌شود — خطر جدی سوختگی کودک با واژگونی دستگاه',
        prosFa: [],
        cautionsFa: ['آکادمی اطفال آمریکا (AAP) به دلیل موارد متعدد سوختگی شدید کودکان، بخور داغ را منع می‌کند.'],
        isGoldStandard: false,
      },
    ],
  },
];
