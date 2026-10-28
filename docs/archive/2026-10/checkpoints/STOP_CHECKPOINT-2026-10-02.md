# 안전중단 체크포인트 — 2026-10-02

사용자가 어제 작업 재개를 요청했고 한국시간 17:30까지 안전중단을 요구했다. 이번 재개 단계의 구현·검증을 완료하여 **15:48:55 KST (06:48:55 UTC)에 QA 서비스와 DB 정상 종료를 완료했다.** 17:20 이후 새 작업·긴 검사는 시작하지 않는다. 사용자가 다음 작업을 요청하기 전 자동 재개하지 않는다. PC 종료·재부팅, 원격 push, main/master merge, PR, 배포는 하지 않았다.

## Git과 변경 범위

- worktree: `C:\SSAFY\PYJ\Codex\2026-10-01\task\exile-ui`
- branch: `ui/workbench-explorer`
- 재개 기준 HEAD: `53d131a7b76446b528d44ab833e6a27150655a1b`. 시작 시 변경 파일이 없었으며 이전 백업 브랜치였다. 오늘 결과는 작업 브랜치의 로컬 커밋으로 보존하고 실제 SHA는 최종 보고한다. 원격에는 추가로 반영하지 않는다.
- 원본: `C:\SSAFY\PYJ\PROJECT\Exile-Hephaistos\Exile-Hephaistos`. 원본 서비스는 재시작/중지하지 않았으며 종료 후 원본 `git status --short`도 출력 없음.
- 변경 파일: `backend/src/main/java/com/poe2craft/crafting/domain/SupportGoals.java`, `backend/src/test/java/com/poe2craft/crafting/SupportGoalsTest.java`, `frontend/src/features/crafting/{CraftSupport.tsx,CraftSupport.test.tsx,supportApi.ts,craft-support.css}`, `README.md`, `docs/{support-transition-design.md,workbench-simulator.md,STOP_CHECKPOINT-2026-10-02.md}`.
- 기존 migration, DB 데이터, volume, 비밀 설정은 변경/삭제하지 않았다. `git diff --check`는 오류 없음; 일부 기존 CRLF 파일에는 LF 변환 안내만 있다.

## 오늘 구현

1. 다중 효과 family의 목표 tier 중복을 제거했다. family API에 stat identity별 `effectExamples`를 추가하고 정의를 tier/ID 순서로 안정 정렬했다. `IncreaseSocketedGemLevel`은 Melee/Projectile/Minion/Spell의 **어느 효과든** 목표로 인정한다는 설명과 네 출처 예시를 표시한다. 실제 수동 modifier 선택의 12개 대안은 그대로 유지한다. 특정 스킬 종류만 고르는 필터를 구현했다고 주장하지 않는다.
2. 결과의 확률 비교를 먼저 보여주고 계산량·시간·rule/ledger·cache 진단은 `Calculation details and sources` 안에 접어 두었다. partial 커버리지·미해결 질량·임시 순위는 계속 펼쳐 표시한다. 390px 모바일 화면에서 가로 넘침 없이 확인했다.
3. README와 Support 설계 문서의 오래된 “UI/저장 미구현·proposed” 표현을 현재 구현 및 실제 한계에 맞게 정리했다.

## 이번 소스의 검증

실제 앱과 Java/Node 검사는 프로젝트 Docker에서 실행했다. 기존 Docker Desktop이 중지되어 있어 시작했고 Windows 설치 CLI를 사용했다. 원본 Exile 컨테이너는 모두 이미 종료되어 있었으며 그 상태를 유지했다. 브라우저는 별도 `qa-chrome` 프로필의 headless Chrome, 제어 스크립트는 Docker Node 24였다.

