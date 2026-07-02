# Pivot Seoul Operation Manual (MVP)

## Operation Manual / 운영 계획서

---

## 1. 문서 정보

| 항목 | 내용 |
| --- | --- |
| 문서명 | Pivot Seoul Operation Manual (MVP) |
| 버전 | v0.1-mvp |
| 작성일 | 2026-07-02 |
| 작성자 | Pivot Seoul 운영 |
| 대상 서비스 | Pivot Seoul (Life Pivot) — RIR 시뮬레이션 MVP |
| 운영 환경 | Production (OCI VM + Docker Compose) / Local |
| 관련 문서 | `docs/DEPLOYMENT-MVP.md`, `docs/TEST-QAQC-MVP.md`, `docs/SDD-MVP.md` |

---

## 2. 문서 목적

배포 이후 **Pivot Seoul MVP**를 안정적으로 운영하기 위한 기준·Runbook·장애 대응·백업 정책을 정의한다.

```
Deployment → Operation → (모니터링·장애대응·백업·변경관리)
```

**운영 핵심 질문 (MVP):**

| 질문 | 답 (요약) |
| --- | --- |
| 정상 여부? | 4컨테이너 healthy + `/health` + MVP API 3종 |
| 장애 시? | SEV 등급 → Runbook → 롤백 (`DEPLOYMENT-MVP.md` §11) |
| 로그? | `docker compose logs` (back, fastapi, front) |
| 백업? | PostgreSQL `pg_dump` 일 1회 |
| 변경? | `main` push → GH Actions → Change 기록 |

---

## 3. 운영 범위

| 구분 | 운영 대상 | MVP 설명 |
| --- | --- | --- |
| Application | front, back, fastapi | Docker Compose 4서비스 |
| Database | PostgreSQL 16 (`pivotseoul-db`) | Flyway V1~V6, `db_data` volume |
| Infrastructure | OCI VM (opc), Docker | `~/pivotSeoul` |
| CI/CD | GitHub Actions `deploy.yml` | GHCR push + SSH deploy |
| Monitoring | Docker healthcheck, 로그 | 외부 APM 없음 (MVP) |
| Security | `.env` Secret, CORS | JWT 미사용(MVP), 익명 세션 |

**MVP 운영 제외:** 관리자 콘솔 SLA, LLM API 키 로테이션, Vector DB

---

## 4. 운영 역할과 책임

| 역할 | MVP 책임 |
| --- | --- |
| Service Owner | 운영 최종 승인, SEV-1 공지 |
| Backend Engineer | Spring, Flyway, `/api/simulation/*` 장애 |
| AI Engineer | FastAPI housing, `/api/ai/status` |
| Frontend Engineer | Next.js front, CORS·API URL 이슈 |
| DevOps | VM, docker-compose, GH Actions, 백업 |
| QA | 장애 재현, P0 TC 회귀 |
| PM | 사용자 영향 판단, 점검 공지 |

---

## 5. 서비스 상태 기준

| 상태 | 의미 | MVP 판정 기준 |
| --- | --- | --- |
| Normal | 정상 | 4컨테이너 up, `/health` ok, Run API COMPLETED |
| Warning | 주의 | Run 지연(>30s), 간헐 5xx, 디스크 >70% |
| Incident | 장애 | Front/Back 다운, Run 전부 FAILED, DB down |
| Maintenance | 점검 | `docker-compose up` 중, Flyway migrate |
| Degraded | 부분 장애 | FastAPI만 down → Run FAILED, 조회는 가능 |

---

## 6. 모니터링 항목

| 영역 | 지표 | MVP 확인 방법 | 기준 |
| --- | --- | --- | --- |
| Container | running/healthy | `docker compose ps` | 4/4 healthy |
| Spring | `/health` | `curl HOST:8080/health` | `status: ok` |
| FastAPI | `/health` | `curl` (internal) 또는 `/api/ai/status` | 200 |
| Front | HTTP 200 | `curl -I HOST:3000` | 200 |
| MVP API | sessions/run/results | curl (TEST-QAQC §10) | P0 Pass |
| DB | connection | back 로그, `pg_isready` | 연결 성공 |
| VM | CPU/Mem/Disk | `top`, `df -h` | Disk <80% |
| CI/CD | deploy job | GitHub Actions | Success |
| Business | Run 완료율 | `simulation_run.run_status` | FAILED 급증 시 조사 |

