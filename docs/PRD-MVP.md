# Pivot Seoul PRD (MVP)

> 생애단계 선택 → 월소득·주거비 입력 → RIR·Red Zone 확인

## 필수 기능 3개

| ID | 기능 | API |
| --- | --- | --- |
| F-001 | 세션·온보딩 | `POST /api/simulation/sessions` |
| F-002 | RIR 시뮬 실행 | `POST /api/simulation-sessions/{id}/run` |
| F-003 | 결과 조회 | `GET /api/simulation/results/{id}` |

## 제외 (v1.0+)

A/B 시나리오, 커리어·보육·노년·정책·LLM, 관리자

## Red Zone 기준

`RIR = 월주거비 / 월소득`, **RIR > 0.4 → Red Zone**
