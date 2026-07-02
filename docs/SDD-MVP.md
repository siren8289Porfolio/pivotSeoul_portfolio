# Pivot Seoul SDD (MVP)

## 플로우

```
Home → Stage → Onboarding → SimulationRun → Results
```

## 계층

| 계층 | 책임 |
| --- | --- |
| `front/src/api/mvp-api.ts` | 3개 API 클라이언트 |
| `SimulationSessionService` | 세션 + Scenario(A) 생성 |
| `SimulationEngineService` | housingAnalyze → 저장 |
| `SimulationResultSaveService` | scenario_result, threshold_result |
| `fastapi/.../housing/rir.py` | RIR 알고리즘 |

## RIR

```
RIR ≤ 0.30 → stable, risk=20
RIR ≤ 0.40 → warning, risk=50
RIR >  0.40 → danger, red_zone=true, risk=80
```

## DB (MVP)

`simulation_session`, `user_condition`, `scenario`, `simulation_run`, `scenario_result`, `threshold_result`

Seed: `V6__mvp_seed_data.sql` (life_stage, threshold_type HOUSING)
