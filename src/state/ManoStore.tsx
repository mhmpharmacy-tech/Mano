import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type {
  CareCardEntry,
  ChildProfile,
  GrowthMeasurementEntry,
  NutritionPlan,
  PharmacistConsultation,
  SavedSyrupScan,
  SavedVisitDraft,
  TimelineDosePoint,
  TimelineTempPoint,
  VaccineDashboardSummary,
  VaccineRecord,
  VaccineReminderSettings,
} from '../domain/models.ts';
import {
  HIGH_FEVER_ALERT_THRESHOLD_C,
  buildTimelineSnapshot,
  formatTempC,
  type FeverDoseTimelineSnapshot,
} from '../domain/feverDoseTimelineEngine.ts';
import {
  createDefaultTreatmentPlan,
  currentDayKey,
  currentTimeFa,
  normalizePlanForToday,
  type PediatricTreatmentPlan,
  type TreatmentMedicationItem,
} from '../domain/treatmentPlan.ts';
import { resolveVaccineSchedule } from '../domain/vaccinationScheduleEngine.ts';
import { toPersianDigits } from '../domain/persianNormalizer.ts';

export type AppRoute =
  | { name: 'home' }
  | { name: 'symptoms'; initialFlowId?: string }
  | { name: 'fever-dose'; prefillFormulationId?: string }
  | { name: 'syrup-scanner' }
  | { name: 'vaccines' }
  | { name: 'growth'; initialTab?: 'chart' | 'milestones' }
  | { name: 'nutrition' }
  | { name: 'treatment-plan' }
  | { name: 'medicines' }
  | { name: 'medicine-detail'; slug: string }
  | { name: 'topics' }
  | { name: 'topic-detail'; slug: string }
  | { name: 'faq' }
  | { name: 'supplements-hygiene'; initialTab?: 'supplements' | 'hygiene' }
  | { name: 'skin' }
  | { name: 'devices' }
  | { name: 'ask-pharmacist'; initialTopic?: string }
  | { name: 'care-cards' }
  | { name: 'my-child' };

export type HomeViewStyle = 'calm' | 'orbital';

export interface ToastMessage {
  id: string;
  text: string;
  tone: 'info' | 'success' | 'danger';
}

const STORAGE_KEY = 'mano_kid_v2_store';

interface PersistedState {
  theme: 'light' | 'dark';
  homeViewStyle: HomeViewStyle;
  onboardingSeen: boolean;
  childProfile: ChildProfile;
  feverLogs: TimelineTempPoint[];
  doseLogs: TimelineDosePoint[];
  growthMeasurements: GrowthMeasurementEntry[];
  checkedMilestones: string[];
  vaccineBirthDateEpochMs: number | null;
  vaccineRecords: Record<string, VaccineRecord>;
  vaccineReminders: VaccineReminderSettings;
  checkedMedSafetyIds: string[];
  checkedHygieneIds: string[];
  bookmarkedSlugs: string[];
  treatmentPlan: PediatricTreatmentPlan;
  savedVisitDrafts: SavedVisitDraft[];
  nutritionPlans: NutritionPlan[];
  savedSyrupScans: SavedSyrupScan[];
  pharmacistConsultations: PharmacistConsultation[];
  careCards: CareCardEntry[];
}

function defaultChildProfile(): ChildProfile {
  return {
    id: 'child_primary',
    aliasFa: 'کودک من',
    ageBand: 'TODDLER_1_3Y',
    ageMonthsExact: null,
    sex: null, // Never fabricate initial biological sex
    birthDateOrAgeFa: '',
    birthDateEpochMs: null,
    weightKgText: '',
    allergiesFa: '',
    updatedAtLabelFa: 'پروفایل آماده ثبت',
  };
}

function createInitialPersistedState(): PersistedState {
  const autostart =
    typeof window !== 'undefined' && new URLSearchParams(window.location.search).get('autostart') === '1';

  return {
    theme: 'light',
    homeViewStyle: 'calm',
    onboardingSeen: autostart,
    childProfile: defaultChildProfile(),
    feverLogs: [],
    doseLogs: [],
    growthMeasurements: [], // Initially empty — zero fabricated percentiles
    checkedMilestones: [],
    vaccineBirthDateEpochMs: null,
    vaccineRecords: {},
    vaccineReminders: { enabled: false, leadDaysBefore: 3 },
    checkedMedSafetyIds: [],
    checkedHygieneIds: [],
    bookmarkedSlugs: [],
    treatmentPlan: createDefaultTreatmentPlan(),
    savedVisitDrafts: [],
    nutritionPlans: [],
    savedSyrupScans: [],
    pharmacistConsultations: [],
    careCards: [],
  };
}

