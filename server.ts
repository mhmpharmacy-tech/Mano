import path from 'node:path';
import { fileURLToPath } from 'node:url';
import express, { type Request, type Response } from 'express';
import { GoogleGenAI, Type } from '@google/genai';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PORT = 3000;
const GEMINI_MODEL = 'gemini-4-hydrogen';
const REQUEST_TIMEOUT_MS = 60_000;

function getAiClient(): GoogleGenAI | null {
  const apiKey = process.env.GEMINI_API_KEY?.trim();
  if (!apiKey) return null;
  return new GoogleGenAI({
    apiKey,
    httpOptions: {
      headers: {
        'User-Agent': 'aistudio-build',
      },
    },
  });
}

function sendMissingKey(res: Response) {
  return res.status(503).json({
    error:
      'کلید سرویس هوش مصنوعی (GEMINI_API_KEY) روی سرور تنظیم نشده است. تمام محاسبه‌گرها، نمودار رشد، راهنمای علائم و ابزارهای بالینی آفلاین بدون نیاز به کلید فعال هستند.',
    code: 'GEMINI_KEY_MISSING',
  });
}

function withTimeout<T>(promise: Promise<T>, ms = REQUEST_TIMEOUT_MS): Promise<T> {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      reject(new Error('زمان پاسخ‌گویی سرویس هوش مصنوعی به پایان رسید (۶۰ ثانیه). لطفاً دوباره تلاش کنید.'));
    }, ms);
    promise.then(
      (val) => {
        clearTimeout(timer);
        resolve(val);
      },
      (err) => {
        clearTimeout(timer);
        reject(err);
      },
    );
  });
}

function cleanAllergyValue(val: unknown): string {
  return typeof val === 'string' ? val.trim() : '';
}

const NO_ALLERGY_TOKENS = new Set([
  '',
  'ندارد',
  'ثبت نشده',
  'موردی ثبت نشده',
  'هیچ',
  'ندارد/ثبت نشده',
  'خیر',
  'none',
]);

const UNDER_12M_FORBIDDEN_TERMS = ['عسل', 'شیر گاو', 'انگور کامل', 'آجیل کامل', 'مغز کامل'];

function stripBase64Prefix(raw: string): string {
  const commaIdx = raw.indexOf(',');
  if (raw.startsWith('data:') && commaIdx !== -1) {
    return raw.slice(commaIdx + 1);
  }
  return raw;
}