- Backend: 기존 `exile-ui-qa-backend-check` 이미지에서 현재 `backend/src`를 bind mount하여 `./gradlew --no-daemon --init-script /qa-javaagent.init.gradle spotlessApply check generateJooq bootJar` 통과. unit/API/ArchUnit와 PostgreSQL/Redis Testcontainers 통합 검사 포함, skip으로 우회하지 않았다. 다중 효과 12개 정의 각각의 tier 판정·family 단일 카운트와 네 예시 회귀 검사를 추가했다. 일회성 검사 컨테이너를 제거하면서 오늘 XML 결과는 별도 추출하지 않았으므로 정확한 검사 개수는 이 문서에 재집계하지 않는다.
- Frontend 최종 소스: Docker Node 24에서 lint/typecheck/format:check, **72개 테스트**, production build 통과. 어제 마지막 Candidate N=0/source example 수정도 이번 검사에 포함됐다. 모바일 정보 배치 수정 후 검사를 다시 통과했다.
- 별도 QA 최신 Docker app/frontend 이미지 빌드·기동 및 Compose `config --quiet` 통과. 비밀 값은 출력하지 않았다.
- 어제 중단된 브라우저 검사: helper의 null 대기를 수정하여 **13개 검사 통과, 관측 runtime errors 0**. 실제 3개 one-slot 비교, weight 질량 보존, 목표 변경별 cache 재사용/확률 변화, Workbench 독립, 목표 달성 즉시 중단 안내, 모바일 폭, 복구 입력 시 이전 결과 제거, 이미 달성된 root의 step 0=100% 포함.
- 추가 브라우저 **17개 검사 통과, 관측 runtime errors 0**. 실제 API: 빈 목표 invalid, 스킬 family 고유 tier 4개/효과 예시 4개, suffix-only omen과 prefix-only 빈 슬롯의 완전 실패, 점유된 낮은 tier의 불가능 판정, 후보 N, verified/unverified text, 실제 Normal-root partial ranking. 화면 정보 순서·진단 펼침·모바일도 확인했다. **서비스 503·빈 partial 결과·늦게 도착하는 실제 응답의 취소는 제어된 사례**로 구분하며 실서비스 장애가 발생했다는 뜻은 아니다.
- helper의 버튼 aria-label 선택과 문자열 보간 오류 두 건은 QA 코드 오류로 수정했다. 앱 결함으로 판정하지 않았고 최종 재실행은 전부 통과했다. 기존 helper는 보존했다.

## 실제 프로세스 재시작 후 PostgreSQL cache

QA app만 Compose `restart --timeout 30 app`으로 정상 재시작한 뒤 Life T1 목표를 먼저 요청했다. 어제/오늘의 이전 process 결과와 namespace, 각 currency sequence의 확률, 질량 보존을 비교했다.

| 요청 | HTTP ms | service ms | memory hits | PostgreSQL hits | computed pools |
|---|---:|---:|---:|---:|---:|
| 재시작 후 Life T1 첫 요청 | 86.15 | 27.78 | 0 | 3 | 0 |
| 다른 목표 Life T2 다음 요청 | 10.90 | 2.90 | 3 | 0 | 0 |

두 목표 모두 재시작 전 같은 목표의 각 순서 성공 확률과 1e-12 이내 일치했다. 단일 환경/단일 측정이며 성능 보장이나 전체 큰 경로 캐시 지속성 보장을 뜻하지 않는다. QA PostgreSQL 볼륨은 유지했고 truncate/reset/export를 하지 않았다.

## 로컬 QA 증거

다음은 repo 상위 `C:\SSAFY\PYJ\Codex\2026-10-01\task`에 저장되어 있으며 커밋 대상이 아니다. 입력은 공개 catalog 기반 fixture만 사용했다.

