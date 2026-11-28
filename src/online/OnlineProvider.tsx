import React, { useState } from 'react';
import { readOnlineConfiguration } from './config.ts';
import { OnlineController } from './firebaseClient.ts';
import { OnlineContext } from './context.ts';

export const OnlineProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [controller] = useState(() => new OnlineController(readOnlineConfiguration(import.meta.env ?? {})));
  return <OnlineContext.Provider value={controller}>{children}</OnlineContext.Provider>;
};
