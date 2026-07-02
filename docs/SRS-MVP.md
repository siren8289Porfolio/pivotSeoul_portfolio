# Pivot Seoul SRS (MVP)

## FR

| ID | 요구사항 |
| --- | --- |
| FR-001 | 생애단계 선택 시 익명 세션 생성 |
| FR-002 | 월소득·주거비·거주 구 저장 |
| FR-003 | RIR 계산 및 Run·결과 저장 |
| FR-004 | RIR·Red Zone·위험점수 조회 |

## NFR

- 결과 조회 P95 1초
- 시뮬 실행 P95 30초
- FastAPI 직접 호출 금지 (Spring 게이트웨이)

## TC

- TC-001: 세션 201 + sessionId
- TC-002: 소득 500만·주거비 225만 → rir=0.45, red_zone=true
- TC-003: GET 결과에 threshold 포함
