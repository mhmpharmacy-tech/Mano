import { parseHashToRoute, routeToHash, type AppRoute } from '../navigation/routes.ts';
import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
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
import { ageBandFromMonths, resolveClinicalAge, shouldShowFeverAlert } from '../domain/clinicalAge.ts';
import {
  clearLocalSnapshot, currentStorageEpoch, LEGACY_EPOCH, persistLocalSnapshot,
  readLocalSnapshot, STORAGE_EPOCH_KEY, STORAGE_KEY, STORAGE_LOCK_NAME, type StorageLock,
} from './localPersistence.ts';

export type { AppRoute } from '../navigation/routes.ts';

export type HomeViewStyle = 'calm' | 'orbital';

export interface ToastMessage {
  id: string;
  text: string;
  tone: 'info' | 'success' | 'danger';
}

interface PersistedState {
  storageEpoch: string;
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

function createInitialPersistedState(storageEpoch = LEGACY_EPOCH): PersistedState {
  const autostart =
    typeof window !== 'undefined' && new URLSearchParams(window.location.search).get('autostart') === '1';

  return {
    storageEpoch,
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
    const { snapshot: parsed, epoch } = readLocalSnapshot<Partial<PersistedState>>(window.localStorage);
    const fresh = createInitialPersistedState(epoch);
    if (!parsed) return fresh;
    const autostart = new URLSearchParams(window.location.search).get('autostart') === '1';
    const merged: PersistedState = {
      ...fresh,
      ...parsed,
      storageEpoch: epoch,
      childProfile: {
        ...fresh.childProfile,
        ...(parsed.childProfile || {}),
      },
      treatmentPlan: normalizePlanForToday(parsed.treatmentPlan || fresh.treatmentPlan),
      onboardingSeen: autostart ? true : Boolean(parsed.onboardingSeen),
    };
    return merged;
  } catch {
    return fallback;
  }
}

interface ManoContextValue {
  dataGeneration: string;
  persistenceIssue: string | null;
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
  addTempLog: (input: { tempCelsius: number; siteFa: string; caregiverFa: string; noteFa: string; measuredAtMs?: number }) => void;
  deleteTempLog: (id: string) => void;
  addDoseLog: (input: {
    medicationSlug: string;
    medicationNameFa: string;
    dosageFormFa: string;
    volumeMlText: string;
    noteFa: string;
    administeredAtMs?: number;
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
  clearAllSavedLocalData: () => Promise<void>;
}

const ManoContext = createContext<ManoContextValue | null>(null);

const browserStorageLock: StorageLock = (operation) => {
  if (!window.navigator.locks) return Promise.reject(new Error('Storage locking is unavailable'));
  return window.navigator.locks.request(STORAGE_LOCK_NAME, operation);
};

export const ManoProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [state, setState] = useState<PersistedState>(() => loadPersistedState());
  const [route, setRoute] = useState<AppRoute>(() =>
    typeof window !== 'undefined' ? parseHashToRoute(window.location.hash) : { name: 'home' },
  );
  const [activeHighFeverAlert, setActiveHighFeverAlert] = useState<TimelineTempPoint | null>(null);
  const [searchOpen, setSearchOpen] = useState(false);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);
  const [persistenceIssue, setPersistenceIssue] = useState<string | null>(null);
  const [timelineNowMs, setTimelineNowMs] = useState(() => Date.now());
  const currentEpochRef = useRef(state.storageEpoch);

  const resetToEpoch = useCallback((epoch: string) => {
    if (currentEpochRef.current === epoch) return;
    currentEpochRef.current = epoch;
    const fresh = createInitialPersistedState(epoch);
    fresh.onboardingSeen = true;
    setState(fresh);
    setActiveHighFeverAlert(null);
    setSearchOpen(false);
    setToasts([]);
  }, []);

  // Sync dark class on <html>
  useEffect(() => {
    const root = document.documentElement;
    if (state.theme === 'dark') {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }
  }, [state.theme]);

  // A cross-tab lock and deletion generation prevent stale snapshots from
  // restoring deleted clinical data. Never persist without the lock.
  useEffect(() => {
    let active = true;
    void (async () => {
      try {
        const result = await persistLocalSnapshot(window.localStorage, browserStorageLock, state);
        if (!active) return;
        if (!result.saved) resetToEpoch(result.epoch);
        setPersistenceIssue(null);
      } catch {
        if (active) setPersistenceIssue('ذخیرهٔ تغییرات در این مرورگر انجام نشد. پیش از بستن صفحه از اطلاعات موردنیاز نسخه بردارید.');
      }
    })();
    return () => { active = false; };
  }, [state, resetToEpoch]);

