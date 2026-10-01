# 중단 체크포인트 — 2026-10-01

사용자의 PC 종료 준비 요청으로 작업을 중단했다. 새 구현·검사 및 자동 재개는 하지 않는다. 사용자가 재개를 요청할 때 아래 지점부터 이어간다. OS 종료/재부팅을 실행하지 않았다. 모든 코드 변경은 디스크에 저장되어 있으며 커밋·푸시하지 않았다.

## 위치와 Git

- 작업 worktree: `C:\SSAFY\PYJ\Codex\2026-10-01\task\exile-ui`
- 브랜치: `ui/workbench-explorer`
- HEAD: `a324f8fbfd6b098beb779dd3582da2ea7400c2c8`
- 원본: `C:\SSAFY\PYJ\PROJECT\Exile-Hephaistos\Exile-Hephaistos`, 이번 중단 직전 `git status --short` 출력 없음. 원본 서비스는 건드리지 않았다.
- Git 상태·diff 통계·tracked 변경 patch: 상위 `task` 디렉터리의 `stop-git-status.txt`, `stop-git-diff-stat.txt`, `stop-tracked-changes.patch`. **새 untracked 파일은 patch에 포함되지 않지만 worktree에 저장되어 있다.** checkout/reset/clean/stash로 변경을 지우지 않는다.
- `git diff --check`에서 whitespace 오류는 없었다. README와 item-state 문서의 CRLF 안내만 출력됐다.

## 구현·검증 완료 범위

- Solar Workbench 커런시 17종/Omen 8종, 실제 아이템 상태 적용, 검증된 텍스트의 보수적인 catalog 연결. 미확인 조합·효과는 차단한다.
- 정상 추가 12종 정확 전이. Workbench와 `AdditionRules`를 공유하며 정수 weight/totalWeight 비율을 유지한다.
- 독립 Support 입력: 서버 Solar base, 검증 텍스트, 수동 modifier tier. 필수 family AND 후보 중 N, 최소 tier, 중복 family/필수·후보 겹침 거부, 이미 달성·불가능 조건 판정.
- PostgreSQL의 목표 독립 지연 pool 저장과 2,048-entry memory cache. source/catalog/rule/assumption/projection namespace와 item level/rarity/family occupancy/action/omen/implicit/conditions key. 원래 tier/ID를 cache 결과로 덮어쓰지 않는다.
- 실제 fixed-sequence first-hit 합산, 성공 흡수/즉시 종료, goal qualification을 유지하는 state merge, budget lower/upper/unresolved mass, 미검토 순서 수, 확정/임시 순위 표시, 최대 5개 비교, 선택 순서의 단계별 중단 안내. 모의 확률을 제품에 넣지 않았다.
- 복구는 입력을 새 root로 교체하며 이전 계산을 삭제하는 안내/UI까지 구현했다. Annulment/Chaos 복구 실행·분기 선택 UI는 아직 없다.
- additive migration `V202610010001__support_addition_pool.sql` 추가. 기존 migration·데이터·볼륨은 보존한다.

마지막 완료된 Docker backend 검사는 **115 unit/API/architecture + 6 integration = 121개**, 실패/error/skip 0이다. `spotlessApply/check`, `check`, `generateJooq`, `bootJar`를 통과했다. 실제 PostgreSQL에서 새 memory cache 인스턴스가 persisted pool을 읽는 통합 검사도 통과했다. 원본 DB가 아닌 별도 Testcontainers DB였다.

프런트엔드는 **71개 테스트**, lint/typecheck/format/build가 통과했다. 그 뒤 `CraftSupport.tsx`의 초기 Candidate N을 0으로 변경하고 source example 설명을 추가하는 작은 수정이 저장됐다. **이 마지막 화면 수정의 포맷 검사·테스트는 아직 다시 실행하지 않았다.** 장기 검사를 새로 시작하지 말라는 중단 요청을 따랐다.

## 실제 QA 결과와 미완료 검사

최신 이미지로 별도 QA 스택을 빌드·기동했고 새 migration이 적용된 상태에서 실제 API를 사용했다. Chrome 검사에서 수동 5-affix Rare 입력, T1–T2 표기, family 겹침 방지, 실제 3개 one-slot 순서 비교, 확률 질량 보존, cold pool 저장, 순서 선택 안내, 동일/다른 목표 cache 재사용, Workbench 상태 독립, 390px 폭, 복구 입력 시 이전 결과 삭제까지 **12개 검사**를 통과했다.

그 뒤 browser helper가 아직 생성되지 않은 `.support-assessment`에 바로 `.textContent`를 읽어 종료됐다. 앱의 목표 판정 실패로 확인된 것은 아니다. `support-qa.cjs`의 해당 wait에 null guard를 넣고 다시 확인해야 한다. 이전에 helper의 CDP 반복 `const el` 선언 오류는 block scope로 수정했다. **전체 browser E2E 성공·runtime errors 0을 주장하지 않는다.**

상위 `task`에 `support-qa.cjs`, `qa-support-comparison-desktop.png`, `qa-support-comparison-mobile.png`, `support-latency-before-restart.json`이 저장되어 있다. 마지막 input-mobile screenshot은 helper 중단으로 생성되지 않았다.