async function createApp() {
  const app = express();
  app.use(express.json({ limit: '12mb' }));

  app.get('/api/health', (_req: Request, res: Response) => {
    res.json({
      ok: true,
      hasGeminiKey: Boolean(process.env.GEMINI_API_KEY?.trim()),
      model: GEMINI_MODEL,
    });
  });

  // 1. Doctor Visit Draft (supports streaming SSE or JSON)
  app.post('/api/gemini/visit-draft', async (req: Request, res: Response) => {
    const ai = getAiClient();
    if (!ai) return sendMissingKey(res);

    const { promptText, stream } = req.body ?? {};
    if (typeof promptText !== 'string' || promptText.trim().length < 15) {
      return res.status(400).json({
        error: 'اطلاعات کافی برای ساخت خلاصه ویزیت پزشک ارسال نشده است.',
      });
    }

    const systemInstruction =
      'شما یک دستیار منظم‌سازی خلاصه بالینی والد برای ارائه به پزشک اطفال هستید. هرگز تشخیص پزشکی قطعی، تجویز داروی جدید یا دوز دارو صادر نکنید. فقط داده‌های ثبت‌شده را در ۵ بخش شفاف، محترمانه و فارسی معیار دسته‌بندی کنید: «خلاصه مشخصات و علت مراجعه»، «روند ۲۴ ساعته تب و دماهای ثبت‌شده»، «داروهای مصرف‌شده در ۲۴ ساعت گذشته»، «علائم بررسی‌شده و موارد نیازمند توجه»، و «پرسش‌های والد برای پزشک در مطب».';

    try {
      if (stream === true) {
        res.setHeader('Content-Type', 'text/event-stream; charset=utf-8');
        res.setHeader('Cache-Control', 'no-cache, no-transform');
        res.setHeader('Connection', 'keep-alive');

        const responseStream = await withTimeout(
          ai.models.generateContentStream({
            model: GEMINI_MODEL,
            contents: promptText.trim(),
            config: {
              systemInstruction,
              temperature: 0.25,
            },
          }),
        );

        let fullText = '';
        for await (const chunk of responseStream) {
          const delta = chunk.text ?? '';
          if (delta) {
            fullText += delta;
            res.write(`data: ${JSON.stringify({ delta, text: fullText })}\n\n`);
          }
        }
        res.write(`data: ${JSON.stringify({ done: true, text: fullText })}\n\n`);
        return res.end();
      }

      const response = await withTimeout(
        ai.models.generateContent({
          model: GEMINI_MODEL,
          contents: promptText.trim(),
          config: {
            systemInstruction,
            temperature: 0.25,
          },
        }),
      );

      const text = (response.text ?? '').trim();
      if (!text) {
        return res.status(502).json({ error: 'پاسخی از مدل دریافت نشد. لطفاً دوباره تلاش کنید.' });
      }
      return res.json({ text });
    } catch (err) {
      const message = err instanceof Error ? err.message : 'خطا در تولید خلاصه ویزیت پزشک.';
      if (res.headersSent) {
        res.write(`data: ${JSON.stringify({ error: message })}\n\n`);
        return res.end();
      }
      return res.status(500).json({ error: message });
    }
  });

  // 2. Pediatric Nutrition Advisor
  app.post('/api/gemini/nutrition', async (req: Request, res: Response) => {
    const ai = getAiClient();
    if (!ai) return sendMissingKey(res);

    const { ageMonths, weightKg, allergiesFa, dietGoalFa, childContextSummaryFa } = req.body ?? {};
    const parsedAge = typeof ageMonths === 'number' && Number.isFinite(ageMonths) ? ageMonths : null;
    const normalizedAllergy = cleanAllergyValue(allergiesFa);

    if (parsedAge === null || parsedAge < 6) {
      return res.status(400).json({
        error:
          'برای کودک زیر ۶ ماه تغذیه تکمیلی توصیه نمی‌شود و تغذیه فقط با شیر مادر یا شیر خشک طبق نظر پزشک کودک انجام می‌شود.',
      });
    }

    if (!NO_ALLERGY_TOKENS.has(normalizedAllergy)) {
      return res.status(400).json({
        error:
          'به دلیل ثبت سابقه حساسیت یا آلرژی غذایی برای کودک، دریافت برنامه غذایی از هوش مصنوعی غیرفعال است و باید مستقیماً با پزشک کودک مشورت شود.',
      });
    }

    const prompt = [
      'برنامه غذایی آموزشی، ایمن و بومی ایرانی برای کودک طراحی کن. تشخیص پزشکی یا ادعای درمان نداشته باش.',
      `سن کودک (ماه): ${parsedAge}`,
      weightKg ? `وزن کودک (کیلوگرم): ${weightKg}` : 'وزن کودک ثبت نشده است.',
      `هدف تغذیه‌ای والد: ${typeof dietGoalFa === 'string' && dietGoalFa.trim() ? dietGoalFa.trim() : 'تغذیه متعادل و رشد سالم متناسب با سن'}`,
      childContextSummaryFa ? `اطلاعات تکمیلی پرونده: ${childContextSummaryFa}` : '',
      parsedAge < 12
        ? 'تاکید حیاتی برای زیر ۱۲ ماه: مطلقاً از عسل، شیر گاو به عنوان نوشیدنی، انگور کامل، آجیل کامل یا مغز کامل استفاده نکن و بافت غذا را نرم یا پوره/انگشتی ایمن پیشنهاد بده.'
        : 'بافت غذا و اندازه سهم متناسب با سن بالای ۱۲ ماه باشد و نکات پیشگیری از خفگی رعایت شود.',
      'حداقل ۳ وعده غذایی متنوع (صبحانه، ناهار، میان‌وعده یا شام) با مواد در دسترس آشپزخانه ایرانی ارائه بده.',
    ]
      .filter(Boolean)
      .join('\n');

    try {
      const response = await withTimeout(
        ai.models.generateContent({
          model: GEMINI_MODEL,
          contents: prompt,
          config: {
            responseMimeType: 'application/json',
            temperature: 0.3,
            responseSchema: {
              type: Type.OBJECT,
              properties: {
                planTitleFa: { type: Type.STRING },
                overviewFa: { type: Type.STRING },
                growthNutritionFocusFa: { type: Type.STRING },
                hydrationTipFa: { type: Type.STRING },
                meals: {
                  type: Type.ARRAY,
                  items: {
                    type: Type.OBJECT,
                    properties: {
                      mealTypeFa: { type: Type.STRING },
                      titleFa: { type: Type.STRING },
                      whySuitableFa: { type: Type.STRING },
                      textureAndPortionFa: { type: Type.STRING },
                      ingredientsFa: { type: Type.ARRAY, items: { type: Type.STRING } },
                      prepStepsFa: { type: Type.ARRAY, items: { type: Type.STRING } },
                      safetyAvoidListFa: { type: Type.ARRAY, items: { type: Type.STRING } },
                      doctorFollowUpHintFa: { type: Type.STRING },
                    },
                    required: [
                      'mealTypeFa',
                      'titleFa',
                      'whySuitableFa',
                      'textureAndPortionFa',
                      'ingredientsFa',
                      'prepStepsFa',
                      'safetyAvoidListFa',
                      'doctorFollowUpHintFa',
                    ],
                  },
                },
              },
              required: ['planTitleFa', 'overviewFa', 'growthNutritionFocusFa', 'hydrationTipFa', 'meals'],
            },
          },
        }),
      );

      const parsed = JSON.parse(response.text || '{}') as {
        planTitleFa?: string;
        overviewFa?: string;
        growthNutritionFocusFa?: string;
        hydrationTipFa?: string;
        meals?: Array<{
          mealTypeFa: string;
          titleFa: string;
          whySuitableFa: string;
          textureAndPortionFa: string;
          ingredientsFa: string[];
          prepStepsFa: string[];
          safetyAvoidListFa: string[];
          doctorFollowUpHintFa: string;
        }>;
      };

      let meals = Array.isArray(parsed.meals) ? parsed.meals : [];
      let filteredWarning = '';

      if (parsedAge < 12) {
        const beforeCount = meals.length;
        meals = meals.filter((meal) => {
          const haystack = [
            meal.titleFa,
            meal.whySuitableFa,
            meal.textureAndPortionFa,
            ...(meal.ingredientsFa || []),
            ...(meal.prepStepsFa || []),
          ].join(' ');
          return !UNDER_12M_FORBIDDEN_TERMS.some((term) => haystack.includes(term));
        });
        if (meals.length < beforeCount) {
          filteredWarning = 'برخی موارد نامناسب برای کودک زیر ۱۲ ماه به‌صورت خودکار از خروجی حذف شدند.';
        }
        if (meals.length === 0) {
          return res.status(422).json({
            error:
              'پاسخ دریافتی شامل اقلام نامناسب برای کودک زیر ۱۲ ماه (مانند عسل یا شیر گاو) بود و برای ایمنی کودک نمایش داده نشد. لطفاً دوباره تلاش کنید.',
          });
        }
      }

      return res.json({
        planTitleFa: parsed.planTitleFa || 'برنامه غذایی پیشنهادی کودک',
        overviewFa: filteredWarning ? `${parsed.overviewFa || ''} (${filteredWarning})`.trim() : parsed.overviewFa || '',
        growthNutritionFocusFa: parsed.growthNutritionFocusFa || '',
        hydrationTipFa: parsed.hydrationTipFa || '',
        meals,
      });
    } catch (err) {
      const message = err instanceof Error ? err.message : 'خطا در دریافت برنامه تغذیه.';
      return res.status(500).json({ error: message });
    }
  });

  // 3. Multimodal Syrup Concentration Scanner
  app.post('/api/gemini/syrup-scan', async (req: Request, res: Response) => {
    const ai = getAiClient();
    if (!ai) return sendMissingKey(res);

    const { imageBase64, mimeType, childContextSummaryFa } = req.body ?? {};
    if (typeof imageBase64 !== 'string' || imageBase64.trim().length < 32) {
      return res.status(400).json({ error: 'تصویر برچسب شربت یا قطره ارسال نشده است.' });
    }

    const cleanData = stripBase64Prefix(imageBase64.trim());
    const resolvedMime = typeof mimeType === 'string' && mimeType.startsWith('image/') ? mimeType : 'image/jpeg';

    const promptText = [
      'برچسب داروی مایع کودک (قطره یا شربت تب‌بر/آلرژی) موجود در تصویر را با دقت بخوان و ساختارمند برگردان.',
      'فقط اگر غلظت میلی‌گرم در میلی‌لیتر روی برچسب خوانا است مقدار دقیق بنویس؛ در غیر این صورت confidenceLevel را LOW و matchedFormulationId را CUSTOM_UNVERIFIED قرار بده.',
      'شناسه فرمولاسیون‌های استاندارد مجاز: acetaminophen_drops_100 (100mg/1mL), acetaminophen_syrup_120_5 (120mg/5mL), acetaminophen_syrup_160_5 (160mg/5mL), ibuprofen_syrup_100_5 (100mg/5mL), cetirizine_syrup_5_5 (5mg/5mL), یا CUSTOM_UNVERIFIED.',
      childContextSummaryFa ? `زمینه پرونده کودک (فقط برای یادداشت احتیاطی): ${childContextSummaryFa}` : '',
    ]
      .filter(Boolean)
      .join('\n');

    try {
      const response = await withTimeout(
        ai.models.generateContent({
          model: GEMINI_MODEL,
          contents: [
            {
              role: 'user',
              parts: [
                { inlineData: { data: cleanData, mimeType: resolvedMime } },
                { text: promptText },
              ],
            },
          ],
          config: {
            responseMimeType: 'application/json',
            temperature: 0.1,
            responseSchema: {
              type: Type.OBJECT,
              properties: {
                detectedDrugNameFa: { type: Type.STRING },
                activeIngredientKey: {
                  type: Type.STRING,
                  enum: ['ACETAMINOPHEN', 'IBUPROFEN', 'CETIRIZINE', 'UNKNOWN'],
                },
                dosageForm: {
                  type: Type.STRING,
                  enum: ['DROPS', 'SYRUP', 'SUSPENSION', 'UNKNOWN'],
                },
                mgAmount: { type: Type.NUMBER },
                perMlAmount: { type: Type.NUMBER },
                mgPerMl: { type: Type.NUMBER },
                matchedFormulationId: {
                  type: Type.STRING,
                  enum: [
                    'acetaminophen_drops_100',
                    'acetaminophen_syrup_120_5',
                    'acetaminophen_syrup_160_5',
                    'ibuprofen_syrup_100_5',
                    'cetirizine_syrup_5_5',
                    'CUSTOM_UNVERIFIED',
                  ],
                },
                confidenceLevel: {
                  type: Type.STRING,
                  enum: ['HIGH', 'MEDIUM', 'LOW'],
                },
                concentrationDisplayFa: { type: Type.STRING },
                safetyNoteFa: { type: Type.STRING },
                isPediatricFeverOrAllergyLiquid: { type: Type.BOOLEAN },
              },
              required: [
                'detectedDrugNameFa',
                'activeIngredientKey',
                'dosageForm',
                'mgAmount',
                'perMlAmount',
                'mgPerMl',
                'matchedFormulationId',
                'confidenceLevel',
                'concentrationDisplayFa',
                'safetyNoteFa',
                'isPediatricFeverOrAllergyLiquid',
              ],
            },
          },
        }),
      );

      const parsed = JSON.parse(response.text || '{}');
      return res.json(parsed);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'خطا در تحلیل تصویر برچسب دارو.';
      return res.status(500).json({ error: message });
    }
  });

  // 4. Symptom Safety Checklist
  app.post('/api/gemini/safety-checklist', async (req: Request, res: Response) => {
    const ai = getAiClient();
    if (!ai) return sendMissingKey(res);

    const { flowTitleFa, urgencyLevel, evaluationSummaryFa, selectedSymptomsFa, childContextSummaryFa } = req.body ?? {};
    if (typeof flowTitleFa !== 'string' || !flowTitleFa.trim()) {
      return res.status(400).json({ error: 'اطلاعات مسیر بررسی علائم ناقص است.' });
    }

    const prompt = [
      'بر اساس ارزیابی اولیه علائم کودک، یک چک‌لیست ایمنی غیرتشخیصی و کاملاً ساختاریافته برای والد تهیه کن.',
      'هرگز تشخیص پزشکی قطعی نگذار و نام تجاری دارو یا دوز دارو تجویز نکن.',
      `مسیر علامت: ${flowTitleFa}`,
      `سطح فوریت ارزیابی‌شده: ${urgencyLevel || 'MONITOR'}`,
      `خلاصه ارزیابی: ${evaluationSummaryFa || ''}`,
      Array.isArray(selectedSymptomsFa) && selectedSymptomsFa.length > 0
        ? `موارد انتخاب‌شده: ${selectedSymptomsFa.join('، ')}`
        : '',
      childContextSummaryFa ? `مشخصات کودک: ${childContextSummaryFa}` : '',
    ]
      .filter(Boolean)
      .join('\n');

    try {
      const response = await withTimeout(
        ai.models.generateContent({
          model: GEMINI_MODEL,
          contents: prompt,
          config: {
            responseMimeType: 'application/json',
            temperature: 0.2,
            responseSchema: {
              type: Type.OBJECT,
              properties: {
                checklistTitleFa: { type: Type.STRING },
                urgencySummaryFa: { type: Type.STRING },
                immediateEmergencySignsFa: { type: Type.ARRAY, items: { type: Type.STRING } },
                sameDayPediatricianVisitSignsFa: { type: Type.ARRAY, items: { type: Type.STRING } },
                homeMonitoringChecklistFa: { type: Type.ARRAY, items: { type: Type.STRING } },
                questionsForPediatricianFa: { type: Type.ARRAY, items: { type: Type.STRING } },
                clinicalSourceCitationFa: { type: Type.STRING },
              },
              required: [
                'checklistTitleFa',
                'urgencySummaryFa',
                'immediateEmergencySignsFa',
                'sameDayPediatricianVisitSignsFa',
                'homeMonitoringChecklistFa',
                'questionsForPediatricianFa',
                'clinicalSourceCitationFa',
              ],
            },
          },
        }),
      );

      const parsed = JSON.parse(response.text || '{}');
      return res.json(parsed);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'خطا در تولید چک‌لیست ایمنی.';
      return res.status(500).json({ error: message });
    }
  });

  // 5. Documented FAQ Q&A
  app.post('/api/gemini/documented-qa', async (req: Request, res: Response) => {
    const ai = getAiClient();
    if (!ai) return sendMissingKey(res);

    const { question, ageBandFa, matchedFaqs } = req.body ?? {};
    if (typeof question !== 'string' || question.trim().length < 4) {
      return res.status(400).json({ error: 'لطفاً پرسش خود را با جزئیات بیشتر (حداقل ۴ کاراکتر) بنویسید.' });
    }

    if (!Array.isArray(matchedFaqs) || matchedFaqs.length === 0) {
      return res.status(400).json({ error: 'مستندات منطبق در پایگاه دانش برای پاسخ‌گویی ارسال نشده است.' });
    }

    const docsBlock = matchedFaqs
      .slice(0, 4)
      .map(
        (faq: { question?: string; shortAnswer?: string; clinicalDetail?: string; citation?: string }, idx: number) =>
          `[${idx + 1}] پرسش مرجع: ${faq.question || ''}\nخلاصه: ${faq.shortAnswer || ''}\nجزئیات: ${faq.clinicalDetail || ''}\nمنبع: ${faq.citation || ''}`,
      )
      .join('\n\n');

    const prompt = [
      'فقط بر اساس پرسش‌ها و مستندات بالینی زیر به پرسش والد پاسخ کوتاه، ایمن و فارسی بده. هیچ عددی یا دوز دارویی از خودت نساز.',
      ageBandFa ? `بازه سنی انتخابی کودک: ${ageBandFa}` : '',
      `پرسش والد: ${question.trim()}`,
      `مستندات تاییدشده مانو کید:\n${docsBlock}`,
    ]
      .filter(Boolean)
      .join('\n\n');

    try {
      const response = await withTimeout(
        ai.models.generateContent({
          model: GEMINI_MODEL,
          contents: prompt,
          config: {
            responseMimeType: 'application/json',
            temperature: 0.2,
            responseSchema: {
              type: Type.OBJECT,
              properties: {
                directAnswerFa: { type: Type.STRING },
                actionableStepsFa: { type: Type.ARRAY, items: { type: Type.STRING } },
                whenToVisitPediatricianFa: { type: Type.ARRAY, items: { type: Type.STRING } },
                citedSourcesFa: { type: Type.ARRAY, items: { type: Type.STRING } },
                safetyDisclaimerFa: { type: Type.STRING },
              },
              required: [
                'directAnswerFa',
                'actionableStepsFa',
                'whenToVisitPediatricianFa',
                'citedSourcesFa',
                'safetyDisclaimerFa',
              ],
            },
          },
        }),
      );

      const parsed = JSON.parse(response.text || '{}');
      return res.json(parsed);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'خطا در پاسخ‌گویی مستند.';
      return res.status(500).json({ error: message });
    }
  });

  if (process.env.NODE_ENV === 'production') {
    const distDir = path.resolve(__dirname, 'dist');
    app.use(express.static(distDir));
    app.get('/{*splat}', (_req: Request, res: Response) => {
      res.sendFile(path.join(distDir, 'index.html'));
    });
  } else {
    const { createServer: createViteServer } = await import('vite');
    const vite = await createViteServer({
      server: {
        middlewareMode: true,
        hmr: false,
      },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`MANO Kid server running on http://0.0.0.0:${PORT}`);
  });
}

createApp().catch((err) => {
  console.error('Failed to start MANO Kid server:', err);
  process.exit(1);
});