function loadPersistedState(): PersistedState {
  const fallback = createInitialPersistedState();
  if (typeof window === 'undefined') return fallback;
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return fallback;
    const parsed = JSON.parse(raw) as Partial<PersistedState>;
    const autostart = new URLSearchParams(window.location.search).get('autostart') === '1';
    const merged: PersistedState = {
      ...fallback,
      ...parsed,
      childProfile: {
        ...fallback.childProfile,
        ...(parsed.childProfile || {}),
      },
      treatmentPlan: normalizePlanForToday(parsed.treatmentPlan || fallback.treatmentPlan),
      onboardingSeen: autostart ? true : Boolean(parsed.onboardingSeen),
    };
    return merged;
  } catch {
    return fallback;
  }
}

export function routeToHash(route: AppRoute): string {
  switch (route.name) {
    case 'home':
      return '#/home';
    case 'symptoms':
      return route.initialFlowId ? `#/symptoms/${encodeURIComponent(route.initialFlowId)}` : '#/symptoms';
    case 'fever-dose':
      return route.prefillFormulationId
        ? `#/fever-dose?formulation=${encodeURIComponent(route.prefillFormulationId)}`
        : '#/fever-dose';
    case 'medicine-detail':
      return `#/medicines/${encodeURIComponent(route.slug)}`;
    case 'topic-detail':
      return `#/topics/${encodeURIComponent(route.slug)}`;
    case 'growth':
      return route.initialTab === 'milestones' ? '#/growth?tab=milestones' : '#/growth';
    case 'supplements-hygiene':
      return route.initialTab === 'hygiene'
        ? '#/supplements-hygiene?tab=hygiene'
        : '#/supplements-hygiene';
    case 'ask-pharmacist':
      return route.initialTopic
        ? `#/ask-pharmacist?topic=${encodeURIComponent(route.initialTopic)}`
        : '#/ask-pharmacist';
    default:
      return `#/${route.name}`;
  }
}