5-affix Rare/Life 목표의 실제 cold/warm 관측(HTTP / backend service, 단일 관측으로 성능 보장 아님):

| 요청 | HTTP ms | service ms | cache |
|---|---:|---:|---|
| Life T2 첫 계산 | 40.3 | 25.49 | computed 3 |
| 같은 목표 재계산 | 6.7 | 1.65 | memory hit 3 |
| 다른 목표 Life T1 첫 계산 | 7.6 | 1.65 | memory hit 3 |
| 다른 목표 재계산 | 5.7 | 0.91 | memory hit 3 |

T2와 T1 목표의 실제 성공 확률이 달랐고 같은 pool을 재사용했다. **실제 backend 프로세스 재시작 후 persistedHits를 확인하는 E2E와 다른 목표의 process-cold 측정은 아직 하지 않았다.** PostgreSQL 지속성은 새 cache 인스턴스 통합 테스트까지 확인했다.

추가로 남은 화면 검사: verified text의 Support 경로, 불가능/잘못된 목표/이미 성공/완전한 실패/미계산 결과 없음/partial ranking/server error 각각의 실제 화면, 후보 N 조건의 실제 E2E, 저장 cache의 재시작 재사용, 모바일 화면 시각 점검과 불편 수정. Code tests는 first-hit 중복 방지, partial 질량 보존, family 중복·필수/후보 겹침 및 tier 조건을 검증한다.

## 우선 확인할 발견 사항

카탈로그의 `IncreaseSocketedGemLevel` family는 **12개 정의, 서로 다른 4개 stat/effect 종류(Melee/Projectile/Minion/Spell), 중복되는 4개 tier 값**을 포함한다. 현재 목표 모델은 family 단위(any subtype)이고 UI tier option을 정의마다 렌더링하여 이 family의 tier 표시가 중복된다. 중단 요청 뒤 새 구현을 시작하지 않았다.

재개 시 이 family를 일반 단일-effect family처럼 설명하지 말 것. 최소한 tier 표시 중복을 제거하고 any-subtype 의미를 명시해야 한다. 정확한 effect 종류 목표를 지원하려면 condition의 effect/stat 필터, goal qualification key와 matching/feasibility를 함께 변경·검증해야 하며 동일 family 중복 카운트 방지는 유지한다. 잘못된 effect를 원하는 효과로 판정하지 않도록 우선 다룬다.

상태 수 계산은 이 12개 정의를 **같은 family에서 선택 가능한 대안**으로 세며 동시에 중첩하지 않으므로 이 발견 때문에 무효 중첩 조합을 더한 것은 아니다. 계산 전제는 positive weight, item level 이하의 explicit, 한 family에서 0 또는 1개, rare 3 prefix/3 suffix, numeric roll 주변화이다. 낮은 차수 rare 0/1은 수동/복구 root에서 유효하나 Normal→Transmutation→Regal 경로에서는 도달하지 않는다. 계산식·전체 상태 수와 범위는 `support-transition-design.md`에 있다. 해당 문서의 일부 “proposed/not implemented” 문구는 현재 구현보다 오래되어 재개 후 갱신해야 한다.

## 정상 종료 상태와 재개 명령

진행 중 Gradle/npm 검사·Docker build는 모두 완료된 뒤 중단했다. 새로운 장기 검사는 없다. 이 작업의 headless Chrome은 CDP `Browser.close`로 종료했다.

별도 QA의 `frontend`, `app`, `postgres`, `redis`만 Compose stop으로 정상 종료했다. 컨테이너·볼륨을 삭제하지 않았다. app 로그에서 **Graceful shutdown complete / HikariPool shutdown completed**, PostgreSQL 로그에서 **checkpoint complete / database system is shut down**를 확인했다. DB 쓰기·migration이 진행 중인 상태로 강제 종료하지 않았다. 종료 상태: frontend 0, app 143(SIGTERM), postgres 0, redis 0. 원본 `exile-hephaistos-*` 서비스와 다른 프로젝트 서비스는 중지하지 않았다.

사용자가 재개를 요청한 후 기존 QA 이미지·볼륨으로 시작:

```powershell
wsl.exe -d Ubuntu --exec docker compose --project-name exile-ui-qa --env-file /mnt/c/SSAFY/PYJ/Codex/2026-10-01/task/qa.env -f /mnt/c/SSAFY/PYJ/Codex/2026-10-01/task/exile-ui/infra/compose.yaml -f /mnt/c/SSAFY/PYJ/Codex/2026-10-01/task/qa.compose.yaml --profile stack up -d --wait --wait-timeout 180 frontend
```

`qa.env`는 기존 것을 그대로 사용하며 비밀 값을 읽거나 출력하지 않는다. `down -v`/volume 삭제/초기화는 하지 않는다. 원본 서비스가 아닌 QA app만 재시작하여 persistent-hit 검사를 한다. 변경된 화면을 반영할 때는 frontend image를 다시 빌드한다.

추천의 큰 Normal-root 경로는 예산 내에 partial일 수 있다. 이를 완성된 최고 순위나 0확률로 보고하지 않는다. 모든 Solar 제작수단, 복구 분기 실행과 새 root jump, 실제 분기별 아이템 안내는 아직 전체 완료가 아니다.