---

## 7. 알림 기준

| Alert ID | 조건 | 심각도 | MVP 대응 |
| --- | --- | --- | --- |
| ALT-001 | `docker compose ps`에 unhealthy | Critical | Runbook §10.1 |
| ALT-002 | `/health` 비200 | Critical | Back 재시작·로그 |
| ALT-003 | `/api/ai/status` fastapi down | Major | FastAPI 재시작 |
| ALT-004 | Run API 전부 503 | Major | FastAPI·네트워크 확인 |
| ALT-005 | GitHub deploy 실패 | Major | Runbook §10.3 |
| ALT-006 | Disk >80% | Warning | 로그·이미지 정리 |
| ALT-007 | Flyway migrate 실패 | Critical | DB 백업·롤백 검토 |

**MVP 알림 채널:** [Slack / 이메일 / 수동 점검] — 운영 인수 시 지정

---

## 8. 운영 점검 체크리스트

### 8.1 일일 점검 (5분)

| 점검 항목 | 명령/방법 | 확인 |
| --- | --- | --- |
| 컨테이너 상태 | `docker compose ps` | □ |
| Spring health | `curl -s HOST:8080/health` | □ |
| AI bridge | `curl -s HOST:8080/api/ai/status` | □ |
| Front 접속 | 브라우저 `HOST:3000` | □ |
| Back error 로그 | `docker compose logs --tail=50 back \| grep -i error` | □ |
| 전일 deploy 성공 | GitHub Actions | □ |

### 8.2 주간 점검

| 점검 항목 | 확인 |
| --- | --- |
| DB 백업 파일 존재·크기 정상 | □ |
| `docker system df` / 미사용 이미지 정리 | □ |
| Open Incident/Bug 리뷰 | □ |
| `simulation_run` FAILED 비율 확인 | □ |
| `.env` Secret 유출 없음 (git) | □ |
| TEST-QAQC P0 샘플 실행 (curl) | □ |

### 8.3 월간 점검

| 점검 항목 | 확인 |
| --- | --- |
| OCI VM 비용·디스크 | □ |
| PostgreSQL volume 용량 | □ |
| Postmortem 액션 완료 여부 | □ |
| 의존성 보안 업데이트 검토 (Java, Node, Python) | □ |
| 운영·배포 문서 최신화 | □ |

---

## 9. Incident 관리

### 9.1 Incident 등급 (MVP)

| 등급 | 기준 | 예시 | 대응 |
| --- | --- | --- | --- |
| SEV-1 | 서비스 전체 불가 | VM down, DB corrupt | 즉시, 롤백 |
| SEV-2 | 핵심 기능 불가 | Run API 전부 실패 | 1시간 내 |
| SEV-3 | 일부 기능 | 결과 조회만 실패 | 업무일 내 |
| SEV-4 | 경미 | UI 문구, 만원 표시 오류 | 다음 배포 |

### 9.2 대응 절차

```
1. 감지 (점검/사용자/CI)
2. SEV 지정 + 담당 배정
3. Runbook 실행 (§10)
4. 필요 시 롤백 (DEPLOYMENT-MVP §11)
5. Normal 복구 확인 (§6 모니터링)
6. Incident 기록 (§9.3)
7. SEV-1~2: Postmortem (§16)
```

### 9.3 Incident 기록 양식

| 항목 | 내용 |
| --- | --- |
| Incident ID | INC-XXX |
| 발생/종료 | YYYY-MM-DD HH:mm |
| 등급 | SEV-1~4 |
| 영향 | Front / Run API / DB / 전체 |
| 증상 | |
| 원인 | |
| 조치 | |
| 담당자 | |
| 상태 | Open / Resolved / Closed |

---

## 10. Runbook

### 10.1 전체 스택 재시작 (VM: `~/pivotSeoul`)

| Step | 작업 | 명령 |
| --- | --- | --- |
| 1 | 상태 확인 | `docker compose ps` |
| 2 | 로그 스냅샷 | `docker compose logs --tail=100 > /tmp/pivot-logs.txt` |
| 3 | 재시작 | `sudo docker-compose restart` |
| 4 | 또는 순차 up | `sudo docker-compose up -d` |
| 5 | Health | `curl -s localhost:8080/health` |
| 6 | MVP API | sessions → run → results (curl) |
| 7 | 기록 | §17 운영 이력 |

### 10.2 FastAPI 장애 (Run FAILED, 503)

