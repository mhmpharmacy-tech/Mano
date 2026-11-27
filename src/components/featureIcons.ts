import {
  Baby, BookOpen, Calculator, ClipboardList, Cpu, HelpCircle, Pill, QrCode,
  Scale, ScanLine, Sparkles, Stethoscope, Syringe, UserCheck, Utensils,
  type LucideIcon,
} from 'lucide-react';
import type { FeatureIconKey } from '../navigation/featureCatalog.ts';

export const FEATURE_ICONS = {
  Baby, BookOpen, Calculator, ClipboardList, Cpu, HelpCircle, Pill, QrCode,
  Scale, ScanLine, Sparkles, Stethoscope, Syringe, UserCheck, Utensils,
} satisfies Record<FeatureIconKey, LucideIcon>;