  useEffect(() => {
    const onStorage = (event: StorageEvent) => {
      try {
        if (event.storageArea !== window.localStorage ||
            (event.key !== STORAGE_EPOCH_KEY && event.key !== STORAGE_KEY && event.key !== null)) return;
        if (event.key === null) {
          // A browser-wide clear also needs a tombstone for surviving tabs.
          void clearLocalSnapshot(window.localStorage, browserStorageLock, crypto.randomUUID())
            .then(resetToEpoch)
            .catch(() => setPersistenceIssue('پاک‌سازی اطلاعات در تب‌های باز تأیید نشد؛ تب‌های دیگر را ببندید و دوباره تلاش کنید.'));
          return;
        }
        resetToEpoch(currentStorageEpoch(window.localStorage));
      } catch {
        setPersistenceIssue('همگام‌سازی حذف اطلاعات در مرورگر انجام نشد. صفحه را دوباره باز کنید.');
      }
    };
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, [resetToEpoch]);

  useEffect(() => {
    const refresh = () => setTimelineNowMs(Date.now());
    const interval = window.setInterval(refresh, 30_000);
    window.addEventListener('focus', refresh);
    document.addEventListener('visibilitychange', refresh);
    return () => {
      window.clearInterval(interval);
      window.removeEventListener('focus', refresh);
      document.removeEventListener('visibilitychange', refresh);
    };
  }, []);

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
    (input: { tempCelsius: number; siteFa: string; caregiverFa: string; noteFa: string; measuredAtMs?: number }) => {
      const now = Date.now();
      const measuredAtMs = input.measuredAtMs ?? now;
      if (!Number.isFinite(input.tempCelsius) || input.tempCelsius < 34 || input.tempCelsius > 43 ||
          !input.siteFa.trim() || !Number.isFinite(measuredAtMs) || measuredAtMs <= 0 || measuredAtMs > now) {
        notify('دمای واقعی، روش اندازه‌گیری و زمان معتبر ثبت شود.', 'danger');
        return;
      }
      const entry: TimelineTempPoint = {
        id: `temp_${crypto.randomUUID()}`,
        tempCelsius: input.tempCelsius,
        timestampMs: measuredAtMs,
        recordedAtMs: now,
        timeLabelFa: currentTimeFa(measuredAtMs),
        siteFa: input.siteFa,
        caregiverFa: input.caregiverFa.trim() || 'ثبت‌کننده مشخص نشده',
        noteFa: input.noteFa.trim(),
      };
      setTimelineNowMs(now);
      setState((prev) => ({ ...prev, feverLogs: [...prev.feverLogs, entry] }));
      if (shouldShowFeverAlert(input.tempCelsius, resolveClinicalAge(state.childProfile, state.vaccineBirthDateEpochMs))) {
        setActiveHighFeverAlert(entry);
      } else {
        notify(`دمای ${formatTempC(input.tempCelsius)} ثبت شد.`, 'success');
      }
    },
    [notify, state.childProfile, state.vaccineBirthDateEpochMs],
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
      administeredAtMs?: number;
    }) => {
      const now = Date.now();
      const administeredAtMs = input.administeredAtMs ?? now;
      if (!input.medicationSlug.trim() || !input.medicationNameFa.trim() || !input.dosageFormFa.trim() ||
          !input.volumeMlText.trim() || !Number.isFinite(administeredAtMs) || administeredAtMs <= 0 || administeredAtMs > now) {
        notify('دارو، غلظت، مقدار واقعاً مصرف‌شده و زمان معتبر ثبت شود.', 'danger');
        return;
      }
      const entry: TimelineDosePoint = {
        id: `dose_${crypto.randomUUID()}`,
        medicationSlug: input.medicationSlug,
        medicationNameFa: input.medicationNameFa,
        dosageFormFa: input.dosageFormFa,
        volumeMlText: input.volumeMlText,
        timestampMs: administeredAtMs,
        recordedAtMs: now,
        givenAtLabelFa: currentTimeFa(administeredAtMs),
        noteFa: input.noteFa.trim(),
      };
      setTimelineNowMs(now);
      setState((prev) => ({ ...prev, doseLogs: [...prev.doseLogs, entry] }));
      notify(`مصرف ${input.medicationNameFa} در جدول ۲۴ ساعته ثبت شد.`, 'success');
    },
    [notify],
  );

  const deleteDoseLog = useCallback((id: string) => {
    setState((prev) => ({ ...prev, doseLogs: prev.doseLogs.filter((d) => d.id !== id) }));
  }, []);

  const timelineSnapshot = useMemo(
    () => buildTimelineSnapshot(state.feverLogs, state.doseLogs, timelineNowMs),
    [state.feverLogs, state.doseLogs, timelineNowMs],
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
      const age = resolveClinicalAge({ ageMonthsExact: null, birthDateEpochMs: epochMs, birthDateOrAgeFa: '' });
      if (epochMs !== null && age.status !== 'known') {
        notify(age.messageFa || 'تاریخ تولد معتبر کودک را ثبت کنید.', 'danger');
        return;
      }
      setState((prev) => ({
        ...prev,
        vaccineBirthDateEpochMs: epochMs,
        childProfile: {
          ...prev.childProfile,
          birthDateEpochMs: epochMs,
          ...(epochMs !== null ? {
            ageMonthsExact: null,
            birthDateOrAgeFa: '',
            ageBand: ageBandFromMonths(age.ageMonths) ?? prev.childProfile.ageBand,
          } : {}),
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

  const clearAllSavedLocalData = useCallback(async () => {
    try {
      const epoch = await clearLocalSnapshot(window.localStorage, browserStorageLock, crypto.randomUUID());
      resetToEpoch(epoch);
      setPersistenceIssue(null);
      notify('اطلاعات محلی حذف شد؛ فرم‌ها و تب‌های باز نیز بازنشانی شدند.', 'info');
    } catch {
      setPersistenceIssue('حذف اطلاعات انجام نشد. دسترسی مرورگر به فضای ذخیره‌سازی را بررسی و دوباره تلاش کنید.');
      notify('حذف اطلاعات تأیید نشد.', 'danger');
    }
  }, [notify, resetToEpoch]);

  const value: ManoContextValue = {
    dataGeneration: state.storageEpoch,
    persistenceIssue,
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