| Step | 작업 | 확인 |
| --- | --- | --- |
| 1 | 상태 | `curl HOST:8080/api/ai/status` |
| 2 | FastAPI 로그 | `docker compose logs --tail=100 fastapi` |
| 3 | 컨테이너 재시작 | `docker compose restart fastapi` |
| 4 | 내부 health | `wget -qO- http://localhost:8000/health` (컨테이너 내) |
| 5 | Back→AI URL | `PIVOT_FASTAPI_BASE_URL=http://fastapi:8000` |
| 6 | Run 재테스트 | POST run → COMPLETED |

### 10.3 DB 연결 장애

| Step | 작업 | 확인 |
| --- | --- | --- |
| 1 | DB 컨테이너 | `docker compose ps db` |
| 2 | pg ready | `docker exec pivotseoul-db pg_isready -U pivotseoul` |
| 3 | Back 로그 | `Connection refused` / auth failed |
| 4 | `.env` | `POSTGRES_PASSWORD`, JDBC URL |
| 5 | DB 재시작 | `docker compose restart db` → `restart back` |
| 6 | Flyway | back 기동 로그 migrate 성공 |

### 10.4 Front CORS / API 호출 실패

| Step | 작업 | 확인 |
| --- | --- | --- |
| 1 | 브라우저 콘솔 | CORS error |
| 2 | `PIVOT_CORS_ORIGINS` | 실제 front origin 포함 |
| 3 | `NEXT_PUBLIC_API_BASE_URL` | 브라우저가 reach 가능한 back URL |
| 4 | front 재빌드 필요 시 | 이미지 재배포 |
| 5 | E2E | Home → Results |

### 10.5 배포 실패 (GitHub Actions)

| Step | 작업 | 확인 |
| --- | --- | --- |
| 1 | Actions 로그 | build vs deploy 단계 |
| 2 | GHCR push | 권한·토큰 |
| 3 | SSH deploy | `SERVER_HOST`, `SERVER_SSH_KEY` |
| 4 | VM | `docker-compose pull` 수동 실행 |
| 5 | 이전 이미지 유지 | 실패 시 롤백 안 함 |
| 6 | 수정 후 재 push | |

### 10.6 Flyway 마이그레이션 실패

| Step | 작업 | 확인 |
| --- | --- | --- |
| 1 | back 로그 | Flyway error |
| 2 | `flyway_schema_history` | DB 조회 |
| 3 | 백업 확인 | 최신 `pg_dump` |
| 4 | 수정 SQL 또는 repair | DBA/Backend 협의 |
| 5 | 복구 후 back 재기동 | |

---

## 11. Problem 관리

| Problem ID | 관련 Incident | 문제 | 원인 | 재발방지 | 상태 |
| --- | --- | --- | --- | --- | --- |
| PRB-001 | — | Run 간헐 503 | FastAPI OOM | memory limit 설정 | Open |
| PRB-002 | — | CORS 오류 | ORIGINS 미설정 | `.env` 체크리스트 | Open |

---

## 12. Change 관리

| Change ID | 변경 | 영향 | 승인 | 배포 | 롤백 |
| --- | --- | --- | --- | --- | --- |
| CHG-001 | MVP 최초 배포 | 높음 | Owner | main push | 이전 이미지 |
| CHG-002 | Flyway V7+ | 중간 | Backend+DevOps | back 배포 | DB restore |

### 12.1 변경 체크리스트

| 항목 | 확인 |
| --- | --- |
| TEST-QAQC P0 영향 분석 | □ |
| Flyway 변경 시 백업 | □ |
| `.env` 변경 공유 | □ |
| DEPLOYMENT-MVP 롤백 준비 | □ |

---

## 13. 백업 및 복구

| 대상 | 주기 | 보관 | 방법 |
| --- | --- | --- | --- |
| PostgreSQL | Daily | 7일 (MVP) | `pg_dump` |
| `db_data` volume | Weekly | 4주 | VM 스냅샷 (선택) |
| `.env` | 변경 시 | 최신 1부 | VM 외부 안전 저장 |
| `docker-compose.yml` | git | 무기한 | GitHub |
| 로그 | — | 7일 | `docker compose logs` 로컬 |

### 13.1 DB 백업 명령

```bash
# VM
docker exec pivotseoul-db pg_dump -U pivotseoul pivotseoul \
  | gzip > ~/backups/pivotseoul_$(date +%Y%m%d_%H%M).sql.gz
```

