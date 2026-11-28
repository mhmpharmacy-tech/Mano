import { createContext, useContext, useSyncExternalStore } from 'react';
import { OnlineController } from './firebaseClient.ts';

export const OnlineContext = createContext<OnlineController | null>(null);

export function useOnline() {
  const controller = useContext(OnlineContext);
  if (!controller) throw new Error('OnlineProvider is required');
  const session = useSyncExternalStore(controller.subscribe, controller.getSession, controller.getSession);
  return { controller, session, enabled: controller.enabled };
}
