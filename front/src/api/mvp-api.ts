/**
 * Pivot Seoul MVP API — 세션 생성 · RIR 실행 · 결과 조회
 */

import { useCallback, useState } from 'react';

const API_BASE =
  (process.env.NEXT_PUBLIC_API_BASE_URL ?? process.env.NEXT_PUBLIC_API_BASE ?? 'http://localhost:8080').replace(/\/$/, '');

export type LifeStageCode = 'youth' | 'family' | 'senior';

export interface CreateSessionRequest {
  lifeStageCode: LifeStageCode;
  currentDistrict: string;
  monthlyIncome: number;
  monthlyHousing: number;
}

export interface CreateSessionResponse {
  ok?: boolean;
  data?: {
    sessionId: number;
    sessionUuid: string;
    sessionStatus: string;
  };
  sessionId?: number;
  sessionUuid?: string;
  sessionStatus?: string;
}

export interface RunSimulationRequest {
  district: string;
  monthly_income: number;
  monthly_housing_cost: number;
}

export interface ThresholdResult {
  thresholdType: string;
  calculatedValue: number | null;
  thresholdValue: number;
  status: string;
  isRedZone: boolean;
}

export interface RunSimulationResponse {
  sessionId: string;
  scenarioResultId: number | null;
  runStatus: 'COMPLETED' | 'FAILED';
  resultStatus: string;
  rir: number | null;
  riskScore: number;
  confidenceScore: number;
  thresholdResults: ThresholdResult[];
  aiResult?: Record<string, unknown>;
}

export interface ThresholdResultDto {
  thresholdResultId: number;
  thresholdTypeId: number;
  status: string;
  calculatedValue: number | null;
  thresholdValue: number | null;
  redZone: boolean;
  summary: string | null;
}

export interface SimulationResultResponse {
  resultId: number;
  simulationRunId: number;
  scenarioId: number;
  resultStatus: string;
  riskStatus: 'safe' | 'warning' | 'danger';
  summary: {
    totalScore: number | null;
    riskScore: number | null;
    confidenceScore: number | null;
    redZoneCount: number;
    message: string;
  };
  scores: Record<string, number | null>;
  thresholds: ThresholdResultDto[];
}

function unwrapSession(json: CreateSessionResponse): {
  sessionId: number;
  sessionUuid: string;
  sessionStatus: string;
} {
  if (json.data?.sessionId) {
    return {
      sessionId: json.data.sessionId,
      sessionUuid: json.data.sessionUuid,
      sessionStatus: json.data.sessionStatus,
    };
  }
  if (json.sessionId) {
    return {
      sessionId: json.sessionId,
      sessionUuid: json.sessionUuid ?? '',
      sessionStatus: json.sessionStatus ?? 'READY',
    };
  }
  throw new Error('세션 생성 응답 형식이 올바르지 않습니다.');
}

export async function createSession(body: CreateSessionRequest) {
  const res = await fetch(`${API_BASE}/api/simulation/sessions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      lifeStageCode: body.lifeStageCode,
      currentDistrict: body.currentDistrict,
      compareDistrict: null,
      monthlyIncome: body.monthlyIncome,
      monthlyHousing: body.monthlyHousing,
      monthlyLiving: null,
      commuteTime: null,
      childcareCost: null,
      returnToWorkMonths: null,
      retirementAge: null,
      savings: null,
    }),
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(`세션 생성 실패 (${res.status}): ${text}`);
  }

  const json = (await res.json()) as CreateSessionResponse;
  return unwrapSession(json);
}

export async function runSimulation(sessionId: number, body: RunSimulationRequest) {
  const res = await fetch(`${API_BASE}/api/simulation-sessions/${sessionId}/run`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

  const data = (await res.json()) as RunSimulationResponse;
  if (!res.ok) {
    throw new Error(
      (data as unknown as { message?: string }).message ??
        `시뮬레이션 실행 실패 (${res.status})`
    );
  }
  return data;
}

export async function getSimulationResult(scenarioResultId: number) {
  const res = await fetch(`${API_BASE}/api/simulation/results/${scenarioResultId}`);
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`결과 조회 실패 (${res.status}): ${text}`);
  }
  return res.json() as Promise<SimulationResultResponse>;
}

/** 만원 단위 → 원 단위 */
export function manwonToWon(manwon: number): number {
  return Math.round(manwon * 10_000);
}

export { API_BASE };

export function useSimulationRun(sessionId: string) {
  const [data, setData] = useState<RunSimulationResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const execute = useCallback(async (body: RunSimulationRequest) => {
    if (!sessionId) {
      setError('세션이 없습니다. 온보딩부터 다시 진행해 주세요.');
      return;
    }

    setIsLoading(true);
    setError(null);
    setData(null);

    try {
      const result = await runSimulation(Number(sessionId), body);
      setData(result);

      if (result.runStatus === 'FAILED') {
        setError(
          String(result.aiResult?.detail ?? result.aiResult?.error ?? 'AI 분석에 실패했습니다.')
        );
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : '알 수 없는 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, [sessionId]);

  return { data, error, isLoading, execute };
}
