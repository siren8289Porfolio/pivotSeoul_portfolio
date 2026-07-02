import { useState } from 'react';
import { useNavigate } from 'react-router';
import { ChevronLeft, ChevronRight, MapPin, Wallet, Loader2 } from 'lucide-react';
import { usePivot, LifeStage } from '../context/PivotContext';
import { useTheme } from '../context/ThemeContext';
import { createSession, manwonToWon } from '../api/mvp-api';

const DISTRICTS = [
  '강남구', '강동구', '강북구', '강서구', '관악구', '광진구', '구로구', '금천구',
  '노원구', '도봉구', '동대문구', '동작구', '마포구', '서대문구', '서초구', '성동구',
  '성북구', '송파구', '양천구', '영등포구', '용산구', '은평구', '종로구', '중구', '중랑구',
];

function SliderField({
  label, value, min, max, step, unit, onChange, hint, color, c,
}: {
  label: string; value: number; min: number; max: number; step: number;
  unit: string; onChange: (v: number) => void; hint?: string; color: string; c: Record<string, string>;
}) {
  const pct = ((value - min) / (max - min)) * 100;
  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <span style={{ color: c.textSec, fontSize: '0.85rem' }}>{label}</span>
        <span style={{ color: c.text, fontSize: '1rem', fontWeight: 600 }}>{value.toLocaleString()}{unit}</span>
      </div>
      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        onChange={(e) => onChange(Number(e.target.value))}
        className="w-full h-1.5 rounded-full appearance-none cursor-pointer"
        style={{
          background: `linear-gradient(to right, ${color} 0%, ${color} ${pct}%, ${c.borderSoft} ${pct}%, ${c.borderSoft} 100%)`,
          outline: 'none',
        }}
      />
      {hint && <p style={{ color: `${color}CC`, fontSize: '0.75rem' }}>{hint}</p>}
    </div>
  );
}

const stageLabel: Record<LifeStage, string> = {
  youth: '청년기',
  family: '신혼·출산기',
  senior: '노년기',
};

export function Onboarding() {
  const navigate = useNavigate();
  const { profile, updateProfile, setSessionId } = usePivot();
  const { c, isDark } = useTheme();
  const [step, setStep] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const housingRatio = profile.monthlyIncome > 0
    ? Math.round((profile.monthlyHousing / profile.monthlyIncome) * 100)
    : 0;

  const handleSubmit = async () => {
    if (!profile.lifeStage) {
      setError('생애단계를 먼저 선택해 주세요.');
      navigate('/stage');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      const session = await createSession({
        lifeStageCode: profile.lifeStage,
        currentDistrict: profile.currentDistrict,
        monthlyIncome: manwonToWon(profile.monthlyIncome),
        monthlyHousing: manwonToWon(profile.monthlyHousing),
      });
      setSessionId(session.sessionId);
      navigate('/simulation-run');
    } catch (e) {
      setError(e instanceof Error ? e.message : '세션 생성에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="h-full flex flex-col p-4 md:p-6 w-full">
      <div className="flex items-center gap-3 mb-4 shrink-0">
        <button
          onClick={() => (step === 0 ? navigate('/stage') : setStep(step - 1))}
          className="w-8 h-8 rounded-lg flex items-center justify-center"
          style={{ background: c.card, border: `1px solid ${c.border}`, color: c.textSec }}
        >
          <ChevronLeft size={16} />
        </button>
        <div className="flex-1">
          <p style={{ color: c.textMuted, fontSize: '0.72rem' }}>STEP 2 / 4 · {stageLabel[profile.lifeStage ?? 'youth']}</p>
          <h2 style={{ color: c.text, fontSize: '1.1rem', fontWeight: 700 }}>주거 조건 입력</h2>
        </div>
      </div>

      <div
        className="flex-1 min-h-0 overflow-y-auto scrollbar-none rounded-2xl p-5 md:p-6"
        style={{ background: c.card, border: `1px solid ${c.cardBorder}`, boxShadow: c.cardShadow }}
      >
        {step === 0 && (
          <div className="space-y-5">
            <div className="flex items-center gap-2 mb-2">
              <MapPin size={18} style={{ color: c.primary }} />
              <span style={{ color: c.text, fontWeight: 600 }}>거주 자치구</span>
            </div>
            <div className="grid grid-cols-5 gap-1.5 max-h-52 overflow-y-auto pr-1">
              {DISTRICTS.map((d) => (
                <button
                  key={d}
                  type="button"
                  onClick={() => updateProfile({ currentDistrict: d })}
                  className="px-2 py-1.5 rounded-lg text-xs transition-all"
                  style={{
                    background: profile.currentDistrict === d ? c.primaryBg : (isDark ? 'rgba(15,23,42,0.5)' : '#F8FAFC'),
                    border: `1px solid ${profile.currentDistrict === d ? c.primary : c.borderSoft}`,
                    color: profile.currentDistrict === d ? c.primary : c.textMuted,
                  }}
                >
                  {d}
                </button>
              ))}
            </div>
          </div>
        )}

        {step === 1 && (
          <div className="space-y-5">
            <div className="flex items-center gap-2 mb-2">
              <Wallet size={18} style={{ color: c.primary }} />
              <span style={{ color: c.text, fontWeight: 600 }}>소득 · 주거비</span>
            </div>
            <SliderField
              label="월 소득 (세후)"
              value={profile.monthlyIncome}
              min={150}
              max={800}
              step={10}
              unit="만원"
              onChange={(v) => updateProfile({ monthlyIncome: v })}
              hint="서울 평균 약 280만원"
              color={c.primary}
              c={c}
            />
            <SliderField
              label="월 주거비"
              value={profile.monthlyHousing}
              min={20}
              max={300}
              step={5}
              unit="만원"
              onChange={(v) => updateProfile({ monthlyHousing: v })}
              hint={`주거비율 약 ${housingRatio}% · RIR 기준 40% 초과 시 Red Zone`}
              color={housingRatio > 40 ? c.error : housingRatio > 30 ? c.warning : c.success}
              c={c}
            />
          </div>
        )}

        {error && (
          <p className="mt-4 text-sm rounded-xl px-4 py-3" style={{ background: c.errorBg, color: c.error }}>
            {error}
          </p>
        )}
      </div>

      <div className="flex items-center justify-between mt-4 shrink-0">
        <button
          onClick={() => (step === 0 ? navigate('/stage') : setStep(step - 1))}
          className="flex items-center gap-2 px-4 py-2 rounded-xl"
          style={{ background: c.card, border: `1px solid ${c.border}`, color: c.textSec, fontSize: '0.88rem' }}
        >
          <ChevronLeft size={15} /> 이전
        </button>
        <button
          disabled={submitting}
          onClick={() => (step < 1 ? setStep(step + 1) : handleSubmit())}
          className="flex items-center gap-2 px-6 py-2.5 rounded-xl disabled:opacity-60"
          style={{
            background: 'linear-gradient(135deg, #6366F1, #818CF8)',
            color: 'white',
            fontSize: '0.9rem',
            fontWeight: 600,
          }}
        >
          {submitting ? (
            <>
              <Loader2 size={16} className="animate-spin" /> 저장 중…
            </>
          ) : step < 1 ? (
            <>
              다음 <ChevronRight size={15} />
            </>
          ) : (
            '시뮬레이션 실행'
          )}
        </button>
      </div>
    </div>
  );
}