export function parseHashToRoute(hash: string): AppRoute {
  const clean = hash.replace(/^#\/?/, '');
  if (!clean || clean === 'home') return { name: 'home' };
  const [pathPart, queryPart] = clean.split('?');
  const segs = pathPart.split('/').filter(Boolean);
  const query = new URLSearchParams(queryPart || '');
  const head = segs[0];
  switch (head) {
    case 'symptoms':
      return { name: 'symptoms', initialFlowId: segs[1] ? decodeURIComponent(segs[1]) : undefined };
    case 'fever-dose':
      return {
        name: 'fever-dose',
        prefillFormulationId: query.get('formulation') || undefined,
      };
    case 'syrup-scanner':
      return { name: 'syrup-scanner' };
    case 'vaccines':
      return { name: 'vaccines' };
    case 'growth':
      return {
        name: 'growth',
        initialTab: query.get('tab') === 'milestones' ? 'milestones' : 'chart',
      };
    case 'nutrition':
      return { name: 'nutrition' };
    case 'treatment-plan':
      return { name: 'treatment-plan' };
    case 'medicines':
      return segs[1]
        ? { name: 'medicine-detail', slug: decodeURIComponent(segs[1]) }
        : { name: 'medicines' };
    case 'topics':
      return segs[1]
        ? { name: 'topic-detail', slug: decodeURIComponent(segs[1]) }
        : { name: 'topics' };
    case 'faq':
      return { name: 'faq' };
    case 'supplements-hygiene':
      return {
        name: 'supplements-hygiene',
        initialTab: query.get('tab') === 'hygiene' ? 'hygiene' : 'supplements',
      };
    case 'skin':
      return { name: 'skin' };
    case 'devices':
      return { name: 'devices' };
    case 'ask-pharmacist':
      return { name: 'ask-pharmacist', initialTopic: query.get('topic') || undefined };
    case 'care-cards':
      return { name: 'care-cards' };
    case 'my-child':
      return { name: 'my-child' };
    default:
      return { name: 'home' };
  }
}

interface ManoContextValue {
  route: AppRoute;
  navigate: (next: AppRoute) => void;
  theme: 'light' | 'dark';
  toggleTheme: () => void;
  homeViewStyle: HomeViewStyle;
  setHomeViewStyle: (style: HomeViewStyle) => void;
  onboardingSeen: boolean;
  completeOnboarding: () => void;
  childProfile: ChildProfile;
  updateChildProfile: (patch: Partial<ChildProfile>) => void;
  feverLogs: TimelineTempPoint[];
  doseLogs: TimelineDosePoint[];
  timelineSnapshot: FeverDoseTimelineSnapshot;
  activeHighFeverAlert: TimelineTempPoint | null;
  dismissHighFeverAlert: () => void;
  addTempLog: (input: { tempCelsius: number; siteFa: string; caregiverFa: string; noteFa: string }) => void;
  deleteTempLog: (id: string) => void;
  addDoseLog: (input: {
    medicationSlug: string;
    medicationNameFa: string;
    dosageFormFa: string;
    volumeMlText: string;
    noteFa: string;
  }) => void;
  deleteDoseLog: (id: string) => void;
  growthMeasurements: GrowthMeasurementEntry[];
  addGrowthMeasurement: (entry: GrowthMeasurementEntry) => void;
  deleteGrowthMeasurement: (id: string) => void;
  checkedMilestones: string[];
  toggleMilestone: (id: string) => void;
  vaccineBirthDateEpochMs: number | null;
  setVaccineBirthDateEpochMs: (epochMs: number | null) => void;
  vaccineRecords: Record<string, VaccineRecord>;
  markVaccineAdministered: (vaccineId: string, administeredDateEpochMs: number, facilityNameFa?: string) => void;
  clearVaccineRecord: (vaccineId: string) => void;
  toggleVaccineDeferred: (vaccineId: string) => void;
  vaccineReminders: VaccineReminderSettings;
  setVaccineReminders: (next: VaccineReminderSettings) => void;
  vaccineSummary: VaccineDashboardSummary;
  checkedMedSafetyIds: string[];
  toggleMedSafetyItem: (id: string) => void;
  checkedHygieneIds: string[];
  toggleHygieneItem: (id: string) => void;
  bookmarkedSlugs: string[];
  toggleBookmark: (slug: string) => void;
  treatmentPlan: PediatricTreatmentPlan;
  toggleTreatmentSlot: (medId: string, slotIndex: number) => void;
  upsertTreatmentMedication: (med: TreatmentMedicationItem) => void;
  deleteTreatmentMedication: (medId: string) => void;
  toggleTreatmentReminders: () => void;
  resetTreatmentToday: () => void;
  savedVisitDrafts: SavedVisitDraft[];
  saveVisitDraft: (titleFa: string, draftTextFa: string) => void;
  deleteVisitDraft: (id: string) => void;
  nutritionPlans: NutritionPlan[];
  saveNutritionPlan: (plan: NutritionPlan) => void;
  savedSyrupScans: SavedSyrupScan[];
  saveSyrupScan: (scan: SavedSyrupScan) => void;
  pharmacistConsultations: PharmacistConsultation[];
  addPharmacistConsultation: (c: PharmacistConsultation) => void;
  replyPharmacistConsultation: (id: string, replyFa: string) => void;
  careCards: CareCardEntry[];
  addCareCard: (card: CareCardEntry) => void;
  deleteCareCard: (id: string) => void;
  searchOpen: boolean;
  setSearchOpen: (open: boolean) => void;
  toasts: ToastMessage[];
  notify: (text: string, tone?: ToastMessage['tone']) => void;
  clearAllSavedLocalData: () => void;
}

const ManoContext = createContext<ManoContextValue | null>(null);

export const ManoProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [state, setState] = useState<PersistedState>(() => loadPersistedState());
  const [route, setRoute] = useState<AppRoute>(() =>
    typeof window !== 'undefined' ? parseHashToRoute(window.location.hash) : { name: 'home' },
  );
  const [activeHighFeverAlert, setActiveHighFeverAlert] = useState<TimelineTempPoint | null>(null);
  const [searchOpen, setSearchOpen] = useState(false);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  // Sync dark class on <html>
  useEffect(() => {
    const root = document.documentElement;
    if (state.theme === 'dark') {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }
  }, [state.theme]);

  // Persist to localStorage
  useEffect(() => {
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch {
      // Ignore storage quota issues
    }
  }, [state]);

  // Listen to browser hash navigation
  useEffect(() => {
    const onHashChange = () => {
      setRoute(parseHashToRoute(window.location.hash));
    };
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, []);

  const notify = useCallback((text: string, tone: ToastMessage['tone'] = 'info') => {
    const id = `t_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`;
    setToasts((prev) => [...prev.slice(-2), { id, text, tone }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((item) => item.id !== id));
    }, 3600);
  }, []);

  const navigate = useCallback((next: AppRoute) => {
    const targetHash = routeToHash(next);
    if (window.location.hash !== targetHash) {
      window.location.hash = targetHash;
    }
    setRoute(next);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, []);

  const toggleTheme = useCallback(() => {
    setState((prev) => ({ ...prev, theme: prev.theme === 'dark' ? 'light' : 'dark' }));
  }, []);

  const setHomeViewStyle = useCallback((homeViewStyle: HomeViewStyle) => {
    setState((prev) => ({ ...prev, homeViewStyle }));
  }, []);

  const completeOnboarding = useCallback(() => {
    setState((prev) => ({ ...prev, onboardingSeen: true }));
  }, []);

  const updateChildProfile = useCallback(
    (patch: Partial<ChildProfile>) => {
      setState((prev) => ({
        ...prev,
        childProfile: {
          ...prev.childProfile,
          ...patch,
          updatedAtLabelFa: `به‌روزرسانی ${currentTimeFa()}`,
        },
        vaccineBirthDateEpochMs:
          patch.birthDateEpochMs !== undefined ? patch.birthDateEpochMs : prev.vaccineBirthDateEpochMs,
      }));
      notify('مشخصات کودک در حافظه همین دستگاه ذخیره شد.', 'success');
    },
    [notify],
  );

  const addTempLog = useCallback(
    (input: { tempCelsius: number; siteFa: string; caregiverFa: string; noteFa: string }) => {
      const now = Date.now();
      const entry: TimelineTempPoint = {
        id: `temp_${now}`,
        tempCelsius: input.tempCelsius,
        timestampMs: now,
        timeLabelFa: currentTimeFa(now),
        siteFa: input.siteFa,
        caregiverFa: input.caregiverFa || 'والد',
        noteFa: input.noteFa.trim(),
      };
      setState((prev) => ({ ...prev, feverLogs: [...prev.feverLogs, entry] }));
      const isYoungInfant = state.childProfile.ageBand === 'NEWBORN';
      if (
        input.tempCelsius > HIGH_FEVER_ALERT_THRESHOLD_C ||
        (isYoungInfant && input.tempCelsius >= 38.0)
      ) {
        setActiveHighFeverAlert(entry);
      } else {
        notify(`دمای ${formatTempC(input.tempCelsius)} ثبت شد.`, 'success');
      }
    },
    [notify, state.childProfile.ageBand],
  );

  const deleteTempLog = useCallback((id: string) => {
    setState((prev) => ({ ...prev, feverLogs: prev.feverLogs.filter((t) => t.id !== id) }));
  }, []);

  const addDoseLog = useCallback(
    (input: {
      medicationSlug: string;
      medicationNameFa: string;
      dosageFormFa: string;
      volumeMlText: string;
      noteFa: string;
    }) => {
      const now = Date.now();
      const entry: TimelineDosePoint = {
        id: `dose_${now}`,
        medicationSlug: input.medicationSlug,
        medicationNameFa: input.medicationNameFa,
        dosageFormFa: input.dosageFormFa,
        volumeMlText: input.volumeMlText,
        timestampMs: now,
        givenAtLabelFa: currentTimeFa(now),
        noteFa: input.noteFa.trim(),
      };
      setState((prev) => ({ ...prev, doseLogs: [...prev.doseLogs, entry] }));
      notify(`مصرف ${input.medicationNameFa} در جدول ۲۴ ساعته ثبت شد.`, 'success');
    },
    [notify],
  );

  const deleteDoseLog = useCallback((id: string) => {
    setState((prev) => ({ ...prev, doseLogs: prev.doseLogs.filter((d) => d.id !== id) }));
  }, []);

  const timelineSnapshot = useMemo(
    () => buildTimelineSnapshot(state.feverLogs, state.doseLogs),
    [state.feverLogs, state.doseLogs],
  );

  const addGrowthMeasurement = useCallback(
    (entry: GrowthMeasurementEntry) => {
      setState((prev) => ({
        ...prev,
        growthMeasurements: [entry, ...prev.growthMeasurements],
      }));
      notify('اندازه‌گیری جدید روی نمودار رشد WHO ثبت شد.', 'success');
    },
    [notify],
  );

  const deleteGrowthMeasurement = useCallback((id: string) => {
    setState((prev) => ({
      ...prev,
      growthMeasurements: prev.growthMeasurements.filter((g) => g.id !== id),
    }));
  }, []);

  const toggleMilestone = useCallback((id: string) => {
    setState((prev) => {
      const exists = prev.checkedMilestones.includes(id);
      return {
        ...prev,
        checkedMilestones: exists
          ? prev.checkedMilestones.filter((m) => m !== id)
          : [...prev.checkedMilestones, id],
      };
    });
  }, []);

  const setVaccineBirthDateEpochMs = useCallback(
    (epochMs: number | null) => {
      setState((prev) => ({
        ...prev,
        vaccineBirthDateEpochMs: epochMs,
        childProfile: {
          ...prev.childProfile,
          birthDateEpochMs: epochMs,
        },
      }));
      if (epochMs) {
        notify('تاریخ تولد و زمان‌بندی واکسیناسیون به‌روزرسانی شد.', 'success');
      }
    },
    [notify],
  );

  const markVaccineAdministered = useCallback(
    (vaccineId: string, administeredDateEpochMs: number, facilityNameFa?: string) => {
      setState((prev) => ({
        ...prev,
        vaccineRecords: {
          ...prev.vaccineRecords,
          [vaccineId]: {
            ...(prev.vaccineRecords[vaccineId] || { vaccineId }),
            vaccineId,
            administeredDateEpochMs,
            isDeferred: false,
            facilityNameFa,
            updatedAtMs: Date.now(),
          },
        },
      }));
      notify('تزریق واکسن ثبت شد.', 'success');
    },
    [notify],
  );

  const clearVaccineRecord = useCallback((vaccineId: string) => {
    setState((prev) => {
      const next = { ...prev.vaccineRecords };
      delete next[vaccineId];
      return { ...prev, vaccineRecords: next };
    });
  }, []);

  const toggleVaccineDeferred = useCallback((vaccineId: string) => {
    setState((prev) => {
      const cur = prev.vaccineRecords[vaccineId];
      return {
        ...prev,
        vaccineRecords: {
          ...prev.vaccineRecords,
          [vaccineId]: {
            ...(cur || { vaccineId }),
            vaccineId,
            isDeferred: !cur?.isDeferred,
            updatedAtMs: Date.now(),
          },
        },
      };
    });
  }, []);

  const setVaccineReminders = useCallback((vaccineReminders: VaccineReminderSettings) => {
    setState((prev) => ({ ...prev, vaccineReminders }));
  }, []);

  const vaccineSummary = useMemo(
    () =>
      resolveVaccineSchedule(
        Object.values(state.vaccineRecords),
        state.vaccineBirthDateEpochMs,
        state.vaccineReminders,
      ),
    [state.vaccineRecords, state.vaccineBirthDateEpochMs, state.vaccineReminders],
  );

  const toggleMedSafetyItem = useCallback((id: string) => {
    setState((prev) => ({
      ...prev,
      checkedMedSafetyIds: prev.checkedMedSafetyIds.includes(id)
        ? prev.checkedMedSafetyIds.filter((x) => x !== id)
        : [...prev.checkedMedSafetyIds, id],
    }));
  }, []);

  const toggleHygieneItem = useCallback((id: string) => {
    setState((prev) => ({
      ...prev,
      checkedHygieneIds: prev.checkedHygieneIds.includes(id)
        ? prev.checkedHygieneIds.filter((x) => x !== id)
        : [...prev.checkedHygieneIds, id],
    }));
  }, []);

  const toggleBookmark = useCallback(
    (slug: string) => {
      setState((prev) => {
        const exists = prev.bookmarkedSlugs.includes(slug);
        notify(exists ? 'از فهرست نشان‌شده‌ها حذف شد.' : 'مطلب ذخیره و نشان شد.', 'info');
        return {
          ...prev,
          bookmarkedSlugs: exists
            ? prev.bookmarkedSlugs.filter((s) => s !== slug)
            : [...prev.bookmarkedSlugs, slug],
        };
      });
    },
    [notify],
  );

  const toggleTreatmentSlot = useCallback((medId: string, slotIndex: number) => {
    setState((prev) => {
      const plan = normalizePlanForToday(prev.treatmentPlan);
      const nextMeds = plan.medications.map((med) => {
        if (med.id !== medId) return med;
        const nextSlots = { ...med.completedSlotTimesFa };
        if (nextSlots[slotIndex]) {
          delete nextSlots[slotIndex];
        } else {
          nextSlots[slotIndex] = currentTimeFa();
        }
        return { ...med, completedSlotTimesFa: nextSlots };
      });
      return {
        ...prev,
        treatmentPlan: {
          ...plan,
          lastCheckoffDayKey: currentDayKey(),
          medications: nextMeds,
        },
      };
    });
  }, []);

  const upsertTreatmentMedication = useCallback(
    (med: TreatmentMedicationItem) => {
      setState((prev) => {
        const plan = normalizePlanForToday(prev.treatmentPlan);
        const exists = plan.medications.some((m) => m.id === med.id);
        const nextMeds = exists
          ? plan.medications.map((m) => (m.id === med.id ? med : m))
          : [...plan.medications, med];
        return { ...prev, treatmentPlan: { ...plan, medications: nextMeds } };
      });
      notify('فهرست دارویی برنامه درمانی به‌روزرسانی شد.', 'success');
    },
    [notify],
  );

  const deleteTreatmentMedication = useCallback(
    (medId: string) => {
      setState((prev) => ({
        ...prev,
        treatmentPlan: {
          ...prev.treatmentPlan,
          medications: prev.treatmentPlan.medications.filter((m) => m.id !== medId),
        },
      }));
      notify('دارو از برنامه درمانی حذف شد.', 'info');
    },
    [notify],
  );

  const toggleTreatmentReminders = useCallback(() => {
    setState((prev) => ({
      ...prev,
      treatmentPlan: {
        ...prev.treatmentPlan,
        smartRemindersEnabled: !prev.treatmentPlan.smartRemindersEnabled,
      },
    }));
  }, []);

  const resetTreatmentToday = useCallback(() => {
    setState((prev) => ({
      ...prev,
      treatmentPlan: {
        ...prev.treatmentPlan,
        lastCheckoffDayKey: currentDayKey(),
        medications: prev.treatmentPlan.medications.map((m) => ({
          ...m,
          completedSlotTimesFa: {},
        })),
      },
    }));
    notify('تیک نوبت‌های امروز بازنشانی شد.', 'info');
  }, [notify]);

  const saveVisitDraft = useCallback(
    (titleFa: string, draftTextFa: string) => {
      const entry: SavedVisitDraft = {
        id: `visit_${Date.now()}`,
        titleFa,
        draftTextFa,
        createdAtLabelFa: `امروز ${currentTimeFa()}`,
      };
      setState((prev) => ({ ...prev, savedVisitDrafts: [entry, ...prev.savedVisitDrafts] }));
      notify('خلاصه ویزیت پزشک ذخیره شد.', 'success');
    },
    [notify],
  );

  const deleteVisitDraft = useCallback((id: string) => {
    setState((prev) => ({
      ...prev,
      savedVisitDrafts: prev.savedVisitDrafts.filter((d) => d.id !== id),
    }));
  }, []);

  const saveNutritionPlan = useCallback(
    (plan: NutritionPlan) => {
      setState((prev) => ({ ...prev, nutritionPlans: [plan, ...prev.nutritionPlans.slice(0, 9)] }));
      notify('برنامه غذایی در تاریخچه ذخیره شد.', 'success');
    },
    [notify],
  );

  const saveSyrupScan = useCallback(
    (scan: SavedSyrupScan) => {
      setState((prev) => ({ ...prev, savedSyrupScans: [scan, ...prev.savedSyrupScans.slice(0, 14)] }));
      notify('ثبت شربت اسکن‌شده ذخیره شد.', 'success');
    },
    [notify],
  );

  const addPharmacistConsultation = useCallback(
    (consultation: PharmacistConsultation) => {
      setState((prev) => ({
        ...prev,
        pharmacistConsultations: [consultation, ...prev.pharmacistConsultations],
      }));
      notify('پرسش شما در حافظه همین دستگاه ثبت شد (ارسال ابری فعال نیست).', 'info');
    },
    [notify],
  );

  const replyPharmacistConsultation = useCallback(
    (id: string, replyFa: string) => {
      setState((prev) => ({
        ...prev,
        pharmacistConsultations: prev.pharmacistConsultations.map((item) =>
          item.id === id
            ? {
                ...item,
                pharmacistReplyFa: replyFa,
                statusFa: 'پاسخ‌داده‌شده (ثبت محلی)',
                repliedAtLabelFa: `امروز ${currentTimeFa()}`,
              }
            : item,
        ),
      }));
      notify('یادداشت پاسخ در میز داروساز ذخیره شد.', 'success');
    },
    [notify],
  );

  const addCareCard = useCallback(
    (card: CareCardEntry) => {
      setState((prev) => ({ ...prev, careCards: [card, ...prev.careCards] }));
      notify('کارت مراقبت جدید ساخته شد.', 'success');
    },
    [notify],
  );

  const deleteCareCard = useCallback((id: string) => {
    setState((prev) => ({ ...prev, careCards: prev.careCards.filter((c) => c.id !== id) }));
  }, []);

  const clearAllSavedLocalData = useCallback(() => {
    const fresh = createInitialPersistedState();
    fresh.onboardingSeen = true;
    setState(fresh);
    notify(`همه ${toPersianDigits(0)} مورد محلی پاک‌سازی و بازنشانی شد.`, 'info');
  }, [notify]);

  const value: ManoContextValue = {
    route,
    navigate,
    theme: state.theme,
    toggleTheme,
    homeViewStyle: state.homeViewStyle,
    setHomeViewStyle,
    onboardingSeen: state.onboardingSeen,
    completeOnboarding,
    childProfile: state.childProfile,
    updateChildProfile,
    feverLogs: state.feverLogs,
    doseLogs: state.doseLogs,
    timelineSnapshot,
    activeHighFeverAlert,
    dismissHighFeverAlert: () => setActiveHighFeverAlert(null),
    addTempLog,
    deleteTempLog,
    addDoseLog,
    deleteDoseLog,
    growthMeasurements: state.growthMeasurements,
    addGrowthMeasurement,
    deleteGrowthMeasurement,
    checkedMilestones: state.checkedMilestones,
    toggleMilestone,
    vaccineBirthDateEpochMs: state.vaccineBirthDateEpochMs,
    setVaccineBirthDateEpochMs,
    vaccineRecords: state.vaccineRecords,
    markVaccineAdministered,
    clearVaccineRecord,
    toggleVaccineDeferred,
    vaccineReminders: state.vaccineReminders,
    setVaccineReminders,
    vaccineSummary,
    checkedMedSafetyIds: state.checkedMedSafetyIds,
    toggleMedSafetyItem,
    checkedHygieneIds: state.checkedHygieneIds,
    toggleHygieneItem,
    bookmarkedSlugs: state.bookmarkedSlugs,
    toggleBookmark,
    treatmentPlan: state.treatmentPlan,
    toggleTreatmentSlot,
    upsertTreatmentMedication,
    deleteTreatmentMedication,
    toggleTreatmentReminders,
    resetTreatmentToday,
    savedVisitDrafts: state.savedVisitDrafts,
    saveVisitDraft,
    deleteVisitDraft,
    nutritionPlans: state.nutritionPlans,
    saveNutritionPlan,
    savedSyrupScans: state.savedSyrupScans,
    saveSyrupScan,
    pharmacistConsultations: state.pharmacistConsultations,
    addPharmacistConsultation,
    replyPharmacistConsultation,
    careCards: state.careCards,
    addCareCard,
    deleteCareCard,
    searchOpen,
    setSearchOpen,
    toasts,
    notify,
    clearAllSavedLocalData,
  };

  return <ManoContext.Provider value={value}>{children}</ManoContext.Provider>;
};

export function useMano() {
  const ctx = useContext(ManoContext);
  if (!ctx) throw new Error('useMano must be used inside ManoProvider');
  return ctx;
}
