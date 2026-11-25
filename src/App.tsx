import React from 'react';
import { ManoProvider, useMano } from './state/ManoStore.tsx';
import { AppShell } from './components/AppShell.tsx';
import { HomeScreen } from './screens/HomeScreen.tsx';
import { SymptomNavigatorScreen } from './screens/SymptomNavigatorScreen.tsx';
import { FeverDoseCalculatorScreen } from './screens/FeverDoseCalculatorScreen.tsx';
import { SyrupScannerScreen } from './screens/SyrupScannerScreen.tsx';
import { VaccinesScreen } from './screens/VaccinesScreen.tsx';
import { GrowthScreen } from './screens/GrowthScreen.tsx';
import { NutritionAdvisorScreen } from './screens/NutritionAdvisorScreen.tsx';
import { TreatmentPlanScreen } from './screens/TreatmentPlanScreen.tsx';
import { MedicinesScreen } from './screens/MedicinesScreen.tsx';
import { MedicineDetailScreen } from './screens/MedicineDetailScreen.tsx';
import { TopicsScreen } from './screens/TopicsScreen.tsx';
import { TopicDetailScreen } from './screens/TopicDetailScreen.tsx';
import { FaqScreen } from './screens/FaqScreen.tsx';
import { SupplementsHygieneScreen } from './screens/SupplementsHygieneScreen.tsx';
import { SkinScreen } from './screens/SkinScreen.tsx';
import { DevicesScreen } from './screens/DevicesScreen.tsx';
import { AskPharmacistScreen } from './screens/AskPharmacistScreen.tsx';
import { CareCardsScreen } from './screens/CareCardsScreen.tsx';
import { MyChildScreen } from './screens/MyChildScreen.tsx';

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
        <RouteView />
      </AppShell>
    </ManoProvider>
  );
};

export default App;
