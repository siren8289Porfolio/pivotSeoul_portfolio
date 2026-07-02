import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router';
import RunButton from '../components/RunButton';
import LoadingState from '../components/LoadingState';
import AiProgressCard from '../components/AiProgressCard';
import SimulationErrorCard from '../components/SimulationErrorCard';
import { usePivot } from '../context/PivotContext';
import { manwonToWon, useSimulationRun } from '../api/mvp-api';

export function SimulationRun() {
  const navigate = useNavigate();
  const { sessionId, profile, setScenarioResultId } = usePivot();
  const sessionKey = sessionId ? String(sessionId) : '';

  const { data, error, isLoading, execute } = useSimulationRun(sessionKey);
  const autoRan = useRef(false);

  useEffect(() => {
    if (!sessionId) {
      navigate('/stage');
      return;
    }
    if (autoRan.current) return;
    autoRan.current = true;
    execute({
      district: profile.currentDistrict,
      monthly_income: manwonToWon(profile.monthlyIncome),
      monthly_housing_cost: manwonToWon(profile.monthlyHousing),
    });
  }, [sessionId, navigate, execute, profile]);

  useEffect(() => {
    if (data?.runStatus === 'COMPLETED' && data.scenarioResultId) {
      setScenarioResultId(data.scenarioResultId);
    }
  }, [data, setScenarioResultId]);

  const handleRun = () => {
    if (!sessionId) return;
    execute({
      district: profile.currentDistrict,
      monthly_income: manwonToWon(profile.monthlyIncome),
      monthly_housing_cost: manwonToWon(profile.monthlyHousing),
    });
  };

  const goResults = () => {
    if (data?.scenarioResultId) {
      navigate('/results');
    }
  };

  return (
    <main className="mx-auto max-w-4xl px-6 py-10">
      <section className="rounded-3xl bg-gray-50 p-8 dark:bg-slate-900/50">
        <p className="text-sm font-semibold text-indigo-600">MVP · RIR 시뮬레이션</p>
        <h1 className="mt-2 text-3xl font-bold">주거비 부담률 계산</h1>
        <p className="mt-3 text-gray-600 dark:text-gray-400">
          입력하신 소득과 주거비로 RIR(주거비 부담률)을 계산합니다. 40%를 넘으면 Red Zone입니다.
        </p>

        <div className="mt-6 rounded-2xl border bg-white p-5 dark:bg-slate-800">
          <h2 className="font-semibold">입력 요약</h2>
          <div className="mt-3 grid gap-2 text-sm text-gray-700 dark:text-gray-300 md:grid-cols-3">
            <p>지역: {profile.currentDistrict}</p>
            <p>월소득: {profile.monthlyIncome.toLocaleString()}만원</p>
            <p>월주거비: {profile.monthlyHousing.toLocaleString()}만원</p>
          </div>
        </div>

        <div className="mt-6 flex flex-wrap gap-3">
          <RunButton onClick={handleRun} disabled={isLoading || !sessionId} />
          {data?.runStatus === 'COMPLETED' && (
            <button
              type="button"
              onClick={goResults}
              className="rounded-xl bg-indigo-600 px-5 py-2.5 text-sm font-semibold text-white"
            >
              결과 보기
            </button>
          )}
        </div>
      </section>

      <section className="mt-6 space-y-4">
        {isLoading && <LoadingState />}
        {error && <SimulationErrorCard message={error} />}
        {data && data.runStatus === 'COMPLETED' && <AiProgressCard data={data} />}
      </section>
    </main>
  );
}