- `support-qa-20261002.cjs`: 13개 검사. 기존 `support-qa.cjs`는 그대로 보존.
- `support-coverage-20261002.cjs`, `support-coverage-20261002.json`: 17개 화면 검사와 실제 API report.
- `qa-cache-restart-20261002.cjs`, `support-cache-restart-20261002.json`: process-cold/다른 목표 재사용 증거.
- `support-latency-20261002.json`: 재시작 전 실제 요청/응답 측정.
- `qa-support-20261002-{desktop,mobile,input-mobile,compact-mobile,partial-mobile}.png`: 화면 증거. compact-mobile/partial-mobile이 최종 UI 이미지다.

helper는 Docker의 `/qa` mount와 QA 포트 18080/18081, headless Chrome CDP 19222를 전제로 한다. 새 환경에서는 경로/포트를 검토한다. 실서비스 상태/입력으로 재사용하지 않는다. 새 PC의 코드 개발에는 Git 브랜치와 재생성 설정으로 충분하고, 이 자료는 QA 연속성 보존용이다.

## 안전 종료 확인

`docker compose --project-name exile-ui-qa --env-file ..\qa.env -f infra\compose.yaml -f ..\qa.compose.yaml stop --timeout 60 frontend app postgres redis`로 작업용 네 서비스만 정지했다.

- frontend exit 0; app exit 143(SIGTERM); PostgreSQL exit 0; Redis exit 0.
- app: `Graceful shutdown complete`, `HikariPool-1 - Shutdown completed`.
- PostgreSQL: `checkpoint complete`, `database system is shut down`.
- 네 컨테이너 `FinishedAt`은 2026-10-02 06:48:54–55 UTC. 컨테이너/볼륨은 삭제하지 않았다.
- 최종 browser helper가 CDP `Browser.close`로 종료했고 QA debugging port 19222의 Chrome 프로세스가 남지 않았다. 검사/build/browser/cache helper 컨테이너도 모두 종료했다.
- 원본 `exile-hephaistos-*`는 작업 전과 같은 기존 종료 상태(약 22시간 전)를 유지했다. 다른 프로젝트 컨테이너나 Docker Desktop 전체를 종료하지 않았다. PC 종료 명령은 실행하지 않았다.

## 다음 재개 지점과 한계

이번에 보강한 화면 흐름 외 모든 가능한 아이템/목표/화폐 조합을 E2E로 검증한 것은 아니다. 큰 Normal-root 경로는 예산 내 partial이며 0확률/확정 최고 순위로 해석하지 않는다.

우선 남은 기능은 Support에서 실제 Annulment/Chaos 복구를 실행하거나 제거/대체 결과 분기를 선택한 뒤 검증된 실제 상태로 새 root 점프하는 흐름이다. 현재는 실제 복구 결과를 수동 modifier나 verified text로 입력하는 방식만 제공한다. 복구 행동 확률을 정상 추가 성공 확률에 합산하거나 곱하지 않는다.

특정 스킬 효과 목표가 필요하면 stat/effect 필터, goal qualification key, matching/feasibility 및 회귀 검사를 함께 변경해야 한다. 현재 계약은 family-any-effect라 추측으로 특정 효과 목표를 추가하지 않았다. 수치 하한·다른 장비·미검증 제작 수단·가격/다른 가치 아이템 추천은 이번 범위에서 구현하지 않았다. continuation token이나 별도 goal-result cache도 없다.

재개 승인이 있을 때 기존 QA 설정/볼륨으로만 시작한다:

```powershell
$dockerPath = 'C:\Users\SSAFY\AppData\Local\Programs\DockerDesktop\resources\bin\docker.exe'
& $dockerPath compose --project-name exile-ui-qa --env-file ..\qa.env -f infra\compose.yaml -f ..\qa.compose.yaml --profile stack up -d --wait --wait-timeout 180 frontend
```

`qa.env`의 비밀 값은 읽거나 공유하지 않는다. 원본 프로젝트 Compose를 worktree에서 기동하지 않으며 `down -v`/volume 삭제/DB 초기화를 하지 않는다. 새 변경 뒤에만 해당 이미지를 rebuild하고 영향을 받는 검사를 수행한다.
