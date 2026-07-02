import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { AlertTriangle, CheckCircle2, ChevronLeft, Home, RefreshCw } from 'lucide-react';
import { usePivot } from '../context/PivotContext';
import { useTheme } from '../context/ThemeContext';
import { GaugeChart } from '../components/GaugeChart';
import { getSimulationResult, type SimulationResultResponse } from '../api/mvp-api';

function statusColor(status: string, c: Record<string, string>) {
  if (status === 'danger' || status === 'DANGER') return c.error;
  if (status === 'warning' || status === 'WARNING') return c.warning;
  return c.success;
}

export function Results() {
  const navigate = useNavigate();
  const { c } = useTheme();
  const { scenarioResultId, profile } = usePivot();
  const [result, setResult] = useState<SimulationResultResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!scenarioResultId) {
      navigate('/simulation-run');
      return;
    }

    let cancelled = false;
    setLoading(true);
    getSimulationResult(scenarioResultId)
      .then((data) => {
        if (!cancelled) setResult(data);
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof Error ? e.message : '결과를 불러오지 못했습니다.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [scenarioResultId, navigate]);

  const housing = result?.thresholds?.[0];
  const rir = housing?.calculatedValue ?? null;
  const rirPct = rir != null ? Math.round(rir * 100) : null;
  const isRedZone = housing?.redZone ?? false;
  const riskStatus = result?.riskStatus ?? 'warning';

  return (
    <div className="h-full overflow-y-auto scrollbar-none p-6">
      <div className="flex items-center gap-3 mb-6">
        <button
          type="button"
          onClick={() => navigate('/simulation-run')}
          className="w-8 h-8 rounded-lg flex items-center justify-center"
          style={{ background: c.card, border: `1px solid ${c.border}`, color: c.textSec }}
        >
          <ChevronLeft size={16} />
        </button>
        <div>
          <p style={{ color: c.textMuted, fontSize: '0.78rem' }}>STEP 4 / 4</p>
          <h2 style={{ color: c.text, fontSize: '1.3rem', fontWeight: 700 }}>RIR · Red Zone 결과</h2>
        </div>
      </div>

      {loading && (
        <div className="flex items-center gap-2 text-sm" style={{ color: c.textSec }}>
          <RefreshCw size={16} className="animate-spin" /> 결과 불러오는 중…
        </div>
      )}

      {error && (
        <div className="rounded-2xl p-4" style={{ background: c.errorBg, color: c.error }}>
          {error}
        </div>
      )}

      {result && !loading && (
        <div className="grid gap-5 lg:grid-cols-2">
          <section
            className="rounded-2xl p-6"
            style={{ background: c.card, border: `1px solid ${c.cardBorder}` }}
          >
            <div className="flex items-start justify-between gap-4">
              <div>
                <p style={{ color: c.textMuted, fontSize: '0.8rem' }}>{profile.currentDistrict}</p>
                <h3 style={{ color: c.text, fontSize: '1.1rem', fontWeight: 700, marginTop: 4 }}>
                  주거비 부담률 (RIR)
                </h3>
              </div>
              {isRedZone ? (
                <span className="flex items-center gap-1 rounded-full px-3 py-1 text-xs font-bold" style={{ background: c.errorBg, color: c.error }}>
                  <AlertTriangle size={14} /> Red Zone
                </span>
              ) : (
                <span className="flex items-center gap-1 rounded-full px-3 py-1 text-xs font-bold" style={{ background: c.successBg, color: c.success }}>
                  <CheckCircle2 size={14} /> 안정
                </span>
              )}
            </div>

            <div className="mt-6 flex flex-col items-center">
              <GaugeChart score={rirPct ?? 0} />
              <p className="mt-4 text-center text-sm" style={{ color: c.textSec }}>
                {rirPct != null ? (
                  <>
                    소득의 <strong style={{ color: c.text }}>{rirPct}%</strong>를 주거비로 사용 중입니다.
                    <br />
                    기준: 40% 초과 시 Red Zone
                  </>
                ) : (
                  'RIR을 계산할 수 없습니다. 소득·주거비를 확인해 주세요.'
                )}
              </p>
            </div>
          </section>

          <section
            className="rounded-2xl p-6 space-y-4"
            style={{ background: c.card, border: `1px solid ${c.cardBorder}` }}
          >
            <h3 style={{ color: c.text, fontWeight: 700 }}>요약</h3>
            <p style={{ color: c.textSec, fontSize: '0.9rem' }}>{result.summary.message}</p>

            <div className="grid grid-cols-2 gap-3 text-sm">
              <div className="rounded-xl p-3" style={{ background: c.badgeBg }}>
                <p style={{ color: c.textMuted }}>위험 점수</p>
                <p style={{ color: statusColor(riskStatus, c), fontSize: '1.25rem', fontWeight: 700 }}>
                  {result.summary.riskScore ?? '-'}
                </p>
              </div>
              <div className="rounded-xl p-3" style={{ background: c.badgeBg }}>
                <p style={{ color: c.textMuted }}>종합 점수</p>
                <p style={{ color: c.text, fontSize: '1.25rem', fontWeight: 700 }}>
                  {result.summary.totalScore ?? '-'}
                </p>
              </div>
              <div className="rounded-xl p-3" style={{ background: c.badgeBg }}>
                <p style={{ color: c.textMuted }}>월 소득</p>
                <p style={{ color: c.text, fontWeight: 600 }}>{profile.monthlyIncome}만원</p>
              </div>
              <div className="rounded-xl p-3" style={{ background: c.badgeBg }}>
                <p style={{ color: c.textMuted }}>월 주거비</p>
                <p style={{ color: c.text, fontWeight: 600 }}>{profile.monthlyHousing}만원</p>
              </div>
            </div>

            {housing?.summary && (
              <p className="text-xs rounded-lg p-3" style={{ background: c.badgeBg, color: c.textMuted }}>
                {housing.summary}
              </p>
            )}

            <button
              type="button"
              onClick={() => navigate('/')}
              className="flex w-full items-center justify-center gap-2 rounded-xl py-3 text-sm font-semibold"
              style={{ background: c.primaryBg, color: c.primary, border: `1px solid ${c.primaryBorder}` }}
            >
              <Home size={16} /> 처음으로
            </button>
          </section>
        </div>
      )}
    </div>
  );
}
