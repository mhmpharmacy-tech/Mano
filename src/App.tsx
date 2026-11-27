import React from 'react';
import { ManoProvider, useMano } from './state/ManoStore.tsx';
import { AppShell } from './components/AppShell.tsx';
import { HomeScreen } from './screens/HomeScreen.tsx';
const SymptomNavigatorScreen = React.lazy(() => import('./screens/SymptomNavigatorScreen.tsx').then((module) => ({ default: module.SymptomNavigatorScreen })));
const FeverDoseCalculatorScreen = React.lazy(() => import('./screens/FeverDoseCalculatorScreen.tsx').then((module) => ({ default: module.FeverDoseCalculatorScreen })));
const SyrupScannerScreen = React.lazy(() => import('./screens/SyrupScannerScreen.tsx').then((module) => ({ default: module.SyrupScannerScreen })));
const VaccinesScreen = React.lazy(() => import('./screens/VaccinesScreen.tsx').then((module) => ({ default: module.VaccinesScreen })));
const GrowthScreen = React.lazy(() => import('./screens/GrowthScreen.tsx').then((module) => ({ default: module.GrowthScreen })));
const NutritionAdvisorScreen = React.lazy(() => import('./screens/NutritionAdvisorScreen.tsx').then((module) => ({ default: module.NutritionAdvisorScreen })));
const TreatmentPlanScreen = React.lazy(() => import('./screens/TreatmentPlanScreen.tsx').then((module) => ({ default: module.TreatmentPlanScreen })));
const MedicinesScreen = React.lazy(() => import('./screens/MedicinesScreen.tsx').then((module) => ({ default: module.MedicinesScreen })));
const MedicineDetailScreen = React.lazy(() => import('./screens/MedicineDetailScreen.tsx').then((module) => ({ default: module.MedicineDetailScreen })));
const TopicsScreen = React.lazy(() => import('./screens/TopicsScreen.tsx').then((module) => ({ default: module.TopicsScreen })));
const TopicDetailScreen = React.lazy(() => import('./screens/TopicDetailScreen.tsx').then((module) => ({ default: module.TopicDetailScreen })));
const FaqScreen = React.lazy(() => import('./screens/FaqScreen.tsx').then((module) => ({ default: module.FaqScreen })));
const SupplementsHygieneScreen = React.lazy(() => import('./screens/SupplementsHygieneScreen.tsx').then((module) => ({ default: module.SupplementsHygieneScreen })));
const SkinScreen = React.lazy(() => import('./screens/SkinScreen.tsx').then((module) => ({ default: module.SkinScreen })));
const DevicesScreen = React.lazy(() => import('./screens/DevicesScreen.tsx').then((module) => ({ default: module.DevicesScreen })));
const AskPharmacistScreen = React.lazy(() => import('./screens/AskPharmacistScreen.tsx').then((module) => ({ default: module.AskPharmacistScreen })));
const CareCardsScreen = React.lazy(() => import('./screens/CareCardsScreen.tsx').then((module) => ({ default: module.CareCardsScreen })));
const MyChildScreen = React.lazy(() => import('./screens/MyChildScreen.tsx').then((module) => ({ default: module.MyChildScreen })));

const RouteView: React.FC = () => {
  const { route } = useMano();

  switch (route.name) {
    case 'home':
      return <HomeScreen />;
    case 'symptoms':
      return <SymptomNavigatorScreen key={route.initialFlowId || 'default'} initialFlowId={route.initialFlowId} />;
    case 'fever-dose':
      return (
        <FeverDoseCalculatorScreen
          key={route.prefillFormulationId || 'default'}
          prefillFormulationId={route.prefillFormulationId}
        />
      );
    case 'syrup-scanner':
      return <SyrupScannerScreen />;
    case 'vaccines':
      return <VaccinesScreen />;
    case 'growth':
      return <GrowthScreen key={route.initialTab || 'chart'} initialTab={route.initialTab} />;
    case 'nutrition':
      return <NutritionAdvisorScreen />;
    case 'treatment-plan':
      return <TreatmentPlanScreen />;
    case 'medicines':
      return <MedicinesScreen />;
    case 'medicine-detail':
      return <MedicineDetailScreen key={route.slug} slug={route.slug} />;
    case 'topics':
      return <TopicsScreen />;
    case 'topic-detail':
      return <TopicDetailScreen key={route.slug} slug={route.slug} />;
    case 'faq':
      return <FaqScreen />;
    case 'supplements-hygiene':
      return (
        <SupplementsHygieneScreen
          key={route.initialTab || 'supplements'}
          initialTab={route.initialTab}
        />
      );
    case 'skin':
      return <SkinScreen />;
    case 'devices':
      return <DevicesScreen />;
    case 'ask-pharmacist':
      return (
        <AskPharmacistScreen
          key={route.initialTopic || 'default'}
          initialTopic={route.initialTopic}
        />
      );
    case 'care-cards':
      return <CareCardsScreen />;
    case 'my-child':
      return <MyChildScreen />;
  }
};

export const App: React.FC = () => {
  return (
    <ManoProvider>
      <AppShell>
        <React.Suspense fallback={<p role="status" className="py-8 text-center text-sm">در حال بازکردن بخش…</p>}>
          <RouteView />
        </React.Suspense>
      </AppShell>
    </ManoProvider>
  );
};

export default App;
