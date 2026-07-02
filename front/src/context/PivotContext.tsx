import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import type { RunSimulationResponse } from '../api/mvp-api';

export type LifeStage = 'youth' | 'family' | 'senior';

export interface UserProfile {
  lifeStage: LifeStage | null;
  currentDistrict: string;
  monthlyIncome: number;
  monthlyHousing: number;
}

interface PivotContextType {
  sessionId: number | null;
  setSessionId: (sessionId: number | null) => void;
  scenarioResultId: number | null;
  setScenarioResultId: (id: number | null) => void;
  lastRun: RunSimulationResponse | null;
  setLastRun: (run: RunSimulationResponse | null) => void;
  profile: UserProfile;
  updateProfile: (updates: Partial<UserProfile>) => void;
}

const defaultProfile: UserProfile = {
  lifeStage: null,
  currentDistrict: '마포구',
  monthlyIncome: 350,
  monthlyHousing: 120,
};

const PivotContext = createContext<PivotContextType | null>(null);

const SESSION_ID_KEY = 'pivotseoul-session-id';
const RESULT_ID_KEY = 'pivotseoul-scenario-result-id';

function readNumber(key: string): number | null {
  if (typeof window === 'undefined') return null;
  const raw = window.localStorage.getItem(key);
  if (!raw) return null;
  const n = Number(raw);
  return Number.isFinite(n) && n > 0 ? n : null;
}

export const PivotProvider = ({ children }: { children: React.ReactNode }) => {
  const [sessionId, setSessionIdState] = useState<number | null>(() => readNumber(SESSION_ID_KEY));
  const [scenarioResultId, setScenarioResultIdState] = useState<number | null>(() => readNumber(RESULT_ID_KEY));
  const [lastRun, setLastRun] = useState<RunSimulationResponse | null>(null);
  const [profile, setProfile] = useState<UserProfile>(defaultProfile);

  const setSessionId = (id: number | null) => {
    setSessionIdState(id);
    if (typeof window === 'undefined') return;
    if (id) window.localStorage.setItem(SESSION_ID_KEY, String(id));
    else window.localStorage.removeItem(SESSION_ID_KEY);
  };

  const setScenarioResultId = (id: number | null) => {
    setScenarioResultIdState(id);
    if (typeof window === 'undefined') return;
    if (id) window.localStorage.setItem(RESULT_ID_KEY, String(id));
    else window.localStorage.removeItem(RESULT_ID_KEY);
  };

  const updateProfile = useCallback((updates: Partial<UserProfile>) => {
    setProfile((prev) => ({ ...prev, ...updates }));
  }, []);

  useEffect(() => {
    setSessionIdState(readNumber(SESSION_ID_KEY));
    setScenarioResultIdState(readNumber(RESULT_ID_KEY));
  }, []);

  return (
    <PivotContext.Provider
      value={{
        sessionId,
        setSessionId,
        scenarioResultId,
        setScenarioResultId,
        lastRun,
        setLastRun,
        profile,
        updateProfile,
      }}
    >
      {children}
    </PivotContext.Provider>
  );
};

export const usePivot = () => {
  const ctx = useContext(PivotContext);
  if (!ctx) throw new Error('usePivot must be used within PivotProvider');
  return ctx;
};