### 13.2 DB 복구 명령

```bash
gunzip -c ~/backups/pivotseoul_YYYYMMDD.sql.gz \
  | docker exec -i pivotseoul-db psql -U pivotseoul pivotseoul
# 이후 back 재시작
docker compose restart back
```

### 13.3 복구 절차

```
1. SEV·영향 범위 확인
2. 최신 정상 백업 선택
3. 서비스 중지 (back, front) 또는 read-only
4. pg_restore / psql 복구
5. Flyway history 정합성 확인
6. Health + MVP API 검증
7. Incident/운영 이력 기록
```

---

## 14. 보안 운영 (MVP)

| 항목 | 기준 |
| --- | --- |
| Secret | `.env` Git 미커밋, VM 권한 opc 제한 |
| DB 비밀번호 | 강도 정책, 주기 교체(분기) |
| FastAPI | `internal` 네트워크만 — 외부 포트 미개방 |
| CORS | `PIVOT_CORS_ORIGINS` 화이트리스트만 |
| SSH | 키 기반, `SERVER_SSH_KEY` GitHub Secret |
| 익명 세션 | PII 최소 저장 (`user_condition`) |
| 관리자 | MVP 미운영 — `/admin` 모니터링만 |

---

## 15. 운영 지표 (MVP 목표)

| 지표 | 설명 | 목표 |
| --- | --- | --- |
| Availability | 일일 health check 성공 | 99% (수동 점검 기준) |
| Run 성공률 | COMPLETED / 전체 Run | 95% |
| API P95 (결과 조회) | GET results | <1s |
| Run P95 | housing 포함 | <30s |
| Incident (SEV-1~2) | 월 건수 | 0 목표 |
| MTTR | SEV-2 복구 | <2h |
| Deploy 성공률 | main push | 100% |
| 백업 성공 | 일일 dump | 100% |

---

## 16. Postmortem

### 16.1 작성 기준

- SEV-1, SEV-2 Incident 종료 후 **7일 이내**
- 비난이 아닌 **재발방지** 목적

### 16.2 템플릿

| 항목 | 내용 |
| --- | --- |
| Incident ID | INC-XXX |
| 제목 | |
| 시간 | 발생 ~ 종료 |
| 영향 | 사용자 수 / Run 실패 건 |
| 원인 | |
| 탐지 | 점검 / 사용자 / CI |
| 타임라인 | HH:mm 조치별 |
| 잘된 점 | |
| 개선점 | |
| Action Items | Owner + 기한 |
| 상태 | Open / Done |

---

## 17. 운영 이력

| 날짜 | 유형 | 내용 | 담당자 | 결과 |
| --- | --- | --- | --- | --- |
| YYYY-MM-DD | 인수 | Operation Manual MVP 작성 | — | 완료 |
| YYYY-MM-DD | 점검 | 일일 점검 | | |
| YYYY-MM-DD | 배포 | MVP v0.1.0 | | |

---

## 18. 운영 완료/인수 기준

| 기준 | 확인 |
| --- | --- |
| 일일/주간 점검 담당자 지정 | □ |
| Runbook §10 담당자 숙지 | □ |
| DB 백업 cron 또는 수동 일정 | □ |
| ALT 채널(Slack 등) 연결 | □ |
| DEPLOYMENT-MVP 롤백 실습 1회 | □ |
| TEST-QAQC curl 3종 운영용 보관 | □ |
| Incident/Postmortem 양식 공유 | □ |
| `.env` 백업 위치 문서화 | □ |

---

## 19. MVP 운영 Quick Reference

```bash
# 경로
cd ~/pivotSeoul

# 상태
docker compose ps
curl -s http://localhost:8080/health
curl -s http://localhost:8080/api/ai/status | jq

# 로그
docker compose logs -f back
docker compose logs -f fastapi --tail=100

# 재시작
sudo docker-compose restart back fastapi

# 백업
docker exec pivotseoul-db pg_dump -U pivotseoul pivotseoul | gzip > ~/backups/pivot_$(date +%Y%m%d).sql.gz
```

**정상 기준 한 줄:** *4컨테이너 healthy + `/health` ok + RIR Run COMPLETED.*

---

*문서 세트: PRD / SRS / SDD / TEST-QAQC / DEPLOYMENT / **OPERATION** (MVP)*
